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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.table.AbstractTableModel;

import org.openpnp.Translations;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.Cycles;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;

@SuppressWarnings("serial")
public class ReferenceNozzleCompatibleNozzleTipsWizard extends AbstractConfigurationWizard {
    private final ReferenceNozzle nozzle;
    private JTable table;
    private NozzleTipsTableModel tableModel;
    private JScrollPane scrollPane;
    private JPanel buttonPanel;
    private JButton btnClearPart;
    private JButton btnClearTip;

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
        contentPanel.add(scrollPane, BorderLayout.SOUTH);
        
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        contentPanel.add(buttonPanel, BorderLayout.NORTH);

        tableModel = new NozzleTipsTableModel();
        table = new JTable(tableModel);
        scrollPane.setViewportView(table);

        btnClearPart = new JButton(clearPartAction);
        btnClearPart.setToolTipText(Translations.getString("JogControlsPanel.btnClearPart.toolTipText")); //$NON-NLS-1$
        buttonPanel.add(btnClearPart);

        btnClearTip = new JButton(clearTipAction);
        btnClearTip.setToolTipText(Translations.getString("JogControlsPanel.btnClearTip.toolTipText")); //$NON-NLS-1$
        buttonPanel.add(btnClearTip);
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
            private String[] columnNames = new String[]{
                    Translations.getString(
                            "ReferenceNozzleCompatibleNozzleTipsWizard.NozzleTipsTableModel.ColumnName.NozzleTip"), //$NON-NLS-1$
                    Translations.getString(
                            "ReferenceNozzleCompatibleNozzleTipsWizard.NozzleTipsTableModel.ColumnName.Compatible"), //$NON-NLS-1$
                    Translations.getString(
                            "ReferenceNozzleCompatibleNozzleTipsWizard.NozzleTipsTableModel.ColumnName.Loaded" //$NON-NLS-1$
                    )};
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
                        MovableUtils.fireTargetedUserAction(nozzle);
                    });
                }
                else {
                    UiUtils.submitUiMachineTask(() -> {
                        nozzle.unloadNozzleTip();
                        MovableUtils.fireTargetedUserAction(nozzle);
                    });
                }
            }
        }
    }

    public Action clearPartAction = new AbstractAction(Translations.getString("JogControlsPanel.Action.ClearPart")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Cycles.clear(nozzle);
            });
        }
    };

    public Action clearTipAction = new AbstractAction(Translations.getString("JogControlsPanel.Action.ClearTip")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                nozzle.clearNozzleTip();
            });
        }
    };

}
