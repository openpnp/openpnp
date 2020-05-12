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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.openpnp.spi.Axis;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Driver;

/**
 * Like the classic OpenPnP Location, the AxesLocation stores a set of coordinates. However
 * AxesLocation can store an arbitrary number of axes. 
 * 
 * All coordinates are handled as Millimeters to speed up calculations and allow for multi-axis transforms 
 * across drivers with different units. Also we don't have problems transforming rotation axes.  
 * 
 */
public class AxesLocation {
    final private HashMap<Axis, Double> location;

    public AxesLocation() {
        // Empty
        location = new HashMap<>(0);
    }
    public AxesLocation(MappedAxes mappedAxes) {
        this(mappedAxes.getAxes());
    }
    public AxesLocation(Axis axis, double coordinate) {
        location = new HashMap<>(1);
        if (axis != null) {
            location.put(axis, coordinate);
        }
    }
    public AxesLocation(Axis axis, Length coordinate) {
        this(axis, coordinate
                .convertToUnits(LengthUnit.Millimeters).getValue());
    }
    public AxesLocation(ControllerAxis... axis) {
        location = new HashMap<>(axis.length);
        for (ControllerAxis oneAxis : axis) {
            location.put(oneAxis, oneAxis.getLengthCoordinate()
                    .convertToUnits(LengthUnit.Millimeters).getValue());
        }
    }
    public AxesLocation(List<ControllerAxis> axes) {
        this(axes, (axis) -> axis.getLengthCoordinate()
                .convertToUnits(LengthUnit.Millimeters).getValue());
    }
    public AxesLocation(List<ControllerAxis> axes, Function<ControllerAxis, Double> initializer) {
        location = new HashMap<>(axes.size());
        for (ControllerAxis axis : axes) {
            location.put(axis, initializer.apply(axis));
        }
    }
    public AxesLocation(BiFunction<Double, Double, Double> function, AxesLocation... axesLocation) {
        location = new HashMap<>();
        for (AxesLocation oneAxesLocation : axesLocation) {
            for (Axis axis : oneAxesLocation.getAxes()) {
                location.merge(axis, oneAxesLocation.getCoordinate(axis), function);
            }
        }
    }
    public AxesLocation(Function<Double, Double> function, AxesLocation axesLocation) {
        location = new HashMap<>();
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

    public double distanceLinear() {
        double d = 0.0;
        for (Entry<Axis, Double> entry : location.entrySet()) {
            if (entry.getKey().getType() != Axis.Type.Rotation) {
                d += entry.getValue()*entry.getValue();
            }
        }
        return Math.sqrt(d);
    }
    public double distanceRotational() {
        double d = 0.0;
        for (Entry<Axis, Double> entry : location.entrySet()) {
            if (entry.getKey().getType() == Axis.Type.Rotation) {
                d += entry.getValue()*entry.getValue();
            }
        }
        return Math.sqrt(d);
    }

    public static LengthUnit getUnits() {
        return LengthUnit.Millimeters;
    }
    public Set<Axis> getAxes() {
        return location.keySet();
    }
    public Set<Axis> getAxes(Driver driver) {
        Set<Axis> axes = new HashSet<>();
        for (Axis axis : getAxes()) {
            if (axis instanceof ControllerAxis) {
                if (((ControllerAxis) axis).getDriver() == driver) {
                    axes.add(axis);
                }
            }
        }
        return axes;
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
        return getLengthCoordinate(axis).convertToUnits(units).getValue();
    }
    public Length getLengthCoordinate(Axis axis) {
        return new Length(getCoordinate(axis), LengthUnit.Millimeters);
    }
    public boolean contains(Axis axis) {
        if (axis == null) {
            return true;
        }
        return (location.containsKey(axis));
    }
}
