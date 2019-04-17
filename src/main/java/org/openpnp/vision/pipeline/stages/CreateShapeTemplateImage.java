package org.openpnp.vision.pipeline.stages;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.openpnp.model.Footprint;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(description="Creates a template from the specified shape and camera properties. The shape is scaled from Millimeters to the camera's units.")
public class CreateShapeTemplateImage extends CvStage {
    @Attribute
    @Property(description = "Name of the shape property to load the template shape from. The shape itself must be provided by the pipeline user.")
    private String templateShapeName;

    @Attribute
    @Property(description = "Oversize factor for border recognition around shape.")
    private double oversize = 1.5;

    public String getTemplateShapeName() {
        return templateShapeName;
    }

    public void setTemplateShapeName(String templateShapeName) {
        this.templateShapeName = templateShapeName;
    }

    public double getOversize() {
        return oversize;
    }

    public void setOversize(double oversize) {
        this.oversize = oversize;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (templateShapeName == null || templateShapeName.trim()
                .equals("")) {
            return null;
        }

        Camera camera = (Camera) pipeline.getProperty("camera");
        Shape shape = (Shape) pipeline.getProperty(templateShapeName);

        if (camera == null) {
            throw new Exception("Property \"camera\" is required.");
        }
        if (shape == null) {
            throw new Exception("Property named after templateShapeName is required.");
        }

        Location unitsPerPixel = camera.getUnitsPerPixel();

        // Determine the scaling factor to go from Outline units to
        // Camera units.
        Length l = new Length(1, LengthUnit.Millimeters);
        l = l.convertToUnits(unitsPerPixel.getUnits());
        double unitScale = l.getValue();

        // Create a transform to scale the Shape by
        AffineTransform tx = new AffineTransform();

        // First we scale by units to convert the units and then we scale
        // by the camera X and Y units per pixels to get pixel locations.
        tx.scale(unitScale, unitScale);
        tx.scale(1.0 / unitsPerPixel.getX(), 1.0 / unitsPerPixel.getY());

        // Transform the Shape and draw it out.
        shape = tx.createTransformedShape(shape);

        Rectangle2D bounds = shape.getBounds2D();

        if (bounds.getWidth() == 0 || bounds.getHeight() == 0) {
            throw new Exception(
                    "Invalid shape found, unable to create template for shape match.");
        }

        // Make the image bigger than the shape. This gives better
        // recognition performance because it allows some border around the edges.
        double oversizeEffective = oversize;
        if (oversizeEffective <= 0.0) {
            oversizeEffective = 1.5;
        }
        double width = Math.ceil(bounds.getWidth() * oversizeEffective);
        double height = Math.ceil(bounds.getHeight() * oversizeEffective);
        BufferedImage template =
                new BufferedImage((int) width, (int) height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) template.getGraphics();

        g2d.setStroke(new BasicStroke(1f));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.white);
        // center the drawing
        g2d.translate(width / 2, height / 2);
        g2d.fill(shape);

        g2d.dispose();

        return new Result(OpenCvUtils.toMat(template));
    }
}
