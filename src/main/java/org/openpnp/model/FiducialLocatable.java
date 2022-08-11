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

import java.beans.IndexedPropertyChangeEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.openpnp.gui.MainFrame;
import org.openpnp.spi.Definable;
import org.openpnp.util.IdentifiableList;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

public abstract class FiducialLocatable extends AbstractModelObject implements Definable<FiducialLocatable>, PropertyChangeListener {
    
    @Attribute(required = false)
    private String name;
    
    @Element(required = false)
    protected Location dimensions = new Location(LengthUnit.Millimeters);

    @ElementList(required = false)
    protected IdentifiableList<Placement> placements = new IdentifiableList<>();

    protected transient FiducialLocatable definedBy;
    protected transient File file;
    
    protected transient boolean dirty;


    public FiducialLocatable() {
        definedBy = this;
    }
    
    public FiducialLocatable(FiducialLocatable fiducialLocatable) {
        name = fiducialLocatable.name;
        dimensions = fiducialLocatable.dimensions;
        placements = new IdentifiableList<>();
        for (Placement placement : fiducialLocatable.placements) {
            placements.add(new Placement(placement));
        }
        file = fiducialLocatable.file;
        dirty = fiducialLocatable.dirty;
        setDefinedBy(fiducialLocatable.definedBy);
    }
    
    public void setTo(FiducialLocatable fiducialLocatable) {
        name = fiducialLocatable.name;
        dimensions = fiducialLocatable.dimensions;
        placements = new IdentifiableList<>();
        for (Placement placement : fiducialLocatable.placements) {
            placements.add(new Placement(placement));
        }
        file = fiducialLocatable.file;
        dirty = fiducialLocatable.dirty;
        setDefinedBy(fiducialLocatable.definedBy);
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        Object oldValue = this.name;
        this.name = name;
        firePropertyChange("name", oldValue, name);
    }

    public Location getDimensions() {
        return dimensions;
    }

    public void setDimensions(Location location) {
        Location oldValue = this.dimensions;
        this.dimensions = location;
        firePropertyChange("dimensions", oldValue, location);
    }

    public IdentifiableList<Placement> getPlacements() {
        return new IdentifiableList<>(placements);
    }

//    public void setPlacements(IdentifiableList<Placement> placements) {
//        IdentifiableList<Placement> oldValue = this.placements;
//        this.placements = placements;
//        for (Placement placement : oldValue) {
//            placement.removePropertyChangeListener(this);
//        }
//        for (Placement placement : this.placements) {
//            placement.addPropertyChangeListener(this);
//        }
//        firePropertyChange("placements", oldValue, placements);
//    }
//    
//    public void addPlacement(Placement placement) {
//        Object oldValue = placements;
//        placements = new IdentifiableList<>(placements);
//        placements.add(placement);
//        firePropertyChange("placements", oldValue, placements);
//        if (placement != null) {
//            placement.addPropertyChangeListener(this);
//        }
//    }
//
//    public void removePlacement(Placement placement) {
//        Object oldValue = placements;
//        placements = new IdentifiableList<>(placements);
//        placements.remove(placement);
//        firePropertyChange("placements", oldValue, placements);
//        if (placement != null) {
//            placement.removePropertyChangeListener(this);
//        }
//    }
//
//    public void removeAllPlacements() {
//        IdentifiableList<Placement> oldValue = placements;
//        placements = new IdentifiableList<>();
//        firePropertyChange("placements", oldValue, placements);
//        for (Placement placement : oldValue) {
//            placement.removePropertyChangeListener(this);
//        }
//    }

    public void setPlacements(IdentifiableList<Placement> placements) {
        this.placements = placements;
    }

    public void setPlacements(int index, Placement placement) {
        placements.add(index, placement);
    }

    public void addPlacement(Placement placement) {
        if (placement != null) {
            placements.add(placement);
            fireIndexedPropertyChange("placements", placements.indexOf(placement), null, placement);
            placement.addPropertyChangeListener(this);
            placement.getDefinedBy().addPropertyChangeListener(placement);
        }
        
    }
    
    public void removePlacement(Placement placement) {
        if (placement != null) {
            int index = placements.indexOf(placement);
            if (index >= 0) {
                placements.remove(placement);
                fireIndexedPropertyChange("placements", index, placement, null);
                placement.removePropertyChangeListener(this);
                placement.getDefinedBy().removePropertyChangeListener(placement);
            }
        }
    }
    
    public void removeAllPlacements() {
        Object oldValue = new IdentifiableList<>(placements);
        for (Placement placement : placements) {
            placement.removePropertyChangeListener(this);
            placement.getDefinedBy().removePropertyChangeListener(placement);
        }
        placements = new IdentifiableList<>();
        firePropertyChange("placements", oldValue, placements);
    }
    
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        Object oldValue = this.file;
        this.file = file;
        firePropertyChange("file", oldValue, file);
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
    public FiducialLocatable getDefinedBy() {
        return definedBy;
    }

    @Override
    public void setDefinedBy(FiducialLocatable definedBy) {
        FiducialLocatable oldValue = this.definedBy;
        this.definedBy = definedBy;
        firePropertyChange("definedBy", oldValue, definedBy);
        if (oldValue != null) {
            oldValue.removePropertyChangeListener(this);
        }
        if (definedBy != null) {
            definedBy.addPropertyChangeListener(this);
        }
    }

    @Override
    public boolean isDefinedBy(FiducialLocatable definedBy) {
        return this.definedBy == definedBy;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Logger.trace(String.format("PropertyChangeEvent handled by FiducialLocatable @%08x = %s", this.hashCode(), evt));
        if (evt.getSource() != FiducialLocatable.this && evt.getPropertyName() != "dirty") {
            dirty = true;
            if (evt.getSource() == definedBy) {
                if (evt.getPropertyName() == "placements") {
                    int index = ((IndexedPropertyChangeEvent) evt).getIndex();
                    if (evt.getNewValue() instanceof Placement) {
                        Placement placement = new Placement((Placement) evt.getNewValue());
                        placement.addPropertyChangeListener(this);
                        placement.getDefinedBy().addPropertyChangeListener(placement);
                        placements.add(placement);
                    }
                    else if (evt.getOldValue() instanceof Placement) {
                        Placement placement = placements.get(index);
                        placement.removePropertyChangeListener(this);
                        placement.getDefinedBy().removePropertyChangeListener(placement);
                        placements.remove(index);
                    }
                }
                else {
                    try {
                        Logger.trace("setting property: " + evt.getPropertyName() + " = " + evt.getNewValue());
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

}
