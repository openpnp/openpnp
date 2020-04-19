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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.SimpleGraphView;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.ReferenceNozzleTip.VacuumMeasurementMethod;
import org.openpnp.machine.reference.feeder.BlindsFeeder;
import org.openpnp.model.Configuration;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JCheckBox;
import java.awt.BorderLayout;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

public class ReferenceNozzleTipPartDetectionWizard extends AbstractConfigurationWizard {
    private final ReferenceNozzleTip nozzleTip;

    private Set<org.openpnp.model.Package> compatiblePackages = new HashSet<>();
    private JPanel panelPartOnVacuumSensing;


    public ReferenceNozzleTipPartDetectionWizard(ReferenceNozzleTip nozzleTip) {
        this.nozzleTip = nozzleTip;
        
        panelPartOnVacuumSensing = new JPanel();
        panelPartOnVacuumSensing.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Part On Vacuum Sensing", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(panelPartOnVacuumSensing);
        panelPartOnVacuumSensing.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblPartOnMeasurement = new JLabel("Measurement Method");
        panelPartOnVacuumSensing.add(lblPartOnMeasurement, "2, 2, right, default");
        
        methodPartOn = new JComboBox(ReferenceNozzleTip.VacuumMeasurementMethod.values());
        methodPartOn.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                adaptDialog();
            }
        });
        panelPartOnVacuumSensing.add(methodPartOn, "4, 2, 3, 1");
        
        lblEstablishPartOnLevel = new JLabel("Establish Level");
        lblEstablishPartOnLevel.setToolTipText("While the nozzle is pressed down on the part in the pick operation, the vacuum level is repeatedly measured until it reaches the Vacuum Range. ");
        panelPartOnVacuumSensing.add(lblEstablishPartOnLevel, "2, 4, right, default");
        
        establishPartOnLevel = new JCheckBox("");
        panelPartOnVacuumSensing.add(establishPartOnLevel, "4, 4");
        
        lblPartOnLowValue = new JLabel("Low Value");
        panelPartOnVacuumSensing.add(lblPartOnLowValue, "4, 6");
        
        lblPartOnHighValue = new JLabel("High Value");
        panelPartOnVacuumSensing.add(lblPartOnHighValue, "6, 6");
        
        lblPartOnLastReading = new JLabel("Last Reading");
        panelPartOnVacuumSensing.add(lblPartOnLastReading, "10, 6");
        
        lblPartOnNozzle = new JLabel("Vacuum Range");
        panelPartOnVacuumSensing.add(lblPartOnNozzle, "2, 8, right, default");
        
        vacuumLevelPartOnLow = new JTextField();
        panelPartOnVacuumSensing.add(vacuumLevelPartOnLow, "4, 8");
        vacuumLevelPartOnLow.setColumns(10);
        
        vacuumLevelPartOnHigh = new JTextField();
        panelPartOnVacuumSensing.add(vacuumLevelPartOnHigh, "6, 8");
        vacuumLevelPartOnHigh.setColumns(10);
        
        vacuumLevelPartOnReading = new JTextField();
        vacuumLevelPartOnReading.setEditable(false);
        panelPartOnVacuumSensing.add(vacuumLevelPartOnReading, "10, 8, right, top");
        vacuumLevelPartOnReading.setColumns(10);
        
        lblPartOnRelativeRange = new JLabel("Relative Range");
        panelPartOnVacuumSensing.add(lblPartOnRelativeRange, "2, 10, right, default");
        
        vacuumRelativePartOnLow = new JTextField();
        vacuumRelativePartOnLow.setColumns(10);
        panelPartOnVacuumSensing.add(vacuumRelativePartOnLow, "4, 10, fill, default");
        
        vacuumRelativePartOnHigh = new JTextField();
        panelPartOnVacuumSensing.add(vacuumRelativePartOnHigh, "6, 10, fill, default");
        vacuumRelativePartOnHigh.setColumns(10);
        
        vacuumRelativePartOnReading = new JTextField();
        vacuumRelativePartOnReading.setEditable(false);
        panelPartOnVacuumSensing.add(vacuumRelativePartOnReading, "10, 10, fill, default");
        vacuumRelativePartOnReading.setColumns(10);
        
        vacuumPartOnGraph = new SimpleGraphView(nozzleTip);
        panelPartOnVacuumSensing.add(vacuumPartOnGraph, "4, 14, 7, 1");
        
        panelPartOffVacuumSensing = new JPanel();
        panelPartOffVacuumSensing.setToolTipText("During the Place operation, the level is repeatedly measured until it reaches the Vacuum Range.");
        panelPartOffVacuumSensing.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Part Off Vacuum Sensing", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(panelPartOffVacuumSensing);
        panelPartOffVacuumSensing.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblPartOffMeasurement = new JLabel("Measurement Method");
        panelPartOffVacuumSensing.add(lblPartOffMeasurement, "2, 2");
        
        methodPartOff = new JComboBox(ReferenceNozzleTip.VacuumMeasurementMethod.values());
        methodPartOff.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                adaptDialog();
            }
        });
        panelPartOffVacuumSensing.add(methodPartOff, "4, 2, 3, 1");
        
        lblEstablishPartOffLevel = new JLabel("Establish Level");
        lblEstablishPartOffLevel.setToolTipText("While the nozzle is pressed down on the part in the pick operation, the vacuum level is repeatedly measured until it reaches the Vacuum Range or the timeout is reached.");
        panelPartOffVacuumSensing.add(lblEstablishPartOffLevel, "2, 4, right, default");
        
        establishPartOffLevel = new JCheckBox("");
        panelPartOffVacuumSensing.add(establishPartOffLevel, "4, 4");
        
        lblPartOffLowValue = new JLabel("Low Value");
        panelPartOffVacuumSensing.add(lblPartOffLowValue, "4, 6");
        
        lblPartOffHighValue = new JLabel("High Value");
        panelPartOffVacuumSensing.add(lblPartOffHighValue, "6, 6");
        
        lblPartOffLastReading = new JLabel("Last Reading");
        panelPartOffVacuumSensing.add(lblPartOffLastReading, "10, 6");
        
        lblPartOffNozzle = new JLabel("Vacuum Range");
        panelPartOffVacuumSensing.add(lblPartOffNozzle, "2, 8, right, default");
        
        vacuumLevelPartOffLow = new JTextField();
        panelPartOffVacuumSensing.add(vacuumLevelPartOffLow, "4, 8");
        vacuumLevelPartOffLow.setColumns(10);
        
        vacuumLevelPartOffHigh = new JTextField();
        panelPartOffVacuumSensing.add(vacuumLevelPartOffHigh, "6, 8");
        vacuumLevelPartOffHigh.setColumns(10);
        
        vacuumLevelPartOffReading = new JTextField();
        vacuumLevelPartOffReading.setEditable(false);
        panelPartOffVacuumSensing.add(vacuumLevelPartOffReading, "10, 8, fill, default");
        vacuumLevelPartOffReading.setColumns(10);
        
        lblProbingTimePartOff = new JLabel("Probing time (ms)");
        panelPartOffVacuumSensing.add(lblProbingTimePartOff, "2, 10, right, default");
        
        partOffProbingMilliseconds = new JTextField();
        panelPartOffVacuumSensing.add(partOffProbingMilliseconds, "4, 10, default, center");
        partOffProbingMilliseconds.setColumns(10);
        
        lblPartOffRelativeRange = new JLabel("Relative Range");
        panelPartOffVacuumSensing.add(lblPartOffRelativeRange, "2, 12, right, default");
        
        vacuumRelativePartOffLow = new JTextField();
        panelPartOffVacuumSensing.add(vacuumRelativePartOffLow, "4, 12, fill, default");
        vacuumRelativePartOffLow.setColumns(10);
        
        vacuumRelativePartOffHigh = new JTextField();
        panelPartOffVacuumSensing.add(vacuumRelativePartOffHigh, "6, 12, fill, default");
        vacuumRelativePartOffHigh.setColumns(10);
        
        vacuumRelativePartOffReading = new JTextField();
        vacuumRelativePartOffReading.setEditable(false);
        panelPartOffVacuumSensing.add(vacuumRelativePartOffReading, "10, 12, fill, default");
        vacuumRelativePartOffReading.setColumns(10);
        
        vacuumPartOffGraph = new SimpleGraphView(nozzleTip);
        panelPartOffVacuumSensing.add(vacuumPartOffGraph, "4, 16, 7, 1");
    }
    
    private JLabel lblPartOnLowValue;
    private JLabel lblPartOnHighValue;
    private JLabel lblPartOnNozzle;
    private JLabel lblPartOffNozzle;
    private JTextField vacuumLevelPartOnLow;
    private JTextField vacuumLevelPartOffLow;
    private JTextField vacuumLevelPartOnHigh;
    private JTextField vacuumLevelPartOffHigh;
    private JLabel lblPartOnMeasurement;
    private JComboBox methodPartOn;
    private JLabel lblPartOffMeasurement;
    private JComboBox methodPartOff;
    private JPanel panelPartOffVacuumSensing;
    private JLabel lblProbingTimePartOff;
    private JTextField partOffProbingMilliseconds;
    private JLabel lblPartOffRelativeRange;
    private JTextField vacuumRelativePartOffLow;
    private JTextField vacuumRelativePartOffHigh;
    private JLabel lblPartOnRelativeRange;
    private JTextField vacuumRelativePartOnLow;
    private JTextField vacuumRelativePartOnHigh;
    private JLabel lblPartOffLowValue;
    private JLabel lblPartOffHighValue;
    private JLabel lblPartOnLastReading;
    private JLabel lblPartOffLastReading;
    private JTextField vacuumLevelPartOnReading;
    private JTextField vacuumRelativePartOnReading;
    private JTextField vacuumLevelPartOffReading;
    private JTextField vacuumRelativePartOffReading;
    private SimpleGraphView vacuumPartOnGraph;
    private SimpleGraphView vacuumPartOffGraph;
    private JCheckBox establishPartOnLevel;
    private JLabel lblEstablishPartOnLevel;
    private JLabel lblEstablishPartOffLevel;
    private JCheckBox establishPartOffLevel;
    
    private void adaptDialog() {
        VacuumMeasurementMethod methodOn = (VacuumMeasurementMethod)methodPartOn.getSelectedItem();
        boolean partOn = true;
        boolean partOnRelative = true;
        if (methodOn == VacuumMeasurementMethod.None) {
            // hide all 
            partOn = false;
            partOnRelative = false;
        }
        else if (!methodOn.isRelativeMethod()) {
            partOnRelative = false;
        }
         
        lblEstablishPartOnLevel.setVisible(partOn);
        establishPartOnLevel.setVisible(partOn);
        lblPartOnLowValue.setVisible(partOn);
        lblPartOnHighValue.setVisible(partOn);
        lblPartOnLastReading.setVisible(partOn);
        lblPartOnNozzle.setVisible(partOn);
        vacuumLevelPartOnLow.setVisible(partOn);
        vacuumLevelPartOnHigh.setVisible(partOn);
        vacuumLevelPartOnReading.setVisible(partOn);
        
        lblPartOnRelativeRange.setVisible(partOnRelative);
        vacuumRelativePartOnLow.setVisible(partOnRelative);
        vacuumRelativePartOnHigh.setVisible(partOnRelative);
        vacuumRelativePartOnReading.setVisible(partOnRelative);
        vacuumPartOnGraph.setVisible(partOn);
        
        VacuumMeasurementMethod methodOff = (VacuumMeasurementMethod)methodPartOff.getSelectedItem();
        boolean partOff = true;
        boolean partOffRelative = true;
        if (methodOff == VacuumMeasurementMethod.None) {
            // hide all 
            partOff = false;
            partOffRelative = false;
        }
        else if (!methodOff.isRelativeMethod()) {
            partOffRelative = false;
        }
         
        lblEstablishPartOffLevel.setVisible(partOn);
        establishPartOffLevel.setVisible(partOn);
        lblPartOffLowValue.setVisible(partOff);
        lblPartOffHighValue.setVisible(partOff);
        lblPartOffLastReading.setVisible(partOff);
        lblPartOffNozzle.setVisible(partOff);
        vacuumLevelPartOffLow.setVisible(partOff);
        vacuumLevelPartOffHigh.setVisible(partOff);
        vacuumLevelPartOffReading.setVisible(partOff);
        
        lblProbingTimePartOff.setVisible(partOffRelative);
        partOffProbingMilliseconds.setVisible(partOffRelative);
        lblPartOffRelativeRange.setVisible(partOffRelative);
        vacuumRelativePartOffLow.setVisible(partOffRelative);
        vacuumRelativePartOffHigh.setVisible(partOffRelative);
        vacuumRelativePartOffReading.setVisible(partOffRelative);
        vacuumPartOffGraph.setVisible(partOff);
    }

    @Override
    public void createBindings() {
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        IntegerConverter integerConverter = new IntegerConverter();

        addWrappedBinding(nozzleTip, "methodPartOn", methodPartOn, "selectedItem");
        addWrappedBinding(nozzleTip, "establishPartOnLevel", establishPartOnLevel, "selected");
        addWrappedBinding(nozzleTip, "vacuumLevelPartOnLow", vacuumLevelPartOnLow, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumLevelPartOnHigh", vacuumLevelPartOnHigh, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumLevelPartOnReading", vacuumLevelPartOnReading, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumRelativePartOnLow", vacuumRelativePartOnLow, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumRelativePartOnHigh", vacuumRelativePartOnHigh, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumRelativePartOnReading", vacuumRelativePartOnReading, "text", doubleConverter);

        addWrappedBinding(nozzleTip, "methodPartOff", methodPartOff, "selectedItem");
        addWrappedBinding(nozzleTip, "establishPartOffLevel", establishPartOffLevel, "selected");
        addWrappedBinding(nozzleTip, "vacuumLevelPartOffLow", vacuumLevelPartOffLow, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumLevelPartOffHigh", vacuumLevelPartOffHigh, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumLevelPartOffReading", vacuumLevelPartOffReading, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "partOffProbingMilliseconds", partOffProbingMilliseconds, "text", integerConverter);
        addWrappedBinding(nozzleTip, "vacuumRelativePartOffLow", vacuumRelativePartOffLow, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumRelativePartOffHigh", vacuumRelativePartOffHigh, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumRelativePartOffReading", vacuumRelativePartOffReading, "text", doubleConverter);
        
        addWrappedBinding(nozzleTip, "vacuumPartOnGraph", vacuumPartOnGraph, "graph");
        addWrappedBinding(nozzleTip, "vacuumPartOffGraph", vacuumPartOffGraph, "graph");

        ComponentDecorators.decorateWithAutoSelect(partOffProbingMilliseconds);
        
        ComponentDecorators.decorateWithAutoSelect(vacuumLevelPartOnLow);
        ComponentDecorators.decorateWithAutoSelect(vacuumLevelPartOnHigh);
        ComponentDecorators.decorateWithAutoSelect(vacuumRelativePartOnLow);
        ComponentDecorators.decorateWithAutoSelect(vacuumRelativePartOnHigh);
        ComponentDecorators.decorateWithAutoSelect(vacuumLevelPartOffLow);
        ComponentDecorators.decorateWithAutoSelect(vacuumLevelPartOffHigh);
        ComponentDecorators.decorateWithAutoSelect(vacuumRelativePartOffLow);
        ComponentDecorators.decorateWithAutoSelect(vacuumRelativePartOffHigh);
    }
}
