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
        panelChanger.setBorder(new TitledBorder(null, Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.Border.title", "Nozzle Tip Changer"),
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


        label = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.Post1ActuatorLabel.text",
                "Post 1 Actuator"));
        panelChanger.add(label, "2, 5, right, center");
        label = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.Post2ActuatorLabel.text",
                "Post 2 Actuator"));
        panelChanger.add(label, "2, 7, right, center");
        label = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.Post3ActuatorLabel.text",
                "Post 3 Actuator"));
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

        lblSpeed = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.SpeedLabel.text",
                "Speed"));
        panelChanger.add(lblSpeed, "10, 2, center, default");

        lblSpeed1_2 = new JLabel("1 ↔ 2");
        panelChanger.add(lblSpeed1_2, "8, 5, right, default");

        lblSpeed2_3 = new JLabel("2 ↔ 3");
        panelChanger.add(lblSpeed2_3, "8, 7, right, default");

        lblSpeed3_4 = new JLabel("3 ↔ 4");
        panelChanger.add(lblSpeed3_4, "8, 9, right, default");

        lblStartLocation = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.FirstLocationLabel.text",
                "First Location"));
        lblStartLocation.setToolTipText(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.FirstLocationLabel.toolTip.text",
                "<html>First location in a nozzle tip loading motion sequence.<br/>\n" +
                        "This is the first way-point when loading the nozzle tip and the<br/>\n" +
                        "last way-point when unloading it.\n" +
                        "</hml>"));
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
        textFieldChangerStartToMidSpeed.setToolTipText(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.ChangerStartToMidSpeedTextField.toolTip.text",
                "Speed between First location and Second location"));
        panelChanger.add(textFieldChangerStartToMidSpeed, "10, 5, fill, default");
        textFieldChangerStartToMidSpeed.setColumns(8);

        changerStartLocationButtonsPanel = new LocationButtonsPanel(textFieldChangerStartX,
                textFieldChangerStartY, textFieldChangerStartZ, (JTextField) null);
        changerStartLocationButtonsPanel.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerStartLocationButtonsPanel, "12, 4, fill, default");

        lblMiddleLocation = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.SecondLocationLabel.text",
                "Second Location"));
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
        textFieldChangerMidToMid2Speed.setToolTipText(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.ChangerMidToMid2SpeedTextField.toolTip.text",
                "Speed between Second location and Third location"));
        textFieldChangerMidToMid2Speed.setColumns(8);
        panelChanger.add(textFieldChangerMidToMid2Speed, "10, 7, fill, default");

        changerMidLocationButtonsPanel = new LocationButtonsPanel(textFieldChangerMidX,
                textFieldChangerMidY, textFieldChangerMidZ, (JTextField) null);
        changerMidLocationButtonsPanel.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerMidLocationButtonsPanel, "12, 6, fill, default");

        lblMiddleLocation_1 = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.ThirdLocationLabel.text",
                "Third Location"));
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
        textFieldChangerMid2ToEndSpeed.setToolTipText(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.ChangerMidToEndSpeedTextField.toolTip.text",
                "Speed between Third location and Last location"));
        textFieldChangerMid2ToEndSpeed.setColumns(8);
        panelChanger.add(textFieldChangerMid2ToEndSpeed, "10, 9, fill, default");

        changerMidButtons2 = new LocationButtonsPanel(textFieldMidX2, textFieldMidY2, textFieldMidZ2, (JTextField) null);
        changerMidButtons2.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerMidButtons2, "12, 8, fill, default");

        lblEndLocation = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.LastLocationLabel.text",
                "Last Location"));
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

        lblTouchLocation = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.TouchLocationLabel.text",
                "Touch Location"));
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

        lblZCalibrate = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.ZCalibrateLabel.text",
                "Auto Z Calibration"));
        lblZCalibrate.setToolTipText(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.ChangerPanel.ZCalibrateLabel.toolTip.text",
                        "<html>\n" +
                                "Calibrate the nozzle/nozzle tip Z by probing against the <strong>Touch Location</strong> surface.<br/>\n" +
                                "Z calibration can be triggered manually using the <strong>Calibrate now</strong> button or automatically:<br/>\n" +
                                "<ul>\n" +
                                "<li><strong>Manual:</strong> No automatic Z calibration is done. Manual calibration is stored<br/>\n" +
                                "permanently in the configuration and remains valid through machine homing.</li>\n" +
                                "<li><strong>MachineHome:</strong> Automatic Z calibration is performed when the machine is homed.<br/>\n" +
                                "The nozzle Z calibration is reused when another Nozzle Tip is loaded.</li>\n" +
                                "<li><strong>NozzleTipChange:</strong> Automatic Z calibration is done whenever this Nozzle Tip<br/>\n" +
                                "is changed or when the machine is homed and this Nozzle Tip is currently loaded.</li>\n" +
                                "</ul>\n" +
                                "Note, you can use a stand-in Nozzle Tip named \"unloaded\" to perform Z calibration of the naked nozzle. \n" +
                                "</html>"));
        panelChanger.add(lblZCalibrate, "2, 18, right, default");

        zCalibrationTrigger = new JComboBox(ZCalibrationTrigger.values());
        zCalibrationTrigger.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        panelChanger.add(zCalibrationTrigger, "4, 18, 3, 1, fill, default");
        
                calibrationOffsetZ = new JTextField();
                calibrationOffsetZ.setToolTipText(Translations.getStringOrDefault(
                        "ReferenceNozzleTipToolChangerWizard.ChangerPanel.CalibrationOffsetZTextField.toolTip.text",
                        "Calibrated Z offset of the nozzle"));
                calibrationOffsetZ.setEditable(false);
                panelChanger.add(calibrationOffsetZ, "8, 18, fill, default");
                calibrationOffsetZ.setColumns(10);
        
                btnReset = new JButton(resetZCalibrationAction);
                panelChanger.add(btnReset, "10, 18, default, fill");

        btnCalibrateNow = new JButton(calibrateZAction);
        panelChanger.add(btnCalibrateNow, "12, 18");
        
                lblFailHoming = new JLabel(Translations.getStringOrDefault(
                        "ReferenceNozzleTipToolChangerWizard.ChangerPanel.FailHomingLabel.text", "Fail Homing?"));
                lblFailHoming.setToolTipText(Translations.getStringOrDefault(
                        "ReferenceNozzleTipToolChangerWizard.ChangerPanel.FailHomingLabel.toolTip.text",
                        "When the Z calibration fails during homing, also fail the homing cycle."));
                panelChanger.add(lblFailHoming, "2, 20, right, default");
        
                zCalibrationFailHoming = new JCheckBox("");
                panelChanger.add(zCalibrationFailHoming, "4, 20");
        
        panelVision = new JPanel();
        panelVision.setBorder(new TitledBorder(null, Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.Border.title",
                "Vision Calibration"), TitledBorder.LEADING, TitledBorder.TOP, null, null));
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
        
        lblVisionCalibration = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.VisionLocationLabel.text",
                "Vision Location"));
        lblVisionCalibration.setToolTipText(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.VisionLocationLabel.toolTip.text",
                "<html>Location for vision calibration, or None for no calibration.<br/>\n" +
                        "Choose a location where the nozzle tip in the slot is visible. \n" +
                        "</html>"));
        panelVision.add(lblVisionCalibration, "2, 2, right, default");
        
        visionCalibration = new JComboBox(VisionCalibration.values());
        visionCalibration.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        panelVision.add(visionCalibration, "4, 2");
        
        lblAdjustZ = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.AdjustZLabel.text",
                "Adjust Z"));
        lblAdjustZ.setToolTipText(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.AdjustZLabel.toolTip.text",
                "<html>\n" +
                        "Adjust the Z coordinate of the Vision Location by this offset. Set it to the Z distance between <br/>\n" +
                        "the template subject that you are detecting with Vision Calibration, e.g. the visible surface of <br/>\n" +
                        "the changer slot, and the (imaginary) underside of the nozzle tip when at that location. Positive <br/>\n" +
                        "adjustment when the nozzle tip is below, negative when it is above that surface.<br/> \n" +
                        "The setting can overcome scaling errors in the camera view, especially when things are much <br/>\n" +
                        "closer to the camera than usual.<br/>\n" +
                        "<strong>3D Units per Pixel must be configured on the camera.</strong> \n" +
                        "</html>"
        ));
        panelVision.add(lblAdjustZ, "6, 2, right, default");
        
        visionCalibrationZAdjust = new JTextField();
        visionCalibrationZAdjust.setToolTipText("");
        panelVision.add(visionCalibrationZAdjust, "8, 2, fill, default");
        visionCalibrationZAdjust.setColumns(10);
        
        lblVisionCalibrationHelp = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.VisionCalibrationHelpLabel.text",
                "<html>\n" +
                        "Capture two template images of your nozzle tip changer slot<br/>\n" +
                        "both in empty and occupied state.<br/>\n" +
                        "Using the templates, vision calibration will then calibrate the<br/>\n" +
                        "changer locations in X/Y and also make sure the slot is empty<br/>\n" +
                        "or occupied as expected.\n" +
                        "</html>"));
        panelVision.add(lblVisionCalibrationHelp, "10, 2, 1, 10, default, top");
        
        lblCalibrationTrigger = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.CalibrationTriggerLabel.text",
                "Calibration Trigger"));
        panelVision.add(lblCalibrationTrigger, "2, 5, right, default");
        
        visionCalibrationTrigger = new JComboBox(VisionCalibrationTrigger.values());
        panelVision.add(visionCalibrationTrigger, "4, 5, fill, default");
        
        lblTemplateDimX = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.TemplateWidthLabel.text",
                "Template Width"));
        lblTemplateDimX.setToolTipText(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.TemplateWidthLabel.toolTip.text",
                "<html>\n" +
                        "Template image width (X). The template is centered around the selected Vision Location.<br/>\n" +
                        "Choose dimensions as small as possible, but the templates should include tell-tale horizontal and vertical<br/>\n" +
                        "edges. Furthermore, the nozzle tip should be visible when it occupies the changer slot.\n" +
                        "</html>"));
        panelVision.add(lblTemplateDimX, "2, 7, right, default");
        
        visionTemplateDimensionX = new JTextField();
        panelVision.add(visionTemplateDimensionX, "4, 7");
        visionTemplateDimensionX.setColumns(10);
        
        lblTemplateDimY = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.TemplateHeightLabel.text",
                "Template Height"));
        lblTemplateDimY.setToolTipText(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.TemplateHeightLabel.toolTip.text",
                "<html>\n" +
                        "Template image height (Y). The template is centered around the selected Vision Location.<br/>\n" +
                        "Choose dimensions as small as possible, but the templates should include tell-tale horizontal and vertical<br/>\n" +
                        "edges. Furthermore, the nozzle tip should be visible when it occupies the changer slot.\n" +
                        "</html>"));
        panelVision.add(lblTemplateDimY, "6, 7, right, default");
        
        visionTemplateDimensionY = new JTextField();
        panelVision.add(visionTemplateDimensionY, "8, 7");
        visionTemplateDimensionY.setColumns(10);
        
        lblTolerance = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.ToleranceLabel.text", "Tolerance"));
        lblTolerance.setToolTipText(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.ToleranceLabel.toolTip.text",
                "Maximum calibration tolerance i.e. how far away from the nominal location the calibrated location can be."));
        panelVision.add(lblTolerance, "2, 9, right, default");
        
        visionTemplateTolerance = new JTextField();
        panelVision.add(visionTemplateTolerance, "4, 9, fill, default");
        visionTemplateTolerance.setColumns(10);
        
        lblPrecision = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.WantedPrecisionLabel.text",
                "Wanted Precision"));
        lblPrecision.setToolTipText(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.WantedPrecisionLabel.toolTip.text",
                "<html>If the detected template image match is further away than the " +
                        "<strong>Wanted Precision</strong>,<br/>\n" +
                        "the camera is re-centered and another vision pass is made.</html>"
        ));
        panelVision.add(lblPrecision, "6, 9, right, default");
        
        visionCalibrationTolerance = new JTextField();
        panelVision.add(visionCalibrationTolerance, "8, 9, fill, default");
        visionCalibrationTolerance.setColumns(10);
        
        lblMaxPasses = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.MaxPassesLabel.text", "Max. Passes"));
        panelVision.add(lblMaxPasses, "2, 11, right, default");
        
        visionCalibrationMaxPasses = new JTextField();
        panelVision.add(visionCalibrationMaxPasses, "4, 11, fill, default");
        visionCalibrationMaxPasses.setColumns(10);
        
        lblMinScore = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.MinScoreLabel.text", "Minimum Score"));
        lblMinScore.setToolTipText(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.MinScoreLabel.toolTip.text",
                "<html>\n" +
                        "When the template images are matched against the camera image, a score is computed<br/>\n" +
                        "indicating the quality of the match. If the obtained score is smaller than the Minimum Score<br/>\n" +
                        "given here, the calibration fails. This should stop the machine from atempting a nozzle tip <br/>\n" +
                        "change, when the position is wrong. \n" +
                        "</html>"
        ));
        panelVision.add(lblMinScore, "2, 13, right, default");
        
        visionMatchMinimumScore = new JTextField();
        panelVision.add(visionMatchMinimumScore, "4, 13, fill, default");
        visionMatchMinimumScore.setColumns(10);
        
        lblLastScore = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.LastScoreLabel.text", "Last Score"));
        panelVision.add(lblLastScore, "6, 13, right, default");
        
        visionMatchLastScore = new JTextField();
        visionMatchLastScore.setEditable(false);
        panelVision.add(visionMatchLastScore, "8, 13, fill, default");
        visionMatchLastScore.setColumns(10);
        
        btnTest = new JButton(visionCalibrateTestAction);
        panelVision.add(btnTest, "10, 13");
        
        lblTemplateEmpty = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.TemplateEmptyLabel.text",
                "Template Empty"));
        panelVision.add(lblTemplateEmpty, "2, 17, right, top");
        
        btnCaptureEmpty = new JButton(captureTemplateImageEmptyAction);
        panelVision.add(btnCaptureEmpty, "4, 17, default, top");
        
        btnResetEmpty = new JButton(resetTemplateImageEmptyAction);
        panelVision.add(btnResetEmpty, "6, 17, default, top");
       
        visionTemplateImageEmpty = new TemplateImageControl();
        visionTemplateImageEmpty.setName("Empty");
        visionTemplateImageEmpty.setCamera(getCamera());
        panelVision.add(visionTemplateImageEmpty, "8, 17, 3, 1");
        
        lblTemplateOccupied = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.VisionCalibrationPanel.TemplateOccupiedLabel.text",
                "Template Occupied"));
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
                Translations.getStringOrDefault(
                        "ReferenceNozzleTipToolChangerWizard.CloningSettingsPanel.Border.title",
                        "Cloning Settings"),
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

        lblTemplate = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.CloningSettingsPanel.BehaviorLabel.text",
                "Behavior"));
        panelClone.add(lblTemplate, "2, 2, right, default");
        lblTemplate.setToolTipText(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.CloningSettingsPanel.BehaviorLabel.toolTip.text",
                "<html>\n" +
                        "One nozzle tip can become the <strong>Template</strong> for others to be cloned from.<br/>\n" +
                        "OpenPnP will automatically translate Locations relative to the First Location.<br/>\n" +
                        "If individual nozzle tips are special, mark them as <strong>Locked</strong>to prevent cloning.\n" +
                        "</html>"));

        templateNozzleTip = new JRadioButton(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.CloningSettingsPanel.TemplateRadioButton.text",
                "Template"));
        templateNozzleTip.setToolTipText(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.CloningSettingsPanel.TemplateRadioButton.toolTip.text",
                "Mark this nozzle tip as the template that can be cloned to and from the other nozzle tips."
        ));
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

        lblLocations = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.UnnamedPanel.LocationsLabel.text", "Locations?"));
        panel.add(lblLocations, "2, 4, right, default");

        cloneLocations = new JCheckBox("");
        cloneLocations.setSelected(true);
        panel.add(cloneLocations, "4, 4");

        lblZCalibration = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.UnnamedPanel.ZCalibrationLabel.text", "Z Calibration?"));
        panel.add(lblZCalibration, "6, 4, right, default");

        cloneZCalibration = new JCheckBox("");
        cloneZCalibration.setSelected(true);
        panel.add(cloneZCalibration, "8, 4");

        lblCloneVisionCalibration = new JLabel(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.UnnamedPanel.VisionCalibrationLabel.text",
                "Vision Calibration?"));
        panel.add(lblCloneVisionCalibration, "10, 4, right, default");

        cloneVisionCalibration = new JCheckBox("");
        cloneVisionCalibration.setSelected(true);
        panel.add(cloneVisionCalibration, "12, 4");

        templateClone = new JRadioButton(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.UnnamedPanel.ClonesFromTemplateRadioButton.text",
                "Clones from Template"));
        templateClone.setToolTipText(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.UnnamedPanel.ClonesFromTemplateRadioButton.toolTip.text",
                "This nozzle tip can clone its settings from the nozzle tip marked as the <strong>Template</strong>."));
        behaviorButtonGroup.add(templateClone);
        panelClone.add(templateClone, "4, 4");

        templateLocked = new JRadioButton(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.UnnamedPanel.LockedRadioButton.text",
                "Locked"));
        templateLocked.setToolTipText(Translations.getStringOrDefault(
                "ReferenceNozzleTipToolChangerWizard.UnnamedPanel.LockedRadioButton.toolTip.text",
                "<html>This nozzle tip is locked against cloning.<br/>Note, its touch location Z" +
                        " can still be calibrated against the template nozzle tip reference. </html>"
        ));
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

    private Action calibrateZAction = new AbstractAction(Translations.getStringOrDefault(
            "ReferenceNozzleTipToolChangerWizard.Action.CalibrateZ", "Calibrate now")) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getStringOrDefault(
                    "ReferenceNozzleTipToolChangerWizard.Action.CalibrateZ.Description",
                    "<html>Calibrate the nozzle/nozzle tip Z by contact-probing against the" +
                            " <strong>Touch Location</strong> surface.</html>"));
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

    private Action resetZCalibrationAction = new AbstractAction(Translations.getStringOrDefault(
            "ReferenceNozzleTipToolChangerWizard.Action.Reset", "Reset")) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getStringOrDefault(
                    "ReferenceNozzleTipToolChangerWizard.Action.Reset.Description",
                    "<html>Reset the Z calibration.</html>"));
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

    private Action cloneFromNozzleTipAction = new AbstractAction(Translations.getStringOrDefault(
            "ReferenceNozzleTipToolChangerWizard.Action.CloneFromNozzleTip",
            "Clone Tool Changer Settings from Template"), Icons.importt) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getStringOrDefault(
                    "ReferenceNozzleTipToolChangerWizard.Action.CloneFromNozzleTip.Description",
                    "<html>Clone the tool changer settings from the nozzle tip marked as <strong>Template</strong>.<br/>"
                            +"All the locations are translated relative to First Location.</html>"));
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

    private Action cloneToAllNozzleTipsAction = new AbstractAction(Translations.getStringOrDefault(
            "ReferenceNozzleTipToolChangerWizard.Action.CloneToAllNozzleTip",
            "Clone Tool Changer Settings to all Nozzle Tips"), Icons.export) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getStringOrDefault(
                    "ReferenceNozzleTipToolChangerWizard.Action.CloneToAllNozzleTip.Description",
                    "<html>Clone the Tool Changer settings from this <strong>Template</strong> nozzle tip,<br/>"
                            +"to all the others. Locations are translated relative to First Locations.</html>"));
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

    private Action referenceZAction = new AbstractAction(Translations.getStringOrDefault(
            "ReferenceNozzleTipToolChangerWizard.Action.ReferenceZ",
            "Calibrate all Touch Locations' Z to Template"), Icons.contactProbeNozzle) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getStringOrDefault(
                    "ReferenceNozzleTipToolChangerWizard.Action.ReferenceZ.Description",
                    "<html>Calibrate all the nozzle tip's touch location Z to the <strong>Template</strong> reference.<br/>"
                            + "This will load the template nozzle tip on the default probing nozzle, recalibrate<br/>"
                            + "the template's touch location Z and then probe and reference all the others to it.<br/>"
                            + "Note, unlike cloning this does include nozzle tips marked as <strong>Locked</strong>.</html>"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                ContactProbeNozzle.referenceAllTouchLocationsZ();
            });
        }
    };

    private Action captureTemplateImageEmptyAction = new AbstractAction(Translations.getStringOrDefault(
            "ReferenceNozzleTipToolChangerWizard.Action.CaptureTemplateImageEmpty",
            "Capture")) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getStringOrDefault(
                    "ReferenceNozzleTipToolChangerWizard.Action.CaptureTemplateImageEmpty.Description",
                    "<html>Capture the template image for the empty nozzle tip holder slot.</html>"));
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

    private Action resetTemplateImageEmptyAction = new AbstractAction(Translations.getStringOrDefault(
            "ReferenceNozzleTipToolChangerWizard.Action.ResetTemplateImageEmpty",
            "Reset")) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getStringOrDefault(
                    "ReferenceNozzleTipToolChangerWizard.Action.ResetTemplateImageEmpty.Description",
                    "<html>Reset the template image for the empty nozzle tip holder slot.</html>"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            nozzleTip.setVisionTemplateImageEmpty(null);
        }
    };

    private Action captureTemplateImageOccupiedAction = new AbstractAction(Translations.getStringOrDefault(
            "ReferenceNozzleTipToolChangerWizard.Action.CaptureTemplateImageOccupied",
            "Capture")) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getStringOrDefault(
                    "ReferenceNozzleTipToolChangerWizard.Action.CaptureTemplateImageOccupied.Description",
                    "<html>Capture the template image for the occupied nozzle tip holder slot.</html>"));
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

    private Action resetTemplateImageOccupiedAction = new AbstractAction(Translations.getStringOrDefault(
            "ReferenceNozzleTipToolChangerWizard.Action.ResetCaptureTemplateImageOccupied",
            "Reset")) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getStringOrDefault(
                    "ReferenceNozzleTipToolChangerWizard.Action.ResetCaptureTemplateImageOccupied.Description",
                    "<html>Reset the template image for the occupied nozzle tip holder slot.</html>"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            nozzleTip.setVisionTemplateImageOccupied(null);
        }
    };
    private Action visionCalibrateTestAction = new AbstractAction(Translations.getStringOrDefault(
            "ReferenceNozzleTipToolChangerWizard.Action.VisionCalibrateTest",
            "Test")) {
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getStringOrDefault(
                    "ReferenceNozzleTipToolChangerWizard.Action.VisionCalibrateTest.Description",
                    "<html>Test the vision calibration.</html>"));
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
