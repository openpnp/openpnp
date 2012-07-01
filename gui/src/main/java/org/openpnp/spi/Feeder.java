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

import org.openpnp.gui.support.Wizard;
import org.openpnp.model.Location;
import org.openpnp.model.Part;



/**
 * A Feeder is an abstraction that represents any type of part source. 
 * It can be a tape and reel feeder, a tray handler, a single part in a 
 * specific location or anything else that can be used as a pick source.
 * 
 * When a Feeder is first configured as part of machine setup it is not
 * given a Part to handle. This is because in the case of something like a
 * reel the user may swap different Parts into the same Feeder without
 * needing to reconfigure the Feeder.
 * 
 * For instance, if the Feeder is setup to feed 5mm cut tape, the user
 * may run a 0805 resistor in the Feeder for one job and a 0805 capacitor
 * in the same Feeder for a different job.
 */
public interface Feeder {
	/**
	 * Gets the id that was set when the Feeder was initialized.
	 * @return
	 */
	public String getId();
	
	public void setId(String id);
	
	public boolean isEnabled();

	public void setEnabled(boolean enabled);
	
	public Location getLocation();
	
	public void setLocation(Location location);
	
	public Part getPart();
	
	public void setPart(Part part);
	
	/**
	 * Returns true if the Feeder is ready and willing to source the Part for
	 * the given Head.
	 * @return
	 */
	public boolean canFeedForHead(Head head); 
	
	
	/**
	 * Allows the Feeder to do anything it needs to to prepare the part to be picked. If the Feeder requires the
	 * pick location to be modified it can return a new Location, otherwise it should just return the original
	 * passed in Location. The incoming pickLocation should not be modified.
	 * @param head
	 * @param part
	 * @param pickLocation
	 * @return
	 * @throws Exception
	 */
	public Location feed(Head head, Location pickLocation) throws Exception;
	
	public Wizard getConfigurationWizard();
}
