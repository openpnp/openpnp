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

import javax.swing.Action;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.ReferenceFeederGroupConfigurationWizard;
import org.openpnp.machine.reference.feeder.wizards.ReferenceRotatedTrayFeederConfigurationWizard;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Feeder;

/**
 * Implementation of Feeder that indexes based on an offset. This allows a tray
 * of parts to be picked from without moving any tape. Can handle trays of
 * arbitrary X and Y count.
 */
public class ReferenceFeederGroup extends ReferenceFeeder {

	@Attribute
	private int trayCountCols = 1;
	@Attribute
	private int trayCountRows = 1;
	@Element
	private Location offsets = new Location(LengthUnit.Millimeters);
	@Attribute
	private int feedCount = 0;
	@Attribute
	private double trayRotation = 0;
	@Element
	protected Location lastComponentLocation = new Location(LengthUnit.Millimeters);
	@Element
	protected Location firstRowLastComponentLocation = new Location(LengthUnit.Millimeters);

	private Location pickLocation;
	private ArrayList<Feeder> children = new ArrayList<>();
	
	public ReferenceFeederGroup() {
	    super();
	    name = name + "_" + getId();
	    part = null;
	    partId = "";
	}
	
	public boolean addChild(Feeder feeder) {
	    return children.add(feeder);
	}
	
	public boolean removeChild(Feeder feeder) {
	    return children.remove(feeder);
	}
	
    @Override
    public void setEnabled(boolean enabled) {
        Object oldValue = this.enabled;
        this.enabled = enabled;
        firePropertyChange("enabled", oldValue, enabled);
        for (Feeder fdr : children) {
            boolean temp = fdr.isLocallyEnabled();
            fdr.setEnabled(!temp);
            fdr.setEnabled(temp);
        }
    }

	public boolean isPotentialOwnerOf(Feeder feeder) {
	    String feederToBeOwned = feeder.getName();
	    if (getName().equals(feederToBeOwned)) {
	        //A feeder can never own itself
	        return false;
	    }
	    int maxDepth = 16; //An arbitrary limit just to prevent possible infinite loops
	    int depth = 0;
	    String onr = owner;
	    while (depth < maxDepth) {
	        if (onr.equals("Machine")) {
	            //Ok, the ownership traces back to the machine 
	            return true;
	        } else if (onr.equals(feederToBeOwned)) {
	            //The ownership would form a loop so this is not a potential owner
	            return false;
	        }
	        onr = Configuration.get().getMachine().getFeederByName(onr).getOwner();
	        depth = depth + 1;
	    }
	    Logger.warn("Possible loop in feeder ownership detected, check ReferenceFeederGroups in machine.xml");
	    return false;
	}
	
	
	@Override
	public Location getPickLocation() throws Exception {
		if (pickLocation == null) {
			pickLocation = location;
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

		double delta_x1 = partX * offsets.getX() * Math.cos(Math.toRadians(trayRotation));
		double delta_y1 = Math.sqrt((partX * offsets.getX() * partX * offsets.getX()) - (delta_x1 * delta_x1));
		Location delta1 = new Location(LengthUnit.Millimeters, delta_x1, delta_y1, 0, 0);

		double delta_y2 = partY * offsets.getY() * Math.cos(Math.toRadians(trayRotation)) * -1;
		double delta_x2 = Math.sqrt((partY * offsets.getY() * partY * offsets.getY()) - (delta_y2 * delta_y2));
		Location delta2 = new Location(LengthUnit.Millimeters, delta_x2, delta_y2, 0, 0);

		pickLocation = location.add(delta1.add(delta2));
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
		return lastComponentLocation;
	}

	public void setLastComponentLocation(Location LastComponentLocation) {
		this.lastComponentLocation = LastComponentLocation;
	}

	public Location getFirstRowLastComponentLocation() {
		return this.firstRowLastComponentLocation;
	}

	public void setFirstRowLastComponentLocation(Location FirstRowLastComponentLocation) {
		this.firstRowLastComponentLocation = FirstRowLastComponentLocation;
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
}
