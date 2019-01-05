package org.openpnp.util;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PartAlignment;
import org.pmw.tinylog.Logger;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

public class VisionUtils {
    public static String PIPELINE_RESULTS_NAME = "results";
    
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
    
    /**
     * Using the given camera, try to find a QR code and return it's text. This is just a wrapper
     * for the generic scanBarcode(Camera) function. This one was added before the other and I don't
     * want to remove it in case people are using it, but it does the same thing. 
     * @param camera
     * @return
     */
    public static String readQrCode(Camera camera) {
        return scanBarcode(camera);
    }
    
    /**
     * Using the given camera, try to find any supported barcode and return it's text. 
     * @param camera
     * @return
     */
    public static String scanBarcode(Camera camera) {
        BufferedImage image = camera.settleAndCapture();
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
                new BufferedImageLuminanceSource(image)));
        try {
            Result qrCodeResult = new MultiFormatReader().decode(binaryBitmap);
            return qrCodeResult.getText();    
        }
        catch (Exception e) {
            return null;
        }
    }
    
    public static PartAlignment.PartAlignmentOffset findPartAlignmentOffsets(PartAlignment p, Part part, BoardLocation boardLocation, Location placementLocation, Nozzle nozzle) throws Exception {
        try {
            Map<String, Object> globals = new HashMap<>();
            globals.put("part", part);
            globals.put("nozzle", nozzle);
            Configuration.get().getScripting().on("Vision.PartAlignment.Before", globals);
        }
        catch (Exception e) {
            Logger.warn(e);
        }
        try {
            return p.findOffsets(part, boardLocation, placementLocation, nozzle);
        }
        finally {
            try {
                Map<String, Object> globals = new HashMap<>();
                globals.put("part", part);
                globals.put("nozzle", nozzle);
                Configuration.get().getScripting().on("Vision.PartAlignment.After", globals);
            }
            catch (Exception e) {
                Logger.warn(e);
            }
        }
    }
}
