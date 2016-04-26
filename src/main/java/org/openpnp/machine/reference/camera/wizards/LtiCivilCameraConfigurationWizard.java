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

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.openpnp.machine.reference.camera.LtiCivilCamera;
import org.openpnp.machine.reference.wizards.ReferenceCameraConfigurationWizard;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class LtiCivilCameraConfigurationWizard extends ReferenceCameraConfigurationWizard {
    private final LtiCivilCamera camera;

    private JPanel panelGeneral;
    private JComboBox comboBoxDeviceId;
    private JCheckBox chckbxForceGrayscale;

    public LtiCivilCameraConfigurationWizard(LtiCivilCamera camera) {
        super(camera);

        this.camera = camera;

        panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        panelGeneral.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "General", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelGeneral.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblDeviceId = new JLabel("Device ID");
        panelGeneral.add(lblDeviceId, "2, 2, right, default");

        Object[] deviceIds = null;
        try {
            deviceIds = camera.getDeviceIds().toArray(new String[] {});
        }
        catch (Exception e) {
            // TODO:
        }
        comboBoxDeviceId = new JComboBox(deviceIds);
        panelGeneral.add(comboBoxDeviceId, "4, 2, left, default");

        chckbxForceGrayscale = new JCheckBox("Force Grayscale?");
        panelGeneral.add(chckbxForceGrayscale, "2, 4, 3, 1");
    }

    @Override
    public void createBindings() {
        super.createBindings();
        // The order of the properties is important. We want all the booleans
        // to be set before we set the driver because setting the driver
        // applies all the settings.
        addWrappedBinding(camera, "forceGrayscale", chckbxForceGrayscale, "selected");
        addWrappedBinding(camera, "deviceId", comboBoxDeviceId, "selectedItem");
    }

}
