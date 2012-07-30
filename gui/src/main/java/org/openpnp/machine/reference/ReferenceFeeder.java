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

package org.openpnp.machine.reference;

import org.openpnp.RequiresConfigurationResolution;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

/**
 * A common base class for Feeders that the ReferenceMachine supports.
 */
public abstract class ReferenceFeeder implements Feeder, RequiresConfigurationResolution {
	// TODO: Remove a few versions from now, once people have had their XML
	// upgraded.
	@Attribute(required=false)
	protected String id;
	@Attribute(required=false)
	protected String name;
	@Attribute(required=false)
	protected boolean enabled;
	@Element(required=false)
	protected Location location = new Location(LengthUnit.Millimeters);
	@Attribute(required=false)
	protected String partId;
	
	protected Part part;
	
	@Override
	public void resolve(Configuration configuration) throws Exception {
		setPart(configuration.getPart(partId));
	}
	
	@SuppressWarnings("unused")
	@Commit
	private void commit() {
		if (name == null) {
			name = id;
		}
		id = null;
	}
	
	@SuppressWarnings("unused")
	@Persist
	private void persist() {
		partId = (part == null ? null : part.getId());
	}
	
	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public void setLocation(Location location) {
		this.location = location;
	}

	public String getPartId() {
		return partId;
	}

	public void setPartId(String partId) {
		this.partId = partId;
	}

	@Override
	public Part getPart() {
		return part;
	}

	@Override
	public void setPart(Part part) {
		this.part = part;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}
	
	public void start(ReferenceMachine machine) throws Exception {
		
	}
}
