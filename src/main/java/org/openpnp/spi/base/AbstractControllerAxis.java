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

package org.openpnp.spi.base;

import org.openpnp.ConfigurationListener;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.spi.Axis;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Driver;
import org.simpleframework.xml.Attribute;

public abstract class AbstractControllerAxis extends AbstractCoordinateAxis implements ControllerAxis {

    private Driver driver;

    @Attribute(required = false)
    private String driverId;

    @Attribute(required = false)
    private String letter;

    /**
     * The coordinate that will be reached when the last motion sent by the driver has completed.
     * Always in driver units.
     */
    private double driverCoordinate;

    protected AbstractControllerAxis () {
        super();
        if (Configuration.isInstanceInitialized()) { // Allow unit tests without Configuration initialized.
            Configuration.get()
            .addListener(new ConfigurationListener.Adapter() {

                @Override
                public void configurationLoaded(Configuration configuration)
                        throws Exception {
                    driver = configuration.getMachine()
                            .getDriver(driverId);
                }
            });
        }
    }

    @Override
    public double getDriverCoordinate() {
        return driverCoordinate;
    }
    @Override
    public Length getDriverLengthCoordinate() {
        return new Length(driverCoordinate, getUnits());
    }

    @Override
    public void setDriverCoordinate(double driverCoordinate) {
        this.driverCoordinate = driverCoordinate;
        // Note, we do not firePropertyChange() as these changes are live from the machine thread,
        // and coordinate changes are handled through MachineListener.machineHeadActivity(Machine, Head).
    }
    @Override
    public void setDriverLengthCoordinate(Length driverCoordinate) {
        if (type == Type.Rotation) {
            // Never convert rotation angles.
            setDriverCoordinate(driverCoordinate.getValue());
        }
        else {
            setDriverCoordinate(driverCoordinate.convertToUnits(getUnits()).getValue());
        }
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
                coordinateA.convertToUnits(getUnits()).getValue(),
                coordinateB.convertToUnits(getUnits()).getValue());
        }
    }

    @Override
    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        Object oldValue = this.driver;
        this.driver = driver;
        this.driverId = (driver == null) ? null : driver.getId();
        firePropertyChange("driver", oldValue, driver);
    }

    @Override
    public LengthUnit getUnits() {
        if (getDriver() != null) {
            return getDriver().getUnits();
        }
        return LengthUnit.Millimeters;
    }

    @Override
    public String getLetter() {
        if (letter == null) {
            return "";
        }
        else {
            return letter;
        }
    }

    @Override
    public void setLetter(String letter) {
        Object oldValue = this.letter;
        this.letter = letter;
        firePropertyChange("letter", oldValue, letter);
    }
}
