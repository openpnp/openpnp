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
 *
 * Changelog:
 * 03/10/2012 Ami: Add rotate using center point
 */

package org.openpnp.util;

import org.openpnp.model.Outline;
import org.openpnp.model.Point;


public class Utils2D {
	// TODO: Move these into their respective classes.
	public static Outline rotateTranslateScaleOutline(Outline outline, double c, double x, double y, double scaleX, double scaleY) {
		Outline newOutline = new Outline();
		newOutline.setUnits(outline.getUnits());
		for (int i = 0; i < outline.getPoints().size(); i++) {
			Point p = outline.getPoints().get(i);
			
			p = rotateTranslateScalePoint(p, c, x, y, scaleX, scaleY);
			
			newOutline.addPoint(p.getX(), p.getY());
		}
		
		return newOutline;
	}
	
	public static Point rotateTranslateScalePoint(Point point, double c, double x, double y, double scaleX, double scaleY) {
		point = rotatePoint(point, c);
		point = translatePoint(point, x, y);
		point = scalePoint(point, scaleX, scaleY);
		return point;
	}
	public static Point rotateTranslateCenterPoint(Point point, double c, double x, double y, Point center) {
		point = translatePoint(point,center.getX()*-1,center.getY()*-1);
		point = rotatePoint(point, c);
		point = translatePoint(point,center.getX(),center.getY());
		point = translatePoint(point, x, y);
	
		return point;
	}
	public static Point translatePoint(Point point, double x, double y) {
		return new Point(point.getX() + x, point.getY() + y);
	}
	
	public static Point rotatePoint(Point point, double c) {
		double x = point.getX();
		double y = point.getY();
		
		// convert degrees to radians
		c = c * Math.PI / 180.0;
		
		// rotate the points
		double xn = x * Math.cos(c) - y * Math.sin(c);
		double yn = x * Math.sin(c) + y * Math.cos(c);
		
		x = xn;
		y = yn;
		
		return new Point(x, y);
	}
	
	public static Point scalePoint(Point point, double scaleX, double scaleY) {
		return new Point(point.getX() * scaleX, point.getY() * scaleY);
	}
	
	public static void main(String[] args) {
		Point p = new Point(0, 1);
		System.out.println(rotatePoint(p, 90));
	}
	
	
	// // Calculate lengths of the sides
	// double a = Math.sqrt(Math.pow(pc.x - pb.x, 2) + Math.pow(pc.y - pb.y,
	// 2));
	// double b = Math.sqrt(Math.pow(pc.x - pa.x, 2) + Math.pow(pc.y - pa.y,
	// 2));
	// double c = Math.sqrt(Math.pow(pb.x - pa.x, 2) + Math.pow(pb.y - pa.y,
	// 2));
	//
	// // Change the variables around so that c is the largest side and a
	// and
	// // b are the others. This allows us to use the cosine rule to
	// // calculate the largest angle.
	// if (a >= b && a >= c) {
	// double t = c;
	// c = a;
	// a = b;
	// b = t;
	// }
	// else if (b >= a && b >= c) {
	// double t = c;
	// c = b;
	// b = a;
	// a = t;
	// }
	//
	// System.out.println(String.format("pa %s, pb %s, pc %s", pa, pb, pc));
	// System.out.println(String.format("a %f, b %f, c %f", a, b, c));
	//
	// // Use the cosine rule to find the angle opposite the largest side
	// double cosC = (Math.pow(a, 2) + Math.pow(b, 2) - Math.pow(c, 2)) / (2
	// * a * b);
	//
	// double C = Math.toDegrees(Math.acos(cosC));
	//
	// double sinA = (a * Math.sin(Math.toRadians(C))) / c;
	//
	// double A = Math.toDegrees(Math.asin(sinA));
	//
	// double B = 180.0 - C - A;
	//
	// System.out.println(String.format("A %f, B %f, C %f", A, B, C));
	
}
