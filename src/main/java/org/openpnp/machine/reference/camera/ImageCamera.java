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
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
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
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Footprint;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.VisionProvider.TemplateMatch;
import org.openpnp.util.ImageUtils;
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

    @Element(required = false)
    private Location imageUnitsPerPixel;

    @Attribute(required = false)
    private double simulatedRotation = 0;

    @Attribute(required = false)
    private double simulatedScale = 1.0;

    @Attribute(required = false)
    private double simulatedDistortion = 0.0;

    @Attribute(required = false)
    private double simulatedYaw = 0.0;

    @Attribute(required = false)
    private boolean simulatedFlipped = false;

    @Element(required = false)
    private Length focalLength = new Length(6, LengthUnit.Millimeters);

    @Element(required = false)
    private Length sensorDiagonal = new Length(4.4, LengthUnit.Millimeters);

    @Element(required = false)
    private Location primaryFiducial = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private Location secondaryFiducial = new Location(LengthUnit.Millimeters);

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

    @Deprecated
    @Attribute(required = false)
    private boolean subPixelRendering = true;

    public ImageCamera() {
        setUnitsPerPixel(new Location(LengthUnit.Millimeters, 0.04233, 0.04233, 0, 0));
    }

    public int getViewWidth() {
        return width;
    }

    public void setViewWidth(int width) {
        this.width = width;
    }

    public int getViewHeight() {
        return height;
    }

    public void setViewHeight(int height) {
        this.height = height;
    }

    public double getSimulatedRotation() {
        return simulatedRotation;
    }

    public void setSimulatedRotation(double simulatedRotation) {
        this.simulatedRotation = simulatedRotation;
    }

    public double getSimulatedScale() {
        return simulatedScale;
    }

    public void setSimulatedScale(double simulatedScale) {
        this.simulatedScale = simulatedScale;
    }

    public double getSimulatedDistortion() {
        return simulatedDistortion;
    }

    public void setSimulatedDistortion(double simulatedDistortion) {
        this.simulatedDistortion = simulatedDistortion;
    }

    public double getSimulatedYaw() {
        return simulatedYaw;
    }

    public void setSimulatedYaw(double simulatedYaw) {
        this.simulatedYaw = simulatedYaw;
    }

    public boolean isSimulatedFlipped() {
        return simulatedFlipped;
    }

    public void setSimulatedFlipped(boolean simulatedFlipped) {
        this.simulatedFlipped = simulatedFlipped;
    }

    public Location getImageUnitsPerPixel() {
        if (imageUnitsPerPixel == null) {
            imageUnitsPerPixel = getUnitsPerPixel();
        }
        return imageUnitsPerPixel;
    }

    public void setImageUnitsPerPixel(Location imageUnitsPerPixel) {
        this.imageUnitsPerPixel = imageUnitsPerPixel;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public void setSourceUri(String sourceUri) {
        String oldValue = this.sourceUri;
        this.sourceUri = sourceUri;
        firePropertyChange("sourceUri", oldValue, sourceUri);
    }

    public Length getFocalLength() {
        return focalLength;
    }

    public void setFocalLength(Length focalLength) {
        this.focalLength = focalLength;
    }

    public Length getSensorDiagonal() {
        return sensorDiagonal;
    }

    public void setSensorDiagonal(Length sensorDiagonal) {
        this.sensorDiagonal = sensorDiagonal;
    }

    public Location getPrimaryFiducial() {
        return primaryFiducial;
    }

    public void setPrimaryFiducial(Location primaryFiducial) {
        this.primaryFiducial = primaryFiducial;
    }

    public Location getSecondaryFiducial() {
        return secondaryFiducial;
    }

    public void setSecondaryFiducial(Location secondaryFiducial) {
        this.secondaryFiducial = secondaryFiducial;
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

        Location upp = getImageUnitsPerPixel().convertToUnits(AxesLocation.getUnits());
        double pixelX = locationX / upp.getX();
        double pixelY = locationY / upp.getY();

        // Draw the image with sub-pixel rendering.
        double dx = (pixelX - (width / 2.0));
        double dy = (source.getHeight() - (pixelY + (height / 2.0)));
        gFrame.clearRect(0, 0, width, height);
        gFrame.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);//VALUE_INTERPOLATION_BILINEAR);
        gFrame.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        gFrame.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        AffineTransform t = new AffineTransform();
        t.translate(-dx, -dy); // x/y set here
        if (simulation) {
            t.translate(dx + width/2, dy + height/2);
            t.scale(isSimulatedFlipped() ? -getSimulatedScale() : getSimulatedScale(), getSimulatedScale());
            t.translate(-dx - width/2, -dy - height/2);
            t.rotate(-Math.toRadians(getSimulatedRotation()), dx + width/2, dy + height/2);
        }
        gFrame.drawImage(source, t, null);

        double cameraViewDiagonal = Math.sqrt(Math.pow(upp.getX()*width, 2) + Math.pow(upp.getY()*height, 2));
        double sensorDiagonal = getSensorDiagonal().convertToUnits(AxesLocation.getUnits()).getValue();
        double focalLength = getFocalLength().convertToUnits(AxesLocation.getUnits()).getValue();
        double cameraDistance = focalLength*cameraViewDiagonal/sensorDiagonal;
        // Draw the calibration fiducials. 
        Location fiducial1 = getPrimaryFiducial();
        if (fiducial1.isInitialized()) {
            fiducial1 = fiducial1.convertToUnits(AxesLocation.getUnits()).subtract(location);
            drawFiducial(gFrame, width, height, upp, upp, fiducial1);
        }
        Location fiducial2 = getSecondaryFiducial();
        if (fiducial2.isInitialized()) {
            fiducial2 = fiducial2.convertToUnits(AxesLocation.getUnits()).subtract(location);
            double secondaryDistance = cameraDistance + fiducial1.getZ() - fiducial2.getZ();
            double xYawOffset = Math.tan(Math.toRadians(getSimulatedYaw()))*(fiducial1.getZ() - fiducial2.getZ());
            fiducial2 = fiducial2.add(new Location(AxesLocation.getUnits(), xYawOffset, 0, 0, 0));
            Location upp2 = upp.multiply(secondaryDistance/cameraDistance);
            drawFiducial(gFrame, width, height, upp, upp2, fiducial2);
        }

        if (getSimulatedDistortion() != 0.0 || getSimulatedYaw() != 0.0) {
            // Simulate camera lens distortion and mounting yaw.
            BufferedImage undistorted  = ImageUtils.clone(frame);
            final double sampling = 0.5;
            final int scansize = 3;
            int [] pixels = new int[scansize*scansize];
            double xo = 0.5 - width/2;
            double yo = 0.5 - height/2;
            double radius = Math.sqrt(width*width+height*height)/2;
            double dist = cameraDistance/(upp.getX()*radius);
            double factor = 1.0/radius;
            double zFactor = 1.0/dist;
            double yawRad = Math.toRadians(getSimulatedYaw());
            double sinYaw = Math.sin(yawRad);
            double cosYaw = Math.cos(yawRad);
            double tanYaw = sinYaw/cosYaw;
            double zFactorYaw = zFactor*sinYaw;
            double distort = 0.01*getSimulatedDistortion();
            double projectionFactor = radius;
            // First pass: stake out the projection by 9 points and calculate the projectionFactor.
            for (int x = 0; x <= width; x += width/2) {
                for (int y = 0; y <= height; y += height/2) {
                    // NOTE: as this cannot be refactored without performance penalty,
                    // the following code block is an exact duplicate in both passes.
                    // BEGIN:-------------------------------------- //
                    // Normed to ±1.0
                    double xN = (x + xo)*factor; 
                    double yN = (y + yo)*factor;
                    // Distortion
                    double vect = Math.sqrt(xN*xN + yN*yN);
                    double distortion = vect*distort + (1.0-distort);
                    double xD = xN*distortion;
                    double yD = yN*distortion;
                    // Reverse perspective transform
                    double alpha = Math.atan2(xD, dist)-yawRad;
                    double xT = (Math.tan(alpha)+tanYaw)*dist;
                    double zT = 1.0 - xT*zFactorYaw;
                    double yT = yD*zT;
                    // Pixel coordinates
                    int xP = (int) (xT*projectionFactor - xo);
                    int yP = (int) (yT*projectionFactor - yo);
                    // END:---------------------------------------- //

                    // Minimize the projectionFactor.
                    if (xP < 0) {
                        projectionFactor = xo/xT;
                    }
                    else if (xP > width) {
                        projectionFactor = (width + xo)/xT;
                    }
                    else if (yP < 0) {
                        projectionFactor = yo/yT;
                    }
                    else if (yP > width) {
                        projectionFactor = (width + yo)/yT;
                    }
                }
            }
            // Second pass: transform the image.
            int grayRGB = new Color(128, 128, 128).getRGB();
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    // NOTE: as this cannot be refactored without performance penalty,
                    // the following code block is an exact duplicate in both passes.
                    // BEGIN:-------------------------------------- //
                    // Normed to ±1.0
                    double xN = (x + xo)*factor; 
                    double yN = (y + yo)*factor;
                    // Distortion
                    double vect = Math.sqrt(xN*xN + yN*yN);
                    double distortion = vect*distort + (1.0-distort);
                    double xD = xN*distortion;
                    double yD = yN*distortion;
                    // Reverse perspective transform
                    double alpha = Math.atan2(xD, dist)-yawRad;
                    double xT = (Math.tan(alpha)+tanYaw)*dist;
                    double zT = 1.0 - xT*zFactorYaw;
                    double yT = yD*zT;
                    // Pixel coordinates
                    int xP = (int) (xT*projectionFactor - xo);
                    int yP = (int) (yT*projectionFactor - yo);
                    // END:---------------------------------------- //

                    // Set the pixel.
                    if (xP >= 0 && xP < width && yP >= 0 && yP < height) {
                        int rgb = undistorted.getRGB(xP, yP);
                        frame.setRGB(x, y, rgb);
                    }
                    else {
                        frame.setRGB(x, y, grayRGB);
                    }
                }
            }
        }
        if (simulation) {
            SimulationModeMachine.simulateCameraExposure(this, gFrame, width, height);
        }

        gFrame.dispose();
        return frame;
    }

    protected void blurObjectIntoView(Graphics2D gView, BufferedImage frame) {
        AffineTransform tx = gView.getTransform();
        gView.setTransform(new AffineTransform());
        double radius = 0.2/getImageUnitsPerPixel().convertToUnits(LengthUnit.Millimeters).getX();
        ConvolveOp op = null;
        if (radius > 0.01) {
            int size = (int)Math.ceil(radius) * 2 + 1;
            float[] data = new float[size * size];
            double sum = 0;
            int num = 0;
            for (int i = 0; i < data.length; i++) {
                double x = i/size - size/2.0 + 0.5;
                double y = i%size - size/2.0 + 0.5;
                double r = Math.sqrt(x*x+y*y);
                // rough approximation
                float weight = (float) Math.max(0, Math.min(1, radius + 1 - r));
                data[i] = weight;
                sum += weight;
                if (weight > 0) {
                    num++;
                }
            }
            if (num > 1) {
                for (int i = 0; i < data.length; i++) {
                    data[i] /= sum;
                }

                Kernel kernel = new Kernel(size, size, data);
                op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
            }
        }
        gView.drawImage(frame, op, 0, 0);
        gView.setTransform(tx);
    }

    protected void drawFiducial(Graphics2D gFrame, int cameraWidth, int cameraHeight, Location uppDefault,
            Location upp, Location fiducial) {
        // Coordinates
        double xc = (cameraWidth / 2.0) + fiducial.getX()/upp.getX();
        double yc = (cameraHeight / 2.0) - fiducial.getY()/upp.getY();
        double w = 1.0/upp.getX();
        double h = 1.0/upp.getY();
        if (xc + w < 0 || xc - w > cameraWidth || yc + h < 0 || yc - w > cameraHeight) { 
            return;
        }
        AffineTransform oldTx = gFrame.getTransform();
        AffineTransform tx = new AffineTransform();
        tx.translate(xc - w/2, yc - h/2);

        if (upp == uppDefault) {
            gFrame.setTransform(tx);
            gFrame.setColor(Color.WHITE);
            gFrame.fillOval(0, 0, (int)w, (int)h);
        }
        else {
            // Simulate focal blur
            BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frame.createGraphics();
            g.setTransform(tx);
            // Clear with transparent background
            g.setBackground(new Color(0, 0, 0, 0));
            g.clearRect(-width/2, -height/2, width, height);
            g.fillOval(0, 0, (int)w, (int)h);
            blurObjectIntoView(gFrame, frame); 
        }

        gFrame.setTransform(oldTx);
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
            Location unitsPerPixel = getImageUnitsPerPixel();
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
                Location offsets = new Location(getImageUnitsPerPixel().getUnits(), offsetX, offsetY, 0, 0);
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
        if (solutions.isTargeting(Milestone.Connect)) {
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
}
