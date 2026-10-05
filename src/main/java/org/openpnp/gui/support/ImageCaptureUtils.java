package org.openpnp.gui.support;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.pmw.tinylog.Logger;

/**
 * Utilities for capturing and processing images from cameras.
 */
public class ImageCaptureUtils {

    /**
     * Settle and capture an image from the camera, ensuring the camera light is on
     * if available.
     * Restores the light state afterwards.
     * 
     * @param camera The camera to capture from.
     * @return The captured image.
     * @throws Exception If capture fails.
     */
    public static BufferedImage settleAndCaptureWithLight(Camera camera) throws Exception {
        Actuator light = camera.getLightActuator();
        boolean lightWasOn = false;
        if (light != null) {
            Boolean actuated = light.isActuated();
            if (actuated == null || !actuated) {
                // Must actuate on machine thread if not already on
                // Note: In some contexts we might be on UI thread or Machine task.
                // Safest to just call actuate(). If we are in a machine task, it works.
                // If we are on UI thread, it might block or throw if machine is busy?
                // The original code in FeedersPanel used UiUtils.submitUiMachineTask wrapper
                // which is fine.
                // Inside here we assume we are inside a context where we can control actuators.
                // However, FeedersPanel.captureImage wrapped this in a task.
                // PlacementsPreviewDialog also submitted.
                // So calling this method implies we are in a background thread or machine task
                // capable of actuation.

                // Double check if we need to wrap in executeIfEnabled like BoardScannerDialog
                // did?
                // FeedersPanel didn't. PlacementsPreviewDialog didn't.

                light.actuate(true);
                Thread.sleep(250);
                lightWasOn = false;
            } else {
                lightWasOn = true;
            }
        }

        try {
            return camera.settleAndCapture();
        } finally {
            if (light != null && !lightWasOn) {
                light.actuate(false);
            }
        }
    }

    /**
     * Rotate a buffered image by the specified degrees.
     * 
     * @param image           The source image.
     * @param rotationDegrees Rotation in degrees.
     * @return The rotated image, or original if 0.
     */
    public static BufferedImage rotateImage(BufferedImage image, double rotationDegrees) {
        if (rotationDegrees == 0) {
            return image;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        int type = image.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : image.getType();
        BufferedImage rotatedImage = new BufferedImage(w, h, type);
        Graphics2D g2 = rotatedImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        AffineTransform xform = new AffineTransform();
        xform.rotate(Math.toRadians(rotationDegrees), w / 2.0, h / 2.0);
        g2.drawRenderedImage(image, xform);
        g2.dispose();
        return rotatedImage;
    }

    /**
     * Crop the image to the part's package footprint body dimensions, with margin.
     * 
     * @param image  The image to crop.
     * @param part   The part containing package/footprint info.
     * @param camera The camera used (for units per pixel).
     * @return The cropped image, or original if no cropping applied.
     */
    public static BufferedImage cropImageToPart(BufferedImage image, Part part, Camera camera) {
        if (part == null || part.getPackage() == null || part.getPackage().getFootprint() == null) {
            return image;
        }
        try {
            org.openpnp.model.Footprint footprint = part.getPackage().getFootprint();
            double bodyWidth = footprint.getBodyWidth();
            double bodyHeight = footprint.getBodyHeight();

            if (bodyWidth > 0 && bodyHeight > 0) {
                Location unitsPerPixel = camera.getUnitsPerPixel();
                double upp = Math.abs(unitsPerPixel.getX());

                // Convert to pixels
                int targetW = (int) Math.ceil(bodyWidth / upp);
                int targetH = (int) Math.ceil(bodyHeight / upp);

                // Add margin (e.g. 20%)
                targetW = (int) (targetW * 1.2);
                targetH = (int) (targetH * 1.2);

                // Ensure we don't crop larger than the image
                targetW = Math.min(targetW, image.getWidth());
                targetH = Math.min(targetH, image.getHeight());

                // Center crop
                int x = (image.getWidth() - targetW) / 2;
                int y = (image.getHeight() - targetH) / 2;

                if (x >= 0 && y >= 0) {
                    return image.getSubimage(x, y, targetW, targetH);
                }
            }
        } catch (Exception e) {
            Logger.debug(e, "Failed to crop image to part.");
        }
        return image;
    }
}
