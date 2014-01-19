/*
 	Copyright (C) 2013 Richard Spelling <openpnp@chebacco.com>
 	
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
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.machine.zippy.ZippyMachine;
import org.openpnp.machine.zippy.ZippyNozzle;
import org.openpnp.machine.zippy.ZippyNozzleTip;

public class NozzleTipsTableModel extends AbstractTableModel {
	final private Configuration configuration;
	
	private String[] columnNames = new String[] { "Id", "Type", "Loaded" };
	private List<ZippyNozzleTip> zippynozzletips;

	public NozzleTipsTableModel(Configuration configuration) {
		this.configuration = configuration;
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            public void configurationComplete(Configuration configuration) throws Exception {
                refresh();
            }
        });
	}

	public void refresh() { //pulls list of nozzle tips on machine
		zippynozzletips = new ArrayList<ZippyNozzleTip>(); //new empty list for nozzle tips
		for (Head head : configuration.getMachine().getHeads()) { //for each head
			for (Nozzle nozzle : head.getNozzles()) { //for each nozzle
				for (NozzleTip nozzletip : nozzle.getNozzleTips()) { //for each nozzletip
					zippynozzletips.add((ZippyNozzleTip) nozzletip); //add to list from above
				}
			}
		}
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
		return (zippynozzletips == null) ? 0 : zippynozzletips.size();
	}
	
	public NozzleTip getNozzleTip(int index) {
		return zippynozzletips.get(index);
	}
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex == 2;
	}
	
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		try {
			if (columnIndex == 2) {
				//find already loaded nozzletip
				for(ZippyNozzleTip nt : zippynozzletips){ //for each nozzletip in the list
					if(nt.isLoaded()){
						ZippyNozzle parent_nozzle = null;
						for (Head head : configuration.getMachine().getHeads()) { //for each head
							for (Nozzle nozzle : head.getNozzles()) { //for each nozzle
								for (NozzleTip temp_nozzletip : nozzle.getNozzleTips()) { //for each nozzletip
									if(temp_nozzletip.getId() == nt.getId()) //get parent nozzle object
										parent_nozzle = (ZippyNozzle) nozzle;
								}
							}
						}
						nt.unload(parent_nozzle);
//						nt.setLoaded(false);
						
					}
				}
				ZippyNozzleTip nt = zippynozzletips.get(rowIndex);
				//iterate through config and find parent nozzle.
				//assumes unique nozzle tip name
				ZippyNozzle parent_nozzle = null;
				for (Head head : configuration.getMachine().getHeads()) { //for each head
					for (Nozzle nozzle : head.getNozzles()) { //for each nozzle
						for (NozzleTip temp_nozzletip : nozzle.getNozzleTips()) { //for each nozzletip
							if(temp_nozzletip.getId() == nt.getId())
								parent_nozzle = (ZippyNozzle) nozzle;
						}
					}
				}
				//load this nozzletip
				nt.load(parent_nozzle);
//				nt.setLoaded(true);
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

	public Object getValueAt(int row, int col) { //just fills in table on left
		switch (col) {
		case 0:
			return zippynozzletips.get(row).getId();
		case 1:
			return zippynozzletips.get(row).getClass().getSimpleName();
		case 2:
			return ((ZippyNozzleTip) zippynozzletips.get(row)).isLoaded();
		default:
			return null;
		}
		
	}
}