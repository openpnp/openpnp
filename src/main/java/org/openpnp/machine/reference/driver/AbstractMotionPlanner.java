package org.openpnp.machine.reference.driver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.axis.ReferenceVirtualAxis;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Motion;
import org.openpnp.model.Motion.Derivative;
import org.openpnp.model.Motion.MotionOption;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.MotionPlanner;
import org.openpnp.spi.Movable.MoveToOption;
import org.openpnp.util.NanosecondTime;
import org.openpnp.util.Utils2D;
import org.simpleframework.xml.Element;

/**
 * The AbstractMotionPlanner provides a basic framework for sub-classing. 
 *
 */
public abstract class AbstractMotionPlanner implements MotionPlanner {

    @Element(required=false)
    Length precision = new Length(0.0001, LengthUnit.Millimeters); 

    private ReferenceMachine machine;

    public static class MotionCommand {
        final double timeRecorded;
        final HeadMountable headMountable;
        final AxesLocation axesLocation;
        final double speed;
        final MoveToOption [] options;

        public MotionCommand(HeadMountable headMountable, AxesLocation axesLocation, double speed,
                MoveToOption[] options) {
            super();
            this.headMountable = headMountable;
            this.timeRecorded = NanosecondTime.getRuntimeSeconds();
            this.axesLocation = axesLocation;
            this.speed = speed;
            this.options = options;
        }

        public double getTimeRecorded() {
            return timeRecorded;
        }

        public AxesLocation getAxesLocation() {
            return axesLocation;
        }

        public HeadMountable getHeadMountable() {
            return headMountable;
        }

        public double getSpeed() {
            return speed;
        }

        public MoveToOption[] getOptions() {
            return options;
        }
    }

    protected List<MotionCommand> commandSequence = new ArrayList<>();

    protected TreeMap<Double, Motion> motionPlan = new TreeMap<Double, Motion>(); 

    @Override
    public synchronized void home() throws Exception {
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
        // Offset all the mapped axes on the respective drivers. 
        for (Driver driver : axesLocation.getAxesDrivers(getMachine())) {
            ((ReferenceDriver) driver).setGlobalOffsets(getMachine(), axesLocation);
        }
        // Offset all the axes (including virtual ones) to their new coordinates.
        for (Axis axis : axesLocation.getAxes()) {
            if (axis instanceof CoordinateAxis) {
                ((CoordinateAxis) axis).setLengthCoordinate(axesLocation.getLengthCoordinate(axis));
            }
        }
    }

    @Override
    public synchronized void moveTo(HeadMountable hm, AxesLocation axesLocation, double speed, MoveToOption... options) throws Exception {
        // Handle soft limits and rotation axes limiting and wrap-around.
        axesLocation = limitAxesLocation(hm, axesLocation, false);

        // Do the internal planning etc.  
        moveToPlanning(hm, axesLocation, speed, options);

        // Set all the axes (including virtual ones) to their new coordinates.
        for (Axis axis : axesLocation.getAxes()) {
            if (axis instanceof CoordinateAxis) {
                ((CoordinateAxis) axis).setLengthCoordinate(axesLocation.getLengthCoordinate(axis));
            }
        }
    }

    @Override
    public AxesLocation limitAxesLocation(HeadMountable hm, AxesLocation axesLocation, boolean silent)
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
                            throw new Exception(String.format("Can't move %s to %s=%s, lower than soft limit %s.",
                                    hm.getName(), refAxis.getName(), coordinate, limit));
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
                            throw new Exception(String.format("Can't move %s to %s=%s, higher than soft limit %s.",
                                    hm.getName(), refAxis.getName(), coordinate, limit));
                        }
                    }
                }
            }
        }
        return axesLocation;
    }

    protected double planExecutedTime = 0;

    public void executeMotionPlan(CompletionType completionType) throws Exception {
        ReferenceMachine machine = (ReferenceMachine) Configuration.get().getMachine();
        List<Head> movedHeads = new ArrayList<>();
        for (Entry<Double, Motion> plannedMotionEntry : motionPlan.tailMap(planExecutedTime, false).entrySet()) {
            // Advance the planExecutedTime up-front, so an exception in the execution will not mean we 
            // are stuck with this motion again and again.
            planExecutedTime = plannedMotionEntry.getKey();
            Motion plannedMotion = plannedMotionEntry.getValue();
            if (!plannedMotion.hasOption(MotionOption.Stillstand)) {
                for (Driver driver : plannedMotion.getLocation().getAxesDrivers(machine)) {
                    AxesLocation previousLocation = new AxesLocation(plannedMotion.getLocation().getAxes(driver), 
                            (axis) -> (axis.getDriverLengthCoordinate()));
                    // Derive the driver's motion from the planned motion.
                    Motion driverMotion = Motion.computeWithLimits(plannedMotion.getMotionCommand(), 
                            previousLocation, plannedMotion.getLocation(), 1.0, false, true,
                            (axis, order) -> (plannedMotion.getVector(order).getCoordinate(axis)));
                    if (driverMotion != null) {
                        ReferenceHeadMountable hm = null;
                        MoveToOption [] options = null;
                        if (plannedMotion.getMotionCommand() != null) {
                            options = plannedMotion.getMotionCommand().getOptions();
                            hm = (ReferenceHeadMountable) plannedMotion.getMotionCommand().getHeadMountable();
                        }
                        ((ReferenceDriver) driver).moveTo(hm, driverMotion, options);
                        if (hm != null) {
                            movedHeads.add(hm.getHead());
                        }
                    }
                }
            }
        }
        for (Head movedHead : movedHeads) {
            machine.fireMachineHeadActivity(movedHead);
        }
    }

    protected void moveToPlanning(HeadMountable hm, AxesLocation axesLocation,
            double speed, MoveToOption... options) throws Exception {
        // Add the command
        MotionCommand motionCommand = new MotionCommand(hm, axesLocation, speed, options);
        commandSequence.add(motionCommand);
        queueMotion(motionCommand);
    }

    protected void queueMotion(MotionCommand motionCommand) throws Exception {
        // Get real-time.
        double now = NanosecondTime.getRuntimeSeconds();
        // Get the last entry.
        double lastMotionTime = Double.MIN_VALUE;
        Motion lastMotion = null;
        Map.Entry<Double, Motion> lastEntry = motionPlan.lastEntry();
        double startTime = now;
        if (lastEntry != null && lastEntry.getKey() >= planExecutedTime) {
            lastMotionTime = lastEntry.getKey();
            lastMotion = lastEntry.getValue();
            if (lastMotionTime > now) {
                // Continue after last motion.
                startTime = lastMotionTime;
            }
            else {
                // Pause between the moves.
                lastMotion = new Motion(0, null, 
                        new AxesLocation [] { lastMotion.getLocation() },
                        MotionOption.FixedWaypoint, MotionOption.CoordinatedWaypoint, MotionOption.Stillstand);
                motionPlan.put(startTime, lastMotion);
            }
        }
        else {
            // No lastMotion, create the previous waypoint from the axes. 
            AxesLocation previousLocation = new AxesLocation(Configuration.get().getMachine()); 
            lastMotion = new Motion(0, null, 
                    new AxesLocation [] { previousLocation }, 
                    MotionOption.FixedWaypoint, MotionOption.CoordinatedWaypoint, MotionOption.Stillstand);
            motionPlan.put(startTime, lastMotion);
        }
        // Note this must include all the machine axes, not just the ones included in this moveTo().
        AxesLocation lastLocation = 
                new AxesLocation(Configuration.get().getMachine())
                .put(lastMotion.getLocation());
        Motion plannedMotion = Motion.computeWithLimits(motionCommand, lastLocation, 
                motionCommand.getAxesLocation(), 
                motionCommand.getSpeed(), true, false);
        if (plannedMotion != null) {
            motionPlan.put(startTime + plannedMotion.getTime(), plannedMotion);
        }
    }

    @Override
    public synchronized Motion getMomentaryMotion(double time) {
        time = Math.min(planExecutedTime, time);
        Map.Entry<Double, Motion> entry0 = motionPlan.floorEntry(time);
        Map.Entry<Double, Motion> entry1 = motionPlan.higherEntry(time);
        if (entry0 != null && entry1 != null) {
            // We're between two way-points, interpolate linearly by time.
            double dt = entry1.getKey() - entry0.getKey();
            double ratio = (time - entry0.getKey())/dt;
            Motion interpolatedMotion = entry0.getValue().interpolate(entry1.getValue(), ratio);
            //Logger.trace("time = "+time+", ratio "+ratio+" motion="+interpolatedMotion);
            return interpolatedMotion;
        }
        else if (entry0 != null){
            // Machine stopped before this time. Just return the last location. 
            return new Motion(0.0, 
                    null, 
                    new AxesLocation [] { entry0.getValue().getVector(Derivative.Location) }, 
                    MotionOption.Stillstand);
        }
        else if (entry1 != null){
            // Planning starts after this time. Return the first known location. 
            return new Motion(0.0, 
                    null,
                    new AxesLocation [] { entry1.getValue().getVector(Derivative.Location) }, 
                    MotionOption.Stillstand);
        }
        else {
            // Nothing in the plan, just get the current axes location.
            AxesLocation currentLocation = new AxesLocation(Configuration.get().getMachine()); 
            return new Motion(0.0, 
                    null, 
                    new AxesLocation [] { currentLocation }, 
                    MotionOption.Stillstand);
        }
    }

    @Override
    public void waitForCompletion(HeadMountable hm, CompletionType completionType)
            throws Exception {
        // Rotation axes wrap-around handling.
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
        // The null motion planner is a pure proxy, so there is nothing left to do except wait for the driver(s).
        ReferenceMachine machine = (ReferenceMachine) Configuration.get().getMachine();
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

    

    @Override
    public ReferenceMachine getMachine() {
        if (machine == null) {
            machine = (ReferenceMachine) Configuration.get().getMachine();
        }
        return machine;
    }

    @Override
    public synchronized AxesLocation getMomentaryLocation(double time) {
        Motion motion = getMomentaryMotion(time);
        return motion.getVector(Derivative.Location);
    }

    @Override
    public synchronized void clearMotionOlderThan(double time) {
        while (commandSequence.size() > 0 && commandSequence.get(0).getTimeRecorded() < time) {
            commandSequence.remove(commandSequence.get(0));
        }
        while (motionPlan.firstKey() < time) {
            motionPlan.remove(motionPlan.firstKey());
        }
    }
}
