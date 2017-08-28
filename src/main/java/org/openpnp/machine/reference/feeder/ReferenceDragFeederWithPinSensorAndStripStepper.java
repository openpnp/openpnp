package org.openpnp.machine.reference.feeder;

import java.util.concurrent.TimeUnit;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.feeder.wizards.ReferenceDragFeederWithPinSensorConfigurationWizard;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

public class ReferenceDragFeederWithPinSensorAndStripStepper extends ReferenceDragFeeder {

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
        if ((!pinSensorName.equals("")) && (pinSensor == null)) {
        	throw new Exception(String.format("No Actuator found with name %s on feed Head %s",
        			pinSensorName, head.getName()));
        }
        
        // Error check, a pinSensor is optional, but if one is specified, then one needs to be found
        if ((!peelActuatorName.equals("")) && (peelActuator == null)) {
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

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceDragFeederWithPinSensorConfigurationWizard(this);
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
}
