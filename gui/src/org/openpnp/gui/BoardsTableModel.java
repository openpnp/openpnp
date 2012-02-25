package org.openpnp.gui;

import javax.swing.table.AbstractTableModel;

import org.openpnp.model.BoardLocation;
import org.openpnp.model.Job;
import org.openpnp.model.Location;

class BoardsTableModel extends AbstractTableModel {
	private String[] columnNames = new String[] { "Board", "X Pos.",
			"Y Pos.", "Z Pos.", "Rotation" };
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
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return true;
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		try {
			BoardLocation boardLocation = job.getBoardLocations().get(rowIndex);
			if (columnIndex == 0) {
				boardLocation.getBoard().setName(aValue.toString());
			}
			else if (columnIndex == 1) {
				boardLocation.getLocation().setX(Double.parseDouble(aValue.toString()));
			}
			else if (columnIndex == 2) {
				boardLocation.getLocation().setY(Double.parseDouble(aValue.toString()));
			}
			else if (columnIndex == 3) {
				boardLocation.getLocation().setZ(Double.parseDouble(aValue.toString()));
			}
			else if (columnIndex == 4) {
				boardLocation.getLocation().setRotation(Double.parseDouble(aValue.toString()));
			}
		}
		catch (Exception e) {
			// TODO: dialog, bad input
		}
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
			return String.format("%2.3f", loc.getZ());
		case 4:
			return String.format("%2.3f", loc.getRotation());
		default:
			return null;
		}
	}
}