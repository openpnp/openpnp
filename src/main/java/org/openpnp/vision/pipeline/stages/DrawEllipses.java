package org.openpnp.vision.pipeline.stages;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
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
description = "Draws RotatedRects from a stage's model. Input can be either a single RotatedRect or a List of RotatedRect")

public class DrawEllipses extends CvStage {
    @Element(required = false)
    @Convert(ColorConverter.class)
    private Color color = null;

    @Attribute(required = false)
    @Property(description = "Stage to input FitEllipse RotatedRects from.")
    private String ellipsesStageName = null;

    @Attribute(required = true)
    @Property(description = "Thickness of Ellipse outline.")
    private int thickness = 1;

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getEllipsesStageName() {
        return ellipsesStageName;
    }

    public void setEllipsesStageName(String ellipsesStageName) {
        this.ellipsesStageName = ellipsesStageName;
    }

    public int getThickness() {
        return thickness;
    }

    public void setThickness(int thickness) {
        this.thickness = thickness;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (ellipsesStageName == null) {
            throw new Exception("ellipsesStageName must be specified.");
        }
        Result result = pipeline.getResult(ellipsesStageName);
        if (result == null || result.model == null) {
            return null;
        }

        Mat mat = pipeline.getWorkingImage();
        List<RotatedRect> rects = new ArrayList();

        if (result.model instanceof RotatedRect) {
            rects.add((RotatedRect) result.model);
        }
        else if (result.model instanceof List<?>) {
            rects = (List<RotatedRect>) result.model;
        }
        for (int i = 0; i < rects.size(); i++) {
            RotatedRect rect = rects.get(i);
            Color thecolor = (color == null ? FluentCv.indexedColor(i) : color);
            Size axes = new Size(rect.size.width*0.5, rect.size.height*0.5);
            //public static void ellipse(Mat img, Point center, Size axes, double angle, double startAngle, double endAngle, Scalar color, int thickness)
            Imgproc.ellipse(mat, rect.center, axes, rect.angle, 0, 360, FluentCv.colorToScalar(thecolor), thickness);
        }
        return new Result(null, rects);
    }
}
