package org.openpnp.vision.pipeline.stages;

import java.awt.Color;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.stages.convert.ColorConverter;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.convert.Convert;

public class DrawContours extends CvStage {
    @Element(required = false)
    @Convert(ColorConverter.class)
    private Color color = null;

    @Attribute(required = false)
    private String contoursStageName = null;
    
    @Attribute
    private int thickness = 1;
    
    @Property(description="The index of the contour in the list to draw. Any negative value will draw all contours.")
    @Attribute
    private int index = -1;
    
    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getContoursStageName() {
        return contoursStageName;
    }

    public void setContoursStageName(String contoursStageName) {
        this.contoursStageName = contoursStageName;
    }

    public int getThickness() {
        return thickness;
    }

    public void setThickness(int thickness) {
        this.thickness = thickness;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (contoursStageName == null) {
            throw new Exception("contoursStageName is required.");
        }
        Result result = pipeline.getResult(contoursStageName);
        if (result == null || result.model == null) {
            throw new Exception("No model found in results.");
        }
        Mat mat = pipeline.getWorkingImage();
        List<MatOfPoint> contours = (List<MatOfPoint>) result.model;
        if (index < 0) {
            for (int i = 0; i < contours.size(); i++) {
                Imgproc.drawContours(mat, contours, i, FluentCv.colorToScalar(color == null ? FluentCv.indexedColor(i) : color), thickness);
            }
        }
        else {
            Imgproc.drawContours(mat, contours, index, FluentCv.colorToScalar(color == null ? FluentCv.indexedColor(index) : color), thickness);
        }
        return null;
    }
}
