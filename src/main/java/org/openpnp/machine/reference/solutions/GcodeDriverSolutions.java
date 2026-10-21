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

package org.openpnp.machine.reference.solutions;


import org.openpnp.Translations;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openpnp.gui.support.Icons;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.driver.AbstractReferenceDriver.CommunicationsType;
import org.openpnp.machine.reference.driver.GcodeAsyncDriver;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.machine.reference.driver.GcodeDriver.Command;
import org.openpnp.machine.reference.driver.GcodeDriver.CommandType;
import org.openpnp.machine.reference.driver.NullDriver;
import org.openpnp.machine.reference.driver.SerialPortCommunications.FlowControl;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.model.Solutions.State;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Driver.MotionControlType;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.util.GcodeServer;
import org.openpnp.util.UiUtils;
import org.openpnp.util.XmlSerialize;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Serializer;

/**
 * This helper class implements the Issues & Solutions for the GcodeDriver and GcodeAsyncDriver. 
 * The idea is not to pollute the driver implementations themselves.
 *
 */
public class GcodeDriverSolutions implements Solutions.Subject {
    private final GcodeDriver gcodeDriver;
    public GcodeDriverSolutions(GcodeDriver gcodeDriver) {
        this.gcodeDriver = gcodeDriver;
    }

    protected enum FirmwareType {
        Generic,
        Smoothieware,
        SmoothiewareGrblSyntax,
        SmoothiewareChmt,
        RepRapFirmware,
        TinyG,
        Marlin,
        Grbl;

        boolean isSmoothie() {
            return this == Smoothieware || this == SmoothiewareGrblSyntax || this == SmoothiewareChmt;
        }

        FlowControl getFlowControl(GcodeDriver gcodeDriver) {
            // If M115 specifies the flow control, take that.
            String flowControl = gcodeDriver.getFirmwareProperty("X-SERIAL_FLOW", "").toUpperCase().trim();
            if (flowControl.equals("NONE") || flowControl.equals("OFF")) {
                return FlowControl.Off;
            }
            else if (flowControl.equals("RTS/CTS")) {
                return FlowControl.RtsCts;
            }
            else if (flowControl.equals("XON/XOFF")) {
                return FlowControl.XonXoff;
            }
            // Default to typical driver setting.
            return (this == TinyG || this == Grbl || this == SmoothiewareChmt) ? FlowControl.Off : FlowControl.RtsCts;
        }
    }

    @Override
    public void findIssues(Solutions solutions) {
        ReferenceMachine machine = (ReferenceMachine) Configuration.get().getMachine();
        boolean hasAxes = !gcodeDriver.getAxisVariables(machine).isEmpty();
        if (solutions.isTargeting(Milestone.Advanced)) {
            if (!(gcodeDriver instanceof GcodeAsyncDriver)) {
                Solutions.Issue issue = new Solutions.Issue(
                        gcodeDriver, 
                        Translations.getString("GcodeDriverSolutions.Issue.UseAsyncDriver"), //$NON-NLS-1$ 
                        Translations.getString("GcodeDriverSolutions.Solution.UseAsyncDriver"), //$NON-NLS-1$ 
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
        }
        else {
            // Conservative settings. 
            if (gcodeDriver instanceof GcodeAsyncDriver) {
                Solutions.Issue issue = new Solutions.Issue(
                        gcodeDriver, 
                        Translations.getString("GcodeDriverSolutions.Issue.UsePlainDriver"), //$NON-NLS-1$ 
                        Translations.getString("GcodeDriverSolutions.Solution.UsePlainDriver"), //$NON-NLS-1$ 
                        Severity.Information,
                        "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver") {

                    @Override
                    public boolean isUnhandled( ) {
                        // Never handle a conservative solution as unhandled.
                        return false;
                    }

                    @Override 
                    public String getExtendedDescription() {
                        return Translations.getString("GcodeDriverSolutions.ExtendedDescription.UsePlainDriver"); //$NON-NLS-1$
                    }

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        if (state == Solutions.State.Solved) {
                            convertToPlain((GcodeAsyncDriver) gcodeDriver);
                        }
                        else if (getState() == Solutions.State.Solved) {
                            // Place the old one back (from the captured gcodeDriver).
                            replaceDriver(gcodeDriver);
                        }
                        super.setState(state);
                    }
                };
                solutions.add(issue);
            }
        }
        if (solutions.isTargeting(Milestone.Connect)) {
            if (gcodeDriver.getCommunicationsType() == CommunicationsType.tcp
                    && gcodeDriver.getIpAddress().contentEquals("GcodeServer")) {
                solutions.add(new Solutions.PlainIssue(
                        gcodeDriver, 
                        Translations.getString("GcodeDriverSolutions.Issue.ConnectDriver"), //$NON-NLS-1$ 
                        Translations.getString("GcodeDriverSolutions.Solution.ConnectDriver"), //$NON-NLS-1$ 
                        Severity.Fundamental,
                        "https://github.com/openpnp/openpnp/wiki/GcodeDriver#connection"));
            }
            if (gcodeDriver.isSpeakingGcode() 
                    && (gcodeDriver.getDetectedFirmware() == null
                    || !gcodeDriver.getDetectedFirmware().equals(GcodeServer.getGenericFirmware()))) {
                try {
                    if (machine.isEnabled()) {
                        gcodeDriver.detectFirmware(true, false);
                    }
                }
                catch (Exception e) {
                    Logger.warn(e, gcodeDriver.getName()+" failure to detect firmware");
                }
            }
            Integer firmwareAxesCount = null;
            Integer firmwarePrimaryAxesCount = null;
            FirmwareType firmware = FirmwareType.Generic;
            if (gcodeDriver.getDetectedFirmware() == null) {
                solutions.add(new Solutions.Issue(
                        gcodeDriver, 
                        Translations.format("GcodeDriverSolutions.Issue.FirmwareNotDetected", //$NON-NLS-1$
                                (machine.isEnabled() ? 
                                        (gcodeDriver.isSpeakingGcode() ? Translations.getString("GcodeDriverSolutions.Choice.FirmwareNotDetected.Failure") : Translations.getString("GcodeDriverSolutions.Choice.FirmwareNotDetected.NoGcode")) 
                                        : Translations.getString("GcodeDriverSolutions.Choice.FirmwareNotDetected.Disabled"))), 
                                Translations.getString("GcodeDriverSolutions.Solution.FirmwareNotDetected"), //$NON-NLS-1$ 
                                Severity.Fundamental,
                        "https://www.reprap.org/wiki/G-code#M115:_Get_Firmware_Version_and_Capabilities") {

                    @Override
                    public Solutions.Issue.Choice[] getChoices() {
                        return new Solutions.Issue.Choice[] {
                                new Solutions.Issue.Choice(true, 
                                        Translations.getString("GcodeDriverSolutions.Choice.DetectFirmware"), //$NON-NLS-1$
                                                Icons.powerOn),
                                new Solutions.Issue.Choice(false, 
                                        Translations.getString("GcodeDriverSolutions.Choice.AssumeGeneric"), //$NON-NLS-1$
                                                Icons.powerOff),
                        };
                    }

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        if (state == Solutions.State.Solved) {
                            if ((Boolean)getChoice()) {
                                final State oldState = getState();
                                try {
                                    gcodeDriver.detectFirmware(false, true);
                                    super.setState(state);
                                }
                                catch (Exception e) { 
                                    UiUtils.showError(e);
                                    // restore old state
                                    UiUtils.messageBoxOnException(() -> setState(oldState));
                                }
                            }
                            else {
                                gcodeDriver.setDetectedFirmware(GcodeServer.getGenericFirmware());
                                super.setState(state);
                            }
                        }
                        else {
                            gcodeDriver.setDetectedFirmware(null);
                            super.setState(state);
                        }
                    }
                });
            }
            else {
                if (gcodeDriver.getFirmwareProperty("FIRMWARE_NAME", "").contains("Smoothieware")) {
                    firmware = (gcodeDriver.getFirmwareProperty("X-GRBL_MODE", "").contains("1"))? 
                            FirmwareType.SmoothiewareGrblSyntax : 
                                (gcodeDriver.getFirmwareProperty("FIRMWARE_VERSION", "").contains("chmt-")
                                        || gcodeDriver.getFirmwareProperty("X-HARDWARE", "").contains("CHMT"))?
                                        FirmwareType.SmoothiewareChmt : FirmwareType.Smoothieware;
                    firmwareAxesCount = Integer.valueOf(gcodeDriver.getFirmwareProperty("X-AXES", "0"));
                    if (firmware == FirmwareType.SmoothiewareChmt) {
                        // OK, CHMT STM32 Smoothieware board. Take PAXES == 5 if missing (legacy build).
                        firmwarePrimaryAxesCount = Integer.valueOf(gcodeDriver.getFirmwareProperty("X-PAXES", "5"));
                    }
                    else if (gcodeDriver.getFirmwareProperty("X-SOURCE_CODE_URL", "").contains("best-for-pnp")) {
                        // OK, regular Smoothieboard with pnp firmware.
                        firmwarePrimaryAxesCount = Integer.valueOf(gcodeDriver.getFirmwareProperty("X-PAXES", "3"));
                    }
                    else {
                        solutions.add(new Solutions.PlainIssue(
                                gcodeDriver, 
                                Translations.format("GcodeDriverSolutions.Issue.SmoothiewareUpgrade", gcodeDriver.getDetectedFirmware()), 
                                Translations.getString("GcodeDriverSolutions.Solution.SmoothiewareUpgrade"), //$NON-NLS-1$ 
                                Severity.Error, 
                                "https://github.com/openpnp/openpnp/wiki/Motion-Controller-Firmwares#smoothieware"));
                    }
                    if (firmwarePrimaryAxesCount != null 
                            && firmwarePrimaryAxesCount != firmwareAxesCount) {
                        solutions.add(new Solutions.PlainIssue(
                                gcodeDriver, 
                                Translations.format("GcodeDriverSolutions.Issue.SmoothiewarePAxis", firmwareAxesCount), 
                                Translations.format("GcodeDriverSolutions.Solution.SmoothiewarePAxis", firmwareAxesCount), 
                                Severity.Warning, 
                                "https://github.com/openpnp/openpnp/wiki/Motion-Controller-Firmwares#smoothieware"));
                    }
                }
                else if (gcodeDriver.getFirmwareProperty("FIRMWARE_NAME", "").contains("RepRapFirmware")) {
                    firmware = FirmwareType.RepRapFirmware;
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
                                Translations.format("GcodeDriverSolutions.Issue.RepRapFirmwareVersion", firmwareVersion),
                                Translations.getString("GcodeDriverSolutions.Solution.RepRapFirmwareVersion"), //$NON-NLS-1$
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
                                    Translations.getString("GcodeDriverSolutions.Issue.RepRapLinearAxes"),
                                    Translations.getString("GcodeDriverSolutions.Solution.RepRapLinearAxes"), //$NON-NLS-1$
                                    Severity.Error,
                                    "https://duet3d.dozuki.com/Wiki/Gcode#Section_M584_Set_drive_mapping"));
                        }
                        else {
                            firmwarePrimaryAxesCount = firmwareAxesCount;
                        }
                    }
                }
                else if (gcodeDriver.getFirmwareProperty("FIRMWARE_NAME", "").contains("Marlin")) {
                    firmware = FirmwareType.Marlin;
                    firmwareAxesCount = Integer.valueOf(gcodeDriver.getFirmwareProperty("AXIS_COUNT", "0"));
                    if (firmwareAxesCount > 3) { 
                        firmwarePrimaryAxesCount = firmwareAxesCount;
                    }
                    else {
                        solutions.add(new Solutions.PlainIssue(
                                gcodeDriver, 
                                Translations.format("GcodeDriverSolutions.Issue.MarlinRotationAxes", gcodeDriver.getDetectedFirmware()), 
                                Translations.getString("GcodeDriverSolutions.Solution.MarlinRotationAxes"), //$NON-NLS-1$ 
                                Severity.Error, 
                                "https://github.com/openpnp/openpnp/wiki/Motion-Controller-Firmwares#marlin-20"));
                    }
                }
                else if (gcodeDriver.getFirmwareProperty("FIRMWARE_NAME", "").contains("TinyG")) {
                    // Having a response already means we have a new firmware.
                    firmware = FirmwareType.TinyG;
                }
                else if (gcodeDriver.getFirmwareProperty("FIRMWARE_NAME", "").contains("Grbl")) {
                    firmware = FirmwareType.Grbl;
                }
                else if (gcodeDriver.getFirmwareProperty("FIRMWARE_NAME", "").contains("GcodeServer")) {
                    firmware = FirmwareType.Generic;
                }
                else { 
                    solutions.add(new Solutions.PlainIssue(
                            gcodeDriver, 
                            Translations.format("GcodeDriverSolutions.Issue.UnknownFirmware", gcodeDriver.getDetectedFirmware()), 
                            Translations.getString("GcodeDriverSolutions.Solution.UnknownFirmware"), //$NON-NLS-1$ 
                            Severity.Warning, 
                            "https://github.com/openpnp/openpnp/wiki/Motion-Controller-Firmwares"));
                }

                if (gcodeDriver.getCommunicationsType() == CommunicationsType.serial 
                        && gcodeDriver.getSerial() != null) {
                    final FlowControl oldFlowControl = gcodeDriver.getSerial().getFlowControl();
                    final FlowControl newFlowControl = firmware.getFlowControl(gcodeDriver);
                    if (oldFlowControl != newFlowControl) {
                        solutions.add(new Solutions.Issue(
                                gcodeDriver, 
                                Translations.getString("GcodeDriverSolutions.Issue.FlowControl"), //$NON-NLS-1$
                                Translations.format("GcodeDriverSolutions.Solution.FlowControl", //$NON-NLS-1$
                                        newFlowControl.name(),
                                        (newFlowControl == FlowControl.Off
                                                ? Translations.format("GcodeDriverSolutions.Solution.FlowControl.OffNote", firmware) //$NON-NLS-1$
                                                : "")),
                                newFlowControl == FlowControl.Off ? Severity.Warning : Severity.Suggestion,
                                "https://en.wikipedia.org/wiki/Flow_control_(data)#Hardware_flow_control") {

                            @Override
                            public void setState(Solutions.State state) throws Exception {
                                gcodeDriver.getSerial().setFlowControl((state == Solutions.State.Solved) ? 
                                        newFlowControl : oldFlowControl);
                                super.setState(state);
                            }
                        });
                    }
                }
            }
            if (gcodeDriver.isConnectionKeepAlive()) {
                solutions.add(new Solutions.Issue(
                        gcodeDriver, 
                        Translations.getString("GcodeDriverSolutions.Issue.KeepAlive"), //$NON-NLS-1$ 
                        Translations.getString("GcodeDriverSolutions.Solution.KeepAlive"), //$NON-NLS-1$ 
                        Severity.Warning,
                        null) {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        gcodeDriver.setConnectionKeepAlive(!(state == Solutions.State.Solved));
                        super.setState(state);
                    }
                });
            }

            if (solutions.isTargeting(Milestone.Basics)) {
                if (gcodeDriver instanceof GcodeAsyncDriver) {
                    boolean locationConfirmation = ((GcodeAsyncDriver)gcodeDriver).isReportedLocationConfirmation();
                    boolean confirmationFlowControl = ((GcodeAsyncDriver)gcodeDriver).isConfirmationFlowControl();
                    boolean locationConfirmationRecommended = hasAxes && firmware!=FirmwareType.Marlin;
                    if (locationConfirmationRecommended != locationConfirmation) {
                        solutions.add(new Solutions.Issue(
                                gcodeDriver, 
                                (locationConfirmationRecommended
                                        ? (confirmationFlowControl
                                                ? Translations.getString("GcodeDriverSolutions.Issue.LocationConfirmation.RecommendedWithFlow") //$NON-NLS-1$
                                                        : Translations.getString("GcodeDriverSolutions.Issue.LocationConfirmation.RequiredNoFlow")) //$NON-NLS-1$
                                                : ( !hasAxes ? Translations.getString("GcodeDriverSolutions.Issue.LocationConfirmation.NotAvailable") //$NON-NLS-1$
                                                             : Translations.getString("GcodeDriverSolutions.Issue.LocationConfirmation.MarlinUnreliable"))), //$NON-NLS-1$
                                (locationConfirmationRecommended ? Translations.getString("GcodeDriverSolutions.Solution.LocationConfirmation.Enable") //$NON-NLS-1$
                                        : Translations.getString("GcodeDriverSolutions.Solution.LocationConfirmation.Disable")), //$NON-NLS-1$
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
                    boolean serialFlowControlOff = (gcodeDriver.getCommunicationsType() == CommunicationsType.serial 
                        && gcodeDriver.getSerial() != null 
                        && gcodeDriver.getSerial().getFlowControl() == FlowControl.Off) || firmware.getFlowControl(gcodeDriver) == FlowControl.Off;
                    boolean confirmationFlowControlRecommended = serialFlowControlOff || ! hasAxes ||
                                                                 firmware==FirmwareType.Marlin; // Marlin require application-level flow control
                    if (confirmationFlowControlRecommended != confirmationFlowControl) {
                        solutions.add(new Solutions.Issue(
                                gcodeDriver,
                                (confirmationFlowControl ?
                                        Translations.getString("GcodeDriverSolutions.Issue.ConfirmationFlowControl.Disable") //$NON-NLS-1$
                                        : Translations.format("GcodeDriverSolutions.Issue.ConfirmationFlowControl.Enable", //$NON-NLS-1$
                                                (hasAxes ? "" : Translations.getString("GcodeDriverSolutions.Issue.ConfirmationFlowControl.Enable.NoAxes")) //$NON-NLS-1$
                                                + (serialFlowControlOff ? Translations.getString("GcodeDriverSolutions.Issue.ConfirmationFlowControl.Enable.NoSerialFlow") : ""))), //$NON-NLS-1$
                                (confirmationFlowControl ?
                                        Translations.getString("GcodeDriverSolutions.Solution.ConfirmationFlowControl.Disable") //$NON-NLS-1$
                                        : Translations.getString("GcodeDriverSolutions.Solution.ConfirmationFlowControl.Enable")), //$NON-NLS-1$
                                confirmationFlowControl ? Severity.Suggestion : Severity.Error,
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
                                Translations.getString("GcodeDriverSolutions.Issue.DisallowPreMove"), 
                                Translations.getString("GcodeDriverSolutions.Solution.DisallowPreMove"), //$NON-NLS-1$ 
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
                                Translations.getString("GcodeDriverSolutions.Issue.UseLetterVariables"), 
                                Translations.getString("GcodeDriverSolutions.Solution.UseLetterVariables"), //$NON-NLS-1$ 
                                Severity.Fundamental,
                                "https://github.com/openpnp/openpnp/wiki/Advanced-Motion-Control#migration-from-a-previous-version") {

                            @Override
                            public void setState(Solutions.State state) throws Exception {
                                gcodeDriver.setUsingLetterVariables((state == Solutions.State.Solved));
                                super.setState(state);
                            }
                        });
                    }

                    if (solutions.isTargeting(Milestone.Kinematics)) {
                        final MotionControlType oldMotionControlType = gcodeDriver.getMotionControlType();
                        final MotionControlType newMotionControlType = (firmware == FirmwareType.TinyG) ?
                                MotionControlType.SimpleSCurve : MotionControlType.ModeratedConstantAcceleration;
                        if (gcodeDriver.getMotionControlType().isUnpredictable() 
                                || ((firmware == FirmwareType.TinyG) && newMotionControlType != oldMotionControlType)) {
                            solutions.add(new Solutions.Issue(
                                    gcodeDriver, 
                                    (firmware == FirmwareType.TinyG)
                                            ? Translations.format("GcodeDriverSolutions.Issue.MotionControlType.TinyG", newMotionControlType.name()) //$NON-NLS-1$
                                            : Translations.getString("GcodeDriverSolutions.Issue.MotionControlType.Advanced"), //$NON-NLS-1$
                                    Translations.format("GcodeDriverSolutions.Solution.MotionControlType", newMotionControlType.name()), //$NON-NLS-1$
                                    ((firmware == FirmwareType.TinyG) ? Severity.Error : Severity.Suggestion),
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
                                    Translations.getString("GcodeDriverSolutions.Issue.MaxFeedRate"), 
                                    Translations.getString("GcodeDriverSolutions.Solution.MaxFeedRate"), //$NON-NLS-1$ 
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
                    else {
                        // Conservative settings. 
                        final MotionControlType oldMotionControlType = gcodeDriver.getMotionControlType();
                        final MotionControlType newMotionControlType = MotionControlType.ToolpathFeedRate;
                        if (oldMotionControlType != newMotionControlType) {
                            solutions.add(new Solutions.Issue(
                                    gcodeDriver, 
                                    Translations.getString("GcodeDriverSolutions.Issue.SimpleMotionControl"), //$NON-NLS-1$
                                    Translations.format("GcodeDriverSolutions.Solution.MotionControlType", newMotionControlType.name()), //$NON-NLS-1$
                                    Severity.Information,
                                    "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#gcodedriver-new-settings") {

                                @Override
                                public boolean isUnhandled( ) {
                                    // Never handle a conservative solution as unhandled.
                                    return false;
                                }

                                @Override 
                                public String getExtendedDescription() {
                                    return Translations.format("GcodeDriverSolutions.ExtendedDescription.SimpleMotionControl", //$NON-NLS-1$
                                            newMotionControlType.name(), oldMotionControlType.name());
                                }

                                @Override
                                public void setState(Solutions.State state) throws Exception {
                                    gcodeDriver.setMotionControlType((state == Solutions.State.Solved) ? 
                                            newMotionControlType : oldMotionControlType);
                                    super.setState(state);
                                }
                            });
                        }
                    }
                }

                if (gcodeDriver.isSpeakingGcode()) {
                    if (solutions.isTargeting(Milestone.Advanced)) {
                        if (!gcodeDriver.isCompressGcode()) {
                            solutions.add(new Solutions.Issue(
                                    gcodeDriver, 
                                    Translations.getString("GcodeDriverSolutions.Issue.CompressGcode.Enable"), 
                                    Translations.getString("GcodeDriverSolutions.Solution.CompressGcode.Enable"), //$NON-NLS-1$ 
                                    Severity.Suggestion,
                                    "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#gcodedriver-new-settings") {

                                @Override
                                public void setState(Solutions.State state) throws Exception {
                                    gcodeDriver.setCompressGcode((state == Solutions.State.Solved));
                                    super.setState(state);
                                }
                            });
                        }
                        if (!gcodeDriver.isRemoveComments()) {
                            solutions.add(new Solutions.Issue(
                                    gcodeDriver, 
                                    Translations.getString("GcodeDriverSolutions.Issue.RemoveComments.Enable"), 
                                    Translations.getString("GcodeDriverSolutions.Solution.RemoveComments.Enable"), //$NON-NLS-1$ 
                                    Severity.Suggestion,
                                    "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#gcodedriver-new-settings") {

                                @Override
                                public void setState(Solutions.State state) throws Exception {
                                    gcodeDriver.setRemoveComments((state == Solutions.State.Solved));
                                    super.setState(state);
                                }
                            });
                        }
                    }
                    else {
                        // Conservative settings. 
                        if (gcodeDriver.isCompressGcode()) {
                            solutions.add(new Solutions.Issue(
                                    gcodeDriver, 
                                    Translations.getString("GcodeDriverSolutions.Issue.CompressGcode.Disable"), 
                                    Translations.getString("GcodeDriverSolutions.Solution.CompressGcode.Disable"), //$NON-NLS-1$ 
                                    Severity.Information,
                                    "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#gcodedriver-new-settings") {

                                @Override
                                public boolean isUnhandled( ) {
                                    // Never handle a conservative solution as unhandled.
                                    return false;
                                }

                                @Override 
                                public String getExtendedDescription() {
                                    return Translations.getString(
                                            "GcodeDriverSolutions.ExtendedDescription.CompressGcode.Disable"); //$NON-NLS-1$
                                }

                                @Override
                                public void setState(Solutions.State state) throws Exception {
                                    gcodeDriver.setCompressGcode((state != Solutions.State.Solved));
                                    super.setState(state);
                                }
                            });
                        }
                        if (gcodeDriver.isRemoveComments()) {
                            solutions.add(new Solutions.Issue(
                                    gcodeDriver, 
                                    Translations.getString("GcodeDriverSolutions.Issue.RemoveComments.Disable"), 
                                    Translations.getString("GcodeDriverSolutions.Solution.RemoveComments.Disable"), //$NON-NLS-1$ 
                                    Severity.Information,
                                    "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver#gcodedriver-new-settings") {

                                @Override
                                public boolean isUnhandled( ) {
                                    // Never handle a conservative solution as unhandled.
                                    return false;
                                }

                                @Override 
                                public String getExtendedDescription() {
                                    return Translations.getString(
                                            "GcodeDriverSolutions.ExtendedDescription.RemoveComments.Disable"); //$NON-NLS-1$
                                }

                                @Override
                                public void setState(Solutions.State state) throws Exception {
                                    gcodeDriver.setRemoveComments((state != Solutions.State.Solved));
                                    super.setState(state);
                                }
                            });
                        }
                    }
                }

                boolean lettersOk = gcodeDriver.isUsingLetterVariables();
                for (ControllerAxis axis : new AxesLocation(machine)
                        .drivenBy(gcodeDriver).getControllerAxes()) {
                    // Note: some of the axis solutions are handled in the axes themselves. 
                    if (axis.getLetter().isEmpty()) {
                        lettersOk = false;
                    }
                    else if (axis instanceof ReferenceControllerAxis) {
                        // Find the index of the axis.
                        int index = gcodeDriver.getReportedAxesLetters().indexOf(axis.getLetter());
                        if (firmwarePrimaryAxesCount != null 
                                && firmwarePrimaryAxesCount > index) {
                            // Check rotation axes handled as primary have the linear switch set.
                            if (axis.isRotationalOnController()) {
                                final boolean oldInvertLinearRotational = ((ReferenceControllerAxis) axis).isInvertLinearRotational();
                                solutions.add(new Solutions.Issue(
                                        axis, 
                                        Translations.getString("GcodeDriverSolutions.Issue.InvertLinearRotational.Primary"), 
                                        Translations.format("GcodeDriverSolutions.Solution.InvertLinearRotational", (oldInvertLinearRotational ? Translations.getString("GcodeDriverSolutions.Choice.InvertLinearRotational.Disable") : Translations.getString("GcodeDriverSolutions.Choice.InvertLinearRotational.Enable"))), 
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
                                        Translations.format("GcodeDriverSolutions.Issue.InvertLinearRotational.Letter", //$NON-NLS-1$
                                                (rotational ? Translations.getString("GcodeDriverSolutions.Choice.InvertLinearRotational.Rotational") //$NON-NLS-1$
                                                        : Translations.getString("GcodeDriverSolutions.Choice.InvertLinearRotational.Linear"))), //$NON-NLS-1$
                                        Translations.format("GcodeDriverSolutions.Solution.InvertLinearRotational", (oldInvertLinearRotational ? Translations.getString("GcodeDriverSolutions.Choice.InvertLinearRotational.Disable") : Translations.getString("GcodeDriverSolutions.Choice.InvertLinearRotational.Enable"))), 
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
                    suggestGcodeCommands(solutions, machine, firmware, hasAxes);
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
                        headMountable = Translations.getString("GcodeDriverSolutions.Choice.ActuatorReadHeadMountable.Default"); //$NON-NLS-1$
                    } else if (command.headMountableId.equals("*")) {
                        headMountable = Translations.getString("GcodeDriverSolutions.Choice.ActuatorReadHeadMountable.CatchAll"); //$NON-NLS-1$
                    } else {
                        headMountable = Translations.format("GcodeDriverSolutions.Choice.ActuatorReadHeadMountable.Id", command.headMountableId); //$NON-NLS-1$
                    }
                    solutions.add(new Solutions.Issue(
                            gcodeDriver,
                            Translations.format("GcodeDriverSolutions.Issue.ActuatorReadDeprecated", headMountable), //$NON-NLS-1$
                                    Translations.getString("GcodeDriverSolutions.Solution.ActuatorReadDeprecated"), //$NON-NLS-1$
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
        }
    }

    private void suggestGcodeCommands(Solutions solutions, ReferenceMachine machine, FirmwareType dialect,
            boolean hasAxes) {
        for (CommandType commandType : gcodeDriver.isSpeakingGcode() ?
                CommandType.values() 
                : new CommandType[] { CommandType.CONNECT_COMMAND }) {
            String rationale = "";
            String command = gcodeDriver.getCommand(null, commandType);
            String commandBuilt = null;
            boolean disallowHeadMountables = false;
            boolean commandModified = false;
            switch (commandType) {
                case CONNECT_COMMAND:
                    if (command == null) {
                        if (gcodeDriver.getUnits() == LengthUnit.Millimeters) {
                            commandBuilt = "G21 ; Set millimeters mode \n";
                        }
                        else if (gcodeDriver.getUnits() == LengthUnit.Inches) {
                            commandBuilt = "G20 ; Set inches mode \n";
                        }
                        commandBuilt += "G90 ; Set absolute positioning mode";
                        if (dialect == FirmwareType.TinyG) {
                            commandBuilt = 
                                    // We no longer propose the $ex setting. You can't change flow-control in mid-connection reliably.
                                    //"$ex=0\n" // off
                                    "$sv=0\n" // Non-verbose
                                    +commandBuilt;
                        }
                    }
                    else {
                        if (gcodeDriver.getUnits() == LengthUnit.Millimeters) {
                            if (command.contains("G20 ")) {
                                rationale += Translations.getString("GcodeDriverSolutions.Rationale.ReplaceG20WithG21"); //$NON-NLS-1$
                                commandBuilt = command
                                        .replace("G20 ", "G21 ")
                                        .replace("inches", "millimeters");
                            }
                            else if (! command.contains("G21 "))
                            {
                                rationale += Translations.getString("GcodeDriverSolutions.Rationale.SetMillimeters"); //$NON-NLS-1$
                                commandBuilt = "G21 ; Set millimeters mode \n" + command;
                            }
                        }
                        else if (gcodeDriver.getUnits() == LengthUnit.Inches) {
                            if (command.contains("G21 ")) {
                                rationale += Translations.getString("GcodeDriverSolutions.Rationale.ReplaceG21WithG20"); //$NON-NLS-1$
                                commandBuilt = command
                                        .replace("G21 ", "G20 ")
                                        .replace("millimeters", "inches");
                            }
                            else if (! command.contains("G20 "))
                            {
                                rationale += Translations.getString("GcodeDriverSolutions.Rationale.SetInches"); //$NON-NLS-1$
                                commandBuilt = "G20 ; Set inches mode \n" + command;
                            }
                        }
                        else {
                            if (command.contains("G21 ") || command.contains("G20 ")) {
                                // This is a bit helpless but functionally better than leaving the wroing mode.
                                commandBuilt = command
                                        .replace("G21 ", "; Unsupported driver unit ")
                                        .replace("G20 ", "; Unsupported driver unit ");
                            }
                        }
                        if (dialect == FirmwareType.TinyG) {
                            commandModified = true;
                            if (command.contains("$ex=2")) {
                                commandBuilt = command.replace("$ex=2", "$ex=0");
                            }
                            else if (command.contains("$ex=1")) {
                                commandBuilt = command.replace("$ex=1", "$ex=0");
                            }
                            // We no longer propose the $ex setting, if not yet present. You can't change flow-control in mid-connection reliably.
                        }
                    }
                    break;
                case COMMAND_CONFIRM_REGEX:
                    if (dialect == FirmwareType.TinyG) {
                        commandBuilt = "^tinyg .* ok.*";
                    }
                    else {
                        commandBuilt = "^ok.*";
                    }
                    break;
                case COMMAND_ERROR_REGEX:
                    if (dialect == FirmwareType.TinyG) {
                        commandBuilt = "^tinyg .* err:.*";
                    }
                    else {
                        //commandBuilt = "^!!*";
                    }
                    break;
                case HOME_COMMAND:
                    if (dialect == FirmwareType.SmoothiewareGrblSyntax || dialect == FirmwareType.Grbl) {
                        commandBuilt = "$H ; Home all axes";
                    }
                    else if (dialect == FirmwareType.TinyG) {
                        commandBuilt = "G28.2 ";
                        for (String variable : gcodeDriver.getAxisVariables(machine)) {
                            if ("XYZ".indexOf(variable) >= 0) {
                                // In TinyG you need to indicate the axis and only 0 is possible as coordinate.
                                commandBuilt += variable+"0 ";  
                            }
                        }
                        commandBuilt += "; Home all axes\n";
                        commandBuilt += "G28.3";
                        for (String variable : gcodeDriver.getAxisVariables(machine)) {
                            commandBuilt += " {"+variable+":"+variable+"%.4f}";
                        }
                        commandBuilt += " ; Set all axes to home coordinates\n";
                        commandBuilt += "G92.1 ; Reset all offsets";
                    }
                    else {
                        // Reset the acceleration (it is not automatically reset on some controllers). 
                        commandBuilt = "{Acceleration:M204 S%.2f ; Initialize acceleration}\n";
                        // Home all axes.
                        commandBuilt += "G28 ; Home all axes";
                    }
                    if (command != null && command.contains(commandBuilt)) {
                        commandBuilt = null;
                    }
                    break;
                case DELAY_COMMAND:
                    commandBuilt = "{TimeMS:G4 P%d} ; Delay for given time in [ms]";
                    break;
                case MOVE_TO_COMMAND:
                    if (hasAxes) {
                        // Determine minimum rates to compute needed decimal digits.
                        double vMin = gcodeDriver.getMinimumRate(1)
                                .convertToUnits(gcodeDriver.getUnits()).getValue();
                        double aMin = gcodeDriver.getMinimumRate(2)
                                .convertToUnits(gcodeDriver.getUnits()).getValue();
                        double jMin = gcodeDriver.getMinimumRate(3)
                                .convertToUnits(gcodeDriver.getUnits()).getValue();
                        if (dialect == FirmwareType.TinyG) {
                            // Apply jerk limits per axis. 
                            int digits = digitsToExpress(jMin);
                            commandBuilt = "M201.3 ";
                            for (String variable : gcodeDriver.getAxisVariables(machine)) {
                                commandBuilt += "{"+variable+"Jerk:"+variable+"%."+digits+"f} ";
                            }
                            // This needs a new-line: "It is an error to put a G-code from group 1 
                            // and a G-code from group 0 on the same line if both of
                            // them use axis words." (RS274/NGC Interpreter - Version 3, §3.4)
                            commandBuilt += "\n";
                        }
                        else {
                            // Apply acceleration limit.
                            int digits = digitsToExpress(aMin);
                            commandBuilt = "{Acceleration:M204 S%."+digits+"f }";
                            if (dialect == FirmwareType.Marlin) {
                                // Non-conformant G-code parser, needs newline.
                                commandBuilt += "\n";
                            }
                        }
                        commandBuilt += "G1 ";
                        for (String variable : gcodeDriver.getAxisVariables(machine)) {
                            // Determine the significant number of digits.
                            int digits = digitsAxisResolution(variable, machine);
                            commandBuilt += "{"+variable+":"+variable+"%."+digits+"f} ";
                        }
                        int digits = digitsToExpress(vMin*60); // F is per minute
                        commandBuilt += "{FeedRate:F%."+digits+"f} ; move to target";
                    }
                    else if (command != null) {
                        commandBuilt = "";
                    }
                    disallowHeadMountables = true;
                    break;
                case MOVE_TO_COMPLETE_COMMAND:
                    // This is provided even if there are no axes on the driver. M400 may still be useful for actuator coordination.
                    if (gcodeDriver.getCommand(null, CommandType.MOVE_TO_COMPLETE_REGEX) == null) {
                        if (dialect == FirmwareType.Grbl) {
                            commandBuilt = "G4 P0 ; Wait for moves to complete before returning";
                        }
                        else {
                            commandBuilt = "M400 ; Wait for moves to complete before returning";
                        }
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
                        if (dialect == FirmwareType.TinyG) {
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
                        try {
                            // We need to parse the report in standard Gcode axis order. This might not cover all the controllers, 
                            // but it's what we can do.
                            commandBuilt = "^.*";
                            int axisIndex = 0;
                            int lastAxisIndex = 26;
                            int axesAdded = 0;
                            String pattern = "";
                            List<String> letters = gcodeDriver.getReportedAxesLetters();
                            if (letters.isEmpty()) {
                                // we don't have reported letters, take a theoretical set
                                letters = new ArrayList<>(Arrays.asList(AxisSolutions.VALID_AXIS_LETTERS));
                            }
                            // if the reportedAxes contains "C:" before the first axis letter, add it to the pattern
                            if (gcodeDriver.getReportedAxes().matches("^.*C:\\s*[" + Arrays.stream(AxisSolutions.VALID_AXIS_LETTERS).collect(Collectors.joining()) + "].*")) {
                                commandBuilt += "C:\\s*";
                            }
                            for (String axisLetter : letters) {
                                for (String variable : gcodeDriver.getAxisVariables(machine)) {
                                    if (variable.equals(axisLetter)) {
                                        if (lastAxisIndex < axisIndex-1) {
                                            // Skipped some axes, add a wild.card.
                                            commandBuilt += ".*";
                                        }
                                        if (axesAdded > 0) {
                                            // if any axis as been added, allow any number of whitespace
                                            commandBuilt += "\\s*";
                                        }
                                        commandBuilt += variable+":(?<"+variable+">-?\\d+\\.\\d+)";
                                        axesAdded++;
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
                                        Translations.format("GcodeDriverSolutions.Issue.PositionReportPattern", pattern, gcodeDriver.getReportedAxes()), //$NON-NLS-1$
                                        (dialect.isSmoothie() ? Translations.getString("GcodeDriverSolutions.Solution.PositionReportPattern.Smoothie") //$NON-NLS-1$
                                                : Translations.getString("GcodeDriverSolutions.Solution.PositionReportPattern.Generic")), //$NON-NLS-1$
                                        Severity.Error,
                                        (dialect.isSmoothie() ? "https://github.com/openpnp/openpnp/wiki/Motion-Controller-Firmwares#axes-vs-extruder-configuration"
                                                : "https://github.com/openpnp/openpnp/wiki/Motion-Controller-Firmwares")));
                            }
                        }
                        catch (Exception e) {
                            // If there are duplicate axis letters, this may throw. But duplicate axis letters are caught in the AxisSolutions,
                            // so we can ignore this exception here.
                        }
                    }
                    else if (command != null) {
                        commandBuilt = "";
                    }
                    break;
            }
            suggestGcodeCommand(gcodeDriver, null, solutions, commandType, commandBuilt, commandModified,
                    disallowHeadMountables, rationale);
        }
    }

    /**
     * @param variable
     * @param machine
     * @return
     */
    private int digitsAxisResolution(String variable, ReferenceMachine machine) {
        int digits = 4;
        for (ControllerAxis axis : gcodeDriver.getAxes(machine)) {
            if (variable.equals(axis.getLetter())) {
                if (axis instanceof ReferenceControllerAxis) {
                    double res = ((ReferenceControllerAxis) axis).getResolution();
                    digits = digitsToExpress(res);
                }
            }
        }
        return digits;
    }

    static private int digitsToExpress(double res) {
        return Math.max(0, Math.min(4, (int)Math.ceil(-Math.log10(res))));
    }

    /**
     * Add a solution for the given gcodeDriver to the issues, to suggest the given suggestedCommand instead of the currentCommand.
     *
     * @param gcodeDriver
     * @param headMountable
     * @param solutions
     * @param commandType
     * @param suggestedCommand
     * @param commandModified
     * @param disallowHeadMountables Set true to disallow the commandType on HeadMountables. This is used when switching to axis letter variables.
     */
    public static void suggestGcodeCommand(GcodeDriver gcodeDriver, HeadMountable headMountable, Solutions solutions,
            CommandType commandType, String suggestedCommand, boolean commandModified,
            boolean disallowHeadMountables, String rationale) {
        String currentCommand = gcodeDriver.getCommand(headMountable, commandType);
        if (suggestedCommand != null && !suggestedCommand.equals(currentCommand)) {
            final String solution;
            if (suggestedCommand.isEmpty()) {
                solution = Translations.getString("GcodeDriverSolutions.Solution.GcodeCommand.Delete"); //$NON-NLS-1$
            } else if (commandModified) {
                solution = Translations.getString("GcodeDriverSolutions.Solution.GcodeCommand.Modify"); //$NON-NLS-1$
            } else {
                solution = Translations.getString("GcodeDriverSolutions.Solution.GcodeCommand.Change"); //$NON-NLS-1$
            }
            final String issueSuffix;
            if (suggestedCommand.isEmpty()) {
                issueSuffix = Translations.getString("GcodeDriverSolutions.Choice.GcodeCommand.Obsolete"); //$NON-NLS-1$
            } else if (commandModified) {
                issueSuffix = Translations.getString("GcodeDriverSolutions.Choice.GcodeCommand.Modification"); //$NON-NLS-1$
            } else {
                issueSuffix = Translations.getString("GcodeDriverSolutions.Choice.GcodeCommand.Suggested"); //$NON-NLS-1$
            }
            final String acceptNote = gcodeDriver.isSpeakingGcode() ? "" : Translations.getString("GcodeDriverSolutions.Issue.GcodeCommand.AcceptNote"); //$NON-NLS-1$
            solutions.add(new Solutions.Issue(
                    (headMountable != null ? headMountable : gcodeDriver),
                    Translations.format("GcodeDriverSolutions.Issue.GcodeCommand", commandType.name(), issueSuffix + acceptNote), //$NON-NLS-1$
                    solution,
                    suggestedCommand.isEmpty() ? Severity.Warning
                            : (gcodeDriver.isSpeakingGcode() ? Severity.Suggestion : Severity.Fundamental),
                    "https://github.com/openpnp/openpnp/wiki/Advanced-Motion-Control#migration-from-a-previous-version") {

                @Override
                public void setState(Solutions.State state) throws Exception {
                    gcodeDriver.setCommand(headMountable, commandType, (state == Solutions.State.Solved) ? suggestedCommand : currentCommand);
                    super.setState(state);
                }
                @Override
                public String getExtendedDescription() {
                    String rationalePart = (rationale != null && !rationale.isEmpty())
                            ? Translations.format("GcodeDriverSolutions.ExtendedDescription.GcodeCommand.Rationale", rationale) //$NON-NLS-1$
                            : "";
                    String commandPart;
                    if (suggestedCommand.isEmpty()) {
                        commandPart = Translations.getString("GcodeDriverSolutions.ExtendedDescription.GcodeCommand.Delete"); //$NON-NLS-1$
                    } else {
                        commandPart = Translations.format("GcodeDriverSolutions.ExtendedDescription.GcodeCommand.Suggested", suggestedCommand); //$NON-NLS-1$
                    }
                    String prev = gcodeDriver.getCommand(headMountable, commandType);
                    String currentPart = (prev != null && !prev.isEmpty())
                            ? Translations.format("GcodeDriverSolutions.ExtendedDescription.GcodeCommand.Current", prev) //$NON-NLS-1$
                            : "";
                    return Translations.format("GcodeDriverSolutions.ExtendedDescription.GcodeCommand", //$NON-NLS-1$
                            rationalePart, commandPart, currentPart);
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
                                Translations.format("GcodeDriverSolutions.Issue.HeadMountableCommandObsolete", //$NON-NLS-1$
                                        hm.getClass().getSimpleName(), hm.getName(), commandType),
                                Translations.getString("GcodeDriverSolutions.Solution.HeadMountableCommandObsolete"), //$NON-NLS-1$
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

    /**
     * Convert an existing GcodeDriver to a GcodeAsyncDriver while keeping all settings and 
     * Axis/Actuator assignments. 
     * 
     * @param gcodeDriver
     * @throws Exception
     */
    public static void convertToAsync(GcodeDriver gcodeDriver) throws Exception {
        // Serialize the GcodeDriver
        Serializer serOut = XmlSerialize.createSerializer();
        StringWriter sw = new StringWriter();
        serOut.write(gcodeDriver, sw);
        String gcodeDriverSerialized = sw.toString();
        // Patch it.
        gcodeDriverSerialized.replace(
                gcodeDriver.getClass().getCanonicalName(), 
                GcodeAsyncDriver.class.getCanonicalName());
        // De-serialize it.
        Serializer serIn = XmlSerialize.createSerializer();
        StringReader sr = new StringReader(gcodeDriverSerialized);
        GcodeAsyncDriver asyncDriver = serIn.read(GcodeAsyncDriver.class, sr);
        // Triple the timeout as asynchronously executed move sequences can be longer than single moves.
        asyncDriver.setTimeoutMilliseconds(asyncDriver.getTimeoutMilliseconds()*3);
        replaceDriver(asyncDriver);
    }

    /**
     * Convert an existing GcodeAsyncDriver to a GcodeDriver while keeping all settings and 
     * Axis/Actuator assignments. Removes sub-class properties.  
     * 
     * @param asyncDriver
     * @throws Exception
     */
    public static void convertToPlain(GcodeAsyncDriver asyncDriver) throws Exception {
        // Serialize the GcodeDriver
        Serializer serOut = XmlSerialize.createSerializer();
        StringWriter sw = new StringWriter();
        serOut.write(asyncDriver, sw);
        String gcodeDriverSerialized = sw.toString();
        // Patch it.
        gcodeDriverSerialized.replace(
                asyncDriver.getClass().getCanonicalName(), 
                GcodeAsyncDriver.class.getCanonicalName());
        // Remove the sub-class properties. 
        gcodeDriverSerialized = XmlSerialize.purgeSubclassXml(GcodeAsyncDriver.class, gcodeDriverSerialized);
        // De-serialize it.
        Serializer serIn = XmlSerialize.createSerializer();
        StringReader sr = new StringReader(gcodeDriverSerialized);
        GcodeDriver gcodeDriver = serIn.read(GcodeDriver.class, sr);
        replaceDriver(gcodeDriver);
    }

    /**
     * Convert an existing NullDriver to a GcodeDriver while keeping the Id and
     * Axis/Actuator assignments.
     *
     * @param nullDriver
     * @throws Exception
     */
    public static void convertToGcode(NullDriver nullDriver) throws Exception {
        GcodeDriver gcodeDriver = new GcodeDriver();
        gcodeDriver.setId(nullDriver.getId());
        // Set to simulation by GcodeServer as a default.
        gcodeDriver.setCommunicationsType(CommunicationsType.tcp);
        gcodeDriver.setIpAddress("GcodeServer");
        replaceDriver(gcodeDriver);
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
