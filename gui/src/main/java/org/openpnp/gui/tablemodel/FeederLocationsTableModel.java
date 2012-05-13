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

package org.openpnp.gui.tablemodel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openpnp.gui.support.FeederCellValue;
import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.model.Configuration;
import org.openpnp.model.FeederLocation;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;

public class FeederLocationsTableModel extends AbstractTableModel implements PropertyChangeListener {
	final private Configuration configuration;
	
	private String[] columnNames = new String[] { "Feeder", "X", "Y", "Z", "ø" };
	private Part part;
	private List<FeederLocation> feederLocations;

	public FeederLocationsTableModel(Configuration configuration) {
		this.configuration = configuration;
		FeederCellValue.setConfiguration(configuration);
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
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == 0) {
			return FeederCellValue.class;
		}
		else if (columnIndex == 1 || columnIndex == 2 || columnIndex == 3) {
			return LengthCellValue.class;
		}
		return super.getColumnClass(columnIndex);
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
				Feeder feeder = ((FeederCellValue) aValue).getFeeder();
				feederLocation.setFeeder(feeder);
			}
			else if (columnIndex == 1) {
				Length length = ((LengthCellValue) aValue).getLength();
				Location location = feederLocation.getLocation();
				if (location.getUnits() == null) {
					location.setUnits(length.getUnits());
				}
				else {
					location = location.convertToUnits(length.getUnits());
				}
				location.setX(length.getValue());
				feederLocation.setLocation(location);
			}
			else if (columnIndex == 2) {
				Length length = ((LengthCellValue) aValue).getLength();
				Location location = feederLocation.getLocation();
				if (location.getUnits() == null) {
					location.setUnits(length.getUnits());
				}
				else {
					location = location.convertToUnits(length.getUnits());
				}
				location.setY(length.getValue());
				feederLocation.setLocation(location);
			}
			else if (columnIndex == 3) {
				Length length = ((LengthCellValue) aValue).getLength();
				Location location = feederLocation.getLocation();
				if (location.getUnits() == null) {
					location.setUnits(length.getUnits());
				}
				else {
					location = location.convertToUnits(length.getUnits());
				}
				location.setZ(length.getValue());
				feederLocation.setLocation(location);
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
		Location loc = feederLocation.getLocation();
		switch (col) {
		case 0:
			return new FeederCellValue(feederLocation.getFeeder());
		case 1:
			return new LengthCellValue(loc.getX(), loc.getUnits());
		case 2:
			return new LengthCellValue(loc.getY(), loc.getUnits());
		case 3:
			return new LengthCellValue(loc.getZ(), loc.getUnits());
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