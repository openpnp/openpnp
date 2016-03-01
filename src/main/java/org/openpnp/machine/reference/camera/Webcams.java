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

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.openpnp.CameraListener;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.wizards.CameraConfigurationWizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.WebcamConfigurationWizard;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamImageTransformer;
import com.github.sarxos.webcam.util.jh.JHGrayFilter;



/**
 * A Camera implementation based on the OpenCV FrameGrabbers.
 */
public class Webcams extends ReferenceCamera implements Runnable, WebcamImageTransformer {

    @Attribute(required = false)
    protected String deviceId = "###DEVICE###";

    @Attribute(required = false)
    private int preferredWidth = 0;
    @Attribute(required = false)
    private int preferredHeight = 0;

    protected Webcam webcam;
    private Thread thread;
    private boolean forceGray;
    private BufferedImage image;

    private static final JHGrayFilter GRAY = new JHGrayFilter();


    @Override
    public BufferedImage transform(BufferedImage image) {
        return GRAY.filter(image, null);
    }

    public Webcams() {

    }

    @Override
    public synchronized BufferedImage capture() {
        if (thread == null) {
            setDeviceId(deviceId);
        }
        if (thread == null) {
            return null;
        }
        try {
            BufferedImage img = webcam.getImage();
            return transformImage(img);
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    public synchronized void startContinuousCapture(CameraListener listener, int maximumFps) {
        if (thread == null) {
            setDeviceId(deviceId);
        }
        super.startContinuousCapture(listener, maximumFps);
    }

    private BufferedImage lastImage = null;
    private BufferedImage redImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);


    public void run() {
        while (!Thread.interrupted()) {
            try {
                BufferedImage image = capture();
                if (image == null) {
                    image = redImage;
                }
                broadcastCapture(image);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000 / 30);
            }
            catch (InterruptedException e) {
                break;
            }
        }
    }

    public String getDeviceId() {
        return deviceId;
    }

    public synchronized void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            thread = null;
            webcam.close();
        }
        try {
            webcam = null;
            for (Webcam cam : Webcam.getWebcams()) {
                if (cam.getName().equals(deviceId)) {
                    webcam = cam;
                }
            }
            if (webcam == null) {
                return;
            }
            if (preferredWidth != 0 && preferredHeight != 0) {
                webcam.setViewSize(new Dimension(preferredWidth, preferredHeight));
            }
            webcam.open();
            if (forceGray) {
                webcam.setImageTransformer(this);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        thread = new Thread(this);
        thread.start();
    }

    public void setForceGray(boolean val) {
        forceGray = val;
    }

    public boolean isForceGray() {
        return forceGray;
    }

    public int getPreferredWidth() {
        return preferredWidth;
    }

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = preferredWidth;
    }

    public int getPreferredHeight() {
        return preferredHeight;
    }

    public void setPreferredHeight(int preferredHeight) {
        this.preferredHeight = preferredHeight;
    }


    @Override
    public Wizard getConfigurationWizard() {
        return new WebcamConfigurationWizard(this);
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


    public List<String> getDeviceIds() throws Exception {
        ArrayList<String> deviceIds = new ArrayList<>();
        for (Webcam cam : Webcam.getWebcams()) {
            deviceIds.add(cam.getName());
        }
        return deviceIds;
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
            webcam.close();
        }
    }
}
