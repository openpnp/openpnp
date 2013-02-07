package org.openpnp.spi;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Location;

/**
 * A Nozzle is a tool capable of picking up parts and releasing them. It is
 * attached to a Head and may move entirely with the head or partially
 * independent of it. A Nozzle has a current NozzleTip that defines what
 * types of Packages it can handle and it may have the capability of changing
 * it's NozzleTip.
 */
public interface Nozzle extends Identifiable, HeadMountable, WizardConfigurable {
    /**
     * Get the NozzleTip currently attached to the Nozzle.
     * @return
     */
    NozzleTip getNozzleTip();
    
    /**
     * Returns true if the Nozzle is capable of picking from the specified
     * Feeder and placing the picked Part at the specified placeLocation.
     * @param feeder
     * @param placeLocation
     * @return
     */
    public boolean canPickAndPlace(Feeder feeder, Location placeLocation);
    
    /**
     * Commands the Nozzle to perform it's pick operation. Generally this just
     * consists of turning on the vacuum.
     * @throws Exception
     */
    public void pick() throws Exception;
    
    /**
     * Commands the Nozzle to perform it's place operation. Generally this just consists
     * of releasing vacuum and may include a puff of air to set the Part.
     * @throws Exception
     */
    public void place() throws Exception;
}
