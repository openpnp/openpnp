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

import org.openpnp.model.Configuration;

public class PackageCellValue {
	private static Configuration configuration;
	private org.openpnp.model.Package packag;
	
	public static void setConfiguration(Configuration configuration) {
		PackageCellValue.configuration = configuration;
	}
	
	public PackageCellValue(org.openpnp.model.Package packag) {
		this.packag = packag;
	}
	
	public PackageCellValue(String value) {
		org.openpnp.model.Package packag = configuration.getPackage(value);
		if (packag == null) {
			throw new NullPointerException();
		}
		this.packag = packag;
	}

	public org.openpnp.model.Package getPackage() {
		return packag;
	}

	public void setPackage(org.openpnp.model.Package packag) {
		this.packag = packag;
	}
	
	@Override
	public String toString() {
		return packag == null ? "" : packag.getId();
	}
}
