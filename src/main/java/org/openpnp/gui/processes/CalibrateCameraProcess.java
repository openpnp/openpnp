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

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.CameraViewFilter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.camera.calibration.AdvancedCalibration;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator;
import org.openpnp.model.Configuration;
import org.openpnp.model.Footprint;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
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
    private static final int changeMaskThreshold = 3;

    private final int numberOfCalibrationHeights;
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
                    "</body></html>",
            "<html><body>Place a spacer under the calibration rig to raise it to about the height "
            + "of the tallest object you will ever want to image with the camera. Again be sure the "
            + "calibration rig is secured so that it can not move during the remainder of the "
            + "calibration process. Click Next when ready to proceed." +
                    "</body></html>",};
    private String[] fixedCameraInstructions = new String[] {
            "<html><body>Select a nozzle and load it with the largest available nozzle tip. This "
            + "will be used to pick-up the calibration rig and move it into the camera's "
            + "field-of-view.  Click Next when ready to proceed." +
                    "</body></html>",
            "<html><body>Place the calibration rig face down on the machine table.  Jog the "
            + "selected nozzle tip over the center of the rig and carefully lower it until it is "
            + "touching the rig. Click Next to pick-up the rig and retract it to safe Z." +
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
                    "</body></html>",
            "<html><body>Changing nozzle height." +
                    "</body></html>",};
    private Part calibrationRig;
    private Package pkg;
    private Footprint footprint;
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
    
    private int maskDiameter;
    
    private int calibrationHeightIndex;

    private List<List<double[]>> testPattern3dPointsList;
    private List<List<double[]>> testPatternImagePointsList;

    private int angleIncrement;

    private double observationWeight;

    private int numberOfAngles;
    
    int angle = 0;

    private ArrayList<Length> calibrationHeights;
    
    private AdvancedCalibration advCal;
    
    private int changeMaskSize = 0;
    
    public CalibrateCameraProcess(MainFrame mainFrame, CameraView cameraView, 
            Part calibrationRigPart, List<Length> calibrationHeights)
            throws Exception {
        this.mainFrame = mainFrame;
        this.cameraView = cameraView;
        this.calibrationRig = calibrationRigPart;
        this.calibrationHeights = new ArrayList<Length>(calibrationHeights);
        numberOfCalibrationHeights = calibrationHeights.size();
        
        camera = cameraView.getCamera();
        pixelsX = camera.getWidth();
        pixelsY = camera.getHeight();
        imageCenterPoint = new Point2D.Double((pixelsX-1.0)/2, (pixelsY-1.0)/2);

        advCal = ((ReferenceCamera)camera).getAdvancedCalibration();

        double areaInSquarePixels = pixelsX * pixelsY;
        double pixelsPerTestPoint = Math.sqrt(areaInSquarePixels / 
                advCal.getDesiredPointsPerTestPattern());
        numberOfPointsPerTestPatternX = 2 * ((int) (pixelsX / (2*pixelsPerTestPoint))) + 1;
        numberOfPointsPerTestPatternY = 2 * ((int) (pixelsY / (2*pixelsPerTestPoint))) + 1;
        actualPointsPerTestPattern = numberOfPointsPerTestPatternX * numberOfPointsPerTestPatternY;

        testPattern3dPointsList = new ArrayList<>();
        testPatternImagePointsList = new ArrayList<>();
        
        calibrationHeightIndex = 0;
        
        isHeadMountedCamera = camera.getHead() != null;
        apparentMotionDirection = isHeadMountedCamera ? -1.0 : +1.0;
        
        angleIncrement = isHeadMountedCamera ? 360 : 180;
        numberOfAngles = 360 / angleIncrement;
        observationWeight = 1.0 / numberOfAngles;
        
        pkg = calibrationRigPart.getPackage();
        footprint = pkg.getFootprint();
        pipeline = ((ReferenceFiducialLocator) Configuration.get().getMachine().
                getFiducialLocator()).getPartSettings(calibrationRigPart).getPipeline();
        pipeline.setProperty("camera", camera);
        pipeline.setProperty("part", calibrationRigPart);
        pipeline.setProperty("package", pkg);
        pipeline.setProperty("footprint", footprint);
        
        maskDiameter = Math.min(pixelsX, pixelsY) / 5;
        
        savedAutoToolSelect = Configuration.get().getMachine().isAutoToolSelect();
        ((ReferenceMachine) Configuration.get().getMachine()).setAutoToolSelect(false);
        
        SwingUtilities.invokeLater(() -> {
            MainFrame.get().getCameraViews().ensureCameraVisible(camera);
        });
        
        advance();
    }

    /**
     * This method is called when the raw calibration data collection has completed and must be 
     * overridden to process the raw calibration data into a usable form
     * 
     * @param testPattern3dPoints - A List of N x 1 MatOfPoint3f containing the 3D machine 
     * coordinates of the corresponding image points in testPatternImagePoints
     * 
     * @param testPatternImagePoints - A List of N x 1 MatOfPoint2f containing the 2D image points 
     * of the corresponding machine coordinates in testPatternImagePoints
     * 
     * @param size - the size of the images
     */
    protected abstract void processRawCalibrationData(double[][][] testPattern3dPoints, 
            double[][][] testPatternImagePoints, Size size);
    
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
            case 5:
                stepResult = step6();
                break;
            case 6:
                stepResult = step7();
                break;
            default :
                break;
        }

        if (!stepResult) {
            return;
        }
        step++;
        if (step > 6) {
            cleanup();
        }
        else {
            mainFrame.showInstructions("Camera Calibration Instructions",
                    isHeadMountedCamera ? moveableCameraInstructions[step] : 
                        fixedCameraInstructions[step],
                    true, true, "Next", cancelActionListener, proceedActionListener);
        }
    }

    /**
     * Action to take after the operator has placed the calibration rig on the machine
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
        
        if (isHeadMountedCamera && Double.isFinite(calibrationHeights.get(calibrationHeightIndex).
                getValue())) {
            testPatternZ = calibrationHeights.get(calibrationHeightIndex).
                    convertToUnits(LengthUnit.Millimeters).getValue();
            movableZ = 0;
            movable = camera;
            step++; //skip the step where the operator measures the height with the nozzle tip
        }
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
            try {
                Future<Void> future = UiUtils.submitUiMachineTask(() -> {
                    nozzle.pick(calibrationRig);
                });
                future.get();
            }
            catch (Exception e) {
                //TODO
            }
            
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
        
        trialLocation1 = centralLocation.subtract(advCal.getTrialStep());
        trialLocation2 = centralLocation.add(advCal.getTrialStep());
        trialPoint1 = null;
        trialPoint2 = null;
        
        swingWorker = new SwingWorker<Void, String>() { 
            @Override
            protected Void doInBackground() throws Exception  
            {
                publish("Checking first test point.");
                Logger.trace("Checking first test point.");
                
                while ((trialPoint1 == null) && !isCancelled()) {
                    trialPoint1 = findAveragedFiducialPoint(trialLocation1, 
                            imageCenterPoint);

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
                
                while ((trialPoint2 == null) && !isCancelled()) {
                    trialPoint2 = findAveragedFiducialPoint(trialLocation2, 
                            imageCenterPoint);
                    
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
                
                publish("Walking to image center.");
                Logger.trace("Walking to image center.");
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
        
        double testPatternFillFraction = advCal.getTestPatternFillFraction();
        
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
                    
                    Logger.trace("Walking from " + centralLocation + " to " + 
                            desiredCameraCorners.get(cornerIdx) + " " + str);
                    
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
                    srcCornerPoint[i] = new org.opencv.core.Point(srcCorner.getX(), 
                            srcCorner.getY());
                    Location dstCorner = cameraCornerLocations.get(i);
                    dstCornerPoint[i] = new org.opencv.core.Point(dstCorner.getX(), 
                            dstCorner.getY());
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
                        expectedImagePoints.put(0, nPoints, 
                                iCol*testPatternStepX + testPatternOffsetX);
                        expectedImagePoints.put(1, nPoints, 
                                iRow*testPatternStepY + testPatternOffsetY);
                        expectedImagePoints.put(2, nPoints, 1);
                        nPoints++;
                    }
                }
                Logger.trace("expectedImagePointsList = " + expectedImagePointsList);
                
                Mat expectedMachinePoints = Mat.zeros(3, actualPointsPerTestPattern, 
                        CvType.CV_64FC1);
                Core.gemm(transformImageToMachine, expectedImagePoints, 1.0, expectedImagePoints, 
                        0, expectedMachinePoints);
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
                Collections.shuffle(testPatternIndiciesList, new Random(calibrationHeightIndex+1));
                
                Logger.trace("testPattern = " + testPatternLocationsList);
                
                advance();
            }
            
        };
        
        swingWorker.execute();
        
        return true;
    }

    /**
     * Action to collect the calibration points for a test pattern (at a single Z height)
     * 
     * @return true if the action was successful and the state machine should move to the next step
     */
    private boolean step5() {
        List<double[]> testPattern3dPoints = new ArrayList<>();
        List<double[]> testPatternImagePoints = new ArrayList<>();
        
        swingWorker = new SwingWorker<Void, String>() { 
            @Override
            protected Void doInBackground() throws Exception  
            {
                org.opencv.core.Point observedPoint = null;
                
                for (int iPoint = 0; (iPoint < actualPointsPerTestPattern) && !isCancelled(); 
                        iPoint++) {
                    publish(String.format("Collecting calibration point %d of %d for calibration "
                            + "pattern %d of %d", iPoint+1, actualPointsPerTestPattern, 
                            calibrationHeightIndex+1, numberOfCalibrationHeights));

                    Location testLocation = testPatternLocationsList.
                            get(testPatternIndiciesList.get(iPoint));
                    
                    Point2D expectedPoint = expectedImagePointsList.
                            get(testPatternIndiciesList.get(iPoint));
                    
                    org.opencv.core.Point measuredPoint = findAveragedFiducialPoint(testLocation, 
                            expectedPoint);
                    
                    //Save the test pattern location and the corresponding image point
                    if (measuredPoint != null) {
                        testPattern3dPoints.add( 
                                new double[] {apparentMotionDirection*testLocation.getX(), 
                                              apparentMotionDirection*testLocation.getY(), 
                                              testPatternZ});
                        testPatternImagePoints.add(
                                new double[] {measuredPoint.x, measuredPoint.y});
                    }
                }
                Logger.trace("testPattern3dPoints = " + testPattern3dPoints);
                Logger.trace("testPatternImagePoints = " + testPatternImagePoints);
                testPattern3dPointsList.add(testPattern3dPoints);
                testPatternImagePointsList.add(testPatternImagePoints);
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
                if (this.isCancelled()) {
                    CalibrateCameraProcess.this.cancel();
                    return;
                }
                
                cameraView.setCameraViewFilter(null);
                
                calibrationHeightIndex++;
                if (calibrationHeightIndex == numberOfCalibrationHeights) {
                    double[][][] testPattern3dPointsArray = 
                            new double[testPattern3dPointsList.size()][][];
                    for (int tpIdx=0; tpIdx<testPattern3dPointsList.size(); tpIdx++) {
                        List<double[]> tp = testPattern3dPointsList.get(tpIdx);
                        testPattern3dPointsArray[tpIdx] = new double[tp.size()][];
                        for (int ptIdx=0; ptIdx<tp.size(); ptIdx++) {
                            double[] pt = tp.get(ptIdx);
                            testPattern3dPointsArray[tpIdx][ptIdx] = pt;
                        }
                    }
                        
                    double[][][] testPatternImagePointsArray = 
                            new double[testPatternImagePointsList.size()][][];
                    for (int tpIdx=0; tpIdx<testPatternImagePointsList.size(); tpIdx++) {
                        List<double[]> tp = testPatternImagePointsList.get(tpIdx);
                        testPatternImagePointsArray[tpIdx] = new double[tp.size()][];
                        for (int ptIdx=0; ptIdx<tp.size(); ptIdx++) {
                            double[] pt = tp.get(ptIdx);
                            testPatternImagePointsArray[tpIdx][ptIdx] = pt;
                        }
                    }
                    
                    processRawCalibrationData(testPattern3dPointsArray, testPatternImagePointsArray, 
                        new Size(pixelsX, pixelsY));
                    
                    CalibrateCameraProcess.this.cancel();
                }
                else {
                    advance();
                }
            }
            
        };
        
        swingWorker.execute();
        
        return true;
    }

    /**
     * Action to take when transitioning from step 5 to step 6
     * 
     * @return true if the action was successful and the state machine should move to the next step
     */
    private boolean step6() {
        if (isHeadMountedCamera) {
            return true;
        }
        else {
            //Set the next calibration height
            testPatternZ = calibrationHeights.get(calibrationHeightIndex).
                    convertToUnits(LengthUnit.Millimeters).getValue();
            movableZ = testPatternZ + 
                    calibrationRig.getHeight().convertToUnits(LengthUnit.Millimeters).getValue();
            
            showCircle(new org.opencv.core.Point(imageCenterPoint.getX(), imageCenterPoint.getY()), 
                    (int)(0.75*maskDiameter/2), Color.GREEN);
        
            Logger.trace("testPatternZ = " + testPatternZ);
            Logger.trace("movableZ = " + movableZ);
            
            //Move back to the center and change the calibration rig to the next calibration height
            UiUtils.submitUiMachineTask(() -> {
                nozzle.moveTo(centralLocation.convertToUnits(LengthUnit.Millimeters).
                        derive(null, null, movableZ, (double)angle));
            });
            
            //Go back to allow the operator to center the pattern in the camera's view
            step = 1;
            
            return true;
        }
    }
    
    /**
     * Action to take after the operator has manually changed the calibration rig's height
     * 
     * @return true if the action was successful and the state machine should move to the next step
     */
    private boolean step7() {
        if (isHeadMountedCamera && 
                Double.isFinite(calibrationHeights.get(calibrationHeightIndex).getValue())) {
            testPatternZ = calibrationHeights.get(calibrationHeightIndex).
                    convertToUnits(LengthUnit.Millimeters).getValue();
            //Go back to have the operator center the fiducial in the camera view
            step = 1;
        }
        else {
            //Go back to have the operator touch the nozzle tip onto the rig to measure its height
            step = 0;
        }
        return true;
    }
    
    /**
     * Gradually "walks" the movable in X and Y such that the calibration rig's fiducial appears at
     * the desired image location.  The steps taken are deliberately small so that errors due to 
     * camera distortion do not lead to erroneous machine motions. 
     * @param desiredCameraPoint - desired image location in pixels
     * @param xScaling - a rough signed estimate of the X axis units per pixel scaling
     * @param yScaling - a rough signed estimate of the Y axis units per pixel scaling
     * @return the machine location where the calibration rig's fiducial appears at the desired
     * image location
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private Location walkToPoint(Point2D desiredCameraPoint, double xScaling,
            double yScaling) throws InterruptedException, ExecutionException {
        //Move the machine to the starting location and wait for it to finish
        Future<?> future = UiUtils.submitUiMachineTask(() -> {
            movable.moveTo(centralLocation.derive(null, null, null, (double)angle));
        });
        future.get();

        Location oldLocation = null;
        Location newLocation = movable.getLocation().convertToUnits(LengthUnit.Millimeters);
        Point2D expectedPoint = (Point2D) imageCenterPoint.clone();
        Logger.trace("expectedPoint = " + expectedPoint);
        org.opencv.core.Point foundPoint = findAveragedFiducialPoint(newLocation, expectedPoint);
        Logger.trace("newPoint = " + foundPoint);
        if (foundPoint == null) {
            return null;
        }
        double errorX = foundPoint.x - desiredCameraPoint.getX();
        double errorY = foundPoint.y - desiredCameraPoint.getY();
        double oldError2 = Double.POSITIVE_INFINITY;
        double newError2 = errorX*errorX + errorY*errorY;
        
        //A fairly low loop gain needs to be used here due to lens and/or perspective distortions
        //that would make the assumption of a linear relation between the error and the correction 
        //invalid. Using a low loop gain ensures the the corrective steps are small which should
        //help keep the non-linearities small as well.
        final double loopGain = 0.25;
        
        while (newError2 < oldError2) {
            if (Thread.interrupted()) {
                return null;
            }
            oldError2 = newError2;
            oldLocation = newLocation;
            expectedPoint.setLocation(foundPoint.x - errorX * loopGain, 
                    foundPoint.y - errorY * loopGain);
            Location correction = new Location(LengthUnit.Millimeters, 
                    -errorX * xScaling * loopGain, 
                    -errorY * yScaling * loopGain, 0, 0);
            newLocation = oldLocation.add(correction).convertToUnits(LengthUnit.Millimeters).
                    derive(null, null, null, (double)angle);
            if (newLocation.getLinearDistanceTo(oldLocation) < 0.01) {
                //Don't bother moving if the move will be very tiny
                return oldLocation;
            }
            final Location moveLocation = newLocation;
            //Move the machine and wait for it to finish
            future = UiUtils.submitUiMachineTask(() -> {
                movable.moveTo(moveLocation);
            });
            future.get();
            
            foundPoint = findAveragedFiducialPoint(newLocation, expectedPoint);
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

    /**
     * Finds the fiducial 2D point averaged over all angles
     * @param testLocation - the machine location where the movable should be positioned to find 
     * the point
     * @param expectedPoint - the 2D point, in pixels, where the fiducial is expected to be found
     * @return the averaged 2D point, in pixels, if the fiducial was found at all angles, otherwise
     * null
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private org.opencv.core.Point findAveragedFiducialPoint(Location testLocation, 
            Point2D expectedPoint) throws InterruptedException, ExecutionException {
        int count = 0;
        org.opencv.core.Point observedPoint = null;
        org.opencv.core.Point measuredPoint = new org.opencv.core.Point(0, 0);
        
        do {
            final Location moveLocation = testLocation.convertToUnits(LengthUnit.Millimeters).
                    derive(null, null, null, (double) angle);

            //Move the machine and capture the fiducial location
            Future<Void> future = UiUtils.submitUiMachineTask(() -> {
                movable.moveTo(moveLocation);
            });

            //Wait for the move to complete
            future.get();

            //Find the calibration rig's fiducial
            observedPoint = findCalibrationRigFiducialPoint(expectedPoint);
            
            if (observedPoint == null) {
                break;
            }
            
            //Average the point
            measuredPoint.x += observationWeight * observedPoint.x;
            measuredPoint.y += observationWeight * observedPoint.y;
            
            //Change the angle after each measurement except the last so that the next time we'll 
            //start with the last angle
            count++;
            if (count < numberOfAngles) {
                angle += angleIncrement;
                if (angle >= 360) {
                    angle = 0;
                }
            }
        } while (count < numberOfAngles);
        
        if (observedPoint == null) {
            return null;
        }
        return measuredPoint;
    }
    
    /**
     * Processes the pipeline to locate the calibration rig's fiducial center in the image
     * @param expectedPoint - the expected location of the fiducial center
     * @return the location in pixels of the fiducial center
     */
    private org.opencv.core.Point findCalibrationRigFiducialPoint(Point2D expectedPoint) {
        //Mask off everything that is not near the expected location
        pipeline.setProperty("MaskCircle.center", new org.opencv.core.Point(expectedPoint.getX(), 
                expectedPoint.getY()));
        pipeline.setProperty("MaskCircle.diameter", maskDiameter);
        
        List<KeyPoint> keypoints = null;
        int attempts = 0;
        
        //Show a circle centered on where the fiducial is expected to be found
        Color circleColor = Color.GREEN;
        showCircle(new org.opencv.core.Point(expectedPoint.getX(), expectedPoint.getY()), 
                maskDiameter/2, circleColor);
        
        //Keep trying to locate the point until it is found or the maximum attempts have been made
        while ((keypoints == null) && (attempts < 3)) {
            if (Thread.interrupted()) {
                return null;
            }
            // Run the pipeline and get the results
            try {
                pipeline.process();
                keypoints = pipeline.getExpectedResult(VisionUtils.PIPELINE_RESULTS_NAME)
                        .getExpectedListModel(KeyPoint.class, 
                                new Exception("Calibration rig fiducial not found."));
            }
            catch (Exception e) {
                keypoints = null;
            }
            
            if (keypoints != null) {
                Logger.trace("keypoints = " + keypoints);
                
                //Of all the points found, keep the one closest to the expected location
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
                double enclosingDiameter = 2*minDistance + bestKeyPoint.size;
                if (enclosingDiameter > 0.80*maskDiameter) {
                    //Turn the point yellow as it is getting too close to the edge of the masked
                    //circle - may want to consider increasing the maskDiameter if this continues
                    //to occur
                    pointColor = Color.YELLOW;
                    changeMaskSize++;
                }
                else if (enclosingDiameter < 0.50*maskDiameter) {
                    //Turn the point blue as it is near the center of the masked circle - may want 
                    //to consider decreasing the maskDiameter if this continues to occur
                    pointColor = Color.BLUE;
                    changeMaskSize--;
                }
                else {
                    changeMaskSize -= (int)Math.signum(changeMaskSize);
                }
                showPointAndCircle(bestKeyPoint.pt, new org.opencv.core.Point(expectedPoint.getX(),
                        expectedPoint.getY()), maskDiameter/2, pointColor, circleColor);
                if (changeMaskSize > changeMaskThreshold) {
                    maskDiameter *= 1.10;
                    pipeline.setProperty("MaskCircle.diameter", maskDiameter);
                    Logger.trace("Increasing mask diameter to " + maskDiameter);
                    changeMaskSize--;
                }
                else if (changeMaskSize < -changeMaskThreshold) {
                    maskDiameter /= 1.10;
                    pipeline.setProperty("MaskCircle.diameter", maskDiameter);
                    Logger.trace("Decreasing mask diameter to " + maskDiameter);
                    changeMaskSize++;
                }
                return bestKeyPoint.pt;
            }
            attempts++;
        }
        return null;
    }

    /**
     * Clean-up when the process is done/cancelled
     */
    protected void cleanup() {
        cameraView.setCameraViewFilter(null);
        
        ((ReferenceMachine) Configuration.get().getMachine()).
            setAutoToolSelect(savedAutoToolSelect);

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
    protected void cancel() {
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

    /**
     * Overlays a point (displayed as a +) on the camera's view
     * @param point - the location, in pixels, of the +
     * @param color - the color of the +
     */
    protected void showPoint(org.opencv.core.Point point, Color color) {
        showPointAndCircle(point, null, 0, color);
    }
    
    /**
     * Overlays a circle on the camera's view
     * @param center - the center, in pixels, of the circle
     * @param radius - the radius, in pixels, of the circle
     * @param color - the color of the circle
     */
     protected void showCircle(org.opencv.core.Point center, int radius, Color color) {
        showPointAndCircle(null, center, radius, color);
    }
    
    /**
     * Overlays a point (displayed as a +) and a circle on the camera's view
     * @param point - the center, in pixels, of the +
     * @param center - the center, in pixels, of the circle
     * @param radius - the radius, in pixels, of the circle
     * @param color - the color of the + and the circle
     */
    protected void showPointAndCircle(org.opencv.core.Point point, org.opencv.core.Point center, 
            int radius, Color color) {
        showPointAndCircle(point, center, radius, color, color);
    }
    
    /**
     * Overlays a point (displayed as a +) and a circle on the camera's view
     * @param point - the center, in pixels, of the +
     * @param center - the center, in pixels, of the circle
     * @param radius - the radius, in pixels, of the circle
     * @param pointColor - the color of the +
     * @param circleColor - the color of the circle
     */
    protected void showPointAndCircle(org.opencv.core.Point point, org.opencv.core.Point center, 
            int radius, Color pointColor, Color circleColor) {
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
}
