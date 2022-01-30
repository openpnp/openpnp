package org.openpnp.vision.pipeline.stages;

import java.awt.image.BufferedImage;
import java.io.File;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
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
    @Property(description="The color space of the image.  Use to select the color space that the original image had when it was written.  Note that this does not change any of the numerical values that represent the image but rather their interpretation when the image is displayed in the pipeline editor.")
    private ColorSpace colorSpace = ColorSpace.Bgr;
    
    @Attribute(required=false)
    @Property(description="Handle the loaded image as if captured by the camera.")
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
        Mat image = Imgcodecs.imread(file.getAbsolutePath());
        if (image.channels() == 1) {
            colorSpace = ColorSpace.Gray;
        }
        if (handleAsCaptured) {
            BufferedImage bufferedImage = OpenCvUtils.toBufferedImage(image);
            pipeline.setLastCapturedImage(bufferedImage);
        }
        return new Result(image, colorSpace);
    }
}
