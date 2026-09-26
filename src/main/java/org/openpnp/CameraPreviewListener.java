package org.openpnp;

import java.awt.image.BufferedImage;

/**
 * A CameraListener that can take frames already scaled to the size it displays them at, so
 * cameras that render on the GPU never hand full-resolution frames to the CPU just for display.
 */
public interface CameraPreviewListener extends CameraListener {
    /**
     * An image to render part of a frame into: the region at x, y of size width x height, in full
     * frame pixels, stretched over the whole image.
     */
    final class Preview {
        public final BufferedImage image;
        public final double x;
        public final double y;
        public final double width;
        public final double height;

        public Preview(BufferedImage image, double x, double y, double width, double height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Whether frames are wanted at all right now, e.g. false while hidden.
     */
    boolean isPreviewWanted();

    /**
     * Where to render a frame of the given full size, as a TYPE_INT_RGB image the listener doesn't
     * display until previewReceived(). Null to receive full frames through frameReceived().
     */
    Preview previewBuffer(int frameWidth, int frameHeight);

    void previewReceived(Preview preview, int frameWidth, int frameHeight);
}
