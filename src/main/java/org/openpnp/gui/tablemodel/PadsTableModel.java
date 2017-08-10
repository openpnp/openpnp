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

import java.util.Locale;

import javax.swing.table.AbstractTableModel;

import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardPad;
import org.openpnp.model.BoardPad.Type;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;

/*
Needle lengths:
6mm, 1/4"
12mm, 1/2"
24mm, 1"
37mm, 1.5"

Needle gauges:
Spec.      Inner Diameter    Outer Diameter       Colour
14gauge       1.55mm             2.10mm                Olive
15gauge       1.36mm             1.80mm                Amber
16gauge       1.32mm             1.60mm                Clear
16gauge      ---      ---         ---
18gauge       0.84mm             1.27mm                Green
19gauge       0.75mm             1.06mm                Black
20gauge       0.60mm             0.91mm                Pink
21gauge       0.51mm             0.82mm                Dark Purple
22gauge       0.41mm             0.72mm                Blue
23gauge       0.34mm             0.64mm                Orange
24gauge       0.30mm             0.55mm                Purple
25gauge       0.26mm             0.51mm                Red
26gauge       0.23mm             0.45mm               Light Brown
27gauge       0.21mm             0.41mm                Clear

http://www.mectronics.in/pdf/heraeus-adhesive-print/heareus-adhesive-print-DispensingglueBenchmarking.pdf

 Generally, 0.4mm
(16mil) ID double needle tips for 0603, 0805 and SOT 23 components,
Normally  0.7  to 0.8mm diameter dots for 0805, 0603 and SOT23 are recommended by using 0.4mm ID double needles.

0.5mm (20mil) ID double needle
for 1206 and 0.6mm (24mil) ID single needle for IC's are used.
For  bigger  components  (1206),  1mm  to  1.2mm  diameter  dots  are  recommended  by  using  0.6mm  ID
single or double needles.  Dot diameter and height should be consistent.

 */
public class PadsTableModel extends AbstractTableModel {
    final Configuration configuration;

    private String[] columnNames = new String[] {"Name", "Side", "X", "Y", "Rot.", "Type", "NozzleSize"};

    private Class[] columnTypes = new Class[] {String.class, Side.class, LengthCellValue.class,
            LengthCellValue.class, String.class, Type.class, Double.class };

    private Board board;

    public PadsTableModel(Configuration configuration) {
        this.configuration = configuration;
    }

    public void setBoard(Board board) {
        this.board = board;
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
        return (board == null) ? 0 : board.getSolderPastePads().size();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            BoardPad pad = board.getSolderPastePads().get(rowIndex);
            if (columnIndex == 0) {
                pad.setName((String) aValue);
            }
            else if (columnIndex == 1) {
                pad.setSide((Side) aValue);
            }
            else if (columnIndex == 2) {
                LengthCellValue value = (LengthCellValue) aValue;
                value.setDisplayNativeUnits(true);
                Length length = value.getLength();
                Location location = pad.getLocation();
                location = Length.setLocationField(configuration, location, length, Length.Field.X,
                        true);
                pad.setLocation(location);
            }
            else if (columnIndex == 3) {
                LengthCellValue value = (LengthCellValue) aValue;
                value.setDisplayNativeUnits(true);
                Length length = value.getLength();
                Location location = pad.getLocation();
                location = Length.setLocationField(configuration, location, length, Length.Field.Y,
                        true);
                pad.setLocation(location);
            }
            else if (columnIndex == 4) {
                pad.setLocation(pad.getLocation().derive(null, null, null,
                        Double.parseDouble(aValue.toString())));
            }
            else if (columnIndex == 5) {
                pad.setType((Type) aValue);
            }
            else if (columnIndex == 6) {
                pad.setNozzleSize((Double) aValue);
            }

        }
        catch (Exception e) {
            // TODO: dialog, bad input
        }
    }

    public Object getValueAt(int row, int col) {
        BoardPad pad = board.getSolderPastePads().get(row);
        Location loc = pad.getLocation();
        switch (col) {
            case 0:
                return pad.getName();
            case 1:
                return pad.getSide();
            case 2:
                return new LengthCellValue(loc.getLengthX(), true);
            case 3:
                return new LengthCellValue(loc.getLengthY(), true);
            case 4:
                return String.format(Locale.US, configuration.getLengthDisplayFormat(),
                        loc.getRotation());
            case 5:
                return pad.getType();
            case 6:
                return pad.getNozzleSize();
            default:
                return null;
        }
    }
}
