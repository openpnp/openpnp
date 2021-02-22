/*
 * Copyright (C) 2017 Sebastian Pichelhofer <sp@apertus.org> based on reference by Jason von Nieda
 * <jason@vonnieda.org>
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

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.machine.reference.HttpActuator;
import org.openpnp.spi.base.AbstractMachine;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class HttpActuatorConfigurationWizard extends AbstractActuatorConfigurationWizard {
    private JPanel panelProperties;
    private JLabel lblName;
    private JTextField nameTf;
    private JLabel lblOnUrl;
    private JTextField onUrlTf;
    private JLabel lblOffUrl;
    private JTextField offUrlTf;
    private JLabel lblParametricUrl;
    private JTextField paramUrl;

    public HttpActuatorConfigurationWizard(AbstractMachine machine, HttpActuator httpActuator) {
        super(machine, httpActuator);
    }

    @Override 
    protected void createUi(AbstractMachine machine) {
        panelProperties = new JPanel();
        panelProperties.setBorder(new TitledBorder(null, "Properties", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelProperties);
        panelProperties.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
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
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblName = new JLabel("Name");
        panelProperties.add(lblName, "2, 2, right, default");

        nameTf = new JTextField();
        panelProperties.add(nameTf, "4, 2");
        nameTf.setColumns(20);

        lblOnUrl = new JLabel("On URL");
        panelProperties.add(lblOnUrl, "2, 4, right, default");

        onUrlTf = new JTextField();
        panelProperties.add(onUrlTf, "4, 4, fill, default");
        onUrlTf.setColumns(40);

        lblOffUrl = new JLabel("Off URL");
        panelProperties.add(lblOffUrl, "2, 6, right, default");

        offUrlTf = new JTextField();
        panelProperties.add(offUrlTf, "4, 6, fill, default");
        offUrlTf.setColumns(40);

        lblParametricUrl = new JLabel("Parametric URL");
        lblParametricUrl.setToolTipText("<html>\r\nUse a parametric template to encode non-boolean actuation values into the URL.<br/>\r\nThe {val} placeholder can be used. <br/>\r\nFormatting is possible in the form of {val:%f} etc.<br/>\r\n<br/>\r\n<strong>Note:</strong> no escaping is performed. If using String actuation values, <br/>\r\nyou can use complex URI fragments e.g. drive multiple parameters. \r\n</html>");
        panelProperties.add(lblParametricUrl, "2, 8, right, default");

        paramUrl = new JTextField();
        panelProperties.add(paramUrl, "4, 8, fill, default");
        paramUrl.setColumns(40);

        lblReadUrl = new JLabel("Read URL");
        panelProperties.add(lblReadUrl, "2, 8, right, default");

        readUrlTf = new JTextField();
        panelProperties.add(readUrlTf, "4, 8, fill, default");
        readUrlTf.setColumns(40);


        super.createUi(machine);
    }

    @Override
    public void createBindings() {
        super.createBindings();
        addWrappedBinding(actuator, "name", nameTf, "text");
        addWrappedBinding(actuator, "onUrl", onUrlTf, "text");
        addWrappedBinding(actuator, "offUrl", offUrlTf, "text");
        addWrappedBinding(actuator, "paramUrl", paramUrl, "text");
        addWrappedBinding(actuator, "readUrl", readUrlTf, "text");

        ComponentDecorators.decorateWithAutoSelect(nameTf);
        ComponentDecorators.decorateWithAutoSelect(onUrlTf);
        ComponentDecorators.decorateWithAutoSelect(offUrlTf);
        ComponentDecorators.decorateWithAutoSelect(paramUrl);
        ComponentDecorators.decorateWithAutoSelect(readUrlTf);

        super.createBindings();
    }

    protected void adaptDialog() {
        super.adaptDialog();
        boolean isBoolean = (valueType.getSelectedItem() == ActuatorValueType.Boolean);
        lblOnUrl.setVisible(isBoolean);
        onUrlTf.setVisible(isBoolean);
        lblOffUrl.setVisible(isBoolean);
        offUrlTf.setVisible(isBoolean);
        lblParametricUrl.setVisible(!isBoolean);
        paramUrl.setVisible(!isBoolean);
        lblReadUrl.setVisible(!isBoolean);
        readUrlTf.setVisible(!isBoolean);
    }
}
