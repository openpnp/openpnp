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
import org.openpnp.machine.reference.feeder.wizards.ReferenceStripFeederConfigurationWizard;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of Feeder that indexes through a strip of cut tape.
 * This is a specialization of the tray feeder that knows specifics
 * about tape so that vision capabilities can be added.
 * 
 * 1. Have the user position the camera over the first tape hole
 * 2. Locate the hole
 * 3. Search each of the cardinal directions for the next hole. This gives us
 * the direction of travel.
 * 
 */
public class ReferenceStripFeeder extends ReferenceFeeder {
	private final static Logger logger = LoggerFactory.getLogger(ReferenceStripFeeder.class);

	@Element
	private Length holeDiameter;
	
	@Element
	private Length holePitch;
	
	@Element
	private Length partPitch;
	
	@Element
	private Length holeToPartLateral;
	
	@Element
	private Length holeToPartLinear;
	
    @Attribute
    private int partCount;
    
    @Attribute
	private int feedCount = 0;
	
	private Location pickLocation;

	@Override
	public boolean canFeedToNozzle(Nozzle nozzle) {
		boolean result = feedCount < partCount;
		logger.debug("{}.canFeedToNozzle({}) => {}", new Object[]{getName(), nozzle, result});
		return result;
	}
	
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
//		logger.debug("{}.feed({})", getName(), nozzle);
//		int partX, partY;
//        
//        if (trayCountX >= trayCountY) {
//            // X major axis.
//            partX = feedCount / trayCountY;
//            partY = feedCount % trayCountY;
//        }
//        else {
//            // Y major axis.
//            partX = feedCount % trayCountX;
//            partY = feedCount / trayCountX;
//        }
//        
//        // Multiply the offsets by the X/Y part indexes to get the total offsets
//        // and then add the pickLocation to offset the final value.
//        // and then add them to the location to get the final pickLocation.
//        pickLocation = location.add(
//                offsets.multiply(partX, partY, 0.0, 0.0));
//
//        logger.debug(String.format(
//                "Feeding part # %d, x %d, y %d, xPos %f, yPos %f, rPos %f", feedCount,
//                partX, partY, pickLocation.getX(), pickLocation.getY(), pickLocation.getRotation()));
//        
//        feedCount++;
        throw new Exception("Not yet implements");
	}
    
	public Length getHoleDiameter() {
        return holeDiameter;
    }

    public void setHoleDiameter(Length holeDiameter) {
        this.holeDiameter = holeDiameter;
    }

    public Length getHolePitch() {
        return holePitch;
    }

    public void setHolePitch(Length holePitch) {
        this.holePitch = holePitch;
    }

    public Length getPartPitch() {
        return partPitch;
    }

    public void setPartPitch(Length partPitch) {
        this.partPitch = partPitch;
    }

    public Length getHoleToPartLateral() {
        return holeToPartLateral;
    }

    public void setHoleToPartLateral(Length holeToPartLateral) {
        this.holeToPartLateral = holeToPartLateral;
    }

    public Length getHoleToPartLinear() {
        return holeToPartLinear;
    }

    public void setHoleToPartLinear(Length holeToPartLinear) {
        this.holeToPartLinear = holeToPartLinear;
    }

    public int getPartCount() {
        return partCount;
    }

    public void setPartCount(int partCount) {
        this.partCount = partCount;
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
        return new ReferenceStripFeederConfigurationWizard(this);
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
