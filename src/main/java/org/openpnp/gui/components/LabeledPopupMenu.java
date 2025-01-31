/*
 * Copyright (C) 2022 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
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

import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

/**
 * A popup menu with a label at the top
 */
@SuppressWarnings("serial")
public class LabeledPopupMenu extends JPopupMenu {
    private String originalLabelText = null;
    private final JLabel label;

    public LabeledPopupMenu() {
        super();
        this.label = null;
    }

    public LabeledPopupMenu(String label) {
        super();
        originalLabelText = label;
        this.label = new JLabel("<html><b>" + label + "</b></html>");
        this.label.setHorizontalAlignment(SwingConstants.CENTER);
        add(this.label);
        addSeparator();
    }

    @Override 
    public void setLabel(String text) {
        if (null == label) {
            return;
        }
        originalLabelText = text;
        label.setText("<html><b>" + text + "</b></html>");
    }

    @Override 
    public String getLabel() {
        return originalLabelText;
    }
}
