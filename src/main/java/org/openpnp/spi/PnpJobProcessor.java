package org.openpnp.spi;

import org.openpnp.model.Job;

public interface PnpJobProcessor {
    void initialize(Job job) throws Exception;
    
    public boolean next() throws Exception;
    
    public void abort() throws Exception;
}
