package org.openpnp.gui;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import org.openpnp.Board;
import org.openpnp.BoardLocation;
import org.openpnp.Configuration;
import org.openpnp.Job;
import org.openpnp.Placement;

class PartsTableModel extends AbstractTableModel {
	private String[] columnNames = new String[] { "Board #", "Part",
			"Package", "Feeder", "X Pos.", "Y Pos.", "Rotation" };
	private ArrayList<PartMetaData> boardPlacements = new ArrayList<PartMetaData>();
	private Configuration configuration;
	
	public PartsTableModel(Configuration configuration) {
		this.configuration = configuration;
	}

	public void setJob(Job job) {
		System.out.println(job.toString());
		boardPlacements.clear();
		int boardNumber = 1;
		for (BoardLocation board : job.getBoards()) {
			for (Placement placement : board.getBoard().getPlacements()) {
				boardPlacements.add(new PartMetaData(boardNumber, board
						.getBoard(), placement));
			}
			boardNumber++;
		}
		fireTableDataChanged();
	}

	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return boardPlacements.size();
	}

	public Object getValueAt(int row, int col) {
		switch (col) {
		case 0:
			return boardPlacements.get(row).boardNumber;
		case 1:
			return boardPlacements.get(row).placement.getPart()
					.getId();
		case 2:
		{
			return boardPlacements.get(row).placement.getPart().getPackageId();
		}
		case 3:
			return boardPlacements.get(row).placement.getPart().getFeederLocations().get(0).getFeederId();
		case 4:
			return String
					.format("%2.3f", boardPlacements.get(row).placement
							.getLocation().getX());
		case 5:
			return String
					.format("%2.3f", boardPlacements.get(row).placement
							.getLocation().getY());
		case 6:
			return String.format("%2.3f",
					boardPlacements.get(row).placement.getLocation()
							.getRotation());
		default:
			return null;
		}
	}

	class PartMetaData {
		public int boardNumber;
		public Board board;
		public Placement placement;

		public PartMetaData(int boardNumber, Board board,
				Placement placement) {
			this.boardNumber = boardNumber;
			this.board = board;
			this.placement = placement;
		}
	}
}