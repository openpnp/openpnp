package org.openpnp.spi;

import org.openpnp.model.Job;

public interface JobProcessor extends PropertySheetHolder {
    void initialize(Job job) throws Exception;
    
    public boolean next() throws Exception;
    
    public void abort() throws Exception;
    
    public void skip() throws Exception;
    
    public boolean canSkip();
}
