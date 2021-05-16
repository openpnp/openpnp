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

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.State;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.base.AbstractHead.VisualHomingMethod;
import org.openpnp.util.LogUtils;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvStage.Result.Circle;
import org.openpnp.vision.pipeline.stages.DetectCircularSymmetry;
import org.openpnp.vision.pipeline.stages.DetectCircularSymmetry.ScoreRange;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
 * This helper class implements the Issues & Solutions for the ReferenceHead. 
 * The idea is not to pollute the head implementation itself.
 *
 */
public class VisionSolutions implements Solutions.Subject {

    // These values can be changed in the machine.xml but they are not exposed in the UI.  

    // See org.openpnp.vision.pipeline.stages.DetectCircularSymmetry.findCircularSymmetry for what these mean.
    @Attribute(required = false)
    private double minSymmetry = 1.2;
    @Attribute(required = false)
    private int subSampling = 4;
    @Attribute(required = false)
    protected long diagnosticsMilliseconds = 2000;

    /**
     * Maximum fiducial sensitive size, relative to the camera size (min of width and height)
     */
    @Attribute(required = false)
    private double maxCameraRelativeFiducialAreaDiameter = 0.5; 
    /**
     * The extra search range, relative to the camera size (min of width and height) when 
     * doing auto-calibration, not knowing anything about the camera.
     */
    @Attribute(required = false)
    private double zeroKnowledgeDisplacementRatio = 0.2;
    /**
     * How far the camera or camera subject should be moved to explore camera units per pixels, rotation 
     * and mirroring, before we know anything about the camera. The default value is thought to be 
     * universally covered by OpenPnP cameras, due to the nature of the application.
     */
    @Attribute(required = false)
    private double zeroKnowledgeDisplacementMm = 1;
    /**
     * How to perform one-sided backlash compensation, before we know anything about the machine. 
     */
    @Element(required = false)
    private Location zeroKnowledgeBacklashOffsets = new Location(LengthUnit.Millimeters, -1, -1, 0.0, -5);
    /**
     * How many times we drill down on fiducials, before we know anything about the machine. 
     */
    @Attribute(required = false)
    private int zeroKnowledgeFiducialLocatorPasses = 4;

    @Attribute(required = false)
    private double fiducialMargin = 1.4;

    public VisionSolutions setMachine(ReferenceMachine machine) {
        this.machine = machine;
        return this;
    }

    private ReferenceMachine machine;

    boolean solvedPrimaryXY = false;
    boolean solvedPrimaryZ = false;
    boolean solvedSecondaryXY = false;
    boolean solvedSecondaryZ = false;

    @Override
    public void findIssues(Solutions solutions) {
        if (solutions.isTargeting(Milestone.Vision)) {
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
                                perHeadCameraSolutions(solutions, head, defaultCamera, defaultNozzle, (ReferenceCamera) camera);
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

    public void perHeadCameraSolutions(Solutions solutions, ReferenceHead head, Camera defaultCamera, Nozzle defaultNozzle, ReferenceCamera camera) {

        // Find the base calibration fiducial.
        if (camera == defaultCamera) {
            final Location oldPrimaryFiducialLocation = head.getCalibrationPrimaryFiducialLocation();
            final CameraCalibrationState cameraCalibrationState = new CameraCalibrationState(camera); 
            solvedPrimaryXY = solutions.add(new Solutions.Issue(
                    camera, 
                    "Primary calibration fiducial position and initial camera calibration.", 
                    "Move the camera over the primary calibration fiducial and capture its position.", 
                    Solutions.Severity.Fundamental,
                    "https://github.com/openpnp/openpnp/wiki/Vision-Solutions#calibration-primary-fiducial") {

                @Override 
                public void activate() throws Exception {
                    MainFrame.get().getMachineControls().setSelectedTool(camera);
                    camera.ensureCameraVisible();
                }

                @Override 
                public String getExtendedDescription() {
                    return "<html>"
                            + "<p>Camera calibration can be performed automatically by looking at fiducials while moving the "
                            + "camera around in a certain pattern. This solution determines the X, Y position of the primary "
                            + "fiducial and it performs preliminary camera calibration.</p><br/>"
                            + "<p>Instructions for how to create and position the primary fiducial must be obtained in the OpenPnP "
                            + "Wiki. There are very important rules that must be observed, so don't miss it! Press the "
                            + "blue Info button to open the Wiki.</p><br/>"
                            + "<p>Once you have prepared the calibration primary fiducial you can capture its position "
                            + "in X, Y.</p><br/>"
                            + "<p>Jog camera " + camera.getName()
                            + " over the privary fiducial. Target it roughly with the cross-hairs.</p><br/>"
                            + "<p>Then press Accept to capture the position. The camera will perform a small calibration movement "
                            + "pattern</p>"
                            + "</html>";
                }

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (state == State.Solved) {
                        final State oldState = getState();
                        UiUtils.submitUiMachineTask(
                                () -> {
                                    // Perform preliminary camera calibration. 
                                    autoCalibrateCamera(camera, camera, null, diagnosticsMilliseconds);
                                    // Get the precise fiducial location.
                                    Location fiducialLocation = getSubjectLocation(camera, camera, diagnosticsMilliseconds);
                                    // Store it.
                                    head.setCalibrationPrimaryFiducialLocation(fiducialLocation);
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
                        head.setCalibrationPrimaryFiducialLocation(oldPrimaryFiducialLocation);
                        cameraCalibrationState.restoreTo(camera);
                        // Persist this unsolved state.
                        solutions.setSolutionsIssueSolved(this, false);
                        super.setState(state);
                    }
                }
            });

            final Location oldSecondaryFiducialLocation = head.getCalibrationSecondaryFiducialLocation();
            solvedSecondaryXY = solutions.add(new Solutions.Issue(
                    camera, 
                    "Secondary calibration fiducial position.", 
                    "Move the camera over the secondary calibration fiducial and capture its position.", 
                    Solutions.Severity.Fundamental,
                    "https://github.com/openpnp/openpnp/wiki/Vision-Solutions#calibration-secondary-fiducial") {

                @Override 
                public void activate() throws Exception {
                    MainFrame.get().getMachineControls().setSelectedTool(camera);
                    camera.ensureCameraVisible();
                }

                @Override 
                public String getExtendedDescription() {
                    return "<html>"
                            + "<p>Camera calibration also requires looking at a secondary fiducial at different "
                            + "Z level. This will provide the calibration algorithm with the needed 3D/spacial information to "
                            + "determine the true focal length of the lens and the optical position of the camera in space.</p><br/>"
                            + "<p>Instructions for how to create and position the secondary fiducial must be obtained in the OpenPnP "
                            + "Wiki. There are very important rules that must be observed, so don't miss it! Press the "
                            + "blue Info button to open the Wiki.</p><br/>"
                            + "<p>Once you have prepared the calibration secondary fiducial you can capture its position "
                            + "in X, Y.</p><br/>"
                            + "<p>Jog camera " + camera.getName()
                            + " over the secondary fiducial. Target it roughly with the cross-hairs.</p><br/>"
                            + "<p>Then press Accept to capture the position.</p>"
                            + "</html>";
                }

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (state == State.Solved) {
                        final State oldState = getState();
                        UiUtils.submitUiMachineTask(
                                () -> {
                                    // Set location as fiducial location.
                                    Location fiducialLocation = getSubjectLocation(camera, camera, diagnosticsMilliseconds);
                                    head.setCalibrationSecondaryFiducialLocation(fiducialLocation);
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
                        head.setCalibrationSecondaryFiducialLocation(oldSecondaryFiducialLocation);
                        // Persist this unsolved state.
                        solutions.setSolutionsIssueSolved(this, false);
                        super.setState(state);
                    }
                }
            });
        }
        else {
            // Not the default camera.
            final Location oldCameraOffsets = camera.getHeadOffsets();
            final CameraCalibrationState cameraCalibrationState = new CameraCalibrationState(camera); 
            solutions.add(new Solutions.Issue(
                    camera, 
                    "Determine the camera head offsets.", 
                    "Move the camera "+camera.getName()+" over the secondary calibration fiducial and capture its offsets.", 
                    Solutions.Severity.Fundamental,
                    "https://github.com/openpnp/openpnp/wiki/Vision-Solutions#down-looking-camera-offsets") {

                @Override 
                public void activate() throws Exception {
                    MainFrame.get().getMachineControls().setSelectedTool(camera);
                    camera.ensureCameraVisible();
                }

                @Override 
                public String getExtendedDescription() {
                    return "<html>"
                            + "<p>Once the calibration primary fiducial is captured in X, Y you can use it to capture the nozzle head "
                            + "offsets (first approximation).</p><br/>"
                            + "<p>Furthermore, this also captures the nozzle offset as a first approximation.</p><br/>"
                            + "<p>Jog nozzle " + camera.getName()
                            + " over the primary fiducial. Target it with the cross-hairs.</p><br/>"
                            + "<p>Then press Accept to capture the camera head offsets.</p>"
                            + "</html>";
                }

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (state == State.Solved) {
                        final State oldState = getState();
                        UiUtils.submitUiMachineTask(
                                () -> {
                                    // Reset any former head offset.
                                    camera.setHeadOffsets(Location.origin);
                                    // Perform preliminary camera calibration. 
                                    autoCalibrateCamera(camera, camera, null, diagnosticsMilliseconds);
                                    // Get the precise fiducial location.
                                    Location fiducialLocation = getSubjectLocation(camera, camera, diagnosticsMilliseconds);
                                    // Determine the camera head offset (remember, we reset the head offset to zero above, so 
                                    // the camera now shows the true offset).
                                    Location headOffsets = head.getCalibrationPrimaryFiducialLocation().subtract(fiducialLocation);
                                    camera.setHeadOffsets(headOffsets);
                                    Logger.info("Set camera "+camera.getName()+" head offsets to "+headOffsets+" (previously "+oldCameraOffsets+")");
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
                        // Restore the camera offset
                        camera.setHeadOffsets(oldCameraOffsets);
                        cameraCalibrationState.restoreTo(camera);
                        // Persist this unsolved state.
                        solutions.setSolutionsIssueSolved(this, false);
                        super.setState(state);
                    }
                }
            });
        }
    }

    private void perUpLookingCameraSolutions(Solutions solutions, 
            Camera defaultCamera, Nozzle defaultNozzle, ReferenceCamera camera) {
        final Location oldCameraOffsets = camera.getHeadOffsets();
        final CameraCalibrationState cameraCalibrationState = new CameraCalibrationState(camera); 
        solutions.add(new Solutions.Issue(
                camera, 
                "Determine the up-looking camera "+camera.getName()+" position.", 
                "Move the nozzle "+defaultNozzle.getName()+" over the up-looking camera "+camera.getName()+" and capture the position.", 
                Solutions.Severity.Fundamental,
                "https://github.com/openpnp/openpnp/wiki/Vision-Solutions#up-looking-camera-offsets") {

            private Length oldVisionDiameter;
            private int featureDiameter;

            {
                featureDiameter = 20;
                if (camera.getUnitsPerPixel().getX() != 0 
                        && defaultNozzle.getNozzleTip() instanceof ReferenceNozzleTip) {
                    ReferenceNozzleTip referenceNozzleTip = (ReferenceNozzleTip)defaultNozzle.getNozzleTip();
                    oldVisionDiameter = referenceNozzleTip.getCalibration().getCalibrationTipDiameter(); 
                    if (oldVisionDiameter.getValue() != 0) {
                        // Existing diameter setting.
                        featureDiameter = (int) Math.round(oldVisionDiameter.divide(camera.getUnitsPerPixel().getLengthX()));
                    }
                }
            }

            @Override 
            public void activate() throws Exception {
                MainFrame.get().getMachineControls().setSelectedTool(defaultNozzle);
                camera.ensureCameraVisible();
            }

            @Override 
            public String getExtendedDescription() {
                return "<html>"
                        + "<p>Up-looking cameras (also known as \"bottom cameras\") cannot look at the calibration fiducial, obviously, "
                        + "so they are calibrated against a nozzle tip. Load the smallest nozzle tip that you can reliably detect "
                        + "(it may take some trial and error).</p><br/>"
                        + "<p>Jog nozzle " + defaultNozzle.getName()
                        + " over the camera "+camera.getName()+". Target it with the cross-hairs.</p><br/>"
                        + "<p>Adjust the <strong>Detected feature diameter</strong> up and down and see if it is detected right in the "
                        + "camera view. Make sure to target a circular edge that can be detected consistently even when seen from the side. "
                        + "This means it has to be a rather sharp-angled edge between faces. Typically, the air bore edge is targeted.</p><br/>"
                        + "<p>Then press Accept to capture the camera position.</p>"
                        + "</html>";
            }

            @Override
            public Solutions.Issue.CustomProperty[] getProperties() {
                return new Solutions.Issue.CustomProperty[] {
                        new Solutions.Issue.IntegerProperty(
                                "Detected feature diameter",
                                "Adjust the nozzle tip feature diameter that should be detected.",
                                10, 500) {
                            @Override
                            public int get() {
                                return featureDiameter;
                            }
                            @Override
                            public void set(int value) {
                                featureDiameter = value;
                                UiUtils.submitUiMachineTask(() -> {
                                    try {
                                        // This show a diagnostic detection image in the camera view for 4000ms.
                                        getSubjectPixelLocation(camera, Double.valueOf(value), 0.4, 4000);
                                    }
                                    catch (Exception e) {
                                        Toolkit.getDefaultToolkit().beep();
                                    }
                                });
                            }
                        },
                };
            }

            @Override
            public void setState(Solutions.State state) throws Exception {
                if (state == State.Solved) {
                    final State oldState = getState();
                    UiUtils.submitUiMachineTask(
                            () -> {
                                // Perform preliminary camera calibration. 
                                Length visionDiameter = autoCalibrateCamera(camera, defaultNozzle, Double.valueOf(featureDiameter), diagnosticsMilliseconds);
                                // Get the nozzle location.
                                Location nozzleLocation = getSubjectLocation(camera, defaultNozzle, diagnosticsMilliseconds);
                                // Determine the camera offsets, the nozzle now shows the true offset.
                                Location headOffsets = nozzleLocation;
                                camera.setHeadOffsets(nozzleLocation);
                                Logger.info("Set camera "+camera.getName()+" offsets to "+headOffsets+" (previously "+oldCameraOffsets+")");
                                if (defaultNozzle.getNozzleTip() instanceof ReferenceNozzleTip) {
                                    ReferenceNozzleTip referenceNozzleTip = (ReferenceNozzleTip)defaultNozzle.getNozzleTip();
                                    referenceNozzleTip.getCalibration().setCalibrationTipDiameter(visionDiameter);
                                    Logger.info("Set nozzle tip "+referenceNozzleTip.getName()+" vision diameter to "+visionDiameter+" (previously "+oldVisionDiameter+")");
                                }
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
                    // Restore the camera offset
                    camera.setHeadOffsets(oldCameraOffsets);
                    cameraCalibrationState.restoreTo(camera);
                    if (defaultNozzle.getNozzleTip() instanceof ReferenceNozzleTip) {
                        ReferenceNozzleTip referenceNozzleTip = (ReferenceNozzleTip)defaultNozzle.getNozzleTip();
                        referenceNozzleTip.getCalibration().setCalibrationTipDiameter(oldVisionDiameter);
                    }
                    // Persist this unsolved state.
                    solutions.setSolutionsIssueSolved(this, false);
                    super.setState(state);
                }
            }
        });
    }

    public void perNozzleSolutions(Solutions solutions, ReferenceHead head, Camera defaultCamera, Nozzle defaultNozzle, ReferenceNozzle nozzle) {
        if (solvedPrimaryXY && solvedSecondaryXY 
                && (solvedPrimaryZ || defaultNozzle == nozzle)) {
            final Location oldPrimaryFiducialLocation = head.getCalibrationPrimaryFiducialLocation();
            final Location oldSecondaryFiducialLocation = head.getCalibrationPrimaryFiducialLocation();
            final Location oldNozzleOffsets = nozzle.getHeadOffsets();
            for (boolean primary : (nozzle == defaultNozzle) ? new boolean [] {true, false} : new boolean [] {true} ) {
                String qualifier = primary ? "primary" : "secondary";
                boolean solved = solutions.add(new Solutions.Issue(
                        nozzle, 
                        "Nozzle offsets for the "+qualifier+" fiducial.", 
                        "Move the nozzle to the "+qualifier+" calibration fiducial and capture its offsets.", 
                        Solutions.Severity.Fundamental,
                        "https://github.com/openpnp/openpnp/wiki/Vision-Solutions#nozzle-offsets") {

                    @Override 
                    public void activate() throws Exception {
                        MainFrame.get().getMachineControls().setSelectedTool(nozzle);
                    }

                    @Override 
                    public String getExtendedDescription() {
                        return "<html>"
                                + "<p>Once the calibration "+qualifier+" fiducial is captured in X, Y you can use it to capture the nozzle head "
                                + "offsets (first approximation).</p><br/>"
                                + ((nozzle == defaultNozzle) ? "<p>This will also capture the calibration "+qualifier+" fiducial Z coordinate.</p><br/>" : "")
                                + "<p>Jog nozzle " + nozzle.getName()
                                + " over the "+qualifier+" fiducial. Lower the nozzle tip down until it touches the fiducial.</p><br/>"
                                + "<p>Then press Accept to capture the nozzle head offsets.</p>"
                                + "</html>";
                    }

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        if (state == State.Solved) {
                            final State oldState = getState();
                            UiUtils.submitUiMachineTask(
                                    () -> {
                                        if (primary) {
                                            // Reset any former head offset.
                                            nozzle.setHeadOffsets(Location.origin);
                                        }
                                        // Get the pure nozzle location.
                                        Location nozzleLocation = nozzle.getLocation();
                                        if (nozzle == defaultNozzle) {
                                            // This is the reference nozzle, set the Z of the fiducial.
                                            if (primary) {
                                                head.setCalibrationPrimaryFiducialLocation(head.getCalibrationPrimaryFiducialLocation()
                                                        .derive(nozzleLocation, false, false, true, false));
                                            }
                                            else {
                                                head.setCalibrationSecondaryFiducialLocation(head.getCalibrationSecondaryFiducialLocation()
                                                        .derive(nozzleLocation, false, false, true, false));
                                            }
                                        }
                                        if (primary) {
                                            // Determine the nozzle head offset (remember, we reset the head offset to zero above, so 
                                            // the nozzle now shows the true offset).
                                            Location headOffsets = head.getCalibrationPrimaryFiducialLocation().subtract(nozzleLocation);
                                            nozzle.setHeadOffsets(headOffsets);
                                            Logger.info("Set nozzle "+nozzle.getName()+" head offsets to "+headOffsets+" (previously "+oldNozzleOffsets+")");
                                        }
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
                            restore();
                            // Persist this unsolved state.
                            solutions.setSolutionsIssueSolved(this, false);
                            super.setState(state);
                        }
                    }

                    private void restore() {
                        if (primary) {
                            // Restore the nozzle offset
                            nozzle.setHeadOffsets(oldNozzleOffsets);
                        }
                        if (nozzle == defaultNozzle) {
                            // Restore the old fiducial Z.
                            if (primary) {
                                head.setCalibrationPrimaryFiducialLocation(head.getCalibrationPrimaryFiducialLocation()
                                        .derive(oldPrimaryFiducialLocation, false, false, true, false));
                            }
                            else {
                                head.setCalibrationSecondaryFiducialLocation(head.getCalibrationSecondaryFiducialLocation()
                                        .derive(oldSecondaryFiducialLocation, false, false, true, false));
                            }
                        }
                    }
                });
                if (nozzle == defaultNozzle) {
                    if (primary) {
                        solvedPrimaryZ = solved;
                    }
                    else {
                        solvedSecondaryZ = solved;
                    }
                }
            }
        }
    }

    public void perHeadSolutions(Solutions solutions, ReferenceHead head, Camera defaultCamera) {
        // Visual Homing
        if (head.getVisualHomingMethod() == VisualHomingMethod.None) {
            final Location oldFiducialLocation = head.getHomingFiducialLocation();
            solutions.add(new Solutions.Issue(
                    head, 
                    "Enable Visual Homing.", 
                    "Mount a permanent fiducial to your machine and use it for repeatable precision X/Y homing.", 
                    Solutions.Severity.Suggestion,
                    "https://github.com/openpnp/openpnp/wiki/Visual-Homing") {

                @Override 
                public void activate() throws Exception {
                    MainFrame.get().getMachineControls().setSelectedTool(defaultCamera);
                    defaultCamera.ensureCameraVisible();
                }

                @Override 
                public String getExtendedDescription() {
                    return "<html>"
                            + "<p>Mount a permanent fiducial to your machine table. Choose a mounting point that is mechanically coupled to the "
                            + "most important parts of your machine table. Make sure it is very unlikely you will ever need to change this new "
                            + "frame of reference. The fiducial must be at the same Z level as the PCB surface. More information is avaible "
                            + "online, please press the blue Info button.</p><br/>"
                            + "<p>Home the machine by means of your controller/manually. The controller must presently work in the wanted "
                            + "coordinate system.</p><br/>"
                            + "<p>Jog camera "+defaultCamera.getName()+" over the fiducial. Target it roughly with the cross-hairs.</p><br/>"
                            + "<p>Then press Accept to detect the precise position of the fiducial and set it up for visual homing.</p><br/>"
                            + "<p>Note: This will not change your present machine coordinate system, but rather pin it down to the fiducial. "
                            + "If the mechanics were to change slightly in the future (e.g. a homing end-switch slightly moved), the "
                            + "homing fiducial will still be able to precisely preserve the frame of reference in X and Y.</p>"
                            + "</html>";
                }

                @Override
                public Icon getExtendedIcon() {
                    return Icons.home;
                }

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (state == State.Solved) {
                        final State oldState = getState();
                        UiUtils.submitUiMachineTask(
                                () -> {
                                    // Set rough location as homing fiducial location.
                                    Location homingFiducialLocation = defaultCamera.getLocation();
                                    head.setHomingFiducialLocation(homingFiducialLocation);
                                    head.setVisualHomingMethod(VisualHomingMethod.ResetToFiducialLocation);
                                    // Perform homing to it, but don't reset machine position. 
                                    head.visualHome(machine, false);
                                    // With the precise location, set the homing fiducial again.
                                    homingFiducialLocation = defaultCamera.getLocation();
                                    head.setHomingFiducialLocation(homingFiducialLocation);
                                    return true;
                                },
                                (result) -> {
                                    UiUtils.messageBoxOnException(() -> super.setState(state));
                                },
                                (t) -> {
                                    UiUtils.showError(t);
                                    // restore old state
                                    UiUtils.messageBoxOnException(() -> setState(oldState));
                                });
                    }
                    else {
                        head.setHomingFiducialLocation(oldFiducialLocation);
                        head.setVisualHomingMethod(VisualHomingMethod.None);
                        super.setState(state);
                    }
                }
            });

        }
    }

    static class CameraCalibrationState {
        boolean flipX;
        boolean flipY;
        double rotation;
        boolean calibrationEnabled;
        Location unitsPerPixel;
        boolean enableUnitsPerPixel3D;

        CameraCalibrationState (ReferenceCamera camera) {
            flipX = camera.isFlipX();
            flipY = camera.isFlipY();
            rotation = camera.getRotation();
            calibrationEnabled = camera.getCalibration().isEnabled();
            unitsPerPixel = camera.getUnitsPerPixel();
            enableUnitsPerPixel3D = camera.isEnableUnitsPerPixel3D();
        }

        void restoreTo(ReferenceCamera camera) {
            camera.setFlipX(flipX);
            camera.setFlipY(flipY);
            camera.setRotation(rotation);
            camera.getCalibration().setEnabled(calibrationEnabled);
            camera.setUnitsPerPixel(unitsPerPixel);
            camera.setEnableUnitsPerPixel3D(enableUnitsPerPixel3D);
        }
    }

    /**
     * As we don't know anything about the camera yet, the preliminary auto-calibration is somewhat of a chicken and egg
     * proposition. The current solution is very simple: we just stake out three points in the camera view that we assume 
     * will always fit in (see {@link #zeroKnowledgeDisplacementMm} The three points define the X and Y axis grid. 
     * 
     * @param camera
     * @param movable
     * @param diagnostics
     * @return 
     * @throws Exception
     */
    public Length autoCalibrateCamera(ReferenceCamera camera, HeadMountable movable, Double fiducialDiameter, long diagnostics) 
            throws Exception {
        Location movableLocation = movable.getLocation();
        try {
            // Reset camera transforms.
            camera.setFlipX(false);
            camera.setFlipY(false);
            camera.setRotation(0);
            camera.getCalibration().setEnabled(false);
            camera.setEnableUnitsPerPixel3D(false);

            if (fiducialDiameter == null) { 
                Circle subject = getSubjectPixelLocation(camera, null, 0.0, diagnostics);
                fiducialDiameter = subject.getDiameter();
            }

            // Perform zero knowledge calibration motion pattern.
            double displacementMm = zeroKnowledgeDisplacementMm;
            if (movable == camera) {
                // We're moving the camera, so the displacement of the subject in the camera view is seen reversed
                // e.g. when we move the camera 1mm to the right, the subject as seen in the camera view goes 
                // 1mm to the left.  
                displacementMm = -displacementMm;
            }
            // else: we are moving the camera subject and displacement is seen as is.

            Location originLocation = movableLocation.add(new Location(LengthUnit.Millimeters,
                    -displacementMm * 0.5, -displacementMm * 0.5, 0, 0));
            zeroKnowledgeMoveTo(movable, originLocation);
            Circle origin = getSubjectPixelLocation(camera, fiducialDiameter, zeroKnowledgeDisplacementRatio, diagnostics);
            Location displacedXLocation = originLocation.add(
                    new Location(LengthUnit.Millimeters, displacementMm, 0, 0, 0));
            zeroKnowledgeMoveTo(movable, displacedXLocation);
            Circle displacedX = getSubjectPixelLocation(camera, fiducialDiameter, zeroKnowledgeDisplacementRatio, diagnostics);
            // Note: pixel coordinate system has flipped Y.
            double dxX = displacedX.x - origin.x;
            double dyX = -(displacedX.y - origin.y);
            Location displacedYLocation = originLocation.add(
                    new Location(LengthUnit.Millimeters, 0, displacementMm, 0, 0));
            zeroKnowledgeMoveTo(movable, displacedYLocation);
            Circle displacedY = getSubjectPixelLocation(camera, fiducialDiameter, zeroKnowledgeDisplacementRatio, diagnostics);
            // Note: pixel coordinate system has flipped Y.
            double dxY = displacedY.x - origin.x;
            double dyY = -(displacedY.y - origin.y);
            Location unitsPerPixel;
            // Compute camera transform
            if (Math.abs(dxX) > Math.abs(dyX)) {
                // Landscape orientation.
                if (dxX > 0 && dyY > 0) {
                    // 0°
                }
                else if (dxX > 0 && dyY < 0) {
                    // Mirrored in Y
                    camera.setFlipY(true);
                }
                else if (dxX < 0 && dyY < 0) {
                    // 180°
                    camera.setFlipX(true);
                    camera.setFlipY(true);
                }
                else if (dxX < 0 && dyY > 0) {
                    // Mirrored in X
                    camera.setFlipX(true);
                }
                unitsPerPixel = new Location(LengthUnit.Millimeters, 
                        zeroKnowledgeDisplacementMm/Math.abs(dxX), 
                        zeroKnowledgeDisplacementMm/Math.abs(dyY), 
                        0, 0);
            }
            else {
                // Portrait orientation.
                if (dxY > 0 && dyX < 0) {
                    // 90°
                    camera.setRotation(90);
                }
                else if (dxY > 0 && dyX > 0) {
                    // 90°, mirrored in X
                    camera.setRotation(90);
                    camera.setFlipX(true);
                }
                else if (dxY < 0 && dyX > 0) {
                    // 270°
                    camera.setRotation(270);
                }
                else if (dxY < 0 && dyX < 0) {
                    // 90°, mirrored in Y
                    camera.setRotation(90);
                    camera.setFlipY(true);
                }
                unitsPerPixel = new Location(LengthUnit.Millimeters, 
                        zeroKnowledgeDisplacementMm/Math.abs(dyX), 
                        zeroKnowledgeDisplacementMm/Math.abs(dxY), 
                        0, 0);
            }
            camera.setUnitsPerPixel(unitsPerPixel);
            return new Length(fiducialDiameter*unitsPerPixel.getX(), LengthUnit.Millimeters);
        }
        finally {
            // Restore the camera location
            zeroKnowledgeMoveTo(movable, movableLocation);
        }
    }

    /**
     * A fiducial locator using the same built-in fiducial detection as the auto-calibration. This supports vision calibration
     * early in the machine setup process, i.e. long before the user should be confronted with editing fiducial locator and 
     * nozzle tip calibration pipelines.
     * 
     * @param camera
     * @param movable
     * @param diagnostics
     * @return
     * @throws Exception
     */
    public Location getSubjectLocation(ReferenceCamera camera, HeadMountable movable, long diagnostics) throws Exception {
        Location location = movable.getLocation();
        for (int pass = 0; pass < zeroKnowledgeFiducialLocatorPasses ; pass++) {
            Circle detected = getSubjectPixelLocation(camera, null, 0, diagnostics);
            Location subjectLocation = VisionUtils.getPixelLocation(camera, movable, detected.x, detected.y);
            if (movable == camera) {
                // When the camera is the movable, we can simply move it to the detected location.
                location = subjectLocation;
            }
            else {
                // When the camera is not the movable, i.e. the movable is the subject, then we need to move
                // the subject to the camera.
                Location offsets = camera.getLocation().subtract(subjectLocation);
                location = location.add(offsets);
            }
            zeroKnowledgeMoveTo(movable, location);
        }
        return location;
    }

    /**
     * Before we know anything about the camera, we cannot use pipelines, so we use the DetectCircularSymmetry stage 
     * directly. We can also only get pixel locations for now.  
     * 
     * @param camera
     * @param subjectDiameter Provides the fiducial diameter in pixels, if known, null otherwise. 
     * @param extraSearchRange Specifies an extra search range, relative to the camera view size (minimum of width, height). 
     * @param diagnostics
     * @return
     * @throws Exception
     */
    public Circle getSubjectPixelLocation(ReferenceCamera camera, Double subjectDiameter, double extraSearchRange, long diagnostics) throws Exception {
        BufferedImage bufferedImage = camera.lightSettleAndCapture();
        Mat image = OpenCvUtils.toMat(bufferedImage);
        try {
            int subjectAreaDiameter = (int) (Math.min(image.cols(), image.rows())
                    * maxCameraRelativeFiducialAreaDiameter);
            int maxDiameter = (subjectDiameter != null ? 
                    ((int)(subjectDiameter*fiducialMargin + 1))
                    : subjectAreaDiameter/2);
            int minDiameter = (subjectDiameter != null ? 
                    ((int)(subjectDiameter/fiducialMargin - 1))
                    : 7);
            int maxDistance = subjectAreaDiameter - maxDiameter 
                    + (int) (Math.min(image.cols(), image.rows())*extraSearchRange);
            ScoreRange scoreRange = new ScoreRange();
            int effectiveSubSampling = Math.max(1, Math.min(subSampling, (maxDiameter-minDiameter)/4));
            List<Circle> results = DetectCircularSymmetry.findCircularSymmetry(image, 
                    bufferedImage.getWidth()/2, bufferedImage.getHeight()/2, 
                    maxDiameter, minDiameter, maxDistance, minSymmetry,
                    effectiveSubSampling, diagnostics > 0, scoreRange);
            if (diagnostics > 0) {
                if (LogUtils.isDebugEnabled()) {
                    File file = Configuration.get().createResourceFile(getClass(), "fidloc_", ".png");
                    Imgcodecs.imwrite(file.getAbsolutePath(), image);
                }                
                final BufferedImage diagnosticImage = OpenCvUtils.toBufferedImage(image);
                SwingUtilities.invokeLater(() -> {
                    MainFrame.get()
                    .getCameraViews()
                    .getCameraView(camera)
                    .showFilteredImage(diagnosticImage,
                            String.format("%.2f", scoreRange.maxScore), diagnostics);
                });
            }
            if (results.size() < 1) {
                throw new Exception("Subject not found.");
            }
            return results.get(0);
        }
        finally {
            image.release();
        }
    }

    /**
     * Moves the head-mountable to a location at safe Z using a conservative backlash-compensation scheme. Used to get precise
     * camera and camera subject positioning before proper backlash compensation can be configured and calibrated. 
     * 
     * @param hm
     * @param location
     * @throws Exception
     */
    public void zeroKnowledgeMoveTo(HeadMountable hm, Location location) throws Exception {
        Location backlashCompensatedLocation = location.add(zeroKnowledgeBacklashOffsets);
        MovableUtils.moveToLocationAtSafeZ(hm, backlashCompensatedLocation);
        hm.moveTo(location);
    }
}
