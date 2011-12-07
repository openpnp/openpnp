package org.openpnp;

import org.openpnp.Board.Side;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

public class BoardLocation {
	@Element
	private Location location;
	@Attribute
	private Side side;
	private Board board;
	
	@Attribute
	private String boardFile;
	
	@SuppressWarnings("unused")
	@Commit
	private void commit() throws Exception {
		board = Configuration.get().getBoard(boardFile);
	}
	
	@SuppressWarnings("unused")
	@Persist
	private void persist() {
//		partId = (part == null ? null : part.getId());
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public Side getSide() {
		return side;
	}

	public void setSide(Side side) {
		this.side = side;
	}
	
	public Board getBoard() {
		return board;
	}

	public void setBoard(Board board) {
		this.board = board;
	}

	@Override
	public String toString() {
		return String.format("board (%s), location (%s), side (%s)", boardFile, location, side);
	}
}