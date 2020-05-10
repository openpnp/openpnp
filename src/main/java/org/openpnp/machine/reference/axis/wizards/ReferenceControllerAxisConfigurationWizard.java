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

package org.openpnp.machine.reference.axis.wizards;

import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AxesComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.DriversComboBoxModel;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.gui.wizards.AbstractAxisConfigurationWizard;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.base.AbstractAxis;
import org.openpnp.spi.base.AbstractMachine;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

@SuppressWarnings("serial")
public class ReferenceControllerAxisConfigurationWizard extends AbstractAxisConfigurationWizard {
    private JPanel panelControllerSettings;
    private JTextField homeCoordinate;
    private JLabel lblDesignator;
    private JTextField letter;
    private JLabel lblDriver;
    private JComboBox driver;
    private JLabel lblResolution;
    private JTextField resolution;
    private JLabel lblPremoveCommand;
    private JTextArea preMoveCommand;
    private JScrollPane scrollPane;
    protected NamedConverter<Driver> driverConverter;
    private JLabel lblBacklashOffset;
    private JTextField backlashOffset;

    public ReferenceControllerAxisConfigurationWizard(ReferenceControllerAxis axis) {
        super(axis);

        panelControllerSettings = new JPanel();
        panelControllerSettings.setBorder(new TitledBorder(null, "Controller Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelControllerSettings);
        panelControllerSettings.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default):grow"),
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
                RowSpec.decode("default:grow"),}));
        
        lblDriver = new JLabel("Driver");
        panelControllerSettings.add(lblDriver, "2, 2, right, default");
        
        driver = new JComboBox(new DriversComboBoxModel((AbstractMachine) Configuration.get().getMachine(), true));
        driver.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                adaptDialog();
            }
        });
        panelControllerSettings.add(driver, "4, 2, fill, default");
        
        lblDesignator = new JLabel("Axis Letter");
        lblDesignator.setToolTipText("The axis letter (X, Y, Z etc.) as used by the Controller.");
        panelControllerSettings.add(lblDesignator, "2, 4, right, default");
        
        letter = new JTextField();
        panelControllerSettings.add(letter, "4, 4, fill, default");
        letter.setColumns(10);

        JLabel lblHomeCoordinate = new JLabel("Home Coordinate");
        panelControllerSettings.add(lblHomeCoordinate, "2, 6, right, default");

        homeCoordinate = new JTextField();
        panelControllerSettings.add(homeCoordinate, "4, 6, fill, default");
        homeCoordinate.setColumns(10);
        
        lblBacklashOffset = new JLabel("Backlash Offset");
        panelControllerSettings.add(lblBacklashOffset, "2, 8, right, default");
        
        backlashOffset = new JTextField();
        panelControllerSettings.add(backlashOffset, "4, 8, fill, default");
        backlashOffset.setColumns(10);
        
        lblResolution = new JLabel("Resolution [Driver Units]");
        lblResolution.setToolTipText("<html>Numeric resolution of this axis. Coordinates will be rounded to the nearest multiple<br/>\r\nwhen comparing them in order to determine whether a move is necessary. <br/>\r\nFor the GcodeDriver, make sure the resolution can be expressed with the format in the <br/>\r\n<code>MOVE_TO_COMMAND</code>. Default is 0.0001 which corresponds to the %.4f <br/>\r\n(four fractional digits) format in the <code>MOVE_TO_COMMAND</code>.<br/>\r\nNote, the resolution is given and applied in driver (not system) units.\r\n</html>");
        panelControllerSettings.add(lblResolution, "2, 10, right, default");
        
        resolution = new JTextField();
        panelControllerSettings.add(resolution, "4, 10, fill, default");
        resolution.setColumns(10);
        
        lblPremoveCommand = new JLabel("Pre-Move Command");
        panelControllerSettings.add(lblPremoveCommand, "2, 14, right, top");
        
        scrollPane = new JScrollPane();
        panelControllerSettings.add(scrollPane, "4, 14, 3, 1, fill, fill");
        
        preMoveCommand = new JTextArea();
        preMoveCommand.setRows(1);
        scrollPane.setViewportView(preMoveCommand);
        
        driverConverter = new NamedConverter<>(Configuration.get().getMachine().getDrivers()); 
    }

    protected void adaptDialog() {
        Driver selectedDriver = driverConverter.convertReverse((String) driver.getSelectedItem());
        boolean showPreMove = (selectedDriver != null && selectedDriver.isSupportingPreMove());
        lblPremoveCommand.setVisible(showPreMove);
        scrollPane.setVisible(showPreMove);
    }

    @Override
    public void createBindings() {
        super.createBindings();
        LengthConverter lengthConverter = new LengthConverter();
        DoubleConverter doubleConverter = new DoubleConverter("%f"); 

        addWrappedBinding(axis, "driver", driver, "selectedItem", driverConverter);
        addWrappedBinding(axis, "letter", letter, "text");
        addWrappedBinding(axis, "homeCoordinate", homeCoordinate, "text", lengthConverter);
        addWrappedBinding(axis, "backlashOffset", backlashOffset, "text", lengthConverter);
        addWrappedBinding(axis, "resolution", resolution, "text", doubleConverter);
        addWrappedBinding(axis, "preMoveCommand", preMoveCommand, "text");
                
        ComponentDecorators.decorateWithAutoSelect(letter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homeCoordinate);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(backlashOffset);
        ComponentDecorators.decorateWithAutoSelect(resolution);
        
        adaptDialog();
    }
}
