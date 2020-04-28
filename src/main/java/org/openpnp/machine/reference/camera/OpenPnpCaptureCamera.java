/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.camera;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import org.openpnp.CameraListener;
import org.openpnp.ConfigurationListener;
import org.openpnp.capture.CaptureDevice;
import org.openpnp.capture.CaptureFormat;
import org.openpnp.capture.CaptureProperty;
import org.openpnp.capture.CaptureStream;
import org.openpnp.capture.OpenPnpCapture;
import org.openpnp.capture.PropertyLimits;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.OpenPnpCaptureCameraConfigurationWizard;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.PropertySheetHolder;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

public class OpenPnpCaptureCamera extends ReferenceCamera implements Runnable {
    private OpenPnpCapture capture = new OpenPnpCapture();
    private Thread thread;

    private CaptureDevice device;
    private CaptureFormat format;
    private CaptureStream stream;

    @Attribute(required = false)
    private String uniqueId;

    @Attribute(required = false)
    private Integer formatId;

    @Attribute(required = false)
    private double fps = 10.;

    @Element(required = false)
    private CapturePropertyHolder backLightCompensation = new CapturePropertyHolder(CaptureProperty.BackLightCompensation);

    @Element(required = false)
    private CapturePropertyHolder brightness = new CapturePropertyHolder(CaptureProperty.Brightness);

    @Element(required = false)
    private CapturePropertyHolder contrast = new CapturePropertyHolder(CaptureProperty.Contrast);

    @Element(required = false)
    private CapturePropertyHolder exposure = new CapturePropertyHolder(CaptureProperty.Exposure);

    @Element(required = false)
    private CapturePropertyHolder focus = new CapturePropertyHolder(CaptureProperty.Focus);

    @Element(required = false)
    private CapturePropertyHolder gain = new CapturePropertyHolder(CaptureProperty.Gain);

    @Element(required = false)
    private CapturePropertyHolder gamma = new CapturePropertyHolder(CaptureProperty.Gamma);

    @Element(required = false)
    private CapturePropertyHolder hue = new CapturePropertyHolder(CaptureProperty.Hue);

    @Element(required = false)
    private CapturePropertyHolder powerLineFrequency = new CapturePropertyHolder(CaptureProperty.PowerLineFrequency);

    @Element(required = false)
    private CapturePropertyHolder saturation = new CapturePropertyHolder(CaptureProperty.Saturation);

    @Element(required = false)
    private CapturePropertyHolder sharpness = new CapturePropertyHolder(CaptureProperty.Sharpness);

    @Element(required = false)
    private CapturePropertyHolder whiteBalance = new CapturePropertyHolder(CaptureProperty.WhiteBalance);

    @Element(required = false)
    private CapturePropertyHolder zoom = new CapturePropertyHolder(CaptureProperty.Zoom);
    
    // Calling notifyAll on this object will wake the stream thread for one loop to broadcast
    // a new image.
    private Object captureNotifier = new Object();

    public OpenPnpCaptureCamera() {
        // TODO Seems silly this has to be in every implementation. Should Camera implement MachineListener
        // and every camera gets added as a listener automatically? And we codify the notifyCapture()
        // system in some way? Seems like the entire broadcast system should move into a base class?
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationComplete(Configuration configuration) throws Exception {
                Configuration.get().getMachine().addListener(new MachineListener.Adapter() {
                    public void notifyCapture() {
                        synchronized(captureNotifier) {
                            captureNotifier.notifyAll();
                        }
                    }
                    
                    @Override
                    public void machineHeadActivity(Machine machine, Head head) {
                        notifyCapture();
                    }

                    @Override
                    public void machineEnabled(Machine machine) {
                        notifyCapture();
                    }
                });
            }
        });
    }

    @Commit
    public void commit() throws Exception {
        super.commit();
        backLightCompensation.setCamera(this);
        brightness.setCamera(this);
        contrast.setCamera(this);
        exposure.setCamera(this);
        focus.setCamera(this);
        gain.setCamera(this);
        gamma.setCamera(this);
        hue.setCamera(this);
        powerLineFrequency.setCamera(this);
        saturation.setCamera(this);
        sharpness.setCamera(this);
        whiteBalance.setCamera(this);
        zoom.setCamera(this);
    }

    public List<CaptureDevice> getCaptureDevices() {
        return capture.getDevices();
    }

    @Override
    public synchronized BufferedImage internalCapture() {
        ensureOpen();
        try {
            /**
             * The timeout is only needed if the stream is somehow in error and not producing frames (anymore) 
             * which can happen, if you disconnect the USB port and then try to capture from a pipeline.  
             */
            long timeout = System.currentTimeMillis()+500;
            while (!stream.hasNewFrame()) {
                Thread.yield();
                if (System.currentTimeMillis() > timeout) {
                    return null;
                }
            }
            BufferedImage img = stream.capture();
            /**
             * We don't ever want to "waste" an image. So even if the thread is running at a low
             * frame rate, if we've been forced to capture an image we broadcast it.
             * 
             * TODO Note that it would be better to do this in ReferenceCamera.capture(), but the
             * other camera implementations still call internalCapture() from their thread, which
             * would cause the image to broadcast twice. Eventually they should be refactored as
             * this one was, to capture frames directly in the loop rather than calling
             * internalCapture().
             * 
             * And further, note that the *reason* the thread doesn't call internalCapture() on
             * this implementation is due to the hasNewFrame() busy loop up there. We don't want
             * the thread busy looping and eating tons of CPU.
             * 
             * Also note that we have to transform here, since we're broadcasting the image
             * directly. Most of the other implementations just call captureForPreview() which
             * handles the transform, but because of the above we can't do that.
             */
            broadcastCapture(transformImage(img));
            return img;
        }
        catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public synchronized void startContinuousCapture(CameraListener listener) {
        ensureOpen();
        super.startContinuousCapture(listener);
    }

    public void run() {
        while (!Thread.interrupted()) {
            try {
                ensureOpen();
                if (stream.hasNewFrame()) {
                    BufferedImage img = stream.capture();
                    img = transformImage(img);
                    broadcastCapture(img);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            try {
                synchronized(captureNotifier) {
                    if (fps == 0) {
                        captureNotifier.wait();
                    }
                    else {
                        captureNotifier.wait((long) (1000. / fps));
                    }
                }
            }
            catch (InterruptedException e) {
                break;
            }
        }
    }
    
    public synchronized void ensureOpen() {
        if (thread == null) {
            open();
        }
    }

    public void open() {
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(3000);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            thread = null;
        }

        if (stream != null) {
            try {
                stream.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        stream = null;
        setPropertiesStream(stream);

        clearCalibrationCache();
        
        // If a device and format are not set, see if we can read them from the stored
        // properties. This will only happen during startup.
        if (device == null && format == null) {
            if (uniqueId == null) {
                return;
            }
            for (CaptureDevice device : capture.getDevices()) {
                if (device.getUniqueId().equals(uniqueId)) {
                    this.device = device;
                }
            }
            if (device == null) {
                Logger.warn("No camera found with ID {} for camera {}", uniqueId, getName());
                return;
            }

            if (formatId == null) {
                return;
            }
            for (CaptureFormat format : device.getFormats()) {
                if (format.getFormatId() == formatId) {
                    this.format = format;
                }
            }
            if (format == null) {
                Logger.warn("No format found with ID {} for camera {}", formatId, getName());
            }
        }


        if (device == null) {
            Logger.debug("open called with null device");
            return;
        }
        if (format == null) {
            Logger.debug("open called with null format");
            return;
        }

        try {
            width = null;
            height = null;

            stream = device.openStream(format);
            setPropertiesStream(stream);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    private void setPropertiesStream(CaptureStream stream) {
        backLightCompensation.setStream(stream);
        brightness.setStream(stream);
        contrast.setStream(stream);
        exposure.setStream(stream);
        focus.setStream(stream);
        gain.setStream(stream);
        gamma.setStream(stream);
        hue.setStream(stream);
        powerLineFrequency.setStream(stream);
        saturation.setStream(stream);
        sharpness.setStream(stream);
        whiteBalance.setStream(stream);
        zoom.setStream(stream);
    }

    @Override
    public void close() throws IOException {
        super.close();

        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(3000);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (stream != null) {
            try {
                stream.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        stream = null;

        capture.close();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new OpenPnpCaptureCameraConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    public CaptureDevice getDevice() {
        return device;
    }

    public void setDevice(CaptureDevice device) {
        this.device = device;
        if (device == null) {
            this.uniqueId = null;
        }
        else {
            this.uniqueId = device.getUniqueId();
        }
        firePropertyChange("device", null, device);
    }

    public CaptureFormat getFormat() {
        return format;
    }

    public void setFormat(CaptureFormat format) {
        this.format = format;
        if (format == null) {
            this.formatId = null;
        }
        else {
            this.formatId = format.getFormatId();
        }
        firePropertyChange("format", null, format);
    }

    public double getFps() {
        return fps;
    }

    public void setFps(double fps) {
        this.fps = fps;
    }

    public CapturePropertyHolder getBackLightCompensation() {
        return backLightCompensation;
    }

    public CapturePropertyHolder getBrightness() {
        return brightness;
    }

    public CapturePropertyHolder getContrast() {
        return contrast;
    }

    public CapturePropertyHolder getExposure() {
        return exposure;
    }

    public CapturePropertyHolder getFocus() {
        return focus;
    }

    public CapturePropertyHolder getGain() {
        return gain;
    }

    public CapturePropertyHolder getGamma() {
        return gamma;
    }

    public CapturePropertyHolder getHue() {
        return hue;
    }

    public CapturePropertyHolder getPowerLineFrequency() {
        return powerLineFrequency;
    }

    public CapturePropertyHolder getSaturation() {
        return saturation;
    }

    public CapturePropertyHolder getSharpness() {
        return sharpness;
    }

    public CapturePropertyHolder getWhiteBalance() {
        return whiteBalance;
    }

    public CapturePropertyHolder getZoom() {
        return zoom;
    }

    public static class CapturePropertyHolder extends AbstractModelObject {
        @Attribute(required = false)
        private CaptureProperty property;

        @Attribute(required = false)
        private Integer value;

        @Attribute(required = false)
        private Boolean auto;

        private CaptureStream stream;

        public CapturePropertyHolder(CaptureProperty property) {
            this.property = property;
        }

        public CapturePropertyHolder() {
            this(null);
        }

        public void setCamera(OpenPnpCaptureCamera camera) {
            camera.addPropertyChangeListener("device", e -> {
                firePropertyChange("supported", null, isSupported());
            });
            camera.addPropertyChangeListener("format", e -> {
                firePropertyChange("supported", null, isSupported());
            });
        }

        public void setStream(CaptureStream stream) {
            this.stream = stream;
            if (stream == null) {
                return;
            }
            if (auto != null) {
                setAuto(auto);
            }
            if (value != null) {
                setValue(value);
            }
            firePropertyChange("supported", null, isSupported());
            firePropertyChange("autoSupported", null, isAutoSupported());
            firePropertyChange("min", null, getMin());
            firePropertyChange("max", null, getMax());
            firePropertyChange("default", null, getDefault());
            firePropertyChange("value", null, getValue());
            firePropertyChange("auto", null, isAuto());
        }

        public int getMin() {
            try {
                PropertyLimits limits = stream.getPropertyLimits(property);
                return limits.getMin();
            }
            catch (Exception e) {
                return 0;
            }
        }

        public int getMax() {
            try {
                PropertyLimits limits = stream.getPropertyLimits(property);
                return limits.getMax();
            }
            catch (Exception e) {
                return 0;
            }
        }

        public int getDefault() {
            try {
                PropertyLimits limits = stream.getPropertyLimits(property);
                return limits.getDefault();
            }
            catch (Exception e) {
                return 0;
            }
        }

        public boolean isAuto() {
            try {
                return this.auto = stream.getAutoProperty(property);
            }
            catch (Exception e) {
                return false;
            }
        }

        public void setAuto(boolean auto) {
            try {
                stream.setAutoProperty(property, auto);
                this.auto = auto;
                firePropertyChange("auto", null, auto);
            }
            catch (Exception e) {
            }
        }

        public void setValue(int value) {
            try {
                stream.setProperty(property, value);
                this.value = value;
                firePropertyChange("value", null, value);
            }
            catch (Exception e) {
            }
        }

        public int getValue() {
            try {
                return this.value = stream.getProperty(property);
            }
            catch (Exception e) {
                return 0;
            }
        }

        public boolean isSupported() {
            try {
                stream.getPropertyLimits(property);
                return true;
            }
            catch (Exception e) {
                return false;
            }
        }

        public boolean isAutoSupported() {
            try {
                stream.getAutoProperty(property);
                return true;
            }
            catch (Exception e) {
                return false;
            }
        }
    }
}
