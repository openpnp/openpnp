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
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.openpnp.Translations;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.machine.reference.ReferenceNozzleTip;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceNozzleTipConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceNozzleTip nozzleTip;
    private JPanel panelDwellTime;
    private JLabel lblPickDwellTime;
    private JLabel lblPlaceDwellTime;
    private JLabel lblDwellTime;
    private JTextField pickDwellTf;
    private JTextField placeDwellTf;

    private Set<org.openpnp.model.Package> compatiblePackages = new HashSet<>();
    private JPanel panel;
    private JLabel lblName;
    private JTextField nameTf;
    private JPanel panelPushAndDrag;
    private JComponent lblPushAndDragAllowed;
    private JLabel lblLowDiameter;
    private JTextField textFieldLowDiameter;
    private JCheckBox chckbxPushAndDragAllowed;
    private JPanel panelPartDimensions;
    private JLabel lblMaxPartHeight;
    private JTextField maxPartHeight;
    private JLabel lblMaxPartDiameter;
    private JTextField maxPartDiameter;
    private JLabel lblMinPartDiameter;
    private JTextField minPartDiameter;
    private JLabel lblMaxPickTolerance;
    private JTextField maxPickTolerance;


    public ReferenceNozzleTipConfigurationWizard(ReferenceNozzleTip nozzleTip) {
        this.nozzleTip = nozzleTip;
        
        panel = new JPanel();
        panel.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceNozzleTipConfigurationWizard.PropertiesPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblName = new JLabel(Translations.getString(
                "ReferenceNozzleTipConfigurationWizard.PropertiesPanel.NameLabel.text")); //$NON-NLS-1$
        panel.add(lblName, "2, 2, right, default");
        
        nameTf = new JTextField();
        panel.add(nameTf, "4, 2, fill, default");
        nameTf.setColumns(10);
        
        panelDwellTime = new JPanel();
        panelDwellTime.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceNozzleTipConfigurationWizard.DwellTimesPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelDwellTime);
        panelDwellTime.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
          
        lblPickDwellTime = new JLabel(Translations.getString(
                "ReferenceNozzleTipConfigurationWizard.DwellTimesPanel.PickDwellTimeLabel.text")); //$NON-NLS-1$
        panelDwellTime.add(lblPickDwellTime, "2, 2, right, default");
        
        pickDwellTf = new JTextField();
        panelDwellTime.add(pickDwellTf, "4, 2");
        pickDwellTf.setColumns(10);
        
        lblPlaceDwellTime = new JLabel(Translations.getString(
                "ReferenceNozzleTipConfigurationWizard.DwellTimesPanel.PlaceDwellTimeLabel.text")); //$NON-NLS-1$
        panelDwellTime.add(lblPlaceDwellTime, "2, 4, right, default");
        
        placeDwellTf = new JTextField();
        panelDwellTime.add(placeDwellTf, "4, 4");
        placeDwellTf.setColumns(10);
        
        CellConstraints cc = new CellConstraints();
        lblDwellTime = new JLabel(Translations.getString(
                "ReferenceNozzleTipConfigurationWizard.DwellTimesPanel.DwellTimeLabel.text")); //$NON-NLS-1$
        panelDwellTime.add(lblDwellTime, cc.xywh(2, 6, 5, 1));

        panelPushAndDrag = new JPanel();
        panelPushAndDrag.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"),
                Translations.getString("ReferenceNozzleTipConfigurationWizard.PushAndDragPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null));
        contentPanel.add(panelPushAndDrag);
        panelPushAndDrag.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblLowDiameter = new JLabel(Translations.getString(
                "ReferenceNozzleTipConfigurationWizard.PushAndDragPanel.OutsideDiameterLabel.text")); //$NON-NLS-1$
        lblLowDiameter.setToolTipText(Translations.getString(
                "ReferenceNozzleTipConfigurationWizard.PushAndDragPanel.OutsideDiameterLabel.toolTipText")); //$NON-NLS-1$
        panelPushAndDrag.add(lblLowDiameter, "2, 4, right, default");
        
        textFieldLowDiameter = new JTextField();
        panelPushAndDrag.add(textFieldLowDiameter, "4, 4");
        textFieldLowDiameter.setColumns(10);
        
        lblPushAndDragAllowed = new JLabel(Translations.getString(
                "ReferenceNozzleTipConfigurationWizard.PushAndDragPanel.PushAndDragAllowedLabel.text")); //$NON-NLS-1$
        lblPushAndDragAllowed.setToolTipText(Translations.getString(
                "ReferenceNozzleTipConfigurationWizard.PushAndDragPanel.PushAndDragAllowedLabel.toolTipText")); //$NON-NLS-1$
        panelPushAndDrag.add(lblPushAndDragAllowed, "2, 2, right, default");
        
        chckbxPushAndDragAllowed = new JCheckBox("");
        panelPushAndDrag.add(chckbxPushAndDragAllowed, "4, 2");

        panelPartDimensions= new JPanel();
        panelPartDimensions.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"),
                Translations.getString(
                        "ReferenceNozzleTipConfigurationWizard.PartDimensionsPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null));
        contentPanel.add(panelPartDimensions);

        panelPartDimensions.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
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
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblMinPartDiameter = new JLabel(Translations.getString(
                "ReferenceNozzleTipConfigurationWizard.PartDimensionsPanel.MinPartDiameterLabel.text")); //$NON-NLS-1$
        lblMinPartDiameter.setToolTipText(Translations.getString(
                "ReferenceNozzleTipConfigurationWizard.PartDimensionsPanel.MinPartDiameterLabel.toolTipText")); //$NON-NLS-1$
        panelPartDimensions.add(lblMinPartDiameter, "2, 2, right, default");
        
        minPartDiameter = new JTextField();
        panelPartDimensions.add(minPartDiameter, "4, 2, fill, default");
        minPartDiameter.setColumns(10);

        lblMaxPartDiameter = new JLabel(Translations.getString(
                "ReferenceNozzleTipConfigurationWizard.PartDimensionsPanel.MaxPartDiameterLabel.text")); //$NON-NLS-1$
        lblMaxPartDiameter.setToolTipText(Translations.getString(
                "ReferenceNozzleTipConfigurationWizard.PartDimensionsPanel.MaxPartDiameterLabel.toolTipText")); //$NON-NLS-1$
        panelPartDimensions.add(lblMaxPartDiameter, "2, 4, right, default");

        maxPartDiameter = new JTextField();
        panelPartDimensions.add(maxPartDiameter, "4, 4, fill, default");
        maxPartDiameter.setColumns(10);

        lblMaxPartHeight = new JLabel(Translations.getString(
                "ReferenceNozzleTipConfigurationWizard.PartDimensionsPanel.MaxPartHeightLabel.text")); //$NON-NLS-1$
        lblMaxPartHeight.setToolTipText(Translations.getString(
                "ReferenceNozzleTipConfigurationWizard.PartDimensionsPanel.MaxPartHeightLabel.toolTipText")); //$NON-NLS-1$
        panelPartDimensions.add(lblMaxPartHeight, "2, 6, right, default");

        maxPartHeight = new JTextField();
        panelPartDimensions.add(maxPartHeight, "4, 6, fill, default");
        maxPartHeight.setColumns(10);
        
        lblMaxPickTolerance = new JLabel(Translations.getString(
                "ReferenceNozzleTipConfigurationWizard.PartDimensionsPanel.MaxPickToleranceLabel.text")); //$NON-NLS-1$
        lblMaxPickTolerance.setToolTipText(Translations.getString(
                "ReferenceNozzleTipConfigurationWizard.PartDimensionsPanel.MaxPickToleranceLabel.toolTipText")); //$NON-NLS-1$
        panelPartDimensions.add(lblMaxPickTolerance, "2, 8, right, default");
        
        maxPickTolerance = new JTextField();
        maxPickTolerance.setToolTipText("");
        panelPartDimensions.add(maxPickTolerance, "4, 8, fill, default");
        maxPickTolerance.setColumns(10);
    }

    @Override
    public void createBindings() {
        IntegerConverter intConverter = new IntegerConverter();
        LengthConverter lengthConverter = new LengthConverter();

        addWrappedBinding(nozzleTip, "name", nameTf, "text");

        addWrappedBinding(nozzleTip, "pickDwellMilliseconds", pickDwellTf, "text", intConverter);
        addWrappedBinding(nozzleTip, "placeDwellMilliseconds", placeDwellTf, "text", intConverter);

        addWrappedBinding(nozzleTip, "diameterLow", textFieldLowDiameter, "text", lengthConverter);

        addWrappedBinding(nozzleTip, "pushAndDragAllowed", chckbxPushAndDragAllowed, "selected");

        addWrappedBinding(nozzleTip, "minPartDiameter", minPartDiameter, "text", lengthConverter);
        addWrappedBinding(nozzleTip, "maxPartDiameter", maxPartDiameter, "text", lengthConverter);
        addWrappedBinding(nozzleTip, "maxPartHeight", maxPartHeight, "text", lengthConverter);
        addWrappedBinding(nozzleTip, "maxPickTolerance", maxPickTolerance, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelect(nameTf);

        ComponentDecorators.decorateWithAutoSelect(pickDwellTf);
        ComponentDecorators.decorateWithAutoSelect(placeDwellTf);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLowDiameter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(minPartDiameter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(maxPartDiameter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(maxPartHeight);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(maxPickTolerance);
    }
}
