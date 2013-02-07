package org.openpnp.planner;

import org.openpnp.model.JobSession;
import org.openpnp.spi.JobPlanner;

public abstract class AbstractJobPlanner implements JobPlanner {
    protected JobSession jobSession;
    
    @Override
    public void setJob(JobSession jobSession) {
        this.jobSession = jobSession;
    }
}
