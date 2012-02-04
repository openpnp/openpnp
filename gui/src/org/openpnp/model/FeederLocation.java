package org.openpnp.model;

import org.openpnp.RequiresConfigurationResolution;
import org.openpnp.spi.Feeder;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Persist;

public class FeederLocation implements RequiresConfigurationResolution {
	private Feeder feeder;
	@Element
	private Location location;
	
	@Attribute
	private String feederId;

	@Override
	public void resolve(Configuration configuration) throws Exception {
		feeder = configuration.getMachine().getFeeder(feederId);
	}
	
	@SuppressWarnings("unused")
	@Persist
	private void persist() {
		feederId = (feeder == null ? null : feeder.getId());
	}
	
	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}
	
	public Feeder getFeeder() {
		return feeder;
	}

	public void setFeeder(Feeder feeder) {
		this.feeder = feeder;
	}

	@Override
	public String toString() {
		return String.format("feederId (%s), location (%s)", feederId, location);
	}
}