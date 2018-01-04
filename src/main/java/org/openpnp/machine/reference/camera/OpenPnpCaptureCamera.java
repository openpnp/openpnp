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

public class OpenPnpCaptureCamera extends ReferenceCamera implements Runnable {
    private OpenPnpCapture capture = new OpenPnpCapture();
    private Thread thread;
    
    private CaptureDevice device;
    private CaptureFormat format;
    private CaptureStream stream;
    
    @Attribute(required=false)
    private String uniqueId;
    
    @Attribute(required=false)
    private Integer formatId;
    
    @Attribute(required=false)
    private int fps = 10;

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
    private CapturePropertyHolder saturation = new CapturePropertyHolder(CaptureProperty.Saturation);
    
    @Element(required=false)
    private CapturePropertyHolder whiteBalance = new CapturePropertyHolder(CaptureProperty.WhiteBalance);

    @Element(required=false)
    private CapturePropertyHolder zoom = new CapturePropertyHolder(CaptureProperty.Zoom);

    public OpenPnpCaptureCamera() {
    }
    
    @Commit
    public void commit() {
        brightness.setCamera(this);
        contrast.setCamera(this);
        exposure.setCamera(this);
        focus.setCamera(this);
        gain.setCamera(this);
        gamma.setCamera(this);
        saturation.setCamera(this);
        whiteBalance.setCamera(this);
        zoom.setCamera(this);
    }
    
    public List<CaptureDevice> getCaptureDevices() {
        return capture.getDevices();
    }

    @Override
    public synchronized BufferedImage internalCapture() {
        if (thread == null) {
            open();
        }
        try {
            BufferedImage img = stream.capture();
            return transformImage(img);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public synchronized void startContinuousCapture(CameraListener listener, int maximumFps) {
        if (thread == null) {
            open();
        }
        super.startContinuousCapture(listener, maximumFps);
    }

    public void run() {
        while (!Thread.interrupted()) {
            try {
                BufferedImage image = internalCapture();
                if (image != null) {
                    broadcastCapture(image);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000 / fps);
            }
            catch (InterruptedException e) {
                break;
            }
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
        brightness.setStream(stream);
        contrast.setStream(stream);
        exposure.setStream(stream);
        focus.setStream(stream);
        gain.setStream(stream);
        gamma.setStream(stream);
        saturation.setStream(stream);
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
    
    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
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

    public CapturePropertyHolder getSaturation() {
        return saturation;
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
