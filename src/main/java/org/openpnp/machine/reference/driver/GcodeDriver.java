package org.openpnp.machine.reference.driver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.driver.wizards.GcodeDriverConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.SimplePropertySheetHolder;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import com.google.common.base.Joiner;

@Root
public class GcodeDriver extends AbstractSerialPortDriver implements Runnable {
    public enum CommandType {
        COMMAND_CONFIRM_REGEX,
        POSITION_REPORT_REGEX,
        COMMAND_ERROR_REGEX,
        CONNECT_COMMAND,
        ENABLE_COMMAND,
        DISABLE_COMMAND,
        POST_VISION_HOME_COMMAND,
        HOME_COMMAND("Id", "Name"),
        PUMP_ON_COMMAND,
        PUMP_OFF_COMMAND,
        MOVE_TO_COMMAND(true, "Id", "Name", "FeedRate", "X", "Y", "Z", "Rotation"),
        MOVE_TO_COMPLETE_REGEX(true),
        PICK_COMMAND(true, "Id", "Name", "VacuumLevelPartOn", "VacuumLevelPartOff"),
        PLACE_COMMAND(true, "Id", "Name"),
        ACTUATE_BOOLEAN_COMMAND(true, "Id", "Name", "Index", "BooleanValue", "True", "False"),
        ACTUATE_DOUBLE_COMMAND(true, "Id", "Name", "Index", "DoubleValue", "IntegerValue"),
        VACUUM_REQUEST_COMMAND(true, "VacuumLevelPartOn", "VacuumLevelPartOff"),
        VACUUM_REPORT_REGEX(true);

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

    @Attribute(required = false)
    protected int timeoutMilliseconds = 5000;

    @Attribute(required = false)
    protected int connectWaitTimeMilliseconds = 1000;

    @Element(required = false)
    protected Location homingFiducialLocation = null;

    @ElementList(required = false, inline = true)
    public ArrayList<Command> commands = new ArrayList<>();

    @ElementList(required = false)
    protected List<ReferenceDriver> subDrivers = new ArrayList<>();

    @ElementList(required = false)
    protected List<Axis> axes = new ArrayList<>();

    private Thread readerThread;
    private boolean disconnectRequested;
    private boolean connected;
    private LinkedBlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();
    private Set<Nozzle> pickedNozzles = new HashSet<>();

    public void createDefaults() {
        axes = new ArrayList<>();
        axes.add(new Axis("x", Axis.Type.X, 0, "*"));
        axes.add(new Axis("y", Axis.Type.Y, 0, "*"));
        axes.add(new Axis("z", Axis.Type.Z, 0, "*"));
        axes.add(new Axis("rotation", Axis.Type.Rotation, 0, "*"));

        commands = new ArrayList<>();
        commands.add(new Command(null, CommandType.COMMAND_CONFIRM_REGEX, "^ok.*"));
        commands.add(new Command(null, CommandType.CONNECT_COMMAND, "G21 ; Set millimeters mode\nG90 ; Set absolute positioning mode\nM82 ; Set absolute mode for extruder"));
        commands.add(new Command(null, CommandType.HOME_COMMAND, "G28 ; Home all axes"));
        commands.add(new Command(null, CommandType.MOVE_TO_COMMAND, "G0 {X:X%.4f} {Y:Y%.4f} {Z:Z%.4f} {Rotation:E%.4f} F{FeedRate:%.0f} ; Send standard Gcode move\nM400 ; Wait for moves to complete before returning"));
    }

    public synchronized void connect() throws Exception {
        super.connect();

        connected = false;
        readerThread = new Thread(this);
        readerThread.start();

        // Wait a bit while the controller starts up
        Thread.sleep(connectWaitTimeMilliseconds);

        // Consume any startup messages
        try {
            while (!sendCommand(null, 250).isEmpty());
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

        for (ReferenceDriver driver : subDrivers) {
            driver.setEnabled(enabled);
        }
    }

    @Override
    public void home(ReferenceHead head) throws Exception {
        // Home is sent with an infinite timeout since it's tough to tell how long it will
        // take.
        String command = getCommand(null, CommandType.HOME_COMMAND);
        command = substituteVariable(command, "Id", head.getId());
        command = substituteVariable(command, "Name", head.getName());
        sendGcode(command, -1);

        for (Axis axis : axes) {
            axis.setCoordinate(axis.getHomeCoordinate());
        }

        for (ReferenceDriver driver : subDrivers) {
            driver.home(head);
        }

        /*
         * The head camera for nozzle-1 should now be (if everything has homed correctly) directly
         * above the homing pin in the machine bed, use the head camera scan for this and make sure
         * this is exactly central - otherwise we move the camera until it is, and then reset all
         * the axis back to 0,0,0,0 as this is calibrated home.
         */
        Part homePart = Configuration.get().getPart("FIDUCIAL-HOME");
        if (homePart != null) {
            Location tmp = new Location(LengthUnit.Millimeters, 0.0, 0.0, 0.0, 0.0);
            Camera camera = Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
            // camera.moveTo(tmp);
            Location homeOffset = Configuration.get().getMachine().getFiducialLocator()
                    .getHomeFiducialLocation(tmp, homePart);

            // homeOffset contains the offset, but we are not really concerned with that,
            // we just reset X,Y back to the home-coordinate at this point.
            double xHomeCoordinate = 0;
            double yHomeCoordinate = 0;
            for (Axis axis : axes) {
                if (axis.getType() == Axis.Type.X) {
                    axis.setCoordinate(axis.getHomeCoordinate());
                    xHomeCoordinate = axis.getHomeCoordinate();
                }
                if (axis.getType() == Axis.Type.Y) {
                    axis.setCoordinate(axis.getHomeCoordinate());
                    yHomeCoordinate = axis.getHomeCoordinate();
                }
            }

            String g92command = getCommand(null, CommandType.POST_VISION_HOME_COMMAND);
            g92command = substituteVariable(g92command, "X", xHomeCoordinate);
            g92command = substituteVariable(g92command, "Y", yHomeCoordinate);
            sendGcode(g92command, -1);

        }
    }

    public Axis getAxis(HeadMountable hm, Axis.Type type) {
        for (Axis axis : axes) {
            if (axis.getType() == type && (axis.getHeadMountableIds().contains("*")
                    || axis.getHeadMountableIds().contains(hm.getId()))) {
                return axis;
            }
        }
        return null;
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
    public Location getLocation(ReferenceHeadMountable hm) {
        // according main driver
        Axis xAxis = getAxis(hm, Axis.Type.X);
        Axis yAxis = getAxis(hm, Axis.Type.Y);
        Axis zAxis = getAxis(hm, Axis.Type.Z);
        Axis rotationAxis = getAxis(hm, Axis.Type.Rotation);

        // additional info might be on subdrivers (note that subdrivers can only be one level deep)
        for (ReferenceDriver driver : subDrivers) {
            GcodeDriver d = (GcodeDriver) driver;
            if (d.getAxis(hm, Axis.Type.X) != null) {
                xAxis = d.getAxis(hm, Axis.Type.X);
            }
            if (d.getAxis(hm, Axis.Type.Y) != null) {
                yAxis = d.getAxis(hm, Axis.Type.Y);
            }
            if (d.getAxis(hm, Axis.Type.Z) != null) {
                zAxis = d.getAxis(hm, Axis.Type.Z);
            }
            if (d.getAxis(hm, Axis.Type.Rotation) != null) {
                rotationAxis = d.getAxis(hm, Axis.Type.Rotation);
            }
        }

        Location location =
                new Location(units, xAxis == null ? 0 : xAxis.getTransformedCoordinate(hm),
                        yAxis == null ? 0 : yAxis.getTransformedCoordinate(hm),
                        zAxis == null ? 0 : zAxis.getTransformedCoordinate(hm),
                        rotationAxis == null ? 0 : rotationAxis.getTransformedCoordinate(hm))
                                .add(hm.getHeadOffsets());
        return location;
    }

    @Override
    public void moveTo(ReferenceHeadMountable hm, Location location, double speed)
            throws Exception {
        // keep copy for calling subdrivers as to not add offset on offset
        Location locationOriginal = location;

        location = location.convertToUnits(units);
        location = location.subtract(hm.getHeadOffsets());

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        double rotation = location.getRotation();

        Axis xAxis = getAxis(hm, Axis.Type.X);
        Axis yAxis = getAxis(hm, Axis.Type.Y);
        Axis zAxis = getAxis(hm, Axis.Type.Z);
        Axis rotationAxis = getAxis(hm, Axis.Type.Rotation);

        // Handle NaNs, which means don't move this axis for this move. We set the appropriate
        // axis reference to null, which we'll check for later.
        if (Double.isNaN(x)) {
            xAxis = null;
        }
        if (Double.isNaN(y)) {
            yAxis = null;
        }
        if (Double.isNaN(z)) {
            zAxis = null;
        }
        if (Double.isNaN(rotation)) {
            rotationAxis = null;
        }

        // Only do something if there at least one axis included in the move
        if (xAxis != null || yAxis != null || zAxis != null || rotationAxis != null) {

            // For each included axis, if the axis has a transform, transform the target coordinate
            // to
            // it's raw value.
            if (xAxis != null && xAxis.getTransform() != null) {
                x = xAxis.getTransform().toRaw(xAxis, hm, x);
            }
            if (yAxis != null && yAxis.getTransform() != null) {
                y = yAxis.getTransform().toRaw(yAxis, hm, y);
            }
            if (zAxis != null && zAxis.getTransform() != null) {
                z = zAxis.getTransform().toRaw(zAxis, hm, z);
            }
            if (rotationAxis != null && rotationAxis.getTransform() != null) {
                rotation = rotationAxis.getTransform().toRaw(rotationAxis, hm, rotation);
            }

            boolean haveToMove = false;

            String command = getCommand(hm, CommandType.MOVE_TO_COMMAND);
            command = substituteVariable(command, "Id", hm.getId());
            command = substituteVariable(command, "Name", hm.getName());
            command = substituteVariable(command, "FeedRate", maxFeedRate * speed);

            if (xAxis == null || xAxis.getCoordinate() == x) {
                command = substituteVariable(command, "X", null);
            }
            else {
                command = substituteVariable(command, "X", x);
                haveToMove = true;
                if (xAxis.getPreMoveCommand() != null) {
                    sendGcode(xAxis.getPreMoveCommand());
                }
                xAxis.setCoordinate(x);
            }

            if (yAxis == null || yAxis.getCoordinate() == y) {
                command = substituteVariable(command, "Y", null);
            }
            else {
                command = substituteVariable(command, "Y", y);
                haveToMove = true;
                if (yAxis.getPreMoveCommand() != null) {
                    sendGcode(yAxis.getPreMoveCommand());
                }
            }

            if (zAxis == null || zAxis.getCoordinate() == z) {
                command = substituteVariable(command, "Z", null);
            }
            else {
                command = substituteVariable(command, "Z", z);
                haveToMove = true;
                if (zAxis.getPreMoveCommand() != null) {
                    sendGcode(zAxis.getPreMoveCommand());
                }
            }

            if (rotationAxis == null || rotationAxis.getCoordinate() == rotation) {
                command = substituteVariable(command, "Rotation", null);
            }
            else {
                command = substituteVariable(command, "Rotation", rotation);
                haveToMove = true;
                if (rotationAxis.getPreMoveCommand() != null) {
                    sendGcode(rotationAxis.getPreMoveCommand());
                }
            }

            // Only give a command when move is necessary
            if (haveToMove) {

                List<String> responses = sendGcode(command);

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

                // And save the final values on the axes.
                if (xAxis != null) {
                    xAxis.setCoordinate(x);
                }
                if (yAxis != null) {
                    yAxis.setCoordinate(y);
                }
                if (zAxis != null) {
                    zAxis.setCoordinate(z);
                }
                if (rotationAxis != null) {
                    rotationAxis.setCoordinate(rotation);
                }

            } // there is a move

        } // there were axes involved

        // regardless of any action above the subdriver needs its actions based on original input
        for (ReferenceDriver driver : subDrivers) {
            driver.moveTo(hm, locationOriginal, speed);
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

    private Integer readVacuumLevel(ReferenceNozzle nozzle) throws Exception {
        String command = getCommand(nozzle, CommandType.VACUUM_REQUEST_COMMAND);
        String regex = getCommand(nozzle, CommandType.VACUUM_REPORT_REGEX);
        if (command == null || regex == null) {
            return null;
        }

        ReferenceNozzleTip nt = nozzle.getNozzleTip();

        command = substituteVariable(command, "VacuumLevelPartOn", nt.getVacuumLevelPartOn());
        command = substituteVariable(command, "VacuumLevelPartOff", nt.getVacuumLevelPartOff());

        List<String> responses = sendGcode(command);

        for (String line : responses) {
            Logger.trace("Check {}", line);
            if (line.matches(regex)) {
                Logger.trace("Vacuum report: {}", line);
                Matcher matcher = Pattern.compile(regex).matcher(line);
                matcher.matches();

                try {
                    String s = matcher.group("Vacuum");
                    return Integer.valueOf(s);
                }
                catch (Exception e) {
                    Logger.warn("Error processing vacuum report", e);
                }
            }
        }

        return null;
    }

    @Override
    public void pick(ReferenceNozzle nozzle) throws Exception {
        pickedNozzles.add(nozzle);
        if (pickedNozzles.size() > 0) {
            sendGcode(getCommand(nozzle, CommandType.PUMP_ON_COMMAND));
        }

        String command = getCommand(nozzle, CommandType.PICK_COMMAND);
        command = substituteVariable(command, "Id", nozzle.getId());
        command = substituteVariable(command, "Name", nozzle.getName());

        ReferenceNozzleTip nt = nozzle.getNozzleTip();
        command = substituteVariable(command, "VacuumLevelPartOn", nt.getVacuumLevelPartOn());
        command = substituteVariable(command, "VacuumLevelPartOff", nt.getVacuumLevelPartOff());

        sendGcode(command);

        Integer vacuumLevel = readVacuumLevel(nozzle);
        if (vacuumLevel != null && vacuumLevel < nt.getVacuumLevelPartOn()) {
            throw new Exception(String.format(
                    "Pick failure: Vacuum level %d is lower than expected value of %d for part on. Part may have failed to pick.",
                    vacuumLevel, nt.getVacuumLevelPartOn()));
        }

        for (ReferenceDriver driver : subDrivers) {
            driver.pick(nozzle);
        }
    }

    @Override
    public void place(ReferenceNozzle nozzle) throws Exception {

        ReferenceNozzleTip nt = nozzle.getNozzleTip();

        String command = getCommand(nozzle, CommandType.PLACE_COMMAND);
        command = substituteVariable(command, "Id", nozzle.getId());
        command = substituteVariable(command, "Name", nozzle.getName());

        command = substituteVariable(command, "VacuumLevelPartOn", nt.getVacuumLevelPartOn());
        command = substituteVariable(command, "VacuumLevelPartOff", nt.getVacuumLevelPartOff());
        sendGcode(command);

        Integer vacuumLevel = readVacuumLevel(nozzle);
        if (vacuumLevel != null && vacuumLevel > nt.getVacuumLevelPartOff()) {
            throw new Exception(String.format(
                    "Place failure: Vacuum level %d is higher than expected value of %d for part off. Part may be stuck to nozzle.",
                    vacuumLevel, nt.getVacuumLevelPartOff()));
        }

        pickedNozzles.remove(nozzle);
        if (pickedNozzles.size() < 1) {
            sendGcode(getCommand(nozzle, CommandType.PUMP_OFF_COMMAND));
        }

        for (ReferenceDriver driver : subDrivers) {
            driver.place(nozzle);
        }
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

        for (ReferenceDriver driver : subDrivers) {
            driver.actuate(actuator, on);
        }
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

        for (ReferenceDriver driver : subDrivers) {
            driver.actuate(actuator, value);
        }
    }

    public synchronized void disconnect() {
        disconnectRequested = true;
        connected = false;

        try {
            if (readerThread != null && readerThread.isAlive()) {
                readerThread.join();
            }
        }
        catch (Exception e) {
            Logger.error("disconnect()", e);
        }

        try {
            super.disconnect();
        }
        catch (Exception e) {
            Logger.error("disconnect()", e);
        }
        disconnectRequested = false;
    }

    @Override
    public void close() throws IOException {
        super.close();

        for (ReferenceDriver driver : subDrivers) {
            driver.close();
        }
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

    protected List<String> sendCommand(String command) throws Exception {
        return sendCommand(command, timeoutMilliseconds);
    }

    protected List<String> sendCommand(String command, long timeout) throws Exception {
        List<String> responses = new ArrayList<>();

        // Read any responses that might be queued up so that when we wait
        // for a response to a command we actually wait for the one we expect.
        responseQueue.drainTo(responses);

        Logger.debug("sendCommand({}, {})...", command, timeout);

        // Send the command, if one was specified
        if (command != null) {
            Logger.trace("[{}] >> {}", portName, command);
            output.write(command.getBytes());
            output.write("\n".getBytes());
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

        Logger.debug("sendCommand({}, {}) => {}",
                new Object[] {command, timeout == Long.MAX_VALUE ? -1 : timeout, responses});
        return responses;
    }

    public void run() {
        while (!disconnectRequested) {
            String line;
            try {
                line = readLine().trim();
            }
            catch (TimeoutException ex) {
                continue;
            }
            catch (IOException e) {
                Logger.error("Read error", e);
                return;
            }
            line = line.trim();
            Logger.trace("[{}] << {}", portName, line);
            if (!processPositionReport(line)) {
                responseQueue.offer(line);
            }
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
        Matcher matcher =
                Pattern.compile(getCommand(null, CommandType.POSITION_REPORT_REGEX)).matcher(line);
        matcher.matches();
        for (Axis axis : axes) {
            try {
                String s = matcher.group(axis.getName());
                Double d = Double.valueOf(s);
                axis.setCoordinate(d);
            }
            catch (Exception e) {
                Logger.warn("Error processing position report for axis {}: {}", axis.getName(), e);
            }
        }

        ReferenceMachine machine = ((ReferenceMachine) Configuration.get().getMachine());
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

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        ArrayList<PropertySheetHolder> children = new ArrayList<>();
        if (!subDrivers.isEmpty()) {
            children.add(new SimplePropertySheetHolder("Sub-Drivers", subDrivers));
        }
        return children.toArray(new PropertySheetHolder[] {});
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(super.getConfigurationWizard(), "Serial"),
                new PropertySheetWizardAdapter(new GcodeDriverConfigurationWizard(this), "Gcode")};
    }

    public static class Axis {
        public enum Type {
            X,
            Y,
            Z,
            Rotation
        };

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

        @Element(required = false)
        private String preMoveCommand;

        /**
         * Stores the current value for this axis.
         */
        private double coordinate = 0;

        public Axis() {

        }

        public Axis(String name, Type type, double homeCoordinate, String... headMountableIds) {
            this.name = name;
            this.type = type;
            this.homeCoordinate = homeCoordinate;
            this.headMountableIds.addAll(Arrays.asList(headMountableIds));
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public double getCoordinate() {
            return coordinate;
        }

        public void setCoordinate(double coordinate) {
            this.coordinate = coordinate;
        }

        public double getHomeCoordinate() {
            return homeCoordinate;
        }

        public void setHomeCoordinate(double homeCoordinate) {
            this.homeCoordinate = homeCoordinate;
        }

        public double getTransformedCoordinate(HeadMountable hm) {
            if (this.transform != null) {
                return transform.toTransformed(this, hm, this.coordinate);
            }
            return this.coordinate;
        }

        public Set<String> getHeadMountableIds() {
            return headMountableIds;
        }

        public void setHeadMountableIds(Set<String> headMountableIds) {
            this.headMountableIds = headMountableIds;
        }

        public AxisTransform getTransform() {
            return transform;
        }

        public void setTransform(AxisTransform transform) {
            this.transform = transform;
        }

        public String getPreMoveCommand() {
            return preMoveCommand;
        }

        public void setPreMoveCommand(String preMoveCommand) {
            this.preMoveCommand = preMoveCommand;
        }
    }

    public interface AxisTransform {
        /**
         * Transform the specified raw coordinate into it's corresponding transformed coordinate.
         * The transformed coordinate is what the user sees, while the raw coordinate is what the
         * motion controller sees.
         * 
         * @param hm
         * @param rawCoordinate
         * @return
         */
        public double toTransformed(Axis axis, HeadMountable hm, double rawCoordinate);

        /**
         * Transform the specified transformed coordinate into it's corresponding raw coordinate.
         * The transformed coordinate is what the user sees, while the raw coordinate is what the
         * motion controller sees.
         * 
         * @param hm
         * @param transformedCoordinate
         * @return
         */
        public double toRaw(Axis axis, HeadMountable hm, double transformedCoordinate);
    }

    /**
     * An AxisTransform for heads with dual linear Z axes powered by one motor. The two Z axes are
     * defined as normal and negated. Normal gets the raw coordinate value and negated gets the same
     * value negated. So, as normal moves up, negated moves down.
     */
    public static class NegatingTransform implements AxisTransform {
        @Element
        private String negatedHeadMountableId;

        @Override
        public double toTransformed(Axis axis, HeadMountable hm, double rawCoordinate) {
            if (hm.getId().equals(negatedHeadMountableId)) {
                return -rawCoordinate;
            }
            return rawCoordinate;
        }

        @Override
        public double toRaw(Axis axis, HeadMountable hm, double transformedCoordinate) {
            // Since we're just negating the value of the coordinate we can just
            // use the same function.
            return toTransformed(axis, hm, transformedCoordinate);
        }
    }

    public static class CamTransform implements AxisTransform {
        @Element
        private String negatedHeadMountableId;

        @Attribute(required = false)
        private double camRadius = 24;

        @Attribute(required = false)
        private double camWheelRadius = 9.5;

        @Attribute(required = false)
        private double camWheelGap = 2;

        @Override
        public double toTransformed(Axis axis, HeadMountable hm, double rawCoordinate) {
            double transformed = Math.sin(Math.toRadians(rawCoordinate)) * camRadius;
            if (hm.getId().equals(negatedHeadMountableId)) {
                transformed = -transformed;
            }
            transformed += camWheelRadius + camWheelGap;
            return transformed;
        }

        @Override
        public double toRaw(Axis axis, HeadMountable hm, double transformedCoordinate) {
            double raw = Math.toDegrees(
                    Math.asin((transformedCoordinate - camWheelRadius - camWheelGap) / camRadius));
            if (hm.getId().equals(negatedHeadMountableId)) {
                raw = -raw;
            }
            return raw;
        }
    }
}
