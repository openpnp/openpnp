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
import java.util.Comparator;

import javax.swing.DefaultComboBoxModel;

import org.openpnp.model.AbstractModelObject;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;

@SuppressWarnings("serial")
public class ActuatorsComboBoxModel extends DefaultComboBoxModel implements PropertyChangeListener {
    private Comparator<Actuator> comparator = new Comparator<Actuator>() {
        @Override
        public int compare(Actuator o1, Actuator o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };
    private AbstractModelObject actuatorsBase;
    
    public ActuatorsComboBoxModel(Object object) {
        this.actuatorsBase = (AbstractModelObject)object;
        addAllElements();
        this.actuatorsBase.addPropertyChangeListener("actuators", this);
    }

    private void addAllElements() {
        ArrayList<Actuator> actuators = null;
        if (actuatorsBase instanceof Machine) {
             actuators = new ArrayList<>(((Machine)actuatorsBase).getActuators());
        }
        if (actuatorsBase instanceof Head) {
            actuators = new ArrayList<>(((Head)actuatorsBase).getActuators());
        }
        Collections.sort(actuators, comparator);
        for (Actuator actuator : actuators) {
            addElement(actuator.getName());
        }
        addElement(new String());
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        removeAllElements();
        addAllElements();
    }
}
