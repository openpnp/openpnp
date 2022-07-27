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
import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.Map;

import org.openpnp.gui.MainFrame;
import org.openpnp.model.Placement.Type;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

public class BoardLocation extends FiducialLocatableLocation {

    @Deprecated
    @Attribute(required = false)
    private String boardFile;

    @Deprecated
    @Attribute(required = false)
    private String panelId; 

    @Deprecated
    @Attribute(required = false)
    private Boolean enabled; 

    BoardLocation() {
        setLocation(new Location(LengthUnit.Millimeters));
    }

    // Copy constructor needed for deep copy of object.
    public BoardLocation(BoardLocation boardLocation) {
        super(boardLocation);
        if (boardLocation.getBoard() != null) {
            setBoard(boardLocation.getBoard());
        }
    }

    public BoardLocation(Board board) {
        this();
        setBoard(board);
    }

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
    
    @Persist
    protected void persist() {
        super.persist();
//        if (MainFrame.get().getJobTab().getJob().getPanelLocations().get(parentId) == null) {
    }
    
//    public int getTotalActivePlacements(){
//    	if (fiducialLocatable == null) {
//    		return 0;
//    	}
//    	int counter = 0;
//    	for(Placement placement : fiducialLocatable.getPlacements()) {
//    		if (placement.getSide() == getSide()
//    		        && placement.getType() == Type.Placement
//    		        && placement.isEnabled()) {
//    				counter++;
//        	}
//    	}
//    	return counter;
//    }
//    
//    public int getActivePlacements() {
//    	if (fiducialLocatable == null) {
//    		return 0;
//    	}
//    	int counter = 0;
//	    for(Placement placement : fiducialLocatable.getPlacements()) {
//            if (placement.getSide() == getSide()
//                    && placement.getType() == Type.Placement
//                    && placement.isEnabled()
//                    && !getPlaced(placement.getId())) {
//                    counter++;
//            }
//        }
//    	return counter;
//    }
//
    public Board getBoard() {
        return (Board) getFiducialLocatable();
    }

    public void setBoard(Board board) {
        setFiducialLocatable(board);
    }

    String getBoardFile() {
        return getFileName();
    }

    void setBoardFile(String boardFile) {
        setFileName(boardFile);
    }

//    public String getPanelId() {
//        return getParentId();
//    }
//
//    public void setPanelId(String id) {
//        setParentId(id);
//    }

//    public void setPlaced(String placementId, boolean placed) {
//        this.placed.put(placementId, placed);
//        firePropertyChange("placed", null, this.placed);
//    }
//
//    public boolean getPlaced(String placementId) {
//        if (placed.containsKey(placementId)) {
//            return placed.get(placementId);
//        } 
//        else {
//            return false;
//        }
//    }
//    
//    public void clearAllPlaced() {
//        this.placed.clear();
//        firePropertyChange("placed", null, this.placed);
//    }
    
    public AffineTransform getPlacementTransform() {
        return getLocalToParentTransform();
    }

    public void setPlacementTransform(AffineTransform placementTransform) {
        setLocalToParentTransform(placementTransform);
    }

    @Override
    public String toString() {
        return String.format("board (%s), location (%s), side (%s)", getFileName(), getLocation(), side);
    }
    
    public void dump(String leader) {
        PanelLocation parentPanelLocation = getParent();
        int parentHashCode = 0;
        if (parentPanelLocation != null) {
            parentHashCode = parentPanelLocation.hashCode();
        }
        Logger.trace(String.format("%sBoardLocation:@%08x defined by @%08x child of @%08x, %s, location=%s globalLocation=%s, side=%s (%s)", leader,  this.hashCode(), this.getDefinedBy().hashCode(), parentHashCode, fileName, getLocation(), getGlobalLocation(), side, getBoard() == null ? "Null" : getBoard().toString()));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Logger.trace(String.format("PropertyChangeEvent handled by BoardLocation @%08x = %s", this.hashCode(), evt));
        super.propertyChange(evt);
    }
}
