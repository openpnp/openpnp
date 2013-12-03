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

import org.openpnp.machine.zippy.VisionManager;
import org.openpnp.machine.zippy.VisionManager.Vision;
import org.openpnp.machine.reference.feeder.ReferenceTapeFeeder;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZippyTapeFeeder extends ReferenceTapeFeeder {
//	@Element(required=false) private VMVision vmvision = new VMVision();
//	@Element(required=false) private Vision vmvision = new Vision();
//	VisionManager.Vision vmvision = vision;
	
	
	 

	private final static Logger logger = LoggerFactory.getLogger(ZippyTapeFeeder.class);
	private Location visionOffset;
	VisionManager visionMgr = new VisionManager();

	
	
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
		
		nozzle.moveToSafeZ(1.0);
		
		Location feedStartLocation = this.feedStartLocation;
		Location feedEndLocation = this.feedEndLocation;
		pickLocation = this.location;
		
		// Move the actuator to the feed start location at safeZ
		actuator.moveTo(feedStartLocation.derive(null, null, Double.NaN, Double.NaN), 1.0);

		// move to start of movement position
		actuator.moveTo(feedStartLocation.derive(null, null, null, Double.NaN), 1.0);

		// move to final laser activation position
		actuator.moveTo(feedEndLocation.derive(null, null, null, Double.NaN), feedSpeed);

		// flash the laser
		actuator.actuate(true);
		Thread.sleep(2000);
		actuator.actuate(false);

		//move back to safeZ
		nozzle.moveToSafeZ(1.0);


		//move camera and get vision offsets for pick operation
		if (vision.isEnabled()) {
			visionOffset = visionMgr.getVisionOffsets(head, location, vision);
			
			logger.debug("final visionOffsets " + visionOffset);
			pickLocation = pickLocation.subtract(visionOffset);
		}
		
        logger.debug("Modified pickLocation {}", pickLocation);
	}

	
}
