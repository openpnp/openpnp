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
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.SimpleGraphView;
import org.openpnp.gui.components.VerticalLabel;
import org.openpnp.gui.processes.CalibrateCameraProcess;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.calibration.AdvancedCalibration;
import org.openpnp.machine.reference.camera.calibration.CameraCalibrationUtils;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.util.SimpleGraph;
import org.openpnp.util.SimpleGraph.DataRow;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.ui.MatView;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JCheckBox;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SpinnerListModel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;

@SuppressWarnings("serial")
public class ReferenceCameraCalibrationWizard extends AbstractConfigurationWizard {
    private final ReferenceCamera referenceCamera;
    private final boolean isMovable;
    private JPanel panelCameraCalibration;
    private JButton startCameraCalibrationBtn;
    private AdvancedCalibration advancedCalibration;
    private List<Length> calibrationHeights;
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
    private Length primaryZ;
    private Length secondaryZ;


    /**
     * @return the calibrationHeights
     */
    public List<Length> getCalibrationHeights() {
        return calibrationHeights;
    }

    /**
     * @param calibrationHeights the calibrationHeights to set
     */
    public void setCalibrationHeights(List<Length> calibrationHeights) {
        List<Length> oldValue = this.calibrationHeights;
        this.calibrationHeights = calibrationHeights;
        firePropertyChange("calibrationHeights", oldValue, calibrationHeights);
    }

    public List<LengthCellValue> getCalibrationHeightSelections() {
        return calibrationHeightSelections;
    }

    public void setCalibrationHeightSelections(List<LengthCellValue> calibrationHeightSelections) {
        this.calibrationHeightSelections = calibrationHeightSelections;
    }

    /**
     * @return the primaryZ
     */
    public Length getPrimaryZ() {
        return primaryZ;
    }

    /**
     * @param primaryZ the primaryZ to set
     */
    public void setPrimaryZ(Length primaryZ) {
        this.primaryZ = primaryZ;
    }

    /**
     * @return the secondaryZ
     */
    public Length getSecondaryZ() {
        return secondaryZ;
    }

    /**
     * @param secondaryZ the secondaryZ to set
     */
    public void setSecondaryZ(Length secondaryZ) {
        this.secondaryZ = secondaryZ;
    }

    public ReferenceCameraCalibrationWizard(ReferenceCamera referenceCamera) {
        this.referenceCamera = referenceCamera;
        isMovable = referenceCamera.getHead() != null;
        advancedCalibration = referenceCamera.getAdvancedCalibration();
        calibrationHeights = new ArrayList<>();
        calibrationHeightSelections = new ArrayList<>();
        if (isMovable) {
            primaryZ = ((ReferenceHead) referenceCamera.getHead()).getCalibrationPrimaryFiducialLocation().getLengthZ();
            secondaryZ = ((ReferenceHead) referenceCamera.getHead()).getCalibrationSecondaryFiducialLocation().getLengthZ();
        }
        else {
            primaryZ = referenceCamera.getUncalibratedHeadOffsets().getLengthZ();
            referenceCamera.setDefaultZ(primaryZ);
            if (advancedCalibration.getSavedTestPattern3dPointsList() != null && advancedCalibration.getSavedTestPattern3dPointsList().length >= 2) {
                secondaryZ = new Length(advancedCalibration.getSavedTestPattern3dPointsList()[1][0][2], LengthUnit.Millimeters);
            }
            else {
                secondaryZ = primaryZ.multiply(0.5); //default to half-way between primaryZ and 0
            }
        }
        if (advancedCalibration.getSavedTestPattern3dPointsList() != null && advancedCalibration.getSavedTestPattern3dPointsList().length >= 2) {
            calibrationHeightSelections.add(new LengthCellValue(new Length(advancedCalibration.getSavedTestPattern3dPointsList()[0][0][2], LengthUnit.Millimeters)));
            calibrationHeightSelections.add(new LengthCellValue(new Length(advancedCalibration.getSavedTestPattern3dPointsList()[1][0][2], LengthUnit.Millimeters)));
        }
        else {
            calibrationHeightSelections.add(new LengthCellValue(primaryZ));
            calibrationHeightSelections.add(new LengthCellValue(secondaryZ));
        };
        
        displayUnits = Configuration.get().getSystemUnits();
        if (displayUnits.equals(LengthUnit.Inches)) {
            smallDisplayUnits = LengthUnit.Mils;
        }
        else {
            smallDisplayUnits = LengthUnit.Microns;
        }
            
        panelCameraCalibration = new JPanel();
        panelCameraCalibration.setBorder(new TitledBorder(null, "Camera Calibration",
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
                FormSpecs.UNRELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.UNRELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.UNRELATED_GAP_ROWSPEC,
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
        
        chckbxAdvancedCalOverride = new JCheckBox("Enable experimental calibration to override old"
                + " style image transforms and distortion correction settings");
        chckbxAdvancedCalOverride.setToolTipText("Enable this to use advanced calibration.  "
                + "Disable this to restore usage of old settings.");
        chckbxAdvancedCalOverride.addActionListener(overrideAction);
        panelCameraCalibration.add(chckbxAdvancedCalOverride, "2, 2, 11, 1");
        
        lblDescription = new JLabel("<html><p width=\"500\">"
                + "The settings on this tab are intended to eventually replace all of the Units "
                + "Per Pixel settings on the General Configuration tab, everything on the Lens "
                + "Calibration tab, and everything on the Image Transforms tab. It will also "
                + "replace the Calibrate Camera Position and Rotation button on the Nozzle Tips "
                + "Calibration tab.</p>"
                + "<p> </p>"
                + "<p width=\"500\">"
                + "<b>Prerequsites:</b> The machine X, Y, and Z axis; backlash compensation; and "
                + "non-squareness correction must all be properly calibrated. And any issues with "
                + "mechanical non-repeatability (missed steps, loose pulleys/cogs, slipping belts "
                + "etcetra) should be resolved before attempting camera calibration. In addition; "
                + "for bottom cameras, the rotation axis and nozzle offsets must be properly "
                + "calibrated; and, visual homing, if it is going to be used, must be setup and "
                + "working properly."
                + "</p></html>");
        panelCameraCalibration.add(lblDescription, "4, 4, 7, 1");
        
        separator_2 = new JSeparator();
        panelCameraCalibration.add(separator_2, "2, 6, 13, 1");
        
        lblNewLabel_31 = new JLabel("General Settings");
        lblNewLabel_31.setFont(new Font("Tahoma", Font.BOLD, 14));
        lblNewLabel_31.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_31, "2, 8, 7, 1");
        
        checkboxDeinterlace = new JCheckBox("Deinterlace");
        checkboxDeinterlace.setToolTipText("Removes interlacing from stacked frames");
        panelCameraCalibration.add(checkboxDeinterlace, "4, 10");
        
        lblNewLabel_20 = new JLabel("Cropped Width");
        panelCameraCalibration.add(lblNewLabel_20, "2, 12, right, default");
        
        textFieldCropWidth = new JTextField();
        panelCameraCalibration.add(textFieldCropWidth, "4, 12, fill, default");
        textFieldCropWidth.setColumns(10);
        
        lblNewLabel_22 = new JLabel("(Set to zero for no cropping)");
        panelCameraCalibration.add(lblNewLabel_22, "6, 12");
        
        lblNewLabel_21 = new JLabel("Cropped Height");
        panelCameraCalibration.add(lblNewLabel_21, "2, 14, right, default");
        
        textFieldCropHeight = new JTextField();
        panelCameraCalibration.add(textFieldCropHeight, "4, 14, fill, default");
        textFieldCropHeight.setColumns(10);
        
        lblNewLabel_23 = new JLabel("(Set to zero for no cropping)");
        panelCameraCalibration.add(lblNewLabel_23, "6, 14");
        
        lblNewLabel_1 = new JLabel("Default Working Plane Z");
        panelCameraCalibration.add(lblNewLabel_1, "2, 16, right, default");
        
        textFieldDefaultZ = new JTextField();
        panelCameraCalibration.add(textFieldDefaultZ, "4, 16, fill, default");
        textFieldDefaultZ.setColumns(10);
        
        separator = new JSeparator();
        panelCameraCalibration.add(separator, "2, 18, 13, 1");
        
        lblNewLabel_32 = new JLabel("Calibration Setup");
        lblNewLabel_32.setFont(new Font("Tahoma", Font.BOLD, 14));
        lblNewLabel_32.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_32, "2, 20, 7, 1");
        
        if (isMovable) {
            textFieldDefaultZ.setToolTipText("<html><p width=\"500\">"
                + "This is the assumed Z coordinate of objects viewed by the "
                + "camera if their true Z coordinate is unknown. Typically this "
                + "is set to the Z coordinate of the working surface of the "
                + "board(s) to be populated.</p></html>");
        }
        else {
            textFieldDefaultZ.setToolTipText("<html><p width=\"500\">"
                    + "This is the Z coordinate to which the bottom surface of "
                    + "parts carried by the nozzle will be lowered for visual "
                    + "alignment.</p></html>");
        }
        
        lblNewLabel_35 = new JLabel("Primary Calibration Z");
        lblNewLabel_35.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel_35, "2, 22, right, default");
        
        textFieldPrimaryCalZ = new JTextField();
        if (isMovable) {
            textFieldPrimaryCalZ.setToolTipText("<html><p width=\"500\">"
                + "This is the Z coordinate of the Primary Calibration Fiducial."
                + "</p></html>");
        }
        else {
            textFieldPrimaryCalZ.setToolTipText("<html><p width=\"500\">"
                    + "This is the Z coordinate where objects are in best focus for this camera."
                    + "</p></html>");
        }
        textFieldPrimaryCalZ.setEnabled(false);
        panelCameraCalibration.add(textFieldPrimaryCalZ, "4, 22, fill, default");
        textFieldPrimaryCalZ.setColumns(10);
        
        if (!isMovable) {
            lblNewLabel_37 = new JLabel("<html><p width=\"500\" "
                    + "style=\"color:Black;background-color:Yellow;\">" 
                    + "Caution - The nozzle tip will be lowered to these Z coordinates and moved "
                    + "over the camera's <b>entire field-of-view</b> during the calibration sequence. "
                    + "Ensure there is sufficient clearance to any obstacles near the camera "
                    + "before starting calibration or machine damage may occur."
                    + "</p></html>");
            panelCameraCalibration.add(lblNewLabel_37, "6, 22, 5, 3");
        }
        
        lblNewLabel_36 = new JLabel("Secondary Calibration Z");
        lblNewLabel_36.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel_36, "2, 24, right, default");
        
        textFieldSecondaryCalZ = new JTextField();
        if (isMovable) {
            textFieldSecondaryCalZ.setToolTipText("<html><p width=\"500\">"
                + "This is the Z coordinate of the Secondary Calibration Fiducial."
                + "</p></html>");
        }
        else {
            textFieldSecondaryCalZ.setToolTipText("<html><p width=\"500\">"
                    + "Set this larger (higher) than the Primary Cal Z as much as possible but "
                    + "such that the nozzle tip is still within reasonable focus."
                    + "</p></html>");
        }
        panelCameraCalibration.add(textFieldSecondaryCalZ, "4, 24, fill, default");
        textFieldSecondaryCalZ.setColumns(10);
        
        lblNewLabel_28 = new JLabel("Radial Lines Per Calibration Z");
        lblNewLabel_28.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel_28, "2, 26, right, default");
        
        textFieldDesiredNumberOfRadialLines = new JTextField();
        textFieldDesiredNumberOfRadialLines.setToolTipText("<html><p width=\"500\">"
                + "This is the desired number of radial lines along which calibration points will "
                + "be collected at each calibration Z coordinate. The actual number will be "
                + "rounded up to the nearest multiple of 4. Decreasing this number shortens the "
                + "calibration collection time but may result in lower quality or even failed "
                + "calibration. Increasing this may result in a higher quality calibration but "
                + "the collection will take longer.</p></html>");
        panelCameraCalibration.add(textFieldDesiredNumberOfRadialLines, "4, 26, fill, default");
        textFieldDesiredNumberOfRadialLines.setColumns(10);
        
        lblNewLabel_38 = new JLabel("Approximate Camera Lens Z");
        lblNewLabel_38.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel_38, "2, 28, right, default");
        
        textFieldApproximateCameraZ = new JTextField();
        textFieldApproximateCameraZ.setToolTipText("<html><p width=\"500\">"
                + "Enter the approximate (+/-20%) Z coordinate of the camera lens here. Use a "
                + "ruler to measure vertically from an object of known Z to the camera lens and "
                + "add/subtract the ruler measurment to/from the object's known Z."
                + "</p></html>");
        panelCameraCalibration.add(textFieldApproximateCameraZ, "4, 28, fill, default");
        textFieldApproximateCameraZ.setColumns(10);
        
        
        startCameraCalibrationBtn = new JButton(startCalibration);
        startCameraCalibrationBtn.setText("Start Calibration");
        panelCameraCalibration.add(startCameraCalibrationBtn, "4, 30");
                
        chckbxUseSavedData = new JCheckBox("Skip New Collection And Reprocess Prior Collection");
        chckbxUseSavedData.setEnabled(advancedCalibration.isValid());
        chckbxUseSavedData.setToolTipText("Set this to skip collection of new calibration data "
                + "and just reprocess previously collected calibration data - only useful for "
                + "code debugging");
        panelCameraCalibration.add(chckbxUseSavedData, "6, 30, 5, 1");
        
        lblNewLabel_34 = new JLabel("Detection Diameter");
        lblNewLabel_34.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel_34, "2, 32");
        
        spinnerDiameter = new JSpinner();
        spinnerDiameter.setToolTipText("When instructed, adjust this value to obtain the best "
                + "detection of the fiducial/nozzle tip.");
        spinnerDiameter.setEnabled(false);
        panelCameraCalibration.add(spinnerDiameter, "4, 32");
        referenceCamera.getAdvancedCalibration().setFiducialDiameter(
                Math.min(referenceCamera.getHeight(), referenceCamera.getWidth())/10);
        
        chckbxEnable = new JCheckBox("Apply Calibration");
        chckbxEnable.setToolTipText("Enable this to apply the new image transform and distortion "
                + "correction settings.  Disable this and no calibration will be applied (raw "
                + "images will be displayed).");
        chckbxEnable.setEnabled(advancedCalibration.isValid());
        panelCameraCalibration.add(chckbxEnable, "2, 34");
        
        sliderAlpha = new JSlider();
        sliderAlpha.setMaximum(100);
        sliderAlpha.setValue(100);
        sliderAlpha.setMajorTickSpacing(10);
        sliderAlpha.setMinorTickSpacing(2);
        sliderAlpha.setPaintTicks(true);
        sliderAlpha.setPaintLabels(true);
        sliderAlpha.addChangeListener(sliderAlphaChanged);
        
        lblNewLabel = new JLabel("Crop All Invalid Pixels");
        lblNewLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel, "2, 36");
        sliderAlpha.setToolTipText("<html><p width=\"500\">"
                + "A value of 0 crops all invalid pixels from the edge of the image but at the "
                + "risk of losing some valid pixels at the edge of the image. A value"
                + " of 100 forces all valid pixels to be displayed but at the risk of some invalid "
                + "(usually black) pixels being displayed around the edges of the image."
                + "</p></html>");
        panelCameraCalibration.add(sliderAlpha, "4, 36, 5, 1");
        
        lblNewLabel_3 = new JLabel("Show All Valid Pixels");
        panelCameraCalibration.add(lblNewLabel_3, "10, 36");
        
        separator_1 = new JSeparator();
        panelCameraCalibration.add(separator_1, "2, 38, 13, 1");
        
        lblNewLabel_33 = new JLabel("Calibration Results/Diagnostics");
        lblNewLabel_33.setHorizontalAlignment(SwingConstants.CENTER);
        lblNewLabel_33.setFont(new Font("Tahoma", Font.BOLD, 14));
        panelCameraCalibration.add(lblNewLabel_33, "2, 40, 7, 1");
        
        lblNewLabel_25 = new JLabel("X");
        panelCameraCalibration.add(lblNewLabel_25, "4, 42");
        
        lblNewLabel_26 = new JLabel("Y");
        panelCameraCalibration.add(lblNewLabel_26, "6, 42");
        
        lblNewLabel_27 = new JLabel("Z");
        panelCameraCalibration.add(lblNewLabel_27, "8, 42");
        
        String offsetOrPositionLabel;
        if (isMovable) {
            offsetOrPositionLabel = "Head Offsets";
        }
        else {
            offsetOrPositionLabel = "Camera Location";
        }
        lblHeadOffsetOrPosition = new JLabel(offsetOrPositionLabel);
        lblHeadOffsetOrPosition.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblHeadOffsetOrPosition, "2, 44, right, default");
        
        textFieldXOffset = new JTextField();
        textFieldXOffset.setEditable(false);
        panelCameraCalibration.add(textFieldXOffset, "4, 44, fill, default");
        textFieldXOffset.setColumns(10);
        
        textFieldYOffset = new JTextField();
        textFieldYOffset.setEditable(false);
        panelCameraCalibration.add(textFieldYOffset, "6, 44, fill, default");
        textFieldYOffset.setColumns(10);
        
        textFieldZOffset = new JTextField();
        textFieldZOffset.setEditable(false);
        panelCameraCalibration.add(textFieldZOffset, "8, 44, fill, default");
        textFieldZOffset.setColumns(10);
        
        lblNewLabel_24 = new JLabel("Units Per Pixel");
        lblNewLabel_24.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel_24, "2, 46, right, default");
        
        textFieldUnitsPerPixel = new JTextField();
        textFieldUnitsPerPixel.setToolTipText("<html><p width=\"500\">"
                + "This is the calculated units per pixel at this camera's default Z. Note that "
                + "the units per pixel is the same in both the X and Y directions.</p></html>");
        textFieldUnitsPerPixel.setEditable(false);
        panelCameraCalibration.add(textFieldUnitsPerPixel, "4, 46, fill, default");
        textFieldUnitsPerPixel.setColumns(10);
        
        lblNewLabel_30 = new JLabel("(at Default Working Plane Z)");
        panelCameraCalibration.add(lblNewLabel_30, "6, 46");
        
        lblNewLabel_5 = new JLabel("X Axis");
        panelCameraCalibration.add(lblNewLabel_5, "4, 48");
        
        lblNewLabel_6 = new JLabel("Y Axis");
        panelCameraCalibration.add(lblNewLabel_6, "6, 48");
        
        lblNewLabel_7 = new JLabel("Z Axis");
        panelCameraCalibration.add(lblNewLabel_7, "8, 48");
        
        lblNewLabel_2 = new JLabel("Camera Mounting Error [Deg]");
        panelCameraCalibration.add(lblNewLabel_2, "2, 50, right, default");
        
        textFieldXRotationError = new JTextField();
        textFieldXRotationError.setToolTipText("<html><p width=\"500\">"
                + "The estimated camera mounting error using the right hand rule about "
                + "the machine X axis. If the camera reticle crosshairs appear too far offset (up "
                + "or down) from the center of the image, correct this physical mounting error and "
                + "re-calibrate the camera. Note that this error does not affect the accuracy of "
                + "the machine but only reduces the maximum size of objects that can be centered "
                + "on the reticle crosshairs and still be fully visible in the image."
                + "</p></html>");
        textFieldXRotationError.setEditable(false);
        panelCameraCalibration.add(textFieldXRotationError, "4, 50, fill, default");
        textFieldXRotationError.setColumns(10);
        
        textFieldYRotationError = new JTextField();
        textFieldYRotationError.setToolTipText("<html><p width=\"500\">"
                + "The estimated camera mounting error using the right hand rule about "
                + "the machine Y axis. If the camera reticle crosshairs appear too far offset "
                + "(left or right) from the center of the image, correct this physical mounting "
                + "error and re-calibrate the camera. Note that this error does not affect the "
                + "accuracy of the machine but only reduces the maximum size of objects that can "
                + "be centered on the reticle crosshairs and still be fully visible in the image."
                + "</p></html>");
        textFieldYRotationError.setEditable(false);
        panelCameraCalibration.add(textFieldYRotationError, "6, 50, fill, default");
        textFieldYRotationError.setColumns(10);
        
        textFieldZRotationError = new JTextField();
        textFieldZRotationError.setToolTipText("<html><p width=\"500\">"
                + "The estimated camera mounting error using the right hand rule about "
                + "the machine Z axis. If the camera image appears rotated too much, correct this "
                + "physical mounting error and re-calibrate the camera. Note that this error does "
                + "not affect the accuracy of the machine but is mainly asthetic.</p></html>");
        textFieldZRotationError.setEditable(false);
        panelCameraCalibration.add(textFieldZRotationError, "8, 50, fill, default");
        textFieldZRotationError.setColumns(10);
        
        lblNewLabel_4 = new JLabel("Estimated Locating Accuracy");
        lblNewLabel_4.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel_4, "2, 52, right, default");
        
        textFieldRmsError = new JTextField();
        textFieldRmsError.setToolTipText("<html><p width=\"500\">"
                + "This is an estimate of the accuracy of object locations measured at "
                + "default Z that can be obtained with this camera. For a large number of "
                + "measurements performed at random locations throughout the image, one should "
                + "expect 63% of the measured locations to be within this distance of their true "
                + "location and 98% within double this distance. See the plots below for more "
                + "details."
                + "</p></html>");
        textFieldRmsError.setEditable(false);
        panelCameraCalibration.add(textFieldRmsError, "4, 52, fill, default");
        textFieldRmsError.setColumns(10);
        
        lblNewLabel_29 = new JLabel("(at Default Working Plane Z)");
        panelCameraCalibration.add(lblNewLabel_29, "6, 52");
        
        lblNewLabel_8 = new JLabel("Selected Cal Z For Plotting");
        lblNewLabel_8.setHorizontalAlignment(SwingConstants.TRAILING);
        panelCameraCalibration.add(lblNewLabel_8, "2, 54");
        
        spinnerModel = new SpinnerListModel(calibrationHeightSelections);
        spinnerIndex = new JSpinner(spinnerModel);
        spinnerIndex.setToolTipText("Used to select the data to display in the plots below.");
        spinnerIndex.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                LengthCellValue selectedHeight = (LengthCellValue) spinnerIndex.getValue();
                heightIndex  = calibrationHeightSelections.indexOf(selectedHeight);
                updateDiagnosticsDisplay();
            }
            
        });
        panelCameraCalibration.add(spinnerIndex, "4, 54");
        
        chckbxShowOutliers = new JCheckBox("Show Outliers");
        chckbxShowOutliers.addActionListener(showOutliersActionListener);
        panelCameraCalibration.add(chckbxShowOutliers, "6, 54");

        
        lblNewLabel_14 = new JLabel("Residual Errors In Collection Order");
        lblNewLabel_14.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblNewLabel_14.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_14, "4, 56, 3, 1");

        lblNewLabel_9 = new VerticalLabel("Residual Error [" + 
                smallDisplayUnits.getShortName() + "]");
        lblNewLabel_9.setVerticalAlignment(SwingConstants.BOTTOM);
        lblNewLabel_9.setRotation(VerticalLabel.ROTATE_LEFT);
        panelCameraCalibration.add(lblNewLabel_9, "2, 58");
        
        modelErrorsTimeSequenceView = new SimpleGraphView();
        modelErrorsTimeSequenceView.setFont(new Font("Dialog", Font.PLAIN, 11));
        panelCameraCalibration.add(modelErrorsTimeSequenceView, "4, 58, 3, 1, fill, fill");
        
        String legend = "\r\n<p><body style=\"text-align:left\">"
                + "\r\n<p>\r\nX Residual "
                + "<span style=\"color:#FF0000\">&mdash;&mdash;</span>\r\n</p>"
                + "\r\n<p>\r\nY Residual "
                + "<span style=\"color:#00BB00\">&mdash;&mdash;</span>\r\n</p>"
                + "\r\n</body></p>\r\n";
        lblNewLabel_17 = new JLabel("<html><p width=\"500\">"
                + "This plot displays the residual location error (that is, the remaining error "
                + "after calibration has been applied) of each calibration point in the order it "
                + "was collected. The residual errors should have zero mean and appear as random "
                + "noise. If there are significant steps in the mean level or trends in the "
                + "errors; depending on their magnitude, there may be a problem with the "
                + "calibration. Some possible causes are: calibration rig movement/slippage during "
                + "the collection; camera or lens moving in its mount; motors missing steps; "
                + "belt/cog slippage; thermal expansion/contraction; etcetera."
                + "</p>" + legend + "</html>");
        panelCameraCalibration.add(lblNewLabel_17, "8, 58, 3, 1, left, default");
        
        lblNewLabel_10 = new JLabel("Collection Sequence Number");
        lblNewLabel_10.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_10, "4, 60, 3, 1");

        
        lblNewLabel_15 = new JLabel("Residual Error X-Y Scatter Plot");
        lblNewLabel_15.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblNewLabel_15.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_15, "4, 62, 3, 1");

        lblNewLabel_11 = new VerticalLabel("Y Residual Error [" + 
                smallDisplayUnits.getShortName() + "]");
        lblNewLabel_11.setVerticalAlignment(SwingConstants.BOTTOM);
        lblNewLabel_11.setRotation(VerticalLabel.ROTATE_LEFT);
        panelCameraCalibration.add(lblNewLabel_11, "2, 64");

        modelErrorsScatterPlotView = new SimpleGraphView();
        modelErrorsScatterPlotView.setFont(new Font("Dialog", Font.PLAIN, 11));
        panelCameraCalibration.add(modelErrorsScatterPlotView, "4, 64, 3, 1, fill, fill");
        
        lblNewLabel_18 = new JLabel("<html><p width=\"500\">"
                + "This plot displays the residual location error of each point collected "
                + "during the calibration process. The green circle marks the approximate boundary "
                + "at which points are considered to be outliers and are not used for determining "
                + "the camera calibration parameters. The residual errors should form a single "
                + "circular cluster centered at (0, 0) and should appear randomly distributed. If "
                + "two or more distinct clusters are present or the cluster is significantly "
                + "non-circular; depending on the magnitude of the errors, there may be a problem "
                + "with the calibration. Some possible causes are: bad vision detection of the "
                + "calibration fiducial, calibration rig movement/slippage during the collection; "
                + "camera or lens moving in its mount; under or over compensated backlash; motors "
                + "missing steps; belt/cog slippage; etcetera."
                + "</p></html>");
        panelCameraCalibration.add(lblNewLabel_18, "8, 64, 3, 1, left, default");
        
        lblNewLabel_12 = new JLabel("X Residual Error [" + 
                smallDisplayUnits.getShortName() + "]");
        lblNewLabel_12.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_12, "4, 66, 3, 1");
        

        lblNewLabel_16 = new JLabel("Residual Error Map");
        lblNewLabel_16.setFont(new Font("Tahoma", Font.BOLD, 13));
        lblNewLabel_16.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_16, "4, 68, 3, 1");

        lblNewLabel_13 = new VerticalLabel("Image Y Location [pixels]");
        lblNewLabel_13.setVerticalAlignment(SwingConstants.BOTTOM);
        lblNewLabel_13.setRotation(VerticalLabel.ROTATE_LEFT);
        panelCameraCalibration.add(lblNewLabel_13, "2, 70");

        modelErrorsView = new MatView();
        panelCameraCalibration.add(modelErrorsView, "4, 70, 3, 1, fill, fill");
        
        lblNewLabel_19 = new JLabel("<html><p width=\"500\">"
                + "This plot displays the magnitude of the residual location error as a "
                + "function of the expected location of the pixel in the image. Dark blue areas "
                + "have very low errors while dark red areas have the highest errors.  Note that "
                + "the color range is always scaled so that zero error is the darkest blue and "
                + "the maximum magnitude error is the darkest red. Therefore, this plot cannot "
                + "be used to judge the magnitude of the error but only its distribution about "
                + "the image. This distribution should look more or less random with no "
                + "discernible patterns. If patterns such as rings or stripes are clearly "
                + "visible and the residual errors observed in the other plots are large, the "
                + "mathematical model of the camera does not fit very well with the physics "
                + "of the camera and may indicate something is wrong with the camera and/or lens."
                + "</p></html>");
        panelCameraCalibration.add(lblNewLabel_19, "8, 70, 5, 1, left, default");
        
        lblNewLabel_12 = new JLabel("Image X Location [pixels]");
        lblNewLabel_12.setHorizontalAlignment(SwingConstants.CENTER);
        panelCameraCalibration.add(lblNewLabel_12, "4, 72, 3, 1");
        
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        LengthConverter lengthConverter = new LengthConverter();
        DoubleConverter doubleConverter = new DoubleConverter("%.3f");
        
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, 
                "overridingOldTransformsAndDistortionCorrectionSettings",
                chckbxAdvancedCalOverride, "selected");
        bind(UpdateStrategy.READ, advancedCalibration, "valid",
                chckbxEnable, "enabled");
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "enabled",
                chckbxEnable, "selected");
        bind(UpdateStrategy.READ, advancedCalibration, "valid",
                spinnerIndex, "enabled");
        
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "alphaPercent",
                sliderAlpha, "value");
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "fiducialDiameter",
                spinnerDiameter, "value");
        bind(UpdateStrategy.READ_WRITE, referenceCamera, "defaultZ", 
                textFieldDefaultZ, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, referenceCamera, "cropWidth", 
                textFieldCropWidth, "text", intConverter);
        bind(UpdateStrategy.READ_WRITE, referenceCamera, "cropHeight", 
                textFieldCropHeight, "text", intConverter);
        bind(UpdateStrategy.READ_WRITE, referenceCamera, "deinterlace", 
                checkboxDeinterlace, "selected");
        
        
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "approximateCameraZ", 
                textFieldApproximateCameraZ, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, this, "primaryZ", 
                textFieldPrimaryCalZ, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, this, "secondaryZ", 
                textFieldSecondaryCalZ, "text", lengthConverter);
        
        bind(UpdateStrategy.READ_WRITE, advancedCalibration, "desiredRadialLinesPerTestPattern",
                textFieldDesiredNumberOfRadialLines, "text", intConverter );
        bind(UpdateStrategy.READ, advancedCalibration, "rotationErrorZ",
                textFieldZRotationError, "text", doubleConverter);
        bind(UpdateStrategy.READ, advancedCalibration, "rotationErrorY",
                textFieldYRotationError, "text", doubleConverter);
        bind(UpdateStrategy.READ, advancedCalibration, "rotationErrorX",
                textFieldXRotationError, "text", doubleConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldApproximateCameraZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldDefaultZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPrimaryCalZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldSecondaryCalZ);
        ComponentDecorators.decorateWithAutoSelect(textFieldDesiredNumberOfRadialLines);
        
        enableControls(advancedCalibration.
                isOverridingOldTransformsAndDistortionCorrectionSettings());
        
        updateDiagnosticsDisplay();
    }

    private void enableControls(boolean b) {
        if (b) {
            chckbxEnable.setEnabled(advancedCalibration.isValid());
            checkboxDeinterlace.setEnabled(true);
            textFieldCropWidth.setEnabled(true);
            textFieldCropHeight.setEnabled(true);
            textFieldDefaultZ.setEnabled(isMovable);
            textFieldSecondaryCalZ.setEnabled(!isMovable);
            textFieldDesiredNumberOfRadialLines.setEnabled(true);
            textFieldApproximateCameraZ.setEnabled(true);
            startCameraCalibrationBtn.setEnabled(true);
            chckbxUseSavedData.setEnabled(advancedCalibration.isDataAvailable());
            sliderAlpha.setEnabled(true);
            spinnerIndex.setEnabled(advancedCalibration.isValid());
            chckbxShowOutliers.setEnabled(advancedCalibration.isValid());
        }
        else {
            chckbxEnable.setEnabled(false);
            checkboxDeinterlace.setEnabled(false);
            textFieldCropWidth.setEnabled(false);
            textFieldCropHeight.setEnabled(false);
            textFieldDefaultZ.setEnabled(false);
            textFieldSecondaryCalZ.setEnabled(false);
            textFieldDesiredNumberOfRadialLines.setEnabled(false);
            textFieldApproximateCameraZ.setEnabled(false);
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
            Logger.trace("thread = " + Thread.currentThread());
            
            ReferenceCamera defaultCamera = null;
            try {
                defaultCamera = (ReferenceCamera) referenceCamera.getHead().getDefaultCamera();
            }
            catch (Exception ex) {
                //TODO - what do we do if the default camera is not a ReferenceCamera???
            }
            if (isMovable && (referenceCamera != defaultCamera) && 
                    !defaultCamera.getAdvancedCalibration().isValid()) {
                MessageBoxes.errorBox(MainFrame.get(), "Default Camera Not Calibrated", 
                        "The default camera on this head must be calibrated first.");
                return;
            }
            
            startCameraCalibrationBtn.setEnabled(false);

            MainFrame.get().getCameraViews().setSelectedCamera(referenceCamera);

            calibrationHeights.clear();
            calibrationHeights.add(primaryZ);
            calibrationHeights.add(secondaryZ);
            
            boolean savedEnabledState = advancedCalibration.isEnabled();
            boolean savedValidState = advancedCalibration.isValid();
            advancedCalibration.setEnabled(false);
            advancedCalibration.setValid(false);
            chckbxEnable.setSelected(false);
            referenceCamera.clearCalibrationCache();
            
            if (!chckbxUseSavedData.isSelected()) {
                spinnerDiameter.setEnabled(true);
                
                CameraView cameraView = MainFrame.get().getCameraViews().
                        getCameraView(referenceCamera);
    
                UiUtils.messageBoxOnException(() -> {
                    new CalibrateCameraProcess(MainFrame.get(), cameraView, 
                            calibrationHeights) {
    
                        @Override 
                        public void processRawCalibrationData(double[][][] testPattern3dPointsList, 
                                double[][][] testPatternImagePointsList, Size size) throws Exception {
                            
                            Logger.trace("processing thread = " + Thread.currentThread());
                            
                            advancedCalibration.setDataAvailable(true);
                            chckbxUseSavedData.setEnabled(true);
                            
                            try {
                                advancedCalibration.processRawCalibrationData(
                                        testPattern3dPointsList, testPatternImagePointsList, 
                                        size);
                                
                                //Reload the calibration heights and refresh the table
                                calibrationHeights.clear();
                                calibrationHeightSelections = new ArrayList<LengthCellValue>();
                                int numberOfCalibrationHeights = advancedCalibration.
                                        getSavedTestPattern3dPointsList().length;
                                for (int i=0; i<numberOfCalibrationHeights; i++) {
                                    calibrationHeights.add( 
                                            new Length(advancedCalibration.
                                                    getSavedTestPattern3dPointsList()[i][0][2], 
                                                    LengthUnit.Millimeters));
                                    calibrationHeightSelections.add(new LengthCellValue(
                                            new Length(advancedCalibration.
                                                    getSavedTestPattern3dPointsList()[i][0][2], 
                                                    LengthUnit.Millimeters)));
                                }
                                
                                spinnerModel.setList(calibrationHeightSelections);
                                
                                advancedCalibration.setValid(true);
                                advancedCalibration.setEnabled(true);
                                chckbxEnable.setSelected(true);
                                chckbxEnable.setEnabled(true);
                                spinnerIndex.setEnabled(true);
                                chckbxShowOutliers.setEnabled(true);
                                
                                updateDiagnosticsDisplay();
                            }
                            catch (Exception e) {
                                MessageBoxes.errorBox(MainFrame.get(), "Error", e);
                                advancedCalibration.setValid(false);
                                advancedCalibration.setEnabled(false);
                                chckbxEnable.setSelected(false);
                                chckbxEnable.setEnabled(false);
                                chckbxUseSavedData.setEnabled(false);
                                spinnerIndex.setEnabled(false);
                                chckbxShowOutliers.setEnabled(false);
                            }
                            
                            startCameraCalibrationBtn.setEnabled(true);
                            spinnerDiameter.setEnabled(false);
                        }
    
                        @Override
                        protected void processCanceled() {
                            Logger.trace("cancelling thread = " + Thread.currentThread());
                            
                            advancedCalibration.setValid(savedValidState);
                            advancedCalibration.setEnabled(savedEnabledState);
                            chckbxEnable.setSelected(savedEnabledState);
                            chckbxEnable.setEnabled(savedValidState);
                            
                            startCameraCalibrationBtn.setEnabled(true);
                            spinnerDiameter.setEnabled(false);
                        }
                    };
                });
            }
            else {
                try {
                    referenceCamera.getAdvancedCalibration().processRawCalibrationData(
                            new Size(referenceCamera.getWidth(), referenceCamera.getHeight()));
                
                    advancedCalibration.setValid(true);
                    advancedCalibration.setEnabled(true);
                    chckbxEnable.setSelected(true);
                    chckbxEnable.setEnabled(true);
                    spinnerIndex.setEnabled(true);
                    chckbxShowOutliers.setEnabled(true);
                    
                    updateDiagnosticsDisplay();
                }
                catch (Exception ex) {
                    MessageBoxes.errorBox(MainFrame.get(), "Error", ex);
                    advancedCalibration.setValid(false);
                    advancedCalibration.setEnabled(false);
                    chckbxEnable.setSelected(false);
                    chckbxEnable.setEnabled(false);
                    spinnerIndex.setEnabled(false);
                    chckbxShowOutliers.setEnabled(false);
                }
                
                startCameraCalibrationBtn.setEnabled(true);
                spinnerDiameter.setEnabled(false);
            }
        }
    };

    private ChangeListener sliderAlphaChanged = new ChangeListener() {

        @Override
        public void stateChanged(ChangeEvent e) {
            if (!sliderAlpha.getValueIsAdjusting()) {
                int alphaPercent = (int)sliderAlpha.getValue();
                advancedCalibration.setAlphaPercent(alphaPercent);
                advancedCalibration.setVirtualCameraMatrix(
                    CameraCalibrationUtils.computeVirtualCameraMatrix(
                        advancedCalibration.getCameraMatrixMat(), 
                        advancedCalibration.getDistortionCoefficientsMat(),
                        advancedCalibration.getRectificationMatrix(), 
                        new Size(referenceCamera.getWidth(), referenceCamera.getHeight()), 
                        alphaPercent / 100.0, 
                        advancedCalibration.
                            getVectorFromPhyCamToDesiredPrincipalPointInPhyCamRefFrame()));
                referenceCamera.clearCalibrationCache();
                updateDiagnosticsDisplay();
            }
        }
        
    };
    
    private void updateDiagnosticsDisplay() {
        if (advancedCalibration.isValid()) {
            Location headOffsets = referenceCamera.getHeadOffsets().convertToUnits(displayUnits);
            textFieldXOffset.setText((new LengthCellValue(headOffsets.getLengthX())).toString());
            textFieldYOffset.setText((new LengthCellValue(headOffsets.getLengthY())).toString());
            textFieldZOffset.setText((new LengthCellValue(headOffsets.getLengthZ())).toString());
            
            Location upp = referenceCamera.getUnitsPerPixel(referenceCamera.getDefaultZ()).
                    convertToUnits(smallDisplayUnits);
            textFieldUnitsPerPixel.setText(
                    (new LengthCellValue(upp.getLengthX(), true)).toString());
            
            textFieldRmsError.setText((new LengthCellValue(upp.getLengthX().
                    multiply(advancedCalibration.getRmsError()), true)).toString());

            upp = referenceCamera.getUnitsPerPixel(calibrationHeightSelections.get(heightIndex).
                    getLength()).convertToUnits(smallDisplayUnits);
            double smallUnitsPerPixel = upp.getLengthX().getValue();
            
            List<double[]> residuals = null;
            try {
                if (chckbxShowOutliers.isSelected()) {
                    residuals = CameraCalibrationUtils.computeResidualErrors(
                        advancedCalibration.getSavedTestPatternImagePointsList(), 
                        advancedCalibration.getModeledImagePointsList(), heightIndex);
                }
                else {
                    residuals = CameraCalibrationUtils.computeResidualErrors(
                            advancedCalibration.getSavedTestPatternImagePointsList(), 
                            advancedCalibration.getModeledImagePointsList(), heightIndex,
                            advancedCalibration.getOutlierPointList());
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
                double radius = smallUnitsPerPixel * advancedCalibration.getRmsError() * 
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
                if (chckbxShowOutliers.isSelected()) {
                    errorImage = CameraCalibrationUtils.generateErrorImage(
                            new Size(referenceCamera.getWidth(), referenceCamera.getHeight()), 
                            heightIndex, 
                            advancedCalibration.getSavedTestPatternImagePointsList(), 
                            advancedCalibration.getModeledImagePointsList(), 
                            null);
                }
                else {
                    errorImage = CameraCalibrationUtils.generateErrorImage(
                            new Size(referenceCamera.getWidth(), referenceCamera.getHeight()), 
                            heightIndex, 
                            advancedCalibration.getSavedTestPatternImagePointsList(), 
                            advancedCalibration.getModeledImagePointsList(), 
                            advancedCalibration.getOutlierPointList());
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
    private JLabel lblNewLabel_38;
    private JTextField textFieldApproximateCameraZ;

}
