package org.openpnp;

import java.awt.Color;
import java.awt.Graphics2D;

import org.openpnp.util.LengthUtil;

public class CrosshairReticle implements Reticle {
	private Color color;
	
	public CrosshairReticle(Color color) {
		setColor(color);
	}
	
	public void setColor(Color color) {
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
	}

}
