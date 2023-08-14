/*
 * Copyright (C) 2017 Sebastian Pichelhofer & Jason von Nieda <jason@vonnieda.org>
 * Rotation Matrix corrections by Martin Gyurkó <nospam@gyurma.de> in 2021
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

import javax.swing.Action;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.ReferenceRotatedTrayFeederConfigurationWizard;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

/**
 * Implementation of Feeder that indexes based on an offset. This allows a tray
 * of parts to be picked from without moving any tape. Can handle trays of
 * arbitrary X and Y count.
 */
public class ReferenceRotatedTrayFeeder extends ReferenceFeeder {

	@Attribute
	private int trayCountCols = 1;
	@Attribute
	private int trayCountRows = 1;
	@Element
	private Location offsets = new Location(LengthUnit.Millimeters);
	@Attribute
	private int feedCount = 0;
	
	@Attribute(required=false)
	@Deprecated
	private Double trayRotation = null;
	
	@Attribute(required=false)
	private double componentRotationInTray = 0;
	
	@Element
	protected Location lastComponentLocation = new Location(LengthUnit.Millimeters);
	@Element
	protected Location firstRowLastComponentLocation = new Location(LengthUnit.Millimeters);

	private Location pickLocation;

	@Commit
	public void commit() {
	    if (trayRotation != null) {
	        //In previous versions, the location held the pick rotation and trayRotation held
	        //the actual rotation of the tray. In this version, the location holds the actual
	        //rotation of the tray and componentRotationInTray holds the rotation of the component
	        //relative to the tray. Note, in almost all cases, componentRotationInTray will be one
	        //of 0, or +/-90, or +/-180. And with this version, pick rotation = 
	        //location.getRotation() + componentRotationInTray
	        
	        //Convert from the old version to the new version
	        componentRotationInTray = location.getRotation() - trayRotation;
	        location = location.derive(null, null, null, trayRotation);
	        
	        //Remove the deprecated attribute
	        trayRotation = null;
	    }
	}
	
	@Override
	public Location getPickLocation() throws Exception {
		if (pickLocation == null || feedCount == 0) {
		    //Normally the pick location is only valid after a feed operation. But in the case of no
		    //prior feed operation (feed count is zero), we set the pick location to the first part
		    //(which is also where it will be after the first feed operation).
	        int partX, partY;
	        int fc = feedCount;
	        if (pickLocation == null && feedCount > 0) {
	            //Since the pick location is null, the machine configuration must have just been
	            //loaded. And since the feed count is greater than 0, this feeder must have already
	            //performed a feed operation prior to the configuration being saved. This means we
	            //must back-up one feed count to point to the pick location for that prior feed 
	            //operation.
	            fc = fc - 1;
	        }

	        if (trayCountCols >= trayCountRows) {
	            // X major axis.
	            partX = fc / trayCountRows;
	            partY = fc % trayCountRows;
	        } else {
	            // Y major axis.
	            partX = fc % trayCountCols;
	            partY = fc / trayCountCols;
	        }

	        calculatePickLocation(partX, partY);
		}
	
		Logger.debug("{}.getPickLocation => {}", getName(), pickLocation);
		
		return pickLocation;
	}

	private void calculatePickLocation(int partX, int partY) throws Exception {

		// Multiply the offsets by the X/Y part indexes to get the total offsets
		// and then rotate it with the correct rotation matrix.
		// and then add them to the location to get the final pickLocation.
		// pickLocation = location.add(offsets.multiply(partX, partY, 0.0, 0.0));

		double sin = Math.sin(Math.toRadians(location.getRotation()));
		double cos = Math.cos(Math.toRadians(location.getRotation()));

		double delta_x = partX * offsets.getX() * cos + partY * offsets.getY() * sin;
		double delta_y = partX * offsets.getX() * sin - partY * offsets.getY() * cos;
		Location delta = new Location(LengthUnit.Millimeters, delta_x, delta_y, 0, componentRotationInTray);

		pickLocation = location.addWithRotation(delta);
	}

	public void feed(Nozzle nozzle) throws Exception {
		Logger.debug("{}.feed({})", getName(), nozzle);

		int partX, partY;

		if (feedCount >= (trayCountCols * trayCountRows)) {
			throw new Exception(this.getName() + " (" + this.partId + ") is empty.");
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

    /**
     * Returns if the feeder can take back a part.
     * Makes the assumption, that after each feed a pick followed,
     * so the pockets are now empty.
     */
    @Override
    public boolean canTakeBackPart() {
        if (feedCount > 0 ) {  
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void takeBackPart(Nozzle nozzle) throws Exception {
        // first check if we can and want to take back this part (should be always be checked before calling, but to be sure)
        if (nozzle.getPart() == null) {
            throw new UnsupportedOperationException("No part loaded that could be taken back.");
        }
        if (!nozzle.getPart().equals(getPart())) {
            throw new UnsupportedOperationException("Feeder: " + getName() + " - Can not take back " + nozzle.getPart().getName() + " this feeder only supports " + getPart().getName());
        }
        if (!canTakeBackPart()) {
            throw new UnsupportedOperationException("Feeder: " + getName() + " - Currently no free slot. Can not take back the part.");
        }

        // ok, now put the part back on the location of the last pick
        nozzle.moveToPickLocation(this);
        nozzle.place();
        nozzle.moveToSafeZ();
        if (nozzle.isPartOffEnabled(Nozzle.PartOffStep.AfterPlace) && !nozzle.isPartOff()) {
            throw new Exception("Feeder: " + getName() + " - Putting part back failed, check nozzle tip");
        }
        // change FeedCount
        setFeedCount(getFeedCount() - 1);

        //Since this is essentially an unpick and unfeed operation we also need to adjust the pick 
        //location back one place so that it points to the location computed for the feed operation
        //prior to the one we just undone.
        int fc = feedCount;
        if (feedCount > 0) {
            fc = fc - 1;
        }

        int partX, partY;
        if (trayCountCols >= trayCountRows) {
            // X major axis.
            partX = fc / trayCountRows;
            partY = fc % trayCountRows;
        } else {
            // Y major axis.
            partX = fc % trayCountCols;
            partY = fc / trayCountCols;
        }

        calculatePickLocation(partX, partY);
    }

	public int getTrayCountCols() {
		return trayCountCols;
	}

	public void setTrayCountCols(int trayCountCols) {
	    int oldValue = this.trayCountCols;
        this.trayCountCols = trayCountCols;
        firePropertyChange("trayCountCols", oldValue, trayCountCols);
        firePropertyChange("remainingCount", trayCountRows*oldValue - feedCount, 
                trayCountRows*trayCountCols - feedCount);
	}

	public int getTrayCountRows() {
		return trayCountRows;
	}

	public void setTrayCountRows(int trayCountRows) {
        int oldValue = this.trayCountRows;
        this.trayCountRows = trayCountRows;
        firePropertyChange("trayCountRows", oldValue, trayCountRows);
        firePropertyChange("remainingCount", oldValue*trayCountCols - feedCount, 
                trayCountRows*trayCountCols - feedCount);
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

	public int getFeedCount() {
		return feedCount;
	}

	public void setFeedCount(int feedCount) {
		int oldValue = this.feedCount;
		this.feedCount = feedCount;
		firePropertyChange("feedCount", oldValue, feedCount);
		firePropertyChange("remainingCount", trayCountRows*trayCountCols - oldValue, 
		        trayCountRows*trayCountCols - feedCount);
	}
	
	public int getRemainingCount() {
	    return trayCountRows*trayCountCols - feedCount;
	}
	
	public double getComponentRotationInTray() {
        return componentRotationInTray;
    }

    public void setComponentRotationInTray(double componentRotationInTray) {
        double oldValue = this.componentRotationInTray;
        this.componentRotationInTray = componentRotationInTray;
        firePropertyChange("componentRotationInTray", oldValue, componentRotationInTray);
    }

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public Wizard getConfigurationWizard() {
		return new ReferenceRotatedTrayFeederConfigurationWizard(this);
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
