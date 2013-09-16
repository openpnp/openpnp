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
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.VisionProvider;
import org.openpnp.util.MovableUtils;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Persist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vision System Description
 * 
 * The Vision Operation is defined as moving the Camera to the defined Pick
 * Location, performing a template match against the Template Image bound by
 * the Area of Interest and then storing the offsets from the Pick Location to
 * the matched image as Vision Offsets.
 * 
 * The feed operation consists of:
 * 1. Apply the Vision Offsets to the Feed Start Location
 * and Feed End Location.
 * 2. Feed the tape with the modified Locations.
 * 3. Perform the Vision Operation.
 * 4. Apply the new Vision Offsets to the Pick Location and return the Pick
 * Location for Picking.
 * 
 * This leaves the head directly above the Pick Location, which means that
 * when the Feeder is then commanded to pick the Part it only needs to move
 * the distance of the Vision Offsets and do the pick. The Vision Offsets are
 * then used in the next feed operation to be sure to hit the tape at the
 * right position.
 */
public class ReferenceTapeFeeder extends ReferenceFeeder {
	private final static Logger logger = LoggerFactory.getLogger(ReferenceTapeFeeder.class);
	
	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	
	@Element
	private Location feedStartLocation = new Location(LengthUnit.Millimeters);
	@Element
	private Location feedEndLocation = new Location(LengthUnit.Millimeters);
	@Element(required=false)
	private double feedSpeed = 1.0;
	@Attribute(required=false)
	private String actuatorId; 
	@Element(required=false)
	private Vision vision = new Vision();
	
	private Location pickLocation;
	private Configuration configuration;

	/*
	 * visionOffset contains the difference between where the part was
	 * expected to be and where it is. Subtracting these offsets from 
	 * the pickLocation produces the correct pick location. Likewise,
	 * subtracting the offsets from the feedStart and feedEndLocations 
	 * should produce the correct feed locations.
	 */
	private Location visionOffset;
	
	@Override
	public boolean canFeedToNozzle(Nozzle nozzle) {
	    return nozzle.getNozzleTip().canHandle(part);
	}
	
	@Override
    public Location getPickLocation() throws Exception {
	    if (pickLocation == null) {
	        pickLocation = location;
	    }
	    return pickLocation;
    }

    @Override
	public void feed(Nozzle nozzle)
			throws Exception {
		logger.debug("feed({})", nozzle);
		
		if (actuatorId == null) {
			throw new Exception("No actuator ID set.");
		}
		
		
		Head head = nozzle.getHead();
		
		/*
		 * TODO: We can optimize the feed process:
		 * If we are already higher than the Z we will move to to index plus
		 * the height of the tape, we don't need to Safe Z first.
		 * There is also probably no reason to Safe Z after extracting the
		 * pin since if the tool was going to hit it would have already hit.
		 */

		Actuator actuator = head.getActuator(actuatorId);
		
		if (actuator == null) {
			throw new Exception(String.format("No Actuator found with ID %s on feed Head %s", actuatorId, head.getId()));
		}
		
		// Convert all the Locations we'll be dealing with to machine native units
//		pickLocation = pickLocation.convertToUnits(head.getMachine().getNativeUnits());
//		Location feedStartLocation = this.feedStartLocation.convertToUnits(head.getMachine().getNativeUnits());
//		Location feedEndLocation = this.feedEndLocation.convertToUnits(head.getMachine().getNativeUnits());
//		Location actuatorOffsets = actuator.getLocation().convertToUnits(head.getMachine().getNativeUnits());
//		Length feedRate = this.feedRate.convertToUnits(head.getMachine().getNativeUnits());
		
//		logger.debug("Converted inputs: pickLocation {}, feedStartLocation {}, feedEndLocation {}, actuatorOffsets {}, feedRate {}", new Object[] {
//				pickLocation, 
//				feedStartLocation, 
//				feedEndLocation, 
//				actuatorOffsets,
//				feedRate});
		
		head.moveToSafeZ(1.0);
		
		double offsetX = 0;
		double offsetY = 0;

		// TODO: temporary bug fix for null location
		getPickLocation();
		
		// Create a new pickLocation with the offsets included.
		pickLocation = new Location(
				pickLocation.getUnits(),
				pickLocation.getX() - offsetX,
				pickLocation.getY() - offsetY,
				pickLocation.getZ(),
				pickLocation.getRotation()
				);
		
		logger.debug("Modified pickLocation {}", pickLocation);
		
		if (vision.isEnabled()) {
			if (visionOffset == null) {
				// This is the first feed with vision, or the offset has
				// been invalidated for some reason. We need to get an offset,
				// complete the feed operation and then get a new offset
				// for the next operation. By front loading this we make sure
				// that all future calls can go directly to the feed operation
				// and skip checking the vision first.
				logger.debug("First feed, running vision pre-flight.");
				
				visionOffset = getVisionOffsets(head, pickLocation);
			}
			
			logger.debug("visionOffsets " + visionOffset);
			
			offsetX = visionOffset.getX();
			offsetY = visionOffset.getY();
		}
		
		// Move the head so that the pin is positioned above the feed hole
		// feedStartLocation is the position of the hole in the tool's coordinate
		// system, so we need to offset that by the actuator offsets.
		actuator.moveTo(feedStartLocation.derive(null, null, Double.NaN, Double.NaN), 1.0);

		// extend the pin
		actuator.actuate(true);

		// insert the pin
		actuator.moveTo(feedStartLocation, 1.0);

		// drag the tape
		actuator.moveTo(feedEndLocation, feedSpeed);

		head.moveToSafeZ(1.0);

		// retract the pin
		actuator.actuate(false);
		
		
		if (vision.isEnabled()) {
			visionOffset = getVisionOffsets(head, pickLocation);
			
			logger.debug("final visionOffsets " + visionOffset);
		}
	}
	
	// TODO: Throw an Exception if vision fails.
    // TODO: Vision temporarily disabled due to refactoring
	private Location getVisionOffsets(Head head, Location pickLocation) throws Exception {
//		configuration = new Configuration("/home/rlspell/.openpnp"); 
//		this.configuration = configuration;
		Configuration configuration = Configuration.get();
		Machine machine = configuration.getMachine();

		// Find the Camera to be used for homing
		// TODO: Consider caching this
		Camera camera = null;
		for (Camera c : machine.getCameras()) {
			if (c.getHead() == head && c.getVisionProvider() != null) {
				camera = c;
			}
		}
//		camera = machine.getCameras(); //testing, returns list, fails
		camera = head.getCamera("C2"); //testing
		
		if (camera == null) {
			throw new Exception("No vision capable camera found on head.");
		}
		
		// Get the camera offsets and convert to native units.
		//assume mm for now
//		Location cameraOffsets = camera.getLocation().convertToUnits(machine.getNativeUnits());
	    //?	visionOffset

//		Location cameraOffsets = camera.getLocation();
		
		// Apply the camera offsets. We subtract instead of adding because we
		// want to position the camera over the location versus wanting to know
		// where the camera is in relation to the location.
//		double x = pickLocation.getX() - cameraOffsets.getX();
//		double y = pickLocation.getY() - cameraOffsets.getY();
//		double z = pickLocation.getZ() - cameraOffsets.getZ();
		
		// Position the camera over the pick location.
//		head.moveTo(x,y,z, 1.0);
		camera.moveToSafeZ(1.0);
		
		// Move the camera to be in focus over the pick location.
//		head.moveTo(head.getX(), head.getY(), z, head.getC());
		camera.moveTo(pickLocation, 1.0);
		
		// Settle the camera
		Thread.sleep(200);
		
		VisionProvider visionProvider = camera.getVisionProvider();
		
		Rectangle aoi = getVision().getAreaOfInterest();
		
		// Perform the template match
		Point[] matchingPoints = visionProvider.locateTemplateMatches(
				aoi.getX(), 
				aoi.getY(), 
				aoi.getWidth(), 
				aoi.getHeight(), 
				0, 
				0, 
				vision.getTemplateImage());
		
		// Get the best match from the array
		Point match = matchingPoints[0];
		
		// match now contains the position, in pixels, from the top left corner
		// of the image to the top left corner of the match. We are interested in
		// knowing how far from the center of the image the center of the match is.
		BufferedImage image = camera.capture();
		double imageWidth = image.getWidth();
		double imageHeight = image.getHeight();
		double templateWidth = vision.getTemplateImage().getWidth();
		double templateHeight = vision.getTemplateImage().getHeight();
		double matchX = match.x;
		double matchY = match.y;
		
		// Adjust the match x and y to be at the center of the match instead of
		// the top left corner.
		matchX += (templateWidth / 2);
		matchY += (templateHeight / 2);
		
		// Calculate the difference between the center of the image to the
		// center of the match.
		double offsetX = (imageWidth / 2) - matchX;
		double offsetY = (imageHeight / 2) - matchY;
		
		// Invert the Y offset because images count top to bottom and the Y
		// axis of the machine counts bottom to top.
		offsetY *= -1;
		
		// And convert pixels to units
		//assume mm for now
//		Location unitsPerPixel = camera.getUnitsPerPixel().convertToUnits(machine.getNativeUnits());
		Location unitsPerPixel = camera.getUnitsPerPixel();
		offsetX *= unitsPerPixel.getX();
		offsetY *= unitsPerPixel.getY();
		
		return new Location(pickLocation.getUnits(), offsetX, offsetY, 0, 0);
	}

	@Override
	public String toString() {
		return String.format("ReferenceTapeFeeder id %s", id);
	}

	@Override
	public Wizard getConfigurationWizard() {
		return new ReferenceTapeFeederConfigurationWizard(this);
	}

	public Location getFeedStartLocation() {
		return feedStartLocation;
	}

	public void setFeedStartLocation(Location feedStartLocation) {
		this.feedStartLocation = feedStartLocation;
	}

	public Location getFeedEndLocation() {
		return feedEndLocation;
	}

	public void setFeedEndLocation(Location feedEndLocation) {
		this.feedEndLocation = feedEndLocation;
	}

	public Double getFeedSpeed() {
		return feedSpeed;
	}

	public void setFeedSpeed(Double feedSpeed) {
		this.feedSpeed = feedSpeed;
	}
	
	public String getActuatorId() {
		return actuatorId;
	}

	public void setActuatorId(String actuatorId) {
		String oldValue = this.actuatorId;
		this.actuatorId = actuatorId;
		propertyChangeSupport.firePropertyChange("actuatorId", oldValue, actuatorId);
	}

	public Vision getVision() {
		return vision;
	}

	public void setVision(Vision vision) {
		this.vision = vision;
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(propertyName,
				listener);
	}

	public static class Vision {
		@Attribute(required=false)
		private boolean enabled;
		@Attribute(required=false)
		private String templateImageName;
		@Element(required=false)
		private Rectangle areaOfInterest = new Rectangle();
		@Element(required=false)
		private Location templateImageTopLeft = new Location(LengthUnit.Millimeters);
		@Element(required=false)
		private Location templateImageBottomRight = new Location(LengthUnit.Millimeters);
		
		private BufferedImage templateImage;
		private boolean templateImageDirty;
		
		public Vision() {
	        Configuration.get().addListener(new ConfigurationListener.Adapter() {
	            @Override
	            public void configurationComplete(Configuration configuration)
	                    throws Exception {
	                if (templateImageName != null) {
	                    File file = configuration.getResourceFile(Vision.this.getClass(), templateImageName);
	                    templateImage = ImageIO.read(file);
	                }
	            }
	        });
		}
		
		@SuppressWarnings("unused")
		@Persist
		private void persist() throws IOException {
			if (templateImageDirty) {
				File file = null;
				if (templateImageName != null) {
					file = Configuration.get().getResourceFile(this.getClass(), templateImageName);
				}
				else {
					file = Configuration.get().createResourceFile(this.getClass(), "tmpl_", ".png");
					templateImageName = file.getName();
				}
				ImageIO.write(templateImage, "png", file);
				templateImageDirty = false;
			}
		}
		
		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
		
		public BufferedImage getTemplateImage() {
			return templateImage;
		}
		
		public void setTemplateImage(BufferedImage templateImage) {
			if (templateImage != this.templateImage) {
				this.templateImage = templateImage;
				templateImageDirty = true;
			}
		}
		
		public Rectangle getAreaOfInterest() {
			return areaOfInterest;
		}

		public void setAreaOfInterest(Rectangle areaOfInterest) {
			this.areaOfInterest = areaOfInterest;
		}

		public Location getTemplateImageTopLeft() {
			return templateImageTopLeft;
		}

		public void setTemplateImageTopLeft(Location templateImageTopLeft) {
			this.templateImageTopLeft = templateImageTopLeft;
		}

		public Location getTemplateImageBottomRight() {
			return templateImageBottomRight;
		}

		public void setTemplateImageBottomRight(Location templateImageBottomRight) {
			this.templateImageBottomRight = templateImageBottomRight;
		}
	}
}
