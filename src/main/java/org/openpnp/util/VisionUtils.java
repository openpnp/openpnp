package org.openpnp.util;

import org.openpnp.model.Location;
import org.openpnp.spi.Camera;

public class VisionUtils {
    /**
     * Given pixel coordinates within the frame of the Camera's image, get
     * the offsets from Camera center to the coordinates in Camera space
     * and units. The resulting value is the distance the Camera
     * can be moved to be centered over the pixel coordinates.
     * 
     * Example: If the x, y coordinates describe a position above and to the
     * left of the center of the camera the offsets will be -,+.
     *  
     * If the coordinates position are below and to the right of center the
     * offsets will be +, -.
     * 
     * Calling camera.getLocation().add(getPixelCenterOffsets(...) will give
     * you the location of x, y with respect to the center of the camera. 
     * @param camera
     * @param x
     * @param y
     * @return
     */
    public static Location getPixelCenterOffsets(Camera camera, double x, double y) {
        double imageWidth = camera.getWidth();
        double imageHeight = camera.getHeight();

        // Calculate the difference between the center of the image to the
        // center of the match.
        double offsetX = x - (imageWidth / 2);
        double offsetY = (imageHeight / 2) - y;

        // And convert pixels to units
        Location unitsPerPixel = camera.getUnitsPerPixel();
        offsetX *= unitsPerPixel.getX();
        offsetY *= unitsPerPixel.getY();

        return new Location(camera.getUnitsPerPixel().getUnits(), offsetX, offsetY, 0, 0);
    }
    
    /**
     * Get the Location of a set of pixel coordinates referenced to the center
     * of the given camera. This is a helper method that simply adds the
     * offsets from {@link VisionUtils#getPixelCenterOffsets(Camera, double, double)}
     * to the Camera's current location.
     * @param camera
     * @param x
     * @param y
     * @return
     */
    public static Location getPixelLocation(Camera camera, double x, double y) {
        return camera.getLocation().add(getPixelCenterOffsets(camera, x, y));
    }
}
