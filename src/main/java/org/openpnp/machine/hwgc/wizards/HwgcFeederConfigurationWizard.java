/*
 * Copyright (C) 2026 mcix
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
import org.openpnp.machine.hwgc.HwgcFeeder;

@SuppressWarnings("serial")
public class HwgcFeederConfigurationWizard extends AbstractConfigurationWizard {
    private final HwgcFeeder feeder;
    private JTextField feederNumberField;
    private JTextField feedDurationField;

    public HwgcFeederConfigurationWizard(HwgcFeeder feeder) {
        this.feeder = feeder;

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null,
                "HWGC Feeder", TitledBorder.LEADING, TitledBorder.TOP,
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
                    FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC, }));

        panel.add(new JLabel("Feeder Number (0-49)"), "2, 2, right, default");
        feederNumberField = new JTextField(10);
        panel.add(feederNumberField, "4, 2, fill, default");

        panel.add(new JLabel("Feed Duration (ms)"), "2, 4, right, default");
        feedDurationField = new JTextField(10);
        panel.add(feedDurationField, "4, 4, fill, default");
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        addWrappedBinding(feeder, "feederNumber", feederNumberField, "text", intConverter);
        addWrappedBinding(feeder, "feedDurationMs", feedDurationField, "text", intConverter);
    }
}
