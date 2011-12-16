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

import org.openpnp.LengthUnit;
import org.openpnp.Location;
import org.openpnp.Part;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.util.LengthUtil;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
<pre>
{@code
<!--
	pickDwellMilliseconds: number of milliseconds to dwell after applying vacuum
		for a pick operation and before moving off the part.
	feedRate: Feed rate in machine units per minute for movement operations.
-->
<Configuration pickDwellMilliseconds="200" feedRate="15000">
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
	private double x, y, z, c;
	private double offsetX, offsetY, offsetZ, offsetC;
	
	@Attribute
	private String id;
	@Attribute
	private int pickDwellMilliseconds;
	@Attribute
	private double feedRate;
	@Element
	private SoftLimits softLimits = new SoftLimits();
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	public void setReferenceMachine(ReferenceMachine machine) throws Exception {
		this.machine = machine;
	}

	@Override
	public void home() throws Exception {
		moveTo(getX(), getY(), 0, getC());
		moveTo(0, 0, 0, 0);
	}
	
	@Override
	public void moveTo(double x, double y, double z, double c) throws Exception {
		moveTo(x, y, z, c, feedRate);
	}

	@Override
	public void moveTo(double x, double y, double z, double c, double feedRatePerMinute) throws Exception {
		if (x < softLimits.minX || x > softLimits.maxX ||
				y < softLimits.minY || y > softLimits.maxY ||
				z < softLimits.minZ || z > softLimits.maxZ ||
				c < softLimits.minC || c > softLimits.maxC) {
			throw new Exception(String.format("Movement to %2.4f, %2.4f, %2.4f, %2.4f would violate soft limits of (%2.4f, %2.4f), (%2.4f, %2.4f), (%2.4f, %2.4f), (%2.4f, %2.4f).", 
					x, y, z, c,
					softLimits.minX, softLimits.maxX,
					softLimits.minY, softLimits.maxY,
					softLimits.minZ, softLimits.maxZ,
					softLimits.minC, softLimits.maxC));
		}
		double feedRateMmPerMinute = LengthUtil.convertLength(feedRatePerMinute, machine.getNativeUnits(), LengthUnit.Millimeters);
		machine.getDriver().moveTo(this, x + offsetX, y + offsetY, z + offsetZ, c + offsetC, feedRateMmPerMinute);
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
		Thread.sleep(pickDwellMilliseconds);
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
	
	static class SoftLimits {
		@Attribute
		private double minX = Double.NEGATIVE_INFINITY;
		@Attribute
		private double maxX = Double.POSITIVE_INFINITY; 
		@Attribute
		private double minY = Double.NEGATIVE_INFINITY;
		@Attribute
		private double maxY = Double.POSITIVE_INFINITY; 
		@Attribute
		private double minZ = Double.NEGATIVE_INFINITY;
		@Attribute
		private double maxZ = Double.POSITIVE_INFINITY; 
		@Attribute
		private double minC = Double.NEGATIVE_INFINITY;
		@Attribute
		private double maxC = Double.POSITIVE_INFINITY; 
	}
}
