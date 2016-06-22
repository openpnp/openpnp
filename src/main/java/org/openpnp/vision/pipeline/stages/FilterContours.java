package org.openpnp.vision.pipeline.stages;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

/**
 * Draws circles from a List<Circle> onto the working image. 
 */
public class FilterContours extends CvStage {
    @Attribute(required = false)
    private String contoursStageName = null;
    
    @Attribute
    private double minArea = -1;
    
    @Attribute
    private double maxArea = -1;
    
    public String getContoursStageName() {
        return contoursStageName;
    }

    public void setContoursStageName(String contoursStageName) {
        this.contoursStageName = contoursStageName;
    }

    public double getMinArea() {
        return minArea;
    }

    public void setMinArea(double minArea) {
        this.minArea = minArea;
    }

    public double getMaxArea() {
        return maxArea;
    }

    public void setMaxArea(double maxArea) {
        this.maxArea = maxArea;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (contoursStageName == null) {
            return null;
        }
        Result result = pipeline.getResult(contoursStageName);
        if (result == null || result.model == null) {
            return null;
        }
        List<MatOfPoint> contours = (List<MatOfPoint>) result.model;
        List<MatOfPoint> results = new ArrayList<MatOfPoint>();
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area >= (minArea == -1 ? Double.MIN_VALUE : minArea) && area <= (maxArea == -1 ? Double.MAX_VALUE : maxArea)) {
                results.add(contour);
            }
        }
        return new Result(null, results);
    }
}
