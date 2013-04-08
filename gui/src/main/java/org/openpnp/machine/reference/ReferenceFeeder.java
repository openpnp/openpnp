package org.openpnp.machine.reference;

import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.base.AbstractFeeder;
import org.simpleframework.xml.Element;

public abstract class ReferenceFeeder extends AbstractFeeder {
    @Element
    protected Location pickLocation = new Location(LengthUnit.Millimeters);
    
    @Override
    public Location getPickLocation() throws Exception{
        return pickLocation;
    }
}
