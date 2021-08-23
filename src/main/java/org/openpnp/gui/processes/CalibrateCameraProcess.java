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
import org.openpnp.model.Point;
import org.openpnp.spi.Camera;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.CameraWalker;
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
    private Point trialPoint1;
    private Point trialPoint2;

    private double xScaling;

    private double yScaling;

    private ArrayList<Point> desiredCameraCorners;

    private ArrayList<Location> cameraCornerLocations;

    protected Mat transformImageToMachine;

    private int numberOfPointsPerTestPatternX;

    private int numberOfPointsPerTestPatternY;

    private int actualPointsPerTestPattern;

    protected ArrayList<Point> expectedImagePointsList;

    protected ArrayList<Location> testPatternLocationsList;

    protected ArrayList<Integer> testPatternIndiciesList;

    private boolean savedAutoVisible;

    private boolean savedAutoToolSelect;
    
    private Point imageCenterPoint;
    
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

    protected CameraWalker cameraWalker;


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
        imageCenterPoint = new Point((pixelsX-1.0)/2, (pixelsY-1.0)/2);

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
                getFiducialLocator()).getPartSettings(calibrationRigPart).getPipeline(); //.clone();
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
                
                cameraWalker = new CameraWalker(movable, 
                        new Location(LengthUnit.Millimeters, xScaling, yScaling, 0, 0), 
                        (Point p)->findAveragedFiducialPoint(p));
                cameraWalker.setLoopGain(0.2);
                
                publish("Finding image center.");
                Logger.trace("Finding image center.");
                centralLocation = cameraWalker.walkToPoint(imageCenterPoint);
                
                cameraWalker.setLoopGain(advCal.getWalkingLoopGain());
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
        desiredCameraCorners = new ArrayList<Point>();
        
        double testPatternFillFraction = advCal.getTestPatternFillFraction();
        
        //In order: upper left, lower left, lower right, and upper right
        desiredCameraCorners.add(new Point((1-testPatternFillFraction)*pixelsX/2,
                (1-testPatternFillFraction)*pixelsY/2));
        desiredCameraCorners.add(new Point((1-testPatternFillFraction)*pixelsX/2,
                (1+testPatternFillFraction)*pixelsY/2));
        desiredCameraCorners.add(new Point((1+testPatternFillFraction)*pixelsX/2,
                (1+testPatternFillFraction)*pixelsY/2));
        desiredCameraCorners.add(new Point((1+testPatternFillFraction)*pixelsX/2,
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
                    
                    Location corner = cameraWalker.walkToPoint(centralLocation, imageCenterPoint,
                            desiredCameraCorners.get(cornerIdx));
                    
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
                    Point srcCorner = desiredCameraCorners.get(i);
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
                
                expectedImagePointsList = new ArrayList<Point>();
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
        cameraWalker.setSaveCoordinates(testPattern3dPoints, testPatternImagePoints);
        cameraWalker.setMaxAllowedPixelStep(Math.min(pixelsX, pixelsY)/12.0);
        cameraWalker.setMinAllowedPixelStep(10);
        
        List<Point> desiredEndPoints = new ArrayList<>();
        
        int numberOfLines = 4 * (int)Math.ceil(desiredNumberOfRadials/4.0);

        for (int i=0; i<numberOfLines; i++) {
            double lineAngle = i * 2 * Math.PI / numberOfLines;
            double[] unitVector = new double[] {Math.cos(lineAngle), Math.sin(lineAngle)};
            double scaling = advCal.getTestPatternFillFraction()*
                    Math.min(pixelsX / (2*Math.abs(unitVector[0])),
                    pixelsY / (2*Math.abs(unitVector[1])));
            desiredEndPoints.add(new Point(scaling*unitVector[0] + pixelsX/2, 
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
                    cameraWalker.walkToPoint(centralLocation, imageCenterPoint, 
                            desiredEndPoints.get(iLine));
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
                    
                    Point expectedPoint = expectedImagePointsList.
                            get(testPatternIndiciesList.get(iPoint));
                    
                    Point measuredPoint = findAveragedFiducialPoint(testLocation, 
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
                ()->requestOperatorToAdjustDiameterAction());
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
        setInstructionsAndProceedAction("Adjust the Detection Diameter spinner until the orange circle turns green and snaps to the outer edge of the fiducial. Click Next when ready", 
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
        
        swingWorker = new SwingWorker<Void, String>() {

            @Override
            protected Void doInBackground() throws Exception {
                while (!isCancelled()) {
                    pipeline.setProperty("MaskCircle.diameter", maskDiameter);
                    pipeline.setProperty("SimpleBlobDetector.area", 
                            Math.PI*Math.pow(advCal.getFiducialDiameter()/2.0, 2));
                    pipeline.setProperty("DetectCircularSymmetry.diameter", 
                            1.0*advCal.getFiducialDiameter());
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
                    
                    if (!isCancelled()) {
                        if (keyPoints != null) {
                            //Of all the points found, keep the one closest to the expected location
                            double minDistance = Double.POSITIVE_INFINITY;
                            KeyPoint bestKeyPoint = null;
                            for (KeyPoint kpt : keyPoints) {
                                double dx = kpt.pt.x - expectedPoint.getX();
                                double dy = kpt.pt.y - expectedPoint.getY();
                                double distance = Math.hypot(dx, dy);
                                if (distance < minDistance) {
                                    bestKeyPoint = kpt;
                                    minDistance = distance;
                                }
                            }
        
                            //Show a green circle centered on where the fiducial was found
                            Color circleColor = Color.GREEN;
                            showCircle(bestKeyPoint.pt, (int)(bestKeyPoint.size/2), circleColor);
                        }
                        else {
                            //Show an orange circle centered on where the fiducial was expected
                            Color circleColor = Color.ORANGE;
                            showCircle(new org.opencv.core.Point((pixelsX-1)/2.0, (pixelsY-1)/2.0), 
                                    advCal.getFiducialDiameter()/2, circleColor);
                        }
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
        estimateUnitsPerPixelAction();
        
        return true;
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
    private Point findAveragedFiducialPoint(Location testLocation, 
            Point expectedPoint) {
        int count = 0;
        Point observedPoint = null;
        Point measuredPoint = new Point(0, 0);
        
        do {
            final Location moveLocation = testLocation.convertToUnits(LengthUnit.Millimeters).
                    derive(null, null, null, (double) angle);

            //Move the machine and capture the fiducial location
            Future<Void> future = UiUtils.submitUiMachineTask(() -> {
                movable.moveTo(moveLocation);
            });

            //Wait for the move to complete
            try {
                future.get();
            }
            catch (Exception e) {
                return null;
            }

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
    
    private Point findAveragedFiducialPoint(Point expectedPoint) {
        int count = 0;
        Point observedPoint = null;
        Point measuredPoint = new Point(0, 0);
        
        do {
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
                
                //Move the machine to the new angle
                final Location moveLocation = movable.getLocation().
                        derive(null, null, null, (double) angle);
                Future<Void> future = UiUtils.submitUiMachineTask(() -> {
                    movable.moveTo(moveLocation);
                });

                //Wait for the move to complete
                try {
                    future.get();
                }
                catch (Exception e) {
                    return null;
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
    private Point findCalibrationRigFiducialPoint(Point expectedPoint) {
        //Mask off everything that is not near the expected location
        pipeline.setProperty("MaskCircle.center", expectedPoint.getX());
        pipeline.setProperty("DetectCircularSymmetry.center", expectedPoint);
        
        maskDiameter = 2*(int)Math.min(expectedPoint.getX(), 
                Math.min(pixelsX-expectedPoint.getX(),
                Math.min(expectedPoint.getY(),
                Math.min(pixelsY-expectedPoint.getY(), maskDiameter/2))));
        
        pipeline.setProperty("MaskCircle.diameter", maskDiameter);
        pipeline.setProperty("DetectCircularSymmetry.maxDistance", maskDiameter/2.0);

        pipeline.setProperty("SimpleBlobDetector.area", Math.PI*Math.pow(advCal.getFiducialDiameter()/2.0, 2));
        pipeline.setProperty("DetectCircularSymmetry.diameter", 1.0*advCal.getFiducialDiameter());
        
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
                    double distance = Math.hypot(dx, dy);
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
                return Point.fromOpencv(bestKeyPoint.pt);
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
