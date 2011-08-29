package org.openpnp.machine.generic;

import org.openpnp.Location;
import org.openpnp.Part;
import org.openpnp.spi.Feeder;
import org.w3c.dom.Node;

/**
 * A common base class for Feeders that the generic machine supports.
 * Provides support for additional configuration and a chance for the Feeder to have it's say
 * during part pickup. 
 */
public abstract class GenericFeeder implements Feeder {
	String reference;
	
	public abstract void configure(Node n) throws Exception;

	@Override
	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}
}
