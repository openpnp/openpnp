package org.openpnp.machine.generic;

import org.openpnp.Configuration;
import org.openpnp.Job;
import org.openpnp.Part;
import org.w3c.dom.Node;

/**
 * Defines the interface for a simple driver that the GenericMachine can drive.
 * All methods result in machine operations and all methods should block until they are complete
 * or throw an error.
 */
public interface GenericDriver {
	public void configure(Node n) throws Exception;
	
	public void prepareJob(Configuration configuration, Job job) throws Exception;
	
	/**
	 * Performing the homing operation on the machine. When this call completes the machine
	 * should be at 0,0,0,0. 
	 * @throws Exception
	 */
	public void home(GenericHead head) throws Exception;
	
	/**
	 * Move the nozzle to the specified location and rotation at full speed.
	 * @param x
	 * @param y
	 * @param z
	 * @param a
	 * @throws Exception
	 */
	public void moveTo(GenericHead head, double x, double y, double z, double a) throws Exception;
	
	/**
	 * Causes the nozzle to apply vacuum and any other operation that it uses for picking
	 * up a part that it is resting on.
	 * @param Part Allows drivers to make accomodations for certain types of Parts.
	 * @throws Exception
	 */
	public void pick(GenericHead head, Part part) throws Exception;
	
	/**
	 * Causes the nozzle to release vacuum and any other operation that it uses for
	 * placing a part that it is currently holding. For instance, it might provide
	 * a brief puff of air to set the part.
	 * @throws Exception
	 */
	public void place(GenericHead head) throws Exception;
	
	/**
	 * Actuates a machine defined object with a boolean state. As an example, in the 
	 * Prototype machine actuating 0, true will extend the index pin to be used for
	 * indexing feeders.
	 * @param tool
	 * @param on
	 * @throws Exception
	 */
	public void actuate(GenericHead head, int index, boolean on) throws Exception;
}
