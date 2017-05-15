package org.openpnp.vision.pipeline.stages;

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import org.opencv.core.Core;

import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.stages.convert.ColorConverter;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.openpnp.vision.pipeline.Property;
import org.simpleframework.xml.convert.Convert;

@Stage(
  category="Image Processing", 
  description="Draws RotatedRects from a stage's model. Input can be either a single RotatedRect or a List of RotatedRect")
  
public class DrawRotatedRects extends CvStage {
    @Element(required = false)
    @Convert(ColorConverter.class)
    private Color color = null;

    @Attribute(required = false)
    @Property(description="Stage to input RotatedRect from.")
    private String rotatedRectsStageName = null;
    
    @Attribute(required = true)
    @Property(description="Thickness of RotatedRect outline.")
    private int thickness = 1;
    
    @Attribute(required = false)
    @Property(description="Draw a circle at the center of each RotatedRect.")
    private boolean drawRectCenter = false;
    
    @Attribute(required = false)
    @Property(description="Radius of circle at center of RotatedRects.")
    private int rectCenterRadius = 20;
    
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

    public boolean isDrawRectCenter() {
        return drawRectCenter;
    }

    public void setDrawRectCenter(boolean drawRectCenter) {
        this.drawRectCenter = drawRectCenter;
    }

    public int getrectCenterRadius() {
        return rectCenterRadius;
    }

    public void setRectCenterRadius(int rectCenterRadius) {
        this.rectCenterRadius = rectCenterRadius;
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
        List<RotatedRect> rects = new ArrayList();
         
        if (result.model instanceof RotatedRect) {
            rects.add((RotatedRect) result.model);
        }
        else if (result.model instanceof List<?>) {
            rects = (List<RotatedRect>) result.model;
        }
        for (int i = 0; i < rects.size(); i++) {
            RotatedRect rect = rects.get(i);
            FluentCv.drawRotatedRect(mat, rect, color == null ? FluentCv.indexedColor(i) : color, thickness);
            if (drawRectCenter) {
                Core.circle(mat, rect.center, rectCenterRadius, FluentCv.colorToScalar(color == null ? FluentCv.indexedColor(i) : color), thickness);
            }
        }
        return null;
    }
}
