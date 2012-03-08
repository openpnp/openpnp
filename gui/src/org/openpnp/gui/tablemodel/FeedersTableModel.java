package org.openpnp.gui.tablemodel;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openpnp.ConfigurationListener;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Feeder;

public class FeedersTableModel extends AbstractTableModel implements ConfigurationListener {
	final private Configuration configuration;
	
	private String[] columnNames = new String[] { "Id", "Type", "Enabled" };
	private List<Feeder> feeders;

	public FeedersTableModel(Configuration configuration) {
		this.configuration = configuration;
		configuration.addListener(this);
	}

	public void configurationLoaded(Configuration configuration) {
		feeders = new ArrayList<Feeder>(configuration.getMachine().getFeeders());
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
	
	public Feeder getFeeder(int index) {
		return feeders.get(index);
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == 2) {
			return Boolean.class;
		}
		return super.getColumnClass(columnIndex);
	}

	public Object getValueAt(int row, int col) {
		switch (col) {
		case 0:
			return feeders.get(row).getId();
		case 1:
			return feeders.get(row).getClass().getSimpleName();
		case 2:
			return feeders.get(row).isEnabled();
		default:
			return null;
		}
	}
}