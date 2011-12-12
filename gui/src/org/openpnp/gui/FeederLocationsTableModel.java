package org.openpnp.gui;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openpnp.Configuration;
import org.openpnp.FeederLocation;

class FeederLocationsTableModel extends AbstractTableModel {
	private String[] columnNames = new String[] { "X Pos.", "Y Pos.", "Z Pos.", "Rotation" };
	private List<FeederLocation> feederLocations;

	public FeederLocationsTableModel() {
	}

	public void setFeederLocations(List<FeederLocation> feederLocations) {
		this.feederLocations = feederLocations;
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
		return (feederLocations == null) ? 0 : feederLocations.size();
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return true;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		try {
			FeederLocation feederLocation = feederLocations.get(rowIndex);
			if (columnIndex == 0) {
				feederLocation.getLocation().setX(Double.parseDouble(aValue.toString()));
			}
			if (columnIndex == 1) {
				feederLocation.getLocation().setY(Double.parseDouble(aValue.toString()));
			}
			if (columnIndex == 2) {
				feederLocation.getLocation().setZ(Double.parseDouble(aValue.toString()));
			}
			if (columnIndex == 3) {
				feederLocation.getLocation().setRotation(Double.parseDouble(aValue.toString()));
			}
			Configuration.get().setDirty(true);
		}
		catch (Exception e) {
			// TODO: dialog, bad input
		}
	}

	public Object getValueAt(int row, int col) {
		FeederLocation feederLocation = feederLocations.get(row);
		switch (col) {
		case 0:
			return String.format("%2.3f", feederLocation.getLocation().getX());
		case 1:
			return String.format("%2.3f", feederLocation.getLocation().getY());
		case 2:
			return String.format("%2.3f", feederLocation.getLocation().getZ());
		case 3:
			return String.format("%2.3f", feederLocation.getLocation().getRotation());
		default:
			return null;
		}
	}
}