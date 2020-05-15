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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeSupport;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.imgproc.Imgproc;
import org.openpnp.CameraListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.SimulationModeMachine;
import org.openpnp.machine.reference.camera.wizards.ImageCameraConfigurationWizard;
import org.openpnp.machine.reference.driver.NullDriver;
import org.openpnp.model.Configuration;
import org.openpnp.model.Footprint;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.VisionProvider.TemplateMatch;
import org.openpnp.util.LogUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.stages.CreateFootprintTemplateImage;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

public class ImageCamera extends ReferenceCamera implements Runnable {
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    @Attribute(required = false)
    private int fps = 24;

    @Element
    private String sourceUri = "classpath://samples/pnp-test/pnp-test.png";

    @Attribute(required = false)
    private int width = 640;

    @Attribute(required = false)
    private int height = 480;

    private BufferedImage source;

    private Thread thread;

    /**
     * In pick location checking, this is the maximum distance allowed.
     */
    @Attribute(required = false)
    private double pickLocationToleranceMm = 0.1;

    @Attribute(required = false)
    private double pickLocationMinimumScore = 0.85;

    @Attribute(required = false)
    private double placeLocationToleranceMm = 0.1;

    @Attribute(required = false)
    private double placeLocationMinimumScore = 0.6;

    @Attribute(required = false)
    boolean filterTestImageVision = true;

    public ImageCamera() {
        setUnitsPerPixel(new Location(LengthUnit.Millimeters, 0.04233, 0.04233, 0, 0));
        try {
            setSourceUri(sourceUri);
        }
        catch (Exception e) {
            
        }
    }

    @SuppressWarnings("unused")
    @Commit
    protected void commit() throws Exception {
        super.commit();
        setSourceUri(sourceUri);
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

            }
            thread = null;
        }
    }

    private synchronized void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public void setSourceUri(String sourceUri) throws Exception {
        String oldValue = this.sourceUri;
        this.sourceUri = sourceUri;
        pcs.firePropertyChange("sourceUri", oldValue, sourceUri);
        initialize();
    }

    @Override
    public synchronized BufferedImage internalCapture() {
        Location location = SimulationModeMachine.getSimulatedPhysicalLocation(this, getLooking());

        BufferedImage frame = locationCapture(location, width, height, true);
        return frame;
    }

    protected BufferedImage locationCapture(Location location, int width, int height, boolean simulation) {
        /*
         * Create a buffer that we will render the image view.
         */
        BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D gFrame = frame.createGraphics();


        double locationX = location.getX();
        double locationY = location.getY();

        double pixelX = locationX / getUnitsPerPixel().getX();
        double pixelY = locationY / getUnitsPerPixel().getY();

        int dx = (int) (pixelX - (width / 2));
        int dy = (int) (source.getHeight() - (pixelY + (height / 2)));
        int dx1 = dx;
        int dy1 = dy;
        int w1 = width;
        int h1 = height;

        if (dx < 0 || dy < 0 || dx+w1 > source.getWidth() || dy+h1 > source.getHeight()) {
            // crop to source area
            w1 += Math.min(0, dx);
            h1 += Math.min(0, dy);
            dx1 = Math.max(0, dx);
            dy1 = Math.max(0, dy);
            w1 = Math.min(w1, source.getWidth() - dx1);
            h1 = Math.min(h1, source.getHeight() - dy1);
            // paint the rest black
            gFrame.setColor(Color.black);
            gFrame.fillRect(0, 0, width, height);
        }
        gFrame.drawImage(source, dx1-dx, dy1-dy, dx1-dx+w1 - 1, dy1-dy+h1 - 1, dx1, dy1, dx1 + w1 - 1, dy1 + h1 - 1, null);

        if (simulation) {
            SimulationModeMachine.drawSimulatedCameraNoise(gFrame, width, height);
        }

        gFrame.dispose();
        return frame;
    }

    /**
     * Check if the specified location is a pick location, by looking at it and checking if a tape pocket with the 
     * shape of the blackened footprint is there.
     * 
     * @param physicalLocation
     * @param part
     * @return
     * @throws Exception
     */
    public boolean isPickLocation(Location physicalLocation, Part part) throws Exception {
        return isPartLocation(physicalLocation, part, true, Color.black, Color.black, Color.white,
                pickLocationMinimumScore, pickLocationToleranceMm);
    }

    /**
     * Check if the specified location is a place location, by looking at it and checking if solder lands with the 
     * shape of footprint pads are there.
     * 
     * @param physicalLocation
     * @param part
     * @return
     * @throws Exception
     */
    public boolean isPlaceLocation(Location physicalLocation, Part part) throws Exception {
        return isPartLocation(physicalLocation, part, false, Color.white, Color.black, null,
                placeLocationMinimumScore, placeLocationToleranceMm);
    }

    protected boolean isPartLocation(Location physicalLocation, Part part, 
            boolean topView, Color padsColor, Color bodyColor, Color backgroundColor,
            double minScore, double maxDistanceMm) 
                    throws Exception {
        Mat mat = null;
        Mat templateMat = null; 
        Mat resultMat = null;
        try {
            // Create a template image of the part.
            org.openpnp.model.Package pkg = part.getPackage();
            Footprint footprint = pkg.getFootprint();
            if (footprint == null) {
                throw new Exception("Part "+part.getId()+" has no footprint.");
            }

            BufferedImage template = OpenCvUtils.createFootprintTemplate(this, footprint, physicalLocation.getRotation(),
                    topView, padsColor, bodyColor, backgroundColor, 1.5, 8);
            templateMat = OpenCvUtils.toMat(template);
            // Blur it to give us a tolerant best match.
            Imgproc.GaussianBlur(templateMat, templateMat, new Size(7, 7), 0);

            // Get a view of the target area that is 5 x bigger than the template but at least 100px. 
            int dimension = Math.max(Math.max(template.getWidth(), template.getHeight())*5, 80);
            BufferedImage targetArea = locationCapture(physicalLocation, dimension, dimension, false);
            mat = OpenCvUtils.toMat(targetArea);
            
            if (filterTestImageVision) {
                // Because the standard ImageCamera PCB view is marked blue and red, we just extract the green 
                // channels.
                Mat greenMat = new Mat();
                Core.extractChannel(templateMat, greenMat, 2);
                templateMat.release();
                templateMat = greenMat;
               //Hue range wraps past 255 back through 0 so the mask needs to include the range from hueMin
                //to 255 in addition to the range from 0 to hueMax.  To accomplish this, a mask for each separate
                //range is created and then ORed together to form the actual mask.
                Imgproc.cvtColor(mat, mat, FluentCv.ColorCode.Bgr2HsvFull.getCode());
                Scalar min = new Scalar(0, topView ? 0 : 1, 64);
                Scalar max = new Scalar(255, 180, 255);
                Mat masked = new Mat();
                Core.inRange(mat, min, max, masked);
                mat.release();
                mat = masked;
            }

            // Search for the shape within the target area.
            resultMat = new Mat();
            Imgproc.matchTemplate(mat, templateMat, resultMat, Imgproc.TM_CCOEFF_NORMED);

            // Find the best match
            TemplateMatch bestMatch = null;
            double bestDistance = Double.MAX_VALUE;
            Location unitsPerPixel = getUnitsPerPixel();
            Location center = new Location(LengthUnit.Millimeters);
            // CV operations are always rounded to the nearest pixel, giving at most an error of 0.5 x pixel diameter. 
            // Furthermore, because the ImageCamera uses a rather coarse image and also does no sub-pixel rendering, 
            // as a real camera would, another 0.5 x pixel diameter error can happen. Therefore we take the whole pixel diameter 
            // as additional tolerance (kind of a "slack") in our assessment of the location distance.   
            double pixelTolerance = center.getLinearDistanceTo(unitsPerPixel);
            for (Point point : OpenCvUtils.matMaxima(resultMat, minScore/3, Double.MAX_VALUE)) {
                int x = point.x;
                int y = point.y;
                double offsetX = x + template.getWidth()/2.0 - dimension/2.0;
                double offsetY = (dimension / 2.0) - (y + template.getHeight()/2.0);
                offsetX *= unitsPerPixel.getX();
                offsetY *= unitsPerPixel.getY();
                Location offsets = new Location(getUnitsPerPixel().getUnits(), offsetX, offsetY, 0, 0);
                double distance = Math.max(0.0,  center.getLinearDistanceTo(offsets)-pixelTolerance);
                double score = resultMat.get(y, x)[0];
                if (bestMatch == null || (bestDistance > distance && bestMatch.score*0.85 < score)) {
                    bestMatch = new TemplateMatch();
                    bestMatch.score = score;
                    bestDistance = distance;
                    bestMatch.location = offsets;
                }
            }
            // Some debugging.
            OpenCvUtils.saveDebugImage(getClass(), "isPartLocation", "targetArea", mat);
            OpenCvUtils.saveDebugImage(getClass(), "isPartLocation", part.getId(), templateMat);
            OpenCvUtils.saveDebugImage(getClass(), "isPartLocation", "matchTemplate", resultMat);
            
            // Interpret the result. 
            if (bestMatch == null) {
                Logger.error("No PnP location recognized in target view at location "+physicalLocation);
                return false;
            }
            if (bestMatch.score < minScore) {
                Logger.error("No good PnP location recognized in target area at location "+physicalLocation
                        +". Best match score "+bestMatch.score+" does not meet "+minScore 
                        +", distance "+bestDistance);
                return false;
            }
            if (bestDistance > maxDistanceMm) {
                Logger.error("PnP location distance "+bestDistance
                        +"mm greater than allowed "+maxDistanceMm+"mm, match "+bestMatch
                        +", at location "+physicalLocation);
                return false;
            }
            Logger.debug("PnP location distance "+bestDistance
                    +"mm from best match score "+bestMatch.score);
            return true;
        }
        finally {
            if (mat != null) {
                mat.release();
                mat = null;
            }
            if (templateMat != null) {
                templateMat.release();
                templateMat = null;
            }
            if (resultMat != null) {
                resultMat.release();
                resultMat = null;
            }
        }
    }

    private synchronized void initialize() throws Exception {
        stop();

        if (sourceUri.startsWith("classpath://")) {
            source = ImageIO.read(getClass().getClassLoader()
                    .getResourceAsStream(sourceUri.substring("classpath://".length())));
        }
        else {
            source = ImageIO.read(new URL(sourceUri));
        }

        if (listeners.size() > 0) {
            start();
        }
    }


    public void run() {
        while (!Thread.interrupted()) {
            broadcastCapture(captureForPreview());
            try {
                Thread.sleep(100 / fps);
            }
            catch (InterruptedException e) {
                return;
            }
        }
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ImageCameraConfigurationWizard(this);
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
