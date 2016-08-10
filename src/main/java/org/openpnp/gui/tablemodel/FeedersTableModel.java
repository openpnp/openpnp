/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui.tablemodel;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openpnp.ConfigurationListener;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;
import org.python.icu.text.StringPrep;

public class FeedersTableModel extends AbstractTableModel {
    final private Configuration configuration;

    private String[] columnNames = new String[] {"Name", "Type", "Part", "Enabled", "Child"};
    private List<Feeder> feeders;

    public FeedersTableModel(Configuration configuration) {
        this.configuration = configuration;
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            public void configurationComplete(Configuration configuration) throws Exception {
                refresh();
            }
        });
    }

    public void refresh() {
        feeders = new ArrayList<>(configuration.getMachine().getFeeders());
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
        if(columnIndex==4)
        {
            return feeders.get(rowIndex).supportsChildren();
        }
        return columnIndex == 0 || columnIndex == 3;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            Feeder feeder = feeders.get(rowIndex);
            if (columnIndex == 0) {
                feeder.setName((String) aValue);
            }
            else if (columnIndex == 3) {
                feeder.setEnabled((Boolean) aValue);
                refresh();
            }
            else if (columnIndex == 4)
            {
                if(isValidValue(aValue))
                {
                    feeder.setChild(configuration.getMachine().getFeederByName((String) aValue));
                    fireTableRowsUpdated(rowIndex,rowIndex);
                }
            }
        }
        catch (Exception e) {
            // TODO: dialog, bad input
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 3) {
            return Boolean.class;
        }
        return super.getColumnClass(columnIndex);
    }

    public Object getValueAt(int row, int col) {
        switch (col) {
            case 0:
                return feeders.get(row).getName();
            case 1:
                return feeders.get(row).getClass().getSimpleName();
            case 2: {
                Part part = feeders.get(row).getPart();
                if (part == null) {
                    return null;
                }
                return part.getId();
            }
            case 3:
                return feeders.get(row).isEnabled();
            case 4: {
                if (!feeders.get(row).supportsChildren()) {
                    return null;
                }
                if (feeders.get(row).getChild() == null) {
                    return "None";
                }
                return feeders.get(row).getChild().getName();
            }
            default:
                return null;
        }
    }


    // Extra public methods
    public String[] getChildFeeders() {
       // return validStates;
        if(configuration.getMachine()== null) {
            String[] feederNameList = new String[1];
            feederNameList[0] = "None";
            return feederNameList;
        }

        List<Feeder> feeders = configuration.getMachine().getFeeders();


        int iChildFeedCount=0;
        for(int i=0;i<feeders.size();i++)
        {
            if(feeders.get(i).isChildFeeder()) {
                iChildFeedCount++;
            }
        }
        String[] feederNameList = new String[iChildFeedCount+1];
        feederNameList[0]="None";
        for(int i=0;i<feeders.size();i++)
        {
            if(feeders.get(i).isChildFeeder()) {
                feederNameList[i + 1] = feeders.get(i).getName();
            }
        }

        return feederNameList;
    }

    // Protected methods
    protected boolean isValidValue(Object value) {
        if (value instanceof String) {
            String sValue = (String)value;

            String[] childFeeders = getChildFeeders();

            for (int i = 0; i < childFeeders.length; i++) {
                if (sValue.equals(childFeeders[i])) {
                    return true;
                }
            }
        }

        return false;
    }

    protected static final int COLUMN_COUNT = 2;

    protected static final String[] validStates = {
            "On order", "In stock", "Out of print"
    };



}
