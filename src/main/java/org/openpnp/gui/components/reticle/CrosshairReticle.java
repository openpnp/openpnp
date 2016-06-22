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

import org.openpnp.model.LengthUnit;
import org.openpnp.util.HslColor;

public class CrosshairReticle implements Reticle {
    protected Color color;
    protected Color complimentaryColor;

    public CrosshairReticle() {
        setColor(Color.red);
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
        complimentaryColor = new HslColor(color).getComplementary();
    }

    @Override
    public void draw(Graphics2D g2d, LengthUnit cameraUnitsPerPixelUnits,
            double cameraUnitsPerPixelX, double cameraUnitsPerPixelY, double viewPortCenterX,
            double viewPortCenterY, int viewPortWidth, int viewPortHeight, double rotation) {

        g2d.setStroke(new BasicStroke(1f));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Calculate half the diagonal size of the viewport.
        int halfDiagonal =
                (int) (Math.sqrt(Math.pow(viewPortWidth, 2) + Math.pow(viewPortHeight, 2)) / 2.0);

        AffineTransform origTx = g2d.getTransform();

        g2d.translate(viewPortCenterX, viewPortCenterY);
        // AffineTransform rotates positive clockwise, so we invert the value.
        g2d.rotate(Math.toRadians(-rotation));
        g2d.setColor(color);
        g2d.drawLine(0, 0, halfDiagonal, 0);
        g2d.drawLine(0, 0, -halfDiagonal, 0);
        g2d.drawLine(0, 0, 0, halfDiagonal);
        g2d.setColor(complimentaryColor);
        g2d.drawLine(0, 0, 0, -halfDiagonal);

        g2d.setTransform(origTx);
    }
}
