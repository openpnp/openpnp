/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui.components.reticle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;

public class RulerReticle extends CrosshairReticle {
    private LengthUnit units;
    private double unitsPerTick;

    public RulerReticle() {
        super();
        this.units = LengthUnit.Millimeters;
        this.unitsPerTick = 1;
        setColor(Color.red);
    }

    public LengthUnit getUnits() {
        return units;
    }

    public void setUnits(LengthUnit units) {
        this.units = units;
    }

    public double getUnitsPerTick() {
        return unitsPerTick;
    }

    public void setUnitsPerTick(double unitsPerTick) {
        this.unitsPerTick = unitsPerTick;
    }

    @Override
    public void draw(Graphics2D g2d, LengthUnit cameraUnitsPerPixelUnits,
            double cameraUnitsPerPixelX, double cameraUnitsPerPixelY,
            double viewPortCenterX, double viewPortCenterY, int viewPortWidth,
            int viewPortHeight, double rotation) {

        super.draw(g2d, cameraUnitsPerPixelUnits, cameraUnitsPerPixelX,
                cameraUnitsPerPixelY, viewPortCenterX, viewPortCenterY,
                viewPortWidth, viewPortHeight, rotation);

        g2d.setStroke(new BasicStroke(1f));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // Calculate half the diagonal size of the viewport.
        int halfDiagonal = (int) (Math.sqrt(Math.pow(viewPortWidth, 2)
                + Math.pow(viewPortHeight, 2)) / 2.0);

        AffineTransform origTx = g2d.getTransform();

        g2d.translate(viewPortCenterX, viewPortCenterY);
        // AffineTransform rotates positive clockwise, so we invert the value.
        g2d.rotate(Math.toRadians(-rotation));

        double uppX = new Length(cameraUnitsPerPixelX, cameraUnitsPerPixelUnits)
                .convertToUnits(this.units).getValue();
        double uppY = new Length(cameraUnitsPerPixelY, cameraUnitsPerPixelUnits)
                .convertToUnits(this.units).getValue();
        double pixelsPerTickX = unitsPerTick / uppX;
        double pixelsPerTickY = unitsPerTick / uppY;
        int tickLength = 10;

        g2d.setColor(color);
        for (int i = 1; i < (halfDiagonal / pixelsPerTickX); i++) {
            int x = (int) (i * pixelsPerTickX);
            g2d.drawLine(x, -tickLength, x, tickLength);
            g2d.drawLine(-x, -tickLength, -x, tickLength);
        }

        for (int i = 1; i < (halfDiagonal / pixelsPerTickY); i++) {
            int y = (int) (i * pixelsPerTickY);
            g2d.setColor(color);
            g2d.drawLine(-tickLength, y, tickLength, y);
            g2d.setColor(complimentaryColor);
            g2d.drawLine(-tickLength, -y, tickLength, -y);
        }
        g2d.setTransform(origTx);
    }

}
