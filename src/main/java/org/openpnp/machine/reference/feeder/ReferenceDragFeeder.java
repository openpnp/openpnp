/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.feeder;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.Action;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.ReferenceDragFeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Rectangle;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.VisionProvider;
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Persist;

/**
 * Vision System Description
 * 
 * The Vision Operation is defined as moving the Camera to the defined Pick Location, performing a
 * template match against the Template Image bound by the Area of Interest and then storing the
 * offsets from the Pick Location to the matched image as Vision Offsets.
 * 
 * The feed operation consists of: 1. Apply the Vision Offsets to the Feed Start Location and Feed
 * End Location. 2. Feed the tape with the modified Locations. 3. Perform the Vision Operation. 4.
 * Apply the new Vision Offsets to the Pick Location and return the Pick Location for Picking.
 * 
 * This leaves the head directly above the Pick Location, which means that when the Feeder is then
 * commanded to pick the Part it only needs to move the distance of the Vision Offsets and do the
 * pick. The Vision Offsets are then used in the next feed operation to be sure to hit the tape at
 * the right position.
 */
public class ReferenceDragFeeder extends ReferenceFeeder {


    public final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    @Element
    protected Location feedStartLocation = new Location(LengthUnit.Millimeters);
    @Element
    protected Location feedEndLocation = new Location(LengthUnit.Millimeters);
    @Element(required = false)
    protected double feedSpeed = 1.0;
    @Attribute(required = false)
    protected String actuatorName;
    @Element(required = false)
    protected Vision vision = new Vision();
    @Element(required = false)
    protected Length backoffDistance = new Length(0, LengthUnit.Millimeters);    

    protected Location pickLocation;

    /*
     * visionOffset contains the difference between where the part was expected to be and where it
     * is. Subtracting these offsets from the pickLocation produces the correct pick location.
     * Likewise, subtracting the offsets from the feedStart and feedEndLocations should produce the
     * correct feed locations.
     */
    protected Location visionOffset;

    @Override
    public Location getPickLocation() throws Exception {
        if (pickLocation == null) {
            pickLocation = location;
        }
        return pickLocation;
    }
    
    //******************************************************
  	//**  Wizard will bind to pinSensorName.  See createBindings() in wizard.
  	//**  
  	
      @Attribute(required = false)
      protected String pinSensorName;
      
      public String getPinSensorName() {
          return pinSensorName;
      }

      public void setPinSensorName(String pinSensorName) {
          String oldValue = this.pinSensorName;
          this.pinSensorName = pinSensorName;
          propertyChangeSupport.firePropertyChange("pinSensorName", oldValue, pinSensorName);
      }
      

  	//******************************************************
  	//**  Wizard will bind to peelActuatorName. See createBindings() in wizard.
  	//**  
      
      @Attribute(required = false)
      protected String peelActuatorName;
      
      
      public String getPeelActuatorName() {
          return peelActuatorName;
      }

      public void setPeelActuatorName(String peelActuatorName) {
          String oldValue = this.peelActuatorName;
          this.peelActuatorName = peelActuatorName;
          propertyChangeSupport.firePropertyChange("peelActuatorName", oldValue, peelActuatorName);
      }
      
      
  	//******************************************************
  	//**  Wizard will bind to pinUpTimeoutMs. See createBindings() in wizard.
  	//**  
      
      @Attribute(required = false)
      protected long pinUpTimeoutMs = 200;
      
      
      public String getPinUpTimeoutMs() {
          return Long.toString(pinUpTimeoutMs);
      }

      public void setPinUpTimeoutMs(String value) {
          Long oldValue = this.pinUpTimeoutMs;
          this.pinUpTimeoutMs = Long.parseLong(value);
          propertyChangeSupport.firePropertyChange("pinUpTimeoutMs", oldValue, value);
      }
      
      
  	//******************************************************
  	//**  Wizard will bind to pinDownTimeoutMs. See createBindings() in wizard.
  	//**  
      
      @Attribute(required = false)
      protected long pinDownTimeoutMs = 500;
      
      
      public String getPinDownTimeoutMs() {
          return Long.toString(pinDownTimeoutMs);
      }

      public void setPinDownTimeoutMs(String value) {
          Long oldValue = this.pinDownTimeoutMs;
          this.pinDownTimeoutMs = Long.parseLong(value);
          propertyChangeSupport.firePropertyChange("pinDownTimeoutMs", oldValue, value);
      }
      
      
      
  	//******************************************************
  	//**  Wizard will bind to pinUpRecoveryTimeoutMs. See createBindings() in wizard.
  	//**  
      
      @Attribute(required = false)
      protected long pinUpRecoveryTimeoutMs = 1000;
      
      
      public String getPinUpRecoveryTimeoutMs() {
          return Long.toString(pinDownTimeoutMs);
      }

      public void setPinUpRecoveryTimeoutMs(String value) {
          Long oldValue = this.pinDownTimeoutMs;
          this.pinUpRecoveryTimeoutMs = Long.parseLong(value);
          propertyChangeSupport.firePropertyChange("pinUpRecoveryTimeoutMs", oldValue, value);
      }
      
  	//******************************************************
  	//**  Wizard will bind to pinUpValue. See createBindings() in wizard.
  	//**  
      
      @Attribute(required = false)
      protected String pinUpValue = "1";
      
      
      public String getPinUpValue() {
          return pinUpValue;
      }

      public void setPinUpValue(String value) {
          String oldValue = this.pinUpValue;
          this.pinUpValue = value;
          propertyChangeSupport.firePropertyChange("pinUpValue", oldValue, value);
      }
      
  	//******************************************************
  	//**  Wizard will bind to pinDownValue. See createBindings() in wizard.
  	//**  
      
      @Attribute(required = false)
      protected String pinDownValue = "0";
      
      
      public String getPinDownValue() {
          return pinDownValue;
      }

      public void setPinDownValue(String value) {
          String oldValue = this.pinDownValue;
          this.pinDownValue = value;
          propertyChangeSupport.firePropertyChange("pinDownValue", oldValue, value);
      }
      
      
  	//******************************************************
  	//**  Wizard will bind to peelMultiplier. See createBindings() in wizard.
  	//**  
      
      @Attribute(required = false)
      protected double peelMultiplier = 1.2;
      
      
      public String getPeelMultiplier() {
          return Double.toString(peelMultiplier);
      }

      public void setPeelMultiplier(String value) {
          double oldValue = this.peelMultiplier;
          this.peelMultiplier = Double.parseDouble(value);
          propertyChangeSupport.firePropertyChange("peelMultiplier", oldValue, value);
      }
      
    //**
  	//**  done with the bindings
  	//****************************************************** 


  	@Override
    public void feed(Nozzle nozzle) throws Exception {
        Logger.debug("feed({})", nozzle);

        if (actuatorName == null) {
            throw new Exception("No actuator name set.");
        }


        Head head = nozzle.getHead();

        /*
         * TODO: We can optimize the feed process: If we are already higher than the Z we will move
         * to to index plus the height of the tape, we don't need to Safe Z first. There is also
         * probably no reason to Safe Z after extracting the pin since if the tool was going to hit
         * it would have already hit.
         */

        Actuator dragPinSolenoid = head.getActuatorByName(actuatorName); // pin that does the dragging
        Actuator pinSensor = head.getActuatorByName(pinSensorName); // sensor to detect pin stuck in tape
        Actuator peelActuator = head.getMachine().getActuatorByName(peelActuatorName);  // peel the cover tape
        
        // Error check, a pin is required
        if (dragPinSolenoid == null) {
            throw new Exception(String.format("No Actuator found with name %s on feed Head %s",
                    actuatorName, head.getName()));
        }
        
        // Error check, a pinSensor is optional, but if one is specified, then one needs to be found
        if ((pinSensorName != null)&&(!pinSensorName.equals("")) && (pinSensor == null)) {
        	throw new Exception(String.format("No Actuator found with name %s on feed Head %s",
        			pinSensorName, head.getName()));
        }
        
        // Error check, a pinSensor is optional, but if one is specified, then one needs to be found
        if ((peelActuatorName != null) && (!peelActuatorName.equals("")) && (peelActuator == null)) {
        	throw new Exception(String.format("No Actuator found with name %s on feed Head %s",
        			peelActuatorName, head.getName()));
        }
        
        head.moveToSafeZ();

        if (vision.isEnabled()) {
            if (visionOffset == null) {
                // This is the first feed with vision, or the offset has
                // been invalidated for some reason. We need to get an offset,
                // complete the feed operation and then get a new offset
                // for the next operation. By front loading this we make sure
                // that all future calls can go directly to the feed operation
                // and skip checking the vision first.
                Logger.debug("First feed, running vision pre-flight.");

                visionOffset = getVisionOffsets(head, location);
            }
            Logger.debug("visionOffsets " + visionOffset);
        } else {
        	// user may have run with vision, then later disabled vision.
        	visionOffset = null;
        }

        // Now we have visionOffsets (if we're using them) so we
        // need to create a local, offset version of the feedStartLocation,
        // feedEndLocation and pickLocation. pickLocation will be saved
        // for the pick operation while feed start and end are used
        // here and then discarded.
        Location feedStartLocation = this.feedStartLocation;
        Location feedEndLocation = this.feedEndLocation;
        pickLocation = this.location;
        if (visionOffset != null) {
            feedStartLocation = feedStartLocation.subtract(visionOffset);
            feedEndLocation = feedEndLocation.subtract(visionOffset);
            pickLocation = pickLocation.subtract(visionOffset);
        }

        // Move the actuator to the feed start location.
        dragPinSolenoid.moveTo(feedStartLocation.derive(null, null, Double.NaN, Double.NaN));


        
        // extend the pin
        dragPinSolenoid.actuate(true);
        

        // for machines with pin sensor
        if(pinSensor != null) {
        	validatePinDown(pinSensor, dragPinSolenoid);
        }

        // insert the pin
        dragPinSolenoid.moveTo(feedStartLocation);

        // same speed for the peel action and the drag function
        double dragSpeed = feedSpeed * dragPinSolenoid.getHead().getMachine().getSpeed();
        
        // start peeling of the tape
        if (peelActuator != null) {
        	
        	// the peel stepper needs to run a little longer than the drag pin
        	// since we start the peel first, do the drag.
        	// if peel was same distance, then a portion of tap cover may remain loose
        	Location l = feedEndLocation.subtract(feedStartLocation).multiply(peelMultiplier, peelMultiplier, 0, 0);
        	double extrude_distance = Math.max(l.getX(), l.getY());
        	
        	peelActuator.extrude(extrude_distance, dragSpeed);
        }
        
        // drag the tape
        dragPinSolenoid.moveTo(feedEndLocation, dragSpeed);
        
        // backoff to release tension from the pin
        Location backoffLocation = null;
        if (backoffDistance.getValue() != 0) {
            backoffLocation = Utils2D.getPointAlongLine(feedEndLocation, feedStartLocation, backoffDistance);
            dragPinSolenoid.moveTo(backoffLocation, feedSpeed * dragPinSolenoid.getHead().getMachine().getSpeed());
        }
        
        head.moveToSafeZ();

        // retract the pin
        dragPinSolenoid.actuate(false);
        
        // for machines with pin sensor
        if(pinSensor != null) {
        	validatePinUp(dragPinSolenoid, pinSensor, feedStartLocation, feedEndLocation, backoffLocation);
        }
        
        

        if (vision.isEnabled()) {
            visionOffset = getVisionOffsets(head, location);

            Logger.debug("final visionOffsets " + visionOffset);
        }

        Logger.debug("Modified pickLocation {}", pickLocation);
    }


    // TODO: Throw an Exception if vision fails.
    public Location getVisionOffsets(Head head, Location pickLocation) throws Exception {
        Logger.debug("getVisionOffsets({}, {})", head.getName(), pickLocation);
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
        
        if (vision.getTemplateImage() == null) {
            throw new Exception("Template image is required when vision is enabled.");
        }
        
        if (vision.getAreaOfInterest().getWidth() == 0 || vision.getAreaOfInterest().getHeight() == 0) {
            throw new Exception("Area of Interest is required when vision is enabled.");
        }

        head.moveToSafeZ();

        // Position the camera over the pick location.
        Logger.debug("Move camera to pick location.");
        camera.moveTo(pickLocation);

        // Move the camera to be in focus over the pick location.
        // head.moveTo(head.getX(), head.getY(), z, head.getC());

        // Settle the camera
        Thread.sleep(camera.getSettleTimeMs());

        VisionProvider visionProvider = camera.getVisionProvider();

        Rectangle aoi = getVision().getAreaOfInterest();

        // Perform the template match
        Logger.debug("Perform template match.");
        Point[] matchingPoints = visionProvider.locateTemplateMatches(aoi.getX(), aoi.getY(),
                aoi.getWidth(), aoi.getHeight(), 0, 0, vision.getTemplateImage());

        // Get the best match from the array
        Point match = matchingPoints[0];

        // match now contains the position, in pixels, from the top left corner
        // of the image to the top left corner of the match. We are interested in
        // knowing how far from the center of the image the center of the match is.
        double imageWidth = camera.getWidth();
        double imageHeight = camera.getHeight();
        double templateWidth = vision.getTemplateImage().getWidth();
        double templateHeight = vision.getTemplateImage().getHeight();
        double matchX = match.x;
        double matchY = match.y;

        Logger.debug("matchX {}, matchY {}", matchX, matchY);

        // Adjust the match x and y to be at the center of the match instead of
        // the top left corner.
        matchX += (templateWidth / 2);
        matchY += (templateHeight / 2);

        Logger.debug("centered matchX {}, matchY {}", matchX, matchY);

        // Calculate the difference between the center of the image to the
        // center of the match.
        double offsetX = (imageWidth / 2) - matchX;
        double offsetY = (imageHeight / 2) - matchY;

        Logger.debug("offsetX {}, offsetY {}", offsetX, offsetY);

        // Invert the Y offset because images count top to bottom and the Y
        // axis of the machine counts bottom to top.
        offsetY *= -1;

        Logger.debug("negated offsetX {}, offsetY {}", offsetX, offsetY);

        // And convert pixels to units
        Location unitsPerPixel = camera.getUnitsPerPixel();
        offsetX *= unitsPerPixel.getX();
        offsetY *= unitsPerPixel.getY();

        Logger.debug("final, in camera units offsetX {}, offsetY {}", offsetX, offsetY);

        return new Location(unitsPerPixel.getUnits(), offsetX, offsetY, 0, 0);
    }

    @Override
    public String toString() {
        return String.format("ReferenceTapeFeeder id %s", id);
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

    public String getActuatorName() {
        return actuatorName;
    }


    public void setActuatorName(String actuatorName) {
        String oldValue = this.actuatorName;
        this.actuatorName = actuatorName;
        propertyChangeSupport.firePropertyChange("actuatorName", oldValue, actuatorName);
    }

    public Length getBackoffDistance() {
        return backoffDistance;
    }

    public void setBackoffDistance(Length backoffDistance) {
        this.backoffDistance = backoffDistance;
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

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceDragFeederConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }
    
	/**
	 * @param pinSensorActuator
	 * @param feedStartLocation
	 * @param feedEndLocation
	 * @param backoffLocation
	 * @throws Exception
	 */
	private void validatePinUp(Actuator pinActuator, Actuator pinSensorActuator, Location feedStartLocation, Location feedEndLocation,
			Location backoffLocation) throws Exception {
		// validate the pin is down
        String pinSensorResult = pinSensorActuator.read();
        //pinSensorResult = dragPinDown.toString(); // TODO debug
        
        if(pinSensorResult != null) {
        	// we have a pin sensor
        	
	        long timeWeStartedWaiting = System.currentTimeMillis();
	        int waitCount =0;
	        
	        // Loop until we've timed out or pin sensor reports pin is up
	        while (System.currentTimeMillis() - timeWeStartedWaiting < pinUpTimeoutMs && !pinSensorResult.equals(pinUpValue)) {
	            waitCount++; // diagnostic useful for dragFeeder without pin sensor
	        	pinSensorResult = pinSensorActuator.read();
	        	//pinSensorResult = dragPinDown.toString(); // TODO debug
	        }
	        if (!pinSensorResult.equals(pinUpValue)) {
	        	// pin failed to rise within specified time
	        	// before declaring failure, attempt a back and forth motion 
	        	if (backoffLocation != null) {
	        		
	        		// will move back and forth from current backoffLocation to a little farther back to feedEndLocation, and back to backoffLocation
	        		Location backoffLocation2 = Utils2D.getPointAlongLine(feedEndLocation, feedStartLocation, backoffDistance.multiply(2));
	        		timeWeStartedWaiting = System.currentTimeMillis();
	        		int waitcount2 = 0;
	        		while (System.currentTimeMillis() - timeWeStartedWaiting < pinUpRecoveryTimeoutMs && !pinSensorResult.equals(pinUpValue)) {
		        		waitcount2++; // diagnostic useful for dragFeeder without pin sensor
	        			pinActuator.moveTo(backoffLocation2, feedSpeed * pinSensorActuator.getHead().getMachine().getSpeed());
		        		pinActuator.moveTo(feedEndLocation, feedSpeed * pinSensorActuator.getHead().getMachine().getSpeed());
		        		pinActuator.moveTo(backoffLocation, feedSpeed * pinSensorActuator.getHead().getMachine().getSpeed());
	        		
		        		Logger.debug("secondary recovery to free pin");
		                
		            	pinSensorResult = pinSensorActuator.read();
	        		}
	        		if (pinSensorResult.equals(pinUpValue)) {
	        			// giggling was successful in freeing the pin
	        			Logger.debug("time for pin to raise=" + String.valueOf(System.currentTimeMillis() - timeWeStartedWaiting) + "ms or " + waitCount + "/" + waitcount2);
	        			return;
	        		} else {
	        			throw new Exception("Drag pin did not raise, even after secondary recovery");
	        		}
	        	} else {
	        		// pin did not raise in time and no backoff, so giggling not possible

	        		throw new Exception("Drag pin did not raise.");
	        	}
	        } else {
	        	// normal path
	        	Logger.debug("time for pin to raise=" + String.valueOf(System.currentTimeMillis() - timeWeStartedWaiting) + "ms or " + waitCount);
	        }
        } else {
        	throw new Exception("failed to read pin sensor");
        }
	}


	/**
	 * @param pinSensor
	 * @throws Exception
	 */
	private void validatePinDown(Actuator pinSensor, Actuator dragPin) throws Exception {
		String pinSensorResult = pinSensor.read();
        
        if(pinSensorResult != null) {
        	// a sensor is available for reading the pin up/down state
        	
	        long timeWeStartedWaiting = System.currentTimeMillis();
	        
	        int waitCount = 0; // used for diagnostics
	        
	        // Loop until we've timed out or pin is confirmed down
	        while (System.currentTimeMillis() - timeWeStartedWaiting < pinDownTimeoutMs && !pinSensorResult.equals(pinDownValue)) {
	            waitCount++; // count number of time we entered this delay loop. Useful in calibrating non sensor drag implementation
	        	pinSensorResult = pinSensor.read();
	        }
	        if (!pinSensorResult.equals(pinDownValue)) {
	        	dragPin.actuate(false); // raise drag pin, ie: a safe place before giving up
	        	throw new Exception("Sensor indicates Drag Pin did not lower.");
	        } else {
	        	Logger.debug("Time for pin to drop=" + String.valueOf(System.currentTimeMillis() - timeWeStartedWaiting) + "ms or " + waitCount);
	        }
        } else
        {
        	throw new Exception("failed to read pin sensor") ;
        }
	}

    public static class Vision {
        @Attribute(required = false)
        private boolean enabled;
        @Attribute(required = false)
        private String templateImageName;
        @Element(required = false)
        private Rectangle areaOfInterest = new Rectangle();
        @Element(required = false)
        private Location templateImageTopLeft = new Location(LengthUnit.Millimeters);
        @Element(required = false)
        private Location templateImageBottomRight = new Location(LengthUnit.Millimeters);

        private BufferedImage templateImage;
        private boolean templateImageDirty;

        public Vision() {
            Configuration.get().addListener(new ConfigurationListener.Adapter() {
                @Override
                public void configurationComplete(Configuration configuration) throws Exception {
                    if (templateImageName != null) {
                        File file = configuration.getResourceFile(Vision.this.getClass(),
                                templateImageName);
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