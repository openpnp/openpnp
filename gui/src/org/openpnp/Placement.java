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

package org.openpnp;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.openpnp.Board.Side;
import org.w3c.dom.Node;


/**
 * A Placement describes a location on a Board where a Part will be placed, along with information about how to place it. 
 * @author jason
 */
public class Placement {
	private String id;
	private Part part;
	private Location location;
	private Side side;
	
	public void parse(Node n, Configuration c) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();

		id = Configuration.getAttribute(n, "reference");
		side = Side.valueOf(Configuration.getAttribute(n, "side", "Top"));
		part = c.getPart(Configuration.getAttribute(n, "part"));
		location = new Location();
		location.parse((Node) xpath.evaluate("Location", n, XPathConstants.NODE));
	}
	
	public String getReference() {
		return id;
	}

	public void setReference(String reference) {
		this.id = reference;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public Part getPart() {
		return part;
	}
	
	public void setPart(Part part) {
		this.part = part;
	}
	
	public Side getSide() {
		return side;
	}

	public void setSide(Side side) {
		this.side = side;
	}

	@Override
	public String toString() {
		return String.format("id %s, side %s, part (%s), location (%s)", id, side, part, location);
	}
}
