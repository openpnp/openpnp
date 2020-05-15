package org.openpnp.vision.pipeline.stages;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.openpnp.model.Footprint;
import org.openpnp.spi.Camera;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.openpnp.vision.pipeline.stages.convert.ColorConverter;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.convert.Convert;

@Stage(description="Creates a template from the specified footprint and camera properties. The template is scaled to the camera's units.")
public class CreateFootprintTemplateImage extends CvStage {

    public enum FootprintView {
        Fiducial,
        TopView,
        BottomView
    }
    @Attribute(required=false)
    @Property(description = "Determines, how the footprint is drawn. Fiducial: only draws the pads, TopView: draws body over pads, "
            + "BottomView: draws pads over body.")
    private FootprintView footprintView = FootprintView.Fiducial;

    @Element(required=false)
    @Convert(ColorConverter.class)
    @Property(description = "Color of the pads.")
    private Color padsColor = Color.white; 

    @Element(required=false)
    @Convert(ColorConverter.class)
    @Property(description = "Color of the body.")
    private Color bodyColor = Color.black; 

    public FootprintView getFootprintView() {
        return footprintView;
    }

    public void setFootprintView(FootprintView footprintView) {
        this.footprintView = footprintView;
    }

    public Color getPadsColor() {
        return padsColor;
    }

    public void setPadsColor(Color padsColor) {
        this.padsColor = padsColor;
    }

    public Color getBodyColor() {
        return bodyColor;
    }

    public void setBodyColor(Color bodyColor) {
        this.bodyColor = bodyColor;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Camera camera = (Camera) pipeline.getProperty("camera");
        Footprint footprint = (Footprint) pipeline.getProperty("footprint");

        if (camera == null) {
            throw new Exception("Property \"camera\" is required.");
        }
        if (footprint == null) {
            throw new Exception("Property \"footprint\" is required.");
        }

        BufferedImage template = OpenCvUtils.createFootprintTemplate(camera, footprint, 0.0,
                footprintView == FootprintView.TopView, 
                padsColor, 
                footprintView == FootprintView.Fiducial ? null : bodyColor, 
                null, 1.5, 3);

        return new Result(OpenCvUtils.toMat(template));
    }
}
