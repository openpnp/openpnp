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

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import org.openpnp.gui.tablemodel.ColumnWidthSaveable;
import org.openpnp.gui.tablemodel.ColumnAlignable;

public class TableUtils {

    /**
     * Sets the horizontal alignment of each of the columns of the specified table..
     * @param tableModel - the table model for the table. Must implement ColumnAlignable.
     * @param table - the table whose columns' horizontal alignment is to be set
     */
    public static void setColumnAlignment(ColumnAlignable tableModel, JTable table) {
        int[] alignments = tableModel.getColumnAlignments();
        for (int iCol=0; iCol<table.getColumnCount(); iCol++) {
            table.getColumnModel().getColumn(iCol).setHeaderRenderer(new CustomAlignmentRenderer<>(
                    table.getTableHeader().getDefaultRenderer(), alignments[iCol]));

            table.getColumnModel().getColumn(iCol).setCellRenderer(new CustomAlignmentRenderer<>(
                    table.getCellRenderer(0, iCol), alignments[iCol]));

        }
    }
    
    /**
     * Installs listeners on each column of the table as well as the table itself that save and 
     * restore the table's column widths. If the table's TableModel implements ColumnWidthSaveable,
     * the restored column widths will be a combination of fixed and proportionally allocated to the
     * table's total width based upon the values returned by the model's getColumnWidthTypes() 
     * method. Otherwise, all columns will be proportionally allocated to the table's total width.
     * @param table - the table whose columns' widths are to be saved/restored
     * @param prefs - the Preferences node where the widths are stored/retrieved
     * @param prefKey - the key prefix for the particular table
     */
    public static void installColumnWidthSavers(JTable table, Preferences prefs, String prefKey) {
        for (int iCol=0; iCol<table.getColumnCount(); iCol++) {
            table.getColumnModel().getColumn(iCol).addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName() == "preferredWidth") {
                        String key = prefKey + ((TableColumn) evt.getSource()).getModelIndex();
                        prefs.putInt(key, (int) evt.getNewValue());
                    }
                }
            });               
        }

        table.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                restoreColumnWidths(table, prefs, prefKey);
            }

        });
    }
    
    private static void restoreColumnWidths(JTable table, Preferences prefs, String prefKey) {
        int nCols = table.getColumnCount();
        int nModelCols = table.getModel().getColumnCount();
        int totalWidth = table.getColumnModel().getTotalColumnWidth();
        int[] widthTypes;
        try {
            widthTypes = ((ColumnWidthSaveable) table.getModel()).getColumnWidthTypes();
        }
        catch (ClassCastException ex) {
            //If that failed, treat all columns as being PROPORTIONAL
            widthTypes = new int[nModelCols];
            for (int i=0; i<nModelCols; i++) {
                widthTypes[i] = ColumnWidthSaveable.PROPORTIONAL;
            }
        }
        int[] prefWidths = new int[nCols];
        int totalFixed = 0;
        int totalProportional = 0;
        int numberProportional = 0;
        //Get the column width saved in the Preferences and compute the sum of the fixed
        //width columns and the sum of the proportional columns 
        for (int iCol=0; iCol<nCols; iCol++) {
            int iModelCol = table.getColumnModel().getColumn(iCol).getModelIndex();
            prefWidths[iCol] = prefs.getInt(prefKey + iModelCol, -1);
            if (prefWidths[iCol] < 0) {
                prefWidths[iCol] = table.getColumnModel().getColumn(iCol).getWidth();
                prefs.putInt(prefKey + iModelCol, prefWidths[iCol]);
            }
            if (widthTypes[iModelCol] == ColumnWidthSaveable.FIXED) {
                totalFixed += prefWidths[iCol];
            }
            if (widthTypes[iModelCol] == ColumnWidthSaveable.PROPORTIONAL) {
                totalProportional += prefWidths[iCol];
                numberProportional++;
            }
        }
        //If none of the columns were marked as being proportionally allocated, treat all
        //columns as being proportionally allocated
        if (totalProportional == 0) {
            totalProportional = totalFixed;
            totalFixed = 0;
        }
        //If the total width of the fixed columns is greater than the width of the table, adjust
        //the total width to allow for a positive width for each of the proportional columns  
        if (totalFixed > totalWidth) {
            totalWidth = totalFixed + 5*numberProportional;
        }
        //Compute the scale factor to be applied to each of the proportional columns
        double scale = (totalWidth - totalFixed) / (double) totalProportional;
        int[] newWidths = new int[nCols];
        int newTotalWidth = 0;
        //Compute the new width of each columns
        for (int iCol=0; iCol<nCols; iCol++) {
            int iModelCol = table.getColumnModel().getColumn(iCol).getModelIndex();
            if (widthTypes[iModelCol] == ColumnWidthSaveable.PROPORTIONAL || totalFixed == 0) {
                newWidths[iCol] = (int) (prefWidths[iCol] * scale);
            }
            else {
                newWidths[iCol] = prefWidths[iCol];
            }
            newTotalWidth += newWidths[iCol];
        }
        int i = 0;
        //Due to truncation, the total of the new columns widths may not sum to the total table
        //width so spread the excess evenly across as many columns as necessary to make-up for
        //the difference
        while (newTotalWidth < totalWidth) {
            int iModelCol = table.getColumnModel().getColumn(i).getModelIndex();
            if (widthTypes[iModelCol] == ColumnWidthSaveable.PROPORTIONAL || totalFixed == 0) {
                newWidths[i]++;
                newTotalWidth++;
            }
            i = (i+1) % nCols;
        }
        
        SwingUtilities.invokeLater(() -> {
            //Set the preferred width of the columns
            for (int iCol=0; iCol<nCols; iCol++) {
                table.getColumnModel().getColumn(iCol).setPreferredWidth(newWidths[iCol]);
            }
        });
    }
}
