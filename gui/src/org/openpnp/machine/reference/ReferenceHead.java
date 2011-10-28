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

import org.openpnp.Location;
import org.openpnp.Part;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.w3c.dom.Node;

public class ReferenceHead implements Head {
	public static final int ACTUATOR_PIN = 0;

	private final double minX, maxX, homeX, minY, maxY, homeY, minZ, maxZ, homeZ, minC, maxC, homeC;

	private ReferenceMachine machine;
	private String reference;
	double x, y, z, c;
	
	public ReferenceHead() {
		minX = 0;
		maxX = 400;
		homeX = 0;
		
		minY = 0;
		maxY = 600;
		homeY = 0;
		
		minZ = 0;
		maxZ = 100;
		homeZ = 0;
		
		minC = 0;
		maxC = 360;
		homeC = 0;
	}
	
	public void configure(Node n) throws Exception {
		
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
		moveTo(x, y, homeZ, c);
		moveTo(homeX, homeY, homeZ, homeC);
	}

	@Override
	public void moveTo(double x, double y, double z, double c) throws Exception {
		machine.getDriver().moveTo(this, x, y, z, c);
		this.x = x;
		this.y = y;
		this.z = z;
		this.c = c;
	}
	
	public void updateCoordinates(double x, double y, double z, double c) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.c = c;
	}
	
	@Override
	public boolean canPickAndPlace(Feeder feeder, Location pickLocation,
			Location placeLocation) {
		return true;
	}
	
	@Override
	public void pick(Part part, Feeder feeder, Location pickLocation) throws Exception{
		// move to the pick location
		moveTo(pickLocation.getX(), pickLocation.getY(), z, pickLocation.getRotation());
		// lower the nozzle
		moveTo(x, y, pickLocation.getZ(), c);
		
		// pick the part
		machine.getDriver().pick(this, part);
	}

	@Override
	public void place(Part part, Location placeLocation) throws Exception {
		// move to the place location
		moveTo(placeLocation.getX(), placeLocation.getY(), z, placeLocation.getRotation());
		// lower the nozzle
		moveTo(x, y, placeLocation.getZ(), c);
		// place the part
		machine.getDriver().place(this);
	}
	
	public void actuate(int index, boolean on) throws Exception {
		machine.getDriver().actuate(this, index, on);
	}

	@Override
	public double getX() {
		return x;
	}

	@Override
	public double getY() {
		return y;
	}

	@Override
	public double getZ() {
		return z;
	}

	@Override
	public double getC() {
		return c;
	}
}
