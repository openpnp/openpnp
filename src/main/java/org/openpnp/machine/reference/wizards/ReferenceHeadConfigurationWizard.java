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
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;

@SuppressWarnings("serial")
public class ReferenceHeadConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceHead head;
    private JTextField parkX;
    private JTextField parkY;


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

        JLabel lblParkLocation = new JLabel("Park Location");
        panel.add(lblParkLocation, "2, 4, right, default");

        parkX = new JTextField();
        panel.add(parkX, "4, 4, fill, default");
        parkX.setColumns(5);

        parkY = new JTextField();
        parkY.setColumns(5);
        panel.add(parkY, "6, 4, fill, default");
        
        JButton btnNewButton = new JButton(captureCameraCoordinatesAction);
        btnNewButton.setHideActionText(true);
        panel.add(btnNewButton, "8, 4");
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();

        MutableLocationProxy parkLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, head, "parkLocation", parkLocation, "location");
        addWrappedBinding(parkLocation, "lengthX", parkX, "text", lengthConverter);
        addWrappedBinding(parkLocation, "lengthY", parkY, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(parkX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(parkY);
    }
    
    private Action captureCameraCoordinatesAction =
            new AbstractAction("Get Camera Coordinates", Icons.captureCamera) {
                {
                    putValue(Action.SHORT_DESCRIPTION,
                            "Capture the location that the camera is centered on.");
                }

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    UiUtils.messageBoxOnException(() -> {
                        Location l = head.getDefaultCamera().getLocation();
                        Helpers.copyLocationIntoTextFields(l, parkX, parkY, null, null);
                    });
                }
            };
}
