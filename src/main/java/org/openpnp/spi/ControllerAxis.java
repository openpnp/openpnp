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
import org.openpnp.model.Location;

/**
 * A ControllerAxis is an axis coordinate dimension as exposed by the machine's controller/driver.
 * These can already be Cartesian-coordinate or rotary axes, or raw actuator axes to be transformed by a TransformedAxis. 
 */
public interface ControllerAxis extends LinearInputAxis {
    /**
     * @return the driver through which this ControllerAxis is controlled. 
     */
    public Driver getDriver();
    
    public void setDriver(Driver driver);

    /**
     * @return the letter (X, Y, Z, etc.) of the ControllerAxis as recognized by the machine controller.
     */
    public String getLetter();

    public void setLetter(String designator);

    /**
     * @return the coordinate after doing the machine controller's homing.  
     */
    public Length getHomeCoordinate();

    public void setHomeCoordinate(Length homeCoordinate);

    public LengthUnit getUnits();

    public double getCoordinate();

    public Length getLengthCoordinate();

    public void setCoordinate(double coordinate);

    public void setLengthCoordinate(Length coordinate);

    double roundedToResolution(double coordinate);

    public boolean locationCoordinateMatches(Location locationA, Location locationB);

}
