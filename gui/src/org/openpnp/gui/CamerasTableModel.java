package org.openpnp.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openpnp.ConfigurationListener;
import org.openpnp.model.Configuration;
import org.openpnp.model.FeederLocation;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;

class CamerasTableModel extends AbstractTableModel implements ConfigurationListener {
	final private Configuration configuration;
	
	private String[] columnNames = new String[] { "Name", "Looking", "X", "Y", "Z", "θ" };
	private List<Camera> cameras;

	public CamerasTableModel(Configuration configuration) {
		this.configuration = configuration;
		configuration.addListener(this);
	}

	public void configurationLoaded(Configuration configuration) {
		cameras = new ArrayList<Camera>(configuration.getMachine().getCameras());
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
		return (cameras == null) ? 0 : cameras.size();
	}
	
	public Camera getCamera(int index) {
		return cameras.get(index);
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return (columnIndex != 1);
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		try {
			Camera camera = cameras.get(rowIndex);
			if (columnIndex == 0) {
				// TODO: setName()
			}
			else if (columnIndex == 2) {
				camera.getLocation().setX(Double.parseDouble(aValue.toString()));
			}
			else if (columnIndex == 3) {
				camera.getLocation().setY(Double.parseDouble(aValue.toString()));
			}
			else if (columnIndex == 4) {
				camera.getLocation().setZ(Double.parseDouble(aValue.toString()));
			}
			else if (columnIndex == 5) {
				camera.getLocation().setRotation(Double.parseDouble(aValue.toString()));
			}
			configuration.setDirty(true);
		}
		catch (Exception e) {
			// TODO: dialog, bad input
		}
	}
	
	
//	@Override
//	public Class<?> getColumnClass(int columnIndex) {
//		if (columnIndex == 2) {
//			return Boolean.class;
//		}
//		return super.getColumnClass(columnIndex);
//	}

	public Object getValueAt(int row, int col) {
		Camera camera = cameras.get(row);
		switch (col) {
		case 0:
			return camera.getName();
		case 1:
			return camera.getLooking().toString();
			
		case 2:
			return String.format("%2.3f", camera.getLocation().getX());
		case 3:
			return String.format("%2.3f", camera.getLocation().getY());
		case 4:
			return String.format("%2.3f", camera.getLocation().getZ());
		case 5:
			return String.format("%2.3f", camera.getLocation().getRotation());
			
		default:
			return null;
		}
	}
}