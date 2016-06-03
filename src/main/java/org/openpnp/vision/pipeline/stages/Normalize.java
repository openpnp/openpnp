package org.openpnp.vision.pipeline.stages;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;

public class Normalize extends CvStage {
    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        throw new Error("Not yet working.");
//        Mat mat = pipeline.getWorkingImage();
//        Mat dst = mat.clone();
//        Core.normalize(mat, dst);
//        return new Result(dst);
    }
}
