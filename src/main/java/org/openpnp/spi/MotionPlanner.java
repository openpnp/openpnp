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
import org.openpnp.model.Motion.MotionOption;

/**
 * <p>
 * The MotionPlanner coordinates motion, homing etc. across multiple drivers. It has a similar interface as the 
 * drivers themselves and acts as a black box to abstract from the complexities of multiple, mixed drivers.</p>
 * 
 * <p>In OpenPnP, the Head does not move on it's own. It is moved by the moving of attached objects:
 * Nozzles, Cameras, Actuators, so-called HeadMountables. The HeadMountables specify by which axes 
 * they are moved. The axes are in turn assigned to the Driver. When you move a HeadMountable the 
 * MotionPlanner will determine which axes are involved and call the drivers accordingly.</p> 
 * 
 * <p>The Motion planner may also perform advanced motion planning i.e. it might reorder, blend and interpolate the 
 * original moveTo() commands in order to improve performance, reduce machine vibrations etc.</p>
 * 
 * <p>These are the most important tasks:</p>  
 * <ul>
 * <li>Perform home() commands across drivers.</li>
 * <li>Accept a sequence of original moveTo() commands.</li>
 * <li>Coordinate motion across multiple drivers.</li>
 * <li>For advanced MotionPlanners, perform optimization on the sequence. </li>
 * <li>As soon as the motion is commited i.e. when waiting for completion, command the drivers to plan and execute the plan.</li>
 * <li>Provide access to the planned motion over time for coordination, simulation, visualization etc. </li>
 * </ul>
 * <p>
 * A MotionPlanner can be a simple proxy, just sending commands directly to the drivers. See the 
 * NullMotionPlanner. Future advanced MotionPlanners can do nth order Kinematics control, motion blending
 * etc. 
 * </p><p>
 * The MotionPlanner is an envelope planner i.e. it will only plan the maximum velocity, acceleration, jerk 
 * allowed to the controller. It is not a real-time planner, therefore the real physical motion that is later executed 
 * by the controller, might be more constrained i.e. take more time. Having said that, it remains a goal to anticipate 
 * the controller's motion as good as possible, so that simulation and reality will be very similar and optimizations 
 * are purposeful. 
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
 * Unless Length and Location are used, units are Millimeters and Seconds.
 * </p>
 * 
 */
public interface MotionPlanner extends PropertySheetHolder {

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
     * and might only be a stand-in in some motion blending scenarios. 
     * @param axesLocation
     * @param speed
     * @param options
     * @throws Exception 
     */
    void moveTo(HeadMountable hm, AxesLocation axesLocation, double speed, 
            MotionOption... options) throws Exception;

    public enum CompletionType {
        /**
         * The motion plan is sent to the drivers, assuming an unfinished motion sequence (e.g. when Jogging). 
         * More specifically, the motion planner's deceleration to still-stand will not be enforced, it is entirely 
         * up to the controller. If a subsequent motion command arrives soon enough, there might be no (or less) deceleration
         * between the moves.<br/>
         * If the driver supports asynchronous execution, this does not wait for the driver to physically complete. 
         */
        CommandJog,
        /**
         * The motion plan is sent to the drivers, finishing in still-stand.<br/>
         * If the driver supports asynchronous execution, this does not wait for the driver to physically complete.
         */
        CommandStillstand,
        /**
         * The motion plan is executed with the drivers, finishing in still-stand.<br/>
         * This does always wait for the driver to complete i.e. the caller can be sure the machine has physically arrived 
         * at the final location.  
         */
        WaitForStillstand,
        
        /**
         * Wait forever.
         */
        WaitForStillstandIndefinitely;

        public boolean isEnforcingStillstand() {
            return isWaitingForDrivers() || this == CommandStillstand;
        }

        public boolean isWaitingForDrivers() {
            return this == WaitForStillstand || this == WaitForStillstandIndefinitely;
        }
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
     * Get the planned motion at a certain time. Works into the future as far as planned and into the past
     * as far as retained. This is used to simulate Camera Views, to analyze excitation by acceleration to 
     * predict vibrations etc.   
     * 
     * @param time
     * @return
     */
    Motion getMomentaryMotion(double time);

     /**
     * Clear the motion planning older than the given real-time from the history of the motion planner. The 
     * MotionPlanner is free to do its own house-keeping and get rid of past planning data before this is called. 
     * 
     * @param time The start time of the motion to keep. This must be a time returned by waitForCompletion().
     * 
     */
    void clearMotionPlanOlderThan(double time);

    /**
     * @param axesLocation
     * @return true if the location is valid, i.e. inside soft limits etc.
     */
    public boolean isValidLocation(AxesLocation axesLocation);
}
