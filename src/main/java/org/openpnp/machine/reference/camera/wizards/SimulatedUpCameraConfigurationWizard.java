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

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.camera.SimulatedUpCamera;
import org.openpnp.machine.reference.wizards.ReferenceCameraConfigurationWizard;
import org.openpnp.model.Configuration;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class SimulatedUpCameraConfigurationWizard extends ReferenceCameraConfigurationWizard {
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

    public SimulatedUpCameraConfigurationWizard(SimulatedUpCamera camera) {
        super(camera);

        this.camera = camera;

        panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        panelGeneral.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "General", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblX = new JLabel("X");
        panelGeneral.add(lblX, "4, 2, center, default");
        
        lblY = new JLabel("Y");
        panelGeneral.add(lblY, "6, 2, center, default");
        
        lblZ = new JLabel("Z");
        panelGeneral.add(lblZ, "8, 2, center, default");
        
        lblRotation = new JLabel("Rotation");
        panelGeneral.add(lblRotation, "10, 2, center, default");
        
        lblErrorOffsets = new JLabel("Error Offsets");
        panelGeneral.add(lblErrorOffsets, "2, 4, right, default");
        
        errorOffsetsX = new JTextField();
        panelGeneral.add(errorOffsetsX, "4, 4, fill, default");
        errorOffsetsX.setColumns(8);
        
        errorOffsetsY = new JTextField();
        panelGeneral.add(errorOffsetsY, "6, 4, fill, default");
        errorOffsetsY.setColumns(8);
        
        errorOffsetsZ = new JTextField();
        panelGeneral.add(errorOffsetsZ, "8, 4, fill, default");
        errorOffsetsZ.setColumns(8);
        
        errorOffsetsRotation = new JTextField();
        panelGeneral.add(errorOffsetsRotation, "10, 4, fill, default");
        errorOffsetsRotation.setColumns(8);
    }

    @Override
    public void createBindings() {
        super.createBindings();
        
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        LengthConverter lengthConverter = new LengthConverter();

        MutableLocationProxy errorOffsets = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, camera, "errorOffsets", errorOffsets,
                "location");
        addWrappedBinding(errorOffsets, "lengthX", errorOffsetsX, "text", lengthConverter);
        addWrappedBinding(errorOffsets, "lengthY", errorOffsetsY, "text", lengthConverter);
        addWrappedBinding(errorOffsets, "lengthZ", errorOffsetsZ, "text", lengthConverter);
        addWrappedBinding(errorOffsets, "rotation", errorOffsetsRotation, "text",
                doubleConverter);
        
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(errorOffsetsX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(errorOffsetsY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(errorOffsetsZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(errorOffsetsRotation);
    }
}
