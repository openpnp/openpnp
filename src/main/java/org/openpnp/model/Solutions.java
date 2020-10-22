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
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.commons.codec.digest.DigestUtils;
import org.openpnp.gui.MainFrame;
import org.openpnp.util.XmlSerialize;

public class Solutions extends AbstractTableModel {

    public interface Subject {
        /**
         * Report any detected issue and proposed solution in the list. 
         * @param report
         */
        public default void findIssues(List<Solutions.Issue> issues) {
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
    }

    public enum Severity {
        Information(new Color(255, 255, 255)),
        Suggestion(new Color(255, 255, 157)),
        Warning(new Color(255, 220, 157)),
        Error(new Color(255, 157, 157)),
        None(new Color(255, 255, 255)), 
        Fundamental(new Color(255, 157, 157));

        private Color color;

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

    public static class Issue extends AbstractModelObject {
        final Subject subject;
        final String issue;
        final String solution;
        final Severity severity;
        final String uri;
        private State state;


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
        public boolean confirmStateChange(State state) {
            if (this.state != state) {
                if (state == State.Solved) {
                    if (JOptionPane.showConfirmDialog(MainFrame.get(), 
                            "<html>"+
                                    "<strong>Subject:</strong><br/>"+XmlSerialize.escapeXml(getSubject().getSubjectText())+"<br/><br/>"+
                                    "<strong>Issue:</strong><br/>"+XmlSerialize.escapeXml(getIssue())+"<br/><br/>"+
                                    "<strong>Solution:</strong><br/>"+XmlSerialize.escapeXml(getSolution())+"<br/><br/>"+
                                    "Do you want to apply the suggested solution?<br/>&nbsp;"+
                                    "</html>", 
                                    "Confirm State "+state, JOptionPane.YES_NO_OPTION) == 0) {
                        return true;
                    }
                }
                else if (this.state == State.Solved) {
                    if(JOptionPane.showConfirmDialog(MainFrame.get(), 
                            "<html>"+
                                    "<strong>Subject:</strong><br/>"+XmlSerialize.escapeXml(getSubject().getSubjectText())+"<br/><br/>"+
                                    "<strong>Issue:</strong><br/>"+XmlSerialize.escapeXml(getIssue())+"<br/><br/>"+
                                    "<strong>Solution applied earlier:</strong><br/>"+XmlSerialize.escapeXml(getSolution())+"<br/><br/>"+
                                    "<strong>Change state to "+state+":</strong><br/>Do you want to <strong>undo</strong> and restore previous settings?<br/>&nbsp;"+
                                    "</html>", 
                                    "Confirm State "+state, JOptionPane.YES_NO_OPTION) == 0 ) {
                        return true;
                    }
                }
                else if (state == State.Dismissed){
                    if(JOptionPane.showConfirmDialog(MainFrame.get(), 
                            "<html>"+
                                    "<strong>Subject:</strong><br/>"+XmlSerialize.escapeXml(getSubject().getSubjectText())+"<br/><br/>"+
                                    "<strong>Issue:</strong><br/>"+XmlSerialize.escapeXml(getIssue())+"<br/><br/>"+
                                    "<strong>Solution to be dismissed:</strong><br/>"+XmlSerialize.escapeXml(getSolution())+"<br/><br/>"+
                                    "Do you want to permanently dismiss this issue?<br/>&nbsp;"+
                                    "</html>", 
                                    "Confirm State "+state, JOptionPane.YES_NO_OPTION) == 0 ) {
                        return true;
                    }
                }
                else {
                    return true;
                }
            }
            return false;
        }
        public void setState(State state) throws Exception {
            Object oldValue = this.state;
            this.state = state;
            firePropertyChange("state", oldValue, state);
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

    List<Issue> issues = new ArrayList<>();

    public List<Issue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    public void setIssues(List<Issue> issues) {
        //Object oldValue = this.issues; 
        this.issues = issues;
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

    static public class SubjectRenderer extends DefaultTableCellRenderer {
        public void setValue(Object value) {
            if (value == null) {
                return;
            }
            Subject subject = (Subject) value; 
            setText(subject.getSubjectText());
        }
    }

    static public class SeverityRenderer extends DefaultTableCellRenderer {
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

    static public class StateRenderer extends DefaultTableCellRenderer {
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
}
