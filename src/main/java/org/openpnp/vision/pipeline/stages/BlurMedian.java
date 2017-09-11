package org.openpnp.vision.pipeline.stages;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(category = "Image Processing",
        description = "Performs median blurring on the working image.")
public class BlurMedian extends CvStage {
    @Attribute
    @Property(description = "Width and height of the blurring kernel. Should be and odd number greater than or equal to 3")
    private int kernelSize = 3;

    public int getKernelSize() {
        return kernelSize;
    }

    public void setKernelSize(int kernelSize) {
        this.kernelSize = 2 * (kernelSize / 2) + 1;
        this.kernelSize = this.kernelSize < 3 ? 3 : this.kernelSize;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        Imgproc.medianBlur(mat, mat, kernelSize);
        return null;
    }
}
