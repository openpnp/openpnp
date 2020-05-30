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
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Movable.MoveToOption;
import org.openpnp.util.NanosecondTime;

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
        planMotionPath();
        // Execute.
        executeMotionPlan();
        // Wait for drivers.
        waitForDriverCompletion(hm, completionType);
        // Now the physical completion is done, do the abstract stuff.
        super.waitForCompletion(hm, completionType);
    }


    public void planMotionPath() throws Exception {
        // Plan the tail of the queued motions.
        ArrayList<Motion> motionPath = new ArrayList<>();
        Motion prevMotion = null; 
        // Note the tail includes the already executed last move. This is the starting point for the plan. 
        Collection<Motion> tail = motionPlan.tailMap(planExecutedTime, true).values();
        if (tail.size() >= 2) {
            for (Motion queuedMotion : tail) {
                if (prevMotion != null) {
                    // The queued motion are the fixed way-points. We need to insert segments for 3rd order motion control.
                    // The simplest general form is a four-segment "mirror-S" curve. A co-linear motion sequence could be even simpler, 
                    // but we ignore this case here. 
                    // TODO: better heuristics 
                    // TODO: remove test with 7 segments.
                    final int segments = 7;
                    for (int interpolate = 1; interpolate < segments; interpolate++) {
                        Motion interpolateMotion = prevMotion.interpolate(queuedMotion, interpolate*1.0/segments);
                        interpolateMotion.setTime(1.0/segments);
                        interpolateMotion.clearOption(Motion.MotionOption.FixedWaypoint);
                        if (isInSaveZZone(prevMotion) && isInSaveZZone(queuedMotion)) {
                            interpolateMotion.clearOption(Motion.MotionOption.CoordinatedWaypoint);
                            interpolateMotion.clearOption(Motion.MotionOption.LimitToSafeZZone);
                        }
                        motionPath.add(interpolateMotion);
                    }
                }
                // Add to list.
                motionPath.add(queuedMotion);
                // Next, please.
                prevMotion = queuedMotion;
            }

            AdvancedMotionSolver solver = new AdvancedMotionSolver();
            solver.createModel(motionPath);
            solver.solve(1000, 0.01);
            // Remove the old plan
            while (true) {
                double last = motionPlan.lastKey();
                if (planExecutedTime >= last) {
                    break;
                }
                motionPlan.remove(last);
            }
            // Add the new plan.
            double time = NanosecondTime.getRuntimeSeconds();
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
