/*
 * Copyright (C) 2021 Tony Luken <tonyluken@att.net>
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.LengthCellValueWithNans;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.wizards.ReferenceCameraCalibrationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;
import org.openpnp.util.BeanUtils;

public class CameraCalibrationHeightsTableModel extends AbstractTableModel {
    final private ReferenceCameraCalibrationWizard wizard;

    private String[] columnNames = new String[] {"Height"};
    private Class[] columnTypes = new Class[] {LengthCellValueWithNans.class};
    private List<Length> heights;

    public CameraCalibrationHeightsTableModel(ReferenceCameraCalibrationWizard wizard) {
        this.wizard = wizard;
        refresh();
    }

    public void refresh() {
        heights = new ArrayList<Length>(wizard.getCalibrationHeights());
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
        return (heights == null) ? 0 : heights.size();
    }

    public Length getHeight(int index) {
        return heights.get(index);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            Length height = heights.get(rowIndex);
            if (columnIndex == 0) {
                LengthCellValueWithNans value = (LengthCellValueWithNans) aValue;
                value.setDisplayNativeUnits(true);
                Length newHeight = value.getLength();
                if (newHeight.getUnits() == null) {
                    if (height != null) {
                        newHeight.setUnits(height.getUnits());
                    }
                    if (newHeight.getUnits() == null) {
                        newHeight.setUnits(Configuration.get().getSystemUnits());
                    }
                }
                if (height != null) {
                    height.setUnits(newHeight.getUnits());
                    height.setValue(newHeight.getValue());
                }
                else {
                    height = newHeight;
                }
            }
        }
        catch (Exception e) {
            // TODO: dialog, bad input
        }
    }

    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
    }

    
    public Object getValueAt(int row, int col) {
        switch (col) {
            case 0:
                return new LengthCellValueWithNans(heights.get(row), false);
            default:
                return null;
        }
    }

}
