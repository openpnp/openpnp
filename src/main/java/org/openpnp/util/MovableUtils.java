package org.openpnp.util;

import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Locatable.LocationOption;
import org.openpnp.spi.Machine;
import org.openpnp.spi.base.AbstractMachine;
import org.pmw.tinylog.Logger;

public class MovableUtils {
    /**
     * Moves the given HeadMountable to the specified Location by first commanding the head to
     * safe-Z all of it's components, then moving the HeadMountable in X, Y and C, followed by
     * moving in Z.
     * Note: this will not move to safe Z if the X, Y, C coordinates remain unchanged.
     * 
     * @param hm
     * @param location
     * @param speed
     * @throws Exception
     */
    public static void moveToLocationAtSafeZ(HeadMountable hm, Location location, double speed)
            throws Exception {
        Head head = hm.getHead();
        Location currentLocationWithNewZ = hm.getLocation().derive(location, false, false, true, false);
        if (! hm.toRaw(hm.toHeadLocation(location, LocationOption.Quiet), LocationOption.Quiet)
                .matches(hm.toRaw(hm.toHeadLocation(currentLocationWithNewZ, LocationOption.Quiet), LocationOption.Quiet))) {
            // Moves in X, Y or C, move to safe Z needed. 
            head.moveToSafeZ(speed);
            // Determine the exit Safe Z of that hm to optimize the move. In shared axis configurations with a Safe Z Zone
            // OpenPnP will then move the hm's transformed Z to the lower limit of the Zone, i.e. ready to dive down as
            // quick as possible. 
            Length safeZ = hm.getEffectiveSafeZ();
            if (safeZ != null) {
                // If the target Z is higher than safe Z, optimize, but stay within the Safe Z Zone.
                if (location.getLengthZ().compareTo(safeZ) > 0) {
                    Length[] zone = hm.getSafeZZone();
                    if (zone[1] != null) { 
                        safeZ = (location.getLengthZ().compareTo(zone[1]) < 0) ?
                                location.getLengthZ() : zone[1];
                    }
                }
                safeZ = safeZ.convertToUnits(location.getUnits());
            }
            hm.moveTo(location.derive(null, null, (safeZ != null ? safeZ.getValue() : Double.NaN), null), speed);
        }
        // else: moves only in Z (or not at all).
        hm.moveTo(location, speed);
    }

    public static void moveToLocationAtSafeZ(HeadMountable hm, Location location) throws Exception {
        moveToLocationAtSafeZ(hm, location, hm.getHead().getMachine().getSpeed());
    }
    
    public static void park(Head head) throws Exception {
        head.moveToSafeZ();
        HeadMountable hm = head.getDefaultHeadMountable();
        Location location = head.getParkLocation();
        location = location.derive(null, null, Double.NaN, Double.NaN);
        hm.moveTo(location);
    }

    public static void fireTargetedUserAction(HeadMountable hm, boolean jogging) {
        Machine machine = Configuration.get().getMachine();
        if (machine instanceof AbstractMachine) {
            ((AbstractMachine)machine).fireMachineTargetedUserAction(hm, jogging);
        }
    }

    public static void fireTargetedUserAction(HeadMountable hm) {
        fireTargetedUserAction(hm, false);
    }

    public static boolean isInSafeZZone(HeadMountable hm) {
        try {
            return hm.isInSafeZZone(hm.getLocation().getLengthZ());
        }
        catch (Exception e) {
            Logger.warn(e);
            return false;
        }
    }
}
