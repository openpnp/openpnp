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

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A Board describes the physical properties of a PCB and has a list of Placements that will be used to
 * specify pick and place operations. 
 */
public class Board {
	private String reference;
	private Outline outline; 
	private List<Fiducial> fiducials = new ArrayList<Fiducial>();
	private List<Placement> placements = new ArrayList<Placement>();
	
	public void parse(Node n, Configuration c) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();

		reference = Configuration.getAttribute(n, "reference");
		outline = new Outline();
		outline.parse((Node) xpath.evaluate("Outline", n, XPathConstants.NODE));
		
		NodeList placementNodes = (NodeList) xpath.evaluate("Placements/Placement", n, XPathConstants.NODESET);
		for (int i = 0; i < placementNodes.getLength(); i++) {
			Node placementNode = placementNodes.item(i);
			Placement placement = new Placement();
			placement.parse(placementNode, c);
			placements.add(placement);
		}
	}
	
	public List<Fiducial> getFiducials() {
		return fiducials;
	}
	
	public void setFiducials(List<Fiducial> fiducials) {
		this.fiducials = fiducials;
	}
	
	public List<Placement> getPlacements() {
		return placements;
	}
	
	public void setPlacements(List<Placement> placements) {
		this.placements = placements;
	}

	public Outline getOutline() {
		return outline;
	}

	public void setOutline(Outline outline) {
		this.outline = outline;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}
	
	@Override
	public String toString() {
		return String.format("reference %s, placements (%s)", reference, placements);
	}
}
