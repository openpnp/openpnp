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

package org.openpnp.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.openpnp.spi.Axis;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Machine;

/**
 * Like the classic OpenPnP Location, the AxesLocation stores a set of coordinates. However AxesLocation 
 * can store an arbitrary number of axes and their coordinates.  
 * 
 * All coordinates are handled as Millimeters to speed up calculations and allow for multi-axis transforms 
 * across drivers with different units. Also we don't have problems handling rotation axes the same way.  
 * 
 */
public class AxesLocation {
    final private LinkedHashMap<Axis, Double> location;
    final public static AxesLocation zero = new AxesLocation();

    public AxesLocation() {
        // Empty
        location = new LinkedHashMap<>(0);
    }
    public AxesLocation(Axis axis, double coordinate) {
        location = new LinkedHashMap<>(1);
        if (axis != null) {
            location.put(axis, coordinate);
        }
    }
    public AxesLocation(Axis axis, Length coordinate) {
        this(axis, coordinate
                .convertToUnits(getUnits()).getValue());
    }
    public AxesLocation(CoordinateAxis... axis) {
        location = new LinkedHashMap<>(axis.length);
        for (CoordinateAxis oneAxis : axis) {
            location.put(oneAxis, oneAxis.getLengthCoordinate().convertToUnits(getUnits()).getValue());
        }
    }
    public AxesLocation(List<CoordinateAxis> axes) {
        this(axes, (axis) -> axis.getLengthCoordinate());
    }
    public <T extends Axis> AxesLocation(Iterable<T> axes, Function<T, Length> initializer) {
        location = new LinkedHashMap<>();
        for (T axis : axes) {
            Length coordinate = initializer.apply(axis);
            if (coordinate != null) {
                location.put(axis, coordinate.convertToUnits(getUnits()).getValue());
            }
        }
    }
    public AxesLocation(Machine machine) {
        this(machine, (axis) -> (axis.getLengthCoordinate()));
    }
    public AxesLocation(Machine machine, Function<CoordinateAxis, Length> initializer) {
        location = new LinkedHashMap<>();
        for (Axis axis : machine.getAxes()) {
            if (axis instanceof CoordinateAxis) {
                Length coordinate = initializer.apply((CoordinateAxis) axis);
                if (coordinate != null) {
                    location.put(axis, coordinate.convertToUnits(getUnits()).getValue());
                }
            }
        }
    }
    public AxesLocation(Machine machine, Driver driver, Function<ControllerAxis, Length> initializer) {
        location = new LinkedHashMap<>();
        for (Axis axis : machine.getAxes()) {
            if (axis instanceof ControllerAxis) {
                if (((ControllerAxis) axis).getDriver() == driver) {
                    Length coordinate = initializer.apply((ControllerAxis) axis);
                    if (coordinate != null) {
                        location.put(axis, coordinate.convertToUnits(getUnits()).getValue());
                    }
                }
            }
        }
    }
    public AxesLocation(BiFunction<Double, Double, Double> function, AxesLocation... axesLocation) {
        location = new LinkedHashMap<>();
        for (AxesLocation oneAxesLocation : axesLocation) {
            for (Axis axis : oneAxesLocation.getAxes()) {
                location.merge(axis, oneAxesLocation.getCoordinate(axis), function);
            }
        }
    }
    public AxesLocation(Function<Double, Double> function, AxesLocation axesLocation) {
        location = new LinkedHashMap<>();
        for (Axis axis : axesLocation.getAxes()) {
            location.put(axis, function.apply(axesLocation.getCoordinate(axis)));
        }
    }

    public AxesLocation add(AxesLocation other) {
        return new AxesLocation((a, b) -> (a + b), this, other);
    }

    public AxesLocation subtract(AxesLocation other) {
        return new AxesLocation((a, b) -> (a - b), this, other);
    }

    public AxesLocation multiply(double factor) {
        return new AxesLocation((a) -> (a*factor), this);
    }

    public AxesLocation put(AxesLocation other) {
        return new AxesLocation((a, b) -> (b), this, other);
    }
    public AxesLocation drivenBy(Driver driver) {
        return new AxesLocation(getAxes(driver), (axis) -> (getLengthCoordinate(axis)));
    }

    public AxesLocation byType(Axis.Type... types) {
        final List<Axis.Type> typeList = Arrays.asList(types);
        return  new AxesLocation(getAxes(), 
                (axis) -> (typeList.contains(axis.getType()) ? 
                        getLengthCoordinate(axis) : null)); 
    }

    public static LengthUnit getUnits() {
        return LengthUnit.Millimeters;
    }
    public Set<Axis> getAxes() {
        return location.keySet();
    }
    public boolean contains(Axis axis) {
        if (axis == null) {
            return true;
        }
        return (location.containsKey(axis));
    }
    public LinkedHashSet<ControllerAxis> getAxes(Driver driver) {
        LinkedHashSet<ControllerAxis> axes = new LinkedHashSet<>();
        for (Axis axis : getAxes()) {
            if (axis instanceof ControllerAxis) {
                if (driver == null || ((ControllerAxis) axis).getDriver() == driver) {
                    axes.add((ControllerAxis) axis);
                }
            }
        }
        return axes;
    }

    public LinkedHashSet<ControllerAxis> getControllerAxes() {
        return getAxes(null);
    }

    /**
     * Returns true if the coordinates of this location match the other's.
     * 
     * Note, this is asymmetric, as only the axes contained in this location are matched.
     * If the other location contains more axes, they are ignored. If this location contains
     * more axes, they are compared against 0.0.    
     * 
     * Uses the resolution of the involved axes for tolerance. 
     * 
     * @param driver
     * @param other
     * @return
     */
    public boolean matches(AxesLocation other) {
        for (Axis axis : getAxes()) {
            if (axis instanceof ControllerAxis) {
                if (!((ControllerAxis) axis).coordinatesMatch(
                    this.getLengthCoordinate(axis), 
                    other.getLengthCoordinate(axis))) {
                    return false;
                }
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

    public void setToCoordinates() {
        for (Axis axis : getAxes()) {
            if (axis instanceof CoordinateAxis) {
                ((CoordinateAxis) axis).setCoordinate(getCoordinate(axis));
            }
        }
    }
    public void setToDriverCoordinates(Driver driver) {
        for (ControllerAxis axis : getAxes(driver)) {
            axis.setDriverLengthCoordinate(getLengthCoordinate(axis));
        }
    }
    
    public double getCoordinate(Axis axis) {
        if (axis != null) {
            Double coordinate = location.get(axis);
            if (coordinate != null) {
                return coordinate;
            }
        }
        return 0.0;
    }
    public double getCoordinate(Axis axis, LengthUnit units) {
        if (axis.getType() == Axis.Type.Rotation) {
            // Never convert rotation angles.
            return getCoordinate(axis);
        }
        else {
            return getLengthCoordinate(axis).convertToUnits(units).getValue();
        }
    }
    public Length getLengthCoordinate(Axis axis) {
        return new Length(getCoordinate(axis), getUnits());
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("(");
        int i = 0;
        for (Entry<Axis, Double> entry : location.entrySet()) {
            if (i++ > 0) {
                str.append(", ");
            }
            str.append(entry.getKey().getName());
            str.append(":");
            str.append(String.format(Locale.US, "%f", entry.getValue())); 
        }
        str.append(")");
        return str.toString();
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
     * Get the drivers of all the ControllerAxes in this AxesLocation.
     *  
     * @param machine
     * @return
     */
    public List<Driver> getAxesDrivers(Machine machine) {
        // Enumerate drivers in the order they are defined in the machine.
        // This is important, so the order of commands is independent of the 
        // axes that happen to be mapped. 
        List<Driver> list = new ArrayList<>();
        for (Driver driver : machine.getDrivers()) {
            // Check if one or more of the axes are mapped to the driver.
            if (!getAxes(driver).isEmpty()) {
                list.add(driver);
            }
        }
        return list;
    }

    /**
     * From the location, return the driver axis of the given type.  
     * 
     * @param driver
     * @param axisType
     * @return
     * @throws Exception
     */
    public ControllerAxis getAxis(Driver driver, Axis.Type axisType) throws Exception {
        ControllerAxis found = null; 
        for (ControllerAxis axis : getAxes(driver)) {
            if (axis.getType() == axisType) {
                if (found != null) {
                    // Make this future-proof: 
                    // Getting axes by type will no longer be allowed inside motion blending applications. 
                    throw new Exception("Axes "+found.getName()+" and "+axis.getName()+" have duplicate type "+axisType+" assigned.");
                }
                found = axis;
            }
        }
        return found;
    }
    public ControllerAxis getAxis(Axis.Type axisType) throws Exception {
        return getAxis(null, axisType);
    }
    /**
     * From the location, return the driver axis with the specified variable name. 
     * 
     * @param variable The name of the variable.
     * @return
     * @throws Exception If the variable names are unassigned or not unique within the mapped axes.
     */
    public ControllerAxis getAxisByVariable(Driver driver, String variable) 
            throws Exception {
        ControllerAxis found = null;
        if (driver.isUsingLetterVariables()) {
            for (ControllerAxis axis : getAxes(driver)) {
                if (axis.getLetter() == null || axis.getLetter().isEmpty()) {
                    throw new Exception("Axis "+axis.getName()+" has no letter assigned.");
                }
                if (axis.getLetter().equals(variable)) {
                    if (found != null) {
                        throw new Exception("Axes "+found.getName()+" and "+axis.getName()+" have duplicate letter "+variable+" assigned.");
                    }
                    found = axis;
                }
            }
        }
        else {
            for (ControllerAxis axis : getAxes(driver)) {
                if (axis.getType().toString().equals(variable)) {
                    if (found != null) {
                        throw new Exception("Axes "+found.getName()+" and "+axis.getName()+" have duplicate type "+variable+" assigned. Use letter variables on the driver.");
                    }
                    found = axis;
                }
            }
        }
        return found;
    }

    /**
     * Create a distance vector over ControllerAxes that are both contained in this and location1
     * and that are not matching in coordinates.
     * 
     * @param location1
     * @return
     */
    public AxesLocation motionSegmentTo(AxesLocation location1) {
        AxesLocation distance = new AxesLocation(getControllerAxes(), 
                (a) -> ( location1.contains(a) ? 
                        (!a.coordinatesMatch(getCoordinate(a), location1.getCoordinate(a)) ? 
                                new Length(location1.getCoordinate(a) - getCoordinate(a), AxesLocation.getUnits())
                                :null)
                        :null));
        return distance;
    }
    public double getEuclideanMetric() {
        double sumSq = 0;
        for (Entry<Axis, Double> entry : location.entrySet()) {
            if (entry.getKey() instanceof ControllerAxis) {
                sumSq += Math.pow(entry.getValue(), 2);
            }
        }
        return Math.sqrt(sumSq);
    }

}
