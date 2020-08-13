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

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.spi.Axis;
import org.openpnp.spi.base.AbstractAxis;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public abstract class AbstractAxisConfigurationWizard extends AbstractConfigurationWizard {
    protected final AbstractAxis axis;
    protected JPanel panelProperties;
    protected JLabel lblName;
    protected JTextField name;
    protected JLabel lblType;
    protected JComboBox type;

    public AbstractAxisConfigurationWizard(AbstractAxis axis) {
        super();
        this.axis = axis;
        panelProperties = new JPanel();
        panelProperties.setBorder(new TitledBorder(null, "Properties", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelProperties);
        panelProperties.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblType = new JLabel("Type");
        panelProperties.add(lblType, "2, 2, right, default");
        
        type = new JComboBox(Axis.Type.values());
        panelProperties.add(type, "4, 2, fill, default");
        
        lblName = new JLabel("Name");
        panelProperties.add(lblName, "2, 4, right, default");
        
        name = new JTextField();
        panelProperties.add(name, "4, 4, fill, default");
        name.setColumns(20);
    }

    protected AbstractAxis getAxis() {
        return axis;
    }
    
    @Override
    public void createBindings() {
        addWrappedBinding(axis, "type", type, "selectedItem");
        addWrappedBinding(axis, "name", name, "text");

        ComponentDecorators.decorateWithAutoSelect(name);
    }
}
