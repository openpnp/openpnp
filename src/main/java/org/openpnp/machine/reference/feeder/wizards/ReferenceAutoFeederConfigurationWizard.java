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

package org.openpnp.machine.reference.feeder.wizards;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.machine.reference.feeder.ReferenceAutoFeeder;
import org.openpnp.model.Configuration;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceAutoFeederConfigurationWizard
        extends AbstractReferenceFeederConfigurationWizard {
    private final ReferenceAutoFeeder feeder;
    private JTextField actuatorName;
    private JTextField actuatorValue;

    public ReferenceAutoFeederConfigurationWizard(ReferenceAutoFeeder feeder) {
        super(feeder);
        this.feeder = feeder;

        JPanel panelActuator = new JPanel();
        panelActuator.setBorder(new TitledBorder(null, "Feed Actuator", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelActuator);
        panelActuator.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblActuatorName = new JLabel("Actuator Name");
        panelActuator.add(lblActuatorName, "2, 2, right, default");

        actuatorName = new JTextField();
        panelActuator.add(actuatorName, "4, 2, fill, default");
        actuatorName.setColumns(10);

        JLabel lblActuatorValue = new JLabel("Actuator Value");
        panelActuator.add(lblActuatorValue, "2, 4, right, default");

        actuatorValue = new JTextField();
        panelActuator.add(actuatorValue, "4, 4, fill, default");
        actuatorValue.setColumns(10);
    }

    @Override
    public void createBindings() {
        super.createBindings();
        
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        addWrappedBinding(feeder, "actuatorName", actuatorName, "text");
        addWrappedBinding(feeder, "actuatorValue", actuatorValue, "text", doubleConverter);
    }
}
