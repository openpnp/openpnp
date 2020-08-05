/*
 * Copyright (C) 2020 Ian Jamison <ian.dev@arkver.com>
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
import java.awt.image.DataBufferInt;
import java.io.IOException;

import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.freedesktop.gstreamer.Structure;
import org.freedesktop.gstreamer.elements.AppSink;
import org.freedesktop.gstreamer.Buffer;
import org.freedesktop.gstreamer.Caps;
import org.freedesktop.gstreamer.FlowReturn;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Sample;

import org.openpnp.CameraListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.GstreamerCameraConfigurationWizard;
import org.openpnp.spi.PropertySheetHolder;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

/**
 * A Camera implementation based on an arbitrary gst_parse_launch pipeline.
 */
public class GstreamerCamera extends ReferenceCamera implements Runnable {
    static {
        Gst.init();
    }

    @Attribute(name = "pipeline", required = true)
    private String pipeString;

    private BufferedImage currentImage;
    private AppSink videosink;
    private Pipeline pipe;
    private Thread thread;

    // Calling notifyAll on this object will wake the stream thread for one loop to
    // broadcast a new image. It is/includes a condition variable.
    private Object captureNotifier = new Object();

    public GstreamerCamera() {
        ensureOpen();
    }

    @Override
    public synchronized BufferedImage internalCapture() {
        ensureOpen();

        /*
         * XXX: Not sure if a ReentrantLock is needed here, or a ping pong buffer. Once
         * this image is broadcast what's done to it and what happens if the appsink
         * listener updates it? If there are steps in transformImage they'll be copying
         * it into a new image anyway. Watch for tearing and fix maybe.
         */
        return currentImage;
    }

    @Override
    public synchronized void startContinuousCapture(CameraListener listener) {
        ensureOpen();
        super.startContinuousCapture(listener);
    }

    public void run() {
        while (!Thread.interrupted()) {
            try {
                broadcastCapture(captureForPreview());
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                synchronized (captureNotifier) {
                    /* Return an image at least once every second even if it's the old image */
                    captureNotifier.wait(1000);
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public synchronized void ensureOpen() {
        if (thread == null) {
            initCamera();
        }
    }

    private void initCamera() {
        if (pipeString == null) {
            return;
        }

        Bin bin = null;
        try {
            // Create ghost src pad as we link the whole bin to the appsink
            bin = Gst.parseBinFromDescription(pipeString, true);
        } catch (Exception e) {
            Logger.warn("Exception parsing pipeline {}", pipeString);
            return;
        }
        if (bin == null) {
            Logger.warn("Failed parsing pipeline {}", pipeString);
            return;
        }

        try {
            videosink = new AppSink("GstCamSink");

            videosink.set("emit-signals", true);
            AppSinkListener listener = new AppSinkListener();
            videosink.connect((AppSink.NEW_SAMPLE) listener);
            videosink.connect((AppSink.NEW_PREROLL) listener);
            StringBuilder caps = new StringBuilder("video/x-raw,");
            // JNA creates ByteBuffer using native byte order, set masks according to that.
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                caps.append("format=BGRx");
            } else {
                caps.append("format=xRGB");
            }
            videosink.setCaps(new Caps(caps.toString()));

            // XXX: What about bus messages? Don't we at least need to drain them?
            pipe = new Pipeline();
            pipe.addMany(bin, videosink);
            Pipeline.linkMany(bin, videosink);
            pipe.play();

        } catch (

        Exception e) {
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

        if (pipe != null) {
            pipe.stop();
        }
        // Dropping refs here hopefully causes gstreamer to dispose of the objects
        pipe = null;
        videosink = null;
        currentImage = null;

        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(3000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Why do none of the other cams do this? Do we never re-open?
        thread = null;
    }

    public String getPipeline() {
        return pipeString;
    }

    public synchronized void setPipeline(String pipe) {
        if (this.pipeString != null && thread != null && !this.pipeString.equals(pipe)) {
            try {
                close();
            } catch (Exception e) {
            }
        }
        pipeString = pipe;
        Logger.debug("Set pipeline: {}", pipeString);
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new GstreamerCameraConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    private BufferedImage getBufferedImage(int width, int height) {
        if (currentImage != null && currentImage.getWidth() == width && currentImage.getHeight() == height) {
            return currentImage;
        }
        if (currentImage != null) {
            currentImage.flush();
        }
        currentImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        currentImage.setAccelerationPriority(0.0f);
        return currentImage;
    }

    // Not sure if we need preroll samples, but the example code showed them anyway.
    private class AppSinkListener implements AppSink.NEW_SAMPLE, AppSink.NEW_PREROLL {

        public void rgbFrame(boolean isPrerollFrame, int width, int height, IntBuffer rgb) {
            try {
                final BufferedImage renderImage = getBufferedImage(width, height);
                int[] pixels = ((DataBufferInt) renderImage.getRaster().getDataBuffer()).getData();
                rgb.get(pixels, 0, width * height);
            } finally {
            }
        }

        private FlowReturn handleSample(Sample sample) {
            Structure capsStruct = sample.getCaps().getStructure(0);
            int w = capsStruct.getInteger("width");
            int h = capsStruct.getInteger("height");
            Buffer buffer = sample.getBuffer();
            ByteBuffer bb = buffer.map(false);
            if (bb != null) {
                rgbFrame(false, w, h, bb.asIntBuffer());
                buffer.unmap();
                synchronized (captureNotifier) {
                    captureNotifier.notifyAll();
                }
            }
            sample.dispose();
            return FlowReturn.OK;
        }

        @Override
        public FlowReturn newSample(AppSink elem) {
            return handleSample(elem.pullSample());
        }

        @Override
        public FlowReturn newPreroll(AppSink elem) {
            return handleSample(elem.pullPreroll());
        }

    }
}
