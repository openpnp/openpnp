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

public class LongConverter extends Converter<Long, String> {
	private String format;
	
	public LongConverter() {
		this("%d");
	}
	
	public LongConverter(String format) {
		this.format = format;
	}
	
	@Override
	public String convertForward(Long arg0) {
		return String.format(Locale.US,format, arg0);
	}

	@Override
	public Long convertReverse(String arg0) {
		return Long.parseLong(arg0);
	}

}
