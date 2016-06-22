package org.openpnp.spi;

import org.openpnp.model.Location;

/**
 * An object which has a Location in 4D space. The Location may be fixed or Movable. The caller of
 * this method becomes the owner of the returned object and may modify the Location. The returned
 * object should be a clone or copy of the object if modification of it will adversely affect the
 * callee.
 */
public interface Locatable {
    public Location getLocation();
}
