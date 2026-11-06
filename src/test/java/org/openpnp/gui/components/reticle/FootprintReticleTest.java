package org.openpnp.gui.components.reticle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;
import org.openpnp.model.Footprint;
import org.openpnp.model.LengthUnit;

public class FootprintReticleTest {
    private static Footprint.Pad pad(String name, double x, boolean mark) {
        Footprint.Pad pad = new Footprint.Pad();
        pad.setName(name);
        pad.setX(x);
        pad.setWidth(1);
        pad.setHeight(1);
        pad.setMark(mark);
        return pad;
    }

    private static Point2D view(AffineTransform tx, double x, double y) {
        return tx.transform(new Point2D.Double(x, y), null);
    }

    @Test
    public void squarePixelsDrawAsBefore() {
        double rotation = 30;
        AffineTransform tx = FootprintReticle.toView(LengthUnit.Millimeters,
                LengthUnit.Millimeters, 0.05, 0.05, 320, 240, rotation);
        // The transform used before: rotated in the view, then scaled with Y inverted.
        AffineTransform before = new AffineTransform();
        before.translate(320, 240);
        before.rotate(Math.toRadians(-rotation));
        before.scale(1 / 0.05, -1 / 0.05);
        for (double[] p : new double[][] {{1, 0}, {0, 1}, {-2, 3}}) {
            Point2D a = view(tx, p[0], p[1]);
            Point2D b = view(before, p[0], p[1]);
            assertEquals(b.getX(), a.getX(), 1e-9);
            assertEquals(b.getY(), a.getY(), 1e-9);
        }
    }

    @Test
    public void eachViewAxisKeepsItsOwnScaleWhenRotated() {
        // 0.1 mm per pixel across, 0.05 mm per pixel down. Turned 90°, the footprint's X runs
        // down the view, so 1 mm along it is 20 pixels, not 10.
        AffineTransform tx = FootprintReticle.toView(LengthUnit.Millimeters,
                LengthUnit.Millimeters, 0.1, 0.05, 0, 0, 90);
        Point2D p = view(tx, 1, 0);
        assertEquals(0, p.getX(), 1e-9);
        assertEquals(-20, p.getY(), 1e-9);
    }

    @Test
    public void footprintUnitsAreConverted() {
        AffineTransform tx = FootprintReticle.toView(LengthUnit.Inches, LengthUnit.Millimeters,
                0.1, 0.1, 0, 0, 0);
        assertEquals(254, view(tx, 1, 0).getX(), 1e-9);
    }

    @Test
    public void onlyTheMarkedPadIsDrawnRed() {
        Footprint footprint = new Footprint();
        footprint.addPad(pad("1", -2, false));
        footprint.addPad(pad("2", 2, true));

        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        AffineTransform tx = FootprintReticle.toView(LengthUnit.Millimeters,
                LengthUnit.Millimeters, 0.1, 0.1, 50, 50, 0);
        FootprintReticle.drawFootprint(g, footprint, tx, Color.yellow);
        g.dispose();

        // Pad 2 is marked: filled red. Pad 1 is named "1" but not marked: outline only.
        Color marked = new Color(image.getRGB(70, 50));
        assertTrue(marked.getRed() > 0 && marked.getGreen() < marked.getRed() / 2,
                "marked pad center " + marked);
        assertEquals(Color.black, new Color(image.getRGB(30, 50)));
        // Its outline is yellow.
        Color edge = new Color(image.getRGB(25, 50));
        assertTrue(edge.getRed() > 0 && edge.getGreen() > 0 && edge.getBlue() == 0,
                "unmarked pad edge " + edge);
    }
}
