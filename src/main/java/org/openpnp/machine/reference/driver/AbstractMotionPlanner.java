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

package org.openpnp.machine.reference.driver;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis.BacklashCompensationMethod;
import org.openpnp.machine.reference.axis.ReferenceVirtualAxis;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Motion;
import org.openpnp.model.Motion.MotionOption;
import org.openpnp.model.Motion.MoveToCommand;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.MotionPlanner;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.NanosecondTime;
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

/**
 * The AbstractMotionPlanner does all the boring legwork for a typical MotionPlanner and provides the basis for 
 * sub-classing in advanced motion planners. <br/>
 * <ul>
 * <li> The moveTo() calls are recorded as MotionCommands but not yet executed.</li>
 * <li> As soon as some facility needs to wait for a move to actually complete (e.g. Vision),   
 *      the recorded MotionCommands are transformed into an execution plan of Motions.</li> 
 * <li> Any advanced motion planning can take place on the execution plan (Overrides on sub-classes).</li>  
 * <li> The execution plan is sent to the drivers.</li>
 * <li> Finally the actual wait for completion takes place.</li>
 * <li> Additional work such as homing(), driver coordination and enumeration, soft-limit checking and rotation 
 *      angle wrap-around is done.</li>
 * </ul>
 *
 */
public abstract class AbstractMotionPlanner extends AbstractModelObject implements MotionPlanner, PropertySheetHolder {

    @Attribute(required=false)
    private double maximumPlanHistory = 60; // s

    private ReferenceMachine machine;

    protected LinkedList<Motion> motionCommands = new LinkedList<>();
    protected TreeMap<Double, Motion> motionPlan = new TreeMap<Double, Motion>();

    private AxesLocation lastDirectionalBacklashOffset = new AxesLocation();
    private List<Driver> lastPlannedDrivers = new ArrayList<Driver>(); 

    @Override
    public synchronized void home() throws Exception {
        // Reset lastDirectionalBacklashOffset (we don't actually know it after homing, but it will be known after the first move).
        lastDirectionalBacklashOffset = new AxesLocation();
        // Home all the drivers with their respective mapped axes (can be an empty map). 
        for (Driver driver : getMachine().getDrivers()) {
            driver.home(getMachine());
        }
        // Home virtual axes to their homing coordinates (and check for unassigned axes).
        for (Axis axis : getMachine().getAxes()) {
            if (axis instanceof ControllerAxis) {
                if (((ControllerAxis) axis).getDriver() == null) {
                    throw new Exception("Axis "+axis.getName()+" has no driver set.");
                }
            }
            else if (axis instanceof ReferenceVirtualAxis) {
                ((CoordinateAxis) axis).setLengthCoordinate(((CoordinateAxis) axis).getHomeCoordinate());
            }
        }
        // Make sure we're on the same page with the controller and wait for still-stand.
        waitForCompletion(null, CompletionType.WaitForStillstandIndefinitely);
    }

    @Override
    public synchronized void setGlobalOffsets(AxesLocation axesLocation) throws Exception {
        // Make sure we're on the same page with the controller, but there is no need to  wait for it to physically complete.
        executeMotionPlan(CompletionType.CommandStillstand);
        // We need to adjust the driver Location for any lastDirectionalBacklashOffset.
        AxesLocation driverLocation = new AxesLocation(axesLocation.getControllerAxes(), 
                (a) -> axesLocation.getLengthCoordinate(a).add(lastDirectionalBacklashOffset.getLengthCoordinate(a)));
        // Offset all the specified axes on the respective drivers. 
        for (Driver driver : driverLocation.getAxesDrivers(getMachine())) {
            driver.setGlobalOffsets(getMachine(), driverLocation.drivenBy(driver));
        }
        // Offset all the axes (including virtual ones) to their new coordinates.
        for (Axis axis : axesLocation.getAxes()) {
            if (axis instanceof CoordinateAxis) {
                ((CoordinateAxis) axis).setLengthCoordinate(axesLocation.getLengthCoordinate(axis));
            }
        }
    }

    @Override
    public void moveTo(HeadMountable hm, AxesLocation axesLocation, double speed, MotionOption... options) throws Exception {
        // Handle soft limits and rotation axes limiting and wrap-around.
        axesLocation = limitAxesLocation(axesLocation, false);

        // Get current planned location of all the axes.
        AxesLocation currentLocation = new AxesLocation(getMachine()); 
        // The new planned locations must include all the machine axes, so put the given axesLocation into the whole set.
        AxesLocation newLocation = 
                currentLocation
                .put(axesLocation);

        // Make sure we don't collide axes across multiple drivers.
        interlockMotionAcrossDrivers(hm, currentLocation, newLocation);
        // Perform InterlockActuator before-move actuations.
        for (Actuator actuator : hm.getHead().getActuators()) {
            if (actuator.getInterlockMonitor() != null) {
                actuator.getInterlockMonitor().interlockActuation(actuator, currentLocation, newLocation, true, speed);
            }
        }

        // Note: the driver/actuator interlock can prompt locations changes (through location confirmation, 
        // rotational axes wrap-around etc.). Therefore we need to determine the coordinates again, same as above.

        // Handle soft limits and rotation axes limiting and wrap-around.
        axesLocation = limitAxesLocation(axesLocation, false);

        // Get current planned location of all the axes.
        currentLocation = new AxesLocation(getMachine()); 
        // The new planned locations must include all the machine axes, so put the given axesLocation into the whole set.
        newLocation = 
                currentLocation
                .put(axesLocation);

        // Create the motion commands needed for backlash compensation if enabled.
        createBacklashCompensatedMotion(hm, speed, currentLocation, newLocation, options);

        // Set all the axes (including virtual ones) to their new coordinates.
        for (Axis axis : axesLocation.getAxes()) {
            if (axis instanceof CoordinateAxis) {
                ((CoordinateAxis) axis).setLengthCoordinate(newLocation.getLengthCoordinate(axis));
            }
        }

        // Perform InterlockActuator after-move actuations.
        for (Actuator actuator : hm.getHead().getActuators()) {
            if (actuator.getInterlockMonitor() != null) {
                actuator.getInterlockMonitor().interlockActuation(actuator, currentLocation, newLocation, false, speed);
            }
        }
    }

    /**
     * When using multiple drivers with axes, we need to interlock motion commands between drivers, i.e. we need
     * to wait for the completion of the previous motion, before we can start the next motion. For example if we 
     * have X and Y on driver 1 and Z on driver 2 we need to make sure Z is up at Safe Z, before we can move X or Y
     * and vice versa. 
     * 
     * The interlock is avoided, when just one driver moves in multiple segments. For the given example, as long as 
     * just X and Y move and Z stays put, there is no need for interlock. 
     * 
     * @param hm
     * @param currentLocation
     * @param newLocation
     * @throws Exception
     */
    protected void interlockMotionAcrossDrivers(HeadMountable hm, AxesLocation currentLocation,
            AxesLocation newLocation) throws Exception {
        AxesLocation segment = currentLocation.motionSegmentTo(newLocation);
        List<Driver> drivers = segment.getAxesDrivers(getMachine());
        if (drivers.size() > 0) {
            if (lastPlannedDrivers.size() > 0 
                    && (drivers.size() > 1   
                            || lastPlannedDrivers.size() > 1
                            || (drivers.size() == 1 
                            && lastPlannedDrivers.size() == 1
                            && drivers.get(0) != lastPlannedDrivers.get(0)))) {
                // Either more than one driver involved in the previous/next move...
                // ... or a different single driver involved... 
                // ... means that we need to interlock motion across drivers and therefore wait for the previous move to complete.
                Logger.debug("Interlock motion accross drivers {} vs. {}", lastPlannedDrivers, drivers);
                // TODO: we might think about optimizing this i.e. not wait for hm that are unrelated. 
                // For now we wait for the whole machine i.e. we pass null for hm.
                waitForCompletion(null, CompletionType.WaitForStillstand);
            }
            lastPlannedDrivers = drivers;
        }
    }

    /**
     * Create the backlash compensated motion according to the settings on the axes. A sub-class might override this
     * method and provide a more advanced implementation, perhaps avoiding some extra moves by motion blending. 
     * 
     * @param hm
     * @param speed
     * @param currentLocation
     * @param newLocation
     * @param options
     * @return
     */
    protected synchronized AxesLocation createBacklashCompensatedMotion(HeadMountable hm, double speed,
            AxesLocation currentLocation, AxesLocation newLocation, MotionOption... options) {
        // Adjust the current location to include any backlash compensation offset that was applied in the last move.
        AxesLocation backlashCompensatedCurrentLocation = currentLocation.add(lastDirectionalBacklashOffset);
        AxesLocation backlashCompensatedNewLocation = newLocation;
        double backlashCompensatedSpeed = speed;
        boolean needsExtraBacklashMove = false;
        int optionFlags = Motion.optionFlags(options);
        // Get the segment vector of axes that really move.
        AxesLocation segment = currentLocation.motionSegmentTo(newLocation);
        // Go through all the axes.
        for (ControllerAxis axis : currentLocation.getControllerAxes()) {
            if (segment.contains(axis)) {
                if (axis instanceof ReferenceControllerAxis) {
                    ReferenceControllerAxis refAxis = ((ReferenceControllerAxis) axis);
                    Length backlashOffset = refAxis.getBacklashOffset(); 
                    if (backlashOffset.getValue() != 0) {
                        // An offset has to be applied.
                        if (refAxis.getBacklashCompensationMethod().isDirectionalMethod()) {
                            // The compensation is determines by the direction in which the axis travels. Because we assume some 
                            // slack or play, we move a bit farther in that direction. The actual compensation is only applied, if its 
                            // signum points into the direction of travel. In that way it is compatible with the other one-sided methods,
                            // the difference is that it has to be accurate.
                            Length axisSegment = segment.getLengthCoordinate(refAxis);
                            Length effectiveBacklashOffset = Math.signum(axisSegment.getValue()) == Math.signum(backlashOffset.getValue()) ? 
                                    backlashOffset 
                                    : new Length(0, LengthUnit.Millimeters);
                            AxesLocation effectiveBacklashAxisOffset = new AxesLocation(refAxis, effectiveBacklashOffset);
                            if (refAxis.getBacklashCompensationMethod() == BacklashCompensationMethod.DirectionalSneakUp) {
                                // Sneak up, this needs an extra move for last segment at slower speed.
                                Length sneakOffset = refAxis.getSneakUpOffset().multiply(Math.signum(axisSegment.getValue())).subtract(effectiveBacklashOffset);
                                backlashCompensatedNewLocation = backlashCompensatedNewLocation.subtract(
                                        new AxesLocation(refAxis, sneakOffset));
                                AxesLocation displacement = backlashCompensatedCurrentLocation.motionSegmentTo(backlashCompensatedNewLocation);
                                if (Math.signum(displacement.getCoordinate(refAxis)) != Math.signum(axisSegment.getValue())) {
                                    // Sneak length larger than displacement - just stay put then.
                                    backlashCompensatedNewLocation = backlashCompensatedNewLocation.put(
                                            new AxesLocation(refAxis, backlashCompensatedCurrentLocation.getLengthCoordinate(refAxis)));
                                }
                                needsExtraBacklashMove = true;
                                // Take the lowest speed factor of any backlash compensated axis. 
                                // Note, unlike in previous versions of OpenPnP this does not multiply with speed, it just lowers
                                // it to the minimum. So an already very low speed parameter will not be lowered further.
                                // The idea of OneSidedPositioning is also that it happens at the same speed every time i.e. the
                                // forces and tensions in the mechanical linkage will be similar.  
                                backlashCompensatedSpeed = Math.min(backlashCompensatedSpeed, refAxis.getBacklashSpeedFactor());
                            }
                            else {
                                // Apply to the backlashCompensatedNewLocation
                                backlashCompensatedNewLocation = backlashCompensatedNewLocation.add(
                                        effectiveBacklashAxisOffset);
                            }
                            // Note, this is also applied to the newLocation, as the target coordinate actually remains offset. 
                            newLocation = newLocation.add(
                                    effectiveBacklashAxisOffset);
                            // Remember the last backlash offset we applied. This is important to have the right starting location
                            // for the next move and other purposes such as setGlobalOffsets() or getMomentaryLocation().  
                            lastDirectionalBacklashOffset = lastDirectionalBacklashOffset.put(
                                    effectiveBacklashAxisOffset);
                        }
                        else {
                            // No directional backlash compensation.
                            lastDirectionalBacklashOffset = lastDirectionalBacklashOffset.put(
                                    new AxesLocation(refAxis, 0));
                            if (!Motion.MotionOption.SpeedOverPrecision.isSetIn(optionFlags)) {
                                // Check other methods
                                if (refAxis.getBacklashCompensationMethod() == BacklashCompensationMethod.OneSidedPositioning
                                        || (refAxis.getBacklashCompensationMethod() == BacklashCompensationMethod.OneSidedOptimizedPositioning
                                        && Math.signum(segment.getCoordinate(refAxis)) == Math.signum(backlashOffset.getValue()))) {
                                    // We have either full OneSidedPositioning or OneSidedOptimizedPositioning with a move that goes into the offset direction. 
                                    // Add the offset to the target location. 
                                    backlashCompensatedNewLocation = backlashCompensatedNewLocation.add(
                                            new AxesLocation(axis, backlashOffset));
                                    // This needs an extra move. 
                                    needsExtraBacklashMove = true;
                                    // Take the lowest speed factor of any backlash compensated axis. 
                                    // Note, unlike in previous versions of OpenPnP this does not multiply with speed, it just lowers
                                    // it to the minimum. So an already very low speed parameter will not be lowered further.
                                    // The idea of OneSidedPositioning is also that it happens at the same speed every time i.e. the
                                    // forces and tensions in the mechanical linkage will be similar.  
                                    backlashCompensatedSpeed = Math.min(backlashCompensatedSpeed, refAxis.getBacklashSpeedFactor());
                                }
                            }
                        }
                    }
                }
            }
            else {
                // No coordinate change in this axis. Adjust the new locations to include any backlash compensation offset 
                // that was applied in the last move.
                backlashCompensatedNewLocation = backlashCompensatedNewLocation.add(
                        new AxesLocation(axis, lastDirectionalBacklashOffset.getLengthCoordinate(axis)));
                newLocation = newLocation.add(
                        new AxesLocation(axis, lastDirectionalBacklashOffset.getLengthCoordinate(axis)));
            }
        }
        if (needsExtraBacklashMove) {
            // First move goes to the extra backlashCompensatedLocation.
            addMotion(hm, speed, 
                    backlashCompensatedCurrentLocation, 
                    backlashCompensatedNewLocation,
                    optionFlags);

            // Second move to the actual target at backlashCompensatedSpeed. 
            addMotion(hm, 
                    backlashCompensatedSpeed,
                    backlashCompensatedNewLocation, 
                    newLocation, 
                    optionFlags);
        }
        else {
            addMotion(hm, speed, 
                    backlashCompensatedCurrentLocation, 
                    newLocation, 
                    optionFlags);
        }
        return newLocation;
    }

    protected Motion addMotion(HeadMountable hm, double speed, AxesLocation location0,
            AxesLocation location1, int options) {
        Motion motionCommand = new Motion(
                hm, 
                location0, 
                location1, 
                speed,
                options);
        // Add to the recorded motion commands. 
        motionCommands.addLast(motionCommand);
        return motionCommand;
    }

    /**
     * Plan and then execute the pending motion commands. 
     * 
     * @param completionType
     * @throws Exception
     */
    protected synchronized void executeMotionPlan(CompletionType completionType) throws Exception {
        if (motionCommands.isEmpty()) {
            return;
        }

        // Put the recorded motion commands into an execution plan. 
        List<Motion> executionPlan = motionCommands;

        // The motion commands are reset.
        motionCommands = new LinkedList<>();

        // Apply any optimization to the execution plan. This is where advanced MotionPlanner sub-classes will shine.
        optimizeExecutionPlan(executionPlan, completionType);

        // Now execute the plan against the drivers.
        // We also record it into the real-time motionPlan to enable motion prediction and simulation. 
        double t = NanosecondTime.getRuntimeSeconds();
        if (motionPlan.isEmpty() == false && motionPlan.lastKey() > t) {
            // Append to a plan that is still running. 
            t = motionPlan.lastKey();
        }
        ReferenceMachine machine = (ReferenceMachine) Configuration.get().getMachine();
        List<Head> movedHeads = new ArrayList<>();
        boolean first = true;
        for (Motion plannedMotion : executionPlan) {
            if (!plannedMotion.hasOption(MotionOption.Stillstand)) {
                // Put into timed plan.
                double dt = plannedMotion.getTime();
                // Note, all-virtual moves can have dt == 0.0, so we take a nano-second, to make sure a new Map entry is created 
                // in the motionPlan.
                t += Math.max(dt, 1e-9);  
                plannedMotion.setPlannedTime1(t);
                motionPlan.put(t, plannedMotion);
                // Execute across drivers.
                ReferenceHeadMountable  hm = (ReferenceHeadMountable) plannedMotion.getHeadMountable();
                if (hm != null) {
                    movedHeads.add(hm.getHead());
                    if (executeMoveTo(machine, hm, plannedMotion, first)) {
                        first = false;
                    }
                }
            }
        }
        // Publish recorded Diagnostics
        publishDiagnostics();
        // Notify heads.
        for (Head movedHead : movedHeads) {
            machine.fireMachineHeadActivity(movedHead);
        }
    }
    /**
     * Subclasses must override this method to implement their advanced planning magic.
     * 
     * @param executionPlan
     * @param completionType
     * @throws Exception 
     */
    protected abstract void optimizeExecutionPlan(List<Motion> executionPlan, CompletionType completionType) throws Exception;

    /**
     * Standard implementation to interpolate and execute driver moveTo commands. 
     * May generate more complex motion with simulated jerk control, and/or curved trajectories from motion blending, etc. 
     * according to the plannedMotion.  
     * 
     * @param machine
     * @param hm
     * @param plannedMotion
     * @param firstAfterCoordination 
     * @return true if a driver move was executed.
     * @throws Exception
     */
    protected boolean executeMoveTo(ReferenceMachine machine, ReferenceHeadMountable hm,
            Motion plannedMotion, boolean firstAfterCoordination) throws Exception {
        AxesLocation motionSegment = plannedMotion.getLocation0().motionSegmentTo(plannedMotion.getLocation1());
        // Note, this loop will be empty if the motion is empty, i.e. if it only contains VirtualAxis movement.
        boolean firstDriver = true;
        for (Driver driver : motionSegment.getAxesDrivers(machine)) {
            for (Motion.MoveToCommand moveToCommand : plannedMotion
                    .interpolatedMoveToCommands(driver, isInterpolationRetiming())) {
                driver.moveTo(hm, moveToCommand);
                try {
                    recordDiagnostics(plannedMotion, moveToCommand, driver, firstAfterCoordination, firstDriver);
                }
                catch (Exception e) {
                    Logger.error(driver.getName()+" diagnostics failed: {}", e);
                }
            }
            firstDriver = false;
        }
        return !firstDriver;
    }

    /**
     * Sub.classes with diagnostics can override this method to record (interpolated) motion.
     * 
     * @param plannedMotion
     * @param moveToCommand
     * @param driver 
     * @param firstAfterCoordination 
     * @param firstDriver
     */
    protected void recordDiagnostics(Motion plannedMotion, MoveToCommand moveToCommand, Driver driver, boolean firstAfterCoordination, boolean firstDriver) {
    }

    protected void publishDiagnostics() {
    }

    /**
     * Limits the specified AxesLocation to nominal coordinates. Throws or returns null if a soft limit is 
     * violated. Also limits rotation axes to their limited or wrapped-around coordinates. 
     * 
     * @param axesLocation
     * @param silent If true, returns null on soft limit violations rather than throwing Exceptions. 
     * @return
     * @throws Exception
     */
    protected AxesLocation limitAxesLocation(AxesLocation axesLocation, boolean silent)
            throws Exception {
        for (ControllerAxis axis : axesLocation.getControllerAxes()) {
            if (axis instanceof ReferenceControllerAxis) {
                ReferenceControllerAxis refAxis = (ReferenceControllerAxis) axis;
                if (refAxis.getType() == Axis.Type.Rotation) {
                    // Handle rotation axes.
                    if (refAxis.isWrapAroundRotation()) {
                        // Set the rotation to be the shortest way around from the current rotation
                        double currentAngle = refAxis.getCoordinate();
                        double specifiedAngle = axesLocation.getCoordinate(axis);
                        double newAngle = currentAngle + Utils2D.normalizeAngle180(specifiedAngle - currentAngle);
                        axesLocation = axesLocation.put(new AxesLocation(refAxis, newAngle));
                    }
                    else if (refAxis.isLimitRotation()) {
                        // Set the rotation to be within the +/-180 degree range
                        axesLocation = axesLocation.put(new AxesLocation(refAxis, 
                                Utils2D.normalizeAngle180(axesLocation.getCoordinate(refAxis))));
                    } 
                    // Note, the combination isLimitRotation() and isWrapAroundRotation() will be handled
                    // when the motion is complete, i.e. in waitForCompletion().
                }
                else { // Never soft-limit a rotation axis, as the config is hidden on the GUI and might contain garbage.
                    Length coordinate = axesLocation.getLengthCoordinate(refAxis).convertToUnits(Configuration.get().getSystemUnits());  
                    if (refAxis.isSoftLimitLowEnabled()) {
                        Length limit = refAxis.getSoftLimitLow().convertToUnits(Configuration.get().getSystemUnits());
                        // It must be lower and not equal in axis resolution.
                        if (coordinate.getValue() < limit.getValue() 
                                && !refAxis.coordinatesMatch(limit, coordinate)) { 
                            if (silent) {
                                return null;    
                            }
                            throw new Exception(String.format("Can't move %s to %s, lower than soft limit %s.",
                                    refAxis.getName(), coordinate, limit));
                        }
                    }
                    if (refAxis.isSoftLimitHighEnabled()) {
                        Length limit = refAxis.getSoftLimitHigh().convertToUnits(Configuration.get().getSystemUnits());
                        // It must be higher and not equal in axis resolution.
                        if (coordinate.getValue() > limit.getValue()
                                && !refAxis.coordinatesMatch(limit, coordinate)) { 
                            if (silent) {
                                return null;    
                            }
                            throw new Exception(String.format("Can't move %s to %s, higher than soft limit %s.",
                                    refAxis.getName(), coordinate, limit));
                        }
                    }
                }
            }
        }
        return axesLocation;
    }

    @Override
    public boolean isValidLocation(AxesLocation axesLocation) {
        try {
            return limitAxesLocation(axesLocation, true) != null;
        }
        catch (Exception e) {
            // Note this will never happen, as we pass silent=true.
            return false;
        }
    }

    @Override
    public synchronized Motion getMomentaryMotion(double time) {
        Map.Entry<Double, Motion> entry1 = motionPlan.higherEntry(time);
        if (entry1 != null) {
            // Return the motion.
            Motion motion = entry1.getValue();
            return motion;
        }
        else {
            // Plan empty or machine stopped before this time.  
            entry1 = motionPlan.lastEntry();
            AxesLocation currentLocation; 
            if (entry1 != null) {
                // Machine stopped before this time, take the last exit location.
                currentLocation = entry1.getValue().getLocation1();
            }
            else {
                // Nothing in the plan (yet), just get the current axes location.
                currentLocation = new AxesLocation(getMachine());
            }
            // Mark it as Stillstand, so callers can wait for it. 
            Motion motion = new Motion( 
                    null, 
                    currentLocation,
                    currentLocation,
                    1.0,
                    MotionOption.Stillstand);
            // Anchor it in real-time.
            motion.setPlannedTime1(time);
            return motion;
        }
    }

    @Override
    public void waitForCompletion(HeadMountable hm, CompletionType completionType)
            throws Exception {
        // Now is high time to plan and execute the queued motion commands. 
        executeMotionPlan(completionType);

        if (completionType.isEnforcingStillstand()) {
            // Wait for the drivers.
            waitForDriverCompletion(hm, completionType);
            // The drivers might have reported new coordinates back. Propagate to planned axis coordinates, 
            // applying the backlash offset in reverse.
            AxesLocation reportedLocation = new AxesLocation(getMachine().getAxes(), 
                (axis) -> ((axis instanceof ControllerAxis) ?
                    ((ControllerAxis) axis).getDriverLengthCoordinate()
                        .subtract(lastDirectionalBacklashOffset.getLengthCoordinate(axis)) :
                        null));
            AxesLocation currentLocation = new AxesLocation(reportedLocation.getControllerAxes(),
                    (axis) -> axis.getLengthCoordinate());
            AxesLocation diff = reportedLocation.motionSegmentTo(currentLocation);
            if (!diff.matches(AxesLocation.zero)) {
                Logger.debug("Reported location changes current location from "+currentLocation+" to "+reportedLocation);
                // Reported position has in deed changed.
                reportedLocation.setToCoordinates();
                // Notify heads.
                for (Head movedHead : getMachine().getHeads()) {
                    getMachine().fireMachineHeadActivity(movedHead);
                }
            }
        }
        // Apply the rotation axes wrap-around handling.
        wrapUpCoordinates();
        // Remove old stuff.
        clearMotionPlanOlderThan(NanosecondTime.getRuntimeSeconds() - maximumPlanHistory);
    }

    /**
     * Apply the rotation axes wrap-around handling.
     * 
     * @throws Exception
     */
    protected synchronized void wrapUpCoordinates() throws Exception {
        AxesLocation mappedAxes = new AxesLocation(getMachine()).byType(Type.Rotation);
        for (ControllerAxis axis : mappedAxes.getControllerAxes()) {
            if (axis instanceof ReferenceControllerAxis) {
                ReferenceControllerAxis refAxis = (ReferenceControllerAxis) axis;
                if (refAxis.isLimitRotation() && refAxis.isWrapAroundRotation()) {
                    double anglePresent = refAxis.getDriverCoordinate();
                    double angleWrappedAround = Utils2D.normalizeAngle180(anglePresent);
                    if (anglePresent != angleWrappedAround) {
                        refAxis.getDriver().setGlobalOffsets(getMachine(), 
                                new AxesLocation(refAxis, angleWrappedAround));
                        // This also reflects in the motion planner's coordinate, but account for any 
                        // directional backlash offset.
                        refAxis.setLengthCoordinate(refAxis.getDriverLengthCoordinate()
                                .subtract(lastDirectionalBacklashOffset.getLengthCoordinate(refAxis)));
                    }
                }
            }
        }
    }

    protected void waitForDriverCompletion(HeadMountable hm, CompletionType completionType)
            throws Exception {
        // Wait for the driver(s).
        ReferenceMachine machine = getMachine();
        // If the hm is given, we just wait for the drivers of that hm, otherwise we wait for all drivers of the machine axes.
        AxesLocation mappedAxes = (hm != null ? 
                hm.getMappedAxes(machine) 
                : new AxesLocation(machine));
        if (!mappedAxes.isEmpty()) {
            for (Driver driver : mappedAxes.getAxesDrivers(machine)) {
                driver.waitForCompletion((ReferenceHeadMountable) hm, completionType);
            }
        }
    }

    public ReferenceMachine getMachine() {
        if (machine == null) {
            machine = (ReferenceMachine) Configuration.get().getMachine();
        }
        return machine;
    }

    @Override
    public synchronized void clearMotionPlanOlderThan(double time) {
        while (motionPlan.isEmpty() == false && motionPlan.firstKey() < time) {
            motionPlan.remove(motionPlan.firstKey());
        }
    }

    public boolean isInterpolationRetiming() {
        return false;
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    abstract public Wizard getConfigurationWizard();

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard(), "Motion Planning")};
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName();
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return null;
    }
}
