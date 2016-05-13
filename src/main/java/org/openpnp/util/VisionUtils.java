package org.openpnp.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;

public class VisionUtils {
    /**
     * Given pixel coordinates within the frame of the Camera's image, get the offsets from Camera
     * center to the coordinates in Camera space and units. The resulting value is the distance the
     * Camera can be moved to be centered over the pixel coordinates.
     * 
     * Example: If the x, y coordinates describe a position above and to the left of the center of
     * the camera the offsets will be -,+.
     * 
     * If the coordinates position are below and to the right of center the offsets will be +, -.
     * 
     * Calling camera.getLocation().add(getPixelCenterOffsets(...) will give you the location of x,
     * y with respect to the center of the camera.
     * 
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
     * Get the Location of a set of pixel coordinates referenced to the center of the given camera.
     * This is a helper method that simply adds the offsets from
     * {@link VisionUtils#getPixelCenterOffsets(Camera, double, double)} to the Camera's current
     * location.
     * 
     * @param camera
     * @param x
     * @param y
     * @return
     */
    public static Location getPixelLocation(Camera camera, double x, double y) {
        return camera.getLocation().add(getPixelCenterOffsets(camera, x, y));
    }

    public static List<Location> sortLocationsByDistance(final Location origin,
            List<Location> locations) {
        // sort the results by distance from center ascending
        Collections.sort(locations, new Comparator<Location>() {
            public int compare(Location o1, Location o2) {
                Double o1d = origin.getLinearDistanceTo(o1);
                Double o2d = origin.getLinearDistanceTo(o2);
                return o1d.compareTo(o2d);
            }
        });
        return locations;
    }
    
    public static Camera getBottomVisionCamera() throws Exception {
        for (Camera camera : Configuration.get().getMachine().getCameras()) {
            if (camera.getLooking() == Camera.Looking.Up) {
                return camera;
            }
        }
        throw new Exception("No up-looking camera found on the machine to use for bottom vision.");
    }
    
    public static double toPixels(Length length, Camera camera) {
        // convert inputs to the same units
        Location unitsPerPixel = camera.getUnitsPerPixel();
        length = length.convertToUnits(unitsPerPixel.getUnits());

        // we average the units per pixel because circles can't be ovals
        double avgUnitsPerPixel = (unitsPerPixel.getX() + unitsPerPixel.getY()) / 2;

        // convert it all to pixels
        return length.getValue() / avgUnitsPerPixel;
    }
}
