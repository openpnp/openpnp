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

import org.openpnp.model.Configuration;
import org.openpnp.spi.Pipeline;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class PipelinesComboBoxModel extends DefaultComboBoxModel<Pipeline> implements PropertyChangeListener {

    private IdentifiableComparator<Pipeline> comparator = new IdentifiableComparator<>();

    public PipelinesComboBoxModel() {
        addAllElements();
        Configuration.get().addPropertyChangeListener("pipelines", this);
    }

    private void addAllElements() {
        ArrayList<Pipeline> pipelines = new ArrayList<>(Configuration.get().getMachine().getPipelines());
        pipelines.sort(comparator);
        for (Pipeline pipeline : pipelines) {
            addElement(pipeline);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        removeAllElements();
        addAllElements();
    }
}
