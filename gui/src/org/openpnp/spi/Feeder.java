package org.openpnp.spi;

import org.openpnp.Location;
import org.openpnp.Part;
import org.openpnp.machine.generic.GenericHead;



/**
 * A Feeder is an abstraction that represents any type of part source. 
 * It can be a tape and reel feeder, a tray handler, a single part in a 
 * specific location or anything else that can be used as a pick source.
 * 
 * When a Feeder is first configured as part of machine setup it is not
 * given a Part to handle. This is because in the case of something like a
 * reel the user may swap different Parts into the same Feeder without
 * needing to reconfigure the Feeder.
 * 
 * For instance, if the Feeder is setup to feed 5mm cut tape, the user
 * may run a 0805 resistor in the Feeder for one job and a 0805 capacitor
 * in the same Feeder for a different job.
 */
public interface Feeder {
	/**
	 * Gets the reference that was set when the Feeder was initialized.
	 * @return
	 */
	String getReference();
	
	/**
	 * Returns true if the Feeder is ready and willing to source the Part.
	 * @return
	 */
	boolean available(); 
	
	
	/**
	 * Allows the Feeder to do anything it needs to to prepare the part to be picked. If the Feeder requires the
	 * pick location to be modified it can return a new Location, otherwise it should just return the original
	 * passed in Location. The incoming pickLocation should not be modified.
	 * @param head
	 * @param part
	 * @param pickLocation
	 * @return
	 * @throws Exception
	 */
	public Location feed(Head head, Part part, Location pickLocation) throws Exception;
}
