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
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Axis;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Locatable.LocationOption;
import org.openpnp.spi.Machine;
import org.openpnp.util.MovableUtils;
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
        this.coordinate = coordinate;
        // Note, we do not firePropertyChange() as these changes are live from the machine thread,
        // and coordinate changes are handled through MachineListener.machineHeadActivity(Machine, Head).
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
        return convertToSystem(homeCoordinate);
    }

    @Override
    public void setHomeCoordinate(Length homeCoordinate) {
        Object oldValue = this.homeCoordinate;
        this.homeCoordinate = convertFromSystem(homeCoordinate);
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

    protected Length convertToSystem(Length length) {
        if (type == Axis.Type.Rotation) {
            // This is actually an angle, not a length, just take it at it numerical  value
            // and present in system units, so no conversion will take place.
            return new Length(length.getValue(), Configuration.get().getSystemUnits());
        }
        else {
            return length;
        }
    }

    protected Length convertFromSystem(Length length) {
        if (type == Axis.Type.Rotation) {
            // This is actually an angle, not a length, just take it at it numerical value
            // and store as neutral mm.
            return new Length(length.getValue(), AxesLocation.getUnits());
        }
        else {
            return length;
        }
    }

    /**
     * Tries to move the axis to the specified raw coordinate in a safe way. 
     * 
     * @param coordinate
     * @throws Exception
     */
    public void moveAxis(Length coordinate) throws Exception {
        // To be safe we need to go through a HeadMountable and the full motion stack.
        // Find one that maps the axis.
        HeadMountable axisMover = getDefaultHeadMountable();
        if (axisMover == null) {
            throw new Exception("The axis "+getName()+" is not mapped to any HeadMountables. Can't move safely.");
        }
        axisMover.moveToSafeZ();
        AxesLocation axesLocation = axisMover.toRaw(axisMover.toHeadLocation(axisMover.getLocation()))
                .put(new AxesLocation(this, coordinate));
        Location location = axisMover.toHeadMountableLocation(axisMover.toTransformed(axesLocation));
        MovableUtils.moveToLocationAtSafeZ(axisMover, location);
        MovableUtils.fireTargetedUserAction(axisMover);
    }

    /**
     * @return The first HeadMountable that has this axis assigned. Used to capture and safely position an axis. 
     * Returns null is the axis is unused.
     */
    public HeadMountable getDefaultHeadMountable() {
        for (Head head : Configuration.get().getMachine().getHeads()) {
            // Try cameras with preference.
            for (HeadMountable hm : head.getCameras()) {
                if (hm.getMappedAxes(Configuration.get().getMachine()).contains(this)) {    
                    return hm;
                }
            }
            // Then the rest.
            for (HeadMountable hm : head.getHeadMountables()) {
                if (hm.getMappedAxes(Configuration.get().getMachine()).contains(this)) {    
                    return hm;
                }
            }
        }
        return null;
    }

}
