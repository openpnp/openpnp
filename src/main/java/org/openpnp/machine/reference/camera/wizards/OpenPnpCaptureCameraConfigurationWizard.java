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

package org.openpnp.machine.reference.camera.wizards;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.capture.CaptureDevice;
import org.openpnp.capture.CaptureFormat;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.JBindings;
import org.openpnp.machine.reference.camera.OpenCvCamera.OpenCvCapturePropertyValue;
import org.openpnp.machine.reference.camera.OpenPnpCaptureCamera;
import org.openpnp.machine.reference.wizards.ReferenceCameraConfigurationWizard;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JCheckBox;

@SuppressWarnings("serial")
public class OpenPnpCaptureCameraConfigurationWizard extends ReferenceCameraConfigurationWizard {
    private final OpenPnpCaptureCamera camera;

    private List<OpenCvCapturePropertyValue> properties = new ArrayList<>();
    private JPanel panel;
    private JComboBox deviceCb;
    private JLabel lblDevice;
    private JLabel lblFormat;
    private JComboBox formatCb;
    private JLabel lblExposure;
    private JLabel exposureMin;
    private JTextField exposureValue;
    private JLabel lblMax;
    private JLabel lblMin_1;
    private JLabel exposureMax;
    private JLabel lblValue;
    private JLabel lblWhiteBalance;
    private JLabel lblFocus;
    private JLabel lblZoom;
    private JLabel lblGain;
    private JLabel label;
    private JLabel label_1;
    private JLabel label_2;
    private JLabel label_3;
    private JLabel label_4;
    private JLabel label_5;
    private JLabel label_6;
    private JLabel label_7;
    private JTextField textField;
    private JTextField textField_1;
    private JTextField textField_2;
    private JTextField textField_3;
    private JLabel lblAuto;
    private JCheckBox exposureAuto;

    public OpenPnpCaptureCameraConfigurationWizard(OpenPnpCaptureCamera camera) {
        super(camera);

        this.camera = camera;
        createUi();
    }

    private void createUi() {

        panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "General", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblDevice = new JLabel("Device");
        panel.add(lblDevice, "2, 2, right, default");

        deviceCb = new JComboBox();
        panel.add(deviceCb, "6, 2, 5, 1");

        lblFormat = new JLabel("Format");
        panel.add(lblFormat, "2, 4, right, default");

        formatCb = new JComboBox();
        panel.add(formatCb, "6, 4, 5, 1, fill, default");
        
        lblAuto = new JLabel("Auto");
        panel.add(lblAuto, "4, 6");
        
        lblMin_1 = new JLabel("Min");
        panel.add(lblMin_1, "6, 6");
        
        lblValue = new JLabel("Value");
        panel.add(lblValue, "8, 6, center, default");
        
        lblMax = new JLabel("Max");
        panel.add(lblMax, "10, 6");
        
        lblExposure = new JLabel("Exposure");
        panel.add(lblExposure, "2, 8, right, default");
        
        exposureAuto = new JCheckBox("");
        panel.add(exposureAuto, "4, 8");
        
        exposureMin = new JLabel("min");
        panel.add(exposureMin, "6, 8");
        
        exposureValue = new JTextField();
        exposureValue.setText("value");
        panel.add(exposureValue, "8, 8, fill, default");
        exposureValue.setColumns(10);
        
        exposureMax = new JLabel("max");
        panel.add(exposureMax, "10, 8");
        
        lblWhiteBalance = new JLabel("White Balance");
        panel.add(lblWhiteBalance, "2, 10, right, default");
        
        label = new JLabel("min");
        panel.add(label, "6, 10, right, default");
        
        textField = new JTextField();
        textField.setText("value");
        textField.setColumns(10);
        panel.add(textField, "8, 10, fill, default");
        
        label_4 = new JLabel("max");
        panel.add(label_4, "10, 10");
        
        lblFocus = new JLabel("Focus");
        panel.add(lblFocus, "2, 12, right, default");
        
        label_1 = new JLabel("min");
        panel.add(label_1, "6, 12, right, default");
        
        textField_1 = new JTextField();
        textField_1.setText("value");
        textField_1.setColumns(10);
        panel.add(textField_1, "8, 12, fill, default");
        
        label_5 = new JLabel("max");
        panel.add(label_5, "10, 12");
        
        lblZoom = new JLabel("Zoom");
        panel.add(lblZoom, "2, 14, right, default");
        
        label_2 = new JLabel("min");
        panel.add(label_2, "6, 14, right, default");
        
        textField_2 = new JTextField();
        textField_2.setText("value");
        textField_2.setColumns(10);
        panel.add(textField_2, "8, 14, fill, default");
        
        label_6 = new JLabel("max");
        panel.add(label_6, "10, 14");
        
        lblGain = new JLabel("Gain");
        panel.add(lblGain, "2, 16, right, default");
        
        label_3 = new JLabel("min");
        panel.add(label_3, "6, 16, right, default");
        
        textField_3 = new JTextField();
        textField_3.setText("value");
        textField_3.setColumns(10);
        panel.add(textField_3, "8, 16, fill, default");
        
        label_7 = new JLabel("max");
        panel.add(label_7, "10, 16");

        deviceCb.addActionListener(l -> {
            formatCb.removeAllItems();
            CaptureDevice dev = (CaptureDevice) deviceCb.getSelectedItem();
            if (dev == null) {
                return;
            }
            for (CaptureFormat format : dev.getFormats()) {
                formatCb.addItem(format);
            }
        });

        for (CaptureDevice dev : camera.getCaptureDevices()) {
            deviceCb.addItem(dev);
        }
    }
    
    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        
        super.createBindings();
        
        addWrappedBinding(camera, "device", deviceCb, "selectedItem");
        addWrappedBinding(camera, "format", formatCb, "selectedItem");
        
        bind(UpdateStrategy.READ, camera, "exposureMin", exposureMin, "text", intConverter);
        bind(UpdateStrategy.READ_WRITE, camera, "exposure", exposureValue, "text", intConverter);
        bind(UpdateStrategy.READ, camera, "exposureMax", exposureMax, "text", intConverter);
        bind(UpdateStrategy.READ_WRITE, camera, "exposureAuto", exposureAuto, "selected");
//        bind(UpdateStrategy.READ, camera, "exposureAuto", exposureValue, "enabled");
    }

    @Override
    protected void saveToModel() {
        super.saveToModel();
        camera.open();
    }
}
