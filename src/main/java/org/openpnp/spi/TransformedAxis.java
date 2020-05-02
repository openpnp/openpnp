package org.openpnp.spi;

import org.openpnp.model.Location;

/**
 * A TransformedAxis is a Cartesian-coordinate or rotary dimension on the machine that 
 * is transformed from/to a MachineAxis. 
 */
public interface TransformedAxis extends Axis {
    public Location transformToRaw(Location location); 
    public Location transformFromRaw(Location location); 
}
