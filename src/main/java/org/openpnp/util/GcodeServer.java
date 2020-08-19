package org.openpnp.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.SimulationModeMachine;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Location;
import org.openpnp.model.Motion;
import org.openpnp.model.Motion.MotionOption;
import org.openpnp.spi.Axis;
import org.openpnp.spi.ControllerAxis;
import org.pmw.tinylog.Logger;

public class GcodeServer extends Thread {
    final Map<String, String> commandResponses = new HashMap<>();
    final ServerSocket serverSocket;
    ReferenceDriver driver;
    SimulationModeMachine machine;
    /**
     * The simulated visual homing offsets are applied to what the simulated down camera sees.
     * Works like Gcode G92. Initialized with the SimulationModeMachine.getHomingError() on homing.
     */
    private AxesLocation homingOffsets = new AxesLocation();

    protected TreeMap<Double, Motion> motionPlan = new TreeMap<Double, Motion>();


    /**
     * Create a GcodeServer listening on the given port.
     * @param port
     * @throws Exception
     */
    public GcodeServer(int port) throws Exception {
        serverSocket = new ServerSocket(port);
        Thread thread = new Thread(this);
        thread.setDaemon(false);
        thread.start();
    }

    /**
     * Create a GcodeServer listening on a random port. The chosen port can be
     * retrived by calling GcodeServer.getListenerPort().
     * @throws Exception
     */
    public GcodeServer() throws Exception {
        this(0);
    }

    public int getListenerPort() {
        return serverSocket.getLocalPort();
    }

    public ReferenceDriver getDriver() {
        return driver;
    }

    public void setDriver(ReferenceDriver driver) {
        this.machine = SimulationModeMachine.getSimulationModeMachine();
        this.driver = driver;
    }

    public void addCommandResponse(String command, String response) {
        commandResponses.put(command, response);
    }

    public void shutdown() {
        try {
            serverSocket.close();
        }
        catch (Exception e) {

        }
    }

    public void run() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                new Worker(socket).start();
            }
            catch (Exception e) {
            }
        }
    }

    enum Gcode {
        // See http://linuxcnc.org/docs/2.4/html/gcode_overview.html#sec:Modal-Groups
        G0(1), G1(1), G2(1), G3(1), G33(1), G38(1), G73(1), G76(1), G80(1), G81(1),
           G82(1), G83(1), G84(1), G85(1), G86(1), G87(1), G88(1), G89(1),
        G17(2), G18(2), G19(2),
        G7(3), G8(3),
        G90(4), G91(4),
        G93(5), G94(5),
        G20(6), G21(6),
        G40(7), G41(7), G42(7),
        G43(8), G49(8),
        G98(9), G99(9),
        G54(10), G55(10), G56(10), G57(10), G58(10), G59(10), 
        M0(11), M1(11), M2(11), M30(11), M60(11),
        M6(12), Tn(12),  
        M3(13), M4(13), M5(13),
        M7(14), M8(14), M9(14),
        M48(15), M49(15),
        O(16),
        G4(0), G10(0), G28(0), G30(0), G53(0), G92(0), M1nn(0), 
        // Smoothie plus
        M204(12),
        M400(0);

        private int modalGroup;
        Gcode(int modalGroup) {
            this.modalGroup = modalGroup;
        }
        public int getModalGroup() {
            return modalGroup;
        }
    }

    class Worker extends Thread {
        final Socket socket;
        final InputStream input;
        final OutputStream output;
        private AxesLocation machineLocation;
        private double feedRate;
        private double acceleration;
        private double jerk;
        private double unit = 1.0; // 1.0 --> mm or 25.4 --> inch
        private boolean absolute = true;

        public Worker(Socket socket) throws Exception {
            this.socket = socket;
            input = socket.getInputStream();
            output = socket.getOutputStream();
        }

        String read() throws Exception {
            StringBuffer line = new StringBuffer();
            while (true) {
                int ch = input.read();
                if (ch == -1) {
                    return null;
                }
                else if (ch == '\n' || ch == '\r') {
                    if (line.length() > 0) {
                        return line.toString();
                    }
                }
                else {
                    line.append((char) ch);
                }
            }
        }

        void write(String s) throws Exception {
            output.write((s + "\n").getBytes("UTF8"));
        }

        public void run() {
            while (true) {
                try {
                    String input = read();
                    if (input != null) {
                        // Canned responses.
                        String response = null;
                        response = commandResponses.get(input.trim());
                        if (response != null) {
                            write(response);
                        }
                        else if (driver != null) {
                            // No canned responses. Try to interpret.
                            interpretGcode(input);
                        }
                        else {    
                            write("error:unknown command");
                        }
                    }
                }
                catch (Exception e) {
                    Logger.error(e);
                    break;
                }
            }
            try {
                input.close();
            }
            catch (Exception e) {
            }
            try {
                output.close();
            }
            catch (Exception e) {
            }
            try {
                socket.close();
            }
            catch (Exception e) {
            }
        }


        protected class GcodeWord {
            final char letter;
            int number = 0;
            int signum = 0;
            int digits = 0;
            int decimal = 0;
            String comment = "";
            private Gcode code;


            int getNumberIntegral() {
                return number/decimal;
            }
            int getNumberFraction() {
                return number % decimal;
            }
            double getNumberDouble() {
                return signum*((double)number)/decimal;
            }

            public GcodeWord(char letter) {
                super();
                this.letter = letter;
            }

            public void recognizeCode() {
                if (decimal == 0) {
                    decimal = 1;
                }

                if (letter == 'T') {
                    this.code = Gcode.Tn;
                }
                else if (letter == 'O') {
                    this.code = Gcode.O;
                }
                else if (letter == 'M' && getNumberIntegral()/100 == 1) {
                    this.code = Gcode.M1nn;
                }
                else {
                    String codeName = ((Character)letter).toString()+getNumberIntegral();
                    for (Gcode code : Gcode.values()) {
                        if (code.toString().equals(codeName)) {
                            this.code = code;
                        }
                    }
                }
            }
            public int getModalGroup() {
                return code != null ? code.modalGroup : -1;   
            }
        }

        public void interpretGcode(String input) throws Exception {
            GcodeWord currentWord = null;
            int col = 0;
            boolean insideComment = false;
            List<GcodeWord> commandWords = new ArrayList<>();
            for (char ch : input.toCharArray()) {
                col++;
                if (ch == ' ') {
                    continue;
                }
                else if (ch == '(') {
                    if (insideComment) {
                        throw new Exception("Nested comment at "+col+": "+input);
                    }
                    insideComment = true;
                    if (currentWord != null) {
                        currentWord.comment = "";
                    }
                }
                else if (ch == ')') {
                    if (!insideComment) {
                        throw new Exception("Mismatched comment at "+col+": "+input);
                    }
                    insideComment = true;
                }
                else if (insideComment) {
                    if (currentWord != null) {
                        currentWord.comment += ch;
                    }
                    continue;
                }
                else if (ch == ';') {
                    break;
                }
                else if (Character.toUpperCase(ch) >= 'A' && Character.toUpperCase(ch) <= 'Z') {
                    commandWords = handleGcodeWord(currentWord, commandWords);
                    currentWord = new GcodeWord(Character.toUpperCase(ch));
                }
                else if (currentWord == null) {
                    // the remaining character are only allowed inside a word 
                    throw new Exception("Syntax error at col "+col+" '"+ch+"' unexpected: "+input);
                }
                else if (ch == '+') {
                    if (currentWord.signum != 0) {
                        throw new Exception("Syntax error at col "+col+" '"+ch+"' unexpected: "+input);
                    }
                    currentWord.signum = +1;
                }
                else if (ch == '-') {
                    if (currentWord.signum != 0) {
                        throw new Exception("Syntax error at col "+col+" '"+ch+"' unexpected: "+input);
                    }
                    currentWord.signum = -1;
                }
                else if (ch >= '0' && ch <= '9') {
                    currentWord.number *= 10;
                    currentWord.number += (int)(ch - '0');
                    currentWord.decimal *= 10;
                    if (currentWord.signum == 0) {
                        currentWord.signum = 1;
                    }
                }
                else if (ch == '.') {
                    if (currentWord.decimal != 0) {
                        throw new Exception("Syntax error at col "+col+" '"+ch+"' unexpected: "+input);
                    }
                    currentWord.decimal = 1;
                    if (currentWord.signum == 0) {
                        currentWord.signum = 1;
                    }
                }
            }
            commandWords = handleGcodeWord(currentWord, commandWords);
            simulateGcode(commandWords);
        }

        public List<GcodeWord> handleGcodeWord(GcodeWord currentWord,
                List<GcodeWord> commandWords) throws Exception {
            if (currentWord != null) {
                // Finish the last word.
                currentWord.recognizeCode();
                int modalGroup = currentWord.getModalGroup();
                if (modalGroup != -1) {
                    for (GcodeWord word : commandWords) {
                        if (word.getModalGroup() == modalGroup) {
                            // This is the same modal group, therefore a new command is begun. 
                            // Note, this is strictly speaking not standard RS274/NGC G-code but as many controllers do it, we must cope with it.
                            // See http://linuxcnc.org/docs/2.4/html/gcode_overview.html#r1_10
                            simulateGcode(commandWords);
                            // Begin a new command.
                            commandWords = new ArrayList<>();
                            break;
                        }
                    }
                }
                // Add the finished Word to the command.
                commandWords.add(currentWord);
            }
            return commandWords;
        }

        GcodeWord getLetterWord(char letter, List<GcodeWord> commandWords) {
            for (GcodeWord word : commandWords) {
                if (word.letter == letter) {
                    return word;
                }
            }
            return null;
        }

        GcodeWord getCodeWord(Gcode code, List<GcodeWord> commandWords) {
            for (GcodeWord word : commandWords) {
                if (word.code == code) {
                    return word;
                }
            }
            return null;
        }

        public String toString(List<GcodeWord> commandWords) {
            StringBuilder regurg = new StringBuilder();
            for (GcodeWord word : commandWords) {
                regurg.append(word.letter);
                if (word.signum < 0) {
                    regurg.append('-');
                }
                regurg.append(word.getNumberIntegral());
                if (word.getNumberFraction() != 0) {
                    regurg.append('.');
                    regurg.append(word.getNumberFraction());
                }
                regurg.append(' ');
            }
            return regurg.toString();
        }

        /**
         * A very crude simulation of some known G-code commands. 
         * 
         * @param commandWords
         * @throws Exception 
         */
        private void simulateGcode(List<GcodeWord> commandWords) throws Exception {
            if (!commandWords.isEmpty()) {

                
                Logger.debug(toString(commandWords));
                
                // Order of execution
                //
                // See http://linuxcnc.org/docs/2.4/html/gcode_overview.html#sec:Order-of-Execution
                //    1.  Comment (including message)
                //    2.  Set feed rate mode (G93, G94). 
                //    3.  Set feed rate (F). 
                //    4.  Set spindle speed (S). 
                //    5.  Select tool (T). 
                //    6.  Change tool (M6).
                //    7.  Spindle on or off (M3, M4, M5).
                //    8.  Coolant on or off (M7, M8, M9).
                //    9.  Enable or disable overrides (M48, M49). 
                //    10. Dwell (G4). 
                //    11. Set active plane (G17, G18, G19). 
                //    12. Set length units (G20, G21).
                //    13. Cutter radius compensation on or off (G40, G41, G42) 
                //    14. Cutter length compensation on or off (G43, G49) 
                //    15. Coordinate system selection (G54, G55, G56, G57, G58, G59, G59.1, G59.2, G59.3). 
                //    16. Set path control mode (G61, G61.1, G64)
                //    17. Set distance mode (G90, G91). 
                //    18. Set retract mode (G98, G99).
                //    19. Go to reference location (G28, G30) or change coordinate system data (G10) or set axis offsets (G92, G92.1, G92.2, G94). 
                //    20. Perform motion (G0 to G3, G33, G73, G76, G80 to G89), as modified (possibly) by G53. 
                //    21. Stop (M0, M1, M2, M30, M60).

                if (machineLocation == null) {
                    machineLocation = new AxesLocation(machine).drivenBy(getDriver());
                }
                // Get some general params.
                GcodeWord sWord = getLetterWord('S', commandWords);
                GcodeWord pWord = getLetterWord('P', commandWords);
                AxesLocation axesLocation = machineLocation;
                AxesLocation axesGiven = AxesLocation.zero;
                for (Axis axis : machine.getAxes()) {
                    if (axis instanceof ControllerAxis) {
                        if (((ControllerAxis) axis).getDriver() == getDriver()) {
                            String letter = ((ControllerAxis) axis).getLetter(); 
                            if (letter.isEmpty() || letter.length() > 1) {
                                throw new Exception("Invalid letter on axis "+axis.getName());
                            }
                            GcodeWord axisWord = getLetterWord(letter.toCharArray()[0], commandWords);
                            if (axisWord != null) {
                                if (absolute) {
                                    axesLocation = axesLocation.put(new AxesLocation(axis, axisWord.getNumberDouble()*unit));
                                }
                                else {
                                    axesLocation = axesLocation.add(new AxesLocation(axis, axisWord.getNumberDouble()*unit));
                                }
                                axesGiven = axesGiven.put(new AxesLocation(axis, axisWord.getNumberDouble()*unit));
                            }
                        }
                    }
                }

                // Feed rate
                GcodeWord fWord = getLetterWord('F', commandWords);
                if (fWord != null) {
                    feedRate = fWord.getNumberDouble()*unit/60; // convert per second
                }
                
                // Acceleration
                GcodeWord m204Word = getCodeWord(Gcode.M204, commandWords);
                if (m204Word != null && sWord != null) {
                    acceleration = sWord.getNumberDouble()*unit;
                }

                // Compute the wait or dwell time. Start with the motion plan completion time. 
                int dwellMilliseconds = (motionPlan.isEmpty() ? 
                        0 : (int)Math.max(0, (motionPlan.lastKey() - NanosecondTime.getRuntimeSeconds())*1000));
                //Logger.debug("Motion ongoing for +"+dwellMilliseconds+" ms, lastKey = "+(motionPlan.isEmpty() ? 0 : motionPlan.lastKey())+", now="+NanosecondTime.getRuntimeSeconds());
                boolean doDwell = false;
                GcodeWord g4Word = getCodeWord(Gcode.G4, commandWords);
                GcodeWord m400Word = getCodeWord(Gcode.M400, commandWords);
                
                // Dwell command
                if (g4Word != null) {
                    if (pWord != null) {
                        dwellMilliseconds += pWord.getNumberIntegral();
                    }
                    if (sWord != null) {
                        dwellMilliseconds += (int)(sWord.getNumberDouble()*1000);
                    }
                    doDwell = true;
                }
                
                // Wait for completion command.
                if (m400Word != null) {
                    doDwell = true;
                }
                

                // Set global offsets. 
                GcodeWord g92Word = getCodeWord(Gcode.G92, commandWords);
                if (g92Word != null) {
                    homingOffsets = axesLocation.subtract(machineLocation).add(homingOffsets);
                    machineLocation = axesLocation;
                    doDwell = true;
                }

                // Set unit. 
                GcodeWord g21Word = getCodeWord(Gcode.G21, commandWords);
                GcodeWord g20Word = getCodeWord(Gcode.G20, commandWords);
                if (g21Word != null) {
                    // Millimeters
                    unit = 1.0;
                }
                if (g20Word != null) {
                    // Inches
                    unit = 25.4;
                }

                GcodeWord g90Word = getCodeWord(Gcode.G90, commandWords);
                GcodeWord g91Word = getCodeWord(Gcode.G91, commandWords);
                if (g90Word != null) {
                    // Absolute mode.
                    absolute = true;
                }
                if (g91Word != null) {
                    // Relative mode.
                    absolute = false;
                }

                
                if (doDwell && dwellMilliseconds > 0) {
                    // There is a command, that waits for completion/dwells.
                    if (dwellMilliseconds > 10000) {
                        // Be reasonable
                        Logger.warn("Dwell time limited to 10s from "+(dwellMilliseconds/1000.)+"s");
                        dwellMilliseconds = 10000;
                    }
                    Logger.trace("Waiting "+dwellMilliseconds+"ms");
                    Thread.sleep(dwellMilliseconds);
                    
                    // Remove old stuff.
                    double time = NanosecondTime.getRuntimeSeconds() - 30;
                    while (motionPlan.isEmpty() == false && motionPlan.firstKey() < time) {
                        motionPlan.remove(motionPlan.firstKey());
                    }
                }

                // Motion.
                GcodeWord g0Word = getCodeWord(Gcode.G0, commandWords);
                GcodeWord g1Word = getCodeWord(Gcode.G1, commandWords);
                GcodeWord g28Word = getCodeWord(Gcode.G28, commandWords);
                if (g0Word != null || g1Word != null || g28Word != null) {
                    double speed = 1.0;
                    if (g28Word != null) {
                        // Handle homing like a move. 
                        // But restore the simulated homing error.
                        Location homingError = machine.getHomingError();
                        homingOffsets = new AxesLocation(machine, getDriver(), (axis) 
                                -> (axis.getType() == Axis.Type.X ? homingError.getLengthX() :
                                    axis.getType() == Axis.Type.Y ? homingError.getLengthY() : 
                                        null));
                        // Make it slower.
                        speed = 0.5;
                        if (axesGiven.isEmpty()) {
                            // G28 has not axes, use preset homing coordinates.
                            axesLocation = new AxesLocation(machineLocation.getAxes(getDriver()), (a) -> a.getHomeCoordinate());
                        }
                        else {
                            // If we're in relative mode, we still want this absolute.
                            axesLocation = machineLocation.put(axesGiven);
                        }
                    }
                    // Create the motion.
                    Motion motion = new Motion(null, machineLocation, axesLocation, speed, 
                            feedRate, acceleration, jerk,
                            (g0Word != null ? MotionOption.UncoordinatedMotion.flag() : 0));
                    double t = NanosecondTime.getRuntimeSeconds();
                    if (motionPlan.isEmpty() == false && motionPlan.lastKey() > t) {
                        // Append to a plan that is still running. 
                        t = motionPlan.lastKey();
                    }
                    // Put into timed plan.
                    t += motion.getTime();
                    Logger.debug("move takes "+(motion.getTime()*1000)+" ms");
                    motionPlan.put(t, motion);
                    // Store new location.
                    machineLocation = machineLocation.put(axesLocation);
                }

                // Standard response.
                write("ok");
            }
        }
    }

    public synchronized Motion getMomentaryMotion(double time) {
        Map.Entry<Double, Motion> entry1 = motionPlan.higherEntry(time);
        if (entry1 != null) {
            // Return the current motion.
            Motion motion = entry1.getValue();
            return motion;
        }
        else {
            // Nothing in the plan or machine stopped before this time, just get the current axes location.
            AxesLocation currentLocation = new AxesLocation(machine); 
            Motion motion = new Motion( 
                    null, 
                    currentLocation,
                    currentLocation,
                    1.0,
                    MotionOption.Stillstand);
            // Anchor it in real-time.
            motion.setPlannedTime1(time);
            return motion;
        }
    }

    public static void main(String[] args) throws Exception {
        GcodeServer server = new GcodeServer();
    }
}
