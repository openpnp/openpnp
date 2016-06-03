package org.openpnp.vision.pipeline.stages;

import java.awt.Color;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.stages.convert.ColorConverter;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.convert.Convert;

/**
 * Draws RotatedRects from a stage's model. Input can be either a single RotatedRect or
 * a List<RotatedRect> 
 */
public class DrawRotatedRects extends CvStage {
    @Element(required = false)
    @Convert(ColorConverter.class)
    private Color color = null;

    @Attribute(required = false)
    private String rotatedRectsStageName = null;
    
    @Attribute
    private int thickness = 1;

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
    
    public String getRotatedRectsStageName() {
        return rotatedRectsStageName;
    }

    public void setRotatedRectsStageName(String rotatedRectsStageName) {
        this.rotatedRectsStageName = rotatedRectsStageName;
    }

    public int getThickness() {
        return thickness;
    }

    public void setThickness(int thickness) {
        this.thickness = thickness;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (rotatedRectsStageName == null) {
            throw new Exception("rotatedRectsStageName must be specified.");
        }
        Result result = pipeline.getResult(rotatedRectsStageName);
        if (result == null || result.model == null) {
            return null;
        }
        Mat mat = pipeline.getWorkingImage();
        if (result.model instanceof RotatedRect) {
            FluentCv.drawRotatedRect(mat, ((RotatedRect) result.model), color == null ? FluentCv.indexedColor(0) : color, thickness);
        }
        else if (result.model instanceof List<?>) {
            List<RotatedRect> rects = (List<RotatedRect>) result.model;
            for (int i = 0; i < rects.size(); i++) {
                RotatedRect rect = rects.get(i);
                FluentCv.drawRotatedRect(mat, rect, color == null ? FluentCv.indexedColor(i) : color, thickness);
            }
        }
        return null;
    }
}
