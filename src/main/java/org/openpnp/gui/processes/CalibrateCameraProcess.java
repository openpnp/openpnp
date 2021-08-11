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
import java.util.function.Function;
import java.util.function.Supplier;

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
import org.openpnp.util.UiUtils.Thrunnable;
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

    private static final double desiredNumberOfRadials = 32;
    
    private final int numberOfCalibrationHeights;
    private final MainFrame mainFrame;
    private final CameraView cameraView;
    private final Camera camera;
    private final boolean isHeadMountedCamera;
    private Nozzle nozzle;
    private double loopGain;
    

    private int step = -1;
    
    private String instructionsTitleStr = "Camera Calibration Instructions/Status";
    
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

    private int initialMaskDiameter;

    private Thrunnable proceedAction;

    protected Boolean previousProceedActionResult;

    private boolean heightIsDefined;

    private Location initialRigLocation;
    
    protected boolean rigIsOnNozzle = false;


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
        loopGain = advCal.getWalkingLoopGain();
        
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
        
        initialMaskDiameter = Math.min(pixelsX, pixelsY) / 4;
        maskDiameter = initialMaskDiameter;
        
        savedAutoToolSelect = Configuration.get().getMachine().isAutoToolSelect();
        ((ReferenceMachine) Configuration.get().getMachine()).setAutoToolSelect(false);
        
        SwingUtilities.invokeLater(() -> {
            MainFrame.get().getCameraViews().ensureCameraVisible(camera);
        });
        
//        advance();
        initialAction();
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
     * @throws Exception 
     */
    protected abstract void processRawCalibrationData(double[][][] testPattern3dPoints, 
            double[][][] testPatternImagePoints, Size size) throws Exception;
    
    /**
     * This method is called when the raw calibration data collection has been canceled and must 
     * be overridden to perform any necessary clean-up
     */
    protected abstract void processCanceled();
    
//    /**
//     * Advances through the steps and displays the appropriate instructions for each step
//     */
//    private void advance() {
//        boolean stepResult = true;
//        switch (step) {
//            case 0:
//                stepResult = step1();
//                break;
//            case 1:
//                stepResult = step2();
//                break;
//            case 2:
//                stepResult = step3();
//                break;
//            case 3:
//                stepResult = step4();
//                break;
//            case 4:
//                stepResult = step5();
//                break;
//            case 5:
//                stepResult = step6();
//                break;
//            case 6:
//                stepResult = step7();
//                break;
//            default :
//                break;
//        }
//
//        if (!stepResult) {
//            return;
//        }
//        step++;
//        if (step > 6) {
//            cleanUpWhenDone();
//        }
//        else {
//            mainFrame.showInstructions("Camera Calibration Instructions/Status",
//                    isHeadMountedCamera ? moveableCameraInstructions[step] : 
//                        fixedCameraInstructions[step],
//                    true, true, "Next", cancelActionListener, proceedActionListener);
//        }
//    }

    /**
     * Initial action to start the calibration process
     * 
     * @return true when completed
     */
    private boolean initialAction() {
        heightIsDefined = Double.isFinite(
                calibrationHeights.get(calibrationHeightIndex).getValue());
        if (isHeadMountedCamera) {
            movable = camera;
            movableZ = 0;
            
            if (advCal.isCalibrationFiducialsFixed()) {
                if (heightIsDefined) {
                    testPatternZ = calibrationHeights.get(calibrationHeightIndex).
                            convertToUnits(LengthUnit.Millimeters).getValue();

                    maskDiameter = initialMaskDiameter;
                    showCircle(new org.opencv.core.Point(imageCenterPoint.getX(), imageCenterPoint.getY()), 
                            (int)(0.75*maskDiameter/2), Color.GREEN);
                    
                    setInstructionsAndProceedAction("Jog the camera so that the calibration fiducial at Z = %s is approximately centered in the green circle.  Click Next to begin the automated calibration sequence.", 
                            ()->estimateUnitsPerPixelAction(),
                            calibrationHeights.get(calibrationHeightIndex).toString());
                }
                else { //need to measure the height
                    setInstructionsAndProceedAction("Select a nozzle tip, jog it over the first calibration fiducial, and carefully lower it until it is just touching.  Click Next to capture the Z coordinate and retract the nozzle tip to safe Z.", 
                            ()->captureZCoordinateAction());
               }
            }
            else {
                if (heightIsDefined) {
                    testPatternZ = calibrationHeights.get(calibrationHeightIndex).
                            convertToUnits(LengthUnit.Millimeters).getValue();

                    setInstructionsAndProceedAction("Place the calibration rig face up on the machine so that the fiducial is at Z = %s. It should be positioned so that the image of the calibration fiducial is observable in all four corners of the camera view by only jogging the camera in X/Y. It is important that the calibration rig does not move due to machine vibrations etc. once the calibration sequence has begun.  It is advisable to secure it to the table with clamps, adhesive tape, or some other means. Click Next when ready to proceed.", 
                            ()->requestOperatorToCenterCameraAction(),
                            calibrationHeights.get(calibrationHeightIndex).toString()); 
                }
                else { //need to measure the height
                    setInstructionsAndProceedAction("Place the calibration rig face up on the machine so that the fiducial is at or below the Default Z Plane for this camera. It should be positioned so that the image of the calibration fiducial is observable in all four corners of the camera view by only jogging the camera in X/Y. It is important that the calibration rig does not move due to machine vibrations etc. once the calibration sequence has begun.  It is advisable to secure it to the table with clamps, adhesive tape, or some other means. Click Next when ready to proceed.",
                            ()->requestOperatorToMeasureZAction());
                }
            }
        }
        else { //bottom camera
            for (int i=0; i<numberOfCalibrationHeights; i++) {
                if (!Double.isFinite(calibrationHeights.get(calibrationHeightIndex).getValue())) {
                    cleanUpWhenCancelled();
                    MessageBoxes.errorBox(MainFrame.get(), "Error", "Need to define finite calibration Z coordinates before starting calibration.");
                }
            }
            
            testPatternZ = calibrationHeights.get(calibrationHeightIndex).
                    convertToUnits(LengthUnit.Millimeters).getValue();
            
            if (advCal.isUsingCalibrationRig()) {
                movableZ = testPatternZ + calibrationRig.getHeight().
                        convertToUnits(LengthUnit.Millimeters).getValue();
                
                setInstructionsAndProceedAction("Select a nozzle and load it with the largest available nozzle tip that will fit the calibration rig. This tip will be used to pick-up the calibration rig and move it into the camera's field-of-view.  Click Next when ready to proceed.",
                        ()->requestOperatorToPickUpRigAction());
            }
            else { //using the nozzle tip
                movableZ = testPatternZ;
                
                setInstructionsAndProceedAction("Select a nozzle and load it with the smallest available nozzle tip. Click Next when ready to proceed.",
                        ()->requestOperatorToCenterNozzleTipAction());
            }
        }
            
        return true;
    }
    
    /**
     * Starts a background thread that makes a rough measurement of the camera's units per pixel at 
     * the current calibration height
     * 
     * @return true when complete
     */
    private boolean estimateUnitsPerPixelAction() {
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
            protected Void doInBackground() throws Exception {
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
                
                publish("Finding image center.");
                Logger.trace("Finding image center.");
                centralLocation = walkToPoint(imageCenterPoint, xScaling, yScaling);

                return null;
            } 

            @Override
            protected void process(List<String> chunksOfStatus) {
                for (String status : chunksOfStatus) {
                    if (!this.isCancelled()) {
                        displayStatus(status);
                    }
                }
            }
            
            @Override
            protected void done()  
            {
                if ((trialPoint1 == null) || (trialPoint2 == null)) {
                    if (!isCancelled()) {
                        MessageBoxes.errorBox(MainFrame.get(), "Error", "Could not estimate units per pixel - calibration aborting.");
                    }
                    cleanUpWhenCancelled();
                    return;
                }
                
//                findImageCornersAction();
                collectCalibrationPointsAlongRadialLinesAction();
            }
        };
        
        swingWorker.execute();
        
        return true;
    }

    /**
     * Starts a background thread that finds the four corners of the image
     * 
     * @return true when complete
     */
    private boolean findImageCornersAction() {
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

        swingWorker = new SwingWorker<Void, String>() {

            @Override
            protected Void doInBackground() throws Exception {
                String str = "";
                int cornerIdx = 0;
                while ((cornerIdx < 4) && !isCancelled()) {
                    switch (cornerIdx) {
                        case 0:
                            str = "Finding Upper Left Image Corner";
                            break;
                        case 1:
                            str = "Finding Lower Left Image Corner";
                            break;
                        case 2:
                            str = "Finding Lower Right Image Corner";
                            break;
                        case 3:
                            str = "Finding Upper Right Image Corner";
                            break;
                    }

                    publish(str);
                    
                    Logger.trace("Walking from " + centralLocation + " to " + 
                            desiredCameraCorners.get(cornerIdx) + " " + str);
                    
                    maskDiameter = initialMaskDiameter;
                    
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
            protected void process(List<String> chunksOfStatus) {
                for (String status : chunksOfStatus) {
                    if (!this.isCancelled()) {
                        displayStatus(status);
                    }
                }
            }
            
            @Override
            protected void done()  
            {
                if (isCancelled()) {
                    cleanUpWhenCancelled();
                    return;
                }
                
                Logger.trace("cameraCornerLocations = " + cameraCornerLocations);
                
                if (cameraCornerLocations.size() < 4) {
                    MessageBoxes.errorBox(MainFrame.get(), "Error", "Unable to locate all four corners of the image - calibration aborting.");
                    cleanUpWhenCancelled();
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
                
                collectCalibrationPointsAtOneHeightAction();
            }
        };
        
        swingWorker.execute();
        
        return true;
    }

    /**
     * Starts a background task to collect calibration points along radial lines.
     * 
     * @return true when complete
     */
    private boolean collectCalibrationPointsAlongRadialLinesAction() {
        List<double[]> testPattern3dPoints = new ArrayList<>();
        List<double[]> testPatternImagePoints = new ArrayList<>();
        
        List<Point2D> desiredEndPoints = new ArrayList<>();
        
        int numberOfLines = 4 * (int)Math.ceil(desiredNumberOfRadials/4.0);

        for (int i=0; i<numberOfLines; i++) {
            double lineAngle = i * 2 * Math.PI / numberOfLines;
            double[] unitVector = new double[] {Math.cos(lineAngle), Math.sin(lineAngle)};
            double scaling = advCal.getTestPatternFillFraction()*
                    Math.min(pixelsX / (2*Math.abs(unitVector[0])),
                    pixelsY / (2*Math.abs(unitVector[1])));
            desiredEndPoints.add(new Point2D.Double(scaling*unitVector[0] + pixelsX/2, 
                    scaling*unitVector[1] + pixelsY/2));
        }
        Collections.shuffle(desiredEndPoints, new Random(calibrationHeightIndex+1));
        
        swingWorker = new SwingWorker<Void, String>() {

            @Override
            protected Void doInBackground() throws Exception {
                for (int iLine=0; iLine<numberOfLines; iLine++) {
                    maskDiameter = initialMaskDiameter;
                    publish(String.format("Collecting calibration points along radial line %d of %d at calibration Z coordinate %d of %d", 
                            iLine+1, numberOfLines, 
                            calibrationHeightIndex+1, numberOfCalibrationHeights));
                    walkToPoint(desiredEndPoints.get(iLine), xScaling, yScaling, 
                            testPattern3dPoints, testPatternImagePoints);
                    if (isCancelled()) {
                        return null;
                    }
                }
                
                testPattern3dPointsList.add(testPattern3dPoints);
                testPatternImagePointsList.add(testPatternImagePoints);
                return null;
            } 

            @Override
            protected void process(List<String> chunksOfStatus) {
                for (String status : chunksOfStatus) {
                    if (!isCancelled()) {
                        displayStatus(status);
                    }
                }
            }
            
            @Override
            protected void done()  
            {
                if (isCancelled()) {
                    cleanUpWhenCancelled();
                    return;
                }
                
                cameraView.setCameraViewFilter(null);
                
                calibrationHeightIndex++;
                if (calibrationHeightIndex >= numberOfCalibrationHeights) {
                    double[][][] testPattern3dPointsArray = 
                            new double[testPattern3dPointsList.size()][][];
                    for (int tpIdx=0; tpIdx<testPattern3dPointsList.size(); tpIdx++) {
                        List<double[]> tp = testPattern3dPointsList.get(tpIdx);
                        testPattern3dPointsArray[tpIdx] = new double[tp.size()][];
                        for (int ptIdx=0; ptIdx<tp.size(); ptIdx++) {
                            double[] pt = tp.get(ptIdx);
                            testPattern3dPointsArray[tpIdx][ptIdx] = new double[] {
                                    apparentMotionDirection*pt[0], apparentMotionDirection*pt[1], 
                                    calibrationHeights.get(tpIdx).
                                    convertToUnits(LengthUnit.Millimeters).getValue()};
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
                    
                    try {
                        processRawCalibrationData(testPattern3dPointsArray, testPatternImagePointsArray, 
                            new Size(pixelsX, pixelsY));
                    }
                    catch (Exception ex) {
                        MessageBoxes.errorBox(MainFrame.get(), "Error", ex);
                    }
                    finally {
                        cleanUpWhenDone();
                        
                        if (!isHeadMountedCamera && advCal.isUsingCalibrationRig()) {
                            if(MessageBoxes.errorBoxWithRetry(MainFrame.get(), "Calibration Complete", "Return calibration rig to original pick location? (otherwise it will remain on the nozzle)")) {
                                returnRigToInitialLocationAction();
                            }
                        }
                    }
                }
                else {
                    repeatAction();
                }
                
            }
        };
        
        swingWorker.execute();
        
        return true;
    }
    
    /**
     * Starts a background thread that collects all the calibration points at the current 
     * calibration height
     * 
     * @return true when completed
     */
    private boolean collectCalibrationPointsAtOneHeightAction() {
        List<double[]> testPattern3dPoints = new ArrayList<>();
        List<double[]> testPatternImagePoints = new ArrayList<>();
        
        swingWorker = new SwingWorker<Void, String>() {

            @Override
            protected Void doInBackground() throws Exception {
                maskDiameter = initialMaskDiameter;
                
                for (int iPoint = 0; (iPoint < actualPointsPerTestPattern) && !isCancelled(); 
                        iPoint++) {
                    publish(String.format("Collecting calibration point %d of %d at calibration Z coordinate %d of %d", 
                            iPoint+1, actualPointsPerTestPattern, 
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
                testPattern3dPointsList.add(testPattern3dPoints);
                testPatternImagePointsList.add(testPatternImagePoints);
                return null;
            } 

            @Override
            protected void process(List<String> chunksOfStatus) {
                for (String status : chunksOfStatus) {
                    if (!isCancelled()) {
                        displayStatus(status);
                    }
                }
            }
            
            @Override
            protected void done()  
            {
                if (isCancelled()) {
                    cleanUpWhenCancelled();
                    return;
                }
                
                cameraView.setCameraViewFilter(null);
                
                calibrationHeightIndex++;
                if (calibrationHeightIndex >= numberOfCalibrationHeights) {
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
                    
                    try {
                        processRawCalibrationData(testPattern3dPointsArray, testPatternImagePointsArray, 
                            new Size(pixelsX, pixelsY));
                    }
                    catch (Exception ex) {
                        MessageBoxes.errorBox(MainFrame.get(), "Error", ex);
                    }
                    finally {
                        cleanUpWhenDone();
                        
                        if (!isHeadMountedCamera && advCal.isUsingCalibrationRig()) {
                            if(MessageBoxes.errorBoxWithRetry(MainFrame.get(), "Calibration Complete", "Return calibration rig to original pick location? (otherwise it will remain on the nozzle)")) {
                                returnRigToInitialLocationAction();
                            }
                        }
                    }
                }
                else {
                    repeatAction();
                }
                
            }
        };
        
        swingWorker.execute();
        
        return true;
    }

    /**
     * Action to repeat the calibration collection at another height
     * 
     * @return true when complete
     */
    protected boolean repeatAction() {
        heightIsDefined = Double.isFinite(
                calibrationHeights.get(calibrationHeightIndex).getValue());
        if (isHeadMountedCamera) {
            movableZ = 0;
            
            if (advCal.isCalibrationFiducialsFixed()) {
                if (heightIsDefined) {
                    testPatternZ = calibrationHeights.get(calibrationHeightIndex).
                            convertToUnits(LengthUnit.Millimeters).getValue();
                    
                    maskDiameter = initialMaskDiameter;
                    showCircle(new org.opencv.core.Point(imageCenterPoint.getX(), imageCenterPoint.getY()), 
                            (int)(0.75*maskDiameter/2), Color.GREEN);

                    setInstructionsAndProceedAction("Jog the camera so that the calibration fiducial at Z = %s is approximately centered in the green circle.  Click Next to begin the automated calibration sequence.", 
                            ()->estimateUnitsPerPixelAction(),
                            calibrationHeights.get(calibrationHeightIndex).toString()); 
                }
                else {
                    //need to measure the height
                    setInstructionsAndProceedAction("Jog the nozzle tip over the next calibration fiducial, and carefully lower it until it is just touching.  Click Next to capture the Z coordinate and retract the nozzle tip to safe Z.", 
                            ()->captureZCoordinateAction());
               }
            }
            else {
                if (heightIsDefined) {
                    testPatternZ = calibrationHeights.get(calibrationHeightIndex).
                            convertToUnits(LengthUnit.Millimeters).getValue();
                    
                    setInstructionsAndProceedAction("Add or remove spacers beneath the calibration rig so that the fiducial is at Z = %s. It should be positioned so that the image of the calibration fiducial is observable in all four corners of the camera view by only jogging the camera in X/Y. It is important that the calibration rig does not move due to machine vibrations etc. once the calibration sequence has begun.  It is advisable to secure it to the table with clamps, adhesive tape, or some other means. Click Next when ready to proceed.", 
                            ()->requestOperatorToCenterCameraAction(),
                            calibrationHeights.get(calibrationHeightIndex).toString()); 
                }
                else {
                    //need to measure the height
                    setInstructionsAndProceedAction("Add spacers beneath calibration rig so that the fiducial is about at the height of the highest item that may ever be imaged by this camera. It should be positioned so that the image of the calibration fiducial is observable in all four corners of the camera view by only jogging the camera in X/Y. It is important that the calibration rig does not move due to machine vibrations etc. once the calibration sequence has begun.  It is advisable to secure it to the table with clamps, adhesive tape, or some other means. Click Next when ready to proceed.", 
                            ()->requestOperatorToMeasureZAction());
                }
            }
        }
        else { //bottom camera
            testPatternZ = calibrationHeights.get(calibrationHeightIndex).
                    convertToUnits(LengthUnit.Millimeters).getValue();

            if (advCal.isUsingCalibrationRig()) {
                movableZ = testPatternZ + calibrationRig.getHeight().
                        convertToUnits(LengthUnit.Millimeters).getValue();
                
                //Move back to the center and change the calibration rig to the next calibration height
                Future<Void> future = UiUtils.submitUiMachineTask(() -> {
                    nozzle.moveTo(centralLocation.convertToUnits(LengthUnit.Millimeters).
                            derive(null, null, movableZ, (double)angle));
                });
                try {
                    future.get();
                }
                catch (Exception ex) {
                    MessageBoxes.errorBox(MainFrame.get(), "Error", ex);
                    cleanUpWhenCancelled();
                }
                
                requestOperatorToVerifyRigIsCenteredAction();
            }
            else { //using nozzle tip
                movableZ = testPatternZ;
                
                //Move back to the center and change the nozzle tip to the next calibration height
                Future<Void> future = UiUtils.submitUiMachineTask(() -> {
                    nozzle.moveTo(centralLocation.convertToUnits(LengthUnit.Millimeters).
                            derive(null, null, movableZ, (double)angle));
                });
                try {
                    future.get();
                }
                catch (Exception ex) {
                    MessageBoxes.errorBox(MainFrame.get(), "Error", ex);
                    cleanUpWhenCancelled();
                }
                
                requestOperatorToVerifyNozzleTipIsCenteredAction();
            }
        }
            
        return true;
    }

    /**
     * Action to capture the Z coordinate of the nozzle tip and return it to safe Z
     * 
     * @return true when completed
     */
    private boolean captureZCoordinateAction() {
        nozzle = (Nozzle) MainFrame.get().getMachineControls().getSelectedTool();
        
        testPatternZ = nozzle.getLocation().convertToUnits(LengthUnit.Millimeters).getZ();
        
        //Return the nozzle to safe Z
        Future<Void> future = UiUtils.submitUiMachineTask(() -> {
            nozzle.moveToSafeZ();
        });
        try {
            future.get();
        }
        catch (Exception ex) {
            MessageBoxes.errorBox(MainFrame.get(), "Error", ex);
            cleanUpWhenCancelled();
        }
        
        requestOperatorToCenterCameraAction();
        
        return true;
    }

    /**
     * Displays a request to the operator to move the camera so that it is centered over the 
     * fiducial
     * 
     * @return true when completed
     */
    private boolean requestOperatorToCenterCameraAction() {
        maskDiameter = initialMaskDiameter;
        showCircle(new org.opencv.core.Point(imageCenterPoint.getX(), imageCenterPoint.getY()), 
                (int)(0.50*maskDiameter/2), Color.GREEN);
        
//        setInstructionsAndProceedAction("Jog the camera so that the calibration fiducial is approximately centered in the green circle. When ready, click Next to begin the calibration collection sequence.", 
//                ()->estimateUnitsPerPixelAction());
        setInstructionsAndProceedAction("Jog the camera so that the calibration fiducial is approximately centered in the green circle. When ready, click Next to begin the calibration collection sequence.", 
                ()->requestOperatorToAdjustDiameterAction());
        return true;
    }

    /**
     * Displays a request to the operator to use the nozzle tip to measure the Z coordinate of the 
     * calibration fiducial
     * 
     * @return true when completed
     */
    private boolean requestOperatorToMeasureZAction() {
        setInstructionsAndProceedAction("Select a nozzle tip, jog it over the calibration fiducial, and carefully lower it until it is just touching.  When ready, click Next to capture the Z coordinate and retract the nozzle tip to safe Z.", 
                ()->captureZCoordinateAction());
        return true;
    }

    /**
     * Displays a request to the operator to position the nozzle tip on the calibration rig in 
     * preparation to pick-up the rig
     * 
     * @return true when completed
     */
    private boolean requestOperatorToPickUpRigAction() {
        setInstructionsAndProceedAction("Place the calibration rig face down on the machine table. Jog the nozzle tip over the center of the rig and carefully lower it until it is just touching.  Click Next to pick-up the rig and retract it to safe Z.", 
                ()->pickUpRigAction());
        return true;
    }

    /**
     * Displays a request to the operator to position the nozzle tip over the center of the camera's 
     * field-of-view
     * 
     * @return true when complete
     */
    private boolean requestOperatorToCenterNozzleTipAction() {
        maskDiameter = initialMaskDiameter;
        showCircle(new org.opencv.core.Point(imageCenterPoint.getX(), imageCenterPoint.getY()), 
                (int)(0.50*maskDiameter/2), Color.GREEN);
        
        setInstructionsAndProceedAction("Jog the nozzle tip so that it is approximately in the center of the green circle. When ready, click Next to lower/raise the nozzle tip to the calibration height.", 
                ()->changeNozzleTipHeightAction());
        return true;
    }

    /**
     * Moves the nozzle tip to the center of the camera's field-of-view and lowers/raises it to the 
     * calibration height
     * 
     * @return
     */
    private boolean changeNozzleTipHeightAction() {
        nozzle = (Nozzle) MainFrame.get().getMachineControls().getSelectedTool();
        
        //Move back to the center and change the nozzle tip to the calibration height
        Future<Void> future = UiUtils.submitUiMachineTask(() -> {
            nozzle.moveTo(centralLocation.convertToUnits(LengthUnit.Millimeters).
                    derive(null, null, movableZ, (double)angle));
        });
        try {
            future.get();
        }
        catch (Exception ex) {
            MessageBoxes.errorBox(MainFrame.get(), "Error", ex);
            cleanUpWhenCancelled();
        }
        
        requestOperatorToVerifyNozzleTipIsCenteredAction();
        
        return true;
    }

    /**
     * Displays a request to the operator to verify that the nozzle tips is centered
     * 
     * @return true when complete
     */
    private boolean requestOperatorToVerifyNozzleTipIsCenteredAction() {
        setInstructionsAndProceedAction("Rotate the nozzle tip through 360 degrees and verify it stays within the green circle. If necessary, jog it in X and/or Y so that it remains within the circle when it is rotated. When ready, click Next to begin the automated calibration collection sequence.", 
                ()->estimateUnitsPerPixelAction());
        return true;
    }

    /**
     * Picks-up the calibration rig and moves it to safe Z
     * 
     * @return true when complete
     */
    private boolean pickUpRigAction() {
        nozzle = (Nozzle) MainFrame.get().getMachineControls().getSelectedTool();
        movable = nozzle;
        
        initialRigLocation = nozzle.getLocation();
        
        Future<Void> future = UiUtils.submitUiMachineTask(() -> {
                nozzle.pick(calibrationRig);
                nozzle.moveToSafeZ();
            });
        try {
            future.get();
        }
        catch (Exception ex) {
            MessageBoxes.errorBox(MainFrame.get(), "Error", ex);
            cleanUpWhenCancelled();
        }
        rigIsOnNozzle = true;

        requestOperatorToCenterRigAction();
        
        return true;
    }

    /**
     * Displays a request to the operator to center the calibration rig over the camera
     * 
     * @return true when complete
     */
    private boolean requestOperatorToCenterRigAction() {
        maskDiameter = initialMaskDiameter;
        showCircle(new org.opencv.core.Point(imageCenterPoint.getX(), imageCenterPoint.getY()), 
                (int)(0.50*maskDiameter/2), Color.GREEN);
        
        setInstructionsAndProceedAction("Jog the nozzle so that the calibration rig's fiducial is approximately in the center of the green circle. When ready, click Next to begin move the rig to the calibration height.", 
                ()->changeRigHeightAction());
        return true;
    }

    /**
     * Moves the calibration rig to the calibration height
     * 
     * @return true when complete
     */
    private boolean changeRigHeightAction() {
        centralLocation = nozzle.getLocation();
        
        //Move back to the center and change the calibration rig to the next calibration height
        Future<Void> future = UiUtils.submitUiMachineTask(() -> {
            nozzle.moveTo(centralLocation.convertToUnits(LengthUnit.Millimeters).
                    derive(null, null, movableZ, (double)angle));
        });
        try {
            future.get();
        }
        catch (Exception ex) {
            MessageBoxes.errorBox(MainFrame.get(), "Error", ex);
            cleanUpWhenCancelled();
        }
      
        requestOperatorToVerifyRigIsCenteredAction();
        
        return true;
    }

    /**
     * Displays a request to the operator to verify the calibration rig is centered in the camera's 
     * field-of-view
     * 
     * @return true when complete
     */
    private boolean requestOperatorToVerifyRigIsCenteredAction() {
        maskDiameter = initialMaskDiameter;
        showCircle(new org.opencv.core.Point(imageCenterPoint.getX(), imageCenterPoint.getY()), 
                (int)(0.50*maskDiameter/2), Color.GREEN);
        
        setInstructionsAndProceedAction("Rotate the nozzle through 360 degrees and verify the calibration rig's fiducial stays within the green circle. If necessary, jog it in X and/or Y and/or adjust the rig's position on the nozzle tip so that it remains within the circle when it is rotated. When ready, click Next to begin the automated calibration collection sequence.", 
                ()->estimateUnitsPerPixelAction());
        return true;
    }


    /**
     * Returns the calibration rig to the location it was originally picked and then returns the
     * nozzle to safe Z
     * 
     * @return true when complete
     */
    private boolean returnRigToInitialLocationAction() {
        Future<Void> future = UiUtils.submitUiMachineTask(() -> {
            nozzle.moveToSafeZ();
            Location safeRigLocation = initialRigLocation.
                    convertToUnits(nozzle.getLocation().getUnits()).
                    derive(null, null, nozzle.getLocation().getZ(), null);
            nozzle.moveTo(safeRigLocation);
            nozzle.moveTo(initialRigLocation);
            nozzle.place();
            nozzle.moveToSafeZ();
        });
        try {
            future.get();
        }
        catch (Exception ex) {
            MessageBoxes.errorBox(MainFrame.get(), "Error", ex);
            cleanUpWhenCancelled();
        }
        
        rigIsOnNozzle = false;

        return true;
    }
  
    private boolean requestOperatorToAdjustDiameterAction() {
        setInstructionsAndProceedAction("Adjust the Fiducial Diameter slider until the orange circle snaps to the fiducial. Click Next when ready", 
                ()->fiducialDiameterIsSetAction());
        
        Point2D expectedPoint = new Point2D.Double((pixelsX-1)/2.0, (pixelsY-1)/2.0);
        pipeline.setProperty("MaskCircle.center", new org.opencv.core.Point(expectedPoint.getX(), 
                expectedPoint.getY()));
        pipeline.setProperty("DetectCircularSymmetry.center", new org.openpnp.model.Point(
                expectedPoint.getX(), expectedPoint.getY()));
        
        maskDiameter = 2*(int)Math.min(expectedPoint.getX(), 
                Math.min(pixelsX-expectedPoint.getX(),
                Math.min(expectedPoint.getY(),
                Math.min(pixelsY-expectedPoint.getY(), maskDiameter/2))));
        
        pipeline.setProperty("MaskCircle.diameter", maskDiameter);
        pipeline.setProperty("DetectCircularSymmetry.maxDistance", maskDiameter/2.0);
        
        swingWorker = new SwingWorker<Void, String>() {

            @Override
            protected Void doInBackground() throws Exception {
                while (!isCancelled()) {
                    pipeline.setProperty("DetectCircularSymmetry.diameter", 1.0*advCal.getFiducialDiameter());
                    List<KeyPoint> keyPoints = null;

                    try {
                        pipeline.process();
                        keyPoints = pipeline.getExpectedResult(VisionUtils.PIPELINE_RESULTS_NAME)
                                .getExpectedListModel(KeyPoint.class, 
                                        new Exception("Calibration rig fiducial not found."));
                    }
                    catch (Exception e) {
                        keyPoints = null;
                    }
                    
                    if ((keyPoints != null) && !isCancelled()) {
                        //Of all the points found, keep the one closest to the expected location
                        double minDistance = Double.POSITIVE_INFINITY;
                        KeyPoint bestKeyPoint = null;
                        for (KeyPoint kpt : keyPoints) {
                            double dx = kpt.pt.x - expectedPoint.getX();
                            double dy = kpt.pt.y - expectedPoint.getY();
                            double distance = Math.sqrt(dx*dx + dy*dy);
                            if (distance < minDistance) {
                                bestKeyPoint = kpt;
                                minDistance = distance;
                            }
                        }
    
                        //Show a circle centered on where the fiducial was found
                        Color circleColor = Color.ORANGE;
                        showCircle(bestKeyPoint.pt, (int)(bestKeyPoint.size/2), circleColor);
                    }
                    else {
                        cameraView.setCameraViewFilter(null);
                    }
                }
                return null;
            } 

            @Override
            protected void done()  
            {
                cameraView.setCameraViewFilter(null);
                
            }
        };
        
        swingWorker.execute();
        
        return true;
    }
    
    private boolean fiducialDiameterIsSetAction() {
        swingWorker.cancel(true);
        while (!swingWorker.isDone()) {
            //spin
        }
        
        return true;
    }
  
  
  
//    /**
//    * Action to take after the operator has placed the calibration rig on the machine
//    * 
//    * @return true if the action was successful and the state machine should move to the next step
//    */
//    private boolean step1() {
//        //Get the default nozzle
//        try {
//            nozzle = Configuration.get().getMachine().getDefaultHead().getDefaultNozzle();
//        }
//        catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//            cleanUpWhenCancelled();
//        }
//        
//        MainFrame.get().getMachineControls().setSelectedTool(nozzle);
//        MovableUtils.fireTargetedUserAction(nozzle);
//        
//        if (isHeadMountedCamera && Double.isFinite(calibrationHeights.get(calibrationHeightIndex).
//                getValue())) {
//            testPatternZ = calibrationHeights.get(calibrationHeightIndex).
//                    convertToUnits(LengthUnit.Millimeters).getValue();
//            movableZ = 0;
//            movable = camera;
//            step++; //skip the step where the operator measures the height with the nozzle tip
//        }
//        return true;
//    }
//
//    /**
//     * Action to take after the operator has placed the nozzle tip on the calibration rig
//     * 
//     * @return true if the action was successful and the state machine should move to the next step
//     */
//    private boolean step2() {
//        nozzle = (Nozzle) MainFrame.get().getMachineControls().getSelectedTool();
//
//        if (isHeadMountedCamera) {
//            //Capture the nozzle's Z coordinate as the test pattern's Z coordinate
//            testPatternZ = nozzle.getLocation().convertToUnits(LengthUnit.Millimeters).getZ();
//            movableZ = 0;
//            movable = camera;
//        }
//        else {
//            testPatternZ = camera.getDefaultZ().convertToUnits(LengthUnit.Millimeters).getValue();
//            movableZ = testPatternZ + 
//                    calibrationRig.getHeight().convertToUnits(LengthUnit.Millimeters).getValue();
//            movable = nozzle;
//            
//            //Pick-up the calibration rig with the nozzle
//            try {
//                Future<Void> future = UiUtils.submitUiMachineTask(() -> {
//                    nozzle.pick(calibrationRig);
//                });
//                future.get();
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        
//        maskDiameter = initialMaskDiameter;
//        showCircle(new org.opencv.core.Point(imageCenterPoint.getX(), imageCenterPoint.getY()), 
//                (int)(0.75*maskDiameter/2), Color.GREEN);
//        
//        Logger.trace("testPatternZ = " + testPatternZ);
//        Logger.trace("movableZ = " + movableZ);
//        
//        //Return the nozzle to safe Z
//        Future future = UiUtils.submitUiMachineTask(() -> {
//            nozzle.moveToSafeZ();
//        });
//        try {
//            future.get();
//        }
//        catch (InterruptedException | ExecutionException e) {
//            e.printStackTrace();
//        }
//        
//        
//        return true;
//    }
//
//    /**
//     * Action to take after the operator has positioned the nozzle/camera so that the
//     * calibration rig's fiducial is centered in the camera's FOV
//     * 
//     * @return true if the action was successful and the state machine should move to the next step
//     */
//    private boolean step3() {
//        ((ReferenceCamera) camera).setAutoVisible(savedAutoVisible);
//        
//        //Capture the movable's coordinate as the approximate center of the camera's field-of-view
//        centralLocation = movable.getLocation().convertToUnits(LengthUnit.Millimeters).
//                derive(null, null, movableZ, 0.0);
//        
//        trialLocation1 = centralLocation.subtract(advCal.getTrialStep());
//        trialLocation2 = centralLocation.add(advCal.getTrialStep());
//        trialPoint1 = null;
//        trialPoint2 = null;
//        
//        swingWorker = new SwingWorker<Void, String>() { 
//            @Override
//            protected Void doInBackground() throws Exception  
//            {
//                publish("Checking first test point.");
//                Logger.trace("Checking first test point.");
//                
//                while ((trialPoint1 == null) && !isCancelled()) {
//                    trialPoint1 = findAveragedFiducialPoint(trialLocation1, 
//                            imageCenterPoint);
//
//                    if (trialPoint1 == null) {
//                        if (MessageBoxes.errorBoxWithRetry(mainFrame, 
//                                "ERROR - Failed to find calibration rig's fiducial.",
//                                "Edit the pipeline and try again?")) {
//                            CvPipelineEditor pipelineEditor = new CvPipelineEditor(pipeline);
//                            JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), 
//                                    "Camera Calibration Rig Fiducial Pipeline", pipelineEditor);
//                            dialog.setModalityType(ModalityType.APPLICATION_MODAL);
//                            
//                            //Because the dialog is modal, this thread of execution will hang
//                            //here until the dialog is closed, at which point it will resume
//                            dialog.setVisible(true);
//                        }
//                        else {
//                            return null;
//                        }
//                    }
//                }
//                
//                publish("Checking second test point.");
//                Logger.trace("Checking second test point.");
//                
//                while ((trialPoint2 == null) && !isCancelled()) {
//                    trialPoint2 = findAveragedFiducialPoint(trialLocation2, 
//                            imageCenterPoint);
//                    
//                    if (trialPoint2 == null) {
//                        if (MessageBoxes.errorBoxWithRetry(mainFrame, 
//                                "ERROR - Failed to find calibration rig's fiducial.",
//                                "Edit the pipeline and try again?")) {
//                            CvPipelineEditor pipelineEditor = new CvPipelineEditor(pipeline);
//                            JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), 
//                                    "Camera Calibration Rig Fiducial Pipeline", pipelineEditor);
//                            dialog.setModalityType(ModalityType.APPLICATION_MODAL);
//                            
//                            //Because the dialog is modal, this thread of execution will hang
//                            //here until the dialog is closed, at which point it will resume
//                            dialog.setVisible(true);
//                        }
//                        else {
//                            return null;
//                        }
//                    }
//                }
//                
//                //Compute a rough estimate of unitsPerPixel (these may be negative but that's ok)
//                Location trialLocationDifference = trialLocation2.
//                        subtract(trialLocation1);
//                xScaling = trialLocationDifference.getX() /
//                        (trialPoint2.x - trialPoint1.x);
//                yScaling = trialLocationDifference.getY() /
//                        (trialPoint2.y - trialPoint1.y);
//                Logger.trace("xScaling = " + xScaling);
//                Logger.trace("yScaling = " + yScaling);
//                
//                publish("Finding image center.");
//                Logger.trace("Finding image center.");
//                centralLocation = walkToPoint(imageCenterPoint, xScaling, yScaling);
//
//                return null;
//            }
//            
//            @Override
//            protected void process(List<String> chunks) {
//                for (String str : chunks) {
//                    if (!this.isCancelled()) {
//                        mainFrame.showInstructions("Camera Calibration Instructions", str,
//                            true, false, "Next", cancelActionListener, proceedActionListener);
//                    }
//                }
//            }
//            
//            @Override
//            protected void done()  
//            {
//                if ((trialPoint1 == null) || (trialPoint2 == null)) {
//                    cleanUpWhenCancelled();
//                    return;
//                }
//                
//                advance();
//            }
//        };
//        
//        swingWorker.execute();
//        
//        return true;
//    }
//
//    /**
//     * Action to locate the four corners of the camera's field-of-view
//     * 
//     * @return true if the action was successful and the state machine should move to the next step
//     */
//    private boolean step4() {
//        desiredCameraCorners = new ArrayList<Point2D>();
//        
//        double testPatternFillFraction = advCal.getTestPatternFillFraction();
//        
//        //In order: upper left, lower left, lower right, and upper right
//        desiredCameraCorners.add(new Point2D.Double((1-testPatternFillFraction)*pixelsX/2,
//                (1-testPatternFillFraction)*pixelsY/2));
//        desiredCameraCorners.add(new Point2D.Double((1-testPatternFillFraction)*pixelsX/2,
//                (1+testPatternFillFraction)*pixelsY/2));
//        desiredCameraCorners.add(new Point2D.Double((1+testPatternFillFraction)*pixelsX/2,
//                (1+testPatternFillFraction)*pixelsY/2));
//        desiredCameraCorners.add(new Point2D.Double((1+testPatternFillFraction)*pixelsX/2,
//                (1-testPatternFillFraction)*pixelsY/2));
//        Logger.trace("desiredCameraCorners = " + desiredCameraCorners);
//
//        cameraCornerLocations = new ArrayList<Location>();
//
//        //Find the four corners and then setup for the test pattern
//        swingWorker = new SwingWorker<Void, String>() { 
//            @Override
//            protected Void doInBackground() throws Exception  
//            {
//                String str = "";
//                int cornerIdx = 0;
//                while ((cornerIdx < 4) && !isCancelled()) {
//                    switch (cornerIdx) {
//                        case 0:
//                            str = "Finding Upper Left Image Corner";
//                            break;
//                        case 1:
//                            str = "Finding Lower Left Image Corner";
//                            break;
//                        case 2:
//                            str = "Finding Lower Right Image Corner";
//                            break;
//                        case 3:
//                            str = "Finding Upper Right Image Corner";
//                            break;
//                    }
//
//                    publish(str);
//                    
//                    Logger.trace("Walking from " + centralLocation + " to " + 
//                            desiredCameraCorners.get(cornerIdx) + " " + str);
//                    
//                    maskDiameter = initialMaskDiameter;
//                    
//                    Location corner = walkToPoint(desiredCameraCorners.get(cornerIdx), 
//                            xScaling, yScaling);
//                    
//                    if (isCancelled()) {
//                        return null;
//                    }
//                    
//                    if (corner != null) {
//                        cameraCornerLocations.add(corner);
//                        cornerIdx++;
//                    }
//                    else {
//                        if (MessageBoxes.errorBoxWithRetry(mainFrame, 
//                                "ERROR - Failed to find calibration rig's fiducial.",
//                                "Edit the pipeline and try again?")) {
//                            CvPipelineEditor pipelineEditor = new CvPipelineEditor(pipeline);
//                            JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), 
//                                    "Camera Calibration Rig Fiducial Pipeline", pipelineEditor);
//                            dialog.setModalityType(ModalityType.APPLICATION_MODAL);
//                            
//                            //Because the dialog is modal, this thread of execution will hang
//                            //here until the dialog is closed, at which point it will resume
//                            dialog.setVisible(true);
//                        }
//                        else {
//                            return null;
//                        }
//                    }
//                }
//                return null;
//            }
//            
//            @Override
//            protected void process(List<String> chunks) {
//                for (String str : chunks) {
//                    if (!this.isCancelled()) {
//                        mainFrame.showInstructions("Camera Calibration Instructions", str,
//                            true, false, "Next", cancelActionListener, proceedActionListener);
//                    }
//                }
//            }
//            
//            @Override
//            protected void done()  
//            {
//                Logger.trace("cameraCornerLocations = " + cameraCornerLocations);
//                
//                if (cameraCornerLocations.size() != 4) {
//                    cleanUpWhenCancelled();
//                    return;
//                }
//                //Find a perspective transform that maps the four image corners to the 
//                //corresponding machine X/Y coordinates
//                org.opencv.core.Point srcCornerPoint[] = new org.opencv.core.Point[4];
//                org.opencv.core.Point dstCornerPoint[] = new org.opencv.core.Point[4];
//                for (int i=0; i<4; i++) {
//                    Point2D srcCorner = desiredCameraCorners.get(i);
//                    srcCornerPoint[i] = new org.opencv.core.Point(srcCorner.getX(), 
//                            srcCorner.getY());
//                    Location dstCorner = cameraCornerLocations.get(i);
//                    dstCornerPoint[i] = new org.opencv.core.Point(dstCorner.getX(), 
//                            dstCorner.getY());
//                }
//                MatOfPoint2f src = new MatOfPoint2f();
//                src.fromArray(srcCornerPoint);
//                MatOfPoint2f dst = new MatOfPoint2f();
//                dst.fromArray(dstCornerPoint);
//
//                Logger.trace("src = " + src.dump());
//                Logger.trace("dst = " + dst.dump());
//                transformImageToMachine = Imgproc.getPerspectiveTransform(src, dst);
//                src.release();
//                dst.release();
//                
//                double testPatternStepX = testPatternFillFraction * (pixelsX - 1) / 
//                        (numberOfPointsPerTestPatternX - 1);
//              
//                double testPatternStepY = testPatternFillFraction * (pixelsY - 1) / 
//                        (numberOfPointsPerTestPatternY - 1);
//                
//                double testPatternOffsetX = 0.5 * (1 - testPatternFillFraction) * (pixelsX - 1);
//                
//                double testPatternOffsetY = 0.5 * (1 - testPatternFillFraction) * (pixelsY - 1);
//                
//                expectedImagePointsList = new ArrayList<Point2D>();
//                Mat expectedImagePoints = Mat.zeros(3, actualPointsPerTestPattern, CvType.CV_64FC1);
//                int nPoints = 0;
//                for (int iRow = 0; iRow < numberOfPointsPerTestPatternY; iRow++) {
//                    for (int iCol = 0; iCol < numberOfPointsPerTestPatternX; iCol++) {
//                        expectedImagePointsList.add(new Point(
//                                (int) (iCol*testPatternStepX + testPatternOffsetX), 
//                                (int) (iRow*testPatternStepY + testPatternOffsetY)) );
//                        expectedImagePoints.put(0, nPoints, 
//                                iCol*testPatternStepX + testPatternOffsetX);
//                        expectedImagePoints.put(1, nPoints, 
//                                iRow*testPatternStepY + testPatternOffsetY);
//                        expectedImagePoints.put(2, nPoints, 1);
//                        nPoints++;
//                    }
//                }
//                Logger.trace("expectedImagePointsList = " + expectedImagePointsList);
//                
//                Mat expectedMachinePoints = Mat.zeros(3, actualPointsPerTestPattern, 
//                        CvType.CV_64FC1);
//                Core.gemm(transformImageToMachine, expectedImagePoints, 1.0, expectedImagePoints, 
//                        0, expectedMachinePoints);
//                expectedImagePoints.release();
//                        
//                testPatternLocationsList = new ArrayList<Location>();
//                testPatternIndiciesList = new ArrayList<Integer>();
//                nPoints = 0;
//                for (int iRow = 0; iRow < numberOfPointsPerTestPatternY; iRow++) {
//                    for (int iCol = 0; iCol < numberOfPointsPerTestPatternX; iCol++) {
//                        double scale = 1.0 / expectedMachinePoints.get(2, nPoints)[0];
//                        testPatternLocationsList.add(new Location(LengthUnit.Millimeters, 
//                                scale * expectedMachinePoints.get(0, nPoints)[0], 
//                                scale * expectedMachinePoints.get(1, nPoints)[0], 
//                                movableZ, 0));
//                        testPatternIndiciesList.add(nPoints);
//                        nPoints++;
//                    }
//                }
//                expectedMachinePoints.release();
//                
//                //To help prevent any machine bias from creeping into the measurements, the test
//                //pattern is shuffled so that the points are visited in a random but deterministic
//                //order
//                Collections.shuffle(testPatternIndiciesList, new Random(calibrationHeightIndex+1));
//                
//                Logger.trace("testPattern = " + testPatternLocationsList);
//                
//                advance();
//            }
//            
//        };
//        
//        swingWorker.execute();
//        
//        return true;
//    }
//
//    /**
//     * Action to collect the calibration points for a test pattern (at a single Z height)
//     * 
//     * @return true if the action was successful and the state machine should move to the next step
//     */
//    private boolean step5() {
//        List<double[]> testPattern3dPoints = new ArrayList<>();
//        List<double[]> testPatternImagePoints = new ArrayList<>();
//        
//        swingWorker = new SwingWorker<Void, String>() { 
//            @Override
//            protected Void doInBackground() throws Exception  
//            {
//                maskDiameter = initialMaskDiameter;
//                
//                for (int iPoint = 0; (iPoint < actualPointsPerTestPattern) && !isCancelled(); 
//                        iPoint++) {
//                    publish(String.format("Collecting calibration point %d of %d for calibration "
//                            + "pattern %d of %d", iPoint+1, actualPointsPerTestPattern, 
//                            calibrationHeightIndex+1, numberOfCalibrationHeights));
//
//                    Location testLocation = testPatternLocationsList.
//                            get(testPatternIndiciesList.get(iPoint));
//                    
//                    Point2D expectedPoint = expectedImagePointsList.
//                            get(testPatternIndiciesList.get(iPoint));
//                    
//                    org.opencv.core.Point measuredPoint = findAveragedFiducialPoint(testLocation, 
//                            expectedPoint);
//                    
//                    //Save the test pattern location and the corresponding image point
//                    if (measuredPoint != null) {
//                        testPattern3dPoints.add( 
//                                new double[] {apparentMotionDirection*testLocation.getX(), 
//                                              apparentMotionDirection*testLocation.getY(), 
//                                              testPatternZ});
//                        testPatternImagePoints.add(
//                                new double[] {measuredPoint.x, measuredPoint.y});
//                    }
//                }
//                testPattern3dPointsList.add(testPattern3dPoints);
//                testPatternImagePointsList.add(testPatternImagePoints);
//                return null;
//            }
//            
//            @Override
//            protected void process(List<String> chunks) {
//                for (String str : chunks) {
//                    if (!this.isCancelled()) {
//                        mainFrame.showInstructions("Camera Calibration Instructions", str,
//                            true, false, "Next", cancelActionListener, proceedActionListener);
//                    }
//                }
//            }
//            
//            @Override
//            protected void done()  
//            {
//                if (this.isCancelled()) {
//                    cleanUpWhenCancelled();
//                    return;
//                }
//                
//                cameraView.setCameraViewFilter(null);
//                
//                calibrationHeightIndex++;
//                if (calibrationHeightIndex == numberOfCalibrationHeights) {
//                    double[][][] testPattern3dPointsArray = 
//                            new double[testPattern3dPointsList.size()][][];
//                    for (int tpIdx=0; tpIdx<testPattern3dPointsList.size(); tpIdx++) {
//                        List<double[]> tp = testPattern3dPointsList.get(tpIdx);
//                        testPattern3dPointsArray[tpIdx] = new double[tp.size()][];
//                        for (int ptIdx=0; ptIdx<tp.size(); ptIdx++) {
//                            double[] pt = tp.get(ptIdx);
//                            testPattern3dPointsArray[tpIdx][ptIdx] = pt;
//                        }
//                    }
//                        
//                    double[][][] testPatternImagePointsArray = 
//                            new double[testPatternImagePointsList.size()][][];
//                    for (int tpIdx=0; tpIdx<testPatternImagePointsList.size(); tpIdx++) {
//                        List<double[]> tp = testPatternImagePointsList.get(tpIdx);
//                        testPatternImagePointsArray[tpIdx] = new double[tp.size()][];
//                        for (int ptIdx=0; ptIdx<tp.size(); ptIdx++) {
//                            double[] pt = tp.get(ptIdx);
//                            testPatternImagePointsArray[tpIdx][ptIdx] = pt;
//                        }
//                    }
//                    
//                    try {
//                        processRawCalibrationData(testPattern3dPointsArray, testPatternImagePointsArray, 
//                            new Size(pixelsX, pixelsY));
//                    }
//                    catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    finally {
//                        CalibrateCameraProcess.this.cleanUpWhenDone();
//                    }
//                }
//                else {
//                    advance();
//                }
//            }
//            
//        };
//        
//        swingWorker.execute();
//        
//        return true;
//    }
//
//    /**
//     * Action to take when transitioning from step 5 to step 6
//     * 
//     * @return true if the action was successful and the state machine should move to the next step
//     */
//    private boolean step6() {
//        if (isHeadMountedCamera) {
//            return true;
//        }
//        else {
//            //Set the next calibration height
//            testPatternZ = calibrationHeights.get(calibrationHeightIndex).
//                    convertToUnits(LengthUnit.Millimeters).getValue();
//            movableZ = testPatternZ + 
//                    calibrationRig.getHeight().convertToUnits(LengthUnit.Millimeters).getValue();
//            
//            showCircle(new org.opencv.core.Point(imageCenterPoint.getX(), imageCenterPoint.getY()), 
//                    (int)(0.75*maskDiameter/2), Color.GREEN);
//        
//            Logger.trace("testPatternZ = " + testPatternZ);
//            Logger.trace("movableZ = " + movableZ);
//            
//            //Move back to the center and change the calibration rig to the next calibration height
//            UiUtils.submitUiMachineTask(() -> {
//                nozzle.moveTo(centralLocation.convertToUnits(LengthUnit.Millimeters).
//                        derive(null, null, movableZ, (double)angle));
//            });
//            
//            //Go back to allow the operator to center the pattern in the camera's view
//            step = 1;
//            
//            return true;
//        }
//    }
//    
//    /**
//     * Action to take after the operator has manually changed the calibration rig's height
//     * 
//     * @return true if the action was successful and the state machine should move to the next step
//     */
//    private boolean step7() {
//        if (isHeadMountedCamera && 
//                Double.isFinite(calibrationHeights.get(calibrationHeightIndex).getValue())) {
//            testPatternZ = calibrationHeights.get(calibrationHeightIndex).
//                    convertToUnits(LengthUnit.Millimeters).getValue();
//            //Go back to have the operator center the fiducial in the camera view
//            step = 1;
//        }
//        else {
//            //Go back to have the operator touch the nozzle tip onto the rig to measure its height
//            step = 0;
//        }
//        return true;
//    }

    private Location walkToPoint(Point2D desiredCameraPoint, double xScaling,
            double yScaling ) throws InterruptedException, ExecutionException {
        return walkToPoint(desiredCameraPoint, xScaling, yScaling, null, null);
    }
    
    /**
     * Gradually "walks" the movable in X and Y such that the calibration rig's fiducial appears at
     * the desired image location.  The steps taken are deliberately small so that errors due to 
     * camera distortion and/or scaling do not lead to erroneous machine motions. 
     * @param desiredCameraPoint - desired image location in pixels
     * @param xScaling - a rough signed estimate of the X axis units per pixel scaling
     * @param yScaling - a rough signed estimate of the Y axis units per pixel scaling
     * @return the machine location where the calibration rig's fiducial appears at the desired
     * image location
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private Location walkToPoint(Point2D desiredCameraPoint, double xScaling,
            double yScaling, List<double[]> machine3DCoordinates, 
            List<double[]> image2DCoordinates) throws InterruptedException, ExecutionException {
        boolean savePoints = (machine3DCoordinates != null) && (image2DCoordinates != null);
        
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
        
        double maxAppliedError2 = Math.pow(Math.min(pixelsX, pixelsY)/5.0, 2);
        
        while (newError2 < oldError2) {
            if (swingWorker.isCancelled() || Thread.interrupted()) {
                return null;
            }
            oldError2 = newError2;
            oldLocation = newLocation;
            
            if (savePoints) {
                machine3DCoordinates.add(new double[] {oldLocation.getX(), oldLocation.getY(), 
                        oldLocation.getZ()});
                image2DCoordinates.add(new double[] {foundPoint.x, foundPoint.y});
            }
            
            if (newError2 * loopGain > maxAppliedError2) {
                double scaling = Math.sqrt(maxAppliedError2/(newError2 * loopGain));
                errorX *= scaling;
                errorY *= scaling;
            }
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
        } while (count < numberOfAngles && !swingWorker.isCancelled() && !Thread.interrupted());

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
        pipeline.setProperty("DetectCircularSymmetry.center", new org.openpnp.model.Point(
                expectedPoint.getX(), expectedPoint.getY()));
        
        maskDiameter = 2*(int)Math.min(expectedPoint.getX(), 
                Math.min(pixelsX-expectedPoint.getX(),
                Math.min(expectedPoint.getY(),
                Math.min(pixelsY-expectedPoint.getY(), maskDiameter/2))));
        
        pipeline.setProperty("MaskCircle.diameter", maskDiameter);
        pipeline.setProperty("DetectCircularSymmetry.maxDistance", maskDiameter/2.0);
        
        List<KeyPoint> keypoints = null;
        int attempts = 0;
        
        //Keep trying to locate the point until it is found or the maximum attempts have been made
        while ((keypoints == null) && (attempts < 3)) {
            if (swingWorker.isCancelled() || Thread.interrupted()) {
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
            
            if (Thread.interrupted()) {
                return null;
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
                
                //Show a circle centered on where the fiducial is expected to be found
                Color circleColor = Color.GREEN;
                Color pointColor = Color.GREEN;
                double enclosingDiameter = 2*minDistance + bestKeyPoint.size;
                if (enclosingDiameter > 0.70*maskDiameter) {
                    //Turn the point yellow as it is getting too close to the edge of the masked
                    //circle - may want to consider increasing the maskDiameter if this continues
                    //to occur
                    pointColor = Color.YELLOW;
                    changeMaskSize++;
                }
                else if (enclosingDiameter < 0.10*maskDiameter) {
                    //Turn the point blue as it is near the center of the masked circle - may want 
                    //to consider decreasing the maskDiameter if this continues to occur
                    pointColor = Color.BLUE;
                    changeMaskSize--;
                }
                else {
                    changeMaskSize -= (int)Math.signum(changeMaskSize);
                }
                if (swingWorker.isCancelled() || Thread.interrupted()) {
                    return null;
                }
                showPointAndCircle(bestKeyPoint.pt, new org.opencv.core.Point(expectedPoint.getX(),
                        expectedPoint.getY()), maskDiameter/2, pointColor, circleColor);
                if (changeMaskSize > changeMaskThreshold) {
                    maskDiameter *= 1.10;
                    pipeline.setProperty("MaskCircle.diameter", maskDiameter);
                    pipeline.setProperty("DetectCircularSymmetry.maxDistance", maskDiameter/2.0);
                    Logger.trace("Increasing mask diameter to " + maskDiameter);
                    changeMaskSize--;
                }
                else if (changeMaskSize < -changeMaskThreshold) {
                    maskDiameter *= 0.95;
                    pipeline.setProperty("MaskCircle.diameter", maskDiameter);
                    pipeline.setProperty("DetectCircularSymmetry.maxDistance", maskDiameter/2.0);
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
    protected void cleanUpWhenDone() {
        while ((swingWorker != null) && !swingWorker.isDone()) {
            swingWorker.cancel(true);
        }
        
        cameraView.setCameraViewFilter(null);

        try {
            mainFrame.hideInstructions();
        }
        catch (Exception e) {
            //Ok - may be already hidden
        }
        
        ((ReferenceMachine) Configuration.get().getMachine()).
            setAutoToolSelect(savedAutoToolSelect);
    }

    /**
     * Clean-up when the process is cancelled
     */
    protected void cleanUpWhenCancelled() {
        cleanUpWhenDone();
        processCanceled();
    }
    
    /**
     * Displays a status string with only a Cancel button visible
     * @param status - the status to display - may include format specifiers acceptable 
     * to String.format() corresponding to the arguments passed in statusArguments
     * @param statusArguments - zero or more arguments to be formated and inserted into the 
     * status string
     */
    private void displayStatus(String status, Object... statusArguments) {
        mainFrame.showInstructions("Camera Calibration Instructions/Status", 
                "<html><body>" + String.format(status, statusArguments) + "</body></html>", 
                true, false, "Next", cancelActionListener, proceedActionListener);
    }
    
    /**
     * Displays instructions in the mainframe and sets the action to take when the Next button is 
     * clicked
     * @param instructions - the instructions to display - may include format specifiers acceptable 
     * to String.format() corresponding to the arguments passed in instructionArguments
     * @param proceedAction - the action to take when the Next button is clicked
     * @param instructionArguments - zero or more arguments to be formated and inserted into the 
     * instructions string
     */
    private void setInstructionsAndProceedAction(String instructions, 
            Thrunnable proceedAction, Object... instructionArguments) {
        mainFrame.showInstructions("Camera Calibration Instructions/Status", 
                "<html><body>" + String.format(instructions, instructionArguments) + "</body></html>", 
                true, true,  "Next", cancelActionListener, proceedActionListener);
        this.proceedAction = proceedAction;
    }
    
    private void setProceedClickAction(Thrunnable proceedFunction) {
        this.proceedAction = proceedFunction;
    }
    
    /**
     * Process Proceed button clicks
     */
    private final ActionListener proceedActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
//            advance();
            if (proceedAction != null) {
                Thrunnable tempAction = proceedAction;
                proceedAction = null;
                try {
                    tempAction.thrun();
                }
                catch (Exception ex) {
                    MessageBoxes.errorBox(MainFrame.get(), "Error", ex);
                    cleanUpWhenCancelled();
                }
            }
        }
    };

    /**
     * Process Cancel button clicks
     */
    private final ActionListener cancelActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            Logger.trace("Calibration cancelled at operator's request");
            cleanUpWhenCancelled();
            
            if (rigIsOnNozzle ) {
                if(MessageBoxes.errorBoxWithRetry(MainFrame.get(), "Calibration Complete", "Return calibration rig to original pick location? (otherwise it will remain on the nozzle)")) {
                    returnRigToInitialLocationAction();
                }
            }
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
                        Imgproc.circle(mat, center, radius, FluentCv.colorToScalar(circleColor), 1);
                    }
                    BufferedImage result = OpenCvUtils.toBufferedImage(mat);
                    mat.release();
                    return result;
                }
            });
        }
    }
}
