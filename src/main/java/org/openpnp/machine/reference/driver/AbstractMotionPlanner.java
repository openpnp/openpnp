package org.openpnp.machine.reference.driver;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.AxesLocation.MotionLimits;
import org.openpnp.model.Motion.Derivative;
import org.openpnp.model.MappedAxes;
import org.openpnp.model.Motion;
import org.openpnp.spi.Axis;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MotionPlanner;
import org.openpnp.spi.Movable.MoveToOption;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.util.NanosecondTime;
import org.simpleframework.xml.Attribute;

/**
 * The AbstractMotionPlanner provides a basic framework for sub-classing. 
 *
 */
public abstract class AbstractMotionPlanner implements MotionPlanner {

    @Attribute (required=false)
    boolean dummy;

    protected static class MotionCommand {
        final double timeRecorded;
        final HeadMountable hm;
        final MappedAxes mappedAxes;
        final AxesLocation axesLocation;
        final double speed;
        final MoveToOption [] options;

        public MotionCommand(HeadMountable hm, MappedAxes mappedAxes, AxesLocation axesLocation, double speed,
                MoveToOption[] options) {
            super();
            this.hm = hm;
            this.mappedAxes = mappedAxes;
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
    public synchronized void home(Machine machine) throws Exception {
        // Home all the drivers with their respective mapped axes (can be an empty map). 
        for (Driver driver : machine.getDrivers()) {
            MappedAxes mappedAxes = new MappedAxes(machine, driver);
            ((ReferenceDriver) driver).home((ReferenceMachine) machine, mappedAxes);
        }
        // Home all the axes (including virtual ones) to their homing coordinates.
        for (Axis axis : machine.getAxes()) {
            if (axis instanceof CoordinateAxis) {
                ((CoordinateAxis) axis).setLengthCoordinate(((CoordinateAxis) axis).getHomeCoordinate());
            }
        }
    }


    @Override
    public synchronized void resetLocation(Machine machine, AxesLocation axesLocation) throws Exception {
        // Reset all the mapped axes on the respective drivers. 
        MappedAxes mappedAxes = new MappedAxes(machine, axesLocation);
        for (Driver driver : mappedAxes.getMappedDrivers(machine)) {
            ((ReferenceDriver) driver).resetLocation((ReferenceMachine) machine, new MappedAxes(mappedAxes, driver), axesLocation);
        }
        // Reset all the axes (including virtual ones) to their new coordinates.
        for (Axis axis : axesLocation.getAxes()) {
            if (axis instanceof CoordinateAxis) {
                ((CoordinateAxis) axis).setCoordinate(axesLocation.getCoordinate(axis));
            }
        }
    }

    @Override
    public synchronized void moveTo(HeadMountable hm, MappedAxes mappedAxes, AxesLocation axesLocation, double speed, MoveToOption... options) throws Exception {
        commandSequence.add(new MotionCommand(hm, mappedAxes, axesLocation, speed, options));
        // Set all the axes (including virtual ones) to their new coordinates.
        for (Axis axis : axesLocation.getAxes()) {
            if (axis instanceof CoordinateAxis) {
                ((CoordinateAxis) axis).setCoordinate(axesLocation.getCoordinate(axis));
            }
        }
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
