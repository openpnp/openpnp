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
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Point;
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
	
    private boolean alreadyCompensatedNozzleTip;
    
    private ZippyNozzleTip currentNozzleTip;
    
    public ZippyNozzle(){
    	for(NozzleTip nt : nozzletips){
    		ZippyNozzleTip znt = (ZippyNozzleTip)nt;
    		if(znt.isLoaded())
    			currentNozzleTip = znt;
    	}
    }
    
    @Override
    public void moveTo(Location location, double speed) throws Exception {
    	for(NozzleTip nt : nozzletips){
    		ZippyNozzleTip znt = (ZippyNozzleTip)nt;
    		if(znt.isLoaded())
    			currentNozzleTip = znt;
    	}

    	//compensation only changes if nozzle rotations changes, so pull current position
    	Location currentLocation = this.getLocation();

    	//work with only first one till we write changer code
//    	currentNozzleTip = (ZippyNozzleTip) nozzletips.get("NT1"); 

    	//pull offsets from current nozzle tip
    	Location offset;
    	if(currentNozzleTip == null)
    		offset = location.derive(0.0, 0.0, 0.0, null);
    	else
    		offset = ((ZippyNozzleTip) currentNozzleTip).getNozzleOffsets();

    	// Create the point that represents the nozzle tip offsets (stored offset always for angle zero)
		Point p = new Point(offset.getX(), 	offset.getY());

    	// Rotate and translate the point into the same rotational coordinate space as the new location
		Point new_p = Utils2D.rotatePoint(p, location.getRotation());

    	// Rotate and translate the point into the same rotational coordinate space as the old location
		Point old_p = Utils2D.rotatePoint(p, currentLocation.getRotation());

		// Update the  offset Location with the difference between the transformed points
		// first move add full compensation, rest of moves only add compensation if nozzle rotates
		if(alreadyCompensatedNozzleTip){
			offset = offset.derive(new_p.getX()-old_p.getX(), new_p.getY()-old_p.getY(), null, null);
		} else {
			offset = offset.derive(old_p.getX(), old_p.getY(), null, null);
			alreadyCompensatedNozzleTip = true;
		}
		//subtract rotated offset 
    	Location adjustedLocation = location.subtract(offset);

    	//log calculated offsets
        logger.debug("{}.moveTo(adjusted {}, original {},  {})", new Object[] { id, adjustedLocation, location, speed } );
    	
        //don't compensate if it would move past zero
        if(adjustedLocation.getX()>0.0 && adjustedLocation.getY()>0.0){ 
	        //call super to move to corrected position
	        super.moveTo(adjustedLocation, speed);
        } else {
        	//call super to move to original position
        	super.moveTo(location, speed);
        	alreadyCompensatedNozzleTip = false;
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
