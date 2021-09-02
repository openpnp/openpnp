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
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.spi.LinearInputAxis;
import org.openpnp.spi.Machine;
import org.simpleframework.xml.Attribute;

public abstract class AbstractSingleTransformedAxis extends AbstractTransformedAxis implements LinearInputAxis {
    // The input axis of the transformation.  
    protected AbstractAxis inputAxis;

    @Attribute(required = false)
    private String inputAxisId;

    protected AbstractSingleTransformedAxis() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {

            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                inputAxis = (AbstractAxis) configuration.getMachine().getAxis(inputAxisId);
            }
        });
    }

    @Override
    public AxesLocation getCoordinateAxes(Machine machine) {
        if (inputAxis != null) {
            return inputAxis.getCoordinateAxes(machine);
        }
        return null;
    }

    public AbstractAxis getInputAxis() {
        return inputAxis;
    }

    public void setInputAxis(AbstractAxis inputAxis) {
        Object oldValue = this.inputAxis;
        this.inputAxis = inputAxis;
        this.inputAxisId = (inputAxis == null) ? null : inputAxis.getId();
        firePropertyChange("inputAxis", oldValue, inputAxis);
    }
}
