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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.model.Configuration;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceHeadConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceHead head;
    private JTextField parkX;
    private JTextField parkY;
    private JTextField parkZ;
    private JTextField parkC;


    public ReferenceHeadConfigurationWizard(ReferenceHead head) {
        this.head = head;

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Locations", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblX = new JLabel("X");
        panel.add(lblX, "4, 2, center, default");

        JLabel lblY = new JLabel("Y");
        panel.add(lblY, "6, 2, center, default");

        JLabel lblZ = new JLabel("Z");
        panel.add(lblZ, "8, 2, center, default");

        JLabel lblRotation = new JLabel("Rotation");
        panel.add(lblRotation, "10, 2, center, default");

        JLabel lblParkLocation = new JLabel("Park Location");
        panel.add(lblParkLocation, "2, 4, right, default");

        parkX = new JTextField();
        panel.add(parkX, "4, 4, fill, default");
        parkX.setColumns(5);

        parkY = new JTextField();
        parkY.setColumns(5);
        panel.add(parkY, "6, 4, fill, default");

        parkZ = new JTextField();
        parkZ.setColumns(5);
        panel.add(parkZ, "8, 4, fill, default");

        parkC = new JTextField();
        parkC.setColumns(5);
        panel.add(parkC, "10, 4, fill, default");
        
        LocationButtonsPanel locationButtonsPanel = new LocationButtonsPanel(parkX, parkY, parkZ, parkC);
        panel.add(locationButtonsPanel, "12, 4");
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        MutableLocationProxy parkLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, head, "parkLocation", parkLocation, "location");
        addWrappedBinding(parkLocation, "lengthX", parkX, "text", lengthConverter);
        addWrappedBinding(parkLocation, "lengthY", parkY, "text", lengthConverter);
        addWrappedBinding(parkLocation, "lengthZ", parkZ, "text", lengthConverter);
        addWrappedBinding(parkLocation, "rotation", parkC, "text", doubleConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(parkX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(parkY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(parkZ);
        ComponentDecorators.decorateWithAutoSelect(parkC);
    }
}
