/*
 * Copyright (C) 2021 Tony Luken <tonyluken62+openpnp@gmail.com>
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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.SimpleGraphView;
import org.openpnp.gui.components.VerticalLabel;
import org.openpnp.gui.processes.CalibrateCameraProcess;
import org.openpnp.gui.processes.CalibrateCameraProcess.CameraCalibrationProcessProperties;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.camera.ImageCamera;
import org.openpnp.machine.reference.camera.ReferenceCamera;
import org.openpnp.machine.reference.camera.calibration.AdvancedCalibration;
import org.openpnp.machine.reference.camera.calibration.CameraCalibrationUtils;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Machine;
import org.openpnp.util.SimpleGraph;
import org.openpnp.util.SimpleGraph.DataRow;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.ui.MatView;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceCameraCalibrationWizard extends AbstractConfigurationWizard {
    private final ReferenceCamera referenceCamera;
    private ReferenceHead referenceHead;
    private final boolean isMovable;
    private JPanel panelCameraCalibration;
    private JButton startCameraCalibrationBtn;
    private AdvancedCalibration advCal;
    private List<Location> calibrationLocations;
    private SimpleGraphView modelErrorsTimeSequenceView;
    private SimpleGraphView modelErrorsScatterPlotView;
    private VerticalLabel lblNewLabel_11;
    private JLabel lblNewLabel_12;
    private VerticalLabel lblNewLabel_13;
    private JLabel lblNewLabel_16;
    private int heightIndex = 0;
    private List<LengthCellValue> calibrationHeightSelections;
    private SpinnerListModel spinnerModel;
    private LengthUnit displayUnits;
    private LengthUnit smallDisplayUnits;
    private Location secondaryLocation;
    private double primaryDiameter;
    private double secondaryDiameter;
    private Machine machine;
    private CameraCalibrationProcessProperties props;


    /**
     * @return the referenceCamera
     */
    public ReferenceCamera getReferenceCamera() {
        return referenceCamera;
    }

    /**
     * @return the calibrationLocations
     */
    public List<Location> getCalibrationLocations() {
        return calibrationLocations;
    }

    /**
     * @param calibrationHeights the calibrationHeights to set
     */
    public void setCalibrationLocations(List<Location> calibrationLocations) {
        List<Location> oldValue = this.calibrationLocations;
        this.calibrationLocations = calibrationLocations;
        firePropertyChange("calibrationLocations", oldValue, calibrationLocations);
    }

    public List<LengthCellValue> getCalibrationHeightSelections() {
        return calibrationHeightSelections;
    }

    public void setCalibrationHeightSelections(List<LengthCellValue> calibrationHeightSelections) {
        this.calibrationHeightSelections = calibrationHeightSelections;
    }

    /**
     * @return the secondaryLocation
     */
    public Location getSecondaryLocation() {
        return secondaryLocation;
    }

    /**
     * @param secondaryLocation the secondaryLocation to set
     */
    public void setSecondaryLocation(Location secondaryLocation) {
        Location oldSetting = this.secondaryLocation;
        this.secondaryLocation = secondaryLocation;
        firePropertyChange("secondaryLocation", oldSetting, secondaryLocation);
    }

    public ReferenceCameraCalibrationWizard(ReferenceCamera referenceCamera) {
        this.referenceCamera = referenceCamera;
        
        machine = Configuration.get().getMachine();
        props = (CameraCalibrationProcessProperties) machine.getProperty("CameraCalibrationProcessProperties");
        
        if (props == null) {
            props = new CameraCalibrationProcessProperties();
            machine.setProperty("CameraCalibrationProcessProperties", props);
        }


        setName(referenceCamera.getName());
        referenceHead = (ReferenceHead) referenceCamera.getHead();
        isMovable = referenceHead != null;
        advCal = referenceCamera.getAdvancedCalibration();
        calibrationHeightSelections = new ArrayList<>();
        Location primaryLocation;
        primaryDiameter = Double.NaN;
        secondaryDiameter = Double.NaN;
        if (isMovable) {
            if (referenceCamera instanceof ImageCamera) {
                primaryLocation = ((ImageCamera) referenceCamera).getPrimaryFiducial();
                secondaryLocation = ((ImageCamera) referenceCamera).getSecondaryFiducial();
            }
            else {
                primaryLocation = referenceHead.getCalibrationPrimaryFiducialLocation();
                secondaryLocation = referenceHead.getCalibrationSecondaryFiducialLocation();
            }
            if (primaryLocation == null) {
                primaryLocation = new Location(LengthUnit.Millimeters, Double.NaN, Double.NaN, Double.NaN, 0);
            }
            if (secondaryLocation == null) {
                secondaryLocation = new Location(LengthUnit.Millimeters, Double.NaN, Double.NaN, Double.NaN, 0);
            }
            try {
                primaryDiameter = referenceHead.getCalibrationPrimaryFiducialDiameter().divide(
                        referenceCamera.getUnitsPerPixel(primaryLocation.getLengthZ()).getLengthX());
                secondaryDiameter = referenceHead.getCalibrationSecondaryFiducialDiameter().divide(
                        referenceCamera.getUnitsPerPixel(secondaryLocation.getLengthZ()).getLengthX());
            }
            catch (Exception e) {
                //Ok - will handle the NaNs later
            }
            if (referenceCamera.getDefaultZ() == null) {
                referenceCamera.setDefaultZ(primaryLocation.getLengthZ());
            }
        }
        else {
            primaryLocation = referenceCamera.getHeadOffsets();
            if (primaryLocation == null) {
                primaryLocation = new Location(LengthUnit.Millimeters, Double.NaN, Double.NaN, Double.NaN, 0);
            }
            if ((referenceCamera.getDefaultZ() == null) && Double.isFinite(primaryLocation.getZ())) {
                referenceCamera.setDefaultZ(primaryLocation.getLengthZ());
            }
            Length secondaryZ;
            if (advCal.getSavedTestPattern3dPointsList() != null && advCal.getSavedTestPattern3dPointsList().length >= 2) {
                secondaryZ = new Length(advCal.getSavedTestPattern3dPointsList()[1][0][2], LengthUnit.Millimeters);
            }
            else {
                secondaryZ = primaryLocation.getLengthZ().add(new Length(props.defaultUpLookingSecondaryOffsetZMm, LengthUnit.Millimeters));
            }
            secondaryLocation = primaryLocation.deriveLengths(null, null, secondaryZ, null);
            
            //Primary and secondary diameters can't be known until a nozzle tip is selected
        }
        advCal.setPrimaryLocation(primaryLocation);
        advCal.setSecondaryLocation(secondaryLocation);
        
        if (advCal.getSavedTestPattern3dPointsList() != null && advCal.getSavedTestPattern3dPointsList().length >= 2) {
            calibrationHeightSelections.add(new LengthCellValue(new Length(advCal.getSavedTestPattern3dPointsList()[0][0][2], LengthUnit.Millimeters)));
            calibrationHeightSelections.add(new LengthCellValue(new Length(advCal.getSavedTestPattern3dPointsList()[1][0][2], LengthUnit.Millimeters)));
        }
        else {
            calibrationHeightSelections.add(new LengthCellValue(primaryLocation.getLengthZ()));
            calibrationHeightSelections.add(new LengthCellValue(secondaryLocation.getLengthZ()));
        };
        
        displayUnits = Configuration.get().getSystemUnits();
        if (displayUnits.equals(LengthUnit.Inches)) {
            smallDisplayUnits = LengthUnit.Mils;
        }
        else {
            smallDisplayUnits = LengthUnit.Microns;
        }
            
        panelCameraCalibration = new JPanel();
        panelCameraCalibration.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelCameraCalibration);
        panelCameraCalibration.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(87dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(87dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(87dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(87dlu;default):grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(39dlu;default):grow"),},
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.UNRELATED_GAP_ROWSPEC,
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
                FormSpecs.UNRELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.UNRELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.UNRELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.UNRELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.UNRELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                RowSpec.decode("10dlu"),
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("max(114dlu;default)"),
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                RowSpec.decode("10dlu"),
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("max(178dlu;default)"),
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                RowSpec.decode("10dlu"),
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("max(151dlu;default)"),
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("default:grow"),}));
        
        chckbxAdvancedCalOverride = new JCheckBox(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.AdvancedCalOverrideChkbox.text")); //$NON-NLS-1$
        chckbxAdvancedCalOverride.setToolTipText(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.AdvancedCalOverrideChkbox.toolTipText")); //$NON-NLS-1$
        chckbxAdvancedCalOverride.addActionListener(overrideAction);
        panelCameraCalibration.add(chckbxAdvancedCalOverride, "2, 2, 11, 1");
        
        lblDescription = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.DescriptionLabel.text")); //$NON-NLS-1$
        panelCameraCalibration.add(lblDescription, "4, 4, 7, 1");
        
        separator_2 = new JSeparator();
        panelCameraCalibration.add(separator_2, "2, 6, 13, 1");
        
        lblNewLabel_31 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.GeneralSettingsLabel.text")); //$NON-NLS-1$
        lblNewLabel_31.setFont(new Font("Tahoma", Font.BOLD, 14));
        lblNewLabel_31.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_31, "2, 8, 7, 1");
        
        checkboxDeinterlace = new JCheckBox(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.DeInterlaceLabel.text")); //$NON-NLS-1$
        checkboxDeinterlace.setToolTipText(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.DeInterlaceLabel.toolTipText")); //$NON-NLS-1$
        panelCameraCalibration.add(checkboxDeinterlace, "4, 10");
        
        lblNewLabel_20 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.CroppedWidthLabel.text")); //$NON-NLS-1$
        panelCameraCalibration.add(lblNewLabel_20, "2, 12, right, default");
        
        textFieldCropWidth = new JTextField();
        panelCameraCalibration.add(textFieldCropWidth, "4, 12, fill, default");
        textFieldCropWidth.setColumns(10);
        
        lblNewLabel_22 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.SetZeroForNoCropped1Label.text")); //$NON-NLS-1$
        panelCameraCalibration.add(lblNewLabel_22, "6, 12");
        
        lblNewLabel_21 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.CroppedHeightLabel.text")); //$NON-NLS-1$
        panelCameraCalibration.add(lblNewLabel_21, "2, 14, right, default");
        
        textFieldCropHeight = new JTextField();
        panelCameraCalibration.add(textFieldCropHeight, "4, 14, fill, default");
        textFieldCropHeight.setColumns(10);
        
        lblNewLabel_23 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.SetZeroForNoCropped2Label.text")); //$NON-NLS-1$
        panelCameraCalibration.add(lblNewLabel_23, "6, 14");
        
        lblNewLabel_1 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.DefaultWorkingPlaneZLabel.text")); //$NON-NLS-1$
        panelCameraCalibration.add(lblNewLabel_1, "2, 16, right, default");
        
        textFieldDefaultZ = new JTextField();
        panelCameraCalibration.add(textFieldDefaultZ, "4, 16, fill, default");
        textFieldDefaultZ.setColumns(10);
        
        separator = new JSeparator();
        panelCameraCalibration.add(separator, "2, 18, 13, 1");
        
        lblNewLabel_32 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.CalibrationSetupLabel.text")); //$NON-NLS-1$
        lblNewLabel_32.setFont(new Font("Tahoma", Font.BOLD, 14));
        lblNewLabel_32.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_32, "2, 20, 7, 1");
        
        if (isMovable) {
            textFieldDefaultZ.setToolTipText(Translations.getString(
                    "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.MovableDefaultZTextFiled.toolTipText")); //$NON-NLS-1$
        }
        else {
            textFieldDefaultZ.setToolTipText(Translations.getString(
                    "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.NotMovableDefaultZTextFiled.toolTipText" //$NON-NLS-1$
            ));
        }
        
        lblNewLabel_35 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.PrimaryCalibrationZLabel.text")); //$NON-NLS-1$
        lblNewLabel_35.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel_35, "2, 22, right, default");
        
        textFieldPrimaryCalZ = new JTextField();
        if (isMovable) {
            textFieldPrimaryCalZ.setToolTipText(Translations.getString(
                    "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.MovablePrimCalZTextField.toolTipText")); //$NON-NLS-1$
        }
        else {
            textFieldPrimaryCalZ.setToolTipText(Translations.getString(
                    "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.NotMovablePrimCalZTextField.toolTipText" //$NON-NLS-1$
            ));
        }
        textFieldPrimaryCalZ.setEnabled(false);
        panelCameraCalibration.add(textFieldPrimaryCalZ, "4, 22, fill, default");
        textFieldPrimaryCalZ.setColumns(10);
        
        if (!isMovable) {
            lblNewLabel_37 = new JLabel(Translations.getString(
                    "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.CautionLabel.text")); //$NON-NLS-1$
            panelCameraCalibration.add(lblNewLabel_37, "6, 22, 5, 3");
        }
        
        lblNewLabel_36 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.SecondaryCalibrationZLabel.text")); //$NON-NLS-1$
        lblNewLabel_36.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel_36, "2, 24, right, default");
        
        textFieldSecondaryCalZ = new JTextField();
        if (isMovable) {
            textFieldSecondaryCalZ.setToolTipText(Translations.getString(
                    "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.MovableSecondaryCalZTextField.toolTipText" //$NON-NLS-1$
            ));
        }
        else {
            textFieldSecondaryCalZ.setToolTipText(Translations.getString(
                    "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.NotMovableSecondaryCalZTextField.toolTipText" //$NON-NLS-1$
            ));
        }
        panelCameraCalibration.add(textFieldSecondaryCalZ, "4, 24, fill, default");
        textFieldSecondaryCalZ.setColumns(10);
        
        lblNewLabel_28 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.RadialLinesPerCalZLabel.text")); //$NON-NLS-1$
        lblNewLabel_28.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel_28, "2, 26, right, default");
        
        textFieldDesiredNumberOfRadialLines = new JTextField();
        textFieldDesiredNumberOfRadialLines.setToolTipText(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.NumberOfRadialLinesTextField.toolTipText" //$NON-NLS-1$
        ));
        panelCameraCalibration.add(textFieldDesiredNumberOfRadialLines, "4, 26, fill, default");
        textFieldDesiredNumberOfRadialLines.setColumns(10);
        
        startCameraCalibrationBtn = new JButton(startCalibration);
        startCameraCalibrationBtn.setForeground(Color.RED);
        startCameraCalibrationBtn.setText(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.StartCameraCalibrationButton.text")); //$NON-NLS-1$
        panelCameraCalibration.add(startCameraCalibrationBtn, "4, 30");
                
        chckbxUseSavedData = new JCheckBox(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.UseSavedDateChkBox.text")); //$NON-NLS-1$
        chckbxUseSavedData.setEnabled(advCal.isValid());
        chckbxUseSavedData.setToolTipText(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.UseSavedDateChkBox.toolTipText")); //$NON-NLS-1$
        panelCameraCalibration.add(chckbxUseSavedData, "6, 30, 5, 1");
        
        lblNewLabel_34 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.DetectionDiameterLabel.text")); //$NON-NLS-1$
        lblNewLabel_34.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel_34, "2, 32");
        
        spinnerDiameter = new JSpinner();
        spinnerDiameter.setToolTipText(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.DiameterSpinner.toolTipText")); //$NON-NLS-1$
        spinnerDiameter.setEnabled(false);
        panelCameraCalibration.add(spinnerDiameter, "4, 32");
        
        chckbxEnable = new JCheckBox(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.ApplyCalibrationChkBox.text")); //$NON-NLS-1$
        chckbxEnable.setToolTipText(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.ApplyCalibrationChkBox.toolTipText")); //$NON-NLS-1$
        chckbxEnable.setEnabled(advCal.isValid());
        chckbxEnable.addActionListener(enableAction);
        panelCameraCalibration.add(chckbxEnable, "2, 34");
        
        sliderAlpha = new JSlider();
        sliderAlpha.setMaximum(100);
        sliderAlpha.setValue(100);
        sliderAlpha.setMajorTickSpacing(10);
        sliderAlpha.setMinorTickSpacing(2);
        sliderAlpha.setPaintTicks(true);
        sliderAlpha.setPaintLabels(true);
        sliderAlpha.addChangeListener(sliderAlphaChanged);
        
        lblNewLabel_3 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.SliderLabel.text")); //$NON-NLS-1$
        panelCameraCalibration.add(lblNewLabel_3, "4, 36, 7, 1");
        
        lblNewLabel = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.CropInvalidPixelsLabel.text")); //$NON-NLS-1$
        lblNewLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel, "2, 38");
        panelCameraCalibration.add(sliderAlpha, "4, 38, 5, 1");
        
        lblNewLabel_38 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.ShowValidPixelsLabel.text")); //$NON-NLS-1$
        panelCameraCalibration.add(lblNewLabel_38, "10, 38");
        
        separator_1 = new JSeparator();
        panelCameraCalibration.add(separator_1, "2, 40, 13, 1");
        
        lblNewLabel_33 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.CalibrationResults.text")); //$NON-NLS-1$
        lblNewLabel_33.setHorizontalAlignment(SwingConstants.CENTER);
        lblNewLabel_33.setFont(new Font("Tahoma", Font.BOLD, 14));
        panelCameraCalibration.add(lblNewLabel_33, "2, 42, 7, 1");
        
        lblNewLabel_25 = new JLabel("X");
        panelCameraCalibration.add(lblNewLabel_25, "4, 44");
        
        lblNewLabel_26 = new JLabel("Y");
        panelCameraCalibration.add(lblNewLabel_26, "6, 44");
        
        lblNewLabel_27 = new JLabel("Z");
        panelCameraCalibration.add(lblNewLabel_27, "8, 44");
        
        String offsetOrPositionLabel;
        if (isMovable) {
            offsetOrPositionLabel = Translations.getString(
                    "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.CalibratedHeadOffsets.text"); //$NON-NLS-1$
        }
        else {
            offsetOrPositionLabel = Translations.getString(
                    "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.CameraLocation.text"); //$NON-NLS-1$
        }
        lblHeadOffsetOrPosition = new JLabel(offsetOrPositionLabel);
        lblHeadOffsetOrPosition.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblHeadOffsetOrPosition, "2, 46, right, default");
        
        textFieldXOffset = new JTextField();
        textFieldXOffset.setEditable(false);
        panelCameraCalibration.add(textFieldXOffset, "4, 46, fill, default");
        textFieldXOffset.setColumns(10);
        
        textFieldYOffset = new JTextField();
        textFieldYOffset.setEditable(false);
        panelCameraCalibration.add(textFieldYOffset, "6, 46, fill, default");
        textFieldYOffset.setColumns(10);
        
        textFieldZOffset = new JTextField();
        textFieldZOffset.setEditable(false);
        panelCameraCalibration.add(textFieldZOffset, "8, 46, fill, default");
        textFieldZOffset.setColumns(10);
        
        lblNewLabel_24 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.UnitsPerPixelLabel.text")); //$NON-NLS-1$
        lblNewLabel_24.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel_24, "2, 48, right, default");
        
        textFieldUnitsPerPixel = new JTextField();
        textFieldUnitsPerPixel.setToolTipText(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.UnitsPerPixelTextField.toolTipText")); //$NON-NLS-1$
        textFieldUnitsPerPixel.setEditable(false);
        panelCameraCalibration.add(textFieldUnitsPerPixel, "4, 48, fill, default");
        textFieldUnitsPerPixel.setColumns(10);
        
        lblNewLabel_30 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.AtDefaultWorkingPlaneZLabel.text")); //$NON-NLS-1$
        panelCameraCalibration.add(lblNewLabel_30, "6, 48");
        
        lblNewLabel_4 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.EstimatedLocatingAccuracyLabel.text")); //$NON-NLS-1$
        lblNewLabel_4.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel_4, "2, 50, right, default");
        
        textFieldRmsError = new JTextField();
        textFieldRmsError.setToolTipText(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.RmsErrorTextField.toolTipText")); //$NON-NLS-1$
        textFieldRmsError.setEditable(false);
        panelCameraCalibration.add(textFieldRmsError, "4, 50, fill, default");
        textFieldRmsError.setColumns(10);
        
        lblNewLabel_29 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.AtDefaultWorkingPlaneZLabel.text")); //$NON-NLS-1$
        panelCameraCalibration.add(lblNewLabel_29, "6, 50");
        
        lblNewLabel_40 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.WidthLabel.text")); //$NON-NLS-1$
        panelCameraCalibration.add(lblNewLabel_40, "4, 52, default, top");
        
        lblNewLabel_41 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.HeightLabel.text")); //$NON-NLS-1$
        panelCameraCalibration.add(lblNewLabel_41, "6, 52");
        
        lblNewLabel_39 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.PhysicalFieldOfViewLabel.text")); //$NON-NLS-1$
        lblNewLabel_39.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel_39, "2, 54, right, default");
        
        textFieldWidthFov = new JTextField();
        textFieldWidthFov.setEditable(false);
        panelCameraCalibration.add(textFieldWidthFov, "4, 54, fill, default");
        textFieldWidthFov.setColumns(10);
        
        textFieldHeightFov = new JTextField();
        textFieldHeightFov.setEditable(false);
        panelCameraCalibration.add(textFieldHeightFov, "6, 54, fill, default");
        textFieldHeightFov.setColumns(10);
        
        lblNewLabel_42 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.EffectiveFieldOfViewLabel.text")); //$NON-NLS-1$
        lblNewLabel_42.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel_42, "2, 56, right, default");
        
        textFieldVirtualWidthFov = new JTextField();
        textFieldVirtualWidthFov.setEditable(false);
        panelCameraCalibration.add(textFieldVirtualWidthFov, "4, 56, fill, default");
        textFieldVirtualWidthFov.setColumns(10);
        
        textFieldVirtualHeightFov = new JTextField();
        textFieldVirtualHeightFov.setEditable(false);
        panelCameraCalibration.add(textFieldVirtualHeightFov, "6, 56, fill, default");
        textFieldVirtualHeightFov.setColumns(10);
        
        lblNewLabel_5 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.XAxisLabel.text")); //$NON-NLS-1$
        panelCameraCalibration.add(lblNewLabel_5, "4, 58");
        
        lblNewLabel_6 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.YAxisLabel.text")); //$NON-NLS-1$
        panelCameraCalibration.add(lblNewLabel_6, "6, 58");
        
        lblNewLabel_7 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.ZAxisLabel.text")); //$NON-NLS-1$
        panelCameraCalibration.add(lblNewLabel_7, "8, 58");
        
        lblNewLabel_2 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.CameraMountingErrorLabel.text")); //$NON-NLS-1$
        panelCameraCalibration.add(lblNewLabel_2, "2, 60, right, default");
        
        textFieldXRotationError = new JTextField();
        textFieldXRotationError.setToolTipText(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.XRotationErrorTextField.toolTipText")); //$NON-NLS-1$
        textFieldXRotationError.setEditable(false);
        panelCameraCalibration.add(textFieldXRotationError, "4, 60, fill, default");
        textFieldXRotationError.setColumns(10);
        
        textFieldYRotationError = new JTextField();
        textFieldYRotationError.setToolTipText(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.YRotationErrorTextField.toolTipText")); //$NON-NLS-1$
        textFieldYRotationError.setEditable(false);
        panelCameraCalibration.add(textFieldYRotationError, "6, 60, fill, default");
        textFieldYRotationError.setColumns(10);
        
        textFieldZRotationError = new JTextField();
        textFieldZRotationError.setToolTipText(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.ZRotationErrorTextField.toolTipText")); //$NON-NLS-1$
        textFieldZRotationError.setEditable(false);
        panelCameraCalibration.add(textFieldZRotationError, "8, 60, fill, default");
        textFieldZRotationError.setColumns(10);
        
        lblNewLabel_8 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.SelectedCalZForPlottingLabel.text")); //$NON-NLS-1$
        lblNewLabel_8.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel_8, "2, 62");
        
        spinnerModel = new SpinnerListModel(calibrationHeightSelections);
        spinnerIndex = new JSpinner(spinnerModel);
        spinnerIndex.setToolTipText(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.IndexSpinner.toolTipText")); //$NON-NLS-1$
        spinnerIndex.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                LengthCellValue selectedHeight = (LengthCellValue) spinnerIndex.getValue();
                heightIndex  = calibrationHeightSelections.indexOf(selectedHeight);
                updateDiagnosticsDisplay();
            }
            
        });
        panelCameraCalibration.add(spinnerIndex, "4, 62");
        
        chckbxShowOutliers = new JCheckBox(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.ShowOutliersChkBox.text")); //$NON-NLS-1$
        chckbxShowOutliers.addActionListener(showOutliersActionListener);
        panelCameraCalibration.add(chckbxShowOutliers, "6, 62");

        
        lblNewLabel_14 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.ResidualErrorsInCollectionOrderLabel.text" //$NON-NLS-1$
        ));
        lblNewLabel_14.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblNewLabel_14.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_14, "4, 64, 3, 1");

        lblNewLabel_9 = new VerticalLabel(String.format(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.ResidualError.text") //$NON-NLS-1$
                + " [%s]", smallDisplayUnits.getShortName()));
        lblNewLabel_9.setVerticalAlignment(SwingConstants.BOTTOM);
        lblNewLabel_9.setRotation(VerticalLabel.ROTATE_LEFT);
        panelCameraCalibration.add(lblNewLabel_9, "2, 66");
        
        modelErrorsTimeSequenceView = new SimpleGraphView();
        modelErrorsTimeSequenceView.setFont(new Font("Dialog", Font.PLAIN, 11));
        panelCameraCalibration.add(modelErrorsTimeSequenceView, "4, 66, 3, 1, fill, fill");

        String legend = Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.Legend.text"); //$NON-NLS-1$

        lblNewLabel_17 = new JLabel(String.format(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.ResidualErrorsLegendDescriptionLabel.text" //$NON-NLS-1$
        ), legend));
        panelCameraCalibration.add(lblNewLabel_17, "8, 66, 3, 1, left, default");
        
        lblNewLabel_10 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.CollectionSequenceNumberLabel.text" //$NON-NLS-1$
        ));
        lblNewLabel_10.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_10, "4, 68, 3, 1");

        lblNewLabel_15 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.ResidualErrorXYScatterPlotLabel.text")); //$NON-NLS-1$
        lblNewLabel_15.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblNewLabel_15.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_15, "4, 70, 3, 1");

        lblNewLabel_11 = new VerticalLabel(String.format("Y %s [%s]", Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.ResidualError.text"), //$NON-NLS-1$
                smallDisplayUnits.getShortName()));
        lblNewLabel_11.setVerticalAlignment(SwingConstants.BOTTOM);
        lblNewLabel_11.setRotation(VerticalLabel.ROTATE_LEFT);
        panelCameraCalibration.add(lblNewLabel_11, "2, 72");

        modelErrorsScatterPlotView = new SimpleGraphView();
        modelErrorsScatterPlotView.setFont(new Font("Dialog", Font.PLAIN, 11));
        panelCameraCalibration.add(modelErrorsScatterPlotView, "4, 72, 3, 1, fill, fill");
        
        lblNewLabel_18 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.ResidualErrorsXYDescriptionLabel.text")); //$NON-NLS-1$
        panelCameraCalibration.add(lblNewLabel_18, "8, 72, 3, 1, left, default");

        lblNewLabel_12 = new JLabel(String.format("X %s [%s]", Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.ResidualError.text"), //$NON-NLS-1$
                smallDisplayUnits.getShortName()));
        lblNewLabel_12.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_12, "4, 74, 3, 1");
        

        lblNewLabel_16 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.ResidualErrorMapLabel.text")); //$NON-NLS-1$
        lblNewLabel_16.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblNewLabel_16.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_16, "4, 76, 3, 1");

        lblNewLabel_13 = new VerticalLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.ImageYLocationLabel.text")); //$NON-NLS-1$
        lblNewLabel_13.setVerticalAlignment(SwingConstants.BOTTOM);
        lblNewLabel_13.setRotation(VerticalLabel.ROTATE_LEFT);
        panelCameraCalibration.add(lblNewLabel_13, "2, 78");

        modelErrorsView = new MatView();
        panelCameraCalibration.add(modelErrorsView, "4, 78, 3, 1, fill, fill");
        
        lblNewLabel_19 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.ResidualErrorMapDescriptionLabel.text")); //$NON-NLS-1$
        panelCameraCalibration.add(lblNewLabel_19, "8, 78, 5, 1, left, default");
        
        lblNewLabel_12 = new JLabel(Translations.getString(
                "ReferenceCameraCalibrationWizard.CameraCalibrationPanel.ImageXLocationLabel.text")); //$NON-NLS-1$
        lblNewLabel_12.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_12, "4, 80, 3, 1");
        
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        LengthConverter lengthConverter = new LengthConverter();
        DoubleConverter doubleConverter = new DoubleConverter("%.3f");
        
        if (isMovable) {
            if (referenceCamera instanceof ImageCamera) {
                bind(UpdateStrategy.READ, (ImageCamera) referenceCamera, "primaryFiducial",
                        advCal, "primaryLocation" );
                bind(UpdateStrategy.READ, (ImageCamera) referenceCamera, "secondaryFiducial",
                        advCal, "secondaryLocation" );
            }
            else {
                bind(UpdateStrategy.READ, referenceHead, "calibrationPrimaryFiducialLocation",
                        advCal, "primaryLocation" );
                bind(UpdateStrategy.READ, referenceHead, "calibrationSecondaryFiducialLocation",
                        advCal, "secondaryLocation" );
            }
        }
        else {
            bind(UpdateStrategy.READ, referenceCamera, "headOffsets",
                    advCal, "primaryLocation" );
            bind(UpdateStrategy.READ_WRITE, this, "secondaryLocation",
                    advCal, "secondaryLocation" );
        }
        
        MutableLocationProxy primaryLocationProxy = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, advCal, "primaryLocation", primaryLocationProxy, "location");
        bind(UpdateStrategy.READ_WRITE, primaryLocationProxy, "lengthZ", textFieldPrimaryCalZ, "text", lengthConverter);

        MutableLocationProxy secondaryLocationProxy = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, advCal, "secondaryLocation", secondaryLocationProxy, "location");
        bind(UpdateStrategy.READ_WRITE, secondaryLocationProxy, "lengthZ", textFieldSecondaryCalZ, "text", lengthConverter);

        bind(UpdateStrategy.READ_WRITE, advCal, 
                "overridingOldTransformsAndDistortionCorrectionSettings",
                chckbxAdvancedCalOverride, "selected");
        bind(UpdateStrategy.READ, advCal, "valid",
                chckbxEnable, "enabled");
        bind(UpdateStrategy.READ_WRITE, advCal, "enabled",
                chckbxEnable, "selected");
        bind(UpdateStrategy.READ, advCal, "valid",
                spinnerIndex, "enabled");
        
        bind(UpdateStrategy.READ_WRITE, advCal, "alphaPercent",
                sliderAlpha, "value");
        bind(UpdateStrategy.READ_WRITE, advCal, "fiducialDiameter",
                spinnerDiameter, "value");
        bind(UpdateStrategy.READ_WRITE, referenceCamera, "defaultZ", 
                textFieldDefaultZ, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, referenceCamera, "cropWidth", 
                textFieldCropWidth, "text", intConverter);
        bind(UpdateStrategy.READ_WRITE, referenceCamera, "cropHeight", 
                textFieldCropHeight, "text", intConverter);
        bind(UpdateStrategy.READ_WRITE, referenceCamera, "deinterlace", 
                checkboxDeinterlace, "selected");
        
        bind(UpdateStrategy.READ_WRITE, advCal, "desiredRadialLinesPerTestPattern",
                textFieldDesiredNumberOfRadialLines, "text", intConverter );
        bind(UpdateStrategy.READ, advCal, "widthFov",
                textFieldWidthFov, "text", doubleConverter );
        bind(UpdateStrategy.READ, advCal, "heightFov",
                textFieldHeightFov, "text", doubleConverter );
        bind(UpdateStrategy.READ, advCal, "virtualWidthFov",
                textFieldVirtualWidthFov, "text", doubleConverter );
        bind(UpdateStrategy.READ, advCal, "virtualHeightFov",
                textFieldVirtualHeightFov, "text", doubleConverter );
        bind(UpdateStrategy.READ, advCal, "rotationErrorZ",
                textFieldZRotationError, "text", doubleConverter);
        bind(UpdateStrategy.READ, advCal, "rotationErrorY",
                textFieldYRotationError, "text", doubleConverter);
        bind(UpdateStrategy.READ, advCal, "rotationErrorX",
                textFieldXRotationError, "text", doubleConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldDefaultZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPrimaryCalZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldSecondaryCalZ);
        ComponentDecorators.decorateWithAutoSelect(textFieldDesiredNumberOfRadialLines);
        
        enableControls(advCal.isOverridingOldTransformsAndDistortionCorrectionSettings());
        
        updateDiagnosticsDisplay();
    }

    private void enableControls(boolean b) {
        if (b) {
            chckbxEnable.setEnabled(advCal.isValid());
            checkboxDeinterlace.setEnabled(true);
            textFieldCropWidth.setEnabled(true);
            textFieldCropHeight.setEnabled(true);
            textFieldDefaultZ.setEnabled(isMovable);
            textFieldSecondaryCalZ.setEnabled(!isMovable);
            textFieldDesiredNumberOfRadialLines.setEnabled(true);
            startCameraCalibrationBtn.setEnabled(true);
            chckbxUseSavedData.setEnabled(advCal.isDataAvailable());
            sliderAlpha.setEnabled(true);
            spinnerIndex.setEnabled(advCal.isValid());
            chckbxShowOutliers.setEnabled(advCal.isValid());
        }
        else {
            chckbxEnable.setEnabled(false);
            checkboxDeinterlace.setEnabled(false);
            textFieldCropWidth.setEnabled(false);
            textFieldCropHeight.setEnabled(false);
            textFieldDefaultZ.setEnabled(false);
            textFieldSecondaryCalZ.setEnabled(false);
            textFieldDesiredNumberOfRadialLines.setEnabled(false);
            startCameraCalibrationBtn.setEnabled(false);
            chckbxUseSavedData.setEnabled(false);
            sliderAlpha.setEnabled(false);
            spinnerIndex.setEnabled(false);
            chckbxShowOutliers.setEnabled(false);
        }
    }
    
    private Action overrideAction = new AbstractAction("Override Old Style") {
        @Override
        public void actionPerformed(ActionEvent e) {
            enableControls(chckbxAdvancedCalOverride.isSelected());
            referenceCamera.setHeadOffsets(referenceCamera.getHeadOffsets());
        }
    };
    
    private Action enableAction = new AbstractAction("Enable Calibration") {
        @Override
        public void actionPerformed(ActionEvent e) {
            referenceCamera.setHeadOffsets(referenceCamera.getHeadOffsets());
        }
    };
    
    private Action showOutliersActionListener = new AbstractAction("Show Outliers") {
        @Override
        public void actionPerformed(ActionEvent e) {
            updateDiagnosticsDisplay();
        }
    };
    
    private Action startCalibration = new AbstractAction("Start Calibration Data Collection") {
        @Override
        public void actionPerformed(ActionEvent e) {
            //Pre-calibration checks
            if (!Configuration.get().getMachine().isHomed()) {
                MessageBoxes.errorBox(MainFrame.get(), "Error", "Machine must be enabled and homed before starting calibration.");
                return;
            }
            if (advCal.getPrimaryLocation() == null || advCal.getSecondaryLocation() == null) {
                if (isMovable) {
                    MessageBoxes.errorBox(MainFrame.get(), "Error", "Must define Primary and Secondary Calibration Fiducial locations before starting calibration.");
                    return;
                }
                else {
                    MessageBoxes.errorBox(MainFrame.get(), "Error", "Must define camera location before starting calibration.");
                    return;
                }
            }
            if (!Double.isFinite(advCal.getPrimaryLocation().getZ()) || !Double.isFinite(advCal.getSecondaryLocation().getZ())) {
                MessageBoxes.errorBox(MainFrame.get(), "Error", "Must define finite Primary and Secondary Calibration Z values before starting calibration.");
                return;
            }
            if (advCal.getPrimaryLocation().getZ() == advCal.getSecondaryLocation().getZ()) {
                MessageBoxes.errorBox(MainFrame.get(), "Error", "Primary and Secondary Calibration Z values must be different.");
                return;
            }
            if (referenceCamera.getDefaultZ() == null || !Double.isFinite(referenceCamera.getDefaultZ().getValue())) {
                MessageBoxes.errorBox(MainFrame.get(), "Error", "Must define finite Default Working Plane Z value before starting calibration.");
                return;
            }
            
            try {
                processStarting();
                
                startCameraCalibrationBtn.setEnabled(false);
    
                MainFrame.get().getCameraViews().setSelectedCamera(referenceCamera);
    
                calibrationLocations = new ArrayList<>();
                calibrationLocations.add(advCal.getPrimaryLocation());
                calibrationLocations.add(advCal.getSecondaryLocation());
                
                ArrayList<Integer> detectionDiameters = new ArrayList<>();
                if (Double.isFinite(primaryDiameter)) {
                    detectionDiameters.add((int) Math.round(primaryDiameter));
                }
                else {
                    detectionDiameters.add(null);
                }
                if (Double.isFinite(secondaryDiameter)) {
                    detectionDiameters.add((int) Math.round(secondaryDiameter));
                }
                else {
                    detectionDiameters.add(null);
                }
                
                boolean savedEnabledState = advCal.isEnabled();
                boolean savedValidState = advCal.isValid();
                advCal.setEnabled(false);
                chckbxEnable.setSelected(false);
                referenceCamera.clearCalibrationCache();
                referenceCamera.captureTransformed(); //force image width and height to be recomputed
                
                if (!chckbxUseSavedData.isSelected()) {
                    spinnerDiameter.setEnabled(true);
                    
                    CameraView cameraView = MainFrame.get().getCameraViews().
                            getCameraView(referenceCamera);
        
                    UiUtils.messageBoxOnException(() -> {
                        new CalibrateCameraProcess(MainFrame.get(), cameraView, 
                                calibrationLocations, detectionDiameters, 0) {
        
                            @Override 
                            public void processRawCalibrationData(double[][][] testPattern3dPointsList, 
                                    double[][][] testPatternImagePointsList, Size size, double mirrored,
                                    double apparentMotionDirection) throws Exception {
                                
                                advCal.setDataAvailable(true);
                                advCal.setValid(false);

                                chckbxUseSavedData.setEnabled(true);
                                
                                try {
                                    advCal.processRawCalibrationData(
                                            testPattern3dPointsList, testPatternImagePointsList, 
                                            size, mirrored, apparentMotionDirection);
                                    
                                    postCalibrationProcessing();
                                }
                                catch (Exception e) {
                                    UiUtils.showError(e);
                                    advCal.setValid(false);
                                    advCal.setEnabled(false);
                                    chckbxEnable.setSelected(false);
                                    chckbxEnable.setEnabled(false);
                                    chckbxUseSavedData.setEnabled(false);
                                    spinnerIndex.setEnabled(false);
                                    chckbxShowOutliers.setEnabled(false);
                                }
                                
                                startCameraCalibrationBtn.setEnabled(true);
                                spinnerDiameter.setEnabled(false);
                                
                                processCompleted();
                            }
        
                            @Override
                            protected void processCanceled() {
                                advCal.setValid(savedValidState);
                                advCal.setEnabled(savedEnabledState);
                                chckbxEnable.setSelected(savedEnabledState);
                                chckbxEnable.setEnabled(savedValidState);
                                
                                startCameraCalibrationBtn.setEnabled(true);
                                spinnerDiameter.setEnabled(false);
    
                                processCompleted();
                            }
                        };
                    });
                }
                else {
                    try {
                        referenceCamera.getAdvancedCalibration().processRawCalibrationData(
                                new Size(advCal.getRawCroppedImageWidth(), advCal.getRawCroppedImageHeight()));
                    
                        postCalibrationProcessing();
                    }
                    catch (Exception ex) {
                        MessageBoxes.errorBox(MainFrame.get(), "Error", ex);
                        advCal.setValid(false);
                        advCal.setEnabled(false);
                        chckbxEnable.setSelected(false);
                        chckbxEnable.setEnabled(false);
                        spinnerIndex.setEnabled(false);
                        chckbxShowOutliers.setEnabled(false);
                    }
                    
                    startCameraCalibrationBtn.setEnabled(true);
                    spinnerDiameter.setEnabled(false);
                    
                    processCompleted();
                }
            }
            catch (IllegalStateException ex) {
                MessageBoxes.errorBox(MainFrame.get(), "Error", ex);
                return;
            }
        }
    };

    private void postCalibrationProcessing() throws Exception {
        advCal.applyCalibrationToMachine(referenceHead, referenceCamera);

        //Reload the calibration heights and refresh the table
        calibrationHeightSelections = new ArrayList<LengthCellValue>();
        int numberOfCalibrationHeights = advCal.
                getSavedTestPattern3dPointsList().length;
        for (int i=0; i<numberOfCalibrationHeights; i++) {
            calibrationHeightSelections.add(new LengthCellValue(
                    new Length(advCal.
                            getSavedTestPattern3dPointsList()[i][0][2], 
                            LengthUnit.Millimeters)));
        }
        
        spinnerModel.setList(calibrationHeightSelections);
        
        chckbxEnable.setSelected(true);
        chckbxEnable.setEnabled(true);
        spinnerIndex.setEnabled(true);
        chckbxShowOutliers.setEnabled(true);
        
        updateDiagnosticsDisplay();
        
        if (isMovable && referenceCamera.getHead().getDefaultCamera() == referenceCamera) {
            int ans = JOptionPane.showConfirmDialog(MainFrame.get(), 
                    "Calibration of the head's default camera is complete and the machine should "
                    + "be re-homed before any new locations are captured/examined. Home the machine now?", 
                    "Calibration Complete", JOptionPane.YES_NO_OPTION);
            if (ans == JOptionPane.YES_OPTION) {
                UiUtils.submitUiMachineTask(() -> {
                    Machine machine = Configuration.get().getMachine();
                    machine.home();
                });
            }
        }
    }
    
    private ChangeListener sliderAlphaChanged = new ChangeListener() {

        @Override
        public void stateChanged(ChangeEvent e) {
            if (!sliderAlpha.getValueIsAdjusting()) {
                int alphaPercent = (int)sliderAlpha.getValue();
                advCal.setAlphaPercent(alphaPercent);
                referenceCamera.clearCalibrationCache();
                updateDiagnosticsDisplay();
            }
        }
        
    };
    
    private void updateDiagnosticsDisplay() {
        if (advCal.isValid()) {
            referenceCamera.captureTransformed(); //make sure the camera is up-to-date

            Location headOffsets;
            if (isMovable) {
                headOffsets = advCal.getCalibratedOffsets().convertToUnits(displayUnits);
            }
            else {
                headOffsets = referenceCamera.getLocation().convertToUnits(displayUnits);
            }
            textFieldXOffset.setText((new LengthCellValue(headOffsets.getLengthX())).toString());
            textFieldYOffset.setText((new LengthCellValue(headOffsets.getLengthY())).toString());
            textFieldZOffset.setText((new LengthCellValue(headOffsets.getLengthZ())).toString());
            
            Location uppAtDefaultZ = referenceCamera.getUnitsPerPixel(referenceCamera.getDefaultZ()).
                    convertToUnits(smallDisplayUnits);
            textFieldUnitsPerPixel.setText(
                    (new LengthCellValue(uppAtDefaultZ.getLengthX(), true)).toString());
            
            textFieldRmsError.setText((new LengthCellValue(uppAtDefaultZ.getLengthX().
                    multiply(advCal.getRmsError()), true)).toString());

            Location uppAtHeight = referenceCamera.getUnitsPerPixel(calibrationHeightSelections.get(heightIndex).
                    getLength()).convertToUnits(smallDisplayUnits);
            double smallUnitsPerPixel = uppAtHeight.getLengthX().getValue();
            
            List<double[]> residuals = null;
            try {
                if (chckbxShowOutliers.isSelected()) {
                    residuals = CameraCalibrationUtils.computeResidualErrors(
                        advCal.getSavedTestPatternImagePointsList(), 
                        advCal.getModeledImagePointsList(), heightIndex);
                }
                else {
                    residuals = CameraCalibrationUtils.computeResidualErrors(
                            advCal.getSavedTestPatternImagePointsList(), 
                            advCal.getModeledImagePointsList(), heightIndex,
                            advCal.getOutlierPointList());
                }
                SimpleGraph sequentialErrorGraph = new SimpleGraph();
                sequentialErrorGraph.setRelativePaddingLeft(0.10);
                SimpleGraph.DataScale dataScale = new SimpleGraph.DataScale("Residual Error");
                dataScale.setRelativePaddingTop(0.05);
                dataScale.setRelativePaddingBottom(0.05);
                dataScale.setSymmetricIfSigned(true);
                dataScale.setColor(Color.GRAY);
                DataRow dataRowX = new SimpleGraph.DataRow("X", Color.RED);
                DataRow dataRowY = new SimpleGraph.DataRow("Y", Color.GREEN);
                int iPoint = 0;
                for (double[] residual : residuals) {
                    dataRowX.recordDataPoint(iPoint, smallUnitsPerPixel*residual[0]);
                    dataRowY.recordDataPoint(iPoint, smallUnitsPerPixel*residual[1]);
                    iPoint++;
                }
                dataScale.addDataRow(dataRowX);
                dataScale.addDataRow(dataRowY);
                sequentialErrorGraph.addDataScale(dataScale);
                modelErrorsTimeSequenceView.setGraph(sequentialErrorGraph);
                
                SimpleGraph scatterErrorGraph = new SimpleGraph();
                scatterErrorGraph.setRelativePaddingLeft(0.10);
                SimpleGraph.DataScale dataScaleScatter = 
                        new SimpleGraph.DataScale("Residual Error");
                dataScaleScatter.setRelativePaddingTop(0.05);
                dataScaleScatter.setRelativePaddingBottom(0.05);
                dataScaleScatter.setSymmetricIfSigned(true);
                dataScaleScatter.setSquareAspectRatio(true);
                dataScaleScatter.setColor(Color.GRAY);
                
                DataRow dataRowXY = new SimpleGraph.DataRow("XY", Color.RED);
                dataRowXY.setLineShown(false);
                dataRowXY.setMarkerShown(true);
                iPoint = 0;
                for (double[] residual : residuals) {
                    dataRowXY.recordDataPoint(smallUnitsPerPixel*residual[0], 
                            smallUnitsPerPixel*residual[1]);
                    iPoint++;
                }
                dataScaleScatter.addDataRow(dataRowXY);
                
                //Need to draw the circle in two parts
                DataRow dataRowCircleTop = new SimpleGraph.DataRow("CircleT", Color.GREEN);
                DataRow dataRowCircleBottom = new SimpleGraph.DataRow("CircleB", Color.GREEN);
                double radius = smallUnitsPerPixel * advCal.getRmsError() * 
                        CameraCalibrationUtils.sigmaThresholdForRejectingOutliers;
                for (int i=0; i<=45; i++) {
                    dataRowCircleTop.recordDataPoint(radius*Math.cos(i*2*Math.PI/90), 
                            radius*Math.sin(i*2*Math.PI/90));
                    dataRowCircleBottom.recordDataPoint(radius*Math.cos((i+45)*2*Math.PI/90), 
                            radius*Math.sin((i+45)*2*Math.PI/90));
                }
                dataScaleScatter.addDataRow(dataRowCircleTop);
                dataScaleScatter.addDataRow(dataRowCircleBottom);
                
                scatterErrorGraph.addDataScale(dataScaleScatter);
                modelErrorsScatterPlotView.setGraph(scatterErrorGraph);
                
                Mat errorImage = new Mat();
                
                int width = referenceCamera.getCropWidth();
                int height = referenceCamera.getCropHeight();
                if (width > 0) {
                    width = Math.min(width, advCal.getRawCroppedImageWidth());
                }
                else {
                    width = advCal.getRawCroppedImageWidth();
                }
                if (height > 0) {
                    height = Math.min(height, advCal.getRawCroppedImageHeight());
                }
                else {
                    height = advCal.getRawCroppedImageHeight();
                }
                
                if (chckbxShowOutliers.isSelected()) {
                    errorImage = CameraCalibrationUtils.generateErrorImage(
                            new Size(width, height), 
                            heightIndex, 
                            advCal.getSavedTestPatternImagePointsList(), 
                            advCal.getModeledImagePointsList(), 
                            null);
                }
                else {
                    errorImage = CameraCalibrationUtils.generateErrorImage(
                            new Size(width, height), 
                            heightIndex, 
                            advCal.getSavedTestPatternImagePointsList(), 
                            advCal.getModeledImagePointsList(), 
                            advCal.getOutlierPointList());
                }
                modelErrorsView.setMat(errorImage);
                errorImage.release();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private JCheckBox chckbxEnable;
    private JSlider sliderAlpha;
    private JLabel lblNewLabel;
    private JTextField textFieldDefaultZ;
    private JLabel lblNewLabel_1;
    private JCheckBox chckbxUseSavedData;
    private JCheckBox chckbxAdvancedCalOverride;
    private JTextField textFieldZRotationError;
    private JTextField textFieldYRotationError;
    private JTextField textFieldXRotationError;
    private JLabel lblNewLabel_2;
    private JLabel lblNewLabel_5;
    private JLabel lblNewLabel_6;
    private JLabel lblNewLabel_7;
    private JLabel lblNewLabel_3;
    private JTextField textFieldRmsError;
    private JLabel lblNewLabel_4;
    private JSeparator separator;
    private JSeparator separator_1;
    private MatView modelErrorsView;
    private JSpinner spinnerIndex;
    private JLabel lblNewLabel_8;
    private VerticalLabel lblNewLabel_9;
    private JLabel lblNewLabel_10;
    private JLabel lblNewLabel_14;
    private JLabel lblNewLabel_15;
    private JLabel lblNewLabel_17;
    private JLabel lblNewLabel_18;
    private JLabel lblNewLabel_19;
    private JCheckBox checkboxDeinterlace;
    private JTextField textFieldCropWidth;
    private JTextField textFieldCropHeight;
    private JLabel lblNewLabel_20;
    private JLabel lblNewLabel_21;
    private JLabel lblNewLabel_22;
    private JLabel lblNewLabel_23;
    private JLabel lblHeadOffsetOrPosition;
    private JTextField textFieldXOffset;
    private JTextField textFieldYOffset;
    private JTextField textFieldZOffset;
    private JTextField textFieldUnitsPerPixel;
    private JLabel lblNewLabel_24;
    private JLabel lblNewLabel_25;
    private JLabel lblNewLabel_26;
    private JLabel lblNewLabel_27;
    private JTextField textFieldDesiredNumberOfRadialLines;
    private JLabel lblNewLabel_28;
    private JLabel lblNewLabel_29;
    private JLabel lblNewLabel_30;
    private JSeparator separator_2;
    private JLabel lblNewLabel_31;
    private JLabel lblNewLabel_32;
    private JLabel lblNewLabel_33;
    private JLabel lblDescription;
    private JSpinner spinnerDiameter;
    private JLabel lblNewLabel_34;
    private JTextField textFieldPrimaryCalZ;
    private JTextField textFieldSecondaryCalZ;
    private JLabel lblNewLabel_35;
    private JLabel lblNewLabel_36;
    private JCheckBox chckbxShowOutliers;
    private JLabel lblNewLabel_37;
    private JLabel lblNewLabel_39;
    private JTextField textFieldWidthFov;
    private JTextField textFieldHeightFov;
    private JLabel lblNewLabel_40;
    private JLabel lblNewLabel_41;
    private JLabel lblNewLabel_42;
    private JTextField textFieldVirtualWidthFov;
    private JTextField textFieldVirtualHeightFov;
    private JLabel lblNewLabel_38;

}
