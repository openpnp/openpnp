/*
 * Copyright (C) 2020 <mark@makr.zone>
 * inspired and based on work
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.spi.base;

import org.openpnp.model.AxesLocation;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.spi.Axis;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Locatable.LocationOption;
import org.simpleframework.xml.Element;

/**
 * An AbstractCoordinateAxis is an axis which can store a coordinate either physically or virtually. 
 *
 */
public abstract class AbstractCoordinateAxis extends AbstractAxis implements CoordinateAxis {
    @Element(required = false)
    private Length homeCoordinate = new Length(0.0, LengthUnit.Millimeters);

    /**
     * The coordinate that was last sent to the MotionPlanner either through a moveTo() or other
     * operations, such as homing. 
     * 
     * This coordinate may not yet be sent to the driver and even less likely reflect the 
     * physical machine position. Only after a waitForCompletion() on the MotionPlanner, can you be sure that 
     * the coordinate is in sync with the machine. 
     * 
     * The coordinate is always in AxesLocation.getUnits() i.e. in Millimeters.
     * 
     */
    private double coordinate;

    @Override
    public double getCoordinate() {
        return coordinate;
    }
    @Override
    public Length getLengthCoordinate() {
        return new Length(coordinate, AxesLocation.getUnits());
    }

    @Override
    public void setCoordinate(double coordinate) {
        Object oldValue = this.coordinate;
        this.coordinate = coordinate;
        firePropertyChange("coordinate", oldValue, coordinate);
        firePropertyChange("lengthCoordinate", null, getLengthCoordinate());
    }

    @Override
    public void setLengthCoordinate(Length coordinate) {
        if (type == Type.Rotation) {
            // Never convert rotation angles.
            setCoordinate(coordinate.getValue());
        }
        else {
            setCoordinate(coordinate.convertToUnits(AxesLocation.getUnits()).getValue());
        }
    }

    @Override
    public Length getHomeCoordinate() {
        return homeCoordinate;
    }

    @Override
    public void setHomeCoordinate(Length homeCoordinate) {
        Object oldValue = this.homeCoordinate;
        this.homeCoordinate = homeCoordinate;
        firePropertyChange("homeCoordinate", oldValue, homeCoordinate);
    }

    @Override
    public AxesLocation getCoordinateAxes(Machine machine) {
        return new AxesLocation(this);
    }

    @Override
    public AxesLocation toTransformed(AxesLocation location, LocationOption... options) {
        // No transformation, obviously
        return location;
    }

    @Override
    public AxesLocation toRaw(AxesLocation location, LocationOption... options) {
        // No transformation, obviously
        return location;
    }

    @Override
    public boolean coordinatesMatch(double coordinateA, double coordinateB) {
        if (type == Axis.Type.Rotation) {
            long a = getResolutionTicks(coordinateA);
            long b = getResolutionTicks(coordinateB);
            long wraparound = getResolutionTicks(360.0);
            boolean ret =  (Math.abs(a - b) % wraparound) == 0;
            return ret;
        }
        else {
            long a = getResolutionTicks(coordinateA);
            long b = getResolutionTicks(coordinateB);
            return a == b;
        }
    }
    
    protected abstract long getResolutionTicks(double coordinate);
}
