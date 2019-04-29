package org.openpnp.spi;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Location;
import org.openpnp.model.Named;

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
     * Get the tool specific calibrated offset for the camera.
     * @see org.openpnp.spi.Camera.getLocation(HeadMountable)
     * 
     * @param camera
     * @return
     */
    Location getCameraToolCalibratedOffset(Camera camera);
}
