package org.openpnp.vision.pipeline.stages;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.openpnp.spi.Camera;
import org.opencv.core.Mat;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Stage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.stages.convert.ColorConverter;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.convert.Convert;

@Stage(description="Set all pixels of the working image to the specified color.")
public class SetColor extends CvStage {
    @Element(required = false)
    @Convert(ColorConverter.class)
    private Color color = null;

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Element(required = false)
    @Property(description="Apply camera transformation.")
    private boolean transform = false;

    public boolean getTransform() {
        return transform;
    }

    public void setTransform(boolean v) {
        this.transform = v;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        /* Create a solid block of color to use as basis
           for transform.

					 If we perform a transform, we must use the pretransform
					 image size.
				 
				 */
				
        Mat mat = pipeline.getWorkingImage();
        mat.setTo(FluentCv.colorToScalar(color));
				if(!this.transform){
        	return null;
				}

        /* Get the active camera */
        Camera camera = (Camera) pipeline.getProperty("camera");
        if (camera == null) {
             throw new Exception("No Camera set on pipeline.");
        }

				/* TODO Somebody else can make this more eloquent.
				   Just trying to make an image with the same color space and
				   parameters as the originating camera image. */
        BufferedImage i = OpenCvUtils.toBufferedImage(mat);
        mat.release();
				BufferedImage image = i.getSubimage(0, 0, camera.getCaptureWidth(), camera.getCaptureHeight());

        /* Apply camera transformation to solid block.
           We now have an image of just the camera transformation. */
        image = camera.camTransformImage(image);
        return new Result(OpenCvUtils.toMat(image));
    }
}
