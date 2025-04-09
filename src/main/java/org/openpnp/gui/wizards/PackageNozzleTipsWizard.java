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

package org.openpnp.gui.wizards;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.openpnp.Translations;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.spi.NozzleTip;
import java.awt.BorderLayout;

@SuppressWarnings("serial")
public class PackageNozzleTipsWizard extends AbstractConfigurationWizard /*JPanel*/ {
    private final org.openpnp.model.Package pkg;
    private JTable table;
    private JScrollPane scrollPane;

    public PackageNozzleTipsWizard(org.openpnp.model.Package pkg) {
        this.pkg = pkg;
        createUi();
    }
    private void createUi() {
        table = new JTable(new NozzleTipsTableModel());
        contentPanel.add(table);
    }

    public class NozzleTipsTableModel extends AbstractTableModel {
        private String[] columnNames = new String[] {
                Translations.getString("NozzleTipsTableModel.ColumnName.NozzleTip"), //$NON-NLS-1$
                Translations.getString("NozzleTipsTableModel.ColumnName.Compatible") }; //$NON-NLS-1$
        private Class[] columnClasses = new Class[] { String.class, Boolean.class };
        private List<NozzleTip> nozzleTips;

        public NozzleTipsTableModel() {
            refresh();
        }

        public void refresh() {
            nozzleTips = new ArrayList<>(Configuration.get().getMachine().getNozzleTips());
            fireTableDataChanged();
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnClasses[columnIndex];
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return nozzleTips.size();
        }

        public NozzleTip getNozzleTip(int index) {
            return nozzleTips.get(index);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1;
        }

        public Object getValueAt(int row, int col) {
            NozzleTip nt = getNozzleTip(row);
            switch (col) {
                case 0:
                    return nt.getName();
                case 1:
                    return pkg.getCompatibleNozzleTips().contains(nt);
                default:
                    return null;
            }
        }
        
        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            NozzleTip nt = getNozzleTip(rowIndex);
            if (columnIndex == 1) {
                if ((Boolean) aValue) {
                    pkg.addCompatibleNozzleTip(nt);
                }
                else {
                    pkg.removeCompatibleNozzleTip(nt);
                }
            }
        }
    }

    @Override
    public void createBindings() {
        //Nothing to bind
    }
}
