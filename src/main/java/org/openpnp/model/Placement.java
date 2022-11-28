/*
 * Copyright (C) 2022 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.model;

import java.beans.PropertyChangeEvent;

import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Version;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;


/**
 * A Placement describes a location on a Board where a Part will be placed, along with information
 * about how to place it.
 * 
 * @author jason
 */
public class Placement extends Abstract2DLocatable<Placement> {
    public enum Type {
        Placement, 
        Fiducial,
        @Deprecated
        Place, 
        @Deprecated
        Ignore
    }
    
    public enum ErrorHandling {
        Alert, Defer
    }

    /**
     * History: 1.0: Initial revision. 
     * 1.1: Replaced Boolean place with Type type. Deprecated place.
     * 1.2: Removed glue attribute.
     * 1.3: Removed checkFids attribute.
     * 1.4: Changed Type.Place to Type.Placement, and removed Type.Ignore.
     */
    @Version(revision = 1.4)
    private double version;

    @Attribute(required = false)
    private String partId;

    @Attribute(required = false)
    private Type type;

    private Part part;

    @Element(required = false)
    private String comments;
    
    @Element(required = false)
    private ErrorHandling errorHandling = ErrorHandling.Alert;
    
    @Attribute(required = false)
    private boolean enabled = true;

    private Placement() {
        super(new Location(LengthUnit.Millimeters));
    }

    public Placement(Placement placement) {
        super(placement);
        this.comments = placement.comments;
        this.enabled = placement.enabled;
        this.errorHandling = placement.errorHandling;
        this.part = placement.part;
        this.partId = placement.partId;
        this.side = placement.side;
        this.type = placement.type;
        this.version = placement.version;
    }
    
    public Placement(String id) {
        super(new Location(LengthUnit.Millimeters));
        this.id = id;
        this.type = Type.Placement;
    }

    @SuppressWarnings("unused")
    @Persist
    private void persist() {
        partId = (part == null ? null : part.getId());
    }

    @SuppressWarnings("unused")
    @Commit
    private void commit() {
        setLocation(getLocation());
        if (getPart() == null) {
            setPart(Configuration.get().getPart(partId));
        }
        if (getType() == Type.Ignore) {
            setType(Type.Placement);
            setEnabled(false);
        }
        if (getType() == Type.Place) {
            setType(Type.Placement);
        }
    }

    public Part getPart() {
        return part;
    }

    public void setPart(Part part) {
        Part oldValue = this.part;
        this.part = part;
        firePropertyChange("part", oldValue, part);
        // Also notify the old/new part that the placement count has changed.
        if (oldValue != null) {
            oldValue.setPlacementCount(-1);
        }
        if (part != null) {
            part.setPlacementCount(+1);
        }
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        Object oldValue = this.type;
        this.type = type;
        firePropertyChange("type", oldValue, type);
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        Object oldValue = this.comments;
        this.comments = comments;
        firePropertyChange("comments", oldValue, comments);
    }
    
    public ErrorHandling getErrorHandling() {
        return errorHandling;
    }

    public void setErrorHandling(ErrorHandling errorHandling) {
        Object oldValue = this.errorHandling;
        this.errorHandling = errorHandling;
        firePropertyChange("errorHandling", oldValue, errorHandling);
    }
    
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        Object oldValue = this.enabled;
        this.enabled = enabled;
        firePropertyChange("enabled", oldValue, enabled);
    }

    @Override
    public String toString() {
        return String.format("id %s, location %s, side %s, part %s, type %s", id, getLocation(), side,
                part, type);
    }
}
