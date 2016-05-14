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



public class Length {
    public enum Field {
        X, Y, Z
    }

    @Attribute
    private double value;
    @Attribute
    private LengthUnit units;

    public Length() {

    }

    public Length(double value, LengthUnit units) {
        this.value = value;
        this.units = units;
    }

    public Length add(Length length) {
        length = length.convertToUnits(units);
        return new Length(value + length.getValue(), units);
    }

    public Length subtract(Length length) {
        length = length.convertToUnits(units);
        return new Length(value - length.getValue(), units);
    }

    public Length multiply(Length length) {
        length = length.convertToUnits(units);
        return new Length(value * length.getValue(), units);
    }

    public Length add(double d) {
        return new Length(value + d, units);
    }

    public Length subtract(double d) {
        return new Length(value - d, units);
    }

    public Length multiply(double d) {
        return new Length(value * d, units);
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public LengthUnit getUnits() {
        return units;
    }

    public void setUnits(LengthUnit units) {
        this.units = units;
    }

    public Length convertToUnits(LengthUnit units) {
        if (this.units == units) {
            return this;
        }
        double mm = 0;
        if (this.units == LengthUnit.Millimeters) {
            mm = value;
        }
        else if (this.units == LengthUnit.Centimeters) {
            mm = value * 10;
        }
        else if (this.units == LengthUnit.Meters) {
            mm = value * 1000;
        }
        else if (this.units == LengthUnit.Inches) {
            mm = value * 25.4;
        }
        else if (this.units == LengthUnit.Feet) {
            mm = value * 25.4 * 12;
        }
        else {
            throw new Error("convertLength() unrecognized units " + this.units);
        }

        if (units == LengthUnit.Millimeters) {
            return new Length(mm, units);
        }
        else if (units == LengthUnit.Centimeters) {
            return new Length(mm / 10, units);
        }
        else if (units == LengthUnit.Meters) {
            return new Length(mm / 1000, units);
        }
        else if (units == LengthUnit.Inches) {
            return new Length(mm * (1 / 25.4), units);
        }
        else if (units == LengthUnit.Feet) {
            return new Length(mm * (1 / 25.4) * 12, units);
        }
        else {
            throw new Error("convertLength() unrecognized units " + units);
        }

    }

    public static double convertToUnits(double value, LengthUnit fromUnits, LengthUnit toUnits) {
        return new Length(value, fromUnits).convertToUnits(toUnits).getValue();
    }

    public static Length parse(String s) {
        return parse(s, false);
    }

    /**
     * Takes a value in the format of a double followed by any number of spaces followed by the
     * shortName of a LengthUnit value and returns the value as a Length object. Returns null if the
     * value could not be parsed.
     */
    public static Length parse(String s, boolean requireUnits) {
        if (s == null) {
            return null;
        }

        s = s.trim();

        Length length = new Length(0, null);
        // find the index of the first character that is not a -, . or digit.
        int startOfUnits = -1;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch != '-' && ch != '.' && !Character.isDigit(ch)) {
                startOfUnits = i;
                break;
            }
        }

        String valueString = null;
        if (startOfUnits != -1) {
            valueString = s.substring(0, startOfUnits);
            String unitsString = s.substring(startOfUnits);
            unitsString = unitsString.trim();
            for (LengthUnit lengthUnit : LengthUnit.values()) {
                if (lengthUnit.getShortName().equalsIgnoreCase(unitsString)) {
                    length.setUnits(lengthUnit);
                    break;
                }
            }
        }
        else {
            valueString = s;
        }

        if (requireUnits && length.getUnits() == null) {
            return null;
        }

        try {
            double value = Double.parseDouble(valueString);
            length.setValue(value);
        }
        catch (Exception e) {
            return null;
        }

        return length;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%2.3f%s", value, units.getShortName());
    }

    /**
     * Performs the same function as toString() but allows the caller to specify the format String
     * that is used. The format String should contain %f and %s in that order for value and
     * units.getShortName().
     * 
     * @param fmt
     * @return
     */
    public String toString(String fmt) {
        if (fmt == null) {
            return toString();
        }
        return String.format(Locale.US, fmt, value, units.getShortName());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((units == null) ? 0 : units.hashCode());
        long temp;
        temp = Double.doubleToLongBits(value);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Length other = (Length) obj;
        if (units != other.units)
            return false;
        if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value))
            return false;
        return true;
    }

    public static Location setLocationField(Configuration configuration, Location location,
            Length length, Field field, boolean defaultToOldUnits) {
        Length oldLength = null;
        if (field == Field.X) {
            oldLength = location.getLengthX();
        }
        else if (field == Field.Y) {
            oldLength = location.getLengthY();
        }
        else if (field == Field.Z) {
            oldLength = location.getLengthZ();
        }
        if (length.getUnits() == null) {
            if (defaultToOldUnits) {
                length.setUnits(oldLength.getUnits());
            }
            if (length.getUnits() == null) {
                length.setUnits(configuration.getSystemUnits());
            }
        }
        if (location.getUnits() == null) {
            throw new Error("This can't happen!");
        }
        else {
            location = location.convertToUnits(length.getUnits());
        }
        if (field == Field.X) {
            location = location.derive(length.getValue(), null, null, null);
        }
        else if (field == Field.Y) {
            location = location.derive(null, length.getValue(), null, null);
        }
        else if (field == Field.Z) {
            location = location.derive(null, null, length.getValue(), null);
        }
        return location;
    }

    /**
     * Sets the specified field on the passed Location object. Enforces application specific unit
     * conversion. If the new Length value does not have units set, this method will set the units
     * of the Length to the system default units. If the Location itself does not have units set,
     * the Location's units are set to the Length's units. Finally, if the Location's units have
     * changed, the entire Location is converted to the new units and the new object is returned.
     * 
     * @param configuration
     * @param location
     * @param length
     * @param field
     * @return
     */
    public static Location setLocationField(Configuration configuration, Location location,
            Length length, Field field) {
        return setLocationField(configuration, location, length, field, false);
    }

}
