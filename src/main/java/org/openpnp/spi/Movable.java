package org.openpnp.spi;

import org.openpnp.model.Location;

public interface Movable extends Locatable {
    /**
     * Move the object to the Location at the feedRate.
     * 
     * @param location The Location to move to. If the movement should not include a particular
     *        axis, specify Double.NaN for that axis.
     * @param speed Feed rate is specified as a percentage of maximum feed between 0 and 1. 1
     *        specifies maximum feed rate as defined by the machine while 0 defines the absolute
     *        minimum feed rate while still moving.
     * @throws Exception
     */
    public void moveTo(Location location, double speed) throws Exception;

    public void moveTo(Location location) throws Exception;

    public void moveToSafeZ(double speed) throws Exception;

    public void moveToSafeZ() throws Exception;
}
