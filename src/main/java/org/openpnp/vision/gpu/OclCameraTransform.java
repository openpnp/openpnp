package org.openpnp.vision.gpu;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;

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

    private static native boolean applyV4l2(long handle, long stream, int timeoutMs, byte[] dst)
            throws IOException;

    public OclCameraTransform() {
        if (!OclSupport.isAvailable()) {
            throw new IllegalStateException("OpenCL is not available");
        }
        handle = create();
    }

    /**
     * mapX and mapY are CV_32FC1 source coordinates for every output pixel, or null to keep the
     * frameWidth x frameHeight input geometry. lut is a 256x1 CV_8UC3 table applied before the
     * remap, or null.
     */
    public synchronized void setTransform(Mat mapX, Mat mapY, Mat lut, int frameWidth, int frameHeight) {
        checkOpen();
        setTransform(handle, mapX == null ? 0 : mapX.nativeObj, mapY == null ? 0 : mapY.nativeObj,
                lut == null ? 0 : lut.nativeObj);
        width = mapX == null ? frameWidth : mapX.cols();
        height = mapX == null ? frameHeight : mapX.rows();
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

    /**
     * Grabs the newest YUYV frame of a V4L2 stream, converts and transforms it on the GPU. Returns
     * null when no frame arrived within timeoutMs. The caller must hold the stream's lock.
     */
    public synchronized BufferedImage applyV4l2(long stream, int timeoutMs) throws IOException {
        checkOpen();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        byte[] dst = ((DataBufferByte) result.getRaster().getDataBuffer()).getData();
        return applyV4l2(handle, stream, timeoutMs, dst) ? result : null;
    }

    public synchronized boolean isClosed() {
        return handle == 0;
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
