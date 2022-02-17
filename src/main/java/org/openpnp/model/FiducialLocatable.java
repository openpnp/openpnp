package org.openpnp.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openpnp.util.IdentifiableList;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

public class FiducialLocatable extends AbstractModelObject implements PropertyChangeListener {
    
    @Attribute(required = false)
    private String name;
    
    @Element(required = false)
    protected Location dimensions = new Location(LengthUnit.Millimeters);

    @ElementList(required = false)
    protected IdentifiableList<Placement> placements = new IdentifiableList<>();

    public FiducialLocatable() {
    }
    
    public FiducialLocatable(FiducialLocatable fiducialLocatable) {
        this.name = fiducialLocatable.name;
        this.dimensions = fiducialLocatable.dimensions;
        placements.addAll(fiducialLocatable.placements);
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
//        return Collections.unmodifiableList(placements);
        return new IdentifiableList<>(placements);
    }

    public void addPlacement(Placement placement) {
        Object oldValue = placements;
        placements = new IdentifiableList<>(placements);
        placements.add(placement);
        firePropertyChange("placements", oldValue, placements);
        if (placement != null) {
            placement.addPropertyChangeListener(this);
        }
    }

    public void removePlacement(Placement placement) {
        Object oldValue = placements;
        placements = new IdentifiableList<>(placements);
        placements.remove(placement);
        firePropertyChange("placements", oldValue, placements);
        if (placement != null) {
            placement.removePropertyChangeListener(this);
        }
    }

    public void removeAllPlacements() {
        ArrayList<Placement> oldValue = placements;
        placements = new IdentifiableList<>();
        firePropertyChange("placements", oldValue, placements);
        for (Placement placement : oldValue) {
            placement.removePropertyChangeListener(this);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // TODO Auto-generated method stub
    }

}
