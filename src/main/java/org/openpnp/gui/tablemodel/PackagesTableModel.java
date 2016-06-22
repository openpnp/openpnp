/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is package of OpenPnP.
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openpnp.model.Configuration;
import org.openpnp.model.Package;
// import org.openpnp.model.Package;

@SuppressWarnings("serial")
public class PackagesTableModel extends AbstractTableModel implements PropertyChangeListener {
    final private Configuration configuration;

    private String[] columnNames = new String[] {"Id", "Description"};
    private Class[] columnTypes = new Class[] {String.class, String.class,};
    private List<Package> packages;

    public PackagesTableModel(Configuration configuration) {
        this.configuration = configuration;
        configuration.addPropertyChangeListener("packages", this);
        packages = new ArrayList<>(configuration.getPackages());

    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return (packages == null) ? 0 : packages.size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 1;
    }

    public Package getPackage(int index) {
        return packages.get(index);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            Package this_package = packages.get(rowIndex);
            if (columnIndex == 1) {
                this_package.setDescription((String) aValue);
            }
        }
        catch (Exception e) {
            // TODO: dialog, bad input
        }
    }

    public Object getValueAt(int row, int col) {
        Package this_package = packages.get(row);
        switch (col) {
            case 0:
                return this_package.getId();
            case 1:
                return this_package.getDescription();
            default:
                return null;
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent arg0) {
        packages = new ArrayList<>(configuration.getPackages());
        fireTableDataChanged();
    }
}
