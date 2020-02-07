/*
 * Copyright (C) 2017 Sebastian Pichelhofer & Jason von Nieda <jason@vonnieda.org>
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

package org.openpnp.machine.reference.feeder;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.Action;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.ReferenceFeederGroupConfigurationWizard;
import org.openpnp.machine.reference.feeder.wizards.ReferenceRotatedTrayFeederConfigurationWizard;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractFeeder;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Feeder;

/**
 * Implementation of Feeder that can be the parent to other feeders.
 */
public class ReferenceFeederGroup extends ReferenceFeeder {
    //Limit on how many generations can exist (prevents infinite loops in the event someone manually edited machine.xml) 
    private final int maxGenerations = 32; 
    
    @Element(required=false)
    private Location expectedFiducial1 = new Location(LengthUnit.Millimeters);
    
    @Element(required=false)
    private Location expectedFiducial2 = new Location(LengthUnit.Millimeters);
    
    @ElementList(required=false)
    private ArrayList<String> childIds = new ArrayList<>();
    
	public ReferenceFeederGroup() {
	    super();
	    name = name + "_" + getId();
	    part = null;
	    partId = "";
	    rotationInFeeder = 0.0;
	}
	
    public void setExpectedFiducial1(Location expectedFiducial1) {
        this.expectedFiducial1 = expectedFiducial1;
    }
    
    public Location getExpectedFiducial1() {
        return expectedFiducial1;
    }
    
    public void setExpectedFiducial2(Location expectedFiducial2) {
        this.expectedFiducial2 = expectedFiducial2;
    }
    
    public Location getExpectedFiducial2() {
        return expectedFiducial2;
    }
    
    @Override
	public void addChild(String ChildId) {
	    childIds.add(ChildId);
	}
	
    @Override
	public void removeChild(String ChildId) {
	    childIds.remove(ChildId);
	}
	
    public ArrayList<String> getChildIds() {
        return childIds;
    }
    
    public boolean hasChildren() {
        return !childIds.isEmpty();
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        Object oldValue = this.enabled;
        this.enabled = enabled;
        firePropertyChange("enabled", oldValue, enabled);
        for (String childId : childIds) {
            ReferenceFeeder child = (ReferenceFeeder) Configuration.get().getMachine().getFeeder(childId);
            boolean temp = child.isLocallyEnabled();
            child.setEnabled(!temp);
            child.setEnabled(temp);
        }
    }

    @Override
	public boolean isPotentialParentOf(Feeder child) {
        if (expectedFiducial1.equals(expectedFiducial2)) {
            //The feeder group hasn't been setup so it can't have children
            return false;
        }
	    String childId = child.getId();
	    if (getId().equals(childId)) {
	        //A feeder can never be its own parent
	        return false;
	    }
        //Search the family tree of this feeder to ensure the child doesn't become its own ancestor
	    int generation = 0;
	    String p = parentId;
	    while (generation < maxGenerations) {
	        if (p.equals(ROOT_FEEDER_ID)) {
	            //Ok, the family tree traces back to the machine 
	            return true;
	        } else if (p.equals(childId)) {
	            //The family tree would form a loop so this is not a potential parent
	            return false;
	        }
	        p = ((ReferenceFeeder) Configuration.get().getMachine().getFeeder(p)).getParentId();
	        generation = generation + 1;
	    }
	    Logger.warn("Possible loop in feeder parentage detected, check ReferenceFeederGroups in machine.xml");
	    return false;
	}
	
	@Override
    public void preDeleteCleanUp() {
	    //Set the parent of all of this feeder group's children to the parent of this feeder group
	    @SuppressWarnings("unchecked")
        ArrayList<String> toReParent = (ArrayList<String>) childIds.clone();
	    for (String childId : toReParent) {
	        Logger.info(this.getName() + " owns " + childId);
	        ((ReferenceFeeder) Configuration.get().getMachine().getFeeder(childId)).setParentId(parentId);
	    }
	    super.preDeleteCleanUp();
	}
	
	@Override
	public String toString() {
		return getName();
	}

	@Override
	public Wizard getConfigurationWizard() {
		return new ReferenceFeederGroupConfigurationWizard(this);
	}

	@Override
	public String getPropertySheetHolderTitle() {
		return getClass().getSimpleName() + " " + getName();
	}

	@Override
	public PropertySheetHolder[] getChildPropertySheetHolders() {
		return null;
	}

	@Override
	public Action[] getPropertySheetHolderActions() {
		return null;
	}

    @Override
    public Location getPickLocation() throws Exception {
        throw new Exception("ReferenceFeederGroups don't have a pick location: " + getName());
    }

    @Override
    public void feed(Nozzle nozzle) throws Exception {
        throw new Exception("ReferenceFeederGroups can't feed: " + getName());
    }
}
