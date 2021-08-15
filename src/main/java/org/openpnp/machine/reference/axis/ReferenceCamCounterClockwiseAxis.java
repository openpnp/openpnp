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
import org.I18n.I18n;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.axis.wizards.ReferenceCamCounterClockwiseAxisConfigurationWizard;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.spi.Locatable.LocationOption;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.AbstractSingleTransformedAxis;
import org.simpleframework.xml.Element;

/**
 * A TransformedAxis for heads with dual rocker or seesaw driven Z axes powered by one motor. 
 * The two Z axes are defined as counter-clockwise and clockwise according how the rocker axis
 * rotates when creating positive Z movement. The zero° point is tied to the balance point.
 * 
 * The classical seesaw model is extended by an angle between the two arms. Normally, a 180°
 * angle defines straight-across arms. If the angle is 0°, it describes a one-armed design that
 * always only pushes one side/nozzle at a time. Other "V-shaped" angles could be used to describe 
 * designs, that eliminate dead-time, that the one-armed design has.
 * 
 * Designs with an arms angle less than 180° (especially the one-armed design) may move more than 
 * 180° i.e more than ± 90° on the arm. This means the sinus of the arm angle would go back down. 
 * But we need a strictly monotonic function so the axis transformation can be inverted. Therefore, 
 * we pretend the transformed axis will slightly move up further in Z, as the arm rotates further 
 * to push down the other nozzle. We create a piece-wise extension slope after the sinus has 
 * reached its maximum (or rather its cutoff value). This is barely visible to the user (in decimal 
 * representation), but still well-defined in the binary double representation. It is also 
 * completely irrelevant mechanically, as there are likely stops, limiting Z travel well before 
 * that point.
 * 
 */
public class ReferenceCamCounterClockwiseAxis extends AbstractSingleTransformedAxis {
    /**
     * The angle cutoff is slightly reduced from 90° to prevent the slope (derivative) from reaching
     * truly zero and the function losing its strict monotonicity. This could be a problem for numerical 
     * solvers that may work on top of the transformation in the future. This is probably not strictly 
     * needed at the moment, but better be safe than sorry.   
     */
    final static double ANGLE_CUTOFF = 89.99; 
    final static double SINUS_CUTOFF = Math.sin(Math.toRadians(ANGLE_CUTOFF));
    final static double EXTENSION_SLOPE = 0.0001/180.0;

    @Element(required = false)
    private Length camRadius = new Length(24.0, LengthUnit.Millimeters);

    @Element(required = false)
    private double camArmsAngle = 180.0;

    @Element(required = false)
    private Length camWheelRadius = new Length(0, LengthUnit.Millimeters);

    @Element(required = false)
    private Length camWheelGap = new Length(0, LengthUnit.Millimeters);

    public ReferenceCamCounterClockwiseAxis() {
        super();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceCamCounterClockwiseAxisConfigurationWizard((AbstractMachine)Configuration.get().getMachine(), this);
    }

    @Override
    public AxesLocation toRaw(AxesLocation location, LocationOption... options) throws Exception {
        if (inputAxis == null) {
            throw new Exception(getName()+I18n.gettext(" has no input axis set"));
        }
        double transformedCoordinate = location.getCoordinate(this);
        double rawCoordinate = toRawCoordinate(transformedCoordinate, false);
        // store the new coordinate
        location = location.put(new AxesLocation(inputAxis, rawCoordinate));
        // recurse
        return inputAxis.toRaw(location, options);
    }

    protected double toRawCoordinate(double transformedCoordinate, boolean clockwise) throws Exception {
        double rawCoordinate = (transformedCoordinate 
                - camWheelRadius.convertToUnits(AxesLocation.getUnits()).getValue() 
                - camWheelGap.convertToUnits(AxesLocation.getUnits()).getValue()) 
                / camRadius.convertToUnits(AxesLocation.getUnits()).getValue();
        if (rawCoordinate <= -SINUS_CUTOFF) {
            rawCoordinate = (rawCoordinate + SINUS_CUTOFF)/EXTENSION_SLOPE - ANGLE_CUTOFF;  
        }
        else if (rawCoordinate >= +SINUS_CUTOFF) {
            rawCoordinate = (rawCoordinate - SINUS_CUTOFF)/EXTENSION_SLOPE + ANGLE_CUTOFF;  
        }
        else {
            rawCoordinate = Math.toDegrees(Math.asin(rawCoordinate));
        }
        // The balance angle is shifted by 90° minus half the arms angle i.e. arms folded out 180° 
        // (90° on each side) are already balanced when at °0. 
        rawCoordinate -= 90.0 - camArmsAngle/2.0;
        if (clockwise) {
            rawCoordinate = -rawCoordinate;
        }
        // Limit to the useful range.
        double range = 180.0 - camArmsAngle/2.0;
        rawCoordinate = Math.max(-range,  Math.min(range, rawCoordinate));
        return rawCoordinate;
    }

    @Override
    public AxesLocation toTransformed(AxesLocation location, LocationOption... options)  {
        if (inputAxis == null) {
            return location.put(new AxesLocation(this, 0.0));
        }
        // recurse
        location = inputAxis.toTransformed(location, options);
        double rawCoordinate = location.getCoordinate(inputAxis);
        double transformedCoordinate  = toTransformedCoordinate(rawCoordinate, false);
        return location.put(new AxesLocation(this, transformedCoordinate));
    }

    protected double toTransformedCoordinate(double rawCoordinate, boolean clockwise) {
        double transformedCoordinate = rawCoordinate;
        if (clockwise) {
            transformedCoordinate = -transformedCoordinate;
        }
        // The balance angle is shifted by 90° minus half the arms angle i.e. arms folded out 180° 
        // (90° on each side) are already balanced when at °0. 
        transformedCoordinate += 90.0 - camArmsAngle/2.0;
        if (transformedCoordinate <= -ANGLE_CUTOFF) {
            transformedCoordinate = (transformedCoordinate + ANGLE_CUTOFF)*EXTENSION_SLOPE - SINUS_CUTOFF;
        }
        else if (transformedCoordinate >= +ANGLE_CUTOFF) {
            transformedCoordinate = (transformedCoordinate - ANGLE_CUTOFF)*EXTENSION_SLOPE + SINUS_CUTOFF;
        }
        else {
            transformedCoordinate = Math.sin(Math.toRadians(transformedCoordinate));
        }
        transformedCoordinate *= camRadius.convertToUnits(AxesLocation.getUnits()).getValue();
        transformedCoordinate += 
                camWheelRadius.convertToUnits(AxesLocation.getUnits()).getValue() 
                + camWheelGap.convertToUnits(AxesLocation.getUnits()).getValue();
        return transformedCoordinate;
    }

    public Length getCamRadius() {
        return camRadius;
    }

    public void setCamRadius(Length camRadius) {
        this.camRadius = camRadius;
    }

    public double getCamArmsAngle() {
        return camArmsAngle;
    }

    public void setCamArmsAngle(double camArmsAngle) {
        this.camArmsAngle = camArmsAngle;
    }

    public Length getCamWheelRadius() {
        return camWheelRadius;
    }

    public void setCamWheelRadius(Length camWheelRadius) {
        this.camWheelRadius = camWheelRadius;
    }

    public Length getCamWheelGap() {
        return camWheelGap;
    }

    public void setCamWheelGap(Length camWheelGap) {
        this.camWheelGap = camWheelGap;
    }
    
}
