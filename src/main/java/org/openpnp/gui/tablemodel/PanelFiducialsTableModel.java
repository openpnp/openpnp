/*
 * Copyright (C) 2022 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
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

import java.util.List;

import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.gui.support.PartCellValue;
import org.openpnp.gui.support.RotationCellValue;
import org.openpnp.model.Board.Side;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Panel;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;

@SuppressWarnings("serial")
public class PanelFiducialsTableModel extends AbstractObjectTableModel {
    private Panel panel;

    private String[] columnNames =
            new String[] {"Enabled", "ID", "Part", "Side", "X", "Y", "Rot."};

    @SuppressWarnings("rawtypes")
    private Class[] columnTypes = new Class[] {Boolean.class, PartCellValue.class, Part.class,
            Side.class, LengthCellValue.class, LengthCellValue.class, RotationCellValue.class};

    public PanelFiducialsTableModel(Panel panel) {
        this.panel = panel;
    }
    
    public Panel getPanel() {
        return panel;
    }

    public void setPanel(Panel panel) {
        this.panel = panel;
        fireTableDataChanged();
    }

    @Override
    public Placement getRowObjectAt(int index) {
        return panel.getPlacements().get(index);
    }

    @Override
    public int indexOf(Object object) {
        return panel.getPlacements().indexOf(object);
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return (panel == null) ? 0 : panel.getPlacements().size();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex != 1;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            Placement placement = panel.getPlacements().get(rowIndex);
            if (columnIndex == 0) {
                placement.setEnabled((Boolean) aValue);
            }
            else if (columnIndex == 2) {
                placement.setPart((Part) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 3) {
                placement.setSide((Side) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 4) {
                LengthCellValue value = (LengthCellValue) aValue;
                value.setDisplayNativeUnits(true);
                Length length = value.getLength();
                Location location = placement.getLocation();
                location = Length.setLocationField(Configuration.get(), location, length,
                        Length.Field.X, true);
                placement.setLocation(location);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 5) {
                LengthCellValue value = (LengthCellValue) aValue;
                value.setDisplayNativeUnits(true);
                Length length = value.getLength();
                Location location = placement.getLocation();
                location = Length.setLocationField(Configuration.get(), location, length,
                        Length.Field.Y, true);
                placement.setLocation(location);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 6) {
                Location location = placement.getLocation();
                double rotation = Double.parseDouble(aValue.toString());
                placement.setLocation(location.derive(null, null, null, rotation));
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
        catch (Exception e) {
            // TODO: dialog, bad input
        }
    }

    public Object getValueAt(int row, int col) {
        Placement placement = panel.getPlacements().get(row);
        Location loc = placement.getLocation();
        switch (col) {
			case 0:
				return placement.isEnabled();
            case 1:
                return new PartCellValue(placement.getId());
            case 2:
                return placement.getPart();
            case 3:
                return placement.getSide();
            case 4:
                return new LengthCellValue(loc.getLengthX(), true);
            case 5:
                return new LengthCellValue(loc.getLengthY(), true);
            case 6:
                return new RotationCellValue(loc.getRotation(), true);
            default:
                return null;
        }
    }
}
