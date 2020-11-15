/*
 * Copyright (C) 2020 Greg Hjelstrom greg.hjelstrom@gmail.com
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

// http://www.walking-productions.com/notslop/2010/04/20/motion-jpeg-in-flash-and-java/
// https://github.com/BITPlan/com.bitplan.mjpegstreamer/tree/master/src/main/java/com/bitplan/mjpegstreamer

package org.openpnp.machine.reference.camera;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;

import org.openpnp.CameraListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.MjpgCaptureCameraWizard;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;



public class MjpgCaptureCamera extends ReferenceCamera implements Runnable {
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    @Attribute(required = false)
    private int fps = 24;

    @Attribute(required = false)
    private String mjpgURL = "http://192.168.1.66:5802";

    @Attribute(required = false)
    private int width = 960;

    @Attribute(required = false)
    private int height = 720;

    private InputStream mjpgStream; // BufferedInputStream mjpgStream;
    private StringWriter lineBuilder;

    private Thread thread;
    private boolean dirty = false;

    // private static final String BOUNDARY_PREFIX = "--";
    // private static final String CONTENT_TYPE_STRING = "Content-Type: ";
    private static final String CONTENT_LENGTH_STRING = "Content-Length: ";


    public MjpgCaptureCamera() {
        setUnitsPerPixel(new Location(LengthUnit.Millimeters, 0.04233, 0.04233, 0, 0));
        try {
            setURL(mjpgURL);
        }
        catch (Exception e) {

        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public String getMjpgURL() {
        return mjpgURL;
    }

    public synchronized void setMjpgURL(String url) {
        this.mjpgURL = url;
        setDirty(true);
        initialize();
    }

    @Override
    @SuppressWarnings("unused")
    @Commit
    protected void commit() throws Exception {
        setURL(mjpgURL);
    }

    @Override
    public synchronized void startContinuousCapture(CameraListener listener) {
        start();
        super.startContinuousCapture(listener);
    }

    @Override
    public synchronized void stopContinuousCapture(CameraListener listener) {
        super.stopContinuousCapture(listener);
        if (listeners.size() == 0) {
            stop();
        }
    }

    private synchronized void stop() {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            try {
                thread.join(3000);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            thread = null;
        }
    }

    private synchronized void start() {

        try {
            URL url = new URL(mjpgURL);
            URLConnection urlcon = url.openConnection();
            urlcon.setConnectTimeout(3000);
            urlcon.setReadTimeout(1000);

            mjpgStream = urlcon.getInputStream(); // new BufferedInputStream(url.openStream());
            lineBuilder = new StringWriter(256);

            if (thread == null) {
                thread = new Thread(this);
                thread.setDaemon(true);
                thread.start();
            }
        }
        catch (Exception e) {
            System.err.println("Unknown error communicating with MJPG stream at " + mjpgURL + ": "
                    + e.toString());
            e.printStackTrace();
        }
    }

    public String getURL() {
        return mjpgURL;
    }

    public void setURL(String url) throws Exception {
        String oldValue = this.mjpgURL;
        this.mjpgURL = url;
        pcs.firePropertyChange("mjpgURL", oldValue, url);
        initialize();
    }

    private synchronized void initialize() {
        stop();
        if (listeners.size() > 0) {
            start();
        }
    }


    @Override
    public BufferedImage internalCapture() {

        int image_size = 0;
        String inputLine;
        lineBuilder.flush();
        lineBuilder.getBuffer()
                   .setLength(0);

        // Read header until we know how big the next image will be
        while (image_size == 0) {
            int next_byte = 0;
            try {
                next_byte = mjpgStream.read();
            }
            catch (IOException e) {
                System.err.println("IOException reading from MJPG stream: " + e.toString());
                e.printStackTrace();
                return null;
            }

            if (next_byte == -1) {
                System.err.println("Could not read header from MJPG stream: " + mjpgURL);
                return null;
            }
            else {
                lineBuilder.write(next_byte);
                if (next_byte == '\n') {
                    inputLine = lineBuilder.toString();
                    if (inputLine.startsWith(CONTENT_LENGTH_STRING)) {
                        // pull the number of bytes out of the content length string
                        String content_len_string =
                                inputLine.substring(CONTENT_LENGTH_STRING.length());
                        content_len_string = content_len_string.replaceAll("(\\r|\\n)", "");
                        image_size = Integer.parseInt(content_len_string);
                    }
                    lineBuilder.getBuffer()
                               .setLength(0);
                }
            }
        }

        // We got what we needed from the header, now just read the steam until we see a 255 which
        // is the beginning of the JPG image
        try {
            while (mjpgStream.read() != 255) {
            }
        }
        catch (IOException e) {
            System.err.println(
                    "Incomplete header in MJPG stream: " + mjpgURL + "\r\n" + e.toString());
            e.printStackTrace();
            return null;
        }

        // Read the jpg image and create a BufferedImage
        byte[] jpg_buffer = new byte[image_size + 1];
        jpg_buffer[0] = (byte) 255;
        int write_cursor = 1;
        boolean got_image = false;
        boolean done = false;

        try {
            while (!done) {
                int bytes_read =
                        mjpgStream.read(jpg_buffer, write_cursor, image_size - write_cursor);
                if (bytes_read > 0) {
                    write_cursor += bytes_read;
                    got_image = (write_cursor >= image_size);
                    done = got_image;
                }
                else {
                    // got -1 (EOF)
                    done = true;
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        if (got_image) {
            ByteArrayInputStream jpg_stream = new ByteArrayInputStream(jpg_buffer);
            BufferedImage frame = null;
            try {
                frame = ImageIO.read(jpg_stream);
            }
            catch (IOException e) {
                System.err.println(
                        "Invalid JPG frame in MJPG steram: " + mjpgURL + "\r\n" + e.toString());
                e.printStackTrace();
            }

            return frame;
        }
        else {
            System.err.println("Incomplete JPG frame in MJPG steram: " + mjpgURL);
            return null;
        }
    }

    @Override
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
                Thread.sleep(10);
            }
            catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new MjpgCaptureCameraWizard(this);
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
