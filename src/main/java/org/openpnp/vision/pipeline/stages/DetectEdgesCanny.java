package org.openpnp.vision.pipeline.stages;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

/**
 * Performs Canny edge detection on the working image, updating it with the results.
 */
public class DetectEdgesCanny extends CvStage {
    @Attribute
    double threshold1 = 40;

    @Attribute
    double threshold2 = 180;

    public double getThreshold1() {
        return threshold1;
    }

    public void setThreshold1(double threshold1) {
        this.threshold1 = threshold1;
    }

    public double getThreshold2() {
        return threshold2;
    }

    public void setThreshold2(double threshold2) {
        this.threshold2 = threshold2;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        Imgproc.Canny(mat, mat, threshold1, threshold2);
        return null;
    }
}
