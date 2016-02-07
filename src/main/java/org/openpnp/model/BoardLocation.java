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

import org.openpnp.model.Board.Side;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

public class BoardLocation extends AbstractModelObject {
	@Element
	private Location location;
	@Attribute
	private Side side = Side.Top;
	private Board board;
	
	@Attribute
	private String boardFile;
	
	@Attribute(required=false)
	private boolean checkFiducials;
	
	@Attribute(required=false)
	private boolean enabled = true;
	
	BoardLocation() {
		setLocation(new Location(LengthUnit.Millimeters));
	}
	
	public BoardLocation(Board board) {
		this();
		setBoard(board);
	}
	
	@SuppressWarnings("unused")
    @Commit
	private void commit() {
		setLocation(location);
		setBoard(board);
	}
	
	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		Location oldValue = this.location;
		this.location = location;
		firePropertyChange("location", oldValue, location);
	}

	public Side getSide() {
		return side;
	}

	public void setSide(Side side) {
		Object oldValue = this.side;
		this.side = side;
		firePropertyChange("side", oldValue, side);
	}
	
	public Board getBoard() {
		return board;
	}

	public void setBoard(Board board) {
		Board oldValue = this.board;
		this.board = board;
		firePropertyChange("board", oldValue, board);
	}
	
	String getBoardFile() {
		return boardFile;
	}
	
	void setBoardFile(String boardFile) {
		this.boardFile = boardFile;
	}
	
	public boolean isCheckFiducials() {
        return checkFiducials;
    }

    public void setCheckFiducials(boolean checkFiducials) {
        boolean oldValue = this.checkFiducials;
        this.checkFiducials = checkFiducials;
        firePropertyChange("checkFiducials", oldValue, checkFiducials);
    }
    
    public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
        boolean oldValue = this.enabled;
        this.enabled = enabled;
        firePropertyChange("enabled", oldValue, enabled);
	}

	@Override
	public String toString() {
		return String.format("board (%s), location (%s), side (%s)", boardFile, location, side);
	}
}
