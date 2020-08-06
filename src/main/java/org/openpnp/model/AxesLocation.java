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
 * can store an arbitrary number of axes and their coordinates, including multiple times the same Axis.Type
 * e.g. multiple Z or C axes. 
 * 
 * All coordinates are handled as Millimeters to speed up calculations and allow for multi-axis transforms 
 * across drivers with different units. This unit-less handling also avoids problems with rotational axis 
 * coordinates that should obviously never be length unit converted.
 * 
 * Like Location, the AxesLoction is sometimes used as a Vector, thought as a distance to the origin. 
 * Furthermore, it is sometimes used as an axis map, i.e. only the included axes are important, not the 
 * coordinates.
 * 
 * Coordinates of axes not present in the AxesLocation are sometimes treated as 0.0, in other cases however a
 * missing axis means that it should not be included in an operation such as moveTo(), etc. Be careful to
 * determine which meaning applies. 
 * 
 * AxesLocations preserve the order of the axes. In particular the Configuration order of axis definitions is 
 * sometimes used to treat axes in their "natural" order, where it may matter.  
 * 
 */
public class AxesLocation {
    final private LinkedHashMap<Axis, Double> location;
    final public static AxesLocation zero = new AxesLocation();

    /**
     * All coordinates of AxesLoactions are handled as Millimeters to speed up calculations and allow for 
     * multi-axis transforms across drivers with different units and other universal vector math. This unit-less 
     * handling also avoids problems with rotational axis coordinates that should obviously never be length unit 
     * converted.
     * 
     * @return
     */
    final public static LengthUnit getUnits() {
        return LengthUnit.Millimeters;
    }

    /**
     * Create an empty AxesLocation. Because coordinates often default to 0.0, this can also be used as the
     * origin or zero AxesLocation. See also org.openpnp.model.AxesLocation.zero.   
     * 
     */
    public AxesLocation() {
        // Empty.
        location = new LinkedHashMap<>(0);
    }
    /**
     * Create a single Axis/coordinate pair AxesLocation.  
     * 
     * @param axis
     * @param coordinate
     */
    public AxesLocation(Axis axis, double coordinate) {
        location = new LinkedHashMap<>(1);
        if (axis != null) {
            location.put(axis, coordinate);
        }
    }
    /**
     * Create a single Axis/Length coordinate pair AxesLocation.  
     * 
     * @param axis
     * @param coordinate
     */
    public AxesLocation(Axis axis, Length coordinate) {
        this(axis, coordinate
                .convertToUnits(getUnits()).getValue());
    }
    /**
     * Create an AxesLocation with the given CoordinateAxis argument list and initialize to the current 
     * coordinates (i.e. planned coordinates). 
     * 
     * @param axes
     */
    public AxesLocation(CoordinateAxis... axis) {
        location = new LinkedHashMap<>(axis.length);
        for (CoordinateAxis oneAxis : axis) {
            location.put(oneAxis, oneAxis.getLengthCoordinate().convertToUnits(getUnits()).getValue());
        }
    }
    /**
     * Create an AxesLocation with the given Axis List and initialize to the current 
     * coordinates (i.e. planned coordinates). 
     * 
     * @param axes
     */
    public AxesLocation(List<CoordinateAxis> axes) {
        this(axes, (axis) -> axis.getLengthCoordinate());
    }
    /**
     * Create an AxesLocation with the given typed Axis Iterable and initialize coordinates with the given function.
     * 
     * @param <T>
     * @param axes
     * @param initializer
     */
    public <T extends Axis> AxesLocation(Iterable<T> axes, Function<T, Length> initializer) {
        location = new LinkedHashMap<>();
        for (T axis : axes) {
            Length coordinate = initializer.apply(axis);
            if (coordinate != null) {
                location.put(axis, coordinate.convertToUnits(getUnits()).getValue());
            }
        }
    }
    /**
     * Create an AxesLoaction over all the ControllerAxes of the machine and initialize to the current 
     * coordinates (i.e. planned coordinates). 
     * 
     * @param machine
     */
    public AxesLocation(Machine machine) {
        this(machine, (axis) -> (axis.getLengthCoordinate()));
    }
    /**
     * Create an AxesLoaction over all the CoordinateAxes of the machine (in Machine Setup order).
     * Apply the given function to initialize the coordinates.  
     * 
     * @param machine
     * @param initializer
     */
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
    /**
     * Create an AxesLoaction over all the ControllerAxes of the machine (in Machine Setup order) and with the given driver.
     * Apply the given function to initialize the coordinates.  
     * 
     * @param machine
     * @param driver
     * @param initializer
     */
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
    /**
     * Using the given binary function, aggregate the given axesLocation argument list.  
     * 
     * @param function
     * @param axesLocation
     */
    public AxesLocation(BiFunction<Double, Double, Double> function, AxesLocation... axesLocation) {
        location = new LinkedHashMap<>();
        for (AxesLocation oneAxesLocation : axesLocation) {
            for (Axis axis : oneAxesLocation.getAxes()) {
                location.merge(axis, oneAxesLocation.getCoordinate(axis), function);
            }
        }
    }
    /**
     * Create a new AxesLocation with the given function applied to the coordinates of axesLocation.
     *  
     * @param function
     * @param axesLocation
     */
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

    /**
     * Filter those axes from the AxesLocation that are driven by the given driver.
     * 
     * @param driver
     * @return
     */
    public AxesLocation drivenBy(Driver driver) {
        return new AxesLocation(getAxes(driver), (axis) -> (getLengthCoordinate(axis)));
    }

    /**
     * Filter those axes from the AxesLocation that have one of the given Axis.Types.
     * 
     * @param types
     * @return
     */
    public AxesLocation byType(Axis.Type... types) {
        final List<Axis.Type> typeList = Arrays.asList(types);
        return  new AxesLocation(getAxes(), 
                (axis) -> (typeList.contains(axis.getType()) ? 
                        getLengthCoordinate(axis) : null)); 
    }

    /**
     * Return all the axes from the AxesLocation.
     * 
     * @return
     */
    public Set<Axis> getAxes() {
        return location.keySet();
    }

    /**
     * Get those axes from the AxesLocation that are driven by the given driver.
     * 
     * @param driver
     * @return
     */
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

    /**
     * Get all the axes from the AxesLocation that are ControllerAxis.
     * 
     * @param driver
     * @return
     */
    public LinkedHashSet<ControllerAxis> getControllerAxes() {
        return getAxes(null);
    }

    public boolean contains(Axis axis) {
        if (axis == null) {
            return true;
        }
        return (location.containsKey(axis));
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

    /**
     * Set this AxesLocation to the axes as the current (planned) location.  
     */
    public void setToCoordinates() {
        for (Axis axis : getAxes()) {
            if (axis instanceof CoordinateAxis) {
                ((CoordinateAxis) axis).setCoordinate(getCoordinate(axis));
            }
        }
    }
    /**
     * Set this AxesLocation to the axes of the given driver as the current driver location.  
     * 
     * @param driver
     */
    public void setToDriverCoordinates(Driver driver) {
        for (ControllerAxis axis : getAxes(driver)) {
            axis.setDriverLengthCoordinate(getLengthCoordinate(axis));
        }
    }

    /**
     * Get the coordinate for the given axis from the AxesLocation.
     * 
     * @param axis
     * @return
     */
    public double getCoordinate(Axis axis) {
        if (axis != null) {
            Double coordinate = location.get(axis);
            if (coordinate != null) {
                return coordinate;
            }
        }
        return 0.0;
    }
    /**
     * Get the coordinate for the given axis from the AxesLocation, converted to the given length units.
     * 
     * @param axis
     * @param units
     * @return
     */
    public double getCoordinate(Axis axis, LengthUnit units) {
        if (axis.getType() == Axis.Type.Rotation) {
            // Never convert rotation angles.
            return getCoordinate(axis);
        }
        else {
            return getLengthCoordinate(axis).convertToUnits(units).getValue();
        }
    }
    /**
     * Get the coordinate for the given axis from the AxesLocation as a Length.
     * 
     * @param axis
     * @return
     */
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

    /**
     * Based on the mapped axes in this, substitute coordinates by Axis.Type to convert a Location to 
     * an AxisLocation. Used together with org.openpnp.spi.MovableMountable.getMappedAxes(Machine).
     * 
     * @param location
     * @return
     * @throws Exception
     */
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
     * From the AxisLocation, return the driver axis of the given type.  
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
     * From the AxisLocation, return the driver axis with the specified variable name. 
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
     * and that are not matching in coordinates. This will return a vector with only the axes that
     * need to move. 
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

    /**
     * Returns the Euclidean metric i.e. the distance in N-dimensional space of the AxesLocation from the origin
     * i.e. treated as a vector. 
     * 
     * @return
     */
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
