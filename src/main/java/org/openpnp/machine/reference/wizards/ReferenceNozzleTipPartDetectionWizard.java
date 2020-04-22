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
import org.openpnp.util.SimpleGraph;

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
import java.awt.Font;

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
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
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
                RowSpec.decode("default:grow"),}));
        
        lblPartOnMeasurement = new JLabel("Measurement Method");
        panelPartOnVacuumSensing.add(lblPartOnMeasurement, "2, 2, right, default");
        
        methodPartOn = new JComboBox(ReferenceNozzleTip.VacuumMeasurementMethod.values());
        methodPartOn.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                adaptDialog();
            }
        });
        panelPartOnVacuumSensing.add(methodPartOn, "4, 2, 3, 1");
        
        lblEstablishPartOnLevel = new JLabel("Establish Level?");
        lblEstablishPartOnLevel.setToolTipText("<html>While the nozzle is pressed down on the part in the pick operation,<br/>\r\nthe vacuum level is repeatedly measured until it builds up to the Vacuum Range <br/>\r\nor the Pick Dwell Time timeout expires, whichever comes first.\r\n</html>");
        panelPartOnVacuumSensing.add(lblEstablishPartOnLevel, "2, 4, right, default");
        
        establishPartOnLevel = new JCheckBox("");
        establishPartOnLevel.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                adaptDialog();
            }
        });
        panelPartOnVacuumSensing.add(establishPartOnLevel, "4, 4");
        
        lblPerformPartOnChecks = new JLabel("Perform Checks?");
        panelPartOnVacuumSensing.add(lblPerformPartOnChecks, "2, 6, right, default");
        
        partOnCheckAfterPick = new JCheckBox("After Pick");
        panelPartOnVacuumSensing.add(partOnCheckAfterPick, "4, 6");
        
        partOnCheckAlign = new JCheckBox("Alignment");
        panelPartOnVacuumSensing.add(partOnCheckAlign, "6, 6");
        
        partOnCheckBeforePlace = new JCheckBox("Before Place");
        panelPartOnVacuumSensing.add(partOnCheckBeforePlace, "10, 6");
        
        lblPartOnLowValue = new JLabel("Low Value");
        panelPartOnVacuumSensing.add(lblPartOnLowValue, "4, 8");
        
        lblPartOnHighValue = new JLabel("High Value");
        panelPartOnVacuumSensing.add(lblPartOnHighValue, "6, 8");
        
        lblPartOnLastReading = new JLabel("Last Reading");
        panelPartOnVacuumSensing.add(lblPartOnLastReading, "10, 8");
        
        lblPartOnNozzle = new JLabel("Vacuum Range");
        panelPartOnVacuumSensing.add(lblPartOnNozzle, "2, 10, right, default");
        
        vacuumLevelPartOnLow = new JTextField();
        panelPartOnVacuumSensing.add(vacuumLevelPartOnLow, "4, 10");
        vacuumLevelPartOnLow.setColumns(10);
        
        vacuumLevelPartOnHigh = new JTextField();
        panelPartOnVacuumSensing.add(vacuumLevelPartOnHigh, "6, 10");
        vacuumLevelPartOnHigh.setColumns(10);
        
        vacuumLevelPartOnReading = new JTextField();
        vacuumLevelPartOnReading.setEditable(false);
        panelPartOnVacuumSensing.add(vacuumLevelPartOnReading, "10, 10, fill, top");
        vacuumLevelPartOnReading.setColumns(10);
        
        lblPartOnDifferenceRange = new JLabel("Difference Range");
        panelPartOnVacuumSensing.add(lblPartOnDifferenceRange, "2, 12, right, default");
        
        vacuumDifferencePartOnLow = new JTextField();
        vacuumDifferencePartOnLow.setColumns(10);
        panelPartOnVacuumSensing.add(vacuumDifferencePartOnLow, "4, 12, fill, default");
        
        vacuumDifferencePartOnHigh = new JTextField();
        panelPartOnVacuumSensing.add(vacuumDifferencePartOnHigh, "6, 12, fill, default");
        vacuumDifferencePartOnHigh.setColumns(10);
        
        vacuumDifferencePartOnReading = new JTextField();
        vacuumDifferencePartOnReading.setEditable(false);
        panelPartOnVacuumSensing.add(vacuumDifferencePartOnReading, "10, 12, fill, default");
        vacuumDifferencePartOnReading.setColumns(10);
        
        lblLegendPartOn = new JLabel("<html>\r\n<p style=\"text-align:right\">Vacuum <span style=\"color:#FF0000\">&mdash;&mdash;</span></p>\r\n<p/>\r\n<p style=\"text-align:right\">Valve <span style=\"color:#005BD9\">&mdash;&mdash;</span></p>\r\n</html>");
        panelPartOnVacuumSensing.add(lblLegendPartOn, "2, 16, right, default");
        
        vacuumPartOnGraph = new SimpleGraphView();
        vacuumPartOnGraph.setFont(new Font("SansSerif", Font.PLAIN, 9));
        panelPartOnVacuumSensing.add(vacuumPartOnGraph, "4, 16, 9, 1, default, fill");
        
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
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
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
                RowSpec.decode("default:grow"),}));
        
        lblPartOffMeasurement = new JLabel("Measurement Method");
        panelPartOffVacuumSensing.add(lblPartOffMeasurement, "2, 2");
        
        methodPartOff = new JComboBox(ReferenceNozzleTip.VacuumMeasurementMethod.values());
        methodPartOff.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                adaptDialog();
            }
        });
        panelPartOffVacuumSensing.add(methodPartOff, "4, 2, 3, 1");
        
        lblEstablishPartOffLevel = new JLabel("Establish Level?");
        lblEstablishPartOffLevel.setToolTipText("<html>While the nozzle is pressed down on the part in the place operation,<br/>\r\nthe vacuum level is repeatedly measured until it decays to the Vacuum Range <br/>\r\nor the Place Dwell Time timeout expires, whichever comes first.\r\n</html>");
        panelPartOffVacuumSensing.add(lblEstablishPartOffLevel, "2, 4, right, default");
        
        establishPartOffLevel = new JCheckBox("");
        establishPartOffLevel.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                adaptDialog();
            }
        });
        panelPartOffVacuumSensing.add(establishPartOffLevel, "4, 4");
        
        lblPerformPartOffChecks = new JLabel("Perform Checks?");
        panelPartOffVacuumSensing.add(lblPerformPartOffChecks, "2, 6, right, default");
        
        partOffCheckAfterPlace = new JCheckBox("After Place");
        panelPartOffVacuumSensing.add(partOffCheckAfterPlace, "4, 6");
        
        partOffCheckBeforePick = new JCheckBox("Before Pick");
        panelPartOffVacuumSensing.add(partOffCheckBeforePick, "6, 6");
        
        lblPartOffLowValue = new JLabel("Low Value");
        panelPartOffVacuumSensing.add(lblPartOffLowValue, "4, 8");
        
        lblPartOffHighValue = new JLabel("High Value");
        panelPartOffVacuumSensing.add(lblPartOffHighValue, "6, 8");
        
        lblPartOffLastReading = new JLabel("Last Reading");
        panelPartOffVacuumSensing.add(lblPartOffLastReading, "10, 8");
        
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
        
        lblProbingTimePartOff = new JLabel("Valve open/close (ms)");
        lblProbingTimePartOff.setToolTipText("<html>\r\n<p>The valve is opened and closed to create a small underpressure pulse. </p>\r\n<p>The open time should be quite short, no point in creating full pick suction.</p>\r\n<p>The close time can be used to wait for the system to react to the pulse.<br/>\r\nincluding delays in sensor signal propagation and readout.</p>\r\n</html>");
        panelPartOffVacuumSensing.add(lblProbingTimePartOff, "2, 12, right, default");
        
        partOffProbingMilliseconds = new JTextField();
        panelPartOffVacuumSensing.add(partOffProbingMilliseconds, "4, 12, default, center");
        partOffProbingMilliseconds.setColumns(10);
        
        partOffDwellMilliseconds = new JTextField();
        panelPartOffVacuumSensing.add(partOffDwellMilliseconds, "6, 12, fill, default");
        partOffDwellMilliseconds.setColumns(10);
        
        lblPartOffDifferenceRange = new JLabel("Difference Range");
        panelPartOffVacuumSensing.add(lblPartOffDifferenceRange, "2, 14, right, default");
        
        vacuumDifferencePartOffLow = new JTextField();
        panelPartOffVacuumSensing.add(vacuumDifferencePartOffLow, "4, 14, fill, default");
        vacuumDifferencePartOffLow.setColumns(10);
        
        vacuumDifferencePartOffHigh = new JTextField();
        panelPartOffVacuumSensing.add(vacuumDifferencePartOffHigh, "6, 14, fill, default");
        vacuumDifferencePartOffHigh.setColumns(10);
        
        vacuumDifferencePartOffReading = new JTextField();
        vacuumDifferencePartOffReading.setEditable(false);
        panelPartOffVacuumSensing.add(vacuumDifferencePartOffReading, "10, 14, fill, default");
        vacuumDifferencePartOffReading.setColumns(10);
        
        lblLegendPartOff = new JLabel("<html>\r\n<p style=\"text-align:right\">Vacuum <span style=\"color:#FF0000\">&mdash;&mdash;</span></p>\r\n<p/>\r\n<p style=\"text-align:right\">Valve <span style=\"color:#005BD9\">&mdash;&mdash;</span></p>\r\n</html>");
        panelPartOffVacuumSensing.add(lblLegendPartOff, "2, 18, right, default");
        
        vacuumPartOffGraph = new SimpleGraphView();
        vacuumPartOffGraph.setFont(new Font("SansSerif", Font.PLAIN, 9));
        panelPartOffVacuumSensing.add(vacuumPartOffGraph, "4, 18, 9, 1, default, fill");
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
    private JLabel lblPartOffDifferenceRange;
    private JTextField vacuumDifferencePartOffLow;
    private JTextField vacuumDifferencePartOffHigh;
    private JLabel lblPartOnDifferenceRange;
    private JTextField vacuumDifferencePartOnLow;
    private JTextField vacuumDifferencePartOnHigh;
    private JLabel lblPartOffLowValue;
    private JLabel lblPartOffHighValue;
    private JLabel lblPartOnLastReading;
    private JLabel lblPartOffLastReading;
    private JTextField vacuumLevelPartOnReading;
    private JTextField vacuumDifferencePartOnReading;
    private JTextField vacuumLevelPartOffReading;
    private JTextField vacuumDifferencePartOffReading;
    private SimpleGraphView vacuumPartOnGraph;
    private SimpleGraphView vacuumPartOffGraph;
    private JCheckBox establishPartOnLevel;
    private JLabel lblEstablishPartOnLevel;
    private JLabel lblEstablishPartOffLevel;
    private JCheckBox establishPartOffLevel;
    private JLabel lblLegendPartOn;
    private JLabel lblLegendPartOff;
    private JLabel lblPerformPartOnChecks;
    private JCheckBox partOnCheckAfterPick;
    private JCheckBox partOnCheckAlign;
    private JCheckBox partOnCheckBeforePlace;
    private JLabel lblPerformPartOffChecks;
    private JCheckBox partOffCheckAfterPlace;
    private JCheckBox partOffCheckBeforePick;
    private JTextField partOffDwellMilliseconds;
    
    private void adaptDialog() {
        VacuumMeasurementMethod methodOn = (VacuumMeasurementMethod)methodPartOn.getSelectedItem();
        boolean establishOn = establishPartOnLevel.isSelected();
        boolean partOn = true;
        boolean partOnDifference = true;
        boolean graphOn = establishOn || methodOn.isDifferenceMethod();
        if (methodOn == VacuumMeasurementMethod.None) {
            // hide all 
            partOn = false;
            partOnDifference = false;
            graphOn = false;
        }
        else if (!methodOn.isDifferenceMethod()) {
            partOnDifference = false;
        }
         
        lblEstablishPartOnLevel.setVisible(partOn);
        establishPartOnLevel.setVisible(partOn);
        lblPerformPartOnChecks.setVisible(partOn);
        partOnCheckAfterPick.setVisible(partOn);
        partOnCheckAlign.setVisible(partOn);
        partOnCheckBeforePlace.setVisible(partOn);
        lblPartOnLowValue.setVisible(partOn);
        lblPartOnHighValue.setVisible(partOn);
        lblPartOnLastReading.setVisible(partOn);
        lblPartOnNozzle.setVisible(partOn);
        vacuumLevelPartOnLow.setVisible(partOn);
        vacuumLevelPartOnHigh.setVisible(partOn);
        vacuumLevelPartOnReading.setVisible(partOn);
        
        lblPartOnDifferenceRange.setVisible(partOnDifference);
        vacuumDifferencePartOnLow.setVisible(partOnDifference);
        vacuumDifferencePartOnHigh.setVisible(partOnDifference);
        vacuumDifferencePartOnReading.setVisible(partOnDifference);
        lblLegendPartOn.setVisible(graphOn);
        vacuumPartOnGraph.setVisible(graphOn);
        
        VacuumMeasurementMethod methodOff = (VacuumMeasurementMethod)methodPartOff.getSelectedItem();
        boolean establishOff = establishPartOffLevel.isSelected();
        boolean partOff = true;
        boolean partOffDifference = true;
        boolean graphOff = establishOff || methodOff.isDifferenceMethod();
        if (methodOff == VacuumMeasurementMethod.None) {
            // hide all 
            partOff = false;
            partOffDifference = false;
            graphOff = false;
        }
        else if (!methodOff.isDifferenceMethod()) {
            partOffDifference = false;
        }
         
        lblEstablishPartOffLevel.setVisible(partOff);
        establishPartOffLevel.setVisible(partOff);
        lblPerformPartOffChecks.setVisible(partOff);
        partOffCheckAfterPlace.setVisible(partOff);
        partOffCheckBeforePick.setVisible(partOff);
        lblPartOffLowValue.setVisible(partOff);
        lblPartOffHighValue.setVisible(partOff);
        lblPartOffLastReading.setVisible(partOff);
        lblPartOffNozzle.setVisible(partOff);
        vacuumLevelPartOffLow.setVisible(partOff);
        vacuumLevelPartOffHigh.setVisible(partOff);
        vacuumLevelPartOffReading.setVisible(partOff);
        lblProbingTimePartOff.setVisible(partOff);
        partOffProbingMilliseconds.setVisible(partOff);
        partOffDwellMilliseconds.setVisible(partOff);
        
        lblPartOffDifferenceRange.setVisible(partOffDifference);
        vacuumDifferencePartOffLow.setVisible(partOffDifference);
        vacuumDifferencePartOffHigh.setVisible(partOffDifference);
        vacuumDifferencePartOffReading.setVisible(partOffDifference);
        lblLegendPartOff.setVisible(graphOff);
        vacuumPartOffGraph.setVisible(graphOff);
    }

    @Override
    public void createBindings() {
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        IntegerConverter integerConverter = new IntegerConverter();

        addWrappedBinding(nozzleTip, "methodPartOn", methodPartOn, "selectedItem");
        addWrappedBinding(nozzleTip, "establishPartOnLevel", establishPartOnLevel, "selected");
        addWrappedBinding(nozzleTip, "partOnCheckAfterPick", partOnCheckAfterPick, "selected");
        addWrappedBinding(nozzleTip, "partOnCheckAlign", partOnCheckAlign, "selected");
        addWrappedBinding(nozzleTip, "partOnCheckBeforePlace", partOnCheckBeforePlace, "selected");
        addWrappedBinding(nozzleTip, "vacuumLevelPartOnLow", vacuumLevelPartOnLow, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumLevelPartOnHigh", vacuumLevelPartOnHigh, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumLevelPartOnReading", vacuumLevelPartOnReading, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumDifferencePartOnLow", vacuumDifferencePartOnLow, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumDifferencePartOnHigh", vacuumDifferencePartOnHigh, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumDifferencePartOnReading", vacuumDifferencePartOnReading, "text", doubleConverter);

        addWrappedBinding(nozzleTip, "methodPartOff", methodPartOff, "selectedItem");
        addWrappedBinding(nozzleTip, "establishPartOffLevel", establishPartOffLevel, "selected");
        addWrappedBinding(nozzleTip, "partOffCheckAfterPlace", partOffCheckAfterPlace, "selected");
        addWrappedBinding(nozzleTip, "partOffCheckBeforePick", partOffCheckBeforePick, "selected");
        addWrappedBinding(nozzleTip, "vacuumLevelPartOffLow", vacuumLevelPartOffLow, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumLevelPartOffHigh", vacuumLevelPartOffHigh, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumLevelPartOffReading", vacuumLevelPartOffReading, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "partOffProbingMilliseconds", partOffProbingMilliseconds, "text", integerConverter);
        addWrappedBinding(nozzleTip, "partOffDwellMilliseconds", partOffDwellMilliseconds, "text", integerConverter);
        addWrappedBinding(nozzleTip, "vacuumDifferencePartOffLow", vacuumDifferencePartOffLow, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumDifferencePartOffHigh", vacuumDifferencePartOffHigh, "text", doubleConverter);
        addWrappedBinding(nozzleTip, "vacuumDifferencePartOffReading", vacuumDifferencePartOffReading, "text", doubleConverter);
        
        addWrappedBinding(nozzleTip, "vacuumPartOnGraph", vacuumPartOnGraph, "graph");
        
        final boolean testMode = false;
        if (!testMode) {
            addWrappedBinding(nozzleTip, "vacuumPartOffGraph", vacuumPartOffGraph, "graph");
        }
        else {
            SimpleGraph vacuumGraph = new SimpleGraph();
            vacuumGraph.setOffsetMode(true);
            vacuumGraph.setRelativePaddingLeft(0.1);
            long t = System.currentTimeMillis();
            double scale = Math.exp(Math.random()*7.0-3.0);
            double offset = (Math.random()-0.3)*scale*2.0;
            long duration = (long)(Math.random()*1000.0);
            // init pressure scale
            SimpleGraph.DataScale vacuumScale =  vacuumGraph.getScale("P");
            vacuumScale.setRelativePaddingBottom(0.25);
            vacuumScale.setColor(new Color(0, 0, 0, 64));
            // init valve scale
            SimpleGraph.DataScale valveScale =  vacuumGraph.getScale("B");
            valveScale.setRelativePaddingTop(0.8);
            valveScale.setRelativePaddingBottom(0.1);
            // record the current pressure
            SimpleGraph.DataRow vacuumData = vacuumGraph.getRow("P", "V");
            vacuumData.setColor(new Color(255, 0, 0));
            // record the valve switching off
            SimpleGraph.DataRow valveData = vacuumGraph.getRow("B", "S");
            valveData.setColor(new Color(00, 0x5B, 0xD9));
            valveData.recordDataPoint(t-1, 0);
            valveData.recordDataPoint(t, 1);
            for (long td = t; td < t+duration; td++) {
                vacuumData.recordDataPoint(td, Math.sin(td*3.0/duration)*scale+offset+Math.random()*scale*0.05);
            }
            valveData.recordDataPoint(t+duration-1, 1);
            valveData.recordDataPoint(t+duration, 0);
            vacuumPartOffGraph.setGraph(vacuumGraph);
        }

        ComponentDecorators.decorateWithAutoSelect(partOffProbingMilliseconds);
        ComponentDecorators.decorateWithAutoSelect(partOffDwellMilliseconds);

        ComponentDecorators.decorateWithAutoSelect(vacuumLevelPartOnLow);
        ComponentDecorators.decorateWithAutoSelect(vacuumLevelPartOnHigh);
        ComponentDecorators.decorateWithAutoSelect(vacuumDifferencePartOnLow);
        ComponentDecorators.decorateWithAutoSelect(vacuumDifferencePartOnHigh);
        ComponentDecorators.decorateWithAutoSelect(vacuumLevelPartOffLow);
        ComponentDecorators.decorateWithAutoSelect(vacuumLevelPartOffHigh);
        ComponentDecorators.decorateWithAutoSelect(vacuumDifferencePartOffLow);
        ComponentDecorators.decorateWithAutoSelect(vacuumDifferencePartOffHigh);
    }
}
