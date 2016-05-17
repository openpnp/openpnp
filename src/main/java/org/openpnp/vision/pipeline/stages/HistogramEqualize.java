package org.openpnp.vision.pipeline.stages;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Root;

@Root
public class HistogramEqualize extends CvStage {
    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        Imgproc.equalizeHist(mat, mat);
        return null;
    }
}
