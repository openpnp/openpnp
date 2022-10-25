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

import org.openpnp.Translations;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.FiducialVisionSettings;
import org.openpnp.model.Package;

@SuppressWarnings("serial")
public class PackagesTableModel extends AbstractObjectTableModel implements PropertyChangeListener {
    final private Configuration configuration;

    private String[] columnNames = new String[] {
            Translations.getStringOrDefault("PackagesTableModel.ColumnName.ID", "ID"),
            Translations.getStringOrDefault("PackagesTableModel.ColumnName.Description", "Description"),
            Translations.getStringOrDefault("PackagesTableModel.ColumnName.TapeSpecification",
                    "Tape Specification"),
            Translations.getStringOrDefault("PackagesTableModel.ColumnName.BottomVision", "BottomVision"),
            Translations.getStringOrDefault("PackagesTableModel.ColumnName.FiducialVision",
                    "FiducialVision")
    };
    private Class[] columnTypes = new Class[] {String.class, String.class, String.class, BottomVisionSettings.class, FiducialVisionSettings.class};
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
        return columnIndex != 0;
    }

    @Override
    public Package getRowObjectAt(int index) {
        return packages.get(index);
    }

    @Override
    public int indexOf(Object selectedPackage) {
        return packages.indexOf(selectedPackage);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            Package this_package = packages.get(rowIndex);
            if (columnIndex == 1) {
                this_package.setDescription((String) aValue);
            }
            else if (columnIndex == 2) {
                this_package.setTapeSpecification((String) aValue);
            }
            else if (columnIndex == 3) {
                this_package.setBottomVisionSettings((BottomVisionSettings) aValue);
            }
            else if (columnIndex == 4) {
                this_package.setFiducialVisionSettings((FiducialVisionSettings) aValue);
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
            case 2:
                return this_package.getTapeSpecification();
            case 3:
                return this_package.getBottomVisionSettings();
            case 4:
                return this_package.getFiducialVisionSettings();
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
