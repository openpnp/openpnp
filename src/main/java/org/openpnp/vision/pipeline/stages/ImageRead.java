package org.openpnp.vision.pipeline.stages;

import java.awt.image.BufferedImage;
import java.io.File;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.openpnp.model.Location;
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
    @Property(description="Handle the loaded image as if captured by the camera. The image resolution and aspect ratio will be adapted, so any "
            + "pixel coordinates are correctly interpreted. The image is also registered as the pipeline captured image.")
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
        if (handleAsCaptured) {
            // Try to emulate camera capturing by adapting the read image to the camera resolution and units per pixel.
            Camera camera  = (Camera) pipeline.getProperty("camera");
            Location upp = ImageUtils.getUnitsPerPixel(file);
            double fx;
            double fy;
            if (upp == null) {
                // No UPP given, all we can do is resize to camera dimensions.
                fx = fy = Math.min(((double)camera.getWidth())/image.cols(), ((double)camera.getHeight())/image.rows());
            }
            else {
                // Scale according to UPP from file against that of the camera.
                Location uppCamera = camera.getUnitsPerPixelAtZ();
                fx = upp.getLengthX().divide(uppCamera.getLengthX());
                fy = upp.getLengthY().divide(uppCamera.getLengthY());
            }
            Imgproc.resize(image, image, new Size(), fx, fy, Imgproc.INTER_LANCZOS4);
            int bx = Math.max(0, camera.getWidth() - image.cols())/2;
            int by = Math.max(0, camera.getHeight() - image.rows())/2;
            if (bx > 0 || by > 0) {
                Core.copyMakeBorder(image, image, by, by, bx, bx, Core.BORDER_CONSTANT);
            }
            int cx = Math.max(0, image.cols() - camera.getWidth())/2;
            int cy = Math.max(0, image.rows() - camera.getHeight())/2;
            if (cx > 0 || cy > 0) {
                Rect roi = new Rect(
                        cx,
                        cy,
                        camera.getWidth(),
                        camera.getHeight());
                Mat tmp = new Mat(image, roi);
                image.release();
                image = tmp;
            }
            // Register as captured.
            BufferedImage bufferedImage = OpenCvUtils.toBufferedImage(image);
            pipeline.setLastCapturedImage(bufferedImage);
        }
        if (image.channels() == 1) {
            colorSpace = ColorSpace.Gray;
        }
        return new Result(image, colorSpace);
    }
}
