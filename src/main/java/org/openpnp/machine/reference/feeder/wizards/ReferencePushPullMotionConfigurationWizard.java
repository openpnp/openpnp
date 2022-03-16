/*
 * Copyright (C) 2020 <mark@makr.zone>
 * based on the ReferenceLeverFeeder 
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

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.LongConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.machine.reference.feeder.ReferencePushPullFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Head;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferencePushPullMotionConfigurationWizard
extends AbstractConfigurationWizard {
    private final ReferencePushPullFeeder feeder;
    private JTextField textFieldFeedStartX;
    private JTextField textFieldFeedStartY;
    private JTextField textFieldFeedStartZ;
    private JTextField textFieldFeedEndX;
    private JTextField textFieldFeedEndY;
    private JTextField textFieldFeedEndZ;
    private JTextField textFieldFeedPush1;
    private JLabel lblFeedMid1Location;
    private JTextField textFieldFeedMid1X;
    private JTextField textFieldFeedMid1Y;
    private JTextField textFieldFeedMid1Z;
    private JLabel lblFeedMid2Location;
    private JTextField textFieldFeedMid2X;
    private JTextField textFieldFeedMid2Y;
    private JTextField textFieldFeedMid2Z;
    private JLabel lblFeedMid3Location;
    private JTextField textFieldFeedMid3X;
    private JTextField textFieldFeedMid3Y;
    private JTextField textFieldFeedMid3Z;
    private JLabel lblActuatorId;
    private JComboBox actuator;
    private JLabel lblPeelOffActuatorId;
    private JComboBox actuator2;
    private JPanel panelLocations;
    private JPanel panelPushPull;
    private LocationButtonsPanel locationButtonsPanelFeedStart;
    private LocationButtonsPanel locationButtonsPanelFeedMid1;
    private LocationButtonsPanel locationButtonsPanelFeedMid2;
    private LocationButtonsPanel locationButtonsPanelFeedMid3;
    private LocationButtonsPanel locationButtonsPanelFeedEnd;
    private JLabel lblPush;
    private JLabel lblMulti;
    private JLabel lblPull;
    private JLabel lblFeedSpeed0_1;
    private JLabel lblFeedSpeed1_2;
    private JLabel lblFeedSpeed2_3;
    private JLabel lblFeedSpeed3_4;
    private JTextField textFieldFeedPush2;
    private JTextField textFieldFeedPush3;
    private JTextField textFieldFeedPush4;
    private JTextField textFieldFeedPull3;
    private JTextField textFieldFeedPull2;
    private JTextField textFieldFeedPull1;
    private JTextField textFieldFeedPull0;
    private JCheckBox chckbxPush1;
    private JCheckBox chckbxPush2;
    private JCheckBox chckbxPush3;
    private JCheckBox chckbxPushEnd;
    private JCheckBox chckbxMulti0;
    private JCheckBox chckbxMulti1;
    private JCheckBox chckbxMulti2;
    private JCheckBox chckbxMulti3;
    private JCheckBox chckbxMultiEnd;
    private JCheckBox chckbxPull0;
    private JCheckBox chckbxPull1;
    private JCheckBox chckbxPull2;
    private JCheckBox chckbxPull3;
    private JLabel lblRotation;
    private JTextField textFieldFeedStartC;
    private JTextField textFieldFeedMid1C;
    private JTextField textFieldFeedMid2C;
    private JTextField textFieldFeedMid3C;
    private JTextField textFieldFeedEndC;
    private JCheckBox additiveRotation;

    public ReferencePushPullMotionConfigurationWizard(ReferencePushPullFeeder feeder) {
        super();
        this.feeder = feeder;

        JPanel panelFields = new JPanel();
        panelFields.setLayout(new BoxLayout(panelFields, BoxLayout.Y_AXIS));
        panelLocations = new JPanel();
        panelLocations.setBorder(new TitledBorder(null, "Tape Settings", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));

        panelPushPull = new JPanel();
        panelFields.add(panelPushPull);
        panelPushPull.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Push-Pull Settings", TitledBorder.LEADING, TitledBorder.TOP, null));
        panelPushPull.setLayout(new FormLayout(new ColumnSpec[] {
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
                ColumnSpec.decode("max(20dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(10dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(10dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(20dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(100dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("left:max(100dlu;default):grow"),},
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

        Head head = null;
        try {
            head = Configuration.get().getMachine().getDefaultHead();
        }
        catch (Exception e) {
            Logger.error(e, "Cannot determine default head of machine.");
        }

        lblActuatorId = new JLabel("Feed Actuator");
        lblActuatorId.setToolTipText("Actuator for feed signal and for motion");
        panelPushPull.add(lblActuatorId, "2, 2, right, default");

        actuator = new JComboBox();
        panelPushPull.add(actuator, "4, 2, 3, 1");
        actuator.setMaximumRowCount(15);
        actuator.setModel(new ActuatorsComboBoxModel(head));


        lblPeelOffActuatorId = new JLabel("Auxiliary Actuator");
        lblPeelOffActuatorId.setToolTipText("Actuator for auxiliary purpose (e.g. peeling).");
        panelPushPull.add(lblPeelOffActuatorId, "2, 4, right, default");

        actuator2 = new JComboBox();
        panelPushPull.add(actuator2, "4, 4, 3, 1");
        actuator2.setMaximumRowCount(15);
        actuator2.setModel(new ActuatorsComboBoxModel(head));

        JLabel lblX = new JLabel("X");
        panelPushPull.add(lblX, "4, 8, center, default");

        JLabel lblY = new JLabel("Y");
        panelPushPull.add(lblY, "6, 8, center, default");

        JLabel lblZ = new JLabel("Z");
        panelPushPull.add(lblZ, "8, 8, center, default");

        lblRotation = new JLabel("Rotation");
        panelPushPull.add(lblRotation, "10, 8, center, default");

        lblPush = new JLabel("↓");
        lblPush.setToolTipText("Locations that are included when pushing.");
        panelPushPull.add(lblPush, "12, 8, center, default");

        lblMulti = new JLabel("↑↓");
        lblMulti.setToolTipText("<html>Locations that are included, when actuating multiple times.<br/>\r\nThe combination with the push ↓ and pull ↑ switch is taken.</html>");
        panelPushPull.add(lblMulti, "14, 8, 3, 1, center, default");

        lblPull = new JLabel("↑");
        lblPull.setToolTipText("Locations that are included when pulling.");
        panelPushPull.add(lblPull, "18, 8, center, default");

        additiveRotation = new JCheckBox("Additive");
        additiveRotation.setToolTipText("<html>Before the feed is performed, the current coordinate of the Rotation axis is taken.<br/>\nRotations stated below are then added to that base angle.<br/>\nThis is typically used for peeling axes, creating a continuous cover tape spool wind-up.\n</html>");
        panelPushPull.add(additiveRotation, "10, 10, center, default");

        JLabel lblFeedStartLocation = new JLabel("Start Location");
        panelPushPull.add(lblFeedStartLocation, "2, 12, right, default");

        textFieldFeedStartX = new JTextField();
        panelPushPull.add(textFieldFeedStartX, "4, 12");
        textFieldFeedStartX.setColumns(8);

        textFieldFeedStartY = new JTextField();
        panelPushPull.add(textFieldFeedStartY, "6, 12");
        textFieldFeedStartY.setColumns(8);

        textFieldFeedStartZ = new JTextField();
        panelPushPull.add(textFieldFeedStartZ, "8, 12");
        textFieldFeedStartZ.setColumns(8);

        textFieldFeedStartC = new JTextField();
        panelPushPull.add(textFieldFeedStartC, "10, 12, fill, default");
        textFieldFeedStartC.setColumns(8);

        chckbxMulti0 = new JCheckBox("");
        chckbxMulti0.setToolTipText("Include the Start Location in multi-actuating motion (if the pull switch is also set).");
        chckbxMulti0.setSelected(true);
        panelPushPull.add(chckbxMulti0, "14, 12, 3, 1, center, default");

        chckbxPull0 = new JCheckBox("");
        chckbxPull0.setToolTipText("Go to the Start Location when pulling.");
        chckbxPull0.setSelected(true);
        panelPushPull.add(chckbxPull0, "18, 12, center, default");

        locationButtonsPanelFeedStart = new LocationButtonsPanel(textFieldFeedStartX,
                textFieldFeedStartY, textFieldFeedStartZ, null);
        locationButtonsPanelFeedStart.setShowPositionToolNoSafeZ(true);
        panelPushPull.add(locationButtonsPanelFeedStart, "20, 12, fill, default");

        lblFeedSpeed0_1 = new JLabel("Speed ↕");
        panelPushPull.add(lblFeedSpeed0_1, "10, 14, right, default");

        textFieldFeedPush1 = new JTextField();
        panelPushPull.add(textFieldFeedPush1, "12, 14, 3, 1");
        textFieldFeedPush1.setColumns(10);

        textFieldFeedPull0 = new JTextField();
        panelPushPull.add(textFieldFeedPull0, "16, 14, 3, 1");
        textFieldFeedPull0.setColumns(10);

        lblFeedMid1Location = new JLabel("Mid 1 Location");
        panelPushPull.add(lblFeedMid1Location, "2, 16, right, default");

        textFieldFeedMid1X = new JTextField();
        panelPushPull.add(textFieldFeedMid1X, "4, 16");
        textFieldFeedMid1X.setColumns(10);

        textFieldFeedMid1Y = new JTextField();
        panelPushPull.add(textFieldFeedMid1Y, "6, 16");
        textFieldFeedMid1Y.setColumns(10);

        textFieldFeedMid1Z = new JTextField();
        panelPushPull.add(textFieldFeedMid1Z, "8, 16");
        textFieldFeedMid1Z.setColumns(10);

        textFieldFeedMid1C = new JTextField();
        panelPushPull.add(textFieldFeedMid1C, "10, 16, fill, default");
        textFieldFeedMid1C.setColumns(8);

        chckbxPush1 = new JCheckBox("");
        chckbxPush1.setToolTipText("Go to the Mid 1 Location when pushing.");
        chckbxPush1.setSelected(true);
        panelPushPull.add(chckbxPush1, "12, 16, center, default");

        chckbxMulti1 = new JCheckBox("");
        chckbxMulti1.setToolTipText("Include the Mid 1 Location in multi-actuation motion (if the push/pull switch is also set).");
        chckbxMulti1.setSelected(true);
        panelPushPull.add(chckbxMulti1, "14, 16, 3, 1, center, default");

        chckbxPull1 = new JCheckBox("");
        chckbxPull1.setToolTipText("Go to the Mid 1 Location when pulling.");
        chckbxPull1.setSelected(true);
        panelPushPull.add(chckbxPull1, "18, 16, center, default");

        locationButtonsPanelFeedMid1 = new LocationButtonsPanel(textFieldFeedMid1X, textFieldFeedMid1Y, textFieldFeedMid1Z, (JTextField) null);
        locationButtonsPanelFeedMid1.setShowPositionToolNoSafeZ(true);
        panelPushPull.add(locationButtonsPanelFeedMid1, "20, 16, fill, default");

        lblFeedSpeed1_2 = new JLabel("Speed ↕");
        panelPushPull.add(lblFeedSpeed1_2, "10, 18, right, default");

        textFieldFeedPush2 = new JTextField();
        panelPushPull.add(textFieldFeedPush2, "12, 18, 3, 1");
        textFieldFeedPush2.setColumns(10);

        textFieldFeedPull1 = new JTextField();
        textFieldFeedPull1.setColumns(10);
        panelPushPull.add(textFieldFeedPull1, "16, 18, 3, 1");

        lblFeedMid2Location = new JLabel("Mid 2 Location");
        panelPushPull.add(lblFeedMid2Location, "2, 20, right, default");

        textFieldFeedMid2X = new JTextField();
        panelPushPull.add(textFieldFeedMid2X, "4, 20");
        textFieldFeedMid2X.setColumns(10);

        textFieldFeedMid2Y = new JTextField();
        panelPushPull.add(textFieldFeedMid2Y, "6, 20");
        textFieldFeedMid2Y.setColumns(10);

        textFieldFeedMid2Z = new JTextField();
        panelPushPull.add(textFieldFeedMid2Z, "8, 20");
        textFieldFeedMid2Z.setColumns(10);

        textFieldFeedMid2C = new JTextField();
        panelPushPull.add(textFieldFeedMid2C, "10, 20, fill, default");
        textFieldFeedMid2C.setColumns(8);

        chckbxPush2 = new JCheckBox("");
        chckbxPush2.setToolTipText("Go to the Mid 2 Location when pushing.");
        chckbxPush2.setSelected(true);
        panelPushPull.add(chckbxPush2, "12, 20, center, default");

        chckbxMulti2 = new JCheckBox("");
        chckbxMulti2.setToolTipText("Include the Mid 2 Location in multi-actuation motion (if the push/pull switch is also set).");
        chckbxMulti2.setSelected(true);
        panelPushPull.add(chckbxMulti2, "14, 20, 3, 1, center, default");

        chckbxPull2 = new JCheckBox("");
        chckbxPull2.setToolTipText("Go to the Mid 2 Location when pulling.");
        chckbxPull2.setSelected(true);
        panelPushPull.add(chckbxPull2, "18, 20, center, default");

        locationButtonsPanelFeedMid2 = new LocationButtonsPanel(textFieldFeedMid2X, textFieldFeedMid2Y, textFieldFeedMid2Z, (JTextField) null);
        locationButtonsPanelFeedMid2.setShowPositionToolNoSafeZ(true);
        panelPushPull.add(locationButtonsPanelFeedMid2, "20, 20, fill, default");

        lblFeedSpeed2_3 = new JLabel("Speed ↕");
        panelPushPull.add(lblFeedSpeed2_3, "10, 22, right, default");

        textFieldFeedPush3 = new JTextField();
        panelPushPull.add(textFieldFeedPush3, "12, 22, 3, 1");
        textFieldFeedPush3.setColumns(10);

        textFieldFeedPull2 = new JTextField();
        textFieldFeedPull2.setColumns(10);
        panelPushPull.add(textFieldFeedPull2, "16, 22, 3, 1");

        lblFeedMid3Location = new JLabel("Mid 3 Location");
        panelPushPull.add(lblFeedMid3Location, "2, 24, right, default");

        textFieldFeedMid3X = new JTextField();
        panelPushPull.add(textFieldFeedMid3X, "4, 24");
        textFieldFeedMid3X.setColumns(10);

        textFieldFeedMid3Y = new JTextField();
        panelPushPull.add(textFieldFeedMid3Y, "6, 24");
        textFieldFeedMid3Y.setColumns(10);

        textFieldFeedMid3Z = new JTextField();
        panelPushPull.add(textFieldFeedMid3Z, "8, 24, fill, default");
        textFieldFeedMid3Z.setColumns(10);

        textFieldFeedMid3C = new JTextField();
        textFieldFeedMid3C.setText("");
        panelPushPull.add(textFieldFeedMid3C, "10, 24, fill, default");
        textFieldFeedMid3C.setColumns(8);

        chckbxPush3 = new JCheckBox("");
        chckbxPush3.setToolTipText("Go to the Mid 3 Location when pushing.");
        chckbxPush3.setSelected(true);
        panelPushPull.add(chckbxPush3, "12, 24, center, default");

        chckbxMulti3 = new JCheckBox("");
        chckbxMulti3.setToolTipText("Include the Mid 3 Location in multi-actuation motion (if the push/pull switch is also set).");
        chckbxMulti3.setSelected(true);
        panelPushPull.add(chckbxMulti3, "14, 24, 3, 1, center, default");

        chckbxPull3 = new JCheckBox("");
        chckbxPull3.setToolTipText("Go to the Mid 3 Location when pulling.");
        chckbxPull3.setSelected(true);
        panelPushPull.add(chckbxPull3, "18, 24, center, default");

        locationButtonsPanelFeedMid3 = new LocationButtonsPanel(textFieldFeedMid3X, textFieldFeedMid3Y, textFieldFeedMid3Z, (JTextField) null);
        locationButtonsPanelFeedMid3.setShowPositionToolNoSafeZ(true);
        panelPushPull.add(locationButtonsPanelFeedMid3, "20, 24, fill, default");

        lblFeedSpeed3_4 = new JLabel("Speed ↕");
        panelPushPull.add(lblFeedSpeed3_4, "10, 26, right, default");

        textFieldFeedPush4 = new JTextField();
        panelPushPull.add(textFieldFeedPush4, "12, 26, 3, 1");
        textFieldFeedPush4.setColumns(10);

        textFieldFeedPull3 = new JTextField();
        textFieldFeedPull3.setColumns(10);
        panelPushPull.add(textFieldFeedPull3, "16, 26, 3, 1");

        JLabel lblFeedEndLocation = new JLabel("End Location");
        panelPushPull.add(lblFeedEndLocation, "2, 28, right, default");

        textFieldFeedEndX = new JTextField();
        panelPushPull.add(textFieldFeedEndX, "4, 28");
        textFieldFeedEndX.setColumns(8);

        textFieldFeedEndY = new JTextField();
        panelPushPull.add(textFieldFeedEndY, "6, 28");
        textFieldFeedEndY.setColumns(8);

        textFieldFeedEndZ = new JTextField();
        panelPushPull.add(textFieldFeedEndZ, "8, 28");
        textFieldFeedEndZ.setColumns(8);

        textFieldFeedEndC = new JTextField();
        panelPushPull.add(textFieldFeedEndC, "10, 28, fill, default");
        textFieldFeedEndC.setColumns(8);

        chckbxPushEnd = new JCheckBox("");
        chckbxPushEnd.setToolTipText("Go to the End Location when pushing.");
        chckbxPushEnd.setSelected(true);
        panelPushPull.add(chckbxPushEnd, "12, 28, center, default");

        chckbxMultiEnd = new JCheckBox("");
        chckbxMultiEnd.setToolTipText("Include the End Location in multi-actuation motion (if the push switch is also set).");
        chckbxMultiEnd.setSelected(true);
        panelPushPull.add(chckbxMultiEnd, "14, 28, 3, 1, center, default");

        locationButtonsPanelFeedEnd = new LocationButtonsPanel(textFieldFeedEndX, textFieldFeedEndY,
                textFieldFeedEndZ, null);
        locationButtonsPanelFeedEnd.setShowPositionToolNoSafeZ(true);
        panelPushPull.add(locationButtonsPanelFeedEnd, "20, 28, fill, default");

        // Compose a font list of the system, the one currently selected, even if the system does not know it (yet), and the empty selection 
        List<String> systemFontList = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames()); 
        List<String> fontList = new ArrayList<>();
        if (!systemFontList.contains(feeder.getOcrFontName())) {
            fontList.add(feeder.getOcrFontName());
        }
        fontList.add("");
        fontList.addAll(systemFontList);

        contentPanel.add(panelFields);
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();
        LongConverter longConverter = new LongConverter();
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        Head head = null;
        try {
            head = Configuration.get().getMachine().getDefaultHead();
        }
        catch (Exception e) {
            Logger.error(e, "Cannot determine default head of machine.");
        }
        NamedConverter<Actuator> actuatorConverter = (new NamedConverter<>(head.getActuators()));

        addWrappedBinding(feeder, "actuator", actuator, "selectedItem", actuatorConverter);
        addWrappedBinding(feeder, "actuator2", actuator2, "selectedItem", actuatorConverter);

        addWrappedBinding(feeder, "feedSpeedPush1", textFieldFeedPush1, "text", doubleConverter);
        addWrappedBinding(feeder, "feedSpeedPush2", textFieldFeedPush2, "text", doubleConverter);
        addWrappedBinding(feeder, "feedSpeedPush3", textFieldFeedPush3, "text", doubleConverter);
        addWrappedBinding(feeder, "feedSpeedPushEnd", textFieldFeedPush4, "text", doubleConverter);
        addWrappedBinding(feeder, "feedSpeedPull3", textFieldFeedPull3, "text", doubleConverter);
        addWrappedBinding(feeder, "feedSpeedPull2", textFieldFeedPull2, "text", doubleConverter);
        addWrappedBinding(feeder, "feedSpeedPull1", textFieldFeedPull1, "text", doubleConverter);
        addWrappedBinding(feeder, "feedSpeedPull0", textFieldFeedPull0, "text", doubleConverter);

        addWrappedBinding(feeder, "includedPush1", chckbxPush1, "selected");
        addWrappedBinding(feeder, "includedPush2", chckbxPush2, "selected");
        addWrappedBinding(feeder, "includedPush3", chckbxPush3, "selected");
        addWrappedBinding(feeder, "includedPushEnd", chckbxPushEnd, "selected");

        addWrappedBinding(feeder, "includedMulti0", chckbxMulti0, "selected");
        addWrappedBinding(feeder, "includedMulti1", chckbxMulti1, "selected");
        addWrappedBinding(feeder, "includedMulti2", chckbxMulti2, "selected");
        addWrappedBinding(feeder, "includedMulti3", chckbxMulti3, "selected");
        addWrappedBinding(feeder, "includedMultiEnd", chckbxMultiEnd, "selected");

        addWrappedBinding(feeder, "includedPull0", chckbxPull0, "selected");
        addWrappedBinding(feeder, "includedPull1", chckbxPull1, "selected");
        addWrappedBinding(feeder, "includedPull2", chckbxPull2, "selected");
        addWrappedBinding(feeder, "includedPull3", chckbxPull3, "selected");

        addWrappedBinding(feeder, "additiveRotation", additiveRotation, "selected");

        MutableLocationProxy feedStartLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "feedStartLocation", feedStartLocation, "location");
        addWrappedBinding(feedStartLocation, "lengthX", textFieldFeedStartX, "text",
                lengthConverter);
        addWrappedBinding(feedStartLocation, "lengthY", textFieldFeedStartY, "text",
                lengthConverter);
        addWrappedBinding(feedStartLocation, "lengthZ", textFieldFeedStartZ, "text",
                lengthConverter);
        addWrappedBinding(feedStartLocation, "rotation", textFieldFeedStartC, "text",
                doubleConverter);

        MutableLocationProxy feedMid1Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "feedMid1Location", feedMid1Location, "location");
        addWrappedBinding(feedMid1Location, "lengthX", textFieldFeedMid1X, "text",
                lengthConverter);
        addWrappedBinding(feedMid1Location, "lengthY", textFieldFeedMid1Y, "text",
                lengthConverter);
        addWrappedBinding(feedMid1Location, "lengthZ", textFieldFeedMid1Z, "text",
                lengthConverter);
        addWrappedBinding(feedMid1Location, "rotation", textFieldFeedMid1C, "text",
                doubleConverter);

        MutableLocationProxy feedMid2Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "feedMid2Location", feedMid2Location, "location");
        addWrappedBinding(feedMid2Location, "lengthX", textFieldFeedMid2X, "text",
                lengthConverter);
        addWrappedBinding(feedMid2Location, "lengthY", textFieldFeedMid2Y, "text",
                lengthConverter);
        addWrappedBinding(feedMid2Location, "lengthZ", textFieldFeedMid2Z, "text",
                lengthConverter);
        addWrappedBinding(feedMid2Location, "rotation", textFieldFeedMid2C, "text",
                doubleConverter);

        MutableLocationProxy feedMid3Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "feedMid3Location", feedMid3Location, "location");
        addWrappedBinding(feedMid3Location, "lengthX", textFieldFeedMid3X, "text",
                lengthConverter);
        addWrappedBinding(feedMid3Location, "lengthY", textFieldFeedMid3Y, "text",
                lengthConverter);
        addWrappedBinding(feedMid3Location, "lengthZ", textFieldFeedMid3Z, "text",
                lengthConverter);
        addWrappedBinding(feedMid3Location, "rotation", textFieldFeedMid3C, "text",
                doubleConverter);

        MutableLocationProxy feedEndLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "feedEndLocation", feedEndLocation, "location");
        addWrappedBinding(feedEndLocation, "lengthX", textFieldFeedEndX, "text", lengthConverter);
        addWrappedBinding(feedEndLocation, "lengthY", textFieldFeedEndY, "text", lengthConverter);
        addWrappedBinding(feedEndLocation, "lengthZ", textFieldFeedEndZ, "text", lengthConverter);
        addWrappedBinding(feedEndLocation, "rotation", textFieldFeedEndC, "text", doubleConverter);

        ComponentDecorators.decorateWithAutoSelect(textFieldFeedPush1);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedPush2);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedPush3);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedPush4);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedPull3);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedPull2);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedPull1);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedPull0);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedStartX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedStartY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedStartZ);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedStartC);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedMid1X);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedMid1Y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedMid1Z);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedMid1C);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedMid2X);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedMid2Y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedMid2Z);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedMid2C);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedMid3X);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedMid3Y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedMid3Z);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedMid3C);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedEndX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedEndY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedEndZ);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedEndC);

        bind(UpdateStrategy.READ, actuator, "selectedItem", locationButtonsPanelFeedStart, "actuatorName");
        bind(UpdateStrategy.READ, actuator, "selectedItem", locationButtonsPanelFeedMid1, "actuatorName");
        bind(UpdateStrategy.READ, actuator, "selectedItem", locationButtonsPanelFeedMid2, "actuatorName");
        bind(UpdateStrategy.READ, actuator, "selectedItem", locationButtonsPanelFeedMid3, "actuatorName");
        bind(UpdateStrategy.READ, actuator, "selectedItem", locationButtonsPanelFeedEnd, "actuatorName");
    }
}
