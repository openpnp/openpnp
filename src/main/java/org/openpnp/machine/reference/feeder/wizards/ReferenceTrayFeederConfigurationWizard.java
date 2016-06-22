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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.feeder.ReferenceTrayFeeder;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceTrayFeederConfigurationWizard
        extends AbstractReferenceFeederConfigurationWizard {
    private final ReferenceTrayFeeder feeder;

    private JTextField textFieldOffsetsX;
    private JTextField textFieldOffsetsY;
    private JTextField textFieldTrayCountX;
    private JTextField textFieldTrayCountY;
    private JTextField textFieldFeedCount;

    public ReferenceTrayFeederConfigurationWizard(ReferenceTrayFeeder feeder) {
        super(feeder);
        this.feeder = feeder;

        JPanel panelFields = new JPanel();

        panelFields.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblX = new JLabel("X");
        panelFields.add(lblX, "4, 2");

        JLabel lblY = new JLabel("Y");
        panelFields.add(lblY, "6, 2");

        JLabel lblFeedStartLocation = new JLabel("Offsets");
        panelFields.add(lblFeedStartLocation, "2, 4, right, default");

        textFieldOffsetsX = new JTextField();
        panelFields.add(textFieldOffsetsX, "4, 4, fill, default");
        textFieldOffsetsX.setColumns(10);

        textFieldOffsetsY = new JTextField();
        panelFields.add(textFieldOffsetsY, "6, 4, fill, default");
        textFieldOffsetsY.setColumns(10);

        JLabel lblTrayCount = new JLabel("Tray Count");
        panelFields.add(lblTrayCount, "2, 6, right, default");

        textFieldTrayCountX = new JTextField();
        panelFields.add(textFieldTrayCountX, "4, 6, fill, default");
        textFieldTrayCountX.setColumns(10);

        textFieldTrayCountY = new JTextField();
        panelFields.add(textFieldTrayCountY, "6, 6, fill, default");
        textFieldTrayCountY.setColumns(10);

        JSeparator separator = new JSeparator();
        panelFields.add(separator, "4, 8, 3, 1");

        JLabel lblFeedCount = new JLabel("Feed Count");
        panelFields.add(lblFeedCount, "2, 10, right, default");

        textFieldFeedCount = new JTextField();
        panelFields.add(textFieldFeedCount, "4, 10, fill, default");
        textFieldFeedCount.setColumns(10);

        contentPanel.add(panelFields);
    }

    @Override
    public void createBindings() {
        super.createBindings();
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter integerConverter = new IntegerConverter();


        MutableLocationProxy offsets = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "offsets", offsets, "location");
        addWrappedBinding(offsets, "lengthX", textFieldOffsetsX, "text", lengthConverter);
        addWrappedBinding(offsets, "lengthY", textFieldOffsetsY, "text", lengthConverter);

        addWrappedBinding(feeder, "trayCountX", textFieldTrayCountX, "text", integerConverter);
        addWrappedBinding(feeder, "trayCountY", textFieldTrayCountY, "text", integerConverter);

        addWrappedBinding(feeder, "feedCount", textFieldFeedCount, "text", integerConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffsetsX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffsetsY);

        ComponentDecorators.decorateWithAutoSelect(textFieldTrayCountX);
        ComponentDecorators.decorateWithAutoSelect(textFieldTrayCountY);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedCount);
    }

    @Override
    protected void saveToModel() {
        super.saveToModel();
        if (feeder.getOffsets().getX() == 0 && feeder.getTrayCountX() > 1) {
            MessageBoxes.errorBox(this, "Error",
                    "X offset must be greater than 0 if X tray count is greater than 1 or feed failure will occur.");
        }
        if (feeder.getOffsets().getY() == 0 && feeder.getTrayCountY() > 1) {
            MessageBoxes.errorBox(this, "Error",
                    "Y offset must be greater than 0 if Y tray count is greater than 1 or feed failure will occur.");
        }
    }
}
