/*
 * Copyright (C) 2021 Tony Luken <tonyluken@att.net>
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
 * A class to represent a quantity of 2D space
 */
public class Area {
    @Attribute
    private double value;
    @Attribute
    private AreaUnit units;

    public Area() {

    }

    public Area(double value, AreaUnit units) {
        this.value = value;
        this.units = units;
    }

    public Area add(Area area) {
        area = area.convertToUnits(units);
        return new Area(value + area.getValue(), units);
    }

    public Area subtract(Area area) {
        area = area.convertToUnits(units);
        return new Area(value - area.getValue(), units);
    }

    public Area add(double d) {
        return new Area(value + d, units);
    }

    public Area subtract(double d) {
        return new Area(value - d, units);
    }

    public Area multiply(double d) {
        return new Area(value * d, units);
    }

    public Volume multiply(Length length) {
        LengthUnit lengthUnit = units.getLengthUnit();
        length = length.convertToUnits(lengthUnit);
        return new Volume(value * length.getValue(), VolumeUnit.fromLengthUnit(lengthUnit));
    }

    public Area divide(double d) {
        return new Area(value / d, units);
    }

    public double divide(Area area) {
        area = area.convertToUnits(units);
        return value / area.getValue();
    }

    public Length divide(Length length) {
        LengthUnit lengthUnit = units.getLengthUnit();
        length = length.convertToUnits(lengthUnit);
        return new Length(value / length.getValue(), lengthUnit);
    }

    public Area modulo(Area area) {
        area = area.convertToUnits(units);
        return new Area(value % area.getValue(), units);
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public AreaUnit getUnits() {
        return units;
    }

    public void setUnits(AreaUnit units) {
        this.units = units;
    }

    public Area convertToUnits(AreaUnit units) {
        if (this.units == units) {
            return this;
        }
        
        LengthUnit oldLengthUnit = this.units.getLengthUnit();
        LengthUnit newLengthUnit = units.getLengthUnit();
        
        double scaleFactor = Math.pow((new Length(1.0, oldLengthUnit)).
                divide(new Length(1.0, newLengthUnit)), 2);
        
        return new Area(value*scaleFactor, units);
    }

    public static double convertToUnits(double value, AreaUnit fromUnits, AreaUnit toUnits) {
        return new Area(value, fromUnits).convertToUnits(toUnits).getValue();
    }

    public static Area parse(String s) {
        return parse(s, false);
    }

    /**
     * Takes a value in the format of a double followed by any number of spaces followed by the
     * shortName of an AreaUnit value and returns the value as an Area object. Returns null if the
     * value could not be parsed.
     */
    public static Area parse(String s, boolean requireUnits) {
        if (s == null) {
            return null;
        }

        s = s.trim();

        Area area = new Area(0, null);
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
            unitsString = unitsString.replace('2', '²'); //convert 2 to superscript 2
            unitsString = unitsString.replace('u', 'μ'); //convert u to μ
            for (AreaUnit areaUnit : AreaUnit.values()) {
                if (areaUnit.getShortName().equalsIgnoreCase(unitsString)) {
                    area.setUnits(areaUnit);
                    break;
                }
            }
        }
        else {
            valueString = s;
        }

        if (requireUnits && area.getUnits() == null) {
            return null;
        }

        try {
            double value = Double.parseDouble(valueString);
            area.setValue(value);
        }
        catch (Exception e) {
            return null;
        }

        return area;
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
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Area other = (Area) obj;
        if (units != other.units) {
            return false;
        }
        if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value)) {
            return false;
        }
        return true;
    }

    public int compareTo(Area other) {
        if (other == null) {
            return Double.valueOf(getValue()).compareTo(Double.valueOf(0));
        }
        return Double.valueOf(getValue()).compareTo(Double.valueOf(other.convertToUnits(units).getValue()));
    }
}
