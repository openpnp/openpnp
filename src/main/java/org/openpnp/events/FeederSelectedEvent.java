package org.openpnp.events;

import org.openpnp.spi.Feeder;

public class FeederSelectedEvent {
    final public Feeder feeder;
    
    public FeederSelectedEvent(Feeder feeder) {
        this.feeder = feeder;
    }
}
