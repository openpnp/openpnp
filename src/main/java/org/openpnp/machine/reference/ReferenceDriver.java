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

import java.io.Closeable;

import org.openpnp.model.Location;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.WizardConfigurable;

/**
 * Defines the interface for a simple driver that the ReferenceMachine can
 * drive. All methods result in machine operations and all methods should block
 * until they are complete or throw an error.
 * 
 * This Driver interface is intended to model a machine with one or more Heads,
 * and each Head having one or more Nozzles and zero or more Cameras and
 * Actuators.
 * 
 * In OpenPnP, the Head does not move on it's own. It is moved by the moving of
 * attached objects: Nozzles, Cameras, Actuators. For this reason, all movements
 * on the driver are specified as movements by one of these objects. This allows
 * the driver to make decisions as to what axes should be moved to accomplish a
 * specific task.
 */
public interface ReferenceDriver extends WizardConfigurable, PropertySheetHolder, Closeable {
    /**
     * Performing the hardware homing operation for the given Head. When this
     * call completes the Head should be at it's 0,0,0,0 position.
     * 
     * @throws Exception
     */
    public void home(ReferenceHead head) throws Exception;

    /**
     * Moves the specified HeadMountable to the given location at a speed
     * defined by (maximum feed rate * speed) where speed is greater than 0 and
     * typically less than or equal to 1. A speed of 0 means to move at the
     * minimum possible speed.
     * 
     * HeadMountable object types include Nozzle, Camera and Actuator.
     * 
     * @param hm
     * @param location
     * @param speed
     * @throws Exception
     */
    public void moveTo(ReferenceHeadMountable hm, Location location, double speed)
            throws Exception;
    
    /**
     * Returns a clone of the HeadMountable's current location. It's important
     * that the returned object is a clone, since the caller may modify the
     * returned Location.
     * @param hm
     * @return
     */
    public Location getLocation(ReferenceHeadMountable hm);

    /**
     * Causes the nozzle to apply vacuum and any other operation that it uses
     * for picking up a part that it is resting on.
     * 
     * @param nozzle
     * @throws Exception
     */
    public void pick(ReferenceNozzle nozzle) throws Exception;

    /**
     * Causes the nozzle to release vacuum and any other operation that it uses
     * for placing a part that it is currently holding. For instance, it might
     * provide a brief puff of air to set the part.
     * 
     * @throws Exception
     */
    public void place(ReferenceNozzle nozzle) throws Exception;

    /**
     * Actuates a machine defined object with a boolean state.
     * 
     * @param actuator
     * @param on
     * @throws Exception
     */
    public void actuate(ReferenceActuator actuator, boolean on) throws Exception;

    /**
     * Actuates a machine defined object with a double value.
     * 
     * @param actuator
     * @param on
     * @throws Exception
     */
    public void actuate(ReferenceActuator actuator, double value) throws Exception;

    /**
     * Attempts to enable the Driver, turning on all outputs.
     * 
     * @param enabled
     * @throws Exception
     */
    public void setEnabled(boolean enabled) throws Exception;
    
    public void dispense(ReferencePasteDispenser dispenser, Location startLocation, Location endLocation, long dispenseTimeMilliseconds) throws Exception;
}
