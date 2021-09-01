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

import org.junit.jupiter.api.Test;
import org.openpnp.model.Area;
import org.openpnp.model.AreaUnit;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Volume;
import org.openpnp.model.VolumeUnit;
import static org.junit.jupiter.api.Assertions.*;

public class ModelUnitsTest {
	
    @Test
    public void testLengths() {
        String xStr = "15.0";
        double x = Double.parseDouble(xStr);
        Length length1 = new Length(x, LengthUnit.Millimeters);
        assertEqualsTo48Bits(x, length1.getValue());
        assertEqualsTo48Bits(x/10, length1.convertToUnits(LengthUnit.Centimeters).getValue());
        assertEqualsTo48Bits(x/304.8, length1.convertToUnits(LengthUnit.Feet).getValue());
        assertEqualsTo48Bits(x/25.4, length1.convertToUnits(LengthUnit.Inches).getValue());
        assertEqualsTo48Bits(x/1000, length1.convertToUnits(LengthUnit.Meters).getValue());
        assertEqualsTo48Bits(x*1000, length1.convertToUnits(LengthUnit.Microns).getValue());
        assertEqualsTo48Bits(x, length1.convertToUnits(LengthUnit.Millimeters).getValue());
        assertEqualsTo48Bits(x*1000/25.4, length1.convertToUnits(LengthUnit.Mils).getValue());
        
        assertEqualsTo48Bits(length1, length1.convertToUnits(LengthUnit.Centimeters).
                convertToUnits(LengthUnit.Feet).convertToUnits(LengthUnit.Meters).
                convertToUnits(LengthUnit.Inches).convertToUnits(LengthUnit.Microns).
                convertToUnits(LengthUnit.Mils).convertToUnits(LengthUnit.Millimeters));
        
        String yStr = "0.3";
        double y = Double.parseDouble(yStr);
        Length length2 = new Length(y, LengthUnit.Centimeters);
        assertEqualsTo48Bits(new Length(x+10*y, LengthUnit.Millimeters), length1.add(length2));
        assertEqualsTo48Bits(new Length(x+y, LengthUnit.Millimeters), length1.add(y));
        assertEqualsTo48Bits(new Length(x-10*y, LengthUnit.Millimeters), length1.subtract(length2));
        assertEqualsTo48Bits(new Length(x-y, LengthUnit.Millimeters), length1.subtract(y));
        assertEqualsTo48Bits(x/(10*y), length1.divide(length2));
        assertEqualsTo48Bits(new Length(x/y, LengthUnit.Millimeters), length1.divide(y));
        assertEqualsTo48Bits(new Length(x%(10*y), LengthUnit.Millimeters), length1.modulo(length2));
        assertEqualsTo48Bits(new Area(x*10*y, AreaUnit.SquareMillimeters), length1.multiply(length2));
        assertEqualsTo48Bits(new Length(x*y, LengthUnit.Millimeters), length1.multiply(y));
        
        assertEqualsTo48Bits(length1, Length.parse(xStr + "mm"));
        assertEqualsTo48Bits(length2, Length.parse(yStr + "cm"));
        assertEqualsTo48Bits(new Length(x, LengthUnit.Microns), Length.parse(xStr + "μm"));
        assertEqualsTo48Bits(Length.parse(xStr + "μm"), Length.parse(xStr + "um"));
    }

    @Test
    public void testAreas() {
        String xStr = "15.0";
        double x = Double.parseDouble(xStr);
        Area area1 = new Area(x, AreaUnit.SquareMillimeters);
        assertEqualsTo48Bits(x, area1.getValue());
        assertEqualsTo48Bits(x/Math.pow(10, 2), 
                area1.convertToUnits(AreaUnit.SquareCentimeters).getValue());
        assertEqualsTo48Bits(x/Math.pow(304.8, 2), 
                area1.convertToUnits(AreaUnit.SquareFeet).getValue());
        assertEqualsTo48Bits(x/Math.pow(25.4, 2), 
                area1.convertToUnits(AreaUnit.SquareInches).getValue());
        assertEqualsTo48Bits(x/Math.pow(1000, 2), 
                area1.convertToUnits(AreaUnit.SquareMeters).getValue());
        assertEqualsTo48Bits(x*Math.pow(1000, 2), 
                area1.convertToUnits(AreaUnit.SquareMicrons).getValue());
        assertEqualsTo48Bits(x, 
                area1.convertToUnits(AreaUnit.SquareMillimeters).getValue());
        assertEqualsTo48Bits(x*Math.pow(1000/25.4, 2), 
                area1.convertToUnits(AreaUnit.SquareMils).getValue());
        
        assertEqualsTo48Bits(area1, area1.convertToUnits(AreaUnit.SquareCentimeters).
                convertToUnits(AreaUnit.SquareFeet).convertToUnits(AreaUnit.SquareMeters).
                convertToUnits(AreaUnit.SquareInches).convertToUnits(AreaUnit.SquareMicrons).
                convertToUnits(AreaUnit.SquareMils).convertToUnits(AreaUnit.SquareMillimeters));
        
        String yStr = "0.3";
        double y = Double.parseDouble(yStr);
        Area area2 = new Area(y, AreaUnit.SquareCentimeters);
        assertEqualsTo48Bits(new Area(x+100*y, AreaUnit.SquareMillimeters), area1.add(area2));
        assertEqualsTo48Bits(new Area(x+y, AreaUnit.SquareMillimeters), area1.add(y));
        assertEqualsTo48Bits(new Area(x-100*y, AreaUnit.SquareMillimeters), area1.subtract(area2));
        assertEqualsTo48Bits(new Area(x-y, AreaUnit.SquareMillimeters), area1.subtract(y));
        assertEqualsTo48Bits(new Area(x*y, AreaUnit.SquareMillimeters), area1.multiply(y));
        assertEqualsTo48Bits(new Volume(x*10*y, VolumeUnit.MicroLiters), 
                area1.multiply(new Length(y, LengthUnit.Centimeters)));
        assertEqualsTo48Bits(new Area(x/y, AreaUnit.SquareMillimeters), area1.divide(y));
        assertEqualsTo48Bits(new Length(x/(10*y), LengthUnit.Millimeters), 
                area1.divide(new Length(y, LengthUnit.Centimeters)));
        assertEqualsTo48Bits(x/(100*y), area1.divide(area2));
        assertEqualsTo48Bits(new Area(x%(100*y), AreaUnit.SquareMillimeters), area1.modulo(area2));
        
        assertEqualsTo48Bits(area1, Area.parse(xStr + "mm²"));
        assertEqualsTo48Bits(new Area(x, AreaUnit.SquareMicrons), Area.parse(xStr + "μm2"));
        assertEqualsTo48Bits(Area.parse(xStr + "μm2"), Area.parse(xStr + "um2"));
    }
    
    @Test
    public void testVolumes() {
        String xStr = "15.0";
        double x = Double.parseDouble(xStr);
        Volume volume1 = new Volume(x, VolumeUnit.MicroLiters);
        assertEqualsTo48Bits(x, volume1.getValue());
        assertEqualsTo48Bits(x/Math.pow(10, 3), 
                volume1.convertToUnits(VolumeUnit.CubicCentimeters).getValue());
        assertEqualsTo48Bits(x/Math.pow(304.8, 3), 
                volume1.convertToUnits(VolumeUnit.CubicFeet).getValue());
        assertEqualsTo48Bits(x/Math.pow(25.4, 3), 
                volume1.convertToUnits(VolumeUnit.CubicInches).getValue());
        assertEqualsTo48Bits(x/Math.pow(1000, 3), 
                volume1.convertToUnits(VolumeUnit.CubicMeters).getValue());
        assertEqualsTo48Bits(x*Math.pow(1000, 3), 
                volume1.convertToUnits(VolumeUnit.CubicMicrons).getValue());
        assertEqualsTo48Bits(x, 
                volume1.convertToUnits(VolumeUnit.CubicMillimeters).getValue());
        assertEqualsTo48Bits(x*Math.pow(1000/25.4, 3), 
                volume1.convertToUnits(VolumeUnit.CubicMils).getValue());
        
        assertEqualsTo48Bits(volume1, volume1.convertToUnits(VolumeUnit.CubicCentimeters).
                convertToUnits(VolumeUnit.CubicFeet).convertToUnits(VolumeUnit.CubicMeters).
                convertToUnits(VolumeUnit.CubicInches).convertToUnits(VolumeUnit.CubicMicrons).
                convertToUnits(VolumeUnit.CubicMils).convertToUnits(VolumeUnit.CubicMillimeters).
                convertToUnits(VolumeUnit.FemtoLiters).convertToUnits(VolumeUnit.MilliLiters).
                convertToUnits(VolumeUnit.MicroLiters));
        

        String yStr = "0.3";
        double y = Double.parseDouble(yStr);
        Volume volume2 = new Volume(y, VolumeUnit.MilliLiters);
        assertEqualsTo48Bits(new Volume(x+1000*y, VolumeUnit.MicroLiters), volume1.add(volume2));
        assertEqualsTo48Bits(new Volume(x+y, VolumeUnit.MicroLiters), volume1.add(y));
        assertEqualsTo48Bits(new Volume(x-1000*y, VolumeUnit.MicroLiters), 
                volume1.subtract(volume2));
        assertEqualsTo48Bits(new Volume(x-y, VolumeUnit.MicroLiters), volume1.subtract(y));
        assertEqualsTo48Bits(new Volume(x*y, VolumeUnit.MicroLiters), volume1.multiply(y));
        assertEqualsTo48Bits(new Volume(x/y, VolumeUnit.MicroLiters), volume1.divide(y));
        assertEqualsTo48Bits(new Area(x/(10*y), AreaUnit.SquareMillimeters), 
                volume1.divide(new Length(y, LengthUnit.Centimeters)));
        assertEqualsTo48Bits(new Length(x/(100*y), LengthUnit.Millimeters), 
                volume1.divide(new Area(y, AreaUnit.SquareCentimeters)));
        assertEqualsTo48Bits(x/(1000*y), volume1.divide(volume2));
        assertEqualsTo48Bits(new Volume(x%(100*y), VolumeUnit.MicroLiters), 
                volume1.modulo(volume2));
        
        assertEqualsTo48Bits(volume1, Volume.parse(xStr + "ul"));
        assertEqualsTo48Bits(new Volume(x, VolumeUnit.MicroLiters), Volume.parse(xStr + "μl"));
        assertEqualsTo48Bits(Volume.parse(xStr + "μl"), Volume.parse(xStr + "ul"));
    }
    
    /**
     * Rounds off the last 4 bits of a double precision number (this still leaves better than 14 
     * decimal digits of precision)
     * @param d - the number to be rounded
     * @return the rounded number
     */
    private double roundTo48Bits(double d) {
        long x = Double.doubleToLongBits(d);
        long xRound = x & 0xfffffffffffffff0L;
        if ((x & 0x0000000000000008L) > 0) {
            xRound += 16;
        }
        return Double.longBitsToDouble(xRound);
    }
    
    private void assertEqualsTo48Bits(Double d1, Double d2) {
        assertEquals(roundTo48Bits(d1), roundTo48Bits(d2));
    }
    
    private void assertEqualsTo48Bits(Length d1, Length d2) {
        org.junit.jupiter.api.Assertions.assertEquals(roundTo48Bits(d1.getValue()), 
                roundTo48Bits(d2.getValue()));
        org.junit.jupiter.api.Assertions.assertEquals(d1.getUnits(), d2.getUnits());
    }
    
    private void assertEqualsTo48Bits(Area d1, Area d2) {
        org.junit.jupiter.api.Assertions.assertEquals(roundTo48Bits(d1.getValue()), 
                roundTo48Bits(d2.getValue()));
        org.junit.jupiter.api.Assertions.assertEquals(d1.getUnits(), d2.getUnits());
    }
    
    private void assertEqualsTo48Bits(Volume d1, Volume d2) {
        org.junit.jupiter.api.Assertions.assertEquals(roundTo48Bits(d1.getValue()), 
                roundTo48Bits(d2.getValue()));
        org.junit.jupiter.api.Assertions.assertEquals(d1.getUnits(), d2.getUnits());
    }
}
