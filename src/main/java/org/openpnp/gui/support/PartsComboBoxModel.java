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
import java.util.HashMap;
import java.util.List;

import javax.swing.DefaultComboBoxModel;

import org.openpnp.model.Configuration;
import org.openpnp.model.Part;

@SuppressWarnings("serial")
public class PartsComboBoxModel extends DefaultComboBoxModel implements PropertyChangeListener {
    private IdentifiableComparator<Part> comparator = new IdentifiableComparator<>();

    public PartsComboBoxModel() {
        addAllElements();
        Configuration.get().addPropertyChangeListener("parts", this);
    }

    /**
     * Call this method when done to cleanup 
     */
    public void dispose() {
        Configuration.get().removePropertyChangeListener("parts", this);
    }
    
    @SuppressWarnings("unchecked")
    private void addAllElements() {
        ArrayList<Part> parts = new ArrayList<>(Configuration.get().getParts());
        Collections.sort(parts, comparator);
        for (Part part : parts) {
            addElement(part);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        //Create a sorted list of parts that the parts combo box should have
        HashMap<String, Part> newParts = (HashMap<String, Part>) evt.getNewValue();
        List<Part> parts = new ArrayList<Part>(newParts.values());
        Collections.sort(parts, comparator);
        
        //Remove any parts from the existing combo box that are not in the new list of parts
        int idx = 0;
        while (idx < getSize()) {
            if (!parts.contains(getElementAt(idx))) {
                removeElementAt(idx);
            }
            else {
                idx++;
            }
        }
        
        //Insert the new parts into the existing combo box at their correct positions
        idx = 0;
        for (Part part : parts) {
            if (this.getSize() == 0) {
                insertElementAt(part, idx);
                continue;
            }
            int cmp = comparator.compare(part, (Part) getElementAt(idx));
            if (cmp < 0) {
                insertElementAt(part, idx);
                idx++;
                continue;
            }
            else if (cmp > 0) {
                while (idx < this.getSize() && comparator.compare(part, (Part) getElementAt(idx)) > 0) {
                    idx++;
                }
                if (idx == this.getSize() || comparator.compare(part, (Part) getElementAt(idx)) < 0) {
                    insertElementAt(part, idx);
                }
            }
        }
    }
}
