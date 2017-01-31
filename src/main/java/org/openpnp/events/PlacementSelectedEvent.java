package org.openpnp.events;

import org.openpnp.model.BoardLocation;
import org.openpnp.model.Placement;

public class PlacementSelectedEvent {
    final public Placement placement;
    final public BoardLocation boardLocation;
    final public Object source;
    
    public PlacementSelectedEvent(Placement placement, BoardLocation boardLocation, Object source) {
        this.placement = placement;
        this.boardLocation = boardLocation;
        this.source = source;
    }
    
    public PlacementSelectedEvent(Placement placement, BoardLocation boardLocation) {
        this(placement, boardLocation, null);
    }
    
    public PlacementSelectedEvent(Placement placement) {
        this(placement, null);
    }
    
    
}
