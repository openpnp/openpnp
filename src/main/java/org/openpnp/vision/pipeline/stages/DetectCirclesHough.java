package org.openpnp.vision.pipeline.stages;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

/**
 * Finds circles in the working image and stores the results as a List<Circle> on the model. 
 */
public class DetectCirclesHough extends CvStage {
    @Attribute
    private int minDistance = 10;

    @Attribute
    private int minDiameter = 10;

    @Attribute
    private int maxDiameter = 100;

    /**
     * Inverse ratio of the accumulator resolution to the image resolution. For example, if dp=1 ,
     * the accumulator has the same resolution as the input image. If dp=2 , the accumulator has
     * half as big width and height.
     */
    @Attribute
    private double dp = 1;

    /**
     * First method-specific parameter. In case of CV_HOUGH_GRADIENT , it is the higher threshold of
     * the two passed to the Canny() edge detector (the lower one is twice smaller).
     */
    @Attribute
    private double param1 = 80;

    /**
     * Second method-specific parameter. In case of CV_HOUGH_GRADIENT , it is the accumulator
     * threshold for the circle centers at the detection stage. The smaller it is, the more false
     * circles may be detected. Circles, corresponding to the larger accumulator values, will be
     * returned first.
     */
    @Attribute
    private double param2 = 10;

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
        Mat mat = pipeline.getWorkingImage();
        Mat output = new Mat();
        Imgproc.HoughCircles(mat, output, Imgproc.CV_HOUGH_GRADIENT, dp, minDistance, param1,
                param2, minDiameter / 2, maxDiameter / 2);
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
