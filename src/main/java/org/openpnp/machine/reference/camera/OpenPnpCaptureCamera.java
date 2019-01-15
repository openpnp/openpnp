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
import java.util.HashMap;
import java.util.List;

import org.openpnp.CameraListener;
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
import org.openpnp.spi.PropertySheetHolder;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

public class OpenPnpCaptureCamera extends ReferenceCamera {
    private OpenPnpCapture capture = new OpenPnpCapture();
    
    private CaptureDevice device;
    private CaptureFormat format;
    private CaptureStream stream;
    
    @Attribute(required=false)
    private String uniqueId;
    
    @Attribute(required=false)
    private Integer formatId;
    
    @Attribute(required=false)
    @Deprecated
    private Integer fps = null;

    @Element(required=false)
    private CapturePropertyHolder backLightCompensation = new CapturePropertyHolder(CaptureProperty.BackLightCompensation);

    @Element(required=false)
    private CapturePropertyHolder brightness = new CapturePropertyHolder(CaptureProperty.Brightness);

    @Element(required=false)
    private CapturePropertyHolder contrast = new CapturePropertyHolder(CaptureProperty.Contrast);

    @Element(required=false)
    private CapturePropertyHolder exposure = new CapturePropertyHolder(CaptureProperty.Exposure);

    @Element(required=false)
    private CapturePropertyHolder focus = new CapturePropertyHolder(CaptureProperty.Focus);

    @Element(required=false)
    private CapturePropertyHolder gain = new CapturePropertyHolder(CaptureProperty.Gain);
    
    @Element(required=false)
    private CapturePropertyHolder gamma = new CapturePropertyHolder(CaptureProperty.Gamma);
    
    @Element(required=false)
    private CapturePropertyHolder hue = new CapturePropertyHolder(CaptureProperty.Hue);

    @Element(required=false)
    private CapturePropertyHolder powerLineFrequency = new CapturePropertyHolder(CaptureProperty.PowerLineFrequency);

    @Element(required=false)
    private CapturePropertyHolder saturation = new CapturePropertyHolder(CaptureProperty.Saturation);
    
    @Element(required=false)
    private CapturePropertyHolder sharpness = new CapturePropertyHolder(CaptureProperty.Sharpness);

    @Element(required=false)
    private CapturePropertyHolder whiteBalance = new CapturePropertyHolder(CaptureProperty.WhiteBalance);

    @Element(required=false)
    private CapturePropertyHolder zoom = new CapturePropertyHolder(CaptureProperty.Zoom);
    
    HashMap<CameraListener, CaptureWorker> workers = new HashMap<CameraListener, CaptureWorker>();

    public OpenPnpCaptureCamera() {
    }
    
    @Commit
    public void commit() {
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
             * If there is already a frame buffered, we don't want it, so we take whatever is
             * buffered and toss it. This ensures hasNewFrame gets reset so that we can wait
             * on the frame to arrive. Note that because hasNewframe, capture, and submitBuffer
             * all lock on the same buffer there is the chance that we throw away a buffer
             * that was just captured, but this is acceptable. We should only be blocked
             * for the time it takes to copy the memory, which should be well under 1ms
             * and then we are blocked for the time to capture a frame, which we expected
             * anyway. 
             * 
             * So, this is all basically working now. But if there are multiple listeners they
             * are gonna fight for images. We only ever have one so who cares, but maybe we care?
             * 
             * Also, it seems like the old FPS setting was just fine, based on all this?
             * 
             * TODO STOPSHIP Can we actually just stream 30 FPS to the screen without blowing
             * up the CPU? Check and see. And if not, why? And also if not, can other programs?
             * 
             * From measurements:
             * hasNewFrame/capture takes ~5
             * !hasNewFrame takes ~20
             * capture takes ~3
             * transform takes ~0
             * 
             * and in the thread, capture takes about 30 and callback takes 0
             * which makes sense, cause the callback just stores the image and
             * calls repaint, which happens on a different thread
             * 
             * 15 FPS is about 50-55% CPU
             * Turning off frameReceived saves about 10%
             * 1 degree of rotation adds about 5%
             * undistort adds about 10%
             * flip adds maybe 2%
             * 
             * OBS uses about 13% to stream 30 FPS, but no effects at all
             * Still, like a quarter of what we use
             * 
             * Turning off the first capture and busy loop gets down to about 34%
             * Turn off transform and frameReceived gets down to about 20.
             */
            
            if (stream.hasNewFrame()) {
                stream.capture();
            }
            
            while (!stream.hasNewFrame()) {
            }
            
            BufferedImage img = stream.capture();
            
            img = transformImage(img);

            return img;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }
    
    private synchronized void ensureOpen() {
        if (stream == null) {
            open();
        }
    }
    
    class CaptureWorker implements Runnable {
        final CameraListener listener;
        final int maximumFps;
        final Thread thread;
        
        public CaptureWorker(CameraListener listener, int maximumFps) {
            this.listener = listener;
            this.maximumFps = maximumFps;
            thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }
        
        public void run() {
            while (!Thread.interrupted()) {
                ensureOpen();
                
                BufferedImage img = internalCapture();
                
                listener.frameReceived(img);
                
                try {
                    Thread.sleep((long) (1000. / maximumFps));
                }
                catch (InterruptedException e) {
                    break;
                }
            }
        }
        
        public void stop() {
            thread.interrupt();
            try {
                thread.join();
            }
            catch (Exception e) {
                
            }
        }
    }

    @Override
    public synchronized void startContinuousCapture(CameraListener listener, int maximumFps) {
        System.out.println("start " + listener + " " + maximumFps);
        CaptureWorker worker = workers.get(listener);
        if (worker != null) {
            worker.stop();
        }
        worker = new CaptureWorker(listener, maximumFps);
        workers.put(listener, worker);
    }
    
    @Override
    public void stopContinuousCapture(CameraListener listener) {
        System.out.println("stop " + listener);
        super.stopContinuousCapture(listener);
        CaptureWorker worker = workers.get(listener);
        if (worker != null) {
            worker.stop();
        }
        workers.remove(listener);
    }

    public synchronized void open() {
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
    }
    
    private synchronized void setPropertiesStream(CaptureStream stream) {
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
    public synchronized void close() throws IOException {
        super.close();
        
        for (CaptureWorker worker : workers.values()) {
            worker.stop();
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
        @Attribute(required=false)
        private CaptureProperty property;

        @Attribute(required=false)
        private Integer value;

        @Attribute(required=false)
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
