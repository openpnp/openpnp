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

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.IOException;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.SimulationModeMachine;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.MappedAxes;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Camera.Looking;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Movable.MoveToOption;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.base.AbstractDriver;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

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
    private AxesLocation vibrationVector;
    private long vibrationStartTime;

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
        mappedAxes.setLocation(new AxesLocation());
    }


    @Override
    public void resetLocation(ReferenceMachine machine, MappedAxes mappedAxes, AxesLocation location)
            throws Exception {
        Logger.debug("resetLocation("+location+")");
        homingOffsets = mappedAxes.getMappedOnlyLocation(location.subtract(mappedAxes.getLocation()).add(homingOffsets));
        mappedAxes.setLocation(location);
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

        // Get the current location of the Head that we'll move
        AxesLocation hl = mappedAxes.getLocation();

        if (feedRateMmPerMinute > 0) {
            simulateMovement(hm, mappedAxes, location, hl, speed);
        }
        if (!mappedAxes.locationsMatch(hl, location)) {
            mappedAxes.setLocation(location);
            Logger.debug("Machine new location {}", new MappedAxes(Configuration.get().getMachine()).getLocation());
        }
    }

    /**
     * Simulates true machine movement, which takes time, by tracing the required movement lines
     * over a period of time based on the input speed.
     * 
     * @param hm
     * @param location
     * @param hl
     * @param speed
     * @throws Exception
     */
    protected void simulateMovement(ReferenceHeadMountable hm, MappedAxes mappedAxes, AxesLocation location, AxesLocation hl,
            double speed) throws Exception {
        // Roughly NIST RS274NGC Interpreter - Version 3, Section 2.1.2.5 rule A
        AxesLocation delta = location.subtract(hl);
        double distanceLinear = delta.distanceLinear();
        double distanceRotational = delta.distanceRotational();
        double timeLinear = distanceLinear / (feedRateMmPerMinute/60.0 * speed);
        double timeRotational = distanceRotational / (36.0*feedRateMmPerMinute/60.0 * speed);
        double time = Math.max(timeLinear, timeRotational);
        ReferenceMachine machine = ((ReferenceMachine) Configuration.get().getMachine());

        long t0 = System.currentTimeMillis();
        double dt;
        while (true) {
            double t = (System.currentTimeMillis() - t0)*0.001;
            dt = Math.min(1.0, t/time);
            AxesLocation l = hl.add(delta.multiply(dt));
            mappedAxes.setLocation(l);

            // Provide live updates to the Machine as the move progresses.
            machine.fireMachineHeadActivity(hm.getHead());
            if (dt >= 1.0) {
                break;
            }
            
            try {
                Thread.sleep(10);
            }
            catch (Exception e) {

            }
        }
        if (distanceLinear > 0.001) {
            vibrationVector = delta.multiply(1.0/distanceLinear)
                    .subtract((vibrationVector != null) ? vibrationVector.multiply(0.5) // shake it up, if not yet done 
                            : new AxesLocation()); 
            vibrationStartTime = System.currentTimeMillis();
        }
        else if (distanceRotational > 0.1) {
            vibrationVector = new AxesLocation(mappedAxes.getAxis(Axis.Type.X), 1.0);
            vibrationStartTime = System.currentTimeMillis();
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


    public AxesLocation getVibrationVector() {
        return vibrationVector;
    }


    public void setVibrationVector(AxesLocation vibrationVector) {
        this.vibrationVector = vibrationVector;
    }


    public long getVibrationStartTime() {
        return vibrationStartTime;
    }


    public void setVibrationStartTime(long vibrationStartTime) {
        this.vibrationStartTime = vibrationStartTime;
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
        ReferenceHead head = (ReferenceHead) machine.getDefaultHead();
        // Use the lower left PCB fiducial as homing fiducial (but not enabling Visual Homing yet).
        head.setHomingFiducialLocation(new Location(LengthUnit.Millimeters, 5.736, 6.112, 0, 0));
    }
}
