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
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.openpnp.spi.Axis;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Driver;
import org.simpleframework.xml.ElementList;

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
    final public static AxesLocation zero = new AxesLocation();

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

    /**
     * Calculated motion limits over the given differential AxesLocation.
     *  
     * The nth order limit in millimeters/seconds^n.
     * The 0th order is the distance. 
     */
    static public class MotionLimits {
        final private double [] linearLimits;
        final private double [] rotationalLimits;
        public MotionLimits(AxesLocation location) {
            super();
            linearLimits = new double [ControllerAxis.motionLimitsOrder+1];
            rotationalLimits = new double [ControllerAxis.motionLimitsOrder+1];
            for (int order = 1; order <= ControllerAxis.motionLimitsOrder; order++) {
                linearLimits[order] = Double.MAX_VALUE;
                rotationalLimits[order] = Double.MAX_VALUE;
            }
            for (Entry<Axis, Double> entry : location.location.entrySet()) {
                Axis axis = entry.getKey();
                if (axis instanceof ControllerAxis) {

                    double dSq = entry.getValue()*entry.getValue();  
                    if (dSq > 0) {
                        if (axis.getType() == Axis.Type.Rotation) {
                            rotationalLimits[0] += dSq;
                            for (int order = 1; order <= ControllerAxis.motionLimitsOrder; order++) {
                                rotationalLimits[order] = Math.min(rotationalLimits[order],
                                        ((ControllerAxis) axis).getMotionLimit(order)
                                        /Math.abs(entry.getValue()));
                            }
                        }
                        else {
                            linearLimits[0] += dSq;
                            for (int order = 1; order <= ControllerAxis.motionLimitsOrder; order++) {
                                linearLimits[order] = Math.min(linearLimits[order],
                                        ((ControllerAxis) axis).getMotionLimit(order)
                                        /Math.abs(entry.getValue()));
                            }
                        }
                    }
                }
            }
            linearLimits[0] = Math.sqrt(linearLimits[0]);
            rotationalLimits[0] = Math.sqrt(rotationalLimits[0]);
            for (int order = 1; order <= ControllerAxis.motionLimitsOrder; order++) {
                if (linearLimits[0] > 0) {
                    linearLimits[order] = linearLimits[order] * linearLimits[0];
                }
                if (rotationalLimits[0] > 0) {
                    rotationalLimits[order] = rotationalLimits[order] * rotationalLimits[0];
                }
            }
        }
        public double getLinearLimit(int order) {
            return linearLimits[order];
        }
        public double getLinearLimit(Motion.Derivative order) {
            return linearLimits[order.ordinal()];
        }
        public double getRotationalLimit(int order) {
            return rotationalLimits[order];
        }
        public double getRotationalLimit(Motion.Derivative order) {
            return rotationalLimits[order.ordinal()];
        }
        public double getLinearDistance() {
            return linearLimits[0];
        }
        public double getRotationalDistance() {
            return rotationalLimits[0];
        }
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

}
