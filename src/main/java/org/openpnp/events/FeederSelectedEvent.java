package org.openpnp.events;

import org.openpnp.spi.Feeder;

public class FeederSelectedEvent {
    final public Feeder feeder;
    final public Object source;
    
    public FeederSelectedEvent(Feeder feeder) {
        this(feeder, null);
    }
    
    public FeederSelectedEvent(Feeder feeder, Object source) {
        this.feeder = feeder;
        this.source = source;
    }
}
