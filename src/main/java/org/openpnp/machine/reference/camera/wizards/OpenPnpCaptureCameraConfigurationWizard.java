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

import org.openpnp.capture.CaptureDevice;
import org.openpnp.capture.CaptureDevice.CaptureFormat;
import org.openpnp.machine.reference.camera.OpenCvCamera.OpenCvCapturePropertyValue;
import org.openpnp.machine.reference.camera.OpenPnpCaptureCamera;
import org.openpnp.machine.reference.wizards.ReferenceCameraConfigurationWizard;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class OpenPnpCaptureCameraConfigurationWizard extends ReferenceCameraConfigurationWizard {
    private final OpenPnpCaptureCamera camera;

    private List<OpenCvCapturePropertyValue> properties = new ArrayList<>();
    private JPanel panel;
    private JComboBox deviceCb;
    private JLabel lblDevice;
    private JLabel lblFormat;
    private JComboBox formatCb;

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
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        lblDevice = new JLabel("Device");
        panel.add(lblDevice, "2, 2, right, default");

        deviceCb = new JComboBox();
        panel.add(deviceCb, "4, 2");

        lblFormat = new JLabel("Format");
        panel.add(lblFormat, "2, 4, right, default");

        formatCb = new JComboBox();
        panel.add(formatCb, "4, 4, fill, default");

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
        super.createBindings();
        
        addWrappedBinding(camera, "device", deviceCb, "selectedItem");
        addWrappedBinding(camera, "format", formatCb, "selectedItem");
    }

    @Override
    protected void saveToModel() {
        super.saveToModel();
        camera.open();
    }
}
