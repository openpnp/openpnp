package org.openpnp.machine.reference.driver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.axis.ReferenceCamClockwiseAxis;
import org.openpnp.machine.reference.axis.ReferenceCamCounterClockwiseAxis;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.axis.ReferenceLinearTransformAxis;
import org.openpnp.machine.reference.axis.ReferenceMappedAxis;
import org.openpnp.machine.reference.driver.wizards.GcodeDriverConsole;
import org.openpnp.machine.reference.driver.wizards.GcodeDriverGcodes;
import org.openpnp.machine.reference.driver.wizards.GcodeDriverSettings;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Motion;
import org.openpnp.model.Motion.MotionOption;
import org.openpnp.model.Named;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.Camera;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.base.AbstractAxis;
import org.openpnp.spi.base.AbstractCamera;
import org.openpnp.spi.base.AbstractHead;
import org.openpnp.spi.base.AbstractHead.VisualHomingMethod;
import org.openpnp.spi.base.AbstractHeadMountable;
import org.openpnp.spi.base.AbstractSingleTransformedAxis;
import org.openpnp.spi.base.AbstractTransformedAxis;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;

import com.google.common.base.Joiner;

@Root
public class GcodeDriver extends AbstractReferenceDriver implements Named, Runnable {
    public enum CommandType {
        COMMAND_CONFIRM_REGEX,
        POSITION_REPORT_REGEX,
        COMMAND_ERROR_REGEX,
        CONNECT_COMMAND,
        ENABLE_COMMAND,
        DISABLE_COMMAND,
        @Deprecated
        POST_VISION_HOME_COMMAND,
        HOME_COMMAND("Id", "Name"),
        HOME_COMPLETE_REGEX(true),
        SET_GLOBAL_OFFSETS_COMMAND("Id", "Name", "X", "Y", "Z", "Rotation"),
        @Deprecated
        PUMP_ON_COMMAND,
        @Deprecated
        PUMP_OFF_COMMAND,
        MOVE_TO_COMMAND(true, "Id", "Name", "FeedRate", "X", "Y", "Z", "Rotation"),
        MOVE_TO_COMPLETE_COMMAND(true),
        MOVE_TO_COMPLETE_REGEX(true),
        @Deprecated
        PICK_COMMAND(true, "Id", "Name", "VacuumLevelPartOn", "VacuumLevelPartOff"),
        @Deprecated
        PLACE_COMMAND(true, "Id", "Name"),
        ACTUATE_BOOLEAN_COMMAND(true, "Id", "Name", "Index", "BooleanValue", "True", "False"),
        ACTUATE_DOUBLE_COMMAND(true, "Id", "Name", "Index", "DoubleValue", "IntegerValue"),
        ACTUATE_STRING_COMMAND(true, "Id", "Name", "Index", "StringValue"),
        ACTUATOR_READ_COMMAND(true, "Id", "Name", "Index"),
        ACTUATOR_READ_WITH_DOUBLE_COMMAND(true, "Id", "Name", "Index", "DoubleValue", "IntegerValue"),
        ACTUATOR_READ_REGEX(true);

        final boolean headMountable;
        final String[] variableNames;

        private CommandType() {
            this(false);
        }

        private CommandType(boolean headMountable) {
            this(headMountable, new String[] {});
        }

        private CommandType(String... variableNames) {
            this(false, variableNames);
        }

        private CommandType(boolean headMountable, String... variableNames) {
            this.headMountable = headMountable;
            this.variableNames = variableNames;
        }

        public boolean isHeadMountable() {
            return headMountable;
        }
    }

    public static class Command {
        @Attribute(required = false)
        public String headMountableId;

        @Attribute(required = true)
        public CommandType type;

        @ElementList(required = false, inline = true, entry = "text", data = true)
        public ArrayList<String> commands = new ArrayList<>();

        public Command(String headMountableId, CommandType type, String text) {
            this.headMountableId = headMountableId;
            this.type = type;
            setCommand(text);
        }

        public void setCommand(String text) {
            this.commands.clear();
            if (text != null) {
                text = text.trim();
                text = text.replaceAll("\r", "");
                String[] commands = text.split("\n");
                this.commands.addAll(Arrays.asList(commands));
            }
        }

        public String getCommand() {
            return Joiner.on('\n').join(commands);
        }

        private Command() {

        }
    }

    @Attribute(required = false)
    protected LengthUnit units = LengthUnit.Millimeters;

    @Attribute(required = false)
    protected int maxFeedRate = 1000;

    @Deprecated
    @Attribute(required = false)
    protected double backlashOffsetX = -1;

    @Deprecated
    @Attribute(required = false)
    protected double backlashOffsetY = -1;

    @Deprecated
    @Attribute(required = false)
    protected double backlashOffsetZ = 0;

    @Deprecated
    @Attribute(required = false)
    protected double backlashOffsetR = 0;

    @Deprecated
    @Attribute(required = false)
    protected double nonSquarenessFactor = 0;

    @Attribute(required = false)
    protected double backlashFeedRateFactor = 0.1;

    @Attribute(required = false)
    protected int timeoutMilliseconds = 5000;

    @Attribute(required = false)
    protected int connectWaitTimeMilliseconds = 3000;

    @Deprecated
    @Attribute(required = false)
    protected boolean visualHomingEnabled = true;

    @Attribute(required = false)
    protected boolean backslashEscapedCharactersEnabled = false;

    @Deprecated
    @Element(required = false)
    protected Location homingFiducialLocation = new Location(LengthUnit.Millimeters);

    @Attribute(required = false)
    boolean supportingPreMove = false;

    @Attribute(required = false)
    boolean usingLetterVariables = true;

    @ElementList(required = false, inline = true)
    public ArrayList<Command> commands = new ArrayList<>();

    @Deprecated
    @ElementList(required = false)
    protected List<GcodeDriver> subDrivers = null;

    @Deprecated
    @ElementList(required = false)
    protected List<Axis> axes = null;

    private Thread readerThread;
    private boolean disconnectRequested;
    private boolean connected;
    private LinkedBlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();

    @Commit
    public void commit() {
        super.commit();
    }

    @Override
    public void createDefaults() throws Exception {
        createAxisMappingDefaults((ReferenceMachine) Configuration.get().getMachine());

        commands = new ArrayList<>();
        commands.add(new Command(null, CommandType.COMMAND_CONFIRM_REGEX, "^ok.*"));
        commands.add(new Command(null, CommandType.CONNECT_COMMAND, "G21 ; Set millimeters mode\nG90 ; Set absolute positioning mode\nM82 ; Set absolute mode for extruder"));
        commands.add(new Command(null, CommandType.HOME_COMMAND, "G28 ; Home all axes"));
        commands.add(new Command(null, CommandType.MOVE_TO_COMMAND, "G0 {XL}{X:%.4f} {YL}{Y:%.4f} {ZL}{Z:%.4f} {RotationL}{Rotation:%.4f} F{FeedRate:%.0f} ; Send standard Gcode move"));
        commands.add(new Command(null, CommandType.MOVE_TO_COMPLETE_COMMAND, "M400 ; Wait for moves to complete before returning"));
    }

    public synchronized void connect() throws Exception {
        getCommunications().connect();

        connected = false;
        readerThread = new Thread(this);
        readerThread.setDaemon(true);
        readerThread.start();

        // Wait a bit while the controller starts up
        Thread.sleep(connectWaitTimeMilliseconds);

        // Consume any startup messages
        try {
            while (!sendCommand(null, 250).isEmpty()) {

            }
        }
        catch (Exception e) {

        }

        // Disable the machine
        setEnabled(false);

        // Send startup Gcode
        sendGcode(getCommand(null, CommandType.CONNECT_COMMAND));

        connected = true;
    }

    @Override
    public void setEnabled(boolean enabled) throws Exception {
        if (enabled && !connected) {
            connect();
        }
        if (connected) {
            if (enabled) {
                sendGcode(getCommand(null, CommandType.ENABLE_COMMAND));
            }
            else {
                sendGcode(getCommand(null, CommandType.DISABLE_COMMAND));
            }
        }

        if (connected && !enabled) {
            if (!connectionKeepAlive) {
                disconnect();
            }
        }
    }

    @Override
    public void home(ReferenceMachine machine) throws Exception {
        // Home is sent with an infinite timeout since it's tough to tell how long it will
        // take.
        String command = getCommand(null, CommandType.HOME_COMMAND);
        // legacy head support
        Head head = machine.getDefaultHead();
        command = substituteVariable(command, "Id", head.getId()); 
        command = substituteVariable(command, "Name", head.getName());
        long timeout = -1;
        List<String> responses = sendGcode(command, timeout);

        // Check home complete response against user's regex
        String homeCompleteRegex = getCommand(null, CommandType.HOME_COMPLETE_REGEX);
        if (homeCompleteRegex != null) {
            if (timeout == -1) {
                timeout = Long.MAX_VALUE;
            }
            if (!containsMatch(responses, homeCompleteRegex)) {
                long t = System.currentTimeMillis();
                boolean done = false;
                while (!done && System.currentTimeMillis() - t < timeout) {
                    done = containsMatch(sendCommand(null, 250), homeCompleteRegex);
                }
                if (!done) {
                    // Should never get here but just in case.
                    throw new Exception("Timed out waiting for home to complete.");
                }
            }
        }

        AxesLocation homeLocation = new AxesLocation(machine, this, (axis) -> (axis.getHomeCoordinate()));
        homeLocation.setToDriverCoordinates(this);
    }

    protected List<String> getAxisVariables(ReferenceMachine machine) {
        List<String> variables = new ArrayList<>();
        if (usingLetterVariables) {
            for (org.openpnp.spi.Axis axis : machine.getAxes()) {
                if (axis instanceof ControllerAxis) {
                    String letter =((ControllerAxis) axis).getLetter(); 
                    if (letter != null && !letter.isEmpty()) {
                        variables.add(letter);
                    }
                }
            }
        }
        else {
            for (Type type : Type.values()) {
                variables.add(type.toString());
            }
        }
        return variables;
    }

    @Override
    public void setGlobalOffsets(ReferenceMachine machine, AxesLocation axesLocation)
            throws Exception {
        // Compose the command
        String command = getCommand(null, CommandType.SET_GLOBAL_OFFSETS_COMMAND);
        if (command != null) {
            // legacy head support
            Head head = machine.getDefaultHead();
            command = substituteVariable(command, "Id", head.getId());
            command = substituteVariable(command, "Name", head.getName());
            for (String variable : getAxisVariables(machine)) {
                ControllerAxis axis = axesLocation.getAxisByVariable(this, variable);
                if (axis != null) {
                    double coordinate = axesLocation.getCoordinate(axis, getUnits());
                    command = substituteVariable(command, variable, coordinate);
                    command = substituteVariable(command, variable+"L", 
                            axis.getLetter());

                    // Store the new driver coordinate on the axis.
                    axis.setDriverCoordinate(coordinate);
                }
                else {
                    command = substituteVariable(command, variable, null);
                    command = substituteVariable(command, variable+"L", null); 
                }
            }
            sendGcode(command, -1);
        }
        else {
            // Try the legacy POST_VISION_HOME_COMMAND
            String postVisionHomeCommand = getCommand(null, CommandType.POST_VISION_HOME_COMMAND);
            ControllerAxis axisX = axesLocation.getAxisByVariable(this, "X");
            ControllerAxis axisY = axesLocation.getAxisByVariable(this, "Y");
            if (postVisionHomeCommand != null 
                    && axisX != null
                    && axisY != null) { 
                // X, Y, are mapped to this driver, legacy support enabled
                postVisionHomeCommand = substituteVariable(postVisionHomeCommand, "X", 
                        axesLocation.getCoordinate(axisX, getUnits()));
                postVisionHomeCommand = substituteVariable(postVisionHomeCommand, "Y", 
                        axesLocation.getCoordinate(axisY, getUnits()));
                // Execute the command
                sendGcode(postVisionHomeCommand, -1);
                // Store the new current coordinate on the axis.
                axisX.setDriverLengthCoordinate(axesLocation.getLengthCoordinate(axisX));
                axisY.setDriverLengthCoordinate(axesLocation.getLengthCoordinate(axisY));
            }
        }
    }


    public Command getCommand(HeadMountable hm, CommandType type, boolean checkDefaults) {
        // If a HeadMountable is specified, see if we can find a match
        // for both the HeadMountable ID and the command type.
        if (type.headMountable && hm != null) {
            for (Command c : commands) {
                if (hm.getId().equals(c.headMountableId) && type == c.type) {
                    return c;
                }
            }
            if (!checkDefaults) {
                return null;
            }
        }
        // If not, see if we can find a match for the command type with a
        // null or * HeadMountable ID.
        for (Command c : commands) {
            if ((c.headMountableId == null || c.headMountableId.equals("*")) && type == c.type) {
                return c;
            }
        }
        // No matches were found.
        return null;
    }

    public String getCommand(HeadMountable hm, CommandType type) {
        Command c = getCommand(hm, type, true);
        if (c == null) {
            return null;
        }
        return c.getCommand();
    }

    public void setCommand(HeadMountable hm, CommandType type, String text) {
        Command c = getCommand(hm, type, false);
        if (text == null || text.trim().length() == 0) {
            if (c != null) {
                commands.remove(c);
            }
        }
        else {
            if (c == null) {
                c = new Command(hm == null ? null : hm.getId(), type, text);
                commands.add(c);
            }
            else {
                c.setCommand(text);
            }
        }
    }

    @Override
    public void moveTo(ReferenceHeadMountable hm, Motion motion)
            throws Exception {
        AxesLocation location = motion.getLocation1();
        double feedRate = motion.getFeedRatePerMinute(this);
        Double acceleration = motion.getAccelerationPerSecond2(this);
        Double jerk = motion.getJerkPerSecond3(this);

        // Start composing the command, will decide later, whether we actually send it.
        String command = getCommand(hm, CommandType.MOVE_TO_COMMAND);
        if (command == null) {
            return;
        }

        boolean enableBacklash = true;
        int options = motion.getOptions();
        if (MotionOption.SpeedOverPrecision.isSetIn(options)) {
            // for this move backslash is zero
            enableBacklash = false;
        }

        command = substituteVariable(command, "Id", hm.getId());
        command = substituteVariable(command, "Name", hm.getName());
        command = substituteVariable(command, "FeedRate", feedRate);
        command = substituteVariable(command, "BacklashFeedRate", enableBacklash ? feedRate * backlashFeedRateFactor : feedRate);
        command = substituteVariable(command, "Acceleration", acceleration);
        command = substituteVariable(command, "Jerk", jerk);

        // Go through all the axes and handle them.
        boolean doesMove = false;
        ReferenceMachine machine = (ReferenceMachine) hm.getHead().getMachine();
        for (String variable : getAxisVariables(machine)) {
            ControllerAxis axis = location.getAxisByVariable(this, variable);
            boolean moveAxis = false;
            boolean includeAxis = false;
            if (axis != null) {
                // Compare the coordinates using the resolution of the axis to tolerate floating point errors
                // from transformation etc. Also suppresses rounded-to-0 moves due to MOVE_TO_COMMANDs format 
                // specifier (usually %.4f).
                moveAxis = !axis.coordinatesMatch(location.getCoordinate(axis, getUnits()), axis.getDriverCoordinate());
                if (moveAxis) {
                    includeAxis = true;
                }
                else {
                    // If the command has forced-output coordinate variables "XF", "YF", "ZF" and "RotationF", 
                    // always include the corresponding axis in the command.
                    // This may be employed for axes, where OpenPNP cannot not keep track when an axis has physically 
                    // moved behind its back. By always forcing the axis coordinate output, the controller will take care 
                    // of restoring the axis position, if necessary.  
                    // As we are always moving in absolute coordinates this has no ill effect if it results in no 
                    // position change after all. 
                    // Note, there is no need for separate backlash compensation variables, as these are always 
                    // substituted alongside. 
                    includeAxis = hasVariable(command, variable+"F");
                }
            }
            if (axis != null && includeAxis) {
                // The move is definitely on. 
                doesMove = true;
                // TODO: discuss whether we should round to axis precision here.
                double coordinate = location.getCoordinate(axis); 
                double previousCoordinate = axis.getCoordinate(); 
                int direction = ((Double)coordinate).compareTo(previousCoordinate);
                // Substitute the axis variables.
                command = substituteVariable(command, variable, coordinate);
                command = substituteVariable(command, variable+"F", coordinate);
                command = substituteVariable(command, variable+"L", axis.getLetter());
                // Apply backlash offset.
                double backlashOffset = (enableBacklash ?
                        ((ReferenceControllerAxis) axis).getBacklashOffset().convertToUnits(getUnits()).getValue()
                        : 0);
                command = substituteVariable(command, "BacklashOffset"+variable,  coordinate + backlashOffset); 
                command = substituteVariable(command, variable+"Decreasing", direction < 0 ? true : null);
                command = substituteVariable(command, variable+"Increasing", direction > 0 ? true : null);
                if (isSupportingPreMove() && axis instanceof ReferenceControllerAxis) {
                    // Check for a pre-move command.
                    String preMoveCommand = ((ReferenceControllerAxis) axis).getPreMoveCommand();
                    if (preMoveCommand != null && !preMoveCommand.isEmpty()) {
                        preMoveCommand = substituteVariable(preMoveCommand, "Coordinate", previousCoordinate);
                        sendGcode(preMoveCommand);
                    }
                }
                // Store the new driver coordinate on the axis.
                axis.setDriverCoordinate(coordinate);
            }
            else {
                // Delete the unused axis variables.
                command = substituteVariable(command, variable, null);
                command = substituteVariable(command, variable+"F", null);
                command = substituteVariable(command, variable+"L", null); 
                command = substituteVariable(command, "BacklashOffset"+variable, null);
                command = substituteVariable(command, variable+"Decreasing", null);
                command = substituteVariable(command, variable+"Increasing", null);
            }
        }
        if (doesMove) {
            // We do actually send the command. 
            List<String> responses = sendGcode(command);

            // TODO: determine if it is technically possible to move this to waitForCompletion() with the 
            // responses correctly associated.

            /*
             * If moveToCompleteRegex is specified we need to wait until we match the regex in a
             * response before continuing. We first search the initial responses from the
             * command for the regex. If it's not found we then collect responses for up to
             * timeoutMillis while searching the responses for the regex. As soon as it is
             * matched we continue. If it's not matched within the timeout we throw an
             * Exception.
             */
            String moveToCompleteRegex = getCommand(hm, CommandType.MOVE_TO_COMPLETE_REGEX);
            if (moveToCompleteRegex != null) {
                if (!containsMatch(responses, moveToCompleteRegex)) {
                    long t = System.currentTimeMillis();
                    boolean done = false;
                    while (!done && System.currentTimeMillis() - t < timeoutMilliseconds) {
                        done = containsMatch(sendCommand(null, 250), moveToCompleteRegex);
                    }
                    if (!done) {
                        throw new Exception("Timed out waiting for move to complete.");
                    }
                }
            }
        }
    }

    @Override
    public void waitForCompletion(ReferenceHeadMountable hm, 
            CompletionType completionType) throws Exception {
        String command = getCommand(hm, CommandType.MOVE_TO_COMPLETE_COMMAND);
        if (command != null) {
            sendGcode(command);
        }
        if (completionType.isWaitingForDrivers()) {
            // TODO: as soon as the async writer/hand-shaking is implemented, we must explicitly
            // wait for the controller's acknowledgment here. 
            // This distinction does not works yet, as we always implicitly wait in sendGcode();
        }
    }

    private boolean containsMatch(List<String> responses, String regex) {
        for (String response : responses) {
            if (response.matches(regex)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void actuate(ReferenceActuator actuator, boolean on) throws Exception {
        String command = getCommand(actuator, CommandType.ACTUATE_BOOLEAN_COMMAND);
        command = substituteVariable(command, "Id", actuator.getId());
        command = substituteVariable(command, "Name", actuator.getName());
        command = substituteVariable(command, "Index", actuator.getIndex());
        command = substituteVariable(command, "BooleanValue", on);
        command = substituteVariable(command, "True", on ? on : null);
        command = substituteVariable(command, "False", on ? null : on);
        sendGcode(command);
    }

    @Override
    public void actuate(ReferenceActuator actuator, double value) throws Exception {
        String command = getCommand(actuator, CommandType.ACTUATE_DOUBLE_COMMAND);
        command = substituteVariable(command, "Id", actuator.getId());
        command = substituteVariable(command, "Name", actuator.getName());
        command = substituteVariable(command, "Index", actuator.getIndex());
        command = substituteVariable(command, "DoubleValue", value);
        command = substituteVariable(command, "IntegerValue", (int) value);
        sendGcode(command);
    }

    @Override
    public void actuate(ReferenceActuator actuator, String value) throws Exception {
        String command = getCommand(actuator, CommandType.ACTUATE_STRING_COMMAND);
        command = substituteVariable(command, "Id", actuator.getId());
        command = substituteVariable(command, "Name", actuator.getName());
        command = substituteVariable(command, "Index", actuator.getIndex());
        command = substituteVariable(command, "StringValue", value);
        sendGcode(command);
    }

    private String actuatorRead(ReferenceActuator actuator, Double parameter) throws Exception {
        /**
         * The logic here is a little complicated. This is the only driver method that is
         * not fire and forget. In this case, we need to know if the command was serviced or not 
         * and throw an Exception if not.
         */
        String command;
        if (parameter == null) {
            command = getCommand(actuator, CommandType.ACTUATOR_READ_COMMAND);
        }
        else {
            command = getCommand(actuator, CommandType.ACTUATOR_READ_WITH_DOUBLE_COMMAND);
        }
        String regex = getCommand(actuator, CommandType.ACTUATOR_READ_REGEX);
        if (command != null && regex != null) {
            /**
             * This driver has the command and regex defined, so it must service the command.
             */
            command = substituteVariable(command, "Id", actuator.getId());
            command = substituteVariable(command, "Name", actuator.getName());
            command = substituteVariable(command, "Index", actuator.getIndex());
            if (parameter != null) {
                command = substituteVariable(command, "DoubleValue", parameter);
                command = substituteVariable(command, "IntegerValue", (int) parameter.doubleValue());
            }

            List<String> responses = sendGcode(command);

            Pattern pattern = Pattern.compile(regex);
            for (String line : responses) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    Logger.trace("actuatorRead response: {}", line);
                    try {
                        String s = matcher.group("Value");
                        return s;
                    }
                    catch (IllegalArgumentException e) {
                        throw new Exception(String.format("Actuator \"%s\" read error: Regex is missing \"Value\" capturing group. See https://github.com/openpnp/openpnp/wiki/GcodeDriver#actuator_read_regex", 
                                actuator.getName()), e);
                    }
                    catch (Exception e) {
                        throw new Exception(String.format("Actuator \"%s\" read error: Failed to parse response. See https://github.com/openpnp/openpnp/wiki/GcodeDriver#actuator_read_regex", 
                                actuator.getName()), e);
                    }
                }
            }

            throw new Exception(String.format("Actuator \"%s\" read error: No matching responses found.", actuator.getName()));
        }
        else {
            throw new Exception(String.format("Actuator \"%s\" read error: Driver configuration is missing ACTUATOR_READ_COMMAND or ACTUATOR_READ_REGEX.", actuator.getName()));
        }
    }

    @Override
    public String actuatorRead(ReferenceActuator actuator) throws Exception {
        return actuatorRead(actuator, null); 
    }

    @Override
    public String actuatorRead(ReferenceActuator actuator, double parameter) throws Exception {
        return actuatorRead(actuator, (Double) parameter);
    }

    public synchronized void disconnect() {
        disconnectRequested = true;
        connected = false;

        try {
            if (readerThread != null && readerThread.isAlive()) {
                readerThread.join(3000);
            }
        }
        catch (Exception e) {
            Logger.error("disconnect()", e);
        }

        try {
            getCommunications().disconnect();
        }
        catch (Exception e) {
            Logger.error("disconnect()", e);
        }
        disconnectRequested = false;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    protected List<String> sendGcode(String gCode) throws Exception {
        return sendGcode(gCode, timeoutMilliseconds);
    }

    protected List<String> sendGcode(String gCode, long timeout) throws Exception {
        if (gCode == null) {
            return new ArrayList<>();
        }
        List<String> responses = new ArrayList<>();
        for (String command : gCode.split("\n")) {
            command = command.trim();
            if (command.length() == 0) {
                continue;
            }
            responses.addAll(sendCommand(command, timeout));
        }
        return responses;
    }

    public List<String> sendCommand(String command) throws Exception {
        return sendCommand(command, timeoutMilliseconds);
    }

    public List<String> sendCommand(String command, long timeout) throws Exception {
        List<String> responses = new ArrayList<>();

        // Read any responses that might be queued up so that when we wait
        // for a response to a command we actually wait for the one we expect.
        responseQueue.drainTo(responses);

        Logger.debug("sendCommand({}, {})...", command, timeout);

        // Send the command, if one was specified
        if (command != null) {
            if (backslashEscapedCharactersEnabled) {
                command = unescape(command);
            }
            Logger.trace("[{}] >> {}", getCommunications().getConnectionName(), command);
            try {
                getCommunications().writeLine(command);
            }
            catch (IOException ex) {
                Logger.error("Failed to write command: ", command);
                disconnect();
                Configuration.get().getMachine().setEnabled(false);
            }
        }

        // Collect responses till we find one with the confirmation or we timeout. Return
        // the collected responses.
        if (timeout == -1) {
            timeout = Long.MAX_VALUE;
        }
        long t = System.currentTimeMillis();
        boolean found = false;
        boolean foundError = false;
        String errorResponse = "";
        // Loop until we've timed out
        while (System.currentTimeMillis() - t < timeout) {
            // Wait to see if a response came in. We wait up until the number of millis remaining
            // in the timeout.
            String response = responseQueue.poll(timeout - (System.currentTimeMillis() - t),
                    TimeUnit.MILLISECONDS);
            // If no response yet, try again.
            if (response == null) {
                continue;
            }
            // Store the response that was received
            responses.add(response);
            // If the response is an ok or error we're done
            if (response.matches(getCommand(null, CommandType.COMMAND_CONFIRM_REGEX))) {
                found = true;
                break;
            }

            if (getCommand(null, CommandType.COMMAND_ERROR_REGEX) != null) {
                if (response.matches(getCommand(null, CommandType.COMMAND_ERROR_REGEX))) {
                    foundError = true;
                    errorResponse = response;
                    break;
                }
            }
        }
        // If a command was specified and no confirmation was found it's a timeout error.
        if (command != null & foundError) {
            throw new Exception("Controller raised an error: " + errorResponse);
        }
        if (command != null && !found) {
            throw new Exception("Timeout waiting for response to " + command);
        }

        // Read any additional responses that came in after the initial one.
        responseQueue.drainTo(responses);

        Logger.debug("sendCommand({} {}, {}) => {}",
                new Object[] {getCommunications().getConnectionName(), command, timeout == Long.MAX_VALUE ? -1 : timeout, responses});
        return responses;
    }

    public void run() {
        while (!disconnectRequested) {
            String line;
            try {
                line = getCommunications().readLine();
                if (line == null) {
                    // Line read failed eg. due to socket closure
                    Logger.error("Failed to read gcode response");
                    return;
                }
                line = line.trim();
            }
            catch (TimeoutException ex) {
                continue;
            }
            catch (IOException e) {
                Logger.error("Read error", e);
                return;
            }
            line = line.trim();
            Logger.trace("[{}] << {}", getCommunications().getConnectionName(), line);
            // extract a position report, if present
            processPositionReport(line);
            // add to the responseQueue (even if it happens to be a position report, it might still also contain the "ok"
            // acknowledgment e.g. on Smoothieware)
            responseQueue.offer(line);
        }
    }

    private boolean processPositionReport(String line) {
        if (getCommand(null, CommandType.POSITION_REPORT_REGEX) == null) {
            return false;
        }

        if (!line.matches(getCommand(null, CommandType.POSITION_REPORT_REGEX))) {
            return false;
        }

        Logger.trace("Position report: {}", line);
        ReferenceMachine machine = ((ReferenceMachine) Configuration.get().getMachine());
        Matcher matcher =
                Pattern.compile(getCommand(null, CommandType.POSITION_REPORT_REGEX)).matcher(line);
        matcher.matches();
        for (org.openpnp.spi.Axis axis : machine.getAxes()) {
            if (axis instanceof ReferenceControllerAxis) {
                if (((ReferenceControllerAxis) axis).getDriver() == this) {
                    try {
                        String s = matcher.group(axis.getName());
                        Double d = Double.valueOf(s);
                        ((ReferenceControllerAxis) axis).setDriverCoordinate(d);
                    }
                    catch (IllegalArgumentException e) {
                        // Axis is not present in pattern. That's OK. 
                    }
                    catch (Exception e) {
                        Logger.warn("Error processing position report for axis {}: {}", axis.getName(), e);
                    }
                }
            }
        }

        for (Head head : Configuration.get().getMachine().getHeads()) {
            machine.fireMachineHeadActivity(head);
        }
        return true;
    }

    /**
     * Find matches of variables in the format {Name:Format} and replace them with the specified
     * value formatted using String.format with the specified Format. Format is optional and
     * defaults to %s. A null value replaces the variable with "".
     */
    static protected String substituteVariable(String command, String name, Object value) {
        if (command == null) {
            return command;
        }
        StringBuffer sb = new StringBuffer();
        Matcher matcher = Pattern.compile("\\{(\\w+)(?::(.+?))?\\}").matcher(command);
        while (matcher.find()) {
            String n = matcher.group(1);
            if (!n.equals(name)) {
                continue;
            }
            String format = matcher.group(2);
            if (format == null) {
                format = "%s";
            }
            String v = "";
            if (value != null) {
                v = String.format((Locale) null, format, value);
            }
            matcher.appendReplacement(sb, v);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Find matches of variables in the format {Name:Format} and return true if present.
     */
    static protected boolean hasVariable(String command, String name) {
        if (command == null) {
            return false;
        }
        Matcher matcher = Pattern.compile("\\{(\\w+)(?::(.+?))?\\}").matcher(command);
        while (matcher.find()) {
            String n = matcher.group(1);
            if (!n.equals(name)) {
                continue;
            }
            return true;
        }
        return false;
    }



    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(super.getConfigurationWizard()),
                new PropertySheetWizardAdapter(new GcodeDriverSettings(this), "Driver Settings"),
                new PropertySheetWizardAdapter(new GcodeDriverGcodes(this), "Gcode"),
                new PropertySheetWizardAdapter(new GcodeDriverConsole(this), "Console"),
        };
    }

    @Override
    public LengthUnit getUnits() {
        return units;
    }

    public void setUnits(LengthUnit units) {
        this.units = units;
    }

    public double getBacklashFeedRateFactor() {
        return backlashFeedRateFactor;
    }

    public void setBacklashFeedRateFactor(double backlashFeedRateFactor) {
        this.backlashFeedRateFactor = backlashFeedRateFactor;
    }

    public int getMaxFeedRate() {
        return maxFeedRate;
    }

    public void setMaxFeedRate(int maxFeedRate) {
        this.maxFeedRate = maxFeedRate;
    }

    @Override
    public Length getFeedRatePerSecond() {
        return new Length(maxFeedRate/60.0, getUnits());
    }

    public int getTimeoutMilliseconds() {
        return timeoutMilliseconds;
    }

    public void setTimeoutMilliseconds(int timeoutMilliseconds) {
        this.timeoutMilliseconds = timeoutMilliseconds;
    }

    public int getConnectWaitTimeMilliseconds() {
        return connectWaitTimeMilliseconds;
    }

    public void setConnectWaitTimeMilliseconds(int connectWaitTimeMilliseconds) {
        this.connectWaitTimeMilliseconds = connectWaitTimeMilliseconds;
    }

    public boolean isBackslashEscapedCharactersEnabled() {
        return backslashEscapedCharactersEnabled;
    }

    public void setBackslashEscapedCharactersEnabled(boolean backslashEscapedCharactersEnabled) {
        this.backslashEscapedCharactersEnabled = backslashEscapedCharactersEnabled;
    }

    public boolean isUsingLetterVariables() {
        return usingLetterVariables;
    }

    public void setUsingLetterVariables(boolean usingLetterVariables) {
        this.usingLetterVariables = usingLetterVariables;
    }

    @Override
    public boolean isSupportingPreMove() {
        return supportingPreMove;
    }

    public void setSupportingPreMove(boolean supportingPreMove) {
        this.supportingPreMove = supportingPreMove;
    }


    @Deprecated
    public Axis getLegacyAxis(HeadMountable hm, Type type) {
        if (axes != null) {
            for (Axis axis : axes) {
                if (axis.type == type && (axis.headMountableIds.contains("*")
                        || axis.headMountableIds.contains(hm.getId()))) {
                    return axis;
                }
            }
        }
        return null;
    }

    @Deprecated
    protected ReferenceControllerAxis migrateAxis(ReferenceMachine machine, Axis legacyAxis)
            throws Exception {
        ReferenceControllerAxis axis;
        axis = new ReferenceControllerAxis();
        axis.setType(legacyAxis.type);
        axis.setName(legacyAxis.name);
        // The letter migration is just a guess and good for Test drivers. It is not relevant until the user switches 
        // on GcodeDriver.usingLetterVariables and it will be unique tested then. 
        if (legacyAxis.type == Type.Rotation) {
            axis.setLetter("E"); // Legacy default.
        }
        else {
            axis.setLetter(legacyAxis.type.toString());
        }
        axis.setHomeCoordinate(new Length(legacyAxis.homeCoordinate, getUnits()));
        axis.setPreMoveCommand(legacyAxis.preMoveCommand);
        if (axis.getPreMoveCommand() != null && !axis.getPreMoveCommand().isEmpty()) {
            setSupportingPreMove(true);
        }
        AbstractHead head = (AbstractHead) machine.getDefaultHead();
        AbstractCamera camera = (AbstractCamera) head.getDefaultCamera();
        Location minLocation = head.getMinLocation().subtract(camera.getHeadOffsets());
        Location maxLocation = head.getMaxLocation().subtract(camera.getHeadOffsets());
        double maxFeedRate = this.maxFeedRate;
        switch (axis.getType()) {
            case X:
                axis.setBacklashOffset(new Length(backlashOffsetX, getUnits()));
                if (head.isSoftLimitsEnabled()) {
                    axis.setSoftLimitLow(minLocation.getLengthX());
                    axis.setSoftLimitLowEnabled(true);
                    axis.setSoftLimitHigh(maxLocation.getLengthX());
                    axis.setSoftLimitHighEnabled(true);
                }
                break;
            case Y:
                axis.setBacklashOffset(new Length(backlashOffsetY, getUnits()));
                if (head.isSoftLimitsEnabled()) {
                    axis.setSoftLimitLow(minLocation.getLengthY());
                    axis.setSoftLimitLowEnabled(true);
                    axis.setSoftLimitHigh(maxLocation.getLengthY());
                    axis.setSoftLimitHighEnabled(true);
                }
                break;
            case Z:
                axis.setBacklashOffset(new Length(backlashOffsetZ, getUnits()));
                break;
            case Rotation:
                axis.setBacklashOffset(new Length(backlashOffsetR, getUnits()));
                maxFeedRate *= 10;
                break;
        }
        axis.setDriver(this);
        // Migrate the feedrate to the axes but change to mm/s.
        axis.setFeedratePerSecond(new Length(maxFeedRate/60.0, getUnits()));
        // Assume 0.5s average acceleration to reach top speed. v = a*t => a = v/t
        axis.setAccelerationPerSecond2(new Length(maxFeedRate/60/0.5, getUnits()));
        // Switch off jerk by default.
        axis.setJerkPerSecond3(new Length(0, getUnits()));
        machine.addAxis(axis);
        return axis;
    }

    @Deprecated
    protected AbstractTransformedAxis migrateAxis(ReferenceMachine machine, ReferenceControllerAxis inputAxis, AxisTransform legacyAxis)
            throws Exception {
        if (legacyAxis instanceof CamTransform) {
            // Create the counter-clockwise  ...
            ReferenceCamCounterClockwiseAxis axisMaster = new ReferenceCamCounterClockwiseAxis();
            axisMaster.setName(inputAxis.getName()+"-cam-counter-clockwise");
            axisMaster.setType(inputAxis.getType());
            axisMaster.setInputAxis(inputAxis);
            axisMaster.setCamRadius(new Length(((CamTransform) legacyAxis).camRadius, getUnits()));
            axisMaster.setCamWheelRadius(new Length(((CamTransform) legacyAxis).camWheelRadius, getUnits()));
            axisMaster.setCamWheelGap(new Length(((CamTransform) legacyAxis).camWheelGap, getUnits()));
            machine.addAxis(axisMaster);
            // ... and the clockwise axis.
            ReferenceCamClockwiseAxis axisSlave = new ReferenceCamClockwiseAxis();
            axisSlave.setType(inputAxis.getType());
            axisSlave.setName(inputAxis.getName()+"-cam-clockwise");
            axisSlave.setInputAxis(axisMaster);
            machine.addAxis(axisSlave);
            // return the slave
            return axisSlave;
        }
        else if (legacyAxis instanceof NegatingTransform) {
            ReferenceMappedAxis axisTransform = new ReferenceMappedAxis();
            axisTransform.setName(inputAxis.getName()+"-neg");
            axisTransform.setType(inputAxis.getType());
            axisTransform.setInputAxis(inputAxis);
            axisTransform.setMapInput0(new Length(0.0, getUnits()));
            axisTransform.setMapOutput0(new Length(0.0, getUnits()));
            axisTransform.setMapInput1(new Length(1.0, getUnits()));
            axisTransform.setMapOutput1(new Length(-1.0, getUnits()));
            machine.addAxis(axisTransform);
            return axisTransform;
        }
        else if (legacyAxis instanceof ScalingTransform) {
            ReferenceMappedAxis axisTransform = new ReferenceMappedAxis();
            axisTransform.setName(inputAxis.getName()+"-scaled");
            axisTransform.setType(inputAxis.getType());
            axisTransform.setInputAxis(inputAxis);
            axisTransform.setMapInput0(new Length(0.0, getUnits()));
            axisTransform.setMapOutput0(new Length(0.0, getUnits()));
            axisTransform.setMapInput1(new Length(((ScalingTransform) legacyAxis).scaleFactor, getUnits()));
            axisTransform.setMapOutput1(new Length(1.0, getUnits()));
            machine.addAxis(axisTransform);
            return axisTransform;
        }
        else if (legacyAxis instanceof OffsetTransform) {
            Logger.error("Migrating OffsetTransform for axis "+inputAxis.getName()+" not supported");
        }
        return null;
    }

    @Deprecated
    @Override
    public void migrateDriver(ReferenceMachine machine) throws Exception {
        machine.addDriver(this);
        // Legacy type variables.
        this.usingLetterVariables = false;
        if (machine.getDrivers().size() > 1 
                && getName().equals("GcodeDriver")) {
            // User has left default name. Make it a bit clearer.
            setName("GcodeDriver "+machine.getDrivers().size());
        }
        if (axes != null) {
            ReferenceLinearTransformAxis nonSquarenessAxis = null;
            for (Axis legacyAxis : axes) {
                ReferenceControllerAxis controllerAxis = migrateAxis(machine, legacyAxis);
                AbstractTransformedAxis transformedAxis = null;
                AbstractAxis axis = controllerAxis;
                if (legacyAxis.transform != null) {
                    transformedAxis = migrateAxis(machine, controllerAxis, legacyAxis.transform);
                    axis = transformedAxis;
                }
                if (axis != null) { 
                    if (axis.getType() == Type.X) {
                        if (nonSquarenessFactor != 0.0) {
                            // Migrate the non-squareness factor as a new axis transform.
                            nonSquarenessAxis = new ReferenceLinearTransformAxis();
                            nonSquarenessAxis.setType(axis.getType());
                            // Take over the name and rename the input axis instead.
                            nonSquarenessAxis.setName(axis.getName());
                            axis.setName(axis.getName()+"-non-square");
                            nonSquarenessAxis.setInputAxisX(axis);
                            nonSquarenessAxis.setFactorX(1.0);
                            machine.addAxis(nonSquarenessAxis);
                            // make this the axis that is assigned to the HeadMountables.
                            axis = nonSquarenessAxis;
                        }
                    }
                    else if (axis.getType() == Type.Y) {
                        if (nonSquarenessAxis != null) {
                            nonSquarenessAxis.setInputAxisY(axis);
                            // Note, in the original code the nonSquarenessFactor was applied transformed --> raw.
                            // The new implementation as a TransformedAxis reverses this to raw --> transformed to unify the 
                            // thinking. Therefore we need to invert the sign.  
                            nonSquarenessAxis.setFactorY(-nonSquarenessFactor);
                            // Make this a compensation transformation so it can be filtered out on demand.
                            nonSquarenessAxis.setCompensation(true);
                        }
                    }

                    // Migrate axes on the default head. 
                    for (Camera hm : machine.getDefaultHead().getCameras()) {
                        migrateAssignAxis(legacyAxis, axis, hm);
                        assignCameraVirtualAxes(machine, hm);
                    }
                    for (Nozzle hm : machine.getDefaultHead().getNozzles()) {
                        migrateAssignAxis(legacyAxis, axis, hm);
                    }
                    for (Actuator hm : machine.getDefaultHead().getActuators()) {
                        migrateAssignAxis(legacyAxis, axis, hm);
                    }
                }
            }
            // lose them!
            axes = null;
        }
        for (Head head : machine.getHeads()) {
            // Migrate visual homing setting.
            if (visualHomingEnabled) {
                // Assuming only one (sub-) driver will have this enabled.  
                // Set the legacy Visual Homing Method, @see VisualHomingMethod for more info.
                ((AbstractHead)head).setVisualHomingMethod(VisualHomingMethod.ResetToHomeLocation);
                Location homingFiducialLocation = this.homingFiducialLocation;
                // because squareness compensation is now properly applied to the homing fiducial location, we need to "unapply" it here.
                homingFiducialLocation = homingFiducialLocation
                        .subtract(new Location(homingFiducialLocation.getUnits(), homingFiducialLocation.getY()*nonSquarenessFactor, 0, 0, 0));
                ((AbstractHead)head).setHomingFiducialLocation(homingFiducialLocation);
            }
            for (Actuator actuator : head.getActuators()) {
                // This is not 100% foolproof. Theoretically an actuator could have been smeared across
                // multiple drivers e.g. ACTUATE_BOOLEAN_COMMAND in the main driver, ACTUATOR_READ_COMMAND in the
                // sub-driver. 
                // We simply no longer support that.  
                if (getCommand(actuator, CommandType.ACTUATE_BOOLEAN_COMMAND) != null 
                        || getCommand(actuator, CommandType.ACTUATE_DOUBLE_COMMAND) != null 
                        || getCommand(actuator, CommandType.ACTUATOR_READ_COMMAND) != null 
                        || getCommand(actuator, CommandType.ACTUATOR_READ_WITH_DOUBLE_COMMAND) != null
                        || getCommand(actuator, CommandType.ACTUATOR_READ_REGEX) != null) {
                    actuator.setDriver(this);
                }
            }
        }
        for (Actuator actuator : machine.getActuators()) {
            // This is not 100% foolproof. Theoretically an actuator could have been smeared across
            // multiple drivers e.g. ACTUATE_BOOLEAN_COMMAND in the main driver, ACTUATOR_READ_COMMAND in the
            // sub-driver. 
            // We simply no longer support that.  
            if (getCommand(actuator, CommandType.ACTUATE_BOOLEAN_COMMAND) != null 
                    || getCommand(actuator, CommandType.ACTUATE_DOUBLE_COMMAND) != null 
                    || getCommand(actuator, CommandType.ACTUATOR_READ_COMMAND) != null 
                    || getCommand(actuator, CommandType.ACTUATOR_READ_WITH_DOUBLE_COMMAND) != null
                    || getCommand(actuator, CommandType.ACTUATOR_READ_REGEX) != null) {
                actuator.setDriver(this);
            }
        }
        // Migrate sub-drivers.
        if (subDrivers != null) {
            for (GcodeDriver gcodeDriver : subDrivers) {
                gcodeDriver.migrateDriver(machine);
            }
            // lose them!
            subDrivers = null;
        }
        // Cleanup unneeded locations.
        for (Head head : machine.getHeads()) {
            ((AbstractHead) head).setMinLocation(null);
            ((AbstractHead) head).setMaxLocation(null);
        }
    }

    @Deprecated
    protected void migrateAssignAxis(Axis legacyAxis, AbstractAxis axis, HeadMountable hm) {
        AbstractAxis assignAxis = axis; 
        if (legacyAxis.transform != null) { 
            if (legacyAxis.transform instanceof CamTransform) {
                if (!((CamTransform) legacyAxis.transform).negatedHeadMountableId.equals(hm.getId())) {
                    // Not the negated axis, take the input axis.
                    assignAxis = ((AbstractSingleTransformedAxis)axis).getInputAxis();
                }
            }
            else if (legacyAxis.transform instanceof NegatingTransform) {
                if (!((NegatingTransform) legacyAxis.transform).negatedHeadMountableId.equals(hm.getId())) {
                    // Not the negated axis, take the input axis.
                    assignAxis = ((AbstractSingleTransformedAxis)axis).getInputAxis();
                }
            }
        }
        // Assign if formerly mapped.
        if (getLegacyAxis(hm, Type.X) == legacyAxis) {
            ((AbstractHeadMountable)hm).setAxisX(assignAxis);
        }
        if (getLegacyAxis(hm, Type.Y) == legacyAxis) {
            ((AbstractHeadMountable)hm).setAxisY(assignAxis);
        }
        if (getLegacyAxis(hm, Type.Z) == legacyAxis) {
            ((AbstractHeadMountable)hm).setAxisZ(assignAxis);
        }
        if (getLegacyAxis(hm, Type.Rotation) == legacyAxis) {
            ((AbstractHeadMountable)hm).setAxisRotation(assignAxis);
            if (hm instanceof ReferenceNozzle && 
                    assignAxis instanceof ReferenceControllerAxis) {
                ((ReferenceControllerAxis) assignAxis).setLimitRotation(((ReferenceNozzle) hm).isLimitRotation());
            }
        }
    }

    @Deprecated
    private static class Axis {

        @Attribute
        private String name;

        @Attribute
        private Type type;

        @Attribute(required = false)
        private double homeCoordinate = 0;

        @ElementList(required = false)
        private Set<String> headMountableIds = new HashSet<String>();

        @Element(required = false)
        private AxisTransform transform;

        @Element(required = false, data = true)
        private String preMoveCommand;

        private Axis() {
        }
    }

    @Deprecated
    public interface AxisTransform {
    }

    /**
     * An AxisTransform for heads with dual linear Z axes powered by one motor. The two Z axes are
     * defined as normal and negated. Normal gets the raw coordinate value and negated gets the same
     * value negated. So, as normal moves up, negated moves down.
     */
    @Deprecated
    public static class NegatingTransform implements AxisTransform {
        @Element
        private String negatedHeadMountableId;

    }

    @Deprecated
    public static class CamTransform implements AxisTransform {
        @Element
        private String negatedHeadMountableId;

        @Attribute(required = false)
        private double camRadius = 24;

        @Attribute(required = false)
        private double camWheelRadius = 9.5;

        @Attribute(required = false)
        private double camWheelGap = 2;

    }

    @Deprecated
    public static class OffsetTransform implements AxisTransform {
        @ElementMap(required=false)
        HashMap<String, Double> offsetsByHeadMountableId = new HashMap<>();

        public OffsetTransform() {
        }
    }

    @Deprecated
    public static class ScalingTransform implements AxisTransform {

        @Attribute(required = false)
        private double scaleFactor = 1;
    }

}
