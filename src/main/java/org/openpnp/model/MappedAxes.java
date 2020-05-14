package org.openpnp.model;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Machine;
import org.openpnp.spi.base.AbstractMachine;

/**
 * The set of ControllerAxes mapped for a specific operation e.g. for a moveTo() or home(). The map is 
 * derived/accumulated from HeadMountables' mapped axes and then separated and filtered by drivers. The map
 * guarantees to list the axes in the order of their Machine Tree definition.
 * 
 * The MappedAxes cannot use the standard OpenPnP 4D Location model because multiple HeadMountables might 
 * use multiple axes of the same Axis.Type (typically multiple Z and C axes) and we need to be able to home() 
 * all of them at once. For advanced motion planning, the same is (will be) true for the inner workings of moveTo().  
 * Furthermore in order to be ready to support advanced Axis Transformations such as Non-Cartesian Arm Solutions, 
 * (e.g. a Revolver Head) there might be more than 4 axes involved at any one time. The AxisLocation model with 
 * arbitrary number of axes is providing the necessary support to work with N-Dimensional-Locations. 
 * 
 */
public class MappedAxes {
    private final List<ControllerAxis> axes;

    final public static MappedAxes empty = new MappedAxes();

    protected MappedAxes() {
        // Empty set
        this.axes = new ArrayList<>();
    }

    public MappedAxes(ControllerAxis axis) {
        // Single axis.
        this.axes = new ArrayList<>(1);
        if (axis != null) {
            this.axes.add(axis);
        }
    }

    public MappedAxes(Machine machine) {
        // All machine axes.
        this.axes = new ArrayList<>();
        for (Axis axis : machine.getAxes()) {
            if (axis instanceof ControllerAxis) {
                axes.add((ControllerAxis) axis);
            }
        }
    }

    public MappedAxes(Machine machine, Driver driver) {
        // All machine axes filtered by driver.
        this.axes = new ArrayList<>();
        for (Axis axis : machine.getAxes()) {
            if (axis instanceof ControllerAxis) {
                if (((ControllerAxis) axis).getDriver() == driver) {
                    axes.add((ControllerAxis) axis);
                }
            }
        }
    }

    public MappedAxes(Machine machine, AxesLocation location) {
        // All the axes from the location but ordered as in the machine.
        this.axes = new ArrayList<>();
        for (Axis axis : machine.getAxes()) {
            if (axis instanceof ControllerAxis) {
                if (location.getAxes().contains(axis)) {
                    this.axes.add((ControllerAxis) axis);
                }
            }
        }
    }

    public MappedAxes(Machine machine, MappedAxes... mappedAxes) {
        // All the axes from the cumulative mappedAxes but ordered as in the machine.
        this.axes = new ArrayList<>();
        for (Axis axis : machine.getAxes()) {
            if (axis instanceof ControllerAxis) {
                for (MappedAxes oneMappedAxes : mappedAxes) {
                    if (oneMappedAxes.getAxes().contains(axis)) {
                        this.axes.add((ControllerAxis) axis);
                        break;
                    }
                }
            }
        }
    }

    public MappedAxes(MappedAxes mappedAxes, Driver driver) {
        // Filtered by driver.
        this.axes = new ArrayList<>();
        for (ControllerAxis axis : mappedAxes.getAxes()) {
            if (axis.getDriver() == driver) {
                axes.add(axis);
            }
        }
    }

    public List<ControllerAxis> getAxes() {
        return axes;
    }

    public ControllerAxis getAxis(Axis.Type axisType) throws Exception {
        ControllerAxis found = null; 
        for (ControllerAxis axis : getAxes()) {
            if (axis.getType() == axisType) {
                if (found != null) {
                    // Make this future-proof: 
                    // Getting axes by type will no longer be allowed inside motion blending applications. 
                    throw new Exception("Mapped Axis "+axis.getName()+" has duplicate type "+axisType+" assigned.");
                }
                found = axis;
            }
        }
        return found;
    }

    public AxesLocation getTypedLocation(Location location) throws Exception {
         location = location.convertToUnits(AxesLocation.getUnits());
         return new AxesLocation((a, b) -> (b),
                new AxesLocation(getAxis(Axis.Type.X), location.getX()),
                new AxesLocation(getAxis(Axis.Type.Y), location.getY()),
                new AxesLocation(getAxis(Axis.Type.Z), location.getZ()),
                new AxesLocation(getAxis(Axis.Type.Rotation), location.getRotation()));
    }

    /**
     * @return The current controller Location stored in the mapped axes. 
     */
    public AxesLocation getLocation() {
        return new AxesLocation(getAxes()); 
    }

    /**
     * Set the current controller Location stored in the mapped axes. 
     */
    public void setLocation(AxesLocation location) {
        for (ControllerAxis axis : getAxes()) {
            axis.setLengthCoordinate(location.getLengthCoordinate(axis));
        }
    }

    /**
     * @return The Home Location in raw motion-controller axes coordinates.
     */
    public AxesLocation getHomeLocation() {
        return new AxesLocation(getAxes(), (axis) -> 
        axis.getHomeCoordinate().convertToUnits(AxesLocation.getUnits()).getValue()); 
    }

    public AxesLocation getMappedOnlyLocation(AxesLocation location) {
        return new AxesLocation(getAxes(), (axis) -> 
        location.getCoordinate(axis));
    }

    public List<Driver> getMappedDrivers(Machine machine) {
        // Enumerate drivers in the order they are defined in the machine.
        // This is important, so the order of commands is independent of the 
        // axes that happen to be mapped. 
        List<Driver> list = new ArrayList<>();
        for (Driver driver : machine.getDrivers()) {
            // Check if one or more of the axes are mapped to the driver.
            for (ControllerAxis axis : getAxes()) {
                if (driver == axis.getDriver()) {
                    list.add(driver);
                    break;
                }
            }
        }
        return list;
    }

    /**
     * Returns true if coordinates of the mapped axes within two locations match against each other. 
     * Uses the resolution of the involved axes for tolerance. 
     * 
     * @param locationA
     * @param locationB
     * @return 
     */
    public boolean locationsMatch(AxesLocation locationA, AxesLocation locationB) {
        for (ControllerAxis axis : getAxes()) {
            if (!axis.coordinatesMatch(
                    locationA.getLengthCoordinate(axis), 
                    locationB.getLengthCoordinate(axis))) {
                return false;
            }
        }
        return true;
    }
    public int size() {
        return getAxes().size();
    }
    public boolean isEmpty() {
        return getAxes().isEmpty();
    }

    /**
     * Returns the axis with the specified variable name. 
     * 
     * @param variable The name of the variable.
     * @param usingLetterVariables If true, the letter name of the axis is used. This is the modern way. 
     * If false, the type of the axis is used. This is the legacy way. 
     * @return
     * @throws Exception If the variable names are unassigned or not unique within the mapped axes.
     */
    public ControllerAxis getAxisByVariable(String variable, boolean usingLetterVariables) 
            throws Exception {
        ControllerAxis found = null;
        if (usingLetterVariables) {
            for (ControllerAxis axis : getAxes()) {
                if (axis.getLetter() == null ||axis.getLetter().isEmpty()) {
                    throw new Exception("Axis "+axis.getName()+" has no letter assigned.");
                }
                if (axis.getLetter().equals(variable)) {
                    if (found != null) {
                        throw new Exception("Mapped Axis "+axis.getName()+" has duplicate letter "+variable+" assigned.");
                    }
                    found = axis;
                }
            }
        }
        else {
            for (ControllerAxis axis : getAxes()) {
                if (axis.getType().toString().equals(variable)) {
                    if (found != null) {
                        throw new Exception("Mapped Axis "+axis.getName()+" has duplicate type "+variable+" assigned. Use letter variables on the Driver.");
                    }
                    found = axis;
                }
            }
        }
        return found;
    }

}


