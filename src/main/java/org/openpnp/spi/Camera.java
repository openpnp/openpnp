/*
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

package org.openpnp.spi;

import java.awt.image.BufferedImage;
import java.io.Closeable;

import org.openpnp.CameraListener;
import org.openpnp.model.Length;
import org.openpnp.model.Location;

/**
 * Represents a Camera attached to the system and allows a caller to retrieve images from it.
 */
public interface Camera extends HeadMountable, WizardConfigurable,
        PropertySheetHolder, Closeable {
    public enum Looking {
        Down, Up
    }

    /**
     * Get the location of the camera including the calibrated offset for the given tool.   
     * If the bottom camera focal plane is different from the PCB surface plane, the various
     * tools might introduce slight offsets in X, Y as their Z axes are not perfectly parallel.
     * This offset is compensated if the getLocation(tool) method is used instead of the plain
     * getLocation() method. 
     * If tool == null it returns the same as plain getLocation().
     *  
     * @param tool
     * @return
     */
    public Location getLocation(HeadMountable tool);

    /**
     * Get the direction the Camera is looking.
     * 
     * @return
     */
    public Looking getLooking();

    public void setLooking(Looking looking);

    /**
     * The number of X and Y units per pixel this camera shows when in perfect focus. The Z value of
     * this location is the height above the camera at which the units per pixel were measured.
     * 
     * @return a Location whose x and y length represent the units per pixel in those axis
     * respectively
     */
    public Location getUnitsPerPixel();

    public void setUnitsPerPixel(Location unitsPerPixel);
    
    /**
     * Gets the units per pixel for determining the physical size of an object in an image given
     * its Z height is known
     * 
     * @param z - a length with the z coordinate of the imaged object, if null, the height of the 
     * default working plane for this camera is used
     * @return a Location whose x and y length represent the units per pixel in those axis
     * respectively
     */
    public Location getUnitsPerPixel(Length z);
    
    /**
     * Gets the units per pixel for determining the physical size of an object in an image given
     * its Z height is known
     * 
     * @param location - a location with the z coordinate of the imaged object, if null, the height
     * of the default working plane for this camera is used
     * @return a Location whose x and y length represent the units per pixel in those axis
     * respectively
     */
    public Location getUnitsPerPixel(Location location);

    /**
     * Gets the secondary units per pixel
     * 
     * @return a location whose x and y coordinates are the measured pixels per unit for those axis
     *         respectively and the z coordinate is the height above the camera at which the
     *         measurements were made.
     */
    public Location getUnitsPerPixelSecondary();
    
    /**
     * Sets the secondary units per pixel
     * 
     * @param unitsPerPixelSecondary - a location whose x and y coordinates are the measured pixels
     * per unit for those axis respectively and the z coordinate is the height above the camera at
     * which the measurements were made.
     */
    public void setUnitsPerPixelSecondary(Location unitsPerPixelSecondary);

    /**
     * Gets the Z  height of the default working plane for this camera.  This is the height
     * at which objects are assumed to be if no other information is available.
     * 
     * @return the Z height of the default working plane
     */
    public Length getDefaultZ();

    /**
     * Sets the Z height of the default working plane for this camera.  This is the height
     * at which objects are assumed to be if no other information is available.
     * 
     * @param defaultZ - the Z height of the default working plane
     */
    public void setDefaultZ(Length defaultZ);
    
    /**
     * Estimates the Z height of an object based upon the observed units per pixel for the
     * object. This is typically found by capturing images of a feature of the object from two
     * different camera positions. The observed units per pixel is then computed by dividing the
     * actual change in camera position (in machine units) by the apparent change in position of the
     * feature (in pixels) between the two images.
     *
     * @param observedUnitsPerPixel - the observed units per pixel for the object
     * @return - the estimated Z height of the object
     */
    public Length estimateZCoordinateOfObject(Location observedUnitsPerPixel) throws Exception;

    /**
     * Immediately captures an image from the camera and returns it in it's native format. Fires
     * the Camera.BeforeCapture and Camera.AfterCapture scripting events before and after.
     * @return
     */
    public BufferedImage capture();
    
    public BufferedImage captureForPreview();
    
    public BufferedImage captureRaw();
    
    /**
     * Same as capture(), but waits the settle time before capturing.
     * 
     * @return
     * @throws Exception 
     */
    public BufferedImage settleAndCapture() throws Exception;

    /**
     * Registers a listener to receive continuous images from the camera.
     * 
     * @param listener
     */
    public void startContinuousCapture(CameraListener listener);

    /**
     * Requests that the continuous capture be stopped for the previously registered listener. If
     * the Camera has other listeners they should still receive updates.
     * 
     * @param listener
     */
    public void stopContinuousCapture(CameraListener listener);

    public void setVisionProvider(VisionProvider visionProvider);

    /**
     * Get the VisionProvider that is attached to this Camera, if any.
     * 
     * @return
     */
    public VisionProvider getVisionProvider();

    /**
     * Get the width of images in pixels that will be returned from this Camera.
     * 
     * @return
     */
    public int getWidth();

    /**
     * Get the height of images in pixels that will be returned from this Camera.
     * 
     * @return
     */
    public int getHeight();
}
