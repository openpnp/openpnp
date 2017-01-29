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
import org.openpnp.gui.support.MessageBoxes;
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
import java.awt.FlowLayout;

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
        
        JPanel slotPanel = new JPanel();
        slotPanel.setBorder(new TitledBorder(null, "Slot", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(slotPanel);
        slotPanel.setLayout(new BoxLayout(slotPanel, BoxLayout.Y_AXIS));
        
        JPanel whateverPanel = new JPanel();
        slotPanel.add(whateverPanel);
        FormLayout fl_whateverPanel = new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,});
        fl_whateverPanel.setColumnGroups(new int[][]{new int[]{4, 6, 8, 10}});
        whateverPanel.setLayout(fl_whateverPanel);
        
        feederNameTf = new JTextField();
        whateverPanel.add(feederNameTf, "8, 2, 3, 1");
        feederNameTf.setColumns(10);
        
        JPanel panel_1 = new JPanel();
        FlowLayout flowLayout_1 = (FlowLayout) panel_1.getLayout();
        flowLayout_1.setAlignment(FlowLayout.LEFT);
        whateverPanel.add(panel_1, "12, 2");
        
        JButton newFeederBtn = new JButton(newFeederAction);
        panel_1.add(newFeederBtn);
        
        JButton deleteFeederBtn = new JButton(deleteFeederAction);
        panel_1.add(deleteFeederBtn);
        
        JLabel lblBank = new JLabel("Bank");
        whateverPanel.add(lblBank, "2, 10, right, default");
        
        bankCb = new JComboBox();
        whateverPanel.add(bankCb, "4, 10, 3, 1");
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
        
        JLabel lblFeeder = new JLabel("Feeder");
        whateverPanel.add(lblFeeder, "2, 2, right, default");
        
        feederCb = new JComboBox();
        whateverPanel.add(feederCb, "4, 2, 3, 1");
        
        JPanel feederPanel = new JPanel();
        feederPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Feeder", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(feederPanel);
        FormLayout fl_feederPanel = new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,});
        fl_feederPanel.setColumnGroups(new int[][]{new int[]{4, 6, 8, 10}});
        feederPanel.setLayout(fl_feederPanel);
        
        JLabel lblX_1 = new JLabel("X");
        feederPanel.add(lblX_1, "4, 2");
        
        JLabel lblY_1 = new JLabel("Y");
        feederPanel.add(lblY_1, "6, 2");
        
        JLabel lblZ_1 = new JLabel("Z");
        feederPanel.add(lblZ_1, "8, 2");
        
        JLabel lblRotation_1 = new JLabel("Rotation");
        feederPanel.add(lblRotation_1, "10, 2");
        
        JLabel lblOffsets = new JLabel("Offsets");
        feederPanel.add(lblOffsets, "2, 4");
        
        xOffsetTf = new JTextField();
        feederPanel.add(xOffsetTf, "4, 4");
        xOffsetTf.setColumns(10);
        
        yOffsetTf = new JTextField();
        feederPanel.add(yOffsetTf, "6, 4");
        yOffsetTf.setColumns(10);
        
        zOffsetTf = new JTextField();
        feederPanel.add(zOffsetTf, "8, 4");
        zOffsetTf.setColumns(10);
        
        rotOffsetTf = new JTextField();
        feederPanel.add(rotOffsetTf, "10, 4");
        rotOffsetTf.setColumns(10);
        
        offsetLocButtons = new LocationButtonsPanel(xOffsetTf, yOffsetTf, zOffsetTf, rotOffsetTf);
        feederPanel.add(offsetLocButtons, "12, 4");
        
        JLabel lblPart = new JLabel("Part");
        feederPanel.add(lblPart, "2, 6, right, default");
        
        feederPartCb = new JComboBox();
        feederPanel.add(feederPartCb, "4, 6, 3, 1");
        feederPartCb.setModel(new PartsComboBoxModel());
        feederPartCb.setRenderer(new IdentifiableListCellRenderer<Part>());

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

        JLabel lblActuatorValue = new JLabel("Actuator Value");
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
        
        JLabel lblForBoolean = new JLabel("For Boolean: 1 = True, 0 = False");
        panelActuator.add(lblForBoolean, "10, 4");

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
        
        JLabel lblForBoolean_1 = new JLabel("For Boolean: 1 = True, 0 = False");
        panelActuator.add(lblForBoolean_1, "10, 6");
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
        
        JLabel lblX = new JLabel("X");
        whateverPanel.add(lblX, "4, 4, center, default");
        
        JLabel lblY = new JLabel("Y");
        whateverPanel.add(lblY, "6, 4, center, default");
        
        JLabel lblZ = new JLabel("Z");
        whateverPanel.add(lblZ, "8, 4, center, default");
        
        JLabel lblRotation = new JLabel("Rotation");
        whateverPanel.add(lblRotation, "10, 4, center, default");
        
        JLabel lblPickLocation = new JLabel("Location");
        whateverPanel.add(lblPickLocation, "2, 6, right, default");
        
        xPickLocTf = new JTextField();
        whateverPanel.add(xPickLocTf, "4, 6");
        xPickLocTf.setColumns(10);
        
        yPickLocTf = new JTextField();
        whateverPanel.add(yPickLocTf, "6, 6");
        yPickLocTf.setColumns(10);
        
        zPickLocTf = new JTextField();
        whateverPanel.add(zPickLocTf, "8, 6");
        zPickLocTf.setColumns(10);
        
        pickLocButtons = new LocationButtonsPanel(xPickLocTf, yPickLocTf, zPickLocTf, rotPickLocTf);
        
        rotPickLocTf = new JTextField();
        whateverPanel.add(rotPickLocTf, "10, 6");
        rotPickLocTf.setColumns(10);
        whateverPanel.add(pickLocButtons, "12, 6");
        
        JLabel lblRetryCount = new JLabel("Retry Count");
        whateverPanel.add(lblRetryCount, "2, 8, right, default");
        
        retryCountTf = new JTextField();
        whateverPanel.add(retryCountTf, "4, 8");
        retryCountTf.setColumns(10);
        
        bankNameTf = new JTextField();
        whateverPanel.add(bankNameTf, "8, 10, 3, 1");
        bankNameTf.setColumns(10);
        
        JPanel panel = new JPanel();
        FlowLayout flowLayout = (FlowLayout) panel.getLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        whateverPanel.add(panel, "12, 10");
        
        JButton newBankBtn = new JButton(newBankAction);
        panel.add(newBankBtn);
        
        JButton deleteBankBtn = new JButton(deleteBankAction);
        panel.add(deleteBankBtn);
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

        /**
         * Note that we set up the bindings here differently than everywhere else. In most
         * wizards the fields are bound with wrapped bindings and the proxy is bound with a hard
         * binding. Here we do the opposite so that when the user captures a new location
         * it is set on the proxy immediately. This allows the offsets to update immediately.
         * I'm not actually sure why we do it the other way everywhere else, since this seems
         * to work fine. Might not matter in most other cases. 
         */
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
        
        feederPartCb.addActionListener(e -> {
            notifyChange();
        });
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

    private Action deleteFeederAction = new AbstractAction("Delete") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Feeder feeder = (Feeder) feederCb.getSelectedItem();
            Bank bank = (Bank) bankCb.getSelectedItem();
            bank.getFeeders().remove(feeder);
            feederCb.removeItem(feeder);
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
    
    private Action deleteBankAction = new AbstractAction("Delete") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Bank bank = (Bank) bankCb.getSelectedItem();
            if (ReferenceSlotAutoFeeder.getBanks().size() < 2) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Error", "Can't delete the only bank. There must always be one bank defined.");
                return;
            }
            ReferenceSlotAutoFeeder.getBanks().remove(bank);
            bankCb.removeItem(bank);
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
