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

package org.openpnp.machine.reference.axis.wizards;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.support.AxesComboBoxModel;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.machine.reference.axis.ReferenceCamCounterClockwiseAxis;
import org.openpnp.machine.reference.axis.ReferenceCamClockwiseAxis;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.base.AbstractMachine;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceCamClockwiseAxisConfigurationWizard extends AbstractAxisConfigurationWizard {

    private JPanel panelTransformation;
    private JLabel lblInputAxis;
    private JComboBox inputAxis;
    private AxesComboBoxModel inputAxisModel;

    public ReferenceCamClockwiseAxisConfigurationWizard(AbstractMachine machine, ReferenceCamClockwiseAxis axis) {
        super(axis);
        panelTransformation = new JPanel();
        panelTransformation.setBorder(new TitledBorder(null, "Cam Clockwise Axis", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelTransformation);
        panelTransformation.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("default:grow(2)"),
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("bottom:default:grow"),
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblInputAxis = new JLabel("Counter-Clockwise Axis");
        panelTransformation.add(lblInputAxis, "2, 2, right, default");

        inputAxisModel = new AxesComboBoxModel(machine, ReferenceCamCounterClockwiseAxis.class, null, true);
        inputAxis = new JComboBox(inputAxisModel);
        panelTransformation.add(inputAxis, "4, 2, fill, default");
        initDataBindings();
    }

    @Override
    public void createBindings() {
        super.createBindings();
        AbstractMachine machine = (AbstractMachine) Configuration.get().getMachine();

        NamedConverter<Axis> axisConverter = new NamedConverter<>(machine.getAxes()); 

        addWrappedBinding(axis, "inputAxis", inputAxis, "selectedItem", axisConverter);
    }

    protected void initDataBindings() {
        BeanProperty<JComboBox, Axis.Type> jComboBoxBeanProperty = BeanProperty.create("selectedItem");
        BeanProperty<AxesComboBoxModel, Type> axesComboBoxModelBeanProperty = BeanProperty.create("axisType");
        AutoBinding<JComboBox, Axis.Type, AxesComboBoxModel, Axis.Type> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, type, jComboBoxBeanProperty, inputAxisModel, axesComboBoxModelBeanProperty);
        autoBinding.bind();
    }
}
