package org.openpnp.vision.pipeline.stages;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;

/**
 * Performs Canny edge detection on the working image, updating it with the results.
 */
public class DetectEdgesLaplacian extends CvStage {

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
	Imgproc.Laplacian(mat, mat, mat.depth());
        return null;
    }
}
