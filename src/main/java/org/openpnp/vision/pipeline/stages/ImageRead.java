package org.openpnp.vision.pipeline.stages;

import java.awt.image.BufferedImage;
import java.io.File;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.openpnp.spi.Camera;
import org.openpnp.util.ImageUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.FluentCv.ColorSpace;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(
  category   ="Image Processing", 
  description="Replace the working image with the image loaded from a given path.")
  
public class ImageRead extends CvStage {
    @Attribute
    @Property(description="Absolute path of the image file to read.")
    private File file = new File("");

    @Attribute(required=false)
    @Property(description="The color space of the image.  Use to select the color space that the original image had when it was written. "
            + "Note that this does not change any of the numerical values that represent the image but rather their interpretation when the "
            + "image is displayed in the pipeline editor.")
    private ColorSpace colorSpace = ColorSpace.Bgr;
    
    @Attribute(required=false)
    @Property(description="Handle the loaded image as if captured by the camera. The image resolution and aspect ratio will be adapted, and "
            + "if information is present (upp.txt), the image is scaled to camera Units per Pixel.<br/>"
            + "Any pixel coordinates obtained from the image are therefore correctly interpreted, as they would from a "
            + "camera captured image.<br/>"
            + "The image is also registered as the pipeline captured image.")
    private boolean handleAsCaptured = false;

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public ColorSpace getColorSpace() {
        return colorSpace;
    }

    public void setColorSpace(ColorSpace colorSpace) {
        this.colorSpace = colorSpace;
    }

    public boolean isHandleAsCaptured() {
        return handleAsCaptured;
    }

    public void setHandleAsCaptured(boolean handleAsCaptured) {
        this.handleAsCaptured = handleAsCaptured;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (!file.exists()) {
            return null;
        }
        Mat image;
        if (handleAsCaptured) {
            // Try to emulate camera capturing by adapting the read image to the camera resolution and units per pixel.
            Camera camera  = (Camera) pipeline.getProperty("camera");
            image = ImageUtils.emulateCameraCapture(camera, file);
            // Register as captured.
            BufferedImage bufferedImage = OpenCvUtils.toBufferedImage(image);
            pipeline.setLastCapturedImage(bufferedImage);
        }
        else  {
            image = Imgcodecs.imread(file.getAbsolutePath());
        }
        if (image.channels() == 1) {
            colorSpace = ColorSpace.Gray;
        }
        return new Result(image, colorSpace);
    }

}
