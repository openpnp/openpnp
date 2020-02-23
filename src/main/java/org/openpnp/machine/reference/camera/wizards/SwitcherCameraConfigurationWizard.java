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

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.ActuatorItem;
import org.openpnp.gui.support.CameraItem;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LongConverter;
import org.openpnp.machine.reference.camera.SwitcherCamera;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class SwitcherCameraConfigurationWizard extends AbstractConfigurationWizard {
    private final SwitcherCamera camera;
    private JLabel lblNewLabel;
    private JLabel lblNewLabel_1;
    private JLabel lblNewLabel_2;
    private JLabel lblNewLabel_3;
    private JComboBox sourceCamera;
    private JTextField switcher;
    private JComboBox actuator;
    private JTextField actuatorDoubleValue;
    private JLabel lblNewLabel_4;
    private JTextField actuatorDelayMillis;
    
    public SwitcherCameraConfigurationWizard(SwitcherCamera camera) {
        this.camera = camera;
        createUi();
    }
    private void createUi() {
        contentPanel.setLayout(new FormLayout(new ColumnSpec[] {
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
        
        lblNewLabel = new JLabel("Source Camera");
        contentPanel.add(lblNewLabel, "2, 2, right, default");
        
        sourceCamera = new JComboBox();
        contentPanel.add(sourceCamera, "4, 2, fill, default");
        
        lblNewLabel_1 = new JLabel("Switcher Number");
        contentPanel.add(lblNewLabel_1, "2, 4, right, default");
        
        switcher = new JTextField();
        contentPanel.add(switcher, "4, 4, fill, default");
        switcher.setColumns(10);
        
        lblNewLabel_2 = new JLabel("Actuator");
        contentPanel.add(lblNewLabel_2, "2, 6, right, default");
        
        actuator = new JComboBox();
        contentPanel.add(actuator, "4, 6, fill, default");
        
        lblNewLabel_4 = new JLabel("Actuator Delay (ms)");
        contentPanel.add(lblNewLabel_4, "2, 8, right, default");
        
        actuatorDelayMillis = new JTextField();
        contentPanel.add(actuatorDelayMillis, "4, 8, fill, default");
        actuatorDelayMillis.setColumns(10);
        
        lblNewLabel_3 = new JLabel("Actuator Value");
        contentPanel.add(lblNewLabel_3, "2, 10, right, default");
        
        actuatorDoubleValue = new JTextField();
        contentPanel.add(actuatorDoubleValue, "4, 10, fill, default");
        actuatorDoubleValue.setColumns(10);
        
        for (Camera camera : Configuration.get().getMachine().getCameras()) {
            sourceCamera.addItem(camera);
        }
        
        for (Actuator actuator : Configuration.get().getMachine().getActuators()) {
            this.actuator.addItem(actuator);
        }
    }

    @Override
    public void createBindings() {
        LongConverter longConverter = new LongConverter();
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter("%f");
        addWrappedBinding(camera, "camera", sourceCamera, "selectedItem");
        addWrappedBinding(camera, "switcher", switcher, "text", intConverter);
        addWrappedBinding(camera, "actuator", actuator, "selectedItem");
        addWrappedBinding(camera, "actuatorDelayMillis", actuatorDelayMillis, "text", longConverter);
        addWrappedBinding(camera, "actuatorDoubleValue", actuatorDoubleValue, "text", doubleConverter);
        
        ComponentDecorators.decorateWithAutoSelect(switcher);
        ComponentDecorators.decorateWithAutoSelect(actuatorDelayMillis);
        ComponentDecorators.decorateWithAutoSelect(actuatorDoubleValue);
    }
}
