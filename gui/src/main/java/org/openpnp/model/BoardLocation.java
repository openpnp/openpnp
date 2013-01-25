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
 * 03/10/2012 Ami: Add center point field.
 * Rotating from (0,0) as it was theoretically should work the same BUT,
 * in reality when adjusting with camera, there is always error,
 * the further the part to (0,0) the bigger the error.
 * By setting rotation center to the center of the board, error is shared equally
 * by all parts.
*/

package org.openpnp.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.openpnp.model.Board.Side;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;

public class BoardLocation extends AbstractModelObject implements PropertyChangeListener {
	@Element
	private Location location;


	@Attribute
	private Side side = Side.Top;
	private Board board;
	
	@Attribute
	private String boardFile;
	
	@Element
	private Point center = new Point(0,0);	// Ami: this is the center point used for rotation

	BoardLocation() {
		setLocation(new Location(LengthUnit.Millimeters));
	}
	
	public BoardLocation(Board board) {
		this();
		setBoard(board);
	}
	
	@Commit
	private void commit() {
		setLocation(location);
		setBoard(board);
	}
	
	public Location getLocation() {
		return location;
	}
	public Point getCenter() {
		return center;
	}
	public void setLocation(Location location) {
		Location oldValue = this.location;
		this.location = location;
		firePropertyChange("location", oldValue, location);
		if (oldValue != null) {
			oldValue.removePropertyChangeListener(this);
		}
		if (location != null) {
			location.addPropertyChangeListener(this);
		}
	}
	public void setCenter(Point p) {

		this.center = p;

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

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		propertyChangeSupport.firePropertyChange(evt);
	}

	@Override
	public String toString() {
		return String.format("board (%s), location (%s), side (%s)", boardFile, location, side);
	}
}