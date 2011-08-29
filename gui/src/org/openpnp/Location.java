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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A Location is a 3D point in X, Y, Z space with a rotation component. The rotation is applied about the Z
 * axis.
 */
public class Location {
	private LengthUnit units;
	private double x;
	private double y;
	private double z;
	private double rotation;
	
	public void parse(Node n) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();

		units = Configuration.getLengthUnitAttribute(n, "units");
		x = Configuration.getDoubleAttribute(n, "x", 0);
		y = Configuration.getDoubleAttribute(n, "y", 0);
		z = Configuration.getDoubleAttribute(n, "z", 0);
		rotation = Configuration.getDoubleAttribute(n, "rotation", 0);
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
	
	public double getZ() {
		return z;
	}

	public void setZ(double z) {
		this.z = z;
	}

	public double getRotation() {
		return rotation;
	}

	public void setRotation(double rotation) {
		this.rotation = rotation;
	}
	
	public LengthUnit getUnits() {
		return units;
	}

	public void setUnits(LengthUnit units) {
		this.units = units;
	}

	@Override
	public String toString() {
		return String.format("units %s, x %f, y %f, z %f, rotation %f", units, x, y, z, rotation);
	}
}
