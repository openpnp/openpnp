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

import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;

/**
 * A ControllerAxis is an axis coordinate dimension as exposed by the machine's controller/driver.
 * These can already be Cartesian linear or rotary axed, or raw actuator axes to be transformed by a TransformedAxis. 
 */
public interface ControllerAxis extends LinearInputAxis, CoordinateAxis {
    /**
     * @return the driver through which this ControllerAxis is controlled. 
     */
    Driver getDriver();
    
    void setDriver(Driver driver);

    /**
     * @return the letter (X, Y, Z, etc.) of the ControllerAxis as recognized by the machine controller.
     */
    public String getLetter();

    public void setLetter(String designator);

    public LengthUnit getUnits();

    /**
     * @return The driver coordinate in length units as determined by getUnits(). This is the coordinate
     * that was last sent to the controller. It may not yet reflect the physical machine position. Only
     * after a MotionController.waitForCompletion() can you be sure that the machine is in sync.    
     */
    double getDriverCoordinate();
    
    void setDriverCoordinate(double coordinate);

    Length getDriverLengthCoordinate();
    
    void setDriverLengthCoordinate(Length coordinate);

    /**
     * Get the nth order motion limit in AxesLocation units (mm) per second^n.
     * 
     * @param order
     * @return 
     */
    double getMotionLimit(int order);

    /**
     * @return Whether the axis is handled as rotational in the controller. This happens if the user is forced
     * to use axes that are rotational for linear axes and vice versa.
     */
    boolean isRotationalOnController();

}
