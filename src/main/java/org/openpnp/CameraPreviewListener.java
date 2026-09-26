package org.openpnp;

import java.awt.image.BufferedImage;

/**
 * A CameraListener that can take frames already scaled to the size it displays them at, so
 * cameras that render on the GPU never hand full-resolution frames to the CPU just for display.
 */
public interface CameraPreviewListener extends CameraListener {
    /**
     * Whether frames are wanted at all right now, e.g. false while hidden.
     */
    boolean isPreviewWanted();

    /**
     * A TYPE_INT_RGB image to render a frame of the given full size into, which the listener
     * doesn't display until previewReceived(). Null to receive full frames through frameReceived().
     */
    BufferedImage previewBuffer(int frameWidth, int frameHeight);

    void previewReceived(BufferedImage preview, int frameWidth, int frameHeight);
}
