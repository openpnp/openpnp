package org.openpnp.spi;

import java.util.List;

import org.openpnp.machine.reference.ReferencePnpJobProcessor.JobPlacement;

public interface PnpJobProcessor extends JobProcessor {

    public List<JobPlacement> getJobPlacementsById(String id);

    public List<JobPlacement> getJobPlacementsById(String id, String status);

}
