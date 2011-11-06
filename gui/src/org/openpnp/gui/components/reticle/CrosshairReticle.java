package org.openpnp.gui.components.reticle;

import java.awt.Color;
import java.awt.Graphics2D;

import org.openpnp.LengthUnit;
import org.openpnp.util.LengthUtil;

public class CrosshairReticle implements Reticle {
	private Color color;
	
	public CrosshairReticle() {
		this.color = Color.red;
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
		
		// draw the horizontal splitter
		g2d.drawLine((int) (viewPortCenterX - (viewPortWidth / 2)), (int) viewPortCenterY, (int) (viewPortCenterX + (viewPortWidth / 2)), (int) viewPortCenterY);
		// draw the vertical splitter
		g2d.drawLine((int) viewPortCenterX, (int) (viewPortCenterY - (viewPortHeight / 2)), (int) viewPortCenterX, (int) (viewPortCenterY + (viewPortHeight / 2)));
	}

}
