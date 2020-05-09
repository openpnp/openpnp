package org.openpnp.spi;

import org.openpnp.model.Location;
import org.openpnp.model.MappedAxes;

/**
 * Anything that can be mounted to the machine and is Movable has an association 
 * to particular axes on the machine.
 *
 */
public interface MovableMountable extends Movable {
    Axis getAxisX();

    Axis getAxisY();

    Axis getAxisZ();

    Axis getAxisRotation();

    Axis getAxis(Axis.Type type);

    MappedAxes getMappedAxes();

    Location toTransformed(Location location);

    Location toRaw(Location location);
}
