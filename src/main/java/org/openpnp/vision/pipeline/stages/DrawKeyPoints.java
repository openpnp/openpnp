package org.openpnp.vision.pipeline.stages;

import java.awt.Color;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.stages.convert.ColorConverter;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.convert.Convert;

/**
 * Draws KeyPoints contained in a List<KeyPoint> by referencing a previous stage's model data. 
 */
public class DrawKeyPoints extends CvStage {
    @Element(required = false)
    @Convert(ColorConverter.class)
    private Color color = null;

    @Attribute(required = false)
    private String keyPointsStageName = null;

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
    
    public String getKeyPointsStageName() {
        return keyPointsStageName;
    }

    public void setKeyPointsStageName(String keyPointsStageName) {
        this.keyPointsStageName = keyPointsStageName;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (keyPointsStageName == null) {
            return null;
        }
        Result result = pipeline.getResult(keyPointsStageName);
        if (result == null || result.model == null) {
            return null;
        }
        Mat mat = pipeline.getWorkingImage();
        List<KeyPoint> keyPoints = (List<KeyPoint>) result.model;
        MatOfKeyPoint matOfKeyPoints = new MatOfKeyPoint(keyPoints.toArray(new KeyPoint[] {}));
        if (color == null) {
            Features2d.drawKeypoints(mat, matOfKeyPoints, mat);
        }
        else {
            Features2d.drawKeypoints(mat, matOfKeyPoints, mat, FluentCv.colorToScalar(color), 0);
        }
        return null;
    }
}
