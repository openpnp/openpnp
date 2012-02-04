package org.openpnp;

import org.openpnp.model.Configuration;

public interface RequiresConfigurationResolution {
	public void resolve(Configuration configuration) throws Exception;
}
