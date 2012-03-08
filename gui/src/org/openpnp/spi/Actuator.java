package org.openpnp.spi;

import org.openpnp.gui.support.Wizard;

/**
 * Defines a simple interface to some type of device that can be actuated
 * on the machine or on a head. This is a minimal interface and it is
 * expected that concrete implementations may have many other capabilities
 * exposed in their specific implementations. 
 */
public interface Actuator {
	/**
	 * The unique id of the Actuator. This valid should be unique for a given
	 * head if the Actuator is attached to a head, or unique within machine
	 * Actuators.
	 * @return
	 */
	public String getId();
	
	/**
	 * Turns the Actuator on or off.
	 * @param on
	 * @throws Exception
	 */
	public void actuate(boolean on) throws Exception;
	
	/**
	 * Gets a Wizard that can be used to confgure this Actuator.
	 * @return
	 */
	public Wizard getConfigurationWizard();
}
