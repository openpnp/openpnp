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
     * consists of turning on the vacuum. This method call is only used by
     * manual user process. During Job processing the 
     * pick(Feeder) method will be called.
     * @throws Exception
     */
    public void pick() throws Exception;
    
    /**
     * Commands the Nozzle to pick a Part from the specified Feeder. In general,
     * this operation should move the Nozzle to the Feeder's pick location 
     * and turn on the vacuum. Before this operation is called the Feeder has
     * already been commanded to feed the Part.
     * @param feeder
     * @throws Exception
     */
    public void pick(Feeder feeder) throws Exception;
    
    /**
     * Commands the Nozzle to perform it's place operation. Generally this just consists
     * of releasing vacuum and may include a puff of air to set the Part. This method
     * is only used by manual user process. During Job processing the place(Part, Location)
     * method will be used. 
     * @throws Exception
     */
    public void place() throws Exception;
    
    /**
     * Commands the Nozzle to place the given Part at the specified Location. In general,
     * this operation should move the nozzle to the specified Location and turn off the
     * vacuum.
     * @param placeLocation
     * @throws Exception
     */
    public void place(Location placeLocation) throws Exception;
}
