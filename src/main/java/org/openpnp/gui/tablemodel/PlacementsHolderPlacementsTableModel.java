/*
 * Copyright (C) 2023 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
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

import java.awt.Container;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;

import org.openpnp.Translations;
import org.openpnp.events.DefinitionStructureChangedEvent;
import org.openpnp.events.PlacementChangedEvent;
import org.openpnp.gui.JobPlacementsPanel;
import org.openpnp.gui.MainFrame;
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
import org.openpnp.spi.Feeder;
import org.openpnp.model.PlacementsHolder;
import org.openpnp.model.PlacementsHolderLocation;
import org.openpnp.util.Utils2D;
import com.google.common.eventbus.Subscribe;

@SuppressWarnings("serial")
public class PlacementsHolderPlacementsTableModel extends AbstractObjectTableModel 
        implements ColumnAlignable, ColumnWidthSaveable {
    private PlacementsHolder<?> placementsHolder = null;

    private String[] columnNames =
            new String[] {Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.Enabled"), //$NON-NLS-1$
                    Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.Id"), //$NON-NLS-1$
                    Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.Part"), //$NON-NLS-1$
                    Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.Side"), //$NON-NLS-1$
                    "X", //$NON-NLS-1$ 
                    "Y", //$NON-NLS-1$
                    Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.Rot"), //$NON-NLS-1$
                    Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.Type"), //$NON-NLS-1$
                    "Placed",
                    "Status",
                    Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.ErrorHandling"), //$NON-NLS-1$
                    Translations.getString("PlacementsHolderPlacementsTableModel.ColumnName.Comments")}; //$NON-NLS-1$

    private String[] propertyNames = new String[] {
            "enabled", //$NON-NLS-1$
            "id", //$NON-NLS-1$
            "part", //$NON-NLS-1$
            "side", //$NON-NLS-1$
            "location", //$NON-NLS-1$
            "location", //$NON-NLS-1$
            "location", //$NON-NLS-1$
            "type", //$NON-NLS-1$
            "placed", //$NON-NLS-1$
            "status", //$NON-NLS-1$
            "errorHandling", //$NON-NLS-1$
            "comments" //$NON-NLS-1$
    };
    
    @SuppressWarnings("rawtypes")
    private Class[] columnTypes = new Class[] {Boolean.class, PartCellValue.class, Part.class, 
            Side.class, LengthCellValue.class, LengthCellValue.class, RotationCellValue.class, 
            Type.class, Boolean.class, Status.class, ErrorHandling.class, String.class};
    
    private int[] columnAlignments = new int[] {CENTER, LEFT, LEFT, CENTER, CENTER, CENTER, 
            CENTER, CENTER, CENTER, CENTER, CENTER, LEFT};

    private int[] columnWidthTypes = new int[] {FIXED, FIXED, PROPORTIONAL, FIXED, FIXED, 
            FIXED, FIXED, FIXED, FIXED, FIXED, FIXED, PROPORTIONAL};
    
    public enum Status {
        Ready,
        MissingPart,
        MissingFeeder,
        ZeroPartHeight,
        Disabled
    }

    private boolean localReferenceFrame = true;

    private PanelLocation parent = null;

    private List<Placement> placements = null;

    private PlacementsHolderLocation<?> placementsHolderLocation;
    private JobPlacementsPanel jobPlacementsPanel;
    private boolean editDefinition;
    private boolean isPanel;
    private Configuration configuration;

    private Container container;
    
    public PlacementsHolderPlacementsTableModel(Container container) {
        super();
        this.container = container;
        configuration = Configuration.get();
        configuration.getBus().register(this);
    }
    
    @Subscribe
    public void definitionStructureChangedEventHandler(DefinitionStructureChangedEvent event) {
        if (event.source != this && event.source != container && 
                event.changedName.contentEquals("placements")) {
            SwingUtilities.invokeLater(() -> {
                fireTableDataChanged();
            });
        }
    }

    @Subscribe
    public void placementChangedEventHandler(PlacementChangedEvent evt) {
        if (evt.source != this && evt.placementsHolder == placementsHolder) {
            Placement placement = evt.placement;
            int index = indexOf(placement);
            if (index < 0) {
                for (index = 0; index < getRowCount(); index++) {
                    if (getRowObjectAt(index).getDefinition() == placement) {
                        break;
                    }
                }
            }
            if (index < getRowCount()) {
                final int idx = index;
                SwingUtilities.invokeLater(() -> {
                    fireTableCellUpdated(idx, TableModelEvent.ALL_COLUMNS);
                });
            }
        }
    }
    
    public PlacementsHolder<?> getPlacementsHolder() {
        return placementsHolder;
    }

    public void setPlacementsHolder(PlacementsHolder<?> placementsHolder) {
        this.placementsHolder = placementsHolder;
        isPanel = placementsHolder instanceof Panel;
        fireTableDataChanged();
    }
    
    public void setPlacements(List<Placement> placements) {
        this.placements = placements;
        placementsHolder = null;
        fireTableDataChanged();
    }

    public void setJobPlacementsPanel(JobPlacementsPanel jobPlacementsPanel) {
        this.jobPlacementsPanel = jobPlacementsPanel;
    }

    public void setPlacementsHolderLocation(PlacementsHolderLocation<?> placementsHolderLocation,
            boolean editDefinition) {
        this.placementsHolderLocation = placementsHolderLocation;
        this.editDefinition = editDefinition;
        if (placementsHolderLocation == null) {
            placementsHolder = null;
        }
        else {
            placementsHolder = placementsHolderLocation.getPlacementsHolder();
            isPanel = placementsHolder instanceof Panel;
        }
        fireTableDataChanged();
    }

    @Override
    public Placement getRowObjectAt(int index) {
        if (placementsHolder != null) {
            int limit = placementsHolder.getPlacements().size();
            if (isPanel && index >= limit) {
                int idx = index - limit;
                if (idx < ((Panel) placementsHolder).getPseudoPlacements().size()) {
                    return ((Panel) placementsHolder).getPseudoPlacement(index - limit);
                }
                else {
                    return null;
                }
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
        return columnIndex != 1 && columnIndex != 9; //Can't edit the Id or Status
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
    }
    
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            Placement placement = getRowObjectAt(rowIndex);
            Placement definition = (Placement) placement.getDefinition();
            if (definition == null) {
                definition = placement;
            }
            if (columnIndex == 0) {
                if (editDefinition) {
                    definition.setEnabled((Boolean) aValue);
                }
                else {
                    placement.setEnabled((Boolean) aValue);
                }
                fireTableCellUpdated(rowIndex, columnIndex);
                if (jobPlacementsPanel != null) {
                    jobPlacementsPanel.updateActivePlacements();
                }
            }
            else if (columnIndex == 2) {
                definition.setPart((Part) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 3) {
                definition.setSide((Side) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
                if (jobPlacementsPanel != null) {
                    jobPlacementsPanel.updateActivePlacements();
                }
            }
            else if (columnIndex == 4) {
                LengthCellValue value = (LengthCellValue) aValue;
                value.setDisplayNativeUnits(true);
                Length length = value.getLength();
                Location oldValue = placement.getLocation();
                Location location = Length.setLocationField(configuration, oldValue, length, Length.Field.X,
                        true);
                definition.setLocation(location);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 5) {
                LengthCellValue value = (LengthCellValue) aValue;
                value.setDisplayNativeUnits(true);
                Length length = value.getLength();
                Location oldValue = placement.getLocation();
                Location location = Length.setLocationField(configuration, oldValue, length, Length.Field.Y,
                        true);
                definition.setLocation(location);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 6) {
                RotationCellValue value = (RotationCellValue) aValue;
                double rotation = value.getRotation();
                Location oldValue = placement.getLocation();
                Location location = oldValue.derive(null, null, null, rotation);
                definition.setLocation(location);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 7) {
                definition.setType((Type) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
                if (jobPlacementsPanel != null) {
                    jobPlacementsPanel.updateActivePlacements();
                }
            }
            else if (columnIndex == 8) {
                jobPlacementsPanel.getJobPanel().getJob()
                    .storePlacedStatus(placementsHolderLocation, placement.getId(), (Boolean) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
                jobPlacementsPanel.updateActivePlacements();
            }
            else if (columnIndex == 10) {
                if (editDefinition) {
                    definition.setErrorHandling((ErrorHandling) aValue);
                }
                else {
                    placement.setErrorHandling((ErrorHandling) aValue);
                }
                fireTableCellUpdated(rowIndex, columnIndex);
             }
            else if (columnIndex == 11) {
                definition.setComments((String) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
        catch (Exception e) {
            // TODO: dialog, bad input
        }
    }

    public void fireTableCellUpdated(Placement placement, String columnName) {
        fireTableCellUpdated(indexOf(placement), findColumn(columnName));
    }
    
    public void fireTableCellUpdated(Placement placement, int columnIndex) {
        fireTableCellUpdated(indexOf(placement), columnIndex);
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
        Configuration.get().getBus().post(new PlacementChangedEvent(placementsHolder, 
                getRowObjectAt(row), propName, null, newValue, this));
    }
    
    public Object getValueAt(int row, int col) {
        Placement placement = getRowObjectAt(row);
        if (placement == null) {
            return null;
        }
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
                // TODO STOPSHIP: Both of these are huge performance hogs and do not belong
                // in the render process. At the least we should cache this information but it
                // would be better if the information was updated out of band by a listener.
                return MainFrame.get().getJobTab().getJob()
                        .retrievePlacedStatus(placementsHolderLocation, placement.getId());
            case 9:
                return getPlacementStatus(placement);
            case 10:
                return placement.getErrorHandling();
            case 11:
                return placement.getComments();
            default:
                return null;
        }
    }

    // TODO: Ideally this would all come from the JobPlanner, but this is a
    // good start for now.
    private Status getPlacementStatus(Placement placement) {
        if (placement.getPart() == null) {
            return Status.MissingPart;
        }
        if (!placement.isEnabled()) {
            return Status.Disabled;
                    
        }
        if (placement.getType() == Placement.Type.Placement && placement.isEnabled()) {
            boolean found = false;
            for (Feeder feeder : Configuration.get().getMachine().getFeeders()) {
                if (feeder.getPart() == placement.getPart() && feeder.isEnabled()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return Status.MissingFeeder;
            }

            if (placement.getPart().isPartHeightUnknown()) {
                return Status.ZeroPartHeight;
            }
        }
        return Status.Ready;
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
