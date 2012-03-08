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

package org.openpnp.model;

import org.simpleframework.xml.Attribute;

/**
 * A Location is a 3D point in X, Y, Z space with a rotation component. The rotation is applied about the Z
 * axis.
 */
public class Location extends AbstractModelObject implements Cloneable {
	@Attribute
	private LengthUnit units;
	@Attribute(required=false)
	private double x;
	@Attribute(required=false)
	private double y;
	@Attribute(required=false)
	private double z;
	@Attribute(required=false)
	private double rotation;
	
	public Location() {
		this(null);
	}
	
	public Location(LengthUnit units) {
		this(units, 0, 0, 0, 0);
	}
	
	public Location(LengthUnit units, double x, double y, double z, double rotation) {
		setUnits(units);
		setX(x);
		setY(y);
		setZ(z);
		setRotation(rotation);
	}
	
	public double getX() {
		return x;
	}
	
	public void setX(double x) {
		double oldValue = this.x;
		this.x = x;
		firePropertyChange("x", oldValue, x);
	}
	
	public double getY() {
		return y;
	}
	
	public void setY(double y) {
		double oldValue = this.y;
		this.y = y;
		firePropertyChange("y", oldValue, y);
	}
	
	public double getZ() {
		return z;
	}

	public void setZ(double z) {
		double oldValue = this.z;
		this.z = z;
		firePropertyChange("z", oldValue, z);
	}

	public double getRotation() {
		return rotation;
	}

	public void setRotation(double rotation) {
		double oldValue = this.rotation;
		this.rotation = rotation;
		firePropertyChange("rotation", oldValue, rotation);
	}
	
	public LengthUnit getUnits() {
		return units;
	}

	public void setUnits(LengthUnit units) {
		LengthUnit oldValue = this.units;
		this.units = units;
		firePropertyChange("units", oldValue, units);
	}
	
	public Location convertToUnits(LengthUnit units) {
		Location location = new Location();
		location.setX(new Length(x, this.units).convertToUnits(units).getValue());
		location.setY(new Length(y, this.units).convertToUnits(units).getValue());
		location.setZ(new Length(z, this.units).convertToUnits(units).getValue());
		location.setRotation(rotation);
		location.setUnits(units);
		return location;
	}
	
	public double getLinearDistanceTo(Location location) {
		return getLinearDistanceTo(location.getX(), location.getY());
	}

	public double getLinearDistanceTo(double x, double y) {
		return (Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.y - y, 2)));
	}
	@Override
	public String toString() {
		return String.format("units %s, x %f, y %f, z %f, rotation %f", units, x, y, z, rotation);
	}
	
	public static void main(String[] args) {
		Location location = new Location(LengthUnit.Millimeters, 1, 1, 2, 42.24);
		System.out.println(location);
		System.out.println(location.convertToUnits(LengthUnit.Millimeters));
		System.out.println(location.convertToUnits(LengthUnit.Meters));
		System.out.println(location.convertToUnits(LengthUnit.Centimeters));
		System.out.println(location.convertToUnits(LengthUnit.Inches));
		System.out.println(location.convertToUnits(LengthUnit.Feet));
	}
}
