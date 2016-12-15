package org.openpnp.events;

import org.openpnp.model.BoardLocation;
import org.openpnp.model.Placement;

public class PlacementSelectedEvent {
    final public Placement placement;
    final public BoardLocation boardLocation;
    
    public PlacementSelectedEvent(Placement placement, BoardLocation boardLocation) {
        this.placement = placement;
        this.boardLocation = boardLocation;
    }
    
    public PlacementSelectedEvent(Placement placement) {
        this(placement, null);
    }
}
