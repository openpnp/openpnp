package org.openpnp.model;

import org.simpleframework.xml.Attribute;



public class Length {
	@Attribute
	private double value;
	@Attribute
	private LengthUnit units;
	
	public Length(double value, LengthUnit units) {
		this.value = value;
		this.units = units;
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
	
	public static Length parse(String s) {
		return parse(s, false);
	}
	
	/**
	 * Takes a value in the format of a double followed by any number of spaces
	 * followed by the shortName of a LengthUnit value and returns the value
	 * as a Length object. Returns null if the value could not be parsed.
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
		return String.format("%2.3f%s", value, units.getShortName());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Length) {
			Length o = (Length) obj;
			return o.value == value && o.units == units;
		}
		else {
			return false;
		}
	}
}
