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
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.hwgc.HwgcDriver;

@SuppressWarnings("serial")
public class HwgcDriverConfigurationWizard extends AbstractConfigurationWizard {
    private final HwgcDriver driver;

    private JTextField maxXField;
    private JTextField maxYField;
    private JTextField maxZField;
    private JTextField nozzleCountField;
    private JTextField scaleXField;
    private JTextField scaleYField;
    private JTextField scaleZField;
    private JTextField scaleAField;
    private JTextField blowDurationField;
    private JTextField comTypeField;
    private JTextField motionToleranceField;

    public HwgcDriverConfigurationWizard(HwgcDriver driver) {
        this.driver = driver;

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null,
                "HWGC Machine Settings", TitledBorder.LEADING, TitledBorder.TOP,
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
                    FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC, }));

        int row = 2;

        panel.add(new JLabel("Hardware Generation (comType)"), "2, " + row + ", right, default");
        comTypeField = new JTextField(10);
        panel.add(comTypeField, "4, " + row + ", fill, default");
        row += 2;

        panel.add(new JLabel("Nozzle Count"), "2, " + row + ", right, default");
        nozzleCountField = new JTextField(10);
        panel.add(nozzleCountField, "4, " + row + ", fill, default");
        row += 2;

        panel.add(new JLabel("Max X (machine units)"), "2, " + row + ", right, default");
        maxXField = new JTextField(10);
        panel.add(maxXField, "4, " + row + ", fill, default");
        row += 2;

        panel.add(new JLabel("Max Y (machine units)"), "2, " + row + ", right, default");
        maxYField = new JTextField(10);
        panel.add(maxYField, "4, " + row + ", fill, default");
        row += 2;

        panel.add(new JLabel("Max Z (machine units)"), "2, " + row + ", right, default");
        maxZField = new JTextField(10);
        panel.add(maxZField, "4, " + row + ", fill, default");
        row += 2;

        panel.add(new JLabel("Scale X (units/mm)"), "2, " + row + ", right, default");
        scaleXField = new JTextField(10);
        panel.add(scaleXField, "4, " + row + ", fill, default");
        row += 2;

        panel.add(new JLabel("Scale Y (units/mm)"), "2, " + row + ", right, default");
        scaleYField = new JTextField(10);
        panel.add(scaleYField, "4, " + row + ", fill, default");
        row += 2;

        panel.add(new JLabel("Scale Z (units/mm)"), "2, " + row + ", right, default");
        scaleZField = new JTextField(10);
        panel.add(scaleZField, "4, " + row + ", fill, default");
        row += 2;

        panel.add(new JLabel("Scale A (units/degree)"), "2, " + row + ", right, default");
        scaleAField = new JTextField(10);
        panel.add(scaleAField, "4, " + row + ", fill, default");
        row += 2;

        panel.add(new JLabel("Blow Duration (sec)"), "2, " + row + ", right, default");
        blowDurationField = new JTextField(10);
        panel.add(blowDurationField, "4, " + row + ", fill, default");
        row += 2;

        panel.add(new JLabel("Motion Tolerance (units)"), "2, " + row + ", right, default");
        motionToleranceField = new JTextField(10);
        panel.add(motionToleranceField, "4, " + row + ", fill, default");
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter("%f");

        addWrappedBinding(driver, "comType", comTypeField, "text", intConverter);
        addWrappedBinding(driver, "nozzleCount", nozzleCountField, "text", intConverter);
        addWrappedBinding(driver, "maxX", maxXField, "text", intConverter);
        addWrappedBinding(driver, "maxY", maxYField, "text", intConverter);
        addWrappedBinding(driver, "maxZ", maxZField, "text", intConverter);
        addWrappedBinding(driver, "scaleX", scaleXField, "text", doubleConverter);
        addWrappedBinding(driver, "scaleY", scaleYField, "text", doubleConverter);
        addWrappedBinding(driver, "scaleZ", scaleZField, "text", doubleConverter);
        addWrappedBinding(driver, "scaleA", scaleAField, "text", doubleConverter);
        addWrappedBinding(driver, "blowDurationSec", blowDurationField, "text", doubleConverter);
        addWrappedBinding(driver, "motionTolerance", motionToleranceField, "text", intConverter);
    }
}
