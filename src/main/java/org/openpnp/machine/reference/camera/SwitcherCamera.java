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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.camera.wizards.SwitcherCameraConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Machine;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractActuator;
import org.pmw.tinylog.Logger;
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

    private static ReentrantLock switchingLock = new ReentrantLock();
    private static Map<Integer, Camera> switchers = new HashMap<>();
    
    protected int getCaptureTryCount() {
        return 1;
    }

    @Override
    public void notifyCapture() {
        if (switchingLock.tryLock()) {
            // Got the switching lock immediately.
            try {
                if (switchingLock.getHoldCount() > 1) {
                    // Prevent reentrance. See the end of this function for comments. 
                    return;
                }
                if (switchers.get(switcher) != this) {
                    // If we're in a machine task already, take the opportunity to switch the camera over, if necessary.
                    // Note if we'd just let the notified camera thread do the actuator switching, it would timeout and 
                    // produce an ugly error frame, because our machine thread is likely still running.
                    Machine machine = Configuration.get().getMachine();
                    if (machine.isTask(Thread.currentThread())) {
                        // Need to switch the camera over, provoke by capturing. 
                        captureTransformed();
                        // Note, captureTransformed() will do another notifyCapture() for us when the 
                        // frame was captured. Therefore return without super call.
                        return;
                    }
                }
                // Do the actual notify.
                super.notifyCapture();
            }
            finally {
                // Never forget to unlock.
                switchingLock.unlock();
            }
        }
        // When we do not get the switching lock immediately and non-reentrantly, swallow the notifyCapture(), 
        // it would potentially create endless recursion. The notifyCapture() will later be done by setLastTransformedImage() 
        // when the current holder of the lock is done. 
        // Note that notifications from functional (machine task) captures will always already own the (reentrant) 
        // switchingLock from internalCapture() and therefore never be in this situation. Conversely, for mere "refresh" 
        // captures it is ok to drop frames, even for different cameras.

        // Therefore return without super call.
    }

    @Override
    public synchronized BufferedImage internalCapture() {
        if (!ensureOpen()) {
            return null;
        }
        try {
            if (switchingLock.tryLock(actuatorDelayMillis, TimeUnit.MILLISECONDS)) {
                try {
                    if (switchers.get(switcher) != this) {
                        // The switching is subject to fail, so make the state indeterminate.
                        switchers.put(switcher, null);
                        // Make sure actuator switching happens within a machine task, but wait for it.
                        Camera switchedCamera = Configuration.get().getMachine().execute(() -> {
                            getActuator().actuate(actuatorDoubleValue);
                            return this;
                        }, 
                                true,                   // Execute only if the Machine is enabled. 
                                actuatorDelayMillis,    // Max wait for machine to be idle.
                                actuatorDelayMillis     // Max wait for it to finish, which prevents deadlock through static SwitcherCamera.switchers. 
                                // Note, because of the switcher actuator and machine task involvement, deadlocks cannot be excluded 
                                // in general .
                                );
                        if (this != switchedCamera) {
                            return null;
                        }
                        Thread.sleep(actuatorDelayMillis);
                        // Succeeded, set the new state.
                        switchers.put(switcher, this);
                    }
                    // Note, the target camera is actually a capture device with multiple analog cameras connected via multiplexer. 
                    // Each analog camera can have a different lens attached and may be subject to different mounting imperfections, 
                    // therefore each SwitcherCamera must have its own set of lens calibration and transforms. 
                    // The target camera device however must not apply any calibration or transform, hence the raw capture.  
                    return getCamera().captureRaw();
                }
                catch (TimeoutException e) {
                    // If the machine is busy we can't switch, so we should return a null image.
                    Logger.debug("SwitcherCamera "+getName()+" failed to actuate "+getActuator().getName()+" with timeout.");
                    return null;
                }
                catch (Exception e) {
                    Logger.warn(e, "SwitcherCamera "+getName()+" failed to actuate "+getActuator().getName()+" with exception.");
                    return null;
                }
                finally {
                    // Never forget to unlock.
                    switchingLock.unlock();
                }
            }
            else {
                Logger.debug("SwitcherCamera "+getName()+" failed to obtain switcher lock within timeout.");
                return null;
            }
        }
        catch (InterruptedException e) {
            Logger.debug("SwitcherCamera "+getName()+" was interrupted trying to obtain switcher lock.");
            return null;
        }
    }

    @Override
    public boolean hasNewFrame() {
        if (!isOpen()) {
            return false;
        }
        if (switchingLock.tryLock()) {
            // Got the switching lock immediately.
            try {
                Camera switchedCamera = switchers.get(switcher);
                if (switchedCamera != this && switchedCamera != null) {
                    // Always assume an off-switched camera has a new frame.
                    return false;
                }
                return getCamera().hasNewFrame();
            }
            finally {
                // Never forget to unlock.
                switchingLock.unlock();
            }
        }
        // Always assume a camera that is currently switching has no new frame (yet).
        return false;
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
