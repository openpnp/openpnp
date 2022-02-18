/*
 * Copyright (C) 2022 Tony Luken <tonyluken62+openpnp@gmail.com>
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

import java.util.Locale;

import javax.swing.table.AbstractTableModel;

import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.PanelLocation;

public class PanelLocationsTableModel extends AbstractTableModel {
    private final Configuration configuration;

    private String[] columnNames = new String[] {"Panel", "Width", "Length", "Side", "X", "Y", "Z",
            "Rot.", "Enabled?", "Check Fids?"};

    private Class[] columnTypes = new Class[] {String.class, LengthCellValue.class,
            LengthCellValue.class, Side.class, LengthCellValue.class, LengthCellValue.class,
            LengthCellValue.class, String.class, Boolean.class, Boolean.class};

    private Job job;

    public PanelLocationsTableModel(Configuration configuration) {
        this.configuration = configuration;
    }

    public void setJob(Job job) {
        this.job = job;
        fireTableDataChanged();
    }

    public Job getJob() {
        return job;
    }

    public PanelLocation getPanelLocation(int index) {
        return job.getPanelLocations().get(index);
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        if (job == null) {
            return 0;
        }
        return job.getPanelLocations().size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return (columnIndex != 0);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            PanelLocation panelLocation = job.getPanelLocations().get(rowIndex);
            if (columnIndex == 0) {
                panelLocation.getPanel().setName((String) aValue);
            }
            else if (columnIndex == 1) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location location = panelLocation.getPanel().getDimensions();
                location = Length.setLocationField(configuration, location, length, Length.Field.X);
                panelLocation.getPanel().setDimensions(location);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 2) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location location = panelLocation.getPanel().getDimensions();
                location = Length.setLocationField(configuration, location, length, Length.Field.Y);
                panelLocation.getPanel().setDimensions(location);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 3) {
                panelLocation.setSide((Side) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 4) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location location = panelLocation.getLocation();
                location = Length.setLocationField(configuration, location, length, Length.Field.X);
                panelLocation.setLocation(location);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 5) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location location = panelLocation.getLocation();
                location = Length.setLocationField(configuration, location, length, Length.Field.Y);
                panelLocation.setLocation(location);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 6) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location location = panelLocation.getLocation();
                location = Length.setLocationField(configuration, location, length, Length.Field.Z);
                panelLocation.setLocation(location);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 7) {
                panelLocation.setLocation(panelLocation.getLocation().derive(null, null, null,
                        Double.parseDouble(aValue.toString())));
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 8) {
                panelLocation.setEnabled((Boolean) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 9) {
                panelLocation.setCheckFiducials((Boolean) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
        catch (Exception e) {
            // TODO: dialog, bad input
        }
    }

    public Object getValueAt(int row, int col) {
        PanelLocation panelLocation = job.getPanelLocations().get(row);
        Location loc = panelLocation.getLocation();
        Location dim = panelLocation.getPanel().getDimensions();
        switch (col) {
            case 0:
                return panelLocation.getPanel().getName();
            case 1:
                return new LengthCellValue(dim.getLengthX());
            case 2:
                return new LengthCellValue(dim.getLengthY());
            case 3:
                return panelLocation.getSide();
            case 4:
                return new LengthCellValue(loc.getLengthX());
            case 5:
                return new LengthCellValue(loc.getLengthY());
            case 6:
                return new LengthCellValue(loc.getLengthZ());
            case 7:
                return String.format(Locale.US, configuration.getLengthDisplayFormat(),
                        loc.getRotation(), "");
            case 8:
                return panelLocation.isEnabled();
            case 9:
                return panelLocation.isCheckFiducials();
            default:
                return null;
        }
    }
}
