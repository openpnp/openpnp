package org.openpnp.vision.pipeline.stages;

import java.awt.image.BufferedImage;

import org.openpnp.spi.Camera;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

public class ImageCapture extends CvStage {
    @Attribute
    private boolean settleFirst;
    
    public boolean isSettleFirst() {
        return settleFirst;
    }

    public void setSettleFirst(boolean settleFirst) {
        this.settleFirst = settleFirst;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Camera camera = pipeline.getCamera();
        if (camera == null) {
            throw new Exception("No Camera set on pipeline.");
        }
        BufferedImage image;
        if (settleFirst) {
            image = camera.settleAndCapture();
        }
        else {
            image = camera.capture();
        }
        return new Result(OpenCvUtils.toMat(image));
    }
}
