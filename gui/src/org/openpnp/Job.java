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

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * A Job specifies a list of one or more Boards to populate along with their locations on the table. 
 */
@Root(name="openpnp-job")
public class Job {
	@ElementList
	private ArrayList<BoardLocation> boardLocations = new ArrayList<BoardLocation>();
	
	private boolean dirty;
	
	public ArrayList<BoardLocation> getBoardLocations() {
		return boardLocations;
	}

	public void setBoardLocations(ArrayList<BoardLocation> boardLocations) {
		this.boardLocations = boardLocations;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
}
