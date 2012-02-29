package org.openpnp.gui;

import javax.swing.table.AbstractTableModel;

import org.openpnp.Length;
import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Job;
import org.openpnp.model.Location;
import org.openpnp.util.LengthUtil;

class BoardLocationsTableModel extends AbstractTableModel {
	private String[] columnNames = new String[] { "Board", "Side", "X",
			"Y", "Z", "θ" };
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
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == 2 || columnIndex == 3 || columnIndex == 4) {
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
			BoardLocation boardLocation = job.getBoardLocations().get(rowIndex);
			if (columnIndex == 0) {
				boardLocation.getBoard().setName((String) aValue);
			}
			else if (columnIndex == 1) {
				boardLocation.setSide((Side) aValue);
			}
			else if (columnIndex == 2) {
				Length length = ((LengthCellValue) aValue).getLength();
				Location location = boardLocation.getLocation();
				if (location.getUnits() == null) {
					location.setUnits(length.getUnits());
				}
				else {
					location = LengthUtil.convertLocation(location, length.getUnits());
				}
				location.setX(length.getValue());
				boardLocation.setLocation(location);
			}
			else if (columnIndex == 3) {
				Length length = ((LengthCellValue) aValue).getLength();
				Location location = boardLocation.getLocation();
				if (location.getUnits() == null) {
					location.setUnits(length.getUnits());
				}
				else {
					location = LengthUtil.convertLocation(location, length.getUnits());
				}
				location.setY(length.getValue());
				boardLocation.setLocation(location);
			}
			else if (columnIndex == 4) {
				Length length = ((LengthCellValue) aValue).getLength();
				Location location = boardLocation.getLocation();
				if (location.getUnits() == null) {
					location.setUnits(length.getUnits());
				}
				else {
					location = LengthUtil.convertLocation(location, length.getUnits());
				}
				location.setZ(length.getValue());
				boardLocation.setLocation(location);
			}
			else if (columnIndex == 5) {
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
			return boardLocation.getSide();
		case 2:
			return new LengthCellValue(loc.getX(), loc.getUnits());
		case 3:
			return new LengthCellValue(loc.getY(), loc.getUnits());
		case 4:
			return new LengthCellValue(loc.getZ(), loc.getUnits());
		case 5:
			return String.format("%2.3f", loc.getRotation());
		default:
			return null;
		}
	}
}