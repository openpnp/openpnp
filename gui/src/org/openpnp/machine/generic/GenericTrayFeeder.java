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
