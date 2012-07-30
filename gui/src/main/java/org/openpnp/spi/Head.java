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

package org.openpnp.spi;

import java.util.List;

import org.openpnp.gui.support.Wizard;
import org.openpnp.model.Identifiable;
import org.openpnp.model.Location;
import org.openpnp.model.Part;


/**
 * A Head is a moving toolholder on a Machine. The head has a current position. A Head
 * is the movable object in a Machine.
 * Unless otherwise noted, the methods in this class block while performing their operations.
 */
public interface Head extends Identifiable {
	/**
	 * Directs the head to move to it's home position and set all it's axes to 0. 
	 */
	void home() throws Exception;
	
	/**
	 * Get the X position of the Head, with perception offsets applied.
	 * @return
	 */
	public double getX();
	/**
	 * Get the Y position of the Head, with perception offsets applied.
	 * @return
	 */
	public double getY();
	/**
	 * Get the Z position of the Head, with perception offsets applied.
	 * @return
	 */
	public double getZ();
	/**
	 * Get the C position of the Head, with perception offsets applied.
	 * @return
	 */
	public double getC();

	public void setPerceivedX(double x);
	public void setPerceivedY(double y);
	public void setPerceivedZ(double z);
	public void setPerceivedC(double c);
	
	/**
	 * Get the X position of the Head, without perception offsets applied.
	 * @return
	 */
	public double getAbsoluteX();
	/**
	 * Get the Y position of the Head, without perception offsets applied.
	 * @return
	 */
	public double getAbsoluteY();
	/**
	 * Get the Z position of the Head, without perception offsets applied.
	 * @return
	 */
	public double getAbsoluteZ();
	/**
	 * Get the C position of the Head, without perception offsets applied.
	 * @return
	 */
	public double getAbsoluteC();
	
	/**
	 * Move the Head to the given position. Values are in Machine native units. Heads are not
	 * required to make the movement in any particular order.
	 * @param x
	 * @param y
	 * @param z
	 * @param a
	 */
	void moveTo(double x, double y, double z, double c) throws Exception;

	/**
	 * Move the Head to the given position. Values are in Machine native units. Heads are not
	 * required to make the movement in any particular order.
	 * @param x
	 * @param y
	 * @param z
	 * @param a
	 * @param feedRatePerMinute
	 */
	void moveTo(double x, double y, double z, double c, double feedRatePerMinute) throws Exception;
	
	/**
	 * Move the head to the Safe-Z location. By default this is 0 but can be
	 * overridden on a per Head basis. This causes movement in the Z axis only.
	 * All other axes remain the same.
	 */
	void moveToSafeZ() throws Exception;

	/**
	 * Queries the Head to determine if it has the ability to pick from the given Feeder
	 * at the given Location and then move the Part to the destination Location.
	 * @param feeder
	 * @param pickLocation
	 * @param placeLocation
	 * @return
	 */
	public boolean canPickAndPlace(Feeder feeder, Location pickLocation, Location placeLocation);
	
	/**
	 * Commands the Head to perform it's pick operation. Generally this just
	 * consists of turning on the vacuum. This method call is only used by
	 * manual user process. During Job processing the 
	 * pick(Part, Feeder, Location) method will be called.
	 * @throws Exception
	 */
	public void pick() throws Exception;
	
	/**
	 * Commands the Head to pick the Part from the Feeder using the given Location. In general,
	 * this operation should move the nozzle to the specified Location and turn on the
	 * vacuum. Before this operation is called the Feeder has already been commanded to feed the
	 * Part.
	 * @param part
	 * @param feeder
	 * @param pickLocation
	 * @throws Exception
	 */
	public void pick(Part part, Feeder feeder, Location pickLocation) throws Exception;
	
	/**
	 * Commands the Head to perform it's place operation. Generally this just consists
	 * of releasing vacuum and may include a puff of air to set the Part. This method
	 * is only used by manual user process. During Job processing the place(Part, Location)
	 * method will be used. 
	 * @throws Exception
	 */
	public void place() throws Exception;
	
	/**
	 * Commands the Head to place the given Part at the specified Location. In general,
	 * this operation should move the nozzle to the specified Location and turn off the
	 * vacuum.
	 * @param part
	 * @param placeLocation
	 * @throws Exception
	 */
	public void place(Part part, Location placeLocation) throws Exception;
	
	/**
	 * Get a list of Actuators that are attached to this head.
	 * @return
	 */
	public List<Actuator> getActuators();
	
	public Actuator getActuator(String id);
	
	/**
	 * Get a Wizard that can be used to configure this Head.
	 * @return
	 */
	public Wizard getConfigurationWizard();
	
	@Override
	public String getId();
	
	public Machine getMachine();
}
