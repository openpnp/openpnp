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

import java.util.ArrayList;
import java.util.Collection;

import org.openpnp.model.AxesLocation;
import org.openpnp.model.Motion;
import org.openpnp.spi.Axis;
import org.openpnp.model.Length;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Movable.MoveToOption;
import org.openpnp.util.NanosecondTime;
import org.pmw.tinylog.Logger;

/**
 * Advanced Motion Planner panning 3rd order (Jerk controller) Motion Control. Supports uncoordinated motion.  
 *
 */
public class ReferenceAdvancedMotionPlanner extends AbstractMotionPlanner {

    @Override
    public void moveToPlanning(HeadMountable hm, AxesLocation axesLocation, double speed, MoveToOption... options) 
            throws Exception {
        super.moveToPlanning(hm, axesLocation, speed, options);
    }


    @Override
    public void waitForCompletion(HeadMountable hm, CompletionType completionType) throws Exception {
        // Plan the queued motion.
        planMotion(completionType);
        // Execute.
        executeMotionPlan(completionType);
        // Wait for drivers.
        waitForDriverCompletion(hm, completionType);
        // Now the physical completion is done, do the abstract stuff.
        super.waitForCompletion(hm, completionType);
    }


    public void planMotion(CompletionType completionType) throws Exception {
        // Plan the tail of the queued motions.
        ArrayList<Motion> motionPath = new ArrayList<>();
        Motion prevMotion = null; 
        // Note the tail includes the already executed last move. This is the starting point for the plan. 
        Collection<Motion> tail = motionPlan.tailMap(planExecutedTime, true).values();
        if (tail.size() >= 2) {
            for (Motion queuedMotion : tail) {
                if (prevMotion != null 
                        && !prevMotion.getLocation().motionSegmentTo(queuedMotion.getLocation()).isEmpty()) {
                    // The queued motion are the fixed way-points. We need to insert segments for 3rd order motion control.
                    // The simplest general form is a four-segment "mirror-S" curve. A co-linear motion sequence could be even simpler, 
                    // but we ignore this case here.
                    AxesLocation zeroVectorAxes = new AxesLocation(queuedMotion.getLocation().getControllerAxes(), 
                            (a) -> new Length(0, AxesLocation.getUnits()));
                            
                    // Kill current "solution", we only interpolate the location.
                    AxesLocation delta = prevMotion.getLocation().motionSegmentTo(queuedMotion.getLocation());
                    // TODO: better heuristics 
                    final int segments = 4;
                    for (int interpolate = 1; interpolate < segments; interpolate++) {
                        Motion interpolateMotion = prevMotion.interpolate(queuedMotion, interpolate*1.0/segments);
                        interpolateMotion.setTime(queuedMotion.getTime()/segments);
                        interpolateMotion.clearOption(Motion.MotionOption.FixedWaypoint);
                        if (isInSaveZZone(prevMotion) && isInSaveZZone(queuedMotion)) {
                            interpolateMotion.clearOption(Motion.MotionOption.CoordinatedWaypoint);
                            interpolateMotion.setOption(Motion.MotionOption.LimitToSafeZZone);
                        }
                        interpolateMotion.setVector(Motion.Derivative.Velocity, delta.multiply(1./(queuedMotion.getTime()+0.01)));
                        interpolateMotion.setVector(Motion.Derivative.Acceleration, zeroVectorAxes);
                        interpolateMotion.setVector(Motion.Derivative.Jerk, zeroVectorAxes);
                        motionPath.add(interpolateMotion);
                    }
                    queuedMotion.setTime(queuedMotion.getTime()/segments);
                    queuedMotion.setVector(Motion.Derivative.Velocity, delta.multiply(1./(queuedMotion.getTime()+0.01)));
                    queuedMotion.setVector(Motion.Derivative.Acceleration, zeroVectorAxes);
                    queuedMotion.setVector(Motion.Derivative.Jerk, zeroVectorAxes);
                    
                }
                // Add to list.
                motionPath.add(queuedMotion);
                // Next, please.
                prevMotion = queuedMotion;
            }

            if (completionType == CompletionType.WaitForStillstand) {
                prevMotion.setOption(Motion.MotionOption.Stillstand);
            }
            AdvancedMotionSolver solver = new AdvancedMotionSolver();
            solver.outputMotionPath("before", motionPath);
            solver.elaborateMotionPath(motionPath, solver.new ConstraintsModeller());
            solver.solve(1000000, precision.convertToUnits(AxesLocation.getUnits()).getValue()*0.00000001);
            Logger.debug("solver state: {}, iterations: {}, error: {}", solver.getSolverState(), solver.getFunctionEvalCount(), solver.getLastError());
            solver.outputMotionPath("after", motionPath);
            // Remove the old plan
            while (motionPlan.size() > 0) {
                double last = motionPlan.lastKey();
                if (planExecutedTime >= last) {
                    break;
                }
                motionPlan.remove(last);
            }
            // Add the new plan.
            double time = Math.max(planExecutedTime+1e-9, NanosecondTime.getRuntimeSeconds());
            for (Motion motion : motionPath) {
                motionPlan.put(time, motion);
                time += motion.getTime();
            }
        }
    }



    private boolean isInSaveZZone(Motion motion) {
        for (ControllerAxis axis : motion.getLocation().byType(Axis.Type.Z).getControllerAxes()) {
            double z = motion.getLocation().getCoordinate(axis);
            // TODO: implement 
            if (z < -15.0 || z > 0.0) {
                return false;
            }
        }
        return true;
    }
}
