package org.openpnp.events;

import org.openpnp.model.FiducialLocatableLocation;
import org.openpnp.model.Placement;

public class PlacementSelectedEvent {
    final public Placement placement;
    final public FiducialLocatableLocation fiducialLocatableLocation;
    final public Object source;
    
    public PlacementSelectedEvent(Placement placement, FiducialLocatableLocation fiducialLocatableLocation, Object source) {
        this.placement = placement;
        this.fiducialLocatableLocation = fiducialLocatableLocation;
        this.source = source;
    }
    
    public PlacementSelectedEvent(Placement placement, FiducialLocatableLocation fiducialLocatableLocation) {
        this(placement, fiducialLocatableLocation, null);
    }
    
    public PlacementSelectedEvent(Placement placement) {
        this(placement, null);
    }
    
    
}
