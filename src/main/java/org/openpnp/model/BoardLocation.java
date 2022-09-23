/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.model;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

/**
 * A container for a Board that gives the board a physical Location relative to its parent. It 
 * also holds a coordinate transformation that is used to convert the board's local coordinates to
 * its parent's coordinates.  In addition, it contains information on where the Board's definition 
 * is stored in the file system.
 */
public class BoardLocation extends PlacementsHolderLocation<BoardLocation> {

    public static final String ID_PREFIX = "Brd";
    
    @Deprecated
    @Attribute(required = false)
    private String boardFile;

    @Deprecated
    @Attribute(required = false)
    private String panelId; 

    @Deprecated
    @Attribute(required = false)
    private Boolean enabled; 

    @Deprecated
    @ElementMap(required = false)
    private Map<String, Boolean> placed = new HashMap<>();

    /**
     * Default constructor
     */
    BoardLocation() {
        setLocation(new Location(LengthUnit.Millimeters));
    }

    /**
     * Constructs a deep copy of the specified BoardLocation
     * @param boardLocation
     */
    public BoardLocation(BoardLocation boardLocation) {
        super(boardLocation);
    }

    /**
     * Constructs a BoardLocation for the specified Board
     * @param board - the specified board
     */
    public BoardLocation(Board board) {
        this();
        setBoard(board);
    }

    /**
     * Called immediately after de-serialization
     */
    @Commit
    protected void commit() {
        super.commit();
        
        //Converted deprecated attributes/elements
        if (boardFile != null) {
            setFileName(boardFile);
            boardFile = null;
        }
        if (enabled != null) {
            setLocallyEnabled(enabled);
            enabled = null;
        }
    }
    
    /**
     * Called just prior to serialization
     */
    @Persist
    protected void persist() {
        placed = null;
    }
    
    /**
     * 
     * @return - the Board associated with this BoardLocation
     */
    public Board getBoard() {
        return (Board) getPlacementsHolder();
    }

    /**
     * Sets the board associated with this BoardLocation
     * @param board - the board to set
     */
    public void setBoard(Board board) {
        setPlacementsHolder(board);
    }

    /**
     * 
     * @return - the filename where the Board associated with this BoardLocation is stored
     */
    String getBoardFile() {
        return getFileName();
    }

    /**
     * Sets the filename where the Board associated with this BoardLocation is stored
     * @param boardFileName - the filename to set
     */
    void setBoardFile(String boardFileName) {
        setFileName(boardFileName);
    }

    /**
     * @deprecated placement status is now saved with the job
     * @return the placement status
     */
    @Deprecated
    public Map<String, Boolean> getPlaced() {
        return placed;
    }
    
    /**
     * 
     * @return - the transform that converts the Board's local coordinates to those of its parent
     */
    public AffineTransform getPlacementTransform() {
        return getLocalToParentTransform();
    }

    /**
     * Sets the transform that converts the Board's local coordinates to those of its parent
     * @param placementTransform - the transform to set
     */
    public void setPlacementTransform(AffineTransform placementTransform) {
        setLocalToParentTransform(placementTransform);
    }

    @Override
    public String toString() {
        return String.format("BoardLocation (%s), location (%s), side (%s)", getFileName(), 
                getLocation(), side);
    }
    
    /**
     * Provides a formated dump to the log file
     * @param leader - prefix to indent the dump 
     */
    public void dump(String leader) {
        PanelLocation parentPanelLocation = getParent();
        int parentHashCode = 0;
        if (parentPanelLocation != null) {
            parentHashCode = parentPanelLocation.hashCode();
        }
        Logger.trace(String.format("%s (%s) BoardLocation:@%08x defined by @%08x child of @%08x, "
                + "%s, location=%s globalLocation=%s, side=%s (%s)", leader,  this.id, 
                this.hashCode(), this.getDefinition().hashCode(), parentHashCode, fileName, 
                getLocation(), getGlobalLocation(), side, 
                getBoard() == null ? "Null" : getBoard().toString()));
    }

}
