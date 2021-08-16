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
 * A class to represent a quantity of three dimensional space
 */
public class Volume {
    @Attribute
    private double value;
    @Attribute
    private VolumeUnit units;

    public Volume() {

    }

    public Volume(double value, VolumeUnit units) {
        this.value = value;
        this.units = units;
    }

    public Volume add(Volume volume) {
        volume = volume.convertToUnits(units);
        return new Volume(value + volume.getValue(), units);
    }

    public Volume subtract(Volume volume) {
        volume = volume.convertToUnits(units);
        return new Volume(value - volume.getValue(), units);
    }

    public Volume add(double d) {
        return new Volume(value + d, units);
    }

    public Volume subtract(double d) {
        return new Volume(value - d, units);
    }

    public Volume multiply(double d) {
        return new Volume(value * d, units);
    }

    public Volume divide(double d) {
        return new Volume(value / d, units);
    }

    public double divide(Volume volume) {
        volume = volume.convertToUnits(units);
        return value / volume.getValue();
    }

    public Area divide(Length length) {
        LengthUnit lengthUnit = units.getLengthUnit();
        length = length.convertToUnits(lengthUnit);
        return new Area(value / length.getValue(), AreaUnit.fromLengthUnit(lengthUnit));
    }

    public Length divide(Area area) {
        LengthUnit lengthUnit = units.getLengthUnit();
        area = area.convertToUnits(AreaUnit.fromLengthUnit(lengthUnit));
        return new Length(value / area.getValue(), lengthUnit);
    }

    public Volume modulo(Volume volume) {
        volume = volume.convertToUnits(units);
        return new Volume(value % volume.getValue(), units);
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public VolumeUnit getUnits() {
        return units;
    }

    public void setUnits(VolumeUnit units) {
        this.units = units;
    }

    public Volume convertToUnits(VolumeUnit units) {
        if (this.units == units) {
            return this;
        }
        
        LengthUnit oldLengthUnit = this.units.getLengthUnit();
        LengthUnit newLengthUnit = units.getLengthUnit();
        
        double scaleFactor = Math.pow((new Length(1.0, oldLengthUnit)).
                divide(new Length(1.0, newLengthUnit)), 3);
        
        return new Volume(value*scaleFactor, units);
    }

    public static double convertToUnits(double value, VolumeUnit fromUnits, VolumeUnit toUnits) {
        return new Volume(value, fromUnits).convertToUnits(toUnits).getValue();
    }

    public static Volume parse(String s) {
        return parse(s, false);
    }

    /**
     * Takes a value in the format of a double followed by any number of spaces followed by the
     * shortName of an VolumeUnit value and returns the value as an Volume object. Returns null if the
     * value could not be parsed.
     */
    public static Volume parse(String s, boolean requireUnits) {
        if (s == null) {
            return null;
        }

        s = s.trim();

        Volume volume = new Volume(0, null);
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
            unitsString = unitsString.replace('3', '³'); //convert 3 to superscript 3
            unitsString = unitsString.replace('u', 'μ'); //convert u to μ
            for (VolumeUnit volumeUnit : VolumeUnit.values()) {
                if (volumeUnit.getShortName().equalsIgnoreCase(unitsString)) {
                    volume.setUnits(volumeUnit);
                    break;
                }
            }
        }
        else {
            valueString = s;
        }

        if (requireUnits && volume.getUnits() == null) {
            return null;
        }

        try {
            double value = Double.parseDouble(valueString);
            volume.setValue(value);
        }
        catch (Exception e) {
            return null;
        }

        return volume;
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
        Volume other = (Volume) obj;
        if (units != other.units) {
            return false;
        }
        if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value)) {
            return false;
        }
        return true;
    }

    public int compareTo(Volume other) {
        if (other == null) {
            return Double.valueOf(getValue()).compareTo(Double.valueOf(0));
        }
        return Double.valueOf(getValue()).compareTo(Double.valueOf(other.convertToUnits(units).getValue()));
    }
}
