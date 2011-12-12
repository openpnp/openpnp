package org.openpnp.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openpnp.Configuration;
import org.openpnp.Length;
import org.openpnp.Part;
import org.openpnp.spi.Feeder;
import org.openpnp.util.LengthUtil;

class FeedersTableModel extends AbstractTableModel {
	private String[] columnNames = new String[] { "Id", "Enabled", "Class" };
	private List<Feeder> feeders;

	public FeedersTableModel() {
	}

	public void refresh() {
		feeders = new ArrayList<Feeder>(Configuration.get().getMachine().getFeeders());
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
		return (feeders == null) ? 0 : feeders.size();
	}
	
	public Object getValueAt(int row, int col) {
		switch (col) {
		case 0:
			return feeders.get(row).getId();
		case 1:
			return feeders.get(row).isEnabled();
		case 2:
			return feeders.get(row).getClass().getSimpleName();
		default:
			return null;
		}
	}
}