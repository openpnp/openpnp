/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.feeder;



import javax.swing.Action;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.ReferenceTrayFeederConfigurationWizard;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of Feeder that indexes based on an offset. This allows a tray of
 * parts to be picked from without moving any tape. Can handle trays of
 * arbitrary X and Y count.
 */
public class ReferenceTrayFeeder extends ReferenceFeeder {
	private final static Logger logger = LoggerFactory.getLogger(ReferenceTrayFeeder.class);
	
	@Attribute
	private int trayCountX = 1;
	@Attribute
	private int trayCountY = 1;
	@Element
	private Location offsets = new Location(LengthUnit.Millimeters);
	@Attribute
	private int feedCount = 0;
	
	private Location pickLocation;

	@Override
    public Location getPickLocation() throws Exception {
	    if (pickLocation == null) {
	        pickLocation = location;
	    }
		logger.debug("{}.getPickLocation => {}", getName(), pickLocation);
		return pickLocation;
    }

    public void feed(Nozzle nozzle)
			throws Exception {
		logger.debug("{}.feed({})", getName(), nozzle);
		int partX, partY;
		
		if (feedCount >= (trayCountX * trayCountY)) {
		    throw new Exception(String.format("Tray empty on feeder %s.", getName()));
		}
        
        if (trayCountX >= trayCountY) {
            // X major axis.
            partX = feedCount / trayCountY;
            partY = feedCount % trayCountY;
        }
        else {
            // Y major axis.
            partX = feedCount % trayCountX;
            partY = feedCount / trayCountX;
        }
        
        // Multiply the offsets by the X/Y part indexes to get the total offsets
        // and then add the pickLocation to offset the final value.
        // and then add them to the location to get the final pickLocation.
        pickLocation = location.add(
                offsets.multiply(partX, partY, 0.0, 0.0));

        logger.debug(String.format(
                "Feeding part # %d, x %d, y %d, xPos %f, yPos %f, rPos %f", feedCount,
                partX, partY, pickLocation.getX(), pickLocation.getY(), pickLocation.getRotation()));
        
        feedCount++;
	}

	public int getTrayCountX() {
		return trayCountX;
	}

	public void setTrayCountX(int trayCountX) {
		this.trayCountX = trayCountX;
	}

	public int getTrayCountY() {
		return trayCountY;
	}

	public void setTrayCountY(int trayCountY) {
		this.trayCountY = trayCountY;
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
		this.feedCount = feedCount;
	}

	@Override
	public String toString() {
		return getName();
	}

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceTrayFeederConfigurationWizard(this);
    }
    
    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(getConfigurationWizard())
        };
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }  
}
