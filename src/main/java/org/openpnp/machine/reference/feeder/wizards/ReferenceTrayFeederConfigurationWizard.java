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

package org.openpnp.machine.reference.feeder.wizards;

import java.awt.event.ActionEvent;

import javax.swing.*;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.*;
import org.openpnp.machine.reference.feeder.ReferenceTrayFeeder;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import org.openpnp.model.Location;
import org.openpnp.util.UiUtils;

@SuppressWarnings("serial")
public class ReferenceTrayFeederConfigurationWizard
        extends AbstractReferenceFeederConfigurationWizard {
    private final ReferenceTrayFeeder feeder;

    private JTextField textFieldOffsetsX;
    private JTextField textFieldOffsetsY;
    private JTextField textFieldOffsetsR;
    private JTextField textFieldTrayCountX;
    private JTextField textFieldTrayCountY;
    private JTextField textFieldFeedCount;

    public ReferenceTrayFeederConfigurationWizard(ReferenceTrayFeeder feeder) {
        super(feeder);
        this.feeder = feeder;

        JPanel panelFields = new JPanel();
        panelFields.setLayout(new FormLayout(
                new ColumnSpec[] {
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),},
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
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblX = new JLabel("X");
        panelFields.add(lblX, "4, 2");

        JLabel lblY = new JLabel("Y");
        panelFields.add(lblY, "6, 2");

        JLabel lblR = new JLabel("Rotation");
        panelFields.add(lblR, "8, 2");

        JLabel lblFeedStartLocation = new JLabel("Offsets");
        panelFields.add(lblFeedStartLocation, "2, 4, right, default");

        textFieldOffsetsX = new JTextField();
        panelFields.add(textFieldOffsetsX, "4, 4, fill, default");
        textFieldOffsetsX.setColumns(10);

        textFieldOffsetsY = new JTextField();
        panelFields.add(textFieldOffsetsY, "6, 4, fill, default");
        textFieldOffsetsY.setColumns(10);

        textFieldOffsetsR = new JTextField();
        panelFields.add(textFieldOffsetsR, "8, 4, fill, default");
        textFieldOffsetsR.setColumns(10);

        JLabel lblTrayCount = new JLabel("Tray Count");
        panelFields.add(lblTrayCount, "2, 6, right, default");

        textFieldTrayCountX = new JTextField();
        panelFields.add(textFieldTrayCountX, "4, 6, fill, default");
        textFieldTrayCountX.setColumns(10);

        textFieldTrayCountY = new JTextField();
        panelFields.add(textFieldTrayCountY, "6, 6, fill, default");
        textFieldTrayCountY.setColumns(10);

        JSeparator separator = new JSeparator();
        panelFields.add(separator, "4, 8, 6, 1");

        JLabel lblFeedCount = new JLabel("Feed Count");
        panelFields.add(lblFeedCount, "2, 10, right, default");

        textFieldFeedCount = new JTextField();
        panelFields.add(textFieldFeedCount, "4, 10, fill, default");
        textFieldFeedCount.setColumns(10);

        JSeparator separator2 = new JSeparator();
        panelFields.add(separator2, "4, 12, 6, 1");

        contentPanel.add(panelFields);
        
        JButton btnResetFeedCount = new JButton(new AbstractAction("Reset") {
            @Override
            public void actionPerformed(ActionEvent e) {
                textFieldFeedCount.setText("0");
                applyAction.actionPerformed(e);
            }
        });
        btnResetFeedCount.setHorizontalAlignment(SwingConstants.LEFT);
        panelFields.add(btnResetFeedCount, "6, 10, left, default");

        JButton calculateRotation = new JButton(
                new AbstractAction("Calculate Rotation", Icons.rotateClockwise) {
                    {
                        putValue(Action.SHORT_DESCRIPTION, "For compensating misaligned tray feeders: " +
                                "Set the pick location to the first part on the tray. Move to the furthest " +
                                "part on the tray and click to calculate rotation.");
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        UiUtils.submitUiMachineTask(() -> {
                            Location toolLocation = MainFrame.get().getMachineControls().getCurrentLocation(); // Current location of the selected tool
                            Location baseLocation = feeder.getLocation(); // Location set previously

                            // calculate angle between points
                            double rotation = -Math.atan2(toolLocation.getX() - baseLocation.getX(), toolLocation.getY() - baseLocation.getY()) * 180 / Math.PI;

                            textFieldOffsetsR.setText(String.valueOf(rotation));
                        });
                        applyAction.actionPerformed(e);
                    }
                });
        calculateRotation.setHorizontalAlignment(SwingConstants.LEFT);
        panelFields.add(calculateRotation, "4, 14, left, default");
    }

    @Override
    public void createBindings() {
        super.createBindings();
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter integerConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter("%f");

        MutableLocationProxy offsets = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "offsets", offsets, "location");
        addWrappedBinding(offsets, "lengthX", textFieldOffsetsX, "text", lengthConverter);
        addWrappedBinding(offsets, "lengthY", textFieldOffsetsY, "text", lengthConverter);
        addWrappedBinding(offsets, "rotation", textFieldOffsetsR, "text", doubleConverter);

        addWrappedBinding(feeder, "trayCountX", textFieldTrayCountX, "text", integerConverter);
        addWrappedBinding(feeder, "trayCountY", textFieldTrayCountY, "text", integerConverter);

        addWrappedBinding(feeder, "feedCount", textFieldFeedCount, "text", integerConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffsetsX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffsetsY);

        ComponentDecorators.decorateWithAutoSelect(textFieldTrayCountX);
        ComponentDecorators.decorateWithAutoSelect(textFieldTrayCountY);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedCount);
    }

    @Override
    protected void saveToModel() {
        super.saveToModel();
        if (feeder.getOffsets().getX() == 0 && feeder.getTrayCountX() > 1) {
            MessageBoxes.errorBox(this, "Error",
                    "X offset must be greater than 0 if X tray count is greater than 1 or feed failure will occur.");
        }
        if (feeder.getOffsets().getY() == 0 && feeder.getTrayCountY() > 1) {
            MessageBoxes.errorBox(this, "Error",
                    "Y offset must be greater than 0 if Y tray count is greater than 1 or feed failure will occur.");
        }
    }
}
