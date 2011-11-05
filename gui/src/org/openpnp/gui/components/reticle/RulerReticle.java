package org.openpnp.gui.components.reticle;

import java.awt.Color;
import java.awt.Graphics2D;

import org.openpnp.LengthUnit;
import org.openpnp.util.LengthUtil;

public class RulerReticle implements Reticle {
	private LengthUnit units;
	private double unitsPerTick;
	private Color color;
	
	public RulerReticle(LengthUnit units, double unitsPerTick, Color color) {
		setUnits(units);
		setColor(color);
		setUnitsPerTick(unitsPerTick);
	}
	
	public void setColor(Color color) {
		this.color = color;
	}
	
	public void setUnits(LengthUnit units) {
		this.units = units;
	}
	
	public void setUnitsPerTick(double unitsPerTick) {
		this.unitsPerTick = unitsPerTick;
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
		// draw the vertical ticks along the horizontal splitter
		double uppX = LengthUtil.convertLength(cameraUnitsPerPixelX, units, this.units);
		double uppY = LengthUtil.convertLength(cameraUnitsPerPixelY, units, this.units);
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
