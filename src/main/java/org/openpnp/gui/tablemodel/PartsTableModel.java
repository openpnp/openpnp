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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.openpnp.Translations;
import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.gui.support.PercentConverter;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.FiducialVisionSettings;
import org.openpnp.model.Length;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.util.Collect;

@SuppressWarnings("serial")
public class PartsTableModel extends AbstractObjectTableModel implements PropertyChangeListener {
    private String[] columnNames =
            new String[] {Translations.getString("PartsTableModel.ColumnName.ID"), //$NON-NLS-1$
                    Translations.getString("PartsTableModel.ColumnName.Description"), //$NON-NLS-1$
                    Translations.getString("PartsTableModel.ColumnName.Height"), //$NON-NLS-1$
                    Translations.getString("PartsTableModel.ColumnName.ThroughBoardDepth"), //$NON-NLS-1$
                    Translations.getString("PartsTableModel.ColumnName.Package"), //$NON-NLS-1$
                    Translations.getString("PartsTableModel.ColumnName.SpeedPercent"), //$NON-NLS-1$
                    Translations.getString("PartsTableModel.ColumnName.BottomVision"), //$NON-NLS-1$
                    Translations.getString("PartsTableModel.ColumnName.FiducialVision"), //$NON-NLS-1$
                    Translations.getString("PartsTableModel.ColumnName.Placements"), //$NON-NLS-1$
                    Translations.getString("PartsTableModel.ColumnName.Feeders") //$NON-NLS-1$
    };
    private Class[] columnTypes = new Class[] {String.class, String.class, LengthCellValue.class, LengthCellValue.class,
            Package.class, String.class, BottomVisionSettings.class, FiducialVisionSettings.class, Integer.class, Integer.class};
    private List<Part> parts;
    private PercentConverter percentConverter = new PercentConverter();

    public PartsTableModel() {
        Configuration.get().addPropertyChangeListener("parts", this);
        parts = new ArrayList<>(Configuration.get().getParts());
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return (parts == null) ? 0 : parts.size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex >= 1 && columnIndex <= 7;
    }

    @Override
    public Part getRowObjectAt(int index) {
        return parts.get(index);
    }

    @Override
    public int indexOf(Object selectedPart) {
        return parts.indexOf(selectedPart);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            Part part = parts.get(rowIndex);
            if (columnIndex == 1) {
                part.setName((String) aValue);
            }
            else if (columnIndex == 2) {
                LengthCellValue value = (LengthCellValue) aValue;
                value.setDisplayNativeUnits(true);
                Length length = value.getLength();
                Length oldLength = part.getHeight();
                if (oldLength != null) {
                    length = length.changeUnitsIfUnspecified(oldLength.getUnits());
                }
                length = length.changeUnitsIfUnspecified(Configuration.get().getSystemUnits());
                part.setHeight(length);
            }
            else if (columnIndex == 3) {
                LengthCellValue value = (LengthCellValue) aValue;
                value.setDisplayNativeUnits(true);
                Length length = value.getLength();
                Length oldDepth = part.getThroughBoardDepth();
                if (oldDepth != null) {
                    length = length.changeUnitsIfUnspecified(oldDepth.getUnits());
                }
                length = length.changeUnitsIfUnspecified(Configuration.get().getSystemUnits());
                part.setThroughBoardDepth(length);
            }
            else if (columnIndex == 4) {
                part.setPackage((Package) aValue);
            }
            else if (columnIndex == 5) {
                part.setSpeed(percentConverter.convertReverse(aValue.toString()));
            }
            else if (columnIndex == 6) {
                part.setBottomVisionSettings((BottomVisionSettings) aValue);
            }
            else if (columnIndex == 7) {
                part.setFiducialVisionSettings((FiducialVisionSettings) aValue);
            }
        }
        catch (Exception e) {
            // TODO: dialog, bad input
        }
    }

    public Object getValueAt(int row, int col) {
        Part part = parts.get(row);
        switch (col) {
            case 0:
                return part.getId();
            case 1:
                return part.getName();
            case 2:
                return new LengthCellValue(part.getHeight(), true);
            case 3:
                return new LengthCellValue(part.getThroughBoardDepth(), true);
            case 4:
                return part.getPackage();
            case 5:
                return percentConverter.convertForward(part.getSpeed());
            case 6:
                return part.getBottomVisionSettings();
            case 7:
                return part.getFiducialVisionSettings();
            case 8:
                return part.getPlacementCount();
            case 9:
                return part.getAssignedFeeders();
            default:
                return null;
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent arg0) {
        if (arg0.getSource() instanceof Part) {
            //Only single part data changed, so notify the table as to which cell to update
            int rowIdx = indexOf(arg0.getSource());
            switch (arg0.getPropertyName()) {
                case "id":
                    fireTableCellUpdated(rowIdx, 0);
                    break;
                case "name":
                    fireTableCellUpdated(rowIdx, 1);
                    break;
                case "height":
                    fireTableCellUpdated(rowIdx, 2);
                    break;
                case "throughBoardDepth":
                    fireTableCellUpdated(rowIdx, 3);
                    break;
                case "package":
                    fireTableCellUpdated(rowIdx, 4);
                    break;
                case "speed":
                    fireTableCellUpdated(rowIdx, 5);
                    break;
                case "bottomVisionSettings":
                    fireTableCellUpdated(rowIdx, 6);
                    break;
                case "fiducialVisionSettings":
                    fireTableCellUpdated(rowIdx, 7);
                    break;
                case "placementCount":
                    fireTableCellUpdated(rowIdx, 8);
                    break;
                case "assignedFeeders":
                    fireTableCellUpdated(rowIdx, 9);
                    break;
                default:
                    //ok - property is not visible in the table so no need to update the table
            }
        }
        else  {
            // Parts list itself changed
            List<Part> newParts = new ArrayList<>(Configuration.get().getParts());
            
            //Compute the indices of those parts to remove and those to add
            List<int[]> indicesToRemove = new ArrayList<>();
            List<int[]> indicesToAdd = new ArrayList<>();
            Collect.computeInPlaceUpdateIndices(parts, newParts, indicesToRemove, indicesToAdd);

            //Remove the unneeded parts in reverse order so as to not disturb indices of those 
            //parts that are yet to be removed
            for (int[] idxRange : indicesToRemove) {
                for (int idx=idxRange[0]; idx>=idxRange[1]; idx--) {
                    Part part = this.getRowObjectAt(idx);
                    part.removePropertyChangeListener(this);
                    parts.remove(idx);
                }
                fireTableRowsDeleted(idxRange[1], idxRange[0]);
            }
            
            //Insert any needed parts into the table
            for (int[] idxRange : indicesToAdd) {
                for (int idx=idxRange[0]; idx<=idxRange[1]; idx++) {
                    Part part = newParts.get(idx);
                    part.addPropertyChangeListener(this);
                    parts.add(idx, part);
                }
                this.fireTableRowsInserted(idxRange[0], idxRange[1]);
            }
        }
    }
}
