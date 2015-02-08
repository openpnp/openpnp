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

import org.openpnp.model.Identifiable;
import org.openpnp.model.Named;

/**
 * Defines a simple interface to some type of device that can be actuated
 * on the machine or on a head. This is a minimal interface and it is
 * expected that concrete implementations may have many other capabilities
 * exposed in their specific implementations. 
 */
public interface Actuator extends Identifiable, Named, HeadMountable, WizardConfigurable, PropertySheetHolder {
	/**
	 * Turns the Actuator on or off.
	 * @param on
	 * @throws Exception
	 */
	public void actuate(boolean on) throws Exception;
	
	/**
	 * Provides the actuator with a double value to which it can respond
	 * in an implementation dependent manner.
	 * @param value
	 * @throws Exception
	 */
	public void actuate(double value) throws Exception;
}
