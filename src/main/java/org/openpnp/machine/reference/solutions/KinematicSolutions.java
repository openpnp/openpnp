/*
 * Copyright (C) 2021 <mark@makr.zone>
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

import javax.swing.Icon;

import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.axis.ReferenceCamClockwiseAxis;
import org.openpnp.machine.reference.axis.ReferenceCamCounterClockwiseAxis;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Issue;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.model.Solutions.State;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.base.AbstractHeadMountable;
import org.openpnp.util.UiUtils;
import org.pmw.tinylog.Logger;

/**
 * This helper class implements the Issues & Solutions for the Kinematics milestone. 
 *
 */
public class KinematicSolutions implements Solutions.Subject {
    private ReferenceMachine machine;

    public KinematicSolutions setMachine(ReferenceMachine machine) {
        this.machine = machine;
        return this;
    }

    @Override
    public void findIssues(Solutions solutions) {
        if (solutions.isTargeting(Milestone.Kinematics)) {
            if (! machine.isHomed()) {
                solutions.add(new Solutions.Issue(
                        machine, 
                        Translations.getString("KinematicSolutions.Issue.MachineMustBeHomed"), //$NON-NLS-1$
                        Translations.getString("KinematicSolutions.Solution.MachineMustBeHomed"), //$NON-NLS-1$
                        Severity.Fundamental,
                        "https://github.com/openpnp/openpnp/wiki/User-Manual#machine-controls") {

                    @Override 
                    public String getExtendedDescription() {
                        return Translations.getString("KinematicSolutions.ExtendedDescription.MachineMustBeHomed"); //$NON-NLS-1$
                    }

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        if (state == State.Solved) {
                            UiUtils.messageBoxOnException(() -> {
                                if (!machine.isEnabled()) {
                                    machine.setEnabled(true);
                                }
                                UiUtils.submitUiMachineTask(() -> {
                                    if (! machine.isHomed()) {
                                        machine.home();
                                    }
                                    UiUtils.messageBoxOnExceptionLater(() -> super.setState(state));
                                });
                            });
                        }
                        else {
                            super.setState(state);
                        }
                    }
                });
                return;
            }
            // Dynamic Safe Z yes/no.
            boolean okDynamicSafeZ = true;
            for (Head head : machine.getHeads()) {
                for (Nozzle nozzle : head.getNozzles()) {
                    if (nozzle instanceof ReferenceNozzle) {
                        final ReferenceNozzle refNozzle = (ReferenceNozzle) nozzle;
                        final boolean oldDynamicSafeZ = refNozzle.isEnableDynamicSafeZ();
                        Issue issue = new Solutions.Issue(
                                nozzle, 
                                Translations.format("KinematicSolutions.Issue.DynamicSafeZ", nozzle.getName()), //$NON-NLS-1$
                                Translations.format("KinematicSolutions.Solution.DynamicSafeZ", nozzle.getName()), //$NON-NLS-1$
                                Solutions.Severity.Fundamental,
                                "https://github.com/openpnp/openpnp/wiki/Kinematic-Solutions#dynamic-safe-z") {
                            {
                                setChoice(oldDynamicSafeZ);
                            }
                            @Override
                            public Solutions.Issue.Choice[] getChoices() {
                                return new Solutions.Issue.Choice[] {
                                        new Solutions.Issue.Choice(true, 
                                                Translations.getString("KinematicSolutions.Choice.DynamicSafeZ"), //$NON-NLS-1$
                                                Icons.safeZDynamic),
                                        new Solutions.Issue.Choice(false, 
                                                Translations.getString("KinematicSolutions.Choice.FixedSafeZ"), //$NON-NLS-1$
                                                        Icons.safeZFixed),
                                };
                            }

                            @Override
                            public void setState(Solutions.State state) throws Exception {
                                refNozzle.setEnableDynamicSafeZ((state == State.Solved) ? (boolean) getChoice() : oldDynamicSafeZ);
                                setChoice((state == State.Solved) ? (boolean) getChoice() : oldDynamicSafeZ);
                                // This is a permanently available solution and we need to save state.
                                solutions.setSolutionsIssueSolved(this, (state == State.Solved));
                                super.setState(state);
                            }
                        };
                        solutions.add(issue);
                        if (!solutions.isSolutionsIssueSolved(issue)) {
                            okDynamicSafeZ = false;
                        }
                    }
                }
            }
            if (okDynamicSafeZ) {

                // Safe Z
                for (Head head : machine.getHeads()) {
                    for (HeadMountable hm: head.getHeadMountables()) {
                        CoordinateAxis rawAxisZ = HeadSolutions.getRawAxis(machine, hm.getAxisZ());
                        if (rawAxisZ instanceof ReferenceControllerAxis) {
                            // We got a head-mountable that has a controller raw Z axis. Needs Safe Z.
                            ReferenceControllerAxis axisZ = (ReferenceControllerAxis) rawAxisZ;
                            if (axisZ.getDriver() != null) {
                                try {
                                    // Driver seems configured. Guess the direction of any transformation by probing two Z coordinates.
                                    Location location1 = hm.getLocation();
                                    AxesLocation axesLocation1 = hm.toRaw(location1);
                                    Location location0 = location1.subtract(new Location(LengthUnit.Millimeters, 0, 0, 1, 0));
                                    AxesLocation axesLocation0 = hm.toRaw(location0);
                                    double zUnit = axesLocation1.subtract(axesLocation0).getCoordinate(axisZ);
                                    boolean isShared = isSharedAxis(head, hm, axisZ);
                                    final boolean partClearance = (hm instanceof ReferenceNozzle) && !(((ReferenceNozzle) hm).isEnableDynamicSafeZ());
                                    final boolean limitLow = (zUnit > 0);
                                    final Length oldLimitLow = axisZ.getSafeZoneLow();
                                    final Length oldLimitHigh = axisZ.getSafeZoneHigh();  
                                    final boolean oldEnableLow = axisZ.isSafeZoneLowEnabled(); 
                                    final boolean oldEnableHigh = axisZ.isSafeZoneHighEnabled();
                                    final boolean bothSafeZoneHighAndLowEnabled = oldEnableHigh && oldEnableLow;
                                    boolean safeZSolved = false;

                                    if (bothSafeZoneHighAndLowEnabled
                                        && axisZ.getSafeZoneLow().compareTo(axisZ.getSafeZoneHigh()) > 0) {
                                        solutions.add(new Solutions.Issue(
                                                axisZ, 
                                                Translations.format("KinematicSolutions.Issue.InvalidSafeZZone", axisZ.getName()), //$NON-NLS-1$
                                                Translations.format("KinematicSolutions.Solution.InvalidSafeZZone", axisZ.getName()), //$NON-NLS-1$
                                                Solutions.Severity.Error,
                                                "https://github.com/openpnp/openpnp/wiki/Machine-Axes#kinematic-settings--axis-limits") {
                                            @Override
                                            public void setState(Solutions.State state) throws Exception {
                                                axisZ.setSafeZoneLowEnabled(state != State.Solved);
                                                axisZ.setSafeZoneHighEnabled(state != State.Solved);
                                                MainFrame.get().getIssuesAndSolutionsTab().findIssuesAndSolutions();
                                            }
                                        });
                                    }
                                    else {
                                        if (bothSafeZoneHighAndLowEnabled
                                            && hm.getSafeZ().convertToUnits(LengthUnit.Millimeters).getValue() > 2.0) {
                                            solutions.add(new Solutions.PlainIssue(
                                                    hm,
                                                    Translations.format("KinematicSolutions.Issue.UnconventionalZ", hm.getName()), //$NON-NLS-1$
                                                    Translations.format("KinematicSolutions.Solution.UnconventionalZ", hm.getName()), //$NON-NLS-1$
                                                    Solutions.Severity.Warning,
                                                    "https://github.com/openpnp/openpnp/wiki/Machine-Axes#a-word-about-z-coordinates"));
                                        }

                                        safeZSolved = solutions.add(new Solutions.Issue(
                                                hm, 
                                                Translations.format("KinematicSolutions.Issue.SetSafeZ", hm.getName()), //$NON-NLS-1$
                                                Translations.format("KinematicSolutions.Solution.SetSafeZ", hm.getName()), //$NON-NLS-1$
                                                Solutions.Severity.Fundamental,
                                                "https://github.com/openpnp/openpnp/wiki/Kinematic-Solutions#capture-safe-z") {

                                            @Override 
                                            public void activate() throws Exception {
                                                MainFrame.get().getMachineControls().setSelectedTool(hm);
                                            }

                                            @Override 
                                            public String getExtendedDescription() {
                                                return Translations.format("KinematicSolutions.ExtendedDescription.SetSafeZ", //$NON-NLS-1$
                                                        hm.getName(),
                                                        (partClearance ? Translations.getString("KinematicSolutions.ExtendedDescription.SetSafeZ.PartClearance") : ""), //$NON-NLS-1$
                                                        (head.getNozzles().size() > 1 ? 
                                                                Translations.getString("KinematicSolutions.ExtendedDescription.SetSafeZ.MultiNozzle") : //$NON-NLS-1$
                                                            ""));
                                            }

                                            @Override
                                            public Icon getExtendedIcon() {
                                                return partClearance ? Icons.safeZFixed : Icons.safeZCapture;
                                            }

                                            @Override
                                            public boolean isForcedUnsolved() {
                                                // Always show this as unsolved, if 
                                                return !(limitLow ? axisZ.isSafeZoneLowEnabled() : axisZ.isSafeZoneHighEnabled()); 
                                            }

                                            @Override
                                            public void setState(Solutions.State state) throws Exception {
                                                Length newLimit = axisZ.getDriverLengthCoordinate();
                                                if (limitLow) {
                                                    axisZ.setSafeZoneLow((state == State.Solved) ? newLimit : oldLimitLow);
                                                    axisZ.setSafeZoneLowEnabled((state == State.Solved) ? true : oldEnableLow);
                                                    if (!isShared) {
                                                        axisZ.setSafeZoneHighEnabled((state == State.Solved) ? false : oldEnableHigh);
                                                    }
                                                }
                                                else {
                                                    axisZ.setSafeZoneHigh((state == State.Solved) ? newLimit : oldLimitHigh);
                                                    axisZ.setSafeZoneHighEnabled((state == State.Solved) ? true : oldEnableHigh);
                                                    if (!isShared) {
                                                        axisZ.setSafeZoneLowEnabled((state == State.Solved) ? false : oldEnableLow);
                                                    }
                                                }
                                                // This is a permanently available solution and we need to save state.
                                                solutions.setSolutionsIssueSolved(this, (state == State.Solved));
                                                super.setState(state);
                                            }
                                        });
                                    }
                                    if (safeZSolved && hm instanceof ReferenceNozzle) {
                                        // try to find a shared Z axis nozzle
                                        ReferenceNozzle sharedZNozzle = null;
                                        for (Nozzle nozzle2 : head.getNozzles()) {
                                            if (nozzle2 instanceof ReferenceNozzle 
                                                    && nozzle2 != hm) {
                                                ReferenceNozzle refNozzle2 = (ReferenceNozzle) nozzle2;
                                                if (refNozzle2.isEnableDynamicSafeZ() 
                                                        && HeadSolutions.getRawAxis(machine, refNozzle2.getAxisZ()) == axisZ) {
                                                    sharedZNozzle = refNozzle2;
                                                    break;
                                                }
                                            }
                                        }
                                        dynamicSafeZSolution(solutions, (ReferenceNozzle)hm, sharedZNozzle, axisZ);
                                    }
                                }
                                catch (Exception e) {
                                    Logger.warn(e);
                                }
                            }
                        }
                    }
                }
            }

            // Cam Transform axes.
            for (Axis axis : machine.getAxes()) {
                if (axis instanceof ReferenceCamClockwiseAxis
                        && axis.getType() == Type.Z) {
                    ReferenceCamClockwiseAxis cam2Axis = (ReferenceCamClockwiseAxis) axis;
                    ReferenceCamCounterClockwiseAxis cam1Axis = cam2Axis.getCounterClockwiseAxis();
                    AbstractHeadMountable hm1x = null;
                    AbstractHeadMountable hm2x = null;
                    for (Head head : machine.getHeads()) {
                        for (HeadMountable hm : head.getHeadMountables()) {
                            if (hm.getAxisZ() == cam1Axis) {
                                hm1x = (AbstractHeadMountable) hm;
                            }
                            else if (hm.getAxisZ() == cam2Axis) {
                                hm2x = (AbstractHeadMountable) hm;
                            }
                        }
                    }
                    final AbstractHeadMountable hm1 = hm1x;
                    final AbstractHeadMountable hm2 = hm2x;
                    final Length oldOffsetZ1 = hm1 != null ? hm1.getHeadOffsets().getLengthZ() : null;
                    final Length oldOffsetZ2 = hm2 != null ? hm2.getHeadOffsets().getLengthZ() : null;
                    final Length oldCamWheelRadius = cam1Axis.getCamWheelRadius();
                    final Length oldCamWheelGap = cam1Axis.getCamWheelGap();
                    if (oldCamWheelRadius.getValue() != 0 || oldCamWheelGap.getValue() != 0) {
                        solutions.add(new Solutions.Issue(
                                cam1Axis, 
                                Translations.format("KinematicSolutions.Issue.DeprecatedCamTransform", cam1Axis.getName()), //$NON-NLS-1$
                                Translations.format("KinematicSolutions.Solution.DeprecatedCamTransform", cam1Axis.getName(), //$NON-NLS-1$
                                        (oldCamWheelRadius.getValue() != 0 ? Translations.getString("KinematicSolutions.Choice.DeprecatedCamTransform.WheelRadius") : "") //$NON-NLS-1$
                                        + (oldCamWheelGap.getValue() != 0 ? Translations.getString("KinematicSolutions.Choice.DeprecatedCamTransform.WheelGap") : "")), //$NON-NLS-1$
                                Solutions.Severity.Warning,
                                "https://github.com/openpnp/openpnp/wiki/Transformed-Axes#referencecamcounterclockwiseaxis") {

                            @Override 
                            public String getExtendedDescription() {
                                String wheelText = (oldCamWheelRadius.getValue() != 0 ? Translations.getString("KinematicSolutions.Choice.DeprecatedCamTransform.WheelRadius") : "") //$NON-NLS-1$
                                        + (oldCamWheelGap.getValue() != 0 ? Translations.getString("KinematicSolutions.Choice.DeprecatedCamTransform.WheelGap") : ""); //$NON-NLS-1$
                                String note = (hm1 != null || hm2 != null ?
                                        Translations.format("KinematicSolutions.ExtendedDescription.DeprecatedCamTransform.Note", //$NON-NLS-1$
                                                (hm1 != null ? hm1.getClass().getSimpleName()+" "+hm1.getName()+", " : "")
                                                + (hm2 != null ? hm2.getClass().getSimpleName()+" "+hm2.getName()+", " : ""))
                                        : "");
                                return Translations.format("KinematicSolutions.ExtendedDescription.DeprecatedCamTransform", //$NON-NLS-1$
                                        cam1Axis.getName(), wheelText, wheelText, note);
                            }

                            @Override
                            public Icon getExtendedIcon() {
                                return Icons.camAxisTransform ;
                            }

                            @Override
                            public void setState(Solutions.State state) throws Exception {
                                if (state == State.Solved) {
                                    cam1Axis.setCamWheelRadius(new Length(0, LengthUnit.Millimeters));
                                    cam1Axis.setCamWheelGap(new Length(0, LengthUnit.Millimeters));
                                    Location newOffsets1 = hm1.getHeadOffsets()
                                            .add(new Location(oldCamWheelRadius.getUnits(),
                                                    0,
                                                    0,
                                                    oldCamWheelRadius.add(oldCamWheelGap).getValue(),
                                                    0));
                                    Location newOffsets2 = hm2.getHeadOffsets()
                                            .add(new Location(oldCamWheelRadius.getUnits(),
                                                    0,
                                                    0,
                                                    oldCamWheelRadius.add(oldCamWheelGap).getValue(),
                                                    0));
                                    hm1.setHeadOffsets(newOffsets1);
                                    hm2.setHeadOffsets(newOffsets2);
                                }
                                else {
                                    cam1Axis.setCamWheelRadius(oldCamWheelRadius);
                                    cam1Axis.setCamWheelGap(oldCamWheelGap);
                                    Location newOffsets1 = hm1.getHeadOffsets();
                                    newOffsets1 = newOffsets1
                                            .derive(null,
                                                    null,
                                                    oldOffsetZ1.convertToUnits(newOffsets1.getUnits()).getValue(),
                                                    null);
                                    Location newOffsets2 = hm2.getHeadOffsets();
                                    newOffsets2 = newOffsets2
                                            .derive(null,
                                                    null,
                                                    oldOffsetZ2.convertToUnits(newOffsets2.getUnits()).getValue(),
                                                    null);
                                    hm1.setHeadOffsets(newOffsets1);
                                    hm2.setHeadOffsets(newOffsets2);
                                }
                                super.setState(state);
                            }
                        });
                    }
                }
            }

            // Soft-limits
            for (Axis axis : machine.getAxes()) {
                if (axis instanceof ReferenceControllerAxis 
                        && axis.getType() != Type.Rotation) {
                    ReferenceControllerAxis controllerAxis = (ReferenceControllerAxis) axis;
                    HeadMountable hm = controllerAxis.getDefaultHeadMountable();
                    if (hm != null) {
                        final Length oldLimitLow = controllerAxis.getSoftLimitLow();
                        final Length oldLimitHigh = controllerAxis.getSoftLimitHigh();  
                        for (boolean limitLow : new Boolean[] {true, false}) {
                            String qualifier = limitLow ? Translations.getString("KinematicSolutions.Choice.SoftLimit.Low") : Translations.getString("KinematicSolutions.Choice.SoftLimit.High"); //$NON-NLS-1$ //$NON-NLS-2$
                            if (!(limitLow ? controllerAxis.isSoftLimitLowEnabled() : controllerAxis.isSoftLimitHighEnabled())) {
                                solutions.add(new Solutions.Issue(
                                        controllerAxis, 
                                        Translations.format("KinematicSolutions.Issue.SoftLimit", qualifier, controllerAxis.getName()), //$NON-NLS-1$
                                        Translations.format("KinematicSolutions.Solution.SoftLimit", controllerAxis.getName(), qualifier), //$NON-NLS-1$
                                        Solutions.Severity.Suggestion,
                                        "https://github.com/openpnp/openpnp/wiki/Kinematic-Solutions#capture-soft-limits") {

                                    @Override 
                                    public void activate() throws Exception {
                                        MainFrame.get().getMachineControls().setSelectedTool(hm);
                                    }

                                    @Override 
                                    public String getExtendedDescription() {
                                        return Translations.format("KinematicSolutions.ExtendedDescription.SoftLimit", //$NON-NLS-1$
                                                controllerAxis.getName(), qualifier,
                                                controllerAxis.getType().getDefaultLetter(),
                                                hm.getClass().getSimpleName(), hm.getName());
                                    }

                                    @Override
                                    public Icon getExtendedIcon() {
                                        return limitLow ? Icons.captureAxisLow : Icons.captureAxisHigh;
                                    }

                                    @Override
                                    public void setState(Solutions.State state) throws Exception {
                                        Length newLimit = controllerAxis.getDriverLengthCoordinate();
                                        if (limitLow) {
                                            controllerAxis.setSoftLimitLow((state == State.Solved) ? newLimit : oldLimitLow);
                                            controllerAxis.setSoftLimitLowEnabled((state == State.Solved));
                                        }
                                        else {
                                            controllerAxis.setSoftLimitHigh((state == State.Solved) ? newLimit : oldLimitHigh);
                                            controllerAxis.setSoftLimitHighEnabled((state == State.Solved));
                                        }
                                        super.setState(state);
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isSharedAxis(Head head, HeadMountable hm, ReferenceControllerAxis axisZ) {
        for (HeadMountable hm2 : head.getHeadMountables()) {
            if (hm2 != hm && HeadSolutions.getRawAxis(machine, hm2.getAxisZ()) == axisZ) {
                return true;
            }
        }
        return false;
    }


    protected void dynamicSafeZSolution(Solutions solutions, ReferenceNozzle nozzle, ReferenceNozzle nozzle2, ReferenceControllerAxis rawAxisZ) 
            throws Exception {
        Length [] zoneZ = nozzle.getSafeZZone();
        if (nozzle.isEnableDynamicSafeZ() 
                && zoneZ[0] != null && zoneZ[1] != null) {
            Length zone = zoneZ[1].subtract(zoneZ[0]);
            LengthConverter lengthConverter = new LengthConverter();
            for (NozzleTip nt : nozzle.getCompatibleNozzleTips()) {
                if (nt instanceof ReferenceNozzleTip) {
                    // Check the maximum part height on a nozzle tip to the Safe Z zone.
                    Length maxHeight = ((ReferenceNozzleTip) nt).getMaxPartHeight();
                    Location z0 = new Location(AxesLocation.getUnits(), 
                            0, 0, nozzle.getSafeZ().convertToUnits(AxesLocation.getUnits()).getValue(), 0);
                    Location z1 = new Location(AxesLocation.getUnits(), 
                            0, 0, nozzle.getSafeZ().add(maxHeight).convertToUnits(AxesLocation.getUnits()).getValue(), 0);
                    AxesLocation az0 = nozzle.toRaw(nozzle.toHeadLocation(z0));
                    AxesLocation az1 = nozzle.toRaw(nozzle.toHeadLocation(z1));
                    int signum1 = (int) Math.signum(az0.motionSegmentTo(az1).getCoordinate(rawAxisZ));
                    AxesLocation az2 = new AxesLocation(rawAxisZ, 
                            signum1 > 0 ? rawAxisZ.getSafeZoneHigh() : rawAxisZ.getSafeZoneLow());
                    int signum2 = (int) Math.signum(az1.motionSegmentTo(az2).getCoordinate(rawAxisZ));
                    if (signum2*signum1 == -1) {
                        solutions.add(new Solutions.PlainIssue(
                                nt, 
                                Translations.format("KinematicSolutions.Issue.SafeZZoneViolation", nozzle.getName(), nt.getName()), //$NON-NLS-1$
                                Translations.format("KinematicSolutions.Solution.SafeZZoneViolation", rawAxisZ.getName()), //$NON-NLS-1$
                                Severity.Error,
                                "https://github.com/openpnp/openpnp/wiki/Kinematic-Solutions#dynamic-safe-z-zone") {

                                    @Override
                                    public String getExtendedDescription() {
                                        return Translations.format("KinematicSolutions.ExtendedDescription.SafeZZoneViolation", //$NON-NLS-1$
                                                nozzle.getName(), nt.getName(),
                                                lengthConverter.convertForward(maxHeight),
                                                lengthConverter.convertForward(zone),
                                                rawAxisZ.getName(),
                                                lengthConverter.convertForward(rawAxisZ.getSafeZoneLow()),
                                                lengthConverter.convertForward(rawAxisZ.getSafeZoneHigh()));
                                    }
                                    @Override
                                    public Icon getExtendedIcon() {
                                        return Icons.safeZDynamic;
                                    }
                        });
                    }
                    else if (nozzle2 != null) { 
                        // Check combined nozzle tip max part heights too.
                        ReferenceNozzleTip nt2 = getCompatibleNozzleTipMaxPartHeight(nozzle2, nt);
                        if (nt2 != null) {
                            Length maxHeight2 = nt2.getMaxPartHeight();
                            Location z3 = new Location(AxesLocation.getUnits(), 
                                    0, 0, nozzle2.getSafeZ().add(maxHeight2).convertToUnits(AxesLocation.getUnits()).getValue(), 0);
                            AxesLocation az3 = nozzle2.toRaw(nozzle2.toHeadLocation(z3));
                            int signum3 = (int) Math.signum(az1.motionSegmentTo(az3).getCoordinate(rawAxisZ));
                            if (signum3*signum1 == -1) {
                                solutions.add(new Solutions.PlainIssue(
                                        nt, 
                                        Translations.format("KinematicSolutions.Issue.SafeZZoneViolationCombined", nozzle.getName(), nt.getName(), nozzle2.getName(), nt2.getName()), //$NON-NLS-1$
                                        Translations.format("KinematicSolutions.Solution.SafeZZoneViolationCombined", rawAxisZ.getName()), //$NON-NLS-1$
                                        Severity.Warning,
                                        "https://github.com/openpnp/openpnp/wiki/Kinematic-Solutions#dynamic-safe-z-zone") {

                                    @Override
                                    public String getExtendedDescription() {
                                        return Translations.format("KinematicSolutions.ExtendedDescription.SafeZZoneViolationCombined", //$NON-NLS-1$
                                                nozzle.getName(), nt.getName(),
                                                lengthConverter.convertForward(maxHeight),
                                                nozzle2.getName(), nt2.getName(),
                                                lengthConverter.convertForward(maxHeight2),
                                                lengthConverter.convertForward(zone),
                                                rawAxisZ.getName(),
                                                lengthConverter.convertForward(rawAxisZ.getSafeZoneLow()),
                                                lengthConverter.convertForward(rawAxisZ.getSafeZoneHigh()),
                                                nozzle2.getName());
                                    }
                                    @Override
                                    public Icon getExtendedIcon() {
                                        return Icons.safeZDynamic;
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }
    }

    protected ReferenceNozzleTip getCompatibleNozzleTipMaxPartHeight(ReferenceNozzle nozzle, NozzleTip nt1) {
        ReferenceNozzleTip ntMax = null;
        for (NozzleTip nt : nozzle.getCompatibleNozzleTips()) {
            if (nt instanceof ReferenceNozzleTip
                    && nt1 != nt) {
                ReferenceNozzleTip ntRef = (ReferenceNozzleTip) nt;
                if (ntMax == null 
                        || ntMax.getMaxPartHeight().compareTo(ntRef.getMaxPartHeight()) < 0) {
                    ntMax = ntRef;
                }
            }
        }
        return ntMax;
    }

}
