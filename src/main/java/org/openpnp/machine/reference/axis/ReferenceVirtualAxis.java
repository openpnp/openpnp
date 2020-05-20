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
import org.openpnp.model.LengthUnit;
import org.openpnp.model.MappedAxes;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Movable.LocationOption;
import org.openpnp.spi.base.AbstractAxis;
import org.simpleframework.xml.Element;

/**
 * The ReferenceVirtualAxis is a pseudo-axis used to track a coordinate virtually i.e. without
 * moving a physical axis.
 */
public class ReferenceVirtualAxis extends AbstractAxis implements CoordinateAxis {
    @Element(required = false)
    private Length homeCoordinate = new Length(0.0, LengthUnit.Millimeters);

    /**
     * The coordinate stored here is the last commanded coordinate. If this virtual axis is moved along with physical axes, 
     * there can be motion that is still ongoing. 
     */
    private double coordinate = 0;

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceVirtualAxisConfigurationWizard(this);
    }

    @Override
    public double getCoordinate() {
        return coordinate;
    }

    @Override
    public Length getLengthCoordinate() {
        return new Length(coordinate, getUnits());
    }

    @Override
    public void setCoordinate(double coordinate) {
        Object oldValue = this.coordinate;
        this.coordinate = coordinate;
        firePropertyChange("coordinate", oldValue, coordinate);
        firePropertyChange("lengthCoordinate", null, getLengthCoordinate());
    }

    @Override
    public void setLengthCoordinate(Length coordinate) {
        setCoordinate(coordinate.convertToUnits(getUnits()).getValue());
    }

    @Override
    public LengthUnit getUnits() {
        return LengthUnit.Millimeters;
    }

    @Override
    public Length getHomeCoordinate() {
        return homeCoordinate;
    }

    @Override
    public void setHomeCoordinate(Length homeCoordinate) {
        Object oldValue = this.homeCoordinate;
        this.homeCoordinate = homeCoordinate;
        firePropertyChange("homeCoordinate", oldValue, homeCoordinate);
    }

    @Override
    public MappedAxes getControllerAxes(Machine machine) {
        // no controller axes
        return MappedAxes.empty;
    }

    @Override
    public AxesLocation toTransformed(AxesLocation location, LocationOption... options) {
        // No transformation, obviously
        return location;
    }

    @Override
    public AxesLocation toRaw(AxesLocation location, LocationOption... options) {
        // No transformation, obviously
        return location;
    }
}
