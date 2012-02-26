package org.openpnp.model;

import org.openpnp.model.Board.Side;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class BoardLocation extends AbstractModelObject {
	@Element
	private Location location = new Location();
	@Attribute
	private Side side = Side.Top;
	private Board board;
	
	@Attribute
	private String boardFile;
	
	BoardLocation() {
		
	}
	
	public BoardLocation(Board board) {
		setBoard(board);
	}
	
	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		Object oldValue = this.location;
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
		Object oldValue = this.board;
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
	public String toString() {
		return String.format("board (%s), location (%s), side (%s)", boardFile, location, side);
	}
}