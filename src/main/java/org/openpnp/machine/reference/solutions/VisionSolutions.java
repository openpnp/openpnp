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
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
import org.openpnp.machine.reference.camera.AutoFocusProvider;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.model.Solutions.State;
import org.openpnp.model.Solutions.Subject;
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
 * This helper class implements the Issues & Solutions for the Vision milestone. 
 *
 */
public class VisionSolutions implements Solutions.Subject {

    // These values can be changed in the machine.xml but they are not exposed in the UI.  

    // See org.openpnp.vision.pipeline.stages.DetectCircularSymmetry.findCircularSymmetry for what these mean.
    @Attribute(required = false)
    private double minSymmetry = 1.25;
    @Attribute(required = false)
    private int subSampling = 4;
    @Attribute(required = false)
    private int superSampling = 12;

    @Attribute(required = false)
    protected long diagnosticsMilliseconds = 2000;

    /**
     * Maximum fiducial sensitive size, relative to the camera size (min of width and height)
     */
    @Attribute(required = false)
    private double maxCameraRelativeFiducialAreaDiameter = 0.2; 
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
    private int zeroKnowledgeFiducialLocatorPasses = 3;

    @Attribute(required = false)
    private double fiducialMargin = 1.1;

    @Attribute(required = false)
    private int zeroKnowledgeRunoutCompensationShots = 6;

    @Attribute(required = false)
    private double zeroKnowledgeAutoFocusDepthMm = 2.0;

    @Attribute(required = false)
    private double zeroKnowledgeBacklashSpeed = 0.2;

    public VisionSolutions setMachine(ReferenceMachine machine) {
        this.machine = machine;
        return this;
    }

    public int getSuperSampling() {
        return superSampling;
    }

    public long getDiagnosticsMilliseconds() {
        return diagnosticsMilliseconds;
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
                    ReferenceCamera defaultCamera = null;
                    try {
                        if (head.getDefaultCamera() instanceof ReferenceCamera) {
                            defaultCamera = (ReferenceCamera) head.getDefaultCamera();
                        }
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

    public class VisionFeatureIssue extends Solutions.Issue {

        public VisionFeatureIssue(Subject subject, ReferenceCamera camera, Length featureDiameterIfKnown, String issue, String solution, Severity severity, String uri) {
            super(subject, issue, solution, severity, uri);
            this.camera = camera;
            featureDiameter = 20;
            if (camera.getUnitsPerPixel().getX() != 0
                    && featureDiameterIfKnown != null
                    && featureDiameterIfKnown.getValue() != 0) {
                // Existing diameter setting.
                featureDiameter = (int) Math.round(featureDiameterIfKnown.divide(camera.getUnitsPerPixel().getLengthX()));
            }
        }

        private ReferenceCamera camera; 
        protected int featureDiameter;

        @Override 
        public void activate() throws Exception {
            camera.ensureCameraVisible();
        }

        @Override
        public Solutions.Issue.CustomProperty[] getProperties() {
            return new Solutions.Issue.CustomProperty[] {
                    new Solutions.Issue.IntegerProperty(
                            "Detected feature diameter",
                            "Adjust the nozzle tip feature diameter that should be detected.",
                            3, Math.min(camera.getWidth(), camera.getHeight())/4) {
                        @Override
                        public int get() {
                            return featureDiameter;
                        }
                        @Override
                        public void set(int value) {
                            featureDiameter = value;
                            try {
                                UiUtils.submitUiMachineTask(() -> {
                                    try {
                                        // This show a diagnostic detection image in the camera view for 2000ms.
                                        getSubjectPixelLocation(camera, null, new Circle(0, 0, value), 0.05, "Diameter "+(int)value+" px");
                                    }
                                    catch (Exception e) {
                                        Toolkit.getDefaultToolkit().beep();
                                    }
                                }).get();
                            }
                            catch (InterruptedException | ExecutionException e) {
                                Logger.warn(e);
                            }
                        }
                    },
            };
        }
    }

    private void perDownLookingCameraSolutions(Solutions solutions, ReferenceHead head, Camera defaultCamera, Nozzle defaultNozzle, ReferenceCamera camera) {

        // Find the calibration fiducials.
        if (camera == defaultCamera) {
            final Location oldPrimaryFiducialLocation = head.getCalibrationPrimaryFiducialLocation();
            final Length oldFiducialDiameter = head.getCalibrationPrimaryFiducialDiameter();
            final CameraCalibrationState cameraCalibrationState = new CameraCalibrationState(camera); 
            solvedPrimaryXY = solutions.add(new VisionFeatureIssue(
                    camera, 
                    camera,
                    oldFiducialDiameter,
                    "Primary calibration fiducial position and initial camera calibration.", 
                    "Move the camera over the primary calibration fiducial and capture its position.", 
                    Solutions.Severity.Fundamental,
                    "https://github.com/openpnp/openpnp/wiki/Vision-Solutions#calibration-primary-fiducial") {

                @Override 
                public void activate() throws Exception {
                    super.activate();
                    MainFrame.get().getMachineControls().setSelectedTool(camera);
                }

                @Override 
                public String getExtendedDescription() {
                    return "<html>"
                            + "<p>Camera calibration can be performed automatically by looking at fiducials while moving the "
                            + "camera around in a certain pattern. This solution determines the X, Y position of the primary "
                            + "fiducial and it performs preliminary camera calibration.</p><br/>"
                            + "<p>Instructions for how to create and position the primary fiducial must be obtained in the OpenPnP "
                            + "Wiki. There are very important rules that must be observed, so don't miss it! Press the "
                            + "blue Info button (below) to open the Wiki.</p><br/>"
                            + "<p>Once you have prepared the calibration primary fiducial you can capture its position "
                            + "in X, Y.</p><br/>"
                            + "<p>Jog camera " + camera.getName()
                            + " over the privary fiducial. Target it roughly with the cross-hairs.</p><br/>"
                            + "<p>Adjust the <strong>Detected feature diameter</strong> up and down and see if it is detected right in the "
                            + "camera view.</p><br/>"
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
                                    Length fiducialDiameter = autoCalibrateCamera(camera, camera, (double) featureDiameter, "Primary Fiducial & Camera Calibration", false);
                                    // Get the precise fiducial location.
                                    Location fiducialLocation = centerInOnSubjectLocation(camera, camera, fiducialDiameter, "Primary Fiducial & Camera Calibration", false);
                                    // Store it.
                                    head.setCalibrationPrimaryFiducialLocation(fiducialLocation);
                                    head.setCalibrationPrimaryFiducialDiameter(fiducialDiameter);
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
                        head.setCalibrationPrimaryFiducialDiameter(oldFiducialDiameter);
                        head.setCalibrationPrimaryFiducialLocation(head.getCalibrationPrimaryFiducialLocation()
                                .derive(oldPrimaryFiducialLocation, true, true, false, false));
                        cameraCalibrationState.restoreTo(camera);
                        // Persist this unsolved state.
                        solutions.setSolutionsIssueSolved(this, false);
                        super.setState(state);
                    }
                }
            });

            final Location oldSecondaryFiducialLocation = head.getCalibrationSecondaryFiducialLocation();
            final Length oldSecondaryFiducialDiameter = head.getCalibrationSecondaryFiducialDiameter();
            final Location oldUnitsPerPixelSecondary = camera.getUnitsPerPixelSecondary();
            solvedSecondaryXY = solutions.add(new VisionFeatureIssue(
                    camera, 
                    camera,
                    oldSecondaryFiducialDiameter,
                    "Secondary calibration fiducial position.", 
                    "Move the camera over the secondary calibration fiducial and capture its position.", 
                    Solutions.Severity.Fundamental,
                    "https://github.com/openpnp/openpnp/wiki/Vision-Solutions#calibration-secondary-fiducial") {

                @Override 
                public void activate() throws Exception {
                    super.activate();
                    MainFrame.get().getMachineControls().setSelectedTool(camera);
                }

                @Override 
                public String getExtendedDescription() {
                    return "<html>"
                            + "<p>Camera calibration also requires looking at a secondary fiducial at different "
                            + "Z level. This will provide the calibration algorithm with the needed 3D/spacial information to "
                            + "determine the true focal length of the lens and the optical position of the camera in space.</p><br/>"
                            + "<p>Instructions for how to create and position the secondary fiducial must be obtained in the OpenPnP "
                            + "Wiki. There are very important rules that must be observed, so don't miss it! Press the "
                            + "blue Info button (below) to open the Wiki.</p><br/>"
                            + "<p>Once you have prepared the calibration secondary fiducial you can capture its position "
                            + "in X, Y.</p><br/>"
                            + "<p>Jog camera " + camera.getName()
                            + " over the secondary fiducial. Target it roughly with the cross-hairs.</p><br/>"
                            + "<p>Adjust the <strong>Detected feature diameter</strong> up and down and see if it is detected right in the "
                            + "camera view.</p><br/>"
                            + "<p>Then press Accept to capture the position.</p>"
                            + "</html>";
                }

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (state == State.Solved) {
                        final State oldState = getState();
                        UiUtils.submitUiMachineTask(
                                () -> {
                                    // Perform preliminary 3D camera calibration. 
                                    Length fiducialDiameter = autoCalibrateCamera(camera, camera, (double) featureDiameter, "Secondary Fiducial Calibration", true);
                                    // Set location as fiducial location.
                                    Location fiducialLocation = centerInOnSubjectLocation(camera, camera, 
                                            fiducialDiameter, "Secondary Fiducial Calibration", true);
                                    head.setCalibrationSecondaryFiducialLocation(fiducialLocation);
                                    head.setCalibrationSecondaryFiducialDiameter(fiducialDiameter);
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
                        head.setCalibrationSecondaryFiducialLocation(head.getCalibrationSecondaryFiducialLocation()
                                .derive(oldSecondaryFiducialLocation, true, true, false, false));
                        head.setCalibrationSecondaryFiducialDiameter(oldSecondaryFiducialDiameter);
                        camera.setUnitsPerPixelSecondary(camera.getUnitsPerPixelSecondary()
                                .derive(oldUnitsPerPixelSecondary, true, true, false, false));
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
            solutions.add(new VisionFeatureIssue(
                    camera, 
                    camera,
                    head.getCalibrationPrimaryFiducialDiameter(),
                    "Determine the camera head offsets.", 
                    "Move the camera "+camera.getName()+" over the secondary calibration fiducial and capture its offsets.", 
                    Solutions.Severity.Fundamental,
                    "https://github.com/openpnp/openpnp/wiki/Vision-Solutions#down-looking-camera-offsets") {

                @Override 
                public void activate() throws Exception {
                    super.activate();
                    MainFrame.get().getMachineControls().setSelectedTool(camera);
                }

                @Override 
                public String getExtendedDescription() {
                    return "<html>"
                            + "<p>Once the calibration primary fiducial is captured you can use it to capture the camera head "
                            + "offsets (first approximation).</p><br/>"
                            + "<p>Jog camera " + camera.getName()
                            + " over the primary fiducial. Target it with the cross-hairs.</p><br/>"
                            + "<p>Adjust the <strong>Detected feature diameter</strong> up and down and see if it is detected right in the "
                            + "camera view.</p><br/>"
                            + "<p>Then press Accept to capture the offsets. The camera will perform a small calibration movement "
                            + "</html>";
                }

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (state == State.Solved) {
                        final State oldState = getState();
                        UiUtils.submitUiMachineTask(
                                () -> {
                                    // Perform preliminary camera calibration. 
                                    autoCalibrateCamera(camera, camera, (double) featureDiameter, "Camera Positional Calibration", false);
                                    // Get the precise fiducial location.
                                    Location fiducialLocation = centerInOnSubjectLocation(camera, camera, 
                                            head.getCalibrationPrimaryFiducialDiameter(), "Camera Positional Calibration", false);
                                    // Determine the camera head offset (remember, we reset the head offset to zero above, so 
                                    // the camera now shows the true offset).
                                    Location headOffsets = camera.getHeadOffsets().add(head.getCalibrationPrimaryFiducialLocation().subtract(fiducialLocation));
                                    camera.setHeadOffsets(headOffsets);
                                    Logger.info("Set camera "+camera.getName()+" head offsets to "+headOffsets
                                            +" (previously "+oldCameraOffsets+")");
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
        final ReferenceNozzleTip referenceNozzleTip = ((defaultNozzle.getNozzleTip() instanceof ReferenceNozzleTip) ?
                (ReferenceNozzleTip)defaultNozzle.getNozzleTip() :
                    null
                );
        final Length oldVisionDiameter = (referenceNozzleTip == null ? null : referenceNozzleTip.getCalibration().getCalibrationTipDiameter());
        solutions.add(new VisionFeatureIssue(
                camera, 
                camera,
                oldVisionDiameter,
                "Determine the up-looking camera "+camera.getName()+" position.", 
                "Move the nozzle "+defaultNozzle.getName()+" over the up-looking camera "+camera.getName()+" and capture the position.", 
                Solutions.Severity.Fundamental,
                "https://github.com/openpnp/openpnp/wiki/Vision-Solutions#up-looking-camera-offsets") {

            @Override 
            public void activate() throws Exception {
                super.activate();
                MainFrame.get().getMachineControls().setSelectedTool(defaultNozzle);
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
            public void setState(Solutions.State state) throws Exception {
                if (state == State.Solved) {
                    final State oldState = getState();
                    UiUtils.submitUiMachineTask(
                            () -> {
                                // Perform preliminary camera calibration. 
                                Length visionDiameter = autoCalibrateCamera(camera, defaultNozzle, Double.valueOf(featureDiameter), "Camera Positional Calibration", false);
                                // Get the nozzle location.
                                Location nozzleLocation = centerInOnSubjectLocation(camera, defaultNozzle, visionDiameter, "Camera Positional Calibration", false);
                                // Determine the camera offsets, the nozzle now shows the true offset.
                                Location headOffsets = nozzleLocation;
                                camera.setHeadOffsets(nozzleLocation);
                                Logger.info("Set camera "+camera.getName()+" offsets to "+headOffsets
                                        +" (previously "+oldCameraOffsets+")");
                                if (referenceNozzleTip != null) {
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
                    if (referenceNozzleTip != null) {
                        referenceNozzleTip.getCalibration().setCalibrationTipDiameter(oldVisionDiameter);
                    }
                    // Persist this unsolved state.
                    solutions.setSolutionsIssueSolved(this, false);
                    super.setState(state);
                }
            }
        });
    }

    private void perNozzleSolutions(Solutions solutions, ReferenceHead head, ReferenceCamera defaultCamera, Nozzle defaultNozzle, ReferenceNozzle nozzle) {
        if (solvedPrimaryXY 
                && (solvedPrimaryZ || defaultNozzle == nozzle)) {
            final Location oldPrimaryFiducialLocation = head.getCalibrationPrimaryFiducialLocation();
            final Location oldSecondaryFiducialLocation = head.getCalibrationPrimaryFiducialLocation();
            final Location oldPrimaryUpp = defaultCamera.getUnitsPerPixelPrimary();
            final Location oldSecondaryUpp = defaultCamera.getUnitsPerPixelSecondary();
            
            final Location oldNozzleOffsets = nozzle.getHeadOffsets();
            final Length oldPrimaryZ = defaultCamera.getCameraPrimaryZ();
            final Length oldSecondaryZ = defaultCamera.getCameraSecondaryZ();
            final boolean oldEnabled3D = defaultCamera.isEnableUnitsPerPixel3D();
            final boolean oldAutoViewPlaneZ = defaultCamera.isAutoViewPlaneZ();
            for (boolean primary : (nozzle == defaultNozzle && solvedSecondaryXY) ? new boolean [] {true, false} : new boolean [] {true} ) {
                String qualifier = primary ? "primary" : "secondary";
                solutions.add(new Solutions.Issue(
                        nozzle, 
                        "Nozzle "+nozzle.getName()+" offsets for the "+qualifier+" fiducial.", 
                        "Move the nozzle "+nozzle.getName()+" to the "+qualifier+" calibration fiducial and capture its offsets.", 
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
                                            if (primary) {
                                                Location upp = defaultCamera.getUnitsPerPixelPrimary()
                                                        .derive(nozzleLocation, false, false, true, false);
                                                defaultCamera.setUnitsPerPixelPrimary(upp);
                                                defaultCamera.setDefaultZ(nozzleLocation.getLengthZ());
                                                solvedPrimaryZ = true;
                                            }
                                            else {
                                                Location upp = defaultCamera.getUnitsPerPixelSecondary()
                                                        .derive(nozzleLocation, false, false, true, false);
                                                defaultCamera.setUnitsPerPixelSecondary(upp);
                                                defaultCamera.setEnableUnitsPerPixel3D(true);
                                                defaultCamera.setAutoViewPlaneZ(true);
                                                solvedSecondaryZ = true;
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
                                        if (nozzle == defaultNozzle) {
                                            if (primary) {
                                                solvedPrimaryZ = false;
                                            }
                                            else {
                                                solvedSecondaryZ = false;
                                            }
                                        }
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
                            if (primary) {
                                // Restore UPP Z stuff.
                                Location upp = defaultCamera.getUnitsPerPixelPrimary()
                                        .derive(oldPrimaryUpp, false, false, true, false);
                                defaultCamera.setUnitsPerPixelPrimary(upp);
                                defaultCamera.setCameraPrimaryZ(oldPrimaryZ);
                            }
                            else {
                                // Restore UPP Z stuff.
                                Location upp = defaultCamera.getUnitsPerPixelSecondary()
                                        .derive(oldSecondaryUpp, false, false, true, false);
                                defaultCamera.setUnitsPerPixelSecondary(upp);
                                defaultCamera.setCameraSecondaryZ(oldSecondaryZ);
                                defaultCamera.setEnableUnitsPerPixel3D(oldEnabled3D);
                                defaultCamera.setAutoViewPlaneZ(oldAutoViewPlaneZ);
                            }
                        }
                    }
                });
            }
        }
    }

    private void perHeadSolutions(Solutions solutions, ReferenceHead head, Camera defaultCamera) {
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
                            + "online, please press the blue Info button (below).</p><br/>"
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

    private static class CameraCalibrationState {
        boolean flipX;
        boolean flipY;
        double rotation;
        // TODO: when Tony Luken's solution is integrated(?): 
        //boolean calibrationEnabled;
        Location unitsPerPixel;

        CameraCalibrationState (ReferenceCamera camera) {
            flipX = camera.isFlipX();
            flipY = camera.isFlipY();
            rotation = camera.getRotation();
            //TODO: when Tony Luken's solution is integrated(?): 
            //calibrationEnabled = camera.getCalibration().isEnabled();
            unitsPerPixel = camera.getUnitsPerPixel();
        }

        void restoreTo(ReferenceCamera camera) {
            camera.setFlipX(flipX);
            camera.setFlipY(flipY);
            camera.setRotation(rotation);
            //TODO: when Tony Luken's solution is integrated(?): 
            //camera.getCalibration().setEnabled(calibrationEnabled);
            camera.setUnitsPerPixel(camera.getUnitsPerPixel()
                    .derive(unitsPerPixel, true, true, false, false));
        }
    }

    /**
     * As we don't know anything about the camera yet, the preliminary auto-calibration is somewhat of a chicken and egg
     * proposition. The current solution is very simple: we just stake out three points in the camera view that we assume 
     * will always fit in (see {@link #zeroKnowledgeDisplacementMm} The three points define the X and Y axis grid. 
     * 
     * @param camera
     * @param movable
     * @param expectedDiameter
     * @param diagnostics
     * @param secondary true if this should calibrate the secondary units per pixel
     * @return The diameter of the detected feature.
     * @throws Exception
     */
    public Length autoCalibrateCamera(ReferenceCamera camera, HeadMountable movable, Double expectedDiameter, String diagnostics, boolean secondary) 
            throws Exception {
        Location initialLocation = movable.getLocation();
        try {
            if (!secondary) {
                // Reset camera transforms.
                camera.setFlipX(false);
                camera.setFlipY(false);
                camera.setRotation(0);
                //TODO: when Tony Luken's solution is integrated(?): 
                // camera.getCalibration().setEnabled(false);
            }

            Circle expectedOffsetsAndDiameter; 
            if (expectedDiameter == null) { 
                // Detect the diameter.
                expectedOffsetsAndDiameter = getSubjectPixelLocation(camera, movable, null, zeroKnowledgeDisplacementRatio, diagnostics);
            }
            else {
                expectedOffsetsAndDiameter = new Circle(0,  0, expectedDiameter);
                // Detect the true diameter.
                expectedOffsetsAndDiameter = getSubjectPixelLocation(camera, movable, expectedOffsetsAndDiameter, zeroKnowledgeDisplacementRatio, diagnostics);
            }
            // Center offset 0, 0 expected.
            expectedOffsetsAndDiameter.setX(0); 
            expectedOffsetsAndDiameter.setY(0); 

            // Perform zero knowledge calibration motion pattern.
            double displacementAbsMm = zeroKnowledgeDisplacementMm;
            Location unitsPerPixel = new Location(null);
            Length featureDiameter = null;
            for (int pass = 0; pass < 3; pass++) {
                double displacementMm = displacementAbsMm;
                if (movable == camera) {
                    // We're moving the camera, so the displacement of the subject in the camera view is seen reversed
                    // e.g. when we move the camera 1mm to the right, the subject as seen in the camera view goes 
                    // 1mm to the left.  
                    displacementMm = -displacementMm;
                }
                // else: we are moving the camera subject and displacement is seen as is.

                // X Axis 
                Location originLocationX = initialLocation.add(new Location(LengthUnit.Millimeters,
                        -displacementMm * 0.5, 0, 0, 0));
                zeroKnowledgeMoveTo(movable, originLocationX, pass == 0);
                if (featureDiameter != null) {
                    expectedOffsetsAndDiameter = getExpectedOffsetsAndDiameter(camera, movable, initialLocation, featureDiameter, secondary);
                }
                Circle originX = getSubjectPixelLocation(camera, movable, expectedOffsetsAndDiameter, zeroKnowledgeDisplacementRatio, diagnostics);
                Location displacedXLocation = originLocationX.add(
                        new Location(LengthUnit.Millimeters, displacementMm, 0, 0, 0));
                zeroKnowledgeMoveTo(movable, displacedXLocation, false);
                if (featureDiameter != null) {
                    expectedOffsetsAndDiameter = getExpectedOffsetsAndDiameter(camera, movable, initialLocation, featureDiameter, secondary);
                }
                Circle displacedX = getSubjectPixelLocation(camera, movable, expectedOffsetsAndDiameter, zeroKnowledgeDisplacementRatio, diagnostics);
                // Note: pixel coordinate system has flipped Y.
                double dxX = displacedX.x - originX.x;
                double dyX = -(displacedX.y - originX.y);

                // Y Axis 
                Location originLocationY = initialLocation.add(new Location(LengthUnit.Millimeters,
                        0, -displacementMm * 0.5, 0, 0));
                zeroKnowledgeMoveTo(movable, originLocationY, false);
                if (featureDiameter != null) {
                    expectedOffsetsAndDiameter = getExpectedOffsetsAndDiameter(camera, movable, initialLocation, featureDiameter, secondary);
                }
                Circle originY = getSubjectPixelLocation(camera, movable, expectedOffsetsAndDiameter, zeroKnowledgeDisplacementRatio, diagnostics);
                Location displacedYLocation = originLocationY.add(
                        new Location(LengthUnit.Millimeters, 0, displacementMm, 0, 0));
                zeroKnowledgeMoveTo(movable, displacedYLocation, false);
                if (featureDiameter != null) {
                    expectedOffsetsAndDiameter = getExpectedOffsetsAndDiameter(camera, movable, initialLocation, featureDiameter, secondary);
                }
                Circle displacedY = getSubjectPixelLocation(camera, movable, expectedOffsetsAndDiameter, zeroKnowledgeDisplacementRatio, diagnostics);
                // Note: pixel coordinate system has flipped Y.
                double dxY = displacedY.x - originY.x;
                double dyY = -(displacedY.y - originY.y);

                // Compute or confirm camera transform
                boolean confirmed = false;
                if (Math.abs(dxX) > Math.abs(dyX)) {
                    // Landscape orientation.
                    if (dxX > 0 && dyY > 0) {
                        // 0°
                        confirmed = true;
                    }
                    else if (dxX > 0 && dyY < 0) {
                        // Mirrored on X axis
                        camera.setFlipX(true);
                    }
                    else if (dxX < 0 && dyY < 0) {
                        // 180°
                        camera.setFlipX(true);
                        camera.setFlipY(true);
                    }
                    else if (dxX < 0 && dyY > 0) {
                        // Mirrored on Y axis
                        camera.setFlipY(true);
                    }
                    unitsPerPixel = new Location(LengthUnit.Millimeters, 
                            displacementAbsMm/Math.abs(dxX), 
                            displacementAbsMm/Math.abs(dyY), 
                            0,
                            0);
                }
                else {
                    // Portrait orientation.
                    if (dxX > 0 && dyY > 0) {
                        // 0°
                        confirmed = true;
                    }
                    else if (dxY > 0 && dyX < 0) {
                        // 90°
                        camera.setRotation(90);
                    }
                    else if (dxY > 0 && dyX > 0) {
                        // 90°, mirrored on Y axis
                        camera.setRotation(90);
                        camera.setFlipY(true);
                    }
                    else if (dxY < 0 && dyX > 0) {
                        // 270°
                        camera.setRotation(270);
                    }
                    else if (dxY < 0 && dyX < 0) {
                        // 90°, mirrored on X axis
                        camera.setRotation(90);
                        camera.setFlipX(true);
                    }
                    unitsPerPixel = new Location(LengthUnit.Millimeters, 
                            displacementAbsMm/Math.abs(dyX), 
                            displacementAbsMm/Math.abs(dxY), 
                            0,
                            0);
                }
                // Settings after this pass.
                featureDiameter = new Length(expectedOffsetsAndDiameter.getDiameter()*unitsPerPixel.getX(), unitsPerPixel.getUnits());
                if (secondary) {
                    // Keep Z (for now).
                    if (camera.getUnitsPerPixelSecondary() != null) {
                        unitsPerPixel = unitsPerPixel.derive(camera.getUnitsPerPixelSecondary(), false, false, true, false);
                    }
                    camera.setUnitsPerPixelSecondary(unitsPerPixel);
                    camera.setCameraSecondaryZ(camera.getCameraPhysicalLocation().getLengthZ());
                }
                else {
                    // Keep Z (for now).
                    unitsPerPixel = unitsPerPixel.derive(camera.getUnitsPerPixelPrimary(), false, false, true, false);
                    camera.setUnitsPerPixelPrimary(unitsPerPixel);
                    camera.setCameraPrimaryZ(camera.getCameraPhysicalLocation().getLengthZ());
                }

                if (pass == 0 && movable != camera && zeroKnowledgeAutoFocusDepthMm != 0) {
                    // Auto-focus and set Z of camera.
                    Location location0 = initialLocation.add(new Location(LengthUnit.Millimeters, 0, 0, 0.5*zeroKnowledgeAutoFocusDepthMm, 0));
                    Location location1 = initialLocation.add(new Location(LengthUnit.Millimeters, 0, 0, -0.5*zeroKnowledgeAutoFocusDepthMm, 0));
                    initialLocation = new AutoFocusProvider().autoFocus(camera, movable, featureDiameter.multiply(4), location0, location1);
                    Location cameraHeadOffsetsNew = camera.getHeadOffsets().derive(initialLocation, false, false, true, false);
                    Logger.info("Setting camera "+camera.getName()+" Z to "+cameraHeadOffsetsNew.getLengthZ()+" (previously "+camera.getHeadOffsets().getLengthZ());
                    camera.setHeadOffsets(cameraHeadOffsetsNew);
                }

                if (pass > 0) {
                    if (!confirmed) {
                        throw new Exception("The camera rotation/mirroring detected earlier was not confirmed.");
                    }
                    // Fine-adjust rotation
                    double angle = Math.toDegrees(Math.atan2(dyX, dxX));
                    if (camera.isFlipX() ^ camera.isFlipY()) {
                        angle = -angle;
                    }
                    camera.setRotation(camera.getRotation() - angle);
                }
                // make sure to record a new image at rotation to reset width and height on the camera. 
                camera.lightSettleAndCapture();

                // Next displacement nearer to edge on the smaller of camera width, height.
                displacementAbsMm = Math.min(camera.getWidth(), camera.getHeight())*(pass + 1)*0.25
                        *unitsPerPixel.convertToUnits(LengthUnit.Millimeters).getX(); 
            }
            return featureDiameter;
        }
        finally {
            // Restore the camera location
            zeroKnowledgeMoveTo(movable, initialLocation, true);
        }
    }

    /**
     * A fiducial/nozzle tip locator using the same built-in subject detection as the auto-calibration. This supports vision calibration
     * early in the machine setup process, i.e. long before the user should be confronted with editing fiducial locator and 
     * nozzle tip calibration pipelines.
     * 
     * @param camera
     * @param movable
     * @param diagnostics
     * @param secondary TODO
     * @return
     * @throws Exception
     */
    public Location centerInOnSubjectLocation(ReferenceCamera camera, HeadMountable movable, Length subjectDiameter, String diagnostics, boolean secondary) 
            throws Exception {
        Location location = movable.getLocation();
        Circle expectedOffsetsAndDiameter =
                getExpectedOffsetsAndDiameter(camera, movable, location, subjectDiameter, secondary);
        for (int pass = 0; pass < zeroKnowledgeFiducialLocatorPasses ; pass++) {
            // Note, we cannot use the VisionUtils functionality yet, need to do it ourselves. 
            Circle detected = getSubjectPixelLocation(camera, movable, expectedOffsetsAndDiameter, 0, diagnostics);
            // Calculate the difference between the center of the image to the center of the match.
            double offsetX = detected.x - ((double) camera.getWidth() / 2);
            double offsetY = ((double) camera.getHeight() / 2) - detected.y;
            // And convert pixels to primary or secondary units 
            Location unitsPerPixel = secondary ?  camera.getUnitsPerPixelSecondary() : camera.getUnitsPerPixelPrimary();
            offsetX *= unitsPerPixel.getX();
            offsetY *= unitsPerPixel.getY();
            Location offset = new Location(unitsPerPixel.getUnits(), offsetX, offsetY, 0, 0);
            Location subjectLocation = camera.getLocation().add(offset);
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
            zeroKnowledgeMoveTo(movable, location, pass == 0);
        }
        return location;
    }

    public Circle getExpectedOffsetsAndDiameter(ReferenceCamera camera, HeadMountable movable,
            Location location, Length expectedDiameter, boolean secondary) {
        Circle expectedOffsetAndDiameter = null;
        if (expectedDiameter != null) {
            // Diameter given, try to calulcate by camera UPP. 
            Location l = (camera != movable ? 
                    movable.getLocation().subtract(location) 
                    : location.subtract(movable.getLocation()));
            // Get the right units per pixel.
            Location unitsPerPixel = secondary ? camera.getUnitsPerPixelSecondary() : camera.getUnitsPerPixelPrimary();
            expectedOffsetAndDiameter = new Circle(
                    l.getLengthX().divide(unitsPerPixel.getLengthX()), 
                    -l.getLengthY().divide(unitsPerPixel.getLengthY()), 
                    expectedDiameter.divide(unitsPerPixel.getLengthX()));
        }
        return expectedOffsetAndDiameter;
    }

    /**
     * Before we know anything about the camera, we cannot use pipelines, so we use the DetectCircularSymmetry stage 
     * directly. We can get a pixel locations for now.  
     * 
     * @param camera
     * @param movable 
     * @param extraSearchRange Specifies an extra search range, relative to the camera view size (minimum of width, height). 
     * @param diagnostics
     * @param subjectDiameter   Provides the fiducial diameter in pixels, if known, null otherwise. 
     * @return
     * @throws Exception
     */
    public Circle getSubjectPixelLocation(ReferenceCamera camera, HeadMountable movable, Circle expectedOffsetAndDiameter, double extraSearchRange, 
            String diagnostics) throws Exception {
        BufferedImage bufferedImage = camera.lightSettleAndCapture();
        Mat image = OpenCvUtils.toMat(bufferedImage);
        try {
            int subjectAreaDiameter = (int) (Math.min(image.cols(), image.rows())
                    * maxCameraRelativeFiducialAreaDiameter);
            int expectedDiameter = (expectedOffsetAndDiameter != null ? 
                    (int)expectedOffsetAndDiameter.getDiameter()
                    : subjectAreaDiameter/2);
            int maxDiameter = (int) (expectedDiameter*fiducialMargin + 1);
            int minDiameter = (int) (expectedOffsetAndDiameter != null ? 
                    expectedDiameter/fiducialMargin - 1
                    : 7);
            int maxDistance = (int) (maxDiameter*2*fiducialMargin
                    + Math.min(image.cols(), image.rows())*extraSearchRange);
            int expectedX = bufferedImage.getWidth()/2 + (int) (expectedOffsetAndDiameter != null ? expectedOffsetAndDiameter.getX() : 0);
            int expectedY = bufferedImage.getHeight()/2 + (int) (expectedOffsetAndDiameter != null ? expectedOffsetAndDiameter.getY() : 0);

            Circle result = null;
            if (movable instanceof Nozzle) {
                // A nozzle can have runout, we average rotated shots. 
                Location l = movable.getLocation();
                double x = 0;
                double y = 0;
                int n = 0;
                int da = 360/zeroKnowledgeRunoutCompensationShots;
                for (int angle = -180+da/2; angle < 180; angle += da) {
                    l = l.derive(new Location(l.getUnits(), 0, 0, 0, angle), false, false, false, true);
                    movable.moveTo(l);
                    bufferedImage = camera.lightSettleAndCapture();
                    image.release();
                    image = OpenCvUtils.toMat(bufferedImage);
                    result = getPixelLocationShot(camera, diagnostics, image, maxDiameter,
                            minDiameter, maxDistance, expectedX, expectedY);
                    // Accumulate
                    x += result.getX();
                    y += result.getY();
                    ++n;
                }
                // Average
                result.setX(x/n);
                result.setY(y/n);
            }
            else {
                // Fiducial can be detected by one shot.
                result = getPixelLocationShot(camera, diagnostics, image, maxDiameter,
                        minDiameter, maxDistance, expectedX, expectedY);
            }
            return result;
        }
        finally {
            image.release();
        }
    }

    private Circle getPixelLocationShot(ReferenceCamera camera, String diagnostics, Mat image,
            int maxDiameter, int minDiameter, int maxDistance, int expectedX, int expectedY) 
                    throws Exception, IOException {
        ScoreRange scoreRange = new ScoreRange();
        List<Circle> results = DetectCircularSymmetry.findCircularSymmetry(image, 
                expectedX, expectedY, 
                maxDiameter, minDiameter, maxDistance, maxDistance, maxDistance, 1,
                minSymmetry, 0.0, subSampling, superSampling, diagnostics != null, scoreRange);
        if (diagnostics != null) {
            if (LogUtils.isDebugEnabled()) {
                File file = Configuration.get().createResourceFile(getClass(), "loc_", ".png");
                Imgcodecs.imwrite(file.getAbsolutePath(), image);
            }                
            final BufferedImage diagnosticImage = OpenCvUtils.toBufferedImage(image);
            SwingUtilities.invokeLater(() -> {
                MainFrame.get()
                .getCameraViews()
                .getCameraView(camera)
                .showFilteredImage(diagnosticImage, diagnostics, diagnosticsMilliseconds);
            });
        }
        if (results.size() < 1) {
            throw new Exception("Subject not found.");
        }
        Circle result = results.get(0);
        return result;
    }

    /**
     * Get the detected Location of a subject. 
     * 
     * @param camera
     * @param movable
     * @param expectedLocation
     * @param expectedDiameter
     * @param diagnostics
     * @param secondary 
     * @return
     * @throws Exception
     */
    Location getDetectedLocation(ReferenceCamera camera, HeadMountable movable, Location expectedLocation, Length expectedDiameter, 
            String diagnostics, boolean secondary) throws Exception {
        Circle expectedFeature = getExpectedOffsetsAndDiameter(camera, movable, expectedLocation, expectedDiameter, secondary);
        Circle detected = getSubjectPixelLocation(camera, movable, expectedFeature, 0.0, diagnostics);
        Location subjectLocation = VisionUtils.getPixelLocation(camera, movable, detected.x, detected.y);
        // Make sure its in the expected units.
        return subjectLocation.convertToUnits(expectedLocation.getUnits());
    }

    /**
     * Moves the head-mountable to a location at safe Z using a conservative backlash-compensation scheme. Used to get precise
     * camera and camera subject positioning before proper backlash compensation can be configured and calibrated. 
     * 
     * @param hm
     * @param location
     * @param safeZ
     * @throws Exception
     */
    public void zeroKnowledgeMoveTo(HeadMountable hm, Location location, boolean safeZ) throws Exception {
        Location backlashCompensatedLocation = location.add(zeroKnowledgeBacklashOffsets);
        if (safeZ) {
            MovableUtils.moveToLocationAtSafeZ(hm, backlashCompensatedLocation);
        }
        else {
            hm.moveTo(backlashCompensatedLocation);
        }
        hm.moveTo(location, zeroKnowledgeBacklashSpeed);
    }
}
