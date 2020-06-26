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

package org.openpnp.spi;

import org.openpnp.model.AxesLocation;
import org.openpnp.model.Motion;
import org.openpnp.spi.Movable.MoveToOption;

/**
 * <p>
 * The MotionPlanner is used for the following tasks:
 * </p><ul>
 * <li>Accept/record a sequence of nominal moveTo() commands.</li>
 * <li>For advanced MotionPlanners, perform post-processing on the sequence. </li>
 * <li>Assert moments of coordinated completion with waitForCompletion().</li>
 * <li>In due time, command the drivers to execute the plan.</li>
 * <li>Provide access to the planned motion over time for simulation, visualization etc. </li>
 * </ul>
 * <p>
 * A MotionPlanner can be a simple proxy, just sending commands directly to the drivers. See the 
 * NullMotionPlanner. Future advanced MotionPlanners can do nth order Kinematics control, motion blending
 * etc. 
 * </p><p>
 * All that is guaranteed is that the MotionPlanner is an envelope planner i.e. it will only plan the 
 * maximum velocity, acceleration, jerk allowed to the controller and its real-time planner. The real 
 * physical motion might be more constrained i.e. take more time. Having said that, it remains a goal 
 * to anticipate the controller's motion as good as possible so that simulation and reality will be very 
 * similar and optimizations are purposeful. 
 * </p><p>
 * An advanced Planner will first record the commands and it may (or may not yet) plan the moves. 
 * A waitForCompletion() will force the planning and generate the needed output commands. Note that proper
 * envelope motion control is only possible for complete moves, because the the planner must know when to
 * decelerate in time. 
 * </p><p>
 * Sometimes, motion might be recorded but no immediate waitForCompletion() is wanted. The most prominent
 * example is jogging, where the planner never knows if and when the user will press another button.  
 * In this case up-front envelope motion planning is impossible, at least for the deceleration part. 
 * Therefore motion control is mostly left to the hardware controller, perhaps with more conservative 
 * deceleration parameters. 
 * </p><p>
 * The MotionPlanner must plan all the Axes of the machine, across all the Drivers. Therefore driver/controller
 * specific behavior must be modeled per Axis. The MotionPlanner should also try to approximate coordinated 
 * motion across controllers. A best-effort approximation should be enough for PnP purposes.  
 * </p><p>
 * Unless Length and Location are provided, units are Millimeters and Seconds.
 * </p>
 * 
 */
public interface MotionPlanner {

    /**
     * Perform the homing operation. This calls the home() methods of the underlying 
     * drivers and resets any virtual axes.  
     * 
     * @throws Exception
     */
    public void home() throws Exception;

    /**
     * Set the current physical or virtual axis positions to be reinterpreted as the specified coordinates. 
     * Used after visual homing and to reset a rotation angle after it has wrapped around. 
     * 
     * In G-Code parlance this is setting a global offset:
     * @see http://www.linuxcnc.org/docs/html/gcode/coordinates.html#_the_g92_commands
     * @see http://smoothieware.org/g92-cnc
     * @see https://github.com/synthetos/TinyG/wiki/Coordinate-Systems#offsets-to-the-offsets-g92  
     *  
     * @param machine
     * @param axesLocation 
     * @throws Exception
     */
    public void setGlobalOffsets(AxesLocation axesLocation) throws Exception;


    /**
     * Add a nominal moveTo() command to the motion sequence. Planner implementations are free to interpolate,
     * reorder, overlap and even slightly change the motion within the constraints of safe OpenPnP machine logic.
     *  
     * - Interpolation might include simulated jerk control for constant acceleration controllers etc.
     * 
     * - Optimization might alter motion in the Safe Z Zone, like overlapping Nozzle rotations, Z<->XY motion blending, 
     *   allowing uncoordinated X/Y moves for best acceleration etc.  
     * 
     * @param hm The HeadMountable having triggered the move. This is mostly for proprietary machine driver support  
     * and might only be a stand-in in some motion blending scenarios on the GcodeDriver.
     * @param axesLocation
     * @param speed
     * @param options
     * @throws Exception 
     */
    void moveTo(HeadMountable hm, AxesLocation axesLocation, double speed, 
            MoveToOption... options) throws Exception;

    public enum CompletionType {
        WaitForStillstand,
        Jog
    }
    /**
     * Perform a coordinated wait for completion. This will force planning and issuing of motion commands 
     * to the driver(s) and waiting for completion on all the drivers involved. This must be issued before 
     * relying on a specific machine positioning e.g. before capturing camera frames etc.  
     * 
     * @param hm The HeadMountable to wait for. If a hm's axes map to only a sub-set of drivers, this will not
     * wait for the other drivers and these are free to move on. If null, wait for all the drivers/machine axes. 
     * @param completionType The kind of completion wanted.
     * @throws Exception 
     */
    void waitForCompletion(HeadMountable hm, CompletionType completionType) throws Exception;

    /**
     * Get the planned motion at a certain time. It contains the nth order motion vector depending on the
     * planner at hand. 
     * 
     * @param time
     * @return
     */
    Motion getMomentaryMotion(double time);

    /**
     * Get the momentary location at a certain time. Simpler wrapper for the getMomentaryMotion() call.
     * 
     * @param time
     * @return
     */
    AxesLocation getMomentaryLocation(double time);

    /**
     * Clear the planning and recording older than the given time from the memory of the motion planner. The 
     * MotionPlanner is free to do its own house-keeping and get rid of past planning data before this is called. 
     * 
     * @param time The start time of the motion to keep. This must be a time returned by waitForCompletion().
     * 
     */
    void clearMotionOlderThan(double time);
    
    Machine getMachine();

    /**
     * @param axesLocation
     * @return true if the location is valid, i.e. inside soft limits etc.
     */
    public boolean isValidLocation(AxesLocation axesLocation);

}
