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

    /**
     * The simulated non-squareness is applied to what the simulated cameras see.
     * Works on ImageCamera and SimulatedUpCamera.
     */
    @Attribute(required = false)
    private double simulatedNonSquarenessFactor = 0.0;

    /**
     * Simulated runout on the nozzle tips.
     * Works on SimulatedUpCamera;
     */
    @Element(required = false)
    private Location simulatedRunout = new Location(LengthUnit.Millimeters);

    /**
     * Simulated camera noise (number of sparks).
     */
    @Attribute(required = false)
    private int simulatedNoise = 0;
    
    /**
     * Simulated vibration to test camera settle. Initial amplitude in mm.
     */
    @Attribute(required = false)
    private double simulatedVibrationAmplitude = 0.0;

    /**
     * Simulated homing error.
     * Works on ImageCamera;
     */
    @Element(required = false)
    private Location homingError = new Location(LengthUnit.Millimeters);

    /**
     * The simulated visual homing offsets are applied to what the simulated down camera sees.
     * This works like G92. It is initialized with the homingError.
     * Works on ImageCamera.
     */
    private Location homingOffsets = new Location(LengthUnit.Millimeters);

    private boolean enabled;

    private AxesLocation vibrationVector;
    private long vibrationTime;

    @Override
    public void home(ReferenceMachine machine, MappedAxes mappedAxes) throws Exception {
        Logger.debug("home()");
        checkEnabled();
        homingOffsets = homingError;
        mappedAxes.setLocation(new AxesLocation());
    }


    @Override
    public void resetLocation(ReferenceMachine machine, MappedAxes mappedAxes, AxesLocation location)
            throws Exception {
        Logger.debug("resetLocation("+location+")");
        AxesLocation offsets = location.subtract(mappedAxes.getLocation());
        double x = 0, y = 0;
        for (ControllerAxis axis : mappedAxes.getAxes()) {
            if (axis.getType() == Axis.Type.X) {
                x = offsets.getCoordinate(axis) + homingOffsets.getX();
            }
            else if (axis.getType() == Axis.Type.Y) {
                y = offsets.getCoordinate(axis) + homingOffsets.getY();
            }
        }
        homingOffsets = new Location(AxesLocation.getUnits(), x, y, 0, 0);
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
        if (distanceLinear > 0.01 && simulatedVibrationAmplitude != 0.0) {
            vibrationVector = delta.multiply(simulatedVibrationAmplitude/Math.max(0.1, distanceLinear));
            vibrationTime = System.currentTimeMillis();
        }
        else if (distanceRotational > 0.1) {
            vibrationVector = new AxesLocation(mappedAxes.getAxis(Axis.Type.X), simulatedVibrationAmplitude);
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


    public double getSimulatedNonSquarenessFactor() {
        return simulatedNonSquarenessFactor;
    }


    public void setSimulatedNonSquarenessFactor(double simulatedNonSquarenessFactor) {
        this.simulatedNonSquarenessFactor = simulatedNonSquarenessFactor;
    }

    public Location getHomingOffsets() {
        return homingOffsets;
    }

    public void setHomingOffsets(Location homingOffsets) {
        this.homingOffsets = homingOffsets;
    }

    public Location getSimulatedRunout() {
        return simulatedRunout;
    }


    public void setSimulatedRunout(Location simulatedRunout) {
        this.simulatedRunout = simulatedRunout;
    }

    /**
     * Simulates imperfections in the physical location of a HeadMountable in the NullDriver. If any other driver is 
     * configured this remains ineffective. For use in simulated Cameras.  
     *  
     * @param hm
     * @param looking
     * @return
     */
    public static Location getSimulatedPhysicalLocation(HeadMountable hm, Looking looking) {
        ReferenceMachine machine = (ReferenceMachine) Configuration.get()
                .getMachine();
        MappedAxes mappedAxes = hm.getMappedAxes(machine);
        Location location = hm.getLocation(); 
        try {
            AxesLocation l = mappedAxes.getLocation();
            if (machine.getDefaultDriver() instanceof NullDriver) {
                NullDriver driver =  (NullDriver) machine.getDefaultDriver();
                // Add vibrations
                if (driver.vibrationVector != null) {
                    double t = (System.currentTimeMillis()-driver.vibrationTime)*0.001;
                    double amplitude = Math.exp(-t/0.07);
                    if (amplitude < 0.02) {
                        driver.vibrationVector = null;
                    }
                    else {
                        double frequency = 9.3754;
                        double shape = Math.sin(t*frequency*2.0*Math.PI)*amplitude;
                        l = l.add(driver.vibrationVector.multiply(shape));
                    }
                }
            }
            ControllerAxis axisX = mappedAxes.getAxis(Axis.Type.X);
            ControllerAxis axisY = mappedAxes.getAxis(Axis.Type.Y);
            ControllerAxis axisZ = mappedAxes.getAxis(Axis.Type.Z);
            ControllerAxis axisRotation = mappedAxes.getAxis(Axis.Type.Rotation);
            location = new Location(AxesLocation.getUnits(), 
                    l.getCoordinate(axisX),
                    l.getCoordinate(axisY),
                    l.getCoordinate(axisZ),
                    l.getCoordinate(axisRotation));
            location.add(((ReferenceHeadMountable) hm).getHeadOffsets());
        }
        catch (Exception e) {
            Logger.error(e);
        }
        location = NullDriver.getSimulatedImperfectLocation(location, hm, looking);
        return location;
    }

    /**
     * Simulates Location imperfections in a NullDriver. If any other driver is configured this remains ineffective. 
     * 
     * @param location The perfect location to be made imperfect.
     * @param looking 
     * @return The imperfect location adjusted for non-squareness compensation and a visual homing offset.
     */
    public static Location getSimulatedImperfectLocation(Location location, HeadMountable hm, Looking looking) {
        ReferenceMachine machine = (ReferenceMachine) Configuration.get()
                .getMachine();
        if (machine.getDefaultDriver() instanceof NullDriver) {
            NullDriver driver =  (NullDriver) machine.getDefaultDriver();
            double simulatedNonSquarenessFactor = driver.getSimulatedNonSquarenessFactor();
            Location homeOffset = driver.getHomingOffsets();
            if (looking == Looking.Down) {
                // Add homing offset
                location = location.subtract(homeOffset);
            }
            else if (looking == Looking.Up && hm instanceof Nozzle) {
                // Add runout.
                location = location.add(driver.getSimulatedRunout().rotateXy(location.getRotation())); 
            }
            // Add Non-Squareness
            location = location.add(new Location(location.getUnits(), 
                    simulatedNonSquarenessFactor*location.getY(), 
                    0, 0, 0));
        }
        return location;
    }

    public static void drawSimulatedCameraNoise(Graphics2D gFrame, int width, int height) {
        ReferenceMachine machine = (ReferenceMachine) Configuration.get()
                .getMachine();
        if (machine.getDefaultDriver() instanceof NullDriver) {
            NullDriver driver =  (NullDriver) machine.getDefaultDriver();
            if (driver.simulatedNoise > 0) { 
                for (int noise = driver.simulatedNoise + (int) (Math.random()*driver.simulatedNoise); noise > 0; noise--) {
                    int x = (int) (Math.random()*width) - 1;
                    int y = (int) (Math.random()*height) - 1;
                    gFrame.setColor(new Color(255, 255, 255, (int)(Math.random()*16)));
                    gFrame.drawLine(x, y, x+(int)(Math.random()*3+0.5), y+(int)(Math.random()*3+0.5));
                }
                if (driver.simulatedNoise > 500) {
                    try {
                        Thread.sleep(3);
                    }
                    catch (InterruptedException e) {
                    }
                }
            }
        }
    }

}
