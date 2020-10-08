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


import java.awt.Font;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.SimpleGraphView;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Icons;
import org.openpnp.machine.reference.driver.ReferenceAdvancedMotionPlanner;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.HeadMountable;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JTextField;
import java.awt.Color;

@SuppressWarnings("serial")
public class ReferenceAdvancedMotionPlannerDiagnosticsWizard extends AbstractConfigurationWizard {
    private final ReferenceAdvancedMotionPlanner motionPlanner;
    private JLabel lblDiagnostics;
    private JCheckBox diagnosticsEnabled;
    private SimpleGraphView motionGraph;
    private JButton btnTest;

    private Action testAction =
            new AbstractAction("Test", Icons.start) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>Test the motion defined in the Motion Planner Wizard.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                applyAction.actionPerformed(e);
                UiUtils.submitUiMachineTask(() -> {
                    
                    HeadMountable selectedTool = MainFrame.get().getMachineControls().getSelectedTool();
                    Location l = selectedTool.getLocation();
                    boolean reverse = (l.getLinearDistanceTo(motionPlanner.getStartLocation()) > l.getLinearDistanceTo(motionPlanner.getEndLocation()));
                    motionPlanner.testMotion(selectedTool, reverse);
                });
            });
        }
    };
    private JLabel lblPlanned;
    private JLabel lblActual;
    private JTextField moveTimePlanned;
    private JTextField moveTimeActual;
    private JLabel interpolationFailed;


    public ReferenceAdvancedMotionPlannerDiagnosticsWizard(ReferenceAdvancedMotionPlanner motionPlanner) {
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
        
        lblDiagnostics = new JLabel("Diagnostics?");
        contentPanel.add(lblDiagnostics, "2, 2, right, default");
        
        diagnosticsEnabled = new JCheckBox("");
        contentPanel.add(diagnosticsEnabled, "4, 2");
        
        btnTest = new JButton("Test");
        btnTest.setAction(testAction);
        contentPanel.add(btnTest, "6, 2");
        
        lblPlanned = new JLabel("Planned [s]");
        contentPanel.add(lblPlanned, "8, 2, right, default");
        
        moveTimePlanned = new JTextField();
        moveTimePlanned.setEditable(false);
        contentPanel.add(moveTimePlanned, "10, 2, fill, default");
        moveTimePlanned.setColumns(10);
        
        lblActual = new JLabel("Actual");
        contentPanel.add(lblActual, "12, 2, right, default");
        
        moveTimeActual = new JTextField();
        moveTimeActual.setEditable(false);
        contentPanel.add(moveTimeActual, "14, 2, fill, default");
        moveTimeActual.setColumns(10);
        
        interpolationFailed = new JLabel("Interpolation failed!");
        interpolationFailed.setForeground(Color.RED);
        contentPanel.add(interpolationFailed, "16, 2");
        
        JLabel lblMotionGraph = new JLabel("<html>\r\n<body style=\"text-align:right\">\r\n<p>\r\nLocation <span style=\"color:#00BB00\">&mdash;&mdash;</span>\r\n&nbsp;&nbsp;\r\nVelocity <span style=\"color:#005BD9\">&mdash;&mdash;</span>\r\n&nbsp;&nbsp;\r\nAcceleration <span style=\"color:#FF0000\">&mdash;&mdash;</span>\r\n&nbsp;&nbsp;\r\nJerk <span style=\"color:#DDBB00\">&mdash;&mdash;</span>\r\n</p>\r\n</body>\r\n</html>");
        contentPanel.add(lblMotionGraph, "18, 2, right, default");
        
        motionGraph = new SimpleGraphView();
        contentPanel.add(motionGraph, "2, 4, 19, 11");
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
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        addWrappedBinding(motionPlanner, "moveTimePlanned", moveTimePlanned, "text", doubleConverter);
        addWrappedBinding(motionPlanner, "moveTimeActual", moveTimeActual, "text", doubleConverter);

        addWrappedBinding(motionPlanner, "diagnosticsEnabled", diagnosticsEnabled, "selected");
        addWrappedBinding(motionPlanner, "motionGraph", motionGraph, "graph");
        addWrappedBinding(motionPlanner, "interpolationFailed", interpolationFailed, "visible");
        
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(moveTimePlanned);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(moveTimeActual);
    }
}
