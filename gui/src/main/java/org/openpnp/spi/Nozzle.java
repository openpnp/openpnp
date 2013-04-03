package org.openpnp.spi;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Location;
import org.openpnp.model.Part;

/**
 * A Nozzle is a tool capable of picking up parts and releasing them. It is
 * attached to a Head and may move entirely with the head or partially
 * independent of it. A Nozzle has a current NozzleTip that defines what
 * types of Packages it can handle and it may have the capability of changing
 * it's NozzleTip.
 */
public interface Nozzle extends Identifiable {
    /**
     * Get the NozzleTip currently attached to the Nozzle.
     * @return
     */
    NozzleTip getNozzleTip();
    
    /**
     * Queries the Head to determine if it has the ability to pick from the
     * given Feeder at the given Location and then move the Part to the
     * destination Location.
     * 
     * @param feeder
     * @param pickLocation
     * @param placeLocation
     * @return
     */
    public boolean canPickAndPlace(Feeder feeder, Location pickLocation,
            Location placeLocation);

    /**
     * Commands the Head to perform it's pick operation. Generally this just
     * consists of turning on the vacuum. This method call is only used by
     * manual user process. During Job processing the pick(Part, Feeder,
     * Location) method will be called.
     * 
     * @throws Exception
     */
    public void pick() throws Exception;

    /**
     * Commands the Head to pick the Part from the Feeder using the given
     * Location. In general, this operation should move the nozzle to the
     * specified Location and turn on the vacuum. Before this operation is
     * called the Feeder has already been commanded to feed the Part.
     * 
     * @param part
     * @param feeder
     * @param pickLocation
     * @throws Exception
     */
    public void pick(Part part, Feeder feeder, Location pickLocation)
            throws Exception;

    /**
     * Commands the Head to perform it's place operation. Generally this just
     * consists of releasing vacuum and may include a puff of air to set the
     * Part. This method is only used by manual user process. During Job
     * processing the place(Part, Location) method will be used.
     * 
     * @throws Exception
     */
    public void place() throws Exception;

    /**
     * Commands the Head to place the given Part at the specified Location. In
     * general, this operation should move the nozzle to the specified Location
     * and turn off the vacuum.
     * 
     * @param part
     * @param placeLocation
     * @throws Exception
     */
    public void place(Part part, Location placeLocation) throws Exception;
}
