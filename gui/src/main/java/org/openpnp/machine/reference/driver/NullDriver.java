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

import java.util.HashMap;

import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Head;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An example of the simplest possible driver that can support multiple heads.
 * This driver maintains a set of coordinates for each Head that it is asked
 * to handle and simply logs all commands sent to it.
 */
public class NullDriver implements ReferenceDriver {
	private final static Logger logger = LoggerFactory.getLogger(NullDriver.class);
	
	@Attribute(required=false)
	private double feedRateMmPerMinute = 250;
	
	private HashMap<Head, Location> headLocations = new HashMap<Head, Location>();
	
	protected Location getHeadLocation(Head head) {
	    Location l = headLocations.get(head);
	    if (l == null) {
	        l = new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
	        headLocations.put(head, l);
	    }
	    return l;
	}

	@Override
	public void home(ReferenceHead head)
			throws Exception {
		logger.info("home()");
		Location l = getHeadLocation(head);
        l.setX(0);
        l.setY(0);
        l.setZ(0);
        l.setRotation(0);
	}
	
	@Override
    public Location getLocation(ReferenceHeadMountable hm) {
	    return getHeadLocation(hm.getHead()).add(hm.getHeadOffsets());
    }

    @Override
    public void moveTo(ReferenceHeadMountable hm, Location location, double speed) throws Exception {
        logger.info("moveTo({}, {}, {})", new Object[] { hm, location, speed });
        
        location = location.subtract(hm.getHeadOffsets());

        location = location.convertToUnits(LengthUnit.Millimeters);
        
        Location hl = getHeadLocation(hm.getHead());

        simulateMovement(hm, location, hl, speed);
        
        if (!Double.isNaN(location.getX())) {
            hl.setX(location.getX());
        }
        if (!Double.isNaN(location.getY())) {
            hl.setY(location.getY());
        }
        if (!Double.isNaN(location.getZ())) {
            hl.setZ(location.getZ());
        }
        if (!Double.isNaN(location.getRotation())) {
            hl.setRotation(location.getRotation());
        }
    }
    
    /**
     * Simulates true machine movement, which takes time, by tracing the
     * required movement lines over a period of time based on the input speed. 
     * @param hm
     * @param location
     * @param hl
     * @param speed
     * @throws Exception
     */
    private void simulateMovement(ReferenceHeadMountable hm, Location location, Location hl, double speed) throws Exception {
        double x = hl.getX();
        double y = hl.getY();
        double z = hl.getZ();
        double c = hl.getRotation();
        
        double x1 = x;
        double y1 = y;
        double z1 = z;
        double c1 = c;
        double x2 = Double.isNaN(location.getX()) ? x : location.getX();
        double y2 = Double.isNaN(location.getY()) ? y : location.getY();
        double z2 = Double.isNaN(location.getZ()) ? z : location.getZ();
        double c2 = Double.isNaN(location.getRotation()) ? c : location.getRotation();

        c2 = c2 % 360.0;
        
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
        double distancePerTick = (feedRateMmPerMinute * speed) / 60.0 / 10.0;
        
        while (dxy < pxy || dz < pz || dc < pc) {
            if (dxy < pxy) {
                x = x1 + (vx / pxy * dxy);
                y = y1 + (vy / pxy * dxy);
            }
            else {
                x = x1;
                y = y1;
            }
            if (dz < pz) {
                z = z1 + dz * (vz < 0 ? -1 : 1);
            }
            else {
                z = z1;
            }
            if (dc < pc) {
                c = c1 + dc * (vc < 0 ? -1 : 1);
            }
            else {
                c = c1;
            }

            hl.setX(x);
            hl.setY(y);
            hl.setZ(z);
            hl.setRotation(c);
            
            // Provide live updates to the Machine as the move progresses.
            ((ReferenceMachine) Configuration.get().getMachine()).fireMachineHeadActivity(hm.getHead());
            
            try {
                Thread.sleep(100);
            }
            catch (Exception e) {
                
            }
            
            dxy = Math.min(pxy, dxy + distancePerTick);
            dz = Math.min(pz, dz + distancePerTick);
            dc = Math.min(pc, dc + distancePerTick);
        }
    }

	@Override
	public void pick(ReferenceNozzle nozzle) throws Exception {
		logger.info("pick({})", nozzle);
		Thread.sleep(500);
	}

	@Override
	public void place(ReferenceNozzle nozzle) throws Exception {
		logger.info("place({})", nozzle);
		Thread.sleep(500);
	}

    @Override
    public void actuate(ReferenceActuator actuator, double value)
            throws Exception {
        logger.info("actuate({}, {})", actuator, value);
        Thread.sleep(500);
    }

	@Override
	public void actuate(ReferenceActuator actuator, boolean on)
			throws Exception {
		logger.info("actuate({}, {})", actuator, on);
		Thread.sleep(500);
	}

	@Override
	public void setEnabled(boolean enabled) throws Exception {
		logger.info("setEnabled({})", enabled);
	}
}
