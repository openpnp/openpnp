/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 *
 * This file is part of OpenPnP.
 *
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.driver;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openpnp.Translations;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.SimulationModeMachine;
import org.openpnp.machine.reference.axis.ReferenceCamClockwiseAxis;
import org.openpnp.machine.reference.axis.ReferenceCamCounterClockwiseAxis;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis.BacklashCompensationMethod;
import org.openpnp.machine.reference.axis.ReferenceLinearTransformAxis;
import org.openpnp.machine.reference.axis.ReferenceMappedAxis;
import org.openpnp.machine.reference.driver.wizards.GcodeDriverConsole;
import org.openpnp.machine.reference.driver.wizards.GcodeDriverGcodes;
import org.openpnp.machine.reference.driver.wizards.GcodeDriverSettings;
import org.openpnp.machine.reference.solutions.GcodeDriverSolutions;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Motion.MoveToCommand;
import org.openpnp.model.Named;
import org.openpnp.model.Solutions;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.Camera;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.base.AbstractAxis;
import org.openpnp.spi.base.AbstractCamera;
import org.openpnp.spi.base.AbstractHead;
import org.openpnp.spi.base.AbstractHead.VisualHomingMethod;
import org.openpnp.spi.base.AbstractHeadMountable;
import org.openpnp.spi.base.AbstractSingleTransformedAxis;
import org.openpnp.spi.base.AbstractTransformedAxis;
import org.openpnp.util.NanosecondTime;
import org.openpnp.util.TextUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;

import com.google.common.base.Joiner;

@Root
public class GcodeDriver extends AbstractReferenceDriver implements Named {
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
        HOME_COMPLETE_REGEX,
        SET_GLOBAL_OFFSETS_COMMAND("Id", "Name", "X", "Y", "Z", "Rotation"),
        GET_POSITION_COMMAND,
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
        ACTUATOR_READ_COMMAND(true, "Id", "Name", "Index", "DoubleValue", "IntegerValue", "Value"),
        @Deprecated
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

        public boolean isDeprecated() {
            try {
                Class<CommandType> commandTypeEnum = CommandType.class;
                Field commandType = commandTypeEnum.getField(toString());
                return commandType.isAnnotationPresent(Deprecated.class);
            }
            catch (NoSuchFieldException e) {
                return false;
            }
            catch (SecurityException e) {
                return false;
            }
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

    @Deprecated
    @Attribute(required = false)
    protected double backlashFeedRateFactor = 0.1;

    @Attribute(required = false)
    protected int timeoutMilliseconds = 5000;

    @Attribute(required = false)
    protected int connectWaitTimeMilliseconds = 3000;

    @Attribute(required = false)
    protected int dollarWaitTimeMilliseconds = 50;

    @Deprecated
    @Attribute(required = false)
    protected boolean visualHomingEnabled = true;

    @Attribute(required = false)
    protected boolean backslashEscapedCharactersEnabled = false;

    @Attribute(required = false)
    protected boolean removeComments;

    @Attribute(required = false)
    protected boolean compressGcode;

    @Attribute(required = false)
    protected boolean loggingGcode;

    @Deprecated
    @Element(required = false)
    protected Location homingFiducialLocation = new Location(LengthUnit.Millimeters);

    @Attribute(required = false)
    boolean supportingPreMove = false;

    @Attribute(required = false)
    boolean usingLetterVariables = true;
    
    @Attribute(required = false) 
    int infinityTimeoutMilliseconds = 60000; // 1 Minute is considered an "eternity" for a controller.

    @Element(required = false, data=true) 
    String detectedFirmware = null; 

    @Element(required = false, data=true) 
    String reportedAxes = null; 

    @Element(required = false, data=true)
    String configuredAxes = null;

    @ElementList(required = false, inline = true)
    public ArrayList<Command> commands = new ArrayList<>();

    @Deprecated
    @ElementList(required = false)
    protected List<GcodeDriver> subDrivers = null;

    @Deprecated
    @ElementList(required = false)
    protected List<Axis> axes = null;

    private ReaderThread readerThread;
    volatile boolean disconnectRequested;
    protected boolean connected;
    
    static public class Line {
        final String line;
        final double transmissionTime;

        public Line(String line) {
            super();
            this.line = line;
            this.transmissionTime = NanosecondTime.getRuntimeSeconds();
        }

        public String getLine() {
            return line;
        }

        /**
         * @return The real-time in seconds (since application start) when this Line was sent or received.
         */
        public double getTransmissionTime() {
            return transmissionTime;
        }

        @Override
        public String toString() {
            return line;
        }
    }

    protected LinkedBlockingQueue<Line> responseQueue = new LinkedBlockingQueue<>();
    protected LinkedBlockingQueue<AxesLocation> reportedLocationsQueue = new LinkedBlockingQueue<>();
    protected LinkedBlockingQueue<Line> receivedConfirmationsQueue = new LinkedBlockingQueue<>();

    protected Line errorResponse;
    private boolean motionPending;

    private PrintWriter gcodeLogger;

    @Commit
    public void commit() {
        super.commit();

        for (Command command : commands) {
            if(command.type == CommandType.ACTUATOR_READ_WITH_DOUBLE_COMMAND) {
                Command actuatorReadCommand = getExactCommand(
                        command.headMountableId,
                        CommandType.ACTUATOR_READ_COMMAND
                );

                if(actuatorReadCommand == null) {
                    command.type = CommandType.ACTUATOR_READ_COMMAND;
                }
            }
        }
    }

    @Override
    public void createDefaults() throws Exception {
        createAxisMappingDefaults((ReferenceMachine) Configuration.get().getMachine());

        createDefaultCommands();
    }

    public void createDefaultCommands() {
        commands = new ArrayList<>();
        commands.add(new Command(null, CommandType.COMMAND_CONFIRM_REGEX, "^ok.*"));
        commands.add(new Command(null, CommandType.CONNECT_COMMAND, "G21 ; Set millimeters mode\nG90 ; Set absolute positioning mode\nM82 ; Set absolute mode for extruder"));
        commands.add(new Command(null, CommandType.HOME_COMMAND, "G28 ; Home all axes"));
        commands.add(new Command(null, CommandType.SET_GLOBAL_OFFSETS_COMMAND, "G92 {XL}{X:%.4f} {YL}{Y:%.4f} {ZL}{Z:%.4f} {RotationL}{Rotation:%.4f} ; Reset current position to given coordinates"));
        commands.add(new Command(null, CommandType.MOVE_TO_COMMAND, "{Acceleration:M204 S%.1f} G0 {XL}{X:%.4f} {YL}{Y:%.4f} {ZL}{Z:%.4f} {RotationL}{Rotation:%.4f} {FeedRate:F%.1f} ; Send standard Gcode move"));
        commands.add(new Command(null, CommandType.MOVE_TO_COMPLETE_COMMAND, "M400 ; Wait for moves to complete before returning"));
    }

    public synchronized void connect() throws Exception {
        disconnectRequested = false;
        getCommunications().connect();
        connected = false;

        connectThreads();

        // Wait a bit while the controller starts up
        Thread.sleep(connectWaitTimeMilliseconds);

        // Consume any startup messages
        try {
            while (!receiveResponses().isEmpty()) {

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

    /**
     * Connect the threads used for communications.
     * 
     * @throws Exception
     */
    protected void connectThreads() throws Exception {
        readerThread = new ReaderThread();
        readerThread.setDaemon(true);
        readerThread.start();
        errorResponse = null;
        receivedConfirmationsQueue = new LinkedBlockingQueue<>();
        reportedLocationsQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void setEnabled(boolean enabled) throws Exception {
        if (enabled && !connected) {
            connect();
        }
        if (connected) {
            if (enabled) {
                // Assume a freshly re-enabled machine has no pending moves anymore.
                motionPending = false;
                sendGcode(getCommand(null, CommandType.ENABLE_COMMAND));
            }
            else {
                try {
                    sendGcode(getCommand(null, CommandType.DISABLE_COMMAND));
                    drainCommandQueue(getTimeoutAtMachineSpeed());
                }
                catch (Exception e) {
                    // When the connection is lost, we have IO errors. We should still be able to go on
                    // disabling the machine.
                    Logger.warn(e);
                }
            }
        }

        if (connected && !enabled) {
            if (isInSimulationMode() || !connectionKeepAlive) {
                disconnect();
            }
        }
        super.setEnabled(enabled);
    }

    @Override
    public void home(Machine machine) throws Exception {
        // Home is sent with an infinite timeout since it's tough to tell how long it will
        // take.
        String command = getCommand(null, CommandType.HOME_COMMAND);
        // legacy head support
        Head head = machine.getDefaultHead();
        command = substituteVariable(command, "Id", head.getId()); 
        command = substituteVariable(command, "Name", head.getName());
        if (isUsingLetterVariables()) {
            AxesLocation axesHomeLocation =  new AxesLocation(machine, 
                    (axis) -> (axis.getHomeCoordinate())); 
            Double feedrate = null;
            Double acceleration = null;
            Double jerk = null;
            for (String variable : getAxisVariables((ReferenceMachine) machine)) {
                ControllerAxis axis = axesHomeLocation.getAxisByVariable(this, variable);
                if (axis != null) {
                    double coordinate;
                    if (axis.getType() == Type.Rotation) {
                        // Never convert rotation to driver units.
                        coordinate = axesHomeLocation.getCoordinate(axis);
                    }
                    else {
                        coordinate = axesHomeLocation.getCoordinate(axis, getUnits());
                    }
                    command = substituteVariable(command, variable, coordinate);
                    command = substituteVariable(command, variable+"L", 
                            axis.getLetter());

                    // Because in homing we don't know which axis is moved when and in what combination, 
                    // we need to find the lowest rates of any axis.
                    if (axis.getMotionLimit(1) != 0.0) {
                        if (feedrate == null || feedrate > axis.getMotionLimit(1)) {
                            feedrate = axis.getMotionLimit(1);
                        }
                    }
                    if (axis.getMotionLimit(2) != 0.0) {
                        if (acceleration == null || acceleration > axis.getMotionLimit(2)) {
                            acceleration = axis.getMotionLimit(2);
                        }
                    }
                    if (axis.getMotionLimit(3) != 0.0) {
                        if (jerk == null || jerk > axis.getMotionLimit(3)) {
                            feedrate = axis.getMotionLimit(3);
                        }
                    }
                }
                else {
                    command = substituteVariable(command, variable, null);
                    command = substituteVariable(command, variable+"L", null); 
                }
            }

            if (getMotionControlType().isUnpredictable()) {
                // Do not initialize rates, as the motion control is unpredictable, i.e. not controlled by us.  
                command = substituteVariable(command, "FeedRate", null);
                command = substituteVariable(command, "Acceleration", null);
                command = substituteVariable(command, "Jerk", null);
            }
            else {
                // For the purpose of homing, initialize the rates to the lowest of any axis. 
                command = substituteVariable(command, "FeedRate", feedrate);
                command = substituteVariable(command, "Acceleration", acceleration);
                command = substituteVariable(command, "Jerk", jerk);
            }
        }
        else {
            // Do not initialize rates in legacy mode.  
            command = substituteVariable(command, "FeedRate", null);
            command = substituteVariable(command, "Acceleration", null);
            command = substituteVariable(command, "Jerk", null);
        }

        long timeout = -1;
        sendGcode(command, timeout);

        // Check home complete response against user's regex
        String homeCompleteRegex = getCommand(null, CommandType.HOME_COMPLETE_REGEX);
        if (homeCompleteRegex != null) {
            receiveResponses(homeCompleteRegex, timeout, (responses) -> { 
                throw new Exception("Timed out waiting for home to complete."); 
            });
        }

        AxesLocation homeLocation = new AxesLocation(machine, this, (axis) -> (axis.getHomeCoordinate()));
        homeLocation.setToDriverCoordinates(this);
    }

    public List<String> getAxisVariables(ReferenceMachine machine) {
        List<String> variables = new ArrayList<>();
        if (usingLetterVariables) {
            for (ControllerAxis axis : getAxes(machine)) {
                String letter = axis.getLetter(); 
                if (letter != null && !letter.isEmpty()) {
                    variables.add(letter);
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
    public void setGlobalOffsets(Machine machine, AxesLocation axesLocation)
            throws Exception {
        // Compose the command
        String command = getCommand(null, CommandType.SET_GLOBAL_OFFSETS_COMMAND);
        if (command != null) {
            // legacy head support
            Head head = machine.getDefaultHead();
            command = substituteVariable(command, "Id", head.getId());
            command = substituteVariable(command, "Name", head.getName());
            boolean isEmpty = true;
            for (String variable : getAxisVariables((ReferenceMachine) machine)) {
                ControllerAxis axis = axesLocation.getAxisByVariable(this, variable);
                if (axis != null) {
                    if (hasVariable(command, variable)) {
                        double coordinate;
                        if (axis.getType() == Type.Rotation) {
                            // Never convert rotation to driver units.
                            coordinate = axesLocation.getCoordinate(axis);
                        }
                        else {
                            coordinate = axesLocation.getCoordinate(axis, getUnits());
                        }
                        command = substituteVariable(command, variable, coordinate);
                        command = substituteVariable(command, variable+"L", 
                                axis.getLetter());
                        // Store the new driver coordinate on the axis.
                        axis.setDriverCoordinate(coordinate);
                        isEmpty = false;
                    }
                    else {
                        // It is imperative that the axis global offset is really set. Otherwise all bets 
                        // are off and collisions in subsequent moves are very likely. 
                        throw new Exception("Axis variable "+variable+" is missing in SET_GLOBAL_OFFSETS_COMMAND.");
                    }
                }
                else {
                    command = substituteVariable(command, variable, null);
                    command = substituteVariable(command, variable+"L", null); 
                }
            }
            if (!isEmpty) {
                // If no axes are included, the G92 command must not be executed, because it would otherwise reset all
                // axes to zero in some controllers! 
                sendGcode(command, -1);
            }
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
                axisX.setDriverCoordinate(axesLocation.getCoordinate(axisX, getUnits()));
                axisY.setDriverCoordinate(axesLocation.getCoordinate(axisY, getUnits()));
            }
        }
    }


    @Override
    public AxesLocation getReportedLocation(long timeout) throws Exception {
        String command = getCommand(null, CommandType.GET_POSITION_COMMAND);
        if (command == null) {
            throw new Exception(getName()+" configuration error: missing GET_POSITION_COMMAND.");
        }
        if (getCommand(null, CommandType.POSITION_REPORT_REGEX) == null) {
            throw new Exception(getName()+" configuration error: missing POSITION_REPORT_REGEX.");
        }

        // TODO: true queued reporting. For now it is sufficient to poll one for one.
        reportedLocationsQueue.clear();
        sendGcode(command, -1);
        if (timeout == -1) {
            timeout = infinityTimeoutMilliseconds;
        }
        AxesLocation lastReportedLocation = reportedLocationsQueue.poll(timeout, TimeUnit.MILLISECONDS);
        if (lastReportedLocation != null) {
            Logger.trace("{} got lastReportedLocation {}", getName(), lastReportedLocation);
            return lastReportedLocation;
        }
        // Timeout expired.
        throw new Exception(getName()+" timeout waiting for response to " + command);
    }

    /**
     * This is similar to the other getCommand calls, except the head mountable id is passed in and it must match
     * exactly. By passing in null or "*", you will get a command that matches that head mountable id or null.
     */
    public Command getExactCommand(String headMountableId, CommandType type) {
        for (Command c : commands) {
            if(c.type != type) {
                continue;
            }

            if(c.headMountableId != null && c.headMountableId.equals(headMountableId)) {
                return c;
            } else if(c.headMountableId == null && headMountableId == null) {
                return c;
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
    public void moveTo(HeadMountable hm, MoveToCommand move)
            throws Exception {
        if (isUsingLetterVariables() && isSupportingPreMove()) {
            throw new Exception(getName()+" configuration error: Cannot enable both Letter Variables and Allow Pre-Move Commands.");
        }
        // Get the axes that are actually moving.
        AxesLocation movedAxesLocation = move.getMovedAxesLocation();
        AxesLocation allAxesLocation = move.getLocation1();
        Double feedRate = move.getFeedRatePerMinute();
        Double acceleration = move.getAccelerationPerSecond2();
        Double jerk = move.getJerkPerSecond3();
        double driverDistance = movedAxesLocation.getEuclideanMetric(this, (axis) -> 
            movedAxesLocation.getLengthCoordinate(axis).convertToUnits(getUnits()).getValue() - axis.getDriverCoordinate()).third;

        // Start composing the command, will decide later, whether we actually send it.
        String command = getCommand(hm, CommandType.MOVE_TO_COMMAND);
        if (command == null) {
            if (movedAxesLocation.isEmpty()) {
                return;
            }
            if (isSupportingPreMove()) {
                throw new Exception(getName()+" MOVE_TO_COMMAND missing for "+hm.getClass().getSimpleName()+" "+hm.getName()
                +", please set the command in the driver (no automatic support by Issues & Solutions due to driver Allow Pre-Move Commands option).");
            }
            else {
                throw new Exception(getName()+" MOVE_TO_COMMAND missing, please use Issues & Solutions to propose proper G-code commands.");
            }
        }
        if (hasVariable(command, "BacklashFeedRate")) {
            throw new Exception(getName()+" configuration upgrade needed: Please remove the extra backlash compensation move from your MOVE_TO_COMMAND. "
                    +"Backlash compensation is now done outside of the drivers and configured on the axes.");
        }

        command = substituteVariable(command, "Id", hm.getId());
        command = substituteVariable(command, "Name", hm.getName());
        command = substituteVariable(command, "FeedRate", feedRate);
        command = substituteVariable(command, "Acceleration", acceleration);
        command = substituteVariable(command, "Jerk", jerk);

        ReferenceMachine machine = (ReferenceMachine) hm.getHead().getMachine();
        // Get a map of the axes of ...
        AxesLocation mappedAxes = (this.usingLetterVariables ?
                allAxesLocation                // ... all the axes in case of using letter variables
                : hm.getMappedAxes(machine))   // ... just the HeadMountable in case of using type variables
                .drivenBy(this);               // ... but just those driven by this driver.
        // Go through all the axes variables and handle them.
        boolean doesMove = false;
        for (String variable : getAxisVariables(machine)) {
            // Note, if the axis is included in the location, this means it actually changes the coordinate in resolution steps. 
            // The resolution stepping is used to suppress artificial coordinate changes due to floating point artifacts from transformations etc. 
            // If set up correctly, this also suppresses "rounded-to-nothing" moves due to MOVE_TO_COMMANDs format specifier (usually %.4f).
            ControllerAxis axis = movedAxesLocation.getAxisByVariable(this, variable);
            if (axis == null) {
                // Axis not moved. Might still be forced.

                // If the command has forced-output coordinate variables "XF", "YF", "ZF" etc., 
                // always include the corresponding axis in the command.
                // This may be employed for axes, where OpenPNP cannot keep track when an axis has physically 
                // moved behind its back. By always forcing the axis coordinate output, the controller will take care 
                // of restoring the axis position, if necessary.  
                // As we are always moving in absolute coordinates this has no ill effect if it results in no 
                // position change after all. 
                // Note, there is no need for separate backlash compensation variables, as these are always 
                // substituted alongside. 
                if (hasVariable(command, variable+"F")) {
                    // Force it! Must get it from the mappedAxes. If the mappedAxes do not have it, it is 
                    // still suppressed (this never happens when using letter variables). 
                    axis = mappedAxes.getAxisByVariable(this, variable);
                }
            }
            if (axis != null) {
                // The move is definitely on. 
                doesMove = true;
                // TODO: discuss whether we should round to axis resolution here.
                double coordinate;
                if (axis.getType() == Type.Rotation) {
                    // Never convert rotation to driver units.
                    coordinate = allAxesLocation.getCoordinate(axis);
                }
                else {
                    coordinate = allAxesLocation.getCoordinate(axis, getUnits());
                }
                double previousCoordinate = axis.getDriverCoordinate(); 
                int direction = ((Double)coordinate).compareTo(previousCoordinate);
                // Substitute the axis variables.
                command = substituteVariable(command, variable, coordinate);
                command = substituteVariable(command, variable+"F", coordinate);
                command = substituteVariable(command, variable+"L", axis.getLetter());
                if (hasVariable(command, "BacklashOffset"+variable)) {
                    throw new Exception(getName()+" configuration upgrade needed: Please remove the extra backlash compensation move from your MOVE_TO_COMMAND. "
                            +"Backlash compensation is now done outside of the drivers.");
                }
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
                // Axis specific jerk limits are needed on TinyG.
                double axisDistance = coordinate - previousCoordinate;
                double axisJerk = (jerk != null ? jerk : 0)*Math.abs(axisDistance)/driverDistance;
                command = substituteVariable(command, variable+"Jerk", axisJerk > 1 ? axisJerk : null);
                command = substituteVariable(command, variable+"JerkMupm3", axisJerk > 4.63 ? axisJerk*1e-6*Math.pow(60, 3) : null); // TinyG: Megaunits/min^3 
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
                command = substituteVariable(command, variable+"Jerk", null);
                command = substituteVariable(command, variable+"JerkMupm3", null);  
            }
        }
        if (doesMove) {
            // We do actually send the command.
            motionPending = true;
            sendGcode(command);
        }
    }

    @Override
    public boolean isMotionPending() {
        return motionPending;
    }

    protected void drainCommandQueue(long timeout) throws InterruptedException {
        // This does nothing in the plain GcodeDriver. It will be overridden in the GcodeAsyncDriver.
    }

    @Override
    public void waitForCompletion(HeadMountable hm,
            CompletionType completionType) throws Exception {
        if (!(completionType.isUnconditionalCoordination() 
                || isMotionPending())) {
            return;
        }
        String command = getCommand(hm, CommandType.MOVE_TO_COMPLETE_COMMAND);
        if (command != null) {
            sendGcode(command, completionType == CompletionType.WaitForStillstandIndefinitely ?
                    -1 : getTimeoutAtMachineSpeed());
        }

        if (completionType.isEnforcingStillstand()) {
            if (isMotionPending()) {
                /*
                 * If moveToCompleteRegex is specified we need to wait until we match the regex in a
                 * response before continuing. We first search the initial responses from the
                 * command for the regex. If it's not found we then collect responses for up to
                 * timeoutMillis while searching the responses for the regex. As soon as it is
                 * matched we continue. If it's not matched within the timeout we throw an
                 * Exception.
                 *
                 * AFAIK, this was used on TinyG and it is now obsolete with new firmware :
                 * https://makr.zone/tinyg-new-g-code-commands-for-openpnp-use/577/
                 */
                String moveToCompleteRegex = getCommand(hm, CommandType.MOVE_TO_COMPLETE_REGEX);
                if (moveToCompleteRegex != null) {
                    receiveResponses(moveToCompleteRegex, completionType == CompletionType.WaitForStillstandIndefinitely ?
                            -1 : getTimeoutAtMachineSpeed(),
                            (responses) -> {
                        throw new Exception("Timed out waiting for move to complete.");
                    });
                }
            }
            // Remember, we're now standing still.
            motionPending = false;
        }
    }

    private boolean containsMatch(List<Line> responses, String regex) {
        for (Line response : responses) {
            if (response.line.matches(regex)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void actuate(Actuator actuator, boolean on) throws Exception {
        String command = getCommand(actuator, CommandType.ACTUATE_BOOLEAN_COMMAND);
        command = substituteVariable(command, "Id", actuator.getId());
        command = substituteVariable(command, "Name", actuator.getName());
        if (actuator instanceof ReferenceActuator) {
            command = substituteVariable(command, "Index", ((ReferenceActuator)actuator).getIndex());
        }
        command = substituteVariable(command, "BooleanValue", on);
        command = substituteVariable(command, "True", on ? on : null);
        command = substituteVariable(command, "False", on ? null : on);
        sendGcode(command);
        SimulationModeMachine.simulateActuate(actuator, on, true);
    }

    @Override
    public void actuate(Actuator actuator, double value) throws Exception {
        String command = getCommand(actuator, CommandType.ACTUATE_DOUBLE_COMMAND);
        command = substituteVariable(command, "Id", actuator.getId());
        command = substituteVariable(command, "Name", actuator.getName());
        if (actuator instanceof ReferenceActuator) {
            command = substituteVariable(command, "Index", ((ReferenceActuator)actuator).getIndex());
        }
        command = substituteVariable(command, "DoubleValue", value);
        command = substituteVariable(command, "IntegerValue", (int) value);
        sendGcode(command);
        SimulationModeMachine.simulateActuate(actuator, value, true);
    }

    @Override
    public void actuate(Actuator actuator, String value) throws Exception {
        String command = getCommand(actuator, CommandType.ACTUATE_STRING_COMMAND);
        command = substituteVariable(command, "Id", actuator.getId());
        command = substituteVariable(command, "Name", actuator.getName());
        if (actuator instanceof ReferenceActuator) {
            command = substituteVariable(command, "Index", ((ReferenceActuator)actuator).getIndex());
        }
        command = substituteVariable(command, "StringValue", value);
        sendGcode(command);
    }

    @Override
    public String actuatorRead(Actuator actuator, Object parameter) throws Exception {
        /*
         * The logic here is a little complicated. This is the only driver method that is
         * not fire and forget. In this case, we need to know if the command was serviced or not
         * and throw an Exception if not.
         */
        String command = getCommand(actuator, CommandType.ACTUATOR_READ_COMMAND);
        String regex = getCommand(actuator, CommandType.ACTUATOR_READ_REGEX);
        if (command != null && regex != null) {
            command = substituteVariable(command, "Id", actuator.getId());
            command = substituteVariable(command, "Name", actuator.getName());
            if (actuator instanceof ReferenceActuator) {
                command = substituteVariable(command, "Index", ((ReferenceActuator)actuator).getIndex());
            }
            if (parameter != null) {
                if (parameter instanceof Double) { // Backwards compatibility
                    Double doubleParameter = (Double) parameter;
                    command = substituteVariable(command, "DoubleValue", doubleParameter);
                    command = substituteVariable(command, "IntegerValue", (int) doubleParameter.doubleValue());
                }

                command = substituteVariable(command, "Value", parameter);
            }
            sendGcode(command);
            List<Line> responses = receiveResponses(regex, timeoutMilliseconds, (r) -> {
                throw new Exception(String.format("Actuator \"%s\" read error: No matching responses found.", actuator.getName()));
            });

            Pattern pattern = Pattern.compile(regex);
            for (Line line : responses) {
                Matcher matcher = pattern.matcher(line.getLine());
                if (matcher.matches()) {
                    Logger.trace("actuatorRead response: {}", line);
                    try {
                        return matcher.group("Value");
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
            // This should not happen, as the regex is pre-matched in receiveResponses().
            throw new Exception(String.format("Actuator \"%s\" read error: Regex matching response vanished.", actuator.getName()));
        }
        else {
            throw new Exception(String.format("Actuator \"%s\" read error: Driver configuration is missing ACTUATOR_READ_COMMAND or ACTUATOR_READ_REGEX.", actuator.getName()));
        }
    }

    @Override
    public String actuatorRead(Actuator actuator) throws Exception {
        return actuatorRead(actuator, null);
    }

    public synchronized void disconnect() {
        disconnectRequested = true;
        connected = false;

        try {
            getCommunications().disconnect();
        }
        catch (Exception e) {
            Logger.error(e, "disconnect()");
        }

        disconnectThreads();

        closeGcodeLogger();
    }

    /**
     *  Disconnect the threads used for communications.
     */
    protected void disconnectThreads() {
        try {
            if (readerThread != null && readerThread.isAlive()) {
                readerThread.join(3000);
            }
        }
        catch (Exception e) {
            Logger.error(e, "disconnect()");
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    protected void sendGcode(String gCode) throws Exception {
        sendGcode(gCode, timeoutMilliseconds);
    }

    protected long getTimeoutAtMachineSpeed() {
        return timeoutMilliseconds == -1 ?
                timeoutMilliseconds 
                : Math.round(timeoutMilliseconds/Math.max(0.05, Configuration.get().getMachine().getSpeed()));
    }

    protected void sendGcode(String gCode, long timeout) throws Exception {
        if (gCode == null) {
            return;
        }
        for (String command : gCode.split("\n")) {
            command = command.trim();
            if (command.length() == 0) {
                continue;
            }
            sendCommand(command, timeout);
        }
    }

    public void sendCommand(String command) throws Exception {
        sendCommand(command, timeoutMilliseconds);
    }

    public void sendCommand(String command, long timeout) throws Exception {
        // An error may have popped up in the meantime. Check and bail on it, before sending the next command. 
        bailOnError();
        if (command == null) {
            return;
        }

        Logger.debug("[{}] >> {}, {}", getCommunications().getConnectionName(), command, timeout);
        command = preProcessCommand(command);
        if (command.isEmpty()) {
            Logger.debug("{} empty command after pre process", getCommunications().getConnectionName());
            return;
        }

        // After sending this, we want one more confirmation. 
        // TODO: true queued reporting. For now it is sufficient to poll one for one.
        receivedConfirmationsQueue.clear();
        try {
            // Send the command.
            getCommunications().writeLine(command);
        }
        catch (IOException ex) {
            Logger.error(ex, "{} failed to write command {}", getCommunications().getConnectionName(), command);
            disconnect();
            Configuration.get().getMachine().setEnabled(false);
        }
        waitForConfirmation(command, timeout);
        if (command.startsWith("$")) {
            Thread.sleep(dollarWaitTimeMilliseconds);
        }
    }

    protected Line waitForConfirmation(String command, long timeout)
            throws Exception {
        if (getCommand(null, CommandType.COMMAND_CONFIRM_REGEX) == null) {
           Logger.warn(getName()+" configuration error: COMMAND_CONFIRM_REGEX missing. Not waiting for confirmation.");
           return null;
        }

        if (timeout == -1) {
            timeout = infinityTimeoutMilliseconds;
        }
        Line receivedConfirmation = receivedConfirmationsQueue.poll(timeout, TimeUnit.MILLISECONDS);
        if (receivedConfirmation != null) {
            Logger.trace("[{}] confirmed {}", getCommunications().getConnectionName(), command);
            return receivedConfirmation;
        }
        // Timeout expired.
        throw new Exception(getCommunications().getConnectionName()+" timeout waiting for response to "+command);
    }

    protected void bailOnError() throws Exception {
        if (errorResponse != null) {
            Line error = errorResponse; 
            errorResponse = null;
            throw new Exception(getCommunications().getConnectionName()+" error response from controller: " + error);
        }
        if (readerThread == null || !readerThread.isAlive()) {
            throw new Exception(getCommunications().getConnectionName()+" IO Error on reading from the controller.");
        }
    }

    public List<Line> receiveResponses() throws Exception {
        bailOnError();
        List<Line> responses = new ArrayList<>();
        // Read any responses that might be queued up.
        responseQueue.drainTo(responses);
        return responses;
    }

    @FunctionalInterface
    public interface TimeoutAction {
        List<Line> apply(List<Line> responses) throws Exception;
    }

    public List<Line> receiveResponses(String regex, long timeout, 
            TimeoutAction timeoutAction)
            throws Exception {
        if (timeout == -1) {
            timeout = infinityTimeoutMilliseconds;
        }
        long t1 = System.currentTimeMillis() + timeout;
        List<Line> responses = new ArrayList<>();
        do{ 
            responses.addAll(receiveResponses());
            if (containsMatch(responses, regex)) {
                return responses;
            }
            Line response = responseQueue.poll(Math.max(1, t1 - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
            if (response != null) {
                responses.add(response);
                continue;
            }
        }
        while (System.currentTimeMillis() < t1);
        // Timeout expired, apply timeout action.
        return timeoutAction.apply(responses);
    }

    public String receiveSingleResponse(String regex) throws Exception {
        List<Line> responses = receiveResponses(regex, getTimeoutMilliseconds(), (r) -> {
            throw new Exception(String.format("\"%s\" read error: No matching responses found.", regex));
        });
        if (responses == null) {
            return null;   
        }
        Pattern pattern = Pattern.compile(regex);
        for (Line line : responses) {
            Matcher matcher = pattern.matcher(line.getLine());
            if (matcher.matches()) {
                return line.getLine();
            }
        }
        return null;
    }

    protected String preProcessCommand(String command) {
        if (removeComments || compressGcode) {
            // See http://linuxcnc.org/docs/2.4/html/gcode_overview.html
            int col = 0;
            boolean insideComment = false;
            boolean decimal = false;
            int trailingZeroes = 0;
            StringBuilder compressedCommand = new StringBuilder();
            for (char ch : command.toCharArray()) {
                col++;
                if (ch == ' ') {
                    // Note, in Gcode, spaces are allowed in the middle of decimals.
                    if (compressGcode) {
                        continue;
                    }
                }
                else if (ch == '(') {
                    trailingZeroes = compressDecimal(trailingZeroes, compressedCommand);
                    decimal = false;
                    insideComment = true;
                    if (removeComments) {
                        continue;
                    }
                }
                else if (ch == ')') {
                    insideComment = false;
                    if (removeComments) {
                        continue;
                    }
                }
                else if (insideComment) {
                    if (removeComments) {
                        continue;
                    }
                }
                else if (ch == ';') {
                    trailingZeroes = compressDecimal(trailingZeroes, compressedCommand);
                    decimal = false;
                    if (removeComments) {
                        break;
                    }
                    else {
                        // Not removed, append as is.
                        compressedCommand.append(command.substring(col-1));
                        break;
                    }
                }
                else if (ch == '.') {
                    decimal = true;
                    trailingZeroes = 1; // treat the dot as a trailing zero character
                }
                else if (ch >= '1' && ch <= '9') {
                    trailingZeroes = 0;
                }
                else if (ch == '0') {
                    if (decimal) {
                        trailingZeroes++;
                    }
                }
                else {
                    trailingZeroes = compressDecimal(trailingZeroes, compressedCommand);
                    decimal = false;
                }
                compressedCommand.append(ch);
            }
            trailingZeroes = compressDecimal(trailingZeroes, compressedCommand);
            decimal = false;
            command = compressedCommand.toString();
            //Logger.trace("Compressed Gcode: {}", command);
        }
        if (backslashEscapedCharactersEnabled) {
            command = unescape(command);
        }
        if (isLoggingGcode()) {

            if (gcodeLogger == null) { 
                File file;
                try {
                    file = Configuration.get().createResourceFile(getClass(), "log", ".g");
                    gcodeLogger = new PrintWriter(file.getAbsolutePath());
                }
                catch (IOException e) {
                    Logger.warn(e, "Cannot open Gcode log");
                }
            }
            if (gcodeLogger != null) {
                gcodeLogger.println(command);
            }
        }
        else {
            closeGcodeLogger();
        }
        return command;
    }

    private int compressDecimal(int trailingZeroes, StringBuilder compressedCommand) {
        if (compressGcode && trailingZeroes > 0) {
            // Cut away trailing zeroes.
            compressedCommand.delete(compressedCommand.length() - trailingZeroes, compressedCommand.length());
        }
        return 0;
    }

    protected class ReaderThread extends Thread {
        @Override
        public void run() {
            while (!disconnectRequested) {
                String receivedLine;
                try {
                    receivedLine = getCommunications().readLine();
                    if (receivedLine == null) {
                        // Line read failed eg. due to socket closure
                        Logger.error("Failed to read gcode response");
                        return;
                    }
                    receivedLine = receivedLine.trim();
                }
                catch (TimeoutException ex) {
                    continue;
                }
                catch (IOException e) {
                    if (disconnectRequested) {
                        Logger.trace("Read error while disconnecting (normal)");
                        return;
                    }
                    else {
                        Logger.error(e, "Read error");
                        return;
                    }
                }
                Line line = new Line(receivedLine);
                Logger.trace("[{}] << {}", getCommunications().getConnectionName(), line);
                // Process the response.
                processResponse(line);
                // Add to the responseQueue for further processing by the caller.
                responseQueue.offer(line);
            }
            Logger.trace("[{}] disconnectRequested, bye-bye.", getCommunications().getConnectionName());
            if (connected) {
                connected = false;
            }
        }
    }

    /**
     * Process a received response immediately. 
     *  
     * @param line
     */
    protected void processResponse(Line line) {
        String regex = getCommand(null, CommandType.COMMAND_CONFIRM_REGEX);
        if (regex != null && line.getLine().matches(regex)) {
            receivedConfirmationsQueue.add(line);
        }
        regex = getCommand(null, CommandType.COMMAND_ERROR_REGEX);
        if (regex != null && line.getLine().matches(regex)) {
            errorResponse = line;
        }
        processPositionReport(line);
    }

    protected boolean processPositionReport(Line line) {
        String regex = getCommand(null, CommandType.POSITION_REPORT_REGEX); 
        if (regex == null) {
            return false;
        }

        if (!line.getLine().matches(regex)) {
            return false;
        }

        Logger.trace("Position report: {}", line);
        ReferenceMachine machine = ((ReferenceMachine) Configuration.get().getMachine());
        Matcher matcher =
                Pattern.compile(regex).matcher(line.getLine());
        matcher.matches();
        AxesLocation position = AxesLocation.zero;
        for (ControllerAxis axis : new AxesLocation(machine).getAxes(this)) {
            try {
                String variable = axis.getLetter(); 
                String s = matcher.group(variable);
                Double d = Double.valueOf(s);
                if (axis.getType() == Type.Rotation) {
                    // Rotation axis is not converted from driver units.
                    position = position.put(new AxesLocation(axis, new Length(d, AxesLocation.getUnits())));
                }
                else {
                    position = position.put(new AxesLocation(axis, new Length(d, getUnits())));
                }
            }
            catch (IllegalArgumentException e) {
                // Axis is not present in pattern. That's a warning, but might not be supported by controller, so we let it go. 
                Logger.warn("Axis {} letter {} missing in POSITION_REPORT_REGEX groups.", axis.getName(), axis.getLetter());
            }
            catch (Exception e) {
                Logger.warn("Error processing position report for axis {}: {}", axis.getName(), e);
            }
        }
        // Store the latest momentary position.
        reportedLocationsQueue.add(position);

        if (motionPending) {
            Logger.warn("Position report cannot be processed when motion might still be pending. Missing Machine Coordination on Actuators?", 
                    position);
        }
        else {
            // Store the actual driver location. This is used to re-sync OpenPnP to the actual controller 
            // location, when its axes might have moved/homed etc. behind its back. 
            position.setToDriverCoordinates(this);
        }
        return true;
    }

    static protected String substituteVariable(String command, String name, Object value) {
        return TextUtils.substituteVar(command, name, value);
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
                new PropertySheetWizardAdapter(new GcodeDriverSettings(this), Translations.getStringOrDefault(
                        "GCodeDriver.GCodeDriverSettings.title", "Driver Settings")),
                new PropertySheetWizardAdapter(new GcodeDriverGcodes(this), Translations.getStringOrDefault(
                        "GCodeDriver.GCode.title", "Gcode")),
                new PropertySheetWizardAdapter(new GcodeDriverConsole(this), Translations.getStringOrDefault(
                        "GCodeDriver.Console.title", "Console")),
        };
    }

    @Override
    public LengthUnit getUnits() {
        return units;
    }

    public void setUnits(LengthUnit units) {
        this.units = units;
    }

    public int getMaxFeedRate() {
        return maxFeedRate;
    }

    public void setMaxFeedRate(int maxFeedRate) {
        Object oldValue = this.maxFeedRate;
        this.maxFeedRate = maxFeedRate;
        firePropertyChange("maxFeedRate", oldValue, maxFeedRate);
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

    public int getDollarWaitTimeMilliseconds() {
        return dollarWaitTimeMilliseconds;
    }

    public void setDollarWaitTimeMilliseconds(int dollarWaitTimeMilliseconds) {
        this.dollarWaitTimeMilliseconds = dollarWaitTimeMilliseconds;
    }

    public boolean isBackslashEscapedCharactersEnabled() {
        return backslashEscapedCharactersEnabled;
    }

    public void setBackslashEscapedCharactersEnabled(boolean backslashEscapedCharactersEnabled) {
        this.backslashEscapedCharactersEnabled = backslashEscapedCharactersEnabled;
    }

    public boolean isRemoveComments() {
        return removeComments;
    }

    public void setRemoveComments(boolean removeComments) {
        Object oldValue = this.removeComments;
        this.removeComments = removeComments;
        firePropertyChange("removeComments", oldValue, removeComments);
    }

    public boolean isCompressGcode() {
        return compressGcode;
    }

    public void setCompressGcode(boolean compressGcode) {
        Object oldValue = this.compressGcode;
        this.compressGcode = compressGcode;
        firePropertyChange("compressGcode", oldValue, compressGcode);
    }

    public boolean isUsingLetterVariables() {
        return usingLetterVariables;
    }

    public void setUsingLetterVariables(boolean usingLetterVariables) {
        Object oldValue = this.usingLetterVariables;
        this.usingLetterVariables = usingLetterVariables;
        firePropertyChange("usingLetterVariables", oldValue, usingLetterVariables);
    }

    @Override
    public boolean isSupportingPreMove() {
        return supportingPreMove;
    }

    public void setSupportingPreMove(boolean supportingPreMove) {
        Object oldValue = this.supportingPreMove;
        this.supportingPreMove = supportingPreMove;
        firePropertyChange("supportingPreMove", oldValue, supportingPreMove);
    }

    public boolean isLoggingGcode() {
        return loggingGcode;
    }

    public void setLoggingGcode(boolean loggingGcode) {
        if (this.loggingGcode != loggingGcode) {
            this.loggingGcode = loggingGcode;
            closeGcodeLogger();
        }
    }

    public String getDetectedFirmware() {
        return detectedFirmware;
    }

    public void setDetectedFirmware(String detectedFirmware) {
        Object oldValue = this.detectedFirmware;
        this.detectedFirmware = detectedFirmware;
        firePropertyChange("detectedFirmware", oldValue, detectedFirmware);
        firePropertyChange("firmwareConfiguration", null, getFirmwareConfiguration());
    }

    public String getReportedAxes() {
        return reportedAxes;
    }

    public void setReportedAxes(String reportedAxes) {
        Object oldValue = this.reportedAxes;
        this.reportedAxes = reportedAxes;
        firePropertyChange("reportedAxes", oldValue, reportedAxes);
        firePropertyChange("firmwareConfiguration", null, getFirmwareConfiguration());
    }

    public List<String> getReportedAxesLetters() {
        List<String> reportedLetters = new ArrayList<>();
        if (getReportedAxes() == null) {
            return reportedLetters;
        }
        Pattern p = Pattern.compile("(?<letter>[A-Z]):-?\\d+.\\d+");
        Matcher m = p.matcher(getReportedAxes());
        while (m.find()) {
            String letter = m.group("letter");
            if (!reportedLetters.contains(letter) // No duplicates.
                    && (!letter.equals("E") || reportedLetters.contains("A"))) { // Not E, if solo.
                reportedLetters.add(letter);
            }
        }
        return reportedLetters;
    }

    public String getConfiguredAxes() {
        return configuredAxes;
    }

    public void setConfiguredAxes(String configuredAxes) {
        Object oldValue = this.configuredAxes;
        this.configuredAxes = configuredAxes;
        firePropertyChange("configuredAxes", oldValue, configuredAxes);
        firePropertyChange("firmwareConfiguration", null, getFirmwareConfiguration());
    }

    public String getFirmwareConfiguration() {
        return (detectedFirmware != null ? detectedFirmware : "")+"\n\n"
                +(reportedAxes != null ? reportedAxes : "")+"\n\n"
                +(configuredAxes != null ? configuredAxes : "");
    }

    public void setFirmwareConfiguration(String configuredAxes) {
    }

    protected void closeGcodeLogger() {
        if (gcodeLogger != null) {
            gcodeLogger.close();
            gcodeLogger = null;
        }
    }

    /**
     * Detect the firmware running on a controller using the M115 command. Also discover axes with M114. 
     * 
     * We want to do this very early in the machine setup, so the machine may not even be ready to be enabled.
     * Instead just connect/disconnect this single driver.
     * 
     * @param preserveOldValue preserve the old value if the detection fails.  
     * 
     * @throws Exception
     */
    public void detectFirmware(boolean preserveOldValue) throws Exception {
        if (!preserveOldValue) {
            setDetectedFirmware(null);
            setReportedAxes(null);
            setConfiguredAxes(null);
        }
        boolean wasConnected = connected;
        if (!wasConnected) {
            connect();
        }

        try {
            Logger.debug("=== Detecting firmware and position reporting, please ignore any errors and warnings.");
            sendCommand("M115");
            String firmware = receiveSingleResponse("^.*FIRMWARE.*");
            if (firmware != null) {
                setDetectedFirmware(firmware);
            }
            sendCommand("M114");
            String reportedAxes = receiveSingleResponse(".*[XYZABCDEUVW]:-?\\d+\\.\\d+.*");
            if (reportedAxes != null) {
                if (firmware != null) {
                    try {
                        if (getFirmwareProperty("FIRMWARE_NAME", "").contains("Duet")) {
                            sendCommand("M584");
                            String axisConfig = receiveSingleResponse("^Driver assignments:.*");
                            if (axisConfig != null) {
                                setConfiguredAxes(axisConfig);
                            }
                        }
                        else {
                            setConfiguredAxes(null);
                        }
                    }
                    catch (Exception e) {
                        // ignore
                    }
                }
                setReportedAxes(reportedAxes);
            }
            Logger.debug("=== End detecting firmware and position reporting.");
        }
        finally {
            if (!wasConnected) {
                disconnect();
            }
        }
    }

    public String getFirmwareProperty(String name, String defaultValue) {
        if (detectedFirmware == null) {
            return defaultValue;
        }
        Pattern pattern = Pattern.compile("([A-Za-z0-9\\_\\-]+):");
        Matcher matcher = pattern.matcher(detectedFirmware);
        while (matcher.find()) {
            if (name.equals(matcher.group(1))) {
                String value;
                int pos = matcher.end();
                if (matcher.find()) {
                    if (matcher.start() <= pos) {
                        // Likely an illegal value with ':' in it, like an URL.
                        if (matcher.find()) {
                            value = detectedFirmware.substring(pos, matcher.start()-1);
                        }
                        else {
                            value = detectedFirmware.substring(pos);
                        }
                    }
                    else {
                        value = detectedFirmware.substring(pos, matcher.start()-1);
                    }
                }
                else {
                    value = detectedFirmware.substring(pos);
                }
                value = value.replace("%3A", ":");
                int comma = value.indexOf(",");
                if (comma >= 0) {
                    value = value.substring(0, comma);
                }
                return value.trim();
            }
        }
        return defaultValue;
    }

    /**
     * @return true if this is a true Gcode speaking driver/controller rather than some other text protocol. 
     * In the absence of a detected firmware, the heuristic is simply to look for a G90 command.
     */
    public boolean isSpeakingGcode() {
        if (getDetectedFirmware() != null) {
            return true;
        }
        else {
            String command = getCommand(null, CommandType.CONNECT_COMMAND);
            return command != null && command.contains("G90");
        }
    }

    @Override
    public void findIssues(Solutions solutions) {
        super.findIssues(solutions);
        new GcodeDriverSolutions(this).findIssues(solutions);
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
                axis.setBacklashCompensationMethod(backlashOffsetX != 0 ?
                        BacklashCompensationMethod.OneSidedPositioning 
                        : BacklashCompensationMethod.None);
                axis.setBacklashOffset(new Length(backlashOffsetX, getUnits()));
                axis.setBacklashSpeedFactor(backlashFeedRateFactor);
                if (head.isSoftLimitsEnabled()) {
                    axis.setSoftLimitLow(minLocation.getLengthX());
                    axis.setSoftLimitLowEnabled(true);
                    axis.setSoftLimitHigh(maxLocation.getLengthX());
                    axis.setSoftLimitHighEnabled(true);
                }
                break;
            case Y:
                axis.setBacklashCompensationMethod(backlashOffsetY != 0 ?
                        BacklashCompensationMethod.OneSidedPositioning 
                        : BacklashCompensationMethod.None);
                axis.setBacklashOffset(new Length(backlashOffsetY, getUnits()));
                axis.setBacklashSpeedFactor(backlashFeedRateFactor);
                if (head.isSoftLimitsEnabled()) {
                    axis.setSoftLimitLow(minLocation.getLengthY());
                    axis.setSoftLimitLowEnabled(true);
                    axis.setSoftLimitHigh(maxLocation.getLengthY());
                    axis.setSoftLimitHighEnabled(true);
                }
                break;
            case Z:
                axis.setBacklashCompensationMethod(backlashOffsetZ != 0 ?
                        BacklashCompensationMethod.OneSidedPositioning 
                        : BacklashCompensationMethod.None);
                axis.setBacklashOffset(new Length(backlashOffsetZ, getUnits()));
                axis.setBacklashSpeedFactor(backlashFeedRateFactor);
                break;
            case Rotation:
                axis.setBacklashCompensationMethod(backlashOffsetR != 0 ?
                        BacklashCompensationMethod.OneSidedPositioning 
                        : BacklashCompensationMethod.None);
                axis.setBacklashOffset(new Length(backlashOffsetR, getUnits()));
                axis.setBacklashSpeedFactor(backlashFeedRateFactor);
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
    public void migrateDriver(Machine machine) throws Exception {
        machine.addDriver(this);
        if (machine instanceof ReferenceMachine) {
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
                    ReferenceControllerAxis controllerAxis = migrateAxis((ReferenceMachine) machine, legacyAxis);
                    AbstractTransformedAxis transformedAxis = null;
                    AbstractAxis axis = controllerAxis;
                    if (legacyAxis.transform != null) {
                        transformedAxis = migrateAxis((ReferenceMachine) machine, controllerAxis, legacyAxis.transform);
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
                            assignCameraVirtualAxes((ReferenceMachine) machine, hm);
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
                else {
                    ((AbstractHead)head).setVisualHomingMethod(VisualHomingMethod.None);
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
            if (hm instanceof ReferenceNozzle) {
                ((ReferenceNozzle)hm).migrateSafeZ();
            }
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
