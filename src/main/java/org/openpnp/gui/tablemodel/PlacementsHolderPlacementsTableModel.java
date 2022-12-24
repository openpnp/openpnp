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

import org.openpnp.Translations;
import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.gui.support.PartCellValue;
import org.openpnp.gui.support.RotationCellValue;
import org.openpnp.model.Abstract2DLocatable.Side;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Panel;
import org.openpnp.model.PanelLocation;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.ErrorHandling;
import org.openpnp.model.Placement.Type;
import org.openpnp.model.PlacementsHolder;
import org.openpnp.util.Utils2D;

@SuppressWarnings("serial")
public class PlacementsHolderPlacementsTableModel extends AbstractObjectTableModel 
        implements ColumnAlignable, ColumnWidthSaveable {
    private PlacementsHolder<?> placementsHolder = null;

    private String[] columnNames =
            new String[] {Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.Enabled"), //$NON-NLS-1$
                    Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.Id"), //$NON-NLS-1$
                    Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.Part"), //$NON-NLS-1$
                    Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.Side"), //$NON-NLS-1$
                    "X", "Y", //$NON-NLS-1$ //$NON-NLS-2$
                    Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.Rot"), //$NON-NLS-1$
                    Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.Type"), //$NON-NLS-1$
                    Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.ErrorHandling"), //$NON-NLS-1$
                    Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.Comments")}; //$NON-NLS-1$

    @SuppressWarnings("rawtypes")
    private Class[] columnTypes = new Class[] {Boolean.class, PartCellValue.class, Part.class, 
            Side.class, LengthCellValue.class, LengthCellValue.class, RotationCellValue.class, 
            Type.class, ErrorHandling.class, String.class};
    
    private int[] columnAlignments = new int[] {CENTER, LEFT, LEFT, CENTER, CENTER, CENTER, 
            CENTER, CENTER, CENTER, LEFT};

    private int[] columnWidthTypes = new int[] {FIXED, FIXED, PROPORTIONAL, FIXED, FIXED, 
            FIXED, FIXED, FIXED, FIXED, PROPORTIONAL};
    
    private boolean localReferenceFrame = true;

    private PanelLocation parent = null;

    private List<Placement> placements = null;

    private boolean isPanel;
    
    public PlacementsHolder<?> getPlacementsHolder() {
        return placementsHolder;
    }

    public void setPlacementsHolder(PlacementsHolder<?> placementsHolder) {
        this.placementsHolder = placementsHolder;
        isPanel = placementsHolder instanceof Panel;
        fireTableDataChanged();
    }
    
    public void setParentLocation(PanelLocation parent) {
        this.parent = parent;
        fireTableDataChanged();
    }
    
    public void setPlacements(List<Placement> placements) {
        this.placements = placements;
        placementsHolder = null;
        fireTableDataChanged();
    }

    @Override
    public Placement getRowObjectAt(int index) {
        if (placementsHolder != null) {
            int limit = placementsHolder.getPlacements().size();
            if (isPanel && index >= limit) {
                return ((Panel) placementsHolder).getPseudoPlacement(index - limit);
            }
            return placementsHolder.getPlacement(index);
        }
        else {
            return placements.get(index);
        }
    }

    @Override
    public int indexOf(Object object) {
        if (placementsHolder != null) {
            int limit = placementsHolder.getPlacements().size();
            int index = placementsHolder.getPlacements().indexOf(object);
            if (isPanel && index < 0) {
                return ((Panel) placementsHolder).getPseudoPlacements().indexOf((Placement) object) + limit;
            }
            return index;
        }
        else {
            return placements.indexOf(object);
        }
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        if (placementsHolder != null) {
            int count = 0;
            if (isPanel) {
                count = ((Panel) placementsHolder).getPseudoPlacements().size();
            }
            return count + placementsHolder.getPlacements().size();
        }
        return (placements == null) ? 0 : placements.size();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex != 1; //Can't edit the Id
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            Placement placement = getRowObjectAt(rowIndex);
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
                double rotation = ((RotationCellValue) aValue).getRotation();
                placement.setLocation(location.derive(null, null, null, rotation));
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 7) {
                placement.setType((Type) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 8) {
                placement.setErrorHandling((ErrorHandling) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 9) {
                placement.setComments((String) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
        catch (Exception e) {
            // TODO: dialog, bad input
        }
    }

    public Object getValueAt(int row, int col) {
        Placement placement = getRowObjectAt(row);
        Location loc;
        Side side;
        if (localReferenceFrame || parent == null) {
            loc = placement.getLocation();
            side = placement.getSide();
        }
        else {
            loc = Utils2D.calculateBoardPlacementLocation(parent, placement);
            side = placement.getSide().flip(parent.getGlobalSide() == Side.Bottom);
        }
        switch (col) {
			case 0:
				return placement.isEnabled();
            case 1:
                return new PartCellValue(placement.getId());
            case 2:
                return placement.getPart();
            case 3:
                return side;
            case 4:
                return new LengthCellValue(loc.getLengthX(), true, true);
            case 5:
                return new LengthCellValue(loc.getLengthY(), true, true);
            case 6:
                return new RotationCellValue(loc.getRotation(), true, true);
            case 7:
                return placement.getType();
            case 8:
                return placement.getErrorHandling();
            case 9:
                return placement.getComments();
            default:
                return null;
        }
    }

    public void setLocalReferenceFrame(boolean b) {
        localReferenceFrame = b;
        fireTableDataChanged();
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
