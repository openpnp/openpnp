/*
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

import java.io.Closeable;

import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Identifiable;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Named;
import org.openpnp.model.Motion.MoveToCommand;
import org.openpnp.spi.MotionPlanner.CompletionType;

/**
 * Defines the interface for a driver that the MotionPlanner can drive. All methods result
 * in machine operations and most methods should block until they are complete or throw an error.
 * 
 * This Driver interface is intended to talk to one controller with Axes and Actuators attached.
 * 
 * Drivers should only expose the functionality of the controller in a unified way. They should not add 
 * additional logic other than what is needed to make an attached controller behave like any other. This is 
 * different from previous versions of OpenPnP where the driver did much more. 
 * 
 */
 public interface Driver extends Identifiable, Named, Closeable, WizardConfigurable, PropertySheetHolder {
    /**
     * @return The LengthUnit used by the controller that is driven by this driver. 
     */
    LengthUnit getUnits();

    /**
     * Perform the hardware homing operation. When this completes the axes should be at their homing location. 
     * The call might return before this physically happens, so a waitForCompletion() is needed if you need to be 
     * sure.
     * 
     * @param machine
     * @param homeLocation
     * @throws Exception
     */
    public void home(ReferenceMachine machine) throws Exception;

    /**
     * Set the current physical axis positions to be reinterpreted as the specified coordinates. 
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
    public void setGlobalOffsets(ReferenceMachine machine, AxesLocation axesLocation) throws Exception;

    /**
     * Executes the given Motion.
     * 
     * @param hm The HeadMountable having triggered the move. This is mostly for proprietary machine driver support  
     * and might only be a stand-in in some motion blending scenarios.
     * @param moveToCommand The moveTo Motion to execute, including target location and limits for feedrate, 
     * acceleration etc. as shaped by the MotionPlanner
     * @throws Exception
     */
    public void moveTo(ReferenceHeadMountable hm, MoveToCommand moveToCommand) throws Exception;

    /**
     * Get the momentary real-time location from the controller. This might be in mid-motion. 
     * @param timeout TODO
     * 
     * @return
     * @throws Exception
     */
    public AxesLocation getMomentaryLocation(long timeout) throws Exception;

    /**
     * @return true if a motion is still assumed to be pending, i.e. waitForCompletion() has not yet been called.  
     * 
     */
    public boolean isMotionPending();

    /**
     * Perform a coordinated wait for completion. This must be issued before capturing camera frames etc.
     * 
     * @param hm The HeadMountable to wait for. If null, wait for all the axes on the driver. Most drivers/controllers will probably 
     * not be able to wait for just a sub-set of axes, so the'll wait for all the axes anyway. 
     * @param completionType The kind of completion wanted.
     * @throws Exception 
     */
    public void waitForCompletion(ReferenceHeadMountable hm, CompletionType completionType) throws Exception;

    /**
     * Actuates a machine defined object with a boolean state.
     * 
     * @param actuator
     * @param on
     * @throws Exception
     */
    public void actuate(ReferenceActuator actuator, boolean on) throws Exception;

    /**
     * Actuates a machine defined object with a double value.
     * 
     * @param actuator
     * @param on
     * @throws Exception
     */
    public void actuate(ReferenceActuator actuator, double value) throws Exception;

    /**
     * Actuates a machine defined object with a String value.
     * 
     * @param actuator
     * @param on
     * @throws Exception
     */
    public default void actuate(ReferenceActuator actuator, String value) throws Exception {
    }

    /**
     * Read a String value from the given Actuator.
     * 
     * @param actuator
     * @return
     * @throws Exception
     */
    public default String actuatorRead(ReferenceActuator actuator) throws Exception {
        return null;
    }

    /**
     * Read a given String value from the given Actuator.
     * 
     * @param actuator
     * @param parameter
     * @return 
     * @throws Exception
     */
    public default String actuatorRead(ReferenceActuator actuator, double parameter) throws Exception {
        return null;
    }

    /**
     * Attempts to enable the Driver, turning on all outputs.
     * 
     * @param enabled
     * @throws Exception
     */
    public void setEnabled(boolean enabled) throws Exception;

    boolean isSupportingPreMove();

    boolean isUsingLetterVariables();

    /**
     * The MotionControlType determines how the OpenPnP MotionPlanner will do its planning and how it will talk 
     * to the controller. 
     *
     */
    public enum MotionControlType {
        /**
         * Apply the nominal driver feed-rate limit multiplied by the speed factor to the tool-path. 
         * The driver feed-rate must be specified. No acceleration control is applied. 
         */
        ToolpathFeedRate,
        /**
         * Apply axis feed-rate, acceleration and jerk limits multiplied by the proper speed factors. 
         * The Euclidean Metric is calculated to allow the machine to run faster in a diagonal.
         * All profile motion control is left to the controller.   
         */
        EuclideanAxisLimits,
        /**
         * Apply motion planning assuming a controller with constant acceleration motion control. 
         */
        ConstantAcceleration,
        /**
         * Apply motion planning assuming a controller constant acceleration motion control but
         * moderate the acceleration and velocity to resemble those of 3rd order control, resulting
         * in a move that takes the same amount of time. 
         * 
         */
        ModeratedConstantAcceleration,
        /**
         * Apply motion planning assuming a controller with simplified S-Curve motion control. 
         * Simplified S-Curves have no constant acceleration phase, only jerk phases (e.g. TinyG, Marlin). 
         */
        SimpleSCurve,
        /**
         * Apply motion planning assuming a controller constant acceleration motion control but
         * simulating 3rd order control with time step interpolation.  
         */
        Simulated3rdOrderControl,
        /**
         * Apply motion planning assuming a controller with full 3rd order motion control. 
         */
        Full3rdOrderControl;

        public boolean isConstantAcceleration() {
            return this == ToolpathFeedRate || this == ConstantAcceleration;
        }

        public boolean isInterpolated() {
            return this == Simulated3rdOrderControl;
        }
    }

    /**
     * @return The MotionControlType that determines how the OpenPnP MotionPlanner will do its planning and how it 
     * will talk to the controller. 
     */
    public MotionControlType getMotionControlType();

    /**
     * @return A driver specific feed-rate limit, applied in addition to axis feed-rate limits and according to 
     * NIST RS274NGC Interpreter - Version 3, Section 2.1.2.5 (p. 7). Given per second (will be converted to the 
     * standard per minute rate when using Gcode F values). 
     *   
     */
    public Length getFeedRatePerSecond();

    /**
     * @return The maximum number of interpolation steps that should be applied to simulate more advanced motion 
     * control. Usually depends on the depth of the look-ahead motion planner queue inside the controller.   
     */
    public default Integer getInterpolationMaxSteps() {
        return null; 
    }

    /**
     * @return The minimal time step that should be used to simulate more advanced motion control. 
     */
    public default Double getInterpolationTimeStep() {
        return null;
    }

    /**
     * @return The minimal interpolation step distance, given in resolution ticks (i.e. usually micro-steps) of the axes. 
     */
    public default Integer getInterpolationMinStep() {
        return null;
    }

    /**
     * @return The minimum velocity the driver supports, in mm/s. Used to prevent "rounded to zero" errors caused in
     * interpolation. 
     */
    double getMinimumVelocity();
}
