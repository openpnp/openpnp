package org.openpnp.events;

import org.openpnp.model.Job;

public class JobLoadedEvent {
    final public Job job;
    
    public JobLoadedEvent(Job job) {
        this.job = job;
    }
}