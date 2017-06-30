package org.openpnp.vision.pipeline.stages;

import java.awt.image.BufferedImage;

import org.openpnp.spi.Camera;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;
import org.openpnp.vision.pipeline.Stage;
import org.openpnp.vision.pipeline.Property;

@Stage(
  category   ="Image Processing", 
  description="Capture an image from the pipeline camera.")

public class ImageCapture extends CvStage {
    @Attribute
    @Property(description="Wait for the camera to settle before capturing an image.")
    private boolean settleFirst;
    
    public boolean isSettleFirst() {
        return settleFirst;
    }

    public void setSettleFirst(boolean settleFirst) {
        this.settleFirst = settleFirst;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Camera camera = (Camera) pipeline.getProperty("camera");
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
