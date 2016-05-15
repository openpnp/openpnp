package org.openpnp.vision.pipeline.stages;

import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;

public class SimpleBlobDetector extends CvStage {
    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.SIMPLEBLOB);
        MatOfKeyPoint keypoints = new MatOfKeyPoint();
        detector.detect(mat, keypoints);
        List<KeyPoint> list = keypoints.toList();
        keypoints.release();
        return new Result(mat, list);
    }
}
