package org.openpnp.vision.pipeline.stages;
import org.I18n.I18n;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.openpnp.model.Footprint;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.FluentCv.ColorSpace;
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

    @Element(required=false)
    @Convert(ColorConverter.class)
    @Property(description = "Color of the background.")
    private Color backgroundColor = Color.black; 

    @Attribute(required=false)
    @Property(description = "If enabled dimensions are only controled by the part size.")
    private boolean minimalImageSize = false;

    
    public boolean isMinimalImageSize() {
        return minimalImageSize;
    }

    public void setMinimalImageSize(boolean minimalImageSize) {
        this.minimalImageSize = minimalImageSize;
    }

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

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Camera camera = (Camera) pipeline.getProperty("camera");
        Footprint footprint = (Footprint) pipeline.getProperty("footprint");
        if (footprint == null && pipeline.getProperty("nozzle") != null) {
            Nozzle nozzle = (Nozzle)pipeline.getProperty("nozzle");
            if (nozzle.getPart() != null) {
                footprint = nozzle.getPart().getPackage().getFootprint();
            }
        }

        if (camera == null) {
            throw new Exception("Property \"camera\" is required.");
        }
        if (footprint == null) {
            throw new Exception("Property \"footprint\" is required.");
        }

        double marginFactor = 1.5f;
        int minimumMarginSize = 3;
        
        if (minimalImageSize) {
            marginFactor = 1;
            minimumMarginSize = 0;
        }
        
        BufferedImage template = OpenCvUtils.createFootprintTemplate(camera, footprint, 0.0,
                footprintView == FootprintView.TopView, 
                padsColor, 
                (footprintView == FootprintView.Fiducial ? null : bodyColor), 
                backgroundColor, marginFactor, minimumMarginSize);

        return new Result(OpenCvUtils.toMat(template), ColorSpace.Bgr);
    }
}
