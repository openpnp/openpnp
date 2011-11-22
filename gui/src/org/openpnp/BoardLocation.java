package org.openpnp;

import org.openpnp.Board.Side;

public class BoardLocation {
	private Location location;
	private Board board;
	private Side side;
	
	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public Board getBoard() {
		return board;
	}
	
	public void setBoard(Board board) {
		this.board = board;
	}
	
	public Side getSide() {
		return side;
	}

	public void setSide(Side side) {
		this.side = side;
	}

	@Override
	public String toString() {
		return String.format("board (%s), location (%s)", board, location);
	}
}