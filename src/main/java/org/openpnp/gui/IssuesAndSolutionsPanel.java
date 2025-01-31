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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableRowSorter;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.openpnp.ConfigurationListener;
import org.openpnp.Translations;
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
    final private MainFrame frame;
    private Solutions solutions;
    private ReferenceMachine machine;
    private IssuePanel issuePanel;

    public IssuesAndSolutionsPanel(Configuration configuration, MainFrame frame) {
        this.configuration = configuration;
        this.frame = frame;

        setLayout(new BorderLayout(0, 0));

        JPanel toolbar = new JPanel();
        add(toolbar, BorderLayout.NORTH);
        toolbar.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.LABEL_COMPONENT_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(20dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.LABEL_COMPONENT_GAP_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,}));

        JLabel lblMilestone = new JLabel(Translations.getString("IssuesAndSolutionsPanel.MilestoneLabel.text")); //$NON-NLS-1$
        lblMilestone.setFont(lblMilestone.getFont().deriveFont(lblMilestone.getFont().getStyle() | Font.BOLD));
        lblMilestone.setToolTipText(Translations.getString("IssuesAndSolutionsPanel.MilestoneLabel.toolTipText")); //$NON-NLS-1$
        toolbar.add(lblMilestone, "4, 3, right, default");

        targetMilestone = new JLabel(" - ");
        targetMilestone.setFont(targetMilestone.getFont().deriveFont(targetMilestone.getFont().getStyle() | Font.BOLD));

        toolbar.add(targetMilestone, "6, 3, fill, default");

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
        issuePane.add(panel, BorderLayout.SOUTH);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
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

        JButton btnUndo = new JButton(reopenSolutionAction);
        panel.add(btnUndo, "7, 2, fill, top");

        JLabel label = new JLabel(" ");
        panel.add(label, "8, 2");

        JButton btnInfo = new JButton(infoAction);
        panel.add(btnInfo, "10, 2, fill, top");

        configuration.addListener(new ConfigurationListener() {
            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {}

            @Override
            public void configurationComplete(Configuration configuration) throws Exception {
                machine = (ReferenceMachine)configuration.getMachine();
                solutions = machine.getSolutions();
                initDataBindings();

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

                        solutionChanged();
                    }
                });
                table.getColumnModel().getColumn(0).setPreferredWidth(150);
                table.getColumnModel().getColumn(1).setPreferredWidth(50);
                table.getColumnModel().getColumn(2).setPreferredWidth(300);
                table.getColumnModel().getColumn(3).setPreferredWidth(300);
                table.getColumnModel().getColumn(4).setPreferredWidth(50);
                JScrollPane scrollPane = new JScrollPane(table);
                splitPane.setLeftComponent(scrollPane);
                solutions.addTableModelListener(new TableModelListener() {

                    @Override
                    public void tableChanged(TableModelEvent e) {
                        solutionChanged();
                    }
                });

                // Execute the first issue search after a delay (the delay prevents a
                // race condition with configuration loading and camera startup).
                new java.util.Timer().schedule( 
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                SwingUtilities.invokeLater(()->findIssuesAndSolutions()); 
                            }
                        },
                        5000
                );
            }
        });

        JButton btnFindSolutions = new JButton(findSolutionsAction);
        toolbar.add(btnFindSolutions, "2, 3, 1, 3, fill, fill");
        
        JLabel lblSolved = new JLabel(Translations.getString("IssuesAndSolutionsPanel.IncludeSolvedLabel.text")); //$NON-NLS-1$
        lblSolved.setToolTipText(Translations.getString("IssuesAndSolutionsPanel.IncludeSolvedLabel.toolTipText")); //$NON-NLS-1$
        toolbar.add(lblSolved, "9, 3, right, default");
        
        showSolved = new JCheckBox("");
        showSolved.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                findIssuesAndSolutions(); 
            }
        });
        toolbar.add(showSolved, "11, 3");
        
        JLabel lblDismissed = new JLabel(Translations.getString("IssuesAndSolutionsPanel.IncludeDismissedLabel.text")); //$NON-NLS-1$
        lblDismissed.setToolTipText(Translations.getString(
                "IssuesAndSolutionsPanel.IncludeDismissedLabel.toolTipText")); //$NON-NLS-1$
        toolbar.add(lblDismissed, "15, 3, right, default");
        
        showDismissed = new JCheckBox("");
        showDismissed.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                findIssuesAndSolutions(); 
            }
        });
        toolbar.add(showDismissed, "17, 3");
        
        JLabel label_2 = new JLabel(" ");
        toolbar.add(label_2, "19, 3");
        
        JButton btnInfoMilestone = new JButton(infoMilestoneAction);
        toolbar.add(btnInfoMilestone, "21, 3, 1, 3");

        label_1 = new JLabel(" - ");
        toolbar.add(label_1, "4, 5, 16, 1");

        labelWarn = new JLabel(Translations.getString("IssuesAndSolutionsPanel.WarnLabel.text")); //$NON-NLS-1$
        labelWarn.setBorder(UIManager.getBorder("ToolTip.border"));
        labelWarn.setBackground(UIManager.getColor("ToolTip.background"));
        toolbar.add(labelWarn, "2, 7, 22, 1, left, default");
        labelWarn.setHorizontalAlignment(SwingConstants.RIGHT);
        labelWarn.setForeground(UIManager.getColor("ToolTip.foreground"));
        labelWarn.setOpaque(true);
        labelWarn.setVisible(false);
        initDataBindings();
    }


    private List<Solutions.Issue> getSelections() {
        List<Solutions.Issue> selections = new ArrayList<>();
        for (int selectedRow : table.getSelectedRows()) {
            selectedRow = table.convertRowIndexToModel(selectedRow);
            selections.add(machine.getSolutions().getIssue(selectedRow));
        }
        return selections;
    }

    protected void selectionActions() {
        List<Solutions.Issue> issues = getSelections();
        boolean needAccept = false;
        boolean needDismiss = false;
        boolean needReopen = false;
        boolean needInfo = false;
        for (Solutions.Issue issue : issues) {
            if (issue.getState() != Solutions.State.Dismissed) {
                needDismiss = true;
            }
            if (issue.getState() == Solutions.State.Dismissed || issue.canBeUndone()) {
                if (issue.getState() != Solutions.State.Open) {
                    needReopen = true;
                }
            }
            if (issue.canBeAccepted()) {
                if (issue.getState() == Solutions.State.Open) {
                    needAccept = true;
                }
            }
            if (issue.getUri() != null) {
                needInfo = true;
            }
        }
        acceptSolutionAction.setEnabled(needAccept && issues.size() == 1);
        dismissSolutionAction.setEnabled(needDismiss);
        reopenSolutionAction.setEnabled(needReopen);
        infoAction.setEnabled(needInfo);
        if (issuePanel != null) {
            issuePane.remove(issuePanel);
            issuePanel.setVisible(false);
            issuePanel = null;
            issuePane.revalidate();
        }
        if (issues.size() == 1) {
            issuePanel = new IssuePanel(issues.get(0), machine);
            issuePane.add(issuePanel, BorderLayout.CENTER);
        }
        issuePane.revalidate();
        issuePane.repaint();
        updateIssueIndicator();
    }

    public void updateIssueIndicator() {
        if (!machine.getSolutions().isShowIndicator()) {
            // See Issue https://github.com/openpnp/openpnp/issues/1199. 
            return;
        }
        Solutions.Severity maxSeverity = Solutions.Severity.None;
        for (Solutions.Issue issue : machine.getSolutions().getIssues()) {
            if (issue.getSeverity().ordinal() >= maxSeverity.ordinal() 
                    && issue.getState() == Solutions.State.Open) {
                maxSeverity = issue.getSeverity();
            }
        }
        JTabbedPane tabs = frame.getTabs();
        if (tabs != null) {
            int index = tabs.indexOfComponent(frame.getIssuesAndSolutionsTab());
            if (index >= 0) {
                if (maxSeverity.ordinal() > Solutions.Severity.Information.ordinal()) {
                    int indicatorUnicode = 0x2B24;
                    Color color = maxSeverity.color;
                    color = saturate(color);
                    //"<html>Issues &amp; Solutions <span style=\"color:#"
                    tabs.setTitleAt(index, Translations.getString(
                            "MainFrame.RightComponent.tabs.IssuesAndSolutionsHtml") //$NON-NLS-1$
                            +String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()) //$NON-NLS-1$
                            +";\">&#"+(indicatorUnicode)+";</span></html>"); //$NON-NLS-1$ //$NON-NLS-2$
                }
                else {
                    tabs.setTitleAt(index, Translations.getString(
                            "MainFrame.RightComponent.tabs.IssuesAndSolutions")); //$NON-NLS-1$
                }
            }
        }
    }

    public Color saturate(Color color) {
        int minChannel = (int)(Math.min(Math.min(color.getRed(), color.getGreen()), color.getBlue())*1);
        int maxChannel = (int)(Math.max(Math.max(color.getRed(), color.getGreen()), color.getBlue())*1);
        double f = 200.0/(maxChannel-minChannel);
        color = new Color((int)((color.getRed()-minChannel)*f), (int)((color.getGreen()-minChannel)*f), (int)((color.getBlue()-minChannel)*f));
        return color;
    }

    public void findIssuesAndSolutions() {
        UiUtils.messageBoxOnException(() -> {
            machine.getSolutions().findIssues();
            machine.getSolutions().publishIssues();
            labelWarn.setVisible(false);
            if (table.getRowCount() > 0) {
                table.setRowSelectionInterval(0, 0);
            }
        });
    }

    protected void notifySolutionsChanged() {
        if (dirty) {
            dirty = false;
            // Reselect the Machine Setup tree path to reload the wizard with potentially different settings and property sheets.
            // Otherwise each and every modified setting would need property change firing support, which is clearly not the case.
            MainFrame.get().getMachineSetupTab().selectCurrentTreePath();
            selectionActions();
            dirty = false;
        }
    }

    private Action findSolutionsAction =
            new AbstractAction(Translations.getString("IssuesAndSolutionsPanel.Action.FindSolution"), //$NON-NLS-1$
                    Icons.solutions) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "IssuesAndSolutionsPanel.Action.FindSolution.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            findIssuesAndSolutions();
        }
    };

    private Action acceptSolutionAction =
            new AbstractAction(Translations.getString("IssuesAndSolutionsPanel.Action.AcceptSolution"), //$NON-NLS-1$
                    Icons.accept) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "IssuesAndSolutionsPanel.Action.AcceptSolution.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> { 
                labelWarn.setVisible(true);
                List<Solutions.Issue> issues = getSelections();
                for (Solutions.Issue issue : issues) {
                    if (issue.canBeAccepted() ) {
                        if (issue.getState() != Solutions.State.Solved) {
                            issue.setStateCall(Solutions.State.Solved);
                        }
                    }
                    else {
                        // Be tolerant, we handle a PlainIssue with no auto-solution as dismissal.
                        if (issue.getState() != Solutions.State.Dismissed) {
                            issue.setStateCall(Solutions.State.Dismissed);
                        }
                    }
                }
            });
        }
    };

    private Action dismissSolutionAction = new AbstractAction(Translations.getString(
                    "IssuesAndSolutionsPanel.Action.DismissSolution"), Icons.dismiss) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "IssuesAndSolutionsPanel.Action.DismissSolution.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> { 
                List<Solutions.Issue> issues = getSelections();
                for (Solutions.Issue issue : issues) {
                    if (issue.getState() != Solutions.State.Dismissed) {
                        issue.setStateCall(Solutions.State.Dismissed);
                    }
                }
            });
        }
    };

    private Action reopenSolutionAction = new AbstractAction(Translations.getString(
            "IssuesAndSolutionsPanel.Action.ReopenSolution"), Icons.undo) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "IssuesAndSolutionsPanel.Action.ReopenSolution.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> { 
                List<Solutions.Issue> issues = getSelections();
                for (Solutions.Issue issue : issues) {
                    if (issue.getState() != Solutions.State.Open) {
                        issue.setStateCall(Solutions.State.Open);
                    }
                }
            });
        }
    }; 

    private Action infoAction =
            new AbstractAction("", Icons.info) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "IssuesAndSolutionsPanel.Action.Info.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            List<Solutions.Issue> issues = getSelections();
            for (Solutions.Issue issue : issues) {
                if (issue.getUri() != null) {
                    UiUtils.browseUri(issue.getUri());
                }
            }
        }
    };

    private Action infoMilestoneAction =
            new AbstractAction("", Icons.info) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "IssuesAndSolutionsPanel.Action.InfoMilestone.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.browseUri("https://github.com/openpnp/openpnp/wiki/Issues-and-Solutions"); 
        }
    };

    private ActionGroup singleSelectionActionGroup;
    private ActionGroup multiSelectionActionGroup;
    private TableRowSorter tableSorter;
    private AutoSelectTextTable table;
    private JLabel labelWarn;
    private JPanel issuePane;
    private JLabel targetMilestone;
    private JLabel label_1;
    private JCheckBox showSolved;
    private JCheckBox showDismissed;
    private boolean dirty = false;

    protected void initDataBindings() {
        BeanProperty<Solutions, String> solutionsBeanProperty = BeanProperty.create("targetMilestone.name");
        BeanProperty<JLabel, String> jComboBoxBeanProperty = BeanProperty.create("text");
        AutoBinding<Solutions, String, JLabel, String> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, solutions, solutionsBeanProperty, targetMilestone, jComboBoxBeanProperty);
        autoBinding.bind();
        //
        BeanProperty<Solutions, String> solutionsBeanProperty_1 = BeanProperty.create("targetMilestone.description");
        AutoBinding<Solutions, String, JLabel, String> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ, solutions, solutionsBeanProperty_1, label_1, jComboBoxBeanProperty);
        autoBinding_1.bind();
        //
        BeanProperty<Solutions, Boolean> solutionsBeanProperty_2 = BeanProperty.create("showSolved");
        BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty = BeanProperty.create("selected");
        AutoBinding<Solutions, Boolean, JCheckBox, Boolean> autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, solutions, solutionsBeanProperty_2, showSolved, jCheckBoxBeanProperty);
        autoBinding_2.bind();
        //
        BeanProperty<Solutions, Boolean> solutionsBeanProperty_3 = BeanProperty.create("showDismissed");
        AutoBinding<Solutions, Boolean, JCheckBox, Boolean> autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, solutions, solutionsBeanProperty_3, showDismissed, jCheckBoxBeanProperty);
        autoBinding_3.bind();
    }


    /**
     * Rebuild the UI as needed, when solutions have changed state, perhaps asynchronously.
     */
    public void solutionChanged() {
        dirty = true;
        SwingUtilities.invokeLater(() -> {
            notifySolutionsChanged();
        });
    }
}
