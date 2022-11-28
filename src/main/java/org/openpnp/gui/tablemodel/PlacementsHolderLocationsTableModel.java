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

import java.util.List;
import java.util.Locale;

import javax.swing.event.TableModelEvent;
import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.gui.support.RotationCellValue;
import org.openpnp.model.Abstract2DLocatable.Side;
import org.openpnp.model.Configuration;
import org.openpnp.model.PlacementsHolderLocation;
import org.openpnp.model.Job;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.PanelLocation;

@SuppressWarnings("serial")
public class PlacementsHolderLocationsTableModel extends AbstractObjectTableModel 
        implements ColumnAlignable, ColumnWidthSaveable {
    private final Configuration configuration;

    private String[] columnNames = new String[] {"Board/Panel Id", "Name", "Width", "Length", 
            "Side", "X", "Y", "Z", "Rot.", "Enabled?", "Check Fids?"};

    @SuppressWarnings("rawtypes")
    private Class[] columnTypes = new Class[] {String.class, String.class, LengthCellValue.class,
            LengthCellValue.class, Side.class, LengthCellValue.class, LengthCellValue.class,
            LengthCellValue.class, RotationCellValue.class, Boolean.class, Boolean.class};

    private int[] columnAlignments = new int[] {LEFT, LEFT, CENTER, CENTER, CENTER, CENTER, 
            CENTER, CENTER, CENTER, CENTER, CENTER};
    
    private int[] columnWidthTypes = new int[] {FIXED, PROPORTIONAL, FIXED, FIXED, FIXED, 
            FIXED, FIXED, FIXED, FIXED, FIXED, FIXED};

    private Job job;

    private PanelLocation rootPanelLocation;

    private List<PlacementsHolderLocation<?>> placementsHolderLocations;

    public PlacementsHolderLocationsTableModel(Configuration configuration) {
        this.configuration = configuration;
    }

    public void setJob(Job job) {
        this.job = job;
        if (job != null) {
            rootPanelLocation = job.getRootPanelLocation();
        }
        else {
            rootPanelLocation = null;
        }
        fireTableDataChanged();
    }

    public Job getJob() {
        return job;
    }

    public PanelLocation getRootPanelLocation() {
        return rootPanelLocation;
    }

    public void setRootPanelLocation(PanelLocation rootPanelLocation) {
        this.rootPanelLocation = rootPanelLocation;
        fireTableDataChanged();
    }

    public List<PlacementsHolderLocation<?>> getPlacementsHolderLocations() {
        if (job != null) {
            return job.getBoardAndPanelLocations();
        }
        else {
            return placementsHolderLocations;
        }
    }

    public void setPlacementsHolderLocations(List<PlacementsHolderLocation<?>> placementsHolderLocations) {
        this.placementsHolderLocations = placementsHolderLocations;
        fireTableDataChanged();
    }

    public PlacementsHolderLocation<?> getPlacementsHolderLocation(int index) {
        return getPlacementsHolderLocations().get(index);
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public int getColumnIndex(String columnName) {
        for (int i=0; i<columnNames.length; i++) {
            if (columnName.equals(columnNames[i])) {
                return i;
            }
        }
        return -1;
    }
    
    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        if (getPlacementsHolderLocations() == null) {
            return 0;
        }
        return getPlacementsHolderLocations().size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex == 0 && (getPlacementsHolderLocation(rowIndex).getParent() == rootPanelLocation)) {
            return true;
        }
        if (columnIndex == 1) {
            return false;
        }
        if ((getPlacementsHolderLocation(rowIndex).getParent() == rootPanelLocation) ||
                (columnIndex == 9) || (columnIndex == 10)) {
            if (((columnIndex == 2) || (columnIndex == 3)) &&
                    (job.instanceCount(getPlacementsHolderLocation(rowIndex).getPlacementsHolder()) > 1)) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            PlacementsHolderLocation<?> placementsHolderLocation = getPlacementsHolderLocation(rowIndex);
            if (columnIndex == 0) {
                String newUniqueId;
                if (placementsHolderLocation.getParent() != null && placementsHolderLocation.getParent().getUniqueId() != null) {
                    newUniqueId = placementsHolderLocation.getParent().getUniqueId() + PlacementsHolderLocation.ID_DELIMITTER + (String) aValue;
                }
                else {
                    newUniqueId = (String) aValue;
                }
                for (int idx = 1; idx < getRowCount(); idx++) {
                    if (idx != rowIndex && getPlacementsHolderLocation(idx).getUniqueId().equals(newUniqueId)) {
                        return;
                    }
                }
                placementsHolderLocation.getDefinition().setId((String) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 1) {
                placementsHolderLocation.getPlacementsHolder().getDefinition().setName((String) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 2) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location dims = placementsHolderLocation.getPlacementsHolder().getDimensions();
                dims = Length.setLocationField(configuration, dims, length, Length.Field.X);
                placementsHolderLocation.getPlacementsHolder().getDefinition().setDimensions(dims);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 3) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location dims = placementsHolderLocation.getPlacementsHolder().getDimensions();
                dims = Length.setLocationField(configuration, dims, length, Length.Field.Y);
                placementsHolderLocation.getPlacementsHolder().getDefinition().setDimensions(dims);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 4) {
                Side oldSide = placementsHolderLocation.getGlobalSide();
                Side newSide = (Side) aValue;
                if (newSide != oldSide) {
                    Location savedLocation = placementsHolderLocation.getGlobalLocation();
                    placementsHolderLocation.setGlobalSide(newSide);
                    if (placementsHolderLocation.getParent() == rootPanelLocation) {
                        placementsHolderLocation.setGlobalLocation(savedLocation);
                    }
                    placementsHolderLocation.setLocalToParentTransform(null);
                }
                fireDecendantsCellUpdated(rowIndex, TableModelEvent.ALL_COLUMNS);
            }
            else if (columnIndex == 5) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location location = placementsHolderLocation.getGlobalLocation();
                location = Length.setLocationField(configuration, location, length, Length.Field.X);
                placementsHolderLocation.setGlobalLocation(location);
                fireDecendantsCellUpdated(rowIndex, TableModelEvent.ALL_COLUMNS);
            }
            else if (columnIndex == 6) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location location = placementsHolderLocation.getGlobalLocation();
                location = Length.setLocationField(configuration, location, length, Length.Field.Y);
                placementsHolderLocation.setGlobalLocation(location);
                fireDecendantsCellUpdated(rowIndex, TableModelEvent.ALL_COLUMNS);
            }
            else if (columnIndex == 7) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location location = placementsHolderLocation.getGlobalLocation();
                location = Length.setLocationField(configuration, location, length, Length.Field.Z);
                placementsHolderLocation.setGlobalLocation(location);
                fireDecendantsCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 8) {
                Location location = placementsHolderLocation.getGlobalLocation();
                double rotation = ((RotationCellValue) aValue).getRotation();
                placementsHolderLocation.setLocation(location.derive(null, null, null, rotation));
                fireDecendantsCellUpdated(rowIndex, TableModelEvent.ALL_COLUMNS);
            }
            else if (columnIndex == 9) {
                placementsHolderLocation.setLocallyEnabled((Boolean) aValue);
                fireDecendantsCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 10) {
                placementsHolderLocation.setCheckFiducials((Boolean) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
        catch (Exception e) {
            // TODO: dialog, bad input
            e.printStackTrace();
        }
    }

    public void fireDecendantsUpdated(PlacementsHolderLocation<?> placementsHolderLocation) {
        fireDecendantsCellUpdated(placementsHolderLocation, TableModelEvent.ALL_COLUMNS);
    }
    
    public void fireDecendantsCellUpdated(PlacementsHolderLocation<?> placementsHolderLocation, int columnIndex) {
        int idx = indexOf(placementsHolderLocation);
        if (idx >= 0) {
            fireDecendantsCellUpdated(idx, columnIndex);
        }
    }

    private void fireDecendantsCellUpdated(int rowIndex, int columnIndex) {
        PlacementsHolderLocation<?> fll = getPlacementsHolderLocation(rowIndex);
        int endRow = rowIndex;
        if (fll instanceof PanelLocation) {
            List<PlacementsHolderLocation<?>> list = ((PanelLocation) fll).getChildren();
            endRow = indexOf(list.get(list.size()-1));
        }
        fireTableChanged(new TableModelEvent(this, rowIndex, endRow, columnIndex));
    }
    
    public Object getValueAt(int row, int col) {
        PlacementsHolderLocation<?> placementsHolderLocation = getPlacementsHolderLocation(row);
        Location loc = placementsHolderLocation.getGlobalLocation();
        Location dim = placementsHolderLocation.getPlacementsHolder().getDimensions();
        switch (col) {
            case 0:
                return placementsHolderLocation.getUniqueId();
            case 1:
                return placementsHolderLocation.getPlacementsHolder().getName();
            case 2:
                return new LengthCellValue(dim.getLengthX(), true, true);
            case 3:
                return new LengthCellValue(dim.getLengthY(), true, true);
            case 4:
                return placementsHolderLocation.getGlobalSide();
            case 5:
                return new LengthCellValue(loc.getLengthX(), true, true);
            case 6:
                return new LengthCellValue(loc.getLengthY(), true, true);
            case 7:
                return new LengthCellValue(loc.getLengthZ(), true, true);
            case 8:
                return new RotationCellValue(loc.getRotation(), true, true);
            case 9:
                return placementsHolderLocation.isEnabled();
            case 10:
                return placementsHolderLocation.isCheckFiducials();
            default:
                return null;
        }
    }

    @Override
    public Object getRowObjectAt(int index) {
        return getPlacementsHolderLocation(index);
    }

    @Override
    public int[] getColumnAlignments() {
        return columnAlignments;
    }

    @Override
    public int[] getColumnWidthTypes() {
        return columnWidthTypes;
    }
}
