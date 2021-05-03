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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
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
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.ReferenceNozzleTip.VisionCalibration;
import org.openpnp.machine.reference.ReferenceNozzleTip.VisionCalibrationTrigger;
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

    private Set<org.openpnp.model.Package> compatiblePackages = new HashSet<>();
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
    private JPanel panel;
    private JComboBox tcPostOneComboBoxActuator;
    private JComboBox tcPostTwoComboBoxActuator;
    private JComboBox tcPostThreeComboBoxActuator;
    private JComboBox copyFromNozzleTip;
    private JLabel lblCopyFrom;
    private JButton btnNewButton;

    private ArrayList<ReferenceNozzleTip> nozzleTips;
    public ReferenceNozzleTipToolChangerWizard(ReferenceNozzleTip nozzleTip) {
        this.nozzleTip = nozzleTip;

        panelChanger = new JPanel();
        panelChanger.setBorder(new TitledBorder(null, "Nozzle Tip Changer", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        
        label = new JLabel("Post 1 Actuator");
        panelChanger.add(label, "2, 5, right, center");
        label = new JLabel("Post 2 Actuator");
        panelChanger.add(label, "2, 7, right, center");
        label = new JLabel("Post 3 Actuator");
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
        panelChanger.add(lblX, "4, 2");

        lblY_1 = new JLabel("Y");
        panelChanger.add(lblY_1, "6, 2");

        lblZ_1 = new JLabel("Z");
        panelChanger.add(lblZ_1, "8, 2");
        
        lblSpeed = new JLabel("Speed");
        panelChanger.add(lblSpeed, "10, 2");
        
        lblSpeed1_2 = new JLabel("1 ↔ 2");
        panelChanger.add(lblSpeed1_2, "8, 5, right, default");
        
        lblSpeed2_3 = new JLabel("2 ↔ 3");
        panelChanger.add(lblSpeed2_3, "8, 7, right, default");
        
        lblSpeed3_4 = new JLabel("3 ↔ 4");
        panelChanger.add(lblSpeed3_4, "8, 9, right, default");
        
        lblStartLocation = new JLabel("First Location");
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
        textFieldChangerStartToMidSpeed.setToolTipText("Speed between First location and Second location");
        panelChanger.add(textFieldChangerStartToMidSpeed, "10, 5, fill, default");
        textFieldChangerStartToMidSpeed.setColumns(8);

        changerStartLocationButtonsPanel = new LocationButtonsPanel(textFieldChangerStartX,
                textFieldChangerStartY, textFieldChangerStartZ, (JTextField) null);
        changerStartLocationButtonsPanel.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerStartLocationButtonsPanel, "12, 4, fill, default");

        lblMiddleLocation = new JLabel("Second Location");
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
        textFieldChangerMidToMid2Speed.setToolTipText("Speed between Second location and Third location");
        textFieldChangerMidToMid2Speed.setColumns(8);
        panelChanger.add(textFieldChangerMidToMid2Speed, "10, 7, fill, default");

        changerMidLocationButtonsPanel = new LocationButtonsPanel(textFieldChangerMidX,
                textFieldChangerMidY, textFieldChangerMidZ, (JTextField) null);
        changerMidLocationButtonsPanel.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerMidLocationButtonsPanel, "12, 6, fill, default");
        
        lblMiddleLocation_1 = new JLabel("Third Location");
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
        textFieldChangerMid2ToEndSpeed.setToolTipText("Speed between Third location and Last location");
        textFieldChangerMid2ToEndSpeed.setColumns(8);
        panelChanger.add(textFieldChangerMid2ToEndSpeed, "10, 9, fill, default");
        
        changerMidButtons2 = new LocationButtonsPanel(textFieldMidX2, textFieldMidY2, textFieldMidZ2, (JTextField) null);
        changerMidButtons2.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerMidButtons2, "12, 8, fill, default");

        lblEndLocation = new JLabel("Last Location");
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

        lblCopyFrom = new JLabel("Template");
        panelChanger.add(lblCopyFrom, "2, 16, right, default");

        nozzleTips = new ArrayList<>();
        copyFromNozzleTip = new JComboBox();
        if (myMachine != null) {
            for (NozzleTip nt : myMachine.getNozzleTips()) {
                if (nt instanceof ReferenceNozzleTip 
                        && nt != nozzleTip) {
                    nozzleTips.add((ReferenceNozzleTip) nt);
                    copyFromNozzleTip.addItem(nt.getName());
                }
            }
        }
        panelChanger.add(copyFromNozzleTip, "4, 16, 7, 1, fill, default");

        btnNewButton = new JButton(cloneFromNozzleTipAction);
        panelChanger.add(btnNewButton, "12, 16");
        if (nozzleTips.size() == 0) {
            copyFromNozzleTip.setEnabled(false);
            btnNewButton.setEnabled(false);
        }
        
        panelVision = new JPanel();
        panelVision.setBorder(new TitledBorder(null, "Vision Calibration", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
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
        
        lblVisionCalibration = new JLabel("Vision Location");
        lblVisionCalibration.setToolTipText("<html>Location for vision calibration, or None for no calibration.<br/>\r\nChoose a location where the nozzle tip in the slot is visible. \r\n</html>");
        panelVision.add(lblVisionCalibration, "2, 2, right, default");
        
        visionCalibration = new JComboBox(VisionCalibration.values());
        visionCalibration.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        panelVision.add(visionCalibration, "4, 2, 3, 1");
        
        lblVisionCalibrationHelp = new JLabel("<html>\r\nCapture two template images of your nozzle tip changer slot<br/>\r\nboth in empty and occupied state.<br/>\r\nUsing the templates, vision calibration will then calibrate the<br/>\r\nchanger locations in X/Y and also make sure the slot is empty<br/>\r\nor occupied as expected.\r\n</html>");
        panelVision.add(lblVisionCalibrationHelp, "10, 2, 1, 10, default, top");
        
        lblCalibrationTrigger = new JLabel("Calibration Trigger");
        panelVision.add(lblCalibrationTrigger, "2, 5, right, default");
        
        visionCalibrationTrigger = new JComboBox(VisionCalibrationTrigger.values());
        panelVision.add(visionCalibrationTrigger, "4, 5, 3, 1, fill, default");
        
        lblTemplateDimX = new JLabel("Template Width");
        lblTemplateDimX.setToolTipText("<html>\r\nTemplate image width (X). The template is centered around the selected Vision Location.<br/>\r\nChoose dimensions as small as possible, but the templates should include tell-tale horizontal and vertical<br/>\r\nedges. Furthermore, the nozzle tip should be visible when it occupies the changer slot.\r\n</html>");
        panelVision.add(lblTemplateDimX, "2, 7, right, default");
        
        visionTemplateDimensionX = new JTextField();
        panelVision.add(visionTemplateDimensionX, "4, 7");
        visionTemplateDimensionX.setColumns(10);
        
        lblTemplateDimY = new JLabel("Template Height");
        lblTemplateDimY.setToolTipText("<html>\r\nTemplate image width (X). The template is centered around the selected Vision Location.<br/>\r\nChoose dimensions as small as possible, but the templates should include tell-tale horizontal and vertical<br/>\r\nedges. Furthermore, the nozzle tip should be visible when it occupies the changer slot.\r\n</html>");
        panelVision.add(lblTemplateDimY, "6, 7, right, default");
        
        visionTemplateDimensionY = new JTextField();
        panelVision.add(visionTemplateDimensionY, "8, 7");
        visionTemplateDimensionY.setColumns(10);
        
        lblTolerance = new JLabel("Tolerance");
        lblTolerance.setToolTipText("Maximum calibration tolerance i.e. how far away from the nominal location the calibrated location can be.");
        panelVision.add(lblTolerance, "2, 9, right, default");
        
        visionTemplateTolerance = new JTextField();
        panelVision.add(visionTemplateTolerance, "4, 9, fill, default");
        visionTemplateTolerance.setColumns(10);
        
        lblPrecision = new JLabel("Wanted Precision");
        lblPrecision.setToolTipText("<html>If the detected template image match is further away than the <strong>Wanted Precision</strong>,<br/>\r\nthe camera is re-centered and another vision pass is made.</html>");
        panelVision.add(lblPrecision, "6, 9, right, default");
        
        visionCalibrationTolerance = new JTextField();
        panelVision.add(visionCalibrationTolerance, "8, 9, fill, default");
        visionCalibrationTolerance.setColumns(10);
        
        lblMaxPasses = new JLabel("Max. Passes");
        panelVision.add(lblMaxPasses, "2, 11, right, default");
        
        visionCalibrationMaxPasses = new JTextField();
        panelVision.add(visionCalibrationMaxPasses, "4, 11, fill, default");
        visionCalibrationMaxPasses.setColumns(10);
        
        lblMinScore = new JLabel("Minimum Score");
        lblMinScore.setToolTipText("<html>\r\nWhen the template images are matched against the camera image, a score is computed<br/>\r\nindicating the quality of the match. If the obtained score is smaller than the Minimum Score<br/>\r\ngiven here, the calibration fails. This should stop the machine from atempting a nozzle tip <br/>\r\nchange, when the position is wrong. \r\n</html>");
        panelVision.add(lblMinScore, "2, 13, right, default");
        
        visionMatchMinimumScore = new JTextField();
        panelVision.add(visionMatchMinimumScore, "4, 13, fill, default");
        visionMatchMinimumScore.setColumns(10);
        
        lblLastScore = new JLabel("Last Score");
        panelVision.add(lblLastScore, "6, 13, right, default");
        
        visionMatchLastScore = new JTextField();
        visionMatchLastScore.setEditable(false);
        panelVision.add(visionMatchLastScore, "8, 13, fill, default");
        visionMatchLastScore.setColumns(10);
        
        btnTest = new JButton(visionCalibrateTestAction);
        panelVision.add(btnTest, "10, 13");
        
        lblTemplateEmpty = new JLabel("Template Empty");
        panelVision.add(lblTemplateEmpty, "2, 17, right, top");
        
        btnCaptureEmpty = new JButton(captureTemplateImageEmptyAction);
        panelVision.add(btnCaptureEmpty, "4, 17, default, top");
        
        btnResetEmpty = new JButton(resetTemplateImageEmptyAction);
        panelVision.add(btnResetEmpty, "6, 17, default, top");
       
        visionTemplateImageEmpty = new TemplateImageControl();
        visionTemplateImageEmpty.setName("Empty");
        visionTemplateImageEmpty.setCamera(getCamera());
        panelVision.add(visionTemplateImageEmpty, "8, 17, 3, 1");
        
        lblTemplateOccupied = new JLabel("Template Occupied");
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

        adaptDialog();
    }

    private void adaptDialog() {
        boolean visionCalib = visionCalibration.getSelectedItem() != VisionCalibration.None;
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
        
        // bert start
        addWrappedBinding(nozzleTip, "changerActuatorPostStepOne", tcPostOneComboBoxActuator, "selectedItem");
        addWrappedBinding(nozzleTip, "changerActuatorPostStepTwo", tcPostTwoComboBoxActuator, "selectedItem");
        addWrappedBinding(nozzleTip, "changerActuatorPostStepThree", tcPostThreeComboBoxActuator, "selectedItem");

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
        Camera camera = getCamera();
        if (camera == null) {
            throw new Exception("No down-looking camera found.");
        }
        MovableUtils.moveToLocationAtSafeZ(camera, location);
        BufferedImage image = camera.lightSettleAndCapture();
        Location upp = camera.getUnitsPerPixel();
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

    private Action cloneFromNozzleTipAction = new AbstractAction("Clone Tool Changer Setting", Icons.importt) {
        {
            putValue(Action.SHORT_DESCRIPTION, "<html>Clone the Tool Changer settings from the selected Template nozzle tip,<br/>"
                    +"but offset the locations relative to the First Location.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                applyAction.actionPerformed(e);
                String name = (String)copyFromNozzleTip.getSelectedItem();
                ReferenceNozzleTip templateNozzleTip= null;
                for (ReferenceNozzleTip nt : nozzleTips) {
                    if (name.equals(nt.getName())) {
                        templateNozzleTip = nt;
                    }
                }
                if (templateNozzleTip != null) {
                    Location offsets = nozzleTip.getChangerStartLocation().subtract(templateNozzleTip.getChangerStartLocation());
                    nozzleTip.setChangerMidLocation(templateNozzleTip.getChangerMidLocation().add(offsets));
                    nozzleTip.setChangerMidLocation2(templateNozzleTip.getChangerMidLocation2().add(offsets));
                    nozzleTip.setChangerEndLocation(templateNozzleTip.getChangerEndLocation().add(offsets));
                    nozzleTip.setChangerActuatorPostStepOne(templateNozzleTip.getChangerActuatorPostStepOne());
                    nozzleTip.setChangerActuatorPostStepTwo(templateNozzleTip.getChangerActuatorPostStepTwo());
                    nozzleTip.setChangerActuatorPostStepThree(templateNozzleTip.getChangerActuatorPostStepThree());
                    nozzleTip.setChangerStartToMidSpeed(templateNozzleTip.getChangerStartToMidSpeed());
                    nozzleTip.setChangerMidToMid2Speed(templateNozzleTip.getChangerMidToMid2Speed());
                    nozzleTip.setChangerMid2ToEndSpeed(templateNozzleTip.getChangerMid2ToEndSpeed());
                }
            });
        }
    };

    private Action captureTemplateImageEmptyAction = new AbstractAction("Capture") {
        {
            putValue(Action.SHORT_DESCRIPTION, 
                    "<html>Capture the template image for the empty nozzle tip holder slot.</html>");
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

    private Action resetTemplateImageEmptyAction = new AbstractAction("Reset") {
        {
            putValue(Action.SHORT_DESCRIPTION, 
                    "<html>Reset the template image for the empty nozzle tip holder slot.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            nozzleTip.setVisionTemplateImageEmpty(null);
        }
    };

    private Action captureTemplateImageOccupiedAction = new AbstractAction("Capture") {
        {
            putValue(Action.SHORT_DESCRIPTION, 
                    "<html>Capture the template image for the occupied nozzle tip holder slot.</html>");
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

    private Action resetTemplateImageOccupiedAction = new AbstractAction("Reset") {
        {
            putValue(Action.SHORT_DESCRIPTION, 
                    "<html>Reset the template image for the occupied nozzle tip holder slot.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            nozzleTip.setVisionTemplateImageOccupied(null);
        }
    };
    private Action visionCalibrateTestAction = new AbstractAction("Test") {
        {
            putValue(Action.SHORT_DESCRIPTION, 
                    "<html>Test the vision calibration.</html>");
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

    private JPanel panelVision;
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
}
