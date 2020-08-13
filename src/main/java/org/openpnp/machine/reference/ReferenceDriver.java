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

package org.openpnp.machine.reference;

import java.io.Closeable;

import org.openpnp.model.AxesLocation;
import org.openpnp.model.Motion;
import org.openpnp.spi.Driver;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.WizardConfigurable;

/**
 * Defines the interface for a simple driver that the MotionPlanner can drive. All methods result
 * in machine operations and most methods should block until they are complete or throw an error.
 * 
 * This Driver interface is intended to talk to one controller with Axes and Actuators attached.
 * 
 * In OpenPnP, the Head does not move on it's own. It is moved by the moving of attached objects:
 * Nozzles, Cameras, Actuators, so-called HeadMountables. The HeadMountables specify by which axes 
 * they are moved. The axes are in turn assigned to the Driver. When you move a HeadMountable the 
 * MotionPlanner will determine which axes are involved and call the drivers accordingly. 
 * 
 * Drivers should only expose the functionality of the controller in a unified way. They should not add 
 * additional logic other than what is needed to make an attached controller behave like any other. This is 
 * different from previous versions of OpenPnP where the driver did much more. 
 * 
 */
public interface ReferenceDriver extends Driver, WizardConfigurable, PropertySheetHolder, Closeable {
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
     * @param motion The moveTo Motion to execute, including target location, feedrate, acceleration etc. as shaped by 
     * the MotionPlanner
     * @throws Exception
     */
    public void moveTo(ReferenceHeadMountable hm, Motion motion) throws Exception;

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

    public default void createDefaults() throws Exception  {}

    /**
     * Migrates the driver for the new global axes implementation. Is marked a deprecated as it can be removed
     * along with the old GcodeDriver Axes implementation, once migration of users is expected to be complete.  
     * 
     * @param machine
     * @throws Exception
     */
    @Deprecated
    void migrateDriver(ReferenceMachine machine) throws Exception;

}
