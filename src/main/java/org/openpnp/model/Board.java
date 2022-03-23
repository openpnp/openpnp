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

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Version;
import org.simpleframework.xml.core.Commit;

/**
 * A Board describes the physical properties of a PCB and has a list of Placements that will be used
 * to specify pick and place operations.
 */
@Root(name = "openpnp-board")
public class Board extends FiducialLocatable implements PropertyChangeListener {
    public enum Side {
        Bottom, Top;
        
        public Side flip(boolean value) {
            if (value) {
                return flip();
            }
            else {
                return this;
            }
        }
        public Side flip() {
            if (this.equals(Side.Top)) {
                return Side.Bottom;
            }
            else {
                return Side.Top;
            }
        }
    }

    @Version(revision=1.1)
    private double version;    

    @ElementList(required = false)
    private ArrayList<Fiducial> fiducials = new ArrayList<>();

    @ElementList(required = false)
    private ArrayList<BoardPad> solderPastePads = new ArrayList<>();

    public Board() {
        setFile(null);
        addPropertyChangeListener(this);
    }

    public Board(File file) {
        setFile(file);
        addPropertyChangeListener(this);
    }
    
    public Board(Board board) {
        super(board);
        this.fiducials = new ArrayList<>(board.fiducials);
        this.solderPastePads = new ArrayList<>(board.solderPastePads);
        addPropertyChangeListener(this);
    }

    @Commit
    private void commit() {
        for (Placement placement : placements) {
            placement.addPropertyChangeListener(this);
        }
        for (BoardPad pad : solderPastePads) {
            pad.addPropertyChangeListener(this);
        }
    }

    public List<Fiducial> getFiducials() {
        return Collections.unmodifiableList(fiducials);
    }

    public void addFiducial(Fiducial fiducial) {
        ArrayList<Fiducial> oldValue = fiducials;
        fiducials = new ArrayList<>(fiducials);
        fiducials.add(fiducial);
        firePropertyChange("fiducials", oldValue, fiducials);
    }

    public void removeFiducial(Fiducial fiducial) {
        ArrayList<Fiducial> oldValue = fiducials;
        fiducials = new ArrayList<>(fiducials);
        fiducials.remove(fiducial);
        firePropertyChange("fiducials", oldValue, fiducials);
    }

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
        }
    }

    @Override
    public String toString() {
        return String.format("Board: file %s, dims: %sx%s, placements: %d", file, dimensions.getLengthX(), dimensions.getLengthY(), placements.size());
    }

}
