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

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.opencv.core.Size;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.processes.CalibrateCameraProcess;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferenceNozzleTipCalibration.BackgroundCalibrationMethod;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis.BacklashCompensationMethod;
import org.openpnp.machine.reference.camera.ImageCamera;
import org.openpnp.machine.reference.camera.ReferenceCamera;
import org.openpnp.machine.reference.camera.calibration.AdvancedCalibration;
import org.openpnp.machine.reference.feeder.ReferenceTubeFeeder;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.State;
import org.openpnp.model.Triangle;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.Camera;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.base.AbstractHead.VisualHomingMethod;
import org.openpnp.util.Collect;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.NanosecondTime;
import org.openpnp.util.SimpleGraph;
import org.openpnp.util.UiUtils;
import org.openpnp.util.Utils2D;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvStage.Result.Circle;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
 * This helper class implements the Issues & Solutions for the Calibration Milestone. 
 */
public class GeometrySolutions implements Solutions.Subject {

    @Element(required = false)
    private Length fiducialDiameter = null;

    private Location[][] locations = new Location[2][3];
    
    private double maxCameraRelativeSubjectDiameter = 0.7;
    
    public GeometrySolutions setMachine(ReferenceMachine machine) {
        this.machine = machine;
        return this;
    }

    private ReferenceMachine machine;
    protected double skewAngle = 0.0;
    protected double skewCoefficient = 0.0;

    @Override
    public void findIssues(Solutions solutions) {
        if (solutions.isTargeting(Milestone.Geometry)) {
            for (Head h : machine.getHeads()) {
                if (h instanceof ReferenceHead) {
                    ReferenceHead head = (ReferenceHead) h;
                    ReferenceCamera defaultCamera = null;
                    if (fiducialDiameter == null) {
                        fiducialDiameter = head.getCalibrationPrimaryFiducialDiameter();
                    }
                    try {
                        defaultCamera = (ReferenceCamera)head.getDefaultCamera();
                    }
                    catch (Exception e) {
                        // Ignore missing camera.
                    }
                    if (defaultCamera != null) {
                        perHeadSolutions(solutions, head, defaultCamera);
                    }
                }
            }
        }
    }

    private void perHeadSolutions(Solutions solutions, ReferenceHead head, ReferenceCamera camera) {

        VisionSolutions visionSolutions = machine.getVisionSolutions();
        if (visionSolutions.isSolvedPrimaryXY(head)) {
            //Squareness calibration
            CoordinateAxis rawAxisX = HeadSolutions.getRawAxis(machine, camera.getAxisX());
            CoordinateAxis rawAxisY = HeadSolutions.getRawAxis(machine, camera.getAxisY());
            
            solutions.add(new Solutions.Issue(
                    camera, 
                    "Calibrate machine squareness", 
                    "Automatically calibrates machine squareness using a reference shape", 
                    Solutions.Severity.Fundamental,
                    "https://github.com/openpnp") {

                {
                    tolerance = new Length(0.25, LengthUnit.Millimeters);
                }
                
                private Length tolerance;
                
                @Override 
                public void activate() throws Exception {
                    MainFrame.get().getMachineControls().setSelectedTool(camera);
                    camera.ensureCameraVisible();
                    checkCaptureComplete();
                    calibrateSquareness();
                }

                @Override 
                public String getExtendedDescription() {
                    return "<html>"
                            + "<p>Squareness compensation calibrates for misalignment between x and y axis. "
                            + "linkages of machine axes. More information can be found in the Wiki (press the blue Info button below).</p><br/>"
                            + "<p><strong color=\"red\">CAUTION 1</strong>: The camera "+camera.getName()+" will move over the primary fiducial "
                            + "and then perform a calibration motion pattern, moving the axis "+" over its full soft-limit range.</p><br/>"
                            + "<p>When ready, press Accept.</p>"
//                            + 
//                                    "<br/><h4>Results:</h4>"
//                                    + "<table>"
//                                    + "<tr><td align=\"right\">Current rotation:" + String.valueOf(currentRotation) + "</td>"
//                                    + "<tr><td align=\"right\">Current position:" + String.valueOf(currentPosition) + "</td>"
//                                    + "</table>" 
                            + (getState() == State.Solved ? 
                                    "<br/><h4>Results:</h4>"
                                    + "<table>"
                                    + "<tr><td align=\"right\">Skew angle:" + String.valueOf(skewAngle) + "deg</td>"
                                    + "<tr><td align=\"right\">Skew coefficient:" + String.valueOf(skewCoefficient) + "deg</td>"
                                    + "</table>" 
                                    : "")
                            + "</html>";
                }

                @Override
                public Solutions.Issue.CustomProperty[] getProperties() {
                    int maxDiameter = (int)(Math.min(camera.getWidth(), camera.getHeight())*maxCameraRelativeSubjectDiameter);
                    Solutions.Issue.CustomProperty[] propsSuper = super.getProperties();
                    
                    Solutions.Issue.CustomProperty[] propsItems = new Solutions.Issue.CustomProperty[] {
                            
                            new Solutions.Issue.LengthProperty(
                                    "Fiducial diameter",
                                    "Adjust the fiducial diameter that should be detected.") {
                                @Override
                                public Length get() {
                                    return fiducialDiameter;
                                }
                                @Override
                                public void set(Length value) {
                                    fiducialDiameter = value;
//                                    try {
//                                        UiUtils.submitUiMachineTask(() -> {
//                                            try {
//                                                // This show a diagnostic detection image in the camera view.
//                                                getSubjectPixelLocation(camera, null, new Circle(0, 0, value), 0.05, 
//                                                        "Diameter "+(int)value+" px - Score {score} ", null, true);
//                                            }
//                                            catch (Exception e) {
//                                                Toolkit.getDefaultToolkit().beep();
//                                            }
//                                        }).get();
//                                    }
//                                    catch (InterruptedException | ExecutionException e) {
//                                        Logger.warn(e);
//                                    }
                                }


                            },
                            new Solutions.Issue.ActionProperty( 
                                    "", "Capture location for rotation 1 and position 1") {
                                @Override
                                public Action get() {
                                    return new AbstractAction("Capture rotation 1 point 1", Icons.captureCamera) {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            UiUtils.submitUiMachineTask(() -> {
                                                captureLocation(camera, 0, 0);
                                            });
                                        }
                                    };
                                }
                            },
                            new Solutions.Issue.ActionProperty( 
                                    "", "Capture location for rotation 1 and position 2") {
                                @Override
                                public Action get() {
                                    return new AbstractAction("Capture rotation 1 point 2", Icons.captureCamera) {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            UiUtils.submitUiMachineTask(() -> {
                                                captureLocation(camera, 0, 1);
                                            });
                                        }
                                    };
                                }
                            },
                            new Solutions.Issue.ActionProperty( 
                                    "", "Capture location for rotation 1 and position 3") {
                                @Override
                                public Action get() {
                                    return new AbstractAction("Capture rotation 1 point 3", Icons.captureCamera) {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            UiUtils.submitUiMachineTask(() -> {
                                                captureLocation(camera, 0, 2);
                                            });
                                        }
                                    };
                                }
                            },
                            new Solutions.Issue.ActionProperty( 
                                    "", "Capture location for rotation 2 and position 1") {
                                @Override
                                public Action get() {
                                    return new AbstractAction("Capture rotation 2 point 1", Icons.captureCamera) {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            UiUtils.submitUiMachineTask(() -> {
                                                captureLocation(camera, 1, 0);
                                            });
                                        }
                                    };
                                }
                            },
                            new Solutions.Issue.ActionProperty( 
                                    "", "Capture location for rotation 2 and position 2") {
                                @Override
                                public Action get() {
                                    return new AbstractAction("Capture rotation 2 point 2", Icons.captureCamera) {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            UiUtils.submitUiMachineTask(() -> {
                                                captureLocation(camera, 1, 1);
                                            });
                                        }
                                    };
                                }
                            },
                            new Solutions.Issue.ActionProperty( 
                                    "", "Capture location for rotation 2 and position 3") {
                                @Override
                                public Action get() {
                                    return new AbstractAction("Capture rotation 12 point 3", Icons.captureCamera) {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            UiUtils.submitUiMachineTask(() -> {
                                                captureLocation(camera, 1, 2);
                                            });
                                        }
                                    };
                                }
                            },
                            
                    };
                    return Collect.concat(propsItems, propsSuper);
                }
                
                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (state == State.Solved) {
                        if (! visionSolutions.isSolvedPrimaryXY(head)) {
                            throw new Exception("The head "+head.getName()+" primary fiducial location X and Y must be set first.");
                        }

                        final State oldState = getState();
                        UiUtils.submitUiMachineTask(
                                () -> {
                                    calibrateSquareness(head, camera, camera);
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
//                            axis.setBacklashCompensationMethod(oldMethod);
//                            axis.setBacklashOffset(oldOffset);
//                            axis.setSneakUpOffset(oldSneakUp);
//                            axis.setBacklashSpeedFactor(oldSpeed);
//                            axis.setAcceptableTolerance(oldAcceptableTolerance);
                        // Persist this unsolved state.
                        solutions.setSolutionsIssueSolved(this, false);
                        super.setState(state);
                    }
                }
            });
                
            if (visionSolutions.isSolvedPrimaryZ(head) // In addition to XY checked in the outer if()
                    && visionSolutions.isSolvedSecondaryXY(head)
                    && visionSolutions.isSolvedSecondaryZ(head)
                    && !camera.getAdvancedCalibration().isOverridingOldTransformsAndDistortionCorrectionSettings()) {
                solutions.add(new Solutions.Issue(
                        camera, 
                        "Advanced camera "+camera.getName()+" calibration.", 
                        "Automatically calibrates the camera "+camera.getName()+" using the primary and secondary calibration fiducials.", 
                        Solutions.Severity.Suggestion,
                        "https://github.com/openpnp/openpnp/wiki/Calibration-Solutions#advanced-camera-calibration") {

                    @Override 
                    public void activate() throws Exception {
//                        MainFrame.get().getMachineControls().setSelectedTool(camera);
//                        camera.ensureCameraVisible();
//                            checkCaptureComplete();
//                        checkCaptureComplete();
//                        calibrateSquareness();
                    }

                    @Override 
                    public String getExtendedDescription() {
                        return "<html>"
                                + "<p>You already performed the preliminary camera calibration earlier, now it is time for the "
                                + "<strong>Advanced Camera Calibration</strong> that includes compensating lens distortion and camera mounting tilt. "
                                + "A more profound and precise 3D Units per Pixel calibration is also applied.</p><br/>"
                                + "<p>More information can be found in the Wiki (press the blue Info button below).</p><br/>"
                                + "<p>The calibration must be performed with the same calibration rig, that you used for the "
                                + "preliminary calibration. Make sure it is ready, and locations (including Z) are still valid.</p><br/>"
                                + "<p>If not, please revisit the <strong>Primary calibration fiducial position</strong> and "
                                + "<strong>Secondary calibration fiducial position</strong> steps first (Enable the "
                                + "<strong>Include Solved?</strong> checkbox above, to see revisitable solutions).</p><br/>"
                                + "<p><strong color=\"red\">CAUTION</strong>: The camera "+camera.getName()+" will move over the "
                                + "calibration rig and perform a length calibration motion pattern.</p><br/>" 
                                + "</html>";
                    }

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        if (state == State.Solved) {
                        }
                        else {
                        }
                        super.setState(state);
                    }
                });
            }
        }
    }
    
    public void captureLocation(ReferenceCamera camera, int rot, int pt) {
        Location loc = null;
        try {
            loc = findFiducial(fiducialDiameter, camera, camera);
        }
        catch (Exception e){
            loc = camera.getLocation();
        }
        locations[rot][pt] = loc;
    }

    protected Location findFiducial(Length fiducialDiameter , ReferenceCamera camera,
            HeadMountable movable) throws Exception {
        Location location = machine.getVisionSolutions()
                .centerInOnSubjectLocation(camera, movable,
                        fiducialDiameter, "Machine squareness - Triangle point capture", false);
        return location;
    }
    
    protected boolean checkCaptureComplete() throws Exception {
        for (int rot=0; rot<2; rot++) {
            for(int pt=0; pt<3; pt++) {
                if (locations[rot][pt] == null || !locations[rot][pt].isInitialized()) {
                    throw new Exception("Rotation" + String.valueOf(rot+1) + " Position:" + String.valueOf(pt+1) + "is not set");
                }
            }
        }
        return true;
    }

    public void calibrateSquareness() throws Exception {
      Triangle rot1 = new Triangle(locations[0][0], locations[0][1], locations[0][2]);
      Triangle rot2 = new Triangle(locations[1][0], locations[1][1], locations[1][2]);
      skewAngle= Utils2D.squarenessFromRotatedTriangles(rot1, rot2);
      skewCoefficient = Math.sin(Math.toRadians(skewAngle)); 
    }
    
    public void calibrateSquareness(ReferenceHead head, ReferenceCamera camera,
        HeadMountable movable) throws Exception {
        // Check pre-conditions (this method can be called from outside Issues & Solutions).
//        if (! head.getCalibrationPrimaryFiducialLocation().isInitialized()) {
//            throw new Exception("Head "+head.getName()+" primary fiducial location must be set for backlash calibration.");
//        }
//        if (head.getCalibrationPrimaryFiducialDiameter() == null 
//                || ! head.getCalibrationPrimaryFiducialDiameter().isInitialized()) {
//            throw new Exception("Head "+head.getName()+" primary fiducial diameter must be set for backlash calibration.");
//        }
        

        
//        Triangle rot1 = new Triangle(rot1pt1, rot1pt2, rot1pt3);
//        Triangle rot2 = new Triangle(rot2pt1, rot2pt2, rot2pt3);

//        double skew = Utils2D.squarenessFromRotatedTriangles(rot1, rot2);
//        // Use the primary calibration fiducial for calibration.
//        Location location = head.getCalibrationPrimaryFiducialLocation();
//        Length fiducialDiameter = head.getCalibrationPrimaryFiducialDiameter();
//        MovableUtils.moveToLocationAtSafeZ(movable, location);
//        location = machine.getVisionSolutions()
//                .centerInOnSubjectLocation(camera, movable,
//                        fiducialDiameter, "Backlash Calibration Speed Control Test", false);

////        // Measure times used for same distance at different speeds.
////        MovableUtils.moveToLocationAtSafeZ(movable, location);
////        movable.waitForCompletion(CompletionType.WaitForStillstand);
////        Location timedLocation = displacedAxisLocation(movable, axis, location, -backlashTestMoveMm*mmAxis, false);
    //
    }

}
