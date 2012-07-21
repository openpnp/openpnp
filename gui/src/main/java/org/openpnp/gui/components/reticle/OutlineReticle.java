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
import java.awt.Insets;
import java.awt.RenderingHints;

import org.openpnp.model.LengthUnit;
import org.openpnp.model.Outline;
import org.openpnp.model.Point;
import org.openpnp.util.HslColor;
import org.openpnp.util.Utils2D;

public class OutlineReticle implements Reticle {
	private Color color;
	private Color complimentaryColor;
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
		complimentaryColor = new HslColor(color).getComplementary();
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
		
		g2d.setStroke(new BasicStroke(1f));
		g2d.setColor(Color.red);

		// Convert the outline to the camera's units per pixel
		Outline outline = this.outline.convertToUnits(cameraUnitsPerPixelUnits);
		
		// Rotate to the head's rotation and scale to fit the window
		outline = Utils2D.rotateTranslateScaleOutline(outline, rotation, 0, 0, 1.0 / cameraUnitsPerPixelX, 1.0 / cameraUnitsPerPixelY);
		
		// TODO: Cache the results of the above two operations.
		
		// Draw it
		for (int i = 0; i < outline.getPoints().size() - 1; i++) {
			Point p1 = outline.getPoints().get(i);
			Point p2 = outline.getPoints().get(i + 1);

			g2d.drawLine(
					(int) (p1.getX() + viewPortCenterX), 
					(int) (p1.getY() + viewPortCenterY), 
					(int) (p2.getX() + viewPortCenterX),
					(int) (p2.getY() + viewPortCenterY));
		}

		Point p1 = outline.getPoints().get(outline.getPoints().size() - 1);
		Point p2 = outline.getPoints().get(0);

		g2d.drawLine(
				(int) (p1.getX() + viewPortCenterX), 
				(int) (p1.getY() + viewPortCenterY), 
				(int) (p2.getX() + viewPortCenterX),
				(int) (p2.getY() + viewPortCenterY));
	}
}
