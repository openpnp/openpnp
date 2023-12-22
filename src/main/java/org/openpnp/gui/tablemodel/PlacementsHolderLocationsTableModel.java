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

import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;

import org.openpnp.Translations;
import org.openpnp.events.PlacementsHolderLocationChangedEvent;
import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.gui.support.RotationCellValue;
import org.openpnp.model.Abstract2DLocatable;
import org.openpnp.model.Abstract2DLocatable.Side;
import org.pmw.tinylog.Logger;

import com.google.common.eventbus.Subscribe;

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

    private final String[] columnNames = new String[] {
            Translations.getString("PlacementsHolderLocationsTableModel.ColumnName.BoardPanelId"), //$NON-NLS-1$
            Translations.getString("PlacementsHolderLocationsTableModel.ColumnName.Name"), //$NON-NLS-1$
            Translations.getString("PlacementsHolderLocationsTableModel.ColumnName.Width"), //$NON-NLS-1$
            Translations.getString("PlacementsHolderLocationsTableModel.ColumnName.Length"), //$NON-NLS-1$
            Translations.getString("PlacementsHolderLocationsTableModel.ColumnName.Side"), //$NON-NLS-1$
            "X", "Y", "Z",  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            Translations.getString("PlacementsHolderLocationsTableModel.ColumnName.Rot"), //$NON-NLS-1$
            Translations.getString("PlacementsHolderLocationsTableModel.ColumnName.Enabled"), //$NON-NLS-1$
            Translations.getString("PlacementsHolderLocationsTableModel.ColumnName.CheckFids")}; //$NON-NLS-1$

    private final String[] propertyNames = new String[] { 
            "id",  //$NON-NLS-1$
            "name",  //$NON-NLS-1$
            "dimensions",  //$NON-NLS-1$
            "dimensions",  //$NON-NLS-1$
            "side",  //$NON-NLS-1$
            "location",  //$NON-NLS-1$
            "location",  //$NON-NLS-1$
            "location",  //$NON-NLS-1$
            "location",  //$NON-NLS-1$
            "locallyEnabled",  //$NON-NLS-1$
            "checkFiducials"}; //$NON-NLS-1$
    
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
        configuration.getBus().register(this);
    }

    @Subscribe
    public void placementsHolderLocationChangedEventHandler(PlacementsHolderLocationChangedEvent evt) {
        if (evt.source != this && rootPanelLocation != null) {
            PlacementsHolderLocation<?> phl = evt.placementsHolderLocation;
            int index = indexOf(phl);
            if (index >= 0) {
                final int idx = index;
                SwingUtilities.invokeLater(() -> {
                    fireTableCellDecendantsUpdated(idx, TableModelEvent.ALL_COLUMNS);
                });
            }
            else {
                for (index = 0; index < getRowCount(); index++) {
                    if (((PlacementsHolderLocation<?>) getRowObjectAt(index)).getDefinition() == phl) {
                        final int idx = index;
                        SwingUtilities.invokeLater(() -> {
                            fireTableCellDecendantsUpdated(idx, TableModelEvent.ALL_COLUMNS);
                        });
                    }
                }
            }
        }
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
                Location oldValue = placementsHolderLocation.getPlacementsHolder().getDimensions();
                Location dims = Length.setLocationField(configuration, oldValue, length, Length.Field.X);
                placementsHolderLocation.getPlacementsHolder().getDefinition().setDimensions(dims);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 3) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location oldValue = placementsHolderLocation.getPlacementsHolder().getDimensions();
                Location dims = Length.setLocationField(configuration, oldValue, length, Length.Field.Y);
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
                    fireTableCellDecendantsUpdated(rowIndex, TableModelEvent.ALL_COLUMNS);
                }
            }
            else if (columnIndex == 5) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Length displayedLength = value.getDisplayedLength();
                Location oldValue = placementsHolderLocation.getGlobalLocation();
                Length oldLength = oldValue.getLengthX();
                Length displayedOldLength = new LengthCellValue(oldLength).getDisplayedLength();
                //Check to see if the operator actually changed anything in the cell. The first check
                //catches changes made to the digits to the right of what would ordinarily be displayed
                //and the second check catches changes to the displayed digits 
                if (length.compareTo(displayedLength) != 0 || length.compareTo(displayedOldLength) != 0) {
                    Location location = Length.setLocationField(configuration, oldValue, length, Length.Field.X);
                    placementsHolderLocation.setGlobalLocation(location);
                    fireTableCellDecendantsUpdated(rowIndex, TableModelEvent.ALL_COLUMNS);
                }
            }
            else if (columnIndex == 6) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Length displayedLength = value.getDisplayedLength();
                Location oldValue = placementsHolderLocation.getGlobalLocation();
                Length oldLength = oldValue.getLengthY();
                Length displayedOldLength = new LengthCellValue(oldLength).getDisplayedLength();
                if (length.compareTo(displayedLength) != 0 || length.compareTo(displayedOldLength) != 0) {
                    Location location = Length.setLocationField(configuration, oldValue, length, Length.Field.Y);
                    placementsHolderLocation.setGlobalLocation(location);
                    fireTableCellDecendantsUpdated(rowIndex, TableModelEvent.ALL_COLUMNS);
                }
            }
            else if (columnIndex == 7) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Length displayedLength = value.getDisplayedLength();
                Location oldValue = placementsHolderLocation.getGlobalLocation();
                Length oldLength = oldValue.getLengthZ();
                Length displayedOldLength = new LengthCellValue(oldLength).getDisplayedLength();
                if (length.compareTo(displayedLength) != 0 || length.compareTo(displayedOldLength) != 0) {
                    Location location = Length.setLocationField(configuration, oldValue, length, Length.Field.Z);
                    placementsHolderLocation.setGlobalLocation(location);
                    fireTableCellDecendantsUpdated(rowIndex, TableModelEvent.ALL_COLUMNS);
                }
            }
            else if (columnIndex == 8) {
                RotationCellValue value = (RotationCellValue) aValue;
                double rotation = value.getRotation();
                double displayedRotation = value.getDisplayedRotation();
                Location oldValue = placementsHolderLocation.getGlobalLocation();
                double oldRotation = oldValue.getRotation();
                double displayedOldRotation = new RotationCellValue(oldRotation).getDisplayedRotation();
                if (rotation != displayedRotation || rotation != displayedOldRotation) {
                    Location location = oldValue.derive(null, null, null, rotation);
                    placementsHolderLocation.setGlobalLocation(location);
                    fireTableCellDecendantsUpdated(rowIndex, TableModelEvent.ALL_COLUMNS);
                }
            }
            else if (columnIndex == 9) {
                if ((placementsHolderLocation.getParent() == null) ||
                        placementsHolderLocation.getParent().isEnabled()) {
                    placementsHolderLocation.setLocallyEnabled((Boolean) aValue);
                    fireTableCellDecendantsUpdated(rowIndex, columnIndex);
                }
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

    public void fireTableCellUpdated(PlacementsHolderLocation<?> placementsHolderLocation, String columnName) {
        fireTableCellUpdated(indexOf(placementsHolderLocation), findColumn(columnName));
    }
    
    public void fireTableCellDecendantsUpdated(PlacementsHolderLocation<?> placementsHolderLocation, String columnName) {
        fireTableCellDecendantsUpdated(indexOf(placementsHolderLocation), findColumn(columnName));
    }
    
    public void fireTableCellDecendantsUpdated(PlacementsHolderLocation<?> placementsHolderLocation, int columnIndex) {
        fireTableCellDecendantsUpdated(indexOf(placementsHolderLocation), columnIndex);
    }
    
    @Override
    public void fireTableCellUpdated(int row, int column) {
        super.fireTableCellUpdated(row, column);
        String propName = "ALL";
        Object newValue = null;
        if (column >= 0 && column < propertyNames.length) {
            propName = propertyNames[column];
            newValue = getValueAt(row, column);
        }
        Configuration.get().getBus().post(new PlacementsHolderLocationChangedEvent(
                getPlacementsHolderLocation(row),
                propName,
                null, newValue, this));
    }
    
    protected void fireTableCellDecendantsUpdated(int row, int column) {
        PlacementsHolderLocation<?> placementsHolderLocation = getPlacementsHolderLocation(row);
        int endRow = row;
        if (placementsHolderLocation instanceof PanelLocation) {
            List<PlacementsHolderLocation<?>> list = ((PanelLocation) placementsHolderLocation).
                    getChildren();
            if (list.size() > 0) {
                endRow = indexOf(list.get(list.size()-1));
            }
        }
        fireTableChanged(new TableModelEvent(this, row, endRow, column));
        String propName = "ALL";
        Object newValue = null;
        if (column >= 0 && column < propertyNames.length) {
            propName = propertyNames[column];
            newValue = getValueAt(row, column);
        }
        Configuration.get().getBus().post(new PlacementsHolderLocationChangedEvent(
                getPlacementsHolderLocation(row),
                propName,
                null, newValue, this));
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
