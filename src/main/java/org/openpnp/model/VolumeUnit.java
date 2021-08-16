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

/**
 * Defined units of three dimensional space
 */
public enum VolumeUnit {
    CubicMeters("m³", "CubicMeter"),
    CubicCentimeters("cm³", "CubicCentimeter"),
    MilliLiters("ml", "MilliLiter"),       //preferred equivalent to CubicCentimeters
    CubicMillimeters("mm³", "CubicMillimeter"),
    MicroLiters("μl", "MicroLiter"),       //preferred equivalent to CubicMillimeters
    CubicFeet("ft³", "CubicFoot"),
    CubicInches("in³", "CubicInch"),
    CubicMils("mil³", "CubicMil"),
    CubicMicrons("μm³", "CubicMicron"),
    FemtoLiters("fl", "FemtoLiter");       //preferred equivalent to CubicMicrons

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
    
    /**
     * Creates a length unit, that when cubed, is equivalent to this volume unit 
     * @return the length unit
     */
    public LengthUnit getLengthUnit() {
        switch (this) {
            case CubicMeters :
                return LengthUnit.Meters;
            case CubicCentimeters :
                return LengthUnit.Centimeters;
            case MilliLiters :
                return LengthUnit.Centimeters;
            case CubicMillimeters :
                return LengthUnit.Millimeters;
            case MicroLiters :
                return LengthUnit.Millimeters;
            case CubicFeet :
                return LengthUnit.Feet;
            case CubicInches :
                return LengthUnit.Inches;
            case CubicMils :
                return LengthUnit.Mils;
            case CubicMicrons :
                return LengthUnit.Microns;
            case FemtoLiters :
                return LengthUnit.Microns;
        }
        return null;
    }
    
    /**
     * Creates a preferred volume unit equivalent to the cube of the given length unit 
     * @param lengthUnit - the given length unit
     * @return the volume unit
     */
    public static VolumeUnit fromLengthUnit(LengthUnit lengthUnit) {
        return fromLengthUnit(lengthUnit, true);
    }
    
    /**
     * Creates a volume unit equivalent to the cube of the given length unit 
     * @param lengthUnit - the given length unit
     * @param preferredUnit - flag to indicate whether the preferred or non-preferred unit
     * should be returned (preferred unit is returned when set to true)
     * @return the volume unit
     */
    public static VolumeUnit fromLengthUnit(LengthUnit lengthUnit, boolean preferredUnit) {
        switch (lengthUnit) {
            case Meters :
                return VolumeUnit.CubicMeters;
            case Centimeters :
                if (preferredUnit) {
                    return VolumeUnit.MilliLiters;
                }
                else {
                    return VolumeUnit.CubicCentimeters;
                }
            case Millimeters :
                if (preferredUnit) {
                    return VolumeUnit.MicroLiters;
                }
                else {
                    return VolumeUnit.CubicMillimeters;
                }
            case Feet :
                return VolumeUnit.CubicFeet;
            case Inches :
                return VolumeUnit.CubicInches;
            case Mils :
                return VolumeUnit.CubicMils;
            case Microns :
                if (preferredUnit) {
                    return VolumeUnit.FemtoLiters;
                }
                else {
                    return VolumeUnit.CubicMicrons;
                }
        }
        return null;
    }
}
