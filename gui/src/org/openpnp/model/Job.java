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

package org.openpnp.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 * A Job specifies a list of one or more BoardLocations. 
 */
@Root(name="openpnp-job")
public class Job extends AbstractModelObject {
	@ElementList
	private ArrayList<BoardLocation> boardLocations = new ArrayList<BoardLocation>();
	
	private transient File file;
	
	public List<BoardLocation> getBoardLocations() {
		return Collections.unmodifiableList(boardLocations);
	}

	public void addBoardLocation(BoardLocation boardLocation) {
		Object oldValue = boardLocations;
		boardLocations = new ArrayList<BoardLocation>(boardLocations);
		boardLocations.add(boardLocation);
		firePropertyChange("boardLocations", oldValue, boardLocations);
	}
	
	public void removeBoardLocation(BoardLocation boardLocation) {
		Object oldValue = boardLocations;
		boardLocations = new ArrayList<BoardLocation>(boardLocations);
		boardLocations.remove(boardLocation);
		firePropertyChange("boardLocations", oldValue, boardLocations);
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		Object oldValue = this.file;
		this.file = file;
		firePropertyChange("file", oldValue, file);
	}
}
