/*
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
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.base.AbstractHead;
import org.openpnp.spi.base.AbstractHead.VisualHomingMethod;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceHeadConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceHead head;

    public ReferenceHeadConfigurationWizard(ReferenceHead head) {
        this.head = head;
        createUi();
    }

    private void createUi() {

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Locations", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(80dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
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
                FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblX = new JLabel("X");
        panel.add(lblX, "4, 2, center, default");

        JLabel lblY = new JLabel("Y");
        panel.add(lblY, "6, 2, center, default");

        JLabel lblHomingFiducial = new JLabel("Homing Fiducial");
        panel.add(lblHomingFiducial, "2, 4, right, default");

        homingFiducialX = new JTextField();
        panel.add(homingFiducialX, "4, 4, fill, default");
        homingFiducialX.setColumns(10);

        homingFiducialY = new JTextField();
        homingFiducialY.setText("");
        panel.add(homingFiducialY, "6, 4, fill, default");
        homingFiducialY.setColumns(10);

        homeLocation = new LocationButtonsPanel(homingFiducialX, homingFiducialY, (JTextField) null, (JTextField) null);
        homeLocation.setShowToolButtons(false); 
        panel.add(homeLocation, "10, 4, left, fill");

        JLabel lblHomingMethod = new JLabel("Homing Method");
        panel.add(lblHomingMethod, "2, 6, right, default");

        visualHomingMethod = new JComboBox(VisualHomingMethod.values());
        visualHomingMethod.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        panel.add(visualHomingMethod, "4, 6, 3, 1, fill, default");
        
        panel_1 = new JPanel();
        panel_1.setBorder(null);
        panel.add(panel_1, "10, 6, left, fill");
        panel_1.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),},
                new RowSpec[] {
                        FormSpecs.DEFAULT_ROWSPEC,}));

        btnHomingTest = new JButton("Visual Test");
        panel_1.add(btnHomingTest, "1, 1, fill, default");
        btnHomingTest.setToolTipText("Test the visual homing fiducial locator without affecting the machine coordinate system.");

        btnVisualHome = new JButton("Visual Home");
        panel_1.add(btnVisualHome, "3, 1");
        btnVisualHome.setToolTipText("Perform visual homing.");
        btnVisualHome.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> head.visualHome(head.getMachine(), true));
            }
        });
        btnHomingTest.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> head.visualHome(head.getMachine(), false));
            }
        });

        JLabel lblWarningChangingThese = new JLabel("<html><p>\r\n<strong>Important Notice</strong>: the homing fiducial should be mounted \r\nand configured early in the build process, before you start capturing a large number of\r\nlocations for the Machine Setup (nozzle tip changer, feeders etc.) \r\n</p>\r\n<p style=\"color:red\">Each time the above settings are changed or the fiducial physically moved, all the already captured locations in the Machine Setup will be broken. </p></html>");
        lblWarningChangingThese.setForeground(Color.BLACK);
        panel.add(lblWarningChangingThese, "4, 8, 7, 1");

        JLabel lblParkLocation = new JLabel("Park Location");
        panel.add(lblParkLocation, "2, 10, right, default");

        parkX = new JTextField();
        panel.add(parkX, "4, 10, fill, default");
        parkX.setColumns(5);

        parkY = new JTextField();
        parkY.setColumns(5);
        panel.add(parkY, "6, 10, fill, default");

        parkLocation = new LocationButtonsPanel(parkX, parkY, (JTextField) null, (JTextField) null);
        parkLocation.setShowToolButtons(false); 
        panel.add(parkLocation, "10, 10, left, fill");
        
        JPanel panelCalibration = new JPanel();
        panelCalibration.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Calibration Rig", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(panelCalibration);
        panelCalibration.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(80dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblX_1 = new JLabel("X");
        panelCalibration.add(lblX_1, "4, 2, center, default");
        
        lblY_1 = new JLabel("Y");
        panelCalibration.add(lblY_1, "6, 2, center, default");
        
        lblZ_1 = new JLabel("Z");
        panelCalibration.add(lblZ_1, "8, 2, center, default");
        
        lblDiameter = new JLabel("Diameter");
        lblDiameter.setToolTipText("Diameter of the fiducial.");
        panelCalibration.add(lblDiameter, "10, 2, center, default");

        lblCalibrationPrimary = new JLabel("Primary Fiducial");
        lblCalibrationPrimary.setToolTipText("Calibration primary fiducial location. Must be placed at PCB surface Z height. ");
        panelCalibration.add(lblCalibrationPrimary, "2, 4, right, default");
        
        calibrationPrimaryX = new JTextField();
        panelCalibration.add(calibrationPrimaryX, "4, 4, left, default");
        calibrationPrimaryX.setColumns(10);
        
        calibrationPrimaryY = new JTextField();
        calibrationPrimaryY.setColumns(10);
        panelCalibration.add(calibrationPrimaryY, "6, 4, fill, default");
        
        calibrationPrimaryZ = new JTextField();
        panelCalibration.add(calibrationPrimaryZ, "8, 4, fill, default");
        calibrationPrimaryZ.setColumns(10);
        
        calibrationPrimaryFiducialDiameter = new JTextField();
        panelCalibration.add(calibrationPrimaryFiducialDiameter, "10, 4, fill, default");
        calibrationPrimaryFiducialDiameter.setColumns(10);
        
        calibrationPrimaryLocation = new LocationButtonsPanel(calibrationPrimaryX, calibrationPrimaryY, calibrationPrimaryZ, (JTextField) null);
        panelCalibration.add(calibrationPrimaryLocation, "12, 4, left, fill");
        
        lblCalibrationSecondary = new JLabel("Secondary Fiducial");
        lblCalibrationSecondary.setToolTipText("Calibration secondary fiducial location. Must be placed at different Z height than the primary fiducial. ");
        panelCalibration.add(lblCalibrationSecondary, "2, 6, right, default");
        
        calibrationSecondaryX = new JTextField();
        panelCalibration.add(calibrationSecondaryX, "4, 6, fill, default");
        calibrationSecondaryX.setColumns(10);
        
        calibrationSecondaryY = new JTextField();
        panelCalibration.add(calibrationSecondaryY, "6, 6, fill, default");
        calibrationSecondaryY.setColumns(10);
        
        calibrationSecondaryZ = new JTextField();
        panelCalibration.add(calibrationSecondaryZ, "8, 6, fill, default");
        calibrationSecondaryZ.setColumns(10);
        
        calibrationSecondaryFiducialDiameter = new JTextField();
        calibrationSecondaryFiducialDiameter.setColumns(10);
        panelCalibration.add(calibrationSecondaryFiducialDiameter, "10, 6, fill, default");
        
        calibrationSecondaryLocation = new LocationButtonsPanel(calibrationSecondaryX, calibrationSecondaryY, calibrationSecondaryZ, (JTextField) null);
        panelCalibration.add(calibrationSecondaryLocation, "12, 6, left, fill");
        
        lblTestObject = new JLabel("Test Object");
        panelCalibration.add(lblTestObject, "2, 8, right, default");
        
        calibrationTestObjectDiameter = new JTextField();
        panelCalibration.add(calibrationTestObjectDiameter, "10, 8, fill, default");
        calibrationTestObjectDiameter.setColumns(10);

        JPanel panel_2 = new JPanel();
        panel_2.setBorder(new TitledBorder(null, "Z Probe", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel_2);
        panel_2.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(80dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblNewLabel_4 = new JLabel("Z Probe Actuator");
        panel_2.add(lblNewLabel_4, "2, 2, right, default");

        comboBoxZProbeActuator = new JComboBox();
        comboBoxZProbeActuator.setModel(new ActuatorsComboBoxModel(head));
        panel_2.add(comboBoxZProbeActuator, "4, 2");

        JPanel panel_3 = new JPanel();
        panel_3.setBorder(new TitledBorder(null, "Pump", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel_3);
        panel_3.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(80dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblVacuumPumpActuator = new JLabel("Vacuum Pump Actuator");
        panel_3.add(lblVacuumPumpActuator, "2, 2, right, default");

        comboBoxPumpActuator = new JComboBox();
        comboBoxPumpActuator.setModel(new ActuatorsComboBoxModel(head));
        panel_3.add(comboBoxPumpActuator, "4, 2, 3, 1, fill, default");
        
        lblPumpControl = new JLabel("Pump Control");
        lblPumpControl.setToolTipText("<html>\r\n<p>Determine how the pump on/off state is controlled:</p>\r\n<ul>\r\n<li><strong>None</strong>: the pump is controlled manually or outside of OpenPnP.<br/>\r\nUse this for a controller-side hysteresis control, for instance.</li>\r\n<li><strong>PartOn</strong>: the pump is switched on when a part is about to be <br/>\r\npicked, it is switched off when no part is on any nozzle.</li>\r\n<li><strong>TaskDuration</strong>: the pump is switched on when a part is about<br/>\r\nto be picked, it is only switched off when queued tasks (e.g. <br/>\r\nthe running job) is finished, given no part is on any nozzle.</li>\r\n<li><strong>KeepRunning</strong>: the pump is switched on when a part is about<br/>\r\nto be picked, it is kept running until explicitly switched off, <br/>\r\nor until the machine is being disabled.</li>\r\n</ul>\r\n</html>");
        panel_3.add(lblPumpControl, "2, 4, right, default");
        
        vacuumPumpControl = new JComboBox(AbstractHead.VacuumPumpControl.values());
        panel_3.add(vacuumPumpControl, "4, 4, 3, 1, fill, default");
        
        lblPumpStartTime = new JLabel("Pump On Wait [ms]");
        lblPumpStartTime.setToolTipText("When switching on the pump, wait this time for it to reach proper pressure.");
        panel_3.add(lblPumpStartTime, "2, 6, right, default");
        
        pumpOnWaitMilliseconds = new JTextField();
        panel_3.add(pumpOnWaitMilliseconds, "4, 6, fill, default");
        pumpOnWaitMilliseconds.setColumns(10);

    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();

        MutableLocationProxy homingFiducialLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, head, "homingFiducialLocation", homingFiducialLocation, "location");
        addWrappedBinding(homingFiducialLocation, "lengthX", homingFiducialX, "text", lengthConverter);
        addWrappedBinding(homingFiducialLocation, "lengthY", homingFiducialY, "text", lengthConverter);

        addWrappedBinding(head, "visualHomingMethod", visualHomingMethod, "selectedItem");

        MutableLocationProxy parkLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, head, "parkLocation", parkLocation, "location");
        addWrappedBinding(parkLocation, "lengthX", parkX, "text", lengthConverter);
        addWrappedBinding(parkLocation, "lengthY", parkY, "text", lengthConverter);

        MutableLocationProxy primaryLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, head, "calibrationPrimaryFiducialLocation", primaryLocation, "location");
        addWrappedBinding(primaryLocation, "lengthX", calibrationPrimaryX, "text", lengthConverter);
        addWrappedBinding(primaryLocation, "lengthY", calibrationPrimaryY, "text", lengthConverter);
        addWrappedBinding(primaryLocation, "lengthZ", calibrationPrimaryZ, "text", lengthConverter);

        MutableLocationProxy secondaryLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, head, "calibrationSecondaryFiducialLocation", secondaryLocation, "location");
        addWrappedBinding(secondaryLocation, "lengthX", calibrationSecondaryX, "text", lengthConverter);
        addWrappedBinding(secondaryLocation, "lengthY", calibrationSecondaryY, "text", lengthConverter);
        addWrappedBinding(secondaryLocation, "lengthZ", calibrationSecondaryZ, "text", lengthConverter);

        addWrappedBinding(head, "calibrationPrimaryFiducialDiameter", calibrationPrimaryFiducialDiameter, "text", lengthConverter);
        addWrappedBinding(head, "calibrationSecondaryFiducialDiameter", calibrationSecondaryFiducialDiameter, "text", lengthConverter);
        addWrappedBinding(head, "calibrationTestObjectDiameter", calibrationTestObjectDiameter, "text", lengthConverter);

        NamedConverter<Actuator> actuatorConverter = (new NamedConverter<>(head.getActuators()));
        addWrappedBinding(head, "zProbeActuator", comboBoxZProbeActuator, "selectedItem", actuatorConverter);
        addWrappedBinding(head, "pumpActuator", comboBoxPumpActuator, "selectedItem", actuatorConverter);
        addWrappedBinding(head, "vacuumPumpControl", vacuumPumpControl, "selectedItem");
        addWrappedBinding(head, "pumpOnWaitMilliseconds", pumpOnWaitMilliseconds, "text", intConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homingFiducialX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homingFiducialY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(parkX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(parkY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(calibrationPrimaryX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(calibrationPrimaryY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(calibrationPrimaryZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(calibrationSecondaryX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(calibrationSecondaryY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(calibrationSecondaryZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(calibrationPrimaryFiducialDiameter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(calibrationSecondaryFiducialDiameter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(calibrationTestObjectDiameter);
        ComponentDecorators.decorateWithAutoSelect(pumpOnWaitMilliseconds);

        adaptDialog();
    }

    protected void adaptDialog() {
        boolean homingCapture = (visualHomingMethod.getSelectedItem() == VisualHomingMethod.ResetToFiducialLocation);
        boolean homingFiducial = (visualHomingMethod.getSelectedItem() != VisualHomingMethod.None);
        homeLocation.setEnabled(homingCapture);
        btnHomingTest.setEnabled(homingCapture);
        btnVisualHome.setEnabled(homingCapture);
        homingFiducialX.setEnabled(homingFiducial);
        homingFiducialY.setEnabled(homingFiducial);
    }

    private JTextField parkX;
    private JTextField parkY;
    private JComboBox comboBoxZProbeActuator;
    private JComboBox comboBoxPumpActuator;
    private JTextField homingFiducialX;
    private JTextField homingFiducialY;

    private JComboBox visualHomingMethod;
    private LocationButtonsPanel homeLocation;
    private LocationButtonsPanel parkLocation;
    private JButton btnHomingTest;
    private JButton btnVisualHome;
    private JLabel lblCalibrationPrimary;
    private JTextField calibrationPrimaryX;
    private JTextField calibrationPrimaryY;
    private JTextField calibrationPrimaryZ;
    private LocationButtonsPanel calibrationPrimaryLocation;
    private JLabel lblCalibrationSecondary;
    private JTextField calibrationSecondaryX;
    private JTextField calibrationSecondaryY;
    private JTextField calibrationSecondaryZ;
    private LocationButtonsPanel calibrationSecondaryLocation;
    private JPanel panel_1;
    private JLabel lblX_1;
    private JLabel lblY_1;
    private JLabel lblZ_1;
    private JLabel lblDiameter;
    private JTextField calibrationPrimaryFiducialDiameter;
    private JTextField calibrationSecondaryFiducialDiameter;
    private JTextField calibrationTestObjectDiameter;
    private JLabel lblTestObject;
    private JLabel lblPumpControl;
    private JComboBox vacuumPumpControl;
    private JLabel lblPumpStartTime;
    private JTextField pumpOnWaitMilliseconds;
}
