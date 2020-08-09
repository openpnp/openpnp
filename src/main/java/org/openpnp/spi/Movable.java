package org.openpnp.spi;

import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Motion.MotionOption;

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
    void moveTo(Location location, double speed, MotionOption... options) throws Exception;

    void moveTo(Location location, MotionOption... options) throws Exception;

    Length getSafeZ();

    Length getEffectiveSafeZ();

    void moveToSafeZ(double speed) throws Exception;

    void moveToSafeZ() throws Exception;

    /**
     * Perform any homing operation on each movable. The head and driver have already been homed
     * at this time. 
     * 
     * @throws Exception
     */
    void home() throws Exception;

}
