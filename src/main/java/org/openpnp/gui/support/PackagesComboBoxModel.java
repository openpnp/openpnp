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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.DefaultComboBoxModel;

import org.openpnp.model.Configuration;

@SuppressWarnings("serial")
public class PackagesComboBoxModel extends DefaultComboBoxModel implements PropertyChangeListener {
    private IdentifiableComparator<org.openpnp.model.Package> comparator =
            new IdentifiableComparator<>();

    public PackagesComboBoxModel() {
        addAllElements();
        Configuration.get().addPropertyChangeListener("packages", this);
    }

    private void addAllElements() {
        ArrayList<org.openpnp.model.Package> packages =
                new ArrayList<>(Configuration.get().getPackages());
        Collections.sort(packages, comparator);
        for (org.openpnp.model.Package pkg : packages) {
            addElement(pkg);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        removeAllElements();
        addAllElements();
    }
}
