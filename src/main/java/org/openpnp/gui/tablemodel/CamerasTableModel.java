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

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.HeadCellValue;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Camera.Looking;
import org.openpnp.spi.Head;

public class CamerasTableModel extends AbstractTableModel {
    final private Configuration configuration;

    private String[] columnNames = new String[] {"Name", "Type", "Looking", "Head"};
    private List<Camera> cameras;

    public CamerasTableModel(Configuration configuration) {
        this.configuration = configuration;
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            public void configurationComplete(Configuration configuration) throws Exception {
                refresh();
            }
        });
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return (cameras == null) ? 0 : cameras.size();
    }

    public Camera getCamera(int index) {
        return cameras.get(index);
    }

    public void refresh() {
        cameras = new ArrayList<>(Configuration.get().getMachine().getCameras());
        for (Head head : Configuration.get().getMachine().getHeads()) {
            cameras.addAll(head.getCameras());
        }
        fireTableDataChanged();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0 || columnIndex == 2 || columnIndex == 3;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 3) {
            return HeadCellValue.class;
        }
        return super.getColumnClass(columnIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            Camera camera = cameras.get(rowIndex);
            if (columnIndex == 0) {
                camera.setName((String) aValue);
            }
            else if (columnIndex == 2) {
                camera.setLooking((Looking) aValue);
            }
            else if (columnIndex == 3) {
                HeadCellValue value = (HeadCellValue) aValue;
                if (camera.getHead() == null) {
                    Configuration.get().getMachine().removeCamera(camera);
                }
                else {
                    camera.getHead().removeCamera(camera);
                }

                if (value.getHead() == null) {
                    Configuration.get().getMachine().addCamera(camera);
                }
                else {
                    value.getHead().addCamera(camera);
                }
                camera.setHead(value.getHead());
            }
        }
        catch (Exception e) {
            // TODO: dialog, bad input
        }
    }

    public Object getValueAt(int row, int col) {
        Camera camera = cameras.get(row);
        Location loc = camera.getLocation();
        switch (col) {
            case 0:
                return camera.getName();
            case 1:
                return camera.getClass().getSimpleName();
            case 2:
                return camera.getLooking();
            case 3:
                return new HeadCellValue(camera.getHead());

            default:
                return null;
        }
    }
}
