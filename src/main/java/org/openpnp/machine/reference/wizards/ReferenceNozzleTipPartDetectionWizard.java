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
        
        lblPartOnLowValue = new JLabel("Low Value");
        panelPartOnVacuumSensing.add(lblPartOnLowValue, "4, 4");
        
        lblPartOnHighValue = new JLabel("High Value");
        panelPartOnVacuumSensing.add(lblPartOnHighValue, "6, 4");
        
        lblPartOnLastReading = new JLabel("Last Reading");
        panelPartOnVacuumSensing.add(lblPartOnLastReading, "10, 4");
        
        lblPartOnDwell = new JLabel("Dwell Time (ms)");
        lblPartOnDwell.setToolTipText("<html>\r\n<p>For the absolute measurement method, this is an extra dwell time after picking <br />\r\nand raising the nozzle to Safe Z.</p>\r\n<p>\r\nFor relative trend measurements methods, this is a dwell time after opening the valve<br />\r\nbefore taking the reference reading for the trend. This happens before any pick\r\ndwell times. </p>\r\n</html>\r\n");
        panelPartOnVacuumSensing.add(lblPartOnDwell, "2, 6, right, default");
        
        partOnDwellMilliseconds = new JTextField();
        partOnDwellMilliseconds.setToolTipText("<html>\r\n<p>For the absolute measurement method, this is an extra dwell time after picking <br />\r\nand raising the nozzle to Safe Z.</p>\r\n<p>\r\nFor relative trend measurements methods, this is a dwell time after opening the valve<br />\r\nbefore taking the reference reading for the trend. This happens before any pick\r\ndwell times. </p>\r\n</html>\r\n");
        panelPartOnVacuumSensing.add(partOnDwellMilliseconds, "4, 6, fill, default");
        partOnDwellMilliseconds.setColumns(10);
        
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
        
        lblTrendTimePartOn = new JLabel("Trend time (ms)");
        panelPartOnVacuumSensing.add(lblTrendTimePartOn, "2, 10, right, default");
        
        partOnTrendMilliseconds = new JTextField();
        panelPartOnVacuumSensing.add(partOnTrendMilliseconds, "4, 10, fill, default");
        partOnTrendMilliseconds.setColumns(10);
        
        lblPartOnTrendRange = new JLabel("Trend Range");
        panelPartOnVacuumSensing.add(lblPartOnTrendRange, "2, 12, right, default");
        
        vacuumTrendPartOnLow = new JTextField();
        vacuumTrendPartOnLow.setColumns(10);
        panelPartOnVacuumSensing.add(vacuumTrendPartOnLow, "4, 12, fill, default");
        
        vacuumTrendPartOnHigh = new JTextField();
        panelPartOnVacuumSensing.add(vacuumTrendPartOnHigh, "6, 12, fill, default");
        vacuumTrendPartOnHigh.setColumns(10);
        
        vacuumTrendPartOnReading = new JTextField();
        vacuumTrendPartOnReading.setEditable(false);
        panelPartOnVacuumSensing.add(vacuumTrendPartOnReading, "10, 12, fill, default");
        vacuumTrendPartOnReading.setColumns(10);
        
        vacuumPartOnGraph = new SimpleGraphView(nozzleTip);
        panelPartOnVacuumSensing.add(vacuumPartOnGraph, "4, 16, 7, 1");
        
        panelPartOffVacuumSensing = new JPanel();
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
        
        lblValveEnabled = new JLabel("Enable Valve for Test?");
        panelPartOffVacuumSensing.add(lblValveEnabled, "2, 4");
        lblValveEnabled.setToolTipText("");
        
        valveEnabledForPartOff = new JCheckBox("");
        panelPartOffVacuumSensing.add(valveEnabledForPartOff, "4, 4");
        
        lblPartOffLowValue = new JLabel("Low Value");
        panelPartOffVacuumSensing.add(lblPartOffLowValue, "4, 6");
        
        lblPartOffHighValue = new JLabel("High Value");
        panelPartOffVacuumSensing.add(lblPartOffHighValue, "6, 6");
        
        lblPartOffLastReading = new JLabel("Last Reading");
        panelPartOffVacuumSensing.add(lblPartOffLastReading, "10, 6");
        
        lblPartOffDwell = new JLabel("Dwell Time (ms)");
        lblPartOffDwell.setToolTipText("<html>\r\n<p>For the absolute measurement method, this is an extra dwell time after placing, <br />\r\nraising the nozzle to Safe Z and optionally opening the valve.</p>\r\n<p>\r\nFor relative trend measurements methods, this is a dwell time after closing the valve<br />\r\nbefore taking the reference reading for the trend. This happens before any place \r\ndwell times. </p>\r\n</html>\r\n");
        panelPartOffVacuumSensing.add(lblPartOffDwell, "2, 8, right, default");
        
        partOffDwellMilliseconds = new JTextField();
        partOffDwellMilliseconds.setToolTipText("<html>\r\n<p>For the absolute measurement method, this is an extra dwell time after placing, <br />\r\nraising the nozzle to Safe Z and optionally opening the valve.</p>\r\n<p>\r\nFor relative trend measurements methods, this is a dwell time after closing the valve<br />\r\nbefore taking the reference reading for the trend. This happens before any place \r\ndwell times. </p>\r\n</html>\r\n");
        panelPartOffVacuumSensing.add(partOffDwellMilliseconds, "4, 8");
        partOffDwellMilliseconds.setColumns(10);
        
        lblPartOffNozzle = new JLabel("Vacuum Range");
        panelPartOffVacuumSensing.add(lblPartOffNozzle, "2, 10, right, default");
        
        vacuumLevelPartOffLow = new JTextField();
        panelPartOffVacuumSensing.add(vacuumLevelPartOffLow, "4, 10");
        vacuumLevelPartOffLow.setColumns(10);
        
        vacuumLevelPartOffHigh = new JTextField();
        panelPartOffVacuumSensing.add(vacuumLevelPartOffHigh, "6, 10");
        vacuumLevelPartOffHigh.setColumns(10);
        
        vacuumLevelPartOffReading = new JTextField();
        vacuumLevelPartOffReading.setEditable(false);
        panelPartOffVacuumSensing.add(vacuumLevelPartOffReading, "10, 10, fill, default");
        vacuumLevelPartOffReading.setColumns(10);
        
        lblTrendTimePartOff = new JLabel("Trend time (ms)");
        panelPartOffVacuumSensing.add(lblTrendTimePartOff, "2, 12, right, default");
        
        partOffTrendMilliseconds = new JTextField();
        panelPartOffVacuumSensing.add(partOffTrendMilliseconds, "4, 12, default, center");
        partOffTrendMilliseconds.setColumns(10);
        
        lblPartOffTrendRange = new JLabel("Trend Range");
        panelPartOffVacuumSensing.add(lblPartOffTrendRange, "2, 14, right, default");
        
        vacuumTrendPartOffLow = new JTextField();
        panelPartOffVacuumSensing.add(vacuumTrendPartOffLow, "4, 14, fill, default");
        vacuumTrendPartOffLow.setColumns(10);
        
        vacuumTrendPartOffHigh = new JTextField();
        panelPartOffVacuumSensing.add(vacuumTrendPartOffHigh, "6, 14, fill, default");
        vacuumTrendPartOffHigh.setColumns(10);
        
        vacuumTrendPartOffReading = new JTextField();
        vacuumTrendPartOffReading.setEditable(false);
        panelPartOffVacuumSensing.add(vacuumTrendPartOffReading, "10, 14, fill, default");
        vacuumTrendPartOffReading.setColumns(10);
        
        vacuumPartOffGraph = new SimpleGraphView(nozzleTip);
        panelPartOffVacuumSensing.add(vacuumPartOffGraph, "4, 18, 7, 1");
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
    private JLabel lblPartOffDwell;
    private JTextField partOffDwellMilliseconds;
    private JCheckBox valveEnabledForPartOff;
    private JLabel lblValveEnabled;
    private JPanel panelPartOffVacuumSensing;
    private JLabel lblTrendTimePartOff;
    private JTextField partOffTrendMilliseconds;
    private JLabel lblPartOffTrendRange;
    private JTextField vacuumTrendPartOffLow;
    private JTextField vacuumTrendPartOffHigh;
    private JLabel lblPartOnDwell;
    private JTextField partOnDwellMilliseconds;
    private JLabel lblTrendTimePartOn;
    private JTextField partOnTrendMilliseconds;
    private JLabel lblPartOnTrendRange;
    private JTextField vacuumTrendPartOnLow;
    private JTextField vacuumTrendPartOnHigh;
    private JLabel lblPartOffLowValue;
    private JLabel lblPartOffHighValue;
    private JLabel lblPartOnLastReading;
    private JLabel lblPartOffLastReading;
    private JTextField vacuumLevelPartOnReading;
    private JTextField vacuumTrendPartOnReading;
    private JTextField vacuumLevelPartOffReading;
    private JTextField vacuumTrendPartOffReading;
    private SimpleGraphView vacuumPartOnGraph;
    private SimpleGraphView vacuumPartOffGraph;
    
    private void adaptDialog() {
        VacuumMeasurementMethod methodOn = (VacuumMeasurementMethod)methodPartOn.getSelectedItem();
        boolean partOn = true;
        boolean partOnTrend = true;
        if (methodOn == VacuumMeasurementMethod.None) {
            // hide all 
            partOn = false;
            partOnTrend = false;
        }
        else if (!methodOn.isTrendMethod()) {
            partOnTrend = false;
        }
         
        lblPartOnLowValue.setVisible(partOn);
        lblPartOnHighValue.setVisible(partOn);
        lblPartOnLastReading.setVisible(partOn);
        lblPartOnDwell.setVisible(partOn);
        partOnDwellMilliseconds.setVisible(partOn);
        lblPartOnNozzle.setVisible(partOn);
        vacuumLevelPartOnLow.setVisible(partOn);
        vacuumLevelPartOnHigh.setVisible(partOn);
        vacuumLevelPartOnReading.setVisible(partOn);
        
        lblTrendTimePartOn.setVisible(partOnTrend);
        partOnTrendMilliseconds.setVisible(partOnTrend);
        lblPartOnTrendRange.setVisible(partOnTrend);
        vacuumTrendPartOnLow.setVisible(partOnTrend);
        vacuumTrendPartOnHigh.setVisible(partOnTrend);
        vacuumTrendPartOnReading.setVisible(partOnTrend);
        
        VacuumMeasurementMethod methodOff = (VacuumMeasurementMethod)methodPartOff.getSelectedItem();
        boolean partOff = true;
        boolean partOffTrend = true;
        if (methodOff == VacuumMeasurementMethod.None) {
            // hide all 
            partOff = false;
            partOffTrend = false;
        }
        else if (!methodOff.isTrendMethod()) {
            partOffTrend = false;
        }
         
        lblValveEnabled.setVisible(partOff && ! partOffTrend);
        valveEnabledForPartOff.setVisible(partOff && ! partOffTrend);
        lblPartOffLowValue.setVisible(partOff);
        lblPartOffHighValue.setVisible(partOff);
        lblPartOffLastReading.setVisible(partOff);
        lblPartOffDwell.setVisible(partOff);
        partOffDwellMilliseconds.setVisible(partOff);
        lblPartOffNozzle.setVisible(partOff);
        vacuumLevelPartOffLow.setVisible(partOff);
        vacuumLevelPartOffHigh.setVisible(partOff);
        vacuumLevelPartOffReading.setVisible(partOff);
        
        lblTrendTimePartOff.setVisible(partOffTrend);
        partOffTrendMilliseconds.setVisible(partOffTrend);
        lblPartOffTrendRange.setVisible(partOffTrend);
        vacuumTrendPartOffLow.setVisible(partOffTrend);
        vacuumTrendPartOffHigh.setVisible(partOffTrend);
        vacuumTrendPartOffReading.setVisible(partOffTrend);
        
    }

    @Override
    public void createBindings() {
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        IntegerConverter integerConverter = new IntegerConverter();

        addWrappedBinding(nozzleTip, "methodPartOn", methodPartOn, "selectedItem");
        addWrappedBinding(nozzleTip, "partOnDwellMilliseconds", partOnDwellMilliseconds, "text", integerConverter);
        addWrappedBinding(nozzleTip, "vacuumLevelPartOnLow", vacuumLevelPartOnLow, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumLevelPartOnHigh", vacuumLevelPartOnHigh, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumLevelPartOnReading", vacuumLevelPartOnReading, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "partOnTrendMilliseconds", partOnTrendMilliseconds, "text", integerConverter);
        addWrappedBinding(nozzleTip, "vacuumTrendPartOnLow", vacuumTrendPartOnLow, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumTrendPartOnHigh", vacuumTrendPartOnHigh, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumTrendPartOnReading", vacuumTrendPartOnReading, "text", doubleConverter);

        addWrappedBinding(nozzleTip, "methodPartOff", methodPartOff, "selectedItem");
        addWrappedBinding(nozzleTip, "valveEnabledForPartOff", valveEnabledForPartOff, "selected");
        addWrappedBinding(nozzleTip, "partOffDwellMilliseconds", partOffDwellMilliseconds, "text", integerConverter);
        addWrappedBinding(nozzleTip, "vacuumLevelPartOffLow", vacuumLevelPartOffLow, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumLevelPartOffHigh", vacuumLevelPartOffHigh, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumLevelPartOffReading", vacuumLevelPartOffReading, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "partOffTrendMilliseconds", partOffTrendMilliseconds, "text", integerConverter);
        addWrappedBinding(nozzleTip, "vacuumTrendPartOffLow", vacuumTrendPartOffLow, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumTrendPartOffHigh", vacuumTrendPartOffHigh, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumTrendPartOffReading", vacuumTrendPartOffReading, "text", doubleConverter);
        
        addWrappedBinding(nozzleTip, "vacuumPartOnGraph", vacuumPartOnGraph, "graph");
        addWrappedBinding(nozzleTip, "vacuumPartOffGraph", vacuumPartOffGraph, "graph");

        ComponentDecorators.decorateWithAutoSelect(partOnDwellMilliseconds);
        ComponentDecorators.decorateWithAutoSelect(partOnTrendMilliseconds);
        ComponentDecorators.decorateWithAutoSelect(partOffDwellMilliseconds);
        ComponentDecorators.decorateWithAutoSelect(partOffTrendMilliseconds);
        
        ComponentDecorators.decorateWithAutoSelect(vacuumLevelPartOnLow);
        ComponentDecorators.decorateWithAutoSelect(vacuumLevelPartOnHigh);
        ComponentDecorators.decorateWithAutoSelect(vacuumTrendPartOnLow);
        ComponentDecorators.decorateWithAutoSelect(vacuumTrendPartOnHigh);
        ComponentDecorators.decorateWithAutoSelect(vacuumLevelPartOffLow);
        ComponentDecorators.decorateWithAutoSelect(vacuumLevelPartOffHigh);
        ComponentDecorators.decorateWithAutoSelect(vacuumTrendPartOffLow);
        ComponentDecorators.decorateWithAutoSelect(vacuumTrendPartOffHigh);
    }
}
