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
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
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
    private JLabel lblCameraWidth;
    private JTextField width;
    private JLabel lblHeight;
    private JTextField height;
    private JLabel lblFocalBlur;
    private JCheckBox simulateFocalBlur;

    public SimulatedUpCameraConfigurationWizard(SimulatedUpCamera camera) {
        this.camera = camera;

        panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        panelGeneral.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "General", TitledBorder.LEADING, TitledBorder.TOP, null));
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblCameraWidth = new JLabel("Width");
        panelGeneral.add(lblCameraWidth, "2, 2, right, default");
        
        width = new JTextField();
        panelGeneral.add(width, "4, 2, fill, default");
        width.setColumns(10);
        
        lblHeight = new JLabel("Height");
        panelGeneral.add(lblHeight, "2, 4, right, default");
        
        height = new JTextField();
        panelGeneral.add(height, "4, 4, fill, default");
        height.setColumns(10);
        
        lblFocalBlur = new JLabel("Simulate Focal Blur?");
        lblFocalBlur.setToolTipText("Simulate focal blur in order to test Auto Focus. This is very slow!");
        panelGeneral.add(lblFocalBlur, "2, 6, right, default");
        
        simulateFocalBlur = new JCheckBox("");
        panelGeneral.add(simulateFocalBlur, "4, 6");
        
        lblX = new JLabel("X");
        panelGeneral.add(lblX, "4, 8, center, default");
        
        lblY = new JLabel("Y");
        panelGeneral.add(lblY, "6, 8, center, default");
        
        lblZ = new JLabel("Z");
        panelGeneral.add(lblZ, "8, 8, center, default");
        
        lblRotation = new JLabel("Rotation");
        panelGeneral.add(lblRotation, "10, 8, center, default");
        
        lblErrorOffsets = new JLabel("Error Offsets");
        panelGeneral.add(lblErrorOffsets, "2, 10, right, default");
        
        errorOffsetsX = new JTextField();
        panelGeneral.add(errorOffsetsX, "4, 10, fill, default");
        errorOffsetsX.setColumns(10);
        
        errorOffsetsY = new JTextField();
        panelGeneral.add(errorOffsetsY, "6, 10, fill, default");
        errorOffsetsY.setColumns(10);
        
        errorOffsetsZ = new JTextField();
        panelGeneral.add(errorOffsetsZ, "8, 10, fill, default");
        errorOffsetsZ.setColumns(10);
        
        errorOffsetsRotation = new JTextField();
        panelGeneral.add(errorOffsetsRotation, "10, 10, fill, default");
        errorOffsetsRotation.setColumns(10);
    }

    @Override
    public void createBindings() {
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        IntegerConverter intConverter = new IntegerConverter();
        LengthConverter lengthConverter = new LengthConverter();

        addWrappedBinding(camera, "width", width, "text", intConverter);
        addWrappedBinding(camera, "height", height, "text", intConverter);

        addWrappedBinding(camera, "simulateFocalBlur", simulateFocalBlur, "selected");

        MutableLocationProxy errorOffsets = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, camera, "errorOffsets", errorOffsets,
                "location");
        addWrappedBinding(errorOffsets, "lengthX", errorOffsetsX, "text", lengthConverter);
        addWrappedBinding(errorOffsets, "lengthY", errorOffsetsY, "text", lengthConverter);
        addWrappedBinding(errorOffsets, "lengthZ", errorOffsetsZ, "text", lengthConverter);
        addWrappedBinding(errorOffsets, "rotation", errorOffsetsRotation, "text",
                doubleConverter);
        
        ComponentDecorators.decorateWithAutoSelect(width);
        ComponentDecorators.decorateWithAutoSelect(height);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(errorOffsetsX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(errorOffsetsY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(errorOffsetsZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(errorOffsetsRotation);
    }
}
