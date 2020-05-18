package org.openpnp.machine.reference.driver;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.openpnp.model.AxesLocation;
import org.openpnp.model.AxesLocation.MotionLimits;
import org.openpnp.model.Motion.Derivative;
import org.openpnp.model.MappedAxes;
import org.openpnp.model.Motion;
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
        final NanosecondTime timeRecorded;
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
            this.timeRecorded = NanosecondTime.get();
            this.axesLocation = axesLocation;
            this.speed = speed;
            this.options = options;
        }

        public NanosecondTime getTimeRecorded() {
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
    public synchronized void moveTo(HeadMountable hm, MappedAxes mappedAxes, AxesLocation axesLocation, double speed, MoveToOption... options) throws Exception {
        commandSequence.add(new MotionCommand(hm, mappedAxes, axesLocation, speed, options));
    }

    @Override
    public synchronized AxesLocation getMomentaryLocation(double time) {
        Motion motion = getMomentaryMotion(time);
        return motion.getVector(Derivative.Location);
    }

    @Override
    public synchronized void clearMotionOlderThan(double time) {
        while (motionPlan.firstKey() < time) {
            motionPlan.remove(motionPlan.firstKey());
        }
    }
}
