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

import org.jdesktop.beansbinding.Converter;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;

public class LengthConverter extends Converter<Length, String> {
	@Override
	public String convertForward(Length length) {
		length = length.convertToUnits(Configuration.get().getSystemUnits());
		return String.format(Locale.US,Configuration.get().getLengthDisplayFormat(), length.getValue());
	}
	
	@Override
	public Length convertReverse(String s) {
		Length length = Length.parse(s, false);
		if (length == null) {
			throw new RuntimeException("Unable to parse " + s);
		}
		if (length.getUnits() == null) {
			length.setUnits(Configuration.get().getSystemUnits());
		}
		return length;
	}
}
