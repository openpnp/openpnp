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
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.openpnp.capture.CaptureDevice;
import org.openpnp.machine.reference.camera.OpenCvCamera.OpenCvCapturePropertyValue;
import org.openpnp.machine.reference.camera.OpenPnpCaptureCamera;
import org.openpnp.machine.reference.wizards.ReferenceCameraConfigurationWizard;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

/*
 * TODO: newly added properties don't get set on first apply
 */
@SuppressWarnings("serial")
public class OpenPnpCaptureCameraConfigurationWizard extends ReferenceCameraConfigurationWizard {
    private final OpenPnpCaptureCamera camera;
    
    private List<OpenCvCapturePropertyValue> properties = new ArrayList<>();
    private JPanel panel;
    private JComboBox comboBox;
    
    public OpenPnpCaptureCameraConfigurationWizard(OpenPnpCaptureCamera camera) {
        super(camera);

        this.camera = camera;
        createUi();
    }
    private void createUi() {
        
        panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "General", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        comboBox = new JComboBox();
        panel.add(comboBox, "2, 2, fill, default");

        for (CaptureDevice dev : camera.getCaptureDevices()) {
            comboBox.addItem(dev);
        }
    }
}
