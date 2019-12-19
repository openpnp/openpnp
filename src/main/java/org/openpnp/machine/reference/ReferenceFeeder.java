package org.openpnp.machine.reference;

import org.openpnp.machine.reference.feeder.ReferenceFeederGroup;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.base.AbstractFeeder;
import org.simpleframework.xml.Element;

public abstract class ReferenceFeeder extends AbstractFeeder {
    @Element
    protected Location location = new Location(LengthUnit.Millimeters);

    protected Location getLocalLocation() {
        return location;
    }

    public Location getLocation() {
        return convertToGlobalLocation(location);
    }

    protected void setLocalLocation(Location localLocation) {
        Object oldValue = this.location;
        this.location = localLocation;
        firePropertyChange("location", oldValue, localLocation);
    }
    
    public void setLocation(Location globalLocation) {
        setLocalLocation(convertToLocalLocation(globalLocation));
    }
    
    public Location convertToGlobalLocation(Location localLocation) {
        Location globalLocation;
        if (owner.equals("Machine")) {
            globalLocation = localLocation;
        } else {
            Location ownerLocation = ((ReferenceFeederGroup) Configuration.get().getMachine().getFeederByName(owner)).getLocation();
            globalLocation = localLocation.rotateXy(ownerLocation.getRotation()).addWithRotation(ownerLocation);
        }
        return globalLocation;
    }
    
    public Location convertToLocalLocation(Location globalLocation) {
        Location localLocation;
        if (owner.equals("Machine")) {
            localLocation = globalLocation;
        } else {
            Location ownerLocation = ((ReferenceFeederGroup) Configuration.get().getMachine().getFeederByName(owner)).getLocation();
            localLocation = globalLocation.rotateXy(-ownerLocation.getRotation()).subtractWithRotation(ownerLocation);
        }
        return localLocation;
    }
}
