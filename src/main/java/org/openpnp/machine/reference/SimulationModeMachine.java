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

import java.awt.Color;
import java.awt.Graphics2D;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.machine.reference.camera.ImageCamera;
import org.openpnp.machine.reference.driver.NullDriver;
import org.openpnp.machine.reference.feeder.BlindsFeeder;
import org.openpnp.machine.reference.feeder.ReferenceStripFeeder;
import org.openpnp.machine.reference.wizards.SimulationModeMachineConfigurationWizard;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Motion;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Camera.Looking;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Locatable.LocationOption;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.NanosecondTime;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
 * Add a basic simulation mode to the ReferenceMachine. 
 * 
 * These are just the foundations of something that could become bigger with time.
 * The idea is to be able to put any machine configuration in simulation mode. 
 * 
 * For a NullDriver this is the only operating mode, for GcodeDriver this mode could mean to redirect 
 * its communications to a GcodeServer back-end. 
 *  
 * Cameras should be redirected to either a SimulatedUpCamera or and ImageCamera. Ideally the image source could  
 * one day be scanned off the real machine table by the regular camera (some concepts exist).  
 * 
 * Because of the new Axis design, multiple simulated drivers must be supported to test new features (in the future), 
 * plus the cameras are obviously involved. Therefore this simulation switchboard must be in the Machine, rather than 
 * the driver. 
 * 
 * For now this only works with NullDriver, SimulatedUpCamera and ImageCamera. Some physical imperfections are 
 * simulated.  
 *
 */
public class SimulationModeMachine extends ReferenceMachine {

    /**
     * The SimulationMode sets the level of simulation.
     * Off: Only available for real machines with real drivers. Switches drivers and cameras to regular operation.  
     * IdealMachine: Simulates the ideal machine, i.e. no imperfections are simulated (same as original NullDriver).  
     * StaticImperfectionsMachine: Simulates static imperfections such as non-squareness, i.e. those that are configured
     * in the Machine Configuration and might break operations, if switched off.   
     * DynamicImperfectionsMachine: Simulates all the imperfections, including dynamic ones, i.e. those that are calibrated 
     * each time.
     *  
     */
    public enum SimulationMode {
        Off,
        IdealMachine,
        StaticImperfectionsMachine,
        DynamicImperfectionsMachine;

        // Using these methods we can refine the choices later.

        public boolean isImperfectMachine() {
            return this.ordinal() > IdealMachine.ordinal();
        }

        public boolean isDynamicallyImperfectMachine() {
            return this.ordinal() > StaticImperfectionsMachine.ordinal();
        }
    }
    @Attribute(required = false)
    private SimulationMode simulationMode = SimulationMode.Off;

    /**
     * The simulated non-squareness is applied to what the simulated cameras see.
     * Works on the ImageCamera.
     */
    @Attribute(required = false)
    private double simulatedNonSquarenessFactor = 0.0;

    /**
     * Simulated runout on nozzle tips (currently all noozle tips get the same).
     * Works on the SimulatedUpCamera.
     */
    @Element(required = false)
    private Length simulatedRunout = new Length(0, LengthUnit.Millimeters);

    @Attribute(required = false)
    private double simulatedRunoutPhase = 30;

    /**
     * Simulated camera noise (number of sparks per frame) to test camera settle. 
     * Works on ImageCamera and SimulatedUpCamera.
     */
    @Attribute(required = false)
    private int simulatedCameraNoise = 0;

    /**
     * Simulated camera lag [s] to test camera settle. 
     * Works on ImageCamera and SimulatedUpCamera.
     */
    @Attribute(required = false)
    private double simulatedCameraLag= 0;

    /**
     * Simulated vibration to test camera settle. Initial max. amplitude.
     */
    @Attribute(required = false)
    private double simulatedVibrationAmplitude = 0;

    /**
     * Simulated vibration to test camera settle. Duration in seconds to ~1%.
     */
    @Attribute(required = false)
    private double simulatedVibrationDuration = 0.2;

    /**
     * Simulated homing error. Introduces an initial location error that Visual Homing needs to correct.
     * Works on ImageCamera.
     */
    @Element(required = false)
    private Location homingError = new Location(LengthUnit.Millimeters);

    /**
     * Checks Picks/Places by visually locating the tape pocket/solder lands in the ImageCamera.
     * Throws errors instead.
     */
    @Element(required = false)
    private boolean pickAndPlaceChecking = false;

    @Override
    public void setEnabled(boolean enabled) throws Exception {
        // TODO: re-wire drivers and cameras. 
        super.setEnabled(enabled);
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard()),
                new PropertySheetWizardAdapter(new SimulationModeMachineConfigurationWizard(this), "Simulation Mode"),};
    }

    public SimulationMode getSimulationMode() {
        return simulationMode;
    }

    public void setSimulationMode(SimulationMode simulationMode) {
        this.simulationMode = simulationMode;
    }

    public double getSimulatedNonSquarenessFactor() {
        return simulatedNonSquarenessFactor;
    }

    public void setSimulatedNonSquarenessFactor(double simulatedNonSquarenessFactor) {
        this.simulatedNonSquarenessFactor = simulatedNonSquarenessFactor;
    }

    public Length getSimulatedRunout() {
        return simulatedRunout;
    }

    public double getSimulatedRunoutPhase() {
        return simulatedRunoutPhase;
    }

    public void setSimulatedRunoutPhase(double simulatedRunoutPhase) {
        this.simulatedRunoutPhase = simulatedRunoutPhase;
    }

    public void setSimulatedRunout(Length simulatedRunout) {
        this.simulatedRunout = simulatedRunout;
    }

    public int getSimulatedCameraNoise() {
        return simulatedCameraNoise;
    }

    public void setSimulatedCameraNoise(int simulatedCameraNoise) {
        this.simulatedCameraNoise = simulatedCameraNoise;
    }

    public double getSimulatedCameraLag() {
        return simulatedCameraLag;
    }

    public void setSimulatedCameraLag(double simulatedCameraLag) {
        this.simulatedCameraLag = simulatedCameraLag;
    }

    public double getSimulatedVibrationAmplitude() {
        return simulatedVibrationAmplitude;
    }

    public void setSimulatedVibrationAmplitude(double simulatedVibrationAmplitude) {
        this.simulatedVibrationAmplitude = simulatedVibrationAmplitude;
    }

    public double getSimulatedVibrationDuration() {
        return simulatedVibrationDuration;
    }

    public void setSimulatedVibrationDuration(double simulatedVibrationDuration) {
        this.simulatedVibrationDuration = simulatedVibrationDuration;
    }

    public Location getHomingError() {
        return homingError;
    }

    public void setHomingError(Location homingError) {
        this.homingError = homingError;
    }

    public boolean isPickAndPlaceChecking() {
        return pickAndPlaceChecking;
    }

    public void setPickAndPlaceChecking(boolean pickAndPlaceChecking) {
        this.pickAndPlaceChecking = pickAndPlaceChecking;
    }

    public void resetAllFeeders() {
        for (Feeder feeder : getFeeders()) {
            if (feeder instanceof ReferenceStripFeeder) {
                ((ReferenceStripFeeder) feeder).setFeedCount(0);
            }
            if (feeder instanceof BlindsFeeder) {
                ((BlindsFeeder) feeder).setFeedCount(0);
            }
        }
    }

    public void setMachineTableZ(Length machineTableZ) {
        for (Feeder feeder : getFeeders()) {
            if (feeder instanceof ReferenceFeeder) {
                ((ReferenceFeeder) feeder)
                 .setLocation(((ReferenceFeeder) feeder).getLocation()
                         .derive(null, null, 
                                 machineTableZ.convertToUnits(((ReferenceFeeder) feeder).getLocation().getUnits())
                                 .getValue(), 
                                 null));
            }
            if (feeder instanceof ReferenceStripFeeder) {
                ((ReferenceStripFeeder) feeder)
                .setReferenceHoleLocation(((ReferenceStripFeeder) feeder).getReferenceHoleLocation()
                        .derive(null, null, 
                                machineTableZ.convertToUnits(((ReferenceStripFeeder) feeder).getReferenceHoleLocation().getUnits())
                                .getValue(), 
                                null));
                ((ReferenceStripFeeder) feeder)
                .setLastHoleLocation(((ReferenceStripFeeder) feeder).getLastHoleLocation()
                        .derive(null, null, 
                                machineTableZ.convertToUnits(((ReferenceStripFeeder) feeder).getLastHoleLocation().getUnits())
                                .getValue(), 
                                null));
            }
        }
        for (BoardLocation boardLocation : MainFrame.get().getJobTab().getJob().getBoardLocations()) {
            boardLocation.setLocation(boardLocation.getLocation().derive(null, null, 
                    machineTableZ.convertToUnits(boardLocation.getLocation().getUnits())
                    .getValue(), 
                    null));
        }
        for (Camera camera : getCameras()) {
            if (camera instanceof ReferenceCamera) {
                ((ReferenceCamera) camera)
                .setHeadOffsets(((ReferenceCamera) camera).getHeadOffsets()
                        .derive(null, null, 
                                machineTableZ.convertToUnits(((ReferenceCamera) camera).getHeadOffsets().getUnits())
                                .getValue(), 
                                null));
            }
        }
    }

    public static SimulationModeMachine getSimulationModeMachine() {
        Machine machine = Configuration.get()
                .getMachine();
        if (machine instanceof SimulationModeMachine) {
            return (SimulationModeMachine) machine;
        }
        return null;
    }

    /**
     * Simulates the Actuator. 
     * 
     * @param actuator
     * @param value
     * @param realtime
     * @throws Exception
     */
    public static void simulateActuate(Actuator actuator, Object value, boolean realtime) throws Exception {
        SimulationModeMachine machine = getSimulationModeMachine();
        if (machine != null 
                && machine.getSimulationMode() != SimulationMode.Off) {
            if (value instanceof Boolean && machine.isPickAndPlaceChecking()) {
                // Check if this is a nozzle vacuum actuator.
                if (actuator.getHead() != null) {
                    Camera camera = actuator.getHead().getDefaultCamera();
                    if (camera instanceof ImageCamera) {
                        for (Nozzle nozzle : actuator.getHead().getNozzles()) {
                            if (nozzle instanceof ReferenceNozzle 
                                    && ((ReferenceNozzle) nozzle).getVacuumActuator() == actuator) {
                                // Got the vacuum actuator, which is a signal to check for pick/place.
                                if (nozzle.getPart() != null) {
                                    Location location = SimulationModeMachine.getSimulatedPhysicalLocation(nozzle, null);
                                    if (location.getLinearDistanceTo(machine.getDiscardLocation()) > 4.0) {
                                        if ((Boolean)value == true) {
                                            // Pick
                                            if (!((ImageCamera) camera).isPickLocation(location, nozzle)) {
                                                throw new Exception("Nozzle "+nozzle.getName()+" part "+nozzle.getPart().getId()
                                                        +" pick location not recognized.");
                                            }
                                        }
                                        else {
                                            // Pick
                                            if (!((ImageCamera) camera).isPlaceLocation(location, nozzle)) {
                                                throw new Exception("Nozzle "+nozzle.getName()+" part "+nozzle.getPart().getId()
                                                        +" place location not recognized.");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (realtime) {
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException e) {
            }
        }
    }

    /**
     * Simulates imperfections in the physical location of a HeadMountable.  
     *  
     * @param hm
     * @param looking
     * @return
     */
    public static Location getSimulatedPhysicalLocation(HeadMountable hm, Looking looking) {
        // Use ideal location as a default, used too in case this fails (not a place to throw).
        Location location = hm.getLocation().convertToUnits(AxesLocation.getUnits()); 
        // Try to get a simulated physical location.
        SimulationModeMachine machine = getSimulationModeMachine();
        if (machine == null || machine.getSimulationMode() == SimulationMode.Off) {
            // Not a simulation machine. Just take the nominal location.
            double cameraTime = NanosecondTime.getRuntimeSeconds();
            Motion momentary = ((ReferenceMachine) Configuration.get()
                    .getMachine()).getMotionPlanner()
                    .getMomentaryMotion(cameraTime);
            AxesLocation axesLocation = momentary.getMomentaryLocation(cameraTime - momentary.getPlannedTime0());
            // NOTE this specifically makes the assumption that the axes transform from raw 1:1
            location = hm.toTransformed(axesLocation);
            // Transform back. This bypasses any compensations, such as runout compensation.
            location = hm.toHeadMountableLocation(location);
        } 
        else {
            double lag = 0;
            if (looking != null
                    && machine.getSimulationMode().isDynamicallyImperfectMachine()) {
                lag = machine.getSimulatedCameraLag();
            }
            double cameraTime = NanosecondTime.getRuntimeSeconds() - lag;
            Motion momentary = machine.getMotionPlanner()
                    .getMomentaryMotion(cameraTime);
            AxesLocation axesLocation = momentary.getMomentaryLocation(cameraTime - momentary.getPlannedTime0());
            //AxesLocation axesVelocity = momentary.getVector(Motion.Derivative.Velocity);
            AxesLocation mappedAxes = hm.getMappedAxes(machine);
            try {
                for (Driver driver : mappedAxes.getAxesDrivers(machine)) {
                    AxesLocation homingOffsets = null;
                    if (driver instanceof NullDriver) {
                        if (looking == Looking.Down) {
                            homingOffsets = ((NullDriver)driver).getHomingOffsets();
                            // Apply homing offset
                            axesLocation = axesLocation.subtract(homingOffsets);
                        }
                    }
                }
                if (machine.getSimulationMode().isDynamicallyImperfectMachine()) {
                    // Add vibrations
                    double amplitude = machine.getSimulatedVibrationAmplitude();
                    if (amplitude != 0) {
                        double vibrationDuration = machine.getSimulatedVibrationDuration(); // s (practical, down to ~1%)
                        double vibrationEigen = 13.313; // Hz
                        double xIntegral = 0, yIntegral = 0;
                        double dt = 1/vibrationEigen/10;
                        Axis xAxis = mappedAxes.getAxis(Axis.Type.X);
                        Axis yAxis = mappedAxes.getAxis(Axis.Type.Y);
                        double t;
                        for (t = 0; t < vibrationDuration; t += dt) {
                            Motion vibration = machine.getMotionPlanner()
                                    .getMomentaryMotion(cameraTime - t);
                            AxesLocation a = vibration.getMomentaryAcceleration(cameraTime - t - vibration.getPlannedTime0());
                            double x = a.getCoordinate(xAxis);
                            double y = a.getCoordinate(yAxis);
                            if (x != 0 || y != 0) {
                                double amp = Math.exp(-t*4/vibrationDuration)*amplitude*dt;
                                double ph = t*2*Math.PI*vibrationEigen;
                                //Logger.trace("t="+(cameraTime - t)+" ax="+x+" ay="+y+" damp="+Math.exp(-t*3/vibrationDuration)+" cos(ph)="+Math.cos(ph));
                                xIntegral += Math.cos(ph)*x*amp;
                                yIntegral += Math.cos(ph)*y*amp;
                            }
                        }
    
                        if (xIntegral != 0 || yIntegral != 0) {
                            //Logger.trace("vibration t="+(cameraTime - t)+" x="+xIntegral+" y="+yIntegral);
                            axesLocation = axesLocation.add(new AxesLocation((a, b) -> (b),
                                new AxesLocation(xAxis, -xIntegral), 
                                new AxesLocation(yAxis, -yIntegral)));
                        }
                        
                    }
                }

                if (hm instanceof Nozzle
                        && machine.getSimulationMode().isDynamicallyImperfectMachine()) {
                    // Add Runout.
                    double runout = machine.getSimulatedRunout()
                            .convertToUnits(AxesLocation.getUnits()).getValue();
                    // Note, positive  phase shift is the positive shift of the curve which means the angle is subtracted,
                    // see: https://en.wikipedia.org/wiki/Phase_(waves)#General_definition  
                    //      ReferenceNozzleTipCalibration.ModelBasedRunoutCompensation.getRunout(double)
                    double rotation = axesLocation.getCoordinate(mappedAxes.getAxis(Axis.Type.Rotation));
                    Location runoutVector = new Location(AxesLocation.getUnits(),
                            runout, 0, 0, 0)
                            .rotateXy(rotation-machine.getSimulatedRunoutPhase());
                    axesLocation = axesLocation.add(mappedAxes.getTypedLocation(runoutVector)); 
                }

                if (machine.getSimulationMode().isImperfectMachine()) {
                    // Subtract Non-Squareness to simulate it in the sim cameras.
                    double y = axesLocation.getCoordinate(mappedAxes.getAxis(Axis.Type.Y));
                    axesLocation = axesLocation.add(new AxesLocation(mappedAxes.getAxis(Axis.Type.X), 
                            machine.getSimulatedNonSquarenessFactor()*y)); 
                }

                // NOTE this specifically makes the assumption that the axes transform from raw 1:1
                location = hm.toTransformed(axesLocation, 
                        LocationOption.SuppressStaticCompensation,
                        LocationOption.SuppressDynamicCompensation);
                // Transform back. This bypasses any compensations, such as runout compensation.
                location = hm.toHeadMountableLocation(location, 
                        LocationOption.SuppressStaticCompensation,
                        LocationOption.SuppressDynamicCompensation);
            }
            catch (Exception e) {
                Logger.error(e);
            }
        }
        return location;
    }

    public static void drawSimulatedCameraNoise(Graphics2D gFrame, int width, int height) {
        SimulationModeMachine machine = getSimulationModeMachine();
        if (machine != null 
                && machine.getSimulationMode().isDynamicallyImperfectMachine()) {
            if (machine.getSimulatedCameraNoise() > 0) { 
                for (int noise = (int) (Math.random()*machine.getSimulatedCameraNoise()); noise > 0; noise--) {
                    int x = (int) (Math.random()*width) - 1;
                    int y = (int) (Math.random()*height) - 1;
                    gFrame.setColor(new Color(255, 255, 255, (int)(Math.random()*16)));
                    gFrame.drawLine(x, y, x+(int)(Math.random()*3-1.0), y+(int)(Math.random()*3-1.0));
                }
            }
        }
    }
}
