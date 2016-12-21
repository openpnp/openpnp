package org.openpnp.events;

import org.openpnp.model.BoardLocation;

public class BoardLocationSelectedEvent {
    final public BoardLocation boardLocation;
    
    public BoardLocationSelectedEvent(BoardLocation boardLocation) {
        this.boardLocation = boardLocation;
    }
}
