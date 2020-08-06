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

/**
 * A CoordinateAxis is an axis which can store a coordinate either physically or virtually. 
 */

public interface CoordinateAxis extends Axis {
    /**
     * @return The coordinate that was last sent to the MotionPlanner either through a moveTo() or other
    * operations, such as homing. 
    * 
    * This coordinate may not yet be sent to the driver and even less likely reflect the 
    * physical machine position. Only after a waitForCompletion() on the MotionPlanner, can you be sure that 
    * the coordinate is in sync with the machine. 
    * 
    * The coordinate is always in AxesLocation.getUnits() i.e. in Millimeters.
     */
    public double getCoordinate();

    /**
     * @return The coordinate that was last sent to the MotionPlanner either through a moveTo() or other
    * operations, such as homing. 
    * 
    * This coordinate may not yet be sent to the driver and even less likely reflect the 
    * physical machine position. Only after a waitForCompletion() on the MotionPlanner, can you be sure that 
    * the coordinate is in sync with the machine. 
    * 
    * The coordinate is always in AxesLocation.getUnits() i.e. in Millimeters.
     */
    public Length getLengthCoordinate();

    public void setCoordinate(double coordinate);

    public void setLengthCoordinate(Length coordinate);

    /**
     * @param coordinateA
     * @param coordinateB
     * @return Whether the coordinates match in their native unit length and resolution as 
     * determined by the sub-class and possibly by a driver.  
     */
    boolean coordinatesMatch(Length coordinateA, Length coordinateB);

    /**
     * @param coordinateA
     * @param coordinateB
     * @return Whether the coordinates match in their native unit length and resolution as 
     * determined by the sub-class and possibly by a driver.  
     */
    boolean coordinatesMatch(double coordinateA, double coordinateB);

    public Length getHomeCoordinate();

    public void setHomeCoordinate(Length homeCoordinate);
}
