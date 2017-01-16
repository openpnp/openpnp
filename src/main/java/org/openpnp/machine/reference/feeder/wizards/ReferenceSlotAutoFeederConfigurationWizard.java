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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AbstractBindingListener;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.BindingListener;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.JBindings.Wrapper;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.machine.reference.feeder.ReferenceAutoFeeder.ActuatorType;
import org.openpnp.machine.reference.feeder.ReferenceSlotAutoFeeder;
import org.openpnp.machine.reference.feeder.ReferenceSlotAutoFeeder.Bank;
import org.openpnp.machine.reference.feeder.ReferenceSlotAutoFeeder.Feeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;

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
    private JTextField feederNameTf;
    private JTextField bankNameTf;

    public ReferenceSlotAutoFeederConfigurationWizard(ReferenceSlotAutoFeeder feeder) {
        this.feeder = feeder;
        
        JPanel panel_1 = new JPanel();
        panel_1.setBorder(new TitledBorder(null, "Slot", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel_1);
        panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.Y_AXIS));
        
        JPanel feederPanel = new JPanel();
        panel_1.add(feederPanel);
        feederPanel.setLayout(new FormLayout(new ColumnSpec[] {
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
        
        JLabel lblBank = new JLabel("Feeder Bank");
        feederPanel.add(lblBank, "2, 2");
        
        bankCb = new JComboBox();
        feederPanel.add(bankCb, "4, 2");
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
        
        bankNameTf = new JTextField();
        feederPanel.add(bankNameTf, "6, 2");
        bankNameTf.setColumns(10);
        
        JButton newBankBtn = new JButton(newBankAction);
        feederPanel.add(newBankBtn, "8, 2");
        
        JButton deleteBankBtn = new JButton("Delete");
        feederPanel.add(deleteBankBtn, "10, 2");
        
        JLabel lblFeeder = new JLabel("Feeder");
        feederPanel.add(lblFeeder, "2, 4, right, default");
        
        feederCb = new JComboBox();
        feederPanel.add(feederCb, "4, 4");
        
        feederNameTf = new JTextField();
        feederPanel.add(feederNameTf, "6, 4");
        feederNameTf.setColumns(10);
        
        JButton newFeederBtn = new JButton(newFeederAction);
        feederPanel.add(newFeederBtn, "8, 4");
        
        JButton deleteFeederBtn = new JButton("Delete");
        feederPanel.add(deleteFeederBtn, "10, 4");
        
        JLabel lblPart = new JLabel("Part");
        feederPanel.add(lblPart, "2, 6, right, default");
        
        feederPartCb = new JComboBox();
        feederPanel.add(feederPartCb, "4, 6");
        feederPartCb.setModel(new PartsComboBoxModel());
        feederPartCb.setRenderer(new IdentifiableListCellRenderer<Part>());
        
        JPanel panel = new JPanel();
        panel_1.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        JLabel lblX = new JLabel("X");
        panel.add(lblX, "4, 2");
        
        JLabel lblY = new JLabel("Y");
        panel.add(lblY, "6, 2");
        
        JLabel lblZ = new JLabel("Z");
        panel.add(lblZ, "8, 2");
        
        JLabel lblRotation = new JLabel("Rotation");
        panel.add(lblRotation, "10, 2");
        
        JLabel lblOffsets = new JLabel("Offsets");
        panel.add(lblOffsets, "2, 4");
        
        xOffsetTf = new JTextField();
        panel.add(xOffsetTf, "4, 4");
        xOffsetTf.setColumns(10);
        
        yOffsetTf = new JTextField();
        panel.add(yOffsetTf, "6, 4");
        yOffsetTf.setColumns(10);
        
        zOffsetTf = new JTextField();
        panel.add(zOffsetTf, "8, 4");
        zOffsetTf.setColumns(10);
        
        rotOffsetTf = new JTextField();
        panel.add(rotOffsetTf, "10, 4");
        rotOffsetTf.setColumns(10);
        
        offsetLocButtons = new LocationButtonsPanel(xOffsetTf, yOffsetTf, zOffsetTf, rotOffsetTf);
        panel.add(offsetLocButtons, "12, 4");
        
        JPanel generalPanel = new JPanel();
        generalPanel.setBorder(new TitledBorder(null, "General Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(generalPanel);
        generalPanel.setLayout(new FormLayout(new ColumnSpec[] {
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
        generalPanel.add(lblX_1, "4, 2");
        
        JLabel lblY_1 = new JLabel("Y");
        generalPanel.add(lblY_1, "6, 2");
        
        JLabel lblZ_1 = new JLabel("Z");
        generalPanel.add(lblZ_1, "8, 2");
        
        JLabel lblRotation_1 = new JLabel("Rotation");
        generalPanel.add(lblRotation_1, "10, 2");
        
        JLabel lblPickLocation = new JLabel("Pick Location");
        generalPanel.add(lblPickLocation, "2, 4");
        
        xPickLocTf = new JTextField();
        generalPanel.add(xPickLocTf, "4, 4, fill, default");
        xPickLocTf.setColumns(10);
        
        yPickLocTf = new JTextField();
        generalPanel.add(yPickLocTf, "6, 4, fill, default");
        yPickLocTf.setColumns(10);
        
        zPickLocTf = new JTextField();
        generalPanel.add(zPickLocTf, "8, 4");
        zPickLocTf.setColumns(10);
        
        rotPickLocTf = new JTextField();
        generalPanel.add(rotPickLocTf, "10, 4, fill, default");
        rotPickLocTf.setColumns(10);
        
        pickLocButtons = new LocationButtonsPanel(xPickLocTf, yPickLocTf, zPickLocTf, rotPickLocTf);
        generalPanel.add(pickLocButtons, "12, 4, fill, fill");
        
        JLabel lblRetryCount = new JLabel("Retry Count");
        generalPanel.add(lblRetryCount, "2, 6, right, default");
        
        retryCountTf = new JTextField();
        generalPanel.add(retryCountTf, "4, 6, fill, default");
        retryCountTf.setColumns(10);

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
        try {
        }
        catch (Throwable t) {
            // Swallow this error. This happens during parsing in
            // in WindowBuilder but doesn't happen during normal run.
        }
        
        for (Bank bank : ReferenceSlotAutoFeeder.getBanks()) {
            bankCb.addItem(bank);
        }
        feederCb.addItem(null);
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
        bind(UpdateStrategy.READ, pickLocation, "location", offsetLocButtons, "baseLocation");
        
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
        bind(UpdateStrategy.READ_WRITE, bankWrapper, "value.name", bankNameTf, "text")
            .addBindingListener(new AbstractBindingListener() {
                @Override
                public void synced(Binding binding) {
                    SwingUtilities.invokeLater(() -> bankCb.repaint());
                }
            });
        bind(UpdateStrategy.READ_WRITE, feederWrapper, "value", feederCb, "selectedItem");
        bind(UpdateStrategy.READ_WRITE, feederWrapper, "value.name", feederNameTf, "text")
            .addBindingListener(new AbstractBindingListener() {
                @Override
                public void synced(Binding binding) {
                    SwingUtilities.invokeLater(() -> feederCb.repaint());
                }
            });
        bind(UpdateStrategy.READ_WRITE, feederWrapper, "value.part", feederPartCb, "selectedItem");

        MutableLocationProxy offsets = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feederWrapper, "value.offsets", offsets, "location");
        bind(UpdateStrategy.READ_WRITE, offsets, "lengthX", xOffsetTf, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, offsets, "lengthY", yOffsetTf, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, offsets, "lengthZ", zOffsetTf, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, offsets, "rotation", rotOffsetTf, "text", doubleConverter);

        ComponentDecorators.decorateWithAutoSelect(actuatorName);
        ComponentDecorators.decorateWithAutoSelect(actuatorValue);
        ComponentDecorators.decorateWithAutoSelect(postPickActuatorName);
        ComponentDecorators.decorateWithAutoSelect(postPickActuatorValue);
        ComponentDecorators.decorateWithAutoSelect(feederNameTf);
        ComponentDecorators.decorateWithAutoSelect(bankNameTf);
        
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(xPickLocTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(yPickLocTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(zPickLocTf);
        ComponentDecorators.decorateWithAutoSelect(rotPickLocTf);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(xOffsetTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(yOffsetTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(zOffsetTf);
        ComponentDecorators.decorateWithAutoSelect(rotOffsetTf);
        
        ComponentDecorators.decorateWithAutoSelect(retryCountTf);
    }
    
    private Action newFeederAction = new AbstractAction("New") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Bank bank = (Bank) bankCb.getSelectedItem();
            Feeder f = new Feeder();
            bank.getFeeders().add(f);
            feederCb.addItem(f);
            feederCb.setSelectedItem(f);
        }
    };

    private Action newBankAction = new AbstractAction("New") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Bank bank = new Bank();
            ReferenceSlotAutoFeeder.getBanks().add(bank);
            bankCb.addItem(bank);
            bankCb.setSelectedItem(bank);
        }
    };
    
    private JComboBox feederCb;
    private JComboBox bankCb;    
    private JComboBox feederPartCb;
    private JTextField xOffsetTf;
    private JTextField yOffsetTf;
    private JTextField zOffsetTf;
    private JTextField rotOffsetTf;
    private JTextField xPickLocTf;
    private JTextField yPickLocTf;
    private JTextField zPickLocTf;
    private JTextField rotPickLocTf;
    private JTextField retryCountTf;
    private LocationButtonsPanel offsetLocButtons;
    private LocationButtonsPanel pickLocButtons;
}
