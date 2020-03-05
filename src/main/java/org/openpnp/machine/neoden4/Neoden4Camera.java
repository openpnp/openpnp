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

    /* so OpenPnP doesn't crash on startup */
    @Attribute(required = false)
    private int resizeWidth;
    @Attribute(required = false)
    private int resizeHeight;

    @Attribute(required = false)
    private int width = 1024;
    @Attribute(required = false)
    private int height = 1024;
    @Attribute(required = false)
    private int timeout = 1000;
    @Attribute(required = false)
    private int cameraId;

    @Attribute(required = false)
    private int exposure;
    @Attribute(required = false)
    private int gain;
    @Attribute(required = false)
    private int lta2;
    @Attribute(required = false)
    private int lta3;

    @Attribute(required = false)
    private int fps = 10;

    @Attribute(required = true)
    private String hostIP;
    @Attribute(required = false)
    private int hostPort = 8080;

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
        URL funcUrl;
        try {
            funcUrl = new URIBuilder(baseURI)
                .setPath(baseURI.getPath() + "imgSetLt")
                .setParameter("a2", String.valueOf(lta2))
                .setParameter("a3", String.valueOf(lta3))
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
