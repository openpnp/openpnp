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

package org.openpnp.model;

import org.openpnp.RequiresConfigurationResolution;
import org.openpnp.spi.Feeder;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Persist;

public class FeederLocation implements RequiresConfigurationResolution {
	private Feeder feeder;
	@Element
	private Location location;
	
	@Attribute
	private String feederId;
	
	public FeederLocation() {
		setLocation(new Location());
	}

	@Override
	public void resolve(Configuration configuration) throws Exception {
		feeder = configuration.getMachine().getFeeder(feederId);
	}
	
	@SuppressWarnings("unused")
	@Persist
	private void persist() {
		feederId = (feeder == null ? null : feeder.getId());
	}
	
	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}
	
	public Feeder getFeeder() {
		return feeder;
	}

	public void setFeeder(Feeder feeder) {
		this.feeder = feeder;
	}

	@Override
	public String toString() {
		return String.format("feederId (%s), location (%s)", feederId, location);
	}
}