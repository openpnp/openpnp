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

    /**
     * Contains all possible options for getting approximative locations.
     */
    public enum LocationOption { 
        /**
         * Keep the raw X coordinate as is, specified in currentLocation. 
         */
        KeepX, 
        /**
         * Keep the raw Y coordinate as is, specified in currentLocation. 
         */
        KeepY, 
        /**
         * Keep the raw Z coordinate as is, specified in currentLocation. 
         */
        KeepZ, 
        /**
         * Keep the raw Rotation coordinate as is, specified in currentLocation. 
         */
        KeepRotation, 
        /**
         * Suppress camera calibration, such as advanced camera calibration head offsets, 
         * i.e. tilt compensation.
         */
        SuppressCameraCalibration, 
        /**
         * Suppress static compensations, such as non-squareness compensation.
         */
        SuppressStaticCompensation, 
        /**
         * Suppress dynamic (i.e. runtime calibrated) compensations, such as
         * NozzleTip runout compensation.
         */
        SuppressDynamicCompensation,
        /**
         * Replaces virtual coordinates with the head offset.
         */
        ReplaceVirtual, 
        /**
         * Apply soft limits.
         */
        ApplySoftLimits, 
        /**
         * Be quiet about transforms. 
         */
        Quiet
    }

    /**
     * Get an approximative Location in order to avoid extra compensation transformations or moves. 
     * 
     * @param currentLocation The current location, usually obtained using Headmountable.getLocation()
     * @param desiredLocation The desired location to approximate. 
     * @param options Options for the approximation.
     * @return
     * @throws Exception 
     */
    Location getApproximativeLocation(Location currentLocation, Location desiredLocation, LocationOption... options) throws Exception;

}
