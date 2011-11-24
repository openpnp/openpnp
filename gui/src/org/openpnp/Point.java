package org.openpnp;

import org.simpleframework.xml.Attribute;

public class Point {
	@Attribute
	public double x;
	@Attribute
	public double y;
	
	public Point() {
		
	}
	
	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}
}