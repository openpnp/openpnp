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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.Translations;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.machine.reference.axis.ReferenceVirtualAxis;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceVirtualAxisConfigurationWizard extends AbstractAxisConfigurationWizard {

    private JPanel panelTransformation;
    private JTextField homeCoordinate;

    public ReferenceVirtualAxisConfigurationWizard(ReferenceVirtualAxis axis) {
        super(axis);
        panelTransformation = new JPanel();
        panelTransformation.setBorder(new TitledBorder(null, Translations.getStringOrDefault(
                "ReferenceVirtualAxisConfigurationWizard.TransformationPanel.Border.title",
                "Virtual Axis"), TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelTransformation);
        panelTransformation.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(50dlu;default)"),
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
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        RowSpec.decode("bottom:default:grow"),}));

        JLabel lblHomeCoordinate = new JLabel(Translations.getStringOrDefault(
                "ReferenceVirtualAxisConfigurationWizard.TransformationPanel.HomeSafeZLabel.text",
                "Home / Safe Z"));
        panelTransformation.add(lblHomeCoordinate, "2, 2, right, default");

        homeCoordinate = new JTextField();
        panelTransformation.add(homeCoordinate, "4, 2, fill, default");
        homeCoordinate.setColumns(10);

    }

    @Override
    public void createBindings() {
        super.createBindings();
        LengthConverter lengthConverter = new LengthConverter();
        addWrappedBinding(axis, "homeCoordinate", homeCoordinate, "text", lengthConverter);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(homeCoordinate);
    }
}
