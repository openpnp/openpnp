package org.openpnp.gui.support;

import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Length;
import org.openpnp.model.Location;

/**
 * A proxy class that allows bindings to mutate a Location field by field
 * by replacing the bound Location whenever a field is changed. 
 */
public class MutableLocationProxy extends AbstractModelObject {
    private Location location;
    
    public Location getLocation() {
        return location;
    }
    
    public void setLocation(Location location) {
        this.location = location;
        firePropertyChange("location", null, getLocation());
    }
    
    public void setLengthX(Length l) {
        if (l.getUnits() != location.getUnits()) {
            location = location.convertToUnits(l.getUnits());
            location = location.derive(l.getValue(), null, null, null);
            firePropertyChange("lengthX", null, getLengthX());
            firePropertyChange("lengthY", null, getLengthY());
            firePropertyChange("lengthZ", null, getLengthZ());
            firePropertyChange("location", null, getLocation());
        }
        else {
            location = location.derive(l.getValue(), null, null, null);
            firePropertyChange("lengthX", null, getLengthX());
            firePropertyChange("location", null, getLocation());
        }
    }
    
    public void setLengthY(Length l) {
        if (l.getUnits() != location.getUnits()) {
            location = location.convertToUnits(l.getUnits());
            location = location.derive(null, l.getValue(), null, null);
            firePropertyChange("lengthX", null, getLengthX());
            firePropertyChange("lengthY", null, getLengthY());
            firePropertyChange("lengthZ", null, getLengthZ());
            firePropertyChange("location", null, getLocation());
        }
        else {
            location = location.derive(null, l.getValue(), null, null);
            firePropertyChange("lengthY", null, getLengthY());
            firePropertyChange("location", null, getLocation());
        }
    }
    
    public void setLengthZ(Length l) {
        if (l.getUnits() != location.getUnits()) {
            location = location.convertToUnits(l.getUnits());
            location = location.derive(null, null, l.getValue(), null);
            firePropertyChange("lengthX", null, getLengthX());
            firePropertyChange("lengthY", null, getLengthY());
            firePropertyChange("lengthZ", null, getLengthZ());
            firePropertyChange("location", null, getLocation());
        }
        else {
            location = location.derive(null, null, l.getValue(), null);
            firePropertyChange("lengthZ", null, getLengthY());
            firePropertyChange("location", null, getLocation());
        }
    }
    
    public double getRotation() {
        return location.getRotation();
    }
    
    public void setRotation(double rotation) {
        location = location.derive(null, null, null, rotation);
        firePropertyChange("rotation", null, getRotation());
        firePropertyChange("location", null, getLocation());
    }
    
    public Length getLengthX() {
        return location.getLengthX();
    }
    
    public Length getLengthY() {
        return location.getLengthY();
    }
    
    public Length getLengthZ() {
        return location.getLengthZ();
    }
}

