package org.openpnp.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openpnp.Length;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.util.LengthUtil;

class PartsTableModel extends AbstractTableModel implements PropertyChangeListener {
	final private Configuration configuration;
	
	private String[] columnNames = new String[] { "Id", "Name",
			"Height", "Package" };
	private List<Part> parts;

	public PartsTableModel(Configuration configuration) {
		this.configuration = configuration;
		configuration.addPropertyChangeListener("parts", this);
		parts = new ArrayList<Part>(configuration.getParts());
	}

	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return (parts == null) ? 0 : parts.size();
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return true;
	}
	
	public Part getPart(int index) {
		return parts.get(index);
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		try {
			if (columnIndex == 0) {
				if (aValue == null || aValue.toString().trim().length() == 0) {
					return;
				}
				parts.get(rowIndex).setId(aValue.toString());
			}
			else if (columnIndex == 1) {
				parts.get(rowIndex).setName(aValue.toString());
			}
			else if (columnIndex == 2) {
				Length length = LengthUtil.parseLengthValue(aValue.toString());
				if (length == null) {
					// TODO: dialog, unable to parse
					return;
				}
				parts.get(rowIndex).setHeight(length.getValue());
				if (length.getUnits() != null) {
					parts.get(rowIndex).setHeightUnits(length.getUnits());
				}
			}
			else if (columnIndex == 3) {
				org.openpnp.model.Package pkg = configuration.getPackage(aValue.toString());
				if (pkg == null) {
					// TODO: dialog, package not found
					return;
				}
				parts.get(rowIndex).setPackage(pkg);
			}
			configuration.setDirty(true);
		}
		catch (Exception e) {
			// TODO: dialog, bad input
		}
	}

	public Object getValueAt(int row, int col) {
		Part part = parts.get(row);
		switch (col) {
		case 0:
			return part.getId();
		case 1:
			 return part.getName();
		case 2:
			return String.format("%2.3f%s", part.getHeight(), part.getHeightUnits().getShortName());
		case 3:
			 return part.getPackage() == null ? null : part.getPackage().getId();
		default:
			return null;
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		parts = new ArrayList<Part>(configuration.getParts());
		fireTableDataChanged();
	}
}