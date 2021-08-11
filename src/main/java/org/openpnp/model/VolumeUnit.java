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

public enum VolumeUnit {
    CubicMeters("m\u00B3", "CubicMeter"),
    CubicCentimeters("cm\u00B3", "CubicCentimeter"),
    CubicMillimeters("mm\u00B3", "CubicMillimeter"),
    CubicFeet("ft\u00B3", "CubicFoot"),
    CubicInches("in\u00B3", "CubicInch"),
    CubicMils("mil\u00B3", "CubicMil"),
    CubicMicrons("μm\u00B3", "CubicMicron");

    private final String shortName;

    private final String singularName;

    private VolumeUnit(String shortName, String singularName) {
        this.shortName = shortName;
        this.singularName = singularName;
    }

    public String getShortName() {
        return shortName;
    }

    public String getSingularName() {
        return singularName;
    }
    
    public LengthUnit getLinearUnit() {
        switch (this) {
            case CubicMeters :
                return LengthUnit.Meters;
            case CubicCentimeters :
                return LengthUnit.Centimeters;
            case CubicMillimeters :
                return LengthUnit.Millimeters;
            case CubicFeet :
                return LengthUnit.Feet;
            case CubicInches :
                return LengthUnit.Inches;
            case CubicMils :
                return LengthUnit.Mils;
           case CubicMicrons :
                return LengthUnit.Microns;
        }
        return null;
    }
    
    public static VolumeUnit fromLinearUnit(LengthUnit lengthUnit) {
        switch (lengthUnit) {
            case Meters :
                return VolumeUnit.CubicMeters;
            case Centimeters :
                return VolumeUnit.CubicCentimeters;
            case Millimeters :
                return VolumeUnit.CubicMillimeters;
            case Feet :
                return VolumeUnit.CubicFeet;
            case Inches :
                return VolumeUnit.CubicInches;
            case Mils :
                return VolumeUnit.CubicMils;
           case Microns :
                return VolumeUnit.CubicMicrons;
        }
        return null;
    }
}
