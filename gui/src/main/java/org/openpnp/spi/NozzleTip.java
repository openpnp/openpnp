package org.openpnp.spi;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Part;

public interface NozzleTip extends Identifiable {
    /**
     * Returns true if this NozzleTip can handle the given Part. The tip can
     * use attributes such as Package, Outline, weight, etc. to determine if
     * this is true.
     * @param part
     * @return
     */
    public boolean canHandle(Part part);
}
