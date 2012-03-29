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
import org.openpnp.spi.Feeder;

public class FeederCellValue {
	private static Configuration configuration;
	private Feeder feeder;
	
	public static void setConfiguration(Configuration configuration) {
		FeederCellValue.configuration = configuration;
	}
	
	public FeederCellValue(Feeder feeder) {
		this.feeder = feeder;
	}
	
	public FeederCellValue(String value) {
		Feeder feeder = configuration.getMachine().getFeeder(value);
		if (feeder == null) {
			throw new NullPointerException();
		}
		this.feeder = feeder;
	}

	public Feeder getFeeder() {
		return feeder;
	}

	public void setFeeder(Feeder feeder) {
		this.feeder = feeder;
	}
	
	@Override
	public String toString() {
		return feeder == null ? "" : feeder.getId();
	}
}
