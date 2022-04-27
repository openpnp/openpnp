/*
 * Copyright (C) 2020 <mark@makr.zone>
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

package org.openpnp.vision.pipeline.stages;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.RegionOfInterest;
import org.openpnp.spi.Camera;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

@Stage(description="Extracts a rectangular (parallelogrammatic) region of interest from the image that can have any position, size, "
        + "rotation, scale, <em>shear</em> or even be mirrored. A so-called Affine Transformation Warp.<br/>"
        + "The coordinates are given in real length units rather than pixels and are relative to the camera center, Y pointing up. "
        + "This allows the pipeline to be independent of camera model and resolution, lens, focus distance etc. <br/>"
        + "To setup, pin the previous stage and read off length unit coordinates from the mouse position.")
public class AffineWarp extends CvStage {
    @Attribute(required = false)
    @Property(description = "Length unit used in this stage.")
    private LengthUnit lengthUnit = LengthUnit.Millimeters;

    @Attribute
    @Property(description = "What will become the left upper corner of the extracted area. X offset from camera location.")
    private double x0 = 0;

    @Attribute
    @Property(description = "What will become the left upper corner of the extracted area. Y offset from camera location.")
    private double y0 = 0;

    @Attribute
    @Property(description = "What will become the right upper corner of the extracted area. X offset from camera location.")
    private double x1 = 0;

    @Attribute
    @Property(description = "What will become the right upper corner of the extracted area. Y offset from camera location.")
    private double y1 = 0;

    @Attribute
    @Property(description = "What will become the left lower corner of the extracted area. X offset from camera location.")
    private double x2 = 0;

    @Attribute
    @Property(description = "What will become the left lower corner of the extracted area. Y offset from camera location.")
    private double y2 = 0;

    @Attribute(required=false)
    @Property(description = "Scale of the transformation. NOTE: subsequent stages must be aware that the camera pixel to units scale has changed.")
    private double scale = 1.0;

    @Attribute(required=false)
    @Property(description = "Rectify the transformation to be rectangular. The point [x2, y2] is not interpreted "
            + "as a corner but as a height indicator.")
    private boolean rectify = true;

    @Attribute(required=false)
    @Property(description = "The caller of the pipeline can override the region of interest under this name.")
    private String regionOfInterestProperty = "regionOfInterest";

    /* broken out in https://github.com/openpnp/openpnp/pull/980
     * uncomment the @Override as soon as PR #980 is merged
    @Override*/
    public LengthUnit getLengthUnit() {
        return lengthUnit;
    }

    public void setLengthUnit(LengthUnit lengthUnit) {
        this.lengthUnit = lengthUnit;
    }

    public double getX0() {
        return x0;
    }

    public void setX0(double x0) {
        this.x0 = x0;
    }

    public double getY0() {
        return y0;
    }

    public void setY0(double y0) {
        this.y0 = y0;
    }

    public double getX1() {
        return x1;
    }

    public void setX1(double x1) {
        this.x1 = x1;
    }

    public double getY1() {
        return y1;
    }

    public void setY1(double y1) {
        this.y1 = y1;
    }

    public double getX2() {
        return x2;
    }

    public void setX2(double x2) {
        this.x2 = x2;
    }

    public double getY2() {
        return y2;
    }

    public void setY2(double y2) {
        this.y2 = y2;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public boolean isRectify() {
        return rectify;
    }

    public void setRectify(boolean rectify) {
        this.rectify = rectify;
    }

    public String getRegionOfInterestProperty() {
        return regionOfInterestProperty;
    }

    public void setRegionOfInterestProperty(String regionOfInterestProperty) {
        this.regionOfInterestProperty = regionOfInterestProperty;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Camera camera = (Camera) pipeline.getProperty("camera");
        if (camera == null) {
            throw new Exception("Property \"camera\" is required.");
        }
        Location unitsPerPixel = camera.getUnitsPerPixelAtZ()
                .convertToUnits(lengthUnit);

        double x0 = this.x0;
        double y0 = this.y0;
        double x1 = this.x1;
        double y1 = this.y1;
        double x2 = this.x2;
        double y2 = this.y2;
        boolean rectify = this.rectify;

        if (getRegionOfInterestProperty() != null && ! getRegionOfInterestProperty().isEmpty()) {
            RegionOfInterest roi = (RegionOfInterest) pipeline.getProperty(getRegionOfInterestProperty());
            if (roi != null) {
                // the region of interest is overridden by the pipeline caller
                x0 = roi.getUpperLeftCorner().convertToUnits(lengthUnit).getX(); 
                y0 = roi.getUpperLeftCorner().convertToUnits(lengthUnit).getY();
                x1 = roi.getUpperRightCorner().convertToUnits(lengthUnit).getX(); 
                y1 = roi.getUpperRightCorner().convertToUnits(lengthUnit).getY();
                x2 = roi.getLowerLeftCorner().convertToUnits(lengthUnit).getX(); 
                y2 = roi.getLowerLeftCorner().convertToUnits(lengthUnit).getY();
                rectify = roi.isRectify();
                // Record the fact that these individual stage properties are all overridden.
                recordPropertyOverride("x0", x0);
                recordPropertyOverride("y0", y0);
                recordPropertyOverride("x1", x1);
                recordPropertyOverride("y1", y1);
                recordPropertyOverride("x2", x2);
                recordPropertyOverride("y2", y2);
                recordPropertyOverride("rectify", rectify);
            }
        }

        // get the working image
        Mat mat = pipeline.getWorkingImage();

        double w = Math.sqrt(Math.pow(x1-x0, 2.0) + Math.pow(y1-y0, 2.0));
        double h = Math.sqrt(Math.pow(x2-x0, 2.0) + Math.pow(y2-y0, 2.0));
        if (w == 0.0 || h == 0.0) {
            return null;
        }

        double x2eff = x2;
        double y2eff = y2;
        if (rectify) {
            // unit vector
            double ux = (x1-x0)/w;
            double uy = (y1-y0)/w; // yes, w is the length to norm with
            // normal vector
            double nx = -uy;
            double ny = ux;
            // height vector
            double hx = (x2-x0);
            double hy = (y2-y0);
            // dot product with the normal vector gives the new height
            h = hx*nx + hy*ny;
            x2eff = x0 + h*nx;
            y2eff = y0 + h*ny;
        }

        // conversion to pixels
        double x0Px = x0/unitsPerPixel.getX()+mat.cols() / 2;
        double y0Px = -y0/unitsPerPixel.getY()+mat.rows() / 2;
        double x1Px = x1/unitsPerPixel.getX()+mat.cols() / 2;
        double y1Px = -y1/unitsPerPixel.getY()+mat.rows() / 2;
        double x2Px = x2eff/unitsPerPixel.getX()+mat.cols() / 2;
        double y2Px = -y2eff/unitsPerPixel.getY()+mat.rows() / 2;


        double wPx = scale*w/unitsPerPixel.getX();
        double hPx = scale*Math.abs(h)/unitsPerPixel.getY();

        double diagonal = Math.sqrt(Math.pow(mat.cols(), 2.0)+Math.pow(mat.rows(), 2.0));
        if (wPx > diagonal+2 || hPx > diagonal+2) {
            // while setting up the affine transform coordinates one by one, huge images can result -> prevent that 
            Logger.error("["+getClass().getName()+"] affineWarp must not generate an image that is larger than the original");
            return new Result(mat, "ERROR: affineWarp must not generate an image that is larger than the original");
        }

        org.opencv.core.Point p0 = new org.opencv.core.Point(x0Px, y0Px); // upper left 
        org.opencv.core.Point p1 = new org.opencv.core.Point(x1Px, y1Px); // upper right 
        org.opencv.core.Point p2 = new org.opencv.core.Point(x2Px, y2Px); // lower left 

        org.opencv.core.Point p0T = new org.opencv.core.Point(0, 0);
        org.opencv.core.Point p1T = new org.opencv.core.Point(wPx, 0);
        org.opencv.core.Point p2T = new org.opencv.core.Point(0, hPx);

        MatOfPoint2f ma1 = new MatOfPoint2f(p0, p1, p2);
        MatOfPoint2f ma2 = new MatOfPoint2f(p0T, p1T, p2T);

        // Create the transformation matrix
        Mat transformMatrix = Imgproc.getAffineTransform(ma1, ma2);

        // Recreate the same in Java
        AffineTransform affineTransform = getAffineTransform(p0, p1, p2, p0T, p1T, p2T);

        // Create the size
        Size size = new Size(wPx, hPx);

        // Perform the warpAffine
        Mat transformed = new Mat();
        Imgproc.warpAffine(mat, transformed, transformMatrix, size);


        return new Result(transformed, affineTransform);
    }

    protected AffineTransform getAffineTransform(
            org.opencv.core.Point p0,
            org.opencv.core.Point p1,
            org.opencv.core.Point p2,
            org.opencv.core.Point p0T,
            org.opencv.core.Point p1T,
            org.opencv.core.Point p2T) {   
        AffineTransform unit = new AffineTransform(p1.x - p0.x, p1.y - p0.y, p2.x - p0.x, p2.y - p0.y, p0.x, p0.y);
        AffineTransform unitT = new AffineTransform(p1T.x -p0T.x, p1T.y - p0T.y, p2T.x - p0T.x, p2T.y - p0T.y, p0T.x, p0T.y);
        AffineTransform unitInv = null;
        try {
            unitInv = unit.createInverse();
        }
        catch (NoninvertibleTransformException e) {
            Logger.error("["+getClass().getName()+"] getAffineTransform() cannot invert transformation");
            return new AffineTransform();
        }
        AffineTransform at = new AffineTransform();
        at.concatenate(unitT);
        at.concatenate(unitInv);
        return at;
    }
}
