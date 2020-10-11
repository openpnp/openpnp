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

package org.openpnp.machine.reference.wizards;

import java.awt.Color;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.AxesComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.DriversComboBoxModel;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.base.AbstractAxis;
import org.openpnp.spi.base.AbstractMachine;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;

@SuppressWarnings("serial")
public abstract class AbstractActuatorConfigurationWizard extends AbstractConfigurationWizard {
    protected final ReferenceActuator actuator;

    private JTextField locationX;
    private JTextField locationY;
    private JTextField locationZ;
    private JPanel panelOffsets;
    private JPanel panelSafeZ;
    private JLabel lblSafeZ;
    private JTextField textFieldSafeZ;
    private JPanel headMountablePanel;
    private JPanel generalPanel;
    private JLabel lblIndex;
    private JTextField indexTextField;
    private JComboBox axisX;
    private JComboBox axisY;
    private JComboBox axisZ;
    private JLabel lblRotation;
    private JComboBox axisRotation;
    private JTextField locationRotation;
    private JLabel lblAxis;
    private JLabel lblOffset;

    private JPanel panelCoordination;
    private JCheckBox coordinatedBeforeActuate;
    private JLabel lblBeforeActuation;
    private JLabel lblAfterActuation;
    private JCheckBox coordinatedAfterActuate;
    private JLabel lblBeforeRead;
    private JCheckBox coordinatedBeforeRead;

    public AbstractActuatorConfigurationWizard(AbstractMachine machine, ReferenceActuator actuator) {
        this.actuator = actuator;
        createUi(machine);
    }
    
    protected void createUi(AbstractMachine machine) {
        headMountablePanel = new JPanel();
        headMountablePanel.setLayout(new BoxLayout(headMountablePanel, BoxLayout.Y_AXIS));
        contentPanel.add(headMountablePanel);

        panelOffsets = new JPanel();
        headMountablePanel.add(panelOffsets);
        panelOffsets.setBorder(new TitledBorder(null,
                "Coordinate System", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelOffsets.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblX = new JLabel("X");
        panelOffsets.add(lblX, "4, 2");

        JLabel lblY = new JLabel("Y");
        panelOffsets.add(lblY, "6, 2");

        JLabel lblZ = new JLabel("Z");
        panelOffsets.add(lblZ, "8, 2");

        lblRotation = new JLabel("Rotation");
        panelOffsets.add(lblRotation, "10, 2, left, default");

        lblAxis = new JLabel("Axis");
        panelOffsets.add(lblAxis, "2, 4, right, default");

        axisX = new JComboBox(new AxesComboBoxModel(machine, AbstractAxis.class, Axis.Type.X, true));
        panelOffsets.add(axisX, "4, 4, fill, default");

        axisY = new JComboBox(new AxesComboBoxModel(machine, AbstractAxis.class, Axis.Type.Y, true));
        panelOffsets.add(axisY, "6, 4, fill, default");

        axisZ = new JComboBox(new AxesComboBoxModel(machine, AbstractAxis.class, Axis.Type.Z, true));
        panelOffsets.add(axisZ, "8, 4, fill, default");

        axisRotation = new JComboBox(new AxesComboBoxModel(machine, AbstractAxis.class, Axis.Type.Rotation, true));
        panelOffsets.add(axisRotation, "10, 4, fill, default");

        lblOffset = new JLabel("Offset");
        panelOffsets.add(lblOffset, "2, 6, right, default");

        locationX = new JTextField();
        panelOffsets.add(locationX, "4, 6");
        locationX.setColumns(10);

        locationY = new JTextField();
        panelOffsets.add(locationY, "6, 6");
        locationY.setColumns(10);

        locationZ = new JTextField();
        panelOffsets.add(locationZ, "8, 6");
        locationZ.setColumns(10);
        
        locationRotation = new JTextField();
        panelOffsets.add(locationRotation, "10, 6, fill, default");
        locationRotation.setColumns(10);

        panelSafeZ = new JPanel();
        headMountablePanel.add(panelSafeZ);
        panelSafeZ.setBorder(new TitledBorder(null, "Safe Z", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panelSafeZ.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        lblSafeZ = new JLabel("Safe Z");
        panelSafeZ.add(lblSafeZ, "2, 2, right, default");

        textFieldSafeZ = new JTextField();
        textFieldSafeZ.setEditable(false);
        panelSafeZ.add(textFieldSafeZ, "4, 2, fill, default");
        textFieldSafeZ.setColumns(10);
        
        generalPanel = new JPanel();
        generalPanel.setBorder(new TitledBorder(null, "General", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(generalPanel);
        generalPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblIndex = new JLabel("Index");
        generalPanel.add(lblIndex, "2, 2, right, default");
        
        indexTextField = new JTextField();
        generalPanel.add(indexTextField, "4, 2, fill, default");
        indexTextField.setColumns(10);
        
        panelCoordination = new JPanel();
        headMountablePanel.add(panelCoordination);
        panelCoordination.setBorder(new TitledBorder(null, "Machine Coordination", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panelCoordination.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblBeforeActuation = new JLabel("Before Actuation?");
        lblBeforeActuation.setToolTipText("<html>\r\nCoordinate with the machine, before the actuator is actuated, i.e. wait for the controllers <br/>\r\nto acknowledge that all the pending commands (including motion) were sent and executed. \r\n</html>");
        panelCoordination.add(lblBeforeActuation, "2, 2, right, default");
        
        coordinatedBeforeActuate = new JCheckBox("");
        panelCoordination.add(coordinatedBeforeActuate, "4, 2, center, bottom");
        
        lblAfterActuation = new JLabel("After Actuation?");
        lblAfterActuation.setToolTipText("<html>\r\nCoordinate with the machine, after the actuator was actuated, i.e. wait for the controllers <br/>\r\nto acknowledge that the actuation as well as all the pending commands (including motion)<br/>\r\nwere sent and executed and any position report processed.\r\n</html>");
        panelCoordination.add(lblAfterActuation, "2, 4, right, default");
        
        coordinatedAfterActuate = new JCheckBox("");
        panelCoordination.add(coordinatedAfterActuate, "4, 4");
        
        lblBeforeRead = new JLabel("Before Read?");
        lblBeforeRead.setToolTipText("<html>\r\nCoordinate with the machine, before the actuator is read, i.e. wait for the controllers <br/>\r\nto acknowledge that all the pending commands (including motion) were sent and executed. \r\n</html>");
        panelCoordination.add(lblBeforeRead, "2, 6, right, default");
        
        coordinatedBeforeRead = new JCheckBox("");
        panelCoordination.add(coordinatedBeforeRead, "4, 6");

        if (actuator.getHead() == null) {
            headMountablePanel.setVisible(false);
        }
    }

    @Override
    public void createBindings() {
        AbstractMachine machine = (AbstractMachine) Configuration.get().getMachine();
        LengthConverter lengthConverter = new LengthConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        IntegerConverter intConverter = new IntegerConverter();
        NamedConverter<Axis> axisConverter = new NamedConverter<>(machine.getAxes()); 

        addWrappedBinding(actuator, "axisX", axisX, "selectedItem", axisConverter);
        addWrappedBinding(actuator, "axisY", axisY, "selectedItem", axisConverter);
        addWrappedBinding(actuator, "axisZ", axisZ, "selectedItem", axisConverter);
        addWrappedBinding(actuator, "axisRotation", axisRotation, "selectedItem", axisConverter);

        MutableLocationProxy headOffsets = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, actuator, "headOffsets", headOffsets, "location");
        addWrappedBinding(headOffsets, "lengthX", locationX, "text", lengthConverter);
        addWrappedBinding(headOffsets, "lengthY", locationY, "text", lengthConverter);
        addWrappedBinding(headOffsets, "lengthZ", locationZ, "text", lengthConverter);
        addWrappedBinding(headOffsets, "rotation", locationRotation, "text", doubleConverter);

        addWrappedBinding(actuator, "safeZ", textFieldSafeZ, "text", lengthConverter);

        addWrappedBinding(actuator, "coordinatedBeforeActuate", coordinatedBeforeActuate, "selected");
        addWrappedBinding(actuator, "coordinatedAfterActuate", coordinatedAfterActuate, "selected");
        addWrappedBinding(actuator, "coordinatedBeforeRead", coordinatedBeforeRead, "selected");

        addWrappedBinding(actuator, "index", indexTextField, "text", intConverter);

        ComponentDecorators.decorateWithAutoSelect(indexTextField);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationZ);
        ComponentDecorators.decorateWithAutoSelect(locationRotation);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldSafeZ);
    }
}
