/*
 * Copyright (C) 2020 Jason von Nieda <jason@vonnieda.org>
 * Copyright (C) 2020 Thomas Langaas <thomas.langaas@gmail.com>
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

package org.openpnp.machine.neoden4.wizards;

import java.awt.Color;

import javax.swing.SwingConstants;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.machine.neoden4.NeoDen4Driver;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class Neoden4DriverConfigurationWizard extends AbstractConfigurationWizard {
    private final NeoDen4Driver driver;

    private JPanel panelMachineDetails;
    private JTextField homeCoordinateXTextField;
    private JTextField homeCoordinateYTextField;
    private JTextField scaleFactorXTextField;
    private JTextField scaleFactorYTextField;

    public Neoden4DriverConfigurationWizard(NeoDen4Driver driver) {
        this.driver = driver;

        JPanel panelMachineDetails = new JPanel();
        panelMachineDetails.setBorder(new TitledBorder(null,
                "Machine Details", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(panelMachineDetails);
        panelMachineDetails.setLayout(new FormLayout(
                new ColumnSpec[] { 
                    FormSpecs.RELATED_GAP_COLSPEC, 
                    FormSpecs.DEFAULT_COLSPEC,
                    FormSpecs.RELATED_GAP_COLSPEC, 
                    FormSpecs.DEFAULT_COLSPEC, 
                    FormSpecs.RELATED_GAP_COLSPEC, },
                new RowSpec[] { 
                    FormSpecs.RELATED_GAP_ROWSPEC, 
                    FormSpecs.DEFAULT_ROWSPEC, 
                    FormSpecs.RELATED_GAP_ROWSPEC,
                    FormSpecs.DEFAULT_ROWSPEC, 
                    FormSpecs.RELATED_GAP_ROWSPEC, 
                    FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC, 
                    FormSpecs.DEFAULT_ROWSPEC,
                    FormSpecs.RELATED_GAP_ROWSPEC, }));

        JLabel lblHomeCoordinateX = new JLabel("Home Coordinate X");
        panelMachineDetails.add(lblHomeCoordinateX, "2, 2, right, default");

        homeCoordinateXTextField = new JTextField();
        panelMachineDetails.add(homeCoordinateXTextField, "4, 2, fill, default");
        homeCoordinateXTextField.setColumns(10);

        JLabel lblHomeCoordinateY = new JLabel("Home Coordinate Y");
        panelMachineDetails.add(lblHomeCoordinateY, "2, 4, right, default");

        homeCoordinateYTextField = new JTextField();
        homeCoordinateYTextField.setColumns(10);
        panelMachineDetails.add(homeCoordinateYTextField, "4, 4, fill, default");

        JLabel lblScaleFactorX = new JLabel("Scale Factor - X");
        panelMachineDetails.add(lblScaleFactorX, "2, 6, right, default");

        scaleFactorXTextField = new JTextField();
        scaleFactorXTextField.setColumns(10);
        panelMachineDetails.add(scaleFactorXTextField, "4, 6, fill, default");

        JLabel lblScaleFactorY = new JLabel("Scale Factor - Y");
        panelMachineDetails.add(lblScaleFactorY, "2, 8, right, default");

        scaleFactorYTextField = new JTextField();
        scaleFactorYTextField.setColumns(10);
        panelMachineDetails.add(scaleFactorYTextField, "4, 8, fill, default");

    }

    @Override
    public void createBindings() {
        DoubleConverter doubleConverter = new DoubleConverter("%f");
        addWrappedBinding(driver, "homeCoordinateX", homeCoordinateXTextField, "text", doubleConverter);
        addWrappedBinding(driver, "homeCoordinateY", homeCoordinateYTextField, "text", doubleConverter);
        addWrappedBinding(driver, "scaleFactorX", scaleFactorXTextField, "text", doubleConverter);
        addWrappedBinding(driver, "scaleFactorY", scaleFactorYTextField, "text", doubleConverter);
    }
}