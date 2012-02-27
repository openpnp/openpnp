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

package org.openpnp.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.openpnp.LengthUnit;
import org.simpleframework.xml.Attribute;

/**
 * A Location is a 3D point in X, Y, Z space with a rotation component. The rotation is applied about the Z
 * axis.
 */
public class Location extends AbstractModelObject {
	@Attribute(required=false)
	private LengthUnit units;
	@Attribute(required=false)
	private double x;
	@Attribute(required=false)
	private double y;
	@Attribute(required=false)
	private double z;
	@Attribute(required=false)
	private double rotation;
	
	public double getX() {
		return x;
	}
	
	public void setX(double x) {
		double oldValue = this.x;
		this.x = x;
		firePropertyChange("x", oldValue, x);
	}
	
	public double getY() {
		return y;
	}
	
	public void setY(double y) {
		double oldValue = this.y;
		this.y = y;
		firePropertyChange("y", oldValue, y);
	}
	
	public double getZ() {
		return z;
	}

	public void setZ(double z) {
		double oldValue = this.z;
		this.z = z;
		firePropertyChange("z", oldValue, z);
	}

	public double getRotation() {
		return rotation;
	}

	public void setRotation(double rotation) {
		double oldValue = this.rotation;
		this.rotation = rotation;
		firePropertyChange("rotation", oldValue, rotation);
	}
	
	public LengthUnit getUnits() {
		return units;
	}

	public void setUnits(LengthUnit units) {
		LengthUnit oldValue = this.units;
		this.units = units;
		firePropertyChange("units", oldValue, units);
	}

	@Override
	public String toString() {
		return String.format("units %s, x %f, y %f, z %f, rotation %f", units, x, y, z, rotation);
	}
}
