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
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Stage;

@Stage(description="Creates a template from the specified footprint and camera properties. The template is scaled to the camera's units.")
public class CreateFootprintTemplateImage extends CvStage {
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
        
        Location unitsPerPixel = camera.getUnitsPerPixel();
        
        Shape shape = footprint.getShape();

        if (shape == null) {
            throw new Exception(
                    "Invalid footprint found, unable to create template for fiducial match. See https://github.com/openpnp/openpnp/wiki/Fiducials.");
        }

        // Determine the scaling factor to go from Outline units to
        // Camera units.
        Length l = new Length(1, footprint.getUnits());
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
                    "Invalid footprint found, unable to create template for fiducial match. Width and height of pads must be greater than 0. See https://github.com/openpnp/openpnp/wiki/Fiducials.");
        }

        // Make the image 50% bigger than the shape. This gives better
        // recognition performance because it allows some border around the edges.
        double width = bounds.getWidth() * 1.5;
        double height = bounds.getHeight() * 1.5;
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
