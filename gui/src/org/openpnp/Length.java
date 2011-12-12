package org.openpnp;

public class Length {
	private double value;
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
}
