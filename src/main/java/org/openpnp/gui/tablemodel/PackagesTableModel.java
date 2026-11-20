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
import java.util.Locale;

import org.openpnp.Translations;
import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.FiducialVisionSettings;
import org.openpnp.model.Length;
import org.openpnp.model.Package;

@SuppressWarnings("serial")
public class PackagesTableModel extends AbstractObjectTableModel implements PropertyChangeListener {
    final private Configuration configuration;

    private String[] columnNames = new String[] {
            Translations.getString("PackagesTableModel.ColumnName.ID"), //$NON-NLS-1$
            Translations.getString("PackagesTableModel.ColumnName.Description"), //$NON-NLS-1$
            Translations.getString("PackagesTableModel.ColumnName.TapeSpecification"), //$NON-NLS-1$
            Translations.getString("PackagesTableModel.ColumnName.BottomVision"), //$NON-NLS-1$
            Translations.getString("PackagesTableModel.ColumnName.FiducialVision"), //$NON-NLS-1$
            Translations.getString("PackagesTableModel.ColumnName.TapeRotation"), //$NON-NLS-1$
            Translations.getString("PackagesTableModel.ColumnName.TapePitch"), //$NON-NLS-1$
            Translations.getString("PackagesTableModel.ColumnName.TapeWidth") //$NON-NLS-1$
    };
    private Class[] columnTypes = new Class[] {String.class, String.class, String.class, BottomVisionSettings.class, FiducialVisionSettings.class,
            String.class, String.class, String.class};
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
            else if (columnIndex == 5) {
                // Blank clears it.
                String text = aValue == null ? "" : aValue.toString().trim();
                this_package.setTapeRotation(text.isEmpty() ? null : Double.parseDouble(text));
            }
            else if (columnIndex == 6) {
                this_package.setTapePitch(parseTapeLength(aValue, this_package.getTapePitch()));
            }
            else if (columnIndex == 7) {
                this_package.setTapeWidth(parseTapeLength(aValue, this_package.getTapeWidth()));
            }
        }
        catch (Exception e) {
            // TODO: dialog, bad input
        }
    }

    /**
     * A tape pitch or width typed into a cell. Without units it takes the old value's units, or
     * else the system units. Null, so not known, for a blank cell or a value that is not positive.
     */
    static Length parseTapeLength(Object aValue, Length oldValue) {
        String text = aValue == null ? "" : aValue.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        Length length = Length.parse(text);
        if (length == null) {
            throw new IllegalArgumentException(text);
        }
        if (oldValue != null) {
            length = length.changeUnitsIfUnspecified(oldValue.getUnits());
        }
        length = length.changeUnitsIfUnspecified(Configuration.get().getSystemUnits());
        return length.getValue() > 0 ? length : null;
    }

    /** A tape pitch or width as lengths are shown in tables, or blank when not known. */
    private static String formatTapeLength(Length length) {
        return length == null ? "" : new LengthCellValue(length, true).toString();
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
            case 5:
                return this_package.getTapeRotation() == null ? ""
                        : String.format(Locale.US, "%.1f", this_package.getTapeRotation());
            case 6:
                return formatTapeLength(this_package.getTapePitch());
            case 7:
                return formatTapeLength(this_package.getTapeWidth());
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
