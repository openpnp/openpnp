/*
 * Copyright (C) 2020 <mark@makr.zone>
 * inspired and based on work
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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.driver.GcodeDriver.Command;
import org.openpnp.machine.reference.driver.GcodeDriver.CommandType;
import org.openpnp.machine.reference.driver.SerialPortCommunications.FlowControl;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Driver.MotionControlType;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.HyphenStyle;
import org.simpleframework.xml.stream.Style;

/**
 * This helper class implements the Troubleshooting for the GcodeDriver and GcodeAsyncDriver. 
 * The idea is not to pollute the driver implementations themselves.
 *
 */
class GcodeDriverSolutions implements Solutions.Subject {
    private final GcodeDriver gcodeDriver;

    GcodeDriverSolutions(GcodeDriver gcodeDriver) {
        this.gcodeDriver = gcodeDriver;
    }

    @Override
    public void findIssues(List<Solutions.Issue> issues) {
        ReferenceMachine machine = (ReferenceMachine) Configuration.get().getMachine();
        boolean hasAxes = !gcodeDriver.getAxisVariables(machine).isEmpty();
        if (!(gcodeDriver instanceof GcodeAsyncDriver)) {
            Solutions.Issue issue = new Solutions.Issue(
                    gcodeDriver, 
                    "Use the GcodeAsyncDriver for advanced features. Accept or Dismiss to continue.", 
                    "Convert to GcodeAsyncDriver.", 
                    Severity.Fundamental,
                    "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver") {

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (confirmStateChange(state)) {
                        if (state == Solutions.State.Solved) {
                            convertToAsync(gcodeDriver);
                        }
                        else if (getState() == Solutions.State.Solved) {
                            // Place the old one back (from the captured gcodeDriver).
                            replaceDriver(gcodeDriver);
                        }
                        super.setState(state);
                    }
                }
            };
            issues.add(issue);
            if (!machine.isSolutionsIssueDismissed(issue)) {
                return; // No further troubleshooting until this is decided.
            }
        }

        Integer firmwareAxesCount = null;
        Integer firmwarePrimaryAxesCount = null;
        boolean smoothie = false;
        boolean grblSyntax = false;
        boolean tinyG = false;
        if (machine.isEnabled() && gcodeDriver.isSpeakingGcode()) {
            try {
                gcodeDriver.detectFirmware(true);
            }
            catch (Exception e) {
                Logger.warn(gcodeDriver.getName()+" failure to detect firmware", e);
            }
        }
        if (gcodeDriver.getDetectedFirmware() == null) {
            issues.add(new Solutions.Issue(
                    gcodeDriver, 
                    "Firmware was not dected ("+
                            (machine.isEnabled() ? 
                                    (gcodeDriver.isSpeakingGcode() ? "failure, check log" : "controller may not speak Gcode") 
                                    : "machine is disabled")+"). The M115 command must be supported by the controller.", 
                            "Retry the detection by connecting to the controller.", 
                            Severity.Fundamental,
                    "https://www.reprap.org/wiki/G-code#M115:_Get_Firmware_Version_and_Capabilities") {

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (confirmStateChange(state)) {
                        if (state == Solutions.State.Solved) {
                            gcodeDriver.detectFirmware(false);
                        }
                        super.setState(state);
                    }
                }
            });
        }
        else {
            if (gcodeDriver.getFirmwareProperty("FIRMWARE_NAME", "").contains("Smoothieware")) {
                smoothie = true;
                grblSyntax = (gcodeDriver.getFirmwareProperty("X-GRBL_MODE", "").contentEquals("1"));
                firmwareAxesCount = Integer.valueOf(gcodeDriver.getFirmwareProperty("X-AXES", "0"));
                firmwarePrimaryAxesCount = Integer.valueOf(gcodeDriver.getFirmwareProperty("X-PAXES", "3"));
                if (firmwarePrimaryAxesCount == firmwareAxesCount) {
                    // OK.
                }
                else {
                    issues.add(new Solutions.PlainIssue(
                            gcodeDriver, 
                            "There is a better Smoothieware firmware available. "+gcodeDriver.getDetectedFirmware(), 
                            "Please upgrade to the special PnP version. See info link.", 
                            Severity.Error, 
                            "https://github.com/openpnp/openpnp/wiki/Motion-Controller-Firmwares#smoothieware"));
                }
            }
            else if (gcodeDriver.getDetectedFirmware().contains("Duet 3")) {
                issues.add(new Solutions.PlainIssue(
                        gcodeDriver, 
                        "The Duet3D firmware is being improved for OpenPnP. "+gcodeDriver.getDetectedFirmware(), 
                        "Follow the progress on the linked web page.", 
                        Severity.Information, 
                        "https://github.com/openpnp/openpnp/wiki/Motion-Controller-Firmwares#duet"));
            }
            else if (gcodeDriver.getDetectedFirmware().contains("Marlin")) {
                firmwareAxesCount = Integer.valueOf(gcodeDriver.getFirmwareProperty("AXIS_COUNT", "0"));
                if (firmwareAxesCount > 3) { 
                    firmwarePrimaryAxesCount = firmwareAxesCount;
                }
                else {
                    issues.add(new Solutions.PlainIssue(
                            gcodeDriver, 
                            "There is a better Marlin firmware available. "+gcodeDriver.getDetectedFirmware(), 
                            "Please upgrade to the special PnP version. See info link.", 
                            Severity.Error, 
                            "https://github.com/openpnp/openpnp/wiki/Motion-Controller-Firmwares#marlin-20"));
                }
            }
            else if (gcodeDriver.getDetectedFirmware().contains("TinyG")) {
                // Having a response already means we have a new firmware.
                tinyG = true;
            }
            else if (gcodeDriver.getDetectedFirmware().contains("GcodeServer")) {
                // Built-in.
            }

            else { 
                issues.add(new Solutions.PlainIssue(
                        gcodeDriver, 
                        "Unknown firmware. "+gcodeDriver.getDetectedFirmware(), 
                        "Check out firmwares known to be well supported. See info link.", 
                        Severity.Warning, 
                        "https://github.com/openpnp/openpnp/wiki/Motion-Controller-Firmwares"));
            }
        }

        if (gcodeDriver instanceof GcodeAsyncDriver) {
            boolean locationConfirmation = ((GcodeAsyncDriver)gcodeDriver).isReportedLocationConfirmation();
            if (hasAxes ^ locationConfirmation) {
                issues.add(new Solutions.Issue(
                        gcodeDriver, 
                        (hasAxes ? 
                                "Switch to Location Confirmation for full asynchronous operation and extra features in homing, contact probing, etc."
                                :"Switch to Confirmation Flow Control (controller has no axes)."),
                        (hasAxes ? 
                                "Enable Location Confirmation, Disable Confirmation Flow Control."
                                :"Disable Location Confirmation, Enable Confirmation Flow Control."), 
                        Severity.Suggestion,
                        "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#advanced-settings") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        if (confirmStateChange(state)) {
                            ((GcodeAsyncDriver) gcodeDriver)
                            .setReportedLocationConfirmation(hasAxes ^ !(state == Solutions.State.Solved));
                            ((GcodeAsyncDriver) gcodeDriver)
                            .setConfirmationFlowControl(hasAxes ^ (state == Solutions.State.Solved));
                            super.setState(state);
                        }
                    }
                });
            }
        }

        if (hasAxes) { 
            if (gcodeDriver.isSupportingPreMove()) {
                issues.add(new Solutions.Issue(
                        gcodeDriver, 
                        "Disallow Pre-Move Commands for advanced features. Accept or Dismiss to continue.", 
                        "Disable Allow Letter Pre-Move Commands.", 
                        Severity.Fundamental,
                        "https://github.com/openpnp/openpnp/wiki/Advanced-Motion-Control#migration-from-a-previous-version") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        if (confirmStateChange(state)) {
                            gcodeDriver.setSupportingPreMove(!(state == Solutions.State.Solved));
                            super.setState(state);
                        }
                    }
                });
            }
            else if (!gcodeDriver.isUsingLetterVariables()) {
                issues.add(new Solutions.Issue(
                        gcodeDriver, 
                        "Use Axis Letter Variables for simpler use and advanced features.", 
                        "Enable Letter Variables.", 
                        Severity.Fundamental,
                        "https://github.com/openpnp/openpnp/wiki/Advanced-Motion-Control#migration-from-a-previous-version") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        if (confirmStateChange(state)) {
                            gcodeDriver.setUsingLetterVariables((state == Solutions.State.Solved));
                            super.setState(state);
                        }
                    }
                });
            }

            final MotionControlType oldMotionControlType = gcodeDriver.getMotionControlType();
            final MotionControlType newMotionControlType = tinyG ? 
                    MotionControlType.SimpleSCurve : MotionControlType.ModeratedConstantAcceleration;
            if (gcodeDriver.getMotionControlType().isUnpredictable() 
                    || (tinyG && newMotionControlType != oldMotionControlType)) {
                issues.add(new Solutions.Issue(
                        gcodeDriver, 
                        (tinyG ? "Choose "+newMotionControlType.name()+" for proper TinyG operation." : 
                                "Choose an advanced Motion Control Type for your controller type."), 
                        "Set to "+newMotionControlType.name()+".", 
                        (tinyG ? Severity.Error : Severity.Suggestion),
                        "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#gcodedriver-new-settings") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        if (confirmStateChange(state)) {
                            gcodeDriver.setMotionControlType((state == Solutions.State.Solved) ? 
                                    newMotionControlType : oldMotionControlType);
                            super.setState(state);
                        }
                    }
                });
            }
            else if (gcodeDriver.getMaxFeedRate() > 0) {
                issues.add(new Solutions.Issue(
                        gcodeDriver, 
                        "Axis velocity limited by driver Maximum Feed Rate. ", 
                        "Remove driver Maximum Feed Rate.", 
                        Severity.Suggestion,
                        "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#gcodedriver-new-settings") {
                    final int oldMaxFeedRate = gcodeDriver.getMaxFeedRate();

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        if (confirmStateChange(state)) {
                            gcodeDriver.setMaxFeedRate((state == Solutions.State.Solved) ? 0 : oldMaxFeedRate);
                            super.setState(state);
                        }
                    }
                });
            }
        }

        if (gcodeDriver.isSpeakingGcode() && !gcodeDriver.isCompressGcode()) {
            issues.add(new Solutions.Issue(
                    gcodeDriver, 
                    "Compress Gcode for superior communications speed.", 
                    "Enable Compress Gcode.", 
                    Severity.Suggestion,
                    "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#gcodedriver-new-settings") {

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (confirmStateChange(state)) {
                        gcodeDriver.setCompressGcode((state == Solutions.State.Solved));
                        super.setState(state);
                    }
                }
            });
        }
        if (gcodeDriver.isSpeakingGcode() && !gcodeDriver.isRemoveComments()) {
            issues.add(new Solutions.Issue(
                    gcodeDriver, 
                    "Remove Gcode comments for superior communications speed.", 
                    "Enable Remove Comments.", 
                    Severity.Suggestion,
                    "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#gcodedriver-new-settings") {

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (confirmStateChange(state)) {
                        gcodeDriver.setRemoveComments((state == Solutions.State.Solved));
                        super.setState(state);
                    }
                }
            });
        }

        if (gcodeDriver.communicationsType.equals("serial") && gcodeDriver.serial != null) {
            final FlowControl oldFlowControl = gcodeDriver.serial.getFlowControl();
            final FlowControl newFlowControl = FlowControl.RtsCts; 
            if (oldFlowControl != newFlowControl) {
                issues.add(new Solutions.Issue(
                        gcodeDriver, 
                        "Serial Port Flow Control recommended.", 
                        "Set "+newFlowControl.name()+" Flow Control on Serial Port.", 
                        gcodeDriver.serial.getFlowControl() == FlowControl.Off ? Severity.Warning : Severity.Suggestion,
                        "https://en.wikipedia.org/wiki/Flow_control_(data)#Hardware_flow_control") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        if (confirmStateChange(state)) {
                            gcodeDriver.serial.setFlowControl((state == Solutions.State.Solved) ? 
                                    newFlowControl : oldFlowControl);
                            super.setState(state);
                        }
                    }
                });
            }
        }
        if (gcodeDriver.isConnectionKeepAlive()) {
            issues.add(new Solutions.Issue(
                    gcodeDriver, 
                    "Use Keep-Alive only when necessary. It may cause hard to diagnose problems.", 
                    "Disable Connection Keep-Alive.", 
                    Severity.Warning,
                    null) {

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (confirmStateChange(state)) {
                        gcodeDriver.setConnectionKeepAlive(!(state == Solutions.State.Solved));
                        super.setState(state);
                    }
                }
            });
        }

        boolean lettersOk = gcodeDriver.isUsingLetterVariables();
        for (ControllerAxis axis : new AxesLocation(machine)
                .drivenBy(gcodeDriver).getControllerAxes()) {
            // Note: some of the axis solutions are handled in the axes themselves. 
            if (axis.getLetter().isEmpty()) {
                lettersOk = false;
            }
            else if (axis instanceof ReferenceControllerAxis) {
                if (firmwarePrimaryAxesCount != null && firmwarePrimaryAxesCount == firmwareAxesCount) { 
                    // Check rotation axes have the linear switch set.
                    if (axis.isRotationalOnController()) {
                        final boolean oldInvertLinearRotational = ((ReferenceControllerAxis) axis).isInvertLinearRotational();
                        issues.add(new Solutions.Issue(
                                axis, 
                                "Axis should be treated as a linear for detected firmware (all-primary axes mode).", 
                                (oldInvertLinearRotational ? "Disable" : "Enable")+" Switch Linear ↔ Rotational.", 
                                Severity.Error,
                                "https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings") {

                            @Override
                            public void setState(Solutions.State state) throws Exception {
                                if (confirmStateChange(state)) {
                                    ((ReferenceControllerAxis) axis).setInvertLinearRotational(
                                            (state == Solutions.State.Solved) ^ oldInvertLinearRotational);
                                    super.setState(state);
                                }
                            }
                        });
                    }
                } 
                else if("XYZABC".contains(axis.getLetter())) {
                    boolean rotational = "ABC".contains(axis.getLetter());
                    if (rotational ^ axis.isRotationalOnController()) {
                        final boolean oldInvertLinearRotational = ((ReferenceControllerAxis) axis).isInvertLinearRotational();
                        issues.add(new Solutions.Issue(
                                axis, 
                                "Axis should be treated as "+(rotational ? "rotational" : "linear")+" according to its letter.", 
                                (oldInvertLinearRotational ? "Disable" : "Enable")+" Switch Linear ↔ Rotational.", 
                                Severity.Warning,
                                "https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings-rotational-axis") {

                            @Override
                            public void setState(Solutions.State state) throws Exception {
                                if (confirmStateChange(state)) {
                                    ((ReferenceControllerAxis) axis).setInvertLinearRotational(
                                            (state == Solutions.State.Solved) ^ oldInvertLinearRotational);
                                    super.setState(state);
                                }
                            }
                        });
                    }
                }
            }
        }
        if (lettersOk) { 
            for (CommandType commandType : gcodeDriver.isSpeakingGcode() ?
                    CommandType.values() 
                    : new CommandType[] { CommandType.CONNECT_COMMAND }) {
                String command = gcodeDriver.getCommand(null, commandType);
                String commandBuilt = null;
                boolean disallowHeadMountables = false;
                switch (commandType) {
                    case CONNECT_COMMAND:
                        if (command == null) {
                            commandBuilt = 
                                    "G21 ; Set millimeters mode \n"
                                            +"G90 ; Set absolute positioning mode";
                            if (tinyG) {
                                commandBuilt = 
                                        "$ex=2\n" // RtsCts
                                        +"$sv=0\n" // Non-verbose
                                        +commandBuilt;
                            }
                        }
                        break;
                    case COMMAND_CONFIRM_REGEX:
                        if (tinyG) {
                            commandBuilt = "^tinyg .* ok.*";
                        }
                        else {
                            commandBuilt = "^ok.*";
                        }
                        break;
                    case COMMAND_ERROR_REGEX:
                        if (tinyG) {
                            commandBuilt = "^tinyg .* err:.*";
                        }
                        else {
                            //commandBuilt = "^!!*";
                        }
                        break;
                    case HOME_COMMAND:
                        if (command == null) {
                            if (grblSyntax) {
                                commandBuilt = "$H ; Home all axes";
                            }
                            else if (tinyG) {
                                commandBuilt = "G28.2 ";
                                for (String variable : gcodeDriver.getAxisVariables(machine)) {
                                    commandBuilt += variable+"0 "; // In TinyG you need to indicate the axis and only 0 is possible. 
                                }
                                commandBuilt += "; Home all axes";
                            }
                            else {
                                commandBuilt = "G28 ; Home all axes";
                            }
                        }
                        break;
                    case MOVE_TO_COMMAND:
                        if (hasAxes) {
                            if (tinyG) { 
                                // Apply jerk limits per axis. 
                                commandBuilt = "M201.3 ";
                                for (String variable : gcodeDriver.getAxisVariables(machine)) {
                                    commandBuilt += "{"+variable+"Jerk:"+variable+"%.0f} ";
                                }
                                // This needs a new-line: "It is an error to put a G-code from group 1 and a G-code from group 0 on the same line if both of
                                // them use axis words." (RS274/NGC Interpreter - Version 3, §3.4)
                                commandBuilt += "\n";
                            }
                            else {
                                // Apply acceleration limit.
                                commandBuilt = "{Acceleration:M204 S%.2f} ";
                            }
                            commandBuilt += "G1 ";
                            for (String variable : gcodeDriver.getAxisVariables(machine)) {
                                commandBuilt += "{"+variable+":"+variable+"%.4f} ";
                            }
                            commandBuilt += "{FeedRate:F%.2f} ; move to target";
                        }
                        else if (command != null) {
                            commandBuilt = "";
                        }
                        disallowHeadMountables = true;
                        break;
                    case MOVE_TO_COMPLETE_COMMAND:
                        // This is provided even if there are no axes on the driver. M400 may still be useful for actuator coordination.
                        if (command == null 
                        && gcodeDriver.getCommand(null, CommandType.MOVE_TO_COMPLETE_REGEX) == null) {
                            commandBuilt = "M400 ; Wait for moves to complete before returning";
                        }
                        break;
                    case MOVE_TO_COMPLETE_REGEX:
                        if (command != null && tinyG) {
                            // Make it obsolete with the new (detected) firmware.
                            commandBuilt = "";
                        }
                        break;
                    case SET_GLOBAL_OFFSETS_COMMAND:
                        if (hasAxes) {
                            if (tinyG) {
                                commandBuilt = "G28.3 ";
                            }
                            else {
                                commandBuilt = "G92 ";
                            }
                            for (String variable : gcodeDriver.getAxisVariables(machine)) {
                                commandBuilt += "{"+variable+":"+variable+"%.4f} ";
                            }
                            commandBuilt += "; reset coordinates";
                        }
                        else if (command != null) {
                            commandBuilt = "";
                        }
                        break;
                    case POST_VISION_HOME_COMMAND:
                        if (command != null) {
                            commandBuilt = "";
                        }
                        break;
                    case GET_POSITION_COMMAND:
                        if (hasAxes) {
                            commandBuilt = "M114 ; get position"; 
                        }
                        else if (command != null) {
                            commandBuilt = "";
                        }
                        break;
                    case POSITION_REPORT_REGEX:
                        if (hasAxes) {
                            // We need to parse the report in standard Gcode axis order. This might not cover all the controllers, but it's what we can do.
                            final String[] cgodeAxisLetters = new String[] { "X", "Y", "Z", "U", "V", "W", "A", "B", "C", "D", "E" };
                            commandBuilt = "^.*";
                            int axisIndex = 0;
                            int lastAxisIndex = 26;
                            String pattern = "";
                            for (String axisLetter: cgodeAxisLetters) {
                                for (String variable : gcodeDriver.getAxisVariables(machine)) {
                                    if (variable.equals(axisLetter)) {
                                        if (lastAxisIndex < axisIndex-1) {
                                            // Skipped some axes, add a wild.card.
                                            commandBuilt += ".*";
                                        }
                                        commandBuilt += variable+":(?<"+variable+">-?\\d+\\.\\d+) ";
                                        pattern += variable+" ";
                                        lastAxisIndex = axisIndex;
                                    }
                                }
                                axisIndex++;
                            }
                            commandBuilt = commandBuilt.trim();
                            commandBuilt += ".*";
                            if (gcodeDriver.getReportedAxes() != null 
                                    && !gcodeDriver.getReportedAxes().matches(commandBuilt)) {
                                issues.add(new Solutions.PlainIssue(
                                        gcodeDriver, 
                                        "The driver does not report axes in the expected "+pattern+" pattern: "+gcodeDriver.getReportedAxes(), 
                                        (smoothie ? "Check axis letters and make sure use a proper 6-axis configuration without extruders."
                                                : "Check axis letters and make sure the controller is capable to use extra axes (i.e. not extruders)."), 
                                        Severity.Error,
                                        (smoothie ? "http://smoothieware.org/6axis" 
                                                : "https://github.com/openpnp/openpnp/wiki/Motion-Controller-Firmwares")));
                            }
                        }
                        else if (command != null) {
                            commandBuilt = "";
                        }
                        break;
                }
                final String commandSuggested = commandBuilt; 
                if (commandSuggested != null && !commandSuggested.equals(command)) {
                    issues.add(new Solutions.Issue(
                            gcodeDriver, 
                            commandType.name()+(commandSuggested.isEmpty() ? " obsolete." : " suggested.")
                            + (gcodeDriver.isSpeakingGcode() ? "" : " Accept if this is a true Gcode controller."), 
                            (commandSuggested.isEmpty() ? "Delete it." : commandSuggested), 
                            commandSuggested.isEmpty() ? Severity.Warning 
                                    : (gcodeDriver.isSpeakingGcode() ? Severity.Suggestion : Severity.Fundamental),
                            "https://github.com/openpnp/openpnp/wiki/Advanced-Motion-Control#migration-from-a-previous-version") {

                        @Override
                        public void setState(Solutions.State state) throws Exception {
                            if (confirmStateChange(state)) {
                                gcodeDriver.setCommand(null, commandType, (state == Solutions.State.Solved) ? commandSuggested : command);
                                super.setState(state);
                            }
                        }
                    });
                }
                if (disallowHeadMountables) {
                    for (Head head : machine.getHeads()) {
                        for (HeadMountable hm : head.getHeadMountables()) {
                            Command commandHeadMountable = gcodeDriver.getCommand(hm, commandType, false);
                            if (commandHeadMountable != null) {
                                issues.add(new Solutions.Issue(
                                        gcodeDriver, 
                                        hm.getClass().getSimpleName()+" "+hm.getName()+" "+commandType+" obsolete with Letter Variables.", 
                                        "Remove the command.", 
                                        Severity.Error,
                                        "https://github.com/openpnp/openpnp/wiki/Advanced-Motion-Control#migration-from-a-previous-version") {

                                    @Override
                                    public void setState(Solutions.State state) throws Exception {
                                        if (confirmStateChange(state)) {
                                            gcodeDriver.setCommand(hm, commandType, (state == Solutions.State.Solved) ? null : commandHeadMountable.getCommand());
                                            super.setState(state);
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }
    }


    private static Serializer createSerializer() {
        Style style = new HyphenStyle();
        Format format = new Format(style);
        AnnotationStrategy strategy = new AnnotationStrategy();
        Serializer serializer = new Persister(strategy, format);
        return serializer;
    }

    /**
     * Convert an existing GcodeDriver to a GcodeAsyncDriver while keeping all settings and 
     * Axis/Actuator assignments. 
     * 
     * @param gcodeDriver
     * @throws Exception
     */
    public static void convertToAsync(GcodeDriver gcodeDriver) throws Exception {
        // Serialize the GcodeDriver
        Serializer serOut = createSerializer();
        StringWriter sw = new StringWriter();
        serOut.write(gcodeDriver, sw);
        String gcodeDriverSerialized = sw.toString();
        // Patch it.
        gcodeDriverSerialized.replace(
                gcodeDriver.getClass().getCanonicalName(), 
                GcodeAsyncDriver.class.getCanonicalName());
        // De-serialize it.
        Serializer serIn = createSerializer();
        StringReader sr = new StringReader(gcodeDriverSerialized);
        GcodeAsyncDriver asyncDriver = serIn.read(GcodeAsyncDriver.class, sr);
        replaceDriver(asyncDriver);
    }

    /**
     * Replace a driver with the same Id at the same place in the machine driver list.
     * 
     * @param gcodeDriver
     * @throws Exception
     */
    public static void replaceDriver(GcodeDriver gcodeDriver) throws Exception {
        // Disable the machine, so the driver isn't connected.
        Machine machine = Configuration.get().getMachine();
        boolean wasEnabled = machine.isEnabled();
        if (wasEnabled) {
            machine.setEnabled(false);
        }
        // Find the old driver with the same Id.
        List<Driver> list = machine.getDrivers();
        Driver replaced = null;
        int index;
        for (index = 0; index < list.size(); index++) {
            if (list.get(index).getId().equals(gcodeDriver.getId())) {
                replaced = list.get(index);
                machine.removeDriver(replaced);
                break;
            }
        }
        // Add the new one.
        machine.addDriver(gcodeDriver);
        // Permutate it back to the old list place (cumbersome but works).
        for (int p = list.size()-index; p > 1; p--) {
            machine.permutateDriver(gcodeDriver, -1);
        }
        // Replace the driver in the Machine Setup.
        for (ControllerAxis axis : new AxesLocation(machine).getControllerAxes()) {
            if (axis.getDriver() == replaced) {
                axis.setDriver(gcodeDriver);
            }
        }
        for (Actuator actuator : machine.getActuators()) {
            if (actuator.getDriver() == replaced) {
                actuator.setDriver(gcodeDriver);
            }
        }
        for (Head head : machine.getHeads()) {
            for (Actuator actuator : head.getActuators()) {
                if (actuator.getDriver() == replaced) {
                    actuator.setDriver(gcodeDriver);
                }
            }
        }
        // Re-enable the machine.
        if (wasEnabled) {
            machine.setEnabled(true);
        }
    }
}