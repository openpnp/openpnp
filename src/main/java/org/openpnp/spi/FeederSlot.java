package org.openpnp.spi;

import org.openpnp.model.Location;


/**
 * Created by matt on 05/09/2016.
 */
public abstract class FeederSlot implements org.openpnp.model.Identifiable {
    SlottedFeeder feeder;
    Location pickLocation;

    public FeederSlot()
    {
        feeder = null;
        pickLocation = null;
    }
    public Location getPickLocation()
    {
        return pickLocation;
    }
}
