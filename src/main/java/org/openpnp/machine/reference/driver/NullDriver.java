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

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
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
import org.openpnp.model.MappedAxes;
import org.openpnp.model.Motion;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.openpnp.spi.Movable.MoveToOption;
import org.openpnp.spi.base.AbstractControllerAxis;
import org.openpnp.spi.base.AbstractDriver;
import org.openpnp.util.NanosecondTime;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

/**
 * An example of the simplest possible driver that can support multiple heads. This driver maintains
 * a set of coordinates for each Head that it is asked to handle and simply logs all commands sent
 * to it.
 */
public class NullDriver extends AbstractDriver implements ReferenceDriver {


    @Attribute(required = false)
    private double feedRateMmPerMinute = 5000;

    private boolean enabled;

    /**
     * The simulated visual homing offsets are applied to what the simulated down camera sees.
     * Works like Gcode G92. Initialized with the SimulationModeMachine.getHomingError() on homing.
     */
    private AxesLocation homingOffsets = new AxesLocation();

    @Override
    public void home(ReferenceMachine machine, MappedAxes mappedAxes) throws Exception {
        Logger.debug("home()");
        checkEnabled();
        if (machine instanceof SimulationModeMachine) {
            Location homingError = ((SimulationModeMachine) machine).getHomingError(); 
            homingOffsets = mappedAxes.getTypedLocation(homingError);
        }
        else {
            homingOffsets = new AxesLocation();
        }
        mappedAxes.setDriverLocation(new AxesLocation());
    }


    @Override
    public void resetLocation(ReferenceMachine machine, MappedAxes mappedAxes, AxesLocation location)
            throws Exception {
        Logger.debug("resetLocation("+location+")");
        homingOffsets = mappedAxes.getMappedOnlyLocation(location.subtract(mappedAxes.getDriverLocation()).add(homingOffsets));
        mappedAxes.setDriverLocation(location);
    }

    /**
     * Commands the driver to move the given ReferenceHeadMountable to the specified Location at the
     * given speed. Please see the comments for this method in the code for some important
     * considerations when writing your own driver.
     */
    @Override
    public void moveTo(ReferenceHeadMountable hm, MappedAxes mappedAxes, AxesLocation location, double speed, MoveToOption... options)
            throws Exception {
        Logger.debug("moveTo({}, {}, {})", hm, location, speed);
        checkEnabled();
        // Get the location of this driver's axes.
        location = mappedAxes.getMappedOnlyLocation(location);

        // Get the current location of the drivers. 
        AxesLocation hl = mappedAxes.getDriverLocation();

        if (!mappedAxes.locationsMatch(hl, location)) {
            mappedAxes.setDriverLocation(location);
            Logger.debug("Machine new location {}", new MappedAxes(Configuration.get().getMachine()).getDriverLocation());
        }
    }

    @Override
    public void waitForCompletion(ReferenceHeadMountable hm, MappedAxes mappedAxes,
            CompletionType completionType) throws Exception {
        ReferenceMachine machine = (ReferenceMachine) Configuration.get().getMachine();
        while (! machine.getMotionPlanner()
                .getMomentaryMotion(NanosecondTime.getRuntimeSeconds())
                .hasOption(Motion.MotionOption.Stillstand)) {
            Thread.sleep(100);
        }
    }

    @Override
    public void actuate(ReferenceActuator actuator, double value) throws Exception {
        Logger.debug("actuate({}, {})", actuator, value);
        checkEnabled();
        SimulationModeMachine.simulateActuate(actuator, value, feedRateMmPerMinute > 0);
    }

    @Override
    public void actuate(ReferenceActuator actuator, boolean on) throws Exception {
        Logger.debug("actuate({}, {})", actuator, on);
        checkEnabled();
        
        SimulationModeMachine.simulateActuate(actuator, on, feedRateMmPerMinute > 0);
    }
    
    @Override
    public String actuatorRead(ReferenceActuator actuator) throws Exception {
        return Math.random() + "";
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
    public void migrateDriver(ReferenceMachine machine) throws Exception {
        machine.addDriver(this);
        createAxisMappingDefaults(machine);
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
                // Assume 0.5s average acceleration to reach top speed. With jerk control that is 4 x feedrate/s.
                ((ReferenceControllerAxis) axis).setAccelerationPerSecond2(new Length(feedRateMmPerMinute*4/60.0, getUnits()));
                // Assume full time +1/-1 jerk time.
                ((ReferenceControllerAxis) axis).setJerkPerSecond3(new Length(feedRateMmPerMinute*8/60.0, getUnits()));
            }
        }
        ReferenceHead head = (ReferenceHead) machine.getDefaultHead();
        // Use the lower left PCB fiducial as homing fiducial (but not enabling Visual Homing yet).
        head.setHomingFiducialLocation(new Location(LengthUnit.Millimeters, 5.736, 6.112, 0, 0));
    }
}
