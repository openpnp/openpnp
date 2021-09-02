/*
 * Copyright (C) 2020 <mark@makr.zone>
 * inspired and based on work
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

import org.openpnp.model.Configuration;
import org.openpnp.spi.Axis;
import org.openpnp.spi.base.AbstractMachine;

@SuppressWarnings({"serial", "rawtypes"})
public class AxesComboBoxModel extends DefaultComboBoxModel implements PropertyChangeListener {
    private Comparator<Axis> comparator = new Comparator<Axis>() {
        @Override
        public int compare(Axis o1, Axis o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };
    final private boolean addEmpty;
    final private Class<? extends Axis> types;
    final private AbstractMachine machine; 
    private Axis.Type axisType; 

    public AxesComboBoxModel(AbstractMachine machine, Class<? extends Axis> types, Axis.Type axisType, boolean addEmpty) {
        this.machine = machine;
        this.addEmpty = addEmpty;
        this.types = types;
        this.axisType = axisType;
        if (machine != null) { // we're not in Window Builder Design Mode
            addAllElements();
            machine.addPropertyChangeListener("axes", this);
        }
    }

    private void addAllElements() {
        if (machine != null) { // we're not in Window Builder Design Mode
            ArrayList<Axis> axes = null;
            axes = new ArrayList<>(machine.getAxes());
            Collections.sort(axes, comparator);
            for (Axis axis : axes) {
                if (types.isInstance(axis)) {
                    if (axisType == null || axisType == axis.getType()) {
                        addElement(axis.getName());
                    }
                }
            }
            if (addEmpty) {
                addElement(new String());
            }
        }
    }

    public Axis.Type getAxisType() {
        return axisType;
    }

    public void setAxisType(Axis.Type axisType) {
        this.axisType = axisType;
        removeAllElements();
        addAllElements();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        removeAllElements();
        addAllElements();
    }
}
