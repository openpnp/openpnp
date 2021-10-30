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
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.commons.codec.digest.DigestUtils;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.support.Icons;
import org.openpnp.machine.reference.driver.NullDriver;
import org.openpnp.machine.reference.driver.NullMotionPlanner;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Camera;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Machine;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.VisionUtils;
import org.openpnp.util.XmlSerialize;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

public class Solutions extends AbstractTableModel {

    @ElementList(required = false)
    private Set<String> dismissedSolutions = new HashSet<>();

    @ElementList(required = false)
    private Set<String> solvedSolutions = new HashSet<>();

    @Attribute(required = false)
    private boolean showIndicator = true;

    private boolean showSolved;

    private boolean showDismissed;

    public enum Milestone implements Subject, Named {
        Welcome     ("Welcome",     "welcome",      "Get to know OpenPnP by using the demo simulation machine. Choose your nozzle configuration."), 
        Connect     ("Connect",     "connect",      "Connect OpenPnP to real controllers and cameras."),
        Basics      ("Basics",      "basics",       "Configure basic machine axes, motion, vacuum switching, light switching."),
        Kinematics  ("Kinematics",  "kinematics",   "Define machine kinematics: Safe Z, soft limits, motion control model, feed-rates, accelerations etc."),
        Vision      ("Vision",      "vision",       "Setup cameras and computer vision."),
        Calibration ("Calibration", "calibration",  "Calibrate the machine for precision motion and vision."),
        Production  ("Production",  "production",   "Configure feeders and solve other production related issues."), 
        Advanced    ("Advanced",    "advanced",     "Enable more advanced features for a faster and more automatic machine.");

        final private String name;
        final private String tag;
        final private String description;

        private Milestone(String name, String tag, String description) {
            this.name = name;
            this.tag = tag;
            this.description = description;
        }

        public Milestone getPrevious() {
            if (ordinal()-1 >= 0) {
                return values()[ordinal() - 1];
            }
            return null;
        }

        public Milestone getNext() {
            if (ordinal()+1 < values().length) {
                return values()[ordinal() + 1];
            }
            return null;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setName(String name) {}

        @Override
        public Icon getSubjectIcon() {
            return Icons.solutions;
        }

        public String getDescription() {
            return description;
        }

        public String getAnchor() {
            return "#"+tag+"-milestone";
        }
    }
    @Attribute(required = false)
    private Milestone targetMilestone;

    // Lacking multiple inheritance, we can't inherit from AbstractModelObject 
    protected final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    public interface Subject {
        /**
         * Report any detected issue and proposed solution in the list. 
         * @param report
         */
        public default void findIssues(Solutions solutions) {
        }
        public default String getSubjectText() {
            if (this instanceof Named) {
                return (this.getClass().getSimpleName()+(((Named) this).getName() != null ? " "+((Named) this).getName() : ""));
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

    public Milestone getTargetMilestone() {
        if (targetMilestone == null) {
            migrateLevel();
        }
        return targetMilestone;
    }
    public void setTargetMilestone(Milestone targetMilestone) {
        Object oldValue = this.targetMilestone;
        this.targetMilestone = targetMilestone;
        propertyChangeSupport.firePropertyChange("targetMilestone", oldValue, targetMilestone);
    }

    public boolean isShowIndicator() {
        return showIndicator;
    }

    public void setShowIndicator(boolean showIndicator) {
        this.showIndicator = showIndicator;
    }

    public boolean isShowSolved() {
        return showSolved;
    }

    public void setShowSolved(boolean showSolved) {
        this.showSolved = showSolved;
    }

    public boolean isShowDismissed() {
        return showDismissed;
    }

    public void setShowDismissed(boolean showDismissed) {
        this.showDismissed = showDismissed;
    }

    public boolean isTargeting(Milestone targetMilestone) {
        return getTargetMilestone().ordinal() >= targetMilestone.ordinal();
    }
    public boolean isAtMostTargeting(Milestone targetMilestone) {
        return getTargetMilestone().ordinal() <= targetMilestone.ordinal();
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

    public boolean confirm(String message, boolean warning) {
        int result = JOptionPane.showConfirmDialog(MainFrame.get(),
                message, warning ? "Warning" : "Question", 
                        JOptionPane.YES_NO_OPTION, 
                        warning ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE);
        return (result == JOptionPane.YES_OPTION);
    }

    public static abstract class Issue extends AbstractModelObject {
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

        public boolean isUnhandled() {
            return state == State.Open;
        }

        public Object getChoice() {
            return choice;
        }
        public void setChoice(Object choice) {
            this.choice = choice;
        }

        /**
         * @return true if this solution is triggered by a machine condition that absolutely needs to be 
         * addressed. This will force a solution to be unsolved, even is marked as "solved". Note, it will not
         * force open a solution that is marked as "dismissed".
         */
        public boolean isForcedUnsolved() {
            return false;
        }
        void setInitialState(State state) {
            this.state = state;
        }
        public String getUri() {
            return uri;
        }
        public boolean canBeAccepted() {
            return true;
        }
        public boolean canBeUndone() {
            return true;
        }

        /**
         * Ultra simple custom property support. 
         *
         */
        public abstract class CustomProperty {
            private final String label;
            private final String toolTip;

            public CustomProperty(String label, String toolTip) {
                super();
                this.label = label;
                this.toolTip = toolTip;
            }

            public String getLabel() {
                return label;
            }
            public String getToolTip() {
                return toolTip;
            }
        }
        public abstract class IntegerProperty extends CustomProperty {
            private final int min;
            private final int max;

            public IntegerProperty(String label, String toolTip, int min, int max) {
                super(label, toolTip);
                this.min = min;
                this.max = max;
            }
            public int getMin() {
                return min;
            }
            public int getMax() {
                return max;
            }
            public abstract int get();
            public abstract void set(int value);
        }
        public abstract class DoubleProperty extends CustomProperty {
            private final double min;
            private final double max;

            public DoubleProperty(String label, String toolTip, double min, double max) {
                super(label, toolTip);
                this.min = min;
                this.max = max;
            }
            public double getMin() {
                return min;
            }
            public double getMax() {
                return max;
            }
            public abstract double get();
            public abstract void set(double value);
        }
        public abstract class LengthProperty extends CustomProperty {
            public LengthProperty(String label, String toolTip) {
                super(label, toolTip);
            }
            public abstract Length get();
            public abstract void set(Length value);
        }
        public abstract class ActionProperty extends CustomProperty {
            public ActionProperty(String label, String toolTip) {
                super(label, toolTip);
            }
            public abstract Action get();
        }

        public CustomProperty [] getProperties() {
            return new CustomProperty[] {};
        }

        /**
         * Ultra-simple multiple-choices system. 
         * @return
         */
        public class Choice {
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

        public Choice [] getChoices() {
            return new Choice[] {};
        }

        public void activate() throws Exception {
        }

        public String getExtendedDescription() {
            return null;
        }

        public Icon getExtendedIcon() {
            return null;
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
        public boolean canBeAccepted() {
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
            solvedSolutions.remove(issue.getFingerprint());
            dismissedSolutions.add(issue.getFingerprint()); 
        }
        else {
            dismissedSolutions.remove(issue.getFingerprint());
        }
    }

    public boolean isSolutionsIssueSolved(Issue issue) {
        return solvedSolutions.contains(issue.getFingerprint());
    }

    public void setSolutionsIssueSolved(Issue issue, boolean solved) {
        if (solved) {
            dismissedSolutions.remove(issue.getFingerprint());
            solvedSolutions.add(issue.getFingerprint()); 
        }
        else {
            solvedSolutions.remove(issue.getFingerprint());
        }
    }

    public List<Issue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    public Machine getMachine() {
        Machine machine = Configuration.get().getMachine();
        return machine;
    }

    /**
     * Perform the Issues & Solutions search. This opens a list of pending issues, i.e. it does not
     * directly affect the table model. Pending issues will only be made visible when 
     * {@link #publishIssues()} is called.    
     */
    public synchronized void findIssues() {
        pendingIssues = new ArrayList<>();
        Machine machine = getMachine();
        machine.findIssues(this);
        // Add the milestone completion solution.
        Milestone targetMilestone = getTargetMilestone();
        pendingIssues.add(new Solutions.Issue(
                targetMilestone, 
                "Complete milestone "+targetMilestone.getName(), 
                targetMilestone.getDescription(), 
                Solutions.Severity.Information,
                "https://github.com/openpnp/openpnp/wiki/Issues-and-Solutions"+targetMilestone.getAnchor()) {
            {
                setChoice(targetMilestone.getNext());
            }
            @Override
            public void setState(Solutions.State state) throws Exception {
                if (state == State.Solved) {
                    boolean ok = true;
                    if (getChoice() == targetMilestone.getNext()) {
                        // Check if proceeding to the next level is ok.
                        // Make a fresh findIssues (this will only affect the pendingIssues, not the actual ones).
                        findIssues();
                        StringBuilder str = new StringBuilder();
                        str.append("<ul>");
                        for (Issue issue : pendingIssues) {
                            if (issue.isUnhandled() 
                                    && !issue.getFingerprint().equals(getFingerprint())) {
                                // There is still an open issue.
                                ok = false;
                                str.append("<li>");
                                str.append(XmlSerialize.escapeXml(issue.getSubject().getSubjectText()));
                                str.append(": ");
                                str.append(XmlSerialize.escapeXml(issue.getIssue()));
                                str.append("</li>");
                            }
                        }
                        str.append("</ul>");
                        pendingIssues = null;
                        if (!ok) {
                            if (confirm("<html>"
                                    + "<p>Issues for milestone <strong>"+targetMilestone+"</strong> are still open:</p>"
                                    + str.toString()
                                    + "<p color=\"red\">It is not recommended to switch to the next target milestone "
                                    + "before these are resolved or dismissed!</p>"
                                    + "<p><br/>Are you sure you still want to proceed?</p>"
                                    + "</html>", 
                                    true)) {
                                ok = true;
                            }
                        }
                    }
                    if (ok) {
                        super.setState(state);
                        setTargetMilestone((Milestone) getChoice());
                        MainFrame.get().getIssuesAndSolutionsTab().findIssuesAndSolutions();
                    }
                }
                else {
                    super.setState(state);
                }
            }

            @Override
            public Solutions.Issue.Choice[] getChoices() {
                return new Solutions.Issue.Choice[] {
                        (targetMilestone.getNext() == null ? null :
                            new Solutions.Issue.Choice(targetMilestone.getNext(), 
                                    "<html><h3>Proceed to "+targetMilestone.getNext().getName()+"</h3>"
                                            + "<p>Confirm to have completed and tested milestone <strong>"+targetMilestone.getName()+"</strong>:</p><br/>"
                                            + "<blockquote>"+XmlSerialize.escapeXml(targetMilestone.getDescription())+"</blockquote><br/>"
                                            + "<p>Note: all issues from this milestone should be resolved or dismissed before you proceed.</p><br/>"
                                            + "<p>Yes? <em>Congratulations!</em></p><br/>"
                                            + "<p>You can proceed to the next milestone "
                                            + "<strong>"+targetMilestone.getNext().getName()+"</strong>:</p><br/>"
                                            + "<blockquote>"+XmlSerialize.escapeXml(targetMilestone.getNext().getDescription())+"</blockquote><br/>"
                                            + "<p>More Issues & Solutions will be presented that need addressing to reach the next milestone.</p>"
                                            + "</html>",
                                            Icons.milestone)),
                        (targetMilestone.getPrevious() == null ? null :
                            new Solutions.Issue.Choice(targetMilestone.getPrevious(), 
                                    "<html><h3>Go back to "+targetMilestone.getPrevious().getName()+"</h3>"
                                            + "<p>To limit the scope for Issues & Solutions, you can go back to the previous milestone "
                                            + "<strong>"+targetMilestone.getPrevious().getName()+"</strong>:</p><br/>"
                                            + "<blockquote>"+XmlSerialize.escapeXml(targetMilestone.getPrevious().getDescription())+"</blockquote><br/>"
                                            + "<p>Note: Most Issues & Solutions from previous milestones are also reported on subsequent milestones. "
                                            + "But in earlier target milestones some solutions proposed are simpler, more conservative. "
                                            + "For troubleshooting, it can therfore be beneficial to go back and try getting it to work there.</p>"
                                            + "</html>",
                                            Icons.milestone)),
                };
            }
        });
        for (Issue issue : new ArrayList<>(pendingIssues)) {
            if (isSolutionsIssueDismissed(issue)) {
                if (isShowDismissed()) {
                    issue.setInitialState(State.Dismissed);
                }
                else {
                    pendingIssues.remove(issue);
                }
            }
            else if (isSolutionsIssueSolved(issue)) {
                if (issue.isForcedUnsolved()) {
                    setSolutionsIssueSolved(issue, false);
                }
                else if (isShowSolved()) {
                    issue.setInitialState(State.Solved);
                }
                else {
                    pendingIssues.remove(issue);
                }
            }
        }
    }

    /**
     * Adds the issue to the list of pending issues that {@link #findIssues()} has opened.
     * This should only be called from inside {@link #Subject.findIssues(Solutions)}
     * 
     * @param issue
     * @return true if the issue was already marked as solved.  
     */
    public synchronized boolean add(Issue issue) {
        // Do not allow duplicates. These will sometimes be generated when multiple machine objects 
        // check the same issue that is relevant to them. 
        String fingerprint = issue.getFingerprint();
        for (Issue duplicate : pendingIssues) {
            if (duplicate.getFingerprint().equals(fingerprint)) {
                return true;
            }
        }
        pendingIssues.add(issue);
        return isSolutionsIssueSolved(issue);
    }

    /**
     * Publish the pending issues that {@link #findIssues()} has found i.e. that were added using {@link #add(Issue)}.
     * This makes them visible trough the TableModel of this.
     */
    public synchronized void publishIssues() {
        // Go through the issues and install listeners to update the dismissedTroubleshooting.
        for (Issue issue : pendingIssues) {
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
        return false;
    }

    public Issue getIssue(int index) {
        return issues.get(index);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
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

    @Deprecated
    private void migrateLevel() {
        // Migration of an older configuration, try reconstructing the level. This is only a very crude heuristic.
        if (getMachine().getDrivers().isEmpty() || getMachine().getDrivers().get(0) instanceof NullDriver) {
            targetMilestone = Milestone.Welcome;
        }
        else if (getMachine().getMotionPlanner() instanceof NullMotionPlanner) {
            targetMilestone = Milestone.Calibration;
            try {
                for (Camera camera : new Camera[] {
                        getMachine().getDefaultHead().getDefaultCamera(), 
                        VisionUtils.getBottomVisionCamera() }) {
                    if (camera.getUnitsPerPixel().getX() == 0 || camera.getUnitsPerPixel().getY() == 0) {
                        targetMilestone = Milestone.Vision;
                    }
                }
            }
            catch (Exception e) {
                targetMilestone = Milestone.Connect;
            }
            for (Axis axis : getMachine().getAxes()) {
                if (axis instanceof ControllerAxis) {
                    if (((ControllerAxis) axis).getDriver() == null 
                            || ((ControllerAxis) axis).getLetter().isEmpty()) {
                        targetMilestone = Milestone.Basics;
                    }
                }
            }
        }
        else {
            targetMilestone = Milestone.Advanced;
        }
    }
}
