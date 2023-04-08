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

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.Translations;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.DriversComboBoxModel;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Driver;
import org.openpnp.spi.base.AbstractMachine;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceActuatorConfigurationWizard extends AbstractActuatorConfigurationWizard {
    private JPanel panelProperties;
    private JLabel lblName;
    private JTextField nameTf;
    private JLabel lblDriver;
    private JComboBox driver;
    
    public ReferenceActuatorConfigurationWizard(AbstractMachine machine, ReferenceActuator actuator) {
        super(machine,  actuator);
    }
        
    @Override 
    protected void createUi(AbstractMachine machine) {
        panelProperties = new JPanel();
        panelProperties.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceActuatorConfigurationWizard.PropertiesPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelProperties);
        panelProperties.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblDriver = new JLabel(Translations.getString(
                "ReferenceActuatorConfigurationWizard.PropertiesPanel.DriverLabel.text")); //$NON-NLS-1$
        panelProperties.add(lblDriver, "2, 2, right, default");
        
        driver = new JComboBox(new DriversComboBoxModel(machine, true));
        panelProperties.add(driver, "4, 2, fill, default");
        
        lblName = new JLabel(Translations.getString(
                "ReferenceActuatorConfigurationWizard.PropertiesPanel.NameLabel.text")); //$NON-NLS-1$
        panelProperties.add(lblName, "2, 4, right, default");
        
        nameTf = new JTextField();
        panelProperties.add(nameTf, "4, 4, fill, default");
        nameTf.setColumns(20);
        
        super.createUi(machine);
    }

    @Override
    public void createBindings() {
        super.createBindings();
        
        AbstractMachine machine = (AbstractMachine) Configuration.get().getMachine();
        NamedConverter<Driver> driverConverter = new NamedConverter<>(machine.getDrivers()); 
        
        addWrappedBinding(actuator, "driver", driver, "selectedItem", driverConverter);
        addWrappedBinding(actuator, "name", nameTf, "text");

        ComponentDecorators.decorateWithAutoSelect(nameTf);
    }
}
