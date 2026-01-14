package org.openpnp.gui.support;

import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Nozzle;

public class FeederUtils {

    /**
     * Calculates the preliminary pick location, adjusting for part height if
     * necessary.
     * 
     * @param feeder The feeder to pick from.
     * @param nozzle The nozzle to use (can be null).
     * @return The calculated pick location.
     * @throws Exception
     */
    public static Location preliminaryPickLocation(Feeder feeder, Nozzle nozzle) throws Exception {
        Location pickLocation = feeder.getPickLocation();
        if (nozzle != null && feeder.isPartHeightAbovePickLocation()) {
            Length partHeight = nozzle.getSafePartHeight(feeder.getPart());
            pickLocation = pickLocation.add(new Location(partHeight.getUnits(), 0, 0, partHeight.getValue(), 0));
        }
        return pickLocation;
    }
}
