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
import org.openpnp.machine.reference.axis.wizards.ReferenceCamClockwiseAxisConfigurationWizard;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Locatable.LocationOption;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.AbstractSingleTransformedAxis;

/**
 * A TransformedAxis for heads with dual rocker or seesaw driven Z axes powered by one motor. 
 * The two Z axes are defined as counter-clockwise and clockwise according how the rocker rotates. 
 */
public class ReferenceCamClockwiseAxis extends AbstractSingleTransformedAxis {

    public ReferenceCamClockwiseAxis() {
        super();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceCamClockwiseAxisConfigurationWizard((AbstractMachine)Configuration.get().getMachine(), this);
    }

    public ReferenceCamCounterClockwiseAxis getCounterClockwiseAxis() {
        if (inputAxis != null) {
            return (ReferenceCamCounterClockwiseAxis)inputAxis;
        }
        return null;
    }

    @Override
    public AxesLocation toRaw(AxesLocation location, LocationOption... options) throws Exception {
        if (getCounterClockwiseAxis() == null) {
            throw new Exception(getName()+" has no counter-clock input axis set");
        }
        double transformedCoordinate = location.getCoordinate(this);
        double rawCoordinate = getCounterClockwiseAxis().toRawCoordinate(transformedCoordinate, true);
        // store the transformed input axis (we're skipping the counter-clock axis)
        location = location.put(new AxesLocation(getCounterClockwiseAxis().getInputAxis(), rawCoordinate));
        // recurse
        return getCounterClockwiseAxis().getInputAxis().toRaw(location, options);
    }

    @Override
    public AxesLocation toTransformed(AxesLocation location, LocationOption... options) {
        if (getCounterClockwiseAxis() == null) {
            return location.put(new AxesLocation(this, 0.0));
        }
        // recurse
        location = getCounterClockwiseAxis().toTransformed(location, options);
        // get the input of the input (we're skipping the counter-clock axis)
        double rawCoordinate = location.getCoordinate(getCounterClockwiseAxis().getInputAxis());
        double transformedCoordinate  = getCounterClockwiseAxis().toTransformedCoordinate(rawCoordinate, true);
        return location.put(new AxesLocation(this, transformedCoordinate));
    }
}
