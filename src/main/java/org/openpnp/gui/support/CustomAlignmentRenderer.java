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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * A TableCellRenderer that horizontally aligns the the contents of the cell
 * @param <T>
 */
@SuppressWarnings("serial")
public class CustomAlignmentRenderer<T extends TableCellRenderer> extends DefaultTableCellRenderer
{
    private T renderer;
    private int alignment;
    
    public CustomAlignmentRenderer(T renderer) {
        this(renderer, SwingConstants.CENTER);
    }

    public CustomAlignmentRenderer(T renderer, int alignment) {
        this.renderer = renderer;
        this.alignment = alignment;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        Component component = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        try {
            ((JLabel) component).setHorizontalAlignment(alignment);
        }
        catch (ClassCastException ex) {
            //do nothing
        }
        return component;
    }
}
