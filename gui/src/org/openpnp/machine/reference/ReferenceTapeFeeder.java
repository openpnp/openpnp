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

package org.openpnp.machine.reference;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.openpnp.LengthUnit;
import org.openpnp.Location;
import org.openpnp.Part;
import org.openpnp.spi.Head;
import org.openpnp.util.LengthUtil;
import org.w3c.dom.Node;

/**
 * Implemention of Feeder that allows the head to index the current part and then
 * pick from a pre-specified position. It is intended that the Head is carrying
 * a pin of some type that can be extended past end of the tool to index the tape.
 * The steps this Feeder takes to feed a part are as follows:
 * Move head to Safe Z
 * Move head to FeedStartLocation x, y
 * Actuate ACTUATOR_PIN
 * Lower head to FeedStartLocation z
 * Move head to FeedEndLocation x, y, z
 * Move head to Safe Z
 * Retract ACTUATOR_PIN
 */
public class ReferenceTapeFeeder extends ReferenceFeeder {
	private Location feedStartLocation;
	private Location feedEndLocation;
	
	@Override
	public void configure(Node n) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();
		
		Node feedStartLocationNode = (Node) xpath.evaluate("FeedStartLocation", n, XPathConstants.NODE);

		feedStartLocation = new Location();
		feedStartLocation.parse(feedStartLocationNode);
		feedStartLocation = LengthUtil.convertLocation(feedStartLocation, LengthUnit.Millimeters);
		
		Node feedEndLocationNode = (Node) xpath.evaluate("FeedEndLocation", n, XPathConstants.NODE);

		feedEndLocation = new Location();
		feedEndLocation.parse(feedEndLocationNode);
		feedEndLocation = LengthUtil.convertLocation(feedEndLocation, LengthUnit.Millimeters);
	}
	
	@Override
	public boolean available() {
		return true;
	}

	public Location feed(Head head_, Part part, Location pickLocation) throws Exception {
		
		ReferenceHead head = (ReferenceHead) head_;
		
		// move to safe Z
		head.moveTo(head.getX(), head.getY(), 0, head.getC());
		
		// move the head so that the pin is positioned above the feed hole
		head.moveTo(feedStartLocation.getX(), feedStartLocation.getY(), head.getZ(), head.getC());
		
		// extend the pin
		head.actuate(ReferenceHead.ACTUATOR_PIN, true);
		
		// insert the pin
		head.moveTo(head.getX(), head.getY(), feedStartLocation.getZ(), head.getC());
		
		// drag the tape
		head.moveTo(feedEndLocation.getX(), feedEndLocation.getY(), feedEndLocation.getZ(), head.getC());
		
		// move to safe Z
		head.moveTo(head.getX(), head.getY(), 0, head.getC());
		
		// retract the pin
		head.actuate(ReferenceHead.ACTUATOR_PIN, false);
		
		return pickLocation;
	}
	
	@Override
	public String toString() {
		return String.format("ReferenceTapeFeeder reference %s", reference);
	}
}
