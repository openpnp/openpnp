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

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.AxesComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Axis;
import org.openpnp.spi.base.AbstractAxis;
import org.openpnp.spi.base.AbstractMachine;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceNozzleConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceNozzle nozzle;

    private JTextField locationX;
    private JTextField locationY;
    private JTextField locationZ;
    private JPanel panelOffsets;
    private JPanel panelChanger;
    private JTextField textFieldSafeZ;
    private JPanel panelProperties;
    private JLabel lblName;
    private JTextField nameTf;
    private JLabel lblDwellTime;
    private JLabel lblPickDwellTime;
    private JLabel lblPlaceDwellTime;
    private JTextField pickDwellTf;
    private JTextField placeDwellTf;
    private JLabel lblDynamicSafeZ;
    private JCheckBox chckbxDynamicsafez;
    private JComboBox axisX;
    private JComboBox axisY;
    private JComboBox axisZ;
    private JLabel lblRotation;
    private JTextField locationRotation;
    private JComboBox axisRotation;
    private JLabel lblAxis;
    private JLabel lblOffset;

    public ReferenceNozzleConfigurationWizard(AbstractMachine machine, ReferenceNozzle nozzle) {
        this.nozzle = nozzle;
        
        panelProperties = new JPanel();
        panelProperties.setBorder(new TitledBorder(null, "Properties", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelProperties);
        panelProperties.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblName = new JLabel("Name");
        panelProperties.add(lblName, "2, 2, right, default");
        
        nameTf = new JTextField();
        panelProperties.add(nameTf, "4, 2");
        nameTf.setColumns(20);

        panelOffsets = new JPanel();
        panelOffsets.setBorder(new TitledBorder(null,
                "Coordinate System", TitledBorder.LEADING, TitledBorder.TOP, null));
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
        panelOffsets.add(lblRotation, "10, 2");

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

        contentPanel.add(panelOffsets);
        
        locationRotation = new JTextField();
        panelOffsets.add(locationRotation, "10, 6, fill, default");
        locationRotation.setColumns(10);

        JPanel panelSafeZ = new JPanel();
        panelSafeZ.setBorder(new TitledBorder(null, "Safe Z", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelSafeZ);
        panelSafeZ.setLayout(new FormLayout(new ColumnSpec[] {
                ColumnSpec.decode("max(81dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("114px"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                RowSpec.decode("24px"),
                RowSpec.decode("19px"),}));
        
                JLabel lblSafeZ = new JLabel("Safe Z");
                panelSafeZ.add(lblSafeZ, "1, 1, right, center");
        
                textFieldSafeZ = new JTextField();
                textFieldSafeZ.setEditable(false);
                panelSafeZ.add(textFieldSafeZ, "3, 1, fill, top");
                textFieldSafeZ.setColumns(10);
                
                lblDynamicSafeZ = new JLabel("Dynamic Safe Z");
                lblDynamicSafeZ.setToolTipText("<html>\r\nWhen moving to Safe Z, account for the part height on the nozzle i.e. lift the nozzle higher with a taller part.<br/>\r\nThis allows you to use a lower Safe Z which might improve the machine speed. \r\n</html>");
                lblDynamicSafeZ.setHorizontalAlignment(SwingConstants.TRAILING);
                panelSafeZ.add(lblDynamicSafeZ, "1, 2");
                
                chckbxDynamicsafez = new JCheckBox("");
                chckbxDynamicsafez.setToolTipText("dynamicaly adjust the safeZ, so the bottom of a loaded part is at safeZ if possible");
                panelSafeZ.add(chckbxDynamicsafez, "3, 2");


        panelChanger = new JPanel();
        panelChanger.setBorder(new TitledBorder(null,
                "Settings", TitledBorder.LEADING, TitledBorder.TOP, null));
        contentPanel.add(panelChanger);
        panelChanger
                .setLayout(
                        new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblPickDwellTime = new JLabel("Pick Dwell Time (ms)");
        panelChanger.add(lblPickDwellTime, "2, 2, right, default");
        
        pickDwellTf = new JTextField();
        panelChanger.add(pickDwellTf, "4, 2, fill, default");
        pickDwellTf.setColumns(10);
        
        lblPlaceDwellTime = new JLabel("Place Dwell Time (ms)");
        panelChanger.add(lblPlaceDwellTime, "2, 4, right, default");
        
        placeDwellTf = new JTextField();
        panelChanger.add(placeDwellTf, "4, 4, fill, default");
        placeDwellTf.setColumns(10);
        
        CellConstraints cc = new CellConstraints();
        lblDwellTime = new JLabel("Note: Total Dwell Time is the sum of Nozzle Dwell Time plus the Nozzle Tip Dwell Time.");
        panelChanger.add(lblDwellTime, "2, 6, 9, 1, fill, default");
    }

    @Override
    public void createBindings() {
        AbstractMachine machine = (AbstractMachine) Configuration.get().getMachine();
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        NamedConverter<Axis> axisConverter = new NamedConverter<>(machine.getAxes()); 

        addWrappedBinding(nozzle, "name", nameTf, "text"); 

        addWrappedBinding(nozzle, "axisX", axisX, "selectedItem", axisConverter);
        addWrappedBinding(nozzle, "axisY", axisY, "selectedItem", axisConverter);
        addWrappedBinding(nozzle, "axisZ", axisZ, "selectedItem", axisConverter);
        addWrappedBinding(nozzle, "axisRotation", axisRotation, "selectedItem", axisConverter);

        MutableLocationProxy headOffsets = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzle, "headOffsets", headOffsets, "location");
        addWrappedBinding(headOffsets, "lengthX", locationX, "text", lengthConverter);
        addWrappedBinding(headOffsets, "lengthY", locationY, "text", lengthConverter);
        addWrappedBinding(headOffsets, "lengthZ", locationZ, "text", lengthConverter);
        addWrappedBinding(headOffsets, "rotation", locationRotation, "text", doubleConverter);

        addWrappedBinding(nozzle, "enableDynamicSafeZ", chckbxDynamicsafez, "selected");
        addWrappedBinding(nozzle, "safeZ", textFieldSafeZ, "text", lengthConverter);
        addWrappedBinding(nozzle, "pickDwellMilliseconds", pickDwellTf, "text", intConverter);
        addWrappedBinding(nozzle, "placeDwellMilliseconds", placeDwellTf, "text", intConverter);

        ComponentDecorators.decorateWithAutoSelect(nameTf);
        ComponentDecorators.decorateWithAutoSelect(pickDwellTf);
        ComponentDecorators.decorateWithAutoSelect(placeDwellTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationZ);
        ComponentDecorators.decorateWithAutoSelect(locationRotation);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldSafeZ);
    }
}
