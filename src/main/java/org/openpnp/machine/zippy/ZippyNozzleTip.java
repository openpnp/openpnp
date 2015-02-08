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

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.feeder.ReferenceTapeFeeder.Vision;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Point;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.Utils2D;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vision System Description
 * 
 * The Vision Operation is defined as moving the Camera to the defined Mirror
 * Location, rotating the nozzle tip, and performing a template match against 
 * the Template Image bound by the Area of Interest and then storing the offsets
 * from the  Location to the matched image as Vision Offsets.
 * 
 * The calibration operation consists of:
 * 1. Rotate nozzle to 0 degrees, take image and locate template.
 * 2. Rotate nozzle to 180 degrees, take image and locate template.
 * 		- nozzle tip offset in X is 1/2 the difference between the two offsets
 * 3. Rotate nozzle to 90 degrees, take image and locate template.
 * 4. Rotate nozzle to 270 degrees, take image and locate template.
 * 		- nozzle tip offset in Y is 1/2 the difference between the two offsets
 */

public class ZippyNozzleTip extends ReferenceNozzleTip {

	private final static Logger logger = LoggerFactory.getLogger(ZippyNozzleTip.class);

    @Attribute(required = false) private int index;
    @Attribute protected double pixelComp;
       
    @Element(required = false)
    private Location nozzleOffsets;

	@Element(required = false)
	private Location mirrorStartLocation = new Location(LengthUnit.Millimeters);
	@Element(required = false)
	private Location mirrorMidLocation = new Location(LengthUnit.Millimeters);
	@Element(required = false)
	private Location mirrorEndLocation = new Location(LengthUnit.Millimeters);
	@Element(required=false)
	private Vision vision = new Vision();
	
	VisionManager visionMgr = new VisionManager();
		
	/*
	 * vision?Offset contains the difference between where the nozzle tip 
	 * was at 0,180 and 90,270. This is used to calculate nozzle tip offset
	 * from a perfectly strait nozzle at 0 degrees. These offsets are used 
	 * to compensate for nozzle crookedness when moving and rotating the nozzle tip
	 */
    public Location calculateOffset(Location location){
    	
    	Location ntOffset = this.nozzleOffsets; //nozzle tip offset from xml file
    	Location calculatedOffset = null; //new calculated offset


    	// Create the point that represents the nozzle tip offsets (stored offset always for angle zero)
		Point nt_p = new Point(ntOffset.getX(), ntOffset.getY());

    	// Rotate and translate the point into the same rotational coordinate space as the new location
		// use point derived from offsets stored in xml
		Point new_p = Utils2D.rotatePoint(nt_p, location.getRotation());

		//calculate actual (not the change in) new offset. this is used to calibrate camera head-offset
		calculatedOffset = location.derive(new_p.getX(), new_p.getY(), 0.0, null);

		
    	//log calculated offsets
        logger.debug("{}.moveTo( stored_off {})", new Object[] { id, ntOffset } );
        logger.debug("{}.moveTo( calculated_off {})", new Object[] { id, calculatedOffset } );
//        logger.debug("{}.moveTo(adjusted {}, original {},  {})", new Object[] { id, adjustedLocation, location, speed } );
        
        return calculatedOffset;
    }
    
	public Location calibrate(Nozzle nozzle) throws Exception {

		//move to safe height
		nozzle.moveToSafeZ(1.0);
		
		//create local variables 
		Location visionX0Offset;
		Location visionX180Offset;
		Location visionY90Offset;
		Location visionY270Offset;
		
		Location newNozzleOffsets = this.nozzleOffsets;

		
		Location mirrorStartLocation = this.mirrorStartLocation;
		Location mirrorMidLocation = this.mirrorMidLocation;
		Location mirrorEndLocation = this.mirrorEndLocation;
		Location Xoffset;
		Location Yoffset;
		double Zoffset;
		
		//do camera magic
		Head head = nozzle.getHead();
		// Find the Camera to be used for vision
		Camera camera = null;
		for (Camera c : head.getCameras()) {
			if (c.getVisionProvider() != null) {
				camera = c;
			}
		}
		
		if (camera == null) {
			throw new Exception("No vision capable camera found on head.");
		}
		// Position the camera over the pick location.
		logger.debug("Move camera to mirror location.");

		//move to mirror position
		camera.moveTo(mirrorStartLocation, 1.0);
		camera.moveTo(mirrorMidLocation, 1.0);
		camera.moveTo(mirrorEndLocation, 1.0);

		//do camera magic
		visionX0Offset = visionMgr.getVisionOffsets(head, mirrorEndLocation.derive(null, null, null, 0.0),vision);
		visionY90Offset = visionMgr.getVisionOffsets(head, mirrorEndLocation.derive(null, null, null, 90.0),vision);
		visionX180Offset = visionMgr.getVisionOffsets(head, mirrorEndLocation.derive(null, null, null, 180.0),vision);
		visionY270Offset = visionMgr.getVisionOffsets(head, mirrorEndLocation.derive(null, null, null, 270.0),vision);
		
		Xoffset = visionX180Offset.subtract(visionX0Offset);
		Yoffset = visionY90Offset.subtract(visionY270Offset);
//		Zoffset = visionX0Offset.getY();
		Zoffset = 0.0; //TODO: fix Z offset
		
		//move away from mirror position
		camera.moveTo(mirrorEndLocation, 1.0);
		camera.moveTo(mirrorMidLocation, 1.0);
		camera.moveTo(mirrorStartLocation, 1.0);
		
		double offsetX = Xoffset.getX()/2;
		double offsetY = Yoffset.getX()/2;
		
		offsetX *= this.pixelComp; //compensate for calibration distance being different than pick distance
		offsetY *= this.pixelComp; //TODO: make this more elegant and configurable
		
		logger.debug("final nozzletip calibration, at angle zero, offsetX {}, offsetY {}", offsetX, offsetY);
		
		newNozzleOffsets = newNozzleOffsets.derive(offsetX, offsetY, Zoffset, null);
		this.nozzleOffsets = newNozzleOffsets;
		return newNozzleOffsets;
	}

	public Vision getVision() {
		return vision;
	}
	public void setVision(Vision vision) {
		this.vision = vision;
	}
	
    @Override
	public Wizard getConfigurationWizard() {
		return new ZippyNozzleTipConfigurationWizard(this);
	}
    
    public void setNozzleOffsets(Location nozzleOffsets) {
        this.nozzleOffsets = nozzleOffsets;
    }
    
    public Location getNozzleOffsets() {
        return nozzleOffsets;
    }
    
    //
    public void setmirrorStartLocation(Location mirrorStartLocation) {
        this.mirrorStartLocation = mirrorStartLocation;
    }
    
    public Location getmirrorStartLocation() {
        return mirrorStartLocation;
    }
    public void setmirrorMidLocation(Location MirrorMidLocation) {
        this.mirrorMidLocation = MirrorMidLocation;
    }
    
    public Location getmirrorMidLocation() {
        return mirrorMidLocation;
    }

    public void setmirrorEndLocation(Location mirrorEndLocation) {
        this.mirrorEndLocation = mirrorEndLocation;
    }
    
    public Location getmirrorEndLocation() {
        return mirrorEndLocation;
    }

	public String getId() {
		return id;
	}

/*    public void moveTo(Location location, double speed) throws Exception {
		logger.debug("{}.moveTo({}, {})", new Object[] { getId(), location, speed } );
		driver.moveTo((ReferenceHeadMountable) this, location, speed);
        Head head = machine.getHead(getId()); //needs work
		machine.fireMachineHeadActivity(head);
    }
*/	
}
