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

package org.openpnp.machine.reference.wizards;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.components.TemplateImageControl;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.ContactProbeNozzle;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.ReferenceNozzleTip.VisionCalibration;
import org.openpnp.machine.reference.ReferenceNozzleTip.VisionCalibrationTrigger;
import org.openpnp.machine.reference.ReferenceNozzleTip.ZCalibrationTrigger;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Machine;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.TemplateImage;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceNozzleTipToolChangerWizard extends AbstractConfigurationWizard {
    private final ReferenceNozzleTip nozzleTip;

    public ReferenceNozzleTipToolChangerWizard(ReferenceNozzleTip nozzleTip) {
        this.nozzleTip = nozzleTip;

        panelChanger = new JPanel();
        panelChanger.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.Border.title"),
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelChanger);
        panelChanger.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
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


        label = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.Post1ActuatorLabel.text"));
        panelChanger.add(label, "2, 5, right, center");
        label = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.Post2ActuatorLabel.text"));
        panelChanger.add(label, "2, 7, right, center");
        label = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.Post3ActuatorLabel.text"));
        panelChanger.add(label, "2, 9, right, center");

        Machine myMachine = null;
        try {
            myMachine = Configuration.get().getMachine();
        }
        catch (Exception e){
            Logger.error(e, "Cannot determine Name of machine.");
        }

        tcPostOneComboBoxActuator = new JComboBox();
        tcPostOneComboBoxActuator.setModel(new ActuatorsComboBoxModel(myMachine));
        panelChanger.add(tcPostOneComboBoxActuator, "4, 5, 3, 1");

        tcPostTwoComboBoxActuator = new JComboBox();
        tcPostTwoComboBoxActuator.setModel(new ActuatorsComboBoxModel(myMachine));
        panelChanger.add(tcPostTwoComboBoxActuator, "4, 7, 3, 1");

        tcPostThreeComboBoxActuator = new JComboBox();
        tcPostThreeComboBoxActuator.setModel(new ActuatorsComboBoxModel(myMachine));
        panelChanger.add(tcPostThreeComboBoxActuator, "4, 9, 3, 1");

        lblX = new JLabel("X");
        panelChanger.add(lblX, "4, 2, center, default");

        lblY_1 = new JLabel("Y");
        panelChanger.add(lblY_1, "6, 2, center, default");

        lblZ_1 = new JLabel("Z");
        panelChanger.add(lblZ_1, "8, 2, center, default");

        lblSpeed = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.SpeedLabel.text"));
        panelChanger.add(lblSpeed, "10, 2, center, default");

        lblSpeed1_2 = new JLabel("1 ↔ 2");
        panelChanger.add(lblSpeed1_2, "8, 5, right, default");

        lblSpeed2_3 = new JLabel("2 ↔ 3");
        panelChanger.add(lblSpeed2_3, "8, 7, right, default");

        lblSpeed3_4 = new JLabel("3 ↔ 4");
        panelChanger.add(lblSpeed3_4, "8, 9, right, default");

        lblStartLocation = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.FirstLocationLabel.text"));
        lblStartLocation.setToolTipText(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.FirstLocationLabel.toolTipText"));
        panelChanger.add(lblStartLocation, "2, 4, right, default");

        textFieldChangerStartX = new JTextField();
        panelChanger.add(textFieldChangerStartX, "4, 4, fill, default");
        textFieldChangerStartX.setColumns(8);

        textFieldChangerStartY = new JTextField();
        panelChanger.add(textFieldChangerStartY, "6, 4, fill, default");
        textFieldChangerStartY.setColumns(8);

        textFieldChangerStartZ = new JTextField();
        panelChanger.add(textFieldChangerStartZ, "8, 4, fill, default");
        textFieldChangerStartZ.setColumns(8);

        textFieldChangerStartToMidSpeed = new JTextField();
        textFieldChangerStartToMidSpeed.setToolTipText(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.ChangerStartToMidSpeedTextField.toolTipText"));
        panelChanger.add(textFieldChangerStartToMidSpeed, "10, 5, fill, default");
        textFieldChangerStartToMidSpeed.setColumns(8);

        changerStartLocationButtonsPanel = new LocationButtonsPanel(textFieldChangerStartX,
                textFieldChangerStartY, textFieldChangerStartZ, (JTextField) null);
        changerStartLocationButtonsPanel.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerStartLocationButtonsPanel, "12, 4, fill, default");

        lblMiddleLocation = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.SecondLocationLabel.text"));
        panelChanger.add(lblMiddleLocation, "2, 6, right, default");

        textFieldChangerMidX = new JTextField();
        panelChanger.add(textFieldChangerMidX, "4, 6, fill, default");
        textFieldChangerMidX.setColumns(8);

        textFieldChangerMidY = new JTextField();
        panelChanger.add(textFieldChangerMidY, "6, 6, fill, default");
        textFieldChangerMidY.setColumns(8);

        textFieldChangerMidZ = new JTextField();
        panelChanger.add(textFieldChangerMidZ, "8, 6, fill, default");
        textFieldChangerMidZ.setColumns(8);

        textFieldChangerMidToMid2Speed = new JTextField();
        textFieldChangerMidToMid2Speed.setToolTipText(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.ChangerMidToMid2SpeedTextField.toolTipText"));
        textFieldChangerMidToMid2Speed.setColumns(8);
        panelChanger.add(textFieldChangerMidToMid2Speed, "10, 7, fill, default");

        changerMidLocationButtonsPanel = new LocationButtonsPanel(textFieldChangerMidX,
                textFieldChangerMidY, textFieldChangerMidZ, (JTextField) null);
        changerMidLocationButtonsPanel.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerMidLocationButtonsPanel, "12, 6, fill, default");

        lblMiddleLocation_1 = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.ThirdLocationLabel.text"));
        panelChanger.add(lblMiddleLocation_1, "2, 8, right, default");

        textFieldMidX2 = new JTextField();
        textFieldMidX2.setColumns(8);
        panelChanger.add(textFieldMidX2, "4, 8, fill, default");

        textFieldMidY2 = new JTextField();
        textFieldMidY2.setColumns(8);
        panelChanger.add(textFieldMidY2, "6, 8, fill, default");

        textFieldMidZ2 = new JTextField();
        textFieldMidZ2.setColumns(8);
        panelChanger.add(textFieldMidZ2, "8, 8, fill, default");

        textFieldChangerMid2ToEndSpeed = new JTextField();
        textFieldChangerMid2ToEndSpeed.setToolTipText(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.ChangerMidToEndSpeedTextField.toolTipText"));
        textFieldChangerMid2ToEndSpeed.setColumns(8);
        panelChanger.add(textFieldChangerMid2ToEndSpeed, "10, 9, fill, default");

        changerMidButtons2 = new LocationButtonsPanel(textFieldMidX2, textFieldMidY2, textFieldMidZ2, (JTextField) null);
        changerMidButtons2.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerMidButtons2, "12, 8, fill, default");

        lblEndLocation = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.LastLocationLabel.text"));
        panelChanger.add(lblEndLocation, "2, 10, right, default");

        textFieldChangerEndX = new JTextField();
        panelChanger.add(textFieldChangerEndX, "4, 10, fill, default");
        textFieldChangerEndX.setColumns(8);

        textFieldChangerEndY = new JTextField();
        panelChanger.add(textFieldChangerEndY, "6, 10, fill, default");
        textFieldChangerEndY.setColumns(8);

        textFieldChangerEndZ = new JTextField();
        panelChanger.add(textFieldChangerEndZ, "8, 10, fill, default");
        textFieldChangerEndZ.setColumns(8);

        changerEndLocationButtonsPanel = new LocationButtonsPanel(textFieldChangerEndX,
                textFieldChangerEndY, textFieldChangerEndZ, (JTextField) null);
        changerEndLocationButtonsPanel.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerEndLocationButtonsPanel, "12, 10, fill, default");

        lblTouchLocation = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.TouchLocationLabel.text"));
        panelChanger.add(lblTouchLocation, "2, 14, right, default");

        touchLocationX = new JTextField();
        panelChanger.add(touchLocationX, "4, 14, fill, default");
        touchLocationX.setColumns(10);

        touchLocationY = new JTextField();
        panelChanger.add(touchLocationY, "6, 14, fill, default");
        touchLocationY.setColumns(10);

        touchLocationZ = new JTextField();
        panelChanger.add(touchLocationZ, "8, 14, fill, default");
        touchLocationZ.setColumns(10);

        touchLocationButtonsPanel = new LocationButtonsPanel(touchLocationX, touchLocationY, touchLocationZ, (JTextField) null);
        touchLocationButtonsPanel.setShowContactProbeTool(true);
        touchLocationButtonsPanel.setContactProbeReference(true);
        panelChanger.add(touchLocationButtonsPanel, "12, 14, fill, fill");

        lblZCalibrate = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.ZCalibrateLabel.text"));
        lblZCalibrate.setToolTipText(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.ZCalibrateLabel.toolTipText"));
        panelChanger.add(lblZCalibrate, "2, 18, right, default");

        zCalibrationTrigger = new JComboBox(ZCalibrationTrigger.values());
        zCalibrationTrigger.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        panelChanger.add(zCalibrationTrigger, "4, 18, 3, 1, fill, default");
        
                calibrationOffsetZ = new JTextField();
                calibrationOffsetZ.setToolTipText(Translations.getString(
                        "ReferenceNozzleTipToolChangerWizard.ChangerPanel.CalibrationOffsetZTextField.toolTipText"));
                calibrationOffsetZ.setEditable(false);
                panelChanger.add(calibrationOffsetZ, "8, 18, fill, default");
                calibrationOffsetZ.setColumns(10);
        
                btnReset = new JButton(resetZCalibrationAction);
                panelChanger.add(btnReset, "10, 18, default, fill");

        btnCalibrateNow = new JButton(calibrateZAction);
        panelChanger.add(btnCalibrateNow, "12, 18");
        
                lblFailHoming = new JLabel(Translations.getString(
                        "ReferenceNozzleTipToolChangerWizard.ChangerPanel.FailHomingLabel.text"));
                lblFailHoming.setToolTipText(Translations.getString(
                        "ReferenceNozzleTipToolChangerWizard.ChangerPanel.FailHomingLabel.toolTipText"));
                panelChanger.add(lblFailHoming, "2, 20, right, default");
        
                zCalibrationFailHoming = new JCheckBox("");
                panelChanger.add(zCalibrationFailHoming, "4, 20");
        
        panelVision = new JPanel();
        panelVision.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.Border.title"),
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelVision);
        
        panelVision.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.LABEL_COMPONENT_GAP_ROWSPEC,
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
        
        lblVisionCalibration = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.VisionLocationLabel.text"));
        lblVisionCalibration.setToolTipText(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.VisionLocationLabel.toolTipText"));
        panelVision.add(lblVisionCalibration, "2, 2, right, default");
        
        visionCalibration = new JComboBox(VisionCalibration.values());
        visionCalibration.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        panelVision.add(visionCalibration, "4, 2");
        
        lblAdjustZ = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.AdjustZLabel.text"));
        lblAdjustZ.setToolTipText(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.AdjustZLabel.toolTipText"));
        panelVision.add(lblAdjustZ, "6, 2, right, default");
        
        visionCalibrationZAdjust = new JTextField();
        visionCalibrationZAdjust.setToolTipText("");
        panelVision.add(visionCalibrationZAdjust, "8, 2, fill, default");
        visionCalibrationZAdjust.setColumns(10);
        
        lblVisionCalibrationHelp = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.VisionCalibrationHelpLabel.text"));
        panelVision.add(lblVisionCalibrationHelp, "10, 2, 1, 10, default, top");
        
        lblCalibrationTrigger = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.CalibrationTriggerLabel.text"));
        panelVision.add(lblCalibrationTrigger, "2, 5, right, default");
        
        visionCalibrationTrigger = new JComboBox(VisionCalibrationTrigger.values());
        panelVision.add(visionCalibrationTrigger, "4, 5, fill, default");
        
        lblTemplateDimX = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.TemplateWidthLabel.text"));
        lblTemplateDimX.setToolTipText(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.TemplateWidthLabel.toolTipText"));
        panelVision.add(lblTemplateDimX, "2, 7, right, default");
        
        visionTemplateDimensionX = new JTextField();
        panelVision.add(visionTemplateDimensionX, "4, 7");
        visionTemplateDimensionX.setColumns(10);
        
        lblTemplateDimY = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.TemplateHeightLabel.text"));
        lblTemplateDimY.setToolTipText(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.TemplateHeightLabel.toolTipText"));
        panelVision.add(lblTemplateDimY, "6, 7, right, default");
        
        visionTemplateDimensionY = new JTextField();
        panelVision.add(visionTemplateDimensionY, "8, 7");
        visionTemplateDimensionY.setColumns(10);
        
        lblTolerance = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.ToleranceLabel.text"));
        lblTolerance.setToolTipText(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.ToleranceLabel.toolTipText"));
        panelVision.add(lblTolerance, "2, 9, right, default");
        
        visionTemplateTolerance = new JTextField();
        panelVision.add(visionTemplateTolerance, "4, 9, fill, default");
        visionTemplateTolerance.setColumns(10);
        
        lblPrecision = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.WantedPrecisionLabel.text"));
        lblPrecision.setToolTipText(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.WantedPrecisionLabel.toolTipText"));
        panelVision.add(lblPrecision, "6, 9, right, default");
        
        visionCalibrationTolerance = new JTextField();
        panelVision.add(visionCalibrationTolerance, "8, 9, fill, default");
        visionCalibrationTolerance.setColumns(10);
        
        lblMaxPasses = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.MaxPassesLabel.text"));
        panelVision.add(lblMaxPasses, "2, 11, right, default");
        
        visionCalibrationMaxPasses = new JTextField();
        panelVision.add(visionCalibrationMaxPasses, "4, 11, fill, default");
        visionCalibrationMaxPasses.setColumns(10);
        
        lblMinScore = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.MinScoreLabel.text"));
        lblMinScore.setToolTipText(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.MinScoreLabel.toolTipText"));
        panelVision.add(lblMinScore, "2, 13, right, default");
        
        visionMatchMinimumScore = new JTextField();
        panelVision.add(visionMatchMinimumScore, "4, 13, fill, default");
        visionMatchMinimumScore.setColumns(10);
        
        lblLastScore = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.LastScoreLabel.text"));
        panelVision.add(lblLastScore, "6, 13, right, default");
        
        visionMatchLastScore = new JTextField();
        visionMatchLastScore.setEditable(false);
        panelVision.add(visionMatchLastScore, "8, 13, fill, default");
        visionMatchLastScore.setColumns(10);
        
        btnTest = new JButton(visionCalibrateTestAction);
        panelVision.add(btnTest, "10, 13");
        
        lblTemplateEmpty = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.TemplateEmptyLabel.text"));
        panelVision.add(lblTemplateEmpty, "2, 17, right, top");
        
        btnCaptureEmpty = new JButton(captureTemplateImageEmptyAction);
        panelVision.add(btnCaptureEmpty, "4, 17, default, top");
        
        btnResetEmpty = new JButton(resetTemplateImageEmptyAction);
        panelVision.add(btnResetEmpty, "6, 17, default, top");
       
        visionTemplateImageEmpty = new TemplateImageControl();
        visionTemplateImageEmpty.setName("Empty");
        visionTemplateImageEmpty.setCamera(getCamera());
        panelVision.add(visionTemplateImageEmpty, "8, 17, 3, 1");
        
        lblTemplateOccupied = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.TemplateOccupiedLabel.text"));
        panelVision.add(lblTemplateOccupied, "2, 19, right, top");
        
        btnCaptureOccupied = new JButton(captureTemplateImageOccupiedAction);
        panelVision.add(btnCaptureOccupied, "4, 19, default, top");
        
        btnResetOccupied = new JButton(resetTemplateImageOccupiedAction);
        panelVision.add(btnResetOccupied, "6, 19, default, top");
        visionTemplateImageOccupied = new TemplateImageControl();
        visionTemplateImageOccupied.setName("Occupied");
        visionTemplateImageOccupied.setCamera(getCamera());
        panelVision.add(visionTemplateImageOccupied, "8, 19, 3, 1");
        visionTemplateImageOccupied.setBackground(Color.DARK_GRAY);

        panelClone = new JPanel();
        panelClone.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"),
                Translations.getString(
                        "ReferenceNozzleTipToolChangerWizard.CloningSettingsPanel.Border.title"),
                TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(panelClone);
        
        panelClone.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
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
                FormSpecs.LINE_GAP_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblTemplate = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.CloningSettingsPanel.BehaviorLabel.text"));
        panelClone.add(lblTemplate, "2, 2, right, default");
        lblTemplate.setToolTipText(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.CloningSettingsPanel.BehaviorLabel.toolTipText"));

        templateNozzleTip = new JRadioButton(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.CloningSettingsPanel.TemplateRadioButton.text"));
        templateNozzleTip.setToolTipText(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.CloningSettingsPanel.TemplateRadioButton.toolTipText"));
        templateNozzleTip.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        behaviorButtonGroup.add(templateNozzleTip);
        panelClone.add(templateNozzleTip, "4, 2");

        panel = new JPanel();
        panel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelClone.add(panel, "8, 2, 1, 5, fill, fill");
        panel.setLayout(new FormLayout(new ColumnSpec[]{
                FormSpecs.LABEL_COMPONENT_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[]{
                        FormSpecs.LABEL_COMPONENT_GAP_ROWSPEC,
                        RowSpec.decode("33px"),
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,}));

        btnCloneButton = new JButton(cloneFromNozzleTipAction);
        panel.add(btnCloneButton, "2, 2, 11, 1, fill, top");

        lblLocations = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.UnnamedPanel.LocationsLabel.text"));
        panel.add(lblLocations, "2, 4, right, default");

        cloneLocations = new JCheckBox("");
        cloneLocations.setSelected(true);
        panel.add(cloneLocations, "4, 4");

        lblZCalibration = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.UnnamedPanel.ZCalibrationLabel.text"));
        panel.add(lblZCalibration, "6, 4, right, default");

        cloneZCalibration = new JCheckBox("");
        cloneZCalibration.setSelected(true);
        panel.add(cloneZCalibration, "8, 4");

        lblCloneVisionCalibration = new JLabel(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.UnnamedPanel.VisionCalibrationLabel.text"));
        panel.add(lblCloneVisionCalibration, "10, 4, right, default");

        cloneVisionCalibration = new JCheckBox("");
        cloneVisionCalibration.setSelected(true);
        panel.add(cloneVisionCalibration, "12, 4");

        templateClone = new JRadioButton(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.UnnamedPanel.ClonesFromTemplateRadioButton.text"));
        templateClone.setToolTipText(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.UnnamedPanel.ClonesFromTemplateRadioButton.toolTipText"));
        behaviorButtonGroup.add(templateClone);
        panelClone.add(templateClone, "4, 4");

        templateLocked = new JRadioButton(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.UnnamedPanel.LockedRadioButton.text"));
        templateLocked.setToolTipText(Translations.getString(
                "ReferenceNozzleTipToolChangerWizard.UnnamedPanel.LockedRadioButton.toolTipText"));
        templateLocked.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        behaviorButtonGroup.add(templateLocked);
        panelClone.add(templateLocked, "4, 6");

        btnLevelZ = new JButton(referenceZAction);
        panelClone.add(btnLevelZ, "8, 9");


        
        adaptDialog();
    }

    private void adaptDialog() {
        boolean isTemplate = templateNozzleTip.isSelected();
        boolean isLocked = templateLocked.isSelected();
        btnCloneButton.setAction(isTemplate ? cloneToAllNozzleTipsAction : cloneFromNozzleTipAction);
        cloneFromNozzleTipAction.setEnabled(!isLocked);
        referenceZAction.setEnabled(isTemplate);

        boolean zProbing = (ContactProbeNozzle.isConfigured());
        lblTouchLocation.setVisible(zProbing);
        touchLocationX.setVisible(zProbing);
        touchLocationY.setVisible(zProbing);
        touchLocationZ.setVisible(zProbing);
        
        touchLocationButtonsPanel.setVisible(zProbing);
        lblZCalibrate.setVisible(zProbing);
        zCalibrationTrigger.setVisible(zProbing);
        calibrationOffsetZ.setVisible(zProbing);
        btnCalibrateNow.setVisible(zProbing);
        btnReset.setVisible(zProbing);

        ZCalibrationTrigger trigger = (ZCalibrationTrigger) zCalibrationTrigger.getSelectedItem();
        lblFailHoming.setVisible(zProbing && trigger != ZCalibrationTrigger.Manual);
        zCalibrationFailHoming.setVisible(zProbing && trigger != ZCalibrationTrigger.Manual);

        lblZCalibration.setVisible(zProbing);
        cloneZCalibration.setVisible(zProbing);

        boolean visionCalib = visionCalibration.getSelectedItem() != VisionCalibration.None;
        lblAdjustZ.setVisible(visionCalib);
        visionCalibrationZAdjust.setVisible(visionCalib);
        lblCalibrationTrigger.setVisible(visionCalib);
        visionCalibrationTrigger.setVisible(visionCalib);
        lblVisionCalibrationHelp.setVisible(visionCalib);
        lblTemplateDimX.setVisible(visionCalib);
        visionTemplateDimensionX.setVisible(visionCalib);
        lblTemplateDimY.setVisible(visionCalib);
        visionTemplateDimensionY.setVisible(visionCalib);
        lblTolerance.setVisible(visionCalib);
        visionTemplateTolerance.setVisible(visionCalib);
        lblPrecision.setVisible(visionCalib);
        visionCalibrationTolerance.setVisible(visionCalib);
        lblMaxPasses.setVisible(visionCalib);
        visionCalibrationMaxPasses.setVisible(visionCalib);
        lblMinScore.setVisible(visionCalib);
        visionMatchMinimumScore.setVisible(visionCalib);
        lblLastScore.setVisible(visionCalib);
        visionMatchLastScore.setVisible(visionCalib);
        btnTest.setVisible(visionCalib);
        lblTemplateEmpty.setVisible(visionCalib);
        btnCaptureEmpty.setVisible(visionCalib);
        btnResetEmpty.setVisible(visionCalib);
        visionTemplateImageEmpty.setVisible(visionCalib);
        lblTemplateOccupied.setVisible(visionCalib);
        btnCaptureOccupied.setVisible(visionCalib);
        btnResetOccupied.setVisible(visionCalib);
        visionTemplateImageOccupied.setVisible(visionCalib);
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        IntegerConverter intConverter = new IntegerConverter();

        MutableLocationProxy changerStartLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzleTip, "changerStartLocation", changerStartLocation,
                "location");
        addWrappedBinding(changerStartLocation, "lengthX", textFieldChangerStartX, "text",
                lengthConverter);
        addWrappedBinding(changerStartLocation, "lengthY", textFieldChangerStartY, "text",
                lengthConverter);
        addWrappedBinding(changerStartLocation, "lengthZ", textFieldChangerStartZ, "text",
                lengthConverter);
        addWrappedBinding(nozzleTip, "changerStartToMidSpeed", textFieldChangerStartToMidSpeed, "text",
                doubleConverter);

        MutableLocationProxy changerMidLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzleTip, "changerMidLocation", changerMidLocation,
                "location");
        addWrappedBinding(changerMidLocation, "lengthX", textFieldChangerMidX, "text",
                lengthConverter);
        addWrappedBinding(changerMidLocation, "lengthY", textFieldChangerMidY, "text",
                lengthConverter);
        addWrappedBinding(changerMidLocation, "lengthZ", textFieldChangerMidZ, "text",
                lengthConverter);
        addWrappedBinding(nozzleTip, "changerMidToMid2Speed", textFieldChangerMidToMid2Speed, "text",
                doubleConverter);

        MutableLocationProxy changerMidLocation2 = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzleTip, "changerMidLocation2", changerMidLocation2,
                "location");
        addWrappedBinding(changerMidLocation2, "lengthX", textFieldMidX2, "text",
                lengthConverter);
        addWrappedBinding(changerMidLocation2, "lengthY", textFieldMidY2, "text",
                lengthConverter);
        addWrappedBinding(changerMidLocation2, "lengthZ", textFieldMidZ2, "text",
                lengthConverter);
        addWrappedBinding(nozzleTip, "changerMid2ToEndSpeed", textFieldChangerMid2ToEndSpeed, "text",
                doubleConverter);

        MutableLocationProxy changerEndLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzleTip, "changerEndLocation", changerEndLocation,
                "location");
        addWrappedBinding(changerEndLocation, "lengthX", textFieldChangerEndX, "text",
                lengthConverter);
        addWrappedBinding(changerEndLocation, "lengthY", textFieldChangerEndY, "text",
                lengthConverter);
        addWrappedBinding(changerEndLocation, "lengthZ", textFieldChangerEndZ, "text",
                lengthConverter);

        MutableLocationProxy touchLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzleTip, "touchLocation", touchLocation,
                "location");
        addWrappedBinding(touchLocation, "lengthX", touchLocationX, "text",
                lengthConverter);
        addWrappedBinding(touchLocation, "lengthY", touchLocationY, "text",
                lengthConverter);
        addWrappedBinding(touchLocation, "lengthZ", touchLocationZ, "text",
                lengthConverter);

        addWrappedBinding(nozzleTip, "changerActuatorPostStepOne", tcPostOneComboBoxActuator, "selectedItem");
        addWrappedBinding(nozzleTip, "changerActuatorPostStepTwo", tcPostTwoComboBoxActuator, "selectedItem");
        addWrappedBinding(nozzleTip, "changerActuatorPostStepThree", tcPostThreeComboBoxActuator, "selectedItem");

        addWrappedBinding(nozzleTip, "visionCalibrationZAdjust", visionCalibrationZAdjust, "text",
                lengthConverter);

        addWrappedBinding(nozzleTip, "visionCalibration", visionCalibration, "selectedItem");
        addWrappedBinding(nozzleTip, "visionCalibrationTrigger", visionCalibrationTrigger, "selectedItem");
        addWrappedBinding(nozzleTip, "visionTemplateDimensionX", visionTemplateDimensionX, "text",
                lengthConverter);
        addWrappedBinding(nozzleTip, "visionTemplateDimensionY", visionTemplateDimensionY, "text",
                lengthConverter);
        addWrappedBinding(nozzleTip, "visionTemplateTolerance", visionTemplateTolerance, "text",
                lengthConverter);
        addWrappedBinding(nozzleTip, "visionCalibrationTolerance", visionCalibrationTolerance, "text",
                lengthConverter);
        addWrappedBinding(nozzleTip, "visionCalibrationMaxPasses", visionCalibrationMaxPasses, "text", 
                intConverter);
        addWrappedBinding(nozzleTip, "visionMatchMinimumScore", visionMatchMinimumScore, "text",
                doubleConverter);
        addWrappedBinding(nozzleTip, "visionMatchLastScore", visionMatchLastScore, "text",
                doubleConverter);

        addWrappedBinding(nozzleTip, "visionTemplateImageEmpty", visionTemplateImageEmpty, "templateImage");
        addWrappedBinding(nozzleTip, "visionTemplateImageOccupied", visionTemplateImageOccupied, "templateImage");

        addWrappedBinding(nozzleTip, "zCalibrationTrigger", zCalibrationTrigger, "selectedItem");
        addWrappedBinding(nozzleTip, "calibrationOffsetZ", calibrationOffsetZ, "text",
                lengthConverter);
        addWrappedBinding(nozzleTip, "zCalibrationFailHoming", zCalibrationFailHoming, "selected");

        addWrappedBinding(nozzleTip, "templateNozzleTip", templateNozzleTip, "selected");
        addWrappedBinding(nozzleTip, "templateClone", templateClone, "selected");
        addWrappedBinding(nozzleTip, "templateLocked", templateLocked, "selected");

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartToMidSpeed);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidToMid2Speed);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMidX2);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMidY2);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMidZ2);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMid2ToEndSpeed);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerEndX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerEndY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerEndZ);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(touchLocationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(touchLocationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(touchLocationZ);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(visionCalibrationZAdjust);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(visionTemplateDimensionX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(visionTemplateDimensionY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(visionTemplateTolerance);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(visionCalibrationTolerance);
        ComponentDecorators.decorateWithAutoSelect(visionCalibrationMaxPasses);
        ComponentDecorators.decorateWithAutoSelect(visionMatchMinimumScore);
        ComponentDecorators.decorateWithAutoSelect(visionMatchLastScore);
    }

    protected BufferedImage captureTemplateImage() throws Exception {
        Location location = nozzleTip.getVisionCalibration().getLocation(nozzleTip);
        if (location == null) {
            throw new Exception("Select a vision calibration location first.");
        }
        location = location.add(new Location(nozzleTip.getVisionCalibrationZAdjust().getUnits(), 
                0, 0, nozzleTip.getVisionCalibrationZAdjust().getValue(), 0));
        Camera camera = getCamera();
        if (camera == null) {
            throw new Exception("No down-looking camera found.");
        }
        MovableUtils.moveToLocationAtSafeZ(camera, location);
        BufferedImage image = camera.lightSettleAndCapture();
        Location upp = camera.getUnitsPerPixelAtZ();
        int width = ((int) Math.ceil(nozzleTip.getVisionTemplateDimensionX().divide(upp.getLengthX()))) & ~1; // divisible by 2
        int height = ((int) Math.ceil(nozzleTip.getVisionTemplateDimensionY().divide(upp.getLengthY()))) & ~1; // divisible by 2
        int x = (image.getWidth() - width)/2;
        int y = (image.getHeight() - height)/2;
        BufferedImage templateImage = image.getSubimage(x, y, width, height);
        return templateImage;
    }

    protected Camera getCamera() {
        try {
            Camera camera = MainFrame.get().getMachineControls().getSelectedTool().getHead()
                    .getDefaultCamera();
            return camera;
        }
        catch (Exception e) {
           return null;
        }
    }

    private Action calibrateZAction = new AbstractAction(Translations.getString(
            "ReferenceNozzleTipToolChangerWizard.Action.CalibrateZ")) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "ReferenceNozzleTipToolChangerWizard.Action.CalibrateZ.Description"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                ReferenceNozzle nozzle = ReferenceNozzleTipCalibrationWizard.getUiCalibrationNozzle(nozzleTip);
                if (nozzle instanceof ContactProbeNozzle) {
                    ((ContactProbeNozzle) nozzle).calibrateZ(nozzleTip);
                    nozzle.moveToSafeZ();
                }
                else {
                    throw new Exception("Nozzle "+nozzle.getName()+" is no ContactProbeNozzle.");
                }
            });
        }
    };

    private Action resetZCalibrationAction = new AbstractAction(Translations.getString(
            "ReferenceNozzleTipToolChangerWizard.Action.Reset")) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "ReferenceNozzleTipToolChangerWizard.Action.Reset.Description"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                ReferenceNozzle nozzle = ReferenceNozzleTipCalibrationWizard.getUiCalibrationNozzle(nozzleTip);
                if (nozzle instanceof ContactProbeNozzle) {
                    ((ContactProbeNozzle) nozzle).resetZCalibration();
                }
                else {
                    throw new Exception("Nozzle "+nozzle.getName()+" is no ContactProbeNozzle.");
                }
            });
        }
    };

    private Action cloneFromNozzleTipAction = new AbstractAction(Translations.getString(
            "ReferenceNozzleTipToolChangerWizard.Action.CloneFromNozzleTip"), Icons.importt) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "ReferenceNozzleTipToolChangerWizard.Action.CloneFromNozzleTip.Description"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.messageBoxOnException(() -> {
                if (!nozzleTip.getChangerStartLocation().isInitialized()) {
                    throw new Exception("Nozzle tip "+nozzleTip.getName()+" must at least have the First Location defined, "
                            + "before it can clone from the template. The displacement between the two changer slots must be known.");
                }
                ReferenceNozzleTip templateNozzleTip = ReferenceNozzleTip.getTemplateNozzleTip();
                nozzleTip.assignNozzleTipChangerSettings(templateNozzleTip, 
                        cloneLocations.isSelected(), cloneZCalibration.isSelected(), cloneVisionCalibration.isSelected());
            });
        }
    };

    private Action cloneToAllNozzleTipsAction = new AbstractAction(Translations.getString(
            "ReferenceNozzleTipToolChangerWizard.Action.CloneToAllNozzleTip"), Icons.export) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "ReferenceNozzleTipToolChangerWizard.Action.CloneToAllNozzleTip.Description"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.messageBoxOnException(() -> {
                for (NozzleTip nt : Configuration.get().getMachine().getNozzleTips()) {
                    if (nt != nozzleTip 
                            && nt instanceof ReferenceNozzleTip) {
                        ((ReferenceNozzleTip) nt).assignNozzleTipChangerSettings(nozzleTip, 
                                cloneLocations.isSelected(), cloneZCalibration.isSelected(), cloneVisionCalibration.isSelected());
                    }
                }
            });
        }
    };

    private Action referenceZAction = new AbstractAction(Translations.getString(
            "ReferenceNozzleTipToolChangerWizard.Action.ReferenceZ"), Icons.contactProbeNozzle) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "ReferenceNozzleTipToolChangerWizard.Action.ReferenceZ.Description"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                ContactProbeNozzle.referenceAllTouchLocationsZ();
            });
        }
    };

    private Action captureTemplateImageEmptyAction = new AbstractAction(Translations.getString(
            "ReferenceNozzleTipToolChangerWizard.Action.CaptureTemplateImageEmpty")) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "ReferenceNozzleTipToolChangerWizard.Action.CaptureTemplateImageEmpty.Description"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                BufferedImage templateImage = captureTemplateImage();
                nozzleTip.setVisionTemplateImageEmpty(new TemplateImage(templateImage));
            });
        }
    };

    private Action resetTemplateImageEmptyAction = new AbstractAction(Translations.getString(
            "ReferenceNozzleTipToolChangerWizard.Action.ResetTemplateImageEmpty")) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "ReferenceNozzleTipToolChangerWizard.Action.ResetTemplateImageEmpty.Description"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            nozzleTip.setVisionTemplateImageEmpty(null);
        }
    };

    private Action captureTemplateImageOccupiedAction = new AbstractAction(Translations.getString(
            "ReferenceNozzleTipToolChangerWizard.Action.CaptureTemplateImageOccupied")) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "ReferenceNozzleTipToolChangerWizard.Action.CaptureTemplateImageOccupied.Description"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                BufferedImage templateImage = captureTemplateImage();
                nozzleTip.setVisionTemplateImageOccupied(new TemplateImage(templateImage));
            });
        }
    };

    private Action resetTemplateImageOccupiedAction = new AbstractAction(Translations.getString(
            "ReferenceNozzleTipToolChangerWizard.Action.ResetCaptureTemplateImageOccupied")) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "ReferenceNozzleTipToolChangerWizard.Action.ResetCaptureTemplateImageOccupied.Description"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            nozzleTip.setVisionTemplateImageOccupied(null);
        }
    };
    private Action visionCalibrateTestAction = new AbstractAction(Translations.getString(
            "ReferenceNozzleTipToolChangerWizard.Action.VisionCalibrateTest")) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "ReferenceNozzleTipToolChangerWizard.Action.VisionCalibrateTest.Description"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                nozzleTip.resetVisionCalibration();
                nozzleTip.ensureVisionCalibration(true);
                nozzleTip.resetVisionCalibration();
            });
        }
    };

    private JPanel panelChanger;
    private JLabel lblY_1;
    private JLabel lblZ_1;
    private LocationButtonsPanel changerStartLocationButtonsPanel;
    private JLabel lblStartLocation;
    private JTextField textFieldChangerStartX;
    private JTextField textFieldChangerStartY;
    private JTextField textFieldChangerStartZ;
    private JLabel lblMiddleLocation;
    private JTextField textFieldChangerMidX;
    private JTextField textFieldChangerMidY;
    private JTextField textFieldChangerMidZ;
    private JLabel lblEndLocation;
    private JTextField textFieldChangerEndX;
    private JTextField textFieldChangerEndY;
    private JTextField textFieldChangerEndZ;
    private LocationButtonsPanel changerMidLocationButtonsPanel;
    private LocationButtonsPanel changerEndLocationButtonsPanel;
    private JLabel lblMiddleLocation_1;
    private JTextField textFieldMidX2;
    private JTextField textFieldMidY2;
    private JTextField textFieldMidZ2;
    private LocationButtonsPanel changerMidButtons2;
    private JTextField textFieldChangerStartToMidSpeed;
    private JTextField textFieldChangerMidToMid2Speed;
    private JTextField textFieldChangerMid2ToEndSpeed;
    private JLabel lblSpeed;
    private JLabel lblX;
    private JLabel lblSpeed1_2;
    private JLabel lblSpeed2_3;
    private JLabel lblSpeed3_4;
    private JLabel label;
    private JComboBox tcPostOneComboBoxActuator;
    private JComboBox tcPostTwoComboBoxActuator;
    private JComboBox tcPostThreeComboBoxActuator;
    private JLabel lblTemplate;
    private JButton btnCloneButton;
    private JLabel lblTouchLocation;
    private JTextField touchLocationX;
    private JTextField touchLocationY;
    private JTextField touchLocationZ;
    private LocationButtonsPanel touchLocationButtonsPanel;
    private JComboBox zCalibrationTrigger;
    private JLabel lblZCalibrate;
    private JButton btnCalibrateNow;
    private JButton btnReset;
    private JTextField calibrationOffsetZ;
    private JLabel lblFailHoming;
    private JCheckBox zCalibrationFailHoming;
    private JButton btnLevelZ;
    private JPanel panelVision;
    private JPanel panelClone;
    private JLabel lblVisionCalibration;
    private JComboBox visionCalibration;
    private TemplateImageControl visionTemplateImageEmpty;
    private TemplateImageControl visionTemplateImageOccupied;
    private JLabel lblTemplateDimX;
    private JTextField visionTemplateDimensionX;
    private JTextField visionTemplateDimensionY;
    private JLabel lblTemplateEmpty;
    private JLabel lblTemplateOccupied;
    private JButton btnCaptureEmpty;
    private JButton btnCaptureOccupied;
    private JButton btnResetEmpty;
    private JButton btnResetOccupied;
    private JLabel lblMinScore;
    private JTextField visionMatchMinimumScore;
    private JLabel lblLocations;
    private JCheckBox cloneLocations;
    private JLabel lblZCalibration;
    private JCheckBox cloneZCalibration;
    private JLabel lblCloneVisionCalibration;
    private JCheckBox cloneVisionCalibration;
    private JPanel panel;
    private JLabel lblVisionCalibrationHelp;
    private JLabel lblLastScore;
    private JTextField visionMatchLastScore;
    private JButton btnTest;
    private JLabel lblTolerance;
    private JTextField visionTemplateTolerance;
    private JLabel lblPrecision;
    private JTextField visionCalibrationTolerance;
    private JLabel lblTemplateDimY;
    private JLabel lblMaxPasses;
    private JTextField visionCalibrationMaxPasses;
    private JLabel lblCalibrationTrigger;
    private JComboBox visionCalibrationTrigger;
    private JRadioButton templateNozzleTip;
    private JRadioButton templateClone;
    private JRadioButton templateLocked;
    private final ButtonGroup behaviorButtonGroup = new ButtonGroup();
    private JTextField visionCalibrationZAdjust;
    private JLabel lblAdjustZ;
}
