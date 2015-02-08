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
import org.openpnp.gui.support.HeadCellValue;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;

public class NozzleTipsTableModel extends AbstractTableModel {
	final private Configuration configuration;
	
	private String[] columnNames = new String[] { "Name", "Nozzle", "Head" };
	private List<NozzleTipWrapper> nozzleTips;

	public NozzleTipsTableModel(Configuration configuration) {
		this.configuration = configuration;
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            public void configurationComplete(Configuration configuration) throws Exception {
                refresh();
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
		return (nozzleTips == null) ? 0 : nozzleTips.size();
	}
	
    public NozzleTip getNozzleTip(int index) {
        return nozzleTips.get(index).nozzleTip;
    }
    
    public Nozzle getNozzle(int index) {
        return nozzleTips.get(index).nozzle;
    }
	
	public void refresh() {
	    nozzleTips = new ArrayList<NozzleTipWrapper>();
		for (Head head : Configuration.get().getMachine().getHeads()) {
		    for (Nozzle nozzle : head.getNozzles()) {
		        for (NozzleTip nozzleTip : nozzle.getNozzleTips()) {
	                nozzleTips.add(new NozzleTipWrapper(nozzleTip, nozzle));
		        }
		    }
		}
		fireTableDataChanged();
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
	    return false;
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == 1) {
			return HeadCellValue.class;
		}
		return super.getColumnClass(columnIndex);
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
	}
	
	public Object getValueAt(int row, int col) {
	    NozzleTipWrapper nozzleTipWrapper = nozzleTips.get(row);
		switch (col) {
		case 0:
			return nozzleTipWrapper.nozzleTip.getName();
		case 1:
		    return nozzleTipWrapper.nozzle.getName();
        case 2:
            return new HeadCellValue(nozzleTipWrapper.nozzle.getHead());
			
		default:
			return null;
		}
	}
	
	class NozzleTipWrapper {
        public NozzleTip nozzleTip;
        public Nozzle nozzle;
        
	    public NozzleTipWrapper(NozzleTip nozzleTip, Nozzle nozzle) {
	        this.nozzleTip = nozzleTip;
	        this.nozzle = nozzle;
	    }
	}
}