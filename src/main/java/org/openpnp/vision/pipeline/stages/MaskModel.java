package org.openpnp.vision.pipeline.stages;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.openpnp.vision.pipeline.stages.convert.ColorConverter;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.convert.Convert;

@Stage(category = "Image Processing",
        description = "Mask an image with model shapes originating from previous stages.")

public class MaskModel extends CvStage {

    @Element(required = false)
    @Convert(ColorConverter.class)
    @Property(description = "Color of mask.")
    private Color color = Color.black;

    @Attribute(required = false)
    @Property(description = "Name of stage to input model data from.")
    private String modelStageName = null;

    @Attribute(required = false)
    @Property(description = "Filter or mask the image.")
    private boolean isMask = false;

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getModelStageName() {
        return modelStageName;
    }

    public void setModelStageName(String modelStageName) {
        this.modelStageName = modelStageName;
    }

    public boolean isIsMask() {
        return isMask;
    }

    public void setIsMask(boolean isMask) {
        this.isMask = isMask;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (modelStageName == null) {
            throw new Exception("Stage name for model must be specified.");
        }
        Mat mat = pipeline.getWorkingImage();
        Mat mask = mat.clone();
        mask.setTo(FluentCv.colorToScalar(color == null ? FluentCv.indexedColor(0) : color));
        Mat masked = mask.clone();

        ArrayList<MatOfPoint> poly = new ArrayList<MatOfPoint>();

        Result result = pipeline.getResult(modelStageName);
        if (result.model instanceof RotatedRect) {
            // just one RotatedRect
            RotatedRect rect = (RotatedRect) result.model;
            // we need this to extract the points
            Point[] points = new Point[4];
            // get the 4 points
            rect.points(points);
            // reuse poly
            poly.clear();
            // convert rect for fillPoly
            poly.add(new MatOfPoint(points));
            // draw rect as poly
            Core.fillPoly(mask, poly, new Scalar(255, 255, 255));

        }
        else if (result.model instanceof Result.Circle) {
            // just one circle
            Result.Circle circle = (Result.Circle) result.model;
            Core.circle(mask, new Point(circle.x, circle.y), (int) circle.diameter / 2,
                    new Scalar(255, 255, 255), -1);

        }
        else if (result.model instanceof List<?>) {
            // we've got multiple Circles or RotatedRects, or contours
            List multi = (List) result.model;
            if (multi.get(0) instanceof Result.Circle) {
                // a collection of circles
                for (int i = 0; i < multi.size(); i++) {
                    Result.Circle circle = (Result.Circle) multi.get(i);
                    Core.circle(mask, new Point(circle.x, circle.y), (int) circle.diameter / 2,
                            new Scalar(255, 255, 255), -1);
                }
            }
            else if (multi.get(0) instanceof RotatedRect) {
                // a collection of Rotatedmulti
                Point[] points = new Point[4];
                for (int i = 0; i < multi.size(); i++) {
                    // get the 4 points of each rotated rect
                    RotatedRect rect = (RotatedRect) multi.get(i);
                    rect.points(points);
                    // reuse poly
                    poly.clear();
                    // convert rect for fillPoly
                    poly.add(new MatOfPoint(points));
                    Core.fillPoly(mask, poly, new Scalar(255, 255, 255));
                }
            }
            else if (multi.get(0) instanceof MatOfPoint) {
                // contours
                for (int i = 0; i < multi.size(); i++) {
                    Imgproc.drawContours(mask, multi, i, new Scalar(255, 255, 255), -1);
                }
            }
        }
        if (isMask) {
            Core.bitwise_not(mask, mask);
        }
        mat.copyTo(masked, mask);
        mask.release();
        return new Result(masked, result.model);
    }
}
