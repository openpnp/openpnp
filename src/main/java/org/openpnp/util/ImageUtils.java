package org.openpnp.util;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.pmw.tinylog.Logger;

public class ImageUtils {

    /**
     * Convert a BufferedImage from it's current type to a new, specified type by creating a new
     * BufferedImage and drawing the source image onto it. If the image is already of the specified
     * type it is returned unchanged.
     * 
     * @param src
     * @param type
     * @return
     */
    public static BufferedImage convertBufferedImage(BufferedImage src, int type) {
        if (src.getType() == type) {
            return src;
        }
        BufferedImage img = new BufferedImage(src.getWidth(), src.getHeight(), type);
        Graphics2D g2d = img.createGraphics();
        g2d.drawImage(src, 0, 0, null);
        g2d.dispose();
        return img;
    }

    /**
     * Clone an image for independent manipulation.  
     * 
     * @param image
     * @return
     */
    public static BufferedImage clone(BufferedImage image) {
        ColorModel colorModel = image.getColorModel();
        WritableRaster raster = image.copyData(image.getRaster().createCompatibleWritableRaster());
        boolean isAlphaPremultiplied = colorModel.isAlphaPremultiplied();
        return new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
    }

    /**
     * Get the Units per Pixel for the given imageFile. 
     * Read the manifest upp.txt file that is side-by-side with the image file. 
     * 
     * @param imageFile
     * @return
     */
    public static Location getUnitsPerPixel(File imageFile) {
        Location upp = null;
        // Try look for units per pixel manifest in the same directory as the image.
        try {
            File uppFile = new File(imageFile.getParent(), "upp.txt");
            if (uppFile.exists()) {
                String upps = FileUtils.readFileToString(uppFile);
                double x = Double.valueOf(upps.substring(0, upps.indexOf(" ")));
                double y = Double.valueOf(upps.substring(upps.indexOf(" ")+1));
                upp = new Location(LengthUnit.Millimeters, x, y, 0, 0);
            }
        }
        catch (NumberFormatException | IOException e) {
           Logger.warn(e);
        }
        return upp;
    }

    /**
     * Emulate an image captured by the given camera, but read it from a file. 
     * The image is adapted to the camera resolution, aspect ratio and to Units per Pixel if a upp.txt manifest is present 
     * side-by-side with the image file. This allows for pixel to lengths conversions typically performed by pipeline
     * stages and pipeline callers to be accurate. The returned image is extend/cropped as needed.
     * 
     * @param camera
     * @param file
     * @return
     */
    public static Mat emulateCameraCapture(Camera camera, File file) {
        Mat image = Imgcodecs.imread(file.getAbsolutePath());
        Location upp = ImageUtils.getUnitsPerPixel(file);
        double fx;
        double fy;
        if (upp == null) {
            // No UPP given, all we can do is resize to camera dimensions.
            fx = fy = Math.min(((double)camera.getWidth())/image.cols(), ((double)camera.getHeight())/image.rows());
        }
        else {
            // Scale according to UPP from file against that of the camera.
            Location uppCamera = camera.getUnitsPerPixel();
            fx = upp.getLengthX().divide(uppCamera.getLengthX());
            fy = upp.getLengthY().divide(uppCamera.getLengthY());
        }
        Imgproc.resize(image, image, new Size(), fx, fy, Imgproc.INTER_LANCZOS4);
        int bx = Math.max(0, camera.getWidth() - image.cols());
        int by = Math.max(0, camera.getHeight() - image.rows());
        if (bx > 0 || by > 0) {
            Core.copyMakeBorder(image, image, by/2, (by+1)/2, bx/2, (bx+1)/2, Core.BORDER_CONSTANT);
        }
        int cx = Math.max(0, image.cols() - camera.getWidth())/2;
        int cy = Math.max(0, image.rows() - camera.getHeight())/2;
        if (cx > 0 || cy > 0) {
            Rect roi = new Rect(
                    cx,
                    cy,
                    camera.getWidth(),
                    camera.getHeight());
            Mat tmp = new Mat(image, roi);
            image.release();
            image = tmp;
        }
        return image;
    }
}
