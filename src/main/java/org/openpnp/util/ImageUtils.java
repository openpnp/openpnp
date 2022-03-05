package org.openpnp.util;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
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

}
