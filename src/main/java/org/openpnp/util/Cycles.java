package org.openpnp.util;

import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;

public class Cycles {
    public static Location zProbe(Location l) throws Exception {
        Actuator zProbe = Configuration.get().getMachine().getDefaultHead().getZProbe();
        if (zProbe == null) {
            return null;
        }
        Location lOld = zProbe.getLocation();
        Location lz = zProbe.getLocation();
        MovableUtils.moveToLocationAtSafeZ(zProbe, l);
        double z = Double.parseDouble(zProbe.read());
        lz = lz.add(new Location(LengthUnit.Millimeters, 0, 0, z, 0));
        MovableUtils.moveToLocationAtSafeZ(zProbe, lOld);
        l = l.derive(null, 
                null, 
                lz.getLengthZ().convertToUnits(l.getUnits()).getValue(), 
                null);
        return l;
    }
}
