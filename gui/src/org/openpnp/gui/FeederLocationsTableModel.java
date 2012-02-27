package org.openpnp.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openpnp.model.Configuration;
import org.openpnp.model.FeederLocation;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;

class FeederLocationsTableModel extends AbstractTableModel implements PropertyChangeListener {
	final private Configuration configuration;
	
	private String[] columnNames = new String[] { "Feeder", "X Pos.", "Y Pos.", "Z Pos.", "Rotation" };
	private Part part;
	private List<FeederLocation> feederLocations;

	public FeederLocationsTableModel(Configuration configuration) {
		this.configuration = configuration;
	}

	public void setPart(Part part) {
		if (this.part != null) {
			this.part.removePropertyChangeListener("feederLocations", this);
		}
		this.part = part;
		if (part == null) {
			feederLocations = null;
		}
		else {
			part.addPropertyChangeListener("feederLocations", this);
			feederLocations = part.getFeederLocations();
		}
		fireTableDataChanged();
	}
	
	public FeederLocation getFeederLocation(int index) {
		return feederLocations.get(index);
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
				Feeder feeder = configuration.getMachine().getFeeder(aValue.toString());
				if (feeder == null) {
					// TODO: dialog, no feeder
					return;
				}
				feederLocation.setFeeder(feeder);
			}
			else if (columnIndex == 1) {
				feederLocation.getLocation().setX(Double.parseDouble(aValue.toString()));
			}
			else if (columnIndex == 2) {
				feederLocation.getLocation().setY(Double.parseDouble(aValue.toString()));
			}
			else if (columnIndex == 3) {
				feederLocation.getLocation().setZ(Double.parseDouble(aValue.toString()));
			}
			else if (columnIndex == 4) {
				feederLocation.getLocation().setRotation(Double.parseDouble(aValue.toString()));
			}
			configuration.setDirty(true);
		}
		catch (Exception e) {
			// TODO: dialog, bad input
		}
	}

	public Object getValueAt(int row, int col) {
		FeederLocation feederLocation = feederLocations.get(row);
		switch (col) {
		case 0:
			return feederLocation.getFeeder() == null ? "" : feederLocation.getFeeder().getId();
		case 1:
			return String.format("%2.3f", feederLocation.getLocation().getX());
		case 2:
			return String.format("%2.3f", feederLocation.getLocation().getY());
		case 3:
			return String.format("%2.3f", feederLocation.getLocation().getZ());
		case 4:
			return String.format("%2.3f", feederLocation.getLocation().getRotation());
		default:
			return null;
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (part == null) {
			feederLocations = null;
		}
		else {
			feederLocations = part.getFeederLocations();
		}
		fireTableDataChanged();
	}
}