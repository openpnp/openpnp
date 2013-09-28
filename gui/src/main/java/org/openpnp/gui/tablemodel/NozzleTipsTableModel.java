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
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.Head;
import org.openpnp.spi.NozzleTip;

public class NozzleTipsTableModel extends AbstractTableModel {
	
	private String[] columnNames = new String[] { "Id", "Nozzle", "Class" };
	private List<NozzleNozzleTip> nozzletips;

	public NozzleTipsTableModel(Configuration configuration) {
	    Configuration.get().addListener(new ConfigurationListener.Adapter() {
	        public void configurationComplete(Configuration configuration) throws Exception {
	        	nozzletips = new ArrayList<NozzleNozzleTip>();
	            for (Head head : configuration.getMachine().getHeads()) {
		            for (Nozzle nozzle : head.getNozzles()) {
		                for (NozzleTip nozzletip : nozzle.getNozzleTips()) {
		                    nozzletips.add(new NozzleNozzleTip(nozzle, nozzletip));
		                }
		            }
	            }
	            fireTableDataChanged();
	        }
	    });
	}

	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return (nozzletips == null) ? 0 : nozzletips.size();
	}
	
	public NozzleTip getNozzleTip(int index) {
		return nozzletips.get(index).nozzletip;
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}
	
	public Object getValueAt(int row, int col) {
		NozzleNozzleTip nozzletip = nozzletips.get(row);
		switch (col) {
		case 0:
			return nozzletip.nozzletip.getId();
		case 1:
			return nozzletip.nozzle != null ? nozzletip.nozzle.getId() : "";
		case 2:
			return nozzletip.nozzletip.getClass().getSimpleName();
		default:
			return null;
		}
	}
	
	private class NozzleNozzleTip {
		public Nozzle nozzle;
		public NozzleTip nozzletip;
		
		public NozzleNozzleTip(Nozzle nozzle, NozzleTip nozzletip) {
			this.nozzle = nozzle;
			this.nozzletip = nozzletip;
		}
	}
}