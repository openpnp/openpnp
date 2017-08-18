/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.model;

import java.util.Locale;

import org.simpleframework.xml.Attribute;

/**
 * A Location is a an immutable 3D point in X, Y, Z space with a rotation component. The rotation is
 * applied about the Z axis.
 */
public class Location {
    /*
     * The fields on this class would be final in a perfect world, but that doesn't work correctly
     * with the XML serialization.
     */

    @Attribute
    private LengthUnit units;
    @Attribute(required = false)
    private double x;
    @Attribute(required = false)
    private double y;
    @Attribute(required = false)
    private double z;
    @Attribute(required = false)
    private double rotation;

    /**
     * Only used by XML serialization.
     */
    @SuppressWarnings("unused")
    private Location() {
        this(null);
    }

    public Location(LengthUnit units) {
        this(units, 0, 0, 0, 0);
    }

    public Location(LengthUnit units, double x, double y, double z, double rotation) {
        this.units = units;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotation = rotation;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getRotation() {
        return rotation;
    }

    public LengthUnit getUnits() {
        return units;
    }

    public Location convertToUnits(LengthUnit units) {
        Location location =
                new Location(units, new Length(x, this.units).convertToUnits(units).getValue(),
                        new Length(y, this.units).convertToUnits(units).getValue(),
                        new Length(z, this.units).convertToUnits(units).getValue(), rotation);
        return location;
    }

    public Length getLinearLengthTo(Location location) {
        double distance = getLinearDistanceTo(location);
        return new Length(distance, getUnits());
    }

    /**
     * Returns the distance between this Location and the specified Location in the units of this
     * Location.
     * 
     * @param location
     * @return
     */
    public double getLinearDistanceTo(Location location) {
        location = location.convertToUnits(getUnits());
        return getLinearDistanceTo(location.getX(), location.getY());
    }

    public double getLinearDistanceTo(double x, double y) {
        return (Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.y - y, 2)));
    }

    public double getXyzDistanceTo(Location location) {
        location = location.convertToUnits(getUnits());
        return (Math.sqrt(Math.pow(this.x - location.getX(), 2)
                + Math.pow(this.y - location.getY(), 2) + Math.pow(this.z - location.getZ(), 2)));
    }

    public Length getLengthX() {
        return new Length(x, units);
    }

    public Length getLengthY() {
        return new Length(y, units);
    }

    public Length getLengthZ() {
        return new Length(z, units);
    }

    /**
     * Returns a new Location with the given Location's X, Y, and Z components subtracted from this
     * Location's X, Y, and Z components. Rotation is left unchanged.
     * 
     * @param l
     * @return
     */
    public Location subtract(Location l) {
        l = l.convertToUnits(getUnits());
        return new Location(l.getUnits(), x - l.getX(), y - l.getY(), z - l.getZ(), getRotation());
    }

    /**
     * Same as {@link Location#subtract(Location)} but also subtracts rotation.
     * 
     * @param l
     * @return
     */
    public Location subtractWithRotation(Location l) {
        l = l.convertToUnits(getUnits());
        return new Location(l.getUnits(), x - l.getX(), y - l.getY(), z - l.getZ(),
                rotation - l.getRotation());
    }

    /**
     * Returns a new Location with the given Location's X, Y, and Z components added to this
     * Location's X, Y, and Z components. Rotation is left unchanged.
     * 
     * @param l
     * @return
     */
    public Location add(Location l) {
        l = l.convertToUnits(getUnits());
        return new Location(l.getUnits(), x + l.getX(), y + l.getY(), z + l.getZ(), rotation);
    }

    /**
     * Returns a new Location with the given Location's X, Y, and Z components added to this
     * Location's X, Y, and Z components. Rotation is included.
     * 
     * @param l
     * @return
     */
    public Location addWithRotation(Location l) {
        l = l.convertToUnits(getUnits());
        return new Location(l.getUnits(), x + l.getX(), y + l.getY(), z + l.getZ(),
                rotation + l.getRotation());
    }

    /**
     * Returns a new Location with the given Location's X, Y and Z components multiplied by this
     * Location's X, Y and Z components. Rotation is left unchanged.
     * 
     * @param l
     * @return
     */
    public Location multiply(Location l) {
        l = l.convertToUnits(getUnits());
        return new Location(l.getUnits(), x * l.getX(), y * l.getY(), z * l.getZ(), getRotation());
    }

    /**
     * Returns a new Location based on this Location with values multiplied by the specified values.
     * Units are the same as this Location.
     * 
     * @param x
     * @param y
     * @param z
     * @param rotation
     * @return
     */
    public Location multiply(double x, double y, double z, double rotation) {
        return new Location(getUnits(), x * getX(), y * getY(), z * getZ(),
                rotation * getRotation());
    }

    /**
     * Returns a new Location with the same units as this one and with any of fields specified as
     * true inverted from the values of this one. Specifically, if one of the x, y, z or rotation
     * fields are specified true in the method call, that field will be multipled by -1 in the
     * returned Location.
     * 
     * @param x
     * @param y
     * @param z
     * @param rotation
     * @return
     */
    public Location invert(boolean x, boolean y, boolean z, boolean rotation) {
        return new Location(getUnits(), getX() * (x ? -1 : 1), getY() * (y ? -1 : 1),
                getZ() * (z ? -1 : 1), getRotation() * (rotation ? -1 : 1));
    }

    /**
     * Returns a new Location with the same units as this one but with values updated to the passed
     * in values. A caveat is that if a specified value is null, the new Location will contain the
     * value from this object instead of the new value.
     * 
     * This is intended as a utility method, useful for creating new Locations based on existing
     * ones with one or more values changed.
     * 
     * @param x
     * @param y
     * @param z
     * @param rotation
     * @return
     */
    public Location derive(Double x, Double y, Double z, Double rotation) {
        return new Location(units, x == null ? this.x : x, y == null ? this.y : y,
                z == null ? this.z : z, rotation == null ? this.rotation : rotation);
    }

    /**
     * Returns a new Location with this Location's X and Y rotated by angle. Z and Rotation are
     * unchanged.
     * 
     * @param angle
     * @return
     */
    public Location rotateXy(double angle) {
        if (angle == 0.0) {
            return this;
        }
        while (angle < 180.) {
            angle += 360;
        }
        while (angle > 180.) {
            angle -= 360;
        }
        angle = Math.toRadians(angle);

        return new Location(getUnits(), getX() * Math.cos(angle) - getY() * Math.sin(angle),
                getX() * Math.sin(angle) + getY() * Math.cos(angle), getZ(), getRotation());
    }
    
    public Location rotateXyCenterPoint(Location center, double angle) {
        Location location = this.subtract(center);
        location = location.rotateXy(angle);
        location = location.add(center);
        return location;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "(%f, %f, %f, %f %s)", x, y, z, rotation,
                units.getShortName());
    }

    public Point getXyPoint() {
        return new Point(getX(), getY());
    }
    
    /**
     * Checks if targetLocation is contained in current location given the current location represents a rectangular item with the origin in originLocation.
     */
    public boolean containsLocation(Location originLocation, Location targetLocation) {
    	Location target = targetLocation.convertToUnits(this.units);
    	double x = target.getX();
    	double y = target.getY();
    	Location origin = originLocation.convertToUnits(this.units);
    	double x1 = origin.getX();
    	double y1 = origin.getY();
    	double x2 = x1 + getX();
    	double y2 = y1 + getY();
    	
    	return (x >= x1) && (x <= x2) && (y > y1) && (y < y2);
    }

    /**
     * Performs a unit agnostic equality check. If the Object being tested is a Location in a
     * different unit, it is first converted to the units of this Location and then each value field
     * is compared.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Location)) {
            return false;
        }
        Location that = (Location) obj;
        that = that.convertToUnits(this.units);
        return this.units == that.units && this.x == that.x && this.y == that.y && this.z == that.z
                && this.rotation == that.rotation;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = this.units != null ? this.units.hashCode() : 0;
        temp = Double.doubleToLongBits(this.x);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.y);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.z);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.rotation);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        return result;
    }
}
