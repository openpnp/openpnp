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

package org.openpnp.machine.neoden4;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.soap.SOAPException;

import com.mashape.unirest.http.Unirest;

import org.apache.http.client.utils.URIBuilder;
import org.openpnp.CameraListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.neoden4.wizards.Neoden4CameraConfigurationWizard;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;
import org.pmw.tinylog.Logger;

/**
 * A Camera implementation for ONVIF compatible IP cameras.
 */
public class Neoden4Camera extends ReferenceCamera implements Runnable {
    @Attribute(required = true)
    private String hostIP = "127.0.0.1";
    @Attribute(required = true)
    private int cameraId = 1;
    @Attribute(required = true)
    private int hostPort = 8080;
    @Attribute(required = false)
    private int fps = 1;

    @Attribute(required = false)
    private int width = 1024;
    @Attribute(required = false)
    private int height = 1024;
    @Attribute(required = false)
    private int timeout = 1000;

    @Attribute(required = false)
    private int exposure = 25;
    @Attribute(required = false)
    private int gain = 8;
    @Attribute(required = false)
    private int shiftX = 0;
    @Attribute(required = false)
    private int shiftY = 0;

    private Thread thread;
    private boolean dirty = false;

    //private String baseURL = "http://{hostname}:{hostport}/cameras/{cameraid}/{func}";
    private URL snapshotURI;
    private java.net.URI baseURI;

    public Neoden4Camera() {
    }

    @Override
    public BufferedImage internalCapture() {
        //Logger.trace(String.format("internalCapture() [cameraId:%d]", cameraId));
        if (thread == null) {
            initCamera();
        }
        try {
            if (snapshotURI == null) {
                return null;
            }
            BufferedImage img = ImageIO.read(snapshotURI);
            return img;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public synchronized void startContinuousCapture(CameraListener listener) {
        Logger.trace(String.format("startContinuousCapture() [cameraId:%d]", cameraId));
        if (thread == null) {
            initCamera();
        }
        super.startContinuousCapture(listener);
    }

    public void run() {
        Logger.trace("run()");
        if (fps==0) {
            fps=1;
        }

        while (!Thread.interrupted()) {
            try {
                BufferedImage image = internalCapture();
                if (image != null) { 
                    broadcastCapture(captureForPreview());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000 / fps);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private URL getImageReadAsyURL() throws MalformedURLException, URISyntaxException {
        Logger.trace(String.format("getImageReadAsyURL() [cameraId:%d, width:%d, height:%d, timeout:%d]", 
            cameraId, width, height, timeout));
        return new URIBuilder(baseURI)
            .setPath(baseURI.getPath() + "imgReadAsy")
            .setParameter("width", String.valueOf(width))
            .setParameter("height", String.valueOf(height))
            .setParameter("timeout", String.valueOf(timeout))
            .build()
            .toURL();
    }

    private void setCameraWidthHeight() {
        Logger.trace(String.format("setCameraWidthHeight() [cameraId:%d, width:%d, height:%d]", cameraId, width, height));
        URL funcUrl;
        try {
            funcUrl = new URIBuilder(baseURI)
                .setPath(baseURI.getPath() + "imgSetWidthHeight")
                .setParameter("width", String.valueOf(width))
                .setParameter("height", String.valueOf(height))
                .build()
                .toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        Unirest.get(funcUrl.toString());
    }

    private void setCameraExposure() {
        Logger.trace(String.format("imgSetExposure() [cameraId:%d]", cameraId, exposure));
        URL funcUrl;
        try {
            funcUrl = new URIBuilder(baseURI)
                .setPath(baseURI.getPath() + "imgSetExposure")
                .setParameter("exposure", String.valueOf(exposure))
                .build()
                .toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        Unirest.get(funcUrl.toString());
    }

    private void setCameraGain() {
        Logger.trace(String.format("imgSetGain() [cameraId:%d, gain:%d]", cameraId, gain));
        URL funcUrl;
        try {
            funcUrl = new URIBuilder(baseURI)
                .setPath(baseURI.getPath() + "imgSetGain")
                .setParameter("gain", String.valueOf(gain))
                .build()
                .toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        Unirest.get(funcUrl.toString());
    }

    private void setCameraLt() {
        Logger.trace(String.format("imgSetLt() [cameraId:%d, shiftX:%d, shiftY:%d]", cameraId, shiftX, shiftY));
        URL funcUrl;
        try {
            funcUrl = new URIBuilder(baseURI)
                .setPath(baseURI.getPath() + "imgSetLt")
                .setParameter("a2", String.valueOf(shiftX))
                .setParameter("a3", String.valueOf(shiftY))
                .build()
                .toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        Unirest.get(funcUrl.toString());
    }

    private void cameraReset() {
        Logger.trace(String.format("imgReset() [cameraId:%d]", cameraId));
        URL funcUrl;
        try {
            funcUrl = new URIBuilder(baseURI)
                .setPath(baseURI.getPath() + "imgReset")
                .build()
                .toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        Unirest.get(funcUrl.toString());
    }

    private void initCamera() {
        Logger.trace(String.format("initCamera() [cameraId:%d, width: %d, height: %d]", cameraId, width, height));
        
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
            snapshotURI = null;
            if ((hostIP != null) && (!hostIP.isEmpty())) {
                try {
                    baseURI = new URIBuilder()
                        .setScheme("http")
                        .setHost(hostIP)
                        .setPort(hostPort)
                        .setPath("/cameras/" + String.valueOf(cameraId) + "/")
                        .build();
                    snapshotURI = getImageReadAsyURL();
                    System.out.println("Snapshot URI: " + snapshotURI.toString());
                }
                catch (MalformedURLException e) {
                    System.err.println("Malformed URL for IP camera at " + hostIP + ": " + e.toString());
                    e.printStackTrace();
                }
                catch (Exception e) {
                    System.err.println("Unknown error initializing IP camera at " + hostIP + ": " + e.toString());
                    e.printStackTrace();
                }

                /* Configure the camera */
                cameraReset();
                setCameraWidthHeight();
                setCameraGain();
                setCameraLt();
                setCameraExposure();
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

    public String getHostIP() {
        return hostIP;
    }

    public synchronized void setHostIP(String hostIP) {
        this.hostIP = hostIP;
        setDirty(true);

        initCamera();
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public int getCameraId() {
        return cameraId;
    }

    public void setCameraId(int cameraId) {
        this.cameraId = cameraId;
    }

    public int getHostPort() {
        return this.hostPort;
    }

    public void setHostPort(int hostPort) {
        this.hostPort = hostPort;
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getTimeout() {
        return this.timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getExposure() {
        return this.exposure;
    }

    public void setExposure(int exposure) {
        this.exposure = exposure;
    }

    public int getGain() {
        return this.gain;
    }

    public void setGain(int gain) {
        this.gain = gain;
    }

    public int getShiftX() {
        return this.shiftX;
    }

    public void setShiftX(int shiftX) {
        this.shiftX = shiftX;
    }

    public int getShiftY() {
        return this.shiftY;
    }

    public void setShiftY(int shiftY) {
        this.shiftY = shiftY;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new Neoden4CameraConfigurationWizard(this);
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
