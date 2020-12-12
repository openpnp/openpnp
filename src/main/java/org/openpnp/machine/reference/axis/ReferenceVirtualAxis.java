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

package org.openpnp.machine.reference.axis;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.axis.wizards.ReferenceVirtualAxisConfigurationWizard;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Length;
import org.openpnp.spi.Axis;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.base.AbstractCoordinateAxis;

/**
 * The ReferenceVirtualAxis is a pseudo-axis used to track a coordinate virtually i.e. without
 * moving a physical axis.
 */
public class ReferenceVirtualAxis extends AbstractCoordinateAxis implements CoordinateAxis {

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceVirtualAxisConfigurationWizard(this);
    }

    @Override
    public boolean coordinatesMatch(Length coordinateA, Length coordinateB) {
        if (type == Axis.Type.Rotation) {
            // Never convert rotation
            return coordinatesMatch(
                    coordinateA.getValue(),
                    coordinateB.getValue());
        }
        else {
            return coordinatesMatch(
                coordinateA.convertToUnits(AxesLocation.getUnits()).getValue(),
                coordinateB.convertToUnits(AxesLocation.getUnits()).getValue());
        }
    }

    protected long getResolutionTicks(double coordinate) {
        return Math.round(coordinate/0.0001);
    }

    @Override
    public boolean isInSafeZone(Length coordinate) {
        coordinate = coordinate.convertToUnits(AxesLocation.getUnits());
        Length homeCoordinate = getHomeCoordinate().convertToUnits(AxesLocation.getUnits());
        return coordinatesMatch(coordinate, homeCoordinate)
                || coordinate.getValue() > homeCoordinate.getValue();
    }
}
