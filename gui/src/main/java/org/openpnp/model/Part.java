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
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Persist;

/**
 * A Part is a single part that can be picked and placed. It has a graphical outline, is retrieved from one or more Feeders
 * and is placed at a Placement as part of a Job. Parts can be used across many boards and should generally represent
 * a single part in the real world.
 */
public class Part extends AbstractModelObject implements RequiresConfigurationResolution, Identifiable {
	@Attribute
	private String id;
	@Attribute(required=false)
	private String name;
	@Attribute
	private LengthUnit heightUnits;
	@Attribute
	private double height;
	
	private Package packag;
	
	@Attribute
	private String packageId;
	
	@Override
	public void resolve(Configuration configuration) throws Exception {
		setPackage(configuration.getPackage(packageId));
	}
	
	@SuppressWarnings("unused")
	@Persist
	private void persist() {
		packageId = (packag == null ? null : packag.getId());
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		if (id == null) {
			throw new IllegalArgumentException("Part.id must not be null.");
		}
		Object oldValue = this.id;
		this.id = id;
		firePropertyChange("id", oldValue, id);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		Object oldValue = this.name;
		this.name = name;
		firePropertyChange("name", oldValue, name);
	}
	
	public Length getHeight() {
		return new Length(height, heightUnits);
	}
	
	public void setHeight(Length height) {
		Object oldValue = getHeight();
		if (height == null) {
			this.height = 0;
			this.heightUnits = null;
		}
		else {
			this.height = height.getValue();
			this.heightUnits = height.getUnits();
		}
		firePropertyChange("height", oldValue, getHeight());
	}
	
	public Package getPackage() {
		return packag;
	}

	public void setPackage(Package packag) {
		Object oldValue = this.packag;
		this.packag = packag;
		firePropertyChange("package", oldValue, packag);
	}

	@Override
	public String toString() {
		return String.format("id %s, name %s, heightUnits %s, height %f, packageId (%s)", id, name, heightUnits, height, packageId);
	}
}
