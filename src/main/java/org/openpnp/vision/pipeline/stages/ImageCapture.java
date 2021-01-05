package org.openpnp.vision.pipeline.stages;

import java.awt.image.BufferedImage;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.CvType;
import org.openpnp.spi.Camera;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.FluentCv.ColorSpace;
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
        } else {
            this.count = 1;
        }
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Camera camera = (Camera) pipeline.getProperty("camera");
        if (camera == null) {
            throw new Exception("No Camera set on pipeline.");
        }
        Mat image;
        Mat avgImage;
        if (settleFirst) {
            image = OpenCvUtils.toMat(camera.settleAndCapture());
        }
        else {
            image = OpenCvUtils.toMat(camera.capture());
        }
        image.convertTo(image, 6); //6=CV_64F
        avgImage = image;
        double beta = 1.0/count;
        Core.addWeighted(avgImage, 0, image, beta, 0, avgImage); // avgImage = image/count
        for (int i=1; i<count; i++) {
            image = OpenCvUtils.toMat(camera.capture());
            image.convertTo(image, 6);
            Core.addWeighted(avgImage, 1, image, beta, 0, avgImage); // avgImage = avgImag + image/count
        }
        avgImage.convertTo(avgImage, 0); //0=CV_8U
        return new Result(avgImage, ColorSpace.Bgr);
    }
}
