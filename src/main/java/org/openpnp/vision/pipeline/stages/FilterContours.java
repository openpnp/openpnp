package org.openpnp.vision.pipeline.stages;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;
import org.openpnp.model.Area;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.simpleframework.xml.Attribute;

/**
 * Draws circles from a List<Circle> onto the working image. 
 */
public class FilterContours extends CvStage {
    @Attribute(required = false)
    private String contoursStageName = null;
    
    @Attribute
    @Property(description = "Minimum area of the contour, or -1 if no minimum limit is wanted.")
    private double minArea = -1;
    
    @Attribute
    @Property(description = "Maximum area of the contour, or -1 if no maximum limit is wanted.")
    private double maxArea = -1;

    @Attribute(required = false)
    @Property(description = "Name of the property through which OpenPnP controls this stage. Use \"FilterContours\" for standard control.")
    private String propertyName = "FilterContours";

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
        if (contoursStageName == null || contoursStageName.trim().isEmpty()) {
            return null;
        }
        Result result = pipeline.getExpectedResult(contoursStageName);
        if (result.model == null) {
            return null;
        }
        double minArea = getPossiblePipelinePropertyOverride(this.minArea, pipeline, propertyName+".minArea",
                Double.class, Area.class);
        double maxArea = getPossiblePipelinePropertyOverride(this.maxArea, pipeline, propertyName+".maxArea",
                Double.class, Area.class);
        List<MatOfPoint> contours = result.getExpectedListModel(MatOfPoint.class, null);
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
