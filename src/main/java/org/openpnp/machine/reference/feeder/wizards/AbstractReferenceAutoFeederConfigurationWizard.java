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

package org.openpnp.machine.reference.feeder.wizards;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.*;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

@SuppressWarnings("serial")
/**
 * TODO: This should become it's own property sheet which the feeders can include.
 */
public abstract class AbstractReferenceAutoFeederConfigurationWizard
        extends AbstractConfigurationWizard {
    private final ReferenceFeeder feeder;

    private JPanel panelLocation;
    private JLabel lblX_1;
    private JLabel lblY_1;
    private JLabel lblZ;
    private JLabel lblRotation;
    private JTextField textFieldLocationX;
    private JTextField textFieldLocationY;
    private JTextField textFieldLocationZ;
    private JTextField textFieldLocationC;
    private JPanel panelPart;

    private JComboBox comboBoxPart;
    private LocationButtonsPanel locationButtonsPanel;
    private JTextField retryCountTf;

    /**
     * @wbp.parser.constructor
     */

    public AbstractReferenceAutoFeederConfigurationWizard(ReferenceFeeder feeder) {
        this.feeder = feeder;

        panelPart = new JPanel();
        panelPart.setBorder(
                new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "General Settings", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(panelPart);
        panelPart.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        comboBoxPart = new JComboBox();
        try {
            comboBoxPart.setModel(new PartsComboBoxModel());
        }
        catch (Throwable t) {
            // Swallow this error. This happens during parsing in
            // in WindowBuilder but doesn't happen during normal run.
        }
        
        JLabel lblPart = new JLabel("Part");
        panelPart.add(lblPart, "2, 2, right, default");
        comboBoxPart.setRenderer(new IdentifiableListCellRenderer<Part>());
        panelPart.add(comboBoxPart, "4, 2, left, default");
        
        JLabel lblRetryCount = new JLabel("Retry Count");
        panelPart.add(lblRetryCount, "2, 4, right, default");
        
        retryCountTf = new JTextField();
        retryCountTf.setText("3");
        panelPart.add(retryCountTf, "4, 4");
        retryCountTf.setColumns(3);
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        addWrappedBinding(feeder, "part", comboBoxPart, "selectedItem");
        addWrappedBinding(feeder, "retryCount", retryCountTf, "text", intConverter);

        ComponentDecorators.decorateWithAutoSelect(retryCountTf);
    }
}
