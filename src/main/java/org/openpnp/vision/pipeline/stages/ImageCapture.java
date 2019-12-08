package org.openpnp.vision.pipeline.stages;

import java.awt.image.BufferedImage;

import org.opencv.core.Core;
import org.opencv.core.Mat;
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
    
    @Attribute(required=false)
    @Property(description="Number of camera images to average.")
    private int count = 1;
    
    public boolean isSettleFirst() {
        return settleFirst;
    }

    public void setSettleFirst(boolean settleFirst) {
        this.settleFirst = settleFirst;
    }
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        if (count > 0) {
            this.count = count;
        }
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Camera camera = (Camera) pipeline.getProperty("camera");
        if (camera == null) {
            throw new Exception("No Camera set on pipeline.");
        }
        Mat workingMat;
        if (settleFirst) {
            workingMat = OpenCvUtils.toMat(camera.settleAndCapture());
        }
        else {
            workingMat = OpenCvUtils.toMat(camera.capture());
        }
        double beta = 1.0/count;
        Core.addWeighted(workingMat, 0, workingMat, beta, 0, workingMat);
        for (int i=1; i<count; i++) {
            Core.addWeighted(workingMat, 1, OpenCvUtils.toMat(camera.capture()), beta, 0, workingMat);
        }
        return new Result(workingMat);
    }
}
