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
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.ReferenceHead;
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
                ColumnSpec.decode("max(100dlu;default)"),},
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
        panel.add(homeLocation, "8, 4, left, fill");
        
        JLabel lblHomingMethod = new JLabel("Homing Method");
        panel.add(lblHomingMethod, "2, 6, right, default");
        
        visualHomingMethod = new JComboBox(VisualHomingMethod.values());
        visualHomingMethod.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        panel.add(visualHomingMethod, "4, 6, 3, 1, fill, default");
        
        btnHomingTest = new JButton("Test");
        btnHomingTest.setToolTipText("Test the homing fiducial locator. Machine homing is not affected. Afterwards check the log.");
        btnHomingTest.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.submitUiMachineTask(() -> head.visualHome(head.getMachine(), false));
            }
        });
        panel.add(btnHomingTest, "8, 6, left, default");
        
        JLabel lblWarningChangingThese = new JLabel("<html><p>\r\n<strong>Important Notice</strong>: the homing fiducial should be mounted \r\nand configured early in the build process, before you start capturing a large number of\r\nlocations for the Machine Setup (nozzle tip changer, feeders etc.) \r\n</p>\r\n<p style=\"color:red\">Each time the above settings are changed or the fiducial physically moved, all the already captured locations in the Machine Setup will be broken. </p></html>");
        lblWarningChangingThese.setForeground(Color.BLACK);
        panel.add(lblWarningChangingThese, "4, 8, 5, 1");

        JLabel lblParkLocation = new JLabel("Park Location");
        panel.add(lblParkLocation, "2, 12, right, default");

        parkX = new JTextField();
        panel.add(parkX, "4, 12, fill, default");
        parkX.setColumns(5);

        parkY = new JTextField();
        parkY.setColumns(5);
        panel.add(parkY, "6, 12, fill, default");
        
        parkLocation = new LocationButtonsPanel(parkX, parkY, (JTextField) null, (JTextField) null);
        parkLocation.setShowToolButtons(false); 
        panel.add(parkLocation, "8, 12, left, fill");

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
                ColumnSpec.decode("max(50dlu;default)"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        JLabel lblVacuumPumpActuator = new JLabel("Vacuum Pump Actuator");
        panel_3.add(lblVacuumPumpActuator, "2, 2, 2, 1, right, default");
        
        comboBoxPumpActuator = new JComboBox();
        comboBoxPumpActuator.setModel(new ActuatorsComboBoxModel(head));
        panel_3.add(comboBoxPumpActuator, "4, 2, fill, default");
        
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();

        MutableLocationProxy homingFiducialLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, head, "homingFiducialLocation", homingFiducialLocation, "location");
        addWrappedBinding(homingFiducialLocation, "lengthX", homingFiducialX, "text", lengthConverter);
        addWrappedBinding(homingFiducialLocation, "lengthY", homingFiducialY, "text", lengthConverter);

        addWrappedBinding(head, "visualHomingMethod", visualHomingMethod, "selectedItem");

        MutableLocationProxy parkLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, head, "parkLocation", parkLocation, "location");
        addWrappedBinding(parkLocation, "lengthX", parkX, "text", lengthConverter);
        addWrappedBinding(parkLocation, "lengthY", parkY, "text", lengthConverter);

        addWrappedBinding(head, "zProbeActuatorName", comboBoxZProbeActuator, "selectedItem");
        addWrappedBinding(head, "pumpActuatorName", comboBoxPumpActuator, "selectedItem");

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homingFiducialX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homingFiducialY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(parkX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(parkY);

        adaptDialog();
    }

    protected void adaptDialog() {
        boolean homingCapture = (visualHomingMethod.getSelectedItem() == VisualHomingMethod.ResetToFiducialLocation);
        boolean homingFiducial = (visualHomingMethod.getSelectedItem() != VisualHomingMethod.None);
        homeLocation.setEnabled(homingCapture);
        btnHomingTest.setEnabled(homingCapture);
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
}
