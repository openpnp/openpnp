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

package org.openpnp.model.outline;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import org.openpnp.model.Point;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;

/**
 * Implementation of Outline which wraps a Path2D.Double to provide a
 * closed path.
 */
public class PolygonOutline extends AbstractOutline {
	@ElementList(inline = true)
	private List<Point> points = new ArrayList<Point>();
	
	private Path2D.Double shape;
	
	@SuppressWarnings("unused")
    @Commit
	private void commit() {
	    generateShape();
	}
	
	private void generateShape() {
        shape = new Path2D.Double();
        for (int i = 0, size = points.size(); i < size; i++) {
            Point point = points.get(i);
            if (i == 0) {
                shape.moveTo(point.x, point.y);
            }
            else {
                shape.lineTo(point.x, point.y);
            }
        }
        shape.closePath();
	}
	
	@Override
    public Shape getShape() {
	    return shape;
    }

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();

		for (Point point : points) {
			sb.append(point.x + "," + point.y + " -> ");
		}
		if (points.size() > 0) {
			sb.append(points.get(0).x + "," + points.get(0).y);
		}

		return String.format("units %s, points (%s)", units, sb);
	}
}
