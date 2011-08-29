package org.openpnp.util;
import java.awt.geom.Point2D;

import org.openpnp.LengthUnit;
import org.openpnp.Location;
import org.openpnp.Outline;


public class Utils2D {
	public static Outline rotateTranslateScaleOutline(Outline outline, double a, double x, double y, double scale) {
		Outline newOutline = new Outline();
		newOutline.setUnits(outline.getUnits());
		for (int i = 0; i < outline.getPoints().size(); i++) {
			Point2D.Double p = outline.getPoints().get(i);
			
			p = rotateTranslateScalePoint(p, a, x, y, scale);
			
			newOutline.addPoint(p.getX(), p.getY());
		}
		
		return newOutline;
	}
	
	public static Point2D.Double rotateTranslateScalePoint(Point2D.Double point, double a, double x, double y, double scale) {
		point = rotatePoint(point, a);
		point = translatePoint(point, x, y);
		point = scalePoint(point, scale);
		return point;
	}
	
	public static Point2D.Double translatePoint(Point2D.Double point, double x, double y) {
		return new Point2D.Double(point.getX() + x, point.getY() + y);
	}
	
	public static Point2D.Double rotatePoint(Point2D.Double point, double a) {
		double x = point.getX();
		double y = point.getY();
		
		// convert degrees to radians
		a = a * Math.PI / 180.0;
		a *= -1;
		
		// rotate the points
		double xn = x * Math.cos(a) - y * Math.sin(a);
		double yn = x * Math.sin(a) + y * Math.cos(a);
		
		x = xn;
		y = yn;
		
		return new Point2D.Double(x, y);
	}
	
	public static Point2D.Double scalePoint(Point2D.Double point, double scale) {
		return new Point2D.Double(point.getX() * scale, point.getY() * scale);
	}
}
