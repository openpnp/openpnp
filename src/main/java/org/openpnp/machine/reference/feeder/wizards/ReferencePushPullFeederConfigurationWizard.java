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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.LongConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.PercentConverter;
import org.openpnp.machine.reference.feeder.ReferencePushPullFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferencePushPullFeederConfigurationWizard
extends AbstractReferenceFeederConfigurationWizard {
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
    private JLabel lblPartPitch;
    private JTextField textFieldPartPitch;
    private JTextField textFieldFeedPitch;
    private JLabel lblFeedPitch;
    private JLabel lblActuatorId;
    private JComboBox comboBoxFeedActuator;
    private JLabel lblPeelOffActuatorId;
    private JComboBox comboBoxPeelOffActuator;
    private JPanel panelTape;
    private JPanel panelVision;
    private JPanel panelLocations;
    private JPanel panelVisionEnabled;
    private LocationButtonsPanel locationButtonsPanelFirstPick;
    private LocationButtonsPanel locationButtonsPanelHole1;
    private LocationButtonsPanel locationButtonsPanelHole2;
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
    private JLabel lblZ_1;
    private JLabel lblRotation;
    private JLabel lblY_1;
    private JLabel lblX_1;
    private JLabel lblPickLocation;
    private JTextField textFieldPickLocationX;
    private JTextField textFieldPickLocationY;
    private JTextField textFieldPickLocationZ;
    private JTextField textFieldPickLocationRotation;
    private JLabel lblHole1Location;
    private JTextField textFieldHole1LocationX;
    private JTextField textFieldHole1LocationY;
    private JTextField textFieldHole2LocationX;
    private JTextField textFieldHole2LocationY;
    private JButton btnEditPipeline;
    private JButton btnResetPipeline;
    private JLabel lblFeedCount;
    private JTextField textFieldFeedCount;
    private JButton btnReset;
    private JButton btnDiscardParts;
    private JTextField textFieldFeedMultiplier;
    private JLabel lblMultiplier;
    private JLabel lblHole2Location;
    private JButton btnShowVisionFeatures;
    private JButton btnAutoSetup;

    public ReferencePushPullFeederConfigurationWizard(ReferencePushPullFeeder feeder) {
        super(feeder, false);
        this.feeder = feeder;

        JPanel panelFields = new JPanel();
        panelFields.setLayout(new BoxLayout(panelFields, BoxLayout.Y_AXIS));

        panelTape = new JPanel();
        panelTape.setBorder(new TitledBorder(null, "Tape Settings", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));

        panelFields.add(panelTape);
        panelTape.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(26dlu;default):grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
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
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,}));

        btnShowVisionFeatures = new JButton(showVisionFeaturesAction);
        btnShowVisionFeatures.setToolTipText("Preview the features recognized by Computer Vision.");
        btnShowVisionFeatures.setText("Preview Vision Features");
        panelTape.add(btnShowVisionFeatures, "2, 2, 3, 1, default, fill");

        btnAutoSetup = new JButton(autoSetupAction);
        panelTape.add(btnAutoSetup, "6, 2, 7, 1");

        lblX_1 = new JLabel("X");
        panelTape.add(lblX_1, "4, 4");

        lblY_1 = new JLabel("Y");
        panelTape.add(lblY_1, "6, 4");

        lblZ_1 = new JLabel("Z");
        panelTape.add(lblZ_1, "8, 4");

        lblRotation = new JLabel("Rotation");
        panelTape.add(lblRotation, "10, 4");

        lblPickLocation = new JLabel("Pick Location");
        lblPickLocation.setToolTipText("<html>Pick Location of the part. If multiple are produced by a feed operation<br/>\r\nthis must be the last one picked i.e. the one closest to the the tape reel.</html>");
        panelTape.add(lblPickLocation, "2, 6, right, default");

        textFieldPickLocationX = new JTextField();
        panelTape.add(textFieldPickLocationX, "4, 6");
        textFieldPickLocationX.setColumns(10);

        textFieldPickLocationY = new JTextField();
        panelTape.add(textFieldPickLocationY, "6, 6");
        textFieldPickLocationY.setColumns(10);

        textFieldPickLocationZ = new JTextField();
        panelTape.add(textFieldPickLocationZ, "8, 6");
        textFieldPickLocationZ.setColumns(10);

        textFieldPickLocationRotation = new JTextField();
        panelTape.add(textFieldPickLocationRotation, "10, 6");
        textFieldPickLocationRotation.setColumns(10);

        locationButtonsPanelFirstPick = new LocationButtonsPanel(textFieldPickLocationX, textFieldPickLocationY, textFieldPickLocationZ, 
                textFieldPickLocationRotation);
        panelTape.add(locationButtonsPanelFirstPick, "12, 6");

        lblHole1Location = new JLabel("Hole 1 Location");
        lblHole1Location.setToolTipText("<html>Choose Hole 1 closer to the tape reel.<br/>\r\nIf possible choose two holes that bracket the part(s) to be picked.\r\n</html>");
        panelTape.add(lblHole1Location, "2, 8, right, default");

        textFieldHole1LocationX = new JTextField();
        panelTape.add(textFieldHole1LocationX, "4, 8");
        textFieldHole1LocationX.setColumns(10);

        textFieldHole1LocationY = new JTextField();
        panelTape.add(textFieldHole1LocationY, "6, 8");
        textFieldHole1LocationY.setColumns(10);

        locationButtonsPanelHole1 = new LocationButtonsPanel(textFieldHole1LocationX, textFieldHole1LocationY, (JTextField) null, (JTextField) null);
        panelTape.add(locationButtonsPanelHole1, "12, 8");

        lblHole2Location = new JLabel("Hole 2 Location");
        lblHole2Location.setToolTipText("<html>Choose Hole 2 further away from the tape reel.<br/>\r\nIf possible choose two holes that bracket the part(s) to be picked.\r\n</html>");
        panelTape.add(lblHole2Location, "2, 10, right, default");

        textFieldHole2LocationX = new JTextField();
        panelTape.add(textFieldHole2LocationX, "4, 10");
        textFieldHole2LocationX.setColumns(10);

        textFieldHole2LocationY = new JTextField();
        panelTape.add(textFieldHole2LocationY, "6, 10");
        textFieldHole2LocationY.setColumns(10);

        locationButtonsPanelHole2 = new LocationButtonsPanel(textFieldHole2LocationX, textFieldHole2LocationY, (JTextField) null, (JTextField) null);
        panelTape.add(locationButtonsPanelHole2, "12, 10");

        lblPartPitch = new JLabel("Part Pitch");
        lblPartPitch.setToolTipText("Pitch of the parts in the tape (2mm, 4mm, 8mm, 12mm, etc.)");
        panelTape.add(lblPartPitch, "2, 14, right, default");

        textFieldPartPitch = new JTextField();
        textFieldPartPitch.setToolTipText("Pitch of the parts in the tape (2mm, 4mm, 8mm, 12mm, etc.)");
        panelTape.add(textFieldPartPitch, "4, 14");
        textFieldPartPitch.setColumns(5);

        lblFeedPitch = new JLabel("Feed Pitch");
        lblFeedPitch.setToolTipText("How much the tape will be advanced by one feed operation (usually multiples of 4mm)");
        panelTape.add(lblFeedPitch, "6, 14, right, default");

        textFieldFeedPitch = new JTextField();
        textFieldFeedPitch.setToolTipText("How much the tape will be advanced by one feed operation (usually multiples of 4mm)");
        panelTape.add(textFieldFeedPitch, "8, 14");
        textFieldFeedPitch.setColumns(10);

        lblMultiplier = new JLabel("Multiplier");
        lblMultiplier.setToolTipText("To improve efficiency you can actuate the feeder multiple times to feed more parts. ");
        panelTape.add(lblMultiplier, "2, 16, right, default");

        textFieldFeedMultiplier = new JTextField();
        textFieldFeedMultiplier.setToolTipText("To improve efficiency you can actuate the feeder multiple times to feed more parts. ");
        panelTape.add(textFieldFeedMultiplier, "4, 16");
        textFieldFeedMultiplier.setColumns(10);

        lblFeedCount = new JLabel("Feed Count");
        lblFeedCount.setToolTipText("Total feed count of the feeder.");
        panelTape.add(lblFeedCount, "6, 16, right, default");

        textFieldFeedCount = new JTextField();
        textFieldFeedCount.setToolTipText("Total feed count of the feeder.");
        panelTape.add(textFieldFeedCount, "8, 16");
        textFieldFeedCount.setColumns(10);

        btnDiscardParts = new JButton(discardPartsAction);
        panelTape.add(btnDiscardParts, "10, 16");

        btnReset = new JButton(resetFeedCountAction);
        panelTape.add(btnReset, "12, 16");

        panelLocations = new JPanel();
        panelFields.add(panelLocations);
        panelLocations.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Push-Pull Settings", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelLocations.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("left:default:grow"),},
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
                        RowSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        RowSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        RowSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,}));

        lblActuatorId = new JLabel("Actuator");
        panelLocations.add(lblActuatorId, "2, 2, right, default");

        Head head = null;
        try {
            head = Configuration.get().getMachine().getDefaultHead();
        }
        catch (Exception e) {
            Logger.error(e, "Cannot determine default head of machine.");
        }

        comboBoxFeedActuator = new JComboBox();
        panelLocations.add(comboBoxFeedActuator, "4, 2");
        comboBoxFeedActuator.setModel(new ActuatorsComboBoxModel(head));

        lblPeelOffActuatorId = new JLabel("Peel Off Actuator");
        panelLocations.add(lblPeelOffActuatorId, "8, 2");

        comboBoxPeelOffActuator = new JComboBox();
        panelLocations.add(comboBoxPeelOffActuator, "10, 2, 7, 1");
        comboBoxPeelOffActuator.setModel(new ActuatorsComboBoxModel(head));

        JLabel lblX = new JLabel("X");
        panelLocations.add(lblX, "4, 6");

        JLabel lblY = new JLabel("Y");
        panelLocations.add(lblY, "6, 6");

        JLabel lblZ = new JLabel("Z");
        panelLocations.add(lblZ, "8, 6");

        lblPush = new JLabel("↓");
        lblPush.setToolTipText("Locations that are included when pushing.");
        panelLocations.add(lblPush, "10, 6, center, default");

        lblMulti = new JLabel("↑↓");
        lblMulti.setToolTipText("<html>Locations that are included, when actuating multiple times.<br/>\r\nThe combination with the push ↓ and pull ↑ switch is taken.</html>");
        panelLocations.add(lblMulti, "12, 6, 3, 1, center, default");

        lblPull = new JLabel("↑");
        lblPull.setToolTipText("Locations that are included when pulling.");
        panelLocations.add(lblPull, "16, 6, center, default");

        JLabel lblFeedStartLocation = new JLabel("Start Location");
        panelLocations.add(lblFeedStartLocation, "2, 8, right, default");

        textFieldFeedStartX = new JTextField();
        panelLocations.add(textFieldFeedStartX, "4, 8");
        textFieldFeedStartX.setColumns(8);

        textFieldFeedStartY = new JTextField();
        panelLocations.add(textFieldFeedStartY, "6, 8");
        textFieldFeedStartY.setColumns(8);

        textFieldFeedStartZ = new JTextField();
        panelLocations.add(textFieldFeedStartZ, "8, 8");
        textFieldFeedStartZ.setColumns(8);

        chckbxMulti0 = new JCheckBox("");
        chckbxMulti0.setToolTipText("Include the Start Location in multi-actuating motion (if the pull switch is also set).");
        chckbxMulti0.setSelected(true);
        panelLocations.add(chckbxMulti0, "12, 8, 3, 1, center, default");

        chckbxPull0 = new JCheckBox("");
        chckbxPull0.setToolTipText("Go to the Start Location when pulling.");
        chckbxPull0.setSelected(true);
        panelLocations.add(chckbxPull0, "16, 8, center, default");

        locationButtonsPanelFeedStart = new LocationButtonsPanel(textFieldFeedStartX,
                textFieldFeedStartY, textFieldFeedStartZ, null);
        panelLocations.add(locationButtonsPanelFeedStart, "18, 8");

        lblFeedSpeed0_1 = new JLabel("Speed ↕");
        panelLocations.add(lblFeedSpeed0_1, "8, 10, right, default");

        textFieldFeedPush1 = new JTextField();
        panelLocations.add(textFieldFeedPush1, "10, 10, 3, 1");
        textFieldFeedPush1.setColumns(5);

        textFieldFeedPull0 = new JTextField();
        panelLocations.add(textFieldFeedPull0, "14, 10, 3, 1");
        textFieldFeedPull0.setColumns(10);

        lblFeedMid1Location = new JLabel("Mid 1 Location");
        panelLocations.add(lblFeedMid1Location, "2, 12, right, default");

        textFieldFeedMid1X = new JTextField();
        panelLocations.add(textFieldFeedMid1X, "4, 12");
        textFieldFeedMid1X.setColumns(10);

        textFieldFeedMid1Y = new JTextField();
        panelLocations.add(textFieldFeedMid1Y, "6, 12");
        textFieldFeedMid1Y.setColumns(10);

        textFieldFeedMid1Z = new JTextField();
        panelLocations.add(textFieldFeedMid1Z, "8, 12");
        textFieldFeedMid1Z.setColumns(10);

        chckbxPush1 = new JCheckBox("");
        chckbxPush1.setToolTipText("Go to the Mid 1 Location when pushing.");
        chckbxPush1.setSelected(true);
        panelLocations.add(chckbxPush1, "10, 12, center, default");

        chckbxMulti1 = new JCheckBox("");
        chckbxMulti1.setToolTipText("Include the Mid 1 Location in multi-actuation motion (if the push/pull switch is also set).");
        chckbxMulti1.setSelected(true);
        panelLocations.add(chckbxMulti1, "12, 12, 3, 1, center, default");

        chckbxPull1 = new JCheckBox("");
        chckbxPull1.setToolTipText("Go to the Mid 1 Location when pulling.");
        chckbxPull1.setSelected(true);
        panelLocations.add(chckbxPull1, "16, 12, center, default");

        locationButtonsPanelFeedMid1 = new LocationButtonsPanel(textFieldFeedMid1X, textFieldFeedMid1Y, textFieldFeedMid1Z, (JTextField) null);
        panelLocations.add(locationButtonsPanelFeedMid1, "18, 12");

        lblFeedSpeed1_2 = new JLabel("Speed ↕");
        panelLocations.add(lblFeedSpeed1_2, "8, 14, right, default");

        textFieldFeedPush2 = new JTextField();
        panelLocations.add(textFieldFeedPush2, "10, 14, 3, 1");
        textFieldFeedPush2.setColumns(10);

        textFieldFeedPull1 = new JTextField();
        textFieldFeedPull1.setColumns(10);
        panelLocations.add(textFieldFeedPull1, "14, 14, 3, 1");

        lblFeedMid2Location = new JLabel("Mid 2 Location");
        panelLocations.add(lblFeedMid2Location, "2, 16, right, default");

        textFieldFeedMid2X = new JTextField();
        panelLocations.add(textFieldFeedMid2X, "4, 16");
        textFieldFeedMid2X.setColumns(10);

        textFieldFeedMid2Y = new JTextField();
        panelLocations.add(textFieldFeedMid2Y, "6, 16");
        textFieldFeedMid2Y.setColumns(10);

        textFieldFeedMid2Z = new JTextField();
        panelLocations.add(textFieldFeedMid2Z, "8, 16");
        textFieldFeedMid2Z.setColumns(10);

        chckbxPush2 = new JCheckBox("");
        chckbxPush2.setToolTipText("Go to the Mid 2 Location when pushing.");
        chckbxPush2.setSelected(true);
        panelLocations.add(chckbxPush2, "10, 16, center, default");

        chckbxMulti2 = new JCheckBox("");
        chckbxMulti2.setToolTipText("Include the Mid 2 Location in multi-actuation motion (if the push/pull switch is also set).");
        chckbxMulti2.setSelected(true);
        panelLocations.add(chckbxMulti2, "12, 16, 3, 1, center, default");

        chckbxPull2 = new JCheckBox("");
        chckbxPull2.setToolTipText("Go to the Mid 2 Location when pulling.");
        chckbxPull2.setSelected(true);
        panelLocations.add(chckbxPull2, "16, 16, center, default");

        locationButtonsPanelFeedMid2 = new LocationButtonsPanel(textFieldFeedMid2X, textFieldFeedMid2Y, textFieldFeedMid2Z, (JTextField) null);
        panelLocations.add(locationButtonsPanelFeedMid2, "18, 16");

        lblFeedSpeed2_3 = new JLabel("Speed ↕");
        panelLocations.add(lblFeedSpeed2_3, "8, 18, right, default");

        textFieldFeedPush3 = new JTextField();
        panelLocations.add(textFieldFeedPush3, "10, 18, 3, 1");
        textFieldFeedPush3.setColumns(10);

        textFieldFeedPull2 = new JTextField();
        textFieldFeedPull2.setColumns(10);
        panelLocations.add(textFieldFeedPull2, "14, 18, 3, 1");

        lblFeedMid3Location = new JLabel("Mid 3 Location");
        panelLocations.add(lblFeedMid3Location, "2, 20, right, default");

        textFieldFeedMid3X = new JTextField();
        panelLocations.add(textFieldFeedMid3X, "4, 20");
        textFieldFeedMid3X.setColumns(10);

        textFieldFeedMid3Y = new JTextField();
        panelLocations.add(textFieldFeedMid3Y, "6, 20");
        textFieldFeedMid3Y.setColumns(10);

        textFieldFeedMid3Z = new JTextField();
        panelLocations.add(textFieldFeedMid3Z, "8, 20, fill, default");
        textFieldFeedMid3Z.setColumns(10);

        chckbxPush3 = new JCheckBox("");
        chckbxPush3.setToolTipText("Go to the Mid 3 Location when pushing.");
        chckbxPush3.setSelected(true);
        panelLocations.add(chckbxPush3, "10, 20, center, default");

        chckbxMulti3 = new JCheckBox("");
        chckbxMulti3.setToolTipText("Include the Mid 3 Location in multi-actuation motion (if the push/pull switch is also set).");
        chckbxMulti3.setSelected(true);
        panelLocations.add(chckbxMulti3, "12, 20, 3, 1, center, default");

        chckbxPull3 = new JCheckBox("");
        chckbxPull3.setToolTipText("Go to the Mid 3 Location when pulling.");
        chckbxPull3.setSelected(true);
        panelLocations.add(chckbxPull3, "16, 20, center, default");

        locationButtonsPanelFeedMid3 = new LocationButtonsPanel(textFieldFeedMid3X, textFieldFeedMid3Y, textFieldFeedMid3Z, (JTextField) null);
        panelLocations.add(locationButtonsPanelFeedMid3, "18, 20");

        lblFeedSpeed3_4 = new JLabel("Speed ↕");
        panelLocations.add(lblFeedSpeed3_4, "8, 22, right, default");

        textFieldFeedPush4 = new JTextField();
        panelLocations.add(textFieldFeedPush4, "10, 22, 3, 1");
        textFieldFeedPush4.setColumns(10);

        textFieldFeedPull3 = new JTextField();
        textFieldFeedPull3.setColumns(10);
        panelLocations.add(textFieldFeedPull3, "14, 22, 3, 1");

        JLabel lblFeedEndLocation = new JLabel("End Location");
        panelLocations.add(lblFeedEndLocation, "2, 24, right, default");

        textFieldFeedEndX = new JTextField();
        panelLocations.add(textFieldFeedEndX, "4, 24");
        textFieldFeedEndX.setColumns(8);

        textFieldFeedEndY = new JTextField();
        panelLocations.add(textFieldFeedEndY, "6, 24");
        textFieldFeedEndY.setColumns(8);

        textFieldFeedEndZ = new JTextField();
        panelLocations.add(textFieldFeedEndZ, "8, 24");
        textFieldFeedEndZ.setColumns(8);

        chckbxPushEnd = new JCheckBox("");
        chckbxPushEnd.setToolTipText("Go to the End Location when pushing.");
        chckbxPushEnd.setSelected(true);
        panelLocations.add(chckbxPushEnd, "10, 24, center, default");

        chckbxMultiEnd = new JCheckBox("");
        chckbxMultiEnd.setToolTipText("Include the End Location in multi-actuation motion (if the push switch is also set).");
        chckbxMultiEnd.setSelected(true);
        panelLocations.add(chckbxMultiEnd, "12, 24, 3, 1, center, default");

        locationButtonsPanelFeedEnd = new LocationButtonsPanel(textFieldFeedEndX, textFieldFeedEndY,
                textFieldFeedEndZ, null);
        panelLocations.add(locationButtonsPanelFeedEnd, "18, 24");

        //
        panelVision = new JPanel();
        panelVision.setBorder(new TitledBorder(null, "Vision", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panelFields.add(panelVision);
        panelVision.setLayout(new BoxLayout(panelVision, BoxLayout.Y_AXIS));

        panelVisionEnabled = new JPanel();
        panelVision.add(panelVisionEnabled);
        panelVisionEnabled.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.LABEL_COMPONENT_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.LINE_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblCalibrationTrigger = new JLabel("Calibration Trigger");
        panelVisionEnabled.add(lblCalibrationTrigger, "2, 2, right, default");

        comboBoxCalibrationTrigger = new JComboBox(ReferencePushPullFeeder.CalibrationTrigger.values());
        panelVisionEnabled.add(comboBoxCalibrationTrigger, "4, 2");
        
        lblPrecisionWanted = new JLabel("Precision wanted");
        lblPrecisionWanted.setToolTipText("Precision wanted i.e. the tolerable pick location offset");
        panelVisionEnabled.add(lblPrecisionWanted, "8, 2, right, default");
        
        textFieldPrecisionWanted = new JTextField();
        textFieldPrecisionWanted.setToolTipText("Precision wanted i.e. the tolerable pick location offset");
        panelVisionEnabled.add(textFieldPrecisionWanted, "10, 2");
        textFieldPrecisionWanted.setColumns(10);
        
        lblPrecisionAverage = new JLabel("Precision Average");
        lblPrecisionAverage.setToolTipText("Obtained precision average i.e. offset of the pick location, as detected by the calibration");
        panelVisionEnabled.add(lblPrecisionAverage, "14, 2, right, default");
        
        textFieldPrecisionAverage = new JTextField();
        textFieldPrecisionAverage.setToolTipText("Obtained precision average i.e. offset of the pick location, as detected by the calibration");
        textFieldPrecisionAverage.setEditable(false);
        panelVisionEnabled.add(textFieldPrecisionAverage, "16, 2");
        textFieldPrecisionAverage.setColumns(10);

        btnEditPipeline = new JButton(editPipelineAction);
        panelVisionEnabled.add(btnEditPipeline, "2, 4");

        btnResetPipeline = new JButton(resetPipelineAction);
        panelVisionEnabled.add(btnResetPipeline, "4, 4");
        
        lblCalibrationCount = new JLabel("Calibration Count");
        panelVisionEnabled.add(lblCalibrationCount, "8, 4, right, default");
        
        textFieldCalibrationCount = new JTextField();
        textFieldCalibrationCount.setEditable(false);
        panelVisionEnabled.add(textFieldCalibrationCount, "10, 4");
        textFieldCalibrationCount.setColumns(10);
        
        btnResetStatistics = new JButton("Reset Statistics");
        panelVisionEnabled.add(btnResetStatistics, "14, 4, 3, 1");

        contentPanel.add(panelFields);
        initDataBindings();
    }

    @Override
    public void createBindings() {
        super.createBindings();
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();
        LongConverter longConverter = new LongConverter();
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        PercentConverter percentConverter = new PercentConverter();

        MutableLocationProxy firstPickLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "location", firstPickLocation, "location");
        addWrappedBinding(firstPickLocation, "lengthX", textFieldPickLocationX, "text",
                lengthConverter);
        addWrappedBinding(firstPickLocation, "lengthY", textFieldPickLocationY, "text",
                lengthConverter);
        addWrappedBinding(firstPickLocation, "lengthZ", textFieldPickLocationZ, "text",
                lengthConverter);
        addWrappedBinding(firstPickLocation, "rotation", textFieldPickLocationRotation, "text",
                doubleConverter);

        MutableLocationProxy hole1Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "hole1Location", hole1Location, "location");
        addWrappedBinding(hole1Location, "lengthX", textFieldHole1LocationX, "text",
                lengthConverter);
        addWrappedBinding(hole1Location, "lengthY", textFieldHole1LocationY, "text",
                lengthConverter);

        MutableLocationProxy hole2Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "hole2Location", hole2Location, "location");
        addWrappedBinding(hole2Location, "lengthX", textFieldHole2LocationX, "text",
                lengthConverter);
        addWrappedBinding(hole2Location, "lengthY", textFieldHole2LocationY, "text",
                lengthConverter);

        addWrappedBinding(feeder, "partPitch", textFieldPartPitch, "text", lengthConverter);
        addWrappedBinding(feeder, "feedPitch", textFieldFeedPitch, "text", lengthConverter);
        addWrappedBinding(feeder, "feedMultiplier", textFieldFeedMultiplier, "text", longConverter);
        addWrappedBinding(feeder, "feedCount", textFieldFeedCount, "text", longConverter);

        addWrappedBinding(feeder, "actuatorName", comboBoxFeedActuator, "selectedItem");
        addWrappedBinding(feeder, "peelOffActuatorName", comboBoxPeelOffActuator, "selectedItem");

        addWrappedBinding(feeder, "feedSpeedPush1", textFieldFeedPush1, "text", percentConverter);
        addWrappedBinding(feeder, "feedSpeedPush2", textFieldFeedPush2, "text", percentConverter);
        addWrappedBinding(feeder, "feedSpeedPush3", textFieldFeedPush3, "text", percentConverter);
        addWrappedBinding(feeder, "feedSpeedPushEnd", textFieldFeedPush4, "text", percentConverter);
        addWrappedBinding(feeder, "feedSpeedPull3", textFieldFeedPull3, "text", percentConverter);
        addWrappedBinding(feeder, "feedSpeedPull2", textFieldFeedPull2, "text", percentConverter);
        addWrappedBinding(feeder, "feedSpeedPull1", textFieldFeedPull1, "text", percentConverter);
        addWrappedBinding(feeder, "feedSpeedPull0", textFieldFeedPull0, "text", percentConverter);

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

        MutableLocationProxy feedStartLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "feedStartLocation", feedStartLocation, "location");
        addWrappedBinding(feedStartLocation, "lengthX", textFieldFeedStartX, "text",
                lengthConverter);
        addWrappedBinding(feedStartLocation, "lengthY", textFieldFeedStartY, "text",
                lengthConverter);
        addWrappedBinding(feedStartLocation, "lengthZ", textFieldFeedStartZ, "text",
                lengthConverter);

        MutableLocationProxy feedMid1Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "feedMid1Location", feedMid1Location, "location");
        addWrappedBinding(feedMid1Location, "lengthX", textFieldFeedMid1X, "text",
                lengthConverter);
        addWrappedBinding(feedMid1Location, "lengthY", textFieldFeedMid1Y, "text",
                lengthConverter);
        addWrappedBinding(feedMid1Location, "lengthZ", textFieldFeedMid1Z, "text",
                lengthConverter);

        MutableLocationProxy feedMid2Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "feedMid2Location", feedMid2Location, "location");
        addWrappedBinding(feedMid2Location, "lengthX", textFieldFeedMid2X, "text",
                lengthConverter);
        addWrappedBinding(feedMid2Location, "lengthY", textFieldFeedMid2Y, "text",
                lengthConverter);
        addWrappedBinding(feedMid2Location, "lengthZ", textFieldFeedMid2Z, "text",
                lengthConverter);

        MutableLocationProxy feedMid3Location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "feedMid3Location", feedMid3Location, "location");
        addWrappedBinding(feedMid3Location, "lengthX", textFieldFeedMid3X, "text",
                lengthConverter);
        addWrappedBinding(feedMid3Location, "lengthY", textFieldFeedMid3Y, "text",
                lengthConverter);
        addWrappedBinding(feedMid3Location, "lengthZ", textFieldFeedMid3Z, "text",
                lengthConverter);

        MutableLocationProxy feedEndLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "feedEndLocation", feedEndLocation, "location");
        addWrappedBinding(feedEndLocation, "lengthX", textFieldFeedEndX, "text", lengthConverter);
        addWrappedBinding(feedEndLocation, "lengthY", textFieldFeedEndY, "text", lengthConverter);
        addWrappedBinding(feedEndLocation, "lengthZ", textFieldFeedEndZ, "text", lengthConverter);

        addWrappedBinding(feeder, "calibrationTrigger", comboBoxCalibrationTrigger, "selectedItem");

        addWrappedBinding(feeder, "calibrationCount", textFieldCalibrationCount, "text", intConverter);
        addWrappedBinding(feeder, "precisionWanted", textFieldPrecisionWanted, "text", lengthConverter);
        addWrappedBinding(feeder, "precisionAverage", textFieldPrecisionAverage, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPickLocationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPickLocationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPickLocationZ);
        ComponentDecorators.decorateWithAutoSelect(textFieldPickLocationRotation);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldHole1LocationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldHole1LocationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPartPitch);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedPitch);
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
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedMid1X);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedMid1Y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedMid1Z);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedMid2X);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedMid2Y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedMid2Z);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedMid3X);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedMid3Y);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedMid3Z);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedEndX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedEndY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedEndZ);

        bind(UpdateStrategy.READ, feeder, "actuatorName", locationButtonsPanelFeedStart, "actuatorName");
        bind(UpdateStrategy.READ, feeder, "actuatorName", locationButtonsPanelFeedMid1, "actuatorName");
        bind(UpdateStrategy.READ, feeder, "actuatorName", locationButtonsPanelFeedMid2, "actuatorName");
        bind(UpdateStrategy.READ, feeder, "actuatorName", locationButtonsPanelFeedMid3, "actuatorName");
        bind(UpdateStrategy.READ, feeder, "actuatorName", locationButtonsPanelFeedEnd, "actuatorName");
    }

    private Action editPipelineAction =
            new AbstractAction("Edit Pipeline") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Edit the Pipeline to be used for all vision operations of this feeder.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                editPipeline();
            });
        }
    };

    private Action resetPipelineAction =
            new AbstractAction("Reset Pipeline") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Reset the Pipeline for this feeder to the OpenPNP standard.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                resetPipeline();
            });
        }
    };

    private Action resetStatisticsAction =
            new AbstractAction("Reset Statistics") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Reset the average obtained precision statistics.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                feeder.resetCalibrationStatistics();
            });
        }
    };

    private Action resetFeedCountAction =
            new AbstractAction("Reset Feed Count") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Reset the feed count e.g. when a tape has been changed.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int result = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
                    "This will reset the recorded feed count of this feeder. Are you sure?",
                    null, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.YES_OPTION) {
                UiUtils.messageBoxOnException(() -> {
                    // we apply this because it is OpenPNP custom to do so 
                    applyAction.actionPerformed(e);
                    // set it back to 0
                    feeder.setFeedCount(0);
                });
            }
        }
    };
    private Action discardPartsAction =
            new AbstractAction("Discard Parts") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Discard parts that have been produced by the last tape transport.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                // we apply this because it is OpenPNP custom to do so 
                applyAction.actionPerformed(e);
                // round the feed count up to the next multiple of the parts per feed operation
                feeder.setFeedCount((feeder.getFeedCount()/feeder.getPartsPerFeedOperation()+1)*feeder.getPartsPerFeedOperation());
            });
        }
    };
    private Action showVisionFeaturesAction =
            new AbstractAction("Preview Vision Features") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Preview the features recognized by Computer Vision.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.submitUiMachineTask(() -> {
                feeder.showFeatures();
            });
        }
    };
    private Action autoSetupAction =
            new AbstractAction("Center the camera on the pick location and press this button to Auto-Setup", Icons.captureCamera) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>Center the camera on the pick location and press this button to Auto-Setup <br/>(if there are multiple picks per tape transport, choose the one closest to the tape reel.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.submitUiMachineTask(() -> {
                feeder.autoSetup();
            });
        }
    };
    private JLabel lblCalibrationTrigger;
    private JComboBox comboBoxCalibrationTrigger;
    private JLabel lblCalibrationCount;
    private JTextField textFieldCalibrationCount;
    private JLabel lblPrecisionAverage;
    private JTextField textFieldPrecisionAverage;
    private JLabel lblPrecisionWanted;
    private JTextField textFieldPrecisionWanted;
    private JButton btnResetStatistics;

    public HeadMountable getTool() throws Exception {
        return MainFrame.get().getMachineControls().getSelectedNozzle();
    }

    private void editPipeline() throws Exception {
        Camera camera = feeder.getCamera();
        CvPipeline pipeline = feeder.getCvPipeline(camera, false);
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new JDialog(MainFrame.get(), feeder.getName() + " Pipeline");
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(editor);
        dialog.setSize(1024, 768);
        dialog.setVisible(true);
    }    

    private void resetPipeline() {
        feeder.resetPipeline();
    }

    protected void initDataBindings() {
    }
}
