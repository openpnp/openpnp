package org.openpnp.vision.gpu;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

/**
 * An 8-bit, 1 or 3 channel image in GPU memory, packed row by row like a continuous Mat. Its pixels
 * never change once written, so pipeline results can share it; it is reference counted and its
 * buffer goes back to a pool, or to the recorded program that wrote it, on the last release().
 * Images written by a GpuRecording have no buffer until the recording is submitted.
 */
public final class GpuImage {
    private static final long POOL_IDLE_NS = TimeUnit.MINUTES.toNanos(1);
    private static final Map<Long, ArrayDeque<Pooled>> pool = new HashMap<>();

    private final int rows;
    private final int cols;
    private final int type;
    private GpuBuffer buffer;
    private GpuRecording recording;
    private GpuRecording.Instance owner;
    private boolean failed;
    private int references = 1;
    private long ready;
    private long lastUse;

    private static final class Pooled {
        final GpuBuffer buffer;
        final long lastUse;
        final long releasedNs = System.nanoTime();

        Pooled(GpuBuffer buffer, long lastUse) {
            this.buffer = buffer;
            this.lastUse = lastUse;
        }
    }

    private GpuImage(int rows, int cols, int type) {
        this.rows = rows;
        this.cols = cols;
        this.type = type;
        this.buffer = acquire(byteSize());
    }

    GpuImage(int rows, int cols, int type, GpuRecording recording) {
        if (!isSupported(type)) {
            throw new IllegalArgumentException("unsupported image type " + CvType.typeToString(type));
        }
        this.rows = rows;
        this.cols = cols;
        this.type = type;
        this.recording = recording;
    }

    public static boolean isSupported(int type) {
        return type == CvType.CV_8UC1 || type == CvType.CV_8UC3;
    }

    public static GpuImage allocate(int rows, int cols, int type) {
        if (!isSupported(type)) {
            throw new IllegalArgumentException("unsupported image type " + CvType.typeToString(type));
        }
        return new GpuImage(rows, cols, type);
    }

    public static GpuImage upload(Mat mat) {
        GpuImage image = allocate(mat.rows(), mat.cols(), mat.type());
        image.awaitIdle();
        mat.copyTo(new Mat(mat.rows(), mat.cols(), mat.type(), image.buffer.map()));
        return image;
    }

    /**
     * Uploads a TYPE_3BYTE_BGR or TYPE_BYTE_GRAY image; null for other types.
     */
    public static GpuImage upload(BufferedImage bufferedImage) {
        int type;
        if (bufferedImage.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            type = CvType.CV_8UC3;
        }
        else if (bufferedImage.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            type = CvType.CV_8UC1;
        }
        else {
            return null;
        }
        GpuImage image = allocate(bufferedImage.getHeight(), bufferedImage.getWidth(), type);
        image.awaitIdle();
        image.buffer.map().put(((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData());
        return image;
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    public int type() {
        return type;
    }

    public int channels() {
        return CvType.channels(type);
    }

    public synchronized GpuImage retain() {
        checkLive();
        references++;
        return this;
    }

    public synchronized void release() {
        checkLive();
        if (--references == 0) {
            if (owner != null) {
                owner.release();
            }
            else if (buffer != null) {
                recycle(buffer, lastUse);
            }
            buffer = null;
            owner = null;
        }
    }

    /**
     * Waits for the pixels and copies them into a new Mat.
     */
    public Mat download() {
        GpuBuffer b = buffer();
        long value;
        synchronized (this) {
            value = ready;
        }
        if (value > 0) {
            GpuRuntime.await(value, GpuCompute.TIMEOUT_NS);
        }
        return new Mat(rows, cols, type, b.map()).clone();
    }

    long byteSize() {
        return (long) rows * cols * CvType.channels(type);
    }

    /**
     * The buffer holding the pixels, submitting the recording that computes them first.
     */
    GpuBuffer buffer() {
        GpuRecording pending;
        synchronized (this) {
            checkLive();
            pending = recording;
        }
        if (pending != null) {
            pending.flush();
        }
        synchronized (this) {
            checkLive();
            if (failed) {
                throw new IllegalStateException("GPU work failed");
            }
            return buffer;
        }
    }

    synchronized void bind(GpuBuffer buffer, GpuRecording.Instance owner) {
        recording = null;
        if (references > 0) {
            this.buffer = buffer;
            this.owner = owner;
            owner.hold();
        }
    }

    synchronized void fail() {
        recording = null;
        failed = true;
    }

    /**
     * Records the GPU submission that writes the pixels.
     */
    synchronized void writtenBy(long value) {
        ready = value;
        lastUse = Math.max(lastUse, value);
    }

    synchronized void readBy(long value) {
        lastUse = Math.max(lastUse, value);
    }

    // A recycled buffer may still be read by a pending submission; the host must not overwrite it
    // before that ran. GPU writes need no wait, submissions run in order.
    private void awaitIdle() {
        if (lastUse > 0) {
            GpuRuntime.await(lastUse, GpuCompute.TIMEOUT_NS);
        }
    }

    private void checkLive() {
        if (references <= 0) {
            throw new IllegalStateException("GpuImage is released");
        }
    }

    private GpuBuffer acquire(long size) {
        size = Math.max(16, size);
        synchronized (pool) {
            evictIdle();
            ArrayDeque<Pooled> free = pool.get(size);
            Pooled pooled = free == null ? null : free.pollLast();
            if (pooled != null) {
                lastUse = pooled.lastUse;
                return pooled.buffer;
            }
        }
        return new GpuBuffer(size, true);
    }

    private static void recycle(GpuBuffer buffer, long lastUse) {
        synchronized (pool) {
            pool.computeIfAbsent(buffer.getSize(), k -> new ArrayDeque<>()).addLast(new Pooled(buffer, lastUse));
            evictIdle();
        }
    }

    private static void evictIdle() {
        long now = System.nanoTime();
        for (Iterator<ArrayDeque<Pooled>> it = pool.values().iterator(); it.hasNext();) {
            ArrayDeque<Pooled> free = it.next();
            while (!free.isEmpty() && now - free.peekFirst().releasedNs > POOL_IDLE_NS) {
                free.pollFirst().buffer.close();
            }
            if (free.isEmpty()) {
                it.remove();
            }
        }
    }
}
