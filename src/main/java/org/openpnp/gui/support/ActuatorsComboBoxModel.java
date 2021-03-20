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
    private Machine actuatorsMachine;
    private Head actuatorsHead;
    private Class<?> type;
    
    private void init() {
        addAllElements();
        if (actuatorsMachine != null) {
            ((AbstractModelObject) actuatorsMachine).addPropertyChangeListener("actuators", this);
        }
        if (actuatorsHead != null) {
            ((AbstractModelObject) actuatorsHead).addPropertyChangeListener("actuators", this);
        }
    }

    public static Class<?> getType(Object o) {
        return o != null? o.getClass(): null;
    }

    public ActuatorsComboBoxModel(Object actuatorsBase, Class<?> type) {
        if (actuatorsBase instanceof Head) {
            this.actuatorsHead = (Head)actuatorsBase;
        }
        else if (actuatorsBase instanceof Machine) {
            this.actuatorsMachine = (Machine)actuatorsBase;
        }
        this.type = type;
        init();
    }
    public ActuatorsComboBoxModel(Head actuatorsHead, Class<?> type) {
        this.actuatorsHead = actuatorsHead;
        this.type = type;
        init();
    }
    public ActuatorsComboBoxModel(Machine actuatorsMachine, Class<?> type) {
        this.actuatorsMachine = actuatorsMachine;
        this.type = type;
        init();
    }
    public ActuatorsComboBoxModel(Machine actuatorsMachine, Head actuatorsHead, Class<?> type) {
        this.actuatorsMachine = actuatorsMachine;
        this.actuatorsHead = actuatorsHead;
        this.type = type;
        init();
    }

    private void addAllElements() {
        ArrayList<Actuator> actuators = new ArrayList<>();
        if (actuatorsMachine != null) {
            actuators.addAll(actuatorsMachine.getActuators());
        }
        if (actuatorsHead != null) {
            actuators.addAll(actuatorsHead.getActuators());
        }
        Collections.sort(actuators, comparator);
        addElement(new String());
        for (Actuator actuator : actuators) {
            if (type == null || type.isAssignableFrom(actuator.getValueClass())) {
                addElement(actuator.getName());
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        removeAllElements();
        addAllElements();
    }
}
