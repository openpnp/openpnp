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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.AxesComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Actuator.ActuatorValueType;
import org.openpnp.spi.base.AbstractAxis;
import org.openpnp.spi.base.AbstractMachine;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

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

    private boolean reloadWizard;
    private JLabel lblAxisInterlock;
    private JCheckBox interlockActuator;
    private JLabel lblValueType;
    protected JComboBox valueType;
    private JTextField defaultOnDouble;
    private JLabel lblOnDouble;
    private JLabel lblOffDouble;
    private JTextField defaultOffDouble;
    private JLabel lblOnString;
    private JLabel lblOffString;
    private JTextField defaultOnString;
    private JTextField defaultOffString;
    private JLabel lblHomingActuation;
    private JComboBox homedActuation;
    private JLabel lblEnableActuation;
    private JLabel lblDisableActuation;
    private JComboBox enabledActuation;
    private JComboBox disabledActuation;
    private JLabel lblMachineStateActuation;
    private JLabel lblMachineState;

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
                Translations.getStringOrDefault("AbstractActuatorConfigurationWizard.CoordinateSystemPanel.Border.title",
                        "Coordinate System"), TitledBorder.LEADING, TitledBorder.TOP, null));
        panelOffsets.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblX = new JLabel("X");
        panelOffsets.add(lblX, "4, 2");

        JLabel lblY = new JLabel("Y");
        panelOffsets.add(lblY, "6, 2");

        JLabel lblZ = new JLabel("Z");
        panelOffsets.add(lblZ, "8, 2");

        lblRotation = new JLabel(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.CoordinateSystemPanel.RotationLabel.text", "Rotation"));
        panelOffsets.add(lblRotation, "10, 2, left, default");

        lblAxis = new JLabel(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.CoordinateSystemPanel.AxisLabel.text", "Axis"));
        panelOffsets.add(lblAxis, "2, 4, right, default");

        axisX = new JComboBox(new AxesComboBoxModel(machine, AbstractAxis.class, Axis.Type.X, true));
        panelOffsets.add(axisX, "4, 4, fill, default");

        axisY = new JComboBox(new AxesComboBoxModel(machine, AbstractAxis.class, Axis.Type.Y, true));
        panelOffsets.add(axisY, "6, 4, fill, default");

        axisZ = new JComboBox(new AxesComboBoxModel(machine, AbstractAxis.class, Axis.Type.Z, true));
        panelOffsets.add(axisZ, "8, 4, fill, default");

        axisRotation = new JComboBox(new AxesComboBoxModel(machine, AbstractAxis.class, Axis.Type.Rotation, true));
        panelOffsets.add(axisRotation, "10, 4, fill, default");

        lblOffset = new JLabel(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.CoordinateSystemPanel.OffsetLabel.text", "Offset"));
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
        
        lblAxisInterlock = new JLabel(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.CoordinateSystemPanel.AxisInterlockLabel.text",
                "Axis Interlock?"));
        lblAxisInterlock.setToolTipText(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.CoordinateSystemPanel.AxisInterlockLabel.toolTip.text",
                "Enable to get an extra Wizard tab to configure an Axis Interlocking Actuator"));
        panelOffsets.add(lblAxisInterlock, "2, 8, right, default");
        
        interlockActuator = new JCheckBox("");
        interlockActuator.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                reloadWizard = true;
            }
        });
        panelOffsets.add(interlockActuator, "4, 8");

        panelSafeZ = new JPanel();
        headMountablePanel.add(panelSafeZ);
        panelSafeZ.setBorder(new TitledBorder(null, Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.SafeZPanel.Border.title", "Safe Z"),
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panelSafeZ.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblSafeZ = new JLabel(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.SafeZPanel.SafeZLabel.text", "Safe Z"));
        panelSafeZ.add(lblSafeZ, "2, 2, right, default");

        textFieldSafeZ = new JTextField();
        textFieldSafeZ.setEditable(false);
        panelSafeZ.add(textFieldSafeZ, "4, 2, fill, default");
        textFieldSafeZ.setColumns(10);
        
        panelCoordination = new JPanel();
        contentPanel.add(panelCoordination);
        panelCoordination.setBorder(new TitledBorder(null, Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.CoordinationPanel.Border.title",
                "Machine Coordination"), TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panelCoordination.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblBeforeActuation = new JLabel(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.CoordinationPanel.BeforeActuationLabel.text",
                "Before Actuation?"));
        lblBeforeActuation.setToolTipText(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.CoordinationPanel.BeforeActuationLabel.toolTip.text",
                "<html>\n" +
                        "Coordinate with the machine, before the actuator is actuated, i.e. wait for the controllers <br/>\n" +
                        "to acknowledge that all the pending commands (including motion) were sent and executed. \n" +
                        "</html>"));
        panelCoordination.add(lblBeforeActuation, "2, 2, right, default");
        
        coordinatedBeforeActuate = new JCheckBox("");
        panelCoordination.add(coordinatedBeforeActuate, "4, 2, center, bottom");
        
        lblAfterActuation = new JLabel(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.CoordinationPanel.AfterActuationLabel.text",
                "After Actuation?"));
        lblAfterActuation.setToolTipText(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.CoordinationPanel.AfterActuationLabel.toolTip.text",
                "<html>\n" +
                        "Coordinate with the machine, after the actuator was actuated, i.e. wait for the controllers <br/>\n" +
                        "to acknowledge that the actuation as well as all the pending commands (including motion)<br/>\n" +
                        "were sent and executed and any position report processed.\n" +
                        "</html>"));
        panelCoordination.add(lblAfterActuation, "2, 4, right, default");
        
        coordinatedAfterActuate = new JCheckBox("");
        panelCoordination.add(coordinatedAfterActuate, "4, 4");
        
        lblBeforeRead = new JLabel(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.CoordinationPanel.BeforeReadLabel.text", "Before Read?"));
        lblBeforeRead.setToolTipText(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.CoordinationPanel.BeforeReadLabel.toolTip.text",
                "<html>\n" +
                        "Coordinate with the machine, before the actuator is read, i.e. wait for the controllers <br/>\n" +
                        "to acknowledge that all the pending commands (including motion) were sent and executed. \n" +
                        "</html>"));
        panelCoordination.add(lblBeforeRead, "2, 6, right, default");
        
        coordinatedBeforeRead = new JCheckBox("");
        panelCoordination.add(coordinatedBeforeRead, "4, 6");
        
        generalPanel = new JPanel();
        generalPanel.setBorder(new TitledBorder(null, Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.GeneralPanel.Border.title", "General"),
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(generalPanel);
        generalPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
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
        
        lblValueType = new JLabel(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.GeneralPanel.ValueTypeLabel.text", "Value Type"));
        lblValueType.setToolTipText(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.GeneralPanel.ValueTypeLabel.toolTip.text",
                "<html>\n" +
                        "<p>\n" +
                        "Determines the primary data type of Actuator write values. \n" +
                        "</p>\n" +
                        "<ul>\n" +
                        "<li><strong>Boolean:</strong><br/>ON/OFF switching Actuator.</li>\n" +
                        "<li><strong>Double:</strong><br/>Numeric Actuator to drive scalar values.</li>\n" +
                        "<li><strong>String:</strong><br/>Textual Actuator to drive arbitrary codes and values.</li>\n" +
                        "<li><strong>Profile:</strong><br/>Multiple-choice Actuator that can define a number of named profiles<br/>\n" +
                        "and drive other Actuators.<br/>\n" +
                        "Press Apply to enable the Profiles configuration Wizard.</li>\n" +
                        "</ul>\n" +
                        "<strong>Note:</strong> the primary data type will not be enforced in the operation of the actuator.<br/>\n" +
                        "Mixed type usage is still possible (for backwards compatibility). \n" +
                        "</html>"
        ));
        generalPanel.add(lblValueType, "2, 2, right, default");
        
        valueType = new JComboBox(Actuator.ActuatorValueType.values());
        valueType.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                reloadWizard = true;
                adaptDialog();
            }
        });
        generalPanel.add(valueType, "4, 2, fill, default");
        
        lblOnDouble = new JLabel(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.GeneralPanel.OnValueLabel.text",
                "ON Value"));
        generalPanel.add(lblOnDouble, "2, 4, right, default");
        
        defaultOnDouble = new JTextField();
        generalPanel.add(defaultOnDouble, "4, 4, fill, default");
        defaultOnDouble.setColumns(10);
        
        lblOffDouble = new JLabel(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.GeneralPanel.OffValueLabel.text",
                "OFF Value"));
        generalPanel.add(lblOffDouble, "6, 4, right, default");
        
        defaultOffDouble = new JTextField();
        generalPanel.add(defaultOffDouble, "8, 4, fill, default");
        defaultOffDouble.setColumns(10);
        
        lblOnString = new JLabel(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.GeneralPanel.OnValueLabel.text",
                "ON Value"));
        generalPanel.add(lblOnString, "2, 5, right, default");
        
        defaultOnString = new JTextField();
        generalPanel.add(defaultOnString, "4, 5, fill, default");
        defaultOnString.setColumns(10);
        
        lblOffString = new JLabel(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.GeneralPanel.OffValueLabel.text",
                "OFF Value"));
        generalPanel.add(lblOffString, "6, 5, right, default");
        
        defaultOffString = new JTextField();
        generalPanel.add(defaultOffString, "8, 5");
        defaultOffString.setColumns(10);
        
        lblMachineState = new JLabel(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.GeneralPanel.MachineStateLabel.text",
                "Machine State"));
        generalPanel.add(lblMachineState, "2, 9, right, default");
        
        lblEnableActuation = new JLabel(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.GeneralPanel.EnabledLabel.text",
                "Enabled"));
        generalPanel.add(lblEnableActuation, "4, 9, center, default");
        
        lblHomingActuation = new JLabel(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.GeneralPanel.HomedLabel.text",
                "Homed"));
        lblHomingActuation.setToolTipText("");
        generalPanel.add(lblHomingActuation, "6, 9, center, default");
        
        lblDisableActuation = new JLabel(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.GeneralPanel.DisabledLabel.text",
                "Disabled"));
        generalPanel.add(lblDisableActuation, "8, 9, center, default");
        
        lblMachineStateActuation = new JLabel(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.GeneralPanel.ActuationLabel.text",
                "Actuation"));
        lblMachineStateActuation.setToolTipText(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.GeneralPanel.ActuationLabel.toolTip.text",
                "<html>\n" +
                        "When the machine state changes, a specific actuation value can be assumed or set. \n" +
                        "</html>\n"));
        generalPanel.add(lblMachineStateActuation, "2, 11, right, default");
        
        enabledActuation = new JComboBox(ReferenceActuator.MachineStateActuation.values());
        generalPanel.add(enabledActuation, "4, 11, fill, default");
        
        homedActuation = new JComboBox(ReferenceActuator.MachineStateActuation.values());
        generalPanel.add(homedActuation, "6, 11, fill, default");
        
        disabledActuation = new JComboBox(ReferenceActuator.MachineStateActuation.values());
        generalPanel.add(disabledActuation, "8, 11, fill, default");
        
        lblIndex = new JLabel(Translations.getStringOrDefault(
                "AbstractActuatorConfigurationWizard.GeneralPanel.IndexLabel.text",
                "Index"));
        generalPanel.add(lblIndex, "2, 15, right, default");
        
        indexTextField = new JTextField();
        generalPanel.add(indexTextField, "4, 15, fill, default");
        indexTextField.setColumns(10);

        if (actuator.getHead() == null) {
            headMountablePanel.setVisible(false);
        }
    }

    protected void adaptDialog() {
        boolean isDouble = (valueType.getSelectedItem() == ActuatorValueType.Double);
        lblOnDouble.setVisible(isDouble);
        defaultOnDouble.setVisible(isDouble);
        lblOffDouble.setVisible(isDouble);
        defaultOffDouble.setVisible(isDouble);
        boolean isString = (valueType.getSelectedItem() == ActuatorValueType.String);
        lblOnString.setVisible(isString);
        defaultOnString.setVisible(isString);
        lblOffString.setVisible(isString);
        defaultOffString.setVisible(isString);
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

        addWrappedBinding(actuator, "interlockActuator", interlockActuator, "selected");

        addWrappedBinding(actuator, "safeZ", textFieldSafeZ, "text", lengthConverter);

        addWrappedBinding(actuator, "coordinatedBeforeActuate", coordinatedBeforeActuate, "selected");
        addWrappedBinding(actuator, "coordinatedAfterActuate", coordinatedAfterActuate, "selected");
        addWrappedBinding(actuator, "coordinatedBeforeRead", coordinatedBeforeRead, "selected");

        addWrappedBinding(actuator, "valueType", valueType, "selectedItem");
        addWrappedBinding(actuator, "defaultOnDouble", defaultOnDouble, "text", doubleConverter);
        addWrappedBinding(actuator, "defaultOffDouble", defaultOffDouble, "text", doubleConverter);
        addWrappedBinding(actuator, "defaultOnString", defaultOnString, "text");
        addWrappedBinding(actuator, "defaultOffString", defaultOffString, "text");

        addWrappedBinding(actuator, "enabledActuation", enabledActuation, "selectedItem");
        addWrappedBinding(actuator, "homedActuation", homedActuation, "selectedItem");
        addWrappedBinding(actuator, "disabledActuation", disabledActuation, "selectedItem");

        addWrappedBinding(actuator, "index", indexTextField, "text", intConverter);

        ComponentDecorators.decorateWithAutoSelect(indexTextField);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationZ);
        ComponentDecorators.decorateWithAutoSelect(locationRotation);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldSafeZ);

        // Reset
        reloadWizard = false;
        adaptDialog();
    }

    @Override
    protected void saveToModel() {
        super.saveToModel();
        if (reloadWizard) {
            // Reselect the tree path to reload the wizard with potentially different property sheets. 
            MainFrame.get().getMachineSetupTab().selectCurrentTreePath();
        }
    }
}
