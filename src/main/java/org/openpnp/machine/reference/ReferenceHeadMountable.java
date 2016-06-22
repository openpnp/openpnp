package org.openpnp.machine.reference;

import org.openpnp.model.Location;
import org.openpnp.spi.HeadMountable;

public interface ReferenceHeadMountable extends HeadMountable {
    public Location getHeadOffsets();

    public void setHeadOffsets(Location headOffsets);
}
