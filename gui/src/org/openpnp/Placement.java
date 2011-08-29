package org.openpnp;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;


/**
 * A Placement describes a location on a Board where a Part will be placed, along with information about how to place it. 
 * @author jason
 */
public class Placement {
	private String reference;
	private Part part;
	private Location location;
	/**
	 * True if the Part is on the bottom of the Board. False if it's on the top. 
	 */
	private boolean onBottom;
	
	public void parse(Node n, Configuration c) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();

		reference = Configuration.getAttribute(n, "reference");
		onBottom = Boolean.parseBoolean(Configuration.getAttribute(n, "bottom", "false"));
		part = c.getPart(Configuration.getAttribute(n, "part"));
		location = new Location();
		location.parse((Node) xpath.evaluate("Location", n, XPathConstants.NODE));
	}
	
	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
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
	
	boolean isOnBottom() {
		return onBottom;
	}

	void setOnBottom(boolean onBottom) {
		this.onBottom = onBottom;
	}

	@Override
	public String toString() {
		return String.format("reference %s, onBottom %s, part (%s), location (%s)", onBottom, reference, part, location);
	}
}
