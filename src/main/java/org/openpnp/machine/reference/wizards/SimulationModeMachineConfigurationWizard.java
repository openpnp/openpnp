/*
 * Copyright (C) 2020 <mark@makr.zone>
 * inspired and based on work
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
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.Translations;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.SimulationModeMachine;
import org.openpnp.machine.reference.SimulationModeMachine.SimulationMode;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class SimulationModeMachineConfigurationWizard extends AbstractConfigurationWizard {

    private final SimulationModeMachine machine;
    private JTextField homingErrorX;
    private JTextField homingErrorY;
    private JTextField simulatedNonSquarenessFactor;
    private JTextField simulatedRunout;
    private JTextField simulatedCameraNoise;
    private JTextField simulatedVibrationAmplitude;
    private JComboBox simulationMode;
    private JTextField simulatedRunoutPhase;
    private JCheckBox pickAndPlaceChecking;
    private JTextField simulatedCameraLag;
    private JTextField machineTableZ;
    private JTextField simulatedVibrationDuration;
    private JCheckBox replacingDrivers;

    public SimulationModeMachineConfigurationWizard(SimulationModeMachine machine) {
        this.machine = machine;

        JPanel panelGeneral = new JPanel();
        contentPanel.add(panelGeneral);
        panelGeneral.setBorder(new TitledBorder(null, Translations.getString("CommonPropertySheet.General"), TitledBorder.LEADING, //$NON-NLS-1$
                TitledBorder.TOP, null, null));
        panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(80dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblSimulationMode = new JLabel(Translations.getString("SimulationModeMachineConfigurationWizard.SimulationMode")); //$NON-NLS-1$
        panelGeneral.add(lblSimulationMode, "2, 2, right, default");

        simulationMode = new JComboBox(SimulationMode.values());
        panelGeneral.add(simulationMode, "4, 2, fill, default");
        
        JLabel lblReplaceDrivers = new JLabel(Translations.getString("SimulationModeMachineConfigurationWizard.ReplaceDrivers")); //$NON-NLS-1$
        lblReplaceDrivers.setToolTipText(Translations.getString("SimulationModeMachineConfigurationWizard.ReplaceDrivers.Tooltip")); //$NON-NLS-1$
        panelGeneral.add(lblReplaceDrivers, "2, 4, right, default");
        
        replacingDrivers = new JCheckBox("");
        panelGeneral.add(replacingDrivers, "4, 4");

        JPanel panelLocations = new JPanel();
        panelLocations.setBorder(new TitledBorder(null, Translations.getString("SimulationModeMachineConfigurationWizard.SimulatedImperfections"), TitledBorder.LEADING, //$NON-NLS-1$
                TitledBorder.TOP, null, null));
        contentPanel.add(panelLocations);
        panelLocations.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(80dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(100dlu;default)"),
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
                RowSpec.decode("default:grow"),}));

        JLabel lblNozzleTipRunout = new JLabel(Translations.getString("SimulationModeMachineConfigurationWizard.NozzleTipRunout")); //$NON-NLS-1$
        lblNozzleTipRunout.setToolTipText(Translations.getString("SimulationModeMachineConfigurationWizard.NozzleTipRunout.Tooltip")); //$NON-NLS-1$
        panelLocations.add(lblNozzleTipRunout, "2, 2, right, default");

        simulatedRunout = new JTextField();
        panelLocations.add(simulatedRunout, "4, 2");
        simulatedRunout.setColumns(10);
        
        JLabel lblWarnRunout = new JLabel(Translations.getString("SimulationModeMachineConfigurationWizard.RunoutWarning")); //$NON-NLS-1$
        panelLocations.add(lblWarnRunout, "6, 2, 3, 5, fill, top");
        
        JLabel lblRunoutPhase = new JLabel(Translations.getString("SimulationModeMachineConfigurationWizard.RunoutPhase")); //$NON-NLS-1$
        lblRunoutPhase.setToolTipText(Translations.getString("SimulationModeMachineConfigurationWizard.RunoutPhase.Tooltip")); //$NON-NLS-1$
        panelLocations.add(lblRunoutPhase, "2, 4, right, default");
        
        simulatedRunoutPhase = new JTextField();
        panelLocations.add(simulatedRunoutPhase, "4, 4, fill, default");
        simulatedRunoutPhase.setColumns(10);
        
                JLabel lblNonsquarenessFactor = new JLabel(Translations.getString("SimulationModeMachineConfigurationWizard.NonSquarenessFactor")); //$NON-NLS-1$
                lblNonsquarenessFactor.setToolTipText(Translations.getString("SimulationModeMachineConfigurationWizard.NonSquarenessFactor.Tooltip")); //$NON-NLS-1$
                panelLocations.add(lblNonsquarenessFactor, "2, 8, right, default");
        
                simulatedNonSquarenessFactor = new JTextField();
                panelLocations.add(simulatedNonSquarenessFactor, "4, 8");
                simulatedNonSquarenessFactor.setColumns(10);
        
        JLabel lblPickPlace = new JLabel(Translations.getString("SimulationModeMachineConfigurationWizard.PickPlaceChecking")); //$NON-NLS-1$
        panelLocations.add(lblPickPlace, "2, 12, right, default");
        
        pickAndPlaceChecking = new JCheckBox("");
        panelLocations.add(pickAndPlaceChecking, "4, 12");
        
        JLabel lblCameraLags = new JLabel(Translations.getString("SimulationModeMachineConfigurationWizard.CameraLag")); //$NON-NLS-1$
        panelLocations.add(lblCameraLags, "2, 16, right, default");
        
        simulatedCameraLag = new JTextField();
        panelLocations.add(simulatedCameraLag, "4, 16, fill, default");
        simulatedCameraLag.setColumns(10);
        
                JLabel lblCameraNoise = new JLabel(Translations.getString("SimulationModeMachineConfigurationWizard.CameraNoise")); //$NON-NLS-1$
                lblCameraNoise.setToolTipText(Translations.getString("SimulationModeMachineConfigurationWizard.CameraNoise.Tooltip")); //$NON-NLS-1$
                panelLocations.add(lblCameraNoise, "2, 18, right, default");
        
                simulatedCameraNoise = new JTextField();
                panelLocations.add(simulatedCameraNoise, "4, 18");
                simulatedCameraNoise.setColumns(10);

        JLabel lblVibrationAmplitude = new JLabel(Translations.getString("SimulationModeMachineConfigurationWizard.VibrationAmplitude")); //$NON-NLS-1$
        lblVibrationAmplitude.setToolTipText(Translations.getString("SimulationModeMachineConfigurationWizard.VibrationAmplitude.Tooltip")); //$NON-NLS-1$
        panelLocations.add(lblVibrationAmplitude, "2, 20, right, default");

        simulatedVibrationAmplitude = new JTextField();
        panelLocations.add(simulatedVibrationAmplitude, "4, 20, fill, default");
        simulatedVibrationAmplitude.setColumns(10);
        
        JLabel lblDuration = new JLabel(Translations.getString("SimulationModeMachineConfigurationWizard.VibrationDuration")); //$NON-NLS-1$
        lblDuration.setToolTipText(Translations.getString("SimulationModeMachineConfigurationWizard.VibrationDuration.Tooltip")); //$NON-NLS-1$
        panelLocations.add(lblDuration, "2, 22, right, default");
        
        simulatedVibrationDuration = new JTextField();
        panelLocations.add(simulatedVibrationDuration, "4, 22, left, default");
        simulatedVibrationDuration.setColumns(10);

        JLabel lblX = new JLabel(Translations.getString("CommonWords.X")); //$NON-NLS-1$
        panelLocations.add(lblX, "4, 26");
        lblX.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblY = new JLabel(Translations.getString("CommonWords.Y")); //$NON-NLS-1$
        panelLocations.add(lblY, "6, 26");
        lblY.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblDiscardPoint = new JLabel(Translations.getString("SimulationModeMachineConfigurationWizard.HomingError")); //$NON-NLS-1$
        lblDiscardPoint.setToolTipText(Translations.getString("SimulationModeMachineConfigurationWizard.HomingError.Tooltip")); //$NON-NLS-1$
        panelLocations.add(lblDiscardPoint, "2, 28, right, default");

        homingErrorX = new JTextField();
        panelLocations.add(homingErrorX, "4, 28");
        homingErrorX.setColumns(10);

        homingErrorY = new JTextField();
        panelLocations.add(homingErrorY, "6, 28");
        homingErrorY.setColumns(10);
        
        JButton btnResetFeeders = new JButton(Translations.getString("SimulationModeMachineConfigurationWizard.ResetFeeders")); //$NON-NLS-1$
        btnResetFeeders.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                machine.resetAllFeeders();
            }
        });
        
        JLabel lblMachineTableZ = new JLabel(Translations.getString("SimulationModeMachineConfigurationWizard.MachineTableZ")); //$NON-NLS-1$
        panelLocations.add(lblMachineTableZ, "2, 30, right, default");
        
        machineTableZ = new JTextField();
        panelLocations.add(machineTableZ, "4, 30, fill, default");
        machineTableZ.setColumns(10);
        
        JButton btnSetMachineTable = new JButton(Translations.getString("SimulationModeMachineConfigurationWizard.SetMachineTableZ")); //$NON-NLS-1$
        btnSetMachineTable.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                LengthConverter lengthConverter = new LengthConverter();
                Length tableZ = lengthConverter.convertReverse(machineTableZ.getText());
                machine.setMachineTableZ(tableZ);
            }
        });
        panelLocations.add(btnSetMachineTable, "8, 30");
        panelLocations.add(btnResetFeeders, "8, 32");
        
        JLabel label = new JLabel(" ");
        panelLocations.add(label, "2, 34");
    }

    @Override
    public void createBindings() {
        DoubleConverter doubleConverter =
                new DoubleConverter("%f");
        DoubleConverter degreeConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        IntegerConverter integerConverter =
                new IntegerConverter();
        LengthConverter lengthConverter = new LengthConverter();

        addWrappedBinding(machine, "simulationMode", simulationMode, "selectedItem");
        addWrappedBinding(machine, "replacingDrivers", replacingDrivers, "selected");

        addWrappedBinding(machine, "simulatedNonSquarenessFactor", simulatedNonSquarenessFactor, "text", doubleConverter);

        addWrappedBinding(machine, "simulatedRunout", simulatedRunout, "text", lengthConverter);
        addWrappedBinding(machine, "simulatedRunoutPhase", simulatedRunoutPhase, "text", degreeConverter);
        addWrappedBinding(machine, "pickAndPlaceChecking", pickAndPlaceChecking, "selected");

        addWrappedBinding(machine, "simulatedVibrationAmplitude", simulatedVibrationAmplitude, "text", doubleConverter);
        addWrappedBinding(machine, "simulatedVibrationDuration", simulatedVibrationDuration, "text", doubleConverter);
        addWrappedBinding(machine, "simulatedCameraNoise", simulatedCameraNoise, "text", integerConverter);
        addWrappedBinding(machine, "simulatedCameraLag", simulatedCameraLag, "text", doubleConverter);

        MutableLocationProxy homingError = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, machine, "homingError", homingError, "location");
        addWrappedBinding(homingError, "lengthX", homingErrorX, "text", lengthConverter);
        addWrappedBinding(homingError, "lengthY", homingErrorY, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homingErrorX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homingErrorY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homingErrorX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homingErrorY);
        
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(machineTableZ);
    }
}
