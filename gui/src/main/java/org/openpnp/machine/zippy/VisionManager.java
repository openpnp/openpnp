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

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.openpnp.ConfigurationListener;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Rectangle;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.VisionProvider;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Persist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VisionManager {
	private final static Logger logger = LoggerFactory.getLogger(VisionManager.class);
	
	public Location getVisionOffsets(Head head, Location calibrationLocation, Vision vision) throws Exception {
	    logger.debug("getVisionOffsets({}, {})", head.getId(), calibrationLocation);
		// Find the Camera to be used for vision
		// TODO: Consider caching this
		ZippyCamera camera = null;
		for (Camera c : head.getCameras()) {
			if (c.getVisionProvider() != null) {
				camera = (ZippyCamera) c;
			}
		}
		
		if (camera == null) {
			throw new Exception("No vision capable camera found on head.");
		}
		
//		head.moveToSafeZ(1.0);
		
		// Position the camera over the calibration location.
		logger.debug("Move camera to calibration location.");
		
		camera.moveTo(calibrationLocation, 1.0);
		
		
		// Settle the camera
		// TODO: This should be configurable, or maybe just built into
		// the VisionProvider
		Thread.sleep(500);
		
		VisionProvider visionProvider = camera.getVisionProvider();
		
		Rectangle aoi = vision.getAreaOfInterest();
		
		// Perform the template match
		logger.debug("Perform template match.");
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

        logger.debug("matchX {}, matchY {}", matchX, matchY);

		// Adjust the match x and y to be at the center of the match instead of
		// the top left corner.
		matchX += (templateWidth / 2);
		matchY += (templateHeight / 2);
		
        logger.debug("centered matchX {}, matchY {}", matchX, matchY);

		// Calculate the difference between the center of the image to the
		// center of the match.
		double offsetX = (imageWidth / 2) - matchX;
		double offsetY = (imageHeight / 2) - matchY;

        logger.debug("offsetX {}, offsetY {}", offsetX, offsetY);
		
		// Invert the Y offset because images count top to bottom and the Y
		// axis of the machine counts bottom to top.
		offsetY *= -1;
		
        logger.debug("negated offsetX {}, offsetY {}", offsetX, offsetY);
		
		// And convert pixels to units
		Location unitsPerPixel = camera.getUnitsPerPixel();
		offsetX *= unitsPerPixel.getX();
		offsetY *= unitsPerPixel.getY();

        logger.debug("final, in camera units offsetX {}, offsetY {}", offsetX, offsetY);
		
        return new Location(unitsPerPixel.getUnits(), offsetX, offsetY, 0, 0);
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
