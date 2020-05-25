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

package org.openpnp.machine.reference.driver;

import java.util.Map;

import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Motion;
import org.openpnp.model.Motion.Derivative;
import org.openpnp.model.Motion.MotionOption;
import org.openpnp.spi.Axis;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Movable.MoveToOption;
import org.openpnp.util.NanosecondTime;

/**
 * Simplest possible implementation of the motion planner. Just plans constant velocity moves for NullDriver 
 * simulation. 
 *
 */
public abstract class AbstractSimpleMotionPlanner extends AbstractMotionPlanner {

    @Override
    public void moveToPlanning(HeadMountable hm, AxesLocation axesLocation, double speed, MoveToOption... options) 
            throws Exception {
        super.moveToPlanning(hm, axesLocation, speed, options);

        // This guy is so primitive, we can plan right now.

        // Get real-time.
        double now = NanosecondTime.getRuntimeSeconds();
        // Get the last entry in friendly names.
        double lastMotionTime = Double.MIN_VALUE;
        Motion lastMotion = null;
        Map.Entry<Double, Motion> lastEntry = motionPlan.lastEntry();
        double startTime = now;
        if (lastEntry != null) {
            lastMotionTime = lastEntry.getKey();
            lastMotion = lastEntry.getValue();
            if (lastMotionTime > now) {
                startTime = lastMotionTime;
            }
            else {
                // Pause between the moves.
                lastMotion = new Motion(startTime, 
                        new AxesLocation [] { lastMotion.getVector(Motion.Derivative.Location) },
                        MotionOption.FixedWaypoint, MotionOption.CoordinatedWaypoint);
                motionPlan.put(startTime, lastMotion);
            }
        }
        else {
            // No lastMotion, create the previous waypoint from the axes. 
            AxesLocation previousLocation = new AxesLocation(Configuration.get().getMachine()); 
            lastMotion = new Motion(startTime, 
                    new AxesLocation [] { previousLocation }, 
                    MotionOption.FixedWaypoint, MotionOption.CoordinatedWaypoint);
            motionPlan.put(startTime, lastMotion);
        }
        // Calculate the 1st order kinematics. Note this must include all the machine axes, not just the ones 
        // included in this moveTo().
        AxesLocation lastLocation = 
                new AxesLocation(Configuration.get().getMachine())
                .put(lastMotion.getVector(Motion.Derivative.Location));

        Motion endMotion = Motion.computeWithLimits(startTime, lastLocation, axesLocation, speed, true, false);
        motionPlan.put(endMotion.getTime(), endMotion);
    }

    @Override
    public Motion getMomentaryMotion(double time) {
        Map.Entry<Double, Motion> entry0 = motionPlan.floorEntry(time);
        Map.Entry<Double, Motion> entry1 = motionPlan.ceilingEntry(time);
        if (entry0 != null && entry1 != null) {
            // We're between two waypoints, interpolate linearly by time.
            double dt = entry1.getKey() - entry0.getKey();
            double ratio = (time - entry0.getKey())/dt;
            Motion interpolatedMotion = entry0.getValue().interpolate(entry1.getValue(), ratio);
            //Logger.trace("time = "+time+", ratio "+ratio+" motion="+interpolatedMotion);
            return interpolatedMotion;
        }
        else if (entry0 != null){
            // Machine stopped before this time. Just return the last location. 
            return new Motion(time, 
                    new AxesLocation [] { entry0.getValue().getVector(Derivative.Location) }, 
                    MotionOption.Stillstand);
        }
        else if (entry1 != null){
            // Planning starts after this time. Return the first known location. 
            return new Motion(time, 
                    new AxesLocation [] { entry1.getValue().getVector(Derivative.Location) }, 
                    MotionOption.Stillstand);
        }
        else {
            // Nothing in the plan, just get the current axes location.
            AxesLocation currentLocation = new AxesLocation(Configuration.get().getMachine()); 
            return new Motion(time, 
                    new AxesLocation [] { currentLocation }, 
                    MotionOption.Stillstand);
        }
    }
}
