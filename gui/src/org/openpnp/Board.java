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
 */


package org.openpnp;

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * A Board describes the physical properties of a PCB and has a list of 
 * Placements that will be used to specify pick and place operations. 
 */
@Root(name="openpnp-board")
public class Board implements RequiresConfigurationResolution {
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
	
	@Override
	public void resolve(Configuration configuration) throws Exception {
		for (Placement placement : placements) {
			placement.resolve(configuration);
		}
	}
	
	public List<Fiducial> getFiducials() {
		return fiducials;
	}
	
	public List<Placement> getPlacements() {
		return placements;
	}
	
	public Outline getOutline() {
		return outline;
	}

	public void setOutline(Outline outline) {
		this.outline = outline;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setFiducials(ArrayList<Fiducial> fiducials) {
		this.fiducials = fiducials;
	}

	public void setPlacements(ArrayList<Placement> placements) {
		this.placements = placements;
	}
}
