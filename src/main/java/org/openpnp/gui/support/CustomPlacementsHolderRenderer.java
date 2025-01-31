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

package org.openpnp.gui.support;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.UIResource;
import javax.swing.table.TableCellRenderer;

import org.openpnp.gui.tablemodel.PlacementsHolderLocationsTableModel;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.PlacementsHolderLocation;
import java.awt.*;
import java.util.Arrays;

/**
 * A renderer for PlacementsHolderLocation Table cells that displays an icon showing the type of 
 * PlacementsHolderLocation the row of the table contains
 */
@SuppressWarnings("serial")
public class CustomPlacementsHolderRenderer extends JLabel implements TableCellRenderer, UIResource
{
    private static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
    private final JLabel left;
    private final JLabel right;
    
    public CustomPlacementsHolderRenderer() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(true);
        left = new JLabel();
        left.setHorizontalTextPosition(LEFT);
        left.setHorizontalAlignment(LEFT);
        right = new JLabel();
        right.setHorizontalTextPosition(RIGHT);
        right.setHorizontalAlignment(LEFT);
        this.add(left);
        this.add(right);
        setHorizontalAlignment(LEFT);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        if (table == null) {
            return this;
        }
        
        Color alternateRowColor = UIManager.getColor("Table.alternateRowColor");
        if (isSelected) {
            setForeground(table.getSelectionForeground());
            setBackground(table.getSelectionBackground());
            left.setForeground(table.getSelectionForeground());
            left.setBackground(table.getSelectionBackground());
            right.setForeground(table.getSelectionForeground());
            right.setBackground(table.getSelectionBackground());
        }
        else {
            setForeground(table.getForeground());
            setBackground(row%2==0 ? table.getBackground() : alternateRowColor);
            left.setForeground(table.getForeground());
            left.setBackground(row%2==0 ? table.getBackground() : alternateRowColor);
            right.setForeground(table.getForeground());
            right.setBackground(row%2==0 ? table.getBackground() : alternateRowColor);
        }
        
        String uniqueId = value == null ? "" : (String) value;
        String id = uniqueId.substring(uniqueId.lastIndexOf(PlacementsHolderLocation.ID_DELIMITTER)+1);
        int depth = 0;
        int idx = -1;
        while ((idx = uniqueId.indexOf(PlacementsHolderLocation.ID_DELIMITTER, idx+1)) >= 0) {
            depth += 4;
        }
        
        char[] charArray = new char[depth];
        Arrays.fill(charArray, ' ');
        left.setText(new String(charArray));
        right.setText(id);
            
        if (((PlacementsHolderLocationsTableModel) table.getModel()).
                getPlacementsHolderLocation(table.convertRowIndexToModel(row)) instanceof BoardLocation) {
            right.setIcon(Icons.board);
        }
        else {
            right.setIcon(Icons.panel);
        }

        if (hasFocus) {
            setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
        } else {
            setBorder(noFocusBorder);
        }

        return this;
    }
}
