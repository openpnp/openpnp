package org.openpnp.spi;

import org.openpnp.model.Location;
import org.openpnp.model.Identifiable;
import org.openpnp.model.Named;

/**
 * Created by matt on 05/09/2016.
 */
public abstract class FeederSlot implements Named,Identifiable {
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
    public Feeder getFeeder()
    {
        return feeder;
    }
    public void setFeeder(SlottedFeeder feeder)
    {
        this.feeder = feeder;
    }
}
