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

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.Converter;
import org.openpnp.capture.CaptureDevice;
import org.openpnp.capture.CaptureFormat;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.camera.OpenCvCamera.OpenCvCapturePropertyValue;
import org.openpnp.machine.reference.camera.OpenPnpCaptureCamera;
import org.openpnp.model.Configuration;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.SwingConstants;
import javax.swing.JTextArea;
import javax.swing.UIManager;

@SuppressWarnings("serial")
public class OpenPnpCaptureCameraConfigurationWizard extends AbstractConfigurationWizard {
    private final OpenPnpCaptureCamera camera;

    private List<OpenCvCapturePropertyValue> properties = new ArrayList<>();
    private JPanel panelProperties;
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
    private JSlider focusSlider;
    private JSlider exposureSlider;
    private JSlider whiteBalanceSlider;
    private JSlider zoomSlider;
    private JCheckBox whiteBalanceAuto;
    private JCheckBox gainAuto;
    private JSlider gainSlider;
    private JLabel lblFps;
    private JTextField fps;
    private JTextField exposureValue;
    private JTextField whiteBalanceValue;
    private JTextField focusValue;
    private JTextField zoomValue;
    private JTextField gainValue;
    private JLabel brightness;
    private JCheckBox brightnessAuto;
    private JLabel brightnessMin;
    private JSlider brightnessSlider;
    private JTextField brightnessValue;
    private JLabel brightnessMax;
    private JLabel contrast;
    private JCheckBox contrastAuto;
    private JLabel contrastMin;
    private JSlider contrastSlider;
    private JTextField contrastValue;
    private JLabel contrastMax;
    private JLabel gamma;
    private JCheckBox gammaAuto;
    private JLabel gammaMin;
    private JSlider gammaSlider;
    private JTextField gammaValue;
    private JLabel gammaMax;
    private JLabel saturation;
    private JCheckBox saturationAuto;
    private JLabel saturationMin;
    private JSlider saturationSlider;
    private JTextField saturationValue;
    private JLabel saturationMax;
    private JLabel lblDefault;
    private JLabel brightnessDefault;
    private JLabel contrastDefault;
    private JLabel exposureDefault;
    private JLabel focusDefault;
    private JLabel gainDefault;
    private JLabel gammaDefault;
    private JLabel saturationDefault;
    private JLabel whiteBalanceDefault;
    private JLabel zoomDefault;
    private JLabel backLightCompensation;
    private JLabel backLightCompensationMin;
    private JSlider backLightCompensationSlider;
    private JTextField backLightCompensationValue;
    private JLabel backLightCompensationMax;
    private JLabel backLightCompensationDefault;
    private JCheckBox backLightCompensationAuto;
    private JLabel hue;
    private JCheckBox hueAuto;
    private JLabel hueMin;
    private JSlider hueSlider;
    private JTextField hueValue;
    private JLabel hueMax;
    private JLabel hueDefault;
    private JLabel powerLineFrequency;
    private JCheckBox powerLineFrequencyAuto;
    private JLabel powerLineFrequencyMin;
    private JSlider powerLineFrequencySlider;
    private JTextField powerLineFrequencyValue;
    private JLabel powerLineFrequencyMax;
    private JLabel powerLineFrequencyDefault;
    private JLabel sharpness;
    private JCheckBox sharpnessAuto;
    private JLabel sharpnessMin;
    private JSlider sharpnessSlider;
    private JTextField sharpnessValue;
    private JLabel sharpnessMax;
    private JLabel sharpnessDefault;
    private JPanel panelGeneral;

    public OpenPnpCaptureCameraConfigurationWizard(OpenPnpCaptureCamera camera) {
        this.camera = camera;
        createUi();
    }

    private void createUi() {
        
        panelGeneral = new JPanel();
        panelGeneral.setBorder(new TitledBorder(null, "Device", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelGeneral);
        panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("left:default"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
                lblDevice = new JLabel("Device");
                panelGeneral.add(lblDevice, "2, 2, right, default");
                
                        deviceCb = new JComboBox();
                        panelGeneral.add(deviceCb, "4, 2");
                        
                                lblFormat = new JLabel("Format");
                                panelGeneral.add(lblFormat, "2, 4, right, default");
                                
                                        formatCb = new JComboBox();
                                        panelGeneral.add(formatCb, "4, 4");
                                        
                                                lblFps = new JLabel("Preview FPS");
                                                panelGeneral.add(lblFps, "2, 6, right, default");
                                                
                                                        fps = new JTextField();
                                                        fps.setToolTipText("<html>\nFrame rate for live camera view. Lower uses less CPU and does not affect vision speed.\n<br>0 will update the camera view only when vision or machine movement happens.\n<br>10 FPS is a good starting point.\n</html>\n");
                                                        panelGeneral.add(fps, "4, 6");
                                                        fps.setColumns(10);
                        
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

        panelProperties = new JPanel();
        panelProperties.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Properties", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(panelProperties);
        panelProperties.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("left:default"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("center:default"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
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

        lblAuto = new JLabel("Auto");
        panelProperties.add(lblAuto, "4, 2, center, default");

        lblMin_1 = new JLabel("Min");
        panelProperties.add(lblMin_1, "8, 2, center, default");

        lblValue = new JLabel("Value");
        panelProperties.add(lblValue, "12, 2, 3, 1, center, default");

        lblMax = new JLabel("Max");
        panelProperties.add(lblMax, "18, 2, center, default");

        lblDefault = new JLabel("Default");
        panelProperties.add(lblDefault, "22, 2");

        brightness = new JLabel("Brightness");
        panelProperties.add(brightness, "2, 4, right, default");

        brightnessAuto = new JCheckBox("");
        panelProperties.add(brightnessAuto, "4, 4");

        brightnessMin = new JLabel("min");
        panelProperties.add(brightnessMin, "8, 4");

        brightnessSlider = new JSlider();
        brightnessSlider.setPaintTicks(true);
        brightnessSlider.setPaintLabels(true);
        panelProperties.add(brightnessSlider, "12, 4");

        brightnessValue = new JTextField();
        brightnessValue.setText("00000");
        brightnessValue.setColumns(5);
        panelProperties.add(brightnessValue, "14, 4, center, default");

        brightnessMax = new JLabel("max");
        panelProperties.add(brightnessMax, "18, 4");

        brightnessDefault = new JLabel("def");
        panelProperties.add(brightnessDefault, "22, 4");
        
        backLightCompensation = new JLabel("Backlight Compensation");
        panelProperties.add(backLightCompensation, "2, 6, right, default");
        
        backLightCompensationAuto = new JCheckBox("");
        panelProperties.add(backLightCompensationAuto, "4, 6");
        
        backLightCompensationMin = new JLabel("min");
        panelProperties.add(backLightCompensationMin, "8, 6");
        
        backLightCompensationSlider = new JSlider();
        backLightCompensationSlider.setPaintTicks(true);
        backLightCompensationSlider.setPaintLabels(true);
        panelProperties.add(backLightCompensationSlider, "12, 6");
        
        backLightCompensationValue = new JTextField();
        backLightCompensationValue.setText("00000");
        backLightCompensationValue.setColumns(5);
        panelProperties.add(backLightCompensationValue, "14, 6, center, default");
        
        backLightCompensationMax = new JLabel("max");
        panelProperties.add(backLightCompensationMax, "18, 6");
        
        backLightCompensationDefault = new JLabel("def");
        panelProperties.add(backLightCompensationDefault, "22, 6");

        contrast = new JLabel("Contrast");
        panelProperties.add(contrast, "2, 8, right, default");

        contrastAuto = new JCheckBox("");
        panelProperties.add(contrastAuto, "4, 8");

        contrastMin = new JLabel("min");
        panelProperties.add(contrastMin, "8, 8");

        contrastSlider = new JSlider();
        contrastSlider.setPaintTicks(true);
        contrastSlider.setPaintLabels(true);
        panelProperties.add(contrastSlider, "12, 8");

        contrastValue = new JTextField();
        contrastValue.setText("00000");
        contrastValue.setColumns(5);
        panelProperties.add(contrastValue, "14, 8, center, default");

        contrastMax = new JLabel("max");
        panelProperties.add(contrastMax, "18, 8");

        contrastDefault = new JLabel("def");
        panelProperties.add(contrastDefault, "22, 8");

        exposure = new JLabel("Exposure");
        panelProperties.add(exposure, "2, 10, right, default");

        exposureAuto = new JCheckBox("");
        panelProperties.add(exposureAuto, "4, 10, center, default");

        exposureMin = new JLabel("min");
        panelProperties.add(exposureMin, "8, 10, center, default");

        exposureSlider = new JSlider();
        exposureSlider.setPaintLabels(true);
        exposureSlider.setPaintTicks(true);
        panelProperties.add(exposureSlider, "12, 10, center, default");

        exposureValue = new JTextField();
        exposureValue.setText("00000");
        panelProperties.add(exposureValue, "14, 10, center, default");
        exposureValue.setColumns(5);

        exposureMax = new JLabel("max");
        panelProperties.add(exposureMax, "18, 10, center, default");

        exposureDefault = new JLabel("def");
        panelProperties.add(exposureDefault, "22, 10");

        focusDefault = new JLabel("def");
        panelProperties.add(focusDefault, "22, 12");

        gainDefault = new JLabel("def");
        panelProperties.add(gainDefault, "22, 14");

        gamma = new JLabel("Gamma");
        panelProperties.add(gamma, "2, 16, right, default");

        gammaAuto = new JCheckBox("");
        panelProperties.add(gammaAuto, "4, 16");

        gammaMin = new JLabel("min");
        panelProperties.add(gammaMin, "8, 16");

        gammaSlider = new JSlider();
        gammaSlider.setPaintTicks(true);
        gammaSlider.setPaintLabels(true);
        panelProperties.add(gammaSlider, "12, 16");

        gammaValue = new JTextField();
        gammaValue.setText("00000");
        gammaValue.setColumns(5);
        panelProperties.add(gammaValue, "14, 16, center, default");

        gammaMax = new JLabel("max");
        panelProperties.add(gammaMax, "18, 16");

        gammaDefault = new JLabel("def");
        panelProperties.add(gammaDefault, "22, 16");
        
        hue = new JLabel("Hue");
        panelProperties.add(hue, "2, 18, right, default");
        
        hueAuto = new JCheckBox("");
        panelProperties.add(hueAuto, "4, 18");
        
        hueMin = new JLabel("min");
        panelProperties.add(hueMin, "8, 18");
        
        hueSlider = new JSlider();
        hueSlider.setPaintTicks(true);
        hueSlider.setPaintLabels(true);
        panelProperties.add(hueSlider, "12, 18");
        
        hueValue = new JTextField();
        hueValue.setText("00000");
        hueValue.setColumns(5);
        panelProperties.add(hueValue, "14, 18, center, default");
        
        hueMax = new JLabel("max");
        panelProperties.add(hueMax, "18, 18");
        
        hueDefault = new JLabel("def");
        panelProperties.add(hueDefault, "22, 18");
        
        powerLineFrequency = new JLabel("Power Line Freq.");
        panelProperties.add(powerLineFrequency, "2, 20, right, default");
        
        powerLineFrequencyAuto = new JCheckBox("");
        panelProperties.add(powerLineFrequencyAuto, "4, 20");
        
        powerLineFrequencyMin = new JLabel("min");
        panelProperties.add(powerLineFrequencyMin, "8, 20");
        
        powerLineFrequencySlider = new JSlider();
        powerLineFrequencySlider.setPaintTicks(true);
        powerLineFrequencySlider.setPaintLabels(true);
        panelProperties.add(powerLineFrequencySlider, "12, 20");
        
        powerLineFrequencyValue = new JTextField();
        powerLineFrequencyValue.setText("00000");
        powerLineFrequencyValue.setColumns(5);
        panelProperties.add(powerLineFrequencyValue, "14, 20, center, default");
        
        powerLineFrequencyMax = new JLabel("max");
        panelProperties.add(powerLineFrequencyMax, "18, 20");
        
        powerLineFrequencyDefault = new JLabel("def");
        panelProperties.add(powerLineFrequencyDefault, "22, 20");

        saturation = new JLabel("Saturation");
        panelProperties.add(saturation, "2, 22, right, default");

        saturationAuto = new JCheckBox("");
        panelProperties.add(saturationAuto, "4, 22");

        saturationMin = new JLabel("min");
        panelProperties.add(saturationMin, "8, 22");

        saturationSlider = new JSlider();
        saturationSlider.setPaintTicks(true);
        saturationSlider.setPaintLabels(true);
        panelProperties.add(saturationSlider, "12, 22");

        saturationValue = new JTextField();
        saturationValue.setText("00000");
        saturationValue.setColumns(5);
        panelProperties.add(saturationValue, "14, 22, center, default");

        saturationMax = new JLabel("max");
        panelProperties.add(saturationMax, "18, 22");

        saturationDefault = new JLabel("def");
        panelProperties.add(saturationDefault, "22, 22");
        
        sharpness = new JLabel("Sharpness");
        panelProperties.add(sharpness, "2, 24, right, default");
        
        sharpnessAuto = new JCheckBox("");
        panelProperties.add(sharpnessAuto, "4, 24");
        
        sharpnessMin = new JLabel("min");
        panelProperties.add(sharpnessMin, "8, 24");
        
        sharpnessSlider = new JSlider();
        sharpnessSlider.setPaintTicks(true);
        sharpnessSlider.setPaintLabels(true);
        panelProperties.add(sharpnessSlider, "12, 24");
        
        sharpnessValue = new JTextField();
        sharpnessValue.setText("00000");
        sharpnessValue.setColumns(5);
        panelProperties.add(sharpnessValue, "14, 24, center, default");
        
        sharpnessMax = new JLabel("max");
        panelProperties.add(sharpnessMax, "18, 24");
        
        sharpnessDefault = new JLabel("def");
        panelProperties.add(sharpnessDefault, "22, 24");

        whiteBalance = new JLabel("White Balance");
        panelProperties.add(whiteBalance, "2, 26, right, default");

        whiteBalanceAuto = new JCheckBox("");
        panelProperties.add(whiteBalanceAuto, "4, 26, center, default");

        whiteBalanceMin = new JLabel("min");
        panelProperties.add(whiteBalanceMin, "8, 26, center, default");

        whiteBalanceSlider = new JSlider();
        whiteBalanceSlider.setPaintTicks(true);
        whiteBalanceSlider.setPaintLabels(true);
        panelProperties.add(whiteBalanceSlider, "12, 26, center, default");

        whiteBalanceValue = new JTextField();
        whiteBalanceValue.setText("00000");
        whiteBalanceValue.setColumns(5);
        panelProperties.add(whiteBalanceValue, "14, 26, center, default");

        whiteBalanceMax = new JLabel("max");
        panelProperties.add(whiteBalanceMax, "18, 26, center, default");

        focus = new JLabel("Focus");
        panelProperties.add(focus, "2, 12, right, default");

        focusAuto = new JCheckBox("");
        panelProperties.add(focusAuto, "4, 12, center, default");

        focusMin = new JLabel("min");
        panelProperties.add(focusMin, "8, 12, center, default");

        focusSlider = new JSlider();
        focusSlider.setPaintTicks(true);
        focusSlider.setPaintLabels(true);
        panelProperties.add(focusSlider, "12, 12, center, default");

        focusValue = new JTextField();
        focusValue.setText("00000");
        focusValue.setColumns(5);
        panelProperties.add(focusValue, "14, 12, center, default");

        focusMax = new JLabel("max");
        panelProperties.add(focusMax, "18, 12, center, default");

        whiteBalanceDefault = new JLabel("def");
        panelProperties.add(whiteBalanceDefault, "22, 26");

        zoom = new JLabel("Zoom");
        panelProperties.add(zoom, "2, 28, right, default");

        zoomAuto = new JCheckBox("");
        panelProperties.add(zoomAuto, "4, 28, center, default");

        zoomMin = new JLabel("min");
        panelProperties.add(zoomMin, "8, 28, center, default");

        zoomSlider = new JSlider();
        zoomSlider.setPaintTicks(true);
        zoomSlider.setPaintLabels(true);
        panelProperties.add(zoomSlider, "12, 28, center, default");

        zoomValue = new JTextField();
        zoomValue.setText("00000");
        zoomValue.setColumns(5);
        panelProperties.add(zoomValue, "14, 28, center, default");

        zoomMax = new JLabel("max");
        panelProperties.add(zoomMax, "18, 28, center, default");

        gain = new JLabel("Gain");
        panelProperties.add(gain, "2, 14, right, default");

        gainAuto = new JCheckBox("");
        panelProperties.add(gainAuto, "4, 14, center, default");

        gainMin = new JLabel("min");
        panelProperties.add(gainMin, "8, 14, center, default");

        gainSlider = new JSlider();
        gainSlider.setPaintTicks(true);
        gainSlider.setPaintLabels(true);
        panelProperties.add(gainSlider, "12, 14, center, default");

        gainValue = new JTextField();
        gainValue.setText("00000");
        gainValue.setColumns(5);
        panelProperties.add(gainValue, "14, 14, center, default");

        gainMax = new JLabel("max");
        panelProperties.add(gainMax, "18, 14, center, default");

        zoomDefault = new JLabel("def");
        panelProperties.add(zoomDefault, "22, 28");

        for (CaptureDevice dev : camera.getCaptureDevices()) {
            deviceCb.addItem(dev);
        }
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        addWrappedBinding(camera, "device", deviceCb, "selectedItem");
        addWrappedBinding(camera, "format", formatCb, "selectedItem");

        addWrappedBinding(camera, "fps", fps, "text", doubleConverter);

        bindProperty("backLightCompensation", backLightCompensationAuto, backLightCompensationMin, 
                backLightCompensationMax, backLightCompensationSlider,
                backLightCompensation, backLightCompensationValue, backLightCompensationDefault);
        bindProperty("brightness", brightnessAuto, brightnessMin, brightnessMax, brightnessSlider,
                brightness, brightnessValue, brightnessDefault);
        bindProperty("contrast", contrastAuto, contrastMin, contrastMax, contrastSlider, contrast,
                contrastValue, contrastDefault);
        bindProperty("exposure", exposureAuto, exposureMin, exposureMax, exposureSlider, exposure,
                exposureValue, exposureDefault);
        bindProperty("focus", focusAuto, focusMin, focusMax, focusSlider, focus, focusValue, focusDefault);
        bindProperty("gain", gainAuto, gainMin, gainMax, gainSlider, gain, gainValue, gainDefault);
        bindProperty("gamma", gammaAuto, gammaMin, gammaMax, gammaSlider, gamma, gammaValue, gammaDefault);
        bindProperty("hue", hueAuto, hueMin, hueMax, hueSlider, hue, hueValue, hueDefault);
        bindProperty("powerLineFrequency", powerLineFrequencyAuto, powerLineFrequencyMin, 
                powerLineFrequencyMax, powerLineFrequencySlider, powerLineFrequency, powerLineFrequencyValue, powerLineFrequencyDefault);
        bindProperty("saturation", saturationAuto, saturationMin, saturationMax, saturationSlider,
                saturation, saturationValue, saturationDefault);
        bindProperty("sharpness", sharpnessAuto, sharpnessMin, sharpnessMax, sharpnessSlider,
                sharpness, sharpnessValue, sharpnessDefault);
        bindProperty("whiteBalance", whiteBalanceAuto, whiteBalanceMin, whiteBalanceMax,
                whiteBalanceSlider, whiteBalance, whiteBalanceValue, whiteBalanceDefault);
        bindProperty("zoom", zoomAuto, zoomMin, zoomMax, zoomSlider, zoom, zoomValue, zoomDefault);

        ComponentDecorators.decorateWithAutoSelect(fps);
    }

    private void bindProperty(String property, JCheckBox auto, JLabel min, JLabel max,
            JSlider slider, JLabel label, JTextField value, JLabel def) {
        IntegerConverter intConverter = new IntegerConverter();

        bind(UpdateStrategy.READ_WRITE, camera, property + ".auto", auto, "selected");

        bind(UpdateStrategy.READ, camera, property + ".min", min, "text", intConverter);
        bind(UpdateStrategy.READ, camera, property + ".max", max, "text", intConverter);
        bind(UpdateStrategy.READ, camera, property + ".default", def, "text", intConverter);

        bind(UpdateStrategy.READ, camera, property + ".min", slider, "minimum");
        bind(UpdateStrategy.READ, camera, property + ".max", slider, "maximum");
        bind(UpdateStrategy.READ_WRITE, camera, property + ".value", slider, "value");
        bind(UpdateStrategy.READ_WRITE, camera, property + ".value", value, "text", intConverter);

        bind(UpdateStrategy.READ, camera, property + ".autoSupported", auto, "enabled");
        bind(UpdateStrategy.READ, camera, property + ".supported", slider, "enabled");
        bind(UpdateStrategy.READ, camera, property + ".supported", label, "enabled");
        bind(UpdateStrategy.READ, camera, property + ".supported", value, "enabled");
        
        ComponentDecorators.decorateWithAutoSelect(value);
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
