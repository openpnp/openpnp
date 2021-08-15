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

package org.openpnp.machine.reference;
import org.I18n.I18n;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ActuatorInterlockMonitorConfigurationWizard;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.base.AbstractActuator;
import org.openpnp.spi.base.AbstractMachine;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public class ActuatorInterlockMonitor extends AbstractModelObject implements Actuator.InterlockMonitor {
    @Attribute(required = false)
    InterlockType interlockType;

    // The interlock axes of the interlock actuator. Any of these can be null. 
    private CoordinateAxis interlockAxis1;
    private CoordinateAxis interlockAxis2;
    private CoordinateAxis interlockAxis3;
    private CoordinateAxis interlockAxis4;

    // Another Actuator that this one interlocks with.   
    private Actuator conditionalActuator;

    @Attribute(required = false)
    private String interlockAxis1Id;
    @Attribute(required = false)
    private String interlockAxis2Id;
    @Attribute(required = false)
    private String interlockAxis3Id;
    @Attribute(required = false)
    private String interlockAxis4Id;
    @Attribute(required = false)
    private double confirmationGoodMin;
    @Attribute(required = false)
    private double confirmationGoodMax;
    @Element(required = false, data=true)
    private String confirmationPattern;
    @Attribute(required = false)
    private boolean confirmationByRegex;  
    @Attribute(required = false)
    private String conditionalActuatorId;
    @Attribute(required = false)
    private ActuatorState conditionalActuatorState = ActuatorState.SwitchedOn;
    @Attribute(required = false)
    private double conditionalSpeedMin = 0.0;
    @Attribute(required = false)
    private double conditionalSpeedMax = 1.0;

    public enum InterlockType {
        None,
        SignalAxesMoving,
        SignalAxesStandingStill,
        SignalAxesInsideSafeZone,
        SignalAxesOutsideSafeZone,
        SignalAxesParked,
        SignalAxesUnparked,
        ConfirmInRangeBeforeAxesMove,
        ConfirmInRangeAfterAxesMove,
        ConfirmMatchBeforeAxesMove,
        ConfirmMatchAfterAxesMove;

        public boolean isReadingDouble() {
            return this == ConfirmInRangeBeforeAxesMove || this == ConfirmInRangeAfterAxesMove;
        }
        public boolean isReadingString() {
            return this == ConfirmMatchBeforeAxesMove || this == ConfirmMatchAfterAxesMove;
        }
    }

    public enum ActuatorState {
        SwitchedOff,
        SwitchedOn,
        SwitchedOffOrUnknown,
        SwitchedOnOrUnknown;

        public boolean mayBeOn() {
            return this == SwitchedOn || this == SwitchedOnOrUnknown;
        }
        public boolean mustBeKnown() {
            return this == SwitchedOn || this == SwitchedOff;
        }
    }

    public ActuatorInterlockMonitor() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {

            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                interlockAxis1 = (CoordinateAxis) configuration.getMachine().getAxis(interlockAxis1Id);
                interlockAxis2 = (CoordinateAxis) configuration.getMachine().getAxis(interlockAxis2Id);
                interlockAxis3 = (CoordinateAxis) configuration.getMachine().getAxis(interlockAxis3Id);
                interlockAxis4 = (CoordinateAxis) configuration.getMachine().getAxis(interlockAxis4Id);
                // We don't have access to the head here. So we need to scan them all. 
                // I'm sure there is a better solution.
                for (Head head : Configuration.get().getMachine().getHeads()) {
                    conditionalActuator = head.getActuator(conditionalActuatorId);
                    if (conditionalActuator != null) {
                        break;
                    }
                }
            }
        });
    }

    public InterlockType getInterlockType() {
        return interlockType;
    }

    public void setInterlockType(InterlockType interlockType) {
        Object oldValue = this.interlockType;
        this.interlockType = interlockType;
        firePropertyChange("interlockType", oldValue, interlockType);
    }

    public CoordinateAxis getInterlockAxis1() {
        return interlockAxis1;
    }
    public void setInterlockAxis1(CoordinateAxis interlockAxis1) {
        Object oldValue = this.interlockAxis1;
        this.interlockAxis1 = interlockAxis1;
        this.interlockAxis1Id = (interlockAxis1 == null) ? null : interlockAxis1.getId();
        firePropertyChange("interlockAxis1", oldValue, interlockAxis1);
    }
    public CoordinateAxis getInterlockAxis2() {
        return interlockAxis2;
    }
    public void setInterlockAxis2(CoordinateAxis interlockAxis2) {
        Object oldValue = this.interlockAxis2;
        this.interlockAxis2 = interlockAxis2;
        this.interlockAxis2Id = (interlockAxis2 == null) ? null : interlockAxis2.getId();
        firePropertyChange("interlockAxis2", oldValue, interlockAxis2);
    }
    public CoordinateAxis getInterlockAxis3() {
        return interlockAxis3;
    }
    public void setInterlockAxis3(CoordinateAxis interlockAxis3) {
        Object oldValue = this.interlockAxis3;
        this.interlockAxis3 = interlockAxis3;
        this.interlockAxis3Id = (interlockAxis3 == null) ? null : interlockAxis3.getId();
        firePropertyChange("interlockAxis3", oldValue, interlockAxis3);
    }
    public CoordinateAxis getInterlockAxis4() {
        return interlockAxis4;
    }
    public void setInterlockAxis4(CoordinateAxis interlockAxis4) {
        Object oldValue = this.interlockAxis4;
        this.interlockAxis4 = interlockAxis4;
        this.interlockAxis4Id = (interlockAxis4 == null) ? null : interlockAxis4.getId();
        firePropertyChange("interlockAxis4", oldValue, interlockAxis4);
    }

    public Actuator getConditionalActuator() {
        return conditionalActuator;
    }

    public void setConditionalActuator(Actuator conditionalActuator) {
        Object oldValue = this.conditionalActuator;
        this.conditionalActuator = conditionalActuator;
        this.conditionalActuatorId = (conditionalActuator == null) ? null : conditionalActuator.getId();
        firePropertyChange("conditionalActuator", oldValue, conditionalActuator);
    }

    public double getConfirmationGoodMin() {
        return confirmationGoodMin;
    }
    public void setConfirmationGoodMin(double confirmationGoodMin) {
        this.confirmationGoodMin = confirmationGoodMin;
    }
    public double getConfirmationGoodMax() {
        return confirmationGoodMax;
    }
    public void setConfirmationGoodMax(double confirmationGoodMax) {
        this.confirmationGoodMax = confirmationGoodMax;
    }

    public String getConfirmationPattern() {
        return confirmationPattern;
    }

    public void setConfirmationPattern(String confirmationPattern) {
        this.confirmationPattern = confirmationPattern;
    }

    public boolean isConfirmationByRegex() {
        return confirmationByRegex;
    }

    public void setConfirmationByRegex(boolean confirmationByRegex) {
        this.confirmationByRegex = confirmationByRegex;
    }

    public ActuatorState getConditionalActuatorState() {
        return conditionalActuatorState;
    }

    public void setConditionalActuatorState(ActuatorState conditionalActuatorState) {
        this.conditionalActuatorState = conditionalActuatorState;
    }

    public double getConditionalSpeedMin() {
        return conditionalSpeedMin;
    }

    public void setConditionalSpeedMin(double conditionalSpeedMin) {
        this.conditionalSpeedMin = conditionalSpeedMin;
    }

    public double getConditionalSpeedMax() {
        return conditionalSpeedMax;
    }

    public void setConditionalSpeedMax(double conditionalSpeedMax) {
        this.conditionalSpeedMax = conditionalSpeedMax;
    }

    public void actuate(Actuator actuator, boolean on) throws Exception {
        // Only actuate, if the value is unknown or it has changed.
        if (!(actuator.getLastActuationValue() instanceof Boolean 
                && on == (boolean)actuator.getLastActuationValue())) {
            Logger.trace(actuator.getName()+" interlock actuation changes to "+on);
            actuator.actuate(on);
        }
    }

    @Override
    public boolean interlockActuation(Actuator actuator, AxesLocation location0, AxesLocation location1, boolean beforeMove, double speed) throws Exception {
        // Filter the locations to our interlocking axes. 
        AxesLocation interlockingLocation0 = new AxesLocation(location0.getAxes(CoordinateAxis.class),
                (axis) -> (
                        (axis == interlockAxis1 || axis == interlockAxis2 || axis == interlockAxis3 || axis == interlockAxis4) 
                        ? location0.getLengthCoordinate(axis) : null));
        AxesLocation interlockingLocation1 = new AxesLocation(location1.getAxes(CoordinateAxis.class),
                (axis) -> (
                        (axis == interlockAxis1 || axis == interlockAxis2 || axis == interlockAxis3 || axis == interlockAxis4) 
                        ? location1.getLengthCoordinate(axis) : null));
        if (!interlockingLocation0.matches(interlockingLocation1)) {
            // Some of our interlock axes are on the move.
            // Check the Interlock conditions.
            if (speed < conditionalSpeedMin || speed > conditionalSpeedMax) {
                // We're outside the speed range, don't apply the interlock.
                Logger.trace(actuator.getName()+" interlock masked by speed "+speed);
                return false;
            }
            if (conditionalActuator != null) {
                if (conditionalActuator.getLastActuationValue() instanceof Boolean) { 
                    // The actuator has a known boolean state.
                    if (conditionalActuatorState.mayBeOn() != (boolean)conditionalActuator.getLastActuationValue()) {
                        // The conditional Actuator has a different state, interlock does not apply. 
                        Logger.trace(actuator.getName()+" interlock masked by conditionalActuator "+conditionalActuator.getName()
                        +" being "+conditionalActuator.getLastActuationValue());
                        return false;
                    }
                }
                else {
                    // Unknown state.
                    if (conditionalActuatorState.mustBeKnown()) {
                        // The conditional Actuator has a different value, interlock does not apply. 
                        Logger.trace(actuator.getName()+" interlock masked by conditionalActuator "+conditionalActuator.getName()
                        +" being unknown");
                        return false;
                    }
                }
            }

            switch (interlockType) {
                case SignalAxesMoving:
                case SignalAxesStandingStill:
                    // TODO: this should probably only signal, if the next move does not contain any of 
                    // interlocking axes. For the moment, it will glitch between these moves.
                    actuate(actuator, beforeMove 
                            ^ (interlockType == InterlockType.SignalAxesStandingStill)); 
                    break;
                case SignalAxesInsideSafeZone:
                case SignalAxesOutsideSafeZone:
                    // Note: signal changes before the move when moving into the safe zone and after the move if moving out.
                    boolean willBeInSafeZone = interlockingLocation1.isInSafeZone();
                    if (beforeMove ^ !willBeInSafeZone) {
                        actuate(actuator, willBeInSafeZone 
                                ^ (interlockType == InterlockType.SignalAxesOutsideSafeZone));
                    }
                    break;
                case SignalAxesParked:
                case SignalAxesUnparked:
                    // Convert the park location to axes raw coordinates.
                    Location parkLocation = actuator.getHead().getParkLocation();
                    // Only take X, Y.
                    parkLocation = parkLocation.derive(null, null, 
                            Double.NaN, 
                            Double.NaN); 
                    HeadMountable hm = actuator.getHead().getDefaultHeadMountable();
                    AxesLocation axesParkLocation = hm.toRaw(hm.toHeadLocation(parkLocation));
                    // Filter to only the X, Y interlock axes.
                    AxesLocation interlockParkLocation = new AxesLocation(axesParkLocation.getAxes(CoordinateAxis.class),
                            (axis) -> (
                                    (axis == interlockAxis1 || axis == interlockAxis2 || axis == interlockAxis3 || axis == interlockAxis4)
                                    && (axis.getType() == Type.X || axis.getType() == Type.Y)
                                    ? axesParkLocation.getLengthCoordinate(axis) : null));
                    boolean willBeParked = interlockParkLocation.matches(interlockingLocation1);
                    // Check Z and C axes: Must be Z in Safe Z Zone and C = 0 as in the Machine Controls.
                    for (CoordinateAxis axis : new CoordinateAxis[] {
                            interlockAxis1, interlockAxis2, interlockAxis3, interlockAxis4}) {
                        if (axis != null) {
                            if (axis.getType() == Type.Z) {
                                willBeParked = willBeParked 
                                        && axis.isInSafeZone(interlockingLocation1.getLengthCoordinate(axis));
                            }
                            else if (axis.getType() == Type.Rotation) {
                                willBeParked = willBeParked 
                                        && axis.coordinatesMatch(interlockingLocation1.getCoordinate(axis), 0);
                            }
                        }
                    }
                    // Note: signal changes after move when moving to the park location and before the move if moving away.
                    if (beforeMove ^ willBeParked) {
                        // Actuate if a match.
                        actuate(actuator, willBeParked ^ (interlockType == InterlockType.SignalAxesUnparked));
                    }
                    break;
                case ConfirmInRangeBeforeAxesMove:
                case ConfirmInRangeAfterAxesMove:
                    if (beforeMove 
                            ^ (interlockType == InterlockType.ConfirmInRangeAfterAxesMove)) {
                        // Read the confirmation sensor.
                        Double confirmation = Double.parseDouble(actuator.read());
                        // Compare against the good range.
                        if (confirmation < confirmationGoodMin) {
                            throw new Exception(actuator.getName()+" interlock confirmation below good range: "+confirmation+" < "+confirmationGoodMin); 
                        }
                        if (confirmation > confirmationGoodMax) {
                            throw new Exception(actuator.getName()+" interlock confirmation above good range: "+confirmation+" > "+confirmationGoodMax); 
                        }
                    }
                    break;
                case ConfirmMatchBeforeAxesMove:
                case ConfirmMatchAfterAxesMove:
                    if (beforeMove 
                            ^ (interlockType == InterlockType.ConfirmMatchAfterAxesMove)) {
                        // Read the confirmation sensor.
                        String confirmation = actuator.read();
                        // Compare against the pattern.
                        if (!(confirmationByRegex ? 
                                confirmation.matches(confirmationPattern) 
                                : confirmation.trim().equals(confirmationPattern.trim()))) {
                            throw new Exception(actuator.getName()+" interlock confirmation does not match: "+confirmation+" vs. "+confirmationPattern); 
                        }
                    }
                    break;
                default:
            }
        }
        return true;
    }

    @Override
    public Wizard getConfigurationWizard(AbstractActuator actuator) {
        return new ActuatorInterlockMonitorConfigurationWizard((AbstractMachine) Configuration.get().getMachine(), actuator, this);
    }
}
