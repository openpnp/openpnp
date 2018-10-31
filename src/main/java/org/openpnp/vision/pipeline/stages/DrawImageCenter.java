package org.openpnp.vision.pipeline.stages;

import java.io.File;
import java.awt.Color;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Point;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.stages.convert.ColorConverter;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.convert.Convert;
import org.openpnp.vision.pipeline.Stage;
import org.openpnp.vision.pipeline.Property;

@Stage(
  category   ="Image Processing", 
  description="Draw a mark at the center of the image.")
  
public class DrawImageCenter extends CvStage {
    @Attribute(required = false)
    @Property(description="Show a mark at the center of the image.")
    private boolean showImageCenter = true;
    
    @Element(required = false)
    @Convert(ColorConverter.class)
    @Property(description="Color for the center mark.")
    private Color color = null;
    
    @Attribute(required = false)
    @Property(description="Thickness of center mark.")
    private int thickness = 2;
    
    @Attribute(required = false)
    @Property(description="Size of center mark.")
    private int size = 40;

    public boolean isShowImageCenter() {
        return showImageCenter;
    }

    public void setShowImageCenter(boolean showImageCenter) {
        this.showImageCenter = showImageCenter;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
    
    public int getThickness() {
        return thickness;
    }

    public void setThickness(int thickness) {
        this.thickness = thickness;
    }
    
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    
    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        if (showImageCenter) {
            int cx = (int)mat.size().width/2;
            int cy = (int)mat.size().height/2;
            Scalar c = FluentCv.colorToScalar( color == null ? FluentCv.indexedColor(0) : color);
            Core.line(mat,new Point(cx - size/2,cy), new Point(cx + size/2,cy), c, thickness);
            Core.line(mat,new Point(cx,cy - size/2), new Point(cx,cy + size/2), c, thickness);
        }
        return new Result(mat);
    }
}
