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

package org.openpnp.machine.reference.feeder.wizards.slot;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.JBindings.Wrapper;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.feeder.ReferenceAutoFeeder.ActuatorType;
import org.openpnp.machine.reference.feeder.ReferenceSlotAutoFeeder;
import org.openpnp.machine.reference.feeder.ReferenceSlotAutoFeeder.Bank;
import org.openpnp.machine.reference.feeder.ReferenceSlotAutoFeeder.Feeder;
import org.openpnp.model.Configuration;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceSlotAutoFeederConfigurationWizard
        extends AbstractConfigurationWizard {
    private final ReferenceSlotAutoFeeder feeder;
    private JTextField actuatorName;
    private JTextField actuatorValue;
    private JTextField postPickActuatorName;
    private JTextField postPickActuatorValue;
    private JComboBox actuatorType;
    private JComboBox postPickActuatorType;

    public ReferenceSlotAutoFeederConfigurationWizard(ReferenceSlotAutoFeeder feeder) {
        this.feeder = feeder;
        
        JPanel slotPanel = new JPanel();
        slotPanel.setBorder(new TitledBorder(null, "Slot", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(slotPanel);
        slotPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("right:default"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        JButton btnConfigureFeeders = new JButton(configureFeedersAction);
        slotPanel.add(btnConfigureFeeders, "6, 2");
        
        JLabel lblBank = new JLabel("Bank");
        slotPanel.add(lblBank, "2, 4, right, default");
        
        bankCb = new JComboBox();
        slotPanel.add(bankCb, "4, 4");
        
        JLabel lblFeeder = new JLabel("Feeder");
        slotPanel.add(lblFeeder, "2, 2, right, default");
        
        feederCb = new JComboBox();
        slotPanel.add(feederCb, "4, 2");
        feederCb.addItem(null);
        
        JButton btnConfigureBanks = new JButton(configureBanksAction);
        slotPanel.add(btnConfigureBanks, "6, 4");
        
        JPanel settingsPanel = new JPanel();
        settingsPanel.setBorder(new TitledBorder(null, "Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(settingsPanel);
        settingsPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        JLabel lblX_1 = new JLabel("X");
        settingsPanel.add(lblX_1, "4, 2");
        
        JLabel lblY_1 = new JLabel("Y");
        settingsPanel.add(lblY_1, "6, 2");
        
        JLabel lblZ_1 = new JLabel("Z");
        settingsPanel.add(lblZ_1, "8, 2");
        
        JLabel lblRotation_1 = new JLabel("Rotation");
        settingsPanel.add(lblRotation_1, "10, 2");
        
        JLabel lblPickLocation = new JLabel("Location");
        settingsPanel.add(lblPickLocation, "2, 4");
        
        xPickLocTf = new JTextField();
        settingsPanel.add(xPickLocTf, "4, 4");
        xPickLocTf.setColumns(10);
        
        yPickLocTf = new JTextField();
        settingsPanel.add(yPickLocTf, "6, 4");
        yPickLocTf.setColumns(10);
        
        zPickLocTf = new JTextField();
        settingsPanel.add(zPickLocTf, "8, 4");
        zPickLocTf.setColumns(10);
        
        rotPickLocTf = new JTextField();
        settingsPanel.add(rotPickLocTf, "10, 4");
        rotPickLocTf.setColumns(10);
        
        LocationButtonsPanel locationButtonsPanel = new LocationButtonsPanel((JTextField) null, (JTextField) null, (JTextField) null, (JTextField) null);
        settingsPanel.add(locationButtonsPanel, "12, 4, fill, fill");
        
        JLabel lblRetryCount = new JLabel("Retry Count");
        settingsPanel.add(lblRetryCount, "2, 6");
        
        retryCountTf = new JTextField();
        settingsPanel.add(retryCountTf, "4, 6");
        retryCountTf.setColumns(10);

        JPanel actuatorsPanel = new JPanel();
        actuatorsPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Actuators", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(actuatorsPanel);
        actuatorsPanel.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblActuatorName = new JLabel("Actuator Name");
        actuatorsPanel.add(lblActuatorName, "4, 2, left, default");

        JLabel lblActuatorType = new JLabel("Actuator Type");
        actuatorsPanel.add(lblActuatorType, "6, 2, left, default");

        JLabel lblActuatorValue = new JLabel("Actuator Value (For Boolean, 0 = false, 1 = true)");
        actuatorsPanel.add(lblActuatorValue, "8, 2, left, default");

        JLabel lblFeed = new JLabel("Feed");
        actuatorsPanel.add(lblFeed, "2, 4, right, default");

        actuatorName = new JTextField();
        actuatorsPanel.add(actuatorName, "4, 4");
        actuatorName.setColumns(10);
        
        actuatorType = new JComboBox(ActuatorType.values());
        actuatorsPanel.add(actuatorType, "6, 4, fill, default");

        actuatorValue = new JTextField();
        actuatorsPanel.add(actuatorValue, "8, 4");
        actuatorValue.setColumns(10);

        JLabel lblPostPick = new JLabel("Post Pick");
        actuatorsPanel.add(lblPostPick, "2, 6, right, default");

        postPickActuatorName = new JTextField();
        postPickActuatorName.setColumns(10);
        actuatorsPanel.add(postPickActuatorName, "4, 6");
        
        postPickActuatorType = new JComboBox(ActuatorType.values());
        actuatorsPanel.add(postPickActuatorType, "6, 6, fill, default");

        postPickActuatorValue = new JTextField();
        postPickActuatorValue.setColumns(10);
        actuatorsPanel.add(postPickActuatorValue, "8, 6");
        try {
        }
        catch (Throwable t) {
            // Swallow this error. This happens during parsing in
            // in WindowBuilder but doesn't happen during normal run.
        }
        
        for (Bank bank : ReferenceSlotAutoFeeder.getBanks()) {
            bankCb.addItem(bank);
        }
        if (feeder.getBank() != null) {
            for (Feeder f : feeder.getBank().getFeeders()) {
                feederCb.addItem(f);
            }
        }
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        addWrappedBinding(feeder, "actuatorName", actuatorName, "text");
        addWrappedBinding(feeder, "actuatorType", actuatorType, "selectedItem");
        addWrappedBinding(feeder, "actuatorValue", actuatorValue, "text", doubleConverter);
        
        addWrappedBinding(feeder, "postPickActuatorName", postPickActuatorName, "text");
        addWrappedBinding(feeder, "postPickActuatorType", postPickActuatorType, "selectedItem");
        addWrappedBinding(feeder, "postPickActuatorValue", postPickActuatorValue, "text", doubleConverter);
        
        
        addWrappedBinding(feeder, "retryCount", retryCountTf, "text", intConverter);

        MutableLocationProxy pickLocation = new MutableLocationProxy();
        addWrappedBinding(feeder, "location", pickLocation, "location");
        bind(UpdateStrategy.READ_WRITE, pickLocation, "lengthX", xPickLocTf, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, pickLocation, "lengthY", yPickLocTf, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, pickLocation, "lengthZ", zPickLocTf, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, pickLocation, "rotation", rotPickLocTf, "text", doubleConverter);
        
        addWrappedBinding(feeder, "bank", bankCb, "selectedItem");
        addWrappedBinding(feeder, "feeder", feederCb, "selectedItem");

        ComponentDecorators.decorateWithAutoSelect(actuatorName);
        ComponentDecorators.decorateWithAutoSelect(actuatorValue);
        ComponentDecorators.decorateWithAutoSelect(postPickActuatorName);
        ComponentDecorators.decorateWithAutoSelect(postPickActuatorValue);
        
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(xPickLocTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(yPickLocTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(zPickLocTf);
        ComponentDecorators.decorateWithAutoSelect(rotPickLocTf);

        ComponentDecorators.decorateWithAutoSelect(retryCountTf);
        
        bankCb.addActionListener(e -> {
            feederCb.removeAllItems();
            Bank bank = (Bank) bankCb.getSelectedItem();
            feederCb.addItem(null);
            if (bank != null) {
                for (Feeder f : bank.getFeeders()) {
                    feederCb.addItem(f);
                }
            }
        });
    }
    
    private Action configureFeedersAction = new AbstractAction("Configure Feeders") {
        @Override
        public void actionPerformed(ActionEvent e) {
            SlotFeederConfigDialog dialog = new SlotFeederConfigDialog(MainFrame.get());
            dialog.pack();
            dialog.setLocationRelativeTo(MainFrame.get());
            dialog.setVisible(true);
            Bank bank = (Bank) bankCb.getSelectedItem();
            Feeder feeder = (Feeder) feederCb.getSelectedItem();
            bankCb.setSelectedItem(bank);
            feederCb.setSelectedItem(feeder);
        }
    };
    
    private Action configureBanksAction = new AbstractAction("Configure Banks") {
        @Override
        public void actionPerformed(ActionEvent e) {
            SlotBankConfigDialog dialog = new SlotBankConfigDialog(MainFrame.get());
            dialog.pack();
            dialog.setLocationRelativeTo(MainFrame.get());
            dialog.setVisible(true);
            Bank bank = (Bank) bankCb.getSelectedItem();
            Feeder feeder = (Feeder) feederCb.getSelectedItem();
            bankCb.removeAllItems();
            for (Bank b : ReferenceSlotAutoFeeder.getBanks()) {
                bankCb.addItem(b);
            }
            bankCb.setSelectedItem(bank);
            feederCb.setSelectedItem(feeder);
        }
    };
    
    private JComboBox feederCb;
    private JComboBox bankCb;    
    private JTextField xPickLocTf;
    private JTextField yPickLocTf;
    private JTextField zPickLocTf;
    private JTextField rotPickLocTf;
    private JTextField retryCountTf;
}
