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

import org.openpnp.gui.MainFrame;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis.BacklashCompensationMethod;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.State;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.Camera;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

/**
 * This helper class implements the Issues & Solutions for the Calibration Milestone. 
 */
public class CalibrationSolutions implements Solutions.Subject {

    @Attribute(required = false)
    private int backlashCalibrationPasses = 3;

    @Attribute(required = false)
    private double errorDampening = 0.9;

    @Attribute(required = false)
    private double backlashTestMoveMm = 20.0;

    @Attribute(required = false)
    private double oneSidedBacklashSafetyFactor = 1.1;


    public CalibrationSolutions setMachine(ReferenceMachine machine) {
        this.machine = machine;
        return this;
    }

    private ReferenceMachine machine;


    @Override
    public void findIssues(Solutions solutions) {
        if (solutions.isTargeting(Milestone.Calibration)) {
            for (Head h : machine.getHeads()) {
                if (h instanceof ReferenceHead) {
                    ReferenceHead head = (ReferenceHead) h;
                    Camera defaultCamera = null;
                    try {
                        defaultCamera = head.getDefaultCamera();
                    }
                    catch (Exception e) {
                        // Ignore missing camera.
                    }
                    Nozzle defaultNozzle = null;
                    try {
                        defaultNozzle = head.getDefaultNozzle();
                    }
                    catch (Exception e1) {
                    }
                    if (defaultCamera != null) {
                        for (Camera camera : head.getCameras()) {
                            if (camera instanceof ReferenceCamera) {
                                perDownLookingCameraSolutions(solutions, head, defaultCamera, defaultNozzle, (ReferenceCamera) camera);
                            }
                        }
                        for (Nozzle nozzle : head.getNozzles()) {
                            if (nozzle instanceof ReferenceNozzle) {
                                perNozzleSolutions(solutions, head, defaultCamera, defaultNozzle, (ReferenceNozzle) nozzle);
                            }
                        }
                        perHeadSolutions(solutions, head, defaultCamera);
                    }
                }
            } 

            Camera defaultCamera = null;
            Nozzle defaultNozzle = null;
            try {
                defaultCamera = VisionUtils.getBottomVisionCamera();
                Head head = machine.getDefaultHead();
                defaultNozzle = head.getDefaultNozzle();
            }
            catch (Exception e1) {
            }
            if (defaultCamera != null && defaultNozzle  != null) {
                for (Camera camera : machine.getCameras()) {
                    if (camera instanceof ReferenceCamera) {
                        perUpLookingCameraSolutions(solutions, defaultCamera, defaultNozzle, (ReferenceCamera) camera);
                    }
                }
            }
        }
    }
    private void perHeadSolutions(Solutions solutions, ReferenceHead head, Camera defaultCamera) {
        // TODO Auto-generated method stub

    }
    private void perDownLookingCameraSolutions(Solutions solutions, ReferenceHead head,
            Camera defaultCamera, Nozzle defaultNozzle, ReferenceCamera camera) {
        // TODO Auto-generated method stub

        // Calibrate backlash.
        if (camera == defaultCamera) {
            CoordinateAxis rawAxisX = HeadSolutions.getRawAxis(machine, camera.getAxisX());
            CoordinateAxis rawAxisY = HeadSolutions.getRawAxis(machine, camera.getAxisY());
            for (CoordinateAxis rawAxis : new CoordinateAxis[] {rawAxisX, rawAxisY}) {
                if (rawAxis instanceof ReferenceControllerAxis) {
                    ReferenceControllerAxis axis = (ReferenceControllerAxis)rawAxis;
                    BacklashCompensationMethod oldMethod = axis.getBacklashCompensationMethod();
                    Length oldOffset = axis.getBacklashOffset();
                    double oldSpeed = axis.getBacklashSpeedFactor();

                    solutions.add(new Solutions.Issue(
                            camera, 
                            "Calibrate backlash compensation for axis "+axis.getName()+".", 
                            "Automatically calibrates the backlash compensation for "+axis.getName()+" using the primary calibration fiducial.", 
                            Solutions.Severity.Fundamental,
                            "https://github.com/openpnp/openpnp/wiki/Calibration-Solutions#calibrating-backlash-compensation") {

                        @Override 
                        public void activate() throws Exception {
                            MainFrame.get().getMachineControls().setSelectedTool(camera);
                            camera.ensureCameraVisible();
                        }

                        @Override 
                        public String getExtendedDescription() {
                            return "<html>"
                                    + "<p>Backlash compensation is used to avoid the effects of any looseness or play in the mechanical "
                                    + "linkages of machine axes. More information can be found in the Wiki (press the blue Info button).</p><br/>"
                                    + "<p><span color=\"red\">CAUTION</span>: The camera "+camera.getName()+" will move over the fiducial "
                                    + "and then perform a "+(backlashTestMoveMm*2)+" mm calibration motion pattern moving axis "
                                    + axis.getName()+".</p><br/>"
                                    + "<p>When ready, press Accept.</p>"
                                    + (getState() == State.Solved ? 
                                            "<br/><h4>Results:</h4>"
                                            + "<table>"
                                            + "<tr><td align=\"right\">Detected Backlash:</td>"
                                            + "<td>"+axis.getBacklashOffset()+"</td></tr>"
                                            + "<tr><td align=\"right\">Selected Method:</td>"
                                            + "<td>"+axis.getBacklashCompensationMethod().toString()+"</td></tr>"
                                            + "<tr><td align=\"right\">Speed Factor:</td>"
                                            + "<td>"+axis.getBacklashSpeedFactor()+"</td></tr>"
                                            + "<tr><td align=\"right\">Applicable Tolerance:</td>"
                                            + "<td>"+String.format("%.4f", getAxisCalibrationTolerance(camera, axis))+" mm</td></tr>"
                                            + "</table>" 
                                            : "")
                                    + "</html>";
                        }

                        @Override
                        public void setState(Solutions.State state) throws Exception {
                            if (state == State.Solved) {
                                final State oldState = getState();
                                UiUtils.submitUiMachineTask(
                                        () -> {
                                            calibrateAxisBacklash(head, camera, camera, axis);
                                            return true;
                                        },
                                        (result) -> {
                                            UiUtils.messageBoxOnException(() -> super.setState(state));
                                            // Persist this solved state.
                                            solutions.setSolutionsIssueSolved(this, true);
                                        },
                                        (t) -> {
                                            UiUtils.showError(t);
                                            // restore old state
                                            UiUtils.messageBoxOnException(() -> setState(oldState));
                                        });
                            }
                            else {
                                axis.setBacklashCompensationMethod(oldMethod);
                                axis.setBacklashOffset(oldOffset);
                                axis.setBacklashSpeedFactor(oldSpeed);
                                // Persist this unsolved state.
                                solutions.setSolutionsIssueSolved(this, false);
                                super.setState(state);
                            }
                        }
                    });
                }
            }
        }
    }

    private void perNozzleSolutions(Solutions solutions, ReferenceHead head, Camera defaultCamera,
            Nozzle defaultNozzle, ReferenceNozzle nozzle) {
        // TODO Auto-generated method stub

    }

    private void perUpLookingCameraSolutions(Solutions solutions, Camera defaultCamera,
            Nozzle defaultNozzle, ReferenceCamera camera) {
        // TODO Auto-generated method stub

    }

    private void calibrateAxisBacklash(ReferenceHead head, ReferenceCamera camera,
            HeadMountable movable, ReferenceControllerAxis axis) throws Exception {
        // Use the primary calibration fiducial for calibration.
        Location location = head.getCalibrationPrimaryFiducialLocation();
        Length fiducialDiameter = head.getCalibrationPrimaryFiducialDiameter();
        // General note: We always use mm.
        // Calculate the unit vector for the axis in both logical and axis coordinates. 
        Location unit = new Location(LengthUnit.Millimeters, 
                (axis.getType() == Type.X ? 1 : 0), 
                (axis.getType() == Type.Y ? 1 : 0),
                0, 0);
        AxesLocation axesLocation0 = movable.toRaw(location);
        AxesLocation axesLocation1 = movable.toRaw(location.add(unit));
        double mmAxis = axesLocation1.getCoordinate(axis) - axesLocation0.getCoordinate(axis); 

        double toleranceMm = getAxisCalibrationTolerance(camera, axis);

        // Determine the needed backlash compensation at various speed factors. 
        double[] speeds = new double [] { 0.25, 0.5, 0.75, 1 };
        double[] backlashOffsetBySpeed = new double [speeds.length];
        int iSpeed = 0;
        for (double speed : speeds) {
            // Reset the config.
            axis.setBacklashCompensationMethod(BacklashCompensationMethod.DirectionalCompensation);
            axis.setBacklashOffset(new Length(0, LengthUnit.Millimeters));
            axis.setBacklashSpeedFactor(1.0);
            // Find the right backlash offset.
            double offsetMm = 0;
            for (int pass = 0; pass < backlashCalibrationPasses; pass++) {
                // Approach from minus.
                MovableUtils.moveToLocationAtSafeZ(movable, displacedAxisLocation(movable, axis, location, -backlashTestMoveMm*mmAxis));
                movable.moveTo(location, speed);
                Location effective0 = machine.getVisualSolutions().getDetectedLocation(camera, camera, 
                        location, fiducialDiameter, 0);

                // Approach from plus.
                MovableUtils.moveToLocationAtSafeZ(movable, displacedAxisLocation(movable, axis, location, backlashTestMoveMm*mmAxis));
                movable.moveTo(location, speed);
                Location effective1 = machine.getVisualSolutions().getDetectedLocation(camera, camera, 
                        location, fiducialDiameter, 0);

                double mmError = effective1.subtract(effective0).dotProduct(unit).getValue();
                if (movable == camera) {
                    // If the camera moves (and not the subject) then the subject's 
                    // displacement is negative.
                    mmError = -mmError;
                }
                offsetMm += mmError*mmAxis*errorDampening;
                if (pass == 0 && mmError <= -toleranceMm) {
                    // Overshoot - cannot compensate.
                    break;
                }
                axis.setBacklashOffset(new Length(offsetMm, LengthUnit.Millimeters)
                        .convertToUnits(axis.getDriver().getUnits()));
                if (Math.abs(mmError) < toleranceMm) {
                    break;
                }
            }
            // Record this for the speed. 
            backlashOffsetBySpeed[iSpeed++] = offsetMm;
        }
        // Determine consistency over speed.
        int consistent = 0;
        double offsetMmSum = 0;
        for (double offsetMm : backlashOffsetBySpeed) {
            if (offsetMm <= -toleranceMm
                    || Math.abs(offsetMm - backlashOffsetBySpeed[0]) > toleranceMm) {
                // inconsistent
                break;
            }
            offsetMmSum += offsetMm;
            consistent++;
        }
        double offsetMmAvg = offsetMmSum/consistent;
        double offsetMmMax = 0;
        iSpeed = 0;
        for (double offsetMm : backlashOffsetBySpeed) {
            offsetMmMax = Math.max(offsetMmMax, Math.abs(offsetMm));
            Logger.debug("Axis "+axis.getName()+" backlash offsets at speed factor "+speeds[iSpeed++]+" is "+offsetMm);
        }
        Logger.debug("Axis "+axis.getName()+" backlash offsets analysis, consistent: "+consistent+", avg offset: "+offsetMmAvg+", max offset: "+offsetMmMax);
        // Set the backlash method according to consistency.
        if (consistent == speeds.length) {
            // We got consistent backlash over all the speeds.
            if (offsetMmAvg < toleranceMm) {
                // Smaller than resolution, no need for compensation.
                axis.setBacklashCompensationMethod(BacklashCompensationMethod.None);
            }
            else {
                // Consistent over speed, can be compensated by directional method. 
                axis.setBacklashCompensationMethod(BacklashCompensationMethod.DirectionalCompensation);
            }
            axis.setBacklashOffset(new Length(offsetMmAvg, LengthUnit.Millimeters));
            axis.setBacklashSpeedFactor(speeds[consistent-1]);
        }
        else if (consistent > 0) {
            // Not consistent over speed.
            axis.setBacklashCompensationMethod(BacklashCompensationMethod.OneSidedPositioning);
            axis.setBacklashOffset(new Length(offsetMmMax*oneSidedBacklashSafetyFactor, LengthUnit.Millimeters));
            axis.setBacklashSpeedFactor(speeds[consistent-1]);
        }
        else {
            throw new Exception("Axis "+axis.getName()+" seems to overshoot, even at the lowest speed factor. "
                    + "Make sure OpenPnP has effective acceleration/jerk control. "
                    + "Automatic compensation not possible.");
        }
    }
    private double getAxisCalibrationTolerance(ReferenceCamera camera,
            ReferenceControllerAxis axis) {
        // Get the axis resolution, but it might still be at the default 0.0001.
        Length resolution = new Length(axis.getResolution(), axis.getDriver().getUnits());
        // Get a minimal pixel step size. 
        Length pixelStep = (axis.getType() == Type.X  
                ? camera.getUnitsPerPixel().getLengthX() 
                        : camera.getUnitsPerPixel().getLengthY()).multiply(1.1);
        if (pixelStep.compareTo(resolution) > 0) {
            // If the pixel step is coarser than the set axis resolution or if 
            // the axis resolution is not (yet) set properly, take the pixel step. 
            resolution = pixelStep;
        }
        resolution = resolution.convertToUnits(LengthUnit.Millimeters);
        return resolution.getValue();
    }

    private Location displacedAxisLocation(HeadMountable movable, ReferenceControllerAxis axis,
            Location location, double displacement) throws Exception {
        AxesLocation axesLocation = movable.toRaw(location);
        axesLocation = axesLocation.add(new AxesLocation(axis, displacement));
        Location newLocation = movable.toTransformed(axesLocation);
        return newLocation;
    }

}