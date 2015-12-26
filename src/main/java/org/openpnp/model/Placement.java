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

import org.openpnp.model.Board.Side;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Version;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;


/**
 * A Placement describes a location on a Board where a Part will be placed, 
 * along with information about how to place it. 
 * @author jason
 */
public class Placement extends AbstractModelObject implements Identifiable {
    public enum Type {
        Place,
        Fiducial,
        Ignore
    }
    
    /**
     * History:
     * 1.0: Initial revision.
     * 1.1: Replaced Boolean place with Type type. Deprecated place.
     */
    @Version(revision=1.1)
    private double version;
    
	@Attribute
	private String id;
	@Element
	private Location location;
	@Attribute
	private Side side = Side.Top;
	
	@Attribute(required=false)
	private String partId;
	
	@Deprecated
	@Attribute(required=false)
	private Boolean place;
	
	@Attribute
	private Type type;
	
	private Part part;

	@SuppressWarnings("unused")
	private Placement() {
		this(null);
	}
	
	public Placement(String id) {
		this.id = id;
		this.type = Type.Place;
		setLocation(new Location(LengthUnit.Millimeters));
	}
	
	@SuppressWarnings("unused")
	@Persist
	private void persist() {
		partId = (part == null ? null : part.getId());
	}
	
	@SuppressWarnings("unused")
	@Commit
	private void commit() {
		setLocation(location);
        if (getPart() == null) {
            setPart(Configuration.get().getPart(partId));
        }
        
        if (version == 1.0) {
            if (place != null && !place) {
                type = Type.Ignore;
            }
            place = null;
        }
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

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		Location oldValue = this.location;
		this.location = location;
		firePropertyChange("location", oldValue, location);
	}

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        Object oldValue = this.side;
        this.side = side;
        firePropertyChange("side", oldValue, side);
    }
    
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        Object oldValue = this.type;
        this.type = type;
        firePropertyChange("type", oldValue, type);
    }
    
    @Override
	public String toString() {
		return String.format("id %s, location %s, side %s, part %s, type %s", id, location, side, part, type);
	}
}
