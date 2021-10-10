package org.openpnp.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.openpnp.Main;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.SimulationModeMachine;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Motion;
import org.openpnp.model.Motion.MotionOption;
import org.openpnp.spi.Axis;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Driver;
import org.pmw.tinylog.Logger;

public class GcodeServer extends Thread {
    final Map<String, String> commandResponses = new HashMap<>();
    final ServerSocket serverSocket;
    Driver driver;
    ReferenceMachine machine;
    /**
     * The simulated visual homing offsets are applied to what the simulated down camera sees.
     * Works like Gcode G92. Initialized with the SimulationModeMachine.getHomingError() on homing.
     */
    private AxesLocation homingOffsets = new AxesLocation();

    protected TreeMap<Double, Motion> motionPlan = new TreeMap<Double, Motion>();
    private AxesLocation machineLocation;

    private long maxDwellTimeMilliseconds = 20000;

    static final String firmware = "FIRMWARE_NAME:GcodeServer, FIRMWARE_URL:http%3A//openpnp.org, X-SOURCE_CODE_URL:https%3A//github.com/openpnp/openpnp, FIRMWARE_VERSION:"+Main.getVersion()+", "
            +"X-FIRMWARE_BUILD_DATE:Oct 23 2020 00:00:00";

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
     * retrieved by calling GcodeServer.getListenerPort().
     * @throws Exception
     */
    public GcodeServer() throws Exception {
        this(0);
    }

    public int getListenerPort() {
        return serverSocket.getLocalPort();
    }

    public Driver getDriver() {
        return driver;
    }

    public AxesLocation getHomingOffsets() {
        return homingOffsets;
    }

    public void setHomingOffsets(AxesLocation homingOffsets) {
        this.homingOffsets = homingOffsets;
    }

    public void setDriver(Driver driver) {
        this.machine = (ReferenceMachine) Configuration.get().getMachine();
        this.driver = driver;
    }

    public AxesLocation getMachineLocation() {
        if (machineLocation == null) {
            machineLocation = new AxesLocation(machine).drivenBy(driver);
        }
        return machineLocation;
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
        Logger.debug("Socket port "+getListenerPort()+" bye-bye.");
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
        M114(0),
        M115(0),
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
        private double feedRate;
        private double acceleration;
        private double jerk;
        private double unit = 1.0; // 1.0 --> mm or 25.4 --> inch
        private LengthUnit lengthUnit = LengthUnit.Millimeters; 
        private boolean absolute = true;
        private String response;

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
            while (!serverSocket.isClosed()) {
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
                            try {
                                // No canned responses. Try to interpret.
                                interpretGcode(input);
                            }
                            catch (Exception e) {
                                Logger.error(e);
                                write("*** Unknown syntax: "+e.getMessage());
                            }
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
            Logger.debug("Worker port "+getListenerPort()+" bye-bye.");
        }


        protected class GcodeWord {
            final char letter;
            final boolean dollar;
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

            public GcodeWord(char letter, boolean dollar) {
                super();
                this.letter = letter;
                this.dollar = dollar;
            }

            public void recognizeCode() {
                if (decimal == 0) {
                    decimal = 1;
                }

                String codeName = ((Character)letter).toString()+getNumberIntegral();
                for (Gcode code : Gcode.values()) {
                    if (code.toString().equals(codeName)) {
                        this.code = code;
                    }
                }
                if (this.code == null) {
                    if (letter == 'T') {
                        this.code = Gcode.Tn;
                    }
                    else if (letter == 'O') {
                        this.code = Gcode.O;
                    }
                    else if (letter == 'M' && getNumberIntegral()/100 == 1) {
                        this.code = Gcode.M1nn;
                    }
                }
            }
            public int getModalGroup() {
                return code != null ? code.modalGroup : -1;   
            }
            public boolean isDollar() {
                return dollar;
            }
        }

        public void interpretGcode(String input) throws Exception {
            // Set standard Response.
            setResponse("ok");
            // Try parse the Gcode.
            GcodeWord currentWord = null;
            int col = 0;
            boolean insideComment = false;
            boolean isDollar = false;
            List<GcodeWord> commandWords = new ArrayList<>();
            for (char ch : input.toCharArray()) {
                col++;
                if (ch == ' ') {
                    continue;
                }
                else if (ch == '$') {
                    isDollar = true;
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
                    insideComment = false;
                }
                else if (insideComment) {
                    if (currentWord != null) {
                        currentWord.comment += ch;
                    }
                    continue;
                }
                else if (ch == ';') {
                    // Comment ends parsing
                    break;
                }
                else if (Character.toUpperCase(ch) >= 'A' && Character.toUpperCase(ch) <= 'Z') {
                    if (isDollar) {
                        // we just support $H, the rest we ignore.
                        if (Character.toUpperCase(ch) != 'H') {
                            break;
                        }
                    }
                    // Finalize the running word.
                    commandWords = finalizeGcodeWord(currentWord, commandWords);
                    // And start a new one.
                    currentWord = new GcodeWord(Character.toUpperCase(ch), isDollar);
                    isDollar = false;
                }
                else if (currentWord == null) {
                    if (isDollar) {
                        // custom command, probably TinyG setting, ignore.
                        break;
                    }
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
            // Finalize the last word.
            commandWords = finalizeGcodeWord(currentWord, commandWords);
            // Now simulate the Gcode command-.
            simulateGcode(commandWords);
            // Send Response.
            write(response);
        }

        public List<GcodeWord> finalizeGcodeWord(GcodeWord currentWord,
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
                if (word.isDollar()) {
                    regurg.append('$');
                }
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


                Logger.debug("Parsed Gcode: "+toString(commandWords));

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

                // Get some general params.
                GcodeWord sWord = getLetterWord('S', commandWords);
                GcodeWord pWord = getLetterWord('P', commandWords);
                AxesLocation axesLocation = getMachineLocation();
                AxesLocation axesGiven = AxesLocation.zero;
                for (Axis axis : machine.getAxes()) {
                    if (axis instanceof ControllerAxis) {
                        ControllerAxis controllerAxis = (ControllerAxis) axis;
                        if (controllerAxis.getDriver() == getDriver()) {
                            String letter = controllerAxis.getLetter(); 
                            if (letter.isEmpty() || letter.length() > 1) {
                                // We're in GcodeServer simulation on this machine. Provide some useful defaults.  
                                if (axis.getName().equals("x") || axis.getName().equals("y") 
                                        || axis.getName().toLowerCase().substring(0, 1).equals("z")) {
                                    controllerAxis.setLetter(axis.getName().toUpperCase().substring(0, 1));
                                }
                                else if (axis.getName().equals("C") || axis.getName().equals("C1")) {
                                    controllerAxis.setLetter("A");
                                }
                                else if (axis.getName().equals("C2")) {
                                    controllerAxis.setLetter("B");
                                }
                                else {
                                    throw new Exception("Invalid letter on axis "+axis.getName());
                                }
                                letter = controllerAxis.getLetter(); 
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

                GcodeWord m114Word = getCodeWord(Gcode.M114, commandWords);
                if (m114Word != null) {
                    StringBuilder response = new StringBuilder();
                    AxesLocation reportedLocation = machineLocation;
                    if (m114Word.getNumberFraction() == 0) {
                        response.append("ok C:");
                    }
                    else if (m114Word.getNumberFraction() == 1) {
                        response.append("ok WCS:");
                        double now = NanosecondTime.getRuntimeSeconds();
                        Motion motion = getMomentaryMotion(now);
                        reportedLocation = motion.getMomentaryLocation(now - motion.getPlannedTime0());
                    }
                    for (Axis axis : machine.getAxes()) {
                        if (axis instanceof ControllerAxis) {
                            if (((ControllerAxis) axis).getDriver() == getDriver()) {
                                response.append(' ');
                                response.append(((ControllerAxis) axis).getLetter());
                                response.append(':');
                                response.append(String.format(Locale.US, "%.4f", reportedLocation.getCoordinate(axis, lengthUnit)));
                            }
                        }
                    }
                    setResponse(response.toString());
                }

                GcodeWord m115Word = getCodeWord(Gcode.M115, commandWords);
                if (m115Word != null) {
                    int axes = 0;
                    int paxes = 0;
                    for (ControllerAxis axis1 :  getMachineLocation().getControllerAxes()) {
                        axes++;
                        if (!axis1.isRotationalOnController()) {
                            paxes++;
                        }
                    }
                    setResponse(firmware+", X-AXES:"+axes+", X-PAXES:"+paxes+"\nok");
                }

                // Acceleration
                GcodeWord m204Word = getCodeWord(Gcode.M204, commandWords);
                if (m204Word != null && sWord != null) {
                    acceleration = sWord.getNumberDouble()*unit;
                }

                // Compute the wait or dwell time. Start with the motion plan completion time. 
                long dwellMilliseconds = (motionPlan.isEmpty() ? 
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

                GcodeWord g92Word = getCodeWord(Gcode.G92, commandWords);
                if (g92Word != null) {
                    doDwell = true; // ???
                }

                // Wait for completion command.
                if (m400Word != null) {
                    doDwell = true;
                }

                if (doDwell && dwellMilliseconds > 0) {
                    // There is a command, that waits for completion/dwells.
                    if (dwellMilliseconds > maxDwellTimeMilliseconds) {
                        // Be reasonable
                        Logger.warn("Dwell time limited from "+(dwellMilliseconds/1000.)+"s");
                        dwellMilliseconds = maxDwellTimeMilliseconds;
                    }
                    Logger.trace("Waiting "+dwellMilliseconds+"ms");
                    Thread.sleep(dwellMilliseconds);

                    // Remove old stuff.
                    double time = NanosecondTime.getRuntimeSeconds() - 30;
                    while (motionPlan.isEmpty() == false && motionPlan.firstKey() < time) {
                        motionPlan.remove(motionPlan.firstKey());
                    }
                }

                // Set global offsets. 
                if (g92Word != null) {
                    homingOffsets = axesLocation.subtract(machineLocation).add(homingOffsets);
                    machineLocation = machineLocation.put(axesLocation);
                    Logger.trace("New global offset location: "+machineLocation);
                }

                // Set unit. 
                GcodeWord g21Word = getCodeWord(Gcode.G21, commandWords);
                GcodeWord g20Word = getCodeWord(Gcode.G20, commandWords);
                if (g21Word != null) {
                    // Millimeters
                    unit = 1.0;
                    lengthUnit = LengthUnit.Millimeters;
                }
                if (g20Word != null) {
                    // Inches
                    unit = 25.4;
                    lengthUnit = LengthUnit.Inches;
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

                // Motion.
                GcodeWord g0Word = getCodeWord(Gcode.G0, commandWords);
                GcodeWord g1Word = getCodeWord(Gcode.G1, commandWords);
                GcodeWord hWord = getLetterWord('H', commandWords);
                GcodeWord g28Word = getCodeWord(Gcode.G28, commandWords);
                if (g0Word != null || g1Word != null || g28Word != null || (hWord != null && hWord.isDollar())) {
                    double speed = 1.0;
                    if (g28Word != null || hWord != null && hWord.isDollar()) {
                        // Homing command.
                        // Handle like a move. 
                        // But restore the simulated homing error.
                        Location homingError;
                        if (machine instanceof SimulationModeMachine) {
                            homingError = ((SimulationModeMachine) machine).getHomingError();
                        }
                        else {
                            homingError = new Location(AxesLocation.getUnits());
                        }
                        homingOffsets = new AxesLocation(machine, getDriver(), (axis) 
                                -> (axis.getType() == Axis.Type.X ? homingError.getLengthX() :
                                    axis.getType() == Axis.Type.Y ? homingError.getLengthY() : 
                                        null));
                        // Make it slower.
                        speed = 0.5;
                        if (axesGiven.isEmpty()) {
                            // G28 has not axes, use preset homing coordinates.
                            axesLocation = new AxesLocation(machineLocation.getAxes(getDriver()), 
                                    (a) -> a.getHomeCoordinate());
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
                    synchronized (motionPlan) {
                        double t = NanosecondTime.getRuntimeSeconds();
                        if (motionPlan.isEmpty() == false && motionPlan.lastKey() > t) {
                            // Append to a plan that is still running. 
                            t = motionPlan.lastKey();
                        }
                        // Put into timed plan.
                        t += motion.getTime();
                        motionPlan.put(t, motion);
                        motion.setPlannedTime1(t);
                    }
                    // Store new location.
                    Logger.trace("Move takes "+(motion.getTime()*1000)+" ms");
                    machineLocation = machineLocation.put(axesLocation);
                    Logger.trace("New location: "+machineLocation);
                }
            }
        }

        private void setResponse(String response) {
            this.response = response;
        }
    }

    public Motion getMomentaryMotion(double time) {
        Map.Entry<Double, Motion> entry1; 
        synchronized (motionPlan) {
            entry1 = motionPlan.higherEntry(time);
        }
        if (entry1 != null) {
            // Return the current motion.
            Motion motion = entry1.getValue();
            return motion;
        }
        else {
            // Nothing in the plan or machine stopped before this time, just get the current axes location.
            Motion motion = new Motion( 
                    null, 
                    getMachineLocation(),
                    getMachineLocation(),
                    1.0,
                    MotionOption.Stillstand);
            // Anchor it in real-time.
            motion.setPlannedTime1(time);
            return motion;
        }
    }

    public static String getGenericFirmware() {
        return firmware;
    }
}
