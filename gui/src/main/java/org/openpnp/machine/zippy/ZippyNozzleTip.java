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

import java.util.List;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzleTip;
//import org.openpnp.machine.reference.feeder.ReferenceTapeFeeder.Vision;
import org.openpnp.machine.reference.feeder.wizards.ReferenceTapeFeederConfigurationWizard;
import org.openpnp.machine.reference.wizards.ReferenceActuatorConfigurationWizard;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.IdentifiableList;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;







//vision stuff
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.ReferenceTapeFeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Rectangle;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.VisionProvider;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Persist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openpnp.machine.zippy.VisionManager;
import org.openpnp.machine.zippy.VisionManager.Vision;

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

    public ZippyNozzleTip(){
    	//set parent nozzle
  //   	Location nozzleOffsets = new Location(); 
    }
/*    private ReferenceMachine machine;
    private ReferenceDriver driver;
*/
    @Attribute(required = false)
    private int index;
    @Attribute
    protected boolean loaded;
       
    @Element(required = false)
    private Location nozzleOffsets;

	@Element(required = false)
	private Location mirrorStartLocation = new Location(LengthUnit.Millimeters);
	@Element(required = false)
	private Location mirrorMidLocation = new Location(LengthUnit.Millimeters);
	@Element(required = false)
	private Location mirrorEndLocation = new Location(LengthUnit.Millimeters);
	@Element(required = false)
	private Location changerStartLocation = new Location(LengthUnit.Millimeters);
	@Element(required = false)
	private Location changerMidLocation = new Location(LengthUnit.Millimeters);
	@Element(required = false)
	private Location changerEndLocation = new Location(LengthUnit.Millimeters);
	@Element(required=false)
	private Vision vision = new Vision();
	
	VisionManager visionMgr = new VisionManager();
	
    public boolean isLoaded() {
        return loaded;
    }

     public void setLoaded(boolean enabled) {
        this.loaded = enabled;
    }

	
	/*
	 * vision?Offset contains the difference between where the nozzle tip 
	 * was at 0,180 and 90,270. This is used to calculate nozzle tip offset
	 * from a perfectly strait nozzle at 0 degrees. These offsets are used 
	 * to compensate for nozzle crookedness when moving and rotating the nozzle tip
	 */

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
		nozzle.moveTo(mirrorStartLocation, 1.0);
		nozzle.moveTo(mirrorMidLocation, 1.0);
		nozzle.moveTo(mirrorEndLocation, 1.0);

		//do camera magic
		visionX0Offset = visionMgr.getVisionOffsets(head, mirrorEndLocation.derive(null, null, null, 0.0),vision);
		visionX180Offset = visionMgr.getVisionOffsets(head, mirrorEndLocation.derive(null, null, null, 180.0),vision);
		visionY90Offset = visionMgr.getVisionOffsets(head, mirrorEndLocation.derive(null, null, null, 90.0),vision);
		visionY270Offset = visionMgr.getVisionOffsets(head, mirrorEndLocation.derive(null, null, null, 270.0),vision);
		
		Xoffset = visionX0Offset.subtract(visionX180Offset);
		Yoffset = visionY90Offset.subtract(visionY270Offset);
		
		//move away from mirror position
		nozzle.moveTo(mirrorEndLocation, 1.0);
		nozzle.moveTo(mirrorMidLocation, 1.0);
		nozzle.moveTo(mirrorStartLocation, 1.0);
		

		return newNozzleOffsets.derive(Xoffset.getX(), Yoffset.getX(), null, null);
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

    public void setchangerStartLocation(Location changerStartLocation) {
        this.changerStartLocation = changerStartLocation;
    }
    
    public Location getchangerStartLocation() {
        return changerStartLocation;
    }

    public void setchangerMidLocation(Location changerMidLocation) {
        this.changerMidLocation = changerMidLocation;
    }
    
    public Location getchangerMidLocation() {
        return changerMidLocation;
    }
    
    public void setchangerEndLocation(Location changerEndLocation) {
        this.changerEndLocation = changerEndLocation;
    }
    
    public Location getchangerEndLocation() {
        return changerEndLocation;
    }


	public String getId() {
		return id;
	}
    public void setId(String id) {
        this.id = id;
    }

/*    public void moveTo(Location location, double speed) throws Exception {
		logger.debug("{}.moveTo({}, {})", new Object[] { getId(), location, speed } );
		driver.moveTo((ReferenceHeadMountable) this, location, speed);
        Head head = machine.getHead(getId()); //needs work
		machine.fireMachineHeadActivity(head);
    }
*/	public void load(Nozzle nozzle) throws Exception {
		//move to safe height
		nozzle.moveToSafeZ(1.0);
		//create local variables for movement
		Location changerStartLocation = this.changerStartLocation;
		Location changerMidLocation = this.changerMidLocation;
		Location changerEndLocation = this.changerEndLocation;
		
		//perform load operation
		nozzle.moveTo(changerStartLocation, 1.0);
		nozzle.moveTo(changerMidLocation, 1.0);
		nozzle.moveTo(changerEndLocation, 1.0);

		//move to safe height
		nozzle.moveToSafeZ(1.0);
		
	}
	public void unload(Nozzle nozzle) throws Exception {
		//move to safe height
		nozzle.moveToSafeZ(1.0);
		
		//create local variables for movement
		Location changerStartLocation = this.changerStartLocation;
		Location changerMidLocation = this.changerMidLocation;
		Location changerEndLocation = this.changerEndLocation;
		
		//perform unload operation
		nozzle.moveTo(changerEndLocation, 1.0);
		nozzle.moveTo(changerMidLocation, 1.0);
		nozzle.moveTo(changerStartLocation, 1.0);

		//move to safe height
		nozzle.moveToSafeZ(1.0);
	}

}
