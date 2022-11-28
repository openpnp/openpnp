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

package org.openpnp.machine.reference.camera.wizards;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.Translations;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.camera.SimulatedUpCamera;
import org.openpnp.model.Configuration;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class SimulatedUpCameraConfigurationWizard extends AbstractConfigurationWizard {
    private final SimulatedUpCamera camera;

    private JPanel panelGeneral;
    private JButton btnBrowse;
    private JLabel lblErrorOffsets;
    private JTextField errorOffsetsX;
    private JTextField errorOffsetsY;
    private JTextField errorOffsetsZ;
    private JTextField errorOffsetsRotation;
    private JLabel lblX;
    private JLabel lblY;
    private JLabel lblZ;
    private JLabel lblRotation;
    private JLabel lblCameraDimension;
    private JTextField width;
    private JTextField height;
    private JLabel lblFocalBlur;
    private JCheckBox simulateFocalBlur;
    private JLabel lblNewLabel;
    private JTextField simulatedLocationX;
    private JTextField simulatedLocationY;
    private JTextField simulatedLocationZ;
    private JLabel lblUnitsPerPixel;
    private JTextField simulatedUnitsPerPixelX;
    private JTextField simulatedUnitsPerPixelY;
    private JTextField simulatedUnitsPerPixelZ;
    private JTextField simulatedLocationRotation;
    private JLabel lblCameraFlipped;
    private JCheckBox simulatedFlipped;
    private JLabel lblFocalLength;
    private JTextField focalLength;
    private JLabel lblSensorDiagonal;
    private JTextField sensorDiagonal;

    public SimulatedUpCameraConfigurationWizard(SimulatedUpCamera camera) {
        this.camera = camera;

        panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        panelGeneral.setBorder(new TitledBorder(null, Translations.getString(
                "SimulatedUpCameraConfigurationWizard.GeneralPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null));
        panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblX = new JLabel("X");
        panelGeneral.add(lblX, "4, 2, center, default");
        
        lblY = new JLabel("Y");
        panelGeneral.add(lblY, "6, 2, center, default");
        
        lblZ = new JLabel("Z");
        panelGeneral.add(lblZ, "8, 2, center, default");
        
        lblRotation = new JLabel(Translations.getString(
                "SimulatedUpCameraConfigurationWizard.GeneralPanel.RotationLabel.text")); //$NON-NLS-1$
        panelGeneral.add(lblRotation, "10, 2, center, default");
        
        lblNewLabel = new JLabel(Translations.getString(
                "SimulatedUpCameraConfigurationWizard.GeneralPanel.CameraLocationLabel.text")); //$NON-NLS-1$
        lblNewLabel.setToolTipText(Translations.getString(
                "SimulatedUpCameraConfigurationWizard.GeneralPanel.CameraLocationLabel.toolTipText")); //$NON-NLS-1$
        panelGeneral.add(lblNewLabel, "2, 4, right, default");
        
        simulatedLocationX = new JTextField();
        panelGeneral.add(simulatedLocationX, "4, 4, fill, default");
        simulatedLocationX.setColumns(10);
        
        simulatedLocationY = new JTextField();
        panelGeneral.add(simulatedLocationY, "6, 4, fill, default");
        simulatedLocationY.setColumns(10);
        
        simulatedLocationZ = new JTextField();
        panelGeneral.add(simulatedLocationZ, "8, 4, fill, default");
        simulatedLocationZ.setColumns(10);
        
        simulatedLocationRotation = new JTextField();
        panelGeneral.add(simulatedLocationRotation, "10, 4, fill, default");
        simulatedLocationRotation.setColumns(10);
        
        lblCameraDimension = new JLabel(Translations.getString(
                "SimulatedUpCameraConfigurationWizard.GeneralPanel.PixelDimensionLabel.text")); //$NON-NLS-1$
        panelGeneral.add(lblCameraDimension, "2, 6, right, default");
        
        width = new JTextField();
        panelGeneral.add(width, "4, 6, fill, default");
        width.setColumns(10);
        
        height = new JTextField();
        panelGeneral.add(height, "6, 6, fill, default");
        height.setColumns(10);
        
        lblUnitsPerPixel = new JLabel(Translations.getString(
                "SimulatedUpCameraConfigurationWizard.GeneralPanel.UnitsPerPixelLabel.text")); //$NON-NLS-1$
        lblUnitsPerPixel.setToolTipText(Translations.getString(
                "SimulatedUpCameraConfigurationWizard.GeneralPanel.UnitsPerPixelLabel.toolTipText")); //$NON-NLS-1$
        panelGeneral.add(lblUnitsPerPixel, "2, 8, right, default");
        
        simulatedUnitsPerPixelX = new JTextField();
        panelGeneral.add(simulatedUnitsPerPixelX, "4, 8, fill, default");
        simulatedUnitsPerPixelX.setColumns(10);
        
        simulatedUnitsPerPixelY = new JTextField();
        simulatedUnitsPerPixelY.setText("");
        panelGeneral.add(simulatedUnitsPerPixelY, "6, 8, fill, default");
        simulatedUnitsPerPixelY.setColumns(10);
        
        lblFocalLength = new JLabel(Translations.getString(
                "SimulatedUpCameraConfigurationWizard.GeneralPanel.FocalLengthLabel.text")); //$NON-NLS-1$
        panelGeneral.add(lblFocalLength, "2, 10, right, default");
        
        focalLength = new JTextField();
        panelGeneral.add(focalLength, "4, 10, fill, default");
        focalLength.setColumns(10);
        
        lblSensorDiagonal = new JLabel(Translations.getString(
                "SimulatedUpCameraConfigurationWizard.GeneralPanel.SensorDiagonalLabel.text")); //$NON-NLS-1$
        panelGeneral.add(lblSensorDiagonal, "2, 12, right, default");
        
        sensorDiagonal = new JTextField();
        panelGeneral.add(sensorDiagonal, "4, 12, fill, default");
        sensorDiagonal.setColumns(10);
        
        lblErrorOffsets = new JLabel(Translations.getString(
                "SimulatedUpCameraConfigurationWizard.GeneralPanel.PickErrorOffsetsLabel.text")); //$NON-NLS-1$
        lblErrorOffsets.setToolTipText(Translations.getString(
                "SimulatedUpCameraConfigurationWizard.GeneralPanel.PickErrorOffsetsLabel.toolTipText")); //$NON-NLS-1$
        panelGeneral.add(lblErrorOffsets, "2, 16, right, default");
        
        errorOffsetsX = new JTextField();
        panelGeneral.add(errorOffsetsX, "4, 16, fill, default");
        errorOffsetsX.setColumns(10);
        
        errorOffsetsY = new JTextField();
        panelGeneral.add(errorOffsetsY, "6, 16, fill, default");
        errorOffsetsY.setColumns(10);
        
        errorOffsetsZ = new JTextField();
        panelGeneral.add(errorOffsetsZ, "8, 16, fill, default");
        errorOffsetsZ.setColumns(10);
        
        errorOffsetsRotation = new JTextField();
        panelGeneral.add(errorOffsetsRotation, "10, 16, fill, default");
        errorOffsetsRotation.setColumns(10);
        
        lblCameraFlipped = new JLabel(Translations.getString(
                "SimulatedUpCameraConfigurationWizard.GeneralPanel.MirroredViewLabel.text")); //$NON-NLS-1$
        lblCameraFlipped.setToolTipText(Translations.getString(
                "SimulatedUpCameraConfigurationWizard.GeneralPanel.MirroredViewLabel.toolTipText")); //$NON-NLS-1$
        panelGeneral.add(lblCameraFlipped, "2, 20, right, default");
        
        simulatedFlipped = new JCheckBox("");
        panelGeneral.add(simulatedFlipped, "4, 20");
        
        lblFocalBlur = new JLabel(Translations.getString(
                "SimulatedUpCameraConfigurationWizard.GeneralPanel.FocalBlurLabel.text")); //$NON-NLS-1$
        lblFocalBlur.setToolTipText(Translations.getString(
                "SimulatedUpCameraConfigurationWizard.GeneralPanel.FocalBlurLabel.toolTipText")); //$NON-NLS-1$
        panelGeneral.add(lblFocalBlur, "2, 22, right, default");
        
        simulateFocalBlur = new JCheckBox("");
        panelGeneral.add(simulateFocalBlur, "4, 22");
    }

    @Override
    public void createBindings() {
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        IntegerConverter intConverter = new IntegerConverter();
        LengthConverter lengthConverter = new LengthConverter();
        LengthConverter uppConverter = new LengthConverter("%.6f");

        addWrappedBinding(camera, "viewWidth", width, "text", intConverter);
        addWrappedBinding(camera, "viewHeight", height, "text", intConverter);

        addWrappedBinding(camera, "simulatedFlipped", simulatedFlipped, "selected");
        addWrappedBinding(camera, "simulateFocalBlur", simulateFocalBlur, "selected");

        MutableLocationProxy errorOffsets = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, camera, "errorOffsets", errorOffsets,
                "location");
        addWrappedBinding(errorOffsets, "lengthX", errorOffsetsX, "text", lengthConverter);
        addWrappedBinding(errorOffsets, "lengthY", errorOffsetsY, "text", lengthConverter);
        addWrappedBinding(errorOffsets, "lengthZ", errorOffsetsZ, "text", lengthConverter);
        addWrappedBinding(errorOffsets, "rotation", errorOffsetsRotation, "text",
                doubleConverter);

        MutableLocationProxy simulatedLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, camera, "simulatedLocation", simulatedLocation,
                "location");
        addWrappedBinding(simulatedLocation, "lengthX", simulatedLocationX, "text", lengthConverter);
        addWrappedBinding(simulatedLocation, "lengthY", simulatedLocationY, "text", lengthConverter);
        addWrappedBinding(simulatedLocation, "lengthZ", simulatedLocationZ, "text", lengthConverter);
        addWrappedBinding(simulatedLocation, "rotation", simulatedLocationRotation, "text", doubleConverter);

        MutableLocationProxy simulatedUnitsPerPixel = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, camera, "simulatedUnitsPerPixel", simulatedUnitsPerPixel,
                "location");
        addWrappedBinding(simulatedUnitsPerPixel, "lengthX", simulatedUnitsPerPixelX, "text", uppConverter);
        addWrappedBinding(simulatedUnitsPerPixel, "lengthY", simulatedUnitsPerPixelY, "text", uppConverter);

        addWrappedBinding(camera, "focalLength", focalLength, "text", lengthConverter);
        addWrappedBinding(camera, "sensorDiagonal", sensorDiagonal, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelect(width);
        ComponentDecorators.decorateWithAutoSelect(height);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(errorOffsetsX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(errorOffsetsY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(errorOffsetsZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(errorOffsetsRotation);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(simulatedLocationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(simulatedLocationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(simulatedLocationZ);
        ComponentDecorators.decorateWithAutoSelect(simulatedLocationRotation);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(focalLength);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(sensorDiagonal);

        //ComponentDecorators.decorateWithAutoSelect(simulatedUnitsPerPixelX);
        //ComponentDecorators.decorateWithAutoSelect(simulatedUnitsPerPixelY);
    }
}
