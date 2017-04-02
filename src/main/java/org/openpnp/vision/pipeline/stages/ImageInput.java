package org.openpnp.vision.pipeline.stages;

import java.awt.image.BufferedImage;

import org.openpnp.spi.Camera;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

public class ImageInput extends CvStage {
    private BufferedImage inputImage;
    
    public BufferedImage getInputImage() {
        return inputImage;
    }

    public void setInputImage(BufferedImage inputImage) {
        this.inputImage = inputImage;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (inputImage == null) {
            throw new Exception("No input image set.");
        }
        return new Result(OpenCvUtils.toMat(inputImage));
    }
}
