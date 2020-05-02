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

import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.wizards.AbstractAxisConfigurationWizard;
import org.openpnp.machine.reference.ReferenceControllerAxis;
import org.openpnp.spi.base.AbstractAxis;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JLabel;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class ReferenceControllerAxisConfigurationWizard extends AbstractAxisConfigurationWizard {
    protected final AbstractAxis axis;
    
    private JPanel panelControllerSettings;
    private JTextField homeCoordinate;

    public ReferenceControllerAxisConfigurationWizard(ReferenceControllerAxis axis) {
        super();
        this.axis = axis;

        panelControllerSettings = new JPanel();
        panelControllerSettings.setBorder(new TitledBorder(null, "Controller Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelControllerSettings);
        panelControllerSettings.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblHomeCoordinate = new JLabel("Home Coordinate");
        panelControllerSettings.add(lblHomeCoordinate, "2, 2, right, default");

        homeCoordinate = new JTextField();
        panelControllerSettings.add(homeCoordinate, "4, 2, fill, default");
        homeCoordinate.setColumns(10);
    }

    @Override
    public void createBindings() {
        super.createBindings();
        LengthConverter lengthConverter = new LengthConverter();

        addWrappedBinding(axis, "homeCoordinate", homeCoordinate, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homeCoordinate);
    }

    @Override
    protected AbstractAxis getAxis() {
        return axis;
    }
}
