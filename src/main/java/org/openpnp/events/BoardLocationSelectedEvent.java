package org.openpnp.events;

import org.openpnp.model.BoardLocation;

public class BoardLocationSelectedEvent {
    final public BoardLocation boardLocation;
    final public Object source;
    
    public BoardLocationSelectedEvent(BoardLocation boardLocation) {
        this(boardLocation, null);
    }
    
    public BoardLocationSelectedEvent(BoardLocation boardLocation, Object source) {
        this.boardLocation = boardLocation;
        this.source = source;
    }
}
