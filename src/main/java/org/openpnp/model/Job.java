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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openpnp.util.IdentifiableList;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;

/**
 * A Job specifies a list of one or more BoardLocations.
 */
@Root(name = "openpnp-job")
public class Job extends AbstractModelObject implements PropertyChangeListener {	
    @ElementList(required = false)
    protected IdentifiableList<Panel> pcbPanels = new IdentifiableList<>();
	
    @ElementList
    private ArrayList<BoardLocation> boardLocations = new ArrayList<>();

    private transient File file;
    private transient boolean dirty;

    public Job() {
        addPropertyChangeListener(this);
    }

    @SuppressWarnings("unused")
    @Commit
    private void commit() {
        for (BoardLocation boardLocation : boardLocations) {
            boardLocation.addPropertyChangeListener(this);
        }
    }

    public List<BoardLocation> getBoardLocations() {
        return Collections.unmodifiableList(boardLocations);
    }

    public void addBoardLocation(BoardLocation boardLocation) {
        Object oldValue = boardLocations;
        boardLocations = new ArrayList<>(boardLocations);
        boardLocations.add(boardLocation);
        firePropertyChange("boardLocations", oldValue, boardLocations);
        boardLocation.addPropertyChangeListener(this);
    }

    public void removeBoardLocation(BoardLocation boardLocation) {
        Object oldValue = boardLocations;
        boardLocations = new ArrayList<>(boardLocations);
        boardLocations.remove(boardLocation);
        firePropertyChange("boardLocations", oldValue, boardLocations);
        boardLocation.removePropertyChangeListener(this);
    }
    
    public void removeAllBoards()
    {
    	ArrayList<BoardLocation> oldValue = boardLocations;
        boardLocations = new ArrayList<>();
        
        firePropertyChange("boardLocations", (Object)oldValue, boardLocations);
        
    	for (int i=0; i<oldValue.size(); i++)
    		oldValue.get(i).removePropertyChangeListener(this);
    }
    
    public void addPcbPanel(Panel panel){
    	pcbPanels.add(panel);
    }
    
    public void clearPcbPanels(){
    	pcbPanels.clear();
    }
    
    public IdentifiableList<Panel> getPcbPanels(){
    	return pcbPanels;
    }
    
    // In the first release of the Auto Panelize software, there is assumed to be a 
    // single panel in use, even though the underlying plumbing supports a list of 
    // panels. This function is intended to let the rest of OpenPNP know if the 
    // autopanelize function is being used
    public boolean isUsingPanel(){
    	if (pcbPanels == null ){
    		return false;
    	}
    	
    	if ( (pcbPanels.size() >= 1) && ((pcbPanels.get(0).getRows() > 1) || (pcbPanels.get(0).getColumns() > 1))){
    		return true;
    	}
    	
    	return false;
    }
    
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        Object oldValue = this.file;
        this.file = file;
        firePropertyChange("file", oldValue, file);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        boolean oldValue = this.dirty;
        this.dirty = dirty;
        firePropertyChange("dirty", oldValue, dirty);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() != Job.this || !evt.getPropertyName().equals("dirty")) {
            setDirty(true);
        }
    }
}
