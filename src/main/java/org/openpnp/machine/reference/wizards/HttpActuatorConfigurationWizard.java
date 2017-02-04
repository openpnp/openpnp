/*
 * Copyright (C) 2017 Sebastian Pichelhofer <sp@apertus.org> based on reference by Jason von Nieda
 * <jason@vonnieda.org>
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
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.HttpActuator;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class HttpActuatorConfigurationWizard extends AbstractConfigurationWizard {
    private final HttpActuator actuator;

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
    private JPanel panelProperties;
    private JLabel lblName;
    private JTextField nameTf;
    private JLabel lblOnUrl;
    private JTextField onUrlTf;
    private JLabel lblOffUrl;
    private JTextField offUrlTf;

    public HttpActuatorConfigurationWizard(HttpActuator httpActuator) {
        this.actuator = httpActuator;

        panelProperties = new JPanel();
        panelProperties.setBorder(new TitledBorder(null, "Properties", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelProperties);
        panelProperties.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        lblName = new JLabel("Name");
        panelProperties.add(lblName, "2, 2, right, default");

        nameTf = new JTextField();
        panelProperties.add(nameTf, "4, 2, fill, default");
        nameTf.setColumns(20);

        lblOnUrl = new JLabel("On URL");
        panelProperties.add(lblOnUrl, "2, 4, right, default");

        onUrlTf = new JTextField();
        panelProperties.add(onUrlTf, "4, 4, fill, default");
        onUrlTf.setColumns(40);

        lblOffUrl = new JLabel("Off URL");
        panelProperties.add(lblOffUrl, "2, 6, right, default");

        offUrlTf = new JTextField();
        panelProperties.add(offUrlTf, "4, 6, fill, default");
        offUrlTf.setColumns(40);

        headMountablePanel = new JPanel();
        headMountablePanel.setLayout(new BoxLayout(headMountablePanel, BoxLayout.Y_AXIS));
        contentPanel.add(headMountablePanel);

        panelOffsets = new JPanel();
        headMountablePanel.add(panelOffsets);
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
        panelSafeZ.add(textFieldSafeZ, "4, 2, fill, default");
        textFieldSafeZ.setColumns(10);

        generalPanel = new JPanel();
        generalPanel.setBorder(new TitledBorder(null, "General", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(generalPanel);
        generalPanel.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        lblIndex = new JLabel("Index");
        generalPanel.add(lblIndex, "2, 2, right, default");

        indexTextField = new JTextField();
        generalPanel.add(indexTextField, "4, 2, fill, default");
        indexTextField.setColumns(10);
        if (httpActuator.getHead() == null) {
            headMountablePanel.setVisible(false);
        }
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();

        addWrappedBinding(actuator, "name", nameTf, "text");
        addWrappedBinding(actuator, "onUrl", onUrlTf, "text");
        addWrappedBinding(actuator, "offUrl", offUrlTf, "text");
        MutableLocationProxy headOffsets = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, actuator, "headOffsets", headOffsets, "location");
        addWrappedBinding(headOffsets, "lengthX", locationX, "text", lengthConverter);
        addWrappedBinding(headOffsets, "lengthY", locationY, "text", lengthConverter);
        addWrappedBinding(headOffsets, "lengthZ", locationZ, "text", lengthConverter);
        addWrappedBinding(actuator, "safeZ", textFieldSafeZ, "text", lengthConverter);
        addWrappedBinding(actuator, "index", indexTextField, "text", intConverter);

        ComponentDecorators.decorateWithAutoSelect(nameTf);
        ComponentDecorators.decorateWithAutoSelect(onUrlTf);
        ComponentDecorators.decorateWithAutoSelect(offUrlTf);
        ComponentDecorators.decorateWithAutoSelect(indexTextField);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(locationZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldSafeZ);
    }
}
