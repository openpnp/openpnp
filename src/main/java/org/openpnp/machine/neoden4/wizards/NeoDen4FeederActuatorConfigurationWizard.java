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

package org.openpnp.machine.neoden4.wizards;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.DriversComboBoxModel;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.machine.neoden4.NeoDen4Driver;
import org.openpnp.machine.neoden4.NeoDen4FeederActuator;
import org.openpnp.machine.reference.wizards.AbstractActuatorConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Driver;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class NeoDen4FeederActuatorConfigurationWizard extends AbstractActuatorConfigurationWizard {
    private JPanel panelProperties;
    private JLabel lblName;
    private JTextField nameTf;
    private JLabel lblDriver;
    private JComboBox driver;
    
    private JLabel lblPeelerId;
    private JTextField peelerIdTextField;
    
    private JLabel lblFeederId;
    private JTextField feederIdTextField;
    
    private JLabel lblPeelStrength;
    private JTextField peelStrengthTextField;
    
    private JLabel lblFeedStrength;
    private JTextField feedStrengthTextField;
    
    private JLabel lblFeedLength;
    private JTextField feedLengthTextField;
    
    private JLabel lblPeelLength;
    private JTextField peelLengthTextField;
    private JLabel lblPeelLengthPercent;
    
    private JLabel lblChangeId;
    private JLabel lblChangeIdNote;
    private JButton btnChangeFeederIdAction;
    private JLabel lblNewText;
    private JComboBox<Integer> newId;
    
    public NeoDen4FeederActuatorConfigurationWizard(AbstractMachine machine, NeoDen4FeederActuator actuator) {
        super(machine,  actuator);
    }
        

    
    
    @Override 
    protected void createUi(AbstractMachine machine) {
        panelProperties = new JPanel();
        panelProperties.setBorder(new TitledBorder(null, "Properties", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelProperties);
        panelProperties.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,  
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC
        		},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
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
                FormSpecs.DEFAULT_ROWSPEC,
                
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC
        }));
        
        lblDriver = new JLabel("Driver");
        panelProperties.add(lblDriver, "2, 2, right, default");
        
        driver = new JComboBox(new DriversComboBoxModel(machine, true));
        panelProperties.add(driver, "4, 2, fill, default");
        
        lblName = new JLabel("Name");
        panelProperties.add(lblName, "2, 4, right, default");
        
        nameTf = new JTextField();
        panelProperties.add(nameTf, "4, 4, fill, default");
        nameTf.setColumns(20);
        
        
        //===================
        
        lblPeelerId = new JLabel("Peeler ID");
        panelProperties.add(lblPeelerId, "2, 6, right, default");
        
        peelerIdTextField = new JTextField();
        panelProperties.add(peelerIdTextField, "4, 6, fill, default");
        peelerIdTextField.setColumns(16);
        
        lblFeederId = new JLabel("Feeder ID");
        panelProperties.add(lblFeederId, "2, 8, right, default");
        
        feederIdTextField = new JTextField();
        panelProperties.add(feederIdTextField, "4, 8, fill, default");
        feederIdTextField.setColumns(16);     
        
        lblFeedStrength = new JLabel("Feed Strength");
        panelProperties.add(lblFeedStrength, "2, 10, right, default");
        
        feedStrengthTextField = new JTextField();
        panelProperties.add(feedStrengthTextField, "4, 10, fill, default");
        feedStrengthTextField.setColumns(16);     
        
        lblPeelStrength = new JLabel("Peel Strength");
        panelProperties.add(lblPeelStrength, "2, 12, right, default");
        
        peelStrengthTextField = new JTextField();
        panelProperties.add(peelStrengthTextField, "4, 12, fill, default");
        peelStrengthTextField.setColumns(16);
        
        lblFeedLength = new JLabel("Feed length");
        panelProperties.add(lblFeedLength, "2, 14, right, default");
        
        feedLengthTextField = new JTextField();
        panelProperties.add(feedLengthTextField, "4, 14, fill, default");
        feedLengthTextField.setColumns(16);
        
        lblPeelLength = new JLabel("Peel length");
        panelProperties.add(lblPeelLength, "2, 16, right, default");
        
        peelLengthTextField = new JTextField();
        panelProperties.add(peelLengthTextField, "4, 16, fill, default");
        peelLengthTextField.setColumns(16);
        
        lblPeelLengthPercent = new JLabel("[%]");
        panelProperties.add(lblPeelLengthPercent, "6, 16, right, default");
        
        
        lblChangeId = new JLabel("Change Feeder ID");
        panelProperties.add(lblChangeId, "2, 18, right, default");
        
        lblChangeIdNote = new JLabel("(Stored in Feeder NVMEM)");
        panelProperties.add(lblChangeIdNote, "4, 18, center, default");
        
        newId = new JComboBox<Integer>();
        for(int i = 0; i <= 99; i++)
            newId.addItem(i);        

        lblNewText = new JLabel("New ID");
        panelProperties.add(lblNewText, "6, 18, right, default");
        
        panelProperties.add(newId, "8, 18, fill, default");
        
        btnChangeFeederIdAction = new JButton(new AbstractAction("Change") {
            @Override
            public void actionPerformed(ActionEvent e) {
                NeoDen4Driver driver = getNeoden4Driver();
                int oldIdInteger = Integer.parseInt(feederIdTextField.getText());
                int newIdInteger = (int)newId.getSelectedItem();
                UiUtils.messageBoxOnException(() -> {
                    driver.changeFeederId(oldIdInteger, newIdInteger);
                });
            }
        });
        

        panelProperties.add(btnChangeFeederIdAction, "10, 18, fill, default");
        
        super.createUi(machine);
    }

    @Override
    public void createBindings() {
        super.createBindings();
        
        IntegerConverter intConverter = new IntegerConverter();
        AbstractMachine machine = (AbstractMachine) Configuration.get().getMachine();
        NamedConverter<Driver> driverConverter = new NamedConverter<>(machine.getDrivers()); 
        
        addWrappedBinding(actuator, "driver", driver, "selectedItem", driverConverter);
        addWrappedBinding(actuator, "name", nameTf, "text");
        
        addWrappedBinding(actuator, "peelerId", peelerIdTextField, "text", intConverter);
        addWrappedBinding(actuator, "feederId", feederIdTextField, "text", intConverter);
        addWrappedBinding(actuator, "feedStrength", feedStrengthTextField, "text", intConverter);
        addWrappedBinding(actuator, "peelStrength", peelStrengthTextField, "text", intConverter);
        addWrappedBinding(actuator, "peelLength", peelLengthTextField, "text", intConverter);
        
        ComponentDecorators.decorateWithAutoSelect(nameTf);
    }
    
    private NeoDen4Driver getNeoden4Driver() {
        NeoDen4Driver driver = null;

        for (Driver d : Configuration.get().getMachine().getDrivers()) {
            if (d instanceof NeoDen4Driver) {
                driver = (NeoDen4Driver) d;
                break;
            }
        }
        return driver;
    }
    
}
