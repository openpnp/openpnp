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

import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.swingx.JXTitledSeparator;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.JBindings.Wrapper;
import org.openpnp.gui.support.LengthConverter;
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

public class ReferenceSlotAutoFeederConfigurationWizard extends AbstractConfigurationWizard {
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
        slotPanel.setBorder(null);
        contentPanel.add(slotPanel);
        FormLayout fl_slotPanel = new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.UNRELATED_GAP_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.UNRELATED_GAP_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,});
        fl_slotPanel.setColumnGroups(new int[][] {new int[] {4, 6, 8, 10}});
        slotPanel.setLayout(fl_slotPanel);

        titledSeparator = new JXTitledSeparator();
        titledSeparator.setBorder(null);
        titledSeparator.setFont(new Font("Lucida Grande", Font.BOLD, 14));
        titledSeparator.setTitle("Slot");
        slotPanel.add(titledSeparator, "2, 2, 11, 1");

        JLabel lblFeeder = new JLabel("Feeder");
        slotPanel.add(lblFeeder, "2, 4, right, default");

        feederCb = new JComboBox();
        slotPanel.add(feederCb, "4, 4, 3, 1");
        feederCb.addItem(null);

        JButton btnConfigureFeeders = new JButton(configureFeedersAction);
        btnConfigureFeeders.setText("Bank and Feeder Setup");
        slotPanel.add(btnConfigureFeeders, "8, 4, 3, 1, left, default");

        JLabel label = new JLabel("X");
        slotPanel.add(label, "4, 6, center, default");

        JLabel label_1 = new JLabel("Y");
        slotPanel.add(label_1, "6, 6, center, default");

        JLabel label_2 = new JLabel("Z");
        slotPanel.add(label_2, "8, 6, center, default");

        JLabel label_3 = new JLabel("Rotation");
        slotPanel.add(label_3, "10, 6, center, default");

        JLabel lblPickLocation = new JLabel("Location");
        slotPanel.add(lblPickLocation, "2, 8, right, default");

        xPickLocTf = new JTextField();
        slotPanel.add(xPickLocTf, "4, 8");
        xPickLocTf.setColumns(10);

        yPickLocTf = new JTextField();
        slotPanel.add(yPickLocTf, "6, 8");
        yPickLocTf.setColumns(10);

        zPickLocTf = new JTextField();
        slotPanel.add(zPickLocTf, "8, 8");
        zPickLocTf.setColumns(10);

        rotPickLocTf = new JTextField();
        slotPanel.add(rotPickLocTf, "10, 8");
        rotPickLocTf.setColumns(10);

        LocationButtonsPanel locationBtns =
                new LocationButtonsPanel(xPickLocTf, yPickLocTf, zPickLocTf, rotPickLocTf);
        slotPanel.add(locationBtns, "12, 8, left, default");

        JLabel lblRetryCount = new JLabel("Retry Count");
        slotPanel.add(lblRetryCount, "2, 10, right, default");

        retryCountTf = new JTextField();
        slotPanel.add(retryCountTf, "4, 10");
        retryCountTf.setColumns(10);

        titledSeparator_2 = new JXTitledSeparator();
        slotPanel.add(titledSeparator_2, "2, 14, 11, 1");
        titledSeparator_2.setFont(new Font("Lucida Grande", Font.BOLD, 14));
        titledSeparator_2.setBorder(null);
        titledSeparator_2.setTitle("Feeder");

        lblX = new JLabel("X");
        slotPanel.add(lblX, "4, 16, center, default");

        lblY = new JLabel("Y");
        slotPanel.add(lblY, "6, 16, center, default");

        lblZ = new JLabel("Z");
        slotPanel.add(lblZ, "8, 16, center, default");

        lblRotation = new JLabel("Rotation");
        slotPanel.add(lblRotation, "10, 16, center, default");

        JLabel lblOffsets = new JLabel("Offsets");
        slotPanel.add(lblOffsets, "2, 18, right, default");

        xOffsetsTf = new JTextField();
        slotPanel.add(xOffsetsTf, "4, 18");
        xOffsetsTf.setColumns(10);

        yOffsetsTf = new JTextField();
        slotPanel.add(yOffsetsTf, "6, 18");
        yOffsetsTf.setColumns(10);

        zOffsetsTf = new JTextField();
        slotPanel.add(zOffsetsTf, "8, 18");
        zOffsetsTf.setColumns(10);

        rotOffsetsTf = new JTextField();
        slotPanel.add(rotOffsetsTf, "10, 18");
        rotOffsetsTf.setColumns(10);
        offsetBtns = new LocationButtonsPanel(xOffsetsTf, yOffsetsTf, zOffsetsTf, rotOffsetsTf);
        slotPanel.add(offsetBtns, "12, 18, left, default");


        lblPart = new JLabel("Part");
        slotPanel.add(lblPart, "2, 20, right, default");

        comboBox = new JComboBox();
        slotPanel.add(comboBox, "4, 20");

        titledSeparator_1 = new JXTitledSeparator();
        slotPanel.add(titledSeparator_1, "2, 24, 11, 1");
        titledSeparator_1.setTitle("Actuators");
        titledSeparator_1.setFont(new Font("Lucida Grande", Font.BOLD, 14));

        JLabel lblActuatorName = new JLabel("Actuator Name");
        slotPanel.add(lblActuatorName, "4, 26");

        JLabel lblActuatorType = new JLabel("Actuator Type");
        slotPanel.add(lblActuatorType, "6, 26");

        JLabel lblActuatorValue = new JLabel("Actuator Value");
        slotPanel.add(lblActuatorValue, "8, 26");

        JLabel lblFeed = new JLabel("Feed");
        slotPanel.add(lblFeed, "2, 28, right, default");

        actuatorName = new JTextField();
        slotPanel.add(actuatorName, "4, 28");
        actuatorName.setColumns(10);

        actuatorType = new JComboBox(ActuatorType.values());
        slotPanel.add(actuatorType, "6, 28");

        actuatorValue = new JTextField();
        slotPanel.add(actuatorValue, "8, 28");
        actuatorValue.setColumns(10);

        lblforBooleanUse = new JLabel("For Boolean: True = 1, False = 0");
        slotPanel.add(lblforBooleanUse, "10, 28, 3, 1");

        JLabel lblPostPick = new JLabel("Post Pick");
        slotPanel.add(lblPostPick, "2, 30, right, default");

        postPickActuatorName = new JTextField();
        slotPanel.add(postPickActuatorName, "4, 30");
        postPickActuatorName.setColumns(10);

        postPickActuatorType = new JComboBox(ActuatorType.values());
        slotPanel.add(postPickActuatorType, "6, 30");

        postPickActuatorValue = new JTextField();
        slotPanel.add(postPickActuatorValue, "8, 30");
        postPickActuatorValue.setColumns(10);

        lblForBooleanTrue = new JLabel("For Boolean: True = 1, False = 0");
        slotPanel.add(lblForBooleanTrue, "10, 30, 3, 1");
        try {
        }
        catch (Throwable t) {
            // Swallow this error. This happens during parsing in
            // in WindowBuilder but doesn't happen during normal run.
        }

        populateFeedersCb();
    }

    private void populateFeedersCb() {
        feederCb.removeAllItems();
        for (Bank bank : ReferenceSlotAutoFeeder.getBanks()) {
            for (Feeder feeder : feeder.getBank().getFeeders()) {
                feederCb.addItem(new FeederItem(bank, feeder));
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
        addWrappedBinding(feeder, "postPickActuatorValue", postPickActuatorValue, "text",
                doubleConverter);


        addWrappedBinding(feeder, "retryCount", retryCountTf, "text", intConverter);

        MutableLocationProxy pickLocation = new MutableLocationProxy();
        addWrappedBinding(feeder, "location", pickLocation, "location");
        bind(UpdateStrategy.READ_WRITE, pickLocation, "lengthX", xPickLocTf, "text",
                lengthConverter);
        bind(UpdateStrategy.READ_WRITE, pickLocation, "lengthY", yPickLocTf, "text",
                lengthConverter);
        bind(UpdateStrategy.READ_WRITE, pickLocation, "lengthZ", zPickLocTf, "text",
                lengthConverter);
        bind(UpdateStrategy.READ_WRITE, pickLocation, "rotation", rotPickLocTf, "text",
                doubleConverter);

        Wrapper<FeederItem> feederWrapper = new Wrapper<>();
        addWrappedBinding(feeder, "feeder", feederWrapper, "value.feeder");
        bind(UpdateStrategy.READ_WRITE, feederWrapper, "value", feederCb, "selectedItem");
        
        MutableLocationProxy offsets = new MutableLocationProxy();
        addWrappedBinding(feederWrapper, "value.feeder.offsets", offsets, "location");
        bind(UpdateStrategy.READ, feeder, "location", offsetBtns, "baseLocation");
        bind(UpdateStrategy.READ, feederWrapper, "value.offsets", offsets, "location");
        bind(UpdateStrategy.READ_WRITE, offsets, "lengthX", xOffsetsTf, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, offsets, "lengthY", yOffsetsTf, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, offsets, "lengthZ", zOffsetsTf, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, offsets, "rotation", rotOffsetsTf, "text", doubleConverter);

        ComponentDecorators.decorateWithAutoSelect(actuatorName);
        ComponentDecorators.decorateWithAutoSelect(actuatorValue);
        ComponentDecorators.decorateWithAutoSelect(postPickActuatorName);
        ComponentDecorators.decorateWithAutoSelect(postPickActuatorValue);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(xPickLocTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(yPickLocTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(zPickLocTf);
        ComponentDecorators.decorateWithAutoSelect(rotPickLocTf);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(xOffsetsTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(yOffsetsTf);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(zOffsetsTf);
        ComponentDecorators.decorateWithAutoSelect(rotOffsetsTf);

        ComponentDecorators.decorateWithAutoSelect(retryCountTf);
    }

    private Action configureFeedersAction = new AbstractAction("Configure Feeders") {
        @Override
        public void actionPerformed(ActionEvent e) {
            SlotFeederConfigDialog dialog = new SlotFeederConfigDialog(MainFrame.get());
            dialog.pack();
            dialog.setLocationRelativeTo(MainFrame.get());
            dialog.setVisible(true);
            // Bank bank = (Bank) bankCb.getSelectedItem();
            // Feeder feeder = (Feeder) feederCb.getSelectedItem();
            // bankCb.setSelectedItem(bank);
            // feederCb.setSelectedItem(feeder);
        }
    };

    private Action configureBanksAction = new AbstractAction("Configure Banks") {
        @Override
        public void actionPerformed(ActionEvent e) {
            SlotBankConfigDialog dialog = new SlotBankConfigDialog(MainFrame.get());
            dialog.pack();
            dialog.setLocationRelativeTo(MainFrame.get());
            dialog.setVisible(true);
            // Bank bank = (Bank) bankCb.getSelectedItem();
            // Feeder feeder = (Feeder) feederCb.getSelectedItem();
            // bankCb.removeAllItems();
            // for (Bank b : ReferenceSlotAutoFeeder.getBanks()) {
            // bankCb.addItem(b);
            // }
            // bankCb.setSelectedItem(bank);
            // feederCb.setSelectedItem(feeder);
        }
    };

    static class FeederItem {
        final Bank bank;
        final Feeder feeder;

        public FeederItem(Bank bank, Feeder feeder) {
            this.bank = bank;
            this.feeder = feeder;
        }

        public Bank getBank() {
            return bank;
        }

        public Feeder getFeeder() {
            return feeder;
        }

        @Override
        public String toString() {
            return String.format("%s : %s", bank.getName(), feeder.getName());
        }
    }

    private JComboBox feederCb;
    private JTextField xPickLocTf;
    private JTextField yPickLocTf;
    private JTextField zPickLocTf;
    private JTextField rotPickLocTf;
    private JTextField retryCountTf;
    private JTextField xOffsetsTf;
    private JTextField yOffsetsTf;
    private JTextField zOffsetsTf;
    private JTextField rotOffsetsTf;
    private LocationButtonsPanel offsetBtns;
    private JXTitledSeparator titledSeparator;
    private JXTitledSeparator titledSeparator_1;
    private JXTitledSeparator titledSeparator_2;
    private LocationButtonsPanel locationButtonsPanel;
    private JLabel lblPart;
    private JComboBox comboBox;
    private JLabel lblforBooleanUse;
    private JLabel lblForBooleanTrue;
    private JLabel lblX;
    private JLabel lblY;
    private JLabel lblZ;
    private JLabel lblRotation;
}
