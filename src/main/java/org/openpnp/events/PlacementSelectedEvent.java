package org.openpnp.events;

import org.openpnp.model.Placement;
import org.openpnp.model.PlacementsHolderLocation;

public class PlacementSelectedEvent {
    final public Placement placement;
    final public PlacementsHolderLocation<?> placementsHolderLocation;
    final public Object source;
    
    public PlacementSelectedEvent(Placement placement, PlacementsHolderLocation<?> placementsHolderLocation, Object source) {
        this.placement = placement;
        this.placementsHolderLocation = placementsHolderLocation;
        this.source = source;
    }
    
    public PlacementSelectedEvent(Placement placement, PlacementsHolderLocation<?> placementsHolderLocation) {
        this(placement, placementsHolderLocation, null);
    }
    
    public PlacementSelectedEvent(Placement placement) {
        this(placement, null);
    }
    
    
}
