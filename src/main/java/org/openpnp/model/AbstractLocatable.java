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
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;
import org.openpnp.spi.Definable;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public abstract class AbstractLocatable extends AbstractModelObject implements Definable<AbstractLocatable>, Identifiable, PropertyChangeListener {

    @Element
    protected Location location;

    @Attribute(required = false)
    protected String id;
    
    protected transient AbstractLocatable definedBy;

    private transient boolean dirty;
    
    AbstractLocatable() {
        definedBy = this;
    }
    
    AbstractLocatable(AbstractLocatable abstractLocatable) {
        super();
        location = abstractLocatable.location;
        setDefinedBy(abstractLocatable.getDefinedBy());
        id = abstractLocatable.id;
    }
    
    AbstractLocatable(Location location) {
        this();
        this.location = location;
    }
    
    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        Location oldValue = this.location;
        this.location = location;
        firePropertyChange("location", oldValue, location);
    }

    public AbstractLocatable getDefinedBy() {
        return definedBy;
    }
    
    public void setDefinedBy(AbstractLocatable definedBy) {
        AbstractLocatable oldValue = this.definedBy;
        this.definedBy = definedBy;
        firePropertyChange("definedBy", oldValue, definedBy);
        if (oldValue != null) {
            oldValue.removePropertyChangeListener(this);
        }
        if (definedBy != null) {
            definedBy.addPropertyChangeListener(this);
        }
    }
    
    public boolean isDefinedBy(AbstractLocatable definedBy) {
        return this.definedBy == definedBy;
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        String oldValue = this.id;
        this.id = id;
        firePropertyChange("id", oldValue, id);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        boolean oldValue = this.dirty;
        this.dirty = dirty;
        firePropertyChange("dirty", oldValue, dirty);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
//        Logger.trace(String.format("PropertyChangeEvent handled by AbstractLocatable @%08x = %s", this.hashCode(), evt));
        if (evt.getSource() != AbstractLocatable.this || evt.getPropertyName() != "dirty") {
            dirty = true;
            if (evt.getSource() == definedBy) {
                try {
                    Logger.trace("Attempting to set property: " + evt.getPropertyName() + " = " + evt.getNewValue());
                    BeanUtils.setProperty(this, evt.getPropertyName(), evt.getNewValue());
                }
                catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        
    }
}
