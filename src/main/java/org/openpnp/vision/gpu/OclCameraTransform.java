package org.openpnp.vision.gpu;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.opencv.core.Mat;

/**
 * Applies a camera's white balance LUT and composite remap on the GPU.
 */
public class OclCameraTransform implements AutoCloseable {
    private long handle;
    private int width;
    private int height;

    private static native long create();

    private static native void release(long handle);

    private static native void setTransform(long handle, long mapX, long mapY, long lut);

    private static native void apply(long handle, byte[] src, int width, int height, int channels,
            byte[] dst);

    public OclCameraTransform() {
        if (!OclSupport.isAvailable()) {
            throw new IllegalStateException("OpenCL is not available");
        }
        handle = create();
    }

    /**
     * mapX and mapY are CV_32FC1 source coordinates for every output pixel, lut is a 256x1 CV_8UC3
     * table applied before the remap, or null.
     */
    public synchronized void setTransform(Mat mapX, Mat mapY, Mat lut) {
        checkOpen();
        setTransform(handle, mapX.nativeObj, mapY.nativeObj, lut == null ? 0 : lut.nativeObj);
        width = mapX.cols();
        height = mapX.rows();
    }

    public synchronized BufferedImage apply(BufferedImage image) {
        checkOpen();
        int channels;
        if (image.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            channels = 3;
        }
        else if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            channels = 1;
        }
        else {
            throw new IllegalArgumentException("Unsupported image type " + image.getType());
        }
        byte[] src = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        BufferedImage result = new BufferedImage(width, height, image.getType());
        byte[] dst = ((DataBufferByte) result.getRaster().getDataBuffer()).getData();
        apply(handle, src, image.getWidth(), image.getHeight(), channels, dst);
        return result;
    }

    private void checkOpen() {
        if (handle == 0) {
            throw new IllegalStateException("closed");
        }
    }

    @Override
    public synchronized void close() {
        if (handle != 0) {
            release(handle);
            handle = 0;
        }
    }
}
