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


import java.util.HashSet;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.driver.ReferenceAdvancedMotionPlanner;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MotionPlanner;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.UIManager;
import java.awt.Color;

@SuppressWarnings("serial")
public class ReferenceAdvancedMotionPlannerConfigurationWizard extends AbstractConfigurationWizard {
    private final MotionPlanner motionPlanner;
    private JPanel panelSettings;
    private JCheckBox allowContinuousMotion;
    private JCheckBox allowUncoordinated;

    private JPanel panel;
    private JLabel lblX;
    private JLabel lblY;
    private JLabel lblZ;

    private JLabel lblStartLocation;
    private JTextField textFieldStartX;
    private JTextField textFieldStartY;
    private JTextField textFieldStartZ;
    private JLabel lblMiddleLocation1;
    private JTextField textFieldMidX1;
    private JTextField textFieldMidY1;
    private JTextField textFieldMidZ1;
    private JLabel lblMiddleLocation2;
    private JTextField textFieldMidX2;
    private JTextField textFieldMidY2;
    private JTextField textFieldMidZ2;
    private JLabel lblEndLocation;
    private JTextField textFieldEndX;
    private JTextField textFieldEndY;
    private JTextField textFieldEndZ;
    private LocationButtonsPanel startLocationButtonsPanel;
    private LocationButtonsPanel midLocation1ButtonsPanel;
    private LocationButtonsPanel midLocation2ButtonsPanel;
    private LocationButtonsPanel endLocationButtonsPanel;

    private JTextField toMid1Speed;
    private JTextField toMid2Speed;
    private JTextField toEndSpeed;
    private JLabel lblSpeed1;
    private JLabel lblSpeed2;
    private JLabel lblSpeedEnd;
    private JLabel lblRotation;
    private JTextField textFieldStartRotation;
    private JTextField textFieldMidRotation1;
    private JTextField textFieldMidRotation2;
    private JTextField textFieldEndRotation;
    private JLabel lblRetime;
    private JCheckBox interpolationRetiming;


    public ReferenceAdvancedMotionPlannerConfigurationWizard(ReferenceAdvancedMotionPlanner motionPlanner) {
        this.motionPlanner = motionPlanner;
        createUi();
    }
    private void createUi() {
        panelSettings = new JPanel();
        panelSettings.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Motion Planner", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(panelSettings);
        panelSettings.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        JLabel lblContinuousMotion = new JLabel("Allow continous motion?");
        lblContinuousMotion.setToolTipText("<html>\r\n<p>Often, OpenPnP directs the controller(s) to execute motion that involves multiple<br/>\r\nsegments. For example, consider a move to Safe Z, followed by a move over the target <br/>\r\nlocation, followed by a move to lower the the nozzle down to pick or place a part. </p> \r\n<p>If the motion planner and/or the motion controller get these commands as one<br/>\r\nsequence, they can apply certain optimizations to them. There are also no delays<br/>\r\nintroduced when communicating back and forth. Furthermore, the planning can go <br/>\r\nahead in parallel while the controller is still executing the last commands.</p>\r\n<p>By allowing continuous motion, you enable these optimizations. However, the <br/>\r\nMachine setup i.e. Gcode, custom scripts etc. must be configured in awareness that the <br/>\r\nplanner no longer waits for motion to complete each time, unless explicitly told to. </p>\r\n</html>");
        panelSettings.add(lblContinuousMotion, "2, 2, right, default");
        
        allowContinuousMotion = new JCheckBox("");
        panelSettings.add(allowContinuousMotion, "4, 2");
        
        JLabel lblAllowUncoordinated = new JLabel("Allow uncoordinated?");
        panelSettings.add(lblAllowUncoordinated, "2, 4, right, default");
        
        allowUncoordinated = new JCheckBox("");
        panelSettings.add(allowUncoordinated, "4, 4");
        
        lblRetime = new JLabel("Interpolation Retiming?");
        lblRetime.setToolTipText("<html>\r\nInterpolation can only approximate the true 3rd-order motion profilesl,<br/>\r\nsome deviations are expected. Re-timing will then stretch the motion<br/>\r\nto match the original 3rd-order timing. However this will slightly reduce<br/>\r\nthe speed. By switching this off, you get the planned speeds but slightly shorter<br/>\r\nmove duration.\r\n</html>\r\n");
        panelSettings.add(lblRetime, "2, 6, right, default");
        
        interpolationRetiming = new JCheckBox("");
        panelSettings.add(interpolationRetiming, "4, 6, right, top");

        panel = new JPanel();
        panel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Test Motion", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        Machine myMachine = null;
        try {
            myMachine = Configuration.get().getMachine();
        }
        catch (Exception e){
            Logger.error(e, "Cannot determine Name of machine.");
        }
        
        lblX = new JLabel("X");
        panel.add(lblX, "4, 2, center, default");

        lblY = new JLabel("Y");
        panel.add(lblY, "6, 2, center, default");

        lblZ = new JLabel("Z");
        panel.add(lblZ, "8, 2, center, default");
        
        lblRotation = new JLabel("Rotation");
        panel.add(lblRotation, "10, 2, center, default");
        
        textFieldStartRotation = new JTextField();
        panel.add(textFieldStartRotation, "10, 4, fill, default");
        textFieldStartRotation.setColumns(10);
        
        lblSpeed1 = new JLabel("Speed 1 ↔ 2");
        panel.add(lblSpeed1, "8, 5, right, default");
        
        toMid1Speed = new JTextField();
        toMid1Speed.setToolTipText("Speed between First location and Second location");
        panel.add(toMid1Speed, "10, 5, fill, default");
        toMid1Speed.setColumns(5);
        
        textFieldMidRotation1 = new JTextField();
        panel.add(textFieldMidRotation1, "10, 6, fill, default");
        textFieldMidRotation1.setColumns(10);
        
        lblSpeed2 = new JLabel("Speed 2 ↔ 3");
        panel.add(lblSpeed2, "8, 7, right, default");
        
        toMid2Speed = new JTextField();
        toMid2Speed.setToolTipText("Speed between Second location and Third location");
        toMid2Speed.setColumns(5);
        panel.add(toMid2Speed, "10, 7, fill, default");
        
        textFieldMidRotation2 = new JTextField();
        panel.add(textFieldMidRotation2, "10, 8, fill, default");
        textFieldMidRotation2.setColumns(10);
        
        lblSpeedEnd = new JLabel("Speed 3 ↔ 4");
        panel.add(lblSpeedEnd, "8, 9, right, default");
        
        lblStartLocation = new JLabel("First Location");
        panel.add(lblStartLocation, "2, 4, right, default");

        textFieldStartX = new JTextField();
        panel.add(textFieldStartX, "4, 4, fill, default");
        textFieldStartX.setColumns(10);

        textFieldStartY = new JTextField();
        panel.add(textFieldStartY, "6, 4, fill, default");
        textFieldStartY.setColumns(10);

        textFieldStartZ = new JTextField();
        panel.add(textFieldStartZ, "8, 4, fill, default");
        textFieldStartZ.setColumns(10);

        startLocationButtonsPanel = new LocationButtonsPanel(textFieldStartX,
                textFieldStartY, textFieldStartZ, (JTextField) null);
        startLocationButtonsPanel.setShowPositionToolNoSafeZ(true);
        panel.add(startLocationButtonsPanel, "12, 4, fill, default");

        lblMiddleLocation1 = new JLabel("Second Location");
        panel.add(lblMiddleLocation1, "2, 6, right, default");

        textFieldMidX1 = new JTextField();
        panel.add(textFieldMidX1, "4, 6, fill, default");
        textFieldMidX1.setColumns(10);

        textFieldMidY1 = new JTextField();
        panel.add(textFieldMidY1, "6, 6, fill, default");
        textFieldMidY1.setColumns(10);

        textFieldMidZ1 = new JTextField();
        panel.add(textFieldMidZ1, "8, 6, fill, default");
        textFieldMidZ1.setColumns(10);

        midLocation1ButtonsPanel = new LocationButtonsPanel(textFieldMidX1,
                textFieldMidY1, textFieldMidZ1, (JTextField) null);
        midLocation1ButtonsPanel.setShowPositionToolNoSafeZ(true);
        panel.add(midLocation1ButtonsPanel, "12, 6, fill, default");
        
        lblMiddleLocation2 = new JLabel("Third Location");
        panel.add(lblMiddleLocation2, "2, 8, right, default");
        
        textFieldMidX2 = new JTextField();
        textFieldMidX2.setColumns(10);
        panel.add(textFieldMidX2, "4, 8, fill, default");
        
        textFieldMidY2 = new JTextField();
        textFieldMidY2.setColumns(10);
        panel.add(textFieldMidY2, "6, 8, fill, default");
        
        textFieldMidZ2 = new JTextField();
        textFieldMidZ2.setColumns(10);
        panel.add(textFieldMidZ2, "8, 8, fill, default");
        
        midLocation2ButtonsPanel = new LocationButtonsPanel(textFieldMidX2, textFieldMidY2, textFieldMidZ2, (JTextField) null);
        midLocation2ButtonsPanel.setShowPositionToolNoSafeZ(true);
        panel.add(midLocation2ButtonsPanel, "12, 8, fill, default");
        
        toEndSpeed = new JTextField();
        toEndSpeed.setToolTipText("Speed between Third location and Last location");
        toEndSpeed.setColumns(5);
        panel.add(toEndSpeed, "10, 9, fill, default");

        lblEndLocation = new JLabel("Last Location");
        panel.add(lblEndLocation, "2, 10, right, default");

        textFieldEndX = new JTextField();
        panel.add(textFieldEndX, "4, 10, fill, default");
        textFieldEndX.setColumns(10);

        textFieldEndY = new JTextField();
        panel.add(textFieldEndY, "6, 10, fill, default");
        textFieldEndY.setColumns(10);

        textFieldEndZ = new JTextField();
        panel.add(textFieldEndZ, "8, 10, fill, default");
        textFieldEndZ.setColumns(10);
        
        textFieldEndRotation = new JTextField();
        panel.add(textFieldEndRotation, "10, 10, fill, default");
        textFieldEndRotation.setColumns(10);

        endLocationButtonsPanel = new LocationButtonsPanel(textFieldEndX,
                textFieldEndY, textFieldEndZ, (JTextField) null);
        endLocationButtonsPanel.setShowPositionToolNoSafeZ(true);
        panel.add(endLocationButtonsPanel, "12, 10, fill, default");
    }
    
    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        addWrappedBinding(motionPlanner, "allowContinuousMotion", allowContinuousMotion, "selected");
        addWrappedBinding(motionPlanner, "allowUncoordinated", allowUncoordinated, "selected");
        addWrappedBinding(motionPlanner, "interpolationRetiming", interpolationRetiming, "selected");

        MutableLocationProxy startLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, motionPlanner, "startLocation", startLocation,
                "location");
        addWrappedBinding(startLocation, "lengthX", textFieldStartX, "text",
                lengthConverter);
        addWrappedBinding(startLocation, "lengthY", textFieldStartY, "text",
                lengthConverter);
        addWrappedBinding(startLocation, "lengthZ", textFieldStartZ, "text",
                lengthConverter);
        addWrappedBinding(startLocation, "rotation", textFieldStartRotation, "text",
                doubleConverter);

        addWrappedBinding(motionPlanner, "toMid1Speed", toMid1Speed, "text",
                doubleConverter);

        MutableLocationProxy midLocation1 = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, motionPlanner, "midLocation1", midLocation1,
                "location");
        addWrappedBinding(midLocation1, "lengthX", textFieldMidX1, "text",
                lengthConverter);
        addWrappedBinding(midLocation1, "lengthY", textFieldMidY1, "text",
                lengthConverter);
        addWrappedBinding(midLocation1, "lengthZ", textFieldMidZ1, "text",
                lengthConverter);
        addWrappedBinding(midLocation1, "rotation", textFieldMidRotation1, "text",
                doubleConverter);

        addWrappedBinding(motionPlanner, "toMid2Speed", toMid2Speed, "text",
                doubleConverter);

        MutableLocationProxy midLocation2 = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, motionPlanner, "midLocation2", midLocation2,
                "location");
        addWrappedBinding(midLocation2, "lengthX", textFieldMidX2, "text",
                lengthConverter);
        addWrappedBinding(midLocation2, "lengthY", textFieldMidY2, "text",
                lengthConverter);
        addWrappedBinding(midLocation2, "lengthZ", textFieldMidZ2, "text",
                lengthConverter);
        addWrappedBinding(midLocation2, "rotation", textFieldMidRotation2, "text",
                doubleConverter);

        addWrappedBinding(motionPlanner, "toEndSpeed", toEndSpeed, "text",
                doubleConverter);

        MutableLocationProxy endLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, motionPlanner, "endLocation", endLocation,
                "location");
        addWrappedBinding(endLocation, "lengthX", textFieldEndX, "text",
                lengthConverter);
        addWrappedBinding(endLocation, "lengthY", textFieldEndY, "text",
                lengthConverter);
        addWrappedBinding(endLocation, "lengthZ", textFieldEndZ, "text",
                lengthConverter);
        addWrappedBinding(endLocation, "rotation", textFieldEndRotation, "text",
                doubleConverter);
        
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldStartX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldStartY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldStartZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldStartRotation);
        
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(toMid1Speed);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMidX1);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMidY1);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMidZ1);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMidRotation1);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(toMid2Speed);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMidX2);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMidY2);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMidZ2);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMidRotation2);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(toEndSpeed);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldEndX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldEndY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldEndZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldEndRotation);
    }
}
