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

import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Point;
import org.openpnp.spi.HeadMountable;
import org.pmw.tinylog.Logger;

/**
 * A CameraWalker iteratively moves the machine to drive some detectable feature in a camera image 
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
    private Length maxAllowedMachineStep = new Length(Double.POSITIVE_INFINITY, 
            LengthUnit.Millimeters);
    private Length minAllowedMachineStep = new Length(0.005, LengthUnit.Millimeters);
    private boolean onlySafeZMovesAllowed = true;
    private Point lastFoundPoint = null;
    private List<double[]> machine3DCoordinates;
    private List<double[]> image2DCoordinates;
    
    /**
     * Constructs a new CameraWalker with the given parameters
     * 
     * @param movable - the Camera or Nozzle that will be moved during the walk
     * @param signedUnitsPerPixel - the signed units per pixel
     * @param findFeatureInImage - the feature detection function
     */
    public CameraWalker(HeadMountable movable, Location signedUnitsPerPixel, 
            Function<Point, Point> findFeatureInImage) {
        this(movable, signedUnitsPerPixel.getLengthX(), signedUnitsPerPixel.getLengthY(), 
                findFeatureInImage);
    }
    
    /**
     * Constructs a new CameraWalker with the given parameters
     * 
     * @param movable - the Camera or Nozzle that will be moved during the walk
     * @param signedUnitsPerPixelX - the signed units per pixel in the X direction
     * @param signedUnitsPerPixelY - the signed units per pixel in the Y direction
     * @param findFeatureInImage - the feature detection function
     */
    public CameraWalker(HeadMountable movable, Length signedUnitsPerPixelX, 
            Length signedUnitsPerPixelY, Function<Point, Point> findFeatureInImage) {
        this.movable = movable;
        this.xScaling = signedUnitsPerPixelX.convertToUnits(LengthUnit.Millimeters).getValue();
        this.yScaling = signedUnitsPerPixelY.convertToUnits(LengthUnit.Millimeters).getValue();
        this.findFeatureInImage = findFeatureInImage;
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
     */
    public Location walkToPoint(Location startingMachineLocation, Point desiredImagePoint) {
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
     */
    public Location walkToPoint(Point desiredImagePoint) {
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
     */
    public Location walkToPoint(Point startingImagePoint, Point desiredImagePoint) {
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
     */
    public Location walkToPoint(Location startingMachineLocation, Point startingImagePoint, 
            Point desiredCameraPoint) {
        lastFoundPoint = null;
        boolean savePoints = (machine3DCoordinates != null) && (image2DCoordinates != null);
        
        //Move the machine to the starting location and wait for it to finish
        Future<?> future = UiUtils.submitUiMachineTask(() -> {
            if (onlySafeZMovesAllowed) {
                movable.moveToSafeZ();
                movable.moveTo(startingMachineLocation.
                        derive(movable.getLocation(), false, false, true, false));
            }
            movable.moveTo(startingMachineLocation);
        });
        try {
            future.get();
        }
        catch (Exception e) {
            return null;
        }

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
                    
                    movable.moveTo(moveLocation.
                            derive(movable.getLocation(), false, false, true, false));
                }
                movable.moveTo(moveLocation);
            });
            try {
                future.get();
            }
            catch (Exception e) {
                return null;
            }

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
     * Gets the signed units per pixel for the X direction in millimeters per pixel
     */
    public Double getSignedUnitsPerPixelX() {
        return xScaling;
    }

    /**
     * Sets the signed units per pixel for the X direction in millimeters per pixel
     */
    public void setSignedUnitsPerPixelX(Double signedUnitsPerPixelX) {
        this.xScaling = signedUnitsPerPixelX;
    }

    /**
     * Gets the signed units per pixel for the Y direction in millimeters per pixel
     */
    public Double getSignedUnitsPerPixelY() {
        return yScaling;
    }

    /**
     * Sets the signed units per pixel for the Y direction in millimeters per pixel
     */
    public void setSignedUnitsPerPixelY(Double signedUnitsPerPixelY) {
        this.yScaling = signedUnitsPerPixelY;
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
     * to 1 (use caution as very small loop gains may cause walks to take a very long time to 
     * complete).
     */
    public void setLoopGain(Double loopGain) {
        if (loopGain <= 0) {
            Logger.error("Loop gain must be a positive number");
            return;
        }
        if (loopGain > 1.0) {
            Logger.error("Loop gain too large, limiting to 1.0 to prevent possible unbounded machine oscillations.");
            loopGain = 1.0;
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
     * Gets the image coordinates of the detected feature at the end of the walk
     */
    public Point getLastFoundPoint() {
        return lastFoundPoint;
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

}
