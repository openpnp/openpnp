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

import java.util.Collections;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.openpnp.Configuration;
import org.openpnp.Location;
import org.openpnp.Part;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.w3c.dom.Node;

/**
<pre>
{@code
<Configuration>
	<SoftLimits
		units="Millimeters"
		xMinimum="0" xMaximum="400" 
		yMinimum="0" yMaximum="600"  
		zMinimum="0" zMaximum="100"  
		cMinimum="0" cMaximum="180" 
	/> 
</Configuration>
}
</pre>
 */
public class ReferenceHead implements Head {
	public static final String PIN_ACTUATOR_NAME = "Pin";
	
	private ReferenceMachine machine;
	private String reference;
	double x, y, z, c;
	double offsetX, offsetY, offsetZ, offsetC;
	double 
		softMinX = Double.NEGATIVE_INFINITY, softMaxX = Double.POSITIVE_INFINITY, 
		softMinY = Double.NEGATIVE_INFINITY, softMaxY = Double.POSITIVE_INFINITY, 
		softMinZ = Double.NEGATIVE_INFINITY, softMaxZ = Double.POSITIVE_INFINITY, 
		softMinC = Double.NEGATIVE_INFINITY, softMaxC = Double.POSITIVE_INFINITY; 
	
	public ReferenceHead() {
	}
	
	public void configure(Node n) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();
		
		Node softLimitsNode = (Node) xpath.evaluate("SoftLimits", n, XPathConstants.NODE);
		
		if (softLimitsNode != null) {
			softMinX = Configuration.getDoubleAttribute(softLimitsNode, "xMinimum", Double.NEGATIVE_INFINITY);
			softMaxX = Configuration.getDoubleAttribute(softLimitsNode, "xMaximum", Double.POSITIVE_INFINITY);
			softMinY = Configuration.getDoubleAttribute(softLimitsNode, "yMinimum", Double.NEGATIVE_INFINITY);
			softMaxY = Configuration.getDoubleAttribute(softLimitsNode, "yMaximum", Double.POSITIVE_INFINITY);
			softMinZ = Configuration.getDoubleAttribute(softLimitsNode, "zMinimum", Double.NEGATIVE_INFINITY);
			softMaxZ = Configuration.getDoubleAttribute(softLimitsNode, "zMaximum", Double.POSITIVE_INFINITY);
			softMinC = Configuration.getDoubleAttribute(softLimitsNode, "cMinimum", Double.NEGATIVE_INFINITY);
			softMaxC = Configuration.getDoubleAttribute(softLimitsNode, "cMaximum", Double.POSITIVE_INFINITY);
		}
	}
	
	
	public void setMachine(ReferenceMachine machine) {
		this.machine = machine;
	}
	
	public void setReference(String reference) {
		this.reference = reference;
	}
	
	public String getReference() {
		return reference;
	}

	@Override
	public void home() throws Exception {
//		moveTo(x, y, homeZ, c);
//		moveTo(homeX, homeY, homeZ, homeC);
	}

	@Override
	public void moveTo(double x, double y, double z, double c) throws Exception {
		if (x < softMinX || x > softMaxX ||
				y < softMinY || y > softMaxY ||
				z < softMinZ || z > softMaxZ ||
				c < softMinC || c > softMaxC) {
			throw new Exception(String.format("Movement to %2.4f, %2.4f, %2.4f, %2.4f would violate soft limits of (%2.4f, %2.4f), (%2.4f, %2.4f), (%2.4f, %2.4f), (%2.4f, %2.4f).", 
					x, y, z, c,
					softMinX, softMaxX,
					softMinY, softMaxY,
					softMinZ, softMaxZ,
					softMinC, softMaxC));
		}
		machine.getDriver().moveTo(this, x + offsetX, y + offsetY, z + offsetZ, c + offsetC);
		this.x = x + offsetX;
		this.y = y + offsetY;
		this.z = z + offsetZ;
		this.c = c + offsetC;
		machine.fireMachineHeadActivity(machine, this);
	}
	
	@Override
	public boolean canPickAndPlace(Feeder feeder, Location pickLocation,
			Location placeLocation) {
		return true;
	}
	
	@Override
	public void pick() throws Exception {
		machine.getDriver().pick(this, null);
		machine.fireMachineHeadActivity(machine, this);
	}

	@Override
	public void pick(Part part, Feeder feeder, Location pickLocation) throws Exception{
		// move to the pick location
		moveTo(pickLocation.getX(), pickLocation.getY(), getZ(), pickLocation.getRotation());
		// lower the nozzle
		moveTo(getX(), getY(), pickLocation.getZ(), getC());
		
		// pick the part
		machine.getDriver().pick(this, part);
		machine.fireMachineHeadActivity(machine, this);
	}

	@Override
	public void place() throws Exception {
		machine.getDriver().place(this);
		machine.fireMachineHeadActivity(machine, this);
	}

	@Override
	public void place(Part part, Location placeLocation) throws Exception {
		// move to the place location
		moveTo(placeLocation.getX(), placeLocation.getY(), getZ(), placeLocation.getRotation());
		// lower the nozzle
		moveTo(getX(), getY(), placeLocation.getZ(), getC());
		// place the part
		machine.getDriver().place(this);
		machine.fireMachineHeadActivity(machine, this);
	}
	
	@Override
	public List<String> getActuatorNames() {
		return Collections.singletonList(PIN_ACTUATOR_NAME);
	}

	@Override
	public void actuate(String actuator, boolean on) throws Exception {
		if (actuator.equals(PIN_ACTUATOR_NAME)) {
			machine.getDriver().actuate(this, 0, on);
			machine.fireMachineHeadActivity(machine, this);
		}
		else {
			throw new Exception("Unrecognized actuator: " + actuator);
		}
	}

	@Override
	public double getX() {
		return x - offsetX;
	}

	@Override
	public double getY() {
		return y - offsetY;
	}

	@Override
	public double getZ() {
		return z - offsetZ;
	}

	@Override
	public double getC() {
		return c - offsetC;
	}

	@Override
	public void setPerceivedX(double perceivedX) {
		offsetX = x - perceivedX;
		machine.fireMachineHeadActivity(machine, this);
	}

	@Override
	public void setPerceivedY(double perceivedY) {
		offsetY = y - perceivedY;
		machine.fireMachineHeadActivity(machine, this);
	}

	@Override
	public void setPerceivedZ(double perceivedZ) {
		offsetZ = z - perceivedZ;
		machine.fireMachineHeadActivity(machine, this);
	}

	@Override
	public void setPerceivedC(double perceivedC) {
		offsetC = c - perceivedC;
		machine.fireMachineHeadActivity(machine, this);
	}

	@Override
	public double getAbsoluteX() {
		return x;
	}

	@Override
	public double getAbsoluteY() {
		return y;
	}

	@Override
	public double getAbsoluteZ() {
		return z;
	}

	@Override
	public double getAbsoluteC() {
		return c;
	}
}
