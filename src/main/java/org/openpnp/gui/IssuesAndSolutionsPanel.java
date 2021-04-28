/*
 * Copyright (C) 2021 <mark@makr.zone>
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

package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.components.IssuePanel;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.Icons;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.Configuration;
import org.openpnp.model.Solutions;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class IssuesAndSolutionsPanel extends JPanel {
    private static final String PREF_DIVIDER_POSITION = "IssuesAndSolutionsPanel.dividerPosition";
    private static final int PREF_DIVIDER_POSITION_DEF = -1;
    private Preferences prefs = Preferences.userNodeForPackage(IssuesAndSolutionsPanel.class);

    final private Configuration configuration;
    final private Frame frame;
    private ReferenceMachine machine;
    private IssuePanel issuePanel;

    public IssuesAndSolutionsPanel(Configuration configuration, Frame frame) {
        this.configuration = configuration;
        this.frame = frame;

        setLayout(new BorderLayout(0, 0));

        JPanel toolbar = new JPanel();
        add(toolbar, BorderLayout.NORTH);
        toolbar.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.LABEL_COMPONENT_GAP_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.LABEL_COMPONENT_GAP_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,}));
        
                labelWarn = new JLabel("After each round of solving issues, please run Find Issues & Solutions again to catch dependent issues.");
                toolbar.add(labelWarn, "5, 3");
                labelWarn.setHorizontalAlignment(SwingConstants.RIGHT);
                labelWarn.setForeground(Color.DARK_GRAY);
                labelWarn.setVisible(false);

        JSplitPane splitPane = new JSplitPane();
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setContinuousLayout(true);
        splitPane
                .setDividerLocation(prefs.getInt(PREF_DIVIDER_POSITION, PREF_DIVIDER_POSITION_DEF));
        splitPane.addPropertyChangeListener("dividerLocation", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                prefs.putInt(PREF_DIVIDER_POSITION, splitPane.getDividerLocation());
            }
        });
        add(splitPane, BorderLayout.CENTER);

        issuePane = new JPanel();
        splitPane.setRightComponent(issuePane);
        issuePane.setLayout(new BorderLayout(0, 0));
        
        JPanel panel = new JPanel();
        issuePane.add(panel, BorderLayout.NORTH);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.LABEL_COMPONENT_GAP_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                ColumnSpec.decode("4dlu:grow"),
                ColumnSpec.decode("53px"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.LABEL_COMPONENT_GAP_COLSPEC,},
            new RowSpec[] {
                FormSpecs.LINE_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.LABEL_COMPONENT_GAP_ROWSPEC,}));
        
        JButton btnAccept = new JButton(acceptSolutionAction);
        panel.add(btnAccept, "3, 2, fill, top");
        
        JButton btnDismiss = new JButton(dismissSolutionAction);
        panel.add(btnDismiss, "5, 2, fill, top");
        
        JButton btnUndo = new JButton(undoSolutionAction);
        panel.add(btnUndo, "7, 2, fill, top");
        
        JButton btnInfo = new JButton(infoAction);
        panel.add(btnInfo, "9, 2, fill, top");

        configuration.addListener(new ConfigurationListener() {
            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {}

            @Override
            public void configurationComplete(Configuration configuration) throws Exception {
                machine = (ReferenceMachine)configuration.getMachine();
                Solutions solutions = machine.getSolutions();
                tableSorter = new TableRowSorter<>(solutions);
                table = new AutoSelectTextTable(solutions) {
                    @Override
                    public String getToolTipText(MouseEvent e) {

                        java.awt.Point p = e.getPoint();
                        int row = rowAtPoint(p);
                        int col = columnAtPoint(p);

                        if (row >= 0) {
                            row = table.convertRowIndexToModel(row);
                            String tip = solutions.getToolTipAt(row, col);
                            if (tip != null) {
                                return tip;
                            }
                        }

                        return super.getToolTipText();
                    }
                };
                table.setRowSorter(tableSorter);
                table.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());
                Solutions.applyTableUi(table);
                table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                    @Override
                    public void valueChanged(ListSelectionEvent e) {
                        if (e.getValueIsAdjusting()) {
                            return;
                        }

                        selectionActions();
                    }
                });
                table.getColumnModel().getColumn(0).setPreferredWidth(150);
                table.getColumnModel().getColumn(1).setPreferredWidth(50);
                table.getColumnModel().getColumn(2).setPreferredWidth(300);
                table.getColumnModel().getColumn(3).setPreferredWidth(300);
                table.getColumnModel().getColumn(4).setPreferredWidth(50);
                JScrollPane scrollPane = new JScrollPane(table);
                splitPane.setLeftComponent(scrollPane);
            }
        });
        
        JButton btnFindSolutions = new JButton(findSolutionsAction);
        toolbar.add(btnFindSolutions, "3, 3, fill, top");
    }

    private List<Solutions.Issue> getSelections() {
        List<Solutions.Issue> selections = new ArrayList<>();
        for (int selectedRow : table.getSelectedRows()) {
            selectedRow = table.convertRowIndexToModel(selectedRow);
            selections.add(machine.getSolutions().getIssue(selectedRow));
        }
        return selections;
    }

    private Solutions.Issue getSelection() {
        List<Solutions.Issue> selections = getSelections();
        if (selections.size() != 1) {
            return null;
        }
        return selections.get(0);
    }

    protected void selectionActions() {
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
        acceptSolutionAction.setEnabled(needAccept && issues.size() == 1);
        dismissSolutionAction.setEnabled(needDismiss);
        undoSolutionAction.setEnabled(needUndo);
        infoAction.setEnabled(needInfo);
        if (issuePanel != null) {
            issuePane.remove(issuePanel);
        }
        if (issues.size() == 1) {
            issuePanel = new IssuePanel(issues.get(0), machine);
            issuePane.add(issuePanel, BorderLayout.CENTER);
        }
        issuePane.revalidate();
        issuePane.repaint();
    }

    private Action findSolutionsAction =
            new AbstractAction("Find Issues & Solutions", Icons.solutions) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>Find Issues and Solutions for your machine.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                machine.getSolutions().findIssues();
                SwingUtilities.invokeLater(() -> {
                    machine.getSolutions().publishIssues();
                    labelWarn.setVisible(false);
                    selectionActions();
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
                selectionActions();
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
            UiUtils.messageBoxOnException(() -> { 
                List<Solutions.Issue> issues = getSelections();
                for (Solutions.Issue issue : issues) {
                    if (issue.getState() != Solutions.State.Dismissed) {
                        issue.setState(Solutions.State.Dismissed);
                    }
                }
                selectionActions();
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
            UiUtils.messageBoxOnException(() -> { 
                List<Solutions.Issue> issues = getSelections();
                for (Solutions.Issue issue : issues) {
                    if (issue.getState() != Solutions.State.Open) {
                        issue.setState(Solutions.State.Open);
                    }
                }
                selectionActions();
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
    private ActionGroup singleSelectionActionGroup;
    private ActionGroup multiSelectionActionGroup;
    private TableRowSorter tableSorter;
    private AutoSelectTextTable table;
    private JLabel labelWarn;
    private JPanel issuePane;

}
