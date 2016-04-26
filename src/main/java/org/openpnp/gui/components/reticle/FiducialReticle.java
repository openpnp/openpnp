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
import java.awt.geom.AffineTransform;

import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;

public class FiducialReticle extends CrosshairReticle {
    public enum Shape {
        Circle, Square
    }

    private Shape shape;
    private boolean filled;
    private LengthUnit units;
    private double size;

    public FiducialReticle() {
        super();
        this.shape = Shape.Circle;
        this.filled = false;
        this.units = LengthUnit.Millimeters;
        this.size = 1;
        setColor(Color.red);
    }

    public Shape getShape() {
        return shape;
    }

    public void setShape(Shape shape) {
        this.shape = shape;
    }

    public boolean isFilled() {
        return filled;
    }

    public void setFilled(boolean filled) {
        this.filled = filled;
    }

    public LengthUnit getUnits() {
        return units;
    }

    public void setUnits(LengthUnit units) {
        this.units = units;
    }

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }

    @Override
    public void draw(Graphics2D g2d, LengthUnit cameraUnitsPerPixelUnits,
            double cameraUnitsPerPixelX, double cameraUnitsPerPixelY, double viewPortCenterX,
            double viewPortCenterY, int viewPortWidth, int viewPortHeight, double rotation) {

        super.draw(g2d, cameraUnitsPerPixelUnits, cameraUnitsPerPixelX, cameraUnitsPerPixelY,
                viewPortCenterX, viewPortCenterY, viewPortWidth, viewPortHeight, rotation);

        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(1f));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        AffineTransform origTx = g2d.getTransform();

        g2d.translate(viewPortCenterX, viewPortCenterY);
        // AffineTransform rotates positive clockwise, so we invert the value.
        g2d.rotate(Math.toRadians(-rotation));

        double pixelsPerUnitX = 1.0 / new Length(cameraUnitsPerPixelX, cameraUnitsPerPixelUnits)
                .convertToUnits(this.units).getValue();
        double pixelsPerUnitY = 1.0 / new Length(cameraUnitsPerPixelY, cameraUnitsPerPixelUnits)
                .convertToUnits(this.units).getValue();

        int width = (int) (size * pixelsPerUnitX);
        int height = (int) (size * pixelsPerUnitY);
        int x = (int) -(width / 2);
        int y = (int) -(height / 2);

        if (shape == Shape.Circle) {
            if (filled) {
                g2d.fillArc(x, y, width, height, 0, 360);
            }
            else {
                g2d.drawArc(x, y, width, height, 0, 360);
            }
        }
        else if (shape == Shape.Square) {
            if (filled) {
                g2d.fillRect(x, y, width, height);
            }
            else {
                g2d.drawRect(x, y, width, height);
            }
        }

        g2d.setTransform(origTx);
    }

}
