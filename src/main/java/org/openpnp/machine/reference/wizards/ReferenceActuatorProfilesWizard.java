/*
 * Copyright (C) 2020 <mark@makr.zone>
 * inspired and based on work
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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.MultisortTableHeaderCellRenderer;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceActuatorProfiles;
import org.openpnp.machine.reference.ReferenceActuatorProfiles.Profile;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceActuatorProfilesWizard extends AbstractConfigurationWizard {
    private ReferenceActuator actuator;
    private ReferenceActuatorProfiles actuatorProfiles;
    private JPanel panelInterlock;
    private JSeparator separator;
    private JPanel panelCondition;
    private JLabel lblActuator_1;
    private JLabel lblActuator_2;
    private JLabel lblActuator_3;
    private JLabel lblActuator_4;
    private JLabel lblActuator_5;
    private JLabel lblActuator_6;
    private JComboBox actuator1;
    private JComboBox actuator2;
    private JComboBox actuator3;
    private JComboBox actuator4;
    private JComboBox actuator5;
    private JComboBox actuator6;
    private JScrollPane scrollPane;
    private boolean reloadWizard;
    private TableRowSorter tableSorter;
    private AutoSelectTextTable table;
    private JButton btnAdd;
    private JLabel label;
    private JButton btnDelete;
    private JButton btnUp;
    private JButton btnDown;

    public ReferenceActuatorProfilesWizard(ReferenceActuator actuator, ReferenceActuatorProfiles referenceActuatorProfiles) {
        super();
        this.actuatorProfiles = referenceActuatorProfiles;
        this.actuator = actuator;
        createUi();
    }

    protected void createUi() {
        AbstractMachine machine = (AbstractMachine) Configuration.get().getMachine();
        panelInterlock = new JPanel();
        panelInterlock.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Actuators", TitledBorder.LEADING, TitledBorder.TOP, null));
        contentPanel.add(panelInterlock);
        panelInterlock.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,}));

        lblActuator_1 = new JLabel("Actuator 1");
        panelInterlock.add(lblActuator_1, "2, 2, right, default");

        actuator1 = new JComboBox(new ActuatorsComboBoxModel(actuator.getHead() != null ? actuator.getHead() : machine));
        actuator1.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                reloadWizard = true;
            }
        });
        panelInterlock.add(actuator1, "4, 2, fill, default");

        lblActuator_2 = new JLabel("Actuator 2");
        panelInterlock.add(lblActuator_2, "2, 4, right, default");

        actuator2 = new JComboBox(new ActuatorsComboBoxModel(actuator.getHead() != null ? actuator.getHead() : machine));
        actuator2.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                reloadWizard = true;
            }
        });
        panelInterlock.add(actuator2, "4, 4, fill, default");

        lblActuator_3 = new JLabel("Actuator 3");
        panelInterlock.add(lblActuator_3, "2, 6, right, default");

        actuator3 = new JComboBox(new ActuatorsComboBoxModel(actuator.getHead() != null ? actuator.getHead() : machine));
        actuator3.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                reloadWizard = true;
            }
        });
        panelInterlock.add(actuator3, "4, 6, fill, default");

        lblActuator_4 = new JLabel("Actuator 4");
        panelInterlock.add(lblActuator_4, "2, 8, right, default");

        actuator4 = new JComboBox(new ActuatorsComboBoxModel(actuator.getHead() != null ? actuator.getHead() : machine));
        actuator4.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                reloadWizard = true;
            }
        });
        panelInterlock.add(actuator4, "4, 8, fill, default");

        lblActuator_5 = new JLabel("Actuator 5");
        panelInterlock.add(lblActuator_5, "2, 10, right, default");

        actuator5 = new JComboBox(new ActuatorsComboBoxModel(actuator.getHead() != null ? actuator.getHead() : machine));
        actuator5.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                reloadWizard = true;
            }
        });
        panelInterlock.add(actuator5, "4, 10, fill, default");

        lblActuator_6 = new JLabel("Actuator 6");
        panelInterlock.add(lblActuator_6, "2, 12, right, default");

        actuator6 = new JComboBox(new ActuatorsComboBoxModel(actuator.getHead() != null ? actuator.getHead() : machine));
        actuator6.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                reloadWizard = true;
            }
        });
        panelInterlock.add(actuator6, "4, 12, fill, default");

        panelCondition = new JPanel();
        panelCondition.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Profiles", TitledBorder.LEADING, TitledBorder.TOP, null));
        contentPanel.add(panelCondition);
        panelCondition.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        table = new AutoSelectTextTable(actuatorProfiles) {
            @Override
            public String getToolTipText(MouseEvent e) {

                java.awt.Point p = e.getPoint();
                int row = rowAtPoint(p);
                int col = columnAtPoint(p);

                if (row >= 0) {
                    row = table.convertRowIndexToModel(row);
                    String tip = actuatorProfiles.getToolTipAt(row, col);
                    if (tip != null) {
                        return tip;
                    }
                }

                return super.getToolTipText();
            }
        };
        tableSorter = new TableRowSorter<>(actuatorProfiles);
        table.setRowSorter(tableSorter);
        table.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                enableActions();
            }
        });
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);

        btnAdd = new JButton(addProfileAction);
        panelCondition.add(btnAdd, "2, 2");

        btnDelete = new JButton(deleteProfileAction);
        panelCondition.add(btnDelete, "4, 2");
        
        btnUp = new JButton(permutateUpAction);
        panelCondition.add(btnUp, "6, 2");
        
        btnDown = new JButton(permutateDownAction);
        panelCondition.add(btnDown, "8, 2");

        label = new JLabel(" ");
        panelCondition.add(label, "10, 2");

        scrollPane = new JScrollPane(table);
        panelCondition.add(scrollPane, "2, 4, 9, 1, fill, fill");
    }

    private void enableActions() {
        deleteProfileAction.setEnabled(!getSelections().isEmpty());
        int pos = (getSelections().size() != 1) ? 
                -1 : actuatorProfiles.getProfiles().indexOf(getSelections().get(0));
        permutateUpAction.setEnabled(pos > 0);
        permutateDownAction.setEnabled(pos >= 0 && pos < actuatorProfiles.getProfiles().size()-1);
    }

    @Override
    public void createBindings() {
        AbstractMachine machine = (AbstractMachine) Configuration.get().getMachine();
        NamedConverter<Actuator> actuatorConverter = new NamedConverter<>((actuator.getHead() != null ? 
                actuator.getHead().getActuators() : machine.getActuators())); 

        addWrappedBinding(actuatorProfiles, "actuator1", actuator1, "selectedItem", actuatorConverter);
        addWrappedBinding(actuatorProfiles, "actuator2", actuator2, "selectedItem", actuatorConverter);
        addWrappedBinding(actuatorProfiles, "actuator3", actuator3, "selectedItem", actuatorConverter);
        addWrappedBinding(actuatorProfiles, "actuator4", actuator4, "selectedItem", actuatorConverter);
        addWrappedBinding(actuatorProfiles, "actuator5", actuator5, "selectedItem", actuatorConverter);
        addWrappedBinding(actuatorProfiles, "actuator6", actuator6, "selectedItem", actuatorConverter);

        // Reset
        reloadWizard = false;
        enableActions();
    }

    @Override
    protected void saveToModel() {
        super.saveToModel();
        if (reloadWizard) {
            // Reselect the tree path to reload the wizard with potentially different property sheets. 
            MainFrame.get().getMachineSetupTab().selectCurrentTreePath();
        }
    }

    private List<ReferenceActuatorProfiles.Profile> getSelections() {
        List<ReferenceActuatorProfiles.Profile> selections = new ArrayList<>();
        for (int selectedRow : table.getSelectedRows()) {
            selectedRow = table.convertRowIndexToModel(selectedRow);
            selections.add(actuatorProfiles.get(selectedRow));
        }
        return selections;
    }

    private Action addProfileAction =
            new AbstractAction("", Icons.add) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>Add a new profile.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.messageBoxOnException(() -> { 
                actuatorProfiles.addNew();
            });
        }
    };
    private Action deleteProfileAction =
            new AbstractAction("", Icons.delete) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>Delete the selected profile.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.messageBoxOnException(() -> { 
                for (Profile profile : getSelections()) {
                    actuatorProfiles.delete(profile);
                }
                actuator.fireProfilesChanged();
            });
        }
    };
    private Action permutateUpAction =
            new AbstractAction("", Icons.arrowUp) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>Move the profile up one position.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.messageBoxOnException(() -> { 
                ArrayList<Profile>profiles = actuatorProfiles.getProfiles();
                table.getRowSorter().setSortKeys(null);
                for (Profile profile : getSelections()) {
                    int pos = profiles.indexOf(profile);
                    if (pos > 0) {
                        profiles.set(pos, profiles.get(pos-1));
                        profiles.set(pos-1, profile); 
                        actuatorProfiles.fireTableRowsUpdated(pos-1, pos);
                        table.clearSelection();
                        pos = table.convertRowIndexToView(pos-1);
                        table.addRowSelectionInterval(pos, pos);
                    }
                    break;// We assume it's just single selection
                }
            });
        }
    };
    private Action permutateDownAction =
            new AbstractAction("", Icons.arrowDown) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>Move the profile down one position.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.messageBoxOnException(() -> { 
                ArrayList<Profile>profiles = actuatorProfiles.getProfiles();
                table.getRowSorter().setSortKeys(null);
                for (Profile profile : getSelections()) {
                    int pos = profiles.indexOf(profile);
                    if (pos < profiles.size()-1) {
                        profiles.set(pos, profiles.get(pos+1));
                        profiles.set(pos+1, profile); 
                        actuatorProfiles.fireTableRowsUpdated(pos, pos+1);
                        table.clearSelection();
                        pos = table.convertRowIndexToView(pos+1);
                        table.addRowSelectionInterval(pos, pos);
                    }
                    break;// We assume it's just single selection
                }
            });
        }
    };
}
