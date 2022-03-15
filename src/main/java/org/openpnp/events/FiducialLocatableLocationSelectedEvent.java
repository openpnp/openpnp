package org.openpnp.events;

import org.openpnp.model.FiducialLocatableLocation;

public class FiducialLocatableLocationSelectedEvent {
    final public FiducialLocatableLocation fiducialLocatableLocation;
    final public Object source;
    
    public FiducialLocatableLocationSelectedEvent(FiducialLocatableLocation fiducialLocatableLocation) {
        this(fiducialLocatableLocation, null);
    }
    
    public FiducialLocatableLocationSelectedEvent(FiducialLocatableLocation fiducialLocatableLocation, Object source) {
        this.fiducialLocatableLocation = fiducialLocatableLocation;
        this.source = source;
    }
}
