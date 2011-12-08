package org.openpnp.gui;

import javax.swing.table.AbstractTableModel;

import org.openpnp.BoardLocation;
import org.openpnp.Job;
import org.openpnp.Location;

class BoardsTableModel extends AbstractTableModel {
	private String[] columnNames = new String[] { "Board", "X Pos.",
			"Y Pos.", "Rotation" };
	private Job job;

	public void setJob(Job job) {
		this.job = job;
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
		if (job == null) {
			return 0;
		}
		return job.getBoardLocations().size();
	}
	
	public Object getValueAt(int row, int col) {
		BoardLocation boardLocation = job.getBoardLocations().get(row);
		Location loc = boardLocation.getLocation();
		switch (col) {
		case 0:
			return boardLocation.getBoard().getName();
		case 1:
			return String.format("%2.3f", loc.getX());
		case 2:
			return String.format("%2.3f", loc.getY());
		case 3:
			return String.format("%2.3f", loc.getRotation());
		default:
			return null;
		}
	}
}