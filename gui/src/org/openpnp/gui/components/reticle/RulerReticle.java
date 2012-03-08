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

import java.awt.Color;
import java.awt.Graphics2D;

import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;

public class RulerReticle implements Reticle {
	private LengthUnit units;
	private double unitsPerTick;
	private Color color;
	
	public RulerReticle() {
		this.units = LengthUnit.Millimeters;
		this.unitsPerTick = 1;
		this.color = Color.red;
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

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	@Override
	public void draw(Graphics2D g2d, 			
			LengthUnit cameraUnitsPerPixelUnits,
			double cameraUnitsPerPixelX, 
			double cameraUnitsPerPixelY, 
			double viewPortCenterX, 
			double viewPortCenterY,
			int viewPortWidth,
			int viewPortHeight) {

		g2d.setColor(color);
		
		// TODO performance, calculate all this stuff only when the incoming values change
		
		// draw the horizontal splitter
		g2d.drawLine((int) (viewPortCenterX - (viewPortWidth / 2)), (int) viewPortCenterY, (int) (viewPortCenterX + (viewPortWidth / 2)), (int) viewPortCenterY);
		// draw the vertical splitter
		g2d.drawLine((int) viewPortCenterX, (int) (viewPortCenterY - (viewPortHeight / 2)), (int) viewPortCenterX, (int) (viewPortCenterY + (viewPortHeight / 2)));
		double uppX = new Length(cameraUnitsPerPixelX, units).convertToUnits(this.units).getValue();
		double uppY = new Length(cameraUnitsPerPixelY, units).convertToUnits(this.units).getValue();
		double pixelsPerTickX = unitsPerTick / uppX;
		double pixelsPerTickY = unitsPerTick / uppY;
		int tickHeight = 10;
		double x;
		x = viewPortCenterX + pixelsPerTickX;
		while (x < (viewPortCenterX + (viewPortWidth / 2))) {
			g2d.drawLine((int) x, (int) viewPortCenterY - tickHeight, (int) x, (int) viewPortCenterY + tickHeight);
			x += pixelsPerTickX;
		}
		x = viewPortCenterX - pixelsPerTickX;
		while (x > (viewPortCenterX - (viewPortWidth / 2))) {
			g2d.drawLine((int) x, (int) viewPortCenterY - tickHeight, (int) x, (int) viewPortCenterY + tickHeight);
			x -= pixelsPerTickX;
		}
		
		double y;
		y = viewPortCenterY + pixelsPerTickY;
		while (y < (viewPortCenterY + (viewPortHeight / 2))) {
			g2d.drawLine((int) viewPortCenterX - tickHeight, (int) y, (int) viewPortCenterX + tickHeight, (int) y);
			y += pixelsPerTickY;
		}
		y = viewPortCenterY - pixelsPerTickY;
		while (y > (viewPortCenterY - (viewPortHeight / 2))) {
			g2d.drawLine((int) viewPortCenterX - tickHeight, (int) y, (int) viewPortCenterX + tickHeight, (int) y);
			y -= pixelsPerTickY;
		}
		
	}

}
