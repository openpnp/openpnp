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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.driver.AbstractReferenceDriver.CommunicationsType;
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
    public void findIssues(Solutions solutions) {
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
                    if (state == Solutions.State.Solved) {
                        convertToAsync(gcodeDriver);
                    }
                    else if (getState() == Solutions.State.Solved) {
                        // Place the old one back (from the captured gcodeDriver).
                        replaceDriver(gcodeDriver);
                    }
                    super.setState(state);
                }
            };
            solutions.add(issue);
            if (!solutions.isSolutionsIssueDismissed(issue)) {
                return; // No further troubleshooting until this is decided.
            }
        }

        if (machine.isEnabled() && gcodeDriver.isSpeakingGcode()) {
            try {
                gcodeDriver.detectFirmware(true);
            }
            catch (Exception e) {
                Logger.warn(e, gcodeDriver.getName()+" failure to detect firmware");
            }
        }
        Integer firmwareAxesCount = null;
        Integer firmwarePrimaryAxesCount = null;
        boolean isSmoothie = false;
        boolean isGrblSyntax = false;
        boolean isTinyG = false;
        boolean isMarlin = false;
        boolean isDuet = false;
        if (gcodeDriver.getDetectedFirmware() == null) {
            solutions.add(new Solutions.Issue(
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
                    if (state == Solutions.State.Solved) {
                        gcodeDriver.detectFirmware(false);
                    }
                    super.setState(state);
                }
            });
        }
        else {
            if (gcodeDriver.getFirmwareProperty("FIRMWARE_NAME", "").contains("Smoothieware")) {
                isSmoothie = true;
                isGrblSyntax = (gcodeDriver.getFirmwareProperty("X-GRBL_MODE", "").contentEquals("1"));
                firmwareAxesCount = Integer.valueOf(gcodeDriver.getFirmwareProperty("X-AXES", "0"));
                firmwarePrimaryAxesCount = Integer.valueOf(gcodeDriver.getFirmwareProperty("X-PAXES", "3"));
                if (firmwarePrimaryAxesCount == firmwareAxesCount) {
                    // OK.
                }
                else {
                    solutions.add(new Solutions.PlainIssue(
                            gcodeDriver, 
                            "There is a better Smoothieware firmware available. "+gcodeDriver.getDetectedFirmware(), 
                            "Please upgrade to the special PnP version. See info link.", 
                            Severity.Error, 
                            "https://github.com/openpnp/openpnp/wiki/Motion-Controller-Firmwares#smoothieware"));
                }
            }
            else if (gcodeDriver.getFirmwareProperty("FIRMWARE_NAME", "").contains("Duet")) {
                isDuet = true;
                String firmwareVersion = gcodeDriver.getFirmwareProperty("FIRMWARE_VERSION", "0.0");
                Integer major = null;
                Integer minor = null;
                try {
                    Matcher matcher =
                            Pattern.compile("(?<major>-?\\d+)\\.(?<minor>-?\\d+).*").matcher(firmwareVersion);
                    matcher.matches();
                    major = Integer.parseUnsignedInt(matcher.group("major"));
                    minor = Integer.parseUnsignedInt(matcher.group("minor"));
                }
                catch (Exception e) {
                    Logger.warn(e);
                }
                if (major == null || minor == null
                        || major < 3 || (major == 3 && minor < 3)) {
                    solutions.add(new Solutions.PlainIssue(
                            gcodeDriver,
                            "Duet3D firmware was improved for OpenPnP, please use version 3.3beta or newer. Current version is "+firmwareVersion,
                            "Get the new version through the linked web page.",
                            Severity.Error,
                            "https://github.com/openpnp/openpnp/wiki/Motion-Controller-Firmwares#duet"));
                }
                if (gcodeDriver.getConfiguredAxes() != null) {
                    try {
                        Matcher matcher =
                                Pattern.compile(".*\\s(?<axes>-?\\d+)\\saxes\\svisible.*").matcher(gcodeDriver.getConfiguredAxes());
                        matcher.matches();
                        firmwareAxesCount = Integer.parseUnsignedInt(matcher.group("axes"));
                    }
                    catch (NumberFormatException e) {
                        // ignore
                    }
                    if (gcodeDriver.getConfiguredAxes().contains("(r)")) {
                        solutions.add(new Solutions.PlainIssue(
                                gcodeDriver,
                                "Axes should be configured as linear in feedrate calculations on the Duet controller. See the linked web page.",
                                "Use the M584 S0 option in your config.g file.",
                                Severity.Error,
                                "https://duet3d.dozuki.com/Wiki/Gcode#Section_M584_Set_drive_mapping"));
                    }
                    else {
                        firmwarePrimaryAxesCount = firmwareAxesCount;
                    }
                }
            }
            else if (gcodeDriver.getDetectedFirmware().contains("Marlin")) {
                isMarlin = true;
                firmwareAxesCount = Integer.valueOf(gcodeDriver.getFirmwareProperty("AXIS_COUNT", "0"));
                if (firmwareAxesCount > 3) { 
                    firmwarePrimaryAxesCount = firmwareAxesCount;
                }
                else {
                    solutions.add(new Solutions.PlainIssue(
                            gcodeDriver, 
                            "There is a better Marlin firmware available. "+gcodeDriver.getDetectedFirmware(), 
                            "Please upgrade to the special PnP version. See info link.", 
                            Severity.Error, 
                            "https://github.com/openpnp/openpnp/wiki/Motion-Controller-Firmwares#marlin-20"));
                }
            }
            else if (gcodeDriver.getDetectedFirmware().contains("TinyG")) {
                // Having a response already means we have a new firmware.
                isTinyG = true;
            }
            else if (gcodeDriver.getDetectedFirmware().contains("GcodeServer")) {
                // Built-in.
            }

            else { 
                solutions.add(new Solutions.PlainIssue(
                        gcodeDriver, 
                        "Unknown firmware. "+gcodeDriver.getDetectedFirmware(), 
                        "Check out firmwares known to be well supported. See info link.", 
                        Severity.Warning, 
                        "https://github.com/openpnp/openpnp/wiki/Motion-Controller-Firmwares"));
            }
        }

        if (gcodeDriver instanceof GcodeAsyncDriver) {
            boolean locationConfirmation = ((GcodeAsyncDriver)gcodeDriver).isReportedLocationConfirmation();
            boolean confirmationFlowControl = ((GcodeAsyncDriver)gcodeDriver).isConfirmationFlowControl();
            boolean locationConfirmationRecommended = hasAxes;
            if (locationConfirmationRecommended != locationConfirmation) {
                solutions.add(new Solutions.Issue(
                        gcodeDriver, 
                        (locationConfirmationRecommended
                                ? (confirmationFlowControl
                                        ? "Location Confirmation recommended for extra features in homing, contact probing, etc."
                                                : "Location Confirmation required when Confirmation Flow Control is off. Supports extra features in homing, contact probing, etc.")
                                        : "Location Confirmation usually not available when no axes present."),
                        (locationConfirmationRecommended ? "Enable Location Confirmation" : "Disable Location Confirmation"),
                        (confirmationFlowControl ? Severity.Suggestion : Severity.Error),
                        "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#advanced-settings") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        ((GcodeAsyncDriver) gcodeDriver)
                        .setReportedLocationConfirmation(locationConfirmation ^ (state == Solutions.State.Solved));
                        super.setState(state);
                    }
                });
            }
            boolean confirmationFlowControlRecommended = isTinyG || ! hasAxes;
            if (confirmationFlowControlRecommended != confirmationFlowControl) {
                solutions.add(new Solutions.Issue(
                        gcodeDriver,
                        (confirmationFlowControl ?
                                "Disable Confirmation Flow Control for full asynchronous operation."
                                : "Enable Confirmation Flow Control" + (hasAxes ? "" : ", controller has no axes") + (isTinyG ? ", TinyG detected" : "") + "."),
                        (confirmationFlowControl ?
                                "Disable Confirmation Flow Control."
                                :"Enable Confirmation Flow Control."),
                        Severity.Suggestion,
                        "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#advanced-settings") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        ((GcodeAsyncDriver) gcodeDriver)
                        .setConfirmationFlowControl(confirmationFlowControl ^ (state == Solutions.State.Solved));
                        super.setState(state);
                    }
                });
            }
        }

        if (hasAxes) { 
            if (gcodeDriver.isSupportingPreMove()) {
                solutions.add(new Solutions.Issue(
                        gcodeDriver, 
                        "Disallow Pre-Move Commands for advanced features. Accept or Dismiss to continue.", 
                        "Disable Allow Letter Pre-Move Commands.", 
                        Severity.Fundamental,
                        "https://github.com/openpnp/openpnp/wiki/Advanced-Motion-Control#migration-from-a-previous-version") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        gcodeDriver.setSupportingPreMove(!(state == Solutions.State.Solved));
                        super.setState(state);
                    }
                });
            }
            else if (!gcodeDriver.isUsingLetterVariables()) {
                solutions.add(new Solutions.Issue(
                        gcodeDriver, 
                        "Use Axis Letter Variables for simpler use and advanced features.", 
                        "Enable Letter Variables.", 
                        Severity.Fundamental,
                        "https://github.com/openpnp/openpnp/wiki/Advanced-Motion-Control#migration-from-a-previous-version") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        gcodeDriver.setUsingLetterVariables((state == Solutions.State.Solved));
                        super.setState(state);
                    }
                });
            }

            final MotionControlType oldMotionControlType = gcodeDriver.getMotionControlType();
            final MotionControlType newMotionControlType = isTinyG ?
                    MotionControlType.SimpleSCurve : MotionControlType.ModeratedConstantAcceleration;
            if (gcodeDriver.getMotionControlType().isUnpredictable() 
                    || (isTinyG && newMotionControlType != oldMotionControlType)) {
                solutions.add(new Solutions.Issue(
                        gcodeDriver, 
                        (isTinyG ? "Choose "+newMotionControlType.name()+" for proper TinyG operation." :
                                "Choose an advanced Motion Control Type for your controller type."), 
                        "Set to "+newMotionControlType.name()+".", 
                        (isTinyG ? Severity.Error : Severity.Suggestion),
                        "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#gcodedriver-new-settings") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        gcodeDriver.setMotionControlType((state == Solutions.State.Solved) ? 
                                newMotionControlType : oldMotionControlType);
                        super.setState(state);
                    }
                });
            }
            else if (gcodeDriver.getMaxFeedRate() > 0) {
                solutions.add(new Solutions.Issue(
                        gcodeDriver, 
                        "Axis velocity limited by driver Maximum Feed Rate. ", 
                        "Remove driver Maximum Feed Rate.", 
                        Severity.Suggestion,
                        "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#gcodedriver-new-settings") {
                    final int oldMaxFeedRate = gcodeDriver.getMaxFeedRate();

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        gcodeDriver.setMaxFeedRate((state == Solutions.State.Solved) ? 0 : oldMaxFeedRate);
                        super.setState(state);
                    }
                });
            }
        }

        if (gcodeDriver.isSpeakingGcode() && !gcodeDriver.isCompressGcode()) {
            solutions.add(new Solutions.Issue(
                    gcodeDriver, 
                    "Compress Gcode for superior communications speed.", 
                    "Enable Compress Gcode.", 
                    Severity.Suggestion,
                    "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#gcodedriver-new-settings") {

                @Override
                public void setState(Solutions.State state) throws Exception {
                    gcodeDriver.setCompressGcode((state == Solutions.State.Solved));
                    super.setState(state);
                }
            });
        }
        if (gcodeDriver.isSpeakingGcode() && !gcodeDriver.isRemoveComments()) {
            solutions.add(new Solutions.Issue(
                    gcodeDriver, 
                    "Remove Gcode comments for superior communications speed.", 
                    "Enable Remove Comments.", 
                    Severity.Suggestion,
                    "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#gcodedriver-new-settings") {

                @Override
                public void setState(Solutions.State state) throws Exception {
                    gcodeDriver.setRemoveComments((state == Solutions.State.Solved));
                    super.setState(state);
                }
            });
        }

        if (gcodeDriver.communicationsType == CommunicationsType.serial && gcodeDriver.serial != null) {
            final FlowControl oldFlowControl = gcodeDriver.serial.getFlowControl();
            final FlowControl newFlowControl = isTinyG ? FlowControl.XonXoff : FlowControl.RtsCts;
            if (oldFlowControl != newFlowControl) {
                solutions.add(new Solutions.Issue(
                        gcodeDriver, 
                        "Change of serial port Flow Control recommended.",
                        "Set "+newFlowControl.name()+" on serial port.",
                        (gcodeDriver.serial.getFlowControl() == FlowControl.Off || isTinyG ? Severity.Warning : Severity.Suggestion),
                        "https://en.wikipedia.org/wiki/Flow_control_(data)#Hardware_flow_control") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        gcodeDriver.serial.setFlowControl((state == Solutions.State.Solved) ? 
                                newFlowControl : oldFlowControl);
                        super.setState(state);
                    }
                });
            }
        }
        if (gcodeDriver.isConnectionKeepAlive()) {
            solutions.add(new Solutions.Issue(
                    gcodeDriver, 
                    "Use Keep-Alive only when necessary. It may cause hard to diagnose problems.", 
                    "Disable Connection Keep-Alive.", 
                    Severity.Warning,
                    null) {

                @Override
                public void setState(Solutions.State state) throws Exception {
                    gcodeDriver.setConnectionKeepAlive(!(state == Solutions.State.Solved));
                    super.setState(state);
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
                        solutions.add(new Solutions.Issue(
                                axis, 
                                "Axis should be treated as a linear for detected firmware (all-primary axes mode).", 
                                (oldInvertLinearRotational ? "Disable" : "Enable")+" Switch Linear ↔ Rotational.", 
                                Severity.Error,
                                "https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings") {

                            @Override
                            public void setState(Solutions.State state) throws Exception {
                                ((ReferenceControllerAxis) axis).setInvertLinearRotational(
                                        (state == Solutions.State.Solved) ^ oldInvertLinearRotational);
                                super.setState(state);
                            }
                        });
                    }
                } 
                else if("XYZABC".contains(axis.getLetter())) {
                    boolean rotational = "ABC".contains(axis.getLetter());
                    if (rotational ^ axis.isRotationalOnController()) {
                        final boolean oldInvertLinearRotational = ((ReferenceControllerAxis) axis).isInvertLinearRotational();
                        solutions.add(new Solutions.Issue(
                                axis, 
                                "Axis should be treated as "+(rotational ? "rotational" : "linear")+" according to its letter.", 
                                (oldInvertLinearRotational ? "Disable" : "Enable")+" Switch Linear ↔ Rotational.", 
                                Severity.Warning,
                                "https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings-rotational-axis") {

                            @Override
                            public void setState(Solutions.State state) throws Exception {
                                ((ReferenceControllerAxis) axis).setInvertLinearRotational(
                                        (state == Solutions.State.Solved) ^ oldInvertLinearRotational);
                                super.setState(state);
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
                boolean commandModified = false;
                switch (commandType) {
                    case CONNECT_COMMAND:
                        if (command == null) {
                            commandBuilt = 
                                    "G21 ; Set millimeters mode \n"
                                            +"G90 ; Set absolute positioning mode";
                            if (isTinyG) {
                                commandBuilt = 
                                        "$ex=1\n" // XonXoff
                                        +"$sv=0\n" // Non-verbose
                                        +commandBuilt;
                            }
                        }
                        else if (isTinyG) {
                            commandModified = true;
                            if (command.contains("$ex=2")) {
                                commandBuilt = command.replace("$ex=2", "$ex=1");
                            }
                            else if (command.contains("$ex=0")) {
                                commandBuilt = command.replace("$ex=0", "$ex=1");
                            }
                            else if (!command.contains("$ex=1")){
                                commandBuilt = "$ex=1\n"
                                        +command;
                            }
                        }
                        break;
                    case COMMAND_CONFIRM_REGEX:
                        if (isTinyG) {
                            commandBuilt = "^tinyg .* ok.*";
                        }
                        else {
                            commandBuilt = "^ok.*";
                        }
                        break;
                    case COMMAND_ERROR_REGEX:
                        if (isTinyG) {
                            commandBuilt = "^tinyg .* err:.*";
                        }
                        else {
                            //commandBuilt = "^!!*";
                        }
                        break;
                    case HOME_COMMAND:
                        if (command == null) {
                            if (isGrblSyntax) {
                                commandBuilt = "$H ; Home all axes";
                            }
                            else if (isTinyG) {
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
                            if (isTinyG) {
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
                                if (isMarlin) {
                                    // Non-conformant G-code parser, needs newline.
                                    commandBuilt += "\n";
                                }
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
                        if (command != null) {
                            // Make it obsolete with the new (detected) firmware.
                            commandBuilt = "";
                        }
                        break;
                    case SET_GLOBAL_OFFSETS_COMMAND:
                        if (hasAxes) {
                            if (isTinyG) {
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
                                solutions.add(new Solutions.PlainIssue(
                                        gcodeDriver, 
                                        "The driver does not report axes in the expected "+pattern+" pattern: "+gcodeDriver.getReportedAxes(), 
                                        (isSmoothie ? "Check axis letters and make sure use a proper 6-axis configuration without extruders."
                                                : "Check axis letters and make sure the controller is capable to use extra axes (i.e. not extruders)."), 
                                        Severity.Error,
                                        (isSmoothie ? "http://smoothieware.org/6axis"
                                                : "https://github.com/openpnp/openpnp/wiki/Motion-Controller-Firmwares")));
                            }
                        }
                        else if (command != null) {
                            commandBuilt = "";
                        }
                        break;
                }
                suggestGcodeCommand(gcodeDriver, null, solutions, commandType, commandBuilt, commandModified,
                        disallowHeadMountables);
            }
        }

        for (Command command : gcodeDriver.commands) {
            if (command.type != CommandType.ACTUATOR_READ_WITH_DOUBLE_COMMAND) {
                continue;
            }

            Command readCommand = gcodeDriver.getExactCommand(
                    command.headMountableId,
                    CommandType.ACTUATOR_READ_COMMAND
            );

            if (readCommand == null) {
                continue;
            }

            String headMountable;
            if (command.headMountableId == null) {
                headMountable = "the default head mountable";
            } else if (command.headMountableId.equals("*")) {
                headMountable = "the catch all head mountable";
            } else {
                headMountable = "head mountable id " + command.headMountableId;
            }
            solutions.add(new Solutions.Issue(
                    gcodeDriver,
                    "Both ACTUATOR_READ_COMMAND and ACTUATOR_READ_WITH_DOUBLE_COMMAND are set for " +
                            headMountable +
                            " but the latter is deprecated",
                    "Accept to replace ACTUATOR_READ_COMMAND with ACTUATOR_READ_WITH_DOUBLE_COMMAND. Dismiss to remove read with double.",
                    Severity.Suggestion,
                    "https://github.com/openpnp/openpnp/wiki/GcodeDriver%3A-Command-Reference#actuator_read_command"
            ) {
                @Override
                public void setState(Solutions.State state) throws Exception {
                    super.setState(state);

                    if (state.equals(Solutions.State.Solved)) {
                        gcodeDriver.commands.remove(readCommand);
                        command.type = CommandType.ACTUATOR_READ_COMMAND;
                    } else if (state.equals(Solutions.State.Dismissed)) {
                        gcodeDriver.commands.remove(command);
                    }
                }
            });
        }
    }

    /**
     * Add a solution for the given gcodeDriver to the issues, to suggest the given suggestedCommand instead of the currentCommand.
     *
     * @param gcodeDriver
     * @param solutions
     * @param commandType
     * @param currentCommand
     * @param suggestedCommand
     * @param disallowHeadMountables Set true to disallow the commandType on HeadMountables. This is used when switching to axis letter variables.
     */
    public static void suggestGcodeCommand(GcodeDriver gcodeDriver, HeadMountable headMountable, Solutions solutions,
            CommandType commandType, String suggestedCommand, boolean commandModified,
            boolean disallowHeadMountables) {
        String currentCommand = gcodeDriver.getCommand(headMountable, commandType);
        if (suggestedCommand != null && !suggestedCommand.equals(currentCommand)) {
            solutions.add(new Solutions.Issue(
                    (headMountable != null ? headMountable : gcodeDriver),
                    commandType.name()+(suggestedCommand.isEmpty() ? " obsolete." : (commandModified ? " modification suggested." : " suggested."))
                    + (gcodeDriver.isSpeakingGcode() ? "" : " Accept if this is a true Gcode controller."),
                    (suggestedCommand.isEmpty() ? "Delete it." : suggestedCommand),
                    suggestedCommand.isEmpty() ? Severity.Warning
                            : (gcodeDriver.isSpeakingGcode() ? Severity.Suggestion : Severity.Fundamental),
                    "https://github.com/openpnp/openpnp/wiki/Advanced-Motion-Control#migration-from-a-previous-version") {

                @Override
                public void setState(Solutions.State state) throws Exception {
                    gcodeDriver.setCommand(headMountable, commandType, (state == Solutions.State.Solved) ? suggestedCommand : currentCommand);
                    super.setState(state);
                }
            });
        }
        if (disallowHeadMountables) {
            for (Head head : Configuration.get().getMachine().getHeads()) {
                for (HeadMountable hm : head.getHeadMountables()) {
                    Command commandHeadMountable = gcodeDriver.getCommand(hm, commandType, false);
                    if (commandHeadMountable != null) {
                        solutions.add(new Solutions.Issue(
                                gcodeDriver,
                                hm.getClass().getSimpleName()+" "+hm.getName()+" "+commandType+" obsolete with Letter Variables.",
                                "Remove the command.",
                                Severity.Error,
                                "https://github.com/openpnp/openpnp/wiki/Advanced-Motion-Control#migration-from-a-previous-version") {

                            @Override
                            public void setState(Solutions.State state) throws Exception {
                                gcodeDriver.setCommand(hm, commandType, (state == Solutions.State.Solved) ? null : commandHeadMountable.getCommand());
                                super.setState(state);
                            }
                        });
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
        // Triple the timeout as asynchronously executed move sequences can be longer than single moves.
        asyncDriver.setTimeoutMilliseconds(asyncDriver.getTimeoutMilliseconds()*3);
        replaceDriver(asyncDriver);
    }

    /**
     * Convert an existing NullDriver to a GcodeAsyncDriver while keeping the Id and
     * Axis/Actuator assignments.
     *
     * @param nullDriver
     * @throws Exception
     */
    public static void convertToAsync(NullDriver nullDriver) throws Exception {
        GcodeAsyncDriver asyncDriver = new GcodeAsyncDriver();
        asyncDriver.setId(nullDriver.getId());
        // Set to simulation by GcodeServer as a default.
        asyncDriver.setCommunicationsType(CommunicationsType.tcp);
        asyncDriver.setIpAddress("GcodeServer");
        replaceDriver(asyncDriver);
    }

    /**
     * Replace a driver with the same Id at the same place in the machine driver list.
     * 
     * @param driver
     * @throws Exception
     */
    public static void replaceDriver(Driver driver) throws Exception {
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
            if (list.get(index).getId().equals(driver.getId())) {
                replaced = list.get(index);
                machine.removeDriver(replaced);
                break;
            }
        }
        // Add the new one.
        machine.addDriver(driver);
        // Permutate it back to the old list place (cumbersome but works).
        for (int p = list.size()-index; p > 1; p--) {
            machine.permutateDriver(driver, -1);
        }
        // Replace the driver in the Machine Setup.
        for (ControllerAxis axis : new AxesLocation(machine).getControllerAxes()) {
            if (axis.getDriver() == replaced) {
                axis.setDriver(driver);
            }
        }
        for (Actuator actuator : machine.getActuators()) {
            if (actuator.getDriver() == replaced) {
                actuator.setDriver(driver);
            }
        }
        for (Head head : machine.getHeads()) {
            for (Actuator actuator : head.getActuators()) {
                if (actuator.getDriver() == replaced) {
                    actuator.setDriver(driver);
                }
            }
        }
        // Re-enable the machine.
        if (wasEnabled) {
            machine.setEnabled(true);
        }
    }
}