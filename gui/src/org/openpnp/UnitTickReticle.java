package org.openpnp;

import java.awt.Color;
import java.awt.Graphics2D;

import org.openpnp.util.LengthUtil;

public class UnitTickReticle implements Reticle {
	private LengthUnit units;
	private double unitsPerTick;
	private Color color;
	
	public UnitTickReticle(LengthUnit units, double unitsPerTick, Color color) {
		this.units = units;
		this.unitsPerTick = unitsPerTick;
		this.color = color;
	}

	@Override
	public void draw(Graphics2D g2d, LengthUnit units, double unitsPerPixelX,
			double unitsPerPixelY, double centerX, double centerY,
			int viewPortWidth, int viewPortHeight) {
		
		g2d.setColor(color);
		
		// draw the horizontal splitter
		g2d.drawLine((int) (centerX - (viewPortWidth / 2)), (int) centerY, (int) (centerX + (viewPortWidth / 2)), (int) centerY);
		// draw the vertical splitter
		g2d.drawLine((int) centerX, (int) (centerY - (viewPortHeight / 2)), (int) centerX, (int) (centerY + (viewPortHeight / 2)));
		// draw the vertical ticks along the horizontal splitter
		double uppX = LengthUtil.convertLength(unitsPerPixelX, units, this.units);
		double uppY = LengthUtil.convertLength(unitsPerPixelY, units, this.units);
		double pixelsPerTickX = unitsPerTick / uppX;
		double pixelsPerTickY = unitsPerTick / uppY;
		int tickHeight = 10;
		double x;
		x = centerX + pixelsPerTickX;
		while (x < (centerX + (viewPortWidth / 2))) {
			g2d.drawLine((int) x, (int) centerY - tickHeight, (int) x, (int) centerY + tickHeight);
			x += pixelsPerTickX;
		}
		x = centerX - pixelsPerTickX;
		while (x > (centerX - (viewPortWidth / 2))) {
			g2d.drawLine((int) x, (int) centerY - tickHeight, (int) x, (int) centerY + tickHeight);
			x -= pixelsPerTickX;
		}
		
		double y;
		y = centerY + pixelsPerTickY;
		while (y < (centerY + (viewPortHeight / 2))) {
			g2d.drawLine((int) centerX - tickHeight, (int) y, (int) centerX + tickHeight, (int) y);
			y += pixelsPerTickY;
		}
		y = centerY - pixelsPerTickY;
		while (y > (centerY - (viewPortHeight / 2))) {
			g2d.drawLine((int) centerX - tickHeight, (int) y, (int) centerX + tickHeight, (int) y);
			y -= pixelsPerTickY;
		}
		
	}

}
