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

package org.openpnp.machine.reference.wizards;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.UiUtils;

@SuppressWarnings("serial")
public class ReferenceNozzleCompatibleNozzleTipsWizard extends AbstractConfigurationWizard {
    private final ReferenceNozzle nozzle;
    private JTable table;
    private NozzleTipsTableModel tableModel;
    private JScrollPane scrollPane;

    public ReferenceNozzleCompatibleNozzleTipsWizard(ReferenceNozzle nozzle) {
        this.nozzle = nozzle;
        createUi();
        
        // Ensures that the table gets refreshed if the loaded nozzle tip gets changed.
        nozzle.addPropertyChangeListener(pce -> {
            tableModel.refresh();
        });
    }
    private void createUi() {
        contentPanel.setLayout(new BorderLayout(0, 0));
        scrollPane = new JScrollPane();
        contentPanel.add(scrollPane);
        
        tableModel = new NozzleTipsTableModel();
        table = new JTable(tableModel);
        scrollPane.setViewportView(table);
    }

    @Override
    public void createBindings() {
    }
    
    private NozzleTip getSelection() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            return null;
        }
        selectedRow = table.convertRowIndexToModel(selectedRow);
        return tableModel.getNozzleTip(selectedRow);
    }

        class NozzleTipsTableModel extends AbstractTableModel {
        private String[] columnNames = new String[] { "Nozzle Tip", "Compatible?", "Loaded?" };
        private Class[] columnClasses = new Class[] { String.class, Boolean.class, Boolean.class };
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
            return columnIndex == 1 || columnIndex == 2;
        }

        public Object getValueAt(int row, int col) {
            NozzleTip nt = getNozzleTip(row);
            switch (col) {
                case 0:
                    return nt.getName();
                case 1:
                    return nozzle.getCompatibleNozzleTips().contains(nt);
                case 2:
                    return nozzle.getNozzleTip() == nt;
                default:
                    return null;
            }
        }
        
        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            NozzleTip nt = getNozzleTip(rowIndex);
            if (columnIndex == 1) {
                if ((Boolean) aValue) {
                    nozzle.addCompatibleNozzleTip(nt);
                }
                else {
                    nozzle.removeCompatibleNozzleTip(nt);
                }
            }
            else if (columnIndex == 2) {
                if ((Boolean) aValue) {
                    UiUtils.submitUiMachineTask(() -> {
                        for (Head head : nozzle.getHead().getMachine().getHeads()) {
                            for (Nozzle nozzle : head.getNozzles()) {
                                if (nozzle.getNozzleTip() == nt) {
                                    nozzle.unloadNozzleTip();
                                    break;
                                }
                            }
                        }
                        nozzle.loadNozzleTip(getSelection());
                    });
                }
                else {
                    UiUtils.submitUiMachineTask(() -> {
                        nozzle.unloadNozzleTip();
                    });
                }
            }
        }
    }
}
