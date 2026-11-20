/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui.components.reticle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

import org.openpnp.model.Footprint;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;

/**
 * Draws a footprint at the center of the camera view: its pads, its body (dashed) and its marked
 * pad, such as pin 1, filled in red.
 */
public class FootprintReticle implements Reticle {
    public static final Color MARK_COLOR = new Color(255, 60, 60);
    static final Color MARK_FILL = new Color(255, 60, 60, 110);

    private Color color;
    private Footprint footprint;

    public FootprintReticle(Footprint footprint) {
        this.footprint = footprint;
        this.color = Color.yellow;
    }

    @Override
    public void draw(Graphics2D g2d, LengthUnit cameraUnitsPerPixelUnits,
            double cameraUnitsPerPixelX, double cameraUnitsPerPixelY, double viewPortCenterX,
            double viewPortCenterY, int viewPortWidth, int viewPortHeight, double rotation) {
        if (footprint == null) {
            return;
        }
        AffineTransform tx = toView(footprint.getUnits(), cameraUnitsPerPixelUnits,
                cameraUnitsPerPixelX, cameraUnitsPerPixelY, viewPortCenterX, viewPortCenterY,
                rotation);
        drawFootprint(g2d, footprint, tx, color);
    }

    /**
     * The transform from footprint units (Y up, centered on the footprint) to view pixels, with the
     * footprint at the given view point and rotated counter-clockwise by the given angle.
     */
    static AffineTransform toView(LengthUnit footprintUnits, LengthUnit cameraUnitsPerPixelUnits,
            double cameraUnitsPerPixelX, double cameraUnitsPerPixelY, double viewX, double viewY,
            double rotation) {
        // Determine the scaling factor to go from footprint units to camera units.
        double unitScale = new Length(1, footprintUnits).convertToUnits(cameraUnitsPerPixelUnits)
                .getValue();

        AffineTransform tx = new AffineTransform();
        tx.translate(viewX, viewY);
        // Camera units to pixels, with Y inverted for the left-hand view. This comes before the
        // rotation, so that each view axis keeps its own scale whatever the angle.
        tx.scale(1 / cameraUnitsPerPixelX, -1 / cameraUnitsPerPixelY);
        // Right-hand coordinates (Y up): counter-clockwise positive.
        tx.rotate(Math.toRadians(rotation));
        tx.scale(unitScale, unitScale);
        return tx;
    }

    /**
     * Draws a footprint with the given transform from footprint units to view pixels: the body
     * dashed, the pads outlined, and the marked pad filled and outlined in red.
     */
    public static void drawFootprint(Graphics2D g2d, Footprint footprint, AffineTransform tx,
            Color color) {
        Graphics2D g = (Graphics2D) g2d.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);

            if (footprint.getBodyWidth() > 0 && footprint.getBodyHeight() > 0) {
                g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                        new float[] {6f, 4f}, 0f));
                g.draw(tx.createTransformedShape(footprint.getBodyShape()));
            }

            g.setStroke(new BasicStroke(1f));
            Footprint.Pad marked = null;
            for (Footprint.Pad pad : footprint.getPads()) {
                if (pad.getMark()) {
                    marked = pad;
                }
                else {
                    g.draw(tx.createTransformedShape(pad.getShape()));
                }
            }
            // Drawn last, so that it stays on top where pads overlap.
            if (marked != null) {
                Shape shape = tx.createTransformedShape(marked.getShape());
                g.setColor(MARK_FILL);
                g.fill(shape);
                g.setColor(MARK_COLOR);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(shape);
            }
        }
        finally {
            g.dispose();
        }
    }
}
