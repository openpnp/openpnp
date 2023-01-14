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

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Version;
import org.simpleframework.xml.core.Commit;

/**
 * A Board describes the physical properties of a PCB and has a list of Placements that will be used
 * to specify pick and place operations.
 */
@Root(name = "openpnp-board")
public class Board extends PlacementsHolder<Board> implements PropertyChangeListener {

    @Version(revision=1.1)
    private double version;    

    /**
     * @deprecated Fiducials are now held in the placements List inherited from 
     * {@link PlacementsHolder}
     */
    @Deprecated
    @ElementList(required = false)
    private ArrayList<Fiducial> fiducials = new ArrayList<>();

    @ElementList(required = false)
    private ArrayList<BoardPad> solderPastePads = new ArrayList<>();

    @Commit
    protected void commit() {
        super.commit();
        for (BoardPad pad : solderPastePads) {
            pad.addPropertyChangeListener(this);
        }
    }

    /**
     * Default constructor
     */
    public Board() {
        super();
        setFile(null);
    }

    /**
     * Constructs a new board and sets its file
     * @param file - the file in which this board will be stored
     */
    public Board(File file) {
        super();
        setFile(file);
    }
    
    /**
     * Constructs a deep copy of the specified Board
     * @param boardToCopy
     */
    public Board(Board boardToCopy) {
        super(boardToCopy);
        this.solderPastePads = new ArrayList<>();
        for (BoardPad pad : boardToCopy.solderPastePads) {
            BoardPad newPad = new BoardPad(pad);
            newPad.addPropertyChangeListener(this);
            this.solderPastePads.add(newPad);
        }
    }

    /**
     * Cleans-up (removes all property change listeners) from this Board
     */
    @Override
    public void dispose() {
        for (BoardPad pad : solderPastePads) {
            pad.removePropertyChangeListener(this);
            pad.dispose();
        }
        super.dispose();
    }
    
    //Note that when/if solder paste dispensing is added back to OpenPnp, the following three 
    //methods and the field solderPastePads should be reviewed.  BoardPad should probably be changed
    //to extent AbstractLocatable and be handled similarly to how placements are handled in 
    //PlacementsHolder.  On second thought, it might make more sense to move all this to the 
    //footprint associated with each package. And then change the enabled field of Placement to 
    //be an enumeration something like: Place Only, Paste Only, Paste and Place, and Disabled.
    public List<BoardPad> getSolderPastePads() {
        return Collections.unmodifiableList(solderPastePads);
    }

    public void addSolderPastePad(BoardPad pad) {
        Object oldValue = solderPastePads;
        solderPastePads = new ArrayList<>(solderPastePads);
        solderPastePads.add(pad);
        firePropertyChange("solderPastePads", oldValue, solderPastePads);
        if (pad != null) {
            pad.addPropertyChangeListener(this);
        }
    }

    public void removeSolderPastePad(BoardPad pad) {
        Object oldValue = solderPastePads;
        solderPastePads = new ArrayList<>(solderPastePads);
        solderPastePads.remove(pad);
        firePropertyChange("solderPastePads", oldValue, solderPastePads);
        if (pad != null) {
            pad.removePropertyChangeListener(this);
            pad.dispose();
        }
    }

    @Override
    public String toString() {
        return String.format("Board @%08x defined by @%08x: file %s, dims: %sx%s, placements: %d @%08x (@%08x)", hashCode(), definition.hashCode(), file, dimensions.getLengthX(), dimensions.getLengthY(), placements.size(), placements.hashCode(), definition.placements.hashCode());
    }
}
