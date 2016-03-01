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
import org.simpleframework.xml.core.Commit;

/**
 * A Board describes the physical properties of a PCB and has a list of Placements that will be used
 * to specify pick and place operations.
 */
@Root(name = "openpnp-board")
public class Board extends AbstractModelObject implements PropertyChangeListener {
    public enum Side {
        Bottom, Top
    }

    @Attribute
    private String name;

    @Element(required = false)
    private Outline outline;

    @Element(required = false)
    private Location dimensions = new Location(LengthUnit.Millimeters);

    @ElementList(required = false)
    private ArrayList<Fiducial> fiducials = new ArrayList<>();

    @ElementList
    private ArrayList<Placement> placements = new ArrayList<>();

    @ElementList(required = false)
    private ArrayList<BoardPad> solderPastePads = new ArrayList<>();

    private transient File file;
    private transient boolean dirty;

    public Board() {
        this(null);
    }

    public Board(File file) {
        setFile(file);
        setOutline(new Outline());
        addPropertyChangeListener(this);
    }

    @SuppressWarnings("unused")
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

    public Location getDimensions() {
        return dimensions;
    }

    public void setDimensions(Location location) {
        Location oldValue = this.dimensions;
        this.dimensions = location;
        firePropertyChange("dimensions", oldValue, location);
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

    public List<Placement> getPlacements() {
        return Collections.unmodifiableList(placements);
    }

    public void addPlacement(Placement placement) {
        Object oldValue = placements;
        placements = new ArrayList<>(placements);
        placements.add(placement);
        firePropertyChange("placements", oldValue, placements);
        if (placement != null) {
            placement.addPropertyChangeListener(this);
        }
    }

    public void removePlacement(Placement placement) {
        Object oldValue = placements;
        placements = new ArrayList<>(placements);
        placements.remove(placement);
        firePropertyChange("placements", oldValue, placements);
        if (placement != null) {
            placement.removePropertyChangeListener(this);
        }
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


    public Outline getOutline() {
        return outline;
    }

    public void setOutline(Outline outline) {
        Outline oldValue = this.outline;
        this.outline = outline;
        firePropertyChange("outline", oldValue, outline);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        Object oldValue = this.name;
        this.name = name;
        firePropertyChange("name", oldValue, name);
    }

    public File getFile() {
        return file;
    }

    void setFile(File file) {
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
        if (evt.getSource() != Board.this || !evt.getPropertyName().equals("dirty")) {
            setDirty(true);
        }
    }
}
