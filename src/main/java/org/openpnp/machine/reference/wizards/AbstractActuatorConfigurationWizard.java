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
import org.openpnp.spi.base.AbstractActuator.ActuatorCoordinationEnumType;
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
    private JComboBox<AxesComboBoxModel> axisX;
    private JComboBox<AxesComboBoxModel> axisY;
    private JComboBox<AxesComboBoxModel> axisZ;
    private JLabel lblRotation;
    private JComboBox<AxesComboBoxModel> axisRotation;
    private JTextField locationRotation;
    private JLabel lblAxis;
    private JLabel lblOffset;

    private JPanel panelCoordination;
    private JLabel lblBeforeActuation;
    private JComboBox<ActuatorCoordinationEnumType> coordinatedBeforeActuate;
    private JLabel lblAfterActuation;
    private JComboBox<ActuatorCoordinationEnumType> coordinatedAfterActuate;
    private JLabel lblBeforeRead;
    private JComboBox<ActuatorCoordinationEnumType> coordinatedBeforeRead;

    private boolean reloadWizard;
    private JLabel lblAxisInterlock;
    private JCheckBox interlockActuator;
    private JLabel lblValueType;
    protected JComboBox<Actuator.ActuatorValueType> valueType;
    private JTextField defaultOnDouble;
    private JLabel lblOnDouble;
    private JLabel lblOffDouble;
    private JTextField defaultOffDouble;
    private JLabel lblOnString;
    private JLabel lblOffString;
    private JTextField defaultOnString;
    private JTextField defaultOffString;
    private JLabel lblHomingActuation;
    private JComboBox<ReferenceActuator.MachineStateActuation> homedActuation;
    private JLabel lblEnableActuation;
    private JComboBox<ReferenceActuator.MachineStateActuation> enabledActuation;
    private JLabel lblDisableActuation;
    private JComboBox<ReferenceActuator.MachineStateActuation> disabledActuation;
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
                Translations.getString("AbstractActuatorConfigurationWizard.CoordinateSystemPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null));
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

        lblRotation = new JLabel(Translations.getString(
                "AbstractActuatorConfigurationWizard.CoordinateSystemPanel.RotationLabel.text")); //$NON-NLS-1$
        panelOffsets.add(lblRotation, "10, 2, left, default");

        lblAxis = new JLabel(Translations.getString(
                "AbstractActuatorConfigurationWizard.CoordinateSystemPanel.AxisLabel.text")); //$NON-NLS-1$
        panelOffsets.add(lblAxis, "2, 4, right, default");

        axisX = new JComboBox<AxesComboBoxModel>(new AxesComboBoxModel(machine, AbstractAxis.class, Axis.Type.X, true));
        panelOffsets.add(axisX, "4, 4, fill, default");

        axisY = new JComboBox<AxesComboBoxModel>(new AxesComboBoxModel(machine, AbstractAxis.class, Axis.Type.Y, true));
        panelOffsets.add(axisY, "6, 4, fill, default");

        axisZ = new JComboBox<AxesComboBoxModel>(new AxesComboBoxModel(machine, AbstractAxis.class, Axis.Type.Z, true));
        panelOffsets.add(axisZ, "8, 4, fill, default");

        axisRotation = new JComboBox<AxesComboBoxModel>(new AxesComboBoxModel(machine, AbstractAxis.class, Axis.Type.Rotation, true));
        panelOffsets.add(axisRotation, "10, 4, fill, default");

        lblOffset = new JLabel(Translations.getString(
                "AbstractActuatorConfigurationWizard.CoordinateSystemPanel.OffsetLabel.text")); //$NON-NLS-1$
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
        
        lblAxisInterlock = new JLabel(Translations.getString(
                "AbstractActuatorConfigurationWizard.CoordinateSystemPanel.AxisInterlockLabel.text")); //$NON-NLS-1$
        lblAxisInterlock.setToolTipText(Translations.getString(
                "AbstractActuatorConfigurationWizard.CoordinateSystemPanel.AxisInterlockLabel.toolTipText")); //$NON-NLS-1$
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
        panelSafeZ.setBorder(new TitledBorder(null, Translations.getString(
                "AbstractActuatorConfigurationWizard.SafeZPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panelSafeZ.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblSafeZ = new JLabel(Translations.getString(
                "AbstractActuatorConfigurationWizard.SafeZPanel.SafeZLabel.text")); //$NON-NLS-1$
        panelSafeZ.add(lblSafeZ, "2, 2, right, default");

        textFieldSafeZ = new JTextField();
        textFieldSafeZ.setEditable(false);
        panelSafeZ.add(textFieldSafeZ, "4, 2, fill, default");
        textFieldSafeZ.setColumns(10);
        
        panelCoordination = new JPanel();
        contentPanel.add(panelCoordination);
        panelCoordination.setBorder(new TitledBorder(null, Translations.getString(
                "AbstractActuatorConfigurationWizard.CoordinationPanel.Border.title" //$NON-NLS-1$
        ), TitledBorder.LEADING, TitledBorder.TOP, null, null));
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
        
        lblBeforeActuation = new JLabel(Translations.getString(
                "AbstractActuatorConfigurationWizard.CoordinationPanel.BeforeActuationLabel.text")); //$NON-NLS-1$
        lblBeforeActuation.setToolTipText(Translations.getString(
                "AbstractActuatorConfigurationWizard.CoordinationPanel.BeforeActuationLabel.toolTipText")); //$NON-NLS-1$
        panelCoordination.add(lblBeforeActuation, "2, 2, right, default");
        
        coordinatedBeforeActuate = new JComboBox<ActuatorCoordinationEnumType>(getCoordinatedBeforeActuateOptions());
        panelCoordination.add(coordinatedBeforeActuate, "4, 2");
        
        lblAfterActuation = new JLabel(Translations.getString(
                "AbstractActuatorConfigurationWizard.CoordinationPanel.AfterActuationLabel.text")); //$NON-NLS-1$
        lblAfterActuation.setToolTipText(Translations.getString(
                "AbstractActuatorConfigurationWizard.CoordinationPanel.AfterActuationLabel.toolTipText")); //$NON-NLS-1$
        panelCoordination.add(lblAfterActuation, "2, 4, right, default");
        
        coordinatedAfterActuate = new JComboBox<ActuatorCoordinationEnumType>(getCoordinatedAfterActuateOptions());
        panelCoordination.add(coordinatedAfterActuate, "4, 4");
        
        lblBeforeRead = new JLabel(Translations.getString(
                "AbstractActuatorConfigurationWizard.CoordinationPanel.BeforeReadLabel.text")); //$NON-NLS-1$
        lblBeforeRead.setToolTipText(Translations.getString(
                "AbstractActuatorConfigurationWizard.CoordinationPanel.BeforeReadLabel.toolTipText")); //$NON-NLS-1$
        panelCoordination.add(lblBeforeRead, "2, 6, right, default");
        
        coordinatedBeforeRead = new JComboBox<ActuatorCoordinationEnumType>(getCoordinatedBeforeReadOptions());
        panelCoordination.add(coordinatedBeforeRead, "4, 6");
        
        generalPanel = new JPanel();
        generalPanel.setBorder(new TitledBorder(null, Translations.getString(
                "AbstractActuatorConfigurationWizard.GeneralPanel.Border.title"), //$NON-NLS-1$
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
        
        lblValueType = new JLabel(Translations.getString(
                "AbstractActuatorConfigurationWizard.GeneralPanel.ValueTypeLabel.text")); //$NON-NLS-1$
        lblValueType.setToolTipText(Translations.getString(
                "AbstractActuatorConfigurationWizard.GeneralPanel.ValueTypeLabel.toolTipText")); //$NON-NLS-1$
        generalPanel.add(lblValueType, "2, 2, right, default");
        
        valueType = new JComboBox<Actuator.ActuatorValueType>(Actuator.ActuatorValueType.values());
        valueType.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                reloadWizard = true;
                adaptDialog();
            }
        });
        generalPanel.add(valueType, "4, 2, fill, default");
        
        lblOnDouble = new JLabel(Translations.getString(
                "AbstractActuatorConfigurationWizard.GeneralPanel.OnValueLabel.text")); //$NON-NLS-1$
        generalPanel.add(lblOnDouble, "2, 4, right, default");
        
        defaultOnDouble = new JTextField();
        generalPanel.add(defaultOnDouble, "4, 4, fill, default");
        defaultOnDouble.setColumns(10);
        
        lblOffDouble = new JLabel(Translations.getString(
                "AbstractActuatorConfigurationWizard.GeneralPanel.OffValueLabel.text")); //$NON-NLS-1$
        generalPanel.add(lblOffDouble, "6, 4, right, default");
        
        defaultOffDouble = new JTextField();
        generalPanel.add(defaultOffDouble, "8, 4, fill, default");
        defaultOffDouble.setColumns(10);
        
        lblOnString = new JLabel(Translations.getString(
                "AbstractActuatorConfigurationWizard.GeneralPanel.OnValueLabel.text")); //$NON-NLS-1$
        generalPanel.add(lblOnString, "2, 5, right, default");
        
        defaultOnString = new JTextField();
        generalPanel.add(defaultOnString, "4, 5, fill, default");
        defaultOnString.setColumns(10);
        
        lblOffString = new JLabel(Translations.getString(
                "AbstractActuatorConfigurationWizard.GeneralPanel.OffValueLabel.text")); //$NON-NLS-1$
        generalPanel.add(lblOffString, "6, 5, right, default");
        
        defaultOffString = new JTextField();
        generalPanel.add(defaultOffString, "8, 5");
        defaultOffString.setColumns(10);
        
        lblMachineState = new JLabel(Translations.getString(
                "AbstractActuatorConfigurationWizard.GeneralPanel.MachineStateLabel.text")); //$NON-NLS-1$
        generalPanel.add(lblMachineState, "2, 9, right, default");
        
        lblEnableActuation = new JLabel(Translations.getString(
                "AbstractActuatorConfigurationWizard.GeneralPanel.EnabledLabel.text")); //$NON-NLS-1$
        generalPanel.add(lblEnableActuation, "4, 9, center, default");
        
        lblHomingActuation = new JLabel(Translations.getString(
                "AbstractActuatorConfigurationWizard.GeneralPanel.HomedLabel.text")); //$NON-NLS-1$
        lblHomingActuation.setToolTipText("");
        generalPanel.add(lblHomingActuation, "6, 9, center, default");
        
        lblDisableActuation = new JLabel(Translations.getString(
                "AbstractActuatorConfigurationWizard.GeneralPanel.DisabledLabel.text")); //$NON-NLS-1$
        generalPanel.add(lblDisableActuation, "8, 9, center, default");
        
        lblMachineStateActuation = new JLabel(Translations.getString(
                "AbstractActuatorConfigurationWizard.GeneralPanel.ActuationLabel.text")); //$NON-NLS-1$
        lblMachineStateActuation.setToolTipText(Translations.getString(
                "AbstractActuatorConfigurationWizard.GeneralPanel.ActuationLabel.toolTipText")); //$NON-NLS-1$
        generalPanel.add(lblMachineStateActuation, "2, 11, right, default");
        
        enabledActuation = new JComboBox<ReferenceActuator.MachineStateActuation>(ReferenceActuator.MachineStateActuation.values());
        generalPanel.add(enabledActuation, "4, 11, fill, default");
        
        homedActuation = new JComboBox<ReferenceActuator.MachineStateActuation>(ReferenceActuator.MachineStateActuation.values());
        generalPanel.add(homedActuation, "6, 11, fill, default");
        
        disabledActuation = new JComboBox<ReferenceActuator.MachineStateActuation>(ReferenceActuator.MachineStateActuation.values());
        generalPanel.add(disabledActuation, "8, 11, fill, default");
        
        lblIndex = new JLabel(Translations.getString(
                "AbstractActuatorConfigurationWizard.GeneralPanel.IndexLabel.text")); //$NON-NLS-1$
        generalPanel.add(lblIndex, "2, 15, right, default");
        
        indexTextField = new JTextField();
        generalPanel.add(indexTextField, "4, 15, fill, default");
        indexTextField.setColumns(10);

        if (actuator.getHead() == null) {
            headMountablePanel.setVisible(false);
        }
    }

    // return all options the user can choose from when specifying coordination the
    private ActuatorCoordinationEnumType[] getCoordinatedBeforeActuateOptions() {
        return new ActuatorCoordinationEnumType[] { 
                ActuatorCoordinationEnumType.None, 
                ActuatorCoordinationEnumType.CommandStillstand,
                ActuatorCoordinationEnumType.WaitForStillstand };
    }
    private ActuatorCoordinationEnumType[] getCoordinatedAfterActuateOptions() {
        return new ActuatorCoordinationEnumType[] { 
                ActuatorCoordinationEnumType.None, 
                ActuatorCoordinationEnumType.WaitForUnconditionalCoordination };
    }
    private ActuatorCoordinationEnumType[] getCoordinatedBeforeReadOptions() {
        return new ActuatorCoordinationEnumType[] { 
                ActuatorCoordinationEnumType.None, 
                ActuatorCoordinationEnumType.WaitForStillstand };
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

        addWrappedBinding(actuator, "coordinatedBeforeActuateEnum", coordinatedBeforeActuate, "selectedItem");
        addWrappedBinding(actuator, "coordinatedAfterActuateEnum", coordinatedAfterActuate, "selectedItem");
        addWrappedBinding(actuator, "coordinatedBeforeReadEnum", coordinatedBeforeRead, "selectedItem");

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
