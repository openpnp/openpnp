package org.openpnp.vision.pipeline.stages;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;

public class DetectEdgesRobertsCross extends CvStage {
    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();

        Mat kernel = Mat.eye(new Size(2, 2), CvType.CV_32FC1);
        kernel.put(0, 0, 0, 1, -1, 0);
        Mat roberts1 = new Mat();
        Imgproc.filter2D(mat, roberts1, CvType.CV_32FC1, kernel);
        Core.convertScaleAbs(roberts1, roberts1);

        kernel.put(0, 0, 1, 0, 0, -1);
        Mat roberts2 = new Mat();
        Imgproc.filter2D(mat, roberts2, CvType.CV_32FC1, kernel);
        Core.convertScaleAbs(roberts2, roberts2);

        Mat roberts = new Mat();
        Core.add(roberts1, roberts2, roberts);
        
        kernel.release();
        roberts1.release();
        roberts2.release();

        return new Result(roberts);
    }
}
