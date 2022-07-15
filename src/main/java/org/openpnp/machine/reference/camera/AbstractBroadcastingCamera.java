/*
 * Copyright (C) 2021 <mark@makr.zone>
 * inspired and based on work
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.openpnp.CameraListener;
import org.openpnp.ConfigurationListener;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.base.AbstractActuator;
import org.openpnp.util.MovableUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

/**
 * AbstractPreviewCamera handles the preview capture broadcasting aspects of a Camera. 
 *
 */
public abstract class AbstractBroadcastingCamera extends AbstractSettlingCamera implements Runnable {
    // Calling notifyAll on this object will wake the stream thread for one loop to broadcast
    // a new image.
    protected Object captureNotifier = new Object();

    @Attribute(required = false)
    protected double fps = 5;

    @Attribute(required = false)
    protected boolean suspendPreviewInTasks = false;

    private volatile Thread thread;

    private static BufferedImage CAPTURE_ERROR_IMAGE = null;

    /**
     * The lastTransformedImage is produced by transformImage() and consumed by the Camera thread.
     */
    private AtomicReference<BufferedImage> lastTransformedImage = new AtomicReference<>();

    volatile private boolean cameraViewDirty;

    AbstractBroadcastingCamera() {
        if (isBroadcasting()) {
            Configuration.get().addListener(new ConfigurationListener.Adapter() {
                @Override
                public void configurationComplete(Configuration configuration) throws Exception {
                    Configuration.get().getMachine().addListener(new MachineListener.Adapter() {
                        @Override
                        public void machineHeadActivity(Machine machine, Head head) {
                            if (!isPreviewSuspended()) {
                                notifyCapture();
                            }
                        }

                        @Override 
                        public void machineEnabled(Machine machine) {
                            notifyCapture();
                        }

                        @Override 
                        public void machineBusy(Machine machine, boolean busy) {
                            if (!busy) {
                                if (cameraViewDirty) {
                                    captureCameraView();
                                }
                            }
                        }

                        @Override
                        public void machineTargetedUserAction(Machine machine, HeadMountable hm, boolean jogging) {
                            // Find the nearest camera.
                            Camera nearestCamera = null;
                            if (hm instanceof Camera) {
                                // That's easy.
                                nearestCamera = (Camera) hm;
                            }
                            else if (hm != null) {
                                // This is not a Camera but it may be a camera subject. Get the nearest camera looking at it.   
                                Location location = hm.getLocation().convertToUnits(LengthUnit.Millimeters);
                                double nearestDistance = Double.POSITIVE_INFINITY;
                                for (Camera camera : machine.getCameras()) {
                                    double distance = location.getLinearDistanceTo(camera.getLocation());
                                    if (distance < 50) {
                                        // Roughly in view of the camera (50mm radius).
                                        if (distance < nearestDistance) {
                                            nearestDistance = distance;
                                            nearestCamera = camera;
                                        }
                                    }
                                }
                            }
                            if (nearestCamera == AbstractBroadcastingCamera.this) {
                                // The nearest is our camera. That's an updated view, then. 
                                cameraViewHasChanged(hm.getLocation());
                            }
                        }
                    });
                }
            });
        }
    }

    protected boolean isBroadcasting() {
        return true;
    }

    public double getPreviewFps() {
        return fps;
    }

    public void setPreviewFps(double fps) {
        Object oldValue = this.fps;
        this.fps = fps;
        firePropertyChange("previewFps", oldValue, fps);
    }

    public boolean isSuspendPreviewInTasks() {
        return suspendPreviewInTasks;
    }

    public void setSuspendPreviewInTasks(boolean suspendPreviewInTasks) {
        Object oldValue = this.suspendPreviewInTasks;
        this.suspendPreviewInTasks = suspendPreviewInTasks;
        firePropertyChange("suspendPreviewInTasks", oldValue, suspendPreviewInTasks);
    }

    protected Thread getThread() {
        return thread;
    }

    protected BufferedImage getLastTransformedImage() {
        return lastTransformedImage.get();
    }

    protected void setLastTransformedImage(BufferedImage lastTransformedImage) {
        this.lastTransformedImage.set(lastTransformedImage);
        notifyCapture();
    }

    protected void notifyCapture() {
        synchronized(captureNotifier) {
            captureNotifier.notifyAll();
        }
    }

    /**
     * Whenever a user action deliberately changes the Camera view via its position, subject, or other action,
     * this method should be called to trigger a new image capture. 
     * If the camera is set to 0 fps or otherwise not continuously capturing, this will generate an updated 
     * camera view (subject to configuration and other constraints).
     *
     * @param location Location at which the view is targeted.
     *  
     */
    public void cameraViewHasChanged(Location location) {
        cameraViewDirty = true;
        notifyCapture();
        if (isAutoVisible()) {
            ensureCameraVisible();
        }
    }

    public synchronized static BufferedImage getCaptureErrorImage() {
        if (CAPTURE_ERROR_IMAGE == null) {
            CAPTURE_ERROR_IMAGE = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) CAPTURE_ERROR_IMAGE.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.darkGray);
            g.fillRect(0, 0, 640, 480);
            g.setColor(Color.red);
            g.setStroke(new BasicStroke(20));
            g.drawLine(20, 20, 100, 100);
            g.drawLine(20, 100, 100, 20);
            g.dispose();
        }
        return CAPTURE_ERROR_IMAGE;
    }

    protected void captureCameraView() {
        try {
            try {
                if (isUserActionLightOn()) {
                    Actuator lightActuator = getLightActuator();
                    if (lightActuator != null) {
                        AbstractActuator.assertOnOffDefined(lightActuator);
                        actuateLight(lightActuator, lightActuator.getDefaultOnValue());
                    }
                }
                if (getPreviewFps() == 0.0) {
                    broadcastCapture(settleAndCapture());
                }
            }
            catch (Exception e) {
                Logger.error(e);
            }
        }
        finally {
            cameraViewDirty = false;
        }
    }

    protected void broadcastCapture(BufferedImage img) {
        for (ListenerEntry listener : new ArrayList<>(listeners)) {
            listener.listener.frameReceived(img);
        }
    }

    @Override
    public synchronized void startContinuousCapture(CameraListener listener) {
        ensureOpen();
        super.startContinuousCapture(listener);
    }

    @Override
    public synchronized void stopContinuousCapture(CameraListener listener) {
        super.stopContinuousCapture(listener);
        if (listeners.size() == 0) {
            stop();
        }
    }

    /**
     * New settings applied to the Camera are activated by reopening the camera.   
     * 
     * @throws Exception
     */
    public synchronized void reinitialize() throws Exception {
        stop();
        ensureOpen();
        MovableUtils.fireTargetedUserAction(this);
    }

    protected synchronized void stop() {
        if (isOpen()) {
            thread.interrupt();
            try {
                thread.join(200);
            }
            catch (Exception e) {
            }
        }
        thread = null;
    }

    protected synchronized boolean isOpen() {
        return (thread != null && thread.isAlive());
    }

    protected synchronized boolean ensureOpen() {
        if (Configuration.get().getMachine() == null) {
            // For some reason, in some test units, but only when run trough mvn test but not with the debugger, 
            // we get calls to internalCapture() with an uninitialized Machine. 
            return false;
        }
        if (!isOpen()) {
            try {
                open();
            }
            catch (Exception e) {
                Logger.error(e);
                return false;
            }
        }
        return true;
    }

    public synchronized void open() throws Exception {
        stop();
        start();
    }

    @Override
    public void close() throws IOException {
        stop();
    }

    protected synchronized void start() {
        if (!isOpen()) {
            thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }
    }

    @Override
    public void run() {
        Logger.trace("Camera "+getName()+" thread "+Thread.currentThread().getId()+" started.");
        while (!Thread.interrupted()) {
            if (thread == null
                    ||thread.getId()!= Thread.currentThread().getId()) {
                // The interrupt must have missed. We're not the running thread.
                Logger.trace("Camera "+getName()+" thread "+Thread.currentThread().getId()+" interrupt failed, closing anyway.");
                break;
            }
            try {
                // The camera should reuse images recently captures by on-going computer vision as 
                // every call to captureTransformed() may consume the frame and make it unavailable 
                // to computer vision.  
                // Note, by using the atomic getAndSet() we make sure not to miss the last image.
                BufferedImage img = lastTransformedImage.getAndSet(null);
                if (img == null && !isPreviewSuspended()) {
                    if (hasNewFrame()){
                        // None available, try capture a new frame.
                        captureTransformed();
                        // Void the last image, so a new one will be triggered next time.
                        img = lastTransformedImage.getAndSet(null);
                    }
                }
                if (img != null) {
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
        Logger.trace("Camera "+getName()+" thread "+Thread.currentThread().getId()+" bye-bye.");
    }

    public boolean isPreviewSuspended() {
        if (cameraViewDirty) {
            return false;
        }
        return (suspendPreviewInTasks && Configuration.get().getMachine().isBusy());
    }
}
