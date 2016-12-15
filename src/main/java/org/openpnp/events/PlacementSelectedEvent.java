package org.openpnp.events;

import org.openpnp.model.BoardLocation;
import org.openpnp.model.Placement;

public class PlacementSelectedEvent {
    final public BoardLocation boardLocation;
    final public Placement placement;
    
    public PlacementSelectedEvent(BoardLocation boardLocation, Placement placement) {
        this.boardLocation = boardLocation;
        this.placement = placement;
    }
    
    public PlacementSelectedEvent(Placement placement) {
        this(null, placement);
    }
}
