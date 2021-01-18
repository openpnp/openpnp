package org.openpnp.vision.pipeline.stages;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(
        category   ="Color Space", 
        description="Converts the internal representation of an image from one color space to another.  Note that the conversions (other than to/from gray) do not alter the colors in the image but rather change the underlying numerical represention of the colors such that the preceived colors in the new color space are the same as those in the old color space.  See <a href=\\\"https://en.wikipedia.org/wiki/Color_space\\\" target=\\\"_blank\\\">https://en.wikipedia.org/wiki/Color_space</a> for a detailed explaination of color spaces.")

/**
 * Converts the underlying numeric representation of the current working image from one color space to another.
 */
public class ConvertColor extends CvStage {
    @Attribute
    @Property(description="Selects the from/to color space conversion.")
    private FluentCv.ColorCode conversion = FluentCv.ColorCode.Bgr2Gray;

    public FluentCv.ColorCode getConversion() {
        return conversion;
    }

    public void setConversion(FluentCv.ColorCode conversion) {
        this.conversion = conversion;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        Imgproc.cvtColor(mat, mat, conversion.getCode());
        pipeline.setWorkingColorSpace(conversion.getResultingColorSpace());
        return null;
    }
}
