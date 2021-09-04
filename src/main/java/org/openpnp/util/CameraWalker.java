/*
 * Copyright (C) 2021 Tony Luken <tonyluken@att.net>
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;

import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Point;
import org.openpnp.spi.HeadMountable;
import org.pmw.tinylog.Logger;

/**
 * A cameraWalker iteratively moves the machine to drive some detectable feature in a camera image 
 * to a specific point in the image.  This works for both top and bottom cameras and does not 
 * require the camera to be completely setup or calibrated.  All that is required is a rough signed
 * estimate of the units per pixel for the camera and a function that can detect the feature and 
 * returns its coordinates in the camera's image.
 * <pre>
 * The image coordinate system has its origin at the upper left pixel with the positive 
 * X-axis to the right and the positive Y-axis down (as viewed on a computer monitor):
 * 
 * 
 *      +--------> +X               +--------> +X
 *      |                           |
 *      |                           |      o  After jogging position
 *      |  o  Initial position      |
 *      V                           V
 *      +Y                          +Y
 *      
 * The signed units per pixel for the camera is calculated by observing how far a 
 * feature moves in the image when the machine is jogged. For example, suppose the
 * "o" shown above is such a feature and that it moves 25 pixels right (+X direction) 
 * and 20 pixels up (-Y direction) when the machine is jogged 1.5 millimeter in both the 
 * positive X and Y directions.  The signed X units per pixel is then 
 * (+1.5)/(+25) = 0.060 mm/pixel, and the signed Y units per pixel is 
 * (+1.5)/(-20) = -0.075 mm/pixel.
 * </pre>
 * The feature detection function will typically process a CvPipeline to detect the feature. To aid
 * in the detection, the function is passed a point in image coordinates where the feature is 
 * expected to be found and can be used to select the search area and/or mask-off regions of the 
 * image that may cause false detections.  The function should return either the image coordinates
 * of the feature or null if the feature could not be found.
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
 * #setMinAllowedMachineStep setMinAllowedMachineStep} (defaults to 0.005 mm) 
 * methods respectively.
 * <p>
 * By default, a CameraWalker will only make safe Z moves; however, that behavior can be changed
 * by using the {@link #setOnlySafeZMovesAllowed setOnlySafeZMovesAllowed} method.
 */
public class CameraWalker {
    private HeadMountable movable;
    private Double xScaling;
    private Double yScaling;
    private Function<Point, Point> findFeatureInImage;
    private Double loopGain = 0.7;
    private double maxAllowedPixelStep = Double.POSITIVE_INFINITY;
    private double minAllowedPixelStep = 0;
    private Length maxAllowedMachineStep = new Length(Double.POSITIVE_INFINITY, LengthUnit.Millimeters);
    private Length minAllowedMachineStep = new Length(0.005, LengthUnit.Millimeters);
    private boolean onlySafeZMovesAllowed = true;
    private Point lastFoundPoint = null;
    private List<double[]> machine3DCoordinates;
    private List<double[]> image2DCoordinates;
    
    /**
     * 
     * @param movable
     * @param signedUnitsPerPixel
     * @param findFeatureInImage
     */
    public CameraWalker(HeadMountable movable, Location signedUnitsPerPixel, Function<Point, Point> findFeatureInImage) {
        this(movable, signedUnitsPerPixel.getLengthX(), signedUnitsPerPixel.getLengthY(), findFeatureInImage);
    }
    
    /**
     * 
     * @param movable
     * @param signedUnitsPerPixelX
     * @param signedUnitsPerPixelY
     * @param findFeatureInImage
     */
    public CameraWalker(HeadMountable movable, Length signedUnitsPerPixelX, Length signedUnitsPerPixelY, Function<Point, Point> findFeatureInImage) {
        this.movable = movable;
        this.xScaling = signedUnitsPerPixelX.convertToUnits(LengthUnit.Millimeters).getValue();
        this.yScaling = signedUnitsPerPixelY.convertToUnits(LengthUnit.Millimeters).getValue();
        this.findFeatureInImage = findFeatureInImage;
    }
    
    /**
     * 
     * @param startingMachineLocation
     * @param desiredCameraPoint
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public Location walkToPoint(Location startingMachineLocation, Point desiredCameraPoint) throws InterruptedException, ExecutionException {
        return walkToPoint(startingMachineLocation, desiredCameraPoint, desiredCameraPoint);
    }
    
    public Location walkToPoint(Point desiredCameraPoint) throws InterruptedException, ExecutionException {
        return walkToPoint(movable.getLocation(), desiredCameraPoint, desiredCameraPoint);
    }
    
    public Location walkToPoint(Point startingImagePoint, Point desiredCameraPoint) throws InterruptedException, ExecutionException {
        return walkToPoint(movable.getLocation(), startingImagePoint, desiredCameraPoint);
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
    public Location walkToPoint(Location startingMachineLocation, Point startingImagePoint, Point desiredCameraPoint) throws InterruptedException, ExecutionException {
        lastFoundPoint = null;
        boolean savePoints = (machine3DCoordinates != null) && (image2DCoordinates != null);
        
        //Move the machine to the starting location and wait for it to finish
        Future<?> future = UiUtils.submitUiMachineTask(() -> {
            if (onlySafeZMovesAllowed) {
                movable.moveToSafeZ();
                movable.moveTo(startingMachineLocation.derive(movable.getLocation(), false, false, true, false));
            }
            movable.moveTo(startingMachineLocation);
        });
        future.get();

        Location oldLocation = null;
        Location newLocation = startingMachineLocation.convertToUnits(LengthUnit.Millimeters);
        Point expectedPoint = new Point(startingImagePoint.x, startingImagePoint.y);
        Logger.trace("expectedPoint = " + expectedPoint);
        Point foundPoint = findFeatureInImage.apply(expectedPoint);
        Logger.trace("newPoint = " + foundPoint);
        if (foundPoint == null) {
            return null;
        }

        Point error = foundPoint.subtract(desiredCameraPoint);
        double oldErrorMagnitude = Double.POSITIVE_INFINITY;
        double newErrorMagnitude = Math.hypot(error.x, error.y);
        
        while ((newErrorMagnitude < oldErrorMagnitude) && 
                (newErrorMagnitude * loopGain >= minAllowedPixelStep)) {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            oldErrorMagnitude = newErrorMagnitude;
            oldLocation = movable.getLocation().convertToUnits(LengthUnit.Millimeters);
            
            lastFoundPoint = foundPoint;
            if (savePoints) {
                machine3DCoordinates.add(new double[] {oldLocation.getX(), oldLocation.getY(), 
                        oldLocation.getZ()});
                image2DCoordinates.add(new double[] {foundPoint.x, foundPoint.y});
            }
            
            double maxLimitScaling = Math.min(1, 
                    maxAllowedPixelStep/(newErrorMagnitude * loopGain));
            maxLimitScaling = Math.min(maxLimitScaling, 
                    maxAllowedMachineStep.convertToUnits(LengthUnit.Millimeters).getValue()/
                    (Math.hypot(error.x * xScaling, error.y * yScaling) * loopGain));
            error.x *= maxLimitScaling;
            error.y *= maxLimitScaling;

            expectedPoint.x = foundPoint.x - error.x * loopGain; 
            expectedPoint.y = foundPoint.y - error.y * loopGain;
            
            Location correction = new Location(LengthUnit.Millimeters, 
                    -error.x * xScaling * loopGain, 
                    -error.y * yScaling * loopGain, 0, 0);
            
            newLocation = oldLocation.add(correction);
            if (newLocation.getLinearLengthTo(oldLocation).compareTo(minAllowedMachineStep) < 0) {
                //Don't bother moving if the move will be very tiny
                return oldLocation;
            }
            
            //Move the machine and wait for it to finish
            final Location moveLocation = newLocation;
            future = UiUtils.submitUiMachineTask(() -> {
                if (onlySafeZMovesAllowed) {
                    movable.moveToSafeZ();
                    
                    movable.moveTo(moveLocation.derive(movable.getLocation(), false, false, true, false));
                }
                movable.moveTo(moveLocation);
            });
            future.get();

            foundPoint = findFeatureInImage.apply(expectedPoint);
            Logger.trace("expectedPoint = " + expectedPoint);
            Logger.trace("newPoint = " + foundPoint);
            if (foundPoint == null) {
                return null;
            }
            
            error = foundPoint.subtract(desiredCameraPoint);
            newErrorMagnitude = Math.hypot(error.x, error.y);
        }
        return oldLocation;
    }

    /**
     * Gets the HeadMountable that will move during the walk
     * @return the movable
     */
    public HeadMountable getMovable() {
        return movable;
    }

    /**
     * Sets the HeadMountable that will do the moving
     * @param movable - the nozzle or camera that will do the moving
     */
    public void setMovable(HeadMountable movable) {
        this.movable = movable;
    }

    /**
     * Gets the signed units per pixel for the X direction
     * @return the signedUnitsPerPixelX in millimeters per pixel
     */
    public Double getSignedUnitsPerPixelX() {
        return xScaling;
    }

    /**
     * @param signedUnitsPerPixelX the signedUnitsPerPixelX in millimeters per pixel
     */
    public void setSignedUnitsPerPixelX(Double signedUnitsPerPixelX) {
        this.xScaling = signedUnitsPerPixelX;
    }

    /**
     * @return the signedUnitsPerPixelY
     */
    public Double getSignedUnitsPerPixelY() {
        return yScaling;
    }

    /**
     * @param signedUnitsPerPixelY the signedUnitsPerPixelY to set
     */
    public void setSignedUnitsPerPixelY(Double signedUnitsPerPixelY) {
        this.yScaling = signedUnitsPerPixelY;
    }

    /**
     * @return the findFeatureInImage
     */
    public Function<Point, Point> getFindFeatureInImage() {
        return findFeatureInImage;
    }

    /**
     * @param findFeatureInImage the findFeatureInImage to set
     */
    public void setFindFeatureInImage(Function<Point, Point> findFeatureInImage) {
        this.findFeatureInImage = findFeatureInImage;
    }

    /**
     * @return the loopGain
     */
    public Double getLoopGain() {
        return loopGain;
    }

    /**
     * @param loopGain the loopGain to set
     */
    public void setLoopGain(Double loopGain) {
        if (loopGain <= 0) {
            Logger.error("Loop gain must be a positive number");
            return;
        }
        if (loopGain > 1.0) {
            Logger.trace("Loop gain too large, limiting to prevent possible unbounded oscillations.");
            loopGain = 1.0;
        }
        this.loopGain = loopGain;
    }

    /**
     * @return the maxAllowedPixelStep
     */
    public double getMaxAllowedPixelStep() {
        return maxAllowedPixelStep;
    }

    /**
     * @param maxAllowedPixelStep the maxAllowedPixelStep to set
     */
    public void setMaxAllowedPixelStep(double maxAllowedPixelStep) {
        this.maxAllowedPixelStep = maxAllowedPixelStep;
    }

    /**
     * @return the minAllowedPixelStep
     */
    public double getMinAllowedPixelStep() {
        return minAllowedPixelStep;
    }

    /**
     * @param minAllowedPixelStep the minAllowedPixelStep to set
     */
    public void setMinAllowedPixelStep(double minAllowedPixelStep) {
        this.minAllowedPixelStep = minAllowedPixelStep;
    }

    /**
     * @return the maxAllowedMachineStep
     */
    public Length getMaxAllowedMachineStep() {
        return maxAllowedMachineStep;
    }

    /**
     * @param maxAllowedMachineStep the maxAllowedMachineStep to set
     */
    public void setMaxAllowedMachineStep(Length maxAllowedMachineStep) {
        this.maxAllowedMachineStep = maxAllowedMachineStep;
    }

    /**
     * @return the minAllowedMachineStep
     */
    public Length getMinAllowedMachineStep() {
        return minAllowedMachineStep;
    }

    /**
     * @param minAllowedMachineStep the minAllowedMachineStep to set
     */
    public void setMinAllowedMachineStep(Length minAllowedMachineStep) {
        this.minAllowedMachineStep = minAllowedMachineStep;
    }

    /**
     * @return the onlySafeZMovesAllowed
     */
    public boolean isOnlySafeZMovesAllowed() {
        return onlySafeZMovesAllowed;
    }

    /**
     * @param onlySafeZMovesAllowed the onlySafeZMovesAllowed to set
     */
    public void setOnlySafeZMovesAllowed(boolean onlySafeZMovesAllowed) {
        this.onlySafeZMovesAllowed = onlySafeZMovesAllowed;
    }

    /**
     * @return the lastFoundPoint
     */
    public Point getLastFoundPoint() {
        return lastFoundPoint;
    }

    /**
     * @return the machine3DCoordinates
     */
    public List<double[]> getMachine3DCoordinates() {
        return machine3DCoordinates;
    }

    /**
     * @return the image2DCoordinates
     */
    public List<double[]> getImage2DCoordinates() {
        return image2DCoordinates;
    }

    /**
     * 
     * @param machine3dCoordinates
     * @param image2dCoordinates
     */
    public void setSaveCoordinates(List<double[]> machine3dCoordinates, List<double[]> image2dCoordinates) {
        machine3DCoordinates = machine3dCoordinates;
        image2DCoordinates = image2dCoordinates;
    }

}
