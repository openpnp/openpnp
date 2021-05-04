/*
 * Copyright (C) 2021 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken@att.net>
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

package org.openpnp.gui.processes;

import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.CameraViewFilter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator;
import org.openpnp.model.Configuration;
import org.openpnp.model.Footprint;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.model.RegionOfInterest;
import org.openpnp.spi.Camera;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.util.Utils2D.AffineInfo;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.stages.MaskCircle;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import org.openpnp.vision.pipeline.ui.CvPipelineEditorDialog;
import org.pmw.tinylog.Logger;

/**
 * Guides the operator on calibrating a camera with step by step instructions.
 * 
 */
public abstract class CalibrateCameraProcess {
    static final boolean useSavedData = false;

    private static final int desiredTestPatternSize = 15;
    private static final int desiredPointsPerTestPattern = desiredTestPatternSize*desiredTestPatternSize;
    private static final double testPatternFillFraction = 0.90;
    private static final double trialStepSize = 0.2;
    private static final Location trialStep = new Location(LengthUnit.Millimeters, -trialStepSize, trialStepSize, 0, 0);
    private final MainFrame mainFrame;
    private final CameraView cameraView;
    private final Camera camera;
    private final boolean isHeadMountedCamera;
    private Nozzle nozzle;

    private int step = -1;
    private String[] moveableCameraInstructions = new String[] {
            "<html><body>Place the calibration rig face up on the machine table. It should be "
            + "positioned so that the image of the calibration fiducial is observable in all "
            + "four corners of the camera view by only jogging the camera in X/Y. It is important "
            + "that the calibration rig does not move due to machine vibrations etc. once the "
            + "calibration sequence has begun.  It is advisable to secure it to the table with "
            + "clamps, adhesive tape, or some other means.  Click Next when ready to proceed." +
                    "</body></html>",
            "<html><body>Measure the calibration rig's Z coordinate by jogging a nozzle tip over "
            + "the rig and carefully lowering it down until it is just touching the rig. Click "
            + "Next to capture the Z coordinate and retract the nozzle tip to safe Z." +
                    "</body></html>",
            "<html><body>Now jog the camera so that the calibration fiducial is centered in the "
            + "camera's field-of-view. Click Next to begin the automated calibration sequence." +
                    "</body></html>",
            "<html><body>Estimating units per pixel." +
                    "</body></html>",
            "<html><body>Locating the %s corner of the camera's view." +
                    "</body></html>",
            "<html><body>Collecting calibration point %d of %d." +
                    "</body></html>",};
    private String[] fixedCameraInstructions = new String[] {
            "<html><body>Select a nozzle and load it with the largest available nozzle tip. This "
            + "will be used to pick-up the calibration rig and move it into the camera's "
            + "field-of-view.  Click Next when ready to proceed." +
                    "</body></html>",
            "<html><body>Place the calibration rig face down on the machine table.  Jog the "
            + "selected nozzle tip over the center of the rig and carefully lower it until it is "
            + "just touching the rig. Click Next to pick-up the rig and retract it to safe Z." +
                    "</body></html>",
            "<html><body>Now jog the nozzle so that the calibration fiducial is centered in the "
            + "camera's field-of-view. Verify the fiducial stays within the green circle when the "
            + "nozzle is rotated through 360 degrees. If necessary, adjust the position of the "
            + "calibration rig on the nozzle tip until the fiducial stays within the green "
            + "circle. Click Next to begin the automated calibration sequence." +
                    "</body></html>",
            "<html><body>Estimating units per pixel." +
                    "</body></html>",
            "<html><body>Locating the %s corner of the camera's view." +
                    "</body></html>",
            "<html><body>Collecting calibration point %d of %d." +
                    "</body></html>",};
    private Part calibrationRig;
    private Package pkg;
    private Footprint footprint;
    private Shape shape;
    private CvPipeline pipeline;
    private double apparentMotionDirection;
    private double movableZ;
    private double testPatternZ;
    private int pixelsX;
    private int pixelsY;
    private Location centralLocation;
    private HeadMountable movable;
    private SwingWorker<Void, String> swingWorker;

    private Location trialLocation1;
    private Location trialLocation2;
    private org.opencv.core.Point trialPoint1;
    private org.opencv.core.Point trialPoint2;

    private double xScaling;

    private double yScaling;

    private ArrayList<Point2D> desiredCameraCorners;

    private ArrayList<Location> cameraCornerLocations;

    protected Mat transformImageToMachine;

    private int numberOfPointsPerTestPatternX;

    private int numberOfPointsPerTestPatternY;

    private int actualPointsPerTestPattern;

    protected ArrayList<Point2D> expectedImagePointsList;

    protected ArrayList<Location> testPatternLocationsList;

    protected ArrayList<Integer> testPatternIndiciesList;

    private boolean savedAutoVisible;

    private boolean savedAutoToolSelect;
    
    private Point2D imageCenterPoint;
    
    private MaskCircle firstMaskCircle;

    private MaskCircle secondMaskCircle;

    private int maskDiameter;

    public CalibrateCameraProcess(MainFrame mainFrame, CameraView cameraView, Part calibrationRigPart)
            throws Exception {
        this.mainFrame = mainFrame;
        this.cameraView = cameraView;
        this.calibrationRig = calibrationRigPart;
        
        camera = cameraView.getCamera();
        pixelsX = camera.getWidth();
        pixelsY = camera.getHeight();
        imageCenterPoint = new Point2D.Double((pixelsX-1.0)/2, (pixelsY-1.0)/2);

        double areaInSquarePixels = pixelsX * pixelsY;
        double pixelsPerTestPoint = Math.sqrt(areaInSquarePixels / desiredPointsPerTestPattern);
        numberOfPointsPerTestPatternX = 2 * ((int) (pixelsX / (2*pixelsPerTestPoint))) + 1;
        numberOfPointsPerTestPatternY = 2 * ((int) (pixelsY / (2*pixelsPerTestPoint))) + 1;
        actualPointsPerTestPattern = numberOfPointsPerTestPatternX * numberOfPointsPerTestPatternY;

        isHeadMountedCamera = camera.getHead() != null;
        apparentMotionDirection = isHeadMountedCamera ? -1.0 : +1.0;
        pkg = calibrationRigPart.getPackage();
        footprint = pkg.getFootprint();
        shape = footprint.getShape();
        pipeline = ((ReferenceFiducialLocator) Configuration.get().getMachine().getFiducialLocator()).getPartSettings(calibrationRigPart).getPipeline();
        pipeline.setProperty("camera", camera);
        pipeline.setProperty("part", calibrationRigPart);
        pipeline.setProperty("package", pkg);
        pipeline.setProperty("footprint", footprint);
        
        firstMaskCircle = (MaskCircle)pipeline.getStage("first_mask");
        secondMaskCircle = (MaskCircle)pipeline.getStage("second_mask");
        maskDiameter = firstMaskCircle.getDiameter();
        
        savedAutoToolSelect = Configuration.get().getMachine().isAutoToolSelect();
        ((ReferenceMachine) Configuration.get().getMachine()).setAutoToolSelect(false);
//        savedAutoVisible = camera.isAutoVisible();
//        ((ReferenceCamera) camera).setAutoVisible(false);
        
        SwingUtilities.invokeLater(() -> {
            MainFrame.get().getCameraViews().ensureCameraVisible(camera);
        });
        
        advance();
    }

    /**
     * This method is called when the raw calibration data collection has completed and must be 
     * overridden to process the raw calibration data into a usable form
     * 
     * @param testPattern3dPoints - an N x 1 MatOfPoint3f containing the 3D machine coordinates of 
     * the corresponding image points in testPatternImagePoints
     * 
     * @param testPatternImagePoints - an N x 1 MatOfPoint2f containing the 2D image points of the 
     * corresponding machine coordinates in testPatternImagePoints
     * 
     * @param xScaling - a very rough signed approximation to the camera's X units-per-pixel
     *  
     * @param yScaling - a very rough signed approximation to the camera's Y units-per-pixel

     * @param size - the size of the images
     */
    protected abstract void processRawCalibrationData(MatOfPoint3f testPattern3dPoints, 
            MatOfPoint2f testPatternImagePoints, double testPatternZ, double xScaling, 
            double yScaling, Size size);
    
    /**
     * This method is called when the raw calibration data collection has been canceled and must 
     * be overridden to perform any necessary clean-up
     */
    protected abstract void processCanceled();
    
    /**
     * Advances through the steps and displays the appropriate instructions for each step
     */
    private void advance() {
        boolean stepResult = true;
        switch (step) {
            case 0:
                stepResult = step1();
                break;
            case 1:
                stepResult = step2();
                break;
            case 2:
                stepResult = step3();
                break;
            case 3:
                stepResult = step4();
                break;
            case 4:
                stepResult = step5();
                break;
            default :
                break;
        }

        if (!stepResult) {
            return;
        }
        step++;
        if (step > 5) {
            cleanup();
        }
        else {
            mainFrame.showInstructions("Camera Calibration Instructions",
                    isHeadMountedCamera ? moveableCameraInstructions[step] : fixedCameraInstructions[step],
                    true, true, "Next", cancelActionListener, proceedActionListener);
        }
    }

    /**
     * Action to take when transitioning from step 0 to step 1
     * 
     * @return true if the action was successful and the state machine should move to the next step
     */
    private boolean step1() {
        //Get the default nozzle
        try {
            nozzle = Configuration.get().getMachine().getDefaultHead().getDefaultNozzle();
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            cancel();
        }
        
        MainFrame.get().getMachineControls().setSelectedTool(nozzle);
        MovableUtils.fireTargetedUserAction(nozzle);
        
        ((ReferenceCamera) camera).getCalibration().setEnabled(false);

        return true;
    }

    /**
     * Action to take after the operator has placed the nozzle tip on the calibration rig
     * 
     * @return true if the action was successful and the state machine should move to the next step
     */
    private boolean step2() {
        nozzle = (Nozzle) MainFrame.get().getMachineControls().getSelectedTool();

        if (isHeadMountedCamera) {
            //Capture the nozzle's Z coordinate as the test pattern's Z coordinate
            testPatternZ = nozzle.getLocation().convertToUnits(LengthUnit.Millimeters).getZ();
            movableZ = 0;
            movable = camera;
        }
        else {
            testPatternZ = camera.getDefaultZ().convertToUnits(LengthUnit.Millimeters).getValue();
            movableZ = testPatternZ + 
                    calibrationRig.getHeight().convertToUnits(LengthUnit.Millimeters).getValue();
            movable = nozzle;
            
            //Pick-up the calibration rig with the nozzle 
            UiUtils.submitUiMachineTask(() -> {
                nozzle.pick(calibrationRig);
            });

            showCircle(new org.opencv.core.Point(imageCenterPoint.getX(), imageCenterPoint.getY()), 
                    (int)(0.75*maskDiameter/2), Color.GREEN);
        }
        
        Logger.trace("testPatternZ = " + testPatternZ);
        Logger.trace("movableZ = " + movableZ);
        
        //Return the nozzle to safe Z
        UiUtils.submitUiMachineTask(() -> {
            nozzle.moveToSafeZ();
        });
        
        
        return true;
    }

    /**
     * Action to take after the operator has positioned the nozzle/camera so that the
     * calibration rig's fiducial is centered in the camera's FOV
     * 
     * @return true if the action was successful and the state machine should move to the next step
     */
    private boolean step3() {
        ((ReferenceCamera) camera).setAutoVisible(savedAutoVisible);
        
        //Capture the movable's coordinate as the approximate center of the camera's field-of-view
        centralLocation = movable.getLocation().convertToUnits(LengthUnit.Millimeters).
                derive(null, null, movableZ, 0.0);
        
        trialLocation1 = centralLocation.subtract(trialStep);
        trialLocation2 = centralLocation.add(trialStep);
        trialPoint1 = null;
        trialPoint2 = null;
        
        swingWorker = new SwingWorker<Void, String>() { 
            @Override
            protected Void doInBackground() throws Exception  
            {
                publish("Checking first test point.");
                Logger.trace("Checking first test point.");
                
                Future<Void> future = UiUtils.submitUiMachineTask(() -> {
                    movable.moveTo(trialLocation1);
                });
                future.get();
                
                if (isCancelled()) {
                    return null;
                }
                
                while ((trialPoint1 == null) && !isCancelled()) {
                    trialPoint1 = findCalibrationRigFiducialPoint(calibrationRig, imageCenterPoint);
                    
                    if (trialPoint1 == null) {
                        if (MessageBoxes.errorBoxWithRetry(mainFrame, 
                                "ERROR - Failed to find calibration rig's fiducial.",
                                "Edit the pipeline and try again?")) {
                            CvPipelineEditor pipelineEditor = new CvPipelineEditor(pipeline);
                            JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), 
                                    "Camera Calibration Rig Fiducial Pipeline", pipelineEditor);
                            dialog.setModalityType(ModalityType.APPLICATION_MODAL);
                            
                            //Because the dialog is modal, this thread of execution will hang
                            //here until the dialog is closed, at which point it will resume
                            dialog.setVisible(true);
                        }
                        else {
                            return null;
                        }
                    }
                }
                
                publish("Checking second test point.");
                Logger.trace("Checking second test point.");
                
                future = UiUtils.submitUiMachineTask(() -> {
                    movable.moveTo(trialLocation2);
                });
                future.get();
                
                if (isCancelled()) {
                    return null;
                }
                
                while ((trialPoint2 == null) && !isCancelled()) {
                    trialPoint2 = findCalibrationRigFiducialPoint(calibrationRig, imageCenterPoint);
                    
                    if (trialPoint2 == null) {
                        if (MessageBoxes.errorBoxWithRetry(mainFrame, 
                                "ERROR - Failed to find calibration rig's fiducial.",
                                "Edit the pipeline and try again?")) {
                            CvPipelineEditor pipelineEditor = new CvPipelineEditor(pipeline);
                            JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), 
                                    "Camera Calibration Rig Fiducial Pipeline", pipelineEditor);
                            dialog.setModalityType(ModalityType.APPLICATION_MODAL);
                            
                            //Because the dialog is modal, this thread of execution will hang
                            //here until the dialog is closed, at which point it will resume
                            dialog.setVisible(true);
                        }
                        else {
                            return null;
                        }
                    }
                }
                
                //Compute a rough estimate of unitsPerPixel (these may be negative but that's ok)
                Location trialLocationDifference = trialLocation2.
                        subtract(trialLocation1);
                xScaling = trialLocationDifference.getX() /
                        (trialPoint2.x - trialPoint1.x);
                yScaling = trialLocationDifference.getY() /
                        (trialPoint2.y - trialPoint1.y);
                Logger.trace("xScaling = " + xScaling);
                Logger.trace("yScaling = " + yScaling);
                
                centralLocation = walkToPoint(imageCenterPoint, xScaling, yScaling);

                return null;
            }
            
            @Override
            protected void process(List<String> chunks) {
                for (String str : chunks) {
                    mainFrame.showInstructions("Camera Calibration Instructions", str,
                            true, false, "Next", cancelActionListener, proceedActionListener);
                }
            }
            
            @Override
            protected void done()  
            {
                if ((trialPoint1 == null) || (trialPoint2 == null)) {
                    CalibrateCameraProcess.this.cancel();
                    return;
                }
                
                advance();
            }
        };
        
        swingWorker.execute();
        
        return true;
    }

    /**
     * Action to locate the four corners of the camera's field-of-view
     * 
     * @return true if the action was successful and the state machine should move to the next step
     */
    private boolean step4() {
        desiredCameraCorners = new ArrayList<Point2D>();
        
        //In order: upper left, lower left, lower right, and upper right
        desiredCameraCorners.add(new Point2D.Double((1-testPatternFillFraction)*pixelsX/2,
                (1-testPatternFillFraction)*pixelsY/2));
        desiredCameraCorners.add(new Point2D.Double((1-testPatternFillFraction)*pixelsX/2,
                (1+testPatternFillFraction)*pixelsY/2));
        desiredCameraCorners.add(new Point2D.Double((1+testPatternFillFraction)*pixelsX/2,
                (1+testPatternFillFraction)*pixelsY/2));
        desiredCameraCorners.add(new Point2D.Double((1+testPatternFillFraction)*pixelsX/2,
                (1-testPatternFillFraction)*pixelsY/2));
        Logger.trace("desiredCameraCorners = " + desiredCameraCorners);

        cameraCornerLocations = new ArrayList<Location>();

        //Find the four corners and then setup for the test pattern
        swingWorker = new SwingWorker<Void, String>() { 
            @Override
            protected Void doInBackground() throws Exception  
            {
                String str = "";
                int cornerIdx = 0;
                while ((cornerIdx < 4) && !isCancelled()) {
                    switch (cornerIdx) {
                        case 0:
                            str = "Finding Upper Left Corner";
                            break;
                        case 1:
                            str = "Finding Lower Left Corner";
                            break;
                        case 2:
                            str = "Finding Lower Right Corner";
                            break;
                        case 3:
                            str = "Finding Upper Right Corner";
                            break;
                    }

                    publish(str);
                    
                    Logger.trace("Walking from " + centralLocation + " to " + desiredCameraCorners.get(cornerIdx) + " " + str);
                    
                    Location corner = walkToPoint(desiredCameraCorners.get(cornerIdx), 
                            xScaling, yScaling);
                    
                    if (isCancelled()) {
                        return null;
                    }
                    
                    if (corner != null) {
                        cameraCornerLocations.add(corner);
                        cornerIdx++;
                    }
                    else {
                        if (MessageBoxes.errorBoxWithRetry(mainFrame, 
                                "ERROR - Failed to find calibration rig's fiducial.",
                                "Edit the pipeline and try again?")) {
                            CvPipelineEditor pipelineEditor = new CvPipelineEditor(pipeline);
                            JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), 
                                    "Camera Calibration Rig Fiducial Pipeline", pipelineEditor);
                            dialog.setModalityType(ModalityType.APPLICATION_MODAL);
                            
                            //Because the dialog is modal, this thread of execution will hang
                            //here until the dialog is closed, at which point it will resume
                            dialog.setVisible(true);
                        }
                        else {
                            return null;
                        }
                    }
                }
                return null;
            }
            
            @Override
            protected void process(List<String> chunks) {
                for (String str : chunks) {
                    mainFrame.showInstructions("Camera Calibration Instructions", str,
                            true, false, "Next", cancelActionListener, proceedActionListener);
                }
            }
            
            @Override
            protected void done()  
            {
                Logger.trace("cameraCornerLocations = " + cameraCornerLocations);
                
                if (cameraCornerLocations.size() != 4) {
                    CalibrateCameraProcess.this.cancel();
                    return;
                }
                //Find a perspective transform that maps the four image corners to the 
                //corresponding machine X/Y coordinates
                org.opencv.core.Point srcCornerPoint[] = new org.opencv.core.Point[4];
                org.opencv.core.Point dstCornerPoint[] = new org.opencv.core.Point[4];
                for (int i=0; i<4; i++) {
                    Point2D srcCorner = desiredCameraCorners.get(i);
                    srcCornerPoint[i] = new org.opencv.core.Point(srcCorner.getX(), srcCorner.getY());
                    Location dstCorner = cameraCornerLocations.get(i);
                    dstCornerPoint[i] = new org.opencv.core.Point(dstCorner.getX(), dstCorner.getY());
                }
                MatOfPoint2f src = new MatOfPoint2f();
                src.fromArray(srcCornerPoint);
                MatOfPoint2f dst = new MatOfPoint2f();
                dst.fromArray(dstCornerPoint);

                Logger.trace("src = " + src.dump());
                Logger.trace("dst = " + dst.dump());
                transformImageToMachine = Imgproc.getPerspectiveTransform(src, dst);
                src.release();
                dst.release();
                
                double testPatternStepX = testPatternFillFraction * (pixelsX - 1) / 
                        (numberOfPointsPerTestPatternX - 1);
              
                double testPatternStepY = testPatternFillFraction * (pixelsY - 1) / 
                        (numberOfPointsPerTestPatternY - 1);
                
                double testPatternOffsetX = 0.5 * (1 - testPatternFillFraction) * (pixelsX - 1);
                
                double testPatternOffsetY = 0.5 * (1 - testPatternFillFraction) * (pixelsY - 1);
                
                expectedImagePointsList = new ArrayList<Point2D>();
                Mat expectedImagePoints = Mat.zeros(3, actualPointsPerTestPattern, CvType.CV_64FC1);
                int nPoints = 0;
                for (int iRow = 0; iRow < numberOfPointsPerTestPatternY; iRow++) {
                    for (int iCol = 0; iCol < numberOfPointsPerTestPatternX; iCol++) {
                        expectedImagePointsList.add(new Point(
                                (int) (iCol*testPatternStepX + testPatternOffsetX), 
                                (int) (iRow*testPatternStepY + testPatternOffsetY)) );
                        expectedImagePoints.put(0, nPoints, iCol*testPatternStepX + testPatternOffsetX);
                        expectedImagePoints.put(1, nPoints, iRow*testPatternStepY + testPatternOffsetY);
                        expectedImagePoints.put(2, nPoints, 1);
                        nPoints++;
                    }
                }
                Logger.trace("expectedImagePointsList = " + expectedImagePointsList);
                
                Mat expectedMachinePoints = Mat.zeros(3,  actualPointsPerTestPattern, CvType.CV_64FC1);
                Core.gemm(transformImageToMachine, expectedImagePoints, 1.0, expectedImagePoints, 0, expectedMachinePoints);
                expectedImagePoints.release();
                        
                testPatternLocationsList = new ArrayList<Location>();
                testPatternIndiciesList = new ArrayList<Integer>();
                nPoints = 0;
                for (int iRow = 0; iRow < numberOfPointsPerTestPatternY; iRow++) {
                    for (int iCol = 0; iCol < numberOfPointsPerTestPatternX; iCol++) {
                        double scale = 1.0 / expectedMachinePoints.get(2, nPoints)[0];
                        testPatternLocationsList.add(new Location(LengthUnit.Millimeters, 
                                scale * expectedMachinePoints.get(0, nPoints)[0], 
                                scale * expectedMachinePoints.get(1, nPoints)[0], 
                                movableZ, 0));
                        testPatternIndiciesList.add(nPoints);
                        nPoints++;
                    }
                }
                expectedMachinePoints.release();
                
                //To help prevent any machine bias from creeping into the measurements, the test
                //pattern is shuffled so that the points are visited in a random but deterministic
                //order
                Collections.shuffle(testPatternIndiciesList, new Random(1));
                
                Logger.trace("testPattern = " + testPatternLocationsList);
                
                advance();
            }
            
        };
        
        swingWorker.execute();
        
        return true;
    }

    /**
     * Action to take when transitioning from step 4 to step 5
     * 
     * @return true if the action was successful and the state machine should move to the next step
     */
    private boolean step5() {
        //Collect the calibration points
 
        MatOfPoint3f testPattern3dPoints = new MatOfPoint3f();
        MatOfPoint2f testPatternImagePoints = new MatOfPoint2f();
        int angleIncrement;
        double observationWeight;
        if (isHeadMountedCamera) {
            angleIncrement = 360;
        }
        else {
            angleIncrement = 180;
        }
        int numberOfAngles = 360 / angleIncrement;
        observationWeight = 1.0 / numberOfAngles;
        
        swingWorker = new SwingWorker<Void, String>() { 
            @Override
            protected Void doInBackground() throws Exception  
            {
                org.opencv.core.Point observedPoint = null;
                
                int angle = 0;
                
                for (int iPoint = 0; (iPoint < actualPointsPerTestPattern) && !isCancelled(); iPoint++) {
                    publish(String.format("Collecting calibration point %d of %d", iPoint, actualPointsPerTestPattern));

                    Location testLocation = testPatternLocationsList.get(testPatternIndiciesList.get(iPoint));
                    
                    org.opencv.core.Point measuredPoint = new org.opencv.core.Point(0, 0);
                    
                    int count = 0;
                    do {
                        final Location moveLocation = testLocation.derive(null, null, null, (double) angle);
    
                        //Move the machine and capture the fiducial location
                        Future<Void> future = UiUtils.submitUiMachineTask(() -> {
                            movable.moveTo(moveLocation);
                        });
    
                        //Wait for the move to complete
                        future.get();
    
                        //Find the calibration rig's fiducial
                        Point2D expectedPoint = expectedImagePointsList.get(testPatternIndiciesList.get(iPoint));
                        observedPoint = findCalibrationRigFiducialPoint(calibrationRig, expectedPoint );
                        
                        if (observedPoint == null) {
                            break;
                        }
                        measuredPoint.x += observationWeight * observedPoint.x;
                        measuredPoint.y += observationWeight * observedPoint.y;
                        
                        count++;
                        if (count < numberOfAngles) {
                            angle += angleIncrement;
                            if (angle >= 360) {
                                angle = 0;
                            }
                        }
                    } while (count < numberOfAngles);
                    
                    //Save the test pattern location and the corresponding image point
                    if (observedPoint != null) {
                        testPattern3dPoints.push_back(
                                new MatOfPoint3f(
                                        new Point3(apparentMotionDirection*testLocation.getX(), 
                                                apparentMotionDirection*testLocation.getY(), 
                                                0)));
                        testPatternImagePoints.push_back(
                                new MatOfPoint2f(measuredPoint));
                    }
                }
                Logger.trace("testPattern3dPoints = " + testPattern3dPoints.dump());
                Logger.trace("testPatternImagePoints = " + testPatternImagePoints.dump());
                return null;
            }
            
            @Override
            protected void process(List<String> chunks) {
                for (String str : chunks) {
                    mainFrame.showInstructions("Camera Calibration Instructions", str,
                            true, false, "Next", cancelActionListener, proceedActionListener);
                }
            }
            
            @Override
            protected void done()  
            {
                if (!this.isCancelled()) {
                    processRawCalibrationData(testPattern3dPoints, testPatternImagePoints, testPatternZ, 
                        xScaling, yScaling, new Size(pixelsX, pixelsY));
                }
                CalibrateCameraProcess.this.cancel();
            }
            
        };
        
        swingWorker.execute();
        
        return true;
    }

    private Location walkToPoint(Point2D desiredCameraPoint, double xScaling,
            double yScaling) throws InterruptedException, ExecutionException {
        //Move the machine to the starting location and wait for it to finish
        Future<?> future = UiUtils.submitUiMachineTask(() -> {
            movable.moveTo(centralLocation);
        });
        future.get();

        Location oldLocation = null;
        Location newLocation = movable.getLocation().convertToUnits(LengthUnit.Millimeters);
        Point2D expectedPoint = (Point2D) imageCenterPoint.clone();
        Logger.trace("expectedPoint = " + expectedPoint);
        org.opencv.core.Point foundPoint = findCalibrationRigFiducialPoint(calibrationRig, expectedPoint);
        Logger.trace("newPoint = " + foundPoint);
        if (foundPoint == null) {
            return null;
        }
        double errorX = foundPoint.x - desiredCameraPoint.getX();
        double errorY = foundPoint.y - desiredCameraPoint.getY();
        double oldError2 = java.lang.Double.POSITIVE_INFINITY;
        double newError2 = errorX*errorX + errorY*errorY;
        
        final double loopGain = 0.25;
        
        while (newError2 < oldError2) {
            if (Thread.interrupted()) {
                return null;
            }
            oldError2 = newError2;
            oldLocation = newLocation;
            expectedPoint.setLocation(foundPoint.x-errorX*loopGain, foundPoint.y-errorY*loopGain);
            Location offset = new Location(LengthUnit.Millimeters, -errorX * xScaling * loopGain, -errorY * yScaling * loopGain, 0, 0);
            newLocation = oldLocation.add(offset);
            if (newLocation.getLinearDistanceTo(oldLocation) < 0.01) {
                return oldLocation;
            }
            final Location moveLocation = newLocation;
            //Move the machine and wait for it to finish
            future = UiUtils.submitUiMachineTask(() -> {
                movable.moveTo(moveLocation);
            });
            future.get();
            
            newLocation = movable.getLocation().convertToUnits(LengthUnit.Millimeters);
            foundPoint = findCalibrationRigFiducialPoint(calibrationRig, expectedPoint);
            Logger.trace("expectedPoint = " + expectedPoint);
            Logger.trace("newPoint = " + foundPoint);
            if (foundPoint == null) {
                return null;
            }
            errorX = foundPoint.x - desiredCameraPoint.getX();
            errorY = foundPoint.y - desiredCameraPoint.getY();
            newError2 = errorX*errorX + errorY*errorY;
        }
        return oldLocation;
    }

    private org.opencv.core.Point findCalibrationRigFiducialPoint(Part part, Point2D expectedPoint) {
        pipeline.setProperty("MaskCircle.center", new org.opencv.core.Point(expectedPoint.getX(), expectedPoint.getY()));

        List<KeyPoint> keypoints = null;
        int attempts = 0;
        
        while ((keypoints == null) && (attempts < 3)) {
            if (Thread.interrupted()) {
                return null;
            }
            pipeline.process();
            // Get the results
            try {
                keypoints = pipeline.getExpectedResult(VisionUtils.PIPELINE_RESULTS_NAME)
                        .getExpectedListModel(KeyPoint.class, 
                                new Exception(part.getId()+" no matches found."));
            }
            catch (Exception e) {
                keypoints = null;
            }
            
            if (keypoints != null) {
                Logger.trace("keypoints = " + keypoints);
                double minDistance = Double.POSITIVE_INFINITY;
                KeyPoint bestKeyPoint = null;
                for (KeyPoint kpt : keypoints) {
                    double dx = kpt.pt.x - expectedPoint.getX();
                    double dy = kpt.pt.y - expectedPoint.getY();
                    double distance = Math.sqrt(dx*dx + dy*dy);
                    if (distance < minDistance) {
                        bestKeyPoint = kpt;
                        minDistance = distance;
                    }
                }
                Color pointColor = Color.GREEN;
                if (2*minDistance + bestKeyPoint.size > 0.80*maskDiameter) {
                    pointColor = Color.YELLOW;
                }
                showPointAndCircle(bestKeyPoint.pt, new org.opencv.core.Point(expectedPoint.getX(), expectedPoint.getY()), maskDiameter/2, pointColor, Color.RED);
                return bestKeyPoint.pt;
            }
            attempts++;
        }
        return null;
    }

    /**
     * Clean-up when the process is done/cancelled
     */
    private void cleanup() {
        ((ReferenceMachine) Configuration.get().getMachine()).setAutoToolSelect(savedAutoToolSelect);
//        ((ReferenceCamera) camera).setAutoVisible(savedAutoVisible);

        restoreCameraView();
        if ((swingWorker != null) && !swingWorker.isDone()) {
            swingWorker.cancel(true);
        }
        else {
            mainFrame.hideInstructions();
        }
    }

    /**
     * Clean-up when the process is cancelled
     */
    private void cancel() {
        cleanup();
        processCanceled();
    }

    /**
     * Process Proceed button clicks
     */
    private final ActionListener proceedActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            advance();
        }
    };

    /**
     * Process Cancel button clicks
     */
    private final ActionListener cancelActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            cancel();
        }
    };

    private void showPoint(org.opencv.core.Point point, Color color) {
        showPointAndCircle(point, null, 0, color);
    }
    
    protected void showCircle(org.opencv.core.Point center, int radius, Color color) {
        showPointAndCircle(null, center, radius, color);
    }
    
    private void showPointAndCircle(org.opencv.core.Point point, org.opencv.core.Point center, int radius, Color color) {
        showPointAndCircle(point, center, radius, color, color);
    }
    
    private void showPointAndCircle(org.opencv.core.Point point, org.opencv.core.Point center, int radius, Color pointColor, Color circleColor) {
        if ((point != null) || (center != null)) {
            cameraView.setCameraViewFilter(new CameraViewFilter() {
                @Override
                public BufferedImage filterCameraImage(Camera camera, BufferedImage image) {
                    Mat mat = OpenCvUtils.toMat(image);
                    if (point != null) {
                        org.opencv.core.Point p1 = new org.opencv.core.Point(point.x-20, point.y);
                        org.opencv.core.Point p2 = new org.opencv.core.Point(point.x+20, point.y);
                        Imgproc.line(mat, p1, p2, FluentCv.colorToScalar(pointColor), 2);
                        p1 = new org.opencv.core.Point(point.x, point.y-20);
                        p2 = new org.opencv.core.Point(point.x, point.y+20);
                        Imgproc.line(mat, p1, p2, FluentCv.colorToScalar(pointColor), 2);
                    }
                    if (center != null) {
                        Imgproc.circle(mat, center, radius, FluentCv.colorToScalar(circleColor), 2);
                    }
                    BufferedImage result = OpenCvUtils.toBufferedImage(mat);
                    mat.release();
                    return result;
                }
            });
        }
    }
    
    protected void restoreCameraView() {
        cameraView.setCameraViewFilter(null);
    }

}
