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
 */

/**
 * SMD tape standard info from http://www.liteplacer.com/setup-tape-positions-2/
 * holes 1.5mm
 * hole pitch 4mm
 * reference hole to part is 2mm
 * tape width is multiple of 4mm
 * part pitch is multiple of 4mm except for 0402 and smaller, where it is 2mm 
 * hole to part is tape width / 2 - 0.5mm 
 */
public class ReferenceStripFeeder extends ReferenceFeeder {
	private final static Logger logger = LoggerFactory.getLogger(ReferenceStripFeeder.class);
	
    // The next four parameters are the critical parameters. The rest should
	// typically be calculable or are just standard.
	@Element(required=false)
    private Location referenceHoleLocation = new Location(LengthUnit.Millimeters);

    @Element(required=false)
    private Location lastHoleLocation = new Location(LengthUnit.Millimeters);

    @Element(required=false)
    private Length partPitch = new Length(4, LengthUnit.Millimeters);
    
    @Element(required=false)
    private Length tapeWidth = new Length(8, LengthUnit.Millimeters);
    
    // Standard or calculable parameters.
    @Element(required=false)
	private Length holeDiameter = new Length(1.5, LengthUnit.Millimeters);
	
	@Element(required=false)
	private Length holePitch = new Length(4, LengthUnit.Millimeters);
	
    @Element(required=false)
    private Length holeToPartLinear = new Length(2, LengthUnit.Millimeters);
    
    @Element(required=false)
    private Length holeToPartLateral = new Length(8 / 2 - 0.5, LengthUnit.Millimeters);
    
    @Attribute
	private int feedCount = 0;
	
	private Location pickLocation;

	@Override
	public boolean canFeedToNozzle(Nozzle nozzle) {
//		boolean result = feedCount < partCount;
//		logger.debug("{}.canFeedToNozzle({}) => {}", new Object[]{getName(), nozzle, result});
//		return result;
	    return true;
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
    
	public Location getReferenceHoleLocation() {
        return referenceHoleLocation;
    }

    public void setReferenceHoleLocation(Location referenceHoleLocation) {
        this.referenceHoleLocation = referenceHoleLocation;
    }

    public Location getLastHoleLocation() {
        return lastHoleLocation;
    }

    public void setLastHoleLocation(Location lastHoleLocation) {
        this.lastHoleLocation = lastHoleLocation;
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

    public Length getTapeWidth() {
        return tapeWidth;
    }

    public void setTapeWidth(Length tapeWidth) {
        this.tapeWidth = tapeWidth;
    }

    public Length getHoleToPartLinear() {
        return holeToPartLinear;
    }

    public void setHoleToPartLinear(Length holeToPartLinear) {
        this.holeToPartLinear = holeToPartLinear;
    }

    public Length getHoleToPartLateral() {
        return holeToPartLateral;
    }

    public void setHoleToPartLateral(Length holeToPartLateral) {
        this.holeToPartLateral = holeToPartLateral;
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
