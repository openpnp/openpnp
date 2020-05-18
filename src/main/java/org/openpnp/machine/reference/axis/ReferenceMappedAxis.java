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
import org.openpnp.machine.reference.axis.wizards.ReferenceMappedAxisConfigurationWizard;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.spi.Movable.LocationOption;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.AbstractSingleTransformedAxis;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Element;

/**
 * A TransformedAxis for heads with dual linear Z axes powered by one motor. The two Z axes are
 * defined as normal and negated. Normal gets the raw coordinate value and negated gets the same
 * value negated. So, as normal moves up, negated moves down.
 */
public class ReferenceMappedAxis extends AbstractSingleTransformedAxis {
    @Element(required = false)
    private Length mapInput0 = new Length(0.0, LengthUnit.Millimeters);

    @Element(required = false)
    private Length mapOutput0 = new Length(0.0, LengthUnit.Millimeters);

    @Element(required = false)
    private Length mapInput1 = new Length(1.0, LengthUnit.Millimeters);

    @Element(required = false)
    private Length mapOutput1 = new Length(1.0, LengthUnit.Millimeters);

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceMappedAxisConfigurationWizard((AbstractMachine)Configuration.get().getMachine(), this);
    }

    protected double getScale() {
        double dividend = (mapOutput1.convertToUnits(AxesLocation.getUnits()).getValue() 
                - mapOutput0.convertToUnits(AxesLocation.getUnits()).getValue()); 
        double divisor = (mapInput1.convertToUnits(AxesLocation.getUnits()).getValue() 
                - mapInput0.convertToUnits(AxesLocation.getUnits()).getValue());
        if (divisor == 0.0 || dividend == 0.0) {
            Logger.info("[ReferenceMappedAxis] "+getName()+" input/output range must not be zero. Scale defaults to 1.");
            return 1.0;
        }
        return dividend / divisor;
    }

    @Override
    public AxesLocation toTransformed(AxesLocation location, LocationOption... options) {
        if (inputAxis == null) {
            return location.put(new AxesLocation(this, 0.0));
        }
        location = inputAxis.toTransformed(location, options);
        // To transformed, i.e. forward mapped transform:
        double coordinate = location.getCoordinate(inputAxis);
        double scale = getScale();
        coordinate = coordinate - mapInput0.convertToUnits(AxesLocation.getUnits()).getValue();
        coordinate = coordinate * scale; 
        coordinate = coordinate + mapOutput0.convertToUnits(AxesLocation.getUnits()).getValue(); 
        return location.put(new AxesLocation(this, coordinate));
    }

    @Override
    public AxesLocation toRaw(AxesLocation location, LocationOption... options) 
            throws Exception {
        if (inputAxis == null) {
            throw new Exception(getName()+" has no input axis set");
        }

        // To raw, i.e. reversed mapped transform:
        double coordinate = location.getCoordinate(this);
        double scale = getScale();
        coordinate = coordinate - mapOutput0.convertToUnits(AxesLocation.getUnits()).getValue();
        coordinate = coordinate / scale; 
        coordinate = coordinate + mapInput0.convertToUnits(AxesLocation.getUnits()).getValue(); 
        return toRaw(location.put(new AxesLocation(inputAxis, coordinate)), options);
    }

    public Length getMapInput0() {
        return mapInput0;
    }

    public void setMapInput0(Length mapInput0) {
        this.mapInput0 = mapInput0;
    }

    public Length getMapOutput0() {
        return mapOutput0;
    }

    public void setMapOutput0(Length mapOutput0) {
        this.mapOutput0 = mapOutput0;
    }

    public Length getMapInput1() {
        return mapInput1;
    }

    public void setMapInput1(Length mapInput1) {
        this.mapInput1 = mapInput1;
    }

    public Length getMapOutput1() {
        return mapOutput1;
    }

    public void setMapOutput1(Length mapOutput1) {
        this.mapOutput1 = mapOutput1;
    }
}
