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
import org.openpnp.machine.reference.axis.wizards.ReferenceControllerAxisConfigurationWizard;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Movable.LocationOption;
import org.openpnp.spi.base.AbstractControllerAxis;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class ReferenceControllerAxis extends AbstractControllerAxis {
    // The more implementation specific properties are in the ReferenceControllerAxis

    @Element(required = false)
    private Length backlashOffset = new Length(0.0, LengthUnit.Millimeters);
    
    /**
     * If limitRotation is enabled the nozzle will reverse directions when commanded to rotate past
     * 180 degrees. So, 190 degrees becomes -170 and -190 becomes 170.
     */
    @Attribute(required = false)
    private boolean limitRotation = false;

    /**
     * If wrapAroundRotation is enabled the nozzle rotation will always wrap-arround the shorter way. If combined 
     * with limitRotation the rotation angle will be reset on the controller to stay within +/-180° (if supported 
     * by the driver setup i.e. needs special G-Code).
     */
    @Attribute(required = false)
    private boolean wrapAroundRotation = false;

    @Element(required = false, data = true)
    private String preMoveCommand;

    @Element(required = false)
    private Length softLimitLow = new Length(0.0, LengthUnit.Millimeters);
    
    @Attribute(required = false)
    private boolean softLimitLowEnabled = false;

    @Element(required = false)
    private Length softLimitHigh = new Length(0.0, LengthUnit.Millimeters);

    @Attribute(required = false)
    private boolean softLimitHighEnabled = false;

    @Element(required = false)
    private Length feedratePerSecond = new Length(250, LengthUnit.Millimeters);

    @Element(required = false)
    private Length accelerationPerSecond2 = new Length(500, LengthUnit.Millimeters);

    @Element(required = false)
    private Length jerkPerSecond3 = new Length(2000, LengthUnit.Millimeters);

    /**
     * The resolution of the axis will be used to determined if an axis has moved i.e. whether the sent coordinate 
     * will be different.  
     * @see %.4f format in CommandType.MOVE_TO_COMMAND in GcodeDriver.createDefaults() or Configuration.getLengthDisplayFormat()
     * Comparing coordinates rounded to resolution will also suppress false differences from floating point artifacts 
     * prompted by forward/backward raw <-> transformed calculations. 
     */
    @Element(required = false)
    private double resolution = 0.0001; // 

    public double getResolution() {
        if (resolution <= 0.0) {
            resolution = 0.0001;
        }
        return resolution;
    }

    public void setResolution(double resolution) {
        Object oldValue = this.resolution;
        if (resolution <= 0.0) {
            resolution = 0.0001;
        }
        this.resolution = resolution;
        firePropertyChange("resolution", oldValue, resolution);
    }

    @Override
    protected long getResolutionTicks(double coordinate) {
        return Math.round(coordinate/getResolution());
    }

    public Length getFeedratePerSecond() {
        return feedratePerSecond;
    }

    public void setFeedratePerSecond(Length feedratePerSecond) {
        this.feedratePerSecond = feedratePerSecond;
    }

    public Length getAccelerationPerSecond2() {
        return accelerationPerSecond2;
    }

    public void setAccelerationPerSecond2(Length accelerationPerSecond2) {
        this.accelerationPerSecond2 = accelerationPerSecond2;
    }

    public Length getJerkPerSecond3() {
        return jerkPerSecond3;
    }

    public void setJerkPerSecond3(Length jerkPerSecond3) {
        this.jerkPerSecond3 = jerkPerSecond3;
    }

    public String getPreMoveCommand() {
        return preMoveCommand;
    }

    public void setPreMoveCommand(String preMoveCommand) {
        this.preMoveCommand = preMoveCommand;
    }

    public Length getBacklashOffset() {
        return backlashOffset;
    }

    public void setBacklashOffset(Length backlashOffset) {
        this.backlashOffset = backlashOffset;
    }

    public boolean isLimitRotation() {
        return limitRotation;
    }

    public void setLimitRotation(boolean limitRotation) {
        this.limitRotation = limitRotation;
    }

    public boolean isWrapAroundRotation() {
        return wrapAroundRotation;
    }

    public void setWrapAroundRotation(boolean wrapAroundRotation) {
        this.wrapAroundRotation = wrapAroundRotation;
    }

    public Length getSoftLimitLow() {
        return softLimitLow;
    }

    public void setSoftLimitLow(Length softLimitLow) {
        this.softLimitLow = softLimitLow;
    }

    public boolean isSoftLimitLowEnabled() {
        return softLimitLowEnabled;
    }

    public void setSoftLimitLowEnabled(boolean softLimitLowEnabled) {
        this.softLimitLowEnabled = softLimitLowEnabled;
    }

    public Length getSoftLimitHigh() {
        return softLimitHigh;
    }

    public void setSoftLimitHigh(Length softLimitHigh) {
        this.softLimitHigh = softLimitHigh;
    }

    public boolean isSoftLimitHighEnabled() {
        return softLimitHighEnabled;
    }

    public void setSoftLimitHighEnabled(boolean softLimitHighEnabled) {
        this.softLimitHighEnabled = softLimitHighEnabled;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceControllerAxisConfigurationWizard(this);
    }

    @Override
    public double getMotionLimit(int order) {
        if (order == 1) {
            return getFeedratePerSecond().convertToUnits(AxesLocation.getUnits()).getValue();
        }
        else if (order == 2) {
            return getAccelerationPerSecond2().convertToUnits(AxesLocation.getUnits()).getValue();
        }
        else if (order == 3) {
            return getJerkPerSecond3().convertToUnits(AxesLocation.getUnits()).getValue();
        }
        return 0;
    }
}
