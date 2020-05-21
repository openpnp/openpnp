package org.openpnp.machine.reference.driver;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.openpnp.ConfigurationListener;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Motion;
import org.openpnp.model.Motion.Derivative;
import org.openpnp.spi.Axis;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MotionPlanner;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.openpnp.spi.Movable.MoveToOption;
import org.openpnp.spi.base.AbstractControllerAxis;
import org.openpnp.util.NanosecondTime;
import org.openpnp.util.Utils2D;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;

/**
 * The AbstractMotionPlanner provides a basic framework for sub-classing. 
 *
 */
public abstract class AbstractMotionPlanner implements MotionPlanner {

    @Attribute (required=false)
    boolean dummy;

    private ReferenceMachine machine;

    protected static class MotionCommand {
        final double timeRecorded;
        final HeadMountable hm;
        final AxesLocation axesLocation;
        final double speed;
        final MoveToOption [] options;

        public MotionCommand(HeadMountable hm, AxesLocation axesLocation, double speed,
                MoveToOption[] options) {
            super();
            this.hm = hm;
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

    protected void moveToPlanning(HeadMountable hm, AxesLocation axesLocation,
            double speed, MoveToOption... options) throws Exception {
        // Add the command
        commandSequence.add(new MotionCommand(hm, axesLocation, speed, options));
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
                    if (!axis.coordinatesMatch(anglePresent, angleWrappedAround)) {
                        ((ReferenceDriver) refAxis.getDriver()).setGlobalOffsets(getMachine(), 
                                new AxesLocation(refAxis, angleWrappedAround));
                        // This also reflects in the motion planner's coordinate.
                        refAxis.setCoordinate(refAxis.getDriverCoordinate());
                    }
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
