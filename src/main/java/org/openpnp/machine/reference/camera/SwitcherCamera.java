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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.SwitcherCameraConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractActuator;
import org.simpleframework.xml.Attribute;

public class SwitcherCamera extends ReferenceCamera {
    @Attribute(required=false)
    private int switcher = 0;
    
    @Attribute(required=false)
    private String cameraId;

    @Attribute(required=false)
    private String actuatorId;
    
    @Attribute(required=false)
    private double actuatorDoubleValue;
    
    @Attribute(required=false)
    private long actuatorDelayMillis = 500;
    
    private static Map<Integer, Camera> switchers = new HashMap<>();
    
    protected int getCaptureTryCount() {
        return 1;
    }

    @Override
    public synchronized BufferedImage internalCapture() {
        if (!ensureOpen()) {
            return null;
        }
        synchronized (switchers) {
            if (switchers.get(switcher) != this) {
                try {
                    // Make sure this happens within a machine task, but wait for it.
                    Camera switchedCamera = Configuration.get().getMachine().execute(() -> {
                            getActuator().actuate(actuatorDoubleValue);
                            return this;
                        }, true, 0); // execute only if the Machine is enabled and with zero timeout if it is busy.
                    if (this != switchedCamera) {
                        return null;
                    }
                    Thread.sleep(actuatorDelayMillis);
                    switchers.put(switcher, this);
                }
                catch (TimeoutException e) {
                    // If the machine is busy we can't switch, so we should return a null image.
                    return null;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
        // Note, the target camera is actually a capture device with multiple analog cameras connected via multiplexer. 
        // Each analog camera can have a different lens attached and may be subject to different mounting imperfections, 
        // therefore each SwitcherCamera must have its own set of lens calibration and transforms. 
        // The target camera device however must not apply any calibration or transform, hence the raw capture.  
        return getCamera().captureRaw();
    }

    @Override
    public boolean hasNewFrame() {
        if (!isOpen()) {
            return false;
        }
        synchronized (switchers) {
            if (switchers.get(switcher) != this) {
                // Always assume an off-switched camera has a new frame.
                return true;
            }
        }
        return getCamera().hasNewFrame();
    }

    @Override
    protected synchronized boolean ensureOpen() {
        if (getCamera() == null || getActuator() == null) {
            return false;
        }
        return super.ensureOpen();
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new SwitcherCameraConfigurationWizard(this);
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
        firePropertyChange("cameraId", null, cameraId);
    }

    public String getActuatorId() {
        return actuatorId;
    }

    public void setActuatorId(String actuatorId) {
        this.actuatorId = actuatorId;
        firePropertyChange("actuatorId", null, actuatorId);
    }
    
    public double getActuatorDoubleValue() {
        return actuatorDoubleValue;
    }

    public void setActuatorDoubleValue(double actuatorDoubleValue) {
        this.actuatorDoubleValue = actuatorDoubleValue;
        firePropertyChange("actuatorDoubleValue", null, actuatorDoubleValue);
    }

    public long getActuatorDelayMillis() {
        return actuatorDelayMillis;
    }

    public void setActuatorDelayMillis(long actuatorDelayMillis) {
        this.actuatorDelayMillis = actuatorDelayMillis;
        firePropertyChange("actuatorDelayMillis", null, actuatorDelayMillis);
    }

    public int getSwitcher() {
        return switcher;
    }

    public void setSwitcher(int switcher) {
        this.switcher = switcher;
        firePropertyChange("switcher", null, switcher);
    }

    public Camera getCamera() {
        return Configuration.get().getMachine().getCamera(cameraId);
    }

    public void setCamera(Camera camera) {
        if (camera == null) {
            setCameraId(null);
        }
        else {
            setCameraId(camera.getId());
        }
        firePropertyChange("camera", null, camera);
    }

    public Actuator getActuator() {
        Actuator actuator = Configuration.get().getMachine().getActuator(actuatorId); 
        AbstractActuator.suggestValueType(actuator, Actuator.ActuatorValueType.Double);
        return actuator;
    }

    public void setActuator(Actuator actuator) {
        if (actuator == null) {
            setActuatorId(null);
        }
        else {
            setActuatorId(actuator.getId());
        }
        firePropertyChange("actuator", null, actuator);
    }
}
