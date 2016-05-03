package org.openpnp.spi;

import org.openpnp.model.Job;

public interface JobProcessor {
    void initialize(Job job) throws Exception;
    
    public boolean next() throws Exception;
    
    // TODO STOPSHIP: Might need nextCycle or something that does a complete cycle where next()
    // just does one step. Or expose enough API that the caller can tell when a cycle is complete.
    // 
    // Actually, maybe next should perform a complete cycle by default, since that's actually
    // what we want. It would just do while(state != Plan) and if it happened to throw
    // an exception that's okay cause the FSM handles it.
    // Could even add a step flag for debugging that actually does individual steps.
    
    public void abort() throws Exception;
}
