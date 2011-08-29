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

package org.openpnp.machine.generic;

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
 * pick from a pre-specified position.
 */
public class GenericTapeFeeder extends GenericFeeder {
	private Location location;
	// TODO will need to know the part's orientation, specifications about the tape, etc
	
	@Override
	public void configure(Node n) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();
		
		Node locationNode = (Node) xpath.evaluate("Location", n, XPathConstants.NODE);

		location = new Location();
		location.parse(locationNode);
		location = LengthUtil.convertLocation(location, LengthUnit.Millimeters);
	}
	
	@Override
	public boolean available() {
		return true;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}
	
	public Location feed(Head head_, Part part, Location pickLocation) throws Exception {
		
		GenericHead head = (GenericHead) head_;
		
		// TODO need to take into consideration the height of the tape, length of pin and that we don't drag anything across
		// the table
		
		// move the head so that the pin is positioned above the feed hole
		// TODO This rotation should be based on the orientation of the feeder
		head.moveTo(pickLocation.getX() + 2, pickLocation.getY() - 2, head.getZ(), 0);
		// extend the pin
		head.actuate(GenericHead.ACTUATOR_PIN, true);
		// insert the pin
		head.moveTo(head.getX(), head.getY(), pickLocation.getZ(), head.getA());
		// drag the tape
		head.moveTo(pickLocation.getX() + 2 + 2, pickLocation.getY() - 2, pickLocation.getZ(), head.getA());
		// retract the pin
		head.actuate(GenericHead.ACTUATOR_PIN, false);
		
		return pickLocation;
	}
	
	@Override
	public String toString() {
		return String.format("GenericTapeFeeder reference %s, location %s", reference, location);
	}
}
