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

import java.util.List;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.axis.wizards.ReferenceControllerAxisConfigurationWizard;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Issue;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.spi.Axis;
import org.openpnp.spi.base.AbstractControllerAxis;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class ReferenceControllerAxis extends AbstractControllerAxis {
    // The more implementation specific properties are in the ReferenceControllerAxis

    /**
     * Backlash compensation is used to counter any looseness or play in the mechanical linkages of the given axis.  
     * When the actuator reverses the direction of travel, there is often a moment where nothing happens, because the
     * slack from the belt or lash/play from the lead/ball screw needs to be taken up, before mechanical force can again 
     * be transmitted. Note that backlash is often unavoidable, otherwise friction will be too high or lubrication 
     * impossible.   
     *
     */
    public enum BacklashCompensationMethod {
        /**
         * No backlash compensation is performed. The offset is ignored.
         */
        None,
        /**
         * Backlash compensation is applied by always moving to the end position from one side. 
         * The backlash offset does not need to be very precise, i.e. it can be larger than the actual backlash and 
         * the machine will still end up in the correct precise position.  
         * The machine always needs to perform an extra move.
         * 
         */
        OneSidedPositioning,
        /**
         * Works like OneSidedPositioning except it will only perform an extra move when moving from the wrong side.
         * 
         */
        OneSidedOptimizedPositioning,
        /**
         * Backlash compensation is applied in the direction of travel. 
         * Half of the offset is added to the actual target location.    
         */
        DirectionalCompensation;

        public boolean isOneSidedPositioningMethod() {
            return this == OneSidedPositioning || this == OneSidedOptimizedPositioning;
        }
    }

    @Attribute(required = false)
    private BacklashCompensationMethod backlashCompensationMethod = BacklashCompensationMethod.None;

    @Element(required = false)
    private Length backlashOffset = new Length(0.0, LengthUnit.Millimeters);

    @Attribute(required = false) 
    private double backlashSpeedFactor = 0.1; 

    /**
     * If limitRotation is enabled the nozzle will reverse directions when commanded to rotate past
     * 180 degrees. So, 190 degrees becomes -170 and -190 becomes 170.
     */
    @Attribute(required = false)
    private boolean limitRotation = false;

    /**
     * If wrapAroundRotation is enabled the nozzle rotation will always wrap-around the shorter way. If combined 
     * with limitRotation the rotation angle will be reset on the controller to stay within +/-180° (if supported 
     * by the driver setup i.e. needs special G-Code).
     */
    @Attribute(required = false)
    private boolean wrapAroundRotation = false;

    @Attribute(required = false)
    private boolean invertLinearRotational;

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
    private Length safeZoneLow = new Length(0.0, LengthUnit.Millimeters);

    @Attribute(required = false)
    private boolean safeZoneLowEnabled = false;

    @Element(required = false)
    private Length safeZoneHigh = new Length(0.0, LengthUnit.Millimeters);

    @Attribute(required = false)
    private boolean safeZoneHighEnabled = false;

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
        return convertToSytem(feedratePerSecond);
    }

    public void setFeedratePerSecond(Length feedratePerSecond) {
        this.feedratePerSecond = convertFromSytem(feedratePerSecond);
    }

    public Length getAccelerationPerSecond2() {
        return convertToSytem(accelerationPerSecond2);
    }

    public void setAccelerationPerSecond2(Length accelerationPerSecond2) {
        this.accelerationPerSecond2 = convertFromSytem(accelerationPerSecond2);
    }

    public Length getJerkPerSecond3() {
        return convertToSytem(jerkPerSecond3);
    }

    public void setJerkPerSecond3(Length jerkPerSecond3) {
        this.jerkPerSecond3 = convertFromSytem(jerkPerSecond3);
    }

    public String getPreMoveCommand() {
        return preMoveCommand;
    }

    public void setPreMoveCommand(String preMoveCommand) {
        this.preMoveCommand = preMoveCommand;
    }

    public BacklashCompensationMethod getBacklashCompensationMethod() {
        return backlashCompensationMethod;
    }

    public void setBacklashCompensationMethod(BacklashCompensationMethod backlashCompensationMethod) {
        this.backlashCompensationMethod = backlashCompensationMethod;
    }

    public Length getBacklashOffset() {
        return convertToSytem(backlashOffset);
    }

    public void setBacklashOffset(Length backlashOffset) {
        this.backlashOffset = convertFromSytem(backlashOffset);
    }

    public double getBacklashSpeedFactor() {
        return backlashSpeedFactor;
    }

    public void setBacklashSpeedFactor(double backlashSpeedFactor) {
        this.backlashSpeedFactor = backlashSpeedFactor;
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

    public Length getSafeZoneLow() {
        return safeZoneLow;
    }

    public void setSafeZoneLow(Length safeZoneLow) {
        this.safeZoneLow = safeZoneLow;
    }

    public boolean isSafeZoneLowEnabled() {
        return safeZoneLowEnabled;
    }

    public void setSafeZoneLowEnabled(boolean safeZoneLowEnabled) {
        this.safeZoneLowEnabled = safeZoneLowEnabled;
    }

    public Length getSafeZoneHigh() {
        return safeZoneHigh;
    }

    public void setSafeZoneHigh(Length safeZoneHigh) {
        this.safeZoneHigh = safeZoneHigh;
    }

    public boolean isSafeZoneHighEnabled() {
        return safeZoneHighEnabled;
    }

    public void setSafeZoneHighEnabled(boolean safeZoneHighEnabled) {
        this.safeZoneHighEnabled = safeZoneHighEnabled;
    }

    public boolean isInvertLinearRotational() {
        return invertLinearRotational;
    }

    public void setInvertLinearRotational(boolean invertLinearRotational) {
        this.invertLinearRotational = invertLinearRotational;
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
            if (getDriver() != null 
                    && getDriver().getMotionControlType().isConstantAcceleration()) {
                // Suppress any jerk setting in constant acceleration motion control.
                return 0;
            }
            return getJerkPerSecond3().convertToUnits(AxesLocation.getUnits()).getValue();
        }
        return 0;
    }

    @Override
    public boolean isRotationalOnController() {
        return getType() == Axis.Type.Rotation ^ invertLinearRotational;
    }

    @Override
    public boolean isInSafeZone(Length coordinate) {
        if (isSafeZoneLowEnabled()) {
            // We have a lower Safe Z Zone limit.
            Length limit = getSafeZoneLow().convertToUnits(coordinate.getUnits());
            if (coordinate.getValue() < limit.getValue()
                    && !coordinatesMatch(coordinate, limit)) {
                // Definitely below the Safe Zone.
                return false;
            }
        }
        if (isSafeZoneHighEnabled()) {
            // We have a upper Safe Z Zone limit.
            Length limit = getSafeZoneHigh().convertToUnits(coordinate.getUnits());
            if (coordinate.getValue() > limit.getValue()
                    && !coordinatesMatch(coordinate, limit)) {
                // Definitely above the Safe Zone.
                return false;
            }
        }
        // We're either inside the limits, or the Safe Zone is not enabled.
        return true;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceControllerAxisConfigurationWizard(this);
    }

    @Override
    public void findIssues(List<Issue> issues) {
        super.findIssues(issues);

        if (getLetter().isEmpty()) {
            issues.add(new Solutions.PlainIssue(
                    this, 
                    "Axis letter is missing. Assign the letter to continue.", 
                    "Please assign the correct controller axis letter.", 
                    Severity.Fundamental,
                    "https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings"));
        }
        else if (getLetter().equals("E")) {
            if (!getDriver().isSupportingPreMove()) {
                issues.add(new Solutions.PlainIssue(
                        this, 
                        "Avoid axis letter E, if possible. Use proper rotation axes instead.", 
                        "Check if your controller supports proper axes A B C instead of E.", 
                        Severity.Warning,
                        "https://github.com/openpnp/openpnp/wiki/Advanced-Motion-Control#migration-from-a-previous-version"));
            }
        }
        final BacklashCompensationMethod oldBacklashCompensationMethod = 
                getBacklashCompensationMethod();
        if (oldBacklashCompensationMethod != BacklashCompensationMethod.None
                && oldBacklashCompensationMethod != BacklashCompensationMethod.DirectionalCompensation) {
            issues.add(new Solutions.Issue(
                    this, 
                    "New directonal backlash compensation method improves performance and allows fluid motion.", 
                    "Set axis to DirectionalCompensation.", 
                    Severity.Suggestion,
                    "https://github.com/openpnp/openpnp/wiki/Backlash-Compensation") {

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (confirmStateChange(state)) {
                        setBacklashCompensationMethod(
                                (state == Solutions.State.Solved) ?  
                                        BacklashCompensationMethod.DirectionalCompensation 
                                        : oldBacklashCompensationMethod);
                        super.setState(state);
                    }
                }
            });
        }
        if (Math.abs(getMotionLimit(1)*2 - getMotionLimit(2)) < 0.1) {
            // HACK: migration sets the acceleration to twice the feed-rate, that's our "signal" that the user has not yet
            // tuned them.
            issues.add(new Solutions.PlainIssue(
                    this, 
                    "Feed-rate, acceleration, jerk etc. can now be set individually per axis.", 
                    "Tune your machine axes for best speed and acceleration.", 
                    Severity.Suggestion,
                    "https://github.com/openpnp/openpnp/wiki/Machine-Axes#kinematic-settings--rate-limits"));
        }
        if (getType() == Type.Rotation) {
            if (!isWrapAroundRotation()) {
                issues.add(new Solutions.Issue(
                        this, 
                        "Rotation can be optimized by wrapping-around the shorter way. Best combined with Limit ±180°.", 
                        "Enable Wrap Around.", 
                        Severity.Suggestion,
                        "https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings-rotational-axis") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        if (confirmStateChange(state)) {
                            setWrapAroundRotation((state == Solutions.State.Solved));
                            super.setState(state);
                        }
                    }
                });
            }
            if (!isLimitRotation()) {
                issues.add(new Solutions.Issue(
                        this, 
                        "Rotation can be optimized by limiting angles to ±180°. Best combined with Wrap Around.", 
                        "Enable Limit ±180°.", 
                        Severity.Suggestion,
                        "https://github.com/openpnp/openpnp/wiki/Machine-Axes#controller-settings-rotational-axis") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        if (confirmStateChange(state)) {
                            setLimitRotation((state == Solutions.State.Solved));
                            super.setState(state);
                        }
                    }
                });
            }
        }
    }
}
