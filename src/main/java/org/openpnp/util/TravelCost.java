/*
 * Copyright (C) 2025 <janm012012@googlemail.com>
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

package org.openpnp.util;

import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.solutions.HeadSolutions;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.pmw.tinylog.Logger;

/**
 * Methods to estimate the cost of motion between two locations.
 * 
 * The basic principle is to estimate the cost using three point motion with
 * two constant acceleration segments and one optional feedrate limited segment.
 * The estimation assumes, that motion starts and ends at standstill.
 */
public class TravelCost {
    
    /**
     * @param movable head mountable's axis to use for cost estimation, if null
     * the default camera on the default head is used
     * @param units the units to calculate the in. static data is prepared in this
     * units and Length input is convert to this units for the estimation
     * @throws if no default camera is define or if the movable has no mapped XY axis
     */
    public TravelCost(HeadMountable movable, LengthUnit units) throws Exception {
        super();

        // use default units if not specified
        if (units == null) {
            units = defaultUnits;
        }
        this.units = units;
        
        // collect axis parameter
        Machine machine = Configuration.get().getMachine();
        // if no movable is specified, use the default camera of the default head
        if (movable == null) {
            movable = machine.getDefaultHead().getDefaultCamera();
        }
        CoordinateAxis rawAxisX = HeadSolutions.getRawAxis(machine, movable.getAxisX());
        CoordinateAxis rawAxisY = HeadSolutions.getRawAxis(machine, movable.getAxisY());
        if (! (rawAxisX instanceof ReferenceControllerAxis
                && rawAxisY instanceof ReferenceControllerAxis)) {
            throw new Exception("HeadMountable "+ movable + "has no XY axis assigned");
        }
        ReferenceControllerAxis referenceControllerAxisX = (ReferenceControllerAxis)rawAxisX;
        ReferenceControllerAxis referenceControllerAxisY = (ReferenceControllerAxis)rawAxisY;

        this.xAxis = new AxisParameter(referenceControllerAxisX, units);
        this.yAxis = new AxisParameter(referenceControllerAxisY, units);
        
        // try to collect information about Z axis
        CoordinateAxis rawAxisZ = HeadSolutions.getRawAxis(machine, movable.getAxisZ());
        if (!(rawAxisZ instanceof ReferenceControllerAxis)) {
            Logger.trace("Travel cost estimation for headmountable " + movable + " not avaiable on Z axis");
            this.zAxis = null;
        }
        else {
            ReferenceControllerAxis referenceControllerAxisZ = (ReferenceControllerAxis)rawAxisZ;
    
            this.zAxis = new AxisParameter(referenceControllerAxisZ, units);
        }
        
        // try to collect information about rotational axis
        CoordinateAxis rawAxisC = HeadSolutions.getRawAxis(machine, movable.getAxisRotation());
        if (!(rawAxisC instanceof ReferenceControllerAxis)) {
            Logger.trace("Travel cost estimation for headmountable " + movable + " not avaiable on rotational axis");
            this.cAxis = null;
        }
        else {
            ReferenceControllerAxis referenceControllerAxisC = (ReferenceControllerAxis)rawAxisC;
    
            this.cAxis = new AxisParameter(referenceControllerAxisC, units);
        }
    }
    public TravelCost(HeadMountable movable) throws Exception {
        this(movable, null);
    }
    public TravelCost() throws Exception {
        this(null);
    }
    
    //* define default units to use for calculations
    private static final LengthUnit defaultUnits = LengthUnit.Millimeters;

    /**
     * Collect and group axis parameter for later use for cost estimation
     */
    private static class AxisParameter {
        private double acceleration;        // in [units]/s^2
        private double feedrate;            // in [units]/s
        private double shortDistanceLimit;  // in [units] - the distance at which motion is feedrate limited
        
        private AxisParameter(ReferenceControllerAxis axis, LengthUnit units) {
            this.acceleration = axis.getAccelerationPerSecond2().convertToUnits(units).getValue();
            this.feedrate     = axis.getFeedratePerSecond().convertToUnits(units).getValue();
            this.shortDistanceLimit = feedrate * feedrate / acceleration;
        }
    }

    private final LengthUnit units;
    private final AxisParameter xAxis;
    private final AxisParameter yAxis;
    private final AxisParameter zAxis;
    private final AxisParameter cAxis;

    /**
     * @param a
     * @param b
     * @return cost to travel from a to b using mapped axes parameters
     */
    public double getCost(Location a, Location b) {
        a = a.convertToUnits(units);
        b = b.convertToUnits(units);
        double costX = estimateCost(a.getX() - b.getX(), xAxis);
        double costY = estimateCost(a.getY() - b.getY(), yAxis);
        
        return Math.max(costX, costY);
    }

    public double getXyzCost(Location a, Location b) {
        a = a.convertToUnits(units);
        b = b.convertToUnits(units);
        double cost = getCost(a, b);
        // if zAxis parameters are not available, assume 0 and return cost on XY only
        if (zAxis != null) {
            cost = Math.max(cost, estimateCost(a.getZ() - b.getZ(), zAxis));
        }
        
        return cost;
    }

    public double getXyzcCost(Location a, Location b) {
        a = a.convertToUnits(units);
        b = b.convertToUnits(units);
        double cost = getXyzCost(a, b);
        // if cAxis parameters are not available, assume 0 and return cost on XYZ only
        if (cAxis != null) {
            cost = Math.max(cost, estimateCost((a.getRotation() - b.getRotation()) % 360, cAxis));
        }
        
        return cost;
    }

    /**
     * Return a cost estimation to travel <distance> using <axis>.
     * The feedrate and acceleration limits are used to estimate 
     * the cost assuming the motion is at stand still before and
     * after <distance>.
     *
     * @param distance distance to travel
     * @param axis axis to take acceleration and feedrate from
     * @return
     */
    private double estimateCost(double distance, AxisParameter axis) {
        double acceleration = axis.acceleration;
        double feedrate     = axis.feedrate;
        double cost;
        
        // make distance a positive number
        distance = Math.abs(distance);
        
        // on short distances, only acceleration has to be taken into account
        if (distance < axis.shortDistanceLimit) {
            cost = 2 * Math.sqrt(distance / acceleration);
        }
        else {
            cost = feedrate / acceleration + distance / feedrate;
        }
        
        return cost;
    }
}
