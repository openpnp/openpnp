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

import java.awt.BorderLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.machine.reference.ReferenceNozzle;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceNozzleVacuumWizard extends AbstractConfigurationWizard {
    private final ReferenceNozzle nozzle;
    private JLabel label;
    private JPanel panel;
    private JComboBox vacuumComboBoxActuator;
    private JComboBox blowOffComboBoxActuator;
    private JLabel lblSensingActuator;
    private JComboBox vacuumSenseActuator;

    public ReferenceNozzleVacuumWizard(ReferenceNozzle nozzle) {
        this.nozzle = nozzle;
        createUi();
    }

    private void createUi() {
        
        CellConstraints cc = new CellConstraints();
        contentPanel.setLayout(new BorderLayout(0, 0));
        
        panel = new JPanel();
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
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
        
        label = new JLabel("Vacuum Actuator");
        panel.add(label, "2, 2, right, center");
        
        vacuumComboBoxActuator = new JComboBox();
        vacuumComboBoxActuator.setMaximumRowCount(15);
        vacuumComboBoxActuator.setModel(new ActuatorsComboBoxModel(nozzle.getHead()));
        panel.add(vacuumComboBoxActuator, "4, 2");
        label = new JLabel("Blow Off Actuator");
        panel.add(label, "2, 4, right, center");

        blowOffComboBoxActuator = new JComboBox();
        blowOffComboBoxActuator.setMaximumRowCount(15);
        blowOffComboBoxActuator.setModel(new ActuatorsComboBoxModel(nozzle.getHead()));
        panel.add(blowOffComboBoxActuator, "4, 4");
        lblSensingActuator = new JLabel("Sensing Actuator");
        panel.add(lblSensingActuator, "2, 6, right, default");

        vacuumSenseActuator = new JComboBox(new ActuatorsComboBoxModel(nozzle.getHead()));
        vacuumSenseActuator.setMaximumRowCount(15);
        panel.add(vacuumSenseActuator, "4, 6, fill, default");
    }

    @Override
    public void createBindings() {

        addWrappedBinding(nozzle, "vacuumActuatorName", vacuumComboBoxActuator, "selectedItem");
        addWrappedBinding(nozzle, "blowOffActuatorName", blowOffComboBoxActuator, "selectedItem");
        addWrappedBinding(nozzle, "vacuumSenseActuatorName", vacuumSenseActuator, "selectedItem");
    }
}
