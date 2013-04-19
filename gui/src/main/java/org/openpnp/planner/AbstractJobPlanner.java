package org.openpnp.planner;

import org.openpnp.model.Job;
import org.openpnp.spi.JobPlanner;

public abstract class AbstractJobPlanner implements JobPlanner {
    protected Job job;
    
    @Override
    public void setJob(Job job) {
        this.job = job;
    }
}
