package org.openpnp.vision.pipeline.stages;

import java.awt.Color;

import org.opencv.core.Mat;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Stage;
import org.openpnp.vision.pipeline.stages.convert.ColorConverter;
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

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        mat.setTo(FluentCv.colorToScalar(color));
        return null;
    }
}
