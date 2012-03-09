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



import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Head;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.PersistenceException;
import org.simpleframework.xml.core.Validate;


public class ReferenceTapeFeeder extends ReferenceFeeder {
	@Element
	private Location feedStartLocation;
	@Element
	private Location feedEndLocation;
	@Attribute
	private double feedRate;
	@Attribute
	private String actuatorId; 
	@Element(required=false)
	private Vision vision = new Vision();
	
	@Override
	public boolean canFeedForHead(Part part, Head head) {
		return true;
	}

	public Location feed(Head head_, Part part, Location pickLocation)
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

		// move to safe Z
		head.moveTo(head.getX(), head.getY(), 0, head.getC());

		// move the head so that the pin is positioned above the feed hole
		head.moveTo(feedStartLocation.getX(), feedStartLocation.getY(),
				head.getZ(), head.getC());

		// extend the pin
		actuator.actuate(true);

		// insert the pin
		head.moveTo(head.getX(), head.getY(), feedStartLocation.getZ(),
				head.getC());

		// drag the tape
		head.moveTo(feedEndLocation.getX(), feedEndLocation.getY(),
				feedEndLocation.getZ(), head.getC(), feedRate);

		// move to safe Z
		head.moveTo(head.getX(), head.getY(), 0, head.getC());

		// retract the pin
		actuator.actuate(false);

		return pickLocation;
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

	public static class Vision {
		@Attribute(required=false)
		private boolean enabled;
		@Attribute(required=false)
		private double tapeFeedHoleDiameter;
		
		@SuppressWarnings("unused")
		@Validate
		private void validate() throws Exception {
			if (enabled) {
				if (tapeFeedHoleDiameter == 0) {
					throw new PersistenceException("ReferenceTapeFeeder: tape-feed-hole-diameter is required if vision is enabled.");
				}
			}
		}
		
	}
}
