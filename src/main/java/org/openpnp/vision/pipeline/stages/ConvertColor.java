package org.openpnp.vision.pipeline.stages;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

/**
 * Converts the color of the current working image to the specified conversion.
 */
public class ConvertColor extends CvStage {
    @Attribute
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
        return null;
    }
}
