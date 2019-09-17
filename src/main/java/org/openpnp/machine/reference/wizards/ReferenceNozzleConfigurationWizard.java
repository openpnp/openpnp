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

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.ReferenceNozzle;

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
    private JCheckBox chckbxLimitRotationTo;
    private JTextField textFieldSafeZ;
    private JPanel panelProperties;
    private JLabel lblName;
    private JTextField nameTf;
    private JLabel lblDwellTime;
    private JLabel lblPickDwellTime;
    private JLabel lblPlaceDwellTime;
    private JTextField pickDwellTf;
    private JTextField placeDwellTf;
    private JLabel lblLimitRota;
    private JTextField zSafeActuatorName;
    private JTextField zSafeActuatorValue;

    public ReferenceNozzleConfigurationWizard(ReferenceNozzle nozzle) {
        this.nozzle = nozzle;

        panelProperties = new JPanel();
        panelProperties.setBorder(new TitledBorder(null, "Properties", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelProperties);
        panelProperties.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        lblName = new JLabel("Name");
        panelProperties.add(lblName, "2, 2, right, default");

        nameTf = new JTextField();
        panelProperties.add(nameTf, "4, 2");
        nameTf.setColumns(20);

        panelOffsets = new JPanel();
        panelOffsets.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Offsets", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelOffsets.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblX = new JLabel("X");
        panelOffsets.add(lblX, "2, 2");

        JLabel lblY = new JLabel("Y");
        panelOffsets.add(lblY, "4, 2");

        JLabel lblZ = new JLabel("Z");
        panelOffsets.add(lblZ, "6, 2");

        locationX = new JTextField();
        panelOffsets.add(locationX, "2, 4");
        locationX.setColumns(5);

        locationY = new JTextField();
        panelOffsets.add(locationY, "4, 4");
        locationY.setColumns(5);

        locationZ = new JTextField();
        panelOffsets.add(locationZ, "6, 4");
        locationZ.setColumns(5);

        contentPanel.add(panelOffsets);

        JPanel panelSafeZ = new JPanel();
        panelSafeZ.setBorder(new TitledBorder(null, "Safe Z", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelSafeZ);
        panelSafeZ.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("left:default:grow"),},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblSafeZ = new JLabel("Safe Z Height");
        panelSafeZ.add(lblSafeZ, "2, 2, right, default");

        textFieldSafeZ = new JTextField();
        panelSafeZ.add(textFieldSafeZ, "4, 2, fill, default");
        textFieldSafeZ.setColumns(10);

        JLabel lblNewLabel_4 = new JLabel("Safe Z Actuator Name");
        panelSafeZ.add(lblNewLabel_4, "2, 4, right, default");

        zSafeActuatorName = new JTextField();
        panelSafeZ.add(zSafeActuatorName, "4, 4, fill, default");
        zSafeActuatorName.setColumns(15);

        JLabel lblNewLabel_5 = new JLabel("Safe Z Value");
        panelSafeZ.add(lblNewLabel_5, "2, 6, right, default");

        zSafeActuatorValue = new JTextField();
        panelSafeZ.add(zSafeActuatorValue, "4, 6, fill, default");
        zSafeActuatorValue.setColumns(15);


        panelChanger = new JPanel();
        panelChanger.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Settings", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(panelChanger);
        panelChanger.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        lblLimitRota = new JLabel("Limit Rotation to 180º");
        panelChanger.add(lblLimitRota, "2, 2, right, default");

        chckbxLimitRotationTo = new JCheckBox("");
        panelChanger.add(chckbxLimitRotationTo, "4, 2");

        lblPickDwellTime = new JLabel("Pick Dwell Time (ms)");
        panelChanger.add(lblPickDwellTime, "2, 4, right, default");

        pickDwellTf = new JTextField();
        panelChanger.add(pickDwellTf, "4, 4, fill, default");
        pickDwellTf.setColumns(10);
        
        lblPlaceDwellTime = new JLabel("Place Dwell Time (ms)");
        panelChanger.add(lblPlaceDwellTime, "2, 6, right, default");
        
        placeDwellTf = new JTextField();
        panelChanger.add(placeDwellTf, "4, 6, fill, default");
        placeDwellTf.setColumns(10);
        
        CellConstraints cc = new CellConstraints();
        lblDwellTime = new JLabel("Note: Total Dwell Time is the sum of Nozzle Dwell Time plus the Nozzle Tip Dwell Time.");
        panelChanger.add(lblDwellTime, "2, 8, 7, 1");
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();

        addWrappedBinding(nozzle, "name", nameTf, "text");
        MutableLocationProxy headOffsets = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzle, "headOffsets", headOffsets, "location");
        addWrappedBinding(headOffsets, "lengthX", locationX, "text", lengthConverter);
        addWrappedBinding(headOffsets, "lengthY", locationY, "text", lengthConverter);
        addWrappedBinding(headOffsets, "lengthZ", locationZ, "text", lengthConverter);

        addWrappedBinding(nozzle, "limitRotation", chckbxLimitRotationTo, "selected");
        addWrappedBinding(nozzle, "safeZ", textFieldSafeZ, "text", lengthConverter);
        addWrappedBinding(nozzle, "zSafeActuatorName", zSafeActuatorName, "text");
        addWrappedBinding(nozzle, "zSafeActuatorValue", zSafeActuatorValue, "text");
        addWrappedBinding(nozzle, "pickDwellMilliseconds", pickDwellTf, "text", intConverter);
        addWrappedBinding(nozzle, "placeDwellMilliseconds", placeDwellTf, "text", intConverter);

        ComponentDecorators.decorateWithAutoSelect(nameTf);
        ComponentDecorators.decorateWithAutoSelect(pickDwellTf);
        ComponentDecorators.decorateWithAutoSelect(placeDwellTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldSafeZ);
        ComponentDecorators.decorateWithAutoSelect(zSafeActuatorName);
        ComponentDecorators.decorateWithAutoSelect(zSafeActuatorValue);
    }
}
