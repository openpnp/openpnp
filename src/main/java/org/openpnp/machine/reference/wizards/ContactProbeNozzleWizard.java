/*
 * Copyright (C) 2019 <mark@makr.zone>
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

import java.awt.BorderLayout;
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
import org.openpnp.machine.reference.ContactProbeNozzle;
import org.openpnp.machine.reference.ReferenceNozzle;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ContactProbeNozzleWizard extends AbstractConfigurationWizard {
    private final ReferenceNozzle nozzle;
    private JPanel panel;
    private JLabel label;
    private JTextField contactProbeActuatorNameTf;
    private JLabel lblProbeOffset;
    private JTextField textFieldProbeOffset;

    public ContactProbeNozzleWizard(ContactProbeNozzle nozzle) {
        this.nozzle = nozzle;
        createUi();
    }
    private void createUi() {
        
        contentPanel.setLayout(new BorderLayout(0, 0));
        
        panel = new JPanel();
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.DEFAULT_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("26px"),
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        label = new JLabel("Contact Probe Actuator Name");
        panel.add(label, "1, 2, left, center");
        
        contactProbeActuatorNameTf = new JTextField();
        panel.add(contactProbeActuatorNameTf, "2, 2, left, top");
        contactProbeActuatorNameTf.setColumns(10);
        
        lblProbeOffset = new JLabel("Probe Offset");
        panel.add(lblProbeOffset, "1, 4, right, default");
        
        textFieldProbeOffset = new JTextField();
        panel.add(textFieldProbeOffset, "2, 4, fill, default");
        textFieldProbeOffset.setColumns(10);
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();

        addWrappedBinding(nozzle, "contactProbeActuatorName", contactProbeActuatorNameTf, "text");
        addWrappedBinding(nozzle, "probeOffset", textFieldProbeOffset,
                "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelect(contactProbeActuatorNameTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldProbeOffset);
    }
}
