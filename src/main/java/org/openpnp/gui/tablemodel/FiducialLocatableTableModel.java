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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import javax.swing.table.AbstractTableModel;

import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.pmw.tinylog.Logger;
import org.openpnp.model.Configuration;
import org.openpnp.model.FiducialLocatable;
import org.openpnp.model.FiducialLocatableLocation;
import org.openpnp.model.Job;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Panel;
import org.openpnp.model.PanelLocation;

@SuppressWarnings("serial")
public class FiducialLocatableTableModel extends AbstractObjectTableModel {
    private final Configuration configuration;

    private String[] columnNames = new String[] {"FiducialLocatable Name", "Width", "Length"};

    @SuppressWarnings("rawtypes")
    private Class[] columnTypes = new Class[] {String.class, LengthCellValue.class,
            LengthCellValue.class};

    private Supplier<List<? extends FiducialLocatable>> fiducialLocatables;

    private Class<? extends FiducialLocatable> classType;

    public FiducialLocatableTableModel(Configuration configuration, Supplier<List<? extends FiducialLocatable>> fiducialLocatables, Class<? extends FiducialLocatable> classType) {
        this.configuration = configuration;
        this.fiducialLocatables = fiducialLocatables;
        this.classType = classType;
    }

    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            if (classType == Board.class) {
                return "Board Name";
            }
            else if (classType == Panel.class) {
                return "Panel Name";
            }
        }
        return columnNames[column];
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        if (fiducialLocatables.get() == null) {
            return 0;
        }
        return fiducialLocatables.get().size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex == -1) {
            return false;
        }
        return true;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            FiducialLocatable fiducialLocatable = fiducialLocatables.get().get(rowIndex);
            if (columnIndex == 0) {
                fiducialLocatable.setName((String) aValue);
            }
            else if (columnIndex == 1) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location dims = fiducialLocatable.getDimensions();
                dims = Length.setLocationField(configuration, dims, length, Length.Field.X);
                fiducialLocatable.setDimensions(dims);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 2) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location dims = fiducialLocatable.getDimensions();
                dims = Length.setLocationField(configuration, dims, length, Length.Field.Y);
                fiducialLocatable.setDimensions(dims);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
        catch (Exception e) {
            // TODO: dialog, bad input
        }
    }

    public Object getValueAt(int row, int col) {
        FiducialLocatable fiducialLocatable = fiducialLocatables.get().get(row);
        Location dim = fiducialLocatable.getDimensions();
        switch (col) {
            case 0:
                return fiducialLocatable.getName();
            case 1:
                return new LengthCellValue(dim.getLengthX());
            case 2:
                return new LengthCellValue(dim.getLengthY());
            default:
                return null;
        }
    }

    @Override
    public Object getRowObjectAt(int index) {
        return fiducialLocatables.get().get(index);
    }
}
