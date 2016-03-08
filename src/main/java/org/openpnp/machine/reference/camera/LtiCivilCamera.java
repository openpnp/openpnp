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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.wizards.CameraConfigurationWizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.LtiCivilCameraConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;

import com.lti.civil.CaptureDeviceInfo;
import com.lti.civil.CaptureException;
import com.lti.civil.CaptureObserver;
import com.lti.civil.CaptureStream;
import com.lti.civil.CaptureSystem;
import com.lti.civil.CaptureSystemFactory;
import com.lti.civil.DefaultCaptureSystemFactorySingleton;
import com.lti.civil.Image;
import com.lti.civil.VideoFormat;
import com.lti.civil.awt.AWTImageConverter;

@Deprecated
public class LtiCivilCamera extends ReferenceCamera implements CaptureObserver {
    private CaptureSystemFactory captureSystemFactory;
    private CaptureSystem captureSystem;
    private CaptureStream captureStream;
    private VideoFormat videoFormat;

    @Attribute(required = false)
    private String deviceId;
    @Attribute(required = false)
    private boolean forceGrayscale;

    private int width, height;

    private BufferedImage lastImage;

    private Object captureLock = new Object();

    public LtiCivilCamera() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {

            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                captureSystemFactory = DefaultCaptureSystemFactorySingleton.instance();
                captureSystem = captureSystemFactory.createCaptureSystem();

                if (deviceId != null && deviceId.trim().length() != 0) {
                    setDeviceId(deviceId);
                }
            }
        });
    }

    public void setDeviceId(String deviceId) throws Exception {
        if (captureStream != null) {
            captureStream.stop();
            captureStream.dispose();
        }
        captureStream = captureSystem.openCaptureDeviceStream(deviceId);
        videoFormat = captureStream.getVideoFormat();
        width = videoFormat.getWidth();
        height = videoFormat.getHeight();
        captureStream.setObserver(this);
        captureStream.start();
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public boolean isForceGrayscale() {
        return forceGrayscale;
    }

    public void setForceGrayscale(boolean forceGrayscale) {
        this.forceGrayscale = forceGrayscale;
    }

    public List<String> getDeviceIds() throws Exception {
        ArrayList<String> deviceIds = new ArrayList<>();
        for (CaptureDeviceInfo captureDeviceInfo : (List<CaptureDeviceInfo>) captureSystem
                .getCaptureDeviceInfoList()) {
            deviceIds.add(captureDeviceInfo.getDeviceID());
        }
        return deviceIds;
    }

    @Override
    public void onError(CaptureStream captureStream, CaptureException captureException) {}

    @Override
    public void onNewImage(CaptureStream captureStream, Image newImage) {
        BufferedImage bImage = AWTImageConverter.toBufferedImage(newImage);
        if (forceGrayscale) {
            BufferedImage grayImage =
                    new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            Graphics g = grayImage.getGraphics();
            g.drawImage(bImage, 0, 0, null);
            g.dispose();
            lastImage = grayImage;
        }
        else {
            lastImage = bImage;
        }
        lastImage = transformImage(lastImage);
        broadcastCapture(lastImage);
        synchronized (captureLock) {
            captureLock.notify();
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
                return null;
            }
        }
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new LtiCivilCameraConfigurationWizard(this);
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
