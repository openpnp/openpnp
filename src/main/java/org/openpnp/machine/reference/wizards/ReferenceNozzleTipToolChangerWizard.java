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

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.ActuatorsComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Machine;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.UiUtils;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceNozzleTipToolChangerWizard extends AbstractConfigurationWizard {
    private final ReferenceNozzleTip nozzleTip;
    private JPanel panelChanger;
    private JLabel lblY_1;
    private JLabel lblZ_1;
    private LocationButtonsPanel changerStartLocationButtonsPanel;
    private JLabel lblStartLocation;
    private JTextField textFieldChangerStartX;
    private JTextField textFieldChangerStartY;
    private JTextField textFieldChangerStartZ;
    private JLabel lblMiddleLocation;
    private JTextField textFieldChangerMidX;
    private JTextField textFieldChangerMidY;
    private JTextField textFieldChangerMidZ;
    private JLabel lblEndLocation;
    private JTextField textFieldChangerEndX;
    private JTextField textFieldChangerEndY;
    private JTextField textFieldChangerEndZ;
    private LocationButtonsPanel changerMidLocationButtonsPanel;
    private LocationButtonsPanel changerEndLocationButtonsPanel;

    private Set<org.openpnp.model.Package> compatiblePackages = new HashSet<>();
    private JLabel lblMiddleLocation_1;
    private JTextField textFieldMidX2;
    private JTextField textFieldMidY2;
    private JTextField textFieldMidZ2;
    private LocationButtonsPanel changerMidButtons2;
    private JTextField textFieldChangerStartToMidSpeed;
    private JTextField textFieldChangerMidToMid2Speed;
    private JTextField textFieldChangerMid2ToEndSpeed;
    private JLabel lblSpeed;
    private JLabel lblX;
    private JLabel lblSpeed1_2;
    private JLabel lblSpeed2_3;
    private JLabel lblSpeed3_4;
    private JLabel label;
    private JPanel panel;
    private JComboBox tcPostOneComboBoxActuator;
    private JComboBox tcPostTwoComboBoxActuator;
    private JComboBox tcPostThreeComboBoxActuator;
    private JComboBox copyFromNozzleTip;
    private JLabel lblCopyFrom;
    private JButton btnNewButton;

    private ArrayList<ReferenceNozzleTip> nozzleTips;
    public ReferenceNozzleTipToolChangerWizard(ReferenceNozzleTip nozzleTip) {
        this.nozzleTip = nozzleTip;

        panelChanger = new JPanel();
        panelChanger.setBorder(new TitledBorder(null, "Nozzle Tip Changer", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelChanger);
        panelChanger.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        
        label = new JLabel("Post 1 Actuator");
        panelChanger.add(label, "2, 5, right, center");
        label = new JLabel("Post 2 Actuator");
        panelChanger.add(label, "2, 7, right, center");
        label = new JLabel("Post 3 Actuator");
        panelChanger.add(label, "2, 9, right, center");

        Machine myMachine = null;
        try {
            myMachine = Configuration.get().getMachine();
        }
        catch (Exception e){
        	Logger.error(e, "Cannot determine Name of machine.");
        }
        
        tcPostOneComboBoxActuator = new JComboBox();
        tcPostOneComboBoxActuator.setModel(new ActuatorsComboBoxModel(myMachine, Boolean.class));
        panelChanger.add(tcPostOneComboBoxActuator, "4, 5, 3, 1");
        
        tcPostTwoComboBoxActuator = new JComboBox();
        tcPostTwoComboBoxActuator.setModel(new ActuatorsComboBoxModel(myMachine, Boolean.class));
        panelChanger.add(tcPostTwoComboBoxActuator, "4, 7, 3, 1");
        
        tcPostThreeComboBoxActuator = new JComboBox();
        tcPostThreeComboBoxActuator.setModel(new ActuatorsComboBoxModel(myMachine, Boolean.class));
        panelChanger.add(tcPostThreeComboBoxActuator, "4, 9, 3, 1");
        
        lblX = new JLabel("X");
        panelChanger.add(lblX, "4, 2");

        lblY_1 = new JLabel("Y");
        panelChanger.add(lblY_1, "6, 2");

        lblZ_1 = new JLabel("Z");
        panelChanger.add(lblZ_1, "8, 2");
        
        lblSpeed = new JLabel("Speed");
        panelChanger.add(lblSpeed, "10, 2");
        
        lblSpeed1_2 = new JLabel("1 ↔ 2");
        panelChanger.add(lblSpeed1_2, "8, 5, right, default");
        
        lblSpeed2_3 = new JLabel("2 ↔ 3");
        panelChanger.add(lblSpeed2_3, "8, 7, right, default");
        
        lblSpeed3_4 = new JLabel("3 ↔ 4");
        panelChanger.add(lblSpeed3_4, "8, 9, right, default");
        
        lblStartLocation = new JLabel("First Location");
        panelChanger.add(lblStartLocation, "2, 4, right, default");

        textFieldChangerStartX = new JTextField();
        panelChanger.add(textFieldChangerStartX, "4, 4, fill, default");
        textFieldChangerStartX.setColumns(8);

        textFieldChangerStartY = new JTextField();
        panelChanger.add(textFieldChangerStartY, "6, 4, fill, default");
        textFieldChangerStartY.setColumns(8);

        textFieldChangerStartZ = new JTextField();
        panelChanger.add(textFieldChangerStartZ, "8, 4, fill, default");
        textFieldChangerStartZ.setColumns(8);
        
        textFieldChangerStartToMidSpeed = new JTextField();
        textFieldChangerStartToMidSpeed.setToolTipText("Speed between First location and Second location");
        panelChanger.add(textFieldChangerStartToMidSpeed, "10, 5, fill, default");
        textFieldChangerStartToMidSpeed.setColumns(8);

        changerStartLocationButtonsPanel = new LocationButtonsPanel(textFieldChangerStartX,
                textFieldChangerStartY, textFieldChangerStartZ, (JTextField) null);
        changerStartLocationButtonsPanel.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerStartLocationButtonsPanel, "12, 4, fill, default");

        lblMiddleLocation = new JLabel("Second Location");
        panelChanger.add(lblMiddleLocation, "2, 6, right, default");

        textFieldChangerMidX = new JTextField();
        panelChanger.add(textFieldChangerMidX, "4, 6, fill, default");
        textFieldChangerMidX.setColumns(8);

        textFieldChangerMidY = new JTextField();
        panelChanger.add(textFieldChangerMidY, "6, 6, fill, default");
        textFieldChangerMidY.setColumns(8);

        textFieldChangerMidZ = new JTextField();
        panelChanger.add(textFieldChangerMidZ, "8, 6, fill, default");
        textFieldChangerMidZ.setColumns(8);
        
        textFieldChangerMidToMid2Speed = new JTextField();
        textFieldChangerMidToMid2Speed.setToolTipText("Speed between Second location and Third location");
        textFieldChangerMidToMid2Speed.setColumns(8);
        panelChanger.add(textFieldChangerMidToMid2Speed, "10, 7, fill, default");

        changerMidLocationButtonsPanel = new LocationButtonsPanel(textFieldChangerMidX,
                textFieldChangerMidY, textFieldChangerMidZ, (JTextField) null);
        changerMidLocationButtonsPanel.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerMidLocationButtonsPanel, "12, 6, fill, default");
        
        lblMiddleLocation_1 = new JLabel("Third Location");
        panelChanger.add(lblMiddleLocation_1, "2, 8, right, default");
        
        textFieldMidX2 = new JTextField();
        textFieldMidX2.setColumns(8);
        panelChanger.add(textFieldMidX2, "4, 8, fill, default");
        
        textFieldMidY2 = new JTextField();
        textFieldMidY2.setColumns(8);
        panelChanger.add(textFieldMidY2, "6, 8, fill, default");
        
        textFieldMidZ2 = new JTextField();
        textFieldMidZ2.setColumns(8);
        panelChanger.add(textFieldMidZ2, "8, 8, fill, default");
        
        textFieldChangerMid2ToEndSpeed = new JTextField();
        textFieldChangerMid2ToEndSpeed.setToolTipText("Speed between Third location and Last location");
        textFieldChangerMid2ToEndSpeed.setColumns(8);
        panelChanger.add(textFieldChangerMid2ToEndSpeed, "10, 9, fill, default");
        
        changerMidButtons2 = new LocationButtonsPanel(textFieldMidX2, textFieldMidY2, textFieldMidZ2, (JTextField) null);
        changerMidButtons2.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerMidButtons2, "12, 8, fill, default");

        lblEndLocation = new JLabel("Last Location");
        panelChanger.add(lblEndLocation, "2, 10, right, default");

        textFieldChangerEndX = new JTextField();
        panelChanger.add(textFieldChangerEndX, "4, 10, fill, default");
        textFieldChangerEndX.setColumns(8);

        textFieldChangerEndY = new JTextField();
        panelChanger.add(textFieldChangerEndY, "6, 10, fill, default");
        textFieldChangerEndY.setColumns(8);

        textFieldChangerEndZ = new JTextField();
        panelChanger.add(textFieldChangerEndZ, "8, 10, fill, default");
        textFieldChangerEndZ.setColumns(8);

        changerEndLocationButtonsPanel = new LocationButtonsPanel(textFieldChangerEndX,
                textFieldChangerEndY, textFieldChangerEndZ, (JTextField) null);
        changerEndLocationButtonsPanel.setShowPositionToolNoSafeZ(true);
        panelChanger.add(changerEndLocationButtonsPanel, "12, 10, fill, default");

        lblCopyFrom = new JLabel("Template");
        panelChanger.add(lblCopyFrom, "2, 16, right, default");

        nozzleTips = new ArrayList<>();
        copyFromNozzleTip = new JComboBox();
        if (myMachine != null) {
            for (NozzleTip nt : myMachine.getNozzleTips()) {
                if (nt instanceof ReferenceNozzleTip 
                        && nt != nozzleTip) {
                    nozzleTips.add((ReferenceNozzleTip) nt);
                    copyFromNozzleTip.addItem(nt.getName());
                }
            }
        }
        panelChanger.add(copyFromNozzleTip, "4, 16, 7, 1, fill, default");

        btnNewButton = new JButton(cloneFromNozzleTipAction);
        panelChanger.add(btnNewButton, "12, 16");
        if (nozzleTips.size() == 0) {
            copyFromNozzleTip.setEnabled(false);
            btnNewButton.setEnabled(false);
        }
    }
    
    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        MutableLocationProxy changerStartLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzleTip, "changerStartLocation", changerStartLocation,
                "location");
        addWrappedBinding(changerStartLocation, "lengthX", textFieldChangerStartX, "text",
                lengthConverter);
        addWrappedBinding(changerStartLocation, "lengthY", textFieldChangerStartY, "text",
                lengthConverter);
        addWrappedBinding(changerStartLocation, "lengthZ", textFieldChangerStartZ, "text",
                lengthConverter);
        addWrappedBinding(nozzleTip, "changerStartToMidSpeed", textFieldChangerStartToMidSpeed, "text",
                doubleConverter);

        MutableLocationProxy changerMidLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzleTip, "changerMidLocation", changerMidLocation,
                "location");
        addWrappedBinding(changerMidLocation, "lengthX", textFieldChangerMidX, "text",
                lengthConverter);
        addWrappedBinding(changerMidLocation, "lengthY", textFieldChangerMidY, "text",
                lengthConverter);
        addWrappedBinding(changerMidLocation, "lengthZ", textFieldChangerMidZ, "text",
                lengthConverter);
        addWrappedBinding(nozzleTip, "changerMidToMid2Speed", textFieldChangerMidToMid2Speed, "text",
                doubleConverter);

        MutableLocationProxy changerMidLocation2 = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzleTip, "changerMidLocation2", changerMidLocation2,
                "location");
        addWrappedBinding(changerMidLocation2, "lengthX", textFieldMidX2, "text",
                lengthConverter);
        addWrappedBinding(changerMidLocation2, "lengthY", textFieldMidY2, "text",
                lengthConverter);
        addWrappedBinding(changerMidLocation2, "lengthZ", textFieldMidZ2, "text",
                lengthConverter);
        addWrappedBinding(nozzleTip, "changerMid2ToEndSpeed", textFieldChangerMid2ToEndSpeed, "text",
                doubleConverter);

        MutableLocationProxy changerEndLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzleTip, "changerEndLocation", changerEndLocation,
                "location");
        addWrappedBinding(changerEndLocation, "lengthX", textFieldChangerEndX, "text",
                lengthConverter);
        addWrappedBinding(changerEndLocation, "lengthY", textFieldChangerEndY, "text",
                lengthConverter);
        addWrappedBinding(changerEndLocation, "lengthZ", textFieldChangerEndZ, "text",
                lengthConverter);
        
        // bert start
        addWrappedBinding(nozzleTip, "changerActuatorPostStepOne", tcPostOneComboBoxActuator, "selectedItem");
        addWrappedBinding(nozzleTip, "changerActuatorPostStepTwo", tcPostTwoComboBoxActuator, "selectedItem");
        addWrappedBinding(nozzleTip, "changerActuatorPostStepThree", tcPostThreeComboBoxActuator, "selectedItem");
        // bert stop
        
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerStartToMidSpeed);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMidToMid2Speed);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMidX2);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMidY2);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldMidZ2);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerMid2ToEndSpeed);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerEndX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerEndY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldChangerEndZ);
    }

    private Action cloneFromNozzleTipAction = new AbstractAction("Clone Tool Changer Setting", Icons.importt) {
        {
            putValue(Action.SHORT_DESCRIPTION, "<html>Clone the Tool Changer settings from the selected Template nozzle tip,<br/>"
                    +"but offset the locations relative to the First Location.</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            UiUtils.messageBoxOnException(() -> {
                applyAction.actionPerformed(e);
                String name = (String)copyFromNozzleTip.getSelectedItem();
                ReferenceNozzleTip templateNozzleTip= null;
                for (ReferenceNozzleTip nt : nozzleTips) {
                    if (name.equals(nt.getName())) {
                        templateNozzleTip = nt;
                    }
                }
                if (templateNozzleTip != null) {
                    Location offsets = nozzleTip.getChangerStartLocation().subtract(templateNozzleTip.getChangerStartLocation());
                    nozzleTip.setChangerMidLocation(templateNozzleTip.getChangerMidLocation().add(offsets));
                    nozzleTip.setChangerMidLocation2(templateNozzleTip.getChangerMidLocation2().add(offsets));
                    nozzleTip.setChangerEndLocation(templateNozzleTip.getChangerEndLocation().add(offsets));
                    nozzleTip.setChangerActuatorPostStepOne(templateNozzleTip.getChangerActuatorPostStepOne());
                    nozzleTip.setChangerActuatorPostStepTwo(templateNozzleTip.getChangerActuatorPostStepTwo());
                    nozzleTip.setChangerActuatorPostStepThree(templateNozzleTip.getChangerActuatorPostStepThree());
                    nozzleTip.setChangerStartToMidSpeed(templateNozzleTip.getChangerStartToMidSpeed());
                    nozzleTip.setChangerMidToMid2Speed(templateNozzleTip.getChangerMidToMid2Speed());
                    nozzleTip.setChangerMid2ToEndSpeed(templateNozzleTip.getChangerMid2ToEndSpeed());
                }
            });
        }
    };
}
