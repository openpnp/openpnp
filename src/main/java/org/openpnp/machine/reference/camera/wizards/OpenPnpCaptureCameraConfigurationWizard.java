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
import org.jdesktop.beansbinding.Converter;
import org.openpnp.capture.CaptureDevice;
import org.openpnp.capture.CaptureFormat;
import org.openpnp.gui.support.AbstractConfigurationWizard;
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
import javax.swing.JSpinner;
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
    private JLabel exposure;
    private JLabel exposureMin;
    private JLabel lblMax;
    private JLabel lblMin_1;
    private JLabel exposureMax;
    private JLabel lblValue;
    private JLabel whiteBalance;
    private JLabel focus;
    private JLabel zoom;
    private JLabel gain;
    private JLabel whiteBalanceMin;
    private JLabel focusMin;
    private JLabel zoomMin;
    private JLabel gainMin;
    private JLabel whiteBalanceMax;
    private JLabel focusMax;
    private JLabel zoomMax;
    private JLabel gainMax;
    private JLabel lblAuto;
    private JCheckBox exposureAuto;
    private JCheckBox zoomAuto;
    private JCheckBox focusAuto;
    private JSlider focusValue;
    private JSlider exposureValue;
    private JSlider whiteBalanceValue;
    private JSlider zoomValue;
    private JCheckBox whiteBalanceAuto;
    private JCheckBox gainAuto;
    private JSlider gainValue;

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
        panel.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

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

        exposure = new JLabel("Exposure");
        panel.add(exposure, "2, 8, right, default");

        exposureAuto = new JCheckBox("");
        panel.add(exposureAuto, "4, 8");

        exposureMin = new JLabel("min");
        panel.add(exposureMin, "6, 8, right, default");

        exposureValue = new JSlider();
        exposureValue.setPaintLabels(true);
        exposureValue.setPaintTicks(true);
        panel.add(exposureValue, "8, 8");

        exposureMax = new JLabel("max");
        panel.add(exposureMax, "10, 8");

        whiteBalance = new JLabel("White Balance");
        panel.add(whiteBalance, "2, 10, right, default");

        whiteBalanceAuto = new JCheckBox("");
        panel.add(whiteBalanceAuto, "4, 10");

        whiteBalanceMin = new JLabel("min");
        panel.add(whiteBalanceMin, "6, 10, right, default");

        whiteBalanceValue = new JSlider();
        whiteBalanceValue.setPaintTicks(true);
        whiteBalanceValue.setPaintLabels(true);
        panel.add(whiteBalanceValue, "8, 10");

        whiteBalanceMax = new JLabel("max");
        panel.add(whiteBalanceMax, "10, 10");

        focus = new JLabel("Focus");
        panel.add(focus, "2, 12, right, default");

        focusAuto = new JCheckBox("");
        panel.add(focusAuto, "4, 12");

        focusMin = new JLabel("min");
        panel.add(focusMin, "6, 12, right, default");

        focusValue = new JSlider();
        focusValue.setPaintTicks(true);
        focusValue.setPaintLabels(true);
        panel.add(focusValue, "8, 12");

        focusMax = new JLabel("max");
        panel.add(focusMax, "10, 12");

        zoom = new JLabel("Zoom");
        panel.add(zoom, "2, 14, right, default");

        zoomAuto = new JCheckBox("");
        panel.add(zoomAuto, "4, 14");

        zoomMin = new JLabel("min");
        panel.add(zoomMin, "6, 14, right, default");

        zoomValue = new JSlider();
        zoomValue.setPaintTicks(true);
        zoomValue.setPaintLabels(true);
        panel.add(zoomValue, "8, 14");

        zoomMax = new JLabel("max");
        panel.add(zoomMax, "10, 14");

        gain = new JLabel("Gain");
        panel.add(gain, "2, 16, right, default");

        gainAuto = new JCheckBox("");
        panel.add(gainAuto, "4, 16");

        gainMin = new JLabel("min");
        panel.add(gainMin, "6, 16, right, default");

        gainValue = new JSlider();
        gainValue.setPaintTicks(true);
        gainValue.setPaintLabels(true);
        panel.add(gainValue, "8, 16");

        gainMax = new JLabel("max");
        panel.add(gainMax, "10, 16");

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

        // super.createBindings();

        addWrappedBinding(camera, "device", deviceCb, "selectedItem");
        addWrappedBinding(camera, "format", formatCb, "selectedItem");

        bindProperty("focus", focusAuto, focusMin, focusMax, focusValue, focus);
        bindProperty("whiteBalance", whiteBalanceAuto, whiteBalanceMin, whiteBalanceMax,
                whiteBalanceValue, whiteBalance);
        bindProperty("gain", gainAuto, gainMin, gainMax, gainValue, gain);
        bindProperty("exposure", exposureAuto, exposureMin, exposureMax, exposureValue, exposure);
        bindProperty("zoom", zoomAuto, zoomMin, zoomMax, zoomValue, zoom);
    }

    private void bindProperty(String property, JCheckBox auto, JLabel min, JLabel max,
            JSlider value, JLabel label) {
        IntegerConverter intConverter = new IntegerConverter();
        
        bind(UpdateStrategy.READ_WRITE, camera, property + ".auto", auto, "selected");

        bind(UpdateStrategy.READ, camera, property + ".min", min, "text", intConverter);
        bind(UpdateStrategy.READ, camera, property + ".max", max, "text", intConverter);
        
        bind(UpdateStrategy.READ, camera, property + ".min", value, "minimum");
        bind(UpdateStrategy.READ, camera, property + ".max", value, "maximum");
        bind(UpdateStrategy.READ_WRITE, camera, property + ".value", value, "value");
        
        bind(UpdateStrategy.READ, camera, property + ".supported", value, "enabled");
        bind(UpdateStrategy.READ, camera, property + ".supported", auto, "enabled");
        bind(UpdateStrategy.READ, camera, property + ".supported", label, "enabled");
    }

    @Override
    protected void saveToModel() {
        super.saveToModel();
        camera.open();
    }
    
    class BooleanInverter extends Converter<Boolean, Boolean> {
        @Override
        public Boolean convertForward(Boolean arg0) {
            return !arg0;
        }

        @Override
        public Boolean convertReverse(Boolean arg0) {
            return !arg0;
        }
    }
}
