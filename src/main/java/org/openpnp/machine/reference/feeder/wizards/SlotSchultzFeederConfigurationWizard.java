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

import java.awt.FlowLayout;
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
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.Converter;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.*;
import org.openpnp.gui.support.JBindings.Wrapper;
import org.openpnp.machine.reference.feeder.SlotSchultzFeeder;
import org.openpnp.machine.reference.feeder.SlotSchultzFeeder.Bank;
import org.openpnp.machine.reference.feeder.SlotSchultzFeeder.Feeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.base.AbstractActuator;
import org.openpnp.util.UiUtils;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import java.util.Objects;

public class SlotSchultzFeederConfigurationWizard
extends AbstractConfigurationWizard {
    private final SlotSchultzFeeder feeder;

    private JComboBox comboBoxFeedActuator;
    private JTextField actuatorValue;

    private JComboBox comboBoxPostPickActuator;

    private JButton btnTestFeedActuator;
    private JButton btnTestPostPickActuator;

    private JComboBox comboBoxFeedCountActuator;
    private JButton btnGetFeedCountActuator;
    private JTextField feedCountValue;

    private JComboBox comboBoxClearCountActuator;
    private JButton btnClearCountActuator;

    private JComboBox comboBoxPitchActuator;
    private JButton btnPitchActuator;
    private JTextField pitchValue;

    private JComboBox comboBoxTogglePitchActuator;
    private JButton btnTogglePitchActuator;

    private JComboBox comboBoxStatusActuator;
    private JButton btnStatusActuator;
    private JTextField statusText;

    private JComboBox comboBoxIdActuator;
    private JButton btnIdActuator;
    private JTextField idText;

    private JTextField feederNameTf;
    private JTextField bankNameTf;

    private JComboBox feederCb;
    private JComboBox bankCb;
    private JComboBox feederPartCb;
    private JTextField fiducialPartTf;
    private JTextField xOffsetTf;
    private JTextField yOffsetTf;
    private JTextField zOffsetTf;
    private JTextField rotOffsetTf;
    private JTextField xPickLocTf;
    private JTextField yPickLocTf;
    private JTextField zPickLocTf;
    private JTextField rotPickLocTf;
    private LocationButtonsPanel offsetLocButtons;
    private LocationButtonsPanel pickLocButtons;
    private JTextField feedRetryCount;
    private JTextField pickRetryCount;

    public SlotSchultzFeederConfigurationWizard(SlotSchultzFeeder feeder) {
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

        JButton loadFeederBtn = new JButton(loadFeederAction);
        loadFeederBtn.setToolTipText("Load installed feeder to slot.");
        panel_1.add(loadFeederBtn);

        //        JButton newFeederBtn = new JButton(newFeederAction);
        //        panel_1.add(newFeederBtn);

        JButton deleteFeederBtn = new JButton(deleteFeederAction);
        deleteFeederBtn.setToolTipText("Remove selected feeder from database.");
        panel_1.add(deleteFeederBtn);

        JLabel lblPickRetryCount = new JLabel("Pick Retry Count");
        whateverPanel.add(lblPickRetryCount, "2, 12, right, default");

        pickRetryCount = new JTextField();
        pickRetryCount.setColumns(10);
        whateverPanel.add(pickRetryCount, "4, 12, fill, default");

        JLabel lblBank = new JLabel("Bank");
        whateverPanel.add(lblBank, "2, 14, right, default");

        bankCb = new JComboBox();
        whateverPanel.add(bankCb, "4, 14, 3, 1");
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
        feederPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Feeder", TitledBorder.LEADING, TitledBorder.TOP, null));
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

        //        offsetLocButtons = new LocationButtonsPanel(xOffsetTf, yOffsetTf, zOffsetTf, rotOffsetTf);
        offsetLocButtons = new LocationButtonsPanel(xOffsetTf, yOffsetTf, zOffsetTf, null);
        feederPanel.add(offsetLocButtons, "12, 4");

        JLabel lblPart = new JLabel("Part");
        feederPanel.add(lblPart, "2, 6, right, default");

        feederPartCb = new JComboBox();
        feederPanel.add(feederPartCb, "4, 6, 3, 1");
        feederPartCb.setModel(new PartsComboBoxModel());
        feederPartCb.setRenderer(new IdentifiableListCellRenderer<Part>());

        JPanel panelActuator = new JPanel();
        panelActuator.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Actuators", TitledBorder.LEADING, TitledBorder.TOP, null));
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
                        FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblActuatorValue = new JLabel("Feeder Number:");
        panelActuator.add(lblActuatorValue, "4, 2, right, default");

        actuatorValue = new JTextField();
        panelActuator.add(actuatorValue, "6, 2");
        actuatorValue.setColumns(6);

        JLabel lblActuator = new JLabel("Actuator");
        panelActuator.add(lblActuator, "4, 4, left, default");

        JLabel lblGetID = new JLabel("Get ID");
        panelActuator.add(lblGetID, "2, 6, right, default");

        comboBoxIdActuator = new JComboBox();
        comboBoxIdActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine(), null));
        panelActuator.add(comboBoxIdActuator, "4, 6, fill, default");

        btnIdActuator = new JButton(getIdActuatorAction);
        panelActuator.add(btnIdActuator, "6, 6");

        idText = new JTextField();
        idText.setColumns(10);
        panelActuator.add(idText, "8, 6");

        JLabel lblFeed = new JLabel("Pre Pick");
        panelActuator.add(lblFeed, "2, 8, right, default");

        comboBoxFeedActuator = new JComboBox();
        comboBoxFeedActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine(), Double.class));
        panelActuator.add(comboBoxFeedActuator, "4, 8, fill, default");

        btnTestFeedActuator = new JButton(testFeedActuatorAction);
        panelActuator.add(btnTestFeedActuator, "6, 8");

        JLabel lblPostPick = new JLabel("Post Pick");
        panelActuator.add(lblPostPick, "2, 10, right, default");

        comboBoxPostPickActuator = new JComboBox();
        comboBoxPostPickActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine(), Double.class));
        panelActuator.add(comboBoxPostPickActuator, "4, 10, fill, default");

        btnTestPostPickActuator = new JButton(testPostPickActuatorAction);
        panelActuator.add(btnTestPostPickActuator, "6, 10");

        JLabel lblFeedCount = new JLabel("Get Feed Count");
        panelActuator.add(lblFeedCount, "2, 12, right, default");

        comboBoxFeedCountActuator = new JComboBox();
        comboBoxFeedCountActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine(), Double.class));
        panelActuator.add(comboBoxFeedCountActuator, "4, 12, fill, default");

        btnGetFeedCountActuator = new JButton(getFeedCountActuatorAction);
        panelActuator.add(btnGetFeedCountActuator, "6, 12");

        feedCountValue = new JTextField();
        feedCountValue.setColumns(8);
        panelActuator.add(feedCountValue, "8, 12");

        JLabel lblClearCount = new JLabel("Clear Feed Count");
        panelActuator.add(lblClearCount, "2, 14, right, default");

        comboBoxClearCountActuator = new JComboBox();
        comboBoxClearCountActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine(), Double.class));
        panelActuator.add(comboBoxClearCountActuator, "4, 14, fill, default");

        btnClearCountActuator = new JButton(clearCountActuatorAction);
        panelActuator.add(btnClearCountActuator, "6, 14");

        JLabel lblGetPitch = new JLabel("Get Pitch");
        panelActuator.add(lblGetPitch, "2, 16, right, default");

        comboBoxPitchActuator = new JComboBox();
        comboBoxPitchActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine(), Double.class));
        panelActuator.add(comboBoxPitchActuator, "4, 16, fill, default");

        btnPitchActuator = new JButton(pitchActuatorAction);
        panelActuator.add(btnPitchActuator, "6, 16");

        pitchValue = new JTextField();
        pitchValue.setColumns(8);
        panelActuator.add(pitchValue, "8, 16");

        JLabel lblTogglePitch = new JLabel("Toggle Pitch");
        panelActuator.add(lblTogglePitch, "2, 18, right, default");

        comboBoxTogglePitchActuator = new JComboBox();
        comboBoxTogglePitchActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine(), Double.class));
        panelActuator.add(comboBoxTogglePitchActuator, "4, 18, fill, default");

        btnTogglePitchActuator = new JButton(togglePitchActuatorAction);
        panelActuator.add(btnTogglePitchActuator, "6, 18");

        JLabel lblTogglePitchDesc = new JLabel("Toggle between 2 MM and 4 MM");
        panelActuator.add(lblTogglePitchDesc, "8, 18, left, default");

        JLabel lblGetStatus = new JLabel("Get Status");
        panelActuator.add(lblGetStatus, "2, 20, right, default");

        comboBoxStatusActuator = new JComboBox();
        comboBoxStatusActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine(), Double.class));
        panelActuator.add(comboBoxStatusActuator, "4, 20, fill, default");

        btnStatusActuator = new JButton(statusActuatorAction);
        panelActuator.add(btnStatusActuator, "6, 20");

        statusText = new JTextField();
        statusText.setColumns(50);
        panelActuator.add(statusText, "8, 20");

        if(Configuration.get().getMachine().isEnabled()){
            getIdActuatorAction.actionPerformed(null);
            getFeedCountActuatorAction.actionPerformed(null);
            pitchActuatorAction.actionPerformed(null);
            statusActuatorAction.actionPerformed(null);
        }

        for (Bank bank : SlotSchultzFeeder.getBanks()) {
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

        JButton fiducialAlign = new JButton(updateLocationAction);
        whateverPanel.add(fiducialAlign, "14, 6");
        fiducialAlign.setIcon(Icons.fiducialCheck);
        fiducialAlign.setToolTipText("Update feeder location based on fiducial");

        JLabel lblFiducialPart = new JLabel("Fiducial Part");
        whateverPanel.add(lblFiducialPart, "2, 8, right, default");

        fiducialPartTf = new JTextField();
        whateverPanel.add(fiducialPartTf, "4, 8, 3, 1");
        fiducialPartTf.addActionListener(e -> {
            feeder.setFiducialPart(fiducialPartTf.getText());
        });

        JLabel lblFeedRetryCount = new JLabel("Feed Retry Count");
        whateverPanel.add(lblFeedRetryCount, "2, 10, right, default");

        feedRetryCount = new JTextField();
        whateverPanel.add(feedRetryCount, "4, 10");
        feedRetryCount.setColumns(10);

        bankNameTf = new JTextField();
        whateverPanel.add(bankNameTf, "8, 14, 3, 1");
        bankNameTf.setColumns(10);

        JPanel panel = new JPanel();
        FlowLayout flowLayout = (FlowLayout) panel.getLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        whateverPanel.add(panel, "12, 14");

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

        addWrappedBinding(feeder, "fiducialPart", fiducialPartTf, "text");
        addWrappedBinding(feeder, "feedRetryCount", feedRetryCount, "text", intConverter);
        addWrappedBinding(feeder, "pickRetryCount", pickRetryCount, "text", intConverter);

        addWrappedBinding(feeder, "actuatorName", comboBoxFeedActuator, "selectedItem");
        addWrappedBinding(feeder, "actuatorValue", actuatorValue, "text", doubleConverter);

        addWrappedBinding(feeder, "postPickActuatorName", comboBoxPostPickActuator, "selectedItem");

        addWrappedBinding(feeder, "feedCountActuatorName", comboBoxFeedCountActuator, "selectedItem");

        addWrappedBinding(feeder, "clearCountActuatorName", comboBoxClearCountActuator, "selectedItem");

        addWrappedBinding(feeder, "pitchActuatorName", comboBoxPitchActuator, "selectedItem");

        addWrappedBinding(feeder, "togglePitchActuatorName", comboBoxTogglePitchActuator, "selectedItem");

        addWrappedBinding(feeder, "statusActuatorName", comboBoxStatusActuator, "selectedItem");

        addWrappedBinding(feeder, "idActuatorName", comboBoxIdActuator, "selectedItem");

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

        ComponentDecorators.decorateWithAutoSelect(fiducialPartTf);

        ComponentDecorators.decorateWithAutoSelect(feedRetryCount);
        ComponentDecorators.decorateWithAutoSelect(pickRetryCount);

        feederPartCb.addActionListener(e -> {
            notifyChange();
        });

    }

    private Action loadFeederAction = new AbstractAction("Load") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Bank bank = (Bank) bankCb.getSelectedItem();
            Feeder f = new Feeder(idText.getText());
            Feeder item;
            int i;
            for (i = 1; i < feederCb.getItemCount(); i++) {
                item = (Feeder) feederCb.getItemAt(i);
                if (item.getName().equals(f.getName()))  {
                    feederCb.setSelectedIndex(i);
                    break;
                }
            }
            if (i == feederCb.getItemCount()) {	  // list did not contain feeder, so create it
                Logger.warn("No feeder {} exists in bank, so creating new.", f);
                bank.getFeeders().add(f);
                feederCb.addItem(f);
                feederCb.setSelectedItem(f);
                xOffsetTf.setText("-5");		// set default offsets for new feeder
                yOffsetTf.setText("-30");
            }
        }
    };

    /*    private Action newFeederAction = new AbstractAction("New") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Bank bank = (Bank) bankCb.getSelectedItem();
            Feeder f = new Feeder(idText.getText());
            bank.getFeeders().add(f);
            feederCb.addItem(f);
            feederCb.setSelectedItem(f);
        	xOffsetTf.setText("-5");		// set default offsets for new feeder
        	yOffsetTf.setText("-30");
        }
    };
     */

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
            SlotSchultzFeeder.getBanks().add(bank);
            bankCb.addItem(bank);
            bankCb.setSelectedItem(bank);
        }
    };

    private Action deleteBankAction = new AbstractAction("Delete") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Bank bank = (Bank) bankCb.getSelectedItem();
            if (SlotSchultzFeeder.getBanks().size() < 2) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Error", "Can't delete the only bank. There must always be one bank defined.");
                return;
            }
            SlotSchultzFeeder.getBanks().remove(bank);
            bankCb.removeItem(bank);
        }
    };

    private Action getIdActuatorAction = new AbstractAction("Get ID") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (!(Configuration.get().getMachine().isEnabled())) {
                    throw new Exception ("Start machine first.");
                }
                if (feeder.getIdActuatorName() == null || feeder.getIdActuatorName().equals("")) {
                    Logger.warn("No getIdActuatorName specified for feeder {}.", feeder.getName());
                    return;
                }
                Actuator actuator = Configuration.get().getMachine()
                        .getActuatorByName(feeder.getIdActuatorName());

                if (actuator == null) {
                    throw new Exception(
                            "Failed, unable to find an actuator named " + feeder.getIdActuatorName());
                }
                Object o = actuator.read(feeder.getActuatorValue());
                Converter<Object, String> converter = Converters.getConverter(actuator.getValueClass());
                idText.setText(Objects.toString(converter.convertForward(o), ""));
            });
        }
    };

    private Action testFeedActuatorAction = new AbstractAction("Test pre pick") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (!(Configuration.get().getMachine().isEnabled())) {
                    throw new Exception ("Start machine first.");
                }
                if (feeder.getActuatorName() == null || feeder.getActuatorName().equals("")) {
                    Logger.warn("No actuatorName specified for feeder {}.", feeder.getName());
                    return;
                }
                Actuator actuator = Configuration.get().getMachine().getActuatorByName(feeder.getActuatorName());

                if (actuator == null) {
                    throw new Exception("Feed failed. Unable to find an actuator named " + feeder.getActuatorName());
                }
                AbstractActuator.suggestValueClass(actuator, Double.class);
                actuator.actuate(feeder.getActuatorValue());
            });
        }
    };

    private Action testPostPickActuatorAction = new AbstractAction("Test post pick") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (!(Configuration.get().getMachine().isEnabled())) {
                    throw new Exception ("Start machine first.");
                }
                if (feeder.getPostPickActuatorName() == null || feeder.getPostPickActuatorName().equals("")) {
                    Logger.warn("No postPickActuatorName specified for feeder {}.", feeder.getName());
                    return;
                }
                Actuator actuator = Configuration.get().getMachine()
                        .getActuatorByName(feeder.getPostPickActuatorName());

                if (actuator == null) {
                    throw new Exception(
                            "Feed failed. Unable to find an actuator named " + feeder.getPostPickActuatorName());
                }
                AbstractActuator.suggestValueClass(actuator, Double.class);
                actuator.actuate(feeder.getActuatorValue());
            });
        }
    };

    private Action getFeedCountActuatorAction = new AbstractAction("Get feed count") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (!(Configuration.get().getMachine().isEnabled())) {
                    throw new Exception ("Start machine first.");
                }
                if (feeder.getFeedCountActuatorName() == null || feeder.getFeedCountActuatorName().equals("")) {
                    Logger.warn("No feedCountActuatorName specified for feeder {}.", feeder.getName());
                    return;
                }
                Actuator actuator = Configuration.get().getMachine()
                        .getActuatorByName(feeder.getFeedCountActuatorName());

                if (actuator == null) {
                    throw new Exception(
                            "Failed, unable to find an actuator named " + feeder.getFeedCountActuatorName());
                }
                Object o = actuator.read(feeder.getActuatorValue());
                Converter<Object, String> converter = Converters.getConverter(actuator.getValueClass());
                feedCountValue.setText(Objects.toString(converter.convertForward(o), ""));
            });
        }
    };

    private Action clearCountActuatorAction = new AbstractAction("Clear feed count") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                if (!(Configuration.get().getMachine().isEnabled())) {
                    throw new Exception ("Start machine first.");
                }
                if (feeder.getClearCountActuatorName() == null || feeder.getClearCountActuatorName().equals("")) {
                    Logger.warn("No clearCountActuatorName specified for feeder {}.", feeder.getName());
                    return;
                }
                Actuator actuator = Configuration.get().getMachine()
                        .getActuatorByName(feeder.getClearCountActuatorName());

                if (actuator == null) {
                    throw new Exception(
                            "Failed, unable to find an actuator named " + feeder.getClearCountActuatorName());
                }
                AbstractActuator.suggestValueClass(actuator, Double.class);
                actuator.actuate(feeder.getActuatorValue());
                feedCountValue.setText("");
            });
        }
    };

    private Action pitchActuatorAction = new AbstractAction("Get pitch") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                if (!(Configuration.get().getMachine().isEnabled())) {
                    throw new Exception ("Start machine first.");
                }
                if (feeder.getPitchActuatorName() == null || feeder.getPitchActuatorName().equals("")) {
                    Logger.warn("No feedCountActuatorName specified for feeder {}.", feeder.getName());
                    return;
                }
                Actuator actuator = Configuration.get().getMachine()
                        .getActuatorByName(feeder.getPitchActuatorName());

                if (actuator == null) {
                    throw new Exception(
                            "Failed, unable to find an actuator named " + feeder.getPitchActuatorName());
                }
                Object o = actuator.read(feeder.getActuatorValue());
                Converter<Object, String> converter = Converters.getConverter(actuator.getValueClass());
                pitchValue.setText(Objects.toString(converter.convertForward(o), ""));
            });
        }
    };

    private Action togglePitchActuatorAction = new AbstractAction("Toggle pitch") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (!(Configuration.get().getMachine().isEnabled())) {
                    throw new Exception ("Start machine first.");
                }
                if (feeder.getTogglePitchActuatorName() == null || feeder.getTogglePitchActuatorName().equals("")) {
                    Logger.warn("No togglePitchActuatorName specified for feeder {}.", feeder.getName());
                    return;
                }
                Actuator actuator = Configuration.get().getMachine()
                        .getActuatorByName(feeder.getTogglePitchActuatorName());

                if (actuator == null) {
                    throw new Exception(
                            "Failed, unable to find an actuator named " + feeder.getTogglePitchActuatorName());
                }
                AbstractActuator.suggestValueClass(actuator, Double.class);
                actuator.actuate(feeder.getActuatorValue());
                pitchActuatorAction.actionPerformed(null);
            });
        }
    };

    private Action statusActuatorAction =  new AbstractAction("Get status") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (!(Configuration.get().getMachine().isEnabled())) {
                    throw new Exception ("Start machine first.");
                }
                if (feeder.getStatusActuatorName() == null || feeder.getStatusActuatorName().equals("")) {
                    Logger.warn("No statusActuatorName specified for feeder {}.", feeder.getName());
                    return;
                }
                Actuator actuator = Configuration.get().getMachine()
                        .getActuatorByName(feeder.getStatusActuatorName());

                if (actuator == null) {
                    throw new Exception(
                            "Failed, unable to find an actuator named " + feeder.getStatusActuatorName());
                }
                Object o = actuator.read(feeder.getActuatorValue());
                Converter<Object, String> converter = Converters.getConverter(actuator.getValueClass());
                statusText.setText(Objects.toString(converter.convertForward(o), ""));
            });
        }
    };

    private Action updateLocationAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                // make sure machine is powered on
                if(Configuration.get().getMachine().isEnabled()) {
                    if (feeder.getFiducialPart() == null) {
                        Logger.warn("No fiducial defined for feeder {}.", feeder.getName());
                        return;
                    }
                    Location newLocation = feeder.getFiducialLocation(feeder.getLocation(), feeder.getFiducialPart());
                    if (newLocation == null) {
                        throw new Exception("Unable to locate fiducial");
                    } else {
                        xPickLocTf.setText(newLocation.getLengthX().toString());
                        yPickLocTf.setText(newLocation.getLengthY().toString());
                    }
                }
            });
        }
    };
}
