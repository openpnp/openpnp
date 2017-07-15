/*
 * Copyright (C) 2016 Jason von Nieda <jason@vonnieda.org>
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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.xml.soap.SOAPException;

import org.onvif.ver10.device.wsdl.GetDeviceInformationResponse;
import org.onvif.ver10.schema.JpegOptions;
import org.onvif.ver10.schema.Profile;
import org.onvif.ver10.schema.VideoEncoderConfiguration;
import org.onvif.ver10.schema.VideoEncoderConfigurationOptions;
import org.onvif.ver10.schema.VideoEncoding;
import org.onvif.ver10.schema.VideoRateControl;
import org.onvif.ver10.schema.VideoResolution;
import org.openpnp.CameraListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.OnvifIPCameraConfigurationWizard;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;

import de.onvif.soap.OnvifDevice;
import de.onvif.soap.devices.InitialDevices;
import de.onvif.soap.devices.MediaDevices;

/**
 * A Camera implementation for ONVIF compatible IP cameras.
 */
public class OnvifIPCamera extends ReferenceCamera implements Runnable {

    @Attribute(required = false)
    private String preferredResolution;
    @Attribute(required = false)
    private int resizeWidth;
    @Attribute(required = false)
    private int resizeHeight;

    @Attribute(required = false)
    private int fps = 10;

    @Attribute(required = false)
    private String hostIP;
    @Attribute(required = false)
    private String username;
    @Attribute(required = false)
    private String password;

    private Thread thread;
    private boolean dirty = false;

    private OnvifDevice nvt;
    private URL snapshotURI;

    public OnvifIPCamera() {}

    @Override
    public BufferedImage internalCapture() {
        if (thread == null) {
            initCamera();
        }
        try {
            if (snapshotURI == null) {
                return null;
            }
            BufferedImage img = ImageIO.read(snapshotURI);
            return transformImage(resizeImage(img));
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private BufferedImage resizeImage(BufferedImage src) {
        int imgW = src.getWidth();
        int imgH = src.getHeight();
        if (resizeWidth != 0) {
            imgW = resizeWidth;
        }
        if (resizeHeight != 0) {
            imgH = resizeHeight;
        }

        if ((imgW != src.getWidth()) || (imgH != src.getHeight())) {
            Image tmp = src.getScaledInstance(imgW, imgH, Image.SCALE_SMOOTH);
            BufferedImage dst = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);

            Graphics2D g2d = dst.createGraphics();
            g2d.drawImage(tmp, 0, 0, null);
            g2d.dispose();

            return dst;
        }

        return src;
    }

    @Override
    public synchronized void startContinuousCapture(CameraListener listener, int maximumFps) {
        if (thread == null) {
            initCamera();
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

    private Profile findJPEGProfile(InitialDevices devices) throws Exception {
        List<Profile> profiles = devices.getProfiles();

        for (Profile profile : profiles) {
            VideoEncoderConfiguration videoEncoderConfiguration = profile.getVideoEncoderConfiguration();
            VideoEncoding videoEncoding = videoEncoderConfiguration.getEncoding();
            if (videoEncoding == VideoEncoding.JPEG) {
                return profile;
            }
        }

        throw new Exception("No JPEG profiles available for camera at " + hostIP);
    }

    private void initCamera() {
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
        try {
            setDirty(false);
            width = null;
            height = null;
            nvt = null;
            snapshotURI = null;

            if ((hostIP != null) && (!hostIP.isEmpty())) {
                try {
                    if ((username != null) && (!username.isEmpty())) {
                        nvt = new OnvifDevice(hostIP, username, password);
                    }
                    else {
                        nvt = new OnvifDevice(hostIP);
                    }

                    InitialDevices devices = nvt.getDevices();
                    GetDeviceInformationResponse deviceInformation = devices.getDeviceInformation();
                    System.out.println("Camera " + hostIP);
                    System.out.println("    Manufacturer    : " + deviceInformation.getManufacturer());
                    System.out.println("    Model           : " + deviceInformation.getModel());
                    System.out.println("    Serial Number   : " + deviceInformation.getSerialNumber());
                    System.out.println("    Hardware ID     : " + deviceInformation.getHardwareId());
                    System.out.println("    Firmware Version: " + deviceInformation.getFirmwareVersion());

                    MediaDevices media = nvt.getMedia();
                    Profile profile = findJPEGProfile(devices);
                    String profileToken = profile.getToken();

                    VideoEncoderConfigurationOptions videoEncoderConfigurationOptions = media
                            .getVideoEncoderConfigurationOptions(profileToken);
                    JpegOptions jpegOptions = videoEncoderConfigurationOptions.getJPEG();
                    List<VideoResolution> jpegResolutions = jpegOptions.getResolutionsAvailable();
                    int maxRes = -1;
                    int selectedResolutionIndex = -1;
                    // Step 1: Select the highest resolution available
                    for (int i = 0; i < jpegResolutions.size(); i++) {
                        VideoResolution jpegResolution = jpegResolutions.get(i);

                        int res = jpegResolution.getWidth() * jpegResolution.getHeight();
                        if (res > maxRes) {
                            maxRes = res;
                            selectedResolutionIndex = i;
                        }
                    }
                    // Step 2: If there's a preferredResolution specified, select that (if it
                    // exists) instead
                    if ((preferredResolution != null) && (!preferredResolution.isEmpty())) {
                        for (int i = 0; i < jpegResolutions.size(); i++) {
                            VideoResolution jpegResolution = jpegResolutions.get(i);
                            String strRes = jpegResolution.getWidth() + "x" + jpegResolution.getHeight();

                            if (strRes.equalsIgnoreCase(preferredResolution)) {
                                selectedResolutionIndex = i;
                                break;
                            }
                        }
                    }

                    if (selectedResolutionIndex >= 0) {
                        VideoEncoderConfiguration videoEncoderConfiguration = profile.getVideoEncoderConfiguration();

                        VideoResolution jpegResolution = jpegResolutions.get(selectedResolutionIndex);
                        videoEncoderConfiguration.setResolution(jpegResolution);
                        System.out.println(
                                " -> Selected " + jpegResolution.getWidth() + "x" + jpegResolution.getHeight());

                        videoEncoderConfiguration
                                .setQuality(videoEncoderConfigurationOptions.getQualityRange().getMax());
                        VideoRateControl videoRateControl = videoEncoderConfiguration.getRateControl();
                        videoRateControl.setFrameRateLimit(jpegOptions.getFrameRateRange().getMax());
                        videoRateControl.setEncodingInterval(jpegOptions.getEncodingIntervalRange().getMin());
                        videoRateControl.setBitrateLimit(
                                videoEncoderConfigurationOptions.getExtension().getJPEG().getBitrateRange().getMax());
                        videoEncoderConfiguration.setRateControl(videoRateControl);

                        profile.setVideoEncoderConfiguration(videoEncoderConfiguration);
                        media.setVideoEncoderConfiguration(videoEncoderConfiguration);
                    }

                    snapshotURI = new URL(media.getSnapshotUri(profileToken));
                    System.out.println("Snapshot URI: " + snapshotURI.toString());
                }
                catch (ConnectException e) {
                    System.err.println("Could not connect to IP camera at " + hostIP + ": " + e.toString());
                    e.printStackTrace();
                }
                catch (SOAPException e) {
                    System.err.println("Error communicating with IP camera at " + hostIP + ": " + e.toString());
                    e.printStackTrace();
                }
                catch (MalformedURLException e) {
                    System.err.println("Malformed URL for IP camera at " + hostIP + ": " + e.toString());
                    e.printStackTrace();
                }
                catch (Exception e) {
                    System.err.println("Unknown error initializing IP camera at " + hostIP + ": " + e.toString());
                    e.printStackTrace();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
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

            }
        }
    }

    public List<VideoResolution> getSupportedResolutions() {
        if (thread == null) {
            initCamera();
        }
        if (nvt == null) {
            return null;
        }

        try {
            InitialDevices devices = nvt.getDevices();
            MediaDevices media = nvt.getMedia();
            Profile profile = findJPEGProfile(devices);
            String profileToken = profile.getToken();

            VideoEncoderConfigurationOptions videoEncoderConfigurationOptions = media
                    .getVideoEncoderConfigurationOptions(profileToken);
            JpegOptions jpegOptions = videoEncoderConfigurationOptions.getJPEG();
            List<VideoResolution> jpegResolutions = jpegOptions.getResolutionsAvailable();

            return jpegResolutions;
        }
        catch (ConnectException e) {
            System.err.println("Could not connect to IP camera at " + hostIP + ": " + e.toString());
            e.printStackTrace();
            return null;
        }
        catch (SOAPException e) {
            System.err.println("Error communicating with IP camera at " + hostIP + ": " + e.toString());
            e.printStackTrace();
            return null;
        }
        catch (Exception e) {
            System.err.println("Unknown error communicating with IP camera at " + hostIP + ": " + e.toString());
            e.printStackTrace();
            return null;
        }
    }

    public String getHostIP() {
        return hostIP;
    }

    public synchronized void setHostIP(String hostIP) {
        this.hostIP = hostIP;
        setDirty(true);

        initCamera();
    }

    public String getUsername() {
        return username;
    }

    public synchronized void setUsername(String username) {
        this.username = username;
        setDirty(true);
    }

    public String getPassword() {
        return password;
    }

    public synchronized void setPassword(String password) {
        this.password = password;
        setDirty(true);
    }

    public String getPreferredResolution() {
        return preferredResolution;
    }

    public void setPreferredResolution(String preferredResolution) {
        this.preferredResolution = preferredResolution;
        setDirty(true);
    }

    public int getResizeWidth() {
        return resizeWidth;
    }

    public void setResizeWidth(int resizeWidth) {
        this.resizeWidth = resizeWidth;
    }

    public int getResizeHeight() {
        return resizeHeight;
    }

    public void setResizeHeight(int resizeHeight) {
        this.resizeHeight = resizeHeight;
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
        return new OnvifIPCameraConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }
}
