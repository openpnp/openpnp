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

package org.openpnp.gui.wizards;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.machine.reference.AbstractBroadcastingCamera;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.base.AbstractCamera;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class CameraConfigurationWizard extends AbstractConfigurationWizard {
    private final AbstractBroadcastingCamera camera;

    private JLabel lblLightingActuator;

    private JComboBox lightActuator;

    private JLabel lblAllowMachineActuators;

    private JCheckBox allowMachineActuators;

    private JLabel lblAutoVisible;

    private JCheckBox autoVisible;
    
    private static String uppFormat = "%.8f";

    public CameraConfigurationWizard(AbstractBroadcastingCamera camera) {
        AbstractMachine machine = (AbstractMachine) Configuration.get().getMachine();
        this.camera = camera;

        panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Properties", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
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

        lblName = new JLabel("Name");
        panel.add(lblName, "2, 2, right, default");

        nameTf = new JTextField();
        panel.add(nameTf, "4, 2");
        nameTf.setColumns(20);

        lblLooking = new JLabel("Looking");
        panel.add(lblLooking, "2, 4, right, default");

        lookingCb = new JComboBox(Camera.Looking.values());
        panel.add(lookingCb, "4, 4");

        lblPreviewFps = new JLabel("Preview FPS");
        panel.add(lblPreviewFps, "2, 6, right, default");

        previewFps = new JTextField();
        panel.add(previewFps, "4, 6, fill, default");
        previewFps.setColumns(10);

        lblSuspendDuringTaks = new JLabel("Suspend during taks?");
        lblSuspendDuringTaks.setToolTipText("<html>Continuous camera preview is suspended during machine tasks, only frames<br/>\r\ncaptured using computer vision are shown. For high Preview FPS this improves <br/>\r\nperformance </html>");
        panel.add(lblSuspendDuringTaks, "6, 6, right, default");

        suspendPreviewInTasks = new JCheckBox("");
        panel.add(suspendPreviewInTasks, "8, 6");

        lblAutoVisible = new JLabel("Auto Camera View?");
        lblAutoVisible.setToolTipText("<html>If enabled, the CameraView will be automatically selected whenever a<br/>\r\nuser action is related to the camera or when a computer vision result is presented.</html>");
        panel.add(lblAutoVisible, "2, 8, right, default");

        autoVisible = new JCheckBox("");
        panel.add(autoVisible, "4, 8");

        lblLightingActuator = new JLabel("Ligh Actuator");
        panel.add(lblLightingActuator, "2, 12, right, default");

        lightActuator = new JComboBox();
        panel.add(lightActuator, "4, 12");

        lblAllowMachineActuators = new JLabel("Allow Machine Actuators?");
        panel.add(lblAllowMachineActuators, "6, 12, right, default");
        lblAllowMachineActuators.setToolTipText("<html>It is recommended to attach the Light Actuator to the camera's head.<br/>\r\nHowever, for backwards-compatibility with how Light Actuators were used in<br/>\r\nScripts, you can enable this switch and choose a Machine actuator. \r\n</html>\r\n");

        allowMachineActuators = new JCheckBox("");
        panel.add(allowMachineActuators, "8, 12");
        allowMachineActuators.setToolTipText("<html>It is recommended to attach the Light Actuator to the camera's head.<br/>\r\nHowever, for backwards-compatibility with how Light Actuators were used in<br/>\r\nScripts, you can enable this switch and choose a Machine actuator. \r\n</html>\r\n");

        lblAutoCameraLight = new JLabel("Auto Camera Light?");
        lblAutoCameraLight.setToolTipText("<html>If enabled, the Light Actuator will be automatically be actuated ON whenever a<br/>\r\nuser action is related to the camera.</html>");
        panel.add(lblAutoCameraLight, "2, 14, right, default");

        autoLight = new JCheckBox("");
        panel.add(autoLight, "4, 14");
        allowMachineActuators.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                setActuatorModel(machine, camera);
            }
        });

        panelUpp = new JPanel();
        contentPanel.add(panelUpp);
        panelUpp.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Units Per Pixel", TitledBorder.LEADING, TitledBorder.TOP, null,
                new Color(0, 0, 0)));
        panelUpp.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        lblWidth = new JLabel("Width");
        panelUpp.add(lblWidth, "2, 2");

        lblHeight = new JLabel("Length");
        panelUpp.add(lblHeight, "4, 2");

        lblX = new JLabel("X");
        panelUpp.add(lblX, "6, 2");

        lblY = new JLabel("Y");
        panelUpp.add(lblY, "8, 2");

        textFieldWidth = new JTextField();
        textFieldWidth.setText("1");
        panelUpp.add(textFieldWidth, "2, 4");
        textFieldWidth.setColumns(8);

        textFieldHeight = new JTextField();
        textFieldHeight.setText("1");
        panelUpp.add(textFieldHeight, "4, 4");
        textFieldHeight.setColumns(8);

        textFieldUppX = new JTextField();
        textFieldUppX.setColumns(8);
        panelUpp.add(textFieldUppX, "6, 4, fill, default");

        textFieldUppY = new JTextField();
        textFieldUppY.setColumns(8);
        panelUpp.add(textFieldUppY, "8, 4, fill, default");

        btnMeasure = new JButton("Measure");
        btnMeasure.setAction(measureAction);
        panelUpp.add(btnMeasure, "10, 4");

        btnCancelMeasure = new JButton("Cancel");
        btnCancelMeasure.setAction(cancelMeasureAction);
        panelUpp.add(btnCancelMeasure, "12, 4");

        lblUppInstructions = new JLabel(
                "<html>\n<ol>\n<li>Place an object with a known width and length on the table. Graphing paper is a good, easy choice for this.\n<li>Enter the width and length of the object into the Width and Length fields.\n<li>Jog the camera to where it is centered over the object and in focus.\n<li>Press Measure and use the camera selection rectangle to measure the object. Press Confirm when finished.\n<li>The calculated units per pixel values will be inserted into the X and Y fields.\n</ol>\n</html>");
        panelUpp.add(lblUppInstructions, "2, 6, 10, 1, default, fill");
    }

    protected void setActuatorModel(AbstractMachine machine, AbstractCamera camera) {
        if (camera.getHead() == null) {
            lightActuator.setModel(new ActuatorsComboBoxModel(machine));
            allowMachineActuators.setVisible(false);
            lblAllowMachineActuators.setVisible(false);
        }
        else if (allowMachineActuators.isSelected()) {
            lightActuator.setModel(new ActuatorsComboBoxModel(machine, camera.getHead()));
        }
        else {
            lightActuator.setModel(new ActuatorsComboBoxModel(camera.getHead()));
        }
    }

    @Override
    public void createBindings() {
        AbstractMachine machine = (AbstractMachine) Configuration.get().getMachine();
        LengthConverter lengthConverter = new LengthConverter(uppFormat);
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        NamedConverter<Actuator> actuatorConverter = (camera.getHead() != null ? 
                new NamedConverter<>(machine.getActuators(), camera.getHead().getActuators()) 
                : new NamedConverter<>(machine.getActuators()));
        
        addWrappedBinding(camera, "name", nameTf, "text");
        addWrappedBinding(camera, "looking", lookingCb, "selectedItem");
        addWrappedBinding(camera, "previewFps", previewFps, "text", doubleConverter);
        addWrappedBinding(camera, "suspendPreviewInTasks", suspendPreviewInTasks, "selected");
        addWrappedBinding(camera, "autoVisible", autoVisible, "selected");

        addWrappedBinding(camera, "allowMachineActuators", allowMachineActuators, "selected");
        addWrappedBinding(camera, "lightActuator", lightActuator, "selectedItem", actuatorConverter);
        addWrappedBinding(camera, "autoLight", autoLight, "selected");

        MutableLocationProxy unitsPerPixel = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, camera, "unitsPerPixel", unitsPerPixel, "location");
        addWrappedBinding(unitsPerPixel, "lengthX", textFieldUppX, "text", lengthConverter);
        addWrappedBinding(unitsPerPixel, "lengthY", textFieldUppY, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelect(textFieldUppX);
        ComponentDecorators.decorateWithAutoSelect(textFieldUppY);
        ComponentDecorators.decorateWithLengthConversion(textFieldUppX, uppFormat);
        ComponentDecorators.decorateWithLengthConversion(textFieldUppY, uppFormat);

        ComponentDecorators.decorateWithAutoSelect(nameTf);
        ComponentDecorators.decorateWithAutoSelect(previewFps);
        ComponentDecorators.decorateWithAutoSelect(textFieldWidth);
        ComponentDecorators.decorateWithAutoSelect(textFieldHeight);

        setActuatorModel(machine, camera);
    }

    private Action measureAction = new AbstractAction("Measure") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            btnMeasure.setAction(confirmMeasureAction);
            cancelMeasureAction.setEnabled(true);
            CameraView cameraView = MainFrame.get().getCameraViews().setSelectedCamera(camera);
            cameraView.setSelectionEnabled(true);
            cameraView.setSelection(0, 0, 100, 100);
        }
    };

    private Action confirmMeasureAction = new AbstractAction("Confirm") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            btnMeasure.setAction(measureAction);
            cancelMeasureAction.setEnabled(false);
            CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(camera);
            cameraView.setSelectionEnabled(false);
            Rectangle selection = cameraView.getSelection();
            double width = Double.parseDouble(textFieldWidth.getText());
            double height = Double.parseDouble(textFieldHeight.getText());
            textFieldUppX.setText(String.format(Locale.US, uppFormat, (width / Math.abs(selection.width))));
            textFieldUppY.setText(String.format(Locale.US, uppFormat, (height / Math.abs(selection.height))));
        }
    };

    private Action cancelMeasureAction = new AbstractAction("Cancel") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            btnMeasure.setAction(measureAction);
            cancelMeasureAction.setEnabled(false);
            CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(camera);
            cameraView.setSelectionEnabled(false);
        }
    };

    @Override
    protected void saveToModel() {
        super.saveToModel();
        UiUtils.messageBoxOnException(() -> {
            camera.reinitialize(); 
        });
    }

    private JPanel panelUpp;
    private JButton btnMeasure;
    private JButton btnCancelMeasure;
    private JLabel lblUppInstructions;
    private JTextField textFieldWidth;
    private JTextField textFieldHeight;
    private JTextField textFieldUppX;
    private JTextField textFieldUppY;
    private JLabel lblWidth;
    private JLabel lblHeight;
    private JLabel lblX;
    private JLabel lblY;
    private JPanel panel;
    private JLabel lblName;
    private JLabel lblLooking;
    private JComboBox lookingCb;
    private JTextField nameTf;
    private JLabel lblPreviewFps;
    private JTextField previewFps;
    private JLabel lblSuspendDuringTaks;
    private JCheckBox suspendPreviewInTasks;
    private JLabel lblAutoCameraLight;
    private JCheckBox autoLight;
}
