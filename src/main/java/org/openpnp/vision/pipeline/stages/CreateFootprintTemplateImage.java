package org.openpnp.vision.pipeline.stages;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.openpnp.model.Footprint;
import org.openpnp.model.Length;
import org.openpnp.spi.Camera;
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

    @Attribute(required = false)
    @Property(description = "Vision offset in X in pixels. The footprint is visually asymmetric, and its detection center offset to the right (+) or left (-).")
    private double xOffset = 0;

    @Attribute(required = false)
    @Property(description = "Vision offset in Y in pixels. The footprint is visually asymmetric, and its detection center offset to the top (+) or bottom (-).")
    private double yOffset = 0;

    @Attribute(required = false)
    @Property(description = "Rotation")
    private double rotation = 0;

    @Attribute(required = false)
    @Property(description = "Max width of the template. Set 0 to get the full width.")
    private double maxWidth = 0;

    @Attribute(required = false)
    @Property(description = "Max height of the template. Set 0 to get the full height.")
    private double maxHeight = 0;

    @Attribute(required = false)
    @Property(description = "Name of the property through which OpenPnP controls this stage. Use \"footprint\" for standard control.")
    private String propertyName = "footprint";

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

    public double getxOffset() {
        return xOffset;
    }

    public void setxOffset(double xOffset) {
        this.xOffset = xOffset;
    }

    public double getyOffset() {
        return yOffset;
    }

    public void setyOffset(double yOffset) {
        this.yOffset = yOffset;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    public double getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(double maxWidth) {
        this.maxWidth = maxWidth;
    }

    public double getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(double maxHeight) {
        this.maxHeight = maxHeight;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (propertyName == null || propertyName.isEmpty()) {
            propertyName = "footprint";
        }

        Camera camera = (Camera) pipeline.getProperty("camera");
        Footprint footprint = (Footprint) pipeline.getProperty("footprint");
        if (camera == null) {
            throw new Exception("Property \"camera\" is required.");
        }
        if (footprint == null) {
            throw new Exception("Property \""+propertyName+"\" is required.");
        }

        double rotation = this.rotation;
        double xOffset = this.xOffset;
        double yOffset = this.xOffset;
        double maxWidth = this.maxWidth;
        double maxHeight = this.maxHeight;
        rotation = getPossiblePipelinePropertyOverride(xOffset, pipeline,
                propertyName + ".rotation", Double.class);
        xOffset = getPossiblePipelinePropertyOverride(xOffset, pipeline,
                propertyName + ".xOffset", Double.class, Integer.class, Length.class);
        yOffset = getPossiblePipelinePropertyOverride(xOffset, pipeline,
                propertyName + ".yOffset", Double.class, Integer.class, Length.class);
        maxWidth = getPossiblePipelinePropertyOverride(maxWidth, pipeline,
                propertyName + ".maxWidth", Double.class, Integer.class, Length.class);
        maxHeight = getPossiblePipelinePropertyOverride(maxHeight, pipeline,
                propertyName + ".maxHeight", Double.class, Integer.class, Length.class);

        double marginFactor = 1.5f;
        int minimumMarginSize = 3;

        if (minimalImageSize) {
            marginFactor = 1;
            minimumMarginSize = 0;
        }
        
        BufferedImage template = OpenCvUtils.createFootprintTemplate(camera, footprint, rotation,
                xOffset, yOffset, maxWidth, maxHeight,
                footprintView == FootprintView.TopView, 
                padsColor, (footprintView == FootprintView.Fiducial ? null : bodyColor), backgroundColor, 
                marginFactor, minimumMarginSize);

        return new Result(OpenCvUtils.toMat(template), ColorSpace.Bgr);
    }
}
