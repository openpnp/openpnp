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
import org.openpnp.model.Pipeline;

@SuppressWarnings("serial")
public class PackagesTableModel extends AbstractTableModel implements PropertyChangeListener {

    private String[] columnNames = new String[] {"ID", "Description", "Tape Specification", "Pipeline"};
    private Class[] columnTypes = new Class[] {String.class, String.class, String.class, Pipeline.class};
    private List<Package> packages;

    public PackagesTableModel() {
        Configuration.get().addPropertyChangeListener("packages", this);
        packages = new ArrayList<>(Configuration.get().getPackages());
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
        return columnIndex != 0;
    }

    public Package getPackage(int index) {
        return packages.get(index);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            Package pkg = packages.get(rowIndex);
            if (columnIndex == 1) {
                pkg.setDescription((String) aValue);
            }
            else if (columnIndex == 2) {
                pkg.setTapeSpecification((String) aValue);
            } else if (columnIndex == 3) {
                pkg.setPipeline((Pipeline) aValue);
            }
        }
        catch (Exception e) {
            // TODO: dialog, bad input
        }
    }

    public Object getValueAt(int row, int col) {
        Package pkg = packages.get(row);
        switch (col) {
            case 0:
                return pkg.getId();
            case 1:
                return pkg.getDescription();
            case 2:
                return pkg.getTapeSpecification();
            case 3:
                return pkg.getPipeline();
            default:
                return null;
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent arg0) {
        packages = new ArrayList<>(Configuration.get().getPackages());
        fireTableDataChanged();

        if (arg0.getSource() instanceof Package) {
            // Only single package data changed, but sort order might change, so still need fireTableDataChanged().
            fireTableDataChanged();
        }
        else  {
            // Parts list itself changes.
            if (packages != null) {
                for (Package pkg : packages) {
                    pkg.removePropertyChangeListener(this);
                }
            }
            packages = new ArrayList<>(Configuration.get().getPackages());
            fireTableDataChanged();
            for (Package pkg : packages) {
                pkg.addPropertyChangeListener(this);
            }
        }
    }
}
