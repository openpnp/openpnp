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

import org.openpnp.gui.JobPlacementsPanel;
import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.gui.support.PartCellValue;
import org.openpnp.gui.support.RotationCellValue;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.ErrorHandling;
import org.openpnp.model.Placement.Type;
import org.openpnp.spi.Feeder;

public class PlacementsTableModel extends AbstractObjectTableModel {
    final Configuration configuration;

    private String[] columnNames =
            new String[] {"Enabled", "ID", "Part", "Side", "X", "Y", "Rot.", "Type", "Placed", "Status", "Error Handling", "Comments"};

    private Class[] columnTypes = new Class[] {Boolean.class, PartCellValue.class, Part.class, Side.class,
            LengthCellValue.class, LengthCellValue.class, RotationCellValue.class, Type.class,
            Boolean.class, Status.class, ErrorHandling.class, String.class};

    public enum Status {
        Ready,
        MissingPart,
        MissingFeeder,
        ZeroPartHeight,
        Disabled
    }

    private Board board;
    private BoardLocation boardLocation;
    private JobPlacementsPanel jobPlacementsPanel;

    public PlacementsTableModel(Configuration configuration) {
        this.configuration = configuration;
    }
    
    public void setJobPlacementsPanel(JobPlacementsPanel jobPlacementsPanel) {
    	this.jobPlacementsPanel = jobPlacementsPanel;
    }

    public void setBoardLocation(BoardLocation boardLocation) {
        this.boardLocation = boardLocation;
        if (boardLocation == null) {
            this.board = null;
        }
        else {
            this.board = boardLocation.getBoard();
        }
        fireTableDataChanged();
    }

    @Override
    public Placement getRowObjectAt(int index) {
        return board.getPlacements().get(index);
    }

    @Override
    public int indexOf(Object object) {
        return board.getPlacements().indexOf(object);
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return (board == null) ? 0 : board.getPlacements().size();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex != 1 && columnIndex != 9;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            Placement placement = board.getPlacements().get(rowIndex);
            if (columnIndex == 0) {
                placement.setEnabled((Boolean) aValue);
                jobPlacementsPanel.updateActivePlacements();
            }
            else if (columnIndex == 2) {
                placement.setPart((Part) aValue);
                fireTableCellUpdated(rowIndex, 8);
            }
            else if (columnIndex == 3) {
                placement.setSide((Side) aValue);
                jobPlacementsPanel.updateActivePlacements();
            }
            else if (columnIndex == 4) {
                LengthCellValue value = (LengthCellValue) aValue;
                value.setDisplayNativeUnits(true);
                Length length = value.getLength();
                Location location = placement.getLocation();
                location = Length.setLocationField(configuration, location, length, Length.Field.X,
                        true);
                placement.setLocation(location);
            }
            else if (columnIndex == 5) {
                LengthCellValue value = (LengthCellValue) aValue;
                value.setDisplayNativeUnits(true);
                Length length = value.getLength();
                Location location = placement.getLocation();
                location = Length.setLocationField(configuration, location, length, Length.Field.Y,
                        true);
                placement.setLocation(location);
            }
            else if (columnIndex == 6) {
                placement.setLocation(placement.getLocation().derive(null, null, null,
                        Double.parseDouble(aValue.toString())));
            }
            else if (columnIndex == 7) {
                placement.setType((Type) aValue);
                fireTableCellUpdated(rowIndex, 8);
                jobPlacementsPanel.updateActivePlacements();
            }
            else if (columnIndex == 8) {
                boardLocation.setPlaced(placement.getId(), (Boolean) aValue);
                jobPlacementsPanel.updateActivePlacements();
            }
            else if (columnIndex == 10) {
                placement.setErrorHandling((ErrorHandling) aValue);
            }
            else if (columnIndex == 11) {
                placement.setComments((String) aValue);
            }
        }
        catch (Exception e) {
            // TODO: dialog, bad input
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

    public Object getValueAt(int row, int col) {
        Placement placement = board.getPlacements().get(row);
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
            case 7:
                return placement.getType();
            case 8:
                // TODO STOPSHIP: Both of these are huge performance hogs and do not belong
                // in the render process. At the least we should cache this information but it
                // would be better if the information was updated out of band by a listener.
            	jobPlacementsPanel.updateActivePlacements();
            	return boardLocation.getPlaced(placement.getId());
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
}
