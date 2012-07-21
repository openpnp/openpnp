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
	private String dummy;
	
	private double x, y, z, c;

	@Override
	public void home(ReferenceHead head, double feedRateMmPerMinute)
			throws Exception {
		logger.info("home()");
	}

	@Override
	public void moveTo(ReferenceHead head, double x, double y, double z,
			double c, double feedRateMmPerMinute) throws Exception {
		logger.info(String.format("moveTo(%f, %f, %f, %f, %f)", x, y, z, c, feedRateMmPerMinute));

		// If the angle is more than 360* we take it's modulo. No reason to
		// travel more than a full circle.
		c = c % 360.0;
		
//		// If the travel is more than 180* we go the opposite direction instead.
//		if (c > 180) {
//			c = (360 - c) * -1;
//		}
		
		double x1 = this.x;
		double y1 = this.y;
		double z1 = this.z;
		double c1 = this.c;
		double x2 = x;
		double y2 = y;
		double z2 = z;
		double c2 = c;
		
		// Calculate the linear distance to travel in each axis.
		double vx = x2 - x1;
		double vy = y2 - y1;
		double vz = z2 - z1;
		double vc = c2 - c1;
		
		// Calculate the linear distance to travel in each plane XY, Z and C.
		double pxy = Math.sqrt(vx*vx + vy*vy);
		double pz = Math.abs(vz);
		double pc = Math.abs(vc); 

		// Distance moved in each plane so far.
		double dxy = 0, dz = 0, dc = 0;
		
		// The distance that we'll move each loop. 
		double distancePerTick = feedRateMmPerMinute / 60.0 / 10.0;
		
		while (dxy < pxy || dz < pz || dc < pc) {
			if (dxy < pxy) {
				this.x = x1 + (vx / pxy * dxy);
				this.y = y1 + (vy / pxy * dxy);
			}
			else {
				this.x = x;
				this.y = y;
			}
			if (dz < pz) {
				this.z = z1 + dz * (vz < 0 ? -1 : 1);
			}
			else {
				this.z = z;
			}
			if (dc < pc) {
				this.c = c1 + dc * (vc < 0 ? -1 : 1);
			}
			else {
				this.c = c;
			}
			
			head.updateDuringMoveTo(this.x, this.y, this.z, this.c);
			
			try {
				Thread.sleep(100);
			}
			catch (Exception e) {
				
			}
			
			dxy = Math.min(pxy, dxy + distancePerTick);
			dz = Math.min(pz, dz + distancePerTick);
			dc = Math.min(pc, dc + distancePerTick);
		}
		
		this.x = x;
		this.y = y;
		this.z = z;
		this.c = c;

		head.updateDuringMoveTo(this.x, this.y, this.z, this.c);
	}

	@Override
	public void pick(ReferenceHead head, Part part) throws Exception {
		logger.info(String.format("pick(%s)", part == null ? "" : part.toString()));
		Thread.sleep(500);
	}

	@Override
	public void place(ReferenceHead head) throws Exception {
		logger.info(String.format("place()"));
		Thread.sleep(500);
	}

	@Override
	public void actuate(ReferenceHead head, int index, boolean on)
			throws Exception {
		logger.info(String.format("actuate(%d, %s)", index, on));
		Thread.sleep(500);
	}

	@Override
	public void setEnabled(boolean enabled) throws Exception {
		logger.info("setEnabled({})", enabled);
	}
}
