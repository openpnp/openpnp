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

package org.openpnp.gui.components;

import org.openpnp.model.Location;

public class CameraViewActionEvent {
	public int componentX, componentY;
	public double physicalX, physicalY;
	public Location location;
	
	public CameraViewActionEvent(
			CameraView source, 
			int componentX, 
			int componentY, 
			double physicalX, 
			double physicalY,
			Location location) {
		this.componentX = componentX;
		this.componentY = componentY;
		this.physicalX = physicalX;
		this.physicalY = physicalY;
		this.location = location;
	}

	public int getComponentX() {
		return componentX;
	}

	public void setComponentX(int componentX) {
		this.componentX = componentX;
	}

	public int getComponentY() {
		return componentY;
	}

	public void setComponentY(int componentY) {
		this.componentY = componentY;
	}

	public double getPhysicalX() {
		return physicalX;
	}

	public void setPhysicalX(double physicalX) {
		this.physicalX = physicalX;
	}

	public double getPhysicalY() {
		return physicalY;
	}

	public void setPhysicalY(double physicalY) {
		this.physicalY = physicalY;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}
}
