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

package org.openpnp.gui.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Locale;

import javax.swing.JTextField;

import org.openpnp.model.Configuration;
import org.openpnp.model.Length;

public class ComponentDecorators {
    /**
     * Adds an auto selection decoration to the JTextField. Whenever the JTextField gains focus the
     * text in it will be selected.
     * 
     * @param textField
     */
    public static void decorateWithAutoSelect(JTextField textField) {
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                ((JTextField) event.getComponent()).selectAll();
            }
        });
        textField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                ((JTextField) event.getSource()).selectAll();
            }
        });
    }

    /**
     * Adds a length conversion decoration to the JTextField. When the JTextField loses focus or has
     * it's action triggered the text will be converted to a Length value in the system units and
     * then have it's text replaced with the value.
     * 
     * @param textField
     */
    public static void decorateWithLengthConversion(JTextField textField) {
        textField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                convertLength(((JTextField) event.getSource()));
            }
        });
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                convertLength(((JTextField) event.getSource()));
            }
        });
    }

    public static void decorateWithAutoSelectAndLengthConversion(JTextField textField) {
        decorateWithAutoSelect(textField);
        decorateWithLengthConversion(textField);
    }

    private static void convertLength(JTextField textField) {
        Length length = Length.parse(textField.getText(), false);
        if (length == null) {
            return;
        }
        if (length.getUnits() == null) {
            length.setUnits(Configuration.get().getSystemUnits());
        }
        length = length.convertToUnits(Configuration.get().getSystemUnits());
        textField.setText(String.format(Locale.US, Configuration.get().getLengthDisplayFormat(),
                length.getValue()));
    }
}
