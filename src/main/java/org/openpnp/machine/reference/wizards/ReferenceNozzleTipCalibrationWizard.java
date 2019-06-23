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

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.ReferenceNozzleTipCalibration;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import org.openpnp.vision.pipeline.ui.CvPipelineEditorDialog;

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
    private JLabel lblCalibrationStatus;
    private JCheckBox calibrationEnabledCheckbox;


    public ReferenceNozzleTipCalibrationWizard(ReferenceNozzleTip nozzleTip) {
        this.nozzleTip = nozzleTip;

        CellConstraints cc = new CellConstraints();


        panelCalibration = new JPanel();
        panelCalibration.setBorder(new TitledBorder(null, "Calibration (EXPERIMENTAL!)",
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelCalibration);
        panelCalibration.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(72dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        calibrationEnabledCheckbox = new JCheckBox("Enable?");
        panelCalibration.add(calibrationEnabledCheckbox, "2, 2, right, default");


        buttonCenterTool = new JButton(positionToolAction);
        buttonCenterTool.setHideActionText(true);
        panelCalibration.add(buttonCenterTool, "4, 2, left, default");


        lblCalibrationInfo = new JLabel("Status");
        panelCalibration.add(lblCalibrationInfo, "2, 4, right, default");

        lblCalibrationStatus = new JLabel(getCalibrationStatus());
        panelCalibration.add(lblCalibrationStatus, "4, 4, 3, 1, left, default");

        lblCalibrate = new JLabel("Calibration");
        panelCalibration.add(lblCalibrate, "2, 6, right, default");

        panel_1 = new JPanel();
        panel_1.setBorder(null);
        FlowLayout flowLayout_1 = (FlowLayout) panel_1.getLayout();
        flowLayout_1.setHgap(0);
        flowLayout_1.setVgap(0);
        panelCalibration.add(panel_1, "4, 6, left, fill");

        btnCalibrate = new JButton("Calibrate");
        panel_1.add(btnCalibrate);


        btnReset = new JButton("Reset");
        panel_1.add(btnReset);
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



        btnCalibrateCamera = new JButton("Calibrate Camera Position and Rotation");
        btnCalibrateCamera.setToolTipText("<html>\r\nCalibrate the bottom vision camera position and rotation <br />\r\naccording to a pattern of measured nozzle positions.\r\n</html>");
        panelCalibration.add(btnCalibrateCamera, "6, 6, 3, 1");
        btnCalibrateCamera.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                calibrateCamera();
            }
        });

        lblAngleIncrements = new JLabel("Circle Divisions");
        panelCalibration.add(lblAngleIncrements, "2, 8, right, default");

        angleIncrementsTf = new JTextField();
        panelCalibration.add(angleIncrementsTf, "4, 8, left, default");
        angleIncrementsTf.setColumns(3);

        lblAllowMisdectects = new JLabel("Allowed Misdectects");
        lblAllowMisdectects.setToolTipText("Number of missed detections tolerated before a calibration fails.");
        panelCalibration.add(lblAllowMisdectects, "6, 8, right, default");

        allowMisdetectsTf = new JTextField();
        panelCalibration.add(allowMisdetectsTf, "8, 8, left, default");
        allowMisdetectsTf.setColumns(3);

        lblOffsetThreshold = new JLabel("Offset Threshold");
        panelCalibration.add(lblOffsetThreshold, "2, 10, right, default");

        offsetThresholdTf = new JTextField();
        panelCalibration.add(offsetThresholdTf, "4, 10, left, default");
        offsetThresholdTf.setColumns(6);

        lblCalibrationZOffset = new JLabel("Calibration Z Offset");
        lblCalibrationZOffset.setToolTipText("<html>\r\n<p>\r\nWhen the vision-detected feature of a nozzle is higher up on the nozzle tip <br />\r\nit is recommended to shift the focus plane with the \"Z Offset\".\r\n</p>\r\n<p>If a nozzle tip is named \"unloaded\" it is used as a stand-in for calibration<br />\r\nof the bare nozzle tip holder. Again the \"Z Offset\" can be used to calibrate at the <br />\r\nproper focal plane. \r\n</p>\r\n</html>");
        panelCalibration.add(lblCalibrationZOffset, "6, 10, right, default");

        calibrationZOffsetTf = new JTextField();
        panelCalibration.add(calibrationZOffsetTf, "8, 10, left, default");
        calibrationZOffsetTf.setColumns(6);

        lblRecalibration = new JLabel("Automatic Recalibration");
        lblRecalibration.setToolTipText("<html>\r\n<p>Determines when a recalibration is automatically executed:</p>\r\n<p><ul><li>On each nozzle tip change.</li>\r\n<li>On each nozzle tip change but only in Jobs.</li>\r\n<li>On machine homing and when first loaded. </li></ul></p>\r\n<p>Manual with stored calibration (only recommended for machines <br /> \r\nwith C axis homing).</p>\r\n</html>");
        panelCalibration.add(lblRecalibration, "2, 12, right, default");

        recalibrationCb = new JComboBox(ReferenceNozzleTipCalibration.RecalibrationTrigger.values());
        panelCalibration.add(recalibrationCb, "4, 12, left, default");

        lblNewLabel = new JLabel("Pipeline");
        panelCalibration.add(lblNewLabel, "2, 14, right, default");

        panel = new JPanel();
        FlowLayout flowLayout = (FlowLayout) panel.getLayout();
        flowLayout.setVgap(0);
        panelCalibration.add(panel, "4, 14, left, default");

        btnEditPipeline = new JButton("Edit");
        panel.add(btnEditPipeline);

        btnResetPipeline = new JButton("Reset");
        panel.add(btnResetPipeline);
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

        initDataBindings();

    }

    @SuppressWarnings("serial")
    private Action positionToolAction = new AbstractAction("Position Tool", Icons.centerTool) {
        {
            putValue(Action.SHORT_DESCRIPTION, "Position the tool over the bottom camera.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                HeadMountable nozzle = getUiCalibrationNozzle();
                Camera camera = VisionUtils.getBottomVisionCamera();
                Location location = camera.getLocation(nozzle)
                        .add(new Location(nozzleTip.getCalibration().getCalibrationZOffset().getUnits(), 0, 0, 
                                nozzleTip.getCalibration().getCalibrationZOffset().getValue(), 0));

                MovableUtils.moveToLocationAtSafeZ(nozzle, location);
            });
        }
    };
    private JLabel lblNewLabel;
    private JLabel lblCalibrate;
    private JPanel panel;
    private JPanel panel_1;
    private JLabel lblAllowMisdectects;
    private JTextField allowMisdetectsTf;
    private JLabel lblCalibrationZOffset;
    private JTextField calibrationZOffsetTf;
    private JLabel lblRecalibration;
    private JComboBox recalibrationCb;
    private JButton btnCalibrateCamera;

    public ReferenceNozzle getUiCalibrationNozzle() throws Exception {
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
            refNozzle = nozzleTip.getNozzleAttachedTo();
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
            nozzle = getUiCalibrationNozzle();
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
        CvPipeline pipeline = nozzleTip.getCalibration()
                                       .getPipeline();
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), "Calibration Pipeline", editor);
        dialog.setVisible(true);
    }

    
    private void calibrate() {
        UiUtils.submitUiMachineTask(() -> {
            nozzleTip.getCalibration()
                .calibrate(getUiCalibrationNozzle());
        });
    }

    private void calibrateCamera() {
        UiUtils.submitUiMachineTask(() -> {
            nozzleTip.getCalibration()
                .calibrateCamera(getUiCalibrationNozzle());
        });
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        LengthConverter lengthConverter = new LengthConverter();
        
        addWrappedBinding(nozzleTip.getCalibration(), "enabled", calibrationEnabledCheckbox,
                "selected");
        addWrappedBinding(nozzleTip.getCalibration(), "angleSubdivisions", angleIncrementsTf,
                "text", intConverter);
        addWrappedBinding(nozzleTip.getCalibration(), "allowMisdetections", allowMisdetectsTf,
                "text", intConverter);
        addWrappedBinding(nozzleTip.getCalibration(), "offsetThresholdLength", offsetThresholdTf,
                "text", lengthConverter);
        addWrappedBinding(nozzleTip.getCalibration(), "calibrationZOffset", calibrationZOffsetTf,
                "text", lengthConverter);
        addWrappedBinding(nozzleTip.getCalibration(), "recalibrationTrigger",
                recalibrationCb, "selectedItem");
        
        bind(UpdateStrategy.READ, this, "calibrationStatus", lblCalibrationStatus, "text");
        
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(offsetThresholdTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(calibrationZOffsetTf);
    }
    protected void initDataBindings() {
        BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty = BeanProperty.create("selected");
        BeanProperty<JButton, Boolean> jButtonBeanProperty = BeanProperty.create("enabled");
        AutoBinding<JCheckBox, Boolean, JButton, Boolean> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, calibrationEnabledCheckbox, jCheckBoxBeanProperty, btnCalibrate, jButtonBeanProperty);
        autoBinding.bind();
        //
        AutoBinding<JCheckBox, Boolean, JButton, Boolean> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ, calibrationEnabledCheckbox, jCheckBoxBeanProperty, btnReset, jButtonBeanProperty);
        autoBinding_1.bind();
        //
        AutoBinding<JCheckBox, Boolean, JButton, Boolean> autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ, calibrationEnabledCheckbox, jCheckBoxBeanProperty, btnCalibrateCamera, jButtonBeanProperty);
        autoBinding_10.bind();
        //
        BeanProperty<JTextField, Boolean> jTextFieldBeanProperty = BeanProperty.create("enabled");
        AutoBinding<JCheckBox, Boolean, JTextField, Boolean> autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ, calibrationEnabledCheckbox, jCheckBoxBeanProperty, angleIncrementsTf, jTextFieldBeanProperty);
        autoBinding_3.bind();
        //
        AutoBinding<JCheckBox, Boolean, JTextField, Boolean> autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ, calibrationEnabledCheckbox, jCheckBoxBeanProperty, offsetThresholdTf, jTextFieldBeanProperty);
        autoBinding_6.bind();
        //
        AutoBinding<JCheckBox, Boolean, JTextField, Boolean> autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ, calibrationEnabledCheckbox, jCheckBoxBeanProperty, calibrationZOffsetTf, jTextFieldBeanProperty);
        autoBinding_7.bind();
        //
        AutoBinding<JCheckBox, Boolean, JTextField, Boolean> autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ, calibrationEnabledCheckbox, jCheckBoxBeanProperty, allowMisdetectsTf, jTextFieldBeanProperty);
        autoBinding_8.bind();
        //
        AutoBinding<JCheckBox, Boolean, JButton, Boolean> autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ, calibrationEnabledCheckbox, jCheckBoxBeanProperty, btnEditPipeline, jButtonBeanProperty);
        autoBinding_4.bind();
        //
        AutoBinding<JCheckBox, Boolean, JButton, Boolean> autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ, calibrationEnabledCheckbox, jCheckBoxBeanProperty, btnResetPipeline, jButtonBeanProperty);
        autoBinding_5.bind();
        //
        BeanProperty<JComboBox, Boolean> jComboBoxBeanProperty = BeanProperty.create("enabled");
        AutoBinding<JCheckBox, Boolean, JComboBox, Boolean> autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ, calibrationEnabledCheckbox, jCheckBoxBeanProperty, recalibrationCb, jComboBoxBeanProperty);
        autoBinding_9.bind();
    }
}
