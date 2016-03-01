package org.openpnp.machine.reference;

import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.base.AbstractFeeder;
import org.simpleframework.xml.Element;

public abstract class ReferenceFeeder extends AbstractFeeder {
    @Element
    protected Location location = new Location(LengthUnit.Millimeters);

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
