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

import org.openpnp.gui.MainFrame;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Point;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.IdentifiableList;
import org.openpnp.util.Utils2D;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZippyNozzle extends ReferenceNozzle {
	
	private final static Logger logger = LoggerFactory
            .getLogger(ZippyNozzle.class);
    
    //private ZippyNozzleTip currentNozzleTip; 
	@Attribute(required=false) protected String currentNozzleTipid;
	
    private ZippyNozzleTip currentNozzleTip;
    private Location appliedOffset;
    private ZippyCamera camera;
    
    
    public ZippyNozzle(){
    	for(NozzleTip nt : nozzletips){
    		ZippyNozzleTip znt = (ZippyNozzleTip)nt;
    		if(znt.isLoaded())
    			currentNozzleTip = znt;
    	}
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
    	
    	Location calculatedOffset; //new calculated offset
    	Location adjustedLocation; //compensated location
    	
    	if(currentNozzleTip==null){
	    	for(NozzleTip nt : nozzletips){
	    		ZippyNozzleTip znt = (ZippyNozzleTip)nt;
	    		if(znt.isLoaded())
	    			currentNozzleTip = znt;
	    	}
    	}

    	// pull calculated offsets for new location
    	calculatedOffset = currentNozzleTip.calculateOffset(location);
    	
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
   
//    @Override
    public boolean canHandle(Part part) {
    	ZippyNozzleTip nt = (ZippyNozzleTip) this.getNozzleTip();
    	boolean result = part.getPackage().getNozzleTipId() == nt.getId();
    	logger.debug("{}.canHandle({}) => {}", new Object[]{getId(), part.getId(), result});
		return result;
	}

    @Override
    public NozzleTip getNozzleTip() {
        return currentNozzleTip;
    }

//    @Override
    public void setNozzleTip(ZippyNozzleTip nozzletip) {
        this.currentNozzleTip = nozzletip;
        currentNozzleTipid = nozzletip.getId();
    }

    public void clearAppliedOffset(){
    	appliedOffset=appliedOffset.derive(0.0, 0.0, 0.0, 0.0);
//    	this.camera.setHeadOffsets(original_camera_offsets);
    }
    public Location getAppliedOffset(){
    	return appliedOffset;
    }
    
    @Override
    public boolean canPickAndPlace(Feeder feeder, Location placeLocation) {
		boolean result = currentNozzleTip.canHandle(feeder.getPart());
		logger.debug("{}.canPickAndPlace({},{}) => {}", new Object[]{getId(), feeder, placeLocation, result});
    	return result;
	}

    //    @Override
    public void addNozzleTip(ZippyNozzleTip nozzletip) throws Exception {
        nozzletips.add(nozzletip);
   }
    @Override
    public void moveToSafeZ(double speed) throws Exception {
		logger.debug("{}.moveToSafeZ({})", new Object[]{getId(), speed});
        Location l = new Location(getLocation().getUnits(), Double.NaN, Double.NaN, 10, Double.NaN);
        driver.moveTo(this, l, speed);
        machine.fireMachineHeadActivity(head);
    }

}
