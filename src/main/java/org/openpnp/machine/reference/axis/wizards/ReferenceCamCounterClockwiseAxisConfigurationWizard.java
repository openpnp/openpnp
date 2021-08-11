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
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AxesComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.machine.reference.axis.ReferenceCamCounterClockwiseAxis;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.base.AbstractControllerAxis;
import org.openpnp.spi.base.AbstractMachine;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceCamCounterClockwiseAxisConfigurationWizard extends AbstractAxisConfigurationWizard {

    private JPanel panelTransformation;
    private JLabel lblInputAxis;
    private JComboBox inputAxis;
    private JTextField camWheelRadius;
    private JLabel lblCamWheelRadius;
    private JLabel lblCamRadius;
    private JTextField camRadius;
    private JLabel lblCamWheelGap;
    private JTextField camWheelGap;
    private JLabel lbIllustration;
    private JLabel labelSpacer;
    private AxesComboBoxModel inputAxisModel;
    private JLabel lblArmsAngle;
    private JTextField camArmsAngle;

    public ReferenceCamCounterClockwiseAxisConfigurationWizard(AbstractMachine machine, ReferenceCamCounterClockwiseAxis axis) {
        super(axis);
        panelTransformation = new JPanel();
        panelTransformation.setBorder(new TitledBorder(null, "Cam Counter-Clockwise Axis", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelTransformation);
        panelTransformation.setLayout(new FormLayout(new ColumnSpec[] {
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
                RowSpec.decode("bottom:default"),}));
        
        lblInputAxis = new JLabel("Input Axis");
        panelTransformation.add(lblInputAxis, "2, 2, right, default");
        inputAxisModel = new AxesComboBoxModel(machine, AbstractControllerAxis.class, null, true);
        inputAxis = new JComboBox(inputAxisModel);
        panelTransformation.add(inputAxis, "4, 2, fill, default");
        
        lblCamRadius = new JLabel("Cam Radius");
        panelTransformation.add(lblCamRadius, "2, 4, right, default");
        
        camRadius = new JTextField();
        panelTransformation.add(camRadius, "4, 4, fill, default");
        camRadius.setColumns(10);
        
        lblArmsAngle = new JLabel("Cam Arms Angle");
        lblArmsAngle.setToolTipText("Angle between the two arms");
        panelTransformation.add(lblArmsAngle, "2, 6, right, default");
        
        camArmsAngle = new JTextField();
        panelTransformation.add(camArmsAngle, "4, 6, fill, default");
        camArmsAngle.setColumns(10);
        
        lblCamWheelRadius = new JLabel("Cam Wheel Radius");
        panelTransformation.add(lblCamWheelRadius, "2, 8, right, default");
        
        camWheelRadius = new JTextField();
        panelTransformation.add(camWheelRadius, "4, 8, fill, default");
        camWheelRadius.setColumns(10);
        
        lblCamWheelGap = new JLabel("Cam Wheel Gap");
        panelTransformation.add(lblCamWheelGap, "2, 10, right, default");
        
        camWheelGap = new JTextField();
        panelTransformation.add(camWheelGap, "4, 10, fill, default");
        camWheelGap.setColumns(10);

        labelSpacer = new JLabel(" ");
        panelTransformation.add(labelSpacer, "6, 10");
        lbIllustration = new JLabel(Icons.comAxisTransform);
        panelTransformation.add(lbIllustration, "2, 12, 5, 1");
        initDataBindings();
    }
    
    @Override
    public void createBindings() {
        super.createBindings();
        AbstractMachine machine = (AbstractMachine) Configuration.get().getMachine();
        
        LengthConverter lengthConverter = new LengthConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        NamedConverter<Axis> axisConverter = new NamedConverter<>(machine.getAxes()); 
        
        addWrappedBinding(axis, "inputAxis", inputAxis, "selectedItem", axisConverter);
        addWrappedBinding(axis, "camRadius", camRadius, "text", lengthConverter);
        addWrappedBinding(axis, "camArmsAngle", camArmsAngle, "text", doubleConverter);
        addWrappedBinding(axis, "camWheelRadius", camWheelRadius, "text", lengthConverter);
        addWrappedBinding(axis, "camWheelGap", camWheelGap, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(camRadius);
        ComponentDecorators.decorateWithAutoSelect(camArmsAngle);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(camWheelRadius);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(camWheelGap);
    }

    protected void initDataBindings() {
        BeanProperty<JComboBox, Axis.Type> jComboBoxBeanProperty = BeanProperty.create("selectedItem");
        BeanProperty<AxesComboBoxModel, Type> axesComboBoxModelBeanProperty = BeanProperty.create("axisType");
        AutoBinding<JComboBox, Axis.Type, AxesComboBoxModel, Axis.Type> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, type, jComboBoxBeanProperty, inputAxisModel, axesComboBoxModelBeanProperty);
        autoBinding.bind();
    }
}
