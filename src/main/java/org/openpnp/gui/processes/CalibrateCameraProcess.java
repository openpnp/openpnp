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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.Size;
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
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.Nozzle.RotationMode;
import org.openpnp.util.CameraWalker;
import org.openpnp.util.ImageUtils;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.UiUtils.Thrunnable;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.pmw.tinylog.Logger;

/**
 * Guides the operator on calibrating a camera with step by step instructions.
 * 
 */
public abstract class CalibrateCameraProcess {
    private CameraCalibrationProcessProperties props;
    
    private final int numberOfCalibrationHeights;
    private final MainFrame mainFrame;
    private final CameraView cameraView;
    private boolean automatic;
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

    private ArrayList<Location> calibrationLocations;
    
    private ArrayList<Integer> detectionDiameters;
    
    private AdvancedCalibration advCal;
    
    private int changeMaskSize = 0;

    private int initialMaskDiameter;

    private Thrunnable proceedAction;

    protected Boolean previousProceedActionResult;

    protected CameraWalker cameraWalker;
    private BufferedImage resultImage;
    private Machine machine;

    private boolean pauseAtSecondaryZ;

    private int automationLevel;

    public CalibrateCameraProcess(MainFrame mainFrame, CameraView cameraView, 
            List<Location> calibrationLocations, ArrayList<Integer> detectionDiameters, int automationLevel)
            throws Exception {
        this.mainFrame = mainFrame;
        this.cameraView = cameraView;
        this.calibrationLocations = new ArrayList<Location>(calibrationLocations);
        this.detectionDiameters = detectionDiameters;
        this.automatic = automationLevel > 0;
        this.automationLevel = automationLevel;

        machine = Configuration.get().getMachine();
        
        props = (CameraCalibrationProcessProperties) machine.getProperty("CameraCalibrationProcessProperties");
    
        if (props == null) {
            props = new CameraCalibrationProcessProperties();
            machine.setProperty("CameraCalibrationProcessProperties", props);
        }

        numberOfCalibrationHeights = calibrationLocations.size();
        Set<Length> setOfDifferentCalibrationHeights = new HashSet<Length>();
        for (Location location : calibrationLocations) {
            setOfDifferentCalibrationHeights.add(location.convertToUnits(LengthUnit.Millimeters).getLengthZ());
        }
        if (setOfDifferentCalibrationHeights.size() < 2) {
            throw new Exception("Number of different calibration heights is less than 2.");
        }
        
        if (detectionDiameters.size() != calibrationLocations.size()) {
            throw new Exception("Number of detection diameters does not equal the number of "
                    + "calibration heights.");
        }
        
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
        
        initialMaskDiameter = (int) (props.initialMaskDiameterFraction * Math.min(pixelsX, pixelsY));
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
        Logger.trace("Entering initialAction");
        advCal.initializeApproximateCameraF();
        testPatternZ = calibrationLocations.get(calibrationHeightIndex).
                convertToUnits(LengthUnit.Millimeters).getZ();

        if (isHeadMountedCamera) {
            movable = camera;

            maskDiameter = initialMaskDiameter;
            resultImage = null;
            showCircle(new org.opencv.core.Point(imageCenterPoint.getX(), imageCenterPoint.getY()), 
                    (int)(props.centeringDiameterFraction*maskDiameter/2), Color.GREEN);

            Location moveLocation = calibrationLocations.get(calibrationHeightIndex).
                    deriveLengths(null, null, camera.getSafeZ(), null);
            movableZ = moveLocation.convertToUnits(LengthUnit.Millimeters).getZ();
            if (Double.isFinite(moveLocation.getX()) && Double.isFinite(moveLocation.getY())) {
                try {
                    machine.execute(() -> {
                        MovableUtils.moveToLocationAtSafeZ(camera, moveLocation, 1.0);
                        return null;
                    });
                }
                catch (Exception ex) {
                    MessageBoxes.errorBox(MainFrame.get(), "Error", ex);
                    cleanUpWhenCancelled();
                }
                // Note, automatic is only available if the moveLocation is already set.
                if (automationLevel >= 2) {
                    requestOperatorToAdjustDiameterAction();
                    return true;
                }
            }
            setInstructionsAndProceedAction("Using the jog controls on the Machine Controls panel, jog the camera so that the calibration fiducial at Z = %s is approximately centered in the green circle.  Click Next when ready to proceed.", 
                    ()->requestOperatorToAdjustDiameterAction(),
                    calibrationLocations.get(calibrationHeightIndex).getLengthZ().toString());
        }
        else { //bottom camera
            movableZ = testPatternZ;

            if (automatic) {
                requestOperatorToCenterNozzleTipAction();
                return true;
            }
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
        Logger.trace("Entering requestOperatorToAdjustDiameterAction");
        Point2D expectedPoint = new Point2D.Double((pixelsX-1)/2.0, (pixelsY-1)/2.0);
        pipeline.setProperty("MaskCircle.center", new org.opencv.core.Point(expectedPoint.getX(), 
                expectedPoint.getY()));
        pipeline.setProperty("DetectCircularSymmetry.center", new org.openpnp.model.Point(
                expectedPoint.getX(), expectedPoint.getY()));

        if (detectionDiameters.get(calibrationHeightIndex) != null) {
            advCal.setFiducialDiameter(detectionDiameters.get(calibrationHeightIndex));
            if (automatic) {
                swingWorker = null;
                fiducialDiameterIsSetAction();
                return true;
            }
        }
        else {
            if (isHeadMountedCamera) {
                advCal.setFiducialDiameter(props.defaultDetectionDiameter);
            }
            else {
                double diameter = Double.NaN;
                try {
                    diameter = ((ReferenceNozzleTip) nozzle.getNozzleTip()).getCalibration().
                            getCalibrationTipDiameter().divide(camera.getUnitsPerPixel(
                                    calibrationLocations.get(calibrationHeightIndex).
                                    getLengthZ()).getLengthX());
                }
                catch (Exception e) {
                    //Ok - we handle NaNs below
                }
                if (Double.isFinite(diameter)) {
                    advCal.setFiducialDiameter((int) Math.round(diameter));
                }
                else {
                    advCal.setFiducialDiameter(props.defaultDetectionDiameter);
                }
            }
        }
        
        maskDiameter = 2*(int)Math.min(expectedPoint.getX(), 
                Math.min(pixelsX-expectedPoint.getX(),
                Math.min(expectedPoint.getY(),
                Math.min(pixelsY-expectedPoint.getY(),
                Math.max(maskDiameter/2, advCal.getFiducialDiameter()*1.2)))));
        
        swingWorker = new SwingWorker<Void, String>() {

            @Override
            protected Void doInBackground() throws Exception {
                while (!isCancelled()) {
                    pipeline.setProperty("MaskCircle.diameter", maskDiameter);
                    pipeline.setProperty("SimpleBlobDetector.area", 
                            Math.PI*Math.pow(advCal.getFiducialDiameter()/2.0, 2));
                    pipeline.setProperty("DetectCircularSymmetry.diameter", 
                            advCal.getFiducialDiameter());
                    List<KeyPoint> keyPoints = null;

                    try {
                        pipeline.process();
                        keyPoints = pipeline.getExpectedResult(VisionUtils.PIPELINE_RESULTS_NAME)
                                .getExpectedListModel(KeyPoint.class, 
                                        new Exception("Calibration rig fiducial not found."));
                        resultImage = OpenCvUtils.toBufferedImage(pipeline.getWorkingImage());
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
                            showPointAndCircle(bestKeyPoint.pt, bestKeyPoint.pt, 
                                    (int)(bestKeyPoint.size/2), circleColor);
                        }
                        else {
                            //Show a red circle centered on where the fiducial was expected
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
                detectionDiameters.set(calibrationHeightIndex, advCal.getFiducialDiameter());
                cameraView.setCameraViewFilter(null);
            }
        };

        setInstructionsAndProceedAction("Use the mouse scroll wheel to zoom in on the fiducial/nozzle tip and then adjust the Detection Diameter spinner until the red circle turns green with a + at its center and is sized to just fit the fiducial/nozzle tip with the + centered on the fiducial/nozzle tip. When ready, click Next to begin the automated calibration collection sequence.", 
                ()->fiducialDiameterIsSetAction());

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
        Logger.trace("Entering fiducialDiameterIsSetAction");
        if (swingWorker != null) {
            swingWorker.cancel(true);
            while (!swingWorker.isDone()) {
                //spin
            }
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
        Logger.trace("Entering estimateUnitsPerPixelAction");
        
        //Capture the movable's coordinate as the approximate center of the camera's field-of-view
        centralLocation = movable.getLocation().convertToUnits(LengthUnit.Millimeters).
                derive(null, null, movableZ, 0.0);
        try {
            if (movable instanceof Nozzle
                    && ((Nozzle) movable).getRotationMode() == RotationMode.LimitedArticulation
                    && angleIncrement < 360) {
                // Make sure it is compliant with limited articulation.
                Location l1 = centralLocation.derive(null, null, null, (double) angleIncrement);
                ((Nozzle) movable).prepareForPickAndPlaceArticulation(centralLocation, l1);
            } 
        }
        catch (Exception e) {
            Logger.warn(e);
        }
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
                    Logger.trace(e);
                    return null;
                }
                
                if (isCancelled() || !cameraWalker.isReadyToWalk()) {
                    return null;
                }
                
                advCal.updateApproximateCameraF(cameraWalker.getEstimatedMillimetersPerPixel(),
                        testPatternZ);
                
                //Walk the fiducial/nozzle tip to the center of the image
                publish("Finding image center.");
                Logger.trace("Finding image center.");
                cameraWalker.setLoopGain(advCal.getWalkingLoopGain());
                centralLocation = cameraWalker.walkToPoint(imageCenterPoint);
                
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
                mirrored = apparentMotionDirection * cameraWalker.getMirror();
                
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
        Logger.trace("Entering collectCalibrationPointsAlongRadialLinesAction");
        List<double[]> testPattern3dPoints = new ArrayList<>();
        List<double[]> testPatternImagePoints = new ArrayList<>();
        cameraWalker.setSaveCoordinates(testPattern3dPoints, testPatternImagePoints);
        cameraWalker.setMaxAllowedPixelStep(props.maxAllowedPixelStepFactor*Math.min(pixelsX, pixelsY));
        cameraWalker.setMinAllowedPixelStep(props.minAllowedPixelStepFactor*Math.min(pixelsX, pixelsY));
        
        int numberOfLines = 4 * (int)Math.ceil(advCal.getDesiredRadialLinesPerTestPattern()/4.0);

        List<CameraWalker> lineWalkers = new ArrayList<>();
        
        for (int i=0; i<numberOfLines; i++) {
            CameraWalker lineWalker = new CameraWalker(movable, (Point p)->findAveragedFiducialPoint(p));
            lineWalker.setLoopGain(advCal.getWalkingLoopGain());
            lineWalker.setSingleStep(true);
            lineWalker.setScalingMat(cameraWalker.getScalingMat());
            lineWalker.setOnlySafeZMovesAllowed(isHeadMountedCamera);
            lineWalker.setSaveCoordinates(testPattern3dPoints, testPatternImagePoints);
            lineWalker.setMaxAllowedPixelStep(props.maxAllowedPixelStepFactor*Math.min(pixelsX, pixelsY));
            lineWalker.setMinAllowedPixelStep(props.minAllowedPixelStepFactor*Math.min(pixelsX, pixelsY));
            
            lineWalkers.add(lineWalker);
        }
        
        swingWorker = new SwingWorker<Void, String>() {
            
            int errorCount = 0;
            
            @Override
            protected Void doInBackground() throws Exception {
                maskDiameter = Math.max(initialMaskDiameter, 3*detectionDiameters.get(calibrationHeightIndex)/2);
                List<Integer> randomIdx = new ArrayList<>();
                double coverageX = pixelsX - detectionDiameters.get(calibrationHeightIndex)*1.2;
                double coverageY = pixelsY - detectionDiameters.get(calibrationHeightIndex)*1.2;
                // For best coverage, make sure the base angle line goes to one image corner. Because the radial lines 
                // are always made to be multiples of 4 this also makes the opposite line go to the corner. 
                double angle0 = Math.atan2((calibrationHeightIndex % 2 == 0 ? coverageY : -coverageY), coverageX);
                for (int iLine=0; iLine<numberOfLines; iLine++) {
                    publish(String.format("Initializing radial line %d of %d at calibration Z coordinate %d of %d", 
                        iLine+1, numberOfLines, 
                        calibrationHeightIndex+1, numberOfCalibrationHeights));
                    
                    randomIdx.add(iLine);
                    double lineAngle = angle0 + iLine * 2 * Math.PI / numberOfLines;
                    double[] unitVector = new double[] {Math.cos(lineAngle), Math.sin(lineAngle)};
                    double scaling = advCal.getTestPatternFillFraction()*
                            Math.min(coverageX / (2*Math.abs(unitVector[0])),
                                    coverageY / (2*Math.abs(unitVector[1])));
                    Location startLocation = centralLocation.derive(null, null, movable.getLocation().getLengthZ().
                            convertToUnits(LengthUnit.Millimeters).getValue(), 
                            movable.getLocation().getRotation()).subtract(new Location(LengthUnit.Millimeters,
                            (3 + Math.random()*5)*advCal.getTrialStep().multiply(unitVector[0]).convertToUnits(LengthUnit.Millimeters).getValue(),
                            (3 + Math.random()*5)*advCal.getTrialStep().multiply(unitVector[1]).convertToUnits(LengthUnit.Millimeters).getValue(),
                            0, 0));
                    try {
                        machine.execute(() -> {
                            movable.moveTo(startLocation);
                            return null;
                        });
                    }
                    catch (Exception ex) {
                        MessageBoxes.errorBox(MainFrame.get(), "Error", ex);
                        cleanUpWhenCancelled();
                    }
                    
                    try {
                        lineWalkers.get(iLine).walkToPoint(centralLocation.
                                derive(null, null, null, movable.getLocation().
                                        convertToUnits(centralLocation.getUnits()).getRotation()), imageCenterPoint, 
                                new Point(scaling*unitVector[0] + pixelsX/2, scaling*unitVector[1] + pixelsY/2));
                    }
                    catch (InterruptedException ie) {
                        Logger.trace(ie);
                        return null;
                    }
                    catch (Exception e) {
                        Logger.trace(e);
                        errorCount++;
                        Logger.trace("errorCount = {}, max allowed = {}", errorCount, props.maxErrorCount);
                        if (errorCount > props.maxErrorCount) {
                            Logger.trace("Exceeded maximum error count - terminating calibration sequence.");
                            return null;
                        }
                    }
                    if (isCancelled()) {
                        return null;
                    }
                }
                errorCount = 0;
                boolean done = false;
                int loopCount = 0;
                while (!done) {
                    Collections.shuffle(randomIdx, new Random(loopCount*numberOfCalibrationHeights + calibrationHeightIndex));
                    done = true;
                    for (int iLine=0; iLine<numberOfLines; iLine++) {
                        publish(String.format("Collecting calibration points along radial line %d of %d, radial step %d, at calibration Z coordinate %d of %d", 
                                iLine+1, numberOfLines,
                                loopCount+1,
                                calibrationHeightIndex+1, numberOfCalibrationHeights));
                        try {
                            if (lineWalkers.get(randomIdx.get(iLine)).step()) {
                                done = false;
                            }
                        }
                        catch (InterruptedException ie) {
                            Logger.trace(ie);
                            return null;
                        }
                        catch (Exception e) {
                            Logger.trace(e);
                            errorCount++;
                            Logger.trace("errorCount = {}, max allowed = {}", errorCount, props.maxErrorCount);
                            if (errorCount > props.maxErrorCount) {
                                Logger.trace("Exceeded maximum error count - terminating calibration sequence.");
                                return null;
                            }
                        }
                        if (isCancelled()) {
                            return null;
                        }
                    }
                    loopCount++;
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
                
                if (errorCount > props.maxErrorCount) {
                    MessageBoxes.errorBox(MainFrame.get(), "Error", "Too many misdetects - retry and verify fiducial/nozzle tip detection." );
                    cleanUpWhenCancelled();
                    return;
                }
                
                cameraView.setCameraViewFilter(null);
                
                calibrationHeightIndex++;
                if (calibrationHeightIndex >= numberOfCalibrationHeights) {
                    mainFrame.showInstructions("Camera Calibration Instructions/Status", 
                            "<html><body>" + "Processing calibration data - this may take a few seconds." + "</body></html>", 
                            true, false,  "Next", cancelActionListener, proceedActionListener);

                    double[][][] testPattern3dPointsArray = 
                            new double[testPattern3dPointsList.size()][][];
                    for (int tpIdx=0; tpIdx<testPattern3dPointsList.size(); tpIdx++) {
                        List<double[]> tp = testPattern3dPointsList.get(tpIdx);
                        testPattern3dPointsArray[tpIdx] = new double[tp.size()][];
                        for (int ptIdx=0; ptIdx<tp.size(); ptIdx++) {
                            double[] pt = tp.get(ptIdx);
                            testPattern3dPointsArray[tpIdx][ptIdx] = new double[] {
                                    mirrored * apparentMotionDirection * pt[0], 
                                    apparentMotionDirection * pt[1], 
                                    calibrationLocations.get(tpIdx).
                                    convertToUnits(LengthUnit.Millimeters).getZ()};
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
        Logger.trace("Entering repeatAction");
        maskDiameter = initialMaskDiameter;
        resultImage = null;
        showCircle(new org.opencv.core.Point(imageCenterPoint.getX(), imageCenterPoint.getY()), 
                (int)(props.centeringDiameterFraction*maskDiameter/2), Color.GREEN);

        testPatternZ = calibrationLocations.get(calibrationHeightIndex).
                convertToUnits(LengthUnit.Millimeters).getZ();
        
        if (isHeadMountedCamera) {
            Location moveLocation = calibrationLocations.get(calibrationHeightIndex).
                    deriveLengths(null, null, camera.getSafeZ(), null);
            movableZ = moveLocation.convertToUnits(LengthUnit.Millimeters).getZ();
            if (Double.isFinite(moveLocation.getX()) && Double.isFinite(moveLocation.getY())) {
                try {
                    machine.execute(() -> {
                        MovableUtils.moveToLocationAtSafeZ(camera, moveLocation, 1.0);
                        return null;
                    });
                }
                catch (Exception ex) {
                    MessageBoxes.errorBox(MainFrame.get(), "Error", ex);
                    cleanUpWhenCancelled();
                }
                // Note, automatic only available when location set. 
                if (automatic) {
                    requestOperatorToAdjustDiameterAction();
                    return true;
                }
            }
            
            setInstructionsAndProceedAction("Using the jog controls on the Machine Controls panel, jog the camera so that the calibration fiducial at Z = %s is approximately centered in the green circle.  Click Next when ready to proceed.", 
                    ()->requestOperatorToAdjustDiameterAction(),
                    calibrationLocations.get(calibrationHeightIndex).getLengthZ().toString()); 
        }
        else { //bottom camera
            movableZ = testPatternZ;
            
            //Move back to the center and change the nozzle tip to the next calibration height
            try {
                machine.execute(() -> {
                    nozzle.moveTo(centralLocation.convertToUnits(LengthUnit.Millimeters).
                            derive(null, null, movableZ, (double)angle));
                    return null;
                });
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
        Logger.trace("Entering requestOperatorToCenterNozzleTipAction");
        maskDiameter = initialMaskDiameter;
        resultImage = null;
        showCircle(new org.opencv.core.Point(imageCenterPoint.getX(), imageCenterPoint.getY()), 
                (int)(props.centeringDiameterFraction*maskDiameter/2), Color.GREEN);
        
        Nozzle selectedNozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
        Location moveLocation = calibrationLocations.get(calibrationHeightIndex).deriveLengths(null, null, selectedNozzle.getSafeZ(), null);
        if (Double.isFinite(moveLocation.getX()) && Double.isFinite(moveLocation.getY())) {
            try {
                machine.execute(() -> {
                    MovableUtils.moveToLocationAtSafeZ(selectedNozzle, moveLocation, 1.0);
                    return null;
                });
            }
            catch (Exception ex) {
                MessageBoxes.errorBox(MainFrame.get(), "Error", ex);
                cleanUpWhenCancelled();
            }
            // Note, automatic only available when location set. 
            if (automatic) {
                captureCentralLocationAction();
                return true;
            }
        }
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
        Logger.trace("Entering captureCentralLocationAction");
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
        Logger.trace("Entering changeNozzleTipHeightAction");
        //Move back to the center and change the nozzle tip to the calibration height
        try {
            machine.execute(() -> {
                nozzle.moveTo(centralLocation.convertToUnits(LengthUnit.Millimeters).
                        derive(null, null, movableZ, (double)angle));
                return null;
            });
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
        Logger.trace("Entering requestOperatorToVerifyNozzleTipIsCenteredAction");
        if (automatic) {
            captureVerifiedCentralLocationAction();
            return true;
        }
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
        Logger.trace("Entering captureVerifiedCentralLocationAction");
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
                Logger.trace("Couldn't find fiducial/nozzle tip in image");
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
                        derive(null, null, null, movable.getLocation().getRotation() + angleIncrement);

                //Wait for the move to complete
                try {
                    machine.execute(() -> {
                        Logger.trace("rotating to angle = " + angle);
                        movable.moveTo(moveLocation);
                        return null;
                    });
                }
                catch (Exception e) {
                    Logger.trace(e);
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
        pipeline.setProperty("MaskCircle.center", expectedPoint);
        pipeline.setProperty("DetectCircularSymmetry.center", expectedPoint);
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
                resultImage = OpenCvUtils.toBufferedImage(pipeline.getWorkingImage());
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
                Color circleColor = Color.YELLOW;
                Color pointColor = Color.GREEN;
                double enclosingDiameter = 2*minDistance + bestKeyPoint.size;
                if (enclosingDiameter > props.maskGrowthThresholdFactor*maskDiameter) {
                    //Turn the point yellow as it is getting too close to the edge of the masked
                    //circle - may want to consider increasing the maskDiameter if this continues
                    //to occur
                    pointColor = Color.YELLOW;
                    changeMaskSize++;
                }
                else if (enclosingDiameter < props.maskShrinkThresholdfactor*maskDiameter) {
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
                        expectedPoint.getY()), -maskDiameter/2, pointColor, circleColor);
                if (changeMaskSize > props.changeMaskThreshold) {
                    maskDiameter *= (1 + props.maskGrowthFactor);
                    pipeline.setProperty("MaskCircle.diameter", maskDiameter);
                    pipeline.setProperty("DetectCircularSymmetry.maxDistance", maskDiameter/2.0);
                    Logger.trace("Increasing mask diameter to " + maskDiameter);
                    changeMaskSize--;
                }
                else if (changeMaskSize < -props.changeMaskThreshold) {
                    maskDiameter *= (1 - props.maskShrinkfactor);
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
        String str = String.format(status, statusArguments);
        Logger.trace(str);
        mainFrame.showInstructions("Camera Calibration Instructions/Status", 
                "<html><body>" + str + "</body></html>", 
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
        String str = String.format(instructions, instructionArguments);
        Logger.trace(str);
        mainFrame.showInstructions("Camera Calibration Instructions/Status", 
                "<html><body>" + str + "</body></html>", 
                true, true,  "Next", cancelActionListener, proceedActionListener);
        this.proceedAction = proceedAction;
    }
    
    /**
     * Process Proceed button clicks
     */
    private final ActionListener proceedActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            Logger.trace("Next button clicked by operator");
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
                    BufferedImage result = ImageUtils.clone(resultImage != null ? resultImage : image);
                    Graphics2D g2D = result.createGraphics();

                    g2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    if (point != null) {
                        double pointSize = Math.min(pixelsX, pixelsY) * 0.05;
                        g2D.setColor(pointColor);
                        g2D.draw(new Line2D.Double(point.x-pointSize, point.y, point.x+pointSize, point.y));
                        g2D.draw(new Line2D.Double(point.x, point.y-pointSize, point.x, point.y+pointSize));
                    }
                    if (center != null) {
                        g2D.setColor(circleColor);
                        if (radius < 0) {
                            g2D.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_BUTT,
                                    BasicStroke.JOIN_MITER,
                                    10.0f, new float[] {(float) (-radius*0.10472)}, 0));
                            g2D.draw(new Ellipse2D.Double(center.x + radius, center.y + radius, -radius*2, -radius*2));
                       }
                        else {
                            g2D.setStroke(new BasicStroke(1.0f));
                            g2D.draw(new Ellipse2D.Double(center.x - radius, center.y - radius, radius*2, radius*2));
                        }
                    }
                    return result;
                }
            });
        }
    }

    public static class CameraCalibrationProcessProperties {
        public double maxAllowedPixelStepFactor = 1/12.0;
        public double minAllowedPixelStepFactor = 1/36.0;
        public int changeMaskThreshold = 3;
        public double maskGrowthThresholdFactor = 0.70;
        public double maskShrinkThresholdfactor = 0.10;
        public double maskGrowthFactor = 0.10;
        public double maskShrinkfactor = 0.05;
        public double initialMaskDiameterFraction = 1/4.0;
        public double centeringDiameterFraction = 0.5;
        public int maxErrorCount = 15;
        public int defaultDetectionDiameter = 25; //pixels
        public double defaultUpLookingSecondaryOffsetZMm = 2;
    }
    
}
