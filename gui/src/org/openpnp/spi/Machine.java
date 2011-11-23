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

import org.openpnp.Configuration;
import org.openpnp.Job;
import org.openpnp.LengthUnit;


/**
 * Machine represents the pick and place machine itself. It provides the information and interface needed to
 * cause the machine to do work. A Machine has one or more Heads.
 * Unless otherwise noted, the methods in this class block while performing their operations.
 */
public interface Machine {
	/**
	 * The units used to describe the machine's measurements.
	 * @return
	 */
	LengthUnit getNativeUnits();
	
	/**
	 * Gets all active heads on the machine.
	 * @return
	 */
	List<Head> getHeads();
	
	/**
	 * Gets the Feeder defined with the specified reference.
	 * @param reference
	 * @return
	 */
	Feeder getFeeder(String reference);
	
	List<Camera> getCameras();
	
	/**
	 * Commands all Heads to move to their home positions and reset their current positions
	 * to 0,0,0,0. Depending on the head configuration of the machine the home positions may
	 * not all be the same but the end result should be that any head commanded to move
	 * to a certain position will end up in the same position.
	 */
	void home() throws Exception;
	
	// TODO: probably remove
	/**
	 * Called by the service layer right before a Job is run. 
	 * Gives the machine an opportunity to do anything it needs to do to prepare to run
	 * the Job. 
	 * @param configuration
	 * @param job
	 */
	void prepareJob(Configuration configuration, Job job) throws Exception;
	
	/**
	 * Returns whether the Machine is currently ready for commands. 
	 */
	boolean isEnabled();
	
	/**
	 * Attempts to bring the Machine to a ready state. This would include turning on motor
	 * drivers, turning on compressors, resetting solenoids, etc. If the Machine is unable to
	 * become ready for any reason it should throw an Exception explaining the reason. This method
	 * should block until the Machine is ready.
	 * After this method is called successfully, isReady() should return true unless the Machine
	 * encounters some error.
	 */
	/**
	 * Stops the machine and disables it as soon as possible. This may include turning off power to
	 * motors and stopping compressors. It is expected that the machine may need to be re-homed after
	 * this is called. 
	 * If the Machine cannot be stopped for any reason, this method may throw an Exception explaining
	 * the reason but this should probably only happen in very extreme cases.
	 * This method should effectively be considered a software emergency stop.
	 * After this method return, isReady() should return false until start() is successfully
	 * called again.
	 */
	public void setEnabled(boolean enabled) throws Exception;
	
	void start() throws Exception;
	
	void addListener(MachineListener listener);
	
	void removeListener(MachineListener listener);
}
