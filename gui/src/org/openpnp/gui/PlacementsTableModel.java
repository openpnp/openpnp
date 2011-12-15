package org.openpnp.gui;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openpnp.Configuration;
import org.openpnp.Part;
import org.openpnp.Placement;

class PlacementsTableModel extends AbstractTableModel {
	final Configuration configuration;
	
	private String[] columnNames = new String[] { "Part", "Package",
			"X Pos.", "Y Pos.", "Rotation" };
	private List<Placement> placements;

	public PlacementsTableModel(Configuration configuration) {
		this.configuration = configuration;
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
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex == 1) {
			return false;
		}
		return true;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		try {
			Placement placement = placements.get(rowIndex);
			if (columnIndex == 0) {
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
		Placement placement = placements.get(row);
		switch (col) {
		case 0:
			return placement.getPart().getId();
		case 1:
			 return placement.getPart().getPackage().getId();
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