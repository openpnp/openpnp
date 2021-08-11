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

public enum AreaUnit {
    SquareMeters("m\u00B2", "SquareMeter"),
    SquareCentimeters("cm\u00B2", "SquareCentimeter"),
    SquareMillimeters("mm\u00B2", "SquareMillimeter"),
    SquareFeet("ft\u00B2", "SquareFoot"),
    SquareInches("in\u00B2", "SquareInch"),
    SquareMils("mil\u00B2", "SquareMil"),
    SquareMicrons("μm\u00B2", "SquareMicron");

    private final String shortName;

    private final String singularName;

    private AreaUnit(String shortName, String singularName) {
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
            case SquareMeters :
                return LengthUnit.Meters;
            case SquareCentimeters :
                return LengthUnit.Centimeters;
            case SquareMillimeters :
                return LengthUnit.Millimeters;
            case SquareFeet :
                return LengthUnit.Feet;
            case SquareInches :
                return LengthUnit.Inches;
            case SquareMils :
                return LengthUnit.Mils;
           case SquareMicrons :
                return LengthUnit.Microns;
        }
        return null;
    }
    
    public static AreaUnit fromLinearUnit(LengthUnit lengthUnit) {
        switch (lengthUnit) {
            case Meters :
                return AreaUnit.SquareMeters;
            case Centimeters :
                return AreaUnit.SquareCentimeters;
            case Millimeters :
                return AreaUnit.SquareMillimeters;
            case Feet :
                return AreaUnit.SquareFeet;
            case Inches :
                return AreaUnit.SquareInches;
            case Mils :
                return AreaUnit.SquareMils;
           case Microns :
                return AreaUnit.SquareMicrons;
        }
        return null;
    }
}
