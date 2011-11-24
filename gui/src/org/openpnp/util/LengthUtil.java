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

import org.openpnp.LengthUnit;
import org.openpnp.Location;
import org.openpnp.Outline;
import org.openpnp.Point;

// TODO move these into their respective classes
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
			Point p = outline.getPoints().get(i);
			
			p = convertPoint(p, outline.getUnits(), toUnits);
			
			newOutline.addPoint(p.getX(), p.getY());
		}
		
		return newOutline;
	}
	
	public static Point convertPoint(Point point, LengthUnit fromUnits, LengthUnit toUnits) {
		double x = point.getX();
		double y = point.getY();
		x = LengthUtil.convertLength(x, fromUnits, toUnits);
		y = LengthUtil.convertLength(y, fromUnits, toUnits);
		return new Point(x, y);
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
