package org.openpnp.util;

import java.awt.image.BufferedImage;

import org.openpnp.model.Location;
import org.openpnp.spi.Camera;

public class VisionUtils {
    public static Location getPixelCenterOffsets(Camera camera, int x, int y) {
        double imageWidth = camera.getWidth();
        double imageHeight = camera.getHeight();

        // Calculate the difference between the center of the image to the
        // center of the match.
        double offsetX = (imageWidth / 2) - x;
        double offsetY = (imageHeight / 2) - y;

        // Invert the Y offset because images count top to bottom and the Y
        // axis of the machine counts bottom to top.
        offsetY *= -1;
        
        // And convert pixels to units
        Location unitsPerPixel = camera.getUnitsPerPixel();
        offsetX *= unitsPerPixel.getX();
        offsetY *= unitsPerPixel.getY();

        return new Location(camera.getUnitsPerPixel().getUnits(), offsetX, offsetY, 0, 0);
    }
}
