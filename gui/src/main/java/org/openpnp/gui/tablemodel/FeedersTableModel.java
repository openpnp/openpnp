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
		refresh();
	}
	
	public void refresh() {
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
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex == 0 || columnIndex == 2;
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		try {
			Feeder feeder = feeders.get(rowIndex);
			if (columnIndex == 0) {
				if (aValue == null || aValue.toString().trim().length() == 0) {
					return;
				}
				feeder.setId(aValue.toString());
			}
			else if (columnIndex == 2) {
				feeder.setEnabled((Boolean) aValue);
			}
			configuration.setDirty(true);
		}
		catch (Exception e) {
			// TODO: dialog, bad input
		}
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