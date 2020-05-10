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

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AxesComboBoxModel;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.gui.wizards.AbstractAxisConfigurationWizard;
import org.openpnp.machine.reference.axis.ReferenceMappedAxis;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Axis;
import org.openpnp.spi.base.AbstractControllerAxis;
import org.openpnp.spi.base.AbstractMachine;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.spi.Axis.Type;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class ReferenceMappedAxisConfigurationWizard extends AbstractAxisConfigurationWizard {

    private JPanel panelTransformation;
    private JLabel lblInputAxis;
    private JComboBox inputAxis;
    private AxesComboBoxModel inputAxisModel;
    private JLabel lblFrom;
    private JLabel lblTo;
    private JTextField mapInput0;
    private JTextField mapOutput0;
    private JTextField mapInput1;
    private JTextField mapOutput1;
    private JLabel lblMapPointA;
    private JLabel lblMapPointB;
    private JLabel label;
    private JLabel label_1;

    public ReferenceMappedAxisConfigurationWizard(AbstractMachine machine, ReferenceMappedAxis axis) {
        super(axis);
        panelTransformation = new JPanel();
        panelTransformation.setBorder(new TitledBorder(null, "Axis Mapping", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelTransformation);
        panelTransformation.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
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
                RowSpec.decode("bottom:default:grow"),}));

        lblInputAxis = new JLabel("Input Axis");
        panelTransformation.add(lblInputAxis, "2, 2, right, default");

        inputAxisModel = new AxesComboBoxModel(machine, AbstractControllerAxis.class, null, true);
        inputAxis = new JComboBox(inputAxisModel);
        panelTransformation.add(inputAxis, "4, 2, fill, default");
        
        lblFrom = new JLabel("Input");
        panelTransformation.add(lblFrom, "4, 4, center, default");
        
        lblTo = new JLabel("Output");
        panelTransformation.add(lblTo, "8, 4, center, default");
        
        lblMapPointA = new JLabel("Map Point A");
        lblMapPointA.setToolTipText("<html>Choose two exemplary points on the axis that you like to map.<br/>\r\nSet the desired Input and Output to create an Offset, Scaling, Negating (etc.).<br/>\r\nNote, the range will not be limited to these points they are just examples.\r\n</html>");
        panelTransformation.add(lblMapPointA, "2, 6, right, default");
        
        mapInput0 = new JTextField();
        panelTransformation.add(mapInput0, "4, 6, fill, default");
        mapInput0.setColumns(10);
        
        label = new JLabel("→");
        panelTransformation.add(label, "6, 6, right, default");
        
        mapOutput0 = new JTextField();
        panelTransformation.add(mapOutput0, "8, 6, fill, default");
        mapOutput0.setColumns(10);
        
        lblMapPointB = new JLabel("Map Point B");
        panelTransformation.add(lblMapPointB, "2, 8, right, default");
        
        mapInput1 = new JTextField();
        panelTransformation.add(mapInput1, "4, 8, fill, default");
        mapInput1.setColumns(10);
        
        label_1 = new JLabel("→");
        panelTransformation.add(label_1, "6, 8, right, default");
        
        mapOutput1 = new JTextField();
        panelTransformation.add(mapOutput1, "8, 8, fill, center");
        mapOutput1.setColumns(10);
        initDataBindings();
    }

    @Override
    public void createBindings() {
        super.createBindings();
        AbstractMachine machine = (AbstractMachine) Configuration.get().getMachine();
        NamedConverter<Axis> axisConverter = new NamedConverter<>(machine.getAxes()); 
        LengthConverter lengthConverter = new LengthConverter();
        
        addWrappedBinding(axis, "inputAxis", inputAxis, "selectedItem", axisConverter);
        addWrappedBinding(getAxis(), "mapInput0", mapInput0, "text", lengthConverter);
        addWrappedBinding(getAxis(), "mapOutput0", mapOutput0, "text", lengthConverter);
        addWrappedBinding(getAxis(), "mapInput1", mapInput1, "text", lengthConverter);
        addWrappedBinding(getAxis(), "mapOutput1", mapOutput1, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(mapInput0);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(mapOutput0);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(mapInput1);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(mapOutput1);
    }

    protected void initDataBindings() {
        BeanProperty<JComboBox, Axis.Type> jComboBoxBeanProperty = BeanProperty.create("selectedItem");
        BeanProperty<AxesComboBoxModel, Type> axesComboBoxModelBeanProperty = BeanProperty.create("axisType");
        AutoBinding<JComboBox, Axis.Type, AxesComboBoxModel, Axis.Type> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ, type, jComboBoxBeanProperty, inputAxisModel, axesComboBoxModelBeanProperty);
        autoBinding.bind();
    }
}
