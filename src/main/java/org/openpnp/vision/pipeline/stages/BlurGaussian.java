package org.openpnp.vision.pipeline.stages;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

/**
 * Converts the color of the current working image to the specified conversion. 
 */
public class BlurGaussian extends CvStage {
    @Attribute
    private int kernelSize = 3;

    public int getKernelSize() {
        return kernelSize;
    }

    public void setKernelSize(int kernelSize) {
        this.kernelSize = kernelSize;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        Imgproc.GaussianBlur(mat, mat, new Size(kernelSize, kernelSize), 0);
        return null;
    }
}
