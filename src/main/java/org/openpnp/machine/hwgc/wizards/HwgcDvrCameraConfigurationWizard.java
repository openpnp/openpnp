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
import org.openpnp.machine.hwgc.HwgcDvrCamera;

@SuppressWarnings("serial")
public class HwgcDvrCameraConfigurationWizard extends AbstractConfigurationWizard {
    private final HwgcDvrCamera camera;
    private JTextField channelField;

    public HwgcDvrCameraConfigurationWizard(HwgcDvrCamera camera) {
        this.camera = camera;

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null,
                "HWGC DVR Camera", TitledBorder.LEADING, TitledBorder.TOP,
                null, new Color(0, 0, 0)));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(
                new ColumnSpec[] {
                    FormSpecs.RELATED_GAP_COLSPEC,
                    FormSpecs.DEFAULT_COLSPEC,
                    FormSpecs.RELATED_GAP_COLSPEC,
                    FormSpecs.DEFAULT_COLSPEC,
                    FormSpecs.RELATED_GAP_COLSPEC, },
                new RowSpec[] {
                    FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC, }));

        panel.add(new JLabel("DVR Channel (0-7)"), "2, 2, right, default");
        channelField = new JTextField(10);
        panel.add(channelField, "4, 2, fill, default");
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        addWrappedBinding(camera, "channel", channelField, "text", intConverter);
    }
}
