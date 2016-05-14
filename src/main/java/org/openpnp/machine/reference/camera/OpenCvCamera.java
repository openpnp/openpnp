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

import javax.swing.Action;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.openpnp.CameraListener;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.wizards.CameraConfigurationWizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.OpenCvCameraConfigurationWizard;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.OpenCvUtils;
import org.simpleframework.xml.Attribute;

/**
 * A Camera implementation based on the OpenCV FrameGrabbers.
 */
public class OpenCvCamera extends ReferenceCamera implements Runnable {
    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    @Attribute(name = "deviceIndex", required = true)
    private int deviceIndex = 0;

    @Attribute(required = false)
    private int preferredWidth;
    @Attribute(required = false)
    private int preferredHeight;
    @Attribute(required = false)
    private int fps = 24;

    private VideoCapture fg = new VideoCapture();
    private Thread thread;
    private boolean dirty = false;

    public OpenCvCamera() {}

    @Override
    public synchronized BufferedImage capture() {
        if (thread == null) {
            setDeviceIndex(deviceIndex);
        }
        Mat mat = new Mat();
        try {
            if (!fg.read(mat)) {
                return null;
            }
            BufferedImage img = OpenCvUtils.toBufferedImage(mat);
            return transformImage(img);
        }
        catch (Exception e) {
            return null;
        }
        finally {
            mat.release();
        }
    }

    @Override
    public synchronized void startContinuousCapture(CameraListener listener, int maximumFps) {
        if (thread == null) {
            setDeviceIndex(deviceIndex);
        }
        super.startContinuousCapture(listener, maximumFps);
    }

    public void run() {
        while (!Thread.interrupted()) {
            try {
                BufferedImage image = capture();
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

    public int getDeviceIndex() {
        return deviceIndex;
    }

    public synchronized void setDeviceIndex(int deviceIndex) {
        this.deviceIndex = deviceIndex;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            thread = null;
        }
        try {
            setDirty(false);
            width = null;
            height = null;
            fg.open(deviceIndex);
            if (preferredWidth != 0) {
                fg.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, preferredWidth);
            }
            if (preferredHeight != 0) {
                fg.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, preferredHeight);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        thread = new Thread(this);
        thread.start();
    }

    public int getPreferredWidth() {
        return preferredWidth;
    }

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = preferredWidth;
        setDirty(true);
    }

    public int getPreferredHeight() {
        return preferredHeight;
    }

    public void setPreferredHeight(int preferredHeight) {
        this.preferredHeight = preferredHeight;
        setDirty(true);
    }
    
    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new OpenCvCameraConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(new CameraConfigurationWizard(this)),
                new PropertySheetWizardAdapter(getConfigurationWizard())};
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join();
            }
            catch (Exception e) {

            }
        }
        if (fg.isOpened()) {
            fg.release();
        }
    }
}
