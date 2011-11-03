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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.openpnp.Configuration;
import org.openpnp.LengthUnit;
import org.openpnp.Location;
import org.openpnp.Part;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.spi.Head;
import org.openpnp.util.LengthUtil;
import org.w3c.dom.Node;

/**
 * Implemention of Feeder that indexes based on an offset. This allows a tray of parts to be picked from without moving any tape.
 * Can handle trays of arbitrary X and Y count.
	<pre>
		<Configuration trayCountX="10" trayCountY="2">
			<Offsets units="Millimeters" x="10" y="10" z="0" rotation="0"/>
		</Configuration>
	</pre>
 */
public class ReferenceTrayFeeder extends ReferenceFeeder {
	private int trayCountX;
	private int trayCountY;
	private Location offsets;  
	
	private int pickCount;
	
	@Override
	public void configure(Node n) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();
		
		trayCountX = (int) Configuration.getDoubleAttribute(n, "trayCountX");
		trayCountY = (int) Configuration.getDoubleAttribute(n, "trayCountY");
		
		Node offsetsNode = (Node) xpath.evaluate("Offsets", n, XPathConstants.NODE);

		offsets = new Location();
		offsets.parse(offsetsNode);
		offsets = LengthUtil.convertLocation(offsets, LengthUnit.Millimeters);
	}
	
	@Override
	public boolean available() {
		return (pickCount < (trayCountX * trayCountY));
	}

	public Location feed(Head head_, Part part, Location pickLocation) throws Exception {
		ReferenceHead head = (ReferenceHead) head_;
		
		int partX = (pickCount / trayCountX);
		int partY = (pickCount - (partX * trayCountX));
		
		Location l = new Location();
		l.setX(pickLocation.getX() + (partX * offsets.getX()));
		l.setY(pickLocation.getY() + (partY * offsets.getY()));
		l.setZ(pickLocation.getZ());
		l.setRotation(pickLocation.getRotation());
		l.setUnits(pickLocation.getUnits());
		
		System.out.println(String.format("Feeding part # %d, x %d, y %d, xPos %f, yPos %f", pickCount, partX, partY, l.getX(), l.getY()));
		
		pickCount++;
		
		return l; 
	}
}
