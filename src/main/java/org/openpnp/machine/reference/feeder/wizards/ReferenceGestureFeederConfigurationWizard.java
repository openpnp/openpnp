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
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.BufferedImageIconConverter;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.LongConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.PercentConverter;
import org.openpnp.machine.reference.feeder.ReferenceGestureFeeder;
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
public class ReferenceGestureFeederConfigurationWizard
extends AbstractReferenceFeederConfigurationWizard {
    private final ReferenceGestureFeeder feeder;
    private JTextField textFieldFeedStartX;
    private JTextField textFieldFeedStartY;
    private JTextField textFieldFeedStartZ;
    private JTextField textFieldFeedEndX;
    private JTextField textFieldFeedEndY;
    private JTextField textFieldFeedEndZ;
    private JTextField textFieldFeedForward1;
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
    private JCheckBox chckbxVisionEnabled;
    private JPanel panelVisionEnabled;
    private JSeparator separator;
    private LocationButtonsPanel locationButtonsPanelFirstPick;
    private LocationButtonsPanel locationButtonsPanelHole1;
    private LocationButtonsPanel locationButtonsPanelHole2;
    private LocationButtonsPanel locationButtonsPanelFeedStart;
    private LocationButtonsPanel locationButtonsPanelFeedMid1;
    private LocationButtonsPanel locationButtonsPanelFeedMid2;
    private LocationButtonsPanel locationButtonsPanelFeedMid3;
    private LocationButtonsPanel locationButtonsPanelFeedEnd;
    private JLabel lblForward;
    private JLabel lblMulti;
    private JLabel lblBackward;
    private JLabel lblFeedSpeed0_1;
    private JLabel lblFeedSpeed1_2;
    private JLabel lblFeedSpeed2_3;
    private JLabel lblFeedSpeed3_4;
    private JTextField textFieldFeedForward2;
    private JTextField textFieldFeedForward3;
    private JTextField textFieldFeedForward4;
    private JTextField textFieldFeedBackward3;
    private JTextField textFieldFeedBackward2;
    private JTextField textFieldFeedBackward1;
    private JTextField textFieldFeedBackward0;
    private JCheckBox chckbxForward1;
    private JCheckBox chckbxForward2;
    private JCheckBox chckbxForward3;
    private JCheckBox chckbxForwardEnd;
    private JCheckBox chckbxMulti0;
    private JCheckBox chckbxMulti1;
    private JCheckBox chckbxMulti2;
    private JCheckBox chckbxMulti3;
    private JCheckBox chckbxMultiEnd;
    private JCheckBox chckbxBackward0;
    private JCheckBox chckbxBackward1;
    private JCheckBox chckbxBackward2;
    private JCheckBox chckbxBackward3;
    private JLabel lblZ_1;
    private JLabel lblRotation;
    private JLabel lblY_1;
    private JLabel lblX_1;
    private JLabel lblFirstPickLocation;
    private JTextField textFieldFirstPickLocationX;
    private JTextField textFieldFirstPickLocationY;
    private JTextField textFieldFirstPickLocationZ;
    private JTextField textFieldFirstPickLocationRotation;
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

    public ReferenceGestureFeederConfigurationWizard(ReferenceGestureFeeder feeder) {
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
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblX_1 = new JLabel("X");
        panelTape.add(lblX_1, "4, 2");

        lblY_1 = new JLabel("Y");
        panelTape.add(lblY_1, "6, 2");

        lblZ_1 = new JLabel("Z");
        panelTape.add(lblZ_1, "8, 2");

        lblRotation = new JLabel("Rotation");
        panelTape.add(lblRotation, "10, 2");

        lblFirstPickLocation = new JLabel("First Pick Location");
        lblFirstPickLocation.setToolTipText("<html>Pick Location of the part. If multiple are produced by a feed operation<br/>\r\nthis must be the first part i.e. the one furthest away from the tape reel.</html>");
        panelTape.add(lblFirstPickLocation, "2, 4, right, default");

        textFieldFirstPickLocationX = new JTextField();
        panelTape.add(textFieldFirstPickLocationX, "4, 4");
        textFieldFirstPickLocationX.setColumns(10);

        textFieldFirstPickLocationY = new JTextField();
        panelTape.add(textFieldFirstPickLocationY, "6, 4");
        textFieldFirstPickLocationY.setColumns(10);

        textFieldFirstPickLocationZ = new JTextField();
        panelTape.add(textFieldFirstPickLocationZ, "8, 4");
        textFieldFirstPickLocationZ.setColumns(10);

        textFieldFirstPickLocationRotation = new JTextField();
        panelTape.add(textFieldFirstPickLocationRotation, "10, 4");
        textFieldFirstPickLocationRotation.setColumns(10);

        locationButtonsPanelFirstPick = new LocationButtonsPanel(textFieldFirstPickLocationX, textFieldFirstPickLocationY, textFieldFirstPickLocationZ, 
                textFieldFirstPickLocationRotation);
        panelTape.add(locationButtonsPanelFirstPick, "12, 4");

        lblHole1Location = new JLabel("Hole 1 Location");
        lblHole1Location.setToolTipText("<html>Choose Hole 1 closer to the tape reel.<br/>\r\nIf possible choose two holes that bracket the part(s) to be picked.\r\n</html>");
        panelTape.add(lblHole1Location, "2, 6, right, default");

        textFieldHole1LocationX = new JTextField();
        panelTape.add(textFieldHole1LocationX, "4, 6");
        textFieldHole1LocationX.setColumns(10);

        textFieldHole1LocationY = new JTextField();
        panelTape.add(textFieldHole1LocationY, "6, 6");
        textFieldHole1LocationY.setColumns(10);

        locationButtonsPanelHole1 = new LocationButtonsPanel(textFieldHole1LocationX, textFieldHole1LocationY, (JTextField) null, (JTextField) null);
        panelTape.add(locationButtonsPanelHole1, "12, 6");
                
                lblHole2Location = new JLabel("Hole 2 Location");
                lblHole2Location.setToolTipText("<html>Choose Hole 2 further away from the tape reel.<br/>\r\nIf possible choose two holes that bracket the part(s) to be picked.\r\n</html>");
                panelTape.add(lblHole2Location, "2, 8, right, default");
                
                textFieldHole2LocationX = new JTextField();
                panelTape.add(textFieldHole2LocationX, "4, 8");
                textFieldHole2LocationX.setColumns(10);
                
                textFieldHole2LocationY = new JTextField();
                panelTape.add(textFieldHole2LocationY, "6, 8");
                textFieldHole2LocationY.setColumns(10);
                
                locationButtonsPanelHole2 = new LocationButtonsPanel(textFieldHole2LocationX, textFieldHole2LocationY, (JTextField) null, (JTextField) null);
                panelTape.add(locationButtonsPanelHole2, "12, 8");
        
                lblPartPitch = new JLabel("Part Pitch");
                lblPartPitch.setToolTipText("Pitch of the parts in the tape (2mm, 4mm, 8mm, 12mm, etc.)");
                panelTape.add(lblPartPitch, "2, 12, right, default");
        
                textFieldPartPitch = new JTextField();
                textFieldPartPitch.setToolTipText("Pitch of the parts in the tape (2mm, 4mm, 8mm, 12mm, etc.)");
                panelTape.add(textFieldPartPitch, "4, 12");
                textFieldPartPitch.setColumns(5);
        
                lblFeedPitch = new JLabel("Feed Pitch");
                lblFeedPitch.setToolTipText("How much the tape will be advanced by one feed operation (usually multiples of 4mm)");
                panelTape.add(lblFeedPitch, "6, 12, right, default");
        
                textFieldFeedPitch = new JTextField();
                textFieldFeedPitch.setToolTipText("How much the tape will be advanced by one feed operation (usually multiples of 4mm)");
                panelTape.add(textFieldFeedPitch, "8, 12");
                textFieldFeedPitch.setColumns(10);
        
        btnShowVisionFeatures = new JButton(showVisionFeaturesAction);
        panelTape.add(btnShowVisionFeatures, "12, 12");
        
        lblMultiplier = new JLabel("Multiplier");
        lblMultiplier.setToolTipText("To improve efficiency you can actuate the feeder multiple times to feed more parts. ");
        panelTape.add(lblMultiplier, "2, 14, right, default");
        
        textFieldFeedMultiplier = new JTextField();
        textFieldFeedMultiplier.setToolTipText("To improve efficiency you can actuate the feeder multiple times to feed more parts. ");
        panelTape.add(textFieldFeedMultiplier, "4, 14");
        textFieldFeedMultiplier.setColumns(10);
        
        lblFeedCount = new JLabel("Feed Count");
        lblFeedCount.setToolTipText("Total feed count of the feeder.");
        panelTape.add(lblFeedCount, "6, 14, right, default");
        
        textFieldFeedCount = new JTextField();
        textFieldFeedCount.setToolTipText("Total feed count of the feeder.");
        panelTape.add(textFieldFeedCount, "8, 14");
        textFieldFeedCount.setColumns(10);
        
        btnDiscardParts = new JButton(discardPartsAction);
        panelTape.add(btnDiscardParts, "10, 14");
        
        btnReset = new JButton(resetFeedCountAction);
        panelTape.add(btnReset, "12, 14");

        panelLocations = new JPanel();
        panelFields.add(panelLocations);
        panelLocations.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Gesture Settings", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
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
        panelLocations.add(lblActuatorId, "2, 2");

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

        lblForward = new JLabel("↓");
        lblForward.setToolTipText("Locations are included when moving forward.");
        panelLocations.add(lblForward, "10, 6, center, default");

        lblMulti = new JLabel("↑↓");
        lblMulti.setToolTipText("<html>Locations are included, when feeding multiple times.<br/>\r\nThe combination with the forward ↓ and backward ↑ switch is taken.</html>");
        panelLocations.add(lblMulti, "12, 6, 3, 1, center, default");

        lblBackward = new JLabel("↑");
        lblBackward.setToolTipText("Locations are included when moving backward.");
        panelLocations.add(lblBackward, "16, 6, center, default");

        JLabel lblFeedStartLocation = new JLabel("Feed Start Location");
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
        chckbxMulti0.setToolTipText("Include the Feed Start Location in multi-feed motion (if the backward switch is also set).");
        chckbxMulti0.setSelected(true);
        panelLocations.add(chckbxMulti0, "12, 8, 3, 1, center, default");

        chckbxBackward0 = new JCheckBox("");
        chckbxBackward0.setToolTipText("Go to the Feed Start Location when moving the lever backward.");
        chckbxBackward0.setSelected(true);
        panelLocations.add(chckbxBackward0, "16, 8, center, default");

        locationButtonsPanelFeedStart = new LocationButtonsPanel(textFieldFeedStartX,
                textFieldFeedStartY, textFieldFeedStartZ, null);
        panelLocations.add(locationButtonsPanelFeedStart, "18, 8");

        lblFeedSpeed0_1 = new JLabel("Feed Speed ↕");
        panelLocations.add(lblFeedSpeed0_1, "8, 10, right, default");

        textFieldFeedForward1 = new JTextField();
        panelLocations.add(textFieldFeedForward1, "10, 10, 3, 1");
        textFieldFeedForward1.setColumns(5);

        textFieldFeedBackward0 = new JTextField();
        panelLocations.add(textFieldFeedBackward0, "14, 10, 3, 1");
        textFieldFeedBackward0.setColumns(10);

        lblFeedMid1Location = new JLabel("Feed Mid 1 Location");
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

        chckbxForward1 = new JCheckBox("");
        chckbxForward1.setToolTipText("Go to the Feed Mid 1 Location when moving the lever forward.");
        chckbxForward1.setSelected(true);
        panelLocations.add(chckbxForward1, "10, 12, center, default");

        chckbxMulti1 = new JCheckBox("");
        chckbxMulti1.setToolTipText("Include the Feed Mid 1 Location in multi-feed motion (if the forward/backward switch is also set).");
        chckbxMulti1.setSelected(true);
        panelLocations.add(chckbxMulti1, "12, 12, 3, 1, center, default");

        chckbxBackward1 = new JCheckBox("");
        chckbxBackward1.setToolTipText("Go to the Feed Mid 1 Location when moving the lever backward.");
        chckbxBackward1.setSelected(true);
        panelLocations.add(chckbxBackward1, "16, 12, center, default");

        locationButtonsPanelFeedMid1 = new LocationButtonsPanel(textFieldFeedMid1X, textFieldFeedMid1Y, textFieldFeedMid1Z, (JTextField) null);
        panelLocations.add(locationButtonsPanelFeedMid1, "18, 12");

        lblFeedSpeed1_2 = new JLabel("Feed Speed ↕");
        panelLocations.add(lblFeedSpeed1_2, "8, 14, right, default");

        textFieldFeedForward2 = new JTextField();
        panelLocations.add(textFieldFeedForward2, "10, 14, 3, 1");
        textFieldFeedForward2.setColumns(10);

        textFieldFeedBackward1 = new JTextField();
        textFieldFeedBackward1.setColumns(10);
        panelLocations.add(textFieldFeedBackward1, "14, 14, 3, 1");

        lblFeedMid2Location = new JLabel("Feed Mid 2 Location");
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

        chckbxForward2 = new JCheckBox("");
        chckbxForward2.setToolTipText("Go to the Feed Mid 2 Location when moving forward.");
        chckbxForward2.setSelected(true);
        panelLocations.add(chckbxForward2, "10, 16, center, default");

        chckbxMulti2 = new JCheckBox("");
        chckbxMulti2.setToolTipText("Include the Feed Mid 2 Location in multi-feed motion (if the forward/backward switch is also set).");
        chckbxMulti2.setSelected(true);
        panelLocations.add(chckbxMulti2, "12, 16, 3, 1, center, default");

        chckbxBackward2 = new JCheckBox("");
        chckbxBackward2.setToolTipText("Go to the Feed Mid 2 Location when moving backward.");
        chckbxBackward2.setSelected(true);
        panelLocations.add(chckbxBackward2, "16, 16, center, default");

        locationButtonsPanelFeedMid2 = new LocationButtonsPanel(textFieldFeedMid2X, textFieldFeedMid2Y, textFieldFeedMid2Z, (JTextField) null);
        panelLocations.add(locationButtonsPanelFeedMid2, "18, 16");

        lblFeedSpeed2_3 = new JLabel("Feed Speed ↕");
        panelLocations.add(lblFeedSpeed2_3, "8, 18, right, default");

        textFieldFeedForward3 = new JTextField();
        panelLocations.add(textFieldFeedForward3, "10, 18, 3, 1");
        textFieldFeedForward3.setColumns(10);

        textFieldFeedBackward2 = new JTextField();
        textFieldFeedBackward2.setColumns(10);
        panelLocations.add(textFieldFeedBackward2, "14, 18, 3, 1");

        lblFeedMid3Location = new JLabel("Feed Mid 3 Location");
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

        chckbxForward3 = new JCheckBox("");
        chckbxForward3.setToolTipText("Go to the Feed Mid 3 Location when moving forward.");
        chckbxForward3.setSelected(true);
        panelLocations.add(chckbxForward3, "10, 20, center, default");

        chckbxMulti3 = new JCheckBox("");
        chckbxMulti3.setToolTipText("Include the Feed Mid 3 Location in multi-feed motion (if the forward/backward switch is also set).");
        chckbxMulti3.setSelected(true);
        panelLocations.add(chckbxMulti3, "12, 20, 3, 1, center, default");

        chckbxBackward3 = new JCheckBox("");
        chckbxBackward3.setToolTipText("Go to the Feed Mid 3 Location when moving backward.");
        chckbxBackward3.setSelected(true);
        panelLocations.add(chckbxBackward3, "16, 20, center, default");

        locationButtonsPanelFeedMid3 = new LocationButtonsPanel(textFieldFeedMid3X, textFieldFeedMid3Y, textFieldFeedMid3Z, (JTextField) null);
        panelLocations.add(locationButtonsPanelFeedMid3, "18, 20");

        lblFeedSpeed3_4 = new JLabel("Feed Speed ↕");
        panelLocations.add(lblFeedSpeed3_4, "8, 22, right, default");

        textFieldFeedForward4 = new JTextField();
        panelLocations.add(textFieldFeedForward4, "10, 22, 3, 1");
        textFieldFeedForward4.setColumns(10);

        textFieldFeedBackward3 = new JTextField();
        textFieldFeedBackward3.setColumns(10);
        panelLocations.add(textFieldFeedBackward3, "14, 22, 3, 1");

        JLabel lblFeedEndLocation = new JLabel("Feed End Location");
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

        chckbxForwardEnd = new JCheckBox("");
        chckbxForwardEnd.setToolTipText("Go to the Feed Mid End Location when moving forward.");
        chckbxForwardEnd.setSelected(true);
        panelLocations.add(chckbxForwardEnd, "10, 24, center, default");

        chckbxMultiEnd = new JCheckBox("");
        chckbxMultiEnd.setToolTipText("Include the Feed Mid 1 Location in multi-feed motion (if the forward switch is also set).");
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
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {
                        FormSpecs.LINE_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,}));

        chckbxVisionEnabled = new JCheckBox("Vision Enabled?");
        panelVisionEnabled.add(chckbxVisionEnabled, "2, 2, fill, default");

        btnEditPipeline = new JButton(editPipelineAction);
        panelVisionEnabled.add(btnEditPipeline, "2, 4");

        btnResetPipeline = new JButton(resetPipelineAction);
        panelVisionEnabled.add(btnResetPipeline, "4, 4");

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
        BufferedImageIconConverter imageConverter = new BufferedImageIconConverter();
        PercentConverter percentConverter = new PercentConverter();

        MutableLocationProxy firstPickLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "location", firstPickLocation, "location");
        addWrappedBinding(firstPickLocation, "lengthX", textFieldFirstPickLocationX, "text",
                lengthConverter);
        addWrappedBinding(firstPickLocation, "lengthY", textFieldFirstPickLocationY, "text",
                lengthConverter);
        addWrappedBinding(firstPickLocation, "lengthZ", textFieldFirstPickLocationZ, "text",
                lengthConverter);
        addWrappedBinding(firstPickLocation, "rotation", textFieldFirstPickLocationRotation, "text",
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

        addWrappedBinding(feeder, "feedSpeedForward1", textFieldFeedForward1, "text", percentConverter);
        addWrappedBinding(feeder, "feedSpeedForward2", textFieldFeedForward2, "text", percentConverter);
        addWrappedBinding(feeder, "feedSpeedForward3", textFieldFeedForward3, "text", percentConverter);
        addWrappedBinding(feeder, "feedSpeedForwardEnd", textFieldFeedForward4, "text", percentConverter);
        addWrappedBinding(feeder, "feedSpeedBackward3", textFieldFeedBackward3, "text", percentConverter);
        addWrappedBinding(feeder, "feedSpeedBackward2", textFieldFeedBackward2, "text", percentConverter);
        addWrappedBinding(feeder, "feedSpeedBackward1", textFieldFeedBackward1, "text", percentConverter);
        addWrappedBinding(feeder, "feedSpeedBackward0", textFieldFeedBackward0, "text", percentConverter);

        addWrappedBinding(feeder, "includedForward1", chckbxForward1, "selected");
        addWrappedBinding(feeder, "includedForward2", chckbxForward2, "selected");
        addWrappedBinding(feeder, "includedForward3", chckbxForward3, "selected");
        addWrappedBinding(feeder, "includedForwardEnd", chckbxForwardEnd, "selected");

        addWrappedBinding(feeder, "includedMulti0", chckbxMulti0, "selected");
        addWrappedBinding(feeder, "includedMulti1", chckbxMulti1, "selected");
        addWrappedBinding(feeder, "includedMulti2", chckbxMulti2, "selected");
        addWrappedBinding(feeder, "includedMulti3", chckbxMulti3, "selected");
        addWrappedBinding(feeder, "includedMultiEnd", chckbxMultiEnd, "selected");

        addWrappedBinding(feeder, "includedBackward0", chckbxBackward0, "selected");
        addWrappedBinding(feeder, "includedBackward1", chckbxBackward1, "selected");
        addWrappedBinding(feeder, "includedBackward2", chckbxBackward2, "selected");
        addWrappedBinding(feeder, "includedBackward3", chckbxBackward3, "selected");

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

        addWrappedBinding(feeder, "visionEnabled", chckbxVisionEnabled, "selected");

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFirstPickLocationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFirstPickLocationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFirstPickLocationZ);
        ComponentDecorators.decorateWithAutoSelect(textFieldFirstPickLocationRotation);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldHole1LocationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldHole1LocationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPartPitch);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedPitch);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedForward1);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedForward2);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedForward3);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedForward4);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedBackward3);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedBackward2);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedBackward1);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedBackward0);
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
                    "Discard parts that have been produced by the last feed operation.");
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
            new AbstractAction("Show Vision Features") {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "Show the Features recognized by Computer Vision.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                // round the feed count up to the next multiple of the parts per feed operation
                feeder.showFeatures();
            });
        }
    };
    private JButton btnShowVisionFeatures;

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
