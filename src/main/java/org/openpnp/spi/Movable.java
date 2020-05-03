package org.openpnp.spi;

import org.openpnp.model.Location;

public interface Movable extends Locatable {    
    /**
     * Contains all possible options for the moveTo command.
     * RAW: disable all internal corrections, just tell the driver to move to that position
     * NO_ADDITIONAL_MOVES: disable backslash compensation or anything else, that causes aditional moves
     */
    public enum MoveToOption { RawMove, SpeedOverPrecision }

    
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
    public void moveTo(Location location, double speed, MoveToOption... options) throws Exception;

    public void moveTo(Location location, MoveToOption... options) throws Exception;

    public void moveToSafeZ(double speed) throws Exception;

    public void moveToSafeZ() throws Exception;
    
    /**
     * Perform any homing operation on each movable. The head and driver have already been homed
     * at this time. 
     * 
     * @throws Exception
     */
    public void home() throws Exception;
}
