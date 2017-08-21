package org.openpnp.machine.reference.feeder;

import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;

public class ReferenceDragFeederWithPinSensorAndStripStepper extends ReferenceDragFeeder {

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

        Actuator actuator = head.getActuatorByName(actuatorName);

        if (actuator == null) {
            throw new Exception(String.format("No Actuator found with name %s on feed Head %s",
                    actuatorName, head.getName()));
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
        actuator.moveTo(feedStartLocation.derive(null, null, Double.NaN, Double.NaN));

        // TODO: make this a parameter that gets provisioned
        String dragPinUp = "1";
        String dragPinDown = "0";
        String pinSensorResult = actuator.read();
        
        // extend the pin
        actuator.actuate(true);
                
        if(pinSensorResult != null) {
        	// a sensor is available for reading the pin up/down state
        	
	        long timeWeStartedWaiting = System.currentTimeMillis();
	        long timeout = 1000; // TODO, how to read this from provisioned parameters
	        
	        int waitCount = 0; // used for diagnostics
	        
	        // Loop until we've timed out or pin is confirmed down
	        while (System.currentTimeMillis() - timeWeStartedWaiting < timeout && !pinSensorResult.equals(dragPinDown)) {
	            waitCount++; // count number of time we entered this delay loop. Useful in calibrating non sensor drag implementation
	        	pinSensorResult = actuator.read();
	        }
	        if (!pinSensorResult.equals(dragPinDown)) {
	        	actuator.actuate(false); // raise drag pin, ie: a safe place before giving up
	        	throw new Exception("Sensor indicates Drag Pin did not lower.");
	        } else {
	        	Logger.debug("Time for pin to drop=" + String.valueOf(System.currentTimeMillis() - timeWeStartedWaiting) + "ms or " + waitCount);
	        }
        } else {
        	// if there is no sensor for the Drag Pin, need to give some time for the pin to mechanically drop before proceeding
        	// user expected to put delay in Gcode.  like G4 P1000
        }

        // insert the pin
        actuator.moveTo(feedStartLocation);

        // drag the tape
        actuator.moveTo(feedEndLocation, feedSpeed * actuator.getHead().getMachine().getSpeed());
        
        // backoff to release tension from the pin
        Location backoffLocation = null;
        if (backoffDistance.getValue() != 0) {
            backoffLocation = Utils2D.getPointAlongLine(feedEndLocation, feedStartLocation, backoffDistance);
            actuator.moveTo(backoffLocation, feedSpeed * actuator.getHead().getMachine().getSpeed());
        }
        
        head.moveToSafeZ();

        // retract the pin
        actuator.actuate(false);
        
        // validate the pin is down
        pinSensorResult = actuator.read();
        
        if(pinSensorResult != null) {
        	// we have a pin sensor
        	
	        long timeWeStartedWaiting = System.currentTimeMillis();
	        long timeout = 1000;
	        int waitCount =0;
	        
	        // Loop until we've timed out or pin sensor reports pin is up
	        while (System.currentTimeMillis() - timeWeStartedWaiting < timeout && !pinSensorResult.equals(dragPinUp)) {
	            waitCount++; // diagnostic useful for dragFeeder without pin sensor
	        	pinSensorResult = actuator.read();
	        }
	        if (!pinSensorResult.equals(dragPinUp)) {
	        	// pin failed to rise within specified time
	        	// before declaring failure, attempt a back and forth motion 
	        	if (backoffLocation != null) {
	        		timeout = 2000; // extend timeout
	        		// will move back and forth from current backoffLocation to a little farther back to feedEndLocation, and back to backoffLocation
	        		Location backoffLocation2 = Utils2D.getPointAlongLine(backoffLocation, feedStartLocation, backoffDistance);
	        		timeWeStartedWaiting = System.currentTimeMillis();
	        		while (System.currentTimeMillis() - timeWeStartedWaiting < timeout && !pinSensorResult.equals(dragPinUp)) {
		        		actuator.moveTo(backoffLocation2, feedSpeed * actuator.getHead().getMachine().getSpeed());
		        		actuator.moveTo(feedEndLocation, feedSpeed * actuator.getHead().getMachine().getSpeed());
		        		actuator.moveTo(backoffLocation2, feedSpeed * actuator.getHead().getMachine().getSpeed());
	        		
		        		Logger.debug("jiggling dragPin");
		                
		            	pinSensorResult = actuator.read();
	        		}
	        	}
	        	
	        	actuator.actuate(false); // raise drag pin, ie: a safe place
	        	throw new Exception("Drag pin did not raise.");
	        } else {
	        	Logger.debug("time for pin to raise=" + String.valueOf(System.currentTimeMillis() - timeWeStartedWaiting) + "ms or " + waitCount);
	        }
        } else {
        	// a delay is needed before moving forward.
        	// user expected to have delay in gcode like G4 P1000
        }
        
        

        if (vision.isEnabled()) {
            visionOffset = getVisionOffsets(head, location);

            Logger.debug("final visionOffsets " + visionOffset);
        }

        Logger.debug("Modified pickLocation {}", pickLocation);
    }
}
