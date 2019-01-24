package org.openpnp.spi;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Named;
import org.openpnp.model.Length;

public interface HeadMountable extends Movable, Identifiable, Named {
    /**
     * Gets the Head that this HeadMountable is attached to. If it is not attached to a Head this
     * method returns null.
     * 
     * @return
     */
    Head getHead();
    
    /**
     * Set the Head that this HeadMountable is attached to. Called by the Head when the
     * HeadMountable is added to it.
     */
    void setHead(Head head);

    /**
     * Gets the Safe Z that this HeadMountable must be raised to before moving. If not needed this
     * method returns NaN.
     * 
     * @return
     */
    Length getSafeZ();
    
    /**
     * Set the Safe Z that this HeadMountable must be raised to before moving. 
     */
    void setSafeZ(Length safeZ);
}
