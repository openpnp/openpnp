package org.openpnp.gui;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openpnp.Board;
import org.openpnp.Placement;

class PlacementsTableModel extends AbstractTableModel {
	private String[] columnNames = new String[] { "Part", "Package",
			"Feeder", "X Pos.", "Y Pos.", "Rotation" };
	private List<Placement> placements;

	public PlacementsTableModel() {
	}

	public void setPlacements(List<Placement> placements) {
		this.placements = placements;
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
		return (placements == null) ? 0 : placements.size();
	}

	public Object getValueAt(int row, int col) {
		switch (col) {
		case 0:
			return placements.get(row).getPart().getId();
		case 1:
			 return placements.get(row).getPart().getPackage().getId();
		case 2:
			 return placements.get(row).getPart().getFeederLocations().get(0).getFeeder().getId();
		case 3:
			return String.format("%2.3f", placements.get(row).getLocation().getX());
		case 4:
			return String.format("%2.3f", placements.get(row).getLocation().getY());
		case 5:
			return String.format("%2.3f", placements.get(row).getLocation().getRotation());
		default:
			return null;
		}
	}
}