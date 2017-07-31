package org.openpnp.vision.pipeline.stages;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.spi.Camera;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
 * Finds circles in the working image and stores the results as a List<Circle> on the model. 
 */
public class DetectCirclesHough extends CvStage {
    @Attribute
    @Property(description = "Use unit measurements (requires a camera to be set on the pipeline), otherwise use pixels.")
    private boolean useUnitMeasurements = false;

    @Attribute(required = false)
    @Property(description = "Minimum distance between circles, in pixels.")
    private int minDistance = 10;

    @Attribute(required = false)
    @Property(description = "Minimum diameter of circles, in pixels.")
    private int minDiameter = 10;

    @Attribute(required = false)
    @Property(description = "Maximum diameter of circles, in pixels.")
    private int maxDiameter = 100;

    @Element(required = false)
    @Property(description = "Minimum distance between circles, in units from the camera.")
    private Length unitMinDistance = new Length(1.0, LengthUnit.Millimeters);

    @Element(required = false)
    @Property(description = "Minimum diameter of circles, in units from the camera.")
    private Length unitMinDiameter = new Length(1.0, LengthUnit.Millimeters);

    @Element(required = false)
    @Property(description = "Maximum diameter of circles, in units from the camera.")
    private Length unitMaxDiameter = new Length(10.0, LengthUnit.Millimeters);

    /**
     * Inverse ratio of the accumulator resolution to the image resolution. For example, if dp=1 ,
     * the accumulator has the same resolution as the input image. If dp=2 , the accumulator has
     * half as big width and height.
     */
    @Attribute(required = false)
    private double dp = 1;

    /**
     * First method-specific parameter. In case of CV_HOUGH_GRADIENT , it is the higher threshold of
     * the two passed to the Canny() edge detector (the lower one is twice smaller).
     */
    @Attribute(required = false)
    private double param1 = 80;

    /**
     * Second method-specific parameter. In case of CV_HOUGH_GRADIENT , it is the accumulator
     * threshold for the circle centers at the detection stage. The smaller it is, the more false
     * circles may be detected. Circles, corresponding to the larger accumulator values, will be
     * returned first.
     */
    @Attribute(required = false)
    private double param2 = 10;

    public boolean isUseUnitMeasurements() {
        return useUnitMeasurements;
    }

    public void setUseUnitMeasurements(boolean useUnitMeasurements) {
        this.useUnitMeasurements = useUnitMeasurements;
    }

    public int getMinDistance() {
        return minDistance;
    }

    public void setMinDistance(int minDistance) {
        this.minDistance = minDistance;
    }

    public int getMinDiameter() {
        return minDiameter;
    }

    public void setMinDiameter(int minDiameter) {
        this.minDiameter = minDiameter;
    }

    public int getMaxDiameter() {
        return maxDiameter;
    }

    public void setMaxDiameter(int maxDiameter) {
        this.maxDiameter = maxDiameter;
    }

    public Length getUnitMinDistance() {
        return unitMinDistance;
    }

    public void setUnitMinDistance(Length unitMinDistance) {
        this.unitMinDistance = unitMinDistance;
    }

    public Length getUnitMinDiameter() {
        return unitMinDiameter;
    }

    public void setUnitMinDiameter(Length unitMinDiameter) {
        this.unitMinDiameter = unitMinDiameter;
    }

    public Length getUnitMaxDiameter() {
        return unitMaxDiameter;
    }

    public void setUnitMaxDiameter(Length unitMaxDiameter) {
        this.unitMaxDiameter = unitMaxDiameter;
    }

    public double getDp() {
        return dp;
    }

    public void setDp(double dp) {
        this.dp = dp;
    }

    public double getParam1() {
        return param1;
    }

    public void setParam1(double param1) {
        this.param1 = param1;
    }

    public double getParam2() {
        return param2;
    }

    public void setParam2(double param2) {
        this.param2 = param2;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Camera camera = (Camera) pipeline.getProperty("camera");
        if ((camera == null) && useUnitMeasurements) {
            throw new Exception("No Camera set on pipeline.");
        }

        int pxMinDistance = minDistance;
        int pxMinDiameter = minDiameter;
        int pxMaxDiameter = maxDiameter;
        if (useUnitMeasurements) {
            pxMinDistance = (int) VisionUtils.toPixels(unitMinDistance, camera);
            pxMinDiameter = (int) VisionUtils.toPixels(unitMinDiameter, camera);
            pxMaxDiameter = (int) VisionUtils.toPixels(unitMaxDiameter, camera);
        }

        Mat mat = pipeline.getWorkingImage();
        Mat output = new Mat();
        Imgproc.HoughCircles(mat, output, Imgproc.CV_HOUGH_GRADIENT, dp,
                pxMinDistance,
                param1, param2,
                pxMinDiameter / 2,
                pxMaxDiameter / 2);
        List<Result.Circle> circles = new ArrayList<>();
        for (int i = 0; i < output.cols(); i++) {
            double[] circle = output.get(0, i);
            double x = circle[0];
            double y = circle[1];
            double radius = circle[2];
            circles.add(new Result.Circle(x, y, radius * 2.0));
        }
        output.release();

        return new Result(null, circles);
    }
}
