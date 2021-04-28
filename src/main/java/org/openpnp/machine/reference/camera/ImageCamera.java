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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.SimulationModeMachine;
import org.openpnp.machine.reference.camera.wizards.ImageCameraConfigurationWizard;
import org.openpnp.model.Footprint;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.VisionProvider.TemplateMatch;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.FluentCv;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class ImageCamera extends ReferenceCamera {
    @Element
    private String sourceUri = "classpath://samples/pnp-test/pnp-test.png";

    @Attribute(required = false)
    private int width = 640;

    @Attribute(required = false)
    private int height = 480;

    private BufferedImage source;

    /**
     * In pick location checking, this is the maximum distance allowed.
     */
    @Attribute(required = false)
    private double pickLocationToleranceMm = 0.1;

    @Attribute(required = false)
    private double pickLocationMinimumScore = 0.87;

    @Attribute(required = false)
    private double placeLocationToleranceMm = 0.1;

    @Attribute(required = false)
    private double placeLocationMinimumScore = 0.58;

    @Attribute(required = false)
    boolean filterTestImageVision = true;

    @Attribute(required = false)
    private boolean subPixelRendering = true;

    public ImageCamera() {
        setUnitsPerPixel(new Location(LengthUnit.Millimeters, 0.04233, 0.04233, 0, 0));
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public void setSourceUri(String sourceUri) {
        String oldValue = this.sourceUri;
        this.sourceUri = sourceUri;
        firePropertyChange("sourceUri", oldValue, sourceUri);
    }

    @Override 
    public synchronized void open() throws Exception {
        if (sourceUri.startsWith("classpath://")) {
            source = ImageIO.read(getClass().getClassLoader()
                    .getResourceAsStream(sourceUri.substring("classpath://".length())));
        }
        else {
            source = ImageIO.read(new URL(sourceUri));
        }
        super.open();
    }

    @Override
    public synchronized BufferedImage internalCapture() {
        if (! ensureOpen()) {
            return null;
        }
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

        
        if (subPixelRendering ) {
            // Sub-pixel rendering.
            double dx = (pixelX - (width / 2.0));
            double dy = (source.getHeight() - (pixelY + (height / 2.0)));
            gFrame.clearRect(0, 0, width, height);
            gFrame.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gFrame.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            gFrame.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            AffineTransform t = new AffineTransform();
            t.translate(-dx, -dy); // x/y set here
            t.scale(1.0, 1.0);
            gFrame.drawImage(source, t, null);
        }
        else {
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
        }

        if (simulation) {
            SimulationModeMachine.simulateCameraExposure(this, gFrame, width, height);
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
    public boolean isPickLocation(Location physicalLocation, Nozzle nozzle) throws Exception {
        return isPartLocation(physicalLocation, nozzle.getPart(), true, Color.black, Color.black, Color.white,
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
    public boolean isPlaceLocation(Location physicalLocation, Nozzle nozzle) throws Exception {
        // TODO: we should adjust for the offset that Alignment found here. But that seems not accessible outside
        // the JobProcessor.
        return isPartLocation(physicalLocation, nozzle.getPart(), false, Color.white, Color.black, null,
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
            int templateDimension = (int)Math.sqrt(Math.pow(template.getWidth(), 2)+Math.pow(template.getHeight(), 2));
            templateMat = OpenCvUtils.toMat(template);
            int kernelSize = (templateDimension/4)|1;
            // Blur it to give us a tolerant best match.
            Imgproc.GaussianBlur(templateMat, templateMat, new Size(kernelSize, kernelSize), 0);

            // Get a view of the target area that is 5 x bigger than the template but at least 100px. 
            int dimension = Math.max(Math.max(template.getWidth(), template.getHeight())*5, 80);
            BufferedImage targetArea = locationCapture(physicalLocation, dimension, dimension, false);
            mat = OpenCvUtils.toMat(targetArea);
            
            if (filterTestImageVision) {
                // Because the standard ImageCamera PCB view is marked blue and red, we just extract the green 
                // channel.
                Mat greenMat = new Mat();
                Core.extractChannel(templateMat, greenMat, 2);
                templateMat.release();
                templateMat = greenMat;
                // create a HSV mask that selects the blue/red half-blended part overlays in the ImageCamera standard image.
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
            // The same is true for CV operation that sets up and adjust the StripFeeder. Test have shown, that the test image has 
            // rendering artifacts creating 1 pixel asymmetry in the parts. 
            // Therefore we take 1.5 pixel diameters as an additional tolerance ("slack") in our assessment of the PnP location error.  
            // In addition to that, we allow 2.5% of the template dimension (diagonal) to allow for larger errors in larger parts.
            // Apparently these happen on the 0805 parts, probably due to aliasing in the gaussian 255 gray tones resolution. 
            double pixelTolerance = center.getLinearDistanceTo(unitsPerPixel)*(1.5+templateDimension*0.025);
            for (Point point : OpenCvUtils.matMaxima(resultMat, minScore/3, Double.MAX_VALUE)) {
                int x = point.x;
                int y = point.y;
                double offsetX = x + template.getWidth()/2.0 - dimension/2.0;
                double offsetY = (dimension / 2.0) - (y + template.getHeight()/2.0);
                offsetX *= unitsPerPixel.getX();
                offsetY *= unitsPerPixel.getY();
                Location offsets = new Location(getUnitsPerPixel().getUnits(), offsetX, offsetY, 0, 0);
                double distance = center.getLinearDistanceTo(offsets);
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
            if (bestDistance > maxDistanceMm + pixelTolerance) {
                Logger.error("PnP location distance "+bestDistance
                        +"mm greater than allowed "+maxDistanceMm+"mm + ("+ pixelTolerance+"mm px tol), match "+bestMatch
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
    

    @Override
    public void findIssues(Solutions solutions) {
        super.findIssues(solutions);
        solutions.add(new Solutions.Issue(
                this, 
                "The simulation ImageCamera can be replaced with a OpenPnpCaptureCamera to connect to a real USB camera.", 
                "Replace with OpenPnpCaptureCamera.", 
                Severity.Fundamental,
                "https://github.com/openpnp/openpnp/wiki/OpenPnpCaptureCamera") {

            @Override
            public void setState(Solutions.State state) throws Exception {
                if (state == Solutions.State.Solved) {
                    OpenPnpCaptureCamera camera = createReplacementCamera();
                    replaceCamera(camera);
                }
                else if (getState() == Solutions.State.Solved) {
                    // Place the old one back (from the captured ImageCamera.this).
                    replaceCamera(ImageCamera.this);
                }
                super.setState(state);
            }
        });
    }
}
