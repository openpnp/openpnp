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

import org.openpnp.RequiresConfigurationResolution;
import org.openpnp.gui.support.Wizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
 * A simple binary Actuator that sends an indexed actuate command to the
 * driver and has a Location which provides offsets from the Head or Machine. 
 */
public class ReferenceActuator implements Actuator, RequiresConfigurationResolution {
	@Attribute
	private String id;
	@Attribute
	private int index;
	/**
	 * If the Actuator is attached to a Head, this Location provides the
	 * offsets from the Head to the Actuator. These offsets are added to
	 * the Head location to get the Actuator location.
	 */
	@Element(required=false)
	private Location location;
	
	private ReferenceMachine machine;
	private ReferenceHead head;
	
	@Override
	public void resolve(Configuration configuration) throws Exception {
		this.machine = (ReferenceMachine) configuration.getMachine();
	}
	
	public void setReferenceHead(ReferenceHead head) {
		this.head = head;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}
	
	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public void actuate(boolean on) throws Exception {
		if (head != null) {
			machine.getDriver().actuate(head, index, on);
		}
		else {
			machine.getDriver().actuate(head, index, on);
		}
		// TODO Build out fireMachineActuatorActivity
//		machine.fireMachineHeadActivity(machine, this);
	}
	
	@Override
	public Wizard getConfigurationWizard() {
		return new ReferenceActuatorConfigurationWizard(this);
	}
}
