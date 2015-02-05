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

import java.util.Locale;

import javax.swing.table.AbstractTableModel;

import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.Type;

public class PlacementsTableModel extends AbstractTableModel {
	final Configuration configuration;
	
	private String[] columnNames = new String[] {
		"Id", 
		"Part", 
		"Side", 
		"X", 
		"Y", 
		"ø",
		"Type"
		};
	private Class[] columnTypes = new Class[] {
		String.class,
		Part.class,
		Side.class,
		LengthCellValue.class,
		LengthCellValue.class,
		String.class,
		Type.class
	};
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
		return columnIndex != 0;
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return columnTypes[columnIndex];
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		try {
			Placement placement = board.getPlacements().get(rowIndex);
			if (columnIndex == 1) {
				placement.setPart((Part) aValue);
			}
			else if (columnIndex == 2) {
				placement.setSide((Side) aValue);
			}
			else if (columnIndex == 3) {
				LengthCellValue value = (LengthCellValue) aValue;
				value.setDisplayNativeUnits(true);
				Length length = value.getLength();
				Location location = placement.getLocation();
				location = Length.setLocationField(configuration, location, length, Length.Field.X, true);
				placement.setLocation(location);
			}
			else if (columnIndex == 4) {
				LengthCellValue value = (LengthCellValue) aValue;
				value.setDisplayNativeUnits(true);
				Length length = value.getLength();
				Location location = placement.getLocation();
				location = Length.setLocationField(configuration, location, length, Length.Field.Y, true);
				placement.setLocation(location);
			}
            else if (columnIndex == 5) {
                placement.setLocation(placement.getLocation().derive(null, null, null, Double.parseDouble(aValue.toString())));
            }
            else if (columnIndex == 6) {
                placement.setType((Type) aValue);
            }
		}
		catch (Exception e) {
			// TODO: dialog, bad input
		}
	}

	public Object getValueAt(int row, int col) {
		Placement placement = board.getPlacements().get(row);
		Location loc = placement.getLocation();
		switch (col) {
		case 0:
			 return placement.getId();
		case 1:
			return placement.getPart();
		case 2:
			 return placement.getSide();
		case 3:
			return new LengthCellValue(loc.getLengthX(), true);
		case 4:
			return new LengthCellValue(loc.getLengthY(), true);
		case 5:
			return String.format(Locale.US,configuration.getLengthDisplayFormat(), loc.getRotation());
		case 6:
		    return placement.getType();
		default:
			return null;
		}
	}
}