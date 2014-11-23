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

import java.util.Locale;

import org.openpnp.model.Configuration;
import org.openpnp.model.Length;

public class LengthCellValue {
	private static Configuration configuration;
	
	private Length length;
	
	/**
	 * When set, the toString() method will show the units contained within
	 * the Length instead of converting to the system units.
	 */
	private boolean displayNativeUnits;
	
	public static void setConfiguration(Configuration configuration) {
		LengthCellValue.configuration = configuration;
	}

	public LengthCellValue(Length length, boolean displayNativeUnits) {
		setLength(length);
		setDisplayNativeUnits(displayNativeUnits);
	}

	public LengthCellValue(Length length) {
		this(length, false);
	}

	public LengthCellValue(String value) {
		Length length = Length.parse(value, false);
		if (length == null) {
			throw new NullPointerException();
		}
		setLength(length);
	}

	public Length getLength() {
		return length;
	}

	public void setLength(Length length) {
		this.length = length;
	}
	
	public boolean isDisplayNativeUnits() {
		return displayNativeUnits;
	}

	public void setDisplayNativeUnits(boolean displayNativeUnits) {
		this.displayNativeUnits = displayNativeUnits;
	}

	@Override
	public String toString() {
		Length l = length;
		if (l.getUnits() == null) {
			return String.format(Locale.US,configuration.getLengthDisplayFormatWithUnits(), l.getValue(), "?");
		}
		if (displayNativeUnits && l.getUnits() != configuration.getSystemUnits()) {
			return String.format(Locale.US,configuration.getLengthDisplayFormatWithUnits(), l.getValue(), l.getUnits().getShortName());
		}
		else {
			l = l.convertToUnits(configuration.getSystemUnits());
			return String.format(Locale.US,configuration.getLengthDisplayFormat(), l.getValue());
		}
	}
}
