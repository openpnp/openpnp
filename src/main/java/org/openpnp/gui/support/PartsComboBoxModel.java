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
import org.openpnp.util.Collect;

@SuppressWarnings("serial")
public class PartsComboBoxModel extends DefaultComboBoxModel implements PropertyChangeListener {
    private IdentifiableComparator<Part> comparator = new IdentifiableComparator<>();
    private List<Part> partsList;
    
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
        partsList = new ArrayList<>(Configuration.get().getParts());
        Collections.sort(partsList, comparator);
        for (Part part : partsList) {
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
        
        //Compute the indices to remove and those to add to modify the current list
        List<int[]> indicesToRemove = new ArrayList<>();
        List<int[]> indicesToAdd = new ArrayList<>();
        Collect.computeInPlaceUpdateIndices(partsList, parts, indicesToRemove, indicesToAdd);
        
        //Remove any parts from the existing combo box that are not in the new list of parts. The
        //current combo box selection will not change as long as it is not one of the parts being
        //removed.
        for (int[] range : indicesToRemove) {
            for (int idx=range[0]; idx>=range[1]; idx--) { //Reverse order indexing!
                removeElementAt(idx);
            }
        }
        
        //Insert the new parts into the existing combo box at their correct positions
        for (int[] range : indicesToAdd) {
            for (int idx=range[0]; idx<=range[1]; idx++) {
                insertElementAt(parts.get(idx), idx);
            }
        }
        
        //Save the new parts list for the next time around
        partsList = parts;
    }
}
