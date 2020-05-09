package org.openpnp.machine.reference.driver;

import java.awt.event.ActionEvent;
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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.driver.GcodeDriver.CommandType;
import org.openpnp.machine.reference.driver.wizards.GcodeDriverConsole;
import org.openpnp.machine.reference.driver.wizards.GcodeDriverGcodes;
import org.openpnp.machine.reference.driver.wizards.GcodeDriverSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.MappedAxes;
import org.openpnp.model.Named;
import org.openpnp.model.Part;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractHead;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.SimplePropertySheetHolder;
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
        POSITION_RESET_COMMAND("Id", "Name", "X", "Y", "Z", "Rotation"),
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
    
    @Attribute(required = false)
    protected double backlashOffsetX = -1;
    
    @Attribute(required = false)
    protected double backlashOffsetY = -1;
    
    @Attribute(required = false)
    protected double backlashOffsetZ = 0;
    
    @Attribute(required = false)
    protected double backlashOffsetR = 0;
    
    @Attribute(required = false)
    protected double nonSquarenessFactor = 0;
    
    @Attribute(required = false)
    protected double backlashFeedRateFactor = 0.1;

    @Attribute(required = false)
    protected int timeoutMilliseconds = 5000;

    @Attribute(required = false)
    protected int connectWaitTimeMilliseconds = 3000;
    
    @Attribute(required = false)
    protected boolean visualHomingEnabled = true;

    @Attribute(required = false)
    protected boolean backslashEscapedCharactersEnabled = false;

    @Element(required = false)
    protected Location homingFiducialLocation = new Location(LengthUnit.Millimeters);

    @ElementList(required = false, inline = true)
    public ArrayList<Command> commands = new ArrayList<>();

    @Deprecated
    @ElementList(required = false)
    protected List<GcodeDriver> subDrivers = new ArrayList<>();

    @ElementList(required = false)
    protected List<Axis> axes = new ArrayList<>();
    
    private Thread readerThread;
    private boolean disconnectRequested;
    private boolean connected;
    private LinkedBlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();
    private GcodeDriver parent = null;
    
    @Commit
    public void commit() {
        super.commit();
    }
    
    public void createDefaults() {
        axes = new ArrayList<>();
        axes.add(new Axis("x", Axis.Type.X, 0, "*"));
        axes.add(new Axis("y", Axis.Type.Y, 0, "*"));
        try {
            List<Nozzle> nozzles = Configuration.get().getMachine().getDefaultHead().getNozzles();
            if (nozzles.size() < 1) {
                throw new Exception("No nozzles.");
            }
            ArrayList<String> ids = new ArrayList<>();
            for (Nozzle nozzle : nozzles) {
                ids.add(nozzle.getId());
            }
            Axis axis = new Axis("z", Axis.Type.Z, 0, ids.toArray(new String[] {}));
            axes.add(axis);
        }
        catch (Exception e) {
            axes.add(new Axis("z", Axis.Type.Z, 0, "*"));
        }
        axes.add(new Axis("rotation", Axis.Type.Rotation, 0, "*"));

        commands = new ArrayList<>();
        commands.add(new Command(null, CommandType.COMMAND_CONFIRM_REGEX, "^ok.*"));
        commands.add(new Command(null, CommandType.CONNECT_COMMAND, "G21 ; Set millimeters mode\nG90 ; Set absolute positioning mode\nM82 ; Set absolute mode for extruder"));
        commands.add(new Command(null, CommandType.HOME_COMMAND, "G28 ; Home all axes"));
        commands.add(new Command(null, CommandType.MOVE_TO_COMMAND, "G0 {X:X%.4f} {Y:Y%.4f} {Z:Z%.4f} {Rotation:E%.4f} F{FeedRate:%.0f} ; Send standard Gcode move"));
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

        for (ReferenceDriver driver : subDrivers) {
            driver.setEnabled(enabled);
        }
        if (connected && !enabled) {
        	if (!connectionKeepAlive) {
            	disconnect();
        	}
        }
    }

    @Override
    public void home(ReferenceHead head, MappedAxes mappedAxes, Location location) throws Exception {
        // Home is sent with an infinite timeout since it's tough to tell how long it will
        // take.
        String command = getCommand(null, CommandType.HOME_COMMAND);
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

        for (ControllerAxis axis : mappedAxes.getAxes()) {
            // Set this axis to the homed coordinate.
            axis.setLengthCoordinate(axis.getHomeCoordinate());
        }
    }

    @Override
    public void resetLocation(ReferenceHead head, MappedAxes mappedAxes, Location location)
            throws Exception {
        // Convert to driver units
        location = location.convertToUnits(getUnits());
        // Compose the command
        String command = getCommand(null, CommandType.POSITION_RESET_COMMAND);
        if (command != null) {
            command = substituteVariable(command, "Id", head.getId());
            command = substituteVariable(command, "Name", head.getName());
            for (org.openpnp.spi.Axis.Type axisType : org.openpnp.spi.Axis.Type.values()) {
                ControllerAxis axis = mappedAxes.getAxis(axisType, this);
                if (axis != null) {
                    command = substituteVariable(command, axisType.toString(), 
                            axis.getLocationAxisCoordinate(location));
                    command = substituteVariable(command, axisType+"S", 
                            axis.getDesignator());
                    // Store the new current coordinate on the axis.
                    axis.setCoordinate(axis.getLocationAxisCoordinate(location));
                }
                else {
                    command = substituteVariable(command, axisType.toString(), null);
                    command = substituteVariable(command, axisType+"S", null); 
                }
            }
            sendGcode(command, -1);
        }
        else {
            // Try the legacy POST_VISION_HOME_COMMAND
            String postVisionHomeCommand = getCommand(null, CommandType.POST_VISION_HOME_COMMAND);
            if (postVisionHomeCommand != null 
                    && mappedAxes.getAxisX() != null 
                    && mappedAxes.getAxisY() != null
                    && mappedAxes.getAxisX().getDriver() == this 
                    && mappedAxes.getAxisY().getDriver() == this) { 
                // X, Y, are mapped to this driver, legacy support enabled
                postVisionHomeCommand = substituteVariable(postVisionHomeCommand, "X", mappedAxes.getAxisX().getLocationAxisCoordinate(location));
                postVisionHomeCommand = substituteVariable(postVisionHomeCommand, "Y", mappedAxes.getAxisY().getLocationAxisCoordinate(location));
                // Store the new current coordinate on the axis.
                mappedAxes.getAxisX().setCoordinate(location.getX());
                mappedAxes.getAxisY().setCoordinate(location.getY());
                sendGcode(postVisionHomeCommand, -1);
            }
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
    public void moveTo(ReferenceHeadMountable hm, MappedAxes mappedAxes, Location location, double speed)
            throws Exception {
        // keep copy for calling subdrivers as to not add offset on offset
        Location locationOriginal = location;

        location = location.convertToUnits(units);

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        double rotation = location.getRotation();

        Axis xAxis = getAxis(hm, Axis.Type.X);
        Axis yAxis = getAxis(hm, Axis.Type.Y);
        Axis zAxis = getAxis(hm, Axis.Type.Z);
        Axis rotationAxis = getAxis(hm, Axis.Type.Rotation);
        
        String command = getCommand(hm, CommandType.MOVE_TO_COMMAND);
        
        // If the command has forced-output coordinate variables "XF", "YF", "ZF" and "RotationF", 
        // always include the corresponding axis in the command.
        // This may be employed for shared physical axes, where OpenPNP cannot not keep track when an axis 
        // has physically moved behind its back through another axis. Consequently getCoordinate() 
        // may not reflect the actual physical coordinate. By always forcing the axis coordinate output, 
        // the controller will take care of restoring the shared axis' correct position, if necessary. 
        // As we are always moving in absolute coordinates this has no ill effect if it results in no 
        // position change after all. 
        // The same can be applied for other situations where OpenPNP may lose track of the physical 
        // location such as with Z-probing or relative moves in custom Gcode.
        // Note there is no need for separate backlash compensation variables, as these are always 
        // substituted alongside. 
        boolean includeX = (xAxis != null && hasVariable(command, "XF"));
        boolean includeY = (yAxis != null && hasVariable(command, "YF"));
        boolean includeZ = (zAxis != null && hasVariable(command, "ZF"));
        boolean includeRotation = (rotationAxis != null && hasVariable(command, "RotationF"));

        // Handle NaNs, which means don't move this axis for this move. We set the appropriate
        // axis reference to null, which we'll check for later. If the axis is force-included 
        // take the recorded current coordinate instead.  
    	
        // For each given coordinate, if the axis has a transform, transform the target coordinate
        // to it's raw value.
        if (Double.isNaN(x)) {
            if (includeX) {
            	x = xAxis.getCoordinate();
            }
            else {
            	xAxis = null;
            }
        }
        else if (xAxis != null && xAxis.getTransform() != null) {
            x = xAxis.getTransform().toRaw(xAxis, hm, x);
        }
        
        if (Double.isNaN(y)) {
        	if (includeY) {
            	y = yAxis.getCoordinate();
            }
            else {
            	yAxis = null;
            }
        }
        else if (yAxis != null && yAxis.getTransform() != null) {
            y = yAxis.getTransform().toRaw(yAxis, hm, y);
        }
        
        if (Double.isNaN(z)) {
        	if (includeZ) {
            	z = zAxis.getCoordinate();
            }
            else {
            	zAxis = null;
            }
        }
        else if (zAxis != null && zAxis.getTransform() != null) {
            z = zAxis.getTransform().toRaw(zAxis, hm, z);
        }
        
        if (Double.isNaN(rotation)) {
        	if (includeRotation) {
            	rotation = rotationAxis.getCoordinate();
            }
            else {
            	rotationAxis = null;
            }
        }
        else if (rotationAxis != null && rotationAxis.getTransform() != null) {
            rotation = rotationAxis.getTransform().toRaw(rotationAxis, hm, rotation);
        }

        // remember if moved
        boolean hasMoved = false;
        
        // Only do something if there at least one axis included in the move
        if (xAxis != null || yAxis != null || zAxis != null || rotationAxis != null) {

            command = substituteVariable(command, "Id", hm.getId());
            command = substituteVariable(command, "Name", hm.getName());
            command = substituteVariable(command, "FeedRate", maxFeedRate * speed);
            command = substituteVariable(command, "BacklashFeedRate", maxFeedRate * speed * backlashFeedRateFactor);

            /**
             * NSF gets applied to X and is multiplied by Y
             * 
             */
            
            // Primary checks to see if an axis should move
            if (xAxis != null && xAxis.getCoordinate() != x) {
                includeX = true;
            }
            if (yAxis != null && yAxis.getCoordinate() != y) {
                includeY = true;
            }
            if (zAxis != null && zAxis.getCoordinate() != z) {
                includeZ = true;
            }
            if (rotationAxis != null && rotationAxis.getCoordinate() != rotation) {
                includeRotation = true;
            }

            // If Y is moving and there is a non squareness factor we also need to move X, even if
            // no move was intended for X.
            if (includeY && nonSquarenessFactor != 0 && xAxis != null) {
                includeX = true;
            }
            
            if (includeX) {
                double newX = x + nonSquarenessFactor * y;
                command = substituteVariable(command, "X", newX);
                command = substituteVariable(command, "XF", newX);
                command = substituteVariable(command, "BacklashOffsetX", x + backlashOffsetX + nonSquarenessFactor * y); // Backlash Compensation
                command = substituteVariable(command, "XDecreasing", newX < xAxis.getCoordinate() ? true : null);
                command = substituteVariable(command, "XIncreasing", newX > xAxis.getCoordinate() ? true : null);
                if (xAxis.getPreMoveCommand() != null) {
                    String preMoveCommand = xAxis.getPreMoveCommand();
                    preMoveCommand = substituteVariable(preMoveCommand, "Coordinate", xAxis.getCoordinate());
                    sendGcode(preMoveCommand);
                }
                xAxis.setCoordinate(x);
            }
            else {
            	command = substituteVariable(command, "X", null);
            	command = substituteVariable(command, "XF", null);
                command = substituteVariable(command, "BacklashOffsetX", null); // Backlash Compensation
                command = substituteVariable(command, "XDecreasing", null);
                command = substituteVariable(command, "XIncreasing", null);
            }

            if (includeY) {
            	command = substituteVariable(command, "Y", y);
            	command = substituteVariable(command, "YF", y);
                command = substituteVariable(command, "BacklashOffsetY", y + backlashOffsetY); // Backlash Compensation
                command = substituteVariable(command, "YDecreasing", y < yAxis.getCoordinate() ? true : null);
                command = substituteVariable(command, "YIncreasing", y > yAxis.getCoordinate() ? true : null);
                if (yAxis.getPreMoveCommand() != null) {
                    String preMoveCommand = yAxis.getPreMoveCommand();
                    preMoveCommand = substituteVariable(preMoveCommand, "Coordinate", yAxis.getCoordinate());
                    sendGcode(preMoveCommand);
                }
            }
            else {
            	command = substituteVariable(command, "Y", null);
            	command = substituteVariable(command, "YF", null);
                command = substituteVariable(command, "BacklashOffsetY", null); // Backlash Compensation
                command = substituteVariable(command, "YDecreasing", null);
                command = substituteVariable(command, "YIncreasing", null);
            }

            if (includeZ) {
            	command = substituteVariable(command, "Z", z);
            	command = substituteVariable(command, "ZF", z);
                command = substituteVariable(command, "BacklashOffsetZ", z + backlashOffsetZ); // Backlash Compensation
                command = substituteVariable(command, "ZDecreasing", z < zAxis.getCoordinate() ? true : null);
                command = substituteVariable(command, "ZIncreasing", z > zAxis.getCoordinate() ? true : null);
                if (zAxis.getPreMoveCommand() != null) {
                    String preMoveCommand = zAxis.getPreMoveCommand();
                    preMoveCommand = substituteVariable(preMoveCommand, "Coordinate", zAxis.getCoordinate());
                    sendGcode(preMoveCommand);
                }
            }
            else {
                command = substituteVariable(command, "Z", null);
                command = substituteVariable(command, "ZF", null);
                command = substituteVariable(command, "BacklashOffsetZ", null); // Backlash Compensation
                command = substituteVariable(command, "ZDecreasing", null);
                command = substituteVariable(command, "ZIncreasing", null);
            }

            if (includeRotation) {
            	command = substituteVariable(command, "Rotation", rotation);
            	command = substituteVariable(command, "RotationF", rotation);
                command = substituteVariable(command, "BacklashOffsetRotation", rotation + backlashOffsetR); // Backlash Compensation
                command = substituteVariable(command, "RotationDecreasing", rotation < rotationAxis.getCoordinate() ? true : null);
                command = substituteVariable(command, "RotationIncreasing", rotation > rotationAxis.getCoordinate() ? true : null);
                if (rotationAxis.getPreMoveCommand() != null) {
                    String preMoveCommand = rotationAxis.getPreMoveCommand();
                    preMoveCommand = substituteVariable(preMoveCommand, "Coordinate", rotationAxis.getCoordinate());
                    sendGcode(preMoveCommand);
                }
            }
            else {
                command = substituteVariable(command, "Rotation", null);
                command = substituteVariable(command, "RotationF", null);
                command = substituteVariable(command, "BacklashOffsetRotation", null); // Backlash Compensation
                command = substituteVariable(command, "RotationDecreasing", null);
                command = substituteVariable(command, "RotationIncreasing", null);
            }

            // Only give a command when move is necessary
            if (includeX || includeY || includeZ || includeRotation) {

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
                
                hasMoved = true;

            } // there is a move

        } // there were axes involved

        // regardless of any action above the subdriver needs its actions based on original input
        for (ReferenceDriver driver : subDrivers) {
            driver.moveTo(hm, mappedAxes, locationOriginal, speed);
        }

        // if there was a move
        if (hasMoved) {
            /*
             * If moveToCompleteCommand is specified, send it
             */
            command = getCommand(hm, CommandType.MOVE_TO_COMPLETE_COMMAND);
            if (command != null) {
                    sendGcode(command);
            }
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
    
    private String actuatorRead(ReferenceActuator actuator, Double parameter) throws Exception {
        /**
         * The logic here is a little complicated. This is the only driver method that is
         * not fire and forget when it comes to sub-drivers. In this case, we need to know
         * if the command was serviced or not and throw an Exception if no (sub)driver was
         * able to service it.
         * 
         * So, the rules are:
         * 
         * 1. If a (sub)driver has a command and regex defined, it is the servicer and must
         *    either return a value or throw an Exception.
         * 2. If a (sub)driver cannot service the command it should defer to to any
         *    child sub-drivers.
         * 3. If the top level driver cannot either service the command or have a sub-driver
         *    service the command it should throw. 
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
            /**
             * If the command or regex is null we'll query the subdrivers. The first to respond
             * with a non-null value wins.
             */
            for (ReferenceDriver driver : subDrivers) {
                String val = driver.actuatorRead(actuator);
                if (val != null) {
                    return val;
                }
            }
            /**
             * If none of the subdrivers returned a value and this is the top level driver then
             * we've exhausted all the options to service the command, so throw an error.
             */
            if (parent == null) {
                throw new Exception(String.format("Actuator \"%s\" read error: Driver configuration is missing ACTUATOR_READ_COMMAND or ACTUATOR_READ_REGEX.", actuator.getName()));
            }
            else {
                return null;
            }
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
            getCommunications().writeLine(command);
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
                line = getCommunications().readLine().trim();
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

    public double getBacklashOffsetX() {
        return backlashOffsetX;
    }
    
    public void setBacklashOffsetX(double BacklashOffsetX) {
        this.backlashOffsetX = BacklashOffsetX;
    }
    
    public double getBacklashOffsetY() {
        return backlashOffsetY;
    }
    
    public void setBacklashOffsetY(double BacklashOffsetY) {
        this.backlashOffsetY = BacklashOffsetY;
    }
    
    public double getBacklashOffsetZ() {
        return backlashOffsetZ;
    }
    
    public void setBacklashOffsetZ(double BacklashOffsetZ) {
        this.backlashOffsetZ = BacklashOffsetZ;
    }
    
    public double getBacklashOffsetR() {
        return backlashOffsetR;
    }
    
    public void setBacklashOffsetR(double BacklashOffsetR) {
        this.backlashOffsetR = BacklashOffsetR;
    }
    
    public double getBacklashFeedRateFactor() {
        return backlashFeedRateFactor;
    }
    
    public void setBacklashFeedRateFactor(double BacklashFeedRateFactor) {
        this.backlashFeedRateFactor = BacklashFeedRateFactor;
    }
    
    public void setNonSquarenessFactor(double NonSquarenessFactor) {
        this.nonSquarenessFactor = NonSquarenessFactor;
    }
    
    public double getNonSquarenessFactor() {
        return this.nonSquarenessFactor;
    }
    
    public int getMaxFeedRate() {
        return maxFeedRate;
    }

    public void setMaxFeedRate(int maxFeedRate) {
        this.maxFeedRate = maxFeedRate;
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
    
    public boolean isVisualHomingEnabled() {
        return visualHomingEnabled;
    }

    public void setVisualHomingEnabled(boolean visualHomingEnabled) {
        this.visualHomingEnabled = visualHomingEnabled;
    }

    public boolean isBackslashEscapedCharactersEnabled() {
        return backslashEscapedCharactersEnabled;
    }

    public void setBackslashEscapedCharactersEnabled(boolean backslashEscapedCharactersEnabled) {
        this.backslashEscapedCharactersEnabled = backslashEscapedCharactersEnabled;
    }

    @Deprecated
    @Override
    public void migrateDriver(ReferenceMachine machine) throws Exception {
        // Migrate visual homing setting.
        for (Head head : machine.getHeads()) {
            if (visualHomingEnabled) {
                // Assuming only one (sub-) driver will have this enabled.  
                ((AbstractHead)head).setVisualHomingEnabled(visualHomingEnabled);
                Location homingFiducialLocation = this.homingFiducialLocation;
                // because squareness compensation is now properly applied to the homing fiducial location, we need to "unapply" it here.
                homingFiducialLocation = homingFiducialLocation
                        .subtract(new Location(homingFiducialLocation.getUnits(), homingFiducialLocation.getY()*nonSquarenessFactor, 0, 0, 0));
                ((AbstractHead)head).setHomingFiducialLocation(homingFiducialLocation);
            }
        }
        // Handle sub-drivers.
        for (GcodeDriver gcodeDriver : subDrivers) {
            machine.addDriver(gcodeDriver);
            gcodeDriver.migrateDriver(machine);
        }
        // TODO: set it null after the last use of it was reworked.
        subDrivers = new ArrayList<>();
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

        @Element(required = false, data = true)
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
            double raw = (transformedCoordinate - camWheelRadius - camWheelGap) / camRadius;
            raw = Math.min(Math.max(raw, -1), 1);
            raw = Math.toDegrees(Math.asin(raw));
            if (hm.getId().equals(negatedHeadMountableId)) {
                raw = -raw;
            }
            return raw;
        }
    }
    
    public static class OffsetTransform implements AxisTransform {
        @ElementMap(required=false)
        HashMap<String, Double> offsetsByHeadMountableId = new HashMap<>();
        
        public OffsetTransform() {
            offsetsByHeadMountableId.put("N1", 1.);
        }

        @Override
        public double toTransformed(Axis axis, HeadMountable hm, double rawCoordinate) {
            Double offset = offsetsByHeadMountableId.get(hm.getId());
            if (offset != null) {
                return rawCoordinate + offset;
            }
            return rawCoordinate;
        }

        @Override
        public double toRaw(Axis axis, HeadMountable hm, double transformedCoordinate) {
            Double offset = offsetsByHeadMountableId.get(hm.getId());
            if (offset != null) {
                return transformedCoordinate - offset;
            }
            return transformedCoordinate;
        }
    }
    
    public static class ScalingTransform implements AxisTransform {

        @Attribute(required = false)
        private double scaleFactor = 1;

        @Override
        public double toTransformed(Axis axis, HeadMountable hm, double rawCoordinate) {
            if (scaleFactor == 0) {
                Logger.info("Scale factor 0 is not allowed, defaults to 1.");
                scaleFactor = 1;
            }
            return rawCoordinate / scaleFactor;
        }

        @Override
        public double toRaw(Axis axis, HeadMountable hm, double transformedCoordinate) {
            if (scaleFactor == 0) {
                Logger.info("Scale factor 0 is not allowed, defaults to 1.");
                scaleFactor = 1;
            }
            return transformedCoordinate * scaleFactor;
        }
    }

}
