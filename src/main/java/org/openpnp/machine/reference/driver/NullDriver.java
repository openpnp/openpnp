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
    private long vibrationTime;

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

        // Get the current location of the Head that we'll move
        AxesLocation hl = mappedAxes.getLocation();

        if (feedRateMmPerMinute > 0) {
            simulateMovement(hm, mappedAxes, location, hl, speed);
        }

        // Now that movement is complete, update the stored Location to the new
        // Location.
        mappedAxes.setLocation(location);
        Logger.debug("Machine new location {}", new MappedAxes(Configuration.get().getMachine()).getLocation());
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
        double time = Math.max(timeLinear, timeRotational) + 0.001;
        
        long t0 = System.currentTimeMillis();
        double dt;
        do {
            double t = (System.currentTimeMillis() - t0)*0.001;
            dt = Math.min(1.0, t/time);
            AxesLocation l = hl.add(delta.multiply(dt));
            mappedAxes.setLocation(l);

            // Provide live updates to the Machine as the move progresses.
            ((ReferenceMachine) Configuration.get().getMachine())
                    .fireMachineHeadActivity(hm.getHead());
            try {
                Thread.sleep(10);
            }
            catch (Exception e) {

            }
        }
        while(dt < 1.0);
        if (distanceLinear > 0.01) {
            vibrationVector = delta.multiply(1.0/Math.max(0.1, distanceLinear));
            vibrationTime = System.currentTimeMillis();
        }
        else if (distanceRotational > 0.1) {
            vibrationVector = new AxesLocation(mappedAxes.getAxis(Axis.Type.X), 1.0);
            vibrationTime = System.currentTimeMillis();
        }
    }

    @Override
    public void actuate(ReferenceActuator actuator, double value) throws Exception {
        Logger.debug("actuate({}, {})", actuator, value);
        checkEnabled();
        if (feedRateMmPerMinute > 0) {
            Thread.sleep(500);
        }
    }

    @Override
    public void actuate(ReferenceActuator actuator, boolean on) throws Exception {
        Logger.debug("actuate({}, {})", actuator, on);
        checkEnabled();
        if (feedRateMmPerMinute > 0) {
            Thread.sleep(500);
        }
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


    public long getVibrationTime() {
        return vibrationTime;
    }


    public void setVibrationTime(long vibrationTime) {
        this.vibrationTime = vibrationTime;
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
    }
}
