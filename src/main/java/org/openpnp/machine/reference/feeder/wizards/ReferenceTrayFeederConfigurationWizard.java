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

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.machine.reference.feeder.ReferenceRotatedTrayFeeder;
import org.openpnp.machine.reference.feeder.ReferenceTrayFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceTrayFeederConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceTrayFeeder feeder;

    private JComboBox comboBoxPart;
    private JLabel lblPickRetryCount;
    private JLabel lblRotationInTray;
    private JTextField textFieldRotationInTray;
    private JTextField textFieldFeedRetryCount;
    private JTextField textFieldPickRetryCount;
    private JTextField textFieldOffsetsX;
    private JTextField textFieldOffsetsY;
    private JTextField textFieldTrayCountX;
    private JTextField textFieldTrayCountY;
    private JTextField textFieldFeedCount;

    private JTextField textFieldLocationX;

    private JTextField textFieldLocationY;

    private JTextField textFieldLocationZ;

    private JTextField textFieldLocationC;

    private boolean includePickLocation;

    /**
     * @wbp.parser.constructor
     */
    public ReferenceTrayFeederConfigurationWizard(ReferenceTrayFeeder feeder) {
        this(feeder, true);
    }

    public ReferenceTrayFeederConfigurationWizard(ReferenceTrayFeeder feeder,
            boolean includePickLocation) {
        this.feeder = feeder;
        this.includePickLocation = includePickLocation;

        JPanel panelPart = new JPanel();
        panelPart.setBorder(
                new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "General Settings", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(panelPart);
        panelPart.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        comboBoxPart = new JComboBox();
        try {
            comboBoxPart.setModel(new PartsComboBoxModel());
        }
        catch (Throwable t) {
            // Swallow this error. This happens during parsing in
            // in WindowBuilder but doesn't happen during normal run.
        }
        
        JLabel lblPart = new JLabel("Part");
        panelPart.add(lblPart, "2, 2, right, default");
        comboBoxPart.setRenderer(new IdentifiableListCellRenderer<Part>());
        panelPart.add(comboBoxPart, "4, 2, left, default");
        
        JLabel lblRotationInTray = new JLabel("Rotation In Tray");
        panelPart.add(lblRotationInTray, "2, 4, left, default");

        textFieldRotationInTray = new JTextField();
        panelPart.add(textFieldRotationInTray, "4, 4, fill, default");
        textFieldRotationInTray.setColumns(4);

        JLabel lblRetryCount = new JLabel("Feed Retry Count");
        panelPart.add(lblRetryCount, "2, 6, right, default");
        
        textFieldFeedRetryCount = new JTextField();
        textFieldFeedRetryCount.setText("3");
        panelPart.add(textFieldFeedRetryCount, "4, 6");
        textFieldFeedRetryCount.setColumns(3);
        
        JLabel lblPickRetryCount = new JLabel("Pick Retry Count");
        panelPart.add(lblPickRetryCount, "2, 8, right, default");
        
        textFieldPickRetryCount = new JTextField();
        textFieldPickRetryCount.setText("3");
        textFieldPickRetryCount.setColumns(3);
        panelPart.add(textFieldPickRetryCount, "4, 8");

        if (includePickLocation) {
            JPanel panelLocation = new JPanel();
            panelLocation.setBorder(new TitledBorder(
                    new EtchedBorder(EtchedBorder.LOWERED, null, null), "Pick Location",
                    TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
            contentPanel.add(panelLocation);
            panelLocation
                    .setLayout(new FormLayout(
                            new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC,
                                    ColumnSpec.decode("default:grow"),
                                    FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec
                                            .decode("default:grow"),
                                    FormSpecs.RELATED_GAP_COLSPEC,
                                    ColumnSpec.decode("default:grow"),
                                    FormSpecs.RELATED_GAP_COLSPEC,
                                    ColumnSpec.decode("default:grow"),
                                    FormSpecs.RELATED_GAP_COLSPEC,
                                    ColumnSpec.decode("left:default:grow"),},
                            new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                                    FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

            JLabel lblX_1 = new JLabel("X");
            panelLocation.add(lblX_1, "2, 2");

            JLabel lblY_1 = new JLabel("Y");
            panelLocation.add(lblY_1, "4, 2");

            JLabel lblZ = new JLabel("Z");
            panelLocation.add(lblZ, "6, 2");

            JLabel lblRotation = new JLabel("Rotation");
            panelLocation.add(lblRotation, "8, 2");

            textFieldLocationX = new JTextField();
            panelLocation.add(textFieldLocationX, "2, 4");
            textFieldLocationX.setColumns(8);

            textFieldLocationY = new JTextField();
            panelLocation.add(textFieldLocationY, "4, 4");
            textFieldLocationY.setColumns(8);

            textFieldLocationZ = new JTextField();
            panelLocation.add(textFieldLocationZ, "6, 4");
            textFieldLocationZ.setColumns(8);

            textFieldLocationC = new JTextField();
            panelLocation.add(textFieldLocationC, "8, 4");
            textFieldLocationC.setColumns(8);

            LocationButtonsPanel locationButtonsPanel = new LocationButtonsPanel(textFieldLocationX, textFieldLocationY,
                    textFieldLocationZ, textFieldLocationC);
            panelLocation.add(locationButtonsPanel, "10, 4");
        }
        
        JPanel panelFields = new JPanel();

        panelFields.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
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

        JLabel lblFeedStartLocation = new JLabel("Offsets");
        panelFields.add(lblFeedStartLocation, "2, 4, right, default");

        textFieldOffsetsX = new JTextField();
        panelFields.add(textFieldOffsetsX, "4, 4, fill, default");
        textFieldOffsetsX.setColumns(10);

        textFieldOffsetsY = new JTextField();
        panelFields.add(textFieldOffsetsY, "6, 4, 2, 1, fill, default");
        textFieldOffsetsY.setColumns(10);

        JLabel lblTrayCount = new JLabel("Tray Count");
        panelFields.add(lblTrayCount, "2, 6, right, default");

        textFieldTrayCountX = new JTextField();
        panelFields.add(textFieldTrayCountX, "4, 6, fill, default");
        textFieldTrayCountX.setColumns(10);

        textFieldTrayCountY = new JTextField();
        panelFields.add(textFieldTrayCountY, "6, 6, 2, 1, fill, default");
        textFieldTrayCountY.setColumns(10);

        JSeparator separator = new JSeparator();
        panelFields.add(separator, "4, 8, 4, 1");

        JLabel lblFeedCount = new JLabel("Feed Count");
        panelFields.add(lblFeedCount, "2, 10, right, default");

        textFieldFeedCount = new JTextField();
        panelFields.add(textFieldFeedCount, "4, 10, fill, default");
        textFieldFeedCount.setColumns(10);

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
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter integerConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());


        addWrappedBinding(feeder, "part", comboBoxPart, "selectedItem");
        addWrappedBinding(feeder, "rotationInFeeder", textFieldRotationInTray, "text", doubleConverter);
        addWrappedBinding(feeder, "feedRetryCount", textFieldFeedRetryCount, "text", integerConverter);
        addWrappedBinding(feeder, "pickRetryCount", textFieldPickRetryCount, "text", integerConverter);

        if (includePickLocation) {
            MutableLocationProxy location = new MutableLocationProxy();
            bind(UpdateStrategy.READ_WRITE, feeder, "location", location, "location");
            addWrappedBinding(location, "lengthX", textFieldLocationX, "text", lengthConverter);
            addWrappedBinding(location, "lengthY", textFieldLocationY, "text", lengthConverter);
            addWrappedBinding(location, "lengthZ", textFieldLocationZ, "text", lengthConverter);
            addWrappedBinding(location, "rotation", textFieldLocationC, "text", doubleConverter);
            ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationX);
            ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationY);
            ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationZ);
            ComponentDecorators.decorateWithAutoSelect(textFieldLocationC);
        }
        
        MutableLocationProxy offsets = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "offsets", offsets, "location");
        addWrappedBinding(offsets, "lengthX", textFieldOffsetsX, "text", lengthConverter);
        addWrappedBinding(offsets, "lengthY", textFieldOffsetsY, "text", lengthConverter);

        addWrappedBinding(feeder, "trayCountX", textFieldTrayCountX, "text", integerConverter);
        addWrappedBinding(feeder, "trayCountY", textFieldTrayCountY, "text", integerConverter);

        addWrappedBinding(feeder, "feedCount", textFieldFeedCount, "text", integerConverter);

        ComponentDecorators.decorateWithAutoSelect(textFieldRotationInTray);

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
