package org.openpnp.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openpnp.Length;
import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.gui.support.PackageCellValue;
import org.openpnp.model.Configuration;
import org.openpnp.model.Package;
import org.openpnp.model.Part;

class PartsTableModel extends AbstractTableModel implements PropertyChangeListener {
	final private Configuration configuration;
	
	private String[] columnNames = new String[] { "Id", "Name",
			"Height", "Package" };
	private List<Part> parts;

	public PartsTableModel(Configuration configuration) {
		this.configuration = configuration;
		PackageCellValue.setConfiguration(configuration);
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
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == 2) {
			return LengthCellValue.class;
		}
		else if (columnIndex == 3) {
			return PackageCellValue.class;
		}
		return super.getColumnClass(columnIndex);
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
			Part part = parts.get(rowIndex);
			if (columnIndex == 0) {
				if (aValue == null || aValue.toString().trim().length() == 0) {
					return;
				}
				part.setId(aValue.toString());
			}
			else if (columnIndex == 1) {
				part.setName((String) aValue);
			}
			else if (columnIndex == 2) {
				Length length = ((LengthCellValue) aValue).getLength();
				part.setHeight(length.getValue());
				part.setHeightUnits(length.getUnits());
			}
			else if (columnIndex == 3) {
				Package packag = ((PackageCellValue) aValue).getPackage(); 
				part.setPackage(packag);
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
			return new LengthCellValue(part.getHeight(), part.getHeightUnits());
		case 3:
			 return new PackageCellValue(part.getPackage());
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