package org.openpnp.machine.reference.camera;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.util.Map;

import org.openpnp.capture.CaptureProperty;
import org.openpnp.capture.PropertyLimits;
import org.openpnp.vision.gpu.OclCameraTransform;
import org.openpnp.vision.gpu.OclSupport;

/**
 * Streams raw YUYV frames from a V4L2 device so they can be converted on the GPU, bypassing
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

    private static native long open(String uniqueId, int width, int height, int fps);

    private static native void close(long handle);

    private static native boolean hasNewFrame(long handle);

    private static native boolean captureBgr(long handle, int timeoutMs, byte[] dst) throws IOException;

    private static native boolean queryControl(long handle, int id, int[] limits);

    private static native int getControl(long handle, int id);

    private static native void setControl(long handle, int id, int value);

    public V4l2Stream(String uniqueId, int width, int height, int fps) {
        if (!OclSupport.isAvailable()) {
            throw new IllegalStateException("OpenCL is not available");
        }
        this.handle = open(uniqueId, width, height, fps);
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public synchronized boolean hasNewFrame() {
        return handle != 0 && hasNewFrame(handle);
    }

    /**
     * Converts the newest frame on the CPU, for raw captures that bypass the GPU transforms.
     */
    public synchronized BufferedImage captureBgr(int timeoutMs) throws IOException {
        checkOpen();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        byte[] dst = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        return captureBgr(handle, timeoutMs, dst) ? image : null;
    }

    public synchronized BufferedImage captureTransformed(OclCameraTransform transform, int timeoutMs)
            throws IOException {
        checkOpen();
        return transform.applyV4l2(handle, timeoutMs);
    }

    @Override
    public synchronized PropertyLimits getPropertyLimits(CaptureProperty property) throws Exception {
        checkOpen();
        int[] limits = new int[3];
        if (!queryControl(handle, control(VALUE_CONTROLS, property), limits)) {
            throw new Exception(property + " is not supported");
        }
        return new PropertyLimits(limits[0], limits[1], limits[2]);
    }

    @Override
    public synchronized void setAutoProperty(CaptureProperty property, boolean auto) throws Exception {
        checkOpen();
        int id = control(AUTO_CONTROLS, property);
        if (id == CID_EXPOSURE_AUTO) {
            setControl(handle, id, auto ? EXPOSURE_APERTURE_PRIORITY : EXPOSURE_MANUAL);
        }
        else {
            setControl(handle, id, auto ? 1 : 0);
        }
    }

    @Override
    public synchronized boolean getAutoProperty(CaptureProperty property) throws Exception {
        checkOpen();
        int id = control(AUTO_CONTROLS, property);
        int value = getControl(handle, id);
        return id == CID_EXPOSURE_AUTO ? value != EXPOSURE_MANUAL : value != 0;
    }

    @Override
    public synchronized void setProperty(CaptureProperty property, int value) throws Exception {
        checkOpen();
        setControl(handle, control(VALUE_CONTROLS, property), value);
    }

    @Override
    public synchronized int getProperty(CaptureProperty property) throws Exception {
        checkOpen();
        return getControl(handle, control(VALUE_CONTROLS, property));
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
    public synchronized void close() {
        if (handle != 0) {
            close(handle);
            handle = 0;
        }
    }
}
