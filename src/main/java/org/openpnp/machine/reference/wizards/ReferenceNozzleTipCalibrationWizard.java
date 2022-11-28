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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.HsvIndicator;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.ReferenceNozzleTipCalibration;
import org.openpnp.machine.reference.ReferenceNozzleTipCalibration.BackgroundCalibrationMethod;
import org.openpnp.machine.reference.ReferenceNozzleTipCalibration.RecalibrationTrigger;
import org.openpnp.machine.reference.camera.ReferenceCamera;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import org.openpnp.vision.pipeline.ui.CvPipelineEditorDialog;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceNozzleTipCalibrationWizard extends AbstractConfigurationWizard {
    private final ReferenceNozzleTip nozzleTip;

    private JPanel panelCalibration;
    private JButton btnEditPipeline;
    private JButton btnResetPipeline;
    private JButton buttonCenterTool;
    private JLabel lblAngleIncrements;
    private JTextField angleIncrementsTf;
    private JLabel lblOffsetThreshold;
    private JTextField offsetThresholdTf;
    private JButton btnCalibrate;
    private JButton btnReset;
    private JLabel lblCalibrationInfo;
    private JLabel calibrationStatus;
    private JCheckBox calibrationEnabledCheckbox;

    private JPanel panelBackground;

    private JPanel panelTop;


    public ReferenceNozzleTipCalibrationWizard(ReferenceNozzleTip nozzleTip) {
        this.nozzleTip = nozzleTip;

        CellConstraints cc = new CellConstraints();

        panelTop = new JPanel();
        panelTop.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.TopPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelTop);
        panelTop.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(96dlu;default)"),
                FormSpecs.UNRELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,}));

        calibrationEnabledCheckbox = new JCheckBox(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.TopPanel.CalibrationEnabledChkBox.text")); //$NON-NLS-1$
        panelTop.add(calibrationEnabledCheckbox, "2, 2, right, default");


        buttonCenterTool = new JButton(positionToolAction);
        panelTop.add(buttonCenterTool, "4, 2, fill, default");
        buttonCenterTool.setHideActionText(true);

        lblCalibrate = new JLabel(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.TopPanel.CalibrationLabel.text")); //$NON-NLS-1$
        panelTop.add(lblCalibrate, "2, 4, right, default");

        btnCalibrate = new JButton(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.TopPanel.CalibrateButton.text")); //$NON-NLS-1$
        panelTop.add(btnCalibrate, "4, 4");

        btnReset = new JButton(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.TopPanel.ResetButton.text")); //$NON-NLS-1$
        panelTop.add(btnReset, "6, 4");

        btnCalibrateCamera = new JButton(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.TopPanel.CalibrateCameraButton.text")); //$NON-NLS-1$
        panelTop.add(btnCalibrateCamera, "8, 4");
        btnCalibrateCamera.setToolTipText(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.TopPanel.CalibrateCameraButton.toolTipText")); //$NON-NLS-1$

        lblRecalibration = new JLabel(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.TopPanel.RecalibrationLabel.text")); //$NON-NLS-1$
        panelTop.add(lblRecalibration, "2, 6, right, default");
        lblRecalibration.setToolTipText(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.TopPanel.RecalibrationLabel.toolTipText")); //$NON-NLS-1$

        recalibrationCb = new JComboBox(ReferenceNozzleTipCalibration.RecalibrationTrigger.values());
        panelTop.add(recalibrationCb, "4, 6, 3, 1");

        lblFailHoming = new JLabel(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.TopPanel.FailHomingLabel.text")); //$NON-NLS-1$
        panelTop.add(lblFailHoming, "2, 8, right, default");
        lblFailHoming.setToolTipText(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.TopPanel.FailHomingLabel.toolTipText")); //$NON-NLS-1$

        failHoming = new JCheckBox("");
        panelTop.add(failHoming, "4, 8");
        recalibrationCb.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        btnCalibrateCamera.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                calibrateCamera();
            }
        });
        btnReset.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                nozzleTip.getCalibration()
                .resetAll();
            }
        });
        btnCalibrate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                calibrate();
            }
        });
        calibrationEnabledCheckbox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent arg0) {
                adaptDialog();
            }
        });

        panelCalibration = new JPanel();
        panelCalibration.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.CalibrationPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelCalibration);
        panelCalibration.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.UNRELATED_GAP_COLSPEC,
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
                        FormSpecs.DEFAULT_ROWSPEC,}));


        lblCalibrationInfo = new JLabel(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.CalibrationPanel.StatusLabel.text")); //$NON-NLS-1$
        panelCalibration.add(lblCalibrationInfo, "2, 2, right, default");

        calibrationStatus = new JLabel(getCalibrationStatus());
        panelCalibration.add(calibrationStatus, "4, 2, 3, 1, left, default");

        lblAngleIncrements = new JLabel(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.CalibrationPanel.AngleIncrementsLabel.text")); //$NON-NLS-1$
        panelCalibration.add(lblAngleIncrements, "2, 4, right, default");

        angleIncrementsTf = new JTextField();
        panelCalibration.add(angleIncrementsTf, "4, 4, fill, default");
        angleIncrementsTf.setColumns(3);

        lblAllowMisdectects = new JLabel(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.CalibrationPanel.AllowMisDetectsLabel.text")); //$NON-NLS-1$
        lblAllowMisdectects.setToolTipText(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.CalibrationPanel.AllowMisDetectsLabel.toolTipText")); //$NON-NLS-1$
        panelCalibration.add(lblAllowMisdectects, "6, 4, right, default");

        allowMisdetectsTf = new JTextField();
        panelCalibration.add(allowMisdetectsTf, "8, 4, fill, default");
        allowMisdetectsTf.setColumns(3);

        lblOffsetThreshold = new JLabel(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.CalibrationPanel.OffsetThresholdLabel.text")); //$NON-NLS-1$
        panelCalibration.add(lblOffsetThreshold, "2, 6, right, default");

        offsetThresholdTf = new JTextField();
        panelCalibration.add(offsetThresholdTf, "4, 6, fill, default");
        offsetThresholdTf.setColumns(10);

        lblCalibrationZOffset = new JLabel(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.CalibrationPanel.CalibrationZOffsetLabel.text")); //$NON-NLS-1$
        lblCalibrationZOffset.setToolTipText(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.CalibrationPanel.CalibrationZOffsetLabel.toolTipText")); //$NON-NLS-1$
        panelCalibration.add(lblCalibrationZOffset, "6, 6, right, default");

        calibrationZOffsetTf = new JTextField();
        panelCalibration.add(calibrationZOffsetTf, "8, 6, fill, default");
        calibrationZOffsetTf.setColumns(10);

        lblNozzleTipDiameter = new JLabel(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.CalibrationPanel.VisionDiameterLabel.text")); //$NON-NLS-1$
        lblNozzleTipDiameter.setToolTipText(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.CalibrationPanel.VisionDiameterLabel.toolTipText")); //$NON-NLS-1$
        panelCalibration.add(lblNozzleTipDiameter, "2, 8, right, default");

        calibrationTipDiameter = new JTextField();
        panelCalibration.add(calibrationTipDiameter, "4, 8, fill, default");
        calibrationTipDiameter.setColumns(10);

        lblNewLabel = new JLabel(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.CalibrationPanel.PipelineLabel.text")); //$NON-NLS-1$
        panelCalibration.add(lblNewLabel, "2, 12, right, default");

        btnEditPipeline = new JButton(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.CalibrationPanel.EditPipelineButton.text")); //$NON-NLS-1$
        panelCalibration.add(btnEditPipeline, "4, 12");

        btnResetPipeline = new JButton(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.CalibrationPanel.ResetPipelineButton.text")); //$NON-NLS-1$
        panelCalibration.add(btnResetPipeline, "6, 12");
        btnResetPipeline.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetCalibrationPipeline();
            }
        });
        btnEditPipeline.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                UiUtils.messageBoxOnException(() -> {
                    editCalibrationPipeline();
                });
            }
        });

        nozzleTip.getCalibration().addPropertyChangeListener("calibrationInformation", e -> {
            firePropertyChange("calibrationStatus", null, null);;
        });
        MainFrame.get().getMachineControls().addPropertyChangeListener("selectedTool", e -> {
            firePropertyChange("calibrationStatus", null, null);;
        });
        for (Head head : Configuration.get().getMachine().getHeads()) {
            for (Nozzle nozzle : head.getNozzles()) {
                if (nozzle instanceof ReferenceNozzle) {
                    ReferenceNozzle refNozzle = (ReferenceNozzle)nozzle;
                    refNozzle.addPropertyChangeListener("nozzleTip", e -> {
                        firePropertyChange("calibrationStatus", null, null);
                    });
                }
            }
        }

        panelBackground = new JPanel();
        panelBackground.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.BackgroundCalibrationPanel.Border.title" //$NON-NLS-1$
        ), TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelBackground);
        panelBackground.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),},
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
                        RowSpec.decode("max(50dlu;default)"),
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,}));

        lblMethod = new JLabel(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.BackgroundCalibrationPanel.MethodLabel.title")); //$NON-NLS-1$
        panelBackground.add(lblMethod, "2, 2, right, default");

        backgroundCalibrationMethod = new JComboBox(BackgroundCalibrationMethod.values());
        backgroundCalibrationMethod.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent arg0) {
                adaptDialog();
            }
        });
        panelBackground.add(backgroundCalibrationMethod, "4, 2, 3, 1, fill, default");

        lblBlowup = new JLabel(" ");
        panelBackground.add(lblBlowup, "10, 2");

        lblDetailSize = new JLabel(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.BackgroundCalibrationPanel.MinDetailSizeLabel.title")); //$NON-NLS-1$
        lblDetailSize.setToolTipText(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.BackgroundCalibrationPanel.MinDetailSizeLabel.toolTipText" //$NON-NLS-1$
        ));
        panelBackground.add(lblDetailSize, "2, 4, right, default");

        minimumDetailSize = new JTextField();
        panelBackground.add(minimumDetailSize, "4, 4, fill, default");
        minimumDetailSize.setColumns(10);

        lblMinimum = new JLabel(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.BackgroundCalibrationPanel.MinimumLabel.text")); //$NON-NLS-1$
        panelBackground.add(lblMinimum, "4, 8, center, default");

        lblMaximum = new JLabel(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.BackgroundCalibrationPanel.MaximumLabel.text")); //$NON-NLS-1$
        panelBackground.add(lblMaximum, "6, 8, center, default");

        lblTolerance = new JLabel(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.BackgroundCalibrationPanel.ToleranceLabel.text")); //$NON-NLS-1$
        panelBackground.add(lblTolerance, "8, 8, center, default");

        lblHue = new JLabel(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.BackgroundCalibrationPanel.HueLabel.text")); //$NON-NLS-1$
        lblHue.setToolTipText("Base Color, Hue in the HSV color model");
        panelBackground.add(lblHue, "2, 10, right, default");

        backgroundMinHue = new JTextField();
        backgroundMinHue.setEditable(false);
        panelBackground.add(backgroundMinHue, "4, 10, fill, default");
        backgroundMinHue.setColumns(10);

        backgroundMaxHue = new JTextField();
        backgroundMaxHue.setEditable(false);
        panelBackground.add(backgroundMaxHue, "6, 10, fill, default");
        backgroundMaxHue.setColumns(10);

        backgroundTolHue = new JTextField();
        panelBackground.add(backgroundTolHue, "8, 10, fill, default");
        backgroundTolHue.setColumns(10);

        lblSaturation = new JLabel(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.BackgroundCalibrationPanel.SaturationLabel.text")); //$NON-NLS-1$
        lblSaturation.setToolTipText(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.BackgroundCalibrationPanel.SaturationLabel.toolTipText")); //$NON-NLS-1$
        panelBackground.add(lblSaturation, "2, 12, right, default");

        backgroundMinSaturation = new JTextField();
        backgroundMinSaturation.setEditable(false);
        panelBackground.add(backgroundMinSaturation, "4, 12, fill, default");
        backgroundMinSaturation.setColumns(10);

        backgroundMaxSaturation = new JTextField();
        backgroundMaxSaturation.setEditable(false);
        panelBackground.add(backgroundMaxSaturation, "6, 12, fill, default");
        backgroundMaxSaturation.setColumns(10);

        backgroundTolSaturation = new JTextField();
        panelBackground.add(backgroundTolSaturation, "8, 12, fill, default");
        backgroundTolSaturation.setColumns(10);

        lblBrightness = new JLabel(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.BackgroundCalibrationPanel.ValueLabel.text")); //$NON-NLS-1$
        lblBrightness.setToolTipText(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.BackgroundCalibrationPanel.ValueLabel.toolTipText")); //$NON-NLS-1$
        panelBackground.add(lblBrightness, "2, 14, right, default");

        backgroundMinValue = new JTextField();
        backgroundMinValue.setEditable(false);
        panelBackground.add(backgroundMinValue, "4, 14, fill, default");
        backgroundMinValue.setColumns(10);

        backgroundMaxValue = new JTextField();
        backgroundMaxValue.setEditable(false);
        panelBackground.add(backgroundMaxValue, "6, 14, fill, default");
        backgroundMaxValue.setColumns(10);

        backgroundTolValue = new JTextField();
        panelBackground.add(backgroundTolValue, "8, 14, fill, default");
        backgroundTolValue.setColumns(10);

        hsvIndicator = new HsvIndicator();
        panelBackground.add(hsvIndicator, "4, 16, 3, 3");

        backgroundDiagnostics = new JLabel(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.BackgroundCalibrationPanel.BackgroundDiagnosticsLabel.text")); //$NON-NLS-1$
        panelBackground.add(backgroundDiagnostics, "8, 16, 3, 1");

        btnShowProblems = new JButton(Translations.getString(
                "ReferenceNozzleTipCalibrationWizard.BackgroundCalibrationPanel.ShowProblemsButton.text")); //$NON-NLS-1$
        btnShowProblems.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                showBackgroundProblems(nozzleTip, true);
            }
        });
        panelBackground.add(btnShowProblems, "8, 18, 3, 1");

        adaptDialog();
    }

    protected void adaptDialog() {
        ReferenceNozzleTipCalibration.RecalibrationTrigger recalibration = (RecalibrationTrigger) recalibrationCb.getSelectedItem();
        boolean calibratesOnHoming = (recalibration == ReferenceNozzleTipCalibration.RecalibrationTrigger.MachineHome
                || recalibration == ReferenceNozzleTipCalibration.RecalibrationTrigger.NozzleTipChange);
        lblFailHoming.setVisible(calibratesOnHoming);
        failHoming.setVisible(calibratesOnHoming);

        boolean enabled = calibrationEnabledCheckbox.isSelected();
        for (Component comp : panelTop.getComponents()) {
            if (comp != calibrationEnabledCheckbox) { 
                comp.setEnabled(enabled); 
            }
        }
        for (Component comp : panelCalibration.getComponents()) {
            comp.setEnabled(enabled); 
        }
        BackgroundCalibrationMethod method = (BackgroundCalibrationMethod) backgroundCalibrationMethod.getSelectedItem();
        for (Component comp : panelBackground.getComponents()) {
            if (comp == backgroundCalibrationMethod 
                    || comp == lblMethod) { 
                comp.setEnabled(enabled); 
            }
            else if (comp == backgroundMinValue || comp == backgroundMaxValue || comp == backgroundTolValue
                    || comp == minimumDetailSize
                    || comp == lblMaximum || comp == lblMinimum || comp == lblTolerance
                    || comp == lblBrightness
                    || comp == lblDetailSize
                    || comp == hsvIndicator
                    || comp == backgroundDiagnostics
                    || comp == btnShowProblems) {
                comp.setEnabled(enabled && (method != BackgroundCalibrationMethod.None)); 
            }
            else {
                comp.setEnabled(enabled && (method == BackgroundCalibrationMethod.BrightnessAndKeyColor)); 
            }
        }
    }

    @SuppressWarnings("serial")
    private Action positionToolAction = new AbstractAction(Translations.getString(
            "ReferenceNozzleTipCalibrationWizard.Action.PositionTool"), Icons.centerTool) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString(
                    "ReferenceNozzleTipCalibrationWizard.Action.PositionTool.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                HeadMountable nozzle = getUiCalibrationNozzle(nozzleTip);
                Camera camera = VisionUtils.getBottomVisionCamera();
                ReferenceNozzleTipCalibration calibration = nozzleTip.getCalibration();
                Location location = calibration.getCalibrationLocation(camera, nozzle);
                MovableUtils.moveToLocationAtSafeZ(nozzle, location);
                MovableUtils.fireTargetedUserAction(nozzle);
            });
        }
    };
    private JLabel lblNewLabel;
    private JLabel lblCalibrate;
    private JLabel lblAllowMisdectects;
    private JTextField allowMisdetectsTf;
    private JLabel lblCalibrationZOffset;
    private JTextField calibrationZOffsetTf;
    private JLabel lblRecalibration;
    private JComboBox recalibrationCb;
    private JButton btnCalibrateCamera;
    private JLabel lblNozzleTipDiameter;
    private JTextField calibrationTipDiameter;
    private JLabel lblFailHoming;
    private JCheckBox failHoming;
    private JLabel lblMethod;
    private JComboBox backgroundCalibrationMethod;
    private JLabel lblDetailSize;
    private JTextField minimumDetailSize;
    private JLabel lblBrightness;
    private JTextField backgroundMinValue;
    private JLabel lblHue;
    private JTextField backgroundMinHue;
    private JTextField backgroundMaxHue;
    private JLabel lblSaturation;
    private JTextField backgroundMinSaturation;
    private JTextField backgroundMaxSaturation;
    private JTextField backgroundMaxValue;
    private HsvIndicator hsvIndicator;
    private JPanel panel_2;
    private JLabel backgroundDiagnostics;
    private JLabel lblBlowup;
    private JButton btnShowProblems;

    private Timer timer;

    private int imageCount;
    private JLabel lblMinimum;
    private JLabel lblMaximum;
    private JLabel lblTolerance;
    private JTextField backgroundTolHue;
    private JTextField backgroundTolSaturation;
    private JTextField backgroundTolValue;

    public static ReferenceNozzle getUiCalibrationNozzle(ReferenceNozzleTip nozzleTip) throws Exception {
        ReferenceNozzle refNozzle; 
        if (nozzleTip.isUnloadedNozzleTipStandin()) {
            // For the "unloaded" stand-in it is not well-defined where it is currently "loaded"
            // if multiple nozzles are bare at the same time. Therefore the currently selected nozzle 
            // from the machine controls is preferred.
            Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
            if (nozzle instanceof ReferenceNozzle) {
                refNozzle = (ReferenceNozzle)nozzle;
                if (refNozzle.getCalibrationNozzleTip() == nozzleTip) {
                    // Yes, it's a match.
                    return refNozzle; 
                }
            }
            throw new Exception("Please unload the nozzle tip on the selected nozzle.");
        }
        else {
            // For real nozzle tips, the nozzle where it is currently attached to is well-defined.
            refNozzle = nozzleTip.getNozzleWhereLoaded();
            if (refNozzle == null) {
                throw new Exception("Please load the nozzle tip on a nozzle.");
            }
            return refNozzle;
        }
    }

    public String getCalibrationStatus() {
        StringBuffer info = new StringBuffer();
        ReferenceNozzle nozzle = null;
        try {
            nozzle = getUiCalibrationNozzle(nozzleTip);
        }
        catch (Exception e) {
            info.append(e.getMessage());
            nozzle = null;
        }
        if (nozzle != null) {
            info.append(nozzleTip.getName());
            info.append(" on ");
            info.append(nozzle.getName());
            info.append(": ");
            if (nozzle.isCalibrated()) {
                info.append(nozzleTip.getCalibration().getCalibrationInformation(nozzle));
            }
            else {
                info.append("Uncalibrated");
            }
        }
        return info.toString();
    }

    private void resetCalibrationPipeline() {
        nozzleTip.getCalibration()
        .resetPipeline();
    }

    private void editCalibrationPipeline() throws Exception {
        Camera camera = VisionUtils.getBottomVisionCamera();
        ReferenceNozzleTipCalibration calibration = nozzleTip.getCalibration();
        // Use the current Nozzle location as the nominal detection location, this allows testing off-center detection.
        ReferenceNozzle nozzle = getUiCalibrationNozzle(nozzleTip);
        Location location = nozzle.getLocation();
        Location distance = location.subtract(camera.getLocation());
        if (Math.abs(distance.getLengthX().divide(camera.getUnitsPerPixelAtZ().getLengthX())) >= camera.getWidth()/2
                || Math.abs(distance.getLengthY().divide(camera.getUnitsPerPixelAtZ().getLengthY())) >= camera.getHeight()/2
                || Math.abs(distance.getLengthZ().convertToUnits(LengthUnit.Millimeters).getValue()) >= 0.1) {
            // Outside the camera view, or Z not set, need to move to the center.
            location = calibration.getCalibrationLocation(camera, nozzle);
        }

        final Location moveToLocation = location;
        UiUtils.confirmMoveToLocationAndAct(getTopLevelAncestor(), 
                "move nozzle "+nozzle.getName()+" to the camera center location before editing the pipeline", 
                nozzle, 
                moveToLocation, true, () -> {
                    CvPipeline pipeline = calibration
                            .getPipeline(camera, nozzle, moveToLocation);
                    CvPipelineEditor editor = new CvPipelineEditor(pipeline);
                    JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), "Calibration Pipeline", editor);
                    dialog.setVisible(true);
                });
    }

    private void calibrate() {
        applyAction.actionPerformed(null);
        UiUtils.submitUiMachineTask(() -> {
            nozzleTip.getCalibration()
            .calibrate(getUiCalibrationNozzle(nozzleTip));
        });
    }

    private void calibrateCamera() {
        applyAction.actionPerformed(null);
        UiUtils.submitUiMachineTask(() -> {
            nozzleTip.getCalibration()
            .calibrateCamera(getUiCalibrationNozzle(nozzleTip));
        });
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        LengthConverter lengthConverter = new LengthConverter();

        addWrappedBinding(nozzleTip.getCalibration(), "enabled", calibrationEnabledCheckbox,
                "selected");
        addWrappedBinding(nozzleTip.getCalibration(), "failHoming", failHoming,
                "selected");
        addWrappedBinding(nozzleTip.getCalibration(), "angleSubdivisions", angleIncrementsTf,
                "text", intConverter);
        addWrappedBinding(nozzleTip.getCalibration(), "allowMisdetections", allowMisdetectsTf,
                "text", intConverter);
        addWrappedBinding(nozzleTip.getCalibration(), "offsetThresholdLength", offsetThresholdTf,
                "text", lengthConverter);
        addWrappedBinding(nozzleTip.getCalibration(), "calibrationZOffset", calibrationZOffsetTf,
                "text", lengthConverter);
        addWrappedBinding(nozzleTip.getCalibration(), "calibrationTipDiameter", calibrationTipDiameter,
                "text", lengthConverter);

        addWrappedBinding(nozzleTip.getCalibration(), "recalibrationTrigger",
                recalibrationCb, "selectedItem");

        bind(UpdateStrategy.READ, this, "calibrationStatus", calibrationStatus, "text");

        addWrappedBinding(nozzleTip.getCalibration(), "backgroundCalibrationMethod",
                backgroundCalibrationMethod, "selectedItem");

        addWrappedBinding(nozzleTip.getCalibration(), "minimumDetailSize", minimumDetailSize,
                "text", lengthConverter);

        addWrappedBinding(nozzleTip.getCalibration(), "backgroundMinValue", backgroundMinValue,
                "text", intConverter);
        addWrappedBinding(nozzleTip.getCalibration(), "backgroundMaxValue", backgroundMaxValue,
                "text", intConverter);
        addWrappedBinding(nozzleTip.getCalibration(), "backgroundTolValue", backgroundTolValue,
                "text", intConverter);
        addWrappedBinding(nozzleTip.getCalibration(), "backgroundMinSaturation", backgroundMinSaturation,
                "text", intConverter);
        addWrappedBinding(nozzleTip.getCalibration(), "backgroundMaxSaturation", backgroundMaxSaturation,
                "text", intConverter);
        addWrappedBinding(nozzleTip.getCalibration(), "backgroundTolSaturation", backgroundTolSaturation,
                "text", intConverter);
        addWrappedBinding(nozzleTip.getCalibration(), "backgroundMinHue", backgroundMinHue,
                "text", intConverter);
        addWrappedBinding(nozzleTip.getCalibration(), "backgroundMaxHue", backgroundMaxHue,
                "text", intConverter);
        addWrappedBinding(nozzleTip.getCalibration(), "backgroundTolHue", backgroundTolHue,
                "text", intConverter);

        bind(UpdateStrategy.READ, backgroundMinHue, "text", hsvIndicator, "minHue");
        bind(UpdateStrategy.READ, backgroundMaxHue, "text", hsvIndicator, "maxHue");
        bind(UpdateStrategy.READ, backgroundMinSaturation, "text", hsvIndicator, "minSaturation");
        bind(UpdateStrategy.READ, backgroundMaxSaturation, "text", hsvIndicator, "maxSaturation");
        bind(UpdateStrategy.READ, backgroundMinValue, "text", hsvIndicator, "minValue");
        bind(UpdateStrategy.READ, backgroundMaxValue, "text", hsvIndicator, "maxValue");

        bind(UpdateStrategy.READ, nozzleTip.getCalibration(), "backgroundDiagnostics", backgroundDiagnostics, "text");

        ComponentDecorators.decorateWithAutoSelect(backgroundMinValue);
        ComponentDecorators.decorateWithAutoSelect(backgroundMaxValue);
        ComponentDecorators.decorateWithAutoSelect(backgroundTolValue);
        ComponentDecorators.decorateWithAutoSelect(backgroundMinSaturation);
        ComponentDecorators.decorateWithAutoSelect(backgroundMaxSaturation);
        ComponentDecorators.decorateWithAutoSelect(backgroundTolSaturation);
        ComponentDecorators.decorateWithAutoSelect(backgroundMinHue);
        ComponentDecorators.decorateWithAutoSelect(backgroundMaxHue);
        ComponentDecorators.decorateWithAutoSelect(backgroundTolHue);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(offsetThresholdTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(calibrationZOffsetTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(calibrationTipDiameter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(minimumDetailSize);

        try {
            Camera camera = VisionUtils.getBottomVisionCamera();
            if (camera instanceof ReferenceCamera) {
                if (((ReferenceCamera) camera).getAdvancedCalibration().isOverridingOldTransformsAndDistortionCorrectionSettings()) {
                    btnCalibrateCamera.setVisible(false);
                }
            }
        }
        catch (Exception e1) {
            Logger.warn(e1);
        }
    }

    public static void showBackgroundProblems(ReferenceNozzleTip nozzleTip, boolean noProblems) {
        UiUtils.messageBoxOnException(() -> {
            BufferedImage[] backgroundCalibrationImages = nozzleTip.getCalibration().getBackgroundCalibrationImages();
            if (backgroundCalibrationImages == null) {
                throw new Exception("No background calibration recorded for "+nozzleTip.getName());
            }
            int n = backgroundCalibrationImages.length;
            if (n == 0) {
                if (noProblems) {
                    throw new Exception("Background calibration for "+nozzleTip.getName()+" does not indicate any problems.");
                }
                else {
                    return;
                }
            }
            Camera camera = VisionUtils.getBottomVisionCamera();
            camera.ensureCameraVisible();
            CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(camera);
            String [] texts = new String[n];
            for (int i = 0; i < n; i++) {
                texts[i] = "Background calibration problems: Frame "+(i/2 + 1)+"/"+(n/2);
            }
            cameraView.showFilteredImages(backgroundCalibrationImages, 
                    texts, 1000);
        });
    }
}
