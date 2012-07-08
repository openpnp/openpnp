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

package org.openpnp.machine.reference.driver;

import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.model.Part;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NullDriver implements ReferenceDriver {
	private final static Logger logger = LoggerFactory.getLogger(NullDriver.class);
	
	@Attribute(required=false)
	private String nullDriver;
	
	private double x, y, z, c;

	@Override
	public void home(ReferenceHead head, double feedRateMmPerMinute)
			throws Exception {
	}

	@Override
	public void moveTo(ReferenceHead head, double x, double y, double z,
			double c, double feedRateMmPerMinute) throws Exception {
		logger.info(String.format("moveTo(%f, %f, %f, %f, %f)", x, y, z, c, feedRateMmPerMinute));

		// angles over 360* are silly
		c = c % 360.0;
		
		// if the travel is more than 180* we go the opposite direction
		if (c > 180) {
			c = (360 - c) * -1;
		}
		
		double x1 = this.x;
		double y1 = this.y;
		double z1 = this.z;
		double c1 = this.c;
		double x2 = x;
		double y2 = y;
		double z2 = z;
		double c2 = c;
		
		// distances to travel in each axis
		double vx = x2 - x1;
		double vy = y2 - y1;
		double vz = z2 - z1;
		double va = c2 - c1;
		
		double mag = Math.sqrt(vx*vx + vy*vy);

		double distance = 0;
		
		double distancePerTick = feedRateMmPerMinute / 60.0 / 10.0;
		
		while (distance < mag) {
			this.x = x1 + (vx / mag * distance);
			this.y = y1 + (vy / mag * distance);
			this.c = c1 + (va / mag * distance);
			
			head.updateDuringMoveTo(this.x, this.y, this.z, this.c);
			
			try {
				Thread.sleep(100);
			}
			catch (Exception e) {
				
			}
			
			distance = Math.min(mag, distance + distancePerTick);
		}
		
		this.x = x;
		this.y = y;
		this.c = c;
		
		head.updateDuringMoveTo(this.x, this.y, this.z, this.c);
		
		mag = Math.abs(vz);
		
		distance = 0;
		
		while (distance < mag) {
			this.z = z1 + (vz / mag * distance);

			head.updateDuringMoveTo(this.x, this.y, this.z, this.c);

			try {
				Thread.sleep(100);
			}
			catch (Exception e) {
				
			}
			
			distance = Math.min(mag, distance + distancePerTick);
		}
		
		this.z = z;

		head.updateDuringMoveTo(this.x, this.y, this.z, this.c);
	}

	@Override
	public void pick(ReferenceHead head, Part part) throws Exception {
		logger.info(String.format("pick(%s)", part.toString()));
	}

	@Override
	public void place(ReferenceHead head) throws Exception {
		logger.info(String.format("place()"));
	}

	@Override
	public void actuate(ReferenceHead head, int index, boolean on)
			throws Exception {
		logger.info(String.format("actuate(%d, %s)", index, on));
	}

	@Override
	public void setEnabled(boolean enabled) throws Exception {
	}
}
