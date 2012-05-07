/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
*/

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
