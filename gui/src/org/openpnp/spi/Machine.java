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
import org.w3c.dom.Node;


/**
 * Machine represents the pick and place machine itself. It provides the information and interface needed to
 * cause the machine to do work. A Machine has one or more Heads.
 */
public interface Machine {
	/**
	 * Allows the Machine to read additional configuration information.
	 * @param config
	 * @throws Exception
	 */
	void configure(Node config) throws Exception ;
	
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
	
	/**
	 * Called by the service layer right before a Job is run. 
	 * Gives the machine an opportunity to do anything it needs to do to prepare to run
	 * the Job. 
	 * @param configuration
	 * @param job
	 */
	void prepareJob(Configuration configuration, Job job) throws Exception;
}
