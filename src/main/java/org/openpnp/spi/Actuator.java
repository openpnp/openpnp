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

import org.openpnp.gui.support.Wizard;
import org.openpnp.model.AxesLocation;
import org.openpnp.spi.base.AbstractActuator;

/**
 * Defines a simple interface to some type of device that can be actuated on the machine or on a
 * head. This is a minimal interface and it is expected that concrete implementations may have many
 * other capabilities exposed in their specific implementations.
 */
public interface Actuator
        extends HeadMountable, WizardConfigurable, PropertySheetHolder {
    /**
     * @return the driver through which this Actuator is controlled. 
     */
    public Driver getDriver();
    
    public void setDriver(Driver driver);

    /**
     * Turns the Actuator on or off.
     * 
     * @param on
     * @throws Exception
     */
    public void actuate(boolean on) throws Exception;

    /**
     * Provides the actuator with a double value to which it can respond in an implementation
     * dependent manner.
     * 
     * @param value
     * @throws Exception
     */
    public void actuate(double value) throws Exception;

    /**
     * Provides the actuator with a String value to which it can respond in an implementation
     * dependent manner.
     * 
     * @param value
     * @throws Exception
     */
    public void actuate(String value) throws Exception;

    /**
     * @return the last actuation value or null if no actuation has happened since the last homing.
     */
    public Object getLastActuationValue();

    /**
     * Read a value from the actuator. The value will be returned exactly as provided by the
     * Actuator and can be interpreted as needed by the caller. 
     * @return The value read.
     * @throws Exception if there was an error reading the actuator.
     */
    public String read() throws Exception;

    public <T> String read(T value) throws Exception;

    boolean isCoordinatedBeforeActuate();

    boolean isCoordinatedAfterActuate();

    boolean isCoordinatedBeforeRead();

    /**
     * The InterlockMonitor controls an actuator to perform an interlock functions in the course of machine motion. 
     * It can actuate its actuator according to specific axis positions or movements. Or it can read its actuator to 
     * confirm the safety of axis movement or lock against it, avoiding potentially dangerous machine motion.
     *
     */
    public static interface InterlockMonitor {
        /**
         * This method is called before and after a move is executed by the motion planner. All interlock action must be performed here.
         * 
         * @param actuator
         * @param location0 Move start location 
         * @param location1 Move end location
         * @param beforeMove true if called before the move, false if called after the move.
         * @param speed Move effective speed
         * @return true if the interlock conditions applied. 
         * @throws Exception 
         * 
         */
        boolean interlockActuation(Actuator actuator, AxesLocation location0, AxesLocation location1, boolean beforeMove, double speed) throws Exception;

        Wizard getConfigurationWizard(AbstractActuator actuator);
    }

    public InterlockMonitor getInterlockMonitor();
}
