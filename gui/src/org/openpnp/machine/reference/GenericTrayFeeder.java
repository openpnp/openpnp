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
 * Implemention of Feeder that indexes based on an offset. This allows a tray of parts to be picked from without moving any tape. 
 */
public class GenericTrayFeeder extends GenericFeeder {
	private Location location;
	// TODO will need to know the part's orientation, specifications about the tape, etc
	private int pickCount;
	private double offset = -10;
	
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
		
		Location l = new Location();
		l.setX(pickLocation.getX() + (pickCount * offset));
		l.setY(pickLocation.getY());
		l.setZ(pickLocation.getZ());
		l.setRotation(pickLocation.getRotation());
		l.setUnits(pickLocation.getUnits());
		pickCount++;
		return l; 
	}
	
	@Override
	public String toString() {
		return String.format("GenericTapeFeeder reference %s, location %s", reference, location);
	}
}
