package org.openpnp.gui.support;

import org.openpnp.Length;
import org.openpnp.LengthUnit;
import org.openpnp.util.LengthUtil;

public class LengthCellValue {
	private Length length;

	public LengthCellValue(double value, LengthUnit units) {
		this.length = new Length(value, units);
	}

	public LengthCellValue(String value) {
		Length length = LengthUtil.parseLengthValue(value, true);
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
