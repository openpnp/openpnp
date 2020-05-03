/*
 * Copyright (C) 2020 <mark@makr.zone>
 * inspired and based on work
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

package org.openpnp.gui.wizards;

import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.support.AxesComboBoxModel;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Axis;
import org.openpnp.spi.base.AbstractControllerAxis;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.AbstractTransformedAxis;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.ComboBoxModel;

@SuppressWarnings("serial")
public abstract class AbstractTransformedAxisConfigurationWizard extends AbstractAxisConfigurationWizard {
    protected final AbstractMachine machine;

    protected JPanel panelTransformation;
    protected JComboBox inputAxisX;
    protected JLabel lblInputAxisY;
    protected JComboBox inputAxisY;
    protected JLabel lblInputAxisX;
    protected JLabel lblInputAxisZ;
    protected JComboBox inputAxisZ;
    protected JLabel lblInputAxisRotation;
    protected JComboBox inputAxisRotation;

    public AbstractTransformedAxisConfigurationWizard(AbstractMachine machine) {
        super();
        this.machine = machine;

        panelTransformation = new JPanel();
        panelTransformation.setBorder(new TitledBorder(null, getTransformationSettingsTitle(), TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelTransformation);
        panelTransformation.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblInputAxisX = new JLabel("Input X");
        panelTransformation.add(lblInputAxisX, "2, 2, right, default");
        
        AbstractTransformedAxis axis = (AbstractTransformedAxis) getAxis();
        inputAxisX = new JComboBox(new AxesComboBoxModel(machine, axis.getInputAxesClass(), true));
        panelTransformation.add(inputAxisX, "4, 2, fill, default");
        
        lblInputAxisY = new JLabel("Input Y");
        panelTransformation.add(lblInputAxisY, "2, 4, right, default");
        
        inputAxisY = new JComboBox(new AxesComboBoxModel(machine, axis.getInputAxesClass(), true));
        panelTransformation.add(inputAxisY, "4, 4, fill, default");
        
        lblInputAxisZ = new JLabel("Input Z");
        panelTransformation.add(lblInputAxisZ, "2, 6, right, default");
        
        inputAxisZ = new JComboBox(new AxesComboBoxModel(machine, axis.getInputAxesClass(), true));
        panelTransformation.add(inputAxisZ, "4, 6, fill, default");
        
        lblInputAxisRotation = new JLabel("Input Rotation");
        panelTransformation.add(lblInputAxisRotation, "2, 8, right, default");
        
        inputAxisRotation = new JComboBox(new AxesComboBoxModel(machine, axis.getInputAxesClass(), true));
        panelTransformation.add(inputAxisRotation, "4, 8, fill, default");
    }

    protected abstract String getTransformationSettingsTitle();

    @Override
    public void createBindings() {
        super.createBindings();
        NamedConverter<Axis> axisConverter = new NamedConverter<>(machine.getAxes()); 
        
        addWrappedBinding(getAxis(), "inputAxisX", inputAxisX, "selectedItem", axisConverter);
        addWrappedBinding(getAxis(), "inputAxisY", inputAxisY, "selectedItem", axisConverter);
        addWrappedBinding(getAxis(), "inputAxisZ", inputAxisZ, "selectedItem", axisConverter);
        addWrappedBinding(getAxis(), "inputAxisRotation", inputAxisRotation, "selectedItem", axisConverter);
    }
}
