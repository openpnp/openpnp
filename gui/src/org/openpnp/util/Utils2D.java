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

package org.openpnp.util;

import java.awt.geom.Point2D;

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
