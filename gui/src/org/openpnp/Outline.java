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

package org.openpnp;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An Outline is a polygon shape with attached units that can be used for bounds checking
 * and can be drawn to a Graphics for display to the user.
 */
public class Outline {
	@ElementList(inline=true)
	private List<Point> points = new ArrayList<Point>();
	@Attribute
	private LengthUnit units;
	
	public void parse(Node n) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();

		units = Configuration.getLengthUnitAttribute(n, "units");
		
		NodeList pointNodes = (NodeList) xpath.evaluate("Point", n, XPathConstants.NODESET);

		for (int i = 0; i < pointNodes.getLength(); i++) {
			Node pointNode = pointNodes.item(i);
			addPoint(
					Configuration.getDoubleAttribute(pointNode, "x"),
					Configuration.getDoubleAttribute(pointNode, "y"));
		}
	}
	
	public void addPoint(double x, double y) {
		points.add(new Point(x, y));
	}
	
	public LengthUnit getUnits() {
		return units;
	}

	public void setUnits(LengthUnit units) {
		this.units = units;
	}
	
	public List<Point> getPoints() {
		return points;
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
