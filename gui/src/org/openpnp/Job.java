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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.openpnp.Board.Side;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A Job specifies a list of one or more Boards to populate along with their locations on the table. 
 */
public class Job {
	private String reference;
	private String name;
	private List<JobBoard> boards = new ArrayList<JobBoard>();
	
	public void parse(Node n, Configuration c) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();
		
		reference = Configuration.getAttribute(n, "reference");
		name = Configuration.getAttribute(n, "name");

		Map<String, Board> boardMap = new HashMap<String, Board>();
		
		NodeList boardNodes = (NodeList) xpath.evaluate("Boards/Board", n, XPathConstants.NODESET);
		for (int i = 0; i < boardNodes.getLength(); i++) {
			Node boardNode = boardNodes.item(i);
			Board board = new Board();
			board.parse(boardNode, c);
			boardMap.put(board.getReference(), board);
		}
		
		NodeList boardLocationNodes = (NodeList) xpath.evaluate("BoardLocations/BoardLocation", n, XPathConstants.NODESET);
		for (int i = 0; i < boardLocationNodes.getLength(); i++) {
			Node boardLocationNode = boardLocationNodes.item(i);
			JobBoard board = new JobBoard();
			board.setBoard(boardMap.get(Configuration.getAttribute(boardLocationNode, "board")));
			board.setSide(Side.valueOf(Configuration.getAttribute(boardLocationNode, "side", "Top")));
			board.setLocation(new Location());
			board.getLocation().parse((Node) xpath.evaluate("Location", boardLocationNode, XPathConstants.NODE));
			boards.add(board);
		}
	}
	
	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<JobBoard> getBoards() {
		return boards;
	}
	
	public void setBoards(List<JobBoard> boards) {
		this.boards = boards;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < boards.size(); i++) {
			sb.append(boards.get(i).toString());
			if (i < boards.size() - 1) {
				sb.append(", ");
			}
		}
		return String.format("reference %s, name %s, boards (%s)", reference, name, sb.toString());
	}
	
	public static class JobBoard {
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
}
