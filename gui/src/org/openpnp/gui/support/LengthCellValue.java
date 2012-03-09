package org.openpnp.gui.support;

import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;

public class LengthCellValue {
	private Length length;

	public LengthCellValue(double value, LengthUnit units) {
		this.length = new Length(value, units);
	}
	
	public LengthCellValue(Length length) {
		this.length = length;
	}

	public LengthCellValue(String value) {
		Length length = Length.parse(value, true);
		if (length == null) {
			throw new NullPointerException();
		}
		this.length = length;
	}
	
	public Length getLength() {
		return length;
	}
	
	public void setLength(Length length) {
		this.length = length;
	}

	@Override
	public String toString() {
		return String.format("%2.3f%s", length.getValue(),
				length.getUnits() == null ? "?" : length.getUnits()
						.getShortName());
	}
}
