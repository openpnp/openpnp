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

import org.openpnp.machine.reference.feeder.ReferenceFeederGroup;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;

@SuppressWarnings("serial")
public class FeederOwnersComboBoxModel extends DefaultComboBoxModel implements PropertyChangeListener {
    private IdentifiableComparator<Feeder> comparator = new IdentifiableComparator<>();
    private Feeder feeder;
    
    public FeederOwnersComboBoxModel(Feeder feeder) {
        this.feeder = feeder;
        addAllElements();
        Configuration.get().addPropertyChangeListener("feeders", this);
    }

    private void addAllElements() {
        ArrayList<Feeder> feeders = new ArrayList<>(Configuration.get().getMachine().getFeeders());
        Collections.sort(feeders, comparator);
        addElement("Machine");
        for (Feeder fdr : feeders) {
            if ((fdr.getClass() == ReferenceFeederGroup.class) && (((ReferenceFeederGroup)fdr).isPotentialOwnerOf(feeder))) {
                addElement(fdr.getName());
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        removeAllElements();
        addAllElements();
    }
}
