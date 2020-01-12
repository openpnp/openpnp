package org.openpnp.machine.reference;

import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.base.AbstractFeeder;
import org.simpleframework.xml.Element;

public abstract class ReferenceFeeder extends AbstractFeeder {
    @Element
    private Location location = new Location(LengthUnit.Millimeters);

    @Override
    public void setParentId(String parentId) {
        Location globalLocation = getLocation();
        super.setParentId(parentId);
        setLocation(globalLocation);
    }

    private Location getLocalLocation() {
        return location;
    }

    private void setLocalLocation(Location localLocation) {
        Object oldValue = this.location;
        this.location = localLocation;
        firePropertyChange("location", oldValue, localLocation);
    }
    
    public Location getLocation() {
        Location parentLocation;
        if (parentId.equals(ROOT_FEEDER_ID)) {
            parentLocation = new Location(LengthUnit.Millimeters,0,0,0,0);
        } else {
            parentLocation = ((ReferenceFeeder) Configuration.get().getMachine().getFeeder(parentId)).getLocation();
        }
        return convertToGlobalLocation(parentLocation, getLocalLocation());
    }

    public void setLocation(Location globalLocation) {
        Location parentLocation;
        if (parentId.equals(ROOT_FEEDER_ID)) {
            parentLocation = new Location(LengthUnit.Millimeters,0,0,0,0);
        } else {
            parentLocation = ((ReferenceFeeder) Configuration.get().getMachine().getFeeder(parentId)).getLocation();
        }
        setLocalLocation(convertToLocalLocation(parentLocation, globalLocation));
    }
    
    public Location convertToGlobalLocation(Location localLocation) {
        Location originLocation = getLocation();
        return convertToGlobalLocation(originLocation, localLocation);
    }
    
    public Location convertToLocalLocation(Location globalLocation) {
        Location originLocation = getLocation();
        return convertToLocalLocation(originLocation, globalLocation);
    }

    public Location convertToGlobalDeltaLocation(Location localDeltaLocation) {
        Location originLocation = getLocation();
        return convertToGlobalDeltaLocation(originLocation, localDeltaLocation);
    }
    
    public Location convertToLocalDeltaLocation(Location globalDeltaLocation) {
        Location originLocation = getLocation();
        return convertToLocalDeltaLocation(originLocation, globalDeltaLocation);
    }

    static public Location convertToGlobalLocation(Location origin, Location localLocation) {
        if (localLocation == null) {
            return null;
        }
        return localLocation.vectorAdd(origin);
    }
    
    static public Location convertToLocalLocation(Location origin, Location globalLocation) {
        if (globalLocation == null) {
            return null;
        }
        return globalLocation.vectorSubtract(origin);
    }
    
    static public Location convertToGlobalDeltaLocation(Location origin, Location localDeltaLocation) {
        if (localDeltaLocation == null) {
            return null;
        }
        return convertToGlobalLocation(origin, localDeltaLocation).subtract(origin);
    }

    static public Location convertToLocalDeltaLocation(Location origin, Location globalDeltaLocation) {
        if (globalDeltaLocation == null) {
            return null;
        }
        return convertToLocalLocation(origin, globalDeltaLocation.add(origin));
    }

}
