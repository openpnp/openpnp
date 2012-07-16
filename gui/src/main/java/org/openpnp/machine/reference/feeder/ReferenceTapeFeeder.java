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
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.openpnp.RequiresConfigurationResolution;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.feeder.wizards.ReferenceTapeFeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.VisionProvider;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Persist;

public class ReferenceTapeFeeder extends ReferenceFeeder implements RequiresConfigurationResolution {
	@Element
	private Location feedStartLocation = new Location(LengthUnit.Millimeters);
	@Element
	private Location feedEndLocation = new Location(LengthUnit.Millimeters);
	@Attribute
	private double feedRate;
	@Attribute
	private String actuatorId; 
	@Element(required=false)
	private Vision vision = new Vision();
	

	/*
	 * visionOffset contains the difference between where the part was
	 * expected to be and where it is. Subtracting these offsets from 
	 * the pickLocation produces the correct pick location. Likewise,
	 * subtracting the offsets from the feedStart and feedEndLocations 
	 * should produce the correct feed locations.
	 */
	private Location visionOffset;
	
	@Override
	public void resolve(Configuration configuration) throws Exception {
		super.resolve(configuration);
		configuration.resolve(vision);
	}
	
	@Override
	public boolean canFeedForHead(Head head) {
		return true;
	}
	
	public Location feed(Head head_, Location pickLocation)
			throws Exception {
		
		/*
		 * TODO: We can optimize the feed process:
		 * If we are already higher than the Z we will move to to index plus
		 * the height of the tape, we don't need to Safe Z first.
		 * There is also probably no reason to Safe Z after extracting the
		 * pin since if the tool was going to hit it would have already hit.
		 */

		ReferenceHead head = (ReferenceHead) head_;
		ReferenceActuator actuator = head.getActuator(actuatorId);
		
		
		// Move to safe Z
		head.moveTo(head.getX(), head.getY(), 0, head.getC());
		
		double offsetX = 0;
		double offsetY = 0;
		
		if (vision.isEnabled()) {
			if (visionOffset == null) {
				// This is the first feed with vision, or the offset has
				// been invalidated for some reason. We need to get an offset,
				// complete the feed operation and then get a new offset
				// for the next operation. By front loading this we make sure
				// that all future calls can go directly to the feed operation
				// and skip checking the vision first.
				
				visionOffset = getVisionOffsets(head, pickLocation);
			}
			
			offsetX = visionOffset.getX();
			offsetY = visionOffset.getY();
		}
		
		// move the head so that the pin is positioned above the feed hole
		// TODO: Need to use actuator offsets here!
		head.moveTo(
				feedStartLocation.getX() - offsetX, 
				feedStartLocation.getY() - offsetY,
				head.getZ(), 
				head.getC());

		// extend the pin
		actuator.actuate(true);

		// insert the pin
		head.moveTo(head.getX(), head.getY(), feedStartLocation.getZ(),
				head.getC());

		// drag the tape
		head.moveTo(
				feedEndLocation.getX() - offsetX, 
				feedEndLocation.getY() - offsetY,
				feedEndLocation.getZ(), 
				head.getC(), 
				feedRate);

		// move to safe Z
		head.moveTo(head.getX(), head.getY(), 0, head.getC());

		// retract the pin
		actuator.actuate(false);
		
		// Create a new pickLocation with the offsets included.
		pickLocation = new Location(
				pickLocation.getUnits(),
				pickLocation.getX() - offsetX,
				pickLocation.getY() - offsetY,
				pickLocation.getZ(),
				pickLocation.getRotation()
				); 
		
		if (vision.isEnabled()) {
			System.out.println("Feed complete, running vision");
			visionOffset = getVisionOffsets(head, pickLocation);
			System.out.println("visionOffsets " + visionOffset);
		}

		return pickLocation;
	}
	
	// TODO: Throw an Exception if vision fails.
	private Location getVisionOffsets(Head head, Location pickLocation) throws Exception {
		Machine machine = head.getMachine();

		// Find the Camera to be used for homing
		// TODO: Consider caching this
		Camera camera = null;
		for (Camera c : machine.getCameras()) {
			if (c.getHead() == head && c.getVisionProvider() != null) {
				camera = c;
			}
		}
		
		if (camera == null) {
			throw new Exception("No vision capable camera found on head.");
		}
		
		// Get the camera offsets and convert to native units.
		Location cameraOffsets = camera.getLocation().convertToUnits(machine.getNativeUnits());
		
		// Apply the camera offsets. We subtract instead of adding because we
		// want to position the camera over the location versus wanting to know
		// where the camera is in relation to the location.
		double x = pickLocation.getX() - cameraOffsets.getX();
		double y = pickLocation.getY() - cameraOffsets.getY();
		double z = pickLocation.getZ() - cameraOffsets.getZ();
		
		// Position the camera over the pick location.
		head.moveTo(x, y, 0, head.getC());
		
		// Move the camera to be in focus over the pick location.
		head.moveTo(head.getX(), head.getY(), z, head.getC());
		
		VisionProvider visionProvider = camera.getVisionProvider();
		
		// Perform the template match
		Point[] matchingPoints = visionProvider.locateTemplateMatches(0, 0, 0, 0, 0, 0, vision.getTemplateImage());
		
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
		Location unitsPerPixel = camera.getUnitsPerPixel().convertToUnits(machine.getNativeUnits());
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

	public double getFeedRate() {
		return feedRate;
	}

	public void setFeedRate(double feedRate) {
		this.feedRate = feedRate;
	}
	
	public String getActuatorId() {
		return actuatorId;
	}

	public void setActuatorId(String actuatorId) {
		this.actuatorId = actuatorId;
	}

	public Vision getVision() {
		return vision;
	}

	public void setVision(Vision vision) {
		this.vision = vision;
	}

	public static class Vision implements RequiresConfigurationResolution {
		@Attribute(required=false)
		private boolean enabled;
		@Attribute(required=false)
		private String templateImageName;
		@Element(required=false)
		private Location areaOfInterestTopLeft = new Location(LengthUnit.Millimeters);
		@Element(required=false)
		private Location areaOfInterestBottomRight = new Location(LengthUnit.Millimeters);
		@Element(required=false)
		private Location templateImageTopLeft = new Location(LengthUnit.Millimeters);
		@Element(required=false)
		private Location templateImageBottomRight = new Location(LengthUnit.Millimeters);
		
		private BufferedImage templateImage;
		private boolean templateImageDirty;
		
		private Configuration configuration;
		
		@Override
		public void resolve(Configuration configuration) throws Exception {
			this.configuration = configuration;
			if (templateImageName != null) {
				File file = configuration.getResourceFile(this.getClass(), templateImageName);
				templateImage = ImageIO.read(file);
			}
		}
		
		@SuppressWarnings("unused")
		@Persist
		private void persist() throws IOException {
			if (templateImageDirty) {
				File file = null;
				if (templateImageName != null) {
					file = configuration.getResourceFile(this.getClass(), templateImageName);
				}
				else {
					file = configuration.createResourceFile(this.getClass(), "tmpl_", ".png");
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

		public Location getAreaOfInterestTopLeft() {
			return areaOfInterestTopLeft;
		}

		public void setAreaOfInterestTopLeft(Location areaOfInterestTopLeft) {
			this.areaOfInterestTopLeft = areaOfInterestTopLeft;
		}

		public Location getAreaOfInterestBottomRight() {
			return areaOfInterestBottomRight;
		}

		public void setAreaOfInterestBottomRight(Location areaOfInterestBottomRight) {
			this.areaOfInterestBottomRight = areaOfInterestBottomRight;
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
