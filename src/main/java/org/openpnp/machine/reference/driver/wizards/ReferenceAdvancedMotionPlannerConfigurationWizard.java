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

package org.openpnp.machine.reference.driver.wizards;


import org.openpnp.gui.components.SimpleGraphView;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.machine.reference.driver.ReferenceAdvancedMotionPlanner;
import org.openpnp.spi.MotionPlanner;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JLabel;

import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JCheckBox;

@SuppressWarnings("serial")
public class ReferenceAdvancedMotionPlannerConfigurationWizard extends AbstractConfigurationWizard {
    private final MotionPlanner motionPlanner;
    private JCheckBox allowContinuousMotion;
    private JCheckBox allowUncoordinated;
    private JLabel lblDiagnostics;
    private JCheckBox diagnosticsEnabled;
    private SimpleGraphView motionGraph;

    public ReferenceAdvancedMotionPlannerConfigurationWizard(ReferenceAdvancedMotionPlanner motionPlanner) {
        this.motionPlanner = motionPlanner;
        createUi();
    }
    private void createUi() {
        contentPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.PREF_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        JLabel lblContinuousMotion = new JLabel("Allow continous motion?");
        lblContinuousMotion.setToolTipText("<html>\r\n<p>Often, OpenPnP directs the controller(s) to execute motion that involves multiple<br/>\r\nsegments. For example, consider a move to Safe Z, followed by a move over the target <br/>\r\nlocation, followed by a move to lower the the nozzle down to pick or place a part. </p> \r\n<p>If the motion planner and/or the motion controller get these commands as one<br/>\r\nsequence, they can apply certain optimizations to them. There are also no delays<br/>\r\nintroduced when communicating back and forth. Furthermore, the planning can go <br/>\r\nahead in parallel while the controller is still executing the last commands.</p>\r\n<p>By allowing continuous motion, you enable these optimizations. However, the <br/>\r\nMachine setup i.e. Gcode, custom scripts etc. must be configured in awareness that the <br/>\r\nplanner no longer waits for motion to complete each time, unless explicitly told to. </p>\r\n</html>");
        contentPanel.add(lblContinuousMotion, "2, 2, right, default");
        
        allowContinuousMotion = new JCheckBox("");
        contentPanel.add(allowContinuousMotion, "4, 2");
        
        JLabel lblAllowUncoordinated = new JLabel("Allow uncoordinated?");
        contentPanel.add(lblAllowUncoordinated, "6, 2, right, default");
        
        allowUncoordinated = new JCheckBox("");
        contentPanel.add(allowUncoordinated, "8, 2");
        
        lblDiagnostics = new JLabel("Diagnostics?");
        contentPanel.add(lblDiagnostics, "10, 2, right, default");
        
        diagnosticsEnabled = new JCheckBox("");
        contentPanel.add(diagnosticsEnabled, "12, 2");
        
        JLabel lblMotionGraph = new JLabel("<html>\r\n<body style=\"text-align:right\">\r\n<p>\r\nLocation <span style=\"color:#00BB00\">&mdash;&mdash;</span>\r\n&nbsp;&nbsp;\r\nVelocity <span style=\"color:#005BD9\">&mdash;&mdash;</span>\r\n&nbsp;&nbsp;\r\nAcceleration <span style=\"color:#FF0000\">&mdash;&mdash;</span>\r\n&nbsp;&nbsp;\r\nJerk <span style=\"color:#FFBB00\">&mdash;&mdash;</span>\r\n</p>\r\n</body>\r\n</html>");
        contentPanel.add(lblMotionGraph, "14, 2, right, default");
        
        motionGraph = new SimpleGraphView();
        contentPanel.add(motionGraph, "2, 6, 15, 9");
        motionGraph.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("selectedX")) {
                    Double t = motionGraph.getSelectedX();
                }
            }
        });
        motionGraph.setFont(new Font("Dialog", Font.PLAIN, 11));
    }

    @Override
    public void createBindings() {
        addWrappedBinding(motionPlanner, "allowContinuousMotion", allowContinuousMotion, "selected");
        addWrappedBinding(motionPlanner, "allowUncoordinated", allowUncoordinated, "selected");
        addWrappedBinding(motionPlanner, "diagnosticsEnabled", diagnosticsEnabled, "selected");
        addWrappedBinding(motionPlanner, "motionGraph", motionGraph, "graph");
    }
}
