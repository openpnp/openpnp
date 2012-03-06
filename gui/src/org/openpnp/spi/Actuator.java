package org.openpnp.spi;

/**
 * Defines a simple interface to some type of device that can be actuated
 * on the machine or on a head. This is a minimal interface and it is
 * expected that concrete implementations may have many other capabilities
 * exposed in their specific implementations. 
 */
public interface Actuator {
	public String getId();
	
	public void actuate(boolean on) throws Exception;
}
