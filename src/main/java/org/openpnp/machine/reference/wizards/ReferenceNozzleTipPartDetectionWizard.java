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

import java.util.HashSet;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.model.Configuration;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceNozzleTipPartDetectionWizard extends AbstractConfigurationWizard {
    private final ReferenceNozzleTip nozzleTip;

    private Set<org.openpnp.model.Package> compatiblePackages = new HashSet<>();
    private JPanel panelVacuumSensing;
    private JLabel lblPartOnNozzle;
    private JLabel lblPartOffNozzle;
    private JTextField vacuumLevelPartOnLow;
    private JTextField vacuumLevelPartOffLow;


    public ReferenceNozzleTipPartDetectionWizard(ReferenceNozzleTip nozzleTip) {
        this.nozzleTip = nozzleTip;
        
        panelVacuumSensing = new JPanel();
        panelVacuumSensing.setBorder(new TitledBorder(null, "Vacuum Sensing", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelVacuumSensing);
        panelVacuumSensing.setLayout(new FormLayout(new ColumnSpec[] {
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
        
        lblNewLabel = new JLabel("Low Value");
        panelVacuumSensing.add(lblNewLabel, "4, 2");
        
        lblNewLabel_1 = new JLabel("High Value");
        panelVacuumSensing.add(lblNewLabel_1, "6, 2");
        
        lblPartOnNozzle = new JLabel("Part On Nozzle Vacuum Range");
        panelVacuumSensing.add(lblPartOnNozzle, "2, 4, right, default");
        
        vacuumLevelPartOnLow = new JTextField();
        panelVacuumSensing.add(vacuumLevelPartOnLow, "4, 4");
        vacuumLevelPartOnLow.setColumns(10);
        
        vacuumLevelPartOnHigh = new JTextField();
        panelVacuumSensing.add(vacuumLevelPartOnHigh, "6, 4");
        vacuumLevelPartOnHigh.setColumns(10);
        
        lblPartOffNozzle = new JLabel("Part Off Nozzle Vacuum Range");
        panelVacuumSensing.add(lblPartOffNozzle, "2, 6, right, default");
        
        vacuumLevelPartOffLow = new JTextField();
        panelVacuumSensing.add(vacuumLevelPartOffLow, "4, 6");
        vacuumLevelPartOffLow.setColumns(10);
        
        vacuumLevelPartOffHigh = new JTextField();
        panelVacuumSensing.add(vacuumLevelPartOffHigh, "6, 6");
        vacuumLevelPartOffHigh.setColumns(10);
    }
    
    private JLabel lblNewLabel;
    private JLabel lblNewLabel_1;
    private JTextField vacuumLevelPartOnHigh;
    private JTextField vacuumLevelPartOffHigh;

    @Override
    public void createBindings() {
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        addWrappedBinding(nozzleTip, "vacuumLevelPartOnLow", vacuumLevelPartOnLow, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumLevelPartOnHigh", vacuumLevelPartOnHigh, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumLevelPartOffLow", vacuumLevelPartOffLow, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumLevelPartOffHigh", vacuumLevelPartOffHigh, "text", doubleConverter);
        
        ComponentDecorators.decorateWithAutoSelect(vacuumLevelPartOnLow);
        ComponentDecorators.decorateWithAutoSelect(vacuumLevelPartOnHigh);
        ComponentDecorators.decorateWithAutoSelect(vacuumLevelPartOffLow);
        ComponentDecorators.decorateWithAutoSelect(vacuumLevelPartOffHigh);
    }
}
