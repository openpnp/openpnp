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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

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
        panel.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblX = new JLabel("X");
        panel.add(lblX, "4, 2, center, default");

        JLabel lblY = new JLabel("Y");
        panel.add(lblY, "6, 2, center, default");

        JLabel lblParkLocation = new JLabel("Park Location");
        panel.add(lblParkLocation, "2, 4, right, default");

        parkX = new JTextField();
        panel.add(parkX, "4, 4, fill, default");
        parkX.setColumns(5);

        parkY = new JTextField();
        parkY.setColumns(5);
        panel.add(parkY, "6, 4, fill, default");

        JButton btnNewButton = new JButton(captureParkCoordinatesAction);
        btnNewButton.setHideActionText(true);
        panel.add(btnNewButton, "8, 4");

        JButton btnNewButton_1 = new JButton(positionParkCoordinatesAction);
        btnNewButton_1.setHideActionText(true);
        panel.add(btnNewButton_1, "10, 4");

        JPanel panel_1 = new JPanel();
        panel_1.setBorder(new TitledBorder(null, "Soft Limits", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panel_1);
        panel_1.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("center:default"),
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("center:default"),
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblNewLabel = new JLabel("X");
        panel_1.add(lblNewLabel, "4, 2");

        JLabel lblNewLabel_1 = new JLabel("Y");
        panel_1.add(lblNewLabel_1, "6, 2");

        JLabel lblNewLabel_2 = new JLabel("Minimum");
        panel_1.add(lblNewLabel_2, "2, 4, right, default");

        minX = new JTextField();
        panel_1.add(minX, "4, 4, fill, default");
        minX.setColumns(5);

        minY = new JTextField();
        panel_1.add(minY, "6, 4, fill, default");
        minY.setColumns(5);

        JButton btncaptureMin = new JButton(captureMinCoordinatesAction);
        btncaptureMin.setHideActionText(true);
        panel_1.add(btncaptureMin, "8, 4");

        JButton btnNewButton_3 = new JButton(positionMinCoordinatesAction);
        btnNewButton_3.setHideActionText(true);
        panel_1.add(btnNewButton_3, "10, 4");

        JLabel lblNewLabel_3 = new JLabel("Maximum");
        panel_1.add(lblNewLabel_3, "2, 6, right, default");

        maxX = new JTextField();
        panel_1.add(maxX, "4, 6, fill, default");
        maxX.setColumns(5);

        maxY = new JTextField();
        panel_1.add(maxY, "6, 6, fill, default");
        maxY.setColumns(5);

        JButton btnNewButton_2 = new JButton(captureMaxCoordinatesAction);
        btnNewButton_2.setHideActionText(true);
        panel_1.add(btnNewButton_2, "8, 6");

        JButton btnNewButton_4 = new JButton(positionMaxCoordinatesAction);
        btnNewButton_4.setHideActionText(true);
        panel_1.add(btnNewButton_4, "10, 6");

        softLimitsEnabled = new JCheckBox("Enabled?");
        panel_1.add(softLimitsEnabled, "2, 8, 7, 1");
        
        JPanel panel_2 = new JPanel();
        panel_2.setBorder(new TitledBorder(null, "Z Probe", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel_2);
        panel_2.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(100dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
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
                ColumnSpec.decode("max(100dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
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

        MutableLocationProxy parkLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, head, "parkLocation", parkLocation, "location");
        addWrappedBinding(parkLocation, "lengthX", parkX, "text", lengthConverter);
        addWrappedBinding(parkLocation, "lengthY", parkY, "text", lengthConverter);

        MutableLocationProxy minLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, head, "minLocation", minLocation, "location");
        addWrappedBinding(minLocation, "lengthX", minX, "text", lengthConverter);
        addWrappedBinding(minLocation, "lengthY", minY, "text", lengthConverter);

        MutableLocationProxy maxLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, head, "maxLocation", maxLocation, "location");
        addWrappedBinding(maxLocation, "lengthX", maxX, "text", lengthConverter);
        addWrappedBinding(maxLocation, "lengthY", maxY, "text", lengthConverter);

        addWrappedBinding(head, "softLimitsEnabled", softLimitsEnabled, "selected");

        addWrappedBinding(head, "zProbeActuatorName", comboBoxZProbeActuator, "selectedItem");
        addWrappedBinding(head, "pumpActuatorName", comboBoxPumpActuator, "selectedItem");

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(parkX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(parkY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(minX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(minY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(maxX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(maxY);
        
    }

    private static Location getParsedLocation(JTextField textFieldX, JTextField textFieldY) {
        double x = 0, y = 0, z = 0, rotation = 0;
        if (textFieldX != null) {
            x = Length.parse(textFieldX.getText())
                      .getValue();
        }
        if (textFieldY != null) {
            y = Length.parse(textFieldY.getText())
                      .getValue();
        }
        return new Location(Configuration.get()
                                         .getSystemUnits(),
                x, y, z, rotation);
    }

    private Action captureParkCoordinatesAction =
            new AbstractAction("Get Camera Coordinates", Icons.captureCamera) {
                {
                    putValue(Action.SHORT_DESCRIPTION,
                            "Capture the location that the camera is centered on.");
                }

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    UiUtils.messageBoxOnException(() -> {
                        Location l = head.getDefaultCamera()
                                         .getLocation();
                        Helpers.copyLocationIntoTextFields(l, parkX, parkY, null, null);
                    });
                }
            };


    private Action positionParkCoordinatesAction =
            new AbstractAction("Position Camera", Icons.centerCamera) {
                {
                    putValue(Action.SHORT_DESCRIPTION,
                            "Position the camera over the center of the location.");
                }

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    UiUtils.submitUiMachineTask(() -> {
                        Camera camera = head.getDefaultCamera();
                        Location location = getParsedLocation(parkX, parkY);
                        MovableUtils.moveToLocationAtSafeZ(camera, location);
                    });
                }
            };

    private Action captureMinCoordinatesAction =
            new AbstractAction("Get Camera Coordinates", Icons.captureCamera) {
                {
                    putValue(Action.SHORT_DESCRIPTION,
                            "Capture the location that the camera is centered on.");
                }

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    UiUtils.messageBoxOnException(() -> {
                        Location l = head.getDefaultCamera()
                                         .getLocation();
                        Helpers.copyLocationIntoTextFields(l, minX, minY, null, null);
                    });
                }
            };

    private Action positionMinCoordinatesAction =
            new AbstractAction("Position Camera", Icons.centerCamera) {
                {
                    putValue(Action.SHORT_DESCRIPTION,
                            "Position the camera over the center of the location.");
                }

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    UiUtils.submitUiMachineTask(() -> {
                        Camera camera = head.getDefaultCamera();
                        Location location = getParsedLocation(minX, minY);
                        MovableUtils.moveToLocationAtSafeZ(camera, location);
                    });
                }
            };

    private Action captureMaxCoordinatesAction =
            new AbstractAction("Get Camera Coordinates", Icons.captureCamera) {
                {
                    putValue(Action.SHORT_DESCRIPTION,
                            "Capture the location that the camera is centered on.");
                }

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    UiUtils.messageBoxOnException(() -> {
                        Location l = head.getDefaultCamera()
                                         .getLocation();
                        Helpers.copyLocationIntoTextFields(l, maxX, maxY, null, null);
                    });
                }
            };
    private Action positionMaxCoordinatesAction =
            new AbstractAction("Position Camera", Icons.centerCamera) {
                {
                    putValue(Action.SHORT_DESCRIPTION,
                            "Position the camera over the center of the location.");
                }

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    UiUtils.submitUiMachineTask(() -> {
                        Camera camera = head.getDefaultCamera();
                        Location location = getParsedLocation(maxX, maxY);
                        MovableUtils.moveToLocationAtSafeZ(camera, location);
                    });
                }
            };


    private JTextField minX;
    private JTextField minY;
    private JTextField maxX;
    private JTextField maxY;
    private JTextField parkX;
    private JTextField parkY;
    private JCheckBox softLimitsEnabled;
    private JComboBox comboBoxZProbeActuator;
    private JComboBox comboBoxPumpActuator;
}
