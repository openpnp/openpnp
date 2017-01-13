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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;

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
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.JBindings;
import org.openpnp.gui.support.JBindings.Wrapper;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.machine.reference.feeder.ReferenceAutoFeeder.ActuatorType;
import org.openpnp.machine.reference.feeder.ReferenceAutoFeederSlot;
import org.openpnp.machine.reference.feeder.ReferenceAutoFeederSlot.Bank;
import org.openpnp.machine.reference.feeder.ReferenceAutoFeederSlot.Feeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceAutoFeederSlotConfigurationWizard
        extends AbstractReferenceFeederConfigurationWizard {
    private final ReferenceAutoFeederSlot feeder;
    private JTextField actuatorName;
    private JTextField actuatorValue;
    private JTextField postPickActuatorName;
    private JTextField postPickActuatorValue;
    private JComboBox actuatorType;
    private JComboBox postPickActuatorType;
    private JTextField feederNameTf;
    private JTextField bankNameTf;

    public ReferenceAutoFeederSlotConfigurationWizard(ReferenceAutoFeederSlot feeder) {
        super(feeder);
        this.feeder = feeder;

        JPanel panelActuator = new JPanel();
        panelActuator.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Actuators", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(panelActuator);
        panelActuator.setLayout(new FormLayout(new ColumnSpec[] {
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
        panelActuator.add(lblActuatorName, "4, 2, left, default");

        JLabel lblActuatorType = new JLabel("Actuator Type");
        panelActuator.add(lblActuatorType, "6, 2, left, default");

        JLabel lblActuatorValue = new JLabel("Actuator Value (For Boolean, 0 = false, 1 = true)");
        panelActuator.add(lblActuatorValue, "8, 2, left, default");

        JLabel lblFeed = new JLabel("Feed");
        panelActuator.add(lblFeed, "2, 4, right, default");

        actuatorName = new JTextField();
        panelActuator.add(actuatorName, "4, 4");
        actuatorName.setColumns(10);
        
        actuatorType = new JComboBox(ActuatorType.values());
        panelActuator.add(actuatorType, "6, 4, fill, default");

        actuatorValue = new JTextField();
        panelActuator.add(actuatorValue, "8, 4");
        actuatorValue.setColumns(10);

        JLabel lblPostPick = new JLabel("Post Pick");
        panelActuator.add(lblPostPick, "2, 6, right, default");

        postPickActuatorName = new JTextField();
        postPickActuatorName.setColumns(10);
        panelActuator.add(postPickActuatorName, "4, 6");
        
        postPickActuatorType = new JComboBox(ActuatorType.values());
        panelActuator.add(postPickActuatorType, "6, 6, fill, default");

        postPickActuatorValue = new JTextField();
        postPickActuatorValue.setColumns(10);
        panelActuator.add(postPickActuatorValue, "8, 6");
        
        JPanel slotPanel = new JPanel();
        slotPanel.setBorder(new TitledBorder(null, "Slot", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(slotPanel);
        slotPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        JLabel lblFeeder = new JLabel("Feeder");
        slotPanel.add(lblFeeder, "2, 2, right, default");
        
        feederCb = new JComboBox();
        slotPanel.add(feederCb, "4, 2, fill, default");
        
        feederNameTf = new JTextField();
        slotPanel.add(feederNameTf, "6, 2, fill, default");
        feederNameTf.setColumns(10);
        
        feederPartCb = new JComboBox();
        try {
            feederPartCb.setModel(new PartsComboBoxModel());
        }
        catch (Throwable t) {
            // Swallow this error. This happens during parsing in
            // in WindowBuilder but doesn't happen during normal run.
        }
        feederPartCb.setRenderer(new IdentifiableListCellRenderer<Part>());
        slotPanel.add(feederPartCb, "8, 2, fill, default");
        
        JButton newFeederBtn = new JButton(newFeederAction);
        slotPanel.add(newFeederBtn, "10, 2");
        
        JButton deleteFeederBtn = new JButton("Delete");
        slotPanel.add(deleteFeederBtn, "12, 2");
        
        JLabel lblBank = new JLabel("Bank");
        slotPanel.add(lblBank, "2, 4, right, default");
        
        bankCb = new JComboBox();
        slotPanel.add(bankCb, "4, 4, fill, default");
        
        bankNameTf = new JTextField();
        slotPanel.add(bankNameTf, "6, 4, fill, default");
        bankNameTf.setColumns(10);
        
        JButton newBankBtn = new JButton(newBankAction);
        slotPanel.add(newBankBtn, "10, 4");
        
        JButton deleteBankBtn = new JButton("Delete");
        slotPanel.add(deleteBankBtn, "12, 4");
        
        for (Bank bank : ReferenceAutoFeederSlot.getBanks()) {
            bankCb.addItem(bank);
        }
        if (feeder.getBank() != null) {
            for (Feeder f : feeder.getBank().getFeeders()) {
                feederCb.addItem(f);
            }
        }
        bankCb.addActionListener(e -> {
            feederCb.removeAllItems();
            Bank bank = (Bank) bankCb.getSelectedItem();
            if (bank != null) {
                for (Feeder f : bank.getFeeders()) {
                    feederCb.addItem(f);
                }
            }
        });
    }

    @Override
    public void createBindings() {
        super.createBindings();

        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        addWrappedBinding(feeder, "actuatorName", actuatorName, "text");
        addWrappedBinding(feeder, "actuatorType", actuatorType, "selectedItem");
        addWrappedBinding(feeder, "actuatorValue", actuatorValue, "text", doubleConverter);
        
        addWrappedBinding(feeder, "postPickActuatorName", postPickActuatorName, "text");
        addWrappedBinding(feeder, "postPickActuatorType", postPickActuatorType, "selectedItem");
        addWrappedBinding(feeder, "postPickActuatorValue", postPickActuatorValue, "text", doubleConverter);

        /**
         * The strategy for the bank and feeder properties are a little complex:
         * We create an observable wrapper for bank and feeder. We add wrapped bindings
         * for these against the source feeder, so if they are changed, then upon hitting
         * Apply they will be changed on the source.
         * In addition we add non-wrapped bindings from the bank and feeder wrappers to their
         * instance properties such as name and part. Thus they will be updated immediately.
         */
        Wrapper<Bank> bankWrapper = new Wrapper<>();
        Wrapper<Feeder> feederWrapper = new Wrapper<>();
        
        addWrappedBinding(feeder, "bank", bankWrapper, "value");
        addWrappedBinding(feeder, "feeder", feederWrapper, "value");
        bind(UpdateStrategy.READ_WRITE, bankWrapper, "value", bankCb, "selectedItem");
        bind(UpdateStrategy.READ_WRITE, bankWrapper, "value.name", bankNameTf, "text");
        bind(UpdateStrategy.READ_WRITE, feederWrapper, "value", feederCb, "selectedItem");
        bind(UpdateStrategy.READ_WRITE, feederWrapper, "value.name", feederNameTf, "text");
        bind(UpdateStrategy.READ_WRITE, feederWrapper, "value.part", feederPartCb, "selectedItem");
        
        ComponentDecorators.decorateWithAutoSelect(actuatorName);
        ComponentDecorators.decorateWithAutoSelect(actuatorValue);
        ComponentDecorators.decorateWithAutoSelect(postPickActuatorName);
        ComponentDecorators.decorateWithAutoSelect(postPickActuatorValue);
        ComponentDecorators.decorateWithAutoSelect(feederNameTf);
        ComponentDecorators.decorateWithAutoSelect(bankNameTf);
    }
    
    private Action newFeederAction = new AbstractAction("New") {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (feeder.getBank() == null) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "No Bank Selected", "Please select a bank first.");
            }
            Feeder f = new Feeder();
            feeder.getBank().getFeeders().add(f);
            feederCb.addItem(f);
            feederCb.setSelectedItem(f);
        }
    };

    private Action newBankAction = new AbstractAction("New") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Bank bank = new Bank();
            ReferenceAutoFeederSlot.getBanks().add(bank);
            bankCb.addItem(bank);
            bankCb.setSelectedItem(bank);
        }
    };
    
    private JComboBox feederCb;
    private JComboBox bankCb;    
    private JComboBox feederPartCb;
}
