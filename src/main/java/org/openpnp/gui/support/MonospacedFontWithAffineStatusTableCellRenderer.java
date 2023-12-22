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
import javax.swing.table.DefaultTableCellRenderer;
import org.openpnp.gui.tablemodel.PlacementsHolderLocationsTableModel;
import org.openpnp.model.PlacementsHolderLocation.PlacementsTransformStatus;

import java.awt.*;

/**
 * Renders a table cell using a mono-spaced font. Useful for displaying numerical values with their
 * decimal points aligned in a column. If the table model is of type
 * {@link PlacementsHolderLocationsTableModel}, the background cell color is changed based on the 
 * status of the {@link PlacementsHolderLocation}'s placement transform.
 */
@SuppressWarnings("serial")
public class MonospacedFontWithAffineStatusTableCellRenderer extends DefaultTableCellRenderer
{

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        Color rowColor = table.getBackground();
        Color globalColor = new Color(157, 255, 168);
        Color localColor = new Color(157, 188, 255);
        Color alternateRowColor = UIManager.getColor("Table.alternateRowColor");
        Color alternateGlobalColor = new Color(141, 230, 151);
        Color alternateLocalColor = new Color(141, 169, 230);
        if (alternateRowColor == null) {
            alternateRowColor = rowColor;
            alternateGlobalColor = globalColor;
            alternateLocalColor = localColor;
        }
        Color foreground = table.getForeground();
        Color background = row%2==0 ? rowColor : alternateRowColor;
        if (isSelected) {
            foreground = table.getSelectionForeground();
            background = table.getSelectionBackground();
        }
        try {
            PlacementsTransformStatus transformStatus = ((PlacementsHolderLocationsTableModel) table.getModel()).getPlacementsHolderLocation(table.convertRowIndexToModel(row)).getPlacementsTransformStatus();
            if (transformStatus == PlacementsTransformStatus.GloballySet) {
                background = row%2==0 ? globalColor : alternateGlobalColor;
                if (isSelected) {
                    background = background.darker();
                }
            }
            else if (transformStatus == PlacementsTransformStatus.LocallySet) {
                background = row%2==0 ? localColor : alternateLocalColor;
                if (isSelected) {
                    background = background.darker();
                }
            }
        }
        catch (Exception ex) {
            //do nothing
        }
        setForeground(foreground);
        setBackground(background);
        setFont(new Font( "Monospaced", Font.BOLD, super.getFont().getSize()));
        return this;
    }
}
