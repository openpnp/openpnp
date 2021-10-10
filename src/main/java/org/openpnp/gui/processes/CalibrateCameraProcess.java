/*
 * Copyright (C) Tony Luken <tonyluken62+openpnp@gmail.com>
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.CameraViewFilter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.camera.calibration.AdvancedCalibration;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Point;
import org.openpnp.spi.Camera;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.CameraWalker;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.UiUtils.Thrunnable;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.pmw.tinylog.Logger;

/**
 * Guides the operator on calibrating a camera with step by step instructions.
 * 
 */
public abstract class CalibrateCameraProcess {
    private static final int changeMaskThreshold = 3;
    private static final double initialMaskDiameterFraction = 1/6.0;
    private static final double centeringDiameterFraction = 0.5;
    
    private final int numberOfCalibrationHeights;
    private final MainFrame mainFrame;
    private final CameraView cameraView;
    private final Camera camera;
    private final boolean isHeadMountedCamera;
    private Nozzle nozzle;
    private CvPipeline pipeline;
    private double apparentMotionDirection;
    private double mirrored;
    private double movableZ;
    private double testPatternZ;
    private int pixelsX;
    private int pixelsY;
    private Location centralLocation;
    private HeadMountable movable;
    private SwingWorker<Void, String> swingWorker;

    protected Mat transformImageToMachine;

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

    protected CameraWalker cameraWalker;


    public CalibrateCameraProcess(MainFrame mainFrame, CameraView cameraView, 
            List<Length> calibrationHeights)
            throws Exception {
        this.mainFrame = mainFrame;
        this.cameraView = cameraView;
        this.calibrationHeights = new ArrayList<Length>(calibrationHeights);
        numberOfCalibrationHeights = calibrationHeights.size();
        
        camera = cameraView.getCamera();
        pixelsX = camera.getWidth();
        pixelsY = camera.getHeight();
        imageCenterPoint = new Point((pixelsX-1.0)/2, (pixelsY-1.0)/2);

        advCal = ((ReferenceCamera)camera).getAdvancedCalibration();
        
        testPattern3dPointsList = new ArrayList<>();
        testPatternImagePointsList = new ArrayList<>();
        
        calibrationHeightIndex = 0;
        
        isHeadMountedCamera = camera.getHead() != null;
        apparentMotionDirection = isHeadMountedCamera ? -1.0 : +1.0;
        
        angleIncrement = isHeadMountedCamera ? 360 : 180;
        numberOfAngles = 360 / angleIncrement;
        observationWeight = 1.0 / numberOfAngles;
        
        pipeline = advCal.getPipeline();
        pipeline.setProperty("camera", camera);
        
        initialMaskDiameter = (int) (initialMaskDiameterFraction * Math.min(pixelsX, pixelsY));
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
     * 
     * @param mirrored - -1.0 if the raw camera image coordinates are mirrored (flipped) relative to
     * the machine coordinates, +1.0 otherwise
     * @param apparentMotionDirection - +1.0 if the apparent motion of objects in the images moves 
     * in the same direction as the machine moves, -1.0 otherwise
     * @throws Exception 
     */
    protected abstract void processRawCalibrationData(double[][][] testPattern3dPoints, 
            double[][][] testPatternImagePoints, Size size, double mirrored, 
            double apparentMotionDirection) throws Exception;
    
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
        if (isHeadMountedCamera) {
            movable = camera;
            testPatternZ = calibrationHeights.get(calibrationHeightIndex).
                    convertToUnits(LengthUnit.Millimeters).getValue();

            movableZ = 0;
            
            maskDiameter = initialMaskDiameter;
            showCircle(new org.opencv.core.Point(imageCenterPoint.getX(), imageCenterPoint.getY()), 
                    (int)(centeringDiameterFraction*maskDiameter/2), Color.GREEN);
            
            setInstructionsAndProceedAction("Using the jog controls on the Machine Controls panel, jog the camera so that the calibration fiducial at Z = %s is approximately centered in the green circle.  Click Next when ready to proceed.", 
                    ()->requestOperatorToAdjustDiameterAction(),
                    calibrationHeights.get(calibrationHeightIndex).toString());
        }
        else { //bottom camera
            testPatternZ = calibrationHeights.get(calibrationHeightIndex).
                    convertToUnits(LengthUnit.Millimeters).getValue();
            
            movableZ = testPatternZ;
            
            setInstructionsAndProceedAction("Select a nozzle and load it with the smallest available nozzle tip. Click Next when ready to proceed.",
                    ()->requestOperatorToCenterNozzleTipAction());
        }
            
        return true;
    }
    
    /**
     * Starts a background task to display the fiducial/nozzle tip detection to the operator as they
     * adjust the detection size.
     * 
     * @return true when complete
     */
    private boolean requestOperatorToAdjustDiameterAction() {
        setInstructionsAndProceedAction("Use the mouse scroll wheel to zoom in on the fiducial/nozzle tip and then adjust the Detection Diameter spinner until the red circle turns green with a + at its center and is sized to just fit the fiducial/nozzle tip with the + centered on the fiducial/nozzle tip. When ready, click Next to begin the automated calibration collection sequence.", 
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
        
                            //Show a green circle with a + centered on where the fiducial was found
                            Color circleColor = Color.GREEN;
                            showPointAndCircle(bestKeyPoint.pt, bestKeyPoint.pt, (int)(bestKeyPoint.size/2), circleColor);
                        }
                        else {
                            //Show an red circle centered on where the fiducial was expected
                            Color circleColor = Color.RED;
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
    
    /**
     * Cancels the background task that displayed the fiducial/nozzle tip detection to the operator
     * and then moves on to the unit per pixel estimation
     * 
     * @return true when complete
     */
    private boolean fiducialDiameterIsSetAction() {
        swingWorker.cancel(true);
        while (!swingWorker.isDone()) {
            //spin
        }
        estimateUnitsPerPixelAction();
        
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
        
        //Setup the camera walker
        cameraWalker = new CameraWalker(movable, (Point p)->findAveragedFiducialPoint(p));
        cameraWalker.setOnlySafeZMovesAllowed(isHeadMountedCamera);
        
        swingWorker = new SwingWorker<Void, String>() {

            @Override
            protected Void doInBackground() throws Exception {
                publish("Estimating Units Per Pixel.");
                try {
                    cameraWalker.estimateScaling(advCal.getTrialStep(), new Point(pixelsX/2, pixelsY/2));
                }
                catch (Exception e) {
                    return null;
                }
                
                if (isCancelled() || !cameraWalker.isReadyToWalk()) {
                    return null;
                }
                
                if (calibrationHeightIndex == 0) {
                    advCal.setApproximateMillimetersPerPixel(
                            cameraWalker.getEstimatedMillimetersPerPixel());
                }
                
                //Walk the fiducial/nozzle tip to the center of the image
                publish("Finding image center.");
                Logger.trace("Finding image center.");
                cameraWalker.setLoopGain(advCal.getWalkingLoopGain());
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
            protected void done() {
                if (isCancelled()) {
                    cleanUpWhenCancelled();
                    return;
                }
                
                if (!cameraWalker.isReadyToWalk()) {
                    if (!isCancelled()) {
                        MessageBoxes.errorBox(MainFrame.get(), "Error", "Could not estimate units per pixel - calibration aborting.");
                    }
                    cleanUpWhenCancelled();
                    return;
                }
                mirrored = cameraWalker.getMirror();
                
                collectCalibrationPointsAlongRadialLinesAction();
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
        cameraWalker.setMinAllowedPixelStep(Math.min(pixelsX, pixelsY)/60.0);
        
        List<Point> desiredEndPoints = new ArrayList<>();
        
        int numberOfLines = 4 * (int)Math.ceil(advCal.getDesiredRadialLinesPerTestPattern()/4.0);

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
            
            int errorCount = 0;
            
            @Override
            protected Void doInBackground() throws Exception {
                for (int iLine=0; iLine<numberOfLines; iLine++) {
                    maskDiameter = initialMaskDiameter;
                    publish(String.format("Collecting calibration points along radial line %d of %d at calibration Z coordinate %d of %d", 
                            iLine+1, numberOfLines, 
                            calibrationHeightIndex+1, numberOfCalibrationHeights));
                    try {
                        cameraWalker.walkToPoint(centralLocation.derive(null, null, null, movable.getLocation().convertToUnits(centralLocation.getUnits()).getRotation()), imageCenterPoint, 
                            desiredEndPoints.get(iLine));
                    }
                    catch (InterruptedException ie) {
                        return null;
                    }
                    catch (Exception e) {
                        errorCount++;
                        if (errorCount > 3) {
                            return null;
                        }
                    }
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
                
                if (errorCount > 3) {
                    MessageBoxes.errorBox(MainFrame.get(), "Error", "Too many misdetects - retry and verify fiducial/nozzle tip detection." );
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
                                    mirrored * pt[0], apparentMotionDirection * pt[1], 
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
                            new Size(pixelsX, pixelsY), mirrored, apparentMotionDirection);
                    }
                    catch (Exception ex) {
                        MessageBoxes.errorBox(MainFrame.get(), "Error", ex);
                    }
                    finally {
                        cleanUpWhenDone();
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
        maskDiameter = initialMaskDiameter;
        showCircle(new org.opencv.core.Point(imageCenterPoint.getX(), imageCenterPoint.getY()), 
                (int)(centeringDiameterFraction*maskDiameter/2), Color.GREEN);

        if (isHeadMountedCamera) {
            movableZ = 0;
            
            testPatternZ = calibrationHeights.get(calibrationHeightIndex).
                    convertToUnits(LengthUnit.Millimeters).getValue();
            
            setInstructionsAndProceedAction("Using the jog controls on the Machine Controls panel, jog the camera so that the calibration fiducial at Z = %s is approximately centered in the green circle.  Click Next when ready to proceed.", 
                    ()->requestOperatorToAdjustDiameterAction(),
                    calibrationHeights.get(calibrationHeightIndex).toString()); 
        }
        else { //bottom camera
            testPatternZ = calibrationHeights.get(calibrationHeightIndex).
                    convertToUnits(LengthUnit.Millimeters).getValue();

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
                (int)(centeringDiameterFraction*maskDiameter/2), Color.GREEN);
        
        setInstructionsAndProceedAction("Using the jog controls on the Machine Controls panel, jog the nozzle tip so that it is approximately in the center of the green circle. When ready, click Next to lower/raise the nozzle tip to the calibration height.", 
                ()->captureCentralLocationAction());
        return true;
    }

    /**
     * Action to Capture the location of the nozzle tip prior to it being lowered to the 
     * calibration height
     * 
     * @return true when complete
     */
    private boolean captureCentralLocationAction() {
        nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
        ((ReferenceNozzleTip) nozzle.getNozzleTip()).getCalibration().resetAll();
        movable = nozzle;
        
        centralLocation = nozzle.getLocation();
        
        changeNozzleTipHeightAction();
        
        return true;
    }
    
    /**
     * Action to move the nozzle tip to the center of the camera's field-of-view and lower/raise it 
     * to the calibration height
     * 
     * @return true when complete
     */ 
    private boolean changeNozzleTipHeightAction() {
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
     * Displays a request to the operator to verify that the nozzle tip is centered
     * 
     * @return true when complete
     */
    private boolean requestOperatorToVerifyNozzleTipIsCenteredAction() {
        setInstructionsAndProceedAction("Using the jog controls on the Machine Controls panel, rotate the nozzle tip through 360 degrees and verify it stays within the green circle. If necessary, jog it in X and/or Y so that it remains within the circle when it is rotated. Click Next when ready.", 
                ()->captureVerifiedCentralLocationAction());
        return true;
    }
    
    /**
     * Action to captures the current nozzle tip location
     * 
     * @return true when complete
     */
    private boolean captureVerifiedCentralLocationAction() {
        centralLocation = nozzle.getLocation();

        requestOperatorToAdjustDiameterAction();
        
        return true;
    }

    /**
     * Finds the fiducial 2D point averaged over all angles
     * @param expectedPoint - the 2D point, in pixels, where the fiducial is expected to be found
     * @return the averaged 2D point, in pixels, if the fiducial was found at all angles, otherwise
     * null
     */
    private Point findAveragedFiducialPoint(Point expectedPoint) {
        int count = 0;
        Point observedPoint = null;
        Point measuredPoint = new Point(0, 0);
        
        do {
            //Find the calibration rig's fiducial
            observedPoint = findCalibrationRigFiducialPoint(expectedPoint);
            
            if (observedPoint == null) {
                return null;
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
                    Logger.trace("rotating to angle = " + angle);
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
        
        //Restrict the mask diameter so as to not extend beyond the edge of  the image
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
        
        if (pipeline != null) {
            pipeline.release();
        }

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
    
    /**
     * Process Proceed button clicks
     */
    private final ActionListener proceedActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
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
                        double pointSize = Math.min(pixelsX, pixelsY) * 0.05;
                        org.opencv.core.Point p1 = new org.opencv.core.Point(point.x-pointSize, point.y);
                        org.opencv.core.Point p2 = new org.opencv.core.Point(point.x+pointSize, point.y);
                        Imgproc.line(mat, p1, p2, FluentCv.colorToScalar(pointColor), 1);
                        p1 = new org.opencv.core.Point(point.x, point.y-pointSize);
                        p2 = new org.opencv.core.Point(point.x, point.y+pointSize);
                        Imgproc.line(mat, p1, p2, FluentCv.colorToScalar(pointColor), 1);
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
