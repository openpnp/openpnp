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
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.openpnp.gui.MultisortTableHeaderCellRenderer;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.Icons;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.Board.Side;
import org.openpnp.model.Solutions;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceMachineSolutionsWizard extends AbstractConfigurationWizard {

    private final ReferenceMachine machine;
    private TableRowSorter tableSorter;
    private AutoSelectTextTable table;

    public ReferenceMachineSolutionsWizard(ReferenceMachine machine) {
        this.machine = machine;

        JPanel panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        //panelGeneral.setBorder(new TitledBorder(null, "Troubleshooting Report", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(20dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.MIN_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("fill:default:grow"),}));

        JButton btnTroubleshoot = new JButton(findSolutionsAction);
        panelGeneral.add(btnTroubleshoot, "2, 2");

        tableSorter = new TableRowSorter<>(machine.getSolutions());
        table = new AutoSelectTextTable(machine.getSolutions()) {
            @Override
            public String getToolTipText(MouseEvent e) {

                java.awt.Point p = e.getPoint();
                int row = rowAtPoint(p);
                int col = columnAtPoint(p);

                if (row >= 0) {
                    row = table.convertRowIndexToModel(row);
                    String tip = machine.getSolutions().getToolTipAt(row, col);
                    if (tip != null) {
                        return tip;
                    }
                }

                return super.getToolTipText();
            }
        };
        table.setRowSorter(tableSorter);
        table.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());

        // Do we want multi-selection? Can be dangerous!
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        //table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); 

        table.setDefaultRenderer(Solutions.Subject.class, new Solutions.SubjectRenderer());
        table.setDefaultRenderer(Solutions.Severity.class, new Solutions.SeverityRenderer());
        table.setDefaultRenderer(Solutions.State.class, new Solutions.StateRenderer());
        JComboBox statesComboBox = new JComboBox(Solutions.State.values());
        table.setDefaultEditor(Solutions.State.class, new DefaultCellEditor(statesComboBox));

        btnAccept = new JButton(acceptSolutionAction);
        panelGeneral.add(btnAccept, "6, 2");

        btnDismiss = new JButton(dismissSolutionAction);
        panelGeneral.add(btnDismiss, "8, 2");

        btnUndo = new JButton(undoSolutionAction);
        panelGeneral.add(btnUndo, "10, 2");

        JLabel label = new JLabel(" ");
        panelGeneral.add(label, "12, 2");

        btnInfo = new JButton(infoAction);
        panelGeneral.add(btnInfo, "14, 2");

        labelWarn = new JLabel("After each round of solving issues, please run Find Issues & Solutions again to catch dependent issues.");
        labelWarn.setForeground(Color.DARK_GRAY);
        labelWarn.setVisible(false);
        panelGeneral.add(labelWarn, "2, 4, 12, 1");
        
        label_1 = new JLabel(" ");
        panelGeneral.add(label_1, "14, 4");


        JScrollPane scrollPane = new JScrollPane(table);
        panelGeneral.add(scrollPane, "2, 6, 13, 1");

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
        table.getColumnModel().getColumn(2).setPreferredWidth(300);
        table.getColumnModel().getColumn(3).setPreferredWidth(300);
        table.getColumnModel().getColumn(4).setPreferredWidth(50);
    }
    private List<Solutions.Issue> getSelections() {
        List<Solutions.Issue> selections = new ArrayList<>();
        for (int selectedRow : table.getSelectedRows()) {
            selectedRow = table.convertRowIndexToModel(selectedRow);
            selections.add(machine.getSolutions().getIssue(selectedRow));
        }
        return selections;
    }

    @Override
    public void createBindings() {
        enableActions();
    }

    protected void enableActions() {
        List<Solutions.Issue> issues = getSelections();
        boolean needAccept = false;
        boolean needDismiss = false;
        boolean needUndo = false;
        boolean needInfo = false;
        for (Solutions.Issue issue : issues) {
            if (issue.getState() != Solutions.State.Dismissed) {
                needDismiss = true;
            }
            if (issue.getState() != Solutions.State.Open) {
                needUndo = true;
            }
            if (issue.canBeAutoSolved()) {
                if (issue.getState() != Solutions.State.Solved) {
                    needAccept = true;
                }
            }
            if (issue.getUri() != null) {
                needInfo = true;
            }
        }
        btnAccept.setEnabled(needAccept);
        btnDismiss.setEnabled(needDismiss);
        btnUndo.setEnabled(needUndo);
        btnInfo.setEnabled(needInfo);
    }

    private Action findSolutionsAction =
            new AbstractAction("Find Issues & Solutions", Icons.solutions) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>Find Issues and Solutions for your machine.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.messageBoxOnException(() -> {
                List<Solutions.Issue> issues = new ArrayList<>(); 
                machine.findIssues(issues);
                SwingUtilities.invokeLater(() -> {
                    machine.setSolutionsIssues(issues);
                    labelWarn.setVisible(false);
                    enableActions();
                });
            });
        }
    };

    private Action acceptSolutionAction =
            new AbstractAction("Accept", Icons.accept) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>Accept the solutions and apply any changes.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.messageBoxOnException(() -> { 
                List<Solutions.Issue> issues = getSelections();
                for (Solutions.Issue issue : issues) {
                    if (issue.canBeAutoSolved() ) {
                        if (issue.getState() != Solutions.State.Solved) {
                            issue.setState(Solutions.State.Solved);
                        }
                    }
                    else {
                        // Be tolerant, we handle a PlainIssue with no auto-solution as dismissal.
                        if (issue.getState() != Solutions.State.Dismissed) {
                            issue.setState(Solutions.State.Dismissed);
                        }
                    }
                }
                enableActions();
                labelWarn.setVisible(true);
            });
        }
    };
    private Action dismissSolutionAction =
            new AbstractAction("Dismiss", Icons.dismiss) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>Dismiss the solutions. If the solution has previously applied any changes, <strong>undo</strong> them.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.messageBoxOnException(() -> { 
                List<Solutions.Issue> issues = getSelections();
                for (Solutions.Issue issue : issues) {
                    if (issue.getState() != Solutions.State.Dismissed) {
                        issue.setState(Solutions.State.Dismissed);
                    }
                }
                enableActions();
            });
        }
    };    
    private Action undoSolutionAction =
            new AbstractAction("Undo", Icons.undo) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>Re-open the solution. If the solution has previously applied any changes, <strong>undo</strong> them.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.messageBoxOnException(() -> { 
                List<Solutions.Issue> issues = getSelections();
                for (Solutions.Issue issue : issues) {
                    if (issue.getState() != Solutions.State.Open) {
                        issue.setState(Solutions.State.Open);
                    }
                }
                enableActions();
            });
        }
    }; 

    private Action infoAction =
            new AbstractAction("", Icons.info) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>Open the Wiki page with instructions related to the issue and possible solutions.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.messageBoxOnException(() -> { 
                List<Solutions.Issue> issues = getSelections();
                for (Solutions.Issue issue : issues) {
                    if (issue.getUri() != null) {
                        Desktop dt = Desktop.getDesktop();
                        dt.browse(new URI(issue.getUri()));;
                    }
                }
            });
        }
    };
    private JButton btnAccept;
    private JButton btnDismiss;
    private JButton btnUndo;
    private JButton btnInfo;
    private JLabel labelWarn;
    private JLabel label_1;
}
