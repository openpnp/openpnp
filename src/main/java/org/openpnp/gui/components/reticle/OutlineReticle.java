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
import java.awt.Shape;
import java.awt.geom.AffineTransform;

import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Outline;

public class OutlineReticle implements Reticle {
	private Color color;
	private Outline outline;
	
	public OutlineReticle(Outline outline) {
		setOutline(outline);
		setColor(Color.red);
	}
	
	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}
	
	public Outline getOutline() {
		return outline;
	}

	public void setOutline(Outline outline) {
		this.outline = outline;
	}

	@Override
	public void draw(Graphics2D g2d,
			LengthUnit cameraUnitsPerPixelUnits,
			double cameraUnitsPerPixelX, 
			double cameraUnitsPerPixelY, 
			double viewPortCenterX, 
			double viewPortCenterY,
			int viewPortWidth,
			int viewPortHeight,
			double rotation) {
		
		g2d.setStroke(new BasicStroke(1f));
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setColor(color);
		

		Shape shape = outline.getShape();
		if (shape == null) {
		    return;
		}
		// Determine the scaling factor to go from Outline units to
		// Camera units.
		Length l = new Length(1, outline.getUnits());
		l = l.convertToUnits(cameraUnitsPerPixelUnits);
		double unitScale = l.getValue();
		
		// Create a transform to scale the Shape by
		AffineTransform tx = new AffineTransform();
		
        tx.translate(viewPortCenterX, viewPortCenterY);
        // AffineTransform rotates positive clockwise, so we invert the value.
        tx.rotate(Math.toRadians(-rotation));
        
        // First we scale by units to convert the units and then we scale
        // by the camera X and Y units per pixels to get pixel locations.
		tx.scale(unitScale, unitScale);
		tx.scale(1.0 / cameraUnitsPerPixelX, 1.0 / cameraUnitsPerPixelY);
		
		// Transform the Shape and draw it out.
		shape = tx.createTransformedShape(shape);
		g2d.draw(shape);
	}
}
