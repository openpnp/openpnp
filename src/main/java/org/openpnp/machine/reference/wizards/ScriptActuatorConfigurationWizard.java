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
import org.I18n.I18n;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.machine.reference.ScriptActuator;
import org.openpnp.spi.base.AbstractMachine;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ScriptActuatorConfigurationWizard extends AbstractActuatorConfigurationWizard {
    private JPanel panelProperties;
    private JLabel lblName;
    private JTextField nameTf;
    private JLabel lblScriptName;
    private JTextField scriptNameTf;


    public ScriptActuatorConfigurationWizard(AbstractMachine machine, ScriptActuator actuator) {
        super(machine, actuator);
    }

    @Override 
    protected void createUi(AbstractMachine machine) {
        panelProperties = new JPanel();
        panelProperties.setBorder(new TitledBorder(null, I18n.gettext("Properties"), TitledBorder.LEADING, TitledBorder.TOP, null, null));
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

        lblName = new JLabel(I18n.gettext("Name"));
        panelProperties.add(lblName, "2, 2, right, default");

        nameTf = new JTextField();
        panelProperties.add(nameTf, "4, 2, fill, default");
        nameTf.setColumns(20);

        lblScriptName = new JLabel(I18n.gettext("Script Name"));
        panelProperties.add(lblScriptName, "2, 4, right, default");

        scriptNameTf = new JTextField();
        panelProperties.add(scriptNameTf, "4, 4, fill, default");
        scriptNameTf.setColumns(40);

        super.createUi(machine);
    }

    @Override
    public void createBindings() {
        super.createBindings();

        addWrappedBinding(actuator, "name", nameTf, "text");
        addWrappedBinding(actuator, "scriptName", scriptNameTf, "text");

        ComponentDecorators.decorateWithAutoSelect(nameTf);
        ComponentDecorators.decorateWithAutoSelect(scriptNameTf);
    }
}
