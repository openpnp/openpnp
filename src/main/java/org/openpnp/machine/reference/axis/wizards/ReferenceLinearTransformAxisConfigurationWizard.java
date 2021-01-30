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
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Axis;
import org.openpnp.spi.LinearInputAxis;
import org.openpnp.spi.base.AbstractControllerAxis;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.AbstractTransformedAxis;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JTextField;
import javax.swing.JCheckBox;

@SuppressWarnings("serial")
public class ReferenceLinearTransformAxisConfigurationWizard extends AbstractAxisConfigurationWizard {
    protected JPanel panelTransformation;
    protected JComboBox inputAxisX;
    protected JLabel lblInputAxisY;
    protected JComboBox inputAxisY;
    protected JLabel lblInputAxisX;
    protected JLabel lblInputAxisZ;
    protected JComboBox inputAxisZ;
    protected JLabel lblInputAxisRotation;
    protected JComboBox inputAxisRotation;
    private JTextField factorX;
    private JTextField factorY;
    private JTextField factorZ;
    private JTextField factorRotation;
    private JLabel lblInput;
    private JLabel lblFactor;
    private JLabel lblOffset;
    private JTextField offset;
    private JLabel label;
    private JLabel label_1;
    private JLabel label_2;
    private JLabel label_3;
    private JLabel label_5;
    private JLabel label_6;
    private JLabel label_7;
    private JLabel label_8;
    private JLabel lblCompensation;
    private JCheckBox compensation;

    public ReferenceLinearTransformAxisConfigurationWizard(AbstractMachine machine, AbstractTransformedAxis axis) {
        super(axis);

        panelTransformation = new JPanel();
        panelTransformation.setBorder(new TitledBorder(null, "Linear Transformation", TitledBorder.LEADING, TitledBorder.TOP, null, null));
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblInput = new JLabel("Input");
        panelTransformation.add(lblInput, "4, 2, center, default");

        lblFactor = new JLabel("Factor");
        panelTransformation.add(lblFactor, "8, 2, center, default");

        lblInputAxisX = new JLabel("X");
        panelTransformation.add(lblInputAxisX, "2, 4, right, default");

        inputAxisX = new JComboBox(new AxesComboBoxModel(machine, LinearInputAxis.class, Axis.Type.X, true));
        panelTransformation.add(inputAxisX, "4, 4, fill, default");

        label = new JLabel(" × ");
        panelTransformation.add(label, "6, 4, center, default");

        factorX = new JTextField();
        panelTransformation.add(factorX, "8, 4, fill, top");
        factorX.setColumns(10);

        label_5 = new JLabel("+");
        panelTransformation.add(label_5, "8, 6, center, default");

        lblInputAxisY = new JLabel("Y");
        panelTransformation.add(lblInputAxisY, "2, 8, right, default");

        inputAxisY = new JComboBox(new AxesComboBoxModel(machine, LinearInputAxis.class, Axis.Type.Y, true));
        panelTransformation.add(inputAxisY, "4, 8, fill, default");

        label_1 = new JLabel("×");
        panelTransformation.add(label_1, "6, 8, center, default");

        factorY = new JTextField();
        panelTransformation.add(factorY, "8, 8, fill, default");
        factorY.setColumns(10);

        label_6 = new JLabel("+");
        panelTransformation.add(label_6, "8, 10, center, default");

        lblInputAxisZ = new JLabel("Z");
        panelTransformation.add(lblInputAxisZ, "2, 12, right, default");

        inputAxisZ = new JComboBox(new AxesComboBoxModel(machine, LinearInputAxis.class, Axis.Type.Z, true));
        panelTransformation.add(inputAxisZ, "4, 12, fill, default");

        label_2 = new JLabel("×");
        panelTransformation.add(label_2, "6, 12, center, default");

        factorZ = new JTextField();
        panelTransformation.add(factorZ, "8, 12, fill, default");
        factorZ.setColumns(10);

        label_7 = new JLabel("+");
        panelTransformation.add(label_7, "8, 14, center, default");

        lblInputAxisRotation = new JLabel("Rotation");
        panelTransformation.add(lblInputAxisRotation, "2, 16, right, default");

        inputAxisRotation = new JComboBox(new AxesComboBoxModel(machine, LinearInputAxis.class, Axis.Type.Rotation, true));
        panelTransformation.add(inputAxisRotation, "4, 16, fill, default");

        label_3 = new JLabel("×");
        panelTransformation.add(label_3, "6, 16, center, default");

        factorRotation = new JTextField();
        panelTransformation.add(factorRotation, "8, 16, fill, default");
        factorRotation.setColumns(10);

        label_8 = new JLabel("+");
        panelTransformation.add(label_8, "8, 18, center, default");

        lblOffset = new JLabel("Offset");
        panelTransformation.add(lblOffset, "4, 20, right, default");

        offset = new JTextField();
        panelTransformation.add(offset, "8, 20, fill, default");
        offset.setColumns(10);

        lblCompensation = new JLabel("Compensation?");
        lblCompensation.setToolTipText("<html>Determines, whether this transformation is a compensation transformation e.g. Non-Squareness Compensation.<br/>\r\nThis can be used by OpenPnP to optimize some motion where precision is not needed. Initial calibration <br/>and simulation are other applications. \r\n</html>");
        panelTransformation.add(lblCompensation, "2, 24, right, default");

        compensation = new JCheckBox("");
        panelTransformation.add(compensation, "4, 24");
    }

    @Override
    public void createBindings() {
        super.createBindings();
        AbstractMachine machine = (AbstractMachine) Configuration.get().getMachine();
        LengthConverter lengthConverter = new LengthConverter();
        DoubleConverter doubleConverter =
                new DoubleConverter("%f");//Configuration.get().getLengthDisplayFormat());
        NamedConverter<Axis> axisConverter = new NamedConverter<>(machine.getAxes()); 

        addWrappedBinding(getAxis(), "inputAxisX", inputAxisX, "selectedItem", axisConverter);
        addWrappedBinding(getAxis(), "inputAxisY", inputAxisY, "selectedItem", axisConverter);
        addWrappedBinding(getAxis(), "inputAxisZ", inputAxisZ, "selectedItem", axisConverter);
        addWrappedBinding(getAxis(), "inputAxisRotation", inputAxisRotation, "selectedItem", axisConverter);

        addWrappedBinding(getAxis(), "factorX", factorX, "text", doubleConverter);
        addWrappedBinding(getAxis(), "factorY", factorY, "text", doubleConverter);
        addWrappedBinding(getAxis(), "factorZ", factorZ, "text", doubleConverter);
        addWrappedBinding(getAxis(), "factorRotation", factorRotation, "text", doubleConverter);
        addWrappedBinding(getAxis(), "offset", offset, "text", lengthConverter);

        addWrappedBinding(getAxis(), "compensation", compensation, "selected");

        ComponentDecorators.decorateWithAutoSelect(factorX);
        ComponentDecorators.decorateWithAutoSelect(factorY);
        ComponentDecorators.decorateWithAutoSelect(factorZ);
        ComponentDecorators.decorateWithAutoSelect(factorRotation);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(offset);
    }
}
