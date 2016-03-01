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
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.wizards.CameraConfigurationWizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.VfwCameraConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;
import org.vonnieda.vfw.CaptureDevice;

public class VfwCamera extends ReferenceCamera implements Runnable {
    @Attribute(required = false)
    private String driver;
    @Attribute(required = false)
    private boolean showVideoSourceDialog;
    @Attribute(required = false)
    private boolean showVideoFormatDialog;
    @Attribute(required = false)
    private boolean showVideoDisplayDialog;

    private CaptureDevice captureDevice;
    private int width, height;

    private BufferedImage lastImage;

    private Object captureLock = new Object();

    private Thread captureThread;

    public VfwCamera() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {

            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                if (driver != null && driver.trim().length() != 0) {
                    setDriver(driver);
                }
            }
        });
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        if (captureThread != null) {
            captureThread.interrupt();
            try {
                captureThread.join();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.driver = driver;
        captureThread = new Thread(this);
        captureThread.start();
    }

    public boolean isShowVideoSourceDialog() {
        return showVideoSourceDialog;
    }

    public void setShowVideoSourceDialog(boolean showVideoSourceDialog) {
        this.showVideoSourceDialog = showVideoSourceDialog;
    }

    public boolean isShowVideoFormatDialog() {
        return showVideoFormatDialog;
    }

    public void setShowVideoFormatDialog(boolean showVideoFormatDialog) {
        this.showVideoFormatDialog = showVideoFormatDialog;
    }

    public boolean isShowVideoDisplayDialog() {
        return showVideoDisplayDialog;
    }

    public void setShowVideoDisplayDialog(boolean showVideoDisplayDialog) {
        this.showVideoDisplayDialog = showVideoDisplayDialog;
    }

    public List<String> getDrivers() {
        ArrayList<String> drivers = new ArrayList<>();
        try {
            for (String s : CaptureDevice.getCaptureDrivers()) {
                drivers.add(s);
            }
        }
        catch (UnsatisfiedLinkError e) {

        }
        return drivers;
    }

    public void run() {
        try {
            captureDevice = CaptureDevice.getCaptureDevice(driver);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (showVideoSourceDialog) {
            captureDevice.showVideoSourceDialog();
        }

        if (showVideoFormatDialog) {
            captureDevice.showVideoFormatDialog();
        }

        if (showVideoDisplayDialog) {
            captureDevice.showVideoDisplayDialog();
        }

        width = (int) captureDevice.getVideoDimensions().getWidth();
        height = (int) captureDevice.getVideoDimensions().getHeight();

        while (!Thread.interrupted()) {
            int[] captureData = captureDevice.captureFrame();
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            image.setRGB(0, 0, width, height, captureData, 0, width);
            lastImage = transformImage(image);
            broadcastCapture(lastImage);
            synchronized (captureLock) {
                captureLock.notify();
            }
            try {
                Thread.sleep(1000 / 30);
            }
            catch (Exception e) {
            }
        }
    }

    @Override
    public BufferedImage capture() {
        synchronized (captureLock) {
            try {
                captureLock.wait();
                BufferedImage image = lastImage;
                return image;
            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new VfwCameraConfigurationWizard(this);
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
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(new CameraConfigurationWizard(this)),
                new PropertySheetWizardAdapter(getConfigurationWizard())};
    }
}
