package org.openpnp.vision.pipeline.stages;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Stage(category="Image Processing", description="Performs adaptive thresholding on the working image.")
public class ThresholdAdaptive extends CvStage {

    public enum AdaptiveMethod {
        Mean(Imgproc.ADAPTIVE_THRESH_MEAN_C),
        Gaussian(Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C);
        
        private int code;

        AdaptiveMethod(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
    
    @Attribute
    @Property(description="Adaptive thresholding algorithm to use: 'Mean' is a mean of the (blockSize x blockSize) neighborhood of a pixel minus cParm. 'Gaussian' is a weighted sum (cross-correlation with a Gaussian window) of the (blockSize x blockSize) neighborhood of a pixel minus cParm")
    private AdaptiveMethod adaptiveMethod = AdaptiveMethod.Mean;
    
    public AdaptiveMethod getAdaptiveMethod() {
        return adaptiveMethod;
    }

    public void setAdaptiveMethod(AdaptiveMethod adaptiveMethod) {
        this.adaptiveMethod = adaptiveMethod;
    }

    @Attribute
    @Property(description="Thresholding type that must be either binary (default) or inverted binary")
    private boolean invert = false;
    
    public boolean isInvert() {
        return invert;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    @Attribute
    @Property(description="Size of a pixel neighborhood that is used to calculate a threshold value for the pixel. Should be and odd number greater than or equal to 3")
    private int blockSize = 127;

    public void setBlockSize(int blocksize) {
        this.blockSize = 2 * (blocksize / 2) + 1;
        this.blockSize = this.blockSize < 3 ? 3 : this.blockSize;
    }

    public int getBlockSize() {
        return blockSize;
    }
    
    @Attribute
    @Property(description="Constant subtracted from the mean or weighted mean. Can take negative values too.")
    private int cParm = 80;

    public void setcParm(int cparm) {
        this.cParm = cparm;
    }
    public int getcParm() {
        return cParm;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        Imgproc.adaptiveThreshold(mat, mat, 255, adaptiveMethod.getCode(), invert ? Imgproc.THRESH_BINARY_INV : Imgproc.THRESH_BINARY, blockSize, cParm);
        return null;
    }
}
