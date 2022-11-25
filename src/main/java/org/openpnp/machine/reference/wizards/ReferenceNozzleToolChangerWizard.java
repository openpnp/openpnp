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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.Translations;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.ReferenceNozzle;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceNozzleToolChangerWizard extends AbstractConfigurationWizard {
    private final ReferenceNozzle nozzle;
    private JPanel panelChanger;
    private JCheckBox chckbxChangerEnabled;
    private JLabel lblChangerEnabled;
    private JLabel lblChangeOnManual;
    private JCheckBox chckbxChangeOnManualFeed;
    private JLabel lblManualChangeLocation;
    private JTextField manualNozzleTipChangeX;
    private JTextField manualNozzleTipChangeY;
    private JTextField manualNozzleTipChangeZ;
    private JLabel lblX;
    private JLabel lblY;
    private JLabel lblZ;
    private LocationButtonsPanel changerStartLocationButtonsPanel;

    public ReferenceNozzleToolChangerWizard(ReferenceNozzle nozzle) {
        this.nozzle = nozzle;


        panelChanger = new JPanel();
        panelChanger.setBorder(new TitledBorder(null, Translations.getString(
                "ReferenceNozzleToolChangerWizard.ChangerPanel.Border.title"),
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelChanger);
        panelChanger
                .setLayout(
                        new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
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
                FormSpecs.DEFAULT_ROWSPEC,}));
                
        lblChangerEnabled = new JLabel(Translations.getString(
                "ReferenceNozzleToolChangerWizard.ChangerPanel.ChangerEnabledLabel.text"));
        panelChanger.add(lblChangerEnabled, "2, 2, right, default");

        chckbxChangerEnabled = new JCheckBox("");
        chckbxChangerEnabled.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                boolean enableManual = !chckbxChangerEnabled.isSelected();
                lblManualChangeLocation.setVisible(enableManual);
                lblX.setVisible(enableManual);
                lblY.setVisible(enableManual);
                lblZ.setVisible(enableManual);
                manualNozzleTipChangeX.setVisible(enableManual);
                manualNozzleTipChangeY.setVisible(enableManual);
                manualNozzleTipChangeZ.setVisible(enableManual);
                changerStartLocationButtonsPanel.setVisible(enableManual);
            }
        });
        panelChanger.add(chckbxChangerEnabled, "4, 2");
        
        lblChangeOnManual = new JLabel(Translations.getString(
                "ReferenceNozzleToolChangerWizard.ChangerPanel.ChangeOnManualLabel.text"));
        panelChanger.add(lblChangeOnManual, "2, 4, right, default");
        
        chckbxChangeOnManualFeed = new JCheckBox("");
        panelChanger.add(chckbxChangeOnManualFeed, "4, 4");
        
        lblX = new JLabel("X");
        panelChanger.add(lblX, "4, 6, center, default");
        
        lblY = new JLabel("Y");
        panelChanger.add(lblY, "6, 6, center, default");
        
        lblZ = new JLabel("Z");
        panelChanger.add(lblZ, "8, 6, center, default");
        
        lblManualChangeLocation = new JLabel(Translations.getString(
                "ReferenceNozzleToolChangerWizard.ChangerPanel.ManualChangeLocationLabel.text"));
        lblManualChangeLocation.setToolTipText("r");
        panelChanger.add(lblManualChangeLocation, "2, 8, right, default");
        
        manualNozzleTipChangeX = new JTextField();
        panelChanger.add(manualNozzleTipChangeX, "4, 8, fill, default");
        manualNozzleTipChangeX.setColumns(8);
        
        manualNozzleTipChangeY = new JTextField();
        panelChanger.add(manualNozzleTipChangeY, "6, 8, fill, default");
        manualNozzleTipChangeY.setColumns(8);
        
        manualNozzleTipChangeZ = new JTextField();
        panelChanger.add(manualNozzleTipChangeZ, "8, 8, fill, default");
        manualNozzleTipChangeZ.setColumns(8);
        
        changerStartLocationButtonsPanel = new LocationButtonsPanel(manualNozzleTipChangeX,
                manualNozzleTipChangeY, manualNozzleTipChangeZ, (JTextField) null);
        panelChanger.add(changerStartLocationButtonsPanel, "10, 8, fill, default");
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();

        addWrappedBinding(nozzle, "changerEnabled", chckbxChangerEnabled, "selected");
        addWrappedBinding(nozzle, "nozzleTipChangedOnManualFeed", chckbxChangeOnManualFeed, "selected");

        MutableLocationProxy manualNozzleTipChangeLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, nozzle, "manualNozzleTipChangeLocation", manualNozzleTipChangeLocation,
                "location");
        addWrappedBinding(manualNozzleTipChangeLocation, "lengthX", manualNozzleTipChangeX, "text",
                lengthConverter);
        addWrappedBinding(manualNozzleTipChangeLocation, "lengthY", manualNozzleTipChangeY, "text",
                lengthConverter);
        addWrappedBinding(manualNozzleTipChangeLocation, "lengthZ", manualNozzleTipChangeZ, "text",
                lengthConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(manualNozzleTipChangeX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(manualNozzleTipChangeY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(manualNozzleTipChangeZ);
    }
}
