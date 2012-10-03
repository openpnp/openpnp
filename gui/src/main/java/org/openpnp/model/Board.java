/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 *
 * Changelog:
 * 03/10/2012 Ami: Add sort placement function
 */


package org.openpnp.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openpnp.RequiresConfigurationResolution;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;

/**
 * A Board describes the physical properties of a PCB and has a list of 
 * Placements that will be used to specify pick and place operations. 
 */
@Root(name="openpnp-board")
public class Board extends AbstractModelObject implements RequiresConfigurationResolution, PropertyChangeListener {
	public enum Side {
		Bottom,
		Top
	}
	@Attribute
	private String name;
	@Element(required=false)
	private Outline outline;
	@ElementList(required=false)
	private ArrayList<Fiducial> fiducials = new ArrayList<Fiducial>();
	@ElementList
	private ArrayList<Placement> placements = new ArrayList<Placement>();
	
	private transient File file;
	private transient boolean dirty;
	
	public Board() {
		this(null);
	}
	
	public Board(File file) {
		setFile(file);
		addPropertyChangeListener(this);
	}
	
	@Override
	public void resolve(Configuration configuration) throws Exception {
		for (Placement placement : placements) {
			placement.resolve(configuration);
		}
	}
	
	@SuppressWarnings("unused")
	@Commit
	private void commit() {
		for (Placement placement : placements) {
			placement.addPropertyChangeListener(this);
		}
	}
	
	public List<Fiducial> getFiducials() {
		return Collections.unmodifiableList(fiducials);
	}
	
	
	public void addFiducial(Fiducial fiducial) {
		ArrayList<Fiducial> oldValue = fiducials;
		fiducials = new ArrayList<Fiducial>(fiducials);
		fiducials.add(fiducial);
		firePropertyChange("fiducials", oldValue, fiducials);
	}
	
	public void removeFiducial(Fiducial fiducial) {
		ArrayList<Fiducial> oldValue = fiducials;
		fiducials = new ArrayList<Fiducial>(fiducials);
		fiducials.remove(fiducial);
		firePropertyChange("fiducials", oldValue, fiducials);
	}
	
	public List<Placement> getPlacements() {
		return Collections.unmodifiableList(placements);
	}

	// Ami: Add sortPlacements function
	public void sortPlacements(List<String> sortedPlacementIdList) {
		ArrayList<Placement> oldPlacements = placements;
		placements = new ArrayList<Placement>();
		for(String placementId : sortedPlacementIdList)
		{
		    for(Placement placement : oldPlacements)
		    {
			if(placement.getId().equals(placementId))
			    placements.add(placement);
	
		    }
		}
		firePropertyChange("placements", oldPlacements, placements);

	}
	// Ami. end.
	public void addPlacement(Placement placement) {
		Object oldValue = placements;
		placements = new ArrayList<Placement>(placements);
		placements.add(placement);
		firePropertyChange("placements", oldValue, placements);
		if (placement != null) {
			placement.addPropertyChangeListener(this); 
		}
	}
	
	public void removePlacement(Placement placement) {
		Object oldValue = placements;
		placements = new ArrayList<Placement>(placements);
		placements.remove(placement);
		firePropertyChange("placements", oldValue, placements);
		if (placement != null) {
			placement.removePropertyChangeListener(this);
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
