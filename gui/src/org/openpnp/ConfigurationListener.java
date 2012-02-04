package org.openpnp;

import org.openpnp.model.Configuration;

public interface ConfigurationListener {
	public void configurationLoaded(Configuration configuration);
	
	static public class Adapter implements ConfigurationListener {
		@Override
		public void configurationLoaded(Configuration configuration) {
		}
	}
}
