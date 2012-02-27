package org.openpnp.gui;

import javax.swing.table.AbstractTableModel;

import org.openpnp.model.Board;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;

class PlacementsTableModel extends AbstractTableModel {
	final Configuration configuration;
	
	private String[] columnNames = new String[] { "Id", "Part", 
			"X Pos.", "Y Pos.", "Rotation" };
	private Board board;

	public PlacementsTableModel(Configuration configuration) {
		this.configuration = configuration;
	}

	public void setBoard(Board board) {
		this.board = board;
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
		return (board == null) ? 0 : board.getPlacements().size();
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return true;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		try {
			Placement placement = board.getPlacements().get(rowIndex);
			if (columnIndex == 0) {
				if (aValue == null || aValue.toString().trim().length() == 0) {
					return;
				}
				placement.setId(aValue.toString());
			}
			else if (columnIndex == 1) {
				Part part = configuration.getPart(aValue.toString());
				if (part == null) {
					// TODO: dialog, bad part id
					return;
				}
				placement.setPart(part);
			}
			else if (columnIndex == 2) {
				placement.getLocation().setX(Double.parseDouble(aValue.toString()));
			}
			else if (columnIndex == 3) {
				placement.getLocation().setY(Double.parseDouble(aValue.toString()));
			}
			else if (columnIndex == 4) {
				placement.getLocation().setRotation(Double.parseDouble(aValue.toString()));
			}
		}
		catch (Exception e) {
			// TODO: dialog, bad input
		}
	}

	public Object getValueAt(int row, int col) {
		Placement placement = board.getPlacements().get(row);
		switch (col) {
		case 0:
			 return placement.getId();
		case 1:
			return placement.getPart() == null ? "" : placement.getPart().getId();
		case 2:
			return String.format("%2.3f", placement.getLocation().getX());
		case 3:
			return String.format("%2.3f", placement.getLocation().getY());
		case 4:
			return String.format("%2.3f", placement.getLocation().getRotation());
		default:
			return null;
		}
	}
}