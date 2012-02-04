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



import org.openpnp.gui.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Head;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;


/**
 * Implemention of Feeder that allows the head to index the current part and
 * then pick from a pre-specified position. It is intended that the Head is
 * carrying a pin of some type that can be extended past end of the tool to
 * index the tape. The steps this Feeder takes to feed a part are as follows:
 * Move head to Safe Z Move head to FeedStartLocation x, y Actuate ACTUATOR_PIN
 * Lower head to FeedStartLocation z Move head to FeedEndLocation x, y, z Move
 * head to Safe Z Retract ACTUATOR_PIN
 * 
 * <pre>
 * {@code
 * <!--
 * 	feedRate: Feed rate in machine units per minute for movement during the
 * 		drag operation.
 * -->
 * <Configuration feedRate="10">
 * 	<FeedStartLocation units="Millimeters" x="100" y="150" z="50" />
 * 	<FeedEndLocation units="Millimeters" x="102" y="150" z="50" />
 * </Configuration>
 * }
 * </pre>
 */
public class ReferenceTapeFeeder extends ReferenceFeeder {
	@Element
	private Location feedStartLocation;
	@Element
	private Location feedEndLocation;
	@Attribute
	private double feedRate;

	/**
	 * @wbp.parser.entryPoint
	 */
	@Override
	public boolean canFeedForHead(Part part, Head head) {
		return true;
	}

	public Location feed(Head head_, Part part, Location pickLocation)
			throws Exception {

		ReferenceHead head = (ReferenceHead) head_;

		// move to safe Z
		head.moveTo(head.getX(), head.getY(), 0, head.getC());

		// move the head so that the pin is positioned above the feed hole
		head.moveTo(feedStartLocation.getX(), feedStartLocation.getY(),
				head.getZ(), head.getC());

		// extend the pin
		head.actuate(ReferenceHead.PIN_ACTUATOR_NAME, true);

		// insert the pin
		head.moveTo(head.getX(), head.getY(), feedStartLocation.getZ(),
				head.getC());

		// drag the tape
		head.moveTo(feedEndLocation.getX(), feedEndLocation.getY(),
				feedEndLocation.getZ(), head.getC(), feedRate);

		// move to safe Z
		head.moveTo(head.getX(), head.getY(), 0, head.getC());

		// retract the pin
		head.actuate(ReferenceHead.PIN_ACTUATOR_NAME, false);

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

}
