package org.openpnp.vision.pipeline.stages;

import java.awt.Color;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.openpnp.util.HslColor;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.stages.convert.ColorConverter;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.convert.Convert;

/**
 * Draws circles from a List<Circle> onto the working image. 
 */
public class DrawCircles extends CvStage {
    @Element(required = false)
    @Convert(ColorConverter.class)
    private Color color = null;

    @Element(required = false)
    @Convert(ColorConverter.class)
    private Color centerColor = null;

    @Attribute(required = false)
    private String circlesStageName = null;

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
    
    public Color getCenterColor() {
        return centerColor;
    }

    public void setCenterColor(Color centerColor) {
        this.centerColor = centerColor;
    }

    public String getCirclesStageName() {
        return circlesStageName;
    }

    public void setCirclesStageName(String modelStageName) {
        this.circlesStageName = modelStageName;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (circlesStageName == null) {
            return null;
        }
        Result result = pipeline.getResult(circlesStageName);
        if (result == null || result.model == null) {
            return null;
        }
        Mat mat = pipeline.getWorkingImage();
        List<Result.Circle> circles = (List<Result.Circle>) result.model;
        for (int i = 0; i < circles.size(); i++) {
            Result.Circle circle = circles.get(i);
            Color color = this.color == null ? FluentCv.indexedColor(i) : this.color;
            Color centerColor = this.centerColor == null ? new HslColor(color).getComplementary() : this.centerColor;
            Core.circle(mat, new Point(circle.x, circle.y), (int) (circle.diameter / 2),
                    FluentCv.colorToScalar(color), 2);
            Core.circle(mat, new Point(circle.x, circle.y), 1, FluentCv.colorToScalar(centerColor),
                    2);
        }
        return null;
    }
}
