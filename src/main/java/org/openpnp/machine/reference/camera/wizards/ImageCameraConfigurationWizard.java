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

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.AbstractAction;
import javax.swing.Action;
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
import org.openpnp.machine.reference.camera.ImageCamera;
import org.openpnp.model.Configuration;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ImageCameraConfigurationWizard extends AbstractConfigurationWizard {
    private final ImageCamera camera;


    public ImageCameraConfigurationWizard(ImageCamera camera) {
        this.camera = camera;

        panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        panelGeneral.setBorder(new TitledBorder(null,
                Translations.getString("ImageCameraConfigurationWizard.GeneralPanel.Border.title"), //$NON-NLS-1$
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
                ColumnSpec.decode("default:grow"),
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblWidth = new JLabel("X");
        panelGeneral.add(lblWidth, "4, 2, center, default");
        
                lblHeight = new JLabel("Y");
                panelGeneral.add(lblHeight, "6, 2, center, default");
        
        lblDimension = new JLabel(Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.PixelDimensionLabel.text")); //$NON-NLS-1$
        panelGeneral.add(lblDimension, "2, 4, right, default");

        width = new JTextField();
        panelGeneral.add(width, "4, 4, fill, default");
        width.setColumns(10);
        
                height = new JTextField();
                panelGeneral.add(height, "6, 4, fill, default");
                height.setColumns(10);
        
        label_1 = new JLabel(" ");
        panelGeneral.add(label_1, "10, 4");
        
        lblUnitsPerPixel = new JLabel(Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.UnitsPerPixelLabel.text")); //$NON-NLS-1$
        panelGeneral.add(lblUnitsPerPixel, "2, 6, right, default");
        lblUnitsPerPixel.setToolTipText(Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.UnitsPerPixelLabel.toolTipText")); //$NON-NLS-1$
        
        imageUnitsPerPixelX = new JTextField();
        panelGeneral.add(imageUnitsPerPixelX, "4, 6");
        imageUnitsPerPixelX.setColumns(10);
        
        imageUnitsPerPixelY = new JTextField();
        panelGeneral.add(imageUnitsPerPixelY, "6, 6");
        imageUnitsPerPixelY.setColumns(10);

        lblRotation = new JLabel(Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.ZRotationLabel.text")); //$NON-NLS-1$
        lblRotation.setToolTipText(Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.ZRotationLabel.toolTipText")); //$NON-NLS-1$
        panelGeneral.add(lblRotation, "2, 8, right, default");

        simulatedRotation = new JTextField();
        panelGeneral.add(simulatedRotation, "4, 8, fill, default");
        simulatedRotation.setColumns(10);
        
        lblYaw = new JLabel(Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.YRotationLabel.text")); //$NON-NLS-1$
        lblYaw.setToolTipText(Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.YRotationLabel.toolTipText")); //$NON-NLS-1$
        panelGeneral.add(lblYaw, "2, 10, right, default");
        
        simulatedYRotation = new JTextField();
        panelGeneral.add(simulatedYRotation, "4, 10, fill, default");
        simulatedYRotation.setColumns(10);
        
        lblScale = new JLabel(Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.ViewingScaleLabel.text")); //$NON-NLS-1$
        panelGeneral.add(lblScale, "2, 12, right, default");
        
        simulatedScale = new JTextField();
        panelGeneral.add(simulatedScale, "4, 12, fill, default");
        simulatedScale.setColumns(10);
        
        lblDistortion = new JLabel(Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.DistortionLabel.text")); //$NON-NLS-1$
        lblDistortion.setToolTipText(Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.DistortionLabel.toolTipText")); //$NON-NLS-1$
        panelGeneral.add(lblDistortion, "2, 14, right, default");
        
        simulatedDistortion = new JTextField();
        panelGeneral.add(simulatedDistortion, "4, 14, fill, default");
        simulatedDistortion.setColumns(10);


        lblCameraFlipped = new JLabel(Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.MirroredViewLabel.text")); //$NON-NLS-1$
        lblCameraFlipped.setToolTipText(Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.MirroredViewLabel.toolTipText")); //$NON-NLS-1$
        panelGeneral.add(lblCameraFlipped, "2, 16, right, default");

        simulatedFlipped = new JCheckBox("");
        panelGeneral.add(simulatedFlipped, "4, 16");

        label = new JLabel(" ");
        panelGeneral.add(label, "8, 16");

        lblSourceUrl = new JLabel(Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.SourceUrlLabel.text")); //$NON-NLS-1$
        panelGeneral.add(lblSourceUrl, "2, 20, right, default");

        textFieldSourceUrl = new JTextField();
        panelGeneral.add(textFieldSourceUrl, "4, 20, 7, 1, fill, default");
        textFieldSourceUrl.setColumns(40);

        btnBrowse = new JButton(browseAction);
        panelGeneral.add(btnBrowse, "12, 20");
        
        panelExtra = new JPanel();
        contentPanel.add(panelExtra);
        panelExtra.setBorder(new TitledBorder(null, Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.ExtraPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelExtra.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblFocalLength = new JLabel(Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.ExtraPanel.FocalLengthLabel.text")); //$NON-NLS-1$
        lblFocalLength.setToolTipText(Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.ExtraPanel.FocalLengthLabel.toolTipText")); //$NON-NLS-1$
        panelExtra.add(lblFocalLength, "2, 2, right, default");
        
        focalLength = new JTextField();
        panelExtra.add(focalLength, "4, 2");
        focalLength.setColumns(10);
        
        lblSensorDiagonal = new JLabel(Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.ExtraPanel.SensorDiagonalLabel.text")); //$NON-NLS-1$
        lblSensorDiagonal.setToolTipText(Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.ExtraPanel.SensorDiagonalLabel.toolTipText")); //$NON-NLS-1$
        panelExtra.add(lblSensorDiagonal, "6, 2, right, default");
        
        sensorDiagonal = new JTextField();
        panelExtra.add(sensorDiagonal, "8, 2");
        sensorDiagonal.setColumns(10);
        lblX = new JLabel("X");
        panelExtra.add(lblX, "4, 6, center, default");
        
        lblY = new JLabel("Y");
        panelExtra.add(lblY, "6, 6, center, default");
        
        lblZ = new JLabel("Z");
        panelExtra.add(lblZ, "8, 6, center, default");
        
        lblPrimaryFiducial = new JLabel(Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.ExtraPanel.PrimaryFiducialLabel.text")); //$NON-NLS-1$
        panelExtra.add(lblPrimaryFiducial, "2, 8, right, default");
        
        primaryFiducialX = new JTextField();
        panelExtra.add(primaryFiducialX, "4, 8, fill, default");
        primaryFiducialX.setColumns(10);
        
        primaryFiducialY = new JTextField();
        panelExtra.add(primaryFiducialY, "6, 8, fill, default");
        primaryFiducialY.setColumns(10);
        
        primaryFiducialZ = new JTextField();
        panelExtra.add(primaryFiducialZ, "8, 8, fill, default");
        primaryFiducialZ.setColumns(10);
        
        lblSecondaryFiducial = new JLabel(Translations.getString(
                "ImageCameraConfigurationWizard.GeneralPanel.ExtraPanel.SecondaryFiducialLabel.text")); //$NON-NLS-1$
        panelExtra.add(lblSecondaryFiducial, "2, 10, right, default");
        
        secondaryFiducialX = new JTextField();
        panelExtra.add(secondaryFiducialX, "4, 10, fill, default");
        secondaryFiducialX.setColumns(10);
        
        secondaryFiducialY = new JTextField();
        panelExtra.add(secondaryFiducialY, "6, 10, fill, default");
        secondaryFiducialY.setColumns(10);
        
        secondaryFiducialZ = new JTextField();
        panelExtra.add(secondaryFiducialZ, "8, 10, fill, default");
        secondaryFiducialZ.setColumns(10);
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

        addWrappedBinding(camera, "simulatedRotation", simulatedRotation, "text", doubleConverter);
        addWrappedBinding(camera, "simulatedScale", simulatedScale, "text", doubleConverter);
        addWrappedBinding(camera, "simulatedDistortion", simulatedDistortion, "text", doubleConverter);
        addWrappedBinding(camera, "simulatedYRotation", simulatedYRotation, "text", doubleConverter);
        addWrappedBinding(camera, "simulatedFlipped", simulatedFlipped, "selected");

        addWrappedBinding(camera, "sourceUri", textFieldSourceUrl, "text");

        MutableLocationProxy imageUnitsPerPixel = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, camera, "imageUnitsPerPixel", imageUnitsPerPixel, "location");
        addWrappedBinding(imageUnitsPerPixel, "lengthX", imageUnitsPerPixelX, "text", uppConverter);
        addWrappedBinding(imageUnitsPerPixel, "lengthY", imageUnitsPerPixelY, "text", uppConverter);

        addWrappedBinding(camera, "focalLength", focalLength, "text", lengthConverter);
        addWrappedBinding(camera, "sensorDiagonal", sensorDiagonal, "text", lengthConverter);

        MutableLocationProxy primaryFiducial = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, camera, "primaryFiducial", primaryFiducial, "location");
        addWrappedBinding(primaryFiducial, "lengthX", primaryFiducialX, "text", lengthConverter);
        addWrappedBinding(primaryFiducial, "lengthY", primaryFiducialY, "text", lengthConverter);
        addWrappedBinding(primaryFiducial, "lengthZ", primaryFiducialZ, "text", lengthConverter);

        MutableLocationProxy secondaryFiducial = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, camera, "secondaryFiducial", secondaryFiducial, "location");
        addWrappedBinding(secondaryFiducial, "lengthX", secondaryFiducialX, "text", lengthConverter);
        addWrappedBinding(secondaryFiducial, "lengthY", secondaryFiducialY, "text", lengthConverter);
        addWrappedBinding(secondaryFiducial, "lengthZ", secondaryFiducialZ, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelect(width);
        ComponentDecorators.decorateWithAutoSelect(height);
        ComponentDecorators.decorateWithAutoSelect(simulatedRotation);
        ComponentDecorators.decorateWithAutoSelect(simulatedScale);

        ComponentDecorators.decorateWithAutoSelect(textFieldSourceUrl);
        ComponentDecorators.decorateWithAutoSelect(imageUnitsPerPixelX);
        ComponentDecorators.decorateWithAutoSelect(imageUnitsPerPixelY);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(focalLength);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(sensorDiagonal);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(primaryFiducialX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(primaryFiducialY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(primaryFiducialZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(secondaryFiducialX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(secondaryFiducialY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(secondaryFiducialZ);
    }

    private Action browseAction = new AbstractAction() {
        {
            putValue(NAME, Translations.getString("ImageCameraConfigurationWizard.Action.Browse")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString(
                    "ImageCameraConfigurationWizard.Action.Browse.Description")); //$NON-NLS-1$
        }

        public void actionPerformed(ActionEvent e) {
            FileDialog fileDialog = new FileDialog((Frame) getTopLevelAncestor());
            fileDialog.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    String[] extensions = new String[] {".png", ".jpg", ".gif", ".tif", ".tiff"};
                    for (String extension : extensions) {
                        if (name.toLowerCase().endsWith(extension)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            fileDialog.setVisible(true);
            if (fileDialog.getFile() == null) {
                return;
            }
            File file = new File(new File(fileDialog.getDirectory()), fileDialog.getFile());
            textFieldSourceUrl.setText(file.toURI().toString());
        }
    };
    private JPanel panelGeneral;
    private JLabel lblWidth;
    private JLabel lblHeight;
    private JTextField width;
    private JTextField height;
    private JLabel lblSourceUrl;
    private JTextField textFieldSourceUrl;
    private JButton btnBrowse;
    private JLabel lblCameraFlipped;
    private JCheckBox simulatedFlipped;
    private JLabel lblRotation;
    private JTextField simulatedRotation;
    private JLabel label;
    private JLabel lblUnitsPerPixel;
    private JTextField imageUnitsPerPixelX;
    private JTextField imageUnitsPerPixelY;
    private JLabel label_1;
    private JLabel lblScale;
    private JTextField simulatedScale;
    private JPanel panelExtra;
    private JLabel lblX;
    private JLabel lblY;
    private JLabel lblFocalLength;
    private JTextField focalLength;
    private JLabel lblPrimaryFiducial;
    private JLabel lblSecondaryFiducial;
    private JTextField primaryFiducialX;
    private JTextField primaryFiducialY;
    private JTextField primaryFiducialZ;
    private JLabel lblZ;
    private JTextField secondaryFiducialX;
    private JTextField secondaryFiducialY;
    private JTextField secondaryFiducialZ;
    private JLabel lblSensorDiagonal;
    private JTextField sensorDiagonal;
    private JLabel lblDimension;
    private JLabel lblDistortion;
    private JTextField simulatedDistortion;
    private JLabel lblYaw;
    private JTextField simulatedYRotation;

    @Override
    protected void saveToModel() {
        super.saveToModel();
        UiUtils.messageBoxOnException(() -> {
            camera.reinitialize(); 
        });
    }
}
