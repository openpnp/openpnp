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
	
	BoardLocation() {
		setLocation(new Location());
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