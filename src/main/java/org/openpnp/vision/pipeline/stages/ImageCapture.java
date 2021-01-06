package org.openpnp.vision.pipeline.stages;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.openpnp.vision.pipeline.ui.PipelinePropertySheetTable;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;


@Stage(
        category   ="Image Processing", 
        description="Capture an image from the pipeline camera.")

public class ImageCapture extends CvStage {
    @Element(required=false)
    @Property(description="Use the default camera lighting.")
    private boolean defaultLight = true;

    @Element(required=false)
    @Property(description="Light actuator value or profile, if default camera lighting is disabled.")
    private Object light = null;

    @Attribute
    @Property(description="Wait for the camera to settle before capturing an image.")
    private boolean settleFirst;

    @Attribute(required=false)
    @Property(description="Number of camera images to average.")
    private int count = 1;

    public boolean isDefaultLight() {
        return defaultLight;
    }

    public void setDefaultLight(boolean defaultLight) {
        this.defaultLight = defaultLight;
    }

    public Object getLight() {
        return light;
    }

    public void setLight(Object light) {
        this.light = light;
    }

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
        // Light, settle and capture the image. Keep the lights on for possible averaging.
        camera.actuateLightBeforeCapture((defaultLight ? null : getLight()));
        try {
            Mat image = OpenCvUtils.toMat(settleFirst ? camera.settleAndCapture() : camera.capture());
            if (count <= 1) { 
                return new Result(image);
            }
            else {
                // Perform averaging in channel type double.
                image.convertTo(image, CvType.CV_64F);
                Mat avgImage = image;
                double beta = 1.0 / count;
                Core.addWeighted(avgImage, 0, image, beta, 0, avgImage); // avgImage = image/count
                for (int i = 1; i < count; i++) {
                    image = OpenCvUtils.toMat(camera.capture());
                    image.convertTo(image, CvType.CV_64F);
                    Core.addWeighted(avgImage, 1, image, beta, 0, avgImage); // avgImage = avgImag + image/count
                    // Release the additional image.
                    image.release();
                }
                avgImage.convertTo(avgImage, CvType.CV_8U);
                return new Result(avgImage);
            }
        }
        finally {
            // Always switch off the light. 
            camera.actuateLightAfterCapture();
        }
    }

    @Override
    public void customizePropertySheet(PipelinePropertySheetTable table, CvPipeline pipeline) {
        super.customizePropertySheet(table, pipeline);
        Camera camera = (Camera) pipeline.getProperty("camera");
        if (camera != null) {
            Actuator actuator = camera.getLightActuator();
            String propertyName = "light";
            table.customizeActuatorProperty(propertyName, actuator);
        }
    }
}
