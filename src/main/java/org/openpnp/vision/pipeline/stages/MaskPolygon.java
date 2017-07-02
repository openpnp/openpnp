package org.openpnp.vision.pipeline.stages;

import java.awt.Color;
import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.openpnp.vision.pipeline.stages.convert.ColorConverter;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.convert.Convert;

@Stage(category = "Image Processing",
        description = "Mask an image with multiple shapes formed with numeric data provided by the user.")

public class MaskPolygon extends CvStage {

    @Element(required = false)
    @Convert(ColorConverter.class)
    @Property(description = "Color of mask.")
    private Color color = Color.black;

    @Attribute(required = false)
    @Property(
            description = "Coordinates forming shapes. X,Y coordinates or sizes are separated by commas, coordinate or size pairs are separated by colons ':'. Multiple shapes are separated by semicolons ';'")
    private String shapes = null;

    @Attribute(required = false)
    @Property(description = "Invert the mask.")
    private boolean inverted = false;

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getShapes() {
        return shapes;
    }

    public void setShapes(String shapes) {
        this.shapes = shapes;
    }

    public boolean isInverted() {
        return inverted;
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (shapes == null) {
            throw new Exception("Mask shape coordinates must be specified.");
        }
        /*
         * decide about the format of the input. Items are separated by semicolons. White space
         * allowed - X,Y : R = circle center + radius - X,Y : W,H = rectangle centered at X,Y -
         * X1,Y1 : X2,Y2 : X3,Y3 = triangle - etc
         */
        Mat mat = pipeline.getWorkingImage();
        Mat mask = mat.clone();
        mask.setTo(FluentCv.colorToScalar(color == null ? FluentCv.indexedColor(0) : color));
        Mat masked = mask.clone();

        String[] items = shapes.split("\\s*;\\s*"), atoms, coords;
        // we will be constructing an array of polygons
        ArrayList<MatOfPoint> poly = new ArrayList<MatOfPoint>();

        for (String item : items) {

            atoms = item.split("\\s*:\\s*");

            if (atoms.length == 1) {

            }
            else if (atoms.length == 2) {
                // this should be a circle or a rectangle. First atom is the center coordinates
                coords = item.split("\\s*(,|:)\\s*");
                try {
                    if (coords.length == 3) {
                        // A circle: the second atom is the radius
                        Core.circle(mask,
                                new Point(Integer.parseInt(coords[0]), Integer.parseInt(coords[1])),
                                Integer.parseInt(coords[2]), new Scalar(255, 255, 255), -1);
                    }
                    else {
                        // A rectangle: the second atom is the width and height of the rectangle
                        int[] coord = new int[4];
                        for (int i = 0; i < 4; i++) {
                            coord[i] = Integer.parseInt(coords[i]);
                        }
                        // calculate two opposite rectangle vertices
                        Core.rectangle(mask,
                                new Point(coord[0] - (int) coord[2] / 2,
                                        coord[1] + (int) coord[3] / 2),
                                new Point(coord[0] + (int) coord[2] / 2,
                                        coord[1] - (int) coord[3] / 2),
                                new Scalar(255, 255, 255), -1);
                    }
                }
                catch (NumberFormatException e) {
                    Logger.error("Cannot parse number. " + e.getMessage());
                }
            }
            else {
                // this should be a polygon
                poly.clear();
                Point[] points = new Point[(int) atoms.length];
                try {
                    for (int i = 0; i < atoms.length; i++) {
                        coords = atoms[i].split("\\s*,\\s*");
                        points[i] =
                                new Point(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
                    }
                    poly.add(new MatOfPoint(points));
                    Core.fillPoly(mask, poly, new Scalar(255, 255, 255));
                }
                catch (NumberFormatException e) {
                    Logger.error("Cannot parse number. " + e.getMessage());
                }
            }
        }
        if (inverted) {
            Core.bitwise_not(mask, mask);
        }
        mat.copyTo(masked, mask);
        mask.release();
        return new Result(masked, null);
    }
}
