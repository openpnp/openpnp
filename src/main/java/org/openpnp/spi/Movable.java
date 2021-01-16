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


    /**
     * @return The lower and upper limits of the Safe Zone as defined on the Z axis. The array elements may be null 
     * if no axis is mapped on the Movable or no Safe Zone defined on the axis. 
     */
    Length[] getSafeZZone();

    /**
     * @return The lower limit of the Safe Zone as defined on the Z axis, or null if now axis is mapped
     * on the Movable or no Safe Zone defined on the axis. 
     */
    Length getSafeZ();

    /**
     * @return The effective Safe Z, including any dynamic adjustment such as for parts on a Nozzle. 
     * @throws Exception 
     */
    Length getEffectiveSafeZ() throws Exception;

    /**
     * @param z
     * @return True if the given z is in the Safe Z Zone.
     * @throws Exception
     */
    boolean isInSafeZZone(Length z) throws Exception;

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
