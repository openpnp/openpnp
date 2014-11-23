/*
 	Copyright (C) 2013 Richard Spelling <openpnp@chebacco.com>
 	
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
package org.openpnp.machine.zippy;

import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZippyNozzle extends ReferenceNozzle {
	
	private final static Logger logger = LoggerFactory
            .getLogger(ZippyNozzle.class);
    
    private Location appliedOffset;
    
    public ZippyNozzle(){
    	appliedOffset = new Location(LengthUnit.Millimeters,0.0,0.0,0.0,0.0);    	
    }
    
    //uncompensated move 
    public void uncompMoveTo(Location location, double speed) throws Exception {
    	super.moveTo(location, speed);
    }
    
    @Override
    public Location getLocation() {
        return driver.getLocation(this).add(appliedOffset);
    }
    
    @Override
    public void moveTo(Location location, double speed) throws Exception {
    	ZippyNozzleTip nozzleTip = (ZippyNozzleTip) getNozzleTip();
        
    	Location calculatedOffset; //new calculated offset
    	Location adjustedLocation; //compensated location
    	
    	// pull calculated offsets for new location
    	calculatedOffset = nozzleTip.calculateOffset(location);
    	
		//each time, add in applied offsets, then subtract out new offset
    	//this way if rotation doesn't change applied offset and calculated offset cancel out
		adjustedLocation = location.add(appliedOffset);
		adjustedLocation = location.subtract(calculatedOffset);
		
        //don't compensate if it would move past zero
        if(adjustedLocation.getX()>0.0 && adjustedLocation.getY()>0.0){ 
	        //above zero, so call super to move to corrected position
	        super.moveTo(adjustedLocation, speed);
	        appliedOffset = calculatedOffset;
	        logger.debug("{}.moveTo(adjusted {}, original {},  {})", new Object[] { id, adjustedLocation, location, speed } );
	        
        } else {
        	//call super to move to original positionoriginal_camera_offsets = camera.getHeadOffsets();
        	// and clear currently applied offset
        	super.moveTo(location, speed);
        	this.clearAppliedOffset();
         }
	       
    }
   
    public void clearAppliedOffset(){
    	appliedOffset=appliedOffset.derive(0.0, 0.0, 0.0, 0.0);
//    	this.camera.setHeadOffsets(original_camera_offsets);
    }
    
    public Location getAppliedOffset(){
    	return appliedOffset;
    }
    
    //    @Override
    public void addNozzleTip(ZippyNozzleTip nozzletip) throws Exception {
        nozzleTips.add(nozzletip);
    }
    
    @Override
    public void moveToSafeZ(double speed) throws Exception {
		logger.debug("{}.moveToSafeZ({})", new Object[]{getId(), speed});
        Location l = new Location(getLocation().getUnits(), Double.NaN, Double.NaN, 10, Double.NaN);
        driver.moveTo(this, l, speed);
        machine.fireMachineHeadActivity(head);
    }
}
