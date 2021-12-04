package org.openpnp.util;

import java.util.HashMap;
import java.util.Map;

import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.Nozzle.RotationMode;

public class Cycles {
    /**
     * Performs a Z Probe canned cycle if a Z Probe actuator name is defined on the default
     * head. If none is defined, null is returned.
     * 
     * The cycle is as follows:
     * 1. Move the Z Probe actuator to the specified location, respecting head offsets.
     * 2. Perform an Actuator.read() and parse the result as a Double in Millimeters.
     * 3. Move back to the location the zProbe was at before the call.
     * 4. Return a Location that matches the passed in location in X, Y, and Rotation with
     *    the Z value set to that of the probe.
     *    
     * Note that currently the Z Probe is expected to return millimeters. This can be made
     * configurable in the future.
     * @param l
     * @return
     * @throws Exception
     */
    public static Location zProbe(Location l) throws Exception {
        Actuator zProbe = Configuration.get().getMachine().getDefaultHead().getZProbe();
        if (zProbe == null) {
            return null;
        }
        Location lOld = zProbe.getLocation();
        Location lz = zProbe.getLocation();
        MovableUtils.moveToLocationAtSafeZ(zProbe, l);
        double z;
        String reading = zProbe.read();
        try {
            z = Double.parseDouble(reading);
        }
        catch (Exception e) {
            throw new Exception("Head "+Configuration.get().getMachine().getDefaultHead().getName()+" Z Probe "+zProbe.getName()
            +" conversion failed ("+reading+")", e);
        }
        lz = lz.add(new Location(LengthUnit.Millimeters, 0, 0, z, 0));
        MovableUtils.moveToLocationAtSafeZ(zProbe, lOld);
        l = l.derive(null, 
                null, 
                lz.getLengthZ().convertToUnits(l.getUnits()).getValue(), 
                null);
        return l;
    }

    /**
     * Discard the Part, if any, on the given Nozzle. The Nozzle is returned to Safe Z at the end of
     * the operation.
     * 
     * @param nozzle
     * @throws Exception
     */
    public static void discard(Nozzle nozzle) throws Exception {
        if (nozzle.getPart() == null) {
            return;
        }

        Map<String, Object> globals = new HashMap<>();
        globals.put("nozzle", nozzle);
        Configuration.get().getScripting().on("Job.BeforeDiscard", globals);

        Location discardLocation = Configuration.get().getMachine().getDiscardLocation();
        if (nozzle.getRotationMode() == RotationMode.LimitedArticulation) {
            // On a limited articulation nozzle, keep the rotation.
            discardLocation = discardLocation.derive(nozzle.getLocation(),
                    false, false, false, true);
        }
        // move to the discard location
        nozzle.moveToPlacementLocation(discardLocation, null);
        // discard the part
        nozzle.place();
        nozzle.moveToSafeZ();

        Configuration.get().getScripting().on("Job.AfterDiscard", globals);
    }
}
