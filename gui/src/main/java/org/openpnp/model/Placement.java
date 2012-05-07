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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.openpnp.RequiresConfigurationResolution;
import org.openpnp.model.Board.Side;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;


/**
 * A Placement describes a location on a Board where a Part will be placed, 
 * along with information about how to place it. 
 * @author jason
 */
public class Placement extends AbstractModelObject implements RequiresConfigurationResolution, PropertyChangeListener {
	@Attribute
	private String id;
	@Element
	private Location location;
	@Attribute
	private Side side = Side.Top;
	private Part part;
	
	@Attribute
	private String partId;
	
	private Placement() {
		setLocation(new Location());
	}
	
	public Placement(String id) {
		this();
		setId(id);
	}
	
	@Override
	public void resolve(Configuration configuration) throws Exception {
		setPart(configuration.getPart(partId));
	}
	
	@SuppressWarnings("unused")
	@Persist
	private void persist() {
		partId = (part == null ? null : part.getId());
	}
	
	@Commit
	private void commit() {
		setLocation(location);
	}
	
	public Part getPart() {
		return part;
	}

	public void setPart(Part part) {
		Part oldValue = this.part;
		this.part = part;
		firePropertyChange("part", oldValue, part);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		Object oldValue = this.id;
		this.id = id;
		firePropertyChange("id", oldValue, id);
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		Location oldValue = this.location;
		this.location = location;
		firePropertyChange("location", oldValue, location);
		if (oldValue != null) {
			oldValue.removePropertyChangeListener(this);
		}
		if (location != null) {
			location.addPropertyChangeListener(this);
		}
	}

	public Side getSide() {
		return side;
	}

	public void setSide(Side side) {
		Object oldValue = this.side;
		this.side = side;
		firePropertyChange("side", oldValue, side);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		propertyChangeSupport.firePropertyChange(evt);
	}
}
