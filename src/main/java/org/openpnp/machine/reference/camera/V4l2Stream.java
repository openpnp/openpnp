package org.openpnp.machine.reference.camera;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openpnp.capture.CaptureProperty;
import org.openpnp.capture.PropertyLimits;
import org.openpnp.vision.gpu.GpuBuffer;
import org.openpnp.vision.gpu.GpuCameraTransform;
import org.openpnp.vision.gpu.GpuRuntime;
import org.pmw.tinylog.Logger;

/**
 * Streams raw YUYV frames from a V4L2 device straight into GPU buffers, bypassing
 * openpnp-capture's CPU conversion. Controls map to the same V4L2 controls openpnp-capture uses,
 * so stored camera properties keep their meaning.
 */
public class V4l2Stream implements OpenPnpCaptureCamera.CaptureControls, AutoCloseable {
    public static final int FOURCC_YUYV = 0x56595559;

    private static final int CID_BRIGHTNESS = 0x980900;
    private static final int CID_CONTRAST = 0x980901;
    private static final int CID_SATURATION = 0x980902;
    private static final int CID_HUE = 0x980903;
    private static final int CID_AUTO_WHITE_BALANCE = 0x98090c;
    private static final int CID_GAMMA = 0x980910;
    private static final int CID_AUTOGAIN = 0x980912;
    private static final int CID_POWER_LINE_FREQUENCY = 0x980918;
    private static final int CID_WHITE_BALANCE_TEMPERATURE = 0x98091a;
    private static final int CID_SHARPNESS = 0x98091b;
    private static final int CID_BACKLIGHT_COMPENSATION = 0x98091c;
    private static final int CID_EXPOSURE_AUTO = 0x9a0901;
    private static final int CID_EXPOSURE_ABSOLUTE = 0x9a0902;
    private static final int CID_FOCUS_ABSOLUTE = 0x9a090a;
    private static final int CID_FOCUS_AUTO = 0x9a090c;
    private static final int CID_ZOOM_ABSOLUTE = 0x9a090d;
    private static final int EXPOSURE_MANUAL = 1;
    private static final int EXPOSURE_APERTURE_PRIORITY = 3;

    // Mirrors openpnp-capture's Linux mapping, which has no manual gain.
    private static final Map<CaptureProperty, Integer> VALUE_CONTROLS = Map.ofEntries(
            Map.entry(CaptureProperty.Exposure, CID_EXPOSURE_ABSOLUTE),
            Map.entry(CaptureProperty.Focus, CID_FOCUS_ABSOLUTE),
            Map.entry(CaptureProperty.Zoom, CID_ZOOM_ABSOLUTE),
            Map.entry(CaptureProperty.WhiteBalance, CID_WHITE_BALANCE_TEMPERATURE),
            Map.entry(CaptureProperty.Brightness, CID_BRIGHTNESS),
            Map.entry(CaptureProperty.Contrast, CID_CONTRAST),
            Map.entry(CaptureProperty.Saturation, CID_SATURATION),
            Map.entry(CaptureProperty.Gamma, CID_GAMMA),
            Map.entry(CaptureProperty.Hue, CID_HUE),
            Map.entry(CaptureProperty.Sharpness, CID_SHARPNESS),
            Map.entry(CaptureProperty.BackLightCompensation, CID_BACKLIGHT_COMPENSATION),
            Map.entry(CaptureProperty.PowerLineFrequency, CID_POWER_LINE_FREQUENCY));

    private static final Map<CaptureProperty, Integer> AUTO_CONTROLS = Map.of(
            CaptureProperty.Exposure, CID_EXPOSURE_AUTO,
            CaptureProperty.Focus, CID_FOCUS_AUTO,
            CaptureProperty.WhiteBalance, CID_AUTO_WHITE_BALANCE,
            CaptureProperty.Gain, CID_AUTOGAIN);

    private long handle;
    private final int width;
    private final int height;
    private final int stride;
    private final GpuBuffer[] slots = new GpuBuffer[16];
    // Vision captures and previews each see every frame once, independently of each other.
    private final AtomicLong lastSequence = new AtomicLong();
    private final AtomicLong previewSequence = new AtomicLong();
    // Captures only need the stream to stay open; blocking in one must not hold up controls.
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private GpuCameraTransform rawTransform;
    private boolean reported;

    private static native long open(String uniqueId, int width, int height, int fps);

    private static native void close(long handle);

    private static native int bytesPerLine(long handle);

    private static native boolean isZeroCopy(long handle);

    private static native boolean hasNewFrame(long handle, long after);

    private static native int acquire(long handle, long after, long notBeforeNs, int timeoutMs, long[] info)
            throws IOException;

    private static native long slotBuffer(long handle, int slot);

    private static native void release(long handle, int slot, long gpuValue);

    private static native boolean queryControl(long handle, int id, int[] limits);

    private static native int getControl(long handle, int id);

    private static native void setControl(long handle, int id, int value);

    public V4l2Stream(String uniqueId, int width, int height, int fps) {
        if (!GpuRuntime.isAvailable()) {
            throw new IllegalStateException("GPU is not available");
        }
        this.handle = open(uniqueId, width, height, fps);
        this.width = width;
        this.height = height;
        this.stride = bytesPerLine(handle);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isZeroCopy() {
        lock.readLock().lock();
        try {
            return handle != 0 && isZeroCopy(handle);
        }
        finally {
            lock.readLock().unlock();
        }
    }

    public boolean hasNewFrame() {
        lock.readLock().lock();
        try {
            return handle != 0 && hasNewFrame(handle, lastSequence.get());
        }
        finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Converts the next frame without any camera transforms.
     */
    public BufferedImage captureBgr(int timeoutMs) throws IOException {
        GpuCameraTransform transform;
        synchronized (this) {
            if (rawTransform == null) {
                rawTransform = new GpuCameraTransform();
            }
            transform = rawTransform;
        }
        return captureTransformed(transform, timeoutMs);
    }

    /**
     * Converts and transforms the next frame not captured yet. With a timeout, the frame must also
     * have been captured after this call, so it never shows the scene before e.g. settling.
     * Returns null on timeout.
     */
    public BufferedImage captureTransformed(GpuCameraTransform transform, int timeoutMs) throws IOException {
        long notBefore = timeoutMs > 0 ? System.nanoTime() : 0;
        return withFrame(lastSequence, notBefore, timeoutMs, (slot, submitted) -> transform.render(slot,
                GpuCameraTransform.Input.Yuyv, width, height, stride, submitted));
    }

    public boolean hasNewPreviewFrame() {
        lock.readLock().lock();
        try {
            return handle != 0 && hasNewFrame(handle, previewSequence.get());
        }
        finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Renders the newest frame not previewed yet into each TYPE_INT_RGB preview image. Returns
     * false when there is no new frame.
     */
    public boolean capturePreview(GpuCameraTransform transform, BufferedImage[] previews) throws IOException {
        Boolean rendered = withFrame(previewSequence, 0, 0, (slot, submitted) -> {
            for (BufferedImage preview : previews) {
                transform.renderPreview(slot, GpuCameraTransform.Input.Yuyv, width, height, stride, preview,
                        submitted);
            }
            return true;
        });
        return rendered != null;
    }

    /**
     * Renders the newest frame, whether or not it was captured or previewed before.
     */
    public BufferedImage captureNewest(GpuCameraTransform transform, int timeoutMs) throws IOException {
        return withFrame(null, 0, timeoutMs, (slot, submitted) -> transform.render(slot,
                GpuCameraTransform.Input.Yuyv, width, height, stride, submitted));
    }

    private interface FrameRenderer<T> {
        T render(GpuBuffer slot, LongConsumer submitted);
    }

    private <T> T withFrame(AtomicLong consumed, long notBeforeNs, int timeoutMs, FrameRenderer<T> renderer)
            throws IOException {
        lock.readLock().lock();
        try {
            checkOpen();
            long[] info = new long[2];
            int slot = acquire(handle, consumed == null ? 0 : consumed.get(), notBeforeNs, timeoutMs, info);
            if (slot < 0) {
                return null;
            }
            if (consumed != null) {
                consumed.accumulateAndGet(info[0], Math::max);
            }
            reportMode();
            long[] gpuValue = new long[1];
            try {
                return renderer.render(slot(slot), value -> gpuValue[0] = Math.max(gpuValue[0], value));
            }
            finally {
                release(handle, slot, gpuValue[0]);
            }
        }
        finally {
            lock.readLock().unlock();
        }
    }

    // Zero copy is only known after the first frame, which checks the GPU sees the buffers coherently.
    private synchronized void reportMode() {
        if (!reported) {
            reported = true;
            Logger.info("V4L2 {}x{} stream {}.", width, height, isZeroCopy(handle)
                    ? "is read by the GPU in place (dma-buf)" : "is copied into GPU buffers");
        }
    }

    private synchronized GpuBuffer slot(int slot) {
        if (slots[slot] == null) {
            slots[slot] = GpuBuffer.adopt(slotBuffer(handle, slot), (long) stride * height, false);
        }
        return slots[slot];
    }

    private interface Control<T> {
        T apply(long handle) throws Exception;
    }

    private <T> T withHandle(Control<T> control) throws Exception {
        lock.readLock().lock();
        try {
            checkOpen();
            return control.apply(handle);
        }
        finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public PropertyLimits getPropertyLimits(CaptureProperty property) throws Exception {
        int id = control(VALUE_CONTROLS, property);
        int[] limits = new int[3];
        if (!withHandle(h -> queryControl(h, id, limits))) {
            throw new Exception(property + " is not supported");
        }
        return new PropertyLimits(limits[0], limits[1], limits[2]);
    }

    @Override
    public void setAutoProperty(CaptureProperty property, boolean auto) throws Exception {
        int id = control(AUTO_CONTROLS, property);
        int value = id == CID_EXPOSURE_AUTO ? (auto ? EXPOSURE_APERTURE_PRIORITY : EXPOSURE_MANUAL) : (auto ? 1 : 0);
        withHandle(h -> {
            setControl(h, id, value);
            return null;
        });
    }

    @Override
    public boolean getAutoProperty(CaptureProperty property) throws Exception {
        int id = control(AUTO_CONTROLS, property);
        int value = withHandle(h -> getControl(h, id));
        return id == CID_EXPOSURE_AUTO ? value != EXPOSURE_MANUAL : value != 0;
    }

    @Override
    public void setProperty(CaptureProperty property, int value) throws Exception {
        int id = control(VALUE_CONTROLS, property);
        withHandle(h -> {
            setControl(h, id, value);
            return null;
        });
    }

    @Override
    public int getProperty(CaptureProperty property) throws Exception {
        int id = control(VALUE_CONTROLS, property);
        return withHandle(h -> getControl(h, id));
    }

    private static int control(Map<CaptureProperty, Integer> controls, CaptureProperty property)
            throws Exception {
        Integer id = controls.get(property);
        if (id == null) {
            throw new Exception(property + " is not supported");
        }
        return id;
    }

    private void checkOpen() {
        if (handle == 0) {
            throw new IllegalStateException("closed");
        }
    }

    @Override
    public void close() {
        lock.writeLock().lock();
        try {
            if (handle != 0) {
                synchronized (this) {
                    for (int i = 0; i < slots.length; i++) {
                        if (slots[i] != null) {
                            slots[i].close();
                            slots[i] = null;
                        }
                    }
                    if (rawTransform != null) {
                        rawTransform.close();
                        rawTransform = null;
                    }
                }
                close(handle);
                handle = 0;
            }
        }
        finally {
            lock.writeLock().unlock();
        }
    }
}
