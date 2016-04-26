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

package org.openpnp.gui.support;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

import org.openpnp.model.Identifiable;

@SuppressWarnings("serial")
public class IdentifiableListCellRenderer<T extends Identifiable> extends DefaultListCellRenderer {
    IdentifiableObjectToStringConverter<T> converter = new IdentifiableObjectToStringConverter<>();

    @Override
    public Component getListCellRendererComponent(JList arg0, Object arg1, int arg2, boolean arg3,
            boolean arg4) {
        Component component = super.getListCellRendererComponent(arg0, arg1, arg2, arg3, arg4);
        ((JLabel) component).setText(converter.getPreferredStringForItem(arg1));
        return component;
    }

}
