/*
 * Copyright (C) 2020 <mark@makr.zone>
 * inspired and based on work
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

package org.openpnp.machine.reference.axis.wizards;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.DriversComboBoxModel;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.Driver;
import org.openpnp.spi.base.AbstractControllerAxis;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceControllerAxisConfigurationWizard extends AbstractAxisConfigurationWizard {
    private JPanel panelControllerSettings;
    private JTextField homeCoordinate;
    private JLabel lblDesignator;
    private JTextField letter;
    private JLabel lblInvertLinearrotational;
    private JCheckBox invertLinearRotational;
    private JLabel lblDriver;
    private JComboBox driver;
    private JLabel lblResolution;
    private JTextField resolution;
    private JLabel lblPremoveCommand;
    private JTextArea preMoveCommand;
    private JScrollPane scrollPane;
    protected NamedConverter<Driver> driverConverter;
    private JPanel panelKinematics;
    private JLabel lblFeedrates;
    private JTextField feedratePerSecond;
    private JLabel lblAccelerations;
    private JTextField accelerationPerSecond2;
    private JLabel lblJerks;
    private JTextField jerkPerSecond3;
    private JLabel lblLimitRotation;
    private JCheckBox limitRotation;
    private JLabel lblWrapAroundRotation;
    private JCheckBox wrapAroundRotation;
    private JLabel lblSoftLimitLow;
    private JLabel lblSoftLimitHigh;
    private JTextField softLimitLow;
    private JTextField softLimitHigh;
    private JCheckBox softLimitLowEnabled;
    private JCheckBox softLimitHighEnabled;
    private JButton btnCaptureSoftLimitLow;
    private JButton btnCaptureSoftLimitHigh;
    private JButton btnPositionSoftLimitLow;
    private JButton btnPositionSoftLimitHigh;
    private JLabel lblSafeZoneLow;
    private JLabel lblSafeZoneHigh;
    private JTextField safeZoneLow;
    private JTextField safeZoneHigh;
    private JCheckBox safeZoneLowEnabled;
    private JCheckBox safeZoneHighEnabled;
    private JButton btnCaptureSafeZoneLow;
    private JButton btnPositionSafeZoneLow;
    private JButton btnCaptureSafeZoneHigh;
    private JButton btnPositionSafeZoneHigh;
    private JLabel lblNotMmmin;

    private Action captureSoftLimitLowAction = new AbstractAction(null, Icons.captureAxisLow) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Capture the current axis position as the low soft-limit.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.submitUiMachineTask(() -> {
                Length position = ((AbstractControllerAxis) axis).getDriverLengthCoordinate();
                SwingUtilities.invokeAndWait(() -> {
                    LengthConverter lengthConverter = new LengthConverter();
                    softLimitLow.setText(lengthConverter.convertForward(position));
                });
            });
        }
    };

    private Action captureSoftLimitHighAction = new AbstractAction(null, Icons.captureAxisHigh) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Capture the current axis position as the high soft-limit.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.submitUiMachineTask(() -> {
                Length position = ((AbstractControllerAxis) axis).getDriverLengthCoordinate();
                SwingUtilities.invokeAndWait(() -> {
                    LengthConverter lengthConverter = new LengthConverter();
                    softLimitHigh.setText(lengthConverter.convertForward(position));
                });
            });
        }
    };

    private Action positionSoftLimitLowAction = new AbstractAction(null, Icons.positionAxisLow) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Position the axis to the low soft-limit coordinate.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            LengthConverter lengthConverter = new LengthConverter();
            Length limit = lengthConverter.convertReverse(softLimitLow.getText());
            UiUtils.submitUiMachineTask(() -> {
                ((AbstractControllerAxis) axis).moveAxis(limit);
            });
        }
    };

    private Action positionSoftLimitHighAction = new AbstractAction(null, Icons.positionAxisHigh) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Position the axis to the high soft-limit coordinate.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            LengthConverter lengthConverter = new LengthConverter();
            Length limit = lengthConverter.convertReverse(softLimitHigh.getText());
            UiUtils.submitUiMachineTask(() -> {
                ((AbstractControllerAxis) axis).moveAxis(limit);
            });
        }
    };


    private Action captureSafeZoneLowAction = new AbstractAction(null, Icons.captureAxisLow) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Capture the current axis position as the lower limit of the safe zone.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.submitUiMachineTask(() -> {
                Length position = ((AbstractControllerAxis) axis).getDriverLengthCoordinate();
                SwingUtilities.invokeAndWait(() -> {
                    LengthConverter lengthConverter = new LengthConverter();
                    safeZoneLow.setText(lengthConverter.convertForward(position));
                });
            });
        }
    };

    private Action captureSafeZoneHighAction = new AbstractAction(null, Icons.captureAxisHigh) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Capture the current axis position as the upper limit of the safe zone.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.submitUiMachineTask(() -> {
                Length position = ((AbstractControllerAxis) axis).getDriverLengthCoordinate();
                SwingUtilities.invokeAndWait(() -> {
                    LengthConverter lengthConverter = new LengthConverter();
                    safeZoneHigh.setText(lengthConverter.convertForward(position));
                });
            });
        }
    };

    private Action positionSafeZoneLowAction = new AbstractAction(null, Icons.positionAxisLow) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Position the axis to the lower limit of the safe zone.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            LengthConverter lengthConverter = new LengthConverter();
            Length limit = lengthConverter.convertReverse(safeZoneLow.getText());
            UiUtils.submitUiMachineTask(() -> {
                ((AbstractControllerAxis) axis).moveAxis(limit);
            });
        }
    };

    private Action positionSafeZoneHighAction = new AbstractAction(null, Icons.positionAxisHigh) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Position the axis to the higher limit of the safe zone.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            LengthConverter lengthConverter = new LengthConverter();
            Length limit = lengthConverter.convertReverse(safeZoneHigh.getText());
            UiUtils.submitUiMachineTask(() -> {
                ((AbstractControllerAxis) axis).moveAxis(limit);
            });
        }
    };    

    public ReferenceControllerAxisConfigurationWizard(ReferenceControllerAxis axis) {
        super(axis);

        panelControllerSettings = new JPanel();
        panelControllerSettings.setBorder(new TitledBorder(null, "Controller Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelControllerSettings);
        panelControllerSettings.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
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
                        RowSpec.decode("default:grow"),}));

        lblDriver = new JLabel("Driver");
        panelControllerSettings.add(lblDriver, "2, 2, right, default");

        driver = new JComboBox(new DriversComboBoxModel((AbstractMachine) Configuration.get().getMachine(), true));
        driver.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        panelControllerSettings.add(driver, "4, 2, fill, default");

        lblDesignator = new JLabel("Axis Letter");
        lblDesignator.setToolTipText("The axis letter (X, Y, Z etc.) as used by the Controller.");
        panelControllerSettings.add(lblDesignator, "2, 4, right, default");

        letter = new JTextField();
        panelControllerSettings.add(letter, "4, 4, fill, default");
        letter.setColumns(10);

        lblInvertLinearrotational = new JLabel("Switch Linear ↔ Rotational?");
        lblInvertLinearrotational.setToolTipText("<html>\r\n<p>It is important that OpenPnP understands whether an Axis is linear or rotational in <br/>\r\nthe controller. </p> \r\n<p>Most of the times this is already determined by the Axis Type, i.e. X, Y, Z are linear <br/>\r\nand Rotation is rotational. But sometimes you may run out of proper axes on the <br/>\r\ncontroller and then have to use a linear controller axis for a rotational OpenPnP axis <br/>\r\nor vice versa.</p>\r\n<p>If you cannot configure your controller to switch this meaning, it is important to enable <br/>\r\nthe Switch Linear ↔ Rotational checkbox.</p>\r\n<p>This is relevant in computing proper limits for feed-rate, acceleration and jerk in mixed<br/>\r\naxes moves, as only the motion of linear axes is taken into consideration for the limts in \\br/>\r\nstandard G-Code.</p>\r\n</html>");
        panelControllerSettings.add(lblInvertLinearrotational, "2, 6, right, default");

        invertLinearRotational = new JCheckBox("");
        invertLinearRotational.setToolTipText("");
        panelControllerSettings.add(invertLinearRotational, "4, 6");

        JLabel lblHomeCoordinate = new JLabel("Home Coordinate");
        panelControllerSettings.add(lblHomeCoordinate, "2, 8, right, default");

        homeCoordinate = new JTextField();
        panelControllerSettings.add(homeCoordinate, "4, 8, fill, default");
        homeCoordinate.setColumns(10);

        lblResolution = new JLabel("Resolution [Driver Units]");
        lblResolution.setToolTipText("<html>Numeric resolution of this axis. Coordinates will be rounded to the nearest multiple<br/>\r\nwhen comparing them in order to determine whether a move is necessary. <br/>\r\nFor the GcodeDriver, make sure the resolution can be expressed with the format in the <br/>\r\n<code>MOVE_TO_COMMAND</code>. Default is 0.0001 which corresponds to the %.4f <br/>\r\n(four fractional digits) format in the <code>MOVE_TO_COMMAND</code>.<br/>\r\nNote, the resolution is given and applied in driver (not system) units.\r\n</html>");
        panelControllerSettings.add(lblResolution, "2, 12, right, default");

        resolution = new JTextField();
        panelControllerSettings.add(resolution, "4, 12, fill, default");
        resolution.setColumns(10);

        lblLimitRotation = new JLabel("Limit to Range");
        lblLimitRotation.setToolTipText("Limit the rotation to -180° ... +180° or the custom Soft-Limits if enabled.");
        panelControllerSettings.add(lblLimitRotation, "2, 14, right, default");

        limitRotation = new JCheckBox("");
        limitRotation.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        panelControllerSettings.add(limitRotation, "4, 14");

        lblWrapAroundRotation = new JLabel("Wrap Around");
        lblWrapAroundRotation.setToolTipText("<html>Always rotate the axis the shorter way around. E.g. if it is at 270° and is commanded <br/>\r\nto go to 0° it will instead go to 360°.<br/>\r\nIf this is combined with Limit to ±180°, the axis is reset to its wrap-around coordinate <br/>\r\nusing a driver Global Offset command. With the GcodeDriver you must configure the<br/> <code>SET_GLOBAL_OFFSETS_COMMAND</code> or this will not work.\r\n</html>\r\n");
        panelControllerSettings.add(lblWrapAroundRotation, "2, 16, right, default");

        wrapAroundRotation = new JCheckBox("");
        panelControllerSettings.add(wrapAroundRotation, "4, 16");

        lblPremoveCommand = new JLabel("Pre-Move Command");
        panelControllerSettings.add(lblPremoveCommand, "2, 20, right, top");

        scrollPane = new JScrollPane();
        panelControllerSettings.add(scrollPane, "4, 20, 3, 1, fill, fill");

        preMoveCommand = new JTextArea();
        preMoveCommand.setRows(1);
        scrollPane.setViewportView(preMoveCommand);

        panelKinematics = new JPanel();
        panelKinematics.setBorder(new TitledBorder(null, "Kinematic Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelKinematics);
        panelKinematics.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
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
                        FormSpecs.DEFAULT_ROWSPEC,}));

        lblSoftLimitLow = new JLabel("Soft Limit Low");
        panelKinematics.add(lblSoftLimitLow, "2, 2, right, default");

        softLimitLow = new JTextField();
        panelKinematics.add(softLimitLow, "4, 2, fill, default");
        softLimitLow.setColumns(10);

        softLimitLowEnabled = new JCheckBox("Enabled?");
        panelKinematics.add(softLimitLowEnabled, "6, 2");

        btnPositionSoftLimitLow = new JButton(positionSoftLimitLowAction);
        panelKinematics.add(btnPositionSoftLimitLow, "8, 2");

        btnCaptureSoftLimitLow = new JButton(captureSoftLimitLowAction);
        panelKinematics.add(btnCaptureSoftLimitLow, "10, 2");

        lblSafeZoneLow = new JLabel("Safe Zone Low");
        panelKinematics.add(lblSafeZoneLow, "2, 4, right, default");

        safeZoneLow = new JTextField();
        panelKinematics.add(safeZoneLow, "4, 4, fill, default");
        safeZoneLow.setColumns(10);

        safeZoneLowEnabled = new JCheckBox("Enabled?");
        panelKinematics.add(safeZoneLowEnabled, "6, 4");

        btnPositionSafeZoneLow = new JButton(positionSafeZoneLowAction);
        panelKinematics.add(btnPositionSafeZoneLow, "8, 4");

        btnCaptureSafeZoneLow = new JButton(captureSafeZoneLowAction);
        panelKinematics.add(btnCaptureSafeZoneLow, "10, 4");

        lblSafeZoneHigh = new JLabel("Safe Zone High");
        panelKinematics.add(lblSafeZoneHigh, "2, 6, right, default");

        safeZoneHigh = new JTextField();
        panelKinematics.add(safeZoneHigh, "4, 6, fill, default");
        safeZoneHigh.setColumns(10);

        safeZoneHighEnabled = new JCheckBox("Enabled?");
        panelKinematics.add(safeZoneHighEnabled, "6, 6");

        btnPositionSafeZoneHigh = new JButton(positionSafeZoneHighAction);
        panelKinematics.add(btnPositionSafeZoneHigh, "8, 6");

        btnCaptureSafeZoneHigh = new JButton(captureSafeZoneHighAction);
        panelKinematics.add(btnCaptureSafeZoneHigh, "10, 6");

        lblSoftLimitHigh = new JLabel("Soft Limit High");
        panelKinematics.add(lblSoftLimitHigh, "2, 8, right, default");

        softLimitHigh = new JTextField();
        panelKinematics.add(softLimitHigh, "4, 8, fill, default");
        softLimitHigh.setColumns(10);

        softLimitHighEnabled = new JCheckBox("Enabled?");
        panelKinematics.add(softLimitHighEnabled, "6, 8");

        btnPositionSoftLimitHigh = new JButton(positionSoftLimitHighAction);
        panelKinematics.add(btnPositionSoftLimitHigh, "8, 8");

        btnCaptureSoftLimitHigh = new JButton(captureSoftLimitHighAction);
        panelKinematics.add(btnCaptureSoftLimitHigh, "10, 8");

        lblFeedrates = new JLabel("Feedrate [/s]");
        panelKinematics.add(lblFeedrates, "2, 12, right, default");

        feedratePerSecond = new JTextField();
        panelKinematics.add(feedratePerSecond, "4, 12, fill, default");
        feedratePerSecond.setColumns(10);

        lblNotMmmin = new JLabel("Not [/min]");
        lblNotMmmin.setForeground(Color.RED);
        panelKinematics.add(lblNotMmmin, "6, 12");

        lblAccelerations = new JLabel("Acceleration [/s²]");
        panelKinematics.add(lblAccelerations, "2, 14, right, default");

        accelerationPerSecond2 = new JTextField();
        panelKinematics.add(accelerationPerSecond2, "4, 14, fill, default");
        accelerationPerSecond2.setColumns(10);

        lblJerks = new JLabel("Jerk [/s³]");
        panelKinematics.add(lblJerks, "2, 16, right, default");

        jerkPerSecond3 = new JTextField();
        panelKinematics.add(jerkPerSecond3, "4, 16, fill, default");
        jerkPerSecond3.setColumns(10);

        driverConverter = new NamedConverter<>(Configuration.get().getMachine().getDrivers()); 
        // Also adapt if the type of the axis changes
        type.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
    }

    protected void adaptDialog() {
        Driver selectedDriver = driverConverter.convertReverse((String) driver.getSelectedItem());
        boolean showPreMove = (selectedDriver != null && selectedDriver.isSupportingPreMove());
        boolean showRotationSettings = type.getSelectedItem() == Type.Rotation;
        boolean showSoftLimits = type.getSelectedItem() != Type.Rotation || limitRotation.isSelected();
        lblPremoveCommand.setVisible(showPreMove);
        scrollPane.setVisible(showPreMove);

        lblLimitRotation.setVisible(showRotationSettings);
        limitRotation.setVisible(showRotationSettings);
        lblWrapAroundRotation.setVisible(showRotationSettings);
        wrapAroundRotation.setVisible(showRotationSettings);

        lblSoftLimitLow.setVisible(showSoftLimits);
        lblSoftLimitHigh.setVisible(showSoftLimits);
        softLimitLow.setVisible(showSoftLimits);
        softLimitHigh.setVisible(showSoftLimits);
        softLimitLowEnabled.setVisible(showSoftLimits);
        softLimitHighEnabled.setVisible(showSoftLimits);
        btnCaptureSoftLimitLow.setVisible(showSoftLimits);
        btnCaptureSoftLimitHigh.setVisible(showSoftLimits);
        btnPositionSoftLimitLow.setVisible(showSoftLimits);
        btnPositionSoftLimitHigh.setVisible(showSoftLimits);

        lblSafeZoneLow.setVisible(!showRotationSettings);
        lblSafeZoneHigh.setVisible(!showRotationSettings);
        safeZoneLow.setVisible(!showRotationSettings);
        safeZoneHigh.setVisible(!showRotationSettings);
        safeZoneLowEnabled.setVisible(!showRotationSettings);
        safeZoneHighEnabled.setVisible(!showRotationSettings);
        btnCaptureSafeZoneLow.setVisible(!showRotationSettings);
        btnCaptureSafeZoneHigh.setVisible(!showRotationSettings);
        btnPositionSafeZoneLow.setVisible(!showRotationSettings);
        btnPositionSafeZoneHigh.setVisible(!showRotationSettings);

    }

    @Override
    public void createBindings() {
        super.createBindings();
        LengthConverter lengthConverter = new LengthConverter();
        DoubleConverter doubleConverter = new DoubleConverter("%f"); 

        addWrappedBinding(axis, "driver", driver, "selectedItem", driverConverter);
        addWrappedBinding(axis, "letter", letter, "text");
        addWrappedBinding(axis, "invertLinearRotational", invertLinearRotational, "selected");
        addWrappedBinding(axis, "homeCoordinate", homeCoordinate, "text", lengthConverter);
        addWrappedBinding(axis, "resolution", resolution, "text", doubleConverter);
        addWrappedBinding(axis, "preMoveCommand", preMoveCommand, "text");

        addWrappedBinding(axis, "limitRotation", limitRotation, "selected");
        addWrappedBinding(axis, "wrapAroundRotation", wrapAroundRotation, "selected");

        addWrappedBinding(axis, "softLimitLow", softLimitLow, "text", lengthConverter);
        addWrappedBinding(axis, "softLimitLowEnabled", softLimitLowEnabled, "selected");
        addWrappedBinding(axis, "softLimitHigh", softLimitHigh, "text", lengthConverter);
        addWrappedBinding(axis, "softLimitHighEnabled", softLimitHighEnabled, "selected");

        addWrappedBinding(axis, "safeZoneLow", safeZoneLow, "text", lengthConverter);
        addWrappedBinding(axis, "safeZoneLowEnabled", safeZoneLowEnabled, "selected");
        addWrappedBinding(axis, "safeZoneHigh", safeZoneHigh, "text", lengthConverter);
        addWrappedBinding(axis, "safeZoneHighEnabled", safeZoneHighEnabled, "selected");

        addWrappedBinding(axis, "feedratePerSecond", feedratePerSecond, "text", lengthConverter);
        addWrappedBinding(axis, "accelerationPerSecond2", accelerationPerSecond2, "text", lengthConverter);
        addWrappedBinding(axis, "jerkPerSecond3", jerkPerSecond3, "text", lengthConverter);


        ComponentDecorators.decorateWithAutoSelect(letter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homeCoordinate);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(feedratePerSecond);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(accelerationPerSecond2);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(jerkPerSecond3);
        ComponentDecorators.decorateWithAutoSelect(resolution);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(softLimitLow);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(softLimitHigh);

        adaptDialog();
    }
}
