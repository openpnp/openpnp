package org.openpnp.model;

import org.simpleframework.xml.Element;

public abstract class AbstractLocatable extends AbstractModelObject {

    @Element
    private Location location;

    AbstractLocatable(AbstractLocatable abstractLocatable) {
        super();
        this.location = abstractLocatable.location;
    }
    
    AbstractLocatable(Location location) {
        super();
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

}
