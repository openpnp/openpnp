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

package org.openpnp.model;

import java.awt.Color;
import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.commons.codec.digest.DigestUtils;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.support.Icons;
import org.openpnp.spi.Machine;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.ElementList;

public class Solutions extends AbstractTableModel {

    @ElementList(required = false)
    private Set<String> dismissedSolutions = new HashSet<>();

    public interface Subject {
        /**
         * Report any detected issue and proposed solution in the list. 
         * @param report
         */
        public default void findIssues(Solutions solutions) {
        }
        public default String getSubjectText() {
            if (this instanceof Named) {
                return (this.getClass().getSimpleName()+" "+((Named) this).getName());
            }
            else if (this instanceof Identifiable) {
                return (this.getClass().getSimpleName()+" "+((Identifiable) this).getId());
            }
            else {
                return (this.getClass().getSimpleName());
            }
        }
        public default Icon getSubjectIcon() {
            Icon icon = null;
            if (this instanceof PropertySheetHolder) {
                icon = ((PropertySheetHolder)this).getPropertySheetHolderIcon();
            }
            if (icon == null) {
                icon = Icons.solutions;
            }
            return icon;
        }
    }

    public enum Severity {
        None(new Color(255, 255, 255)), 
        Information(new Color(255, 255, 255)),
        Suggestion(new Color(255, 255, 157)),
        Warning(new Color(255, 220, 157)),
        Error(new Color(255, 157, 157)),
        Fundamental(new Color(200, 220, 255));

        final public Color color;

        Severity(Color color) {
            this.color = color;
        }
    }

    public enum State {
        Open(new Color(255, 255, 255)),
        Solved(new Color(157, 255, 168)),
        Dismissed(new Color(220, 220, 220));

        private Color color;

        State(Color color) {
            this.color = color;
        }
    }

    public static class Choice {
        final private Object value;
        final private Icon icon;
        final private String description;
        public Choice(Object value, String description, Icon icon) {
            super();
            this.value = value;
            this.description = description;
            this.icon = icon;
        }
        public Object getValue() {
            return value;
        }
        public Icon getIcon() {
            return icon;
        }
        public String getDescription() {
            return description;
        }
    }

    public static class Issue extends AbstractModelObject {
        final Subject subject;
        final String issue;
        final String solution;
        final Severity severity;
        final String uri;
        private State state;
        private Object choice;

        public Issue(Subject subject, String issue, String solution, Severity severity, String uri) {
            super();
            this.subject = subject; 
            this.issue = issue;
            this.solution = solution;
            this.severity = severity;
            this.uri = uri;
            if (solution.isEmpty()) {
                state = State.Dismissed;
            }
            else {
                state = State.Open;
            }
        }
        public Subject getSubject() {
            return subject;
        }
        public String getIssue() {
            return issue;
        }
        public String getSolution() {
            return solution;
        }
        public Severity getSeverity() {
            if (state == State.Dismissed) {
                return Severity.None;
            }
            return severity;
        }
        public String getFingerprint() {
            return DigestUtils.shaHex(subject.getSubjectText()+"\n"+issue+"\n"+solution);
        }

        public State getState() {
            return state;
        }

        public void setState(State state) throws Exception {
            Object oldValue = this.state;
            this.state = state;
            firePropertyChange("state", oldValue, state);
        }

        public Object getChoice() {
            return choice;
        }
        public void setChoice(Object choice) {
            this.choice = choice;
        }

        public void setInitiallyDismissed() {
            this.state = State.Dismissed;
        }
        public String getUri() {
            return uri;
        }
        public boolean canBeAutoSolved() {
            return true;
        }
        public Choice [] getChoices() {
            return new Choice[] {};
        }
    }

    public static class PlainIssue extends Issue {

        public PlainIssue(Subject subject, String issue, String solution, Severity severity,
                String uri) {
            super(subject, issue, solution, severity, uri);
        }

        @Override
        public void setState(State state) throws Exception {
            if (state == State.Solved) {
                Desktop dt = Desktop.getDesktop();
                dt.browse(new URI(uri));
            }
            super.setState(state);
        }
        public boolean canBeAutoSolved() {
            return false;
        }
    }

    private List<Issue> pendingIssues = null;
    private List<Issue> issues = new ArrayList<>();

    public boolean isSolutionsIssueDismissed(Issue issue) {
        return dismissedSolutions.contains(issue.getFingerprint());
    }
    public void setSolutionsIssueDismissed(Issue issue, boolean dismissed) {
        if (dismissed) {
            dismissedSolutions.add(issue.getFingerprint()); 
        }
        else {
            dismissedSolutions.remove(issue.getFingerprint());
        }
    }

    public List<Issue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    public Machine getMachine() {
        Machine machine = Configuration.get().getMachine();
        return machine;
    }

    public synchronized void findIssues() {
        pendingIssues = new ArrayList<>();
        getMachine().findIssues(this);
    }

    public synchronized void add(Issue issue) {
        pendingIssues.add(issue);
    }

    public synchronized void publishIssues() {
        if (pendingIssues.size() == 0) {
            pendingIssues.add(new Solutions.Issue(
                    getMachine(), 
                    "No issues detected.", 
                    "", 
                    Solutions.Severity.Information,
                    null));
        }
        // Go through the issues and set initially dismissed ones.
        // Also install listeners to update the dismissedTroubleshooting.
        for (Issue issue : pendingIssues) {
            if (isSolutionsIssueDismissed(issue)) {
                issue.setInitiallyDismissed();
            }
            issue.addPropertyChangeListener("state", e -> {
                if (e.getOldValue() == Solutions.State.Dismissed) {
                    setSolutionsIssueDismissed(issue, false);
                }
                if (issue.getState() == Solutions.State.Dismissed) {
                    setSolutionsIssueDismissed(issue, true);
                }
                int row = getIssues().indexOf(issue);
                fireTableRowsUpdated(row, row);
            });
        }
        // Sort by state (initially only Open and Dismissed possible) and place Fundamentals first.
        pendingIssues.sort(new Comparator<Issue>() {
            @Override
            public int compare(Issue o1, Issue o2) {
                int d = o1.getState().ordinal() - o2.getState().ordinal();
                if (d != 0) {
                    return d;
                }
                if (o1.getSeverity() == Severity.Fundamental && o2.getSeverity() != Severity.Fundamental) {
                    return -1;
                }
                else if (o2.getSeverity() == Severity.Fundamental) {
                    return 1;
                }
                else {
                    return 0;
                }
            }
        });
        //Object oldValue = this.issues; 
        this.issues = pendingIssues;
        pendingIssues = null;
        fireTableDataChanged();
        //firePropertyChange("issues", oldValue, this.issues);
    }

    private String[] columnNames =
            new String[] {"Subject", "Severity", "Issue", "Solution", "State"};
    private Class[] columnTypes = new Class[] {Subject.class, Severity.class, String.class, String.class, State.class};

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return issues.size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        /*Issue issue = getIssue(rowIndex); 
        if (columnIndex == 4) {
            return true;
        }*/
        return false;
    }

    public Issue getIssue(int index) {
        return issues.get(index);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        /*
        Issue issue = getIssue(rowIndex); 
        if (columnIndex == 4) {
            SwingUtilities.invokeLater(() -> {
                UiUtils.messageBoxOnException(() -> {
                    issue.setState((State) aValue);
                });
            });
        }
*/
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Issue issue = getIssue(rowIndex); 
        switch (columnIndex) {
            case 0:
                return issue.getSubject();
            case 1:
                return issue.getSeverity();
            case 2:
                return issue.getIssue();
            case 3:
                return issue.getSolution();
            case 4:
                return issue.getState();
            default:
                return null;
        }
    }
    public String getToolTipAt(int rowIndex, int columnIndex) {
        Issue issue = getIssue(rowIndex); 
        switch (columnIndex) {
            case 2:
                return issue.getIssue();
            case 3:
                return issue.getSolution();
        }
        return null;
    }

    static protected class SubjectRenderer extends DefaultTableCellRenderer {
        public void setValue(Object value) {
            if (value == null) {
                return;
            }
            Subject subject = (Subject) value; 
            setText(subject.getSubjectText());
        }
    }

    static protected class SeverityRenderer extends DefaultTableCellRenderer {
        public void setValue(Object value) {
            if (value == null) {
                return;
            }
            Severity severity = (Severity) value; 
            setForeground(Color.black);
            setBackground(severity.color);
            setText(severity.toString());
            setBorder(new LineBorder(getBackground()));
        }
    }

    static protected class StateRenderer extends DefaultTableCellRenderer {
        public void setValue(Object value) {
            if (value == null) {
                return;
            }
            State state = (State) value; 
            setForeground(Color.black);
            setBackground(state.color);
            setText(state.toString());
            setBorder(new LineBorder(getBackground()));
        }
    }

    public static void applyTableUi(AutoSelectTextTable table) {
        table.setDefaultRenderer(Solutions.Subject.class, new Solutions.SubjectRenderer());
        table.setDefaultRenderer(Solutions.Severity.class, new Solutions.SeverityRenderer());
        table.setDefaultRenderer(Solutions.State.class, new Solutions.StateRenderer());
        //JComboBox statesComboBox = new JComboBox(Solutions.State.values());
        //table.setDefaultEditor(Solutions.State.class, new DefaultCellEditor(statesComboBox));
    }

    @Deprecated
    public void migrateDismissedSolutions(Set<String> dismissedSolutions) {
        this.dismissedSolutions = dismissedSolutions;
    }
}
