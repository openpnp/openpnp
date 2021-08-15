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
import org.I18n.I18n;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.machine.reference.feeder.SchultzFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.base.AbstractActuator;
import org.openpnp.util.UiUtils;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JComboBox;

@SuppressWarnings("serial")
public class SchultzFeederConfigurationWizard extends AbstractReferenceFeederConfigurationWizard {
    private final SchultzFeeder feeder;

    private JComboBox comboBoxFeedActuator;
    private JTextField actuatorValue;

    private JComboBox comboBoxPostPickActuator;
    private JTextField postPickActuatorValue;

    private JButton btnTestFeedActuator;
    private JButton btnTestPostPickActuator;

    private JComboBox comboBoxFeedCountActuator;
    private JTextField feedCountActuatorValue;
    private JButton btnGetFeedCountActuator;
    private JTextField feedCountValue;

    private JComboBox comboBoxClearCountActuator;
    private JTextField clearCountActuatorValue;
    private JButton btnClearCountActuator;

    private JComboBox comboBoxPitchActuator;
    private JTextField pitchActuatorValue;
    private JButton btnPitchActuator;
    private JTextField pitchValue;

    private JComboBox comboBoxTogglePitchActuator;
    private JTextField togglePitchActuatorValue;
    private JButton btnTogglePitchActuator;

    private JComboBox comboBoxStatusActuator;
    private JTextField statusActuatorValue;
    private JButton btnStatusActuator;
    private JTextField statusText;

    private JComboBox comboBoxIdActuator;
    private JTextField idActuatorValue;
    private JButton btnIdActuator;
    private JTextField idText;

    public SchultzFeederConfigurationWizard(SchultzFeeder feeder) {
        super(feeder);
        this.feeder = feeder;

        JPanel panelActuator = new JPanel();
        panelActuator.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                I18n.gettext("Actuators"), TitledBorder.LEADING, TitledBorder.TOP, null));
        contentPanel.add(panelActuator);
        panelActuator.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
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

        JLabel lblActuatorValue = new JLabel(I18n.gettext("Feeder Number:"));
        panelActuator.add(lblActuatorValue, "4, 2, right, default");

        actuatorValue = new JTextField();
        panelActuator.add(actuatorValue, "6, 2");
        actuatorValue.setColumns(6);

        JLabel lblActuator = new JLabel(I18n.gettext("Actuator"));
        panelActuator.add(lblActuator, "4, 4, left, default");

        JLabel lblGetID = new JLabel(I18n.gettext("Get ID"));
        panelActuator.add(lblGetID, "2, 6, right, default");

        comboBoxIdActuator = new JComboBox();
        comboBoxIdActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
        panelActuator.add(comboBoxIdActuator, "4, 6, fill, default");

        btnIdActuator = new JButton(getIdActuatorAction);
        panelActuator.add(btnIdActuator, "6, 6");

        idText = new JTextField();
        idText.setColumns(10);
        panelActuator.add(idText, "8, 6");

        JLabel lblFeed = new JLabel(I18n.gettext("Pre Pick"));
        panelActuator.add(lblFeed, "2, 8, right, default");

        comboBoxFeedActuator = new JComboBox();
        comboBoxFeedActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
        panelActuator.add(comboBoxFeedActuator, "4, 8, fill, default");

        btnTestFeedActuator = new JButton(testFeedActuatorAction);
        panelActuator.add(btnTestFeedActuator, "6, 8");

        JLabel lblPostPick = new JLabel(I18n.gettext("Post Pick"));
        panelActuator.add(lblPostPick, "2, 10, right, default");

        comboBoxPostPickActuator = new JComboBox();
        comboBoxPostPickActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
        panelActuator.add(comboBoxPostPickActuator, "4, 10, fill, default");

        btnTestPostPickActuator = new JButton(testPostPickActuatorAction);
        panelActuator.add(btnTestPostPickActuator, "6, 10");

        JLabel lblFeedCount = new JLabel(I18n.gettext("Get Feed Count"));
        panelActuator.add(lblFeedCount, "2, 12, right, default");

        comboBoxFeedCountActuator = new JComboBox();
        comboBoxFeedCountActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
        panelActuator.add(comboBoxFeedCountActuator, "4, 12, fill, default");

        btnGetFeedCountActuator = new JButton(getFeedCountActuatorAction);
        panelActuator.add(btnGetFeedCountActuator, "6, 12");

        feedCountValue = new JTextField();
        feedCountValue.setColumns(8);
        panelActuator.add(feedCountValue, "8, 12");

        JLabel lblClearCount = new JLabel(I18n.gettext("Clear Feed Count"));
        panelActuator.add(lblClearCount, "2, 14, right, default");

        comboBoxClearCountActuator = new JComboBox();
        comboBoxClearCountActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
        panelActuator.add(comboBoxClearCountActuator, "4, 14, fill, default");

        btnClearCountActuator = new JButton(clearCountActuatorAction);
        panelActuator.add(btnClearCountActuator, "6, 14");

        JLabel lblGetPitch = new JLabel(I18n.gettext("Get Pitch"));
        panelActuator.add(lblGetPitch, "2, 16, right, default");

        comboBoxPitchActuator = new JComboBox();
        comboBoxPitchActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
        panelActuator.add(comboBoxPitchActuator, "4, 16, fill, default");

        btnPitchActuator = new JButton(pitchActuatorAction);
        panelActuator.add(btnPitchActuator, "6, 16");

        pitchValue = new JTextField();
        pitchValue.setColumns(8);
        panelActuator.add(pitchValue, "8, 16");

        JLabel lblTogglePitch = new JLabel(I18n.gettext("Toggle Pitch"));
        panelActuator.add(lblTogglePitch, "2, 18, right, default");

        comboBoxTogglePitchActuator = new JComboBox();
        comboBoxTogglePitchActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
        panelActuator.add(comboBoxTogglePitchActuator, "4, 18, fill, default");

        btnTogglePitchActuator = new JButton(togglePitchActuatorAction);
        panelActuator.add(btnTogglePitchActuator, "6, 18");

        JLabel lblTogglePitchDesc = new JLabel(I18n.gettext("Toggle between 2 MM and 4 MM"));
        panelActuator.add(lblTogglePitchDesc, "8, 18, left, default");

        JLabel lblGetStatus = new JLabel(I18n.gettext("Get Status"));
        panelActuator.add(lblGetStatus, "2, 20, right, default");

        comboBoxStatusActuator = new JComboBox();
        comboBoxStatusActuator.setModel(new ActuatorsComboBoxModel(Configuration.get().getMachine()));
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

    }

    @Override
    public void createBindings() {
        super.createBindings();

        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        addWrappedBinding(feeder, "actuatorName", comboBoxFeedActuator, "selectedItem");
        addWrappedBinding(feeder, "actuatorValue", actuatorValue, "text", doubleConverter);

        addWrappedBinding(feeder, "postPickActuatorName", comboBoxPostPickActuator, "selectedItem");
        addWrappedBinding(feeder, "feedCountActuatorName", comboBoxFeedCountActuator, "selectedItem");
        addWrappedBinding(feeder, "clearCountActuatorName", comboBoxClearCountActuator, "selectedItem");
        addWrappedBinding(feeder, "pitchActuatorName", comboBoxPitchActuator, "selectedItem");
        addWrappedBinding(feeder, "togglePitchActuatorName", comboBoxTogglePitchActuator, "selectedItem");
        addWrappedBinding(feeder, "statusActuatorName", comboBoxStatusActuator, "selectedItem");
        addWrappedBinding(feeder, "idActuatorName", comboBoxIdActuator, "selectedItem");

        ComponentDecorators.decorateWithAutoSelect(actuatorValue);
    }

    private Action getIdActuatorAction = new AbstractAction("Get ID") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (!(Configuration.get().getMachine().isEnabled())) {
                    throw new Exception (I18n.gettext("Start machine first."));
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
                String s = actuator.read(feeder.getActuatorValue());
                idText.setText(s == null ? "" : s);
            });
        }
    };

    private Action testFeedActuatorAction = new AbstractAction("Test pre pick") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (!(Configuration.get().getMachine().isEnabled())) {
                    throw new Exception (I18n.gettext("Start machine first."));
                }
                if (feeder.getActuatorName() == null || feeder.getActuatorName().equals("")) {
                    Logger.warn("No actuatorName specified for feeder {}.", feeder.getName());
                    return;
                }
                Actuator actuator = Configuration.get().getMachine().getActuatorByName(feeder.getActuatorName());

                if (actuator == null) {
                    throw new Exception(I18n.gettext("Feed failed. Unable to find an actuator named ") + feeder.getActuatorName());
                }
                AbstractActuator.suggestValueType(actuator, Actuator.ActuatorValueType.Double);
                actuator.actuate(feeder.getActuatorValue());
            });
        }
    };

    private Action testPostPickActuatorAction = new AbstractAction("Test post pick") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (!(Configuration.get().getMachine().isEnabled())) {
                    throw new Exception (I18n.gettext("Start machine first."));
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
                AbstractActuator.suggestValueType(actuator, Actuator.ActuatorValueType.Double);
                actuator.actuate(feeder.getActuatorValue());
                getFeedCountActuatorAction.actionPerformed(null);
            });
        }
    };

    private Action getFeedCountActuatorAction = new AbstractAction("Get feed count") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (!(Configuration.get().getMachine().isEnabled())) {
                    throw new Exception (I18n.gettext("Start machine first."));
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
                String s = actuator.read(feeder.getActuatorValue());
                feedCountValue.setText(s == null ? "" : s);
            });
        }
    };

    private Action clearCountActuatorAction = new AbstractAction("Clear feed count") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (!(Configuration.get().getMachine().isEnabled())) {
                    throw new Exception (I18n.gettext("Start machine first."));
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

                AbstractActuator.suggestValueType(actuator, Actuator.ActuatorValueType.Double);
                actuator.actuate(feeder.getActuatorValue());
                feedCountValue.setText("");
            });
        }
    };

    private Action pitchActuatorAction = new AbstractAction("Get pitch") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (!(Configuration.get().getMachine().isEnabled())) {
                    throw new Exception (I18n.gettext("Start machine first."));
                }
                if (feeder.getPitchActuatorName() == null || feeder.getPitchActuatorName().equals("")) {
                    Logger.warn("No pitchActuatorName specified for feeder {}.", feeder.getName());
                    return;
                }
                Actuator actuator = Configuration.get().getMachine()
                        .getActuatorByName(feeder.getPitchActuatorName());

                if (actuator == null) {
                    throw new Exception(
                            "Failed, unable to find an actuator named " + feeder.getPitchActuatorName());
                }
                String s = actuator.read(feeder.getActuatorValue());
                pitchValue.setText(s == null ? "" : s);
            });
        }
    };

    private Action togglePitchActuatorAction = new AbstractAction("Toggle pitch") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (!(Configuration.get().getMachine().isEnabled())) {
                    throw new Exception (I18n.gettext("Start machine first."));
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
                AbstractActuator.suggestValueType(actuator, Actuator.ActuatorValueType.Double);
                actuator.actuate(feeder.getActuatorValue());
                pitchActuatorAction.actionPerformed(null);
            });
        }
    };

    private Action statusActuatorAction = new AbstractAction("Get status") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                if (!(Configuration.get().getMachine().isEnabled())) {
                    throw new Exception (I18n.gettext("Start machine first."));
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
                String s = actuator.read(feeder.getActuatorValue());
                statusText.setText(s == null ? "" : s);
            });
        }
    };
}
