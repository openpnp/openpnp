package org.openpnp.util;

import java.awt.geom.Point2D;

import org.openpnp.LengthUnit;
import org.openpnp.Location;
import org.openpnp.Outline;

public class LengthUtil {
	public static Location convertLocation(Location l, LengthUnit toUnits) {
		Location ln = new Location();
		ln.setUnits(toUnits);
		ln.setX(LengthUtil.convertLength(l.getX(), l.getUnits(), toUnits));
		ln.setY(LengthUtil.convertLength(l.getY(), l.getUnits(), toUnits));
		ln.setZ(LengthUtil.convertLength(l.getZ(), l.getUnits(), toUnits));
		ln.setRotation(l.getRotation());
		return ln;
	}
	
	public static Outline convertOutline(Outline outline, LengthUnit toUnits) {
		Outline newOutline = new Outline();
		newOutline.setUnits(outline.getUnits());
		for (int i = 0; i < outline.getPoints().size(); i++) {
			Point2D.Double p = outline.getPoints().get(i);
			
			p = convertPoint(p, outline.getUnits(), toUnits);
			
			newOutline.addPoint(p.getX(), p.getY());
		}
		
		return newOutline;
	}
	
	public static Point2D.Double convertPoint(Point2D.Double point, LengthUnit fromUnits, LengthUnit toUnits) {
		double x = point.getX();
		double y = point.getY();
		x = LengthUtil.convertLength(x, fromUnits, toUnits);
		y = LengthUtil.convertLength(y, fromUnits, toUnits);
		return new Point2D.Double(x, y);
	}
	
	public static double convertLength(double length, LengthUnit fromUnits, LengthUnit toUnits) {
		if (fromUnits == toUnits) {
			return length;
		}
		double mm = 0;
		if (fromUnits == LengthUnit.Millimeters) {
			mm = length;
		}
		else if (fromUnits == LengthUnit.Centimeters) {
			mm = length * 10;
		}
		else if (fromUnits == LengthUnit.Meters) {
			mm = length * 1000;
		}
		else if (fromUnits == LengthUnit.Inches) {
			mm = length * 25.4;
		}
		else if (fromUnits == LengthUnit.Feet) {
			mm = length * 25.4 * 12;
		}
		else {
			return Double.NaN;
		}
		
		if (toUnits == LengthUnit.Millimeters) {
			return mm;
		}
		else if (toUnits == LengthUnit.Centimeters) {
			return mm / 10;
		}
		else if (toUnits == LengthUnit.Meters) {
			return mm / 1000;
		}
		else if (toUnits == LengthUnit.Inches) {
			return mm * (1 / 25.4);
		}
		else if (toUnits == LengthUnit.Feet) {
			return mm * (1 / 25.4) * 12;
		}
		else {
			return Double.NaN;
		}
	}
}
