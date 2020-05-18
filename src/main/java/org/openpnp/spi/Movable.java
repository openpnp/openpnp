package org.openpnp.spi;

import org.openpnp.model.Length;
import org.openpnp.model.Location;

public interface Movable extends Locatable {
    /**
     * Contains all possible options for the moveTo command.
     * SpeedOverPrecision: disable backslash compensation or anything else, that causes additional moves
     */
    public enum MoveToOption { SpeedOverPrecision }

    /**
     * Contains all possible options for getting approximative locations.
     * KeepX, KeepY, KeepZ, KeepRotation: keep these raw coordinates the same. 
     * SuppressStaticCompensation: suppress static compensation, such as non-squareness compensation.
     * SuppressDynamicCompensation: suppress dynamic (i.e. runtime calibrated) compensation, such as
     * nozzle tip runout compensation.
     */
    public enum LocationOption { KeepX, KeepY, KeepZ, KeepRotation, SuppressStaticCompensation, SuppressDynamicCompensation }

    /**
     * Get an approximative Location in order to avoid extra compensation moves. 
     * 
     * @param currentLocation The current location, usually obtained using Headmountable.getLocation()
     * @param desiredLocation The desired location to approximate. 
     * @param options Options for the approximation.
     * @return
     * @throws Exception 
     */
    Location getApproximativeLocation(Location currentLocation, Location desiredLocation, LocationOption... options) throws Exception;

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
    void moveTo(Location location, double speed, MoveToOption... options) throws Exception;

    void moveTo(Location location, MoveToOption... options) throws Exception;

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
