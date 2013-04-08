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
	
	public Length getLengthX() {
		return new Length(x, units);
	}
	
	public void setLengthX(Length length) {
		setX(length.getValue());
		if (length.getUnits() != units) {
			setY(getLengthY().convertToUnits(length.getUnits()).getValue());
			setZ(getLengthZ().convertToUnits(length.getUnits()).getValue());
			setUnits(length.getUnits());
			firePropertyChange("lengthY", null, getLengthY());
			firePropertyChange("lengthZ", null, getLengthZ());
		}
		firePropertyChange("lengthX", null, getLengthX());
	}
	
	public Length getLengthY() {
		return new Length(y, units);
	}
	
	public void setLengthY(Length length) {
		setY(length.getValue());
		if (length.getUnits() != units) {
			setX(getLengthX().convertToUnits(length.getUnits()).getValue());
			setZ(getLengthZ().convertToUnits(length.getUnits()).getValue());
			setUnits(length.getUnits());
			firePropertyChange("lengthX", null, getLengthX());
			firePropertyChange("lengthZ", null, getLengthZ());
		}
		firePropertyChange("lengthY", null, getLengthY());
	}
	
	public Length getLengthZ() {
		return new Length(z, units);
	}
	
	public void setLengthZ(Length length) {
		setZ(length.getValue());
		if (length.getUnits() != units) {
			setX(getLengthX().convertToUnits(length.getUnits()).getValue());
			setY(getLengthY().convertToUnits(length.getUnits()).getValue());
			setUnits(length.getUnits());
			firePropertyChange("lengthX", null, getLengthX());
			firePropertyChange("lengthY", null, getLengthY());
		}
		firePropertyChange("lengthZ", null, getLengthZ());
	}
	
	/**
	 * Returns a new Location with the given Location's X, Y, and Z components
	 * subtracted from this Location's X, Y, and Z components. Rotation is left
	 * unchanged.
	 * @param l
	 * @return
	 */
	public Location subtract(Location l) {
		l = l.convertToUnits(getUnits());
		return new Location(l.getUnits(), x - l.getX(), y - l.getY(), z - l.getZ(), getRotation());
	}
	
	/**
	 * Returns a new Location with the given Location's X, Y, and Z components
	 * added to this Location's X, Y, and Z components. Rotation is left
	 * unchanged.
	 * @param l
	 * @return
	 */
	public Location add(Location l) {
		l = l.convertToUnits(getUnits());
		return new Location(l.getUnits(), x + l.getX(), y + l.getY(), z + l.getZ(), getRotation());
	}

	/**
	 * Returns a new Location with the given Location's X, Y and Z components
	 * multiplied by this Location's X, Y and Z components. Rotation is left
	 * unchanged.
	 * @param l
	 * @return
	 */
	public Location multiply(Location l) {
	    l = l.convertToUnits(getUnits());
        return new Location(l.getUnits(), x * l.getX(), y * l.getY(), z * l.getZ(), getRotation());
	}
	
	/**
	 * Returns a new Location based on this Location with values multiplied by
	 * the specified values. Units are the same as this Location.
	 * @param x
	 * @param y
	 * @param z
	 * @param rotation
	 * @return
	 */
	public Location multiply(double x, double y, double z, double rotation) {
        return new Location(getUnits(), x * getX(), y * getY(), z * getZ(), rotation * getRotation());
	}
	
	/**
	 * Returns a new Location with the same units as this one and with any of
	 * fields specified as true inverted from the values of this one.
	 * Specifically, if one of the x, y, z or rotation fields are specified
	 * true in the method call, that field will be multipled by -1 in the
	 * returned Location.
	 * @param x
	 * @param y
	 * @param z
	 * @param rotation
	 * @return
	 */
	public Location invert(boolean x, boolean y, boolean z, boolean rotation) {
		Location l = new Location(getUnits());

		l.setX(getX() * (x ? -1 : 1));
		l.setY(getY() * (y ? -1 : 1));
		l.setZ(getZ() * (z ? -1 : 1));
		l.setRotation(getRotation() * (rotation ? -1 : 1));
		
		return l;
	}
	
	public Location clone() {
        return new Location(units, x, y, z, rotation);
	}
	
	/**
	 * Returns a new Location with the same units as this one but with values
	 * updated to the passed in values. A caveat is that if a specified value
	 * is null, the new Location will contain the value from this object
	 * instead of the new value.
	 * 
	 * This is intended as a utility method, useful for creating new Locations
	 * based on existing ones with one or more values changed.
	 * @param x
	 * @param y
	 * @param z
	 * @param rotation
	 * @return
	 */
	public Location derive(Double x, Double y, Double z, Double rotation) {
	    return new Location(
	            units,
                x == null ? this.x : x,
                y == null ? this.y : y,
                z == null ? this.z : z,
                rotation == null ? this.rotation : rotation
	            );
	}
	
	@Override
	public String toString() {
		return String.format("units %s, x %f, y %f, z %f, rotation %f", units, x, y, z, rotation);
	}
}
