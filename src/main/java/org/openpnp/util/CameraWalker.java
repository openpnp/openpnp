/*
 * Copyright (C) 2021 Tony Luken <tonyluken62+openpnp@gmail.com>
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

package org.openpnp.util;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Function;

import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Point;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.pmw.tinylog.Logger;

/**
 * A CameraWalker iteratively moves the machine to drive some detectable feature in a camera image 
 * to a specific point in the image.  This works for both top and bottom cameras and does not 
 * require the camera to be completely setup or calibrated, i.e., settings for rotation, flips, 
 * units per pixel, and lens distortion are all irrelevant to its operation. However, it does need 
 * a function to detect the feature in the image.
 * <p>
 * The feature detection function will typically process a CvPipeline to detect the feature. It is 
 * important that the stages used in the pipeline have no dependencies on any of the aforementioned
 * uncalibrated settings. To aid in the detection, the function is passed a point in image 
 * coordinates where the feature is expected to be found and can be used to select the search area 
 * and/or mask-off regions of the image that may cause false detections.  The function should return
 * either the image coordinates of the feature if it is found or null if the feature could not be 
 * found.
 * <p>
 * The size of each step a CameraWalker takes is normally determined by the distance between the 
 * detected feature image coordinates and its desired image coordinates. That distance is scaled by 
 * the loopGain (defaults to 0.7 but can be changed using the {@link #setLoopGain setLoopGain} 
 * method) to determine the step size. The maximum size of the steps a CameraWalker takes can be 
 * further limited by using the {@link #setMaxAllowedPixelStep setMaxAllowedPixelStep} and/or 
 * {@link #setMaxAllowedMachineStep setMaxAllowedMachineStep} methods.
 * <p>
 * The walk is considered done when the distance between the detected feature image coordinates and 
 * its desired image coordinates no longer decreases. The walk can also be completed if the detected 
 * feature is within a set distance (in pixels) of the desired image coordinates or if the machine 
 * step is below a set distance (in machine units). These can be set with the 
 * {@link #setMinAllowedPixelStep setMinAllowedPixelStep} (defaults to 0) and {@link 
 * #setMinAllowedMachineStep setMinAllowedMachineStep} (defaults to 0.0001 mm) 
 * methods respectively.
 * <p>
 * By default, a CameraWalker will only make safe Z moves; however, that behavior can be changed
 * by using the {@link #setOnlySafeZMovesAllowed setOnlySafeZMovesAllowed} method.
 */
public class CameraWalker {
    private HeadMountable movable;
    private RealMatrix scalingMat;
    private double mirror = 1;
    private double estimatedMillimetersPerPixel;
    private Function<Point, Point> findFeatureInImage;
    private Double loopGain = 0.7;
    private double maxAllowedPixelStep = Double.POSITIVE_INFINITY;
    private double minAllowedPixelStep = 0;
    private Length maxAllowedMachineStep = new Length(Double.POSITIVE_INFINITY, 
            LengthUnit.Millimeters);
    private Length minAllowedMachineStep = new Length(0.0001, LengthUnit.Millimeters);
    private boolean onlySafeZMovesAllowed = true;
    private Point lastFoundPoint = null;
    private List<double[]> machine3DCoordinates;
    private List<double[]> image2DCoordinates;
    private boolean singleStep = false;
    private boolean walkComplete = true;
    private Point desiredCameraPoint;
    private boolean savePoints;
    private Location oldLocation;
    private Location newLocation;
    private Location savedLocation;
    private Point expectedPoint;
    private Point foundPoint;
    private Point error;
    private double oldErrorMagnitude;
    private double newErrorMagnitude;
    private Machine machine;


    
    /**
     * Constructs a new CameraWalker with the given parameters
     * 
     * @param movable - the Camera or Nozzle that will be moved during the walk
     * @param signedUnitsPerPixelX - the signed units per pixel in the X direction
     * @param signedUnitsPerPixelY - the signed units per pixel in the Y direction
     * @param findFeatureInImage - the feature detection function
     */
    public CameraWalker(HeadMountable movable, Function<Point, Point> findFeatureInImage) {
        this.movable = movable;
        this.findFeatureInImage = findFeatureInImage;
        scalingMat = null;
        machine = Configuration.get().getMachine();
    }
    
    /**
     * Estimates the machine units to pixel scaling by stepping the machine and observing how far 
     * the feature moves in the image. The machine needs to be jogged such that the feature is 
     * visible in the image and close to the expectedPoint prior to calling this method. This method
     * must be called prior to beginning a walk.
     *  
     * @param testStepLength - the distance the machine will step from its current location. This 
     * should be kept relatively small.
     * @param expectedPoint - the expected point where the feature is expected to be detected in 
     * the image
     * @return the 2x2 scaling matrix that converts changes in image pixel locations to changes in 
     * machine coordinates
     * @throws Exception if the scaling can't be estimated
     */
    public RealMatrix estimateScaling(Length testStepLength, Point expectedPoint) throws Exception {
        cancelWalk();
        testStepLength = testStepLength.convertToUnits(LengthUnit.Millimeters);
        Location start = movable.getLocation();
        
        RealMatrix mMat = MatrixUtils.createRealMatrix(2,2);
        RealMatrix pMat = MatrixUtils.createRealMatrix(2,2);
        
        for (int i=0; i<4; i++) {
            double s = testStepLength.getValue();
            double x = (i==0) || (i==3) ? s : -s;
            double y = (i==0) || (i==2) ? s : -s;
            Location step = new Location(LengthUnit.Millimeters, x, y, 0, 0);
            final Location testLocation = start.add(step).derive(null, null, null, movable.getLocation().getRotation());
            
            //Move the machine to the test location and wait for it to finish
            machine.execute(() -> {
                if (onlySafeZMovesAllowed) {
                    MovableUtils.moveToLocationAtSafeZ(movable, testLocation, 1.0);
                }
                else {
                    movable.moveTo(testLocation);
                }
                return null;
            });
            
            Point foundPoint = findFeatureInImage.apply(expectedPoint);
            if (foundPoint == null) {
                throw new Exception("Unable to estimate machine to pixel scaling - can't find feature in image.");
            }
            
            switch (i%2) {
                case 0:
                    mMat.setEntry(0, i/2, step.getX());
                    mMat.setEntry(1, i/2, step.getY());
                    pMat.setEntry(0, i/2, foundPoint.x);
                    pMat.setEntry(1, i/2, foundPoint.y);
                    break;
                case 1:
                    mMat.setEntry(0, i/2, step.getX() - mMat.getEntry(0, i/2));
                    mMat.setEntry(1, i/2, step.getY() - mMat.getEntry(1, i/2));
                    pMat.setEntry(0, i/2, foundPoint.x - pMat.getEntry(0, i/2));
                    pMat.setEntry(1, i/2, foundPoint.y - pMat.getEntry(1, i/2));
                    break;
            }
        }
        
        scalingMat = mMat.multiply(new SingularValueDecomposition(pMat).getSolver().getInverse());
        Logger.trace("scalingMat = " + scalingMat);
        
        RealMatrix s = new SingularValueDecomposition(scalingMat).getS();
        Logger.trace("s = " + s);
        estimatedMillimetersPerPixel = Math.sqrt(s.getEntry(0, 0) * s.getEntry(1, 1));
        Logger.trace("estimatedMillimetersPerPixel = " + estimatedMillimetersPerPixel);
        
        if (estimatedMillimetersPerPixel <= 0) {
            throw new Exception("Unable to estimate machine to pixel scaling - testStepLength may be too small.");
        }
        
        mirror  = Math.signum((new LUDecomposition(scalingMat)).getDeterminant());
        
        return scalingMat;
    }
    
    /**
     * Returns true if the CameraWalker is ready to walk.
     */
    public boolean isReadyToWalk() {
        return scalingMat != null && Double.isFinite(scalingMat.getFrobeniusNorm()) && walkComplete;
    }
    
    /**
     * Iteratively moves the machine in X and Y until the detected feature appears at the desired 
     * image coordinates (or until it is close enough depending on the settings of 
     * minAllowedPixelStep and minAllowedMachineStep).
     * 
     * @param startingMachineLocation - the machine is moved to this location before beginning the 
     * walk
     * @param desiredImagePoint - the desired image coordinates of the feature when the walk is 
     * completed and is also the expected coordinates of the feature when the machine is at 
     * the starting location
     * @return the location of the machine at the completion of the walk or null if the walk failed 
     * due to the feature not being found or the task being interrupted. The image coordinates of 
     * the feature corresponding to this location can be found with {@link #getLastFoundPoint 
     * getLastFoundPoint}
     * @throws Exception 
     */
    public Location walkToPoint(Location startingMachineLocation, Point desiredImagePoint) throws Exception {
        return walkToPoint(startingMachineLocation, desiredImagePoint, desiredImagePoint);
    }
    
    /**
     * Iteratively moves the machine in X and Y from its current location until the detected feature 
     * appears at the desired image coordinates (or until it is close enough depending on the 
     * settings of minAllowedPixelStep and minAllowedMachineStep).
     * 
     * @param desiredImagePoint - the desired image coordinates of the feature when the walk is 
     * completed and is also the expected coordinates of the feature with the machine at its
     * current location
     * @return the location of the machine at the completion of the walk or null if the walk failed 
     * due to the feature not being found or the task being interrupted. The image coordinates of 
     * the feature corresponding to this location can be found with {@link #getLastFoundPoint 
     * getLastFoundPoint}
     * @throws Exception 
     */
    public Location walkToPoint(Point desiredImagePoint) throws Exception {
        return walkToPoint(movable.getLocation(), desiredImagePoint, desiredImagePoint);
    }
    
    /**
     * Iteratively moves the machine in X and Y from its current location until the detected feature 
     * appears at the desired image coordinates (or until it is close enough depending on the 
     * settings of minAllowedPixelStep and minAllowedMachineStep).
     * 
     * @param startingImagePoint - the expected image coordinates of the feature with the 
     * machine at its current location
     * @param desiredImagePoint - the desired image coordinates of the feature when the walk is 
     * completed
     * @return the location of the machine at the completion of the walk or null if the walk failed 
     * due to the feature not being found or the task being interrupted. The image coordinates of 
     * the feature corresponding to this location can be found with {@link #getLastFoundPoint 
     * getLastFoundPoint}
     * @throws Exception 
     */
    public Location walkToPoint(Point startingImagePoint, Point desiredImagePoint) throws Exception {
        return walkToPoint(movable.getLocation(), startingImagePoint, desiredImagePoint);
    }
    
    /**
     * Iteratively moves the machine in X and Y until the detected feature appears at the desired 
     * image coordinates (or until it is close enough depending on the settings of 
     * minAllowedPixelStep and minAllowedMachineStep).
     * 
     * @param startingMachineLocation - the machine is moved to this location before beginning the 
     * walk
     * @param startingImagePoint - the expected image coordinates of the feature when the 
     * machine is at the starting location
     * @param desiredCameraPoint - the desired image coordinates of the feature when the walk is 
     * completed
     * @return the location of the machine at the completion of the walk or null if the walk failed 
     * due to the feature not being found or the task being interrupted. The image coordinates of 
     * the feature corresponding to this location can be found with {@link #getLastFoundPoint 
     * getLastFoundPoint}
     * @throws Exception if the walk can't be started and/or completed
     */
    public Location walkToPoint(Location startingMachineLocation, Point startingImagePoint, 
            Point desiredCameraPoint) throws Exception {
        this.desiredCameraPoint = desiredCameraPoint;
        lastFoundPoint = null;
        if (!isReadyToWalk()) {
            throw new Exception("CameraWalker not ready to walk, must call estimateScaling method first.");
        }
        walkComplete = false;
        
        savePoints = (machine3DCoordinates != null) && (image2DCoordinates != null);
        
        //Move the machine to the starting location and wait for it to finish
        machine.execute(() -> {
            if (onlySafeZMovesAllowed) {
                MovableUtils.moveToLocationAtSafeZ(movable, startingMachineLocation, 1.0);
            }
            else {
                movable.moveTo(startingMachineLocation);
            }
            return null;
        });
        savedLocation = movable.getLocation().convertToUnits(LengthUnit.Millimeters);

        oldLocation = startingMachineLocation.convertToUnits(LengthUnit.Millimeters);
        newLocation = oldLocation;
        expectedPoint = new Point(startingImagePoint.x, startingImagePoint.y);
        Logger.trace("expectedPoint = " + expectedPoint);
        foundPoint = findFeatureInImage.apply(expectedPoint);
        Logger.trace("foundPoint = " + foundPoint);
        if (foundPoint == null) {
            throw new Exception("Unable to start walk - can't find feature in image.");
        }

        error = foundPoint.subtract(desiredCameraPoint);
        oldErrorMagnitude = Double.POSITIVE_INFINITY;
        newErrorMagnitude = Math.hypot(error.x, error.y);
        
        if (isSingleStep()) {
            return oldLocation;
        }
        
        while (step()) {
            //keep stepping until done
        }
        
        return oldLocation;
    }

    public boolean step() throws Exception {
        if (walkComplete) {
            return false;
        }
        oldErrorMagnitude = newErrorMagnitude;
        oldLocation = savedLocation.convertToUnits(LengthUnit.Millimeters);
        
        lastFoundPoint = foundPoint;
        if (savePoints) {
            machine3DCoordinates.add(new double[] {oldLocation.getX(), oldLocation.getY(), 
                    oldLocation.getZ()});
            image2DCoordinates.add(new double[] {foundPoint.x, foundPoint.y});
        }
        
        RealMatrix errorMat = MatrixUtils.createColumnRealMatrix(new double[] {error.x, error.y});
        RealMatrix machineErrorMat = scalingMat.multiply(errorMat);
        
        double maxLimitScaling = Math.min(1, 
                maxAllowedPixelStep/(newErrorMagnitude * loopGain));
        maxLimitScaling = Math.min(maxLimitScaling, 
                maxAllowedMachineStep.convertToUnits(LengthUnit.Millimeters).getValue()/
                (machineErrorMat.getFrobeniusNorm() * loopGain));
        error.x *= maxLimitScaling;
        error.y *= maxLimitScaling;
        
        errorMat = errorMat.scalarMultiply(maxLimitScaling);
        machineErrorMat = machineErrorMat.scalarMultiply(maxLimitScaling);
        
        expectedPoint.x = foundPoint.x - error.x * loopGain; 
        expectedPoint.y = foundPoint.y - error.y * loopGain;
        
        Location correction = new Location(LengthUnit.Millimeters, 
                -machineErrorMat.getEntry(0, 0) * loopGain, 
                -machineErrorMat.getEntry(1, 0) * loopGain, 0, 0);
        
        newLocation = oldLocation.add(correction);
        if (newLocation.getLinearLengthTo(oldLocation).compareTo(minAllowedMachineStep) < 0) {
            //Don't bother moving if the move will be very tiny
            walkComplete = true;
            return false;
        }
        
        //Move the machine and wait for it to finish
        final Location moveLocation = newLocation.derive(null, null, null, movable.getLocation().getRotation());
        machine.execute(() -> {
            if (onlySafeZMovesAllowed) {
                MovableUtils.moveToLocationAtSafeZ(movable, moveLocation, 1.0);
            }
            else {
                movable.moveTo(moveLocation);
            }
            return null;
        });
        
        savedLocation = movable.getLocation().convertToUnits(LengthUnit.Millimeters);

        foundPoint = findFeatureInImage.apply(expectedPoint);
        Logger.trace("expectedPoint = " + expectedPoint);
        Logger.trace("newPoint = " + foundPoint);
        if (foundPoint == null) {
            walkComplete = true;
            Logger.trace("Unable to complete walk - can't find feature in image.");
            throw new Exception("Unable to complete walk - can't find feature in image.");
        }
        
        error = foundPoint.subtract(desiredCameraPoint);
        newErrorMagnitude = Math.hypot(error.x, error.y);
        
        if ((newErrorMagnitude >= oldErrorMagnitude) || 
                (newErrorMagnitude < minAllowedPixelStep)) {
            walkComplete = true;
        }
        
        return !walkComplete;
    }
    
    public void cancelWalk() {
        walkComplete = true;
    }
    
    public boolean isWalkComplete() {
        return walkComplete;
    }
    
    /**
     * Gets an estimate of a change in machine location for a given change in pixels
     * 
     * @param deltaPoint - the change in pixels
     * @return the change in machine location
     */
    public Location getDeltaLocation(Point deltaPoint) {
        RealMatrix deltaPointMat = MatrixUtils.createColumnRealMatrix(new double[] {deltaPoint.x, deltaPoint.y});
        RealMatrix deltaLocationMat = scalingMat.multiply(deltaPointMat);
        return new Location(LengthUnit.Millimeters, deltaLocationMat.getEntry(0, 0), deltaLocationMat.getEntry(1, 0), 0, 0);
    }
    
    /**
     * Gets the Camera or Nozzle that will do the moving during the walk
     */
    public HeadMountable getMovable() {
        return movable;
    }

    /**
     * Sets the Camera or Nozzle that will do the moving during the walk
     */
    public void setMovable(HeadMountable movable) {
        this.movable = movable;
    }

    /**
     * Gets the scaling matrix
     */
    public RealMatrix getScalingMat() {
        return scalingMat;
    }

    /**
     * @param scalingMat the scalingMat to set
     */
    public void setScalingMat(RealMatrix scalingMat) {
        this.scalingMat = scalingMat;
    }

    /**
     * @return the mirror
     */
    public double getMirror() {
        return mirror;
    }

    /**
     * @param mirror the mirror to set
     */
    public void setMirror(double mirror) {
        this.mirror = mirror;
    }

    /**
     * Gets the estimated millimeters per pixel
     */
    public double getEstimatedMillimetersPerPixel() {
        return estimatedMillimetersPerPixel;
    }

    /**
     * Gets the feature detection function
     */
    public Function<Point, Point> getFindFeatureInImage() {
        return findFeatureInImage;
    }

    /**
     * Sets the feature detection function
     */
    public void setFindFeatureInImage(Function<Point, Point> findFeatureInImage) {
        this.findFeatureInImage = findFeatureInImage;
    }

    /**
     * Gets the loop gain to be used during the walk
     */
    public Double getLoopGain() {
        return loopGain;
    }

    /**
     * Sets the loop gain to be used during the walk, must be greater than 0 and less than or equal 
     * to 1.7 (use caution as very small loop gains may cause walks to take a very long time to 
     * complete and too large may cause unbounded machine oscillations).
     */
    public void setLoopGain(Double loopGain) {
        if (loopGain <= 0) {
            Logger.error("Loop gain must be a positive number");
            return;
        }
        if (loopGain > 1.7) {
            Logger.error("Loop gain too large, limiting to 1.7 to prevent possible unbounded machine oscillations.");
            loopGain = 1.7;
        }
        this.loopGain = loopGain;
    }

    /**
     * Gets the maximum allowable step in pixels that the walk can take
     */
    public double getMaxAllowedPixelStep() {
        return maxAllowedPixelStep;
    }

    /**
     * Sets the maximum allowable step in pixels that the walk can take.  Must be greater than 0 
     * (use caution as small values may result in walks that take a very long time to complete).
     */
    public void setMaxAllowedPixelStep(double maxAllowedPixelStep) {
        if (maxAllowedPixelStep <= 0) {
            Logger.error("Maximum allowable step in pixels must be greater than 0");
            return;
        }
        this.maxAllowedPixelStep = maxAllowedPixelStep;
    }

    /**
     * Gets the minimum allowable step in pixels that the walk can take.
     */
    public double getMinAllowedPixelStep() {
        return minAllowedPixelStep;
    }

    /**
     * Sets the minimum allowable step in pixels that the walk can take. Must be non-negative.
     */
    public void setMinAllowedPixelStep(double minAllowedPixelStep) {
        if (minAllowedPixelStep <= 0) {
            Logger.error("Minimum allowable step in pixels must be non-negative");
            return;
        }
        this.minAllowedPixelStep = minAllowedPixelStep;
    }

    /**
     * Gets the maximum allowable step in machine units that the walk can take
     */
    public Length getMaxAllowedMachineStep() {
        return maxAllowedMachineStep;
    }

    /**
     * Sets the maximum allowable step in machine units that the walk can take. Must be greater 
     * than 0 (use caution as small values may result in walks that take a very long time to 
     * complete).
     */
    public void setMaxAllowedMachineStep(Length maxAllowedMachineStep) {
        if (maxAllowedMachineStep.getValue() <= 0) {
            Logger.error("Maximum allowable step in machine units must be greater than 0");
            return;
        }
        this.maxAllowedMachineStep = maxAllowedMachineStep;
    }

    /**
     * Gets the minimum allowable step in machine units that the walk can take.
     */
    public Length getMinAllowedMachineStep() {
        return minAllowedMachineStep;
    }

    /**
     * Sets the minimum allowable step in machine units that the walk can take. Must be 
     * greater than 0 (use caution as very small values can result in a long stream of very tiny 
     * moves being generated which can result in controller buffer overflows).
     */
    public void setMinAllowedMachineStep(Length minAllowedMachineStep) {
        if (minAllowedMachineStep.getValue() <= 0) {
            Logger.error("Minimum allowable step in machine units must be greater than 0");
            return;
        }
        this.minAllowedMachineStep = minAllowedMachineStep;
    }

    /**
     * Checks to see if only safe Z moves are allowed
     * 
     * @return true if only safe Z moves are allowed, false otherwise
     */
    public boolean isOnlySafeZMovesAllowed() {
        return onlySafeZMovesAllowed;
    }

    /**
     * Sets the only safe Z moves are allowed flag to the given state
     * 
     * @param onlySafeZMovesAllowed - the state to set
     */
    public void setOnlySafeZMovesAllowed(boolean onlySafeZMovesAllowed) {
        this.onlySafeZMovesAllowed = onlySafeZMovesAllowed;
    }

    /**
     * Gets the image coordinates of the detected feature at the end of the last walk
     */
    public Point getLastFoundPoint() {
        return lastFoundPoint;
    }

    /**
     * Gets the machine coordinates at the end of the last walk
     */
    public Location getLastFoundLocation() {
        return oldLocation;
    }

    /**
     * Gets the list to which the CameraWalker adds machine coordinates as the walk takes place. 
     * Returns null if {@link #setSaveCoordinates setSaveCoordinates} was not used to set a list to 
     * save the coordinates.
     */
    public List<double[]> getMachine3DCoordinates() {
        return machine3DCoordinates;
    }

    /**
     * Gets the list to which the CameraWalker adds image coordinates as the walk takes place. 
     * Returns null if {@link #setSaveCoordinates setSaveCoordinates} was not used to set a list to 
     * save the coordinates.
     */
    public List<double[]> getImage2DCoordinates() {
        return image2DCoordinates;
    }

    /**
     * The CameraWalker will add (save) the coordinates visited during the walk to the lists 
     * provided here. Note that both parameters must be non-null in order for the CameraWalker to 
     * save the coordinates.
     * 
     * @param machine3dCoordinates - the list to which the machine coordinates are added
     * @param image2dCoordinates - the list to which the image coordinates are added
     */
    public void setSaveCoordinates(List<double[]> machine3dCoordinates, 
            List<double[]> image2dCoordinates) {
        machine3DCoordinates = machine3dCoordinates;
        image2DCoordinates = image2dCoordinates;
    }

    /**
     * @return the singleStep
     */
    public boolean isSingleStep() {
        return singleStep;
    }

    /**
     * @param singleStep the singleStep to set
     */
    public void setSingleStep(boolean singleStep) {
        this.singleStep = singleStep;
    }

}
