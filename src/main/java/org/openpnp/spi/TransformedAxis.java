package org.openpnp.spi;

import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;

/**
 * A TransformedAxis is a Cartesian-coordinate or rotary dimension on the machine that 
 * is transformed from/to a MachineAxis. There are simple single axis transforms and 
 * multi-axis transforms that need to be solved over all the axes. 
 */
public interface TransformedAxis extends Axis {
}
