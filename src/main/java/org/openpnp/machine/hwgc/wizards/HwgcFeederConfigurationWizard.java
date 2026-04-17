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
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.hwgc.HwgcFeeder;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class HwgcFeederConfigurationWizard extends AbstractConfigurationWizard {
    private final HwgcFeeder feeder;

    private JTextField feederNumberField;
    private JTextField feedDurationField;

    private JTextField pickX;
    private JTextField pickY;
    private JTextField pickZ;
    private LocationButtonsPanel pickButtons;

    public HwgcFeederConfigurationWizard(HwgcFeeder feeder) {
        this.feeder = feeder;

        contentPanel.add(buildSettingsPanel());
        contentPanel.add(buildLocationPanel());
    }

    private JPanel buildSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null,
                "HWGC Feeder", TitledBorder.LEADING, TitledBorder.TOP,
                null, new Color(0, 0, 0)));
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

        panel.add(new JLabel("Slot Number (1-50)"), "2, 2, right, default");
        feederNumberField = new JTextField(10);
        panel.add(feederNumberField, "4, 2, fill, default");

        panel.add(new JLabel("Feed Duration (ms)"), "2, 4, right, default");
        feedDurationField = new JTextField(10);
        panel.add(feedDurationField, "4, 4, fill, default");

        return panel;
    }

    private JPanel buildLocationPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null,
                "Pick Location", TitledBorder.LEADING, TitledBorder.TOP,
                null, new Color(0, 0, 0)));
        panel.setLayout(new FormLayout(
                new ColumnSpec[] {
                        FormSpecs.RELATED_GAP_COLSPEC,
                        FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC,
                        FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC,
                        FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC,
                        FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC,
                        FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, },
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, }));

        panel.add(new JLabel("X / Y / Z"), "2, 2, right, default");
        pickX = new JTextField(8);
        panel.add(pickX, "4, 2, fill, default");
        pickY = new JTextField(8);
        panel.add(pickY, "6, 2, fill, default");
        pickZ = new JTextField(8);
        panel.add(pickZ, "8, 2, fill, default");

        pickButtons = new LocationButtonsPanel(pickX, pickY, pickZ, null);
        panel.add(pickButtons, "10, 2");

        JButton openBtn = new JButton(openFeederAction);
        panel.add(openBtn, "4, 4");
        JButton closeBtn = new JButton(closeFeederAction);
        panel.add(closeBtn, "6, 4");

        return panel;
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        LengthConverter lengthConverter = new LengthConverter();

        addWrappedBinding(feeder, "feederNumber", feederNumberField, "text", intConverter);
        addWrappedBinding(feeder, "feedDurationMs", feedDurationField, "text", intConverter);

        MutableLocationProxy pickLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "location", pickLocation, "location");
        addWrappedBinding(pickLocation, "lengthX", pickX, "text", lengthConverter);
        addWrappedBinding(pickLocation, "lengthY", pickY, "text", lengthConverter);
        addWrappedBinding(pickLocation, "lengthZ", pickZ, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelect(feederNumberField);
        ComponentDecorators.decorateWithAutoSelect(feedDurationField);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(pickX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(pickY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(pickZ);
    }

    private final Action openFeederAction = new AbstractAction("Open") {
        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> feeder.setOpen(true));
        }
    };

    private final Action closeFeederAction = new AbstractAction("Close") {
        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> feeder.setOpen(false));
        }
    };
}
