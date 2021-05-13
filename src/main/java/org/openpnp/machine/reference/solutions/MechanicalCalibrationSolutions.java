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

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Issue;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.State;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.pmw.tinylog.Logger;

/**
 * This helper class implements the Issues & Solutions for the ReferenceHead. 
 * The idea is not to pollute the head implementation itself.
 *
 */
public class MechanicalCalibrationSolutions implements Solutions.Subject {
    private ReferenceMachine machine;

    public MechanicalCalibrationSolutions setMachine(ReferenceMachine machine) {
        this.machine = machine;
        return this;
    }

    @Override
    public void findIssues(Solutions solutions) {
        if (solutions.getTargetMilestone() == Milestone.Calibration) {
            // Dynamic Safe Z yes/no.
            boolean okDynamicSafeZ = true;
            for (Head head : machine.getHeads()) {
                for (Nozzle nozzle : head.getNozzles()) {
                    if (nozzle instanceof ReferenceNozzle) {
                        final ReferenceNozzle refNozzle = (ReferenceNozzle) nozzle;
                        final boolean oldDynamicSafeZ = refNozzle.isEnableDynamicSafeZ();
                        Issue issue = new Solutions.Issue(
                                nozzle, 
                                "Dynamic Safe Z for "+nozzle.getName()+".", 
                                "Decide whether "+nozzle.getName()+" has dynamic Safe Z or not.", 
                                Solutions.Severity.Fundamental,
                                "https://github.com/openpnp/openpnp/wiki/Setup-and-Calibration%3A-Nozzle-Setup") {
                            {
                                setChoice(oldDynamicSafeZ);
                            }
                            @Override
                            public Solutions.Issue.Choice[] getChoices() {
                                return new Solutions.Issue.Choice[] {
                                        new Solutions.Issue.Choice(true, 
                                                "<html><h3>Dynamic Safe Z</h3>"
                                                        + "<p>If a part is on the nozzle, the nozzle is lifted to Safe Z <strong>+</strong> part height.</p><br/>"
                                                        + "Safe Z must only account for the tallest obstacle.</p>"
                                                        + "</html>",
                                                        Icons.safeZDynamic),
                                        new Solutions.Issue.Choice(false, 
                                                "<html><h3>Fixed Safe Z</h3>"
                                                        + "<p>Safe Z is always at a fixed level.</p><br/>"
                                                        + " Safe Z must account for <em>both</em> the tallest "
                                                        + "obstacle <em>and</em> the tallest part on the nozzle.</p>"
                                                        + "</html>",
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

                                    solutions.add(new Solutions.Issue(
                                            hm, 
                                            "Calibrate Safe Z of "+hm.getName()+".", 
                                            "Jog "+hm.getName()+" over the tallest obstacle and capture.", 
                                            Solutions.Severity.Fundamental,
                                            "https://github.com/openpnp/openpnp/wiki/Machine-Axes#kinematic-settings--axis-limits") {

                                        @Override 
                                        public void activate() throws Exception {
                                            MainFrame.get().getMachineControls().setSelectedTool(hm);
                                        }

                                        @Override 
                                        public String getExtendedDescription() {
                                            return "<html>"
                                                    + "<p>Jog "+hm.getName()+" over the tallest obstacle on your machine.</p><br/>"
                                                    + "<p>Then lower it down so it  still has sufficient clearance"
                                                    + (partClearance ? " even with the tallest part on the nozzle" : "")
                                                    +".</p><br/>"
                                                    + "<p>Then press Accept to capture the Safe Z.</p>"
                                                    + "</html>";
                                        }

                                        @Override
                                        public Icon getExtendedIcon() {
                                            return partClearance ? Icons.safeZFixed : Icons.safeZCapture;
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
                                catch (Exception e) {
                                    Logger.warn(e);
                                }
                            }
                        }
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
                            String qualifier = limitLow ? "low side" : "high side";
                            if (!(limitLow ? controllerAxis.isSoftLimitLowEnabled() : controllerAxis.isSoftLimitHighEnabled())) {
                                solutions.add(new Solutions.Issue(
                                        controllerAxis, 
                                        "Calibrate the "+qualifier+" soft limit of "+controllerAxis.getName()+".", 
                                        "Move axis "+controllerAxis.getName()+" to the "+qualifier+" soft limit and capture.", 
                                        Solutions.Severity.Suggestion,
                                        "https://github.com/openpnp/openpnp/wiki/Machine-Axes#kinematic-settings--axis-limits") {

                                    @Override 
                                    public void activate() throws Exception {
                                        MainFrame.get().getMachineControls().setSelectedTool(hm);
                                    }

                                    @Override 
                                    public String getExtendedDescription() {
                                        return "<html>"
                                                + "<p>Move axis "+controllerAxis.getName()+" to the "+qualifier+" soft limit.</p><br/>"
                                                + "<p>Jog "+controllerAxis.getType().getDefaultLetter()+" of "
                                                + hm.getClass().getSimpleName()+" "+hm.getName()+" to do so.</p><br/>"
                                                + "<p>If the axis has a limit switch, use a position close to it but still safe "
                                                + "to not trigger the switch by accident.</p><br/>"
                                                + "<p>Then press Accept to capture the lower soft limit.</p>"
                                                + "</html>";
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
}
