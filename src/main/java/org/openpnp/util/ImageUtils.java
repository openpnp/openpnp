package org.openpnp.util;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

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

}
