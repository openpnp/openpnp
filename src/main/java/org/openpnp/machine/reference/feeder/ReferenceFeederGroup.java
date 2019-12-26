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
 * Implementation of Feeder that holds other feeders.
 */
public class ReferenceFeederGroup extends ReferenceFeeder {

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
    
	public boolean addChild(String ChildId) {
	    //Configuration.get().getMachine().getFeeder(ChildId).setParentId(getId());
	    return childIds.add(ChildId);
	}
	
	public boolean removeChild(String ChildId) {
	    //Configuration.get().getMachine().getFeeder(ChildId).setParentId(parentId);
	    //if (parentId != AbstractFeeder.ROOT_FEEDER_ID) {
	    //    ((ReferenceFeederGroup) Configuration.get().getMachine().getFeeder(parentId)).addChild(ChildId);
	    //}
	    return childIds.remove(ChildId);
	}
	
	public ArrayList<String> getChildIds() {
	    return childIds;
	}
	
	@Override
	public void setParentId(String parentId) {
	    //Location oldLastComponentLocation = getLastComponentLocation();
	    //Location oldFirstRowLastComponentLocation = getFirstRowLastComponentLocation();
	    super.setParentId(parentId);
	    //setLastComponentLocation(oldLastComponentLocation);
	    //setFirstRowLastComponentLocation(oldFirstRowLastComponentLocation);
	}
	
    @Override
    public void setEnabled(boolean enabled) {
        Object oldValue = this.enabled;
        this.enabled = enabled;
        firePropertyChange("enabled", oldValue, enabled);
        for (String childId : childIds) {
            Feeder child = Configuration.get().getMachine().getFeeder(childId);
            boolean temp = child.isLocallyEnabled();
            child.setEnabled(!temp);
            child.setEnabled(temp);
        }
    }

    @Override
	public boolean isPotentialParentOf(Feeder feeder) {
	    String childId = feeder.getId();
	    if (getId().equals(childId)) {
	        //A feeder can never be its own parent
	        return false;
	    }
	    int maxDepth = 16; //An arbitrary limit just to prevent possible infinite loops
	    int depth = 0;
	    String p = parentId;
	    while (depth < maxDepth) {
	        if (p.equals(AbstractFeeder.ROOT_FEEDER_ID)) {
	            //Ok, the family tree traces back to the machine 
	            return true;
	        } else if (p.equals(childId)) {
	            //The family tree would form a loop so this is not a potential parent
	            return false;
	        }
	        p = Configuration.get().getMachine().getFeeder(p).getParentId();
	        depth = depth + 1;
	    }
	    Logger.warn("Possible loop in feeder parentage detected, check ReferenceFeederGroups in machine.xml");
	    return false;
	}
	
	@Override
    public void preDeleteCleanUp() {
	    ArrayList<String> toReParent = (ArrayList<String>) childIds.clone();
	    for (String childId : toReParent) {
	        Logger.info(this.getName() + " owns " + childId);
	        Configuration.get().getMachine().getFeeder(childId).setParentId(parentId);
	    }
	    super.preDeleteCleanUp();
	}
	
	
/*	@Override
	public Location getPickLocation() throws Exception {
		if (pickLocation == null) {
			pickLocation = getLocation();
		}
		int partX, partY;

		if (feedCount >= (trayCountCols * trayCountRows)) {
			throw new Exception("Tray empty.");
		}

		if (trayCountCols >= trayCountRows) {
			// X major axis.
			partX = feedCount / trayCountRows;
			partY = feedCount % trayCountRows;
		} else {
			// Y major axis.
			partX = feedCount % trayCountCols;
			partY = feedCount / trayCountCols;
		}

		calculatePickLocation(partX, partY);
	
		Logger.debug("{}.getPickLocation => {}", getName(), pickLocation);
		
		return pickLocation;
	}

	private void calculatePickLocation(int partX, int partY) throws Exception {

		// Multiply the offsets by the X/Y part indexes to get the total offsets
		// and then add the pickLocation to offset the final value.
		// and then add them to the location to get the final pickLocation.
		// pickLocation = location.add(offsets.multiply(partX, partY, 0.0,
		// 0.0));
	    
	    Location globalOffsets = getOffsets();
		double delta_x1 = partX * globalOffsets.getX() * Math.cos(Math.toRadians(trayRotation));
		double delta_y1 = Math.sqrt((partX * globalOffsets.getX() * partX * globalOffsets.getX()) - (delta_x1 * delta_x1));
		Location delta1 = new Location(LengthUnit.Millimeters, delta_x1, delta_y1, 0, 0);

		double delta_y2 = partY * globalOffsets.getY() * Math.cos(Math.toRadians(trayRotation)) * -1;
		double delta_x2 = Math.sqrt((partY * globalOffsets.getY() * partY * globalOffsets.getY()) - (delta_y2 * delta_y2));
		Location delta2 = new Location(LengthUnit.Millimeters, delta_x2, delta_y2, 0, 0);

		pickLocation = getLocation().add(delta1.add(delta2));
	}

	public void feed(Nozzle nozzle) throws Exception {
		Logger.debug("{}.feed({})", getName(), nozzle);

		int partX, partY;

		if (feedCount >= (trayCountCols * trayCountRows)) {
			throw new Exception("Tray empty.");
		}

		if (trayCountCols >= trayCountRows) {
			// X major axis.
			partX = feedCount / trayCountRows;
			partY = feedCount % trayCountRows;
		} else {
			// Y major axis.
			partX = feedCount % trayCountCols;
			partY = feedCount / trayCountCols;
		}

		calculatePickLocation(partX, partY);

		Logger.debug(String.format("Feeding part # %d, x %d, y %d, xPos %f, yPos %f, rPos %f", feedCount, partX, partY,
				pickLocation.getX(), pickLocation.getY(), pickLocation.getRotation()));

		setFeedCount(getFeedCount() + 1);
	}

	public int getTrayCountCols() {
		return trayCountCols;
	}

	public void setTrayCountCols(int trayCountCols) {
		this.trayCountCols = trayCountCols;
	}

	public int getTrayCountRows() {
		return trayCountRows;
	}

	public void setTrayCountRows(int trayCountRows) {
		this.trayCountRows = trayCountRows;
	}

	public Location getLastComponentLocation() {
		return convertToGlobalLocation(lastComponentLocation);
	}

	public void setLastComponentLocation(Location LastComponentLocation) {
		this.lastComponentLocation = convertToLocalLocation(LastComponentLocation);
	}

	public Location getFirstRowLastComponentLocation() {
		return convertToGlobalLocation(firstRowLastComponentLocation);
	}

	public void setFirstRowLastComponentLocation(Location FirstRowLastComponentLocation) {
		this.firstRowLastComponentLocation = convertToLocalLocation(FirstRowLastComponentLocation);
	}

	public Location getOffsets() {
		return offsets;
	}

	public void setOffsets(Location offsets) {
		this.offsets = offsets;
	}

	public double getTrayRotation() {
		return trayRotation;
	}

	public void setTrayRotation(double trayrotation) {
		this.trayRotation = trayrotation;
	}

	public int getFeedCount() {
		return feedCount;
	}

	public void setFeedCount(int feedCount) {
		int oldValue = this.feedCount;
		this.feedCount = feedCount;
		firePropertyChange("feedCount", oldValue, feedCount);
	}
*/
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void feed(Nozzle nozzle) throws Exception {
        // TODO Auto-generated method stub
        
    }
}
