/*
 * Copyright (C) 2023 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
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

import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.io.File;
import org.openpnp.spi.Definable;
import org.openpnp.util.IdentifiableList;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;

/**
 * A PlacementsHolder is an abstraction of an object that has physical 2D extent and contains
 * Placements
 */
public abstract class PlacementsHolder<T extends PlacementsHolder<T>> 
        extends AbstractModelObject implements Definable<T>, PropertyChangeListener {
    
    /**
     * The name of this PlacementsHolder
     */
    @Attribute(required = false)
    protected String name;
    
    /**
     * The physical extent of this PlacementsHolder
     */
    @Element(required = false)
    protected Location dimensions = new Location(LengthUnit.Millimeters);

    /**
     * The list of Placements contained by this PlacementsHolder
     */
    @ElementList(required = false)
    protected IdentifiableList<Placement> placements = new IdentifiableList<>();

    /**
     * The physical outline of this PlacementsHolder
     */
    @Element(required = false)
    protected GeometricPath2D profile = null;
    
    protected transient T definition;
    protected transient File file;
    protected transient boolean dirty;

    @Commit
    protected void commit() {
        for (Placement placement : placements) {
            placement.addPropertyChangeListener(this);
        }
    }
    
    /**
     * Constructs a new PlacementsHolder
     */
    @SuppressWarnings("unchecked")
    public PlacementsHolder() {
        definition = (T) this;
        addPropertyChangeListener(this);
    }
    
    /**
     * Constructs a deep copy of the specified PlacementsHolder
     * @param holderToCopy
     */
    public PlacementsHolder(PlacementsHolder<T> holderToCopy) {
        name = holderToCopy.name;
        dimensions = holderToCopy.dimensions;
        placements = new IdentifiableList<>();
        for (Placement placement : holderToCopy.placements) {
            Placement newPlacement = new Placement(placement);
            placements.add(newPlacement);
            newPlacement.addPropertyChangeListener(this);
        }
        file = holderToCopy.file;
        dirty = holderToCopy.dirty;
        setDefinition(holderToCopy.definition);
        addPropertyChangeListener(this);
    }
    
    /**
     * Cleans-up property change listeners associated with this PlacementsHolder
     */
    @Override
    public void dispose() {
        removePropertyChangeListener(this);
        if (this != definition) {
            definition.removePropertyChangeListener(this);
        }
        for (Placement placement : placements) {
            placement.removePropertyChangeListener(this);
            placement.dispose();
        }
        super.dispose();
    }
    
    /**
     * 
     * @return the name of this PlacementsHolder
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this PlacementsHolder
     * @param name - the name to set
     */
    public void setName(String name) {
        Object oldValue = this.name;
        this.name = name;
        firePropertyChange("name", oldValue, name);
    }

    /**
     * 
     * @return the dimensions (physical extent) of this PlacementsHolder (contained in the X and Y 
     * fields)
     */
    public Location getDimensions() {
        return dimensions;
    }

    /**
     * Sets the dimensions (physical extent) of this PlacementsHolder (contained in the X and Y 
     * fields)
     * @param dimensions
     */
    public void setDimensions(Location dimensions) {
        Location oldValue = this.dimensions;
        this.dimensions = dimensions.derive(null, null, 0.0, 0.0);
        firePropertyChange("dimensions", oldValue, dimensions);
    }

    /**
     * Gets the profile (physical outline) of this PlacementsHolder. If no profile has been defined,
     * a rectangle is returned based on the set dimensions.
     * @return - the profile
     */
    public GeometricPath2D getProfile() {
        if (profile != null) {
            return profile;
        }
        else {
            return new GeometricPath2D(
                    new Rectangle2D.Double(0, 0, dimensions.getX(), dimensions.getY()),
                    dimensions.getUnits());
        }
    }

    /**
     * Sets the profile (physical outline) of this PlacementsHolder.
     * @param profile - the profile of this PlacementsHolder
     */
    public void setProfile(GeometricPath2D profile) {
        Object oldValue = this.profile;
        this.profile = profile;
        firePropertyChange("profile", oldValue, dimensions);
    }

    /**
     * 
     * @return a list of placements contained by this PlacementsHolder
     */
    public IdentifiableList<Placement> getPlacements() {
        return placements;
    }

    /**
     * Sets the list of placements contained by this PlacementsHolder
     * @param placements - the list of placements to set
     */
    public void setPlacements(IdentifiableList<Placement> placements) {
        Object oldValue = this.placements;
        this.placements = placements;
        firePropertyChange("placements", oldValue, placements);
    }

    /**
     * Gets the specified placement.
     * @param index - the index of the placement to get
     * @return the placement
     */
    public Placement getPlacement(int index) {
        return placements.get(index);
    }
    
    /**
     * Sets the placement at the specified index
     * @param index - the index of the placement to set
     * @param placement - the placement to set or null to remove the placement at the specified 
     * index
     */
    public void setPlacement(int index, Placement placement) {
        if (placement != null) {
            placement = new Placement(placement);
            if (index >= placements.size()) {
                placements.add(placement);
            }
            else {
                Placement oldPlacement = placements.get(index);
                oldPlacement.removePropertyChangeListener(this);
                oldPlacement.dispose();
                placements.set(index, placement);
            }
            fireIndexedPropertyChange("placement", index, null, placement);
            placement.addPropertyChangeListener(this);
        }
        else {
            if (index >= 0 && index < placements.size()) {
                placement = placements.get(index);
                fireIndexedPropertyChange("placement", index, placement, null);
                placements.remove(index);
                placement.removePropertyChangeListener(this);
                placement.dispose();
            }
        }
    }

    /**
     * Adds a placement to the list of placements
     * @param placement - the placement to add
     */
    public void addPlacement(Placement placement) {
        if (placement != null) {
            placements.add(placement);
            fireIndexedPropertyChange("placement", placements.indexOf(placement), null, placement);
            placement.addPropertyChangeListener(this);
        }
        
    }
    
    /**
     * Removes the specified placement from the list of placements
     * @param placement - the placement to remove
     */
    public void removePlacement(Placement placement) {
        if (placement != null) {
            setPlacement(placements.indexOf(placement), null);
        }
    }
    
    /**
     * Removes the placement at the specified index 
     * @param index - the index of the placement to remove
     */
    public void removePlacement(int index) {
        setPlacement(index, null);
    }
    
    /**
     * Removes all placements
     */
    public void removeAllPlacements() {
        IdentifiableList<Placement> oldValue = new IdentifiableList<>(placements);
        for (Placement placement : oldValue) {
            removePlacement(placement);
        }
    }
    
    /**
     * Gets the file where this PlacementsHolder is stored
     * @return
     */
    public File getFile() {
        return file;
    }

    /**
     * Sets the file where this PlacementsHolder will be stored
     * @param file
     */
    public void setFile(File file) {
        Object oldValue = this.file;
        this.file = file;
        firePropertyChange("file", oldValue, file);
    }

    @Override
    public T getDefinition() {
        return definition;
    }

    @Override
    public void setDefinition(T definedBy) {
        PlacementsHolder<T> oldValue = this.definition;
        this.definition = definedBy;
        firePropertyChange("definition", oldValue, definedBy);
        if (oldValue != null) {
            oldValue.removePropertyChangeListener(this);
        }
        if (definedBy != null) {
            definedBy.addPropertyChangeListener(this);
        }
    }

    @Override
    public boolean isDefinition(Object definedBy) {
        return this.definition == definedBy;
    }

    /**
     * @return true if this PlacementsHolder has been modified
     */
    @Override
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Sets the state of the dirty flag (used to indicate that this PlacementsHolder has been 
     * modified) to the specified value
     * @param dirty - the state to set the flag
     */
    @Override
    public void setDirty(boolean dirty) {
        boolean oldValue = this.dirty;
        this.dirty = dirty;
        firePropertyChange("dirty", oldValue, dirty);
    }

}
