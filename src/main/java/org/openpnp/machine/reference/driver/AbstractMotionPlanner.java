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

import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis.BacklashCompensationMethod;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Motion;
import org.openpnp.model.Motion.MotionOption;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.MotionPlanner;
import org.openpnp.util.NanosecondTime;
import org.openpnp.util.Utils2D;
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
public abstract class AbstractMotionPlanner implements MotionPlanner {

    @Attribute(required=false)
    private double maximumPlanHistory = 60; 

    private ReferenceMachine machine;

    protected LinkedList<Motion> motionCommands = new LinkedList<>();
    protected TreeMap<Double, Motion> motionPlan = new TreeMap<Double, Motion>();

    private AxesLocation lastDirectionalBacklashOffset = new AxesLocation();

    @Override
    public synchronized void home() throws Exception {
        // Just to make sure we're on the same page in the planner.
        waitForCompletion(null, CompletionType.WaitForStillstand);
        // Reset lastDirectionalBacklashOffset (we don't actually know it, but it will be known after the first move).
        lastDirectionalBacklashOffset = new AxesLocation();
        // Home all the drivers with their respective mapped axes (can be an empty map). 
        for (Driver driver : getMachine().getDrivers()) {
            ((ReferenceDriver) driver).home(getMachine());
        }
        // Home all the axes (including virtual ones) to their homing coordinates.
        for (Axis axis : getMachine().getAxes()) {
            if (axis instanceof CoordinateAxis) {
                ((CoordinateAxis) axis).setLengthCoordinate(((CoordinateAxis) axis).getHomeCoordinate());
            }
        }
    }

    @Override
    public synchronized void setGlobalOffsets(AxesLocation axesLocation) throws Exception {
        // We need to adjust the driver Location for any lastDirectionalBacklashOffset.
        AxesLocation driverLocation = new AxesLocation(axesLocation.getControllerAxes(), 
                (a) -> axesLocation.getLengthCoordinate(a).add(lastDirectionalBacklashOffset.getLengthCoordinate(a)));
        // Offset all the specified axes on the respective drivers. 
        for (Driver driver : driverLocation.getAxesDrivers(getMachine())) {
            ((ReferenceDriver) driver).setGlobalOffsets(getMachine(), driverLocation.drivenBy(driver));
        }
        // Offset all the axes (including virtual ones) to their new coordinates.
        for (Axis axis : axesLocation.getAxes()) {
            if (axis instanceof CoordinateAxis) {
                ((CoordinateAxis) axis).setLengthCoordinate(axesLocation.getLengthCoordinate(axis));
            }
        }
    }

    @Override
    public synchronized void moveTo(HeadMountable hm, AxesLocation axesLocation, double speed, MotionOption... options) throws Exception {
        // Handle soft limits and rotation axes limiting and wrap-around.
        axesLocation = limitAxesLocation(axesLocation, false);

        // Get current location of all the axes.
        AxesLocation currentLocation = new AxesLocation(getMachine()); 
        // The new locations must include all the machine axes, so put them into the whole set.
        AxesLocation newLocation = 
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
    }

    protected AxesLocation createBacklashCompensatedMotion(HeadMountable hm, double speed,
            AxesLocation currentLocation, AxesLocation newLocation, MotionOption... options) {
        int optionFlags = Motion.optionFlags(options);
        AxesLocation backlashCompensatedLocation = newLocation;
        double backlashCompensatedSpeed = speed;
        boolean needsExtraBacklashMove = false;
        if (!Motion.MotionOption.SpeedOverPrecision.isSetIn(optionFlags)) {
            // Get the segment vector of axes that really move.
            AxesLocation segment = currentLocation.motionSegmentTo(newLocation);
            for (ControllerAxis axis : segment.getControllerAxes()) {
                if (axis instanceof ReferenceControllerAxis) {
                    ReferenceControllerAxis refAxis = ((ReferenceControllerAxis) axis);
                    Length backlashOffset = refAxis.getBacklashOffset(); 
                    if (backlashOffset.getValue() != 0) {
                        // An offset has to be applied.
                        if (refAxis.getBacklashCompensationMethod() == BacklashCompensationMethod.OneSidedPositioning
                                || (refAxis.getBacklashCompensationMethod() == BacklashCompensationMethod.OneSidedOptimizedPositioning
                                && Math.signum(segment.getCoordinate(refAxis)) == Math.signum(backlashOffset.getValue()))) {
                            // We have either full OneSidedPositioning or OneSidedOptimizedPositioning with a move that goes into the offset direction. 
                            // Add the offset to the target location, and the move back from that to the actual target location. 
                            backlashCompensatedLocation = backlashCompensatedLocation.add(
                                    new AxesLocation(axis, backlashOffset));
                            // This needs an extra move. 
                            needsExtraBacklashMove = true;
                            // Take the lowest speed factor of any backlash compensated axis. 
                            // Note, unlike in previous versions of OpenPnP this does not multiply with speed, it just lowers
                            // it to the minimum. So an already very low speed parameter  will not be lowered further.
                            // The idea of OneSidedPositioning is also that it happens at the same speed every time i.e. the
                            // forces and tensions in the mechanical linkage will be similar.  
                            backlashCompensatedSpeed = Math.min(backlashCompensatedSpeed, refAxis.getBacklashSpeedFactor());
                        }
                        else if (refAxis.getBacklashCompensationMethod() == BacklashCompensationMethod.DirectionalCompensation) {
                            // The compensation is determines by the direction in which the axis travels. Because we assume some 
                            // slack or play, we always move a bit farther in that direction.
                            // Note, this is applied to both the extra backlashCompensatedLocation and the newLocation, in case the 
                            // methods are mixed across axes.
                            // The actual compensation is only half the absolute offset, always into the direction of travel. 
                            // The absolute offset is taken (by applying signum), so the user can switch the method back and forth from/to 
                            // OneSidedPositioning where a negative offset might make sense. 
                            double signum = Math.signum(backlashOffset.getValue());
                            Length halfBacklashOffset = backlashOffset.multiply(segment.getCoordinate(refAxis) > 0 ? signum*0.5 : signum*-0.5);
                            backlashCompensatedLocation = backlashCompensatedLocation.add(
                                    new AxesLocation(axis, halfBacklashOffset));
                            newLocation = newLocation.add(
                                    new AxesLocation(axis, halfBacklashOffset));
                            // Remember the last backlash offset we applied. This is important if we use setGlobalOffsets() or 
                            // interpret position reports later.  
                            lastDirectionalBacklashOffset = lastDirectionalBacklashOffset.put(new AxesLocation(refAxis, halfBacklashOffset));
                        }
                    }
                }
            }
        }
        if (needsExtraBacklashMove) {
            // First move goes to the extra backlashCompensatedLocation.
            Motion motionCommand = new Motion(
                    hm, 
                    currentLocation, 
                    backlashCompensatedLocation, 
                    speed,
                    optionFlags);
            // Add to the recorded motion commands. 
            motionCommands.addLast(motionCommand);

            // Second move to the actual target at backlashCompensatedSpeed. 
            motionCommand = new Motion(
                    hm, 
                    backlashCompensatedLocation, 
                    newLocation, 
                    backlashCompensatedSpeed,
                    optionFlags);
            // Add to the recorded motion commands. 
            motionCommands.addLast(motionCommand);
        }
        else {
            // Can go directly to the target.
            Motion motionCommand = new Motion(
                    hm, 
                    currentLocation, 
                    newLocation, 
                    speed,
                    optionFlags);
            // Add to the recorded motion commands. 
            motionCommands.addLast(motionCommand);
        }
        return newLocation;
    }

    /**
     * Plan and then execute the pending motion commands. 
     * 
     * @param completionType
     * @throws Exception
     */
    protected synchronized void executeMotionPlan(CompletionType completionType) throws Exception {
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
        for (Motion plannedMotion : executionPlan) {
            if (!plannedMotion.hasOption(MotionOption.Stillstand)) {
                // Put into timed plan.
                t += plannedMotion.getTime();
                motionPlan.put(t, plannedMotion);
                plannedMotion.setPlannedTime1(t);
                // Execute across drivers.
                ReferenceHeadMountable  hm = (ReferenceHeadMountable) plannedMotion.getHeadMountable();
                if (hm != null) {
                    movedHeads.add(hm.getHead());
                    executeMoveTo(machine, hm, plannedMotion);
                }
            }
        }
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
     */
    protected abstract void optimizeExecutionPlan(List<Motion> executionPlan, CompletionType completionType);

    /**
     * Sub-classes may override this in order to execute/interpolate more complex MotionProfiles with simulated jerk control, 
     * and/or curved trajectories from motion blending, etc. 
     * 
     * @param machine
     * @param hm
     * @param plannedMotion
     * @throws Exception
     */
    protected void executeMoveTo(ReferenceMachine machine, ReferenceHeadMountable hm,
            Motion plannedMotion) throws Exception {
        AxesLocation motionSegment = plannedMotion.getLocation0().motionSegmentTo(plannedMotion.getLocation1());
        // Note, this loop will be empty if the motion is empty.
        for (Driver driver : motionSegment.getAxesDrivers(machine)) {
            ((ReferenceDriver) driver).moveTo(hm, plannedMotion);
        }
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
                        if (!refAxis.coordinatesMatch(specifiedAngle, newAngle)) {
                            axesLocation = axesLocation.put(new AxesLocation(refAxis, newAngle));
                        }
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
            // Return the current motion.
            Motion motion = entry1.getValue();
            // Anchor it in real-time.
            //motion.setPlannedTime1(entry1.getKey());
            return motion;
        }
        else {
            // Nothing in the plan or machine stopped before this time, just get the current axes location.
            AxesLocation currentLocation = new AxesLocation(getMachine()); 
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
            // Tell the drivers.
            waitForDriverCompletion(hm, completionType);
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
                        ((ReferenceDriver) refAxis.getDriver()).setGlobalOffsets(getMachine(), 
                                new AxesLocation(refAxis, angleWrappedAround));
                        // This also reflects in the motion planner's coordinate.
                        refAxis.setCoordinate(refAxis.getDriverCoordinate());
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
                ((ReferenceDriver) driver).waitForCompletion((ReferenceHeadMountable) hm, completionType);
            }
            if (hm != null) {
                machine.fireMachineHeadActivity(hm.getHead());
            }
            else {
                for (Head head : machine.getHeads()) {
                    machine.fireMachineHeadActivity(head);
                }
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
}
