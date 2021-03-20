/*
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

package org.openpnp.machine.reference.driver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.List;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.SimulationModeMachine;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Motion;
import org.openpnp.model.Solutions;
import org.openpnp.model.Motion.MoveToCommand;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.openpnp.spi.base.AbstractDriver;
import org.openpnp.util.NanosecondTime;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

/**
 * An example of the simplest possible driver. This driver maintains a set of coordinates for each Axis
 * that it is asked to handle and simply logs all commands sent to it.
 */
public class NullDriver extends AbstractDriver {


    @Attribute(required = false)
    private double feedRateMmPerMinute = 5000;

    private boolean enabled;

    /**
     * The simulated visual homing offsets are applied to what the simulated down camera sees.
     * Works like Gcode G92. Initialized with the SimulationModeMachine.getHomingError() on homing.
     */
    private AxesLocation homingOffsets = new AxesLocation();

    private boolean motionPending;

    @Override
    public void home(ReferenceMachine machine) throws Exception {
        Logger.debug("home()");
        checkEnabled();
        if (machine instanceof SimulationModeMachine) {
            Location homingError = ((SimulationModeMachine) machine).getHomingError();
            homingOffsets = new AxesLocation(machine, this, (axis)
                    -> (axis.getType() == Axis.Type.X ? homingError.getLengthX() :
                        axis.getType() == Axis.Type.Y ? homingError.getLengthY() :
                            null));
        }
        else {
            homingOffsets = new AxesLocation();
        }
        // Store the new homing coordinates on the axes
        AxesLocation homeLocation = new AxesLocation(machine, this, (axis) -> (axis.getHomeCoordinate()));
        homeLocation.setToDriverCoordinates(this);
    }


    @Override
    public void setGlobalOffsets(ReferenceMachine machine, AxesLocation location)
            throws Exception {
        // Take only this driver's axes.
        AxesLocation newDriverLocation = location.drivenBy(this);
        // Take the current driver location of the given axes.
        AxesLocation oldDriverLocation = new AxesLocation(newDriverLocation.getAxes(this),
                (axis) -> (axis.getDriverLengthCoordinate()));
        Logger.debug("setGlobalOffsets("+oldDriverLocation+" -> "+newDriverLocation+")");
        // Calculate the new machine to working coordinate system offset.
        homingOffsets = newDriverLocation.subtract(oldDriverLocation).add(homingOffsets);
        // Store to axes
        newDriverLocation.setToDriverCoordinates(this);
    }

    /**
     * Commands the driver to move the given ReferenceHeadMountable to the specified Location at the
     * given speed. Please see the comments for this method in the code for some important
     * considerations when writing your own driver.
     */
    @Override
    public void moveTo(ReferenceHeadMountable hm, MoveToCommand move)
            throws Exception {
        Logger.debug("moveTo({}, {}, {})", hm, move.getLocation1(), move.getFeedRatePerSecond());
        checkEnabled();
        AxesLocation newDriverLocation = move.getLocation1();
        // Take the current driver location of the given axes.
        AxesLocation oldDriverLocation = new AxesLocation(newDriverLocation.getAxes(this),
                (axis) -> (axis.getDriverLengthCoordinate()));
        if (!oldDriverLocation.matches(newDriverLocation)) {
            // Store to axes
            newDriverLocation.setToDriverCoordinates(this);
            Logger.debug("Machine new location {}", newDriverLocation);
            motionPending = true;
        }
    }

    @Override
    public boolean isMotionPending() {
        return motionPending;
    }

    @Override
    public void waitForCompletion(ReferenceHeadMountable hm, CompletionType completionType) throws Exception {
        ReferenceMachine machine = (ReferenceMachine) Configuration.get().getMachine();
        while (! machine.getMotionPlanner()
                .getMomentaryMotion(NanosecondTime.getRuntimeSeconds())
                .hasOption(Motion.MotionOption.Stillstand)) {
            Thread.sleep(1);
        }
        motionPending = false;
    }

    @Override
    public AxesLocation getReportedLocation(long timeout) throws Exception {
        ReferenceMachine machine = (ReferenceMachine) Configuration.get().getMachine();
        double now = NanosecondTime.getRuntimeSeconds();
        Motion motion = machine.getMotionPlanner()
                .getMomentaryMotion(now);
        return motion.getMomentaryLocation(now - motion.getPlannedTime0());
    }


    @Override
    public void actuate(ReferenceActuator actuator, Object value) throws Exception {
        Logger.debug("actuate({}, {})", actuator, value);
        checkEnabled();
        SimulationModeMachine.simulateActuate(actuator, value, feedRateMmPerMinute > 0);
    }

    @Override
    public Object actuatorRead(ReferenceActuator actuator, Object parameter) throws Exception {
        return Objects.toString(parameter, "") + Math.random();
    }

    @Override
    public void setEnabled(boolean enabled) throws Exception {
        Logger.debug("setEnabled({})", enabled);
        this.enabled = enabled;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return null;
    }
    private void checkEnabled() throws Exception {
        if (!enabled) {
            throw new Exception("Driver is not yet enabled!");
        }
    }

    public double getFeedRateMmPerMinute() {
        return feedRateMmPerMinute;
    }

    public void setFeedRateMmPerMinute(double feedRateMmPerMinute) {
        this.feedRateMmPerMinute = feedRateMmPerMinute;
    }

    @Override
    public Length getFeedRatePerSecond() {
        return new Length(feedRateMmPerMinute/60.0, getUnits());
    }


    @Override
    public void close() throws IOException {

    }

    public AxesLocation getHomingOffsets() {
        return homingOffsets;
    }


    public void setHomingOffsets(AxesLocation homingOffsets) {
        this.homingOffsets = homingOffsets;
    }

    @Override
    public LengthUnit getUnits() {
        return LengthUnit.Millimeters;
    }

    @Deprecated
    @Override
    public void migrateDriver(Machine machine) throws Exception {
        machine.addDriver(this);
        if (machine instanceof ReferenceMachine) {
            createAxisMappingDefaults((ReferenceMachine) machine);
            // Migrate feedrates etc.
            for (Axis axis : machine.getAxes()) {
                if (axis instanceof ReferenceControllerAxis) {
                    double feedRateMmPerMinute = this.feedRateMmPerMinute;
                    if (axis.getType() ==Type.Rotation) {
                        // like in the original NullDriver simulation, rotation is at 10 x speed
                        feedRateMmPerMinute *= 10.0;
                    }
                    // Migrate the feedrate to the axes but change to mm/s.
                    ((ReferenceControllerAxis) axis).setFeedratePerSecond(new Length(feedRateMmPerMinute/60.0, getUnits()));
                    // Assume 0.5s average acceleration to reach top speed. v = a*t => a = v/t
                    ((ReferenceControllerAxis) axis).setAccelerationPerSecond2(new Length(feedRateMmPerMinute/60/0.5, getUnits()));
                    // Switch off jerk by default.
                    ((ReferenceControllerAxis) axis).setJerkPerSecond3(new Length(0, getUnits()));
                }
            }
            // Switch the driver limit off, only the axes' limits remains.
            this.feedRateMmPerMinute = 0;
            ReferenceHead head = (ReferenceHead) machine.getDefaultHead();
            // Use the lower left PCB fiducial as homing fiducial (but not enabling Visual Homing yet).
            head.setHomingFiducialLocation(new Location(LengthUnit.Millimeters, 5.736, 6.112, 0, 0));
        }
    }


    @Override
    public boolean isUsingLetterVariables() {
        return false;
    }

    @Override
    public MotionControlType getMotionControlType() {
        return MotionControlType.Full3rdOrderControl;
    }

    @Override
    public void findIssues(List<Solutions.Issue> issues) {
        super.findIssues(issues);
        issues.add(new Solutions.Issue(
                this,
                "The simulation NullDriver can replaced with a GcodeAsyncDriver to drive a real controller.",
                "Replace with GcodeAsyncDriver.",
                Severity.Fundamental,
                "https://github.com/openpnp/openpnp/wiki/GcodeAsyncDriver") {

            @Override
            public void setState(Solutions.State state) throws Exception {
                if (confirmStateChange(state)) {
                    if (state == Solutions.State.Solved) {
                        GcodeDriverSolutions.convertToAsync(NullDriver.this);
                    }
                    else if (getState() == Solutions.State.Solved) {
                        // Place the old one back (from the captured NullDriver.this).
                        GcodeDriverSolutions.replaceDriver(NullDriver.this);
                    }
                    super.setState(state);
                }
            }
        });
    }
}
