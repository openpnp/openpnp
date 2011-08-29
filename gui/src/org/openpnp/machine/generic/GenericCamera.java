package org.openpnp.machine.generic;

import org.openpnp.Configuration;
import org.openpnp.Job;
import org.openpnp.spi.Camera;

public interface GenericCamera extends Camera {
	public void prepareJob(Configuration configuration, Job job) throws Exception;
}
