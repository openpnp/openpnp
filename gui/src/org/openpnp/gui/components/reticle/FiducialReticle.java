package org.openpnp.gui.components.reticle;

import java.awt.Color;
import java.awt.Graphics2D;

import org.openpnp.LengthUnit;
import org.openpnp.util.LengthUtil;

public class FiducialReticle implements Reticle {
	public enum Shape {
		Circle,
		Square
	}
	private Shape shape;
	private boolean filled;
	private Color color;
	private LengthUnit units;
	private double size;
	
	public FiducialReticle() {
		this.shape = Shape.Circle;
		this.filled = false;
		this.color = Color.red;
		this.units = LengthUnit.Millimeters;
		this.size = 1;
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

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
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
		
		double pixelsPerUnitX = 1.0 / LengthUtil.convertLength(cameraUnitsPerPixelX, cameraUnitsPerPixelUnits, this.units);
		double pixelsPerUnitY = 1.0 / LengthUtil.convertLength(cameraUnitsPerPixelY, cameraUnitsPerPixelUnits, this.units);

		if (shape == Shape.Circle) {
			int width = (int) (size * pixelsPerUnitX);
			int height = (int) (size * pixelsPerUnitY);
			int x = (int) (viewPortCenterX - (width / 2));
			int y = (int) (viewPortCenterY - (height / 2));
			
			if (filled) {
				g2d.fillArc(x, y, width, height, 0, 360);
			}
			else {
				g2d.drawArc(x, y, width, height, 0, 360);
			}
		}
		else if (shape == Shape.Square) {
			int width = (int) (size * pixelsPerUnitX);
			int height = (int) (size * pixelsPerUnitY);
			int x = (int) (viewPortCenterX - (width / 2));
			int y = (int) (viewPortCenterY - (height / 2));
			
			if (filled) {
				g2d.fillRect(x, y, width, height);
			}
			else {
				g2d.drawRect(x, y, width, height);
			}
		}
	}

}
