/*
 * Copyright (C) 2026 Contributed by Arnoud @ DeltaProto <arnoud@deltaproto.com>
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

package org.openpnp.machine.hwgc.wizards;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.hwgc.HwgcUsb3Camera;

@SuppressWarnings("serial")
public class HwgcUsb3CameraConfigurationWizard extends AbstractConfigurationWizard {
    private final HwgcUsb3Camera camera;
    private JTextField sdkPathField;
    private JTextField captureTimeoutField;

    public HwgcUsb3CameraConfigurationWizard(HwgcUsb3Camera camera) {
        this.camera = camera;

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null,
                "HWGC USB3 Camera (ChinaVision SDK)", TitledBorder.LEADING, TitledBorder.TOP,
                null, new Color(0, 0, 0)));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(
                new ColumnSpec[] {
                    FormSpecs.RELATED_GAP_COLSPEC,
                    FormSpecs.DEFAULT_COLSPEC,
                    FormSpecs.RELATED_GAP_COLSPEC,
                    ColumnSpec.decode("200dlu"),
                    FormSpecs.RELATED_GAP_COLSPEC, },
                new RowSpec[] {
                    FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC, }));

        panel.add(new JLabel("SDK Path (MVCAMSDK_X64.dll)"), "2, 2, right, default");
        sdkPathField = new JTextField(40);
        panel.add(sdkPathField, "4, 2, fill, default");

        panel.add(new JLabel("Capture Timeout (ms)"), "2, 4, right, default");
        captureTimeoutField = new JTextField(10);
        panel.add(captureTimeoutField, "4, 4, fill, default");
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        addWrappedBinding(camera, "sdkPath", sdkPathField, "text");
        addWrappedBinding(camera, "captureTimeoutMs", captureTimeoutField, "text", intConverter);
    }
}
