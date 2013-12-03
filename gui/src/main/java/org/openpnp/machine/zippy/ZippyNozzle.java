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
    private Location original_camera_offsets;
    private Location stored_camera_offsets;
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
    
    //@Override
    public Location xxgetLocation() {
    	Location currentLocation = driver.getLocation(this);
    	Location offset = this.appliedOffset;
    	
		// Create the point that represents the currently applied offsets (stored offset always for angle zero)
		Point ao_p = new Point(appliedOffset.getX(), appliedOffset.getY());

    	// Rotate and translate the point into the same rotational coordinate space as the current location
		Point actual_p = Utils2D.rotatePoint(ao_p, currentLocation.getRotation());

		offset = offset.derive(actual_p.getX(), actual_p.getY(), 0.0, 0.0);
		
		//invert offset so we can remove it and return uncompenstated coordinates
    	Location inv_offset = offset.derive(offset.getX()*-1, offset.getY()*-1,0.0,0.0);

    	Location uncompensated_location = currentLocation.subtract(inv_offset);
    	
    	return uncompensated_location; 
    }

	
	
    @Override
    public void moveTo(Location location, double speed) throws Exception {
    	Configuration configuration = Configuration.get();
    	if(this.camera==null){
//			heads = new ArrayList<Head>(configuration.getMachine().getHeads());
//			cameras = new ArrayList<Camera>((ReferenceCamera)heads[0].getCameras());
//			camera = cameras[0];
	    	camera = (ZippyCamera) configuration.getMachine().getHead("H1").getCamera("C2");
	    	
    	}
		stored_camera_offsets = camera.getHeadOffsets();
    	
    	Location ntOffset; //nozzle tip offset from xml file
    	Location deltaOffset = null; //new change in offset
    	Location currentOffset = null; //new calculated offset
    	Location adjustedLocation; //compensated location
    	
    	for(NozzleTip nt : nozzletips){
    		ZippyNozzleTip znt = (ZippyNozzleTip)nt;
    		if(znt.isLoaded())
    			currentNozzleTip = znt;
    	}

    	//compensation only changes if nozzle rotations changes, so pull current position
    	Location currentLocation = this.getLocation();

    	//pull offsets from current nozzle tip
    	if(currentNozzleTip == null)
    		ntOffset = location.derive(0.0, 0.0, 0.0, null);
    	else
    		ntOffset = ((ZippyNozzleTip) currentNozzleTip).getNozzleOffsets();

    	// Create the point that represents the nozzle tip offsets (stored offset always for angle zero)
		Point nt_p = new Point(ntOffset.getX(), ntOffset.getY());
		// Create the point that represents the currently applied offsets (stored offset always for angle zero)
		Point ao_p = new Point(appliedOffset.getX(), appliedOffset.getY());

    	// Rotate and translate the point into the same rotational coordinate space as the new location
		// use point derived from offsets stored in xml
		Point new_p = Utils2D.rotatePoint(nt_p, location.getRotation());

    	// Rotate and translate the point into the same rotational coordinate space as the old location
		// use point derived from offset already applied (same as above if no change in xml)
		Point old_p = Utils2D.rotatePoint(ao_p, currentLocation.getRotation());

		//calculate change in offset based on rotational change
		//if no change in rotation and no change in xml this is zero
		//this changes each time from data stored in xml, so will be updated each time this changes
		//due to calibration, etc
		deltaOffset = location.derive(new_p.getX()-old_p.getX(), new_p.getY()-old_p.getY(), 0.0, null);
		
		//calculate actual (not the change in) new offset. this is used to calibrate camera head-offset
		currentOffset = location.derive(new_p.getX(), new_p.getY(), 0.0, null);

		//each time, subtract out change in offset
		adjustedLocation = location.subtract(deltaOffset);
		
    	//log calculated offsets
        logger.debug("{}.moveTo( applied_off {})", new Object[] { id, appliedOffset } );
        logger.debug("{}.moveTo( stored_off {})", new Object[] { id, ntOffset } );
        logger.debug("{}.moveTo( delta_off {})", new Object[] { id, deltaOffset } );
        logger.debug("{}.moveTo( original {})", new Object[] { id, location } );
        logger.debug("{}.moveTo( adjusted {})", new Object[] { id, adjustedLocation } );
//        logger.debug("{}.moveTo(adjusted {}, original {},  {})", new Object[] { id, adjustedLocation, location, speed } );
    	
        //don't compensate if it would move past zero
        if(adjustedLocation.getX()>0.0 && adjustedLocation.getY()>0.0){ 
	        //above zero, so call super to move to corrected position
	        super.moveTo(adjustedLocation, speed);
	        appliedOffset = ntOffset;
	        if(deltaOffset.getX()!=0.0 || deltaOffset.getY()!=0.0){ //only tweek camera head offset if there is change
//				Location current_offsets = this.camera.getHeadOffsets();
				this.camera.setCurrentOffset(currentOffset);
//	        	this.camera.setHeadOffsets(stored_camera_offsets.subtract(currentOffset));
//	        	configuration.setDirty(true);
//	        	configuration.save();
	        }
	        
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
