package org.openpnp.machine.reference.driver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Length;
import org.openpnp.model.Motion;
import org.openpnp.spi.Axis;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.util.Triplet;
import org.openpnp.util.TruncatedNewtonConstrainedSolver;

public class AdvancedMotionSolver extends TruncatedNewtonConstrainedSolver {

    @Override
    protected double function(double[] x, double[] g) throws Exception {
        double err = 0.0;
        for (Constraints constraints : getConstraintsList()) {
            err += constraints.function(x, g);
        }
        return err;
    }

    private double lastError;

    private List<Var> variables = new ArrayList<>();

    /**
     * Solver variables can be registered and named to simplify solver development and debugging.
     *  
     */
    public abstract class Var {
        final private int index;
        final private String name;
        final private double lowerLimit; 
        final private double upperLimit;

        protected Var(String name, double lowerLimit, double upperLimit) {
            this.index = variables.size();
            variables.add(this);
            this.name = name;
            this.lowerLimit = lowerLimit;
            this.upperLimit = upperLimit;

        }
        public abstract double get();
        protected abstract void set(double value);
        public int i() {
            return index;
        }
        public String getName() {
            return name;
        }
        public double getLowerLimit() {
            return lowerLimit;
        }
        public double getUpperLimit() {
            return upperLimit;
        }
    }
    /**
     * The Constraints of the problem can be broken up into several sub-constraints. The classes here 
     * register these. 
     *
     */
    protected abstract class Constraints {
        public abstract double function(double[] x, double[] g);
    }
    private List<Constraints> constraintsList = new ArrayList<>();
    protected void addConstraints(Constraints constraints) {
        constraintsList.add(constraints);
    }

    protected List<Constraints> getConstraintsList() {
        return constraintsList;
    }

    private List<TimeVar> timeVariables = new ArrayList<>();
    public class TimeVar extends Var { 
        final private Motion motion;
        protected TimeVar(Motion motion, String name, double lowerLimit, double upperLimit) {
            super(name, lowerLimit, upperLimit);
            this.motion = motion;
            timeVariables.add(this);
        }
        @Override
        public double get() {
            return motion.getTime();
        }
        @Override
        protected void set(double value) {
            motion.setTime(value);
        }
        public Motion getMotion() {
            return motion;
        }
    }

    private Map<Triplet<Motion, CoordinateAxis, String>, AxisVar> axisVariables = new HashMap<>();

    public class AxisVar extends Var {
        final private Motion motion;
        final private CoordinateAxis axis;
        final private int order;

        protected AxisVar(Motion motion, CoordinateAxis axis, String name, double lowerLimit, double upperLimit) throws Exception {
            super(name, lowerLimit, upperLimit);
            this.motion = motion;
            this.axis = axis;
            // The variable name gives us the derivative order. 
            switch (name.charAt(0)) {
                case 's':
                    this.order = 0;
                    break;
                case 'V':
                    this.order = 1;
                    break;
                case 'a':
                    this.order = 2;
                    break;
                case 'j':
                    this.order = 3;
                    break;
                default:
                    throw new Exception("Unsupported variable name "+name);
            }
            axisVariables.put(new Triplet<>(motion, axis, name), this);
        }

        @Override
        public double get() {
            return motion.getVector(order).getCoordinate(axis);
        }
        @Override
        protected void set(double value) {
            motion.setVector(order, motion.getVector(order).put(new AxesLocation (axis, value)));
        }
        public Motion getMotion() {
            return motion;
        }
        public CoordinateAxis getAxis() {
            return axis;
        }
        public int getOrder() {
            return order;
        }
    }
    protected AxisVar getAxisVar(Motion motion, CoordinateAxis axis, String name, double lowerLimit, double upperLimit) throws Exception {
        AxisVar axisVar = axisVariables.get(new Triplet<>(motion, axis, name));
        if (axisVar == null) {
            // Not there yet, create it.
            axisVar = new AxisVar(motion, axis, name, lowerLimit, upperLimit);
        }
        return axisVar;
    }
    protected AxisVar getAxisVar(Motion motion, CoordinateAxis axis, String name) throws Exception {
        return getAxisVar(motion, axis, name, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }
    /**
     * Control model: Constant Jerk between two points.
     * 
     * The motion is modeled as if the constant jerk model is always available on controllers.
     * That is not the case however, so it will be simulated in these cases by an integral number of 
     * stepped accelerations that result in the same average acceleration as the smooth transition.
     * Making sure the average acceleration is the same will also assert that the integrals (velocity 
     * and position) end up being the same. These assertions let us disregard the simulation entirely.  
     * 
     * Solver constraints from a Motion.
     * 
     * s1 = s0 + V0*t + 1/2*a0*t^2 + 1/6*j*t^3
     * V1 = V0 + a0*t + 1/2*j*t^2
     * a1 = a0 + j*t
     * 
     * Apply motion limits with acceleration and jerk possibly being velocity dependent (@V1).
     * 
     * -Vmax <= V1 <= Vmax       | can be implemented with bounds
     * -amax@V1 <= a <= amax@V1  
     * -jmax@V1 <= j <= jmax@V1
     * 
     * If a coordinated move is wanted: 
     * 
     * V1 = V0*euclidean(V1x)/euclidean(V0x)  | x iterating over all the axes 
     * 
     * And as a soft goal we want to be fast:
     * 
     * t = 0 
     * 
     * ______________
     * 
     * Recreate the math with Sage Math: https://www.sagemath.org/ by copying & pasting the following script. 
     * Note: Sage math is Python based so we can't indent this.
     * 
t = var ('t')
s0 = var ('s0')
V0 = var ('V0')
a0 = var ('a0')
s1 = var ('s1')
V1 = var ('V1')
a1 = var ('a1')
j = var('j')

s1t = s0 + V0*t + 1/2*a0*t^2 + 1/6*j*t^3
V1t = V0 + a0*t + 1/2*j*t^2
a1t = a0 + j*t

err = (s1-s1t)^2 + (V1-V1t)^2 + (a1 - a1t)^2  

print("g[t] += ")
diff(err, t)
print("g[s0] += ")
diff(err, s0)
print("g[V0] += ")
diff(err, V0)
print("g[a0] += ")
diff(err, a0)
print("g[s1] += ")
diff(err, s1)
print("g[V1] += ")
diff(err, V1)
print("g[a1] += ")
diff(err, a1)
print("g[j] += ")
diff(err, j)

>>>

g[t] +=
1/6*(j*t^3 + 3*a0*t^2 + 6*V0*t + 6*s0 - 6*s1)*(j*t^2 + 2*a0*t + 2*V0) + (j*t^2 + 2*a0*t + 2*V0 - 2*V1)*(j*t + a0) + 2*(j*t + a0 - a1)*j
g[s0] +=
1/3*j*t^3 + a0*t^2 + 2*V0*t + 2*s0 - 2*s1
g[V0] +=
j*t^2 + 1/3*(j*t^3 + 3*a0*t^2 + 6*V0*t + 6*s0 - 6*s1)*t + 2*a0*t + 2*V0 - 2*V1
g[a0] +=
1/6*(j*t^3 + 3*a0*t^2 + 6*V0*t + 6*s0 - 6*s1)*t^2 + (j*t^2 + 2*a0*t + 2*V0 - 2*V1)*t + 2*j*t + 2*a0 - 2*a1
g[s1] +=
-1/3*j*t^3 - a0*t^2 - 2*V0*t - 2*s0 + 2*s1
g[V1] +=
-j*t^2 - 2*a0*t - 2*V0 + 2*V1
g[a1] +=
-2*j*t - 2*a0 + 2*a1
g[j] +=
1/18*(j*t^3 + 3*a0*t^2 + 6*V0*t + 6*s0 - 6*s1)*t^3 + 1/2*(j*t^2 + 2*a0*t + 2*V0 - 2*V1)*t^2 + 2*(j*t + a0 - a1)*t

     * 
     */
    protected void elaborateMotionPath(List<Motion> motionPath, SolverStep step) throws Exception {
        if (motionPath.size() < 2) {
            return;
        }
        // TODO: are we sure we got all the same axes in all Motions?
        Set<ControllerAxis> axesIndividual = motionPath.get(0).getLocation().getControllerAxes();
        Set<CoordinateAxis> axesEuclidean = Collections.singleton(Motion.Euclidean);
        Motion prevFixed = null;
        Motion nextFixed = null;
        Motion prevMotion = null;
        Iterable<? extends CoordinateAxis> axes = null;
        double[] weight0 = null;
        double[] weight1 = null;
        Motion coordinatedMotionLimits = null;
        for (int i = 0; i < motionPath.size(); i++) {
            final Motion motion = motionPath.get(i);
            // Make sure we got the previous and next fixed Waypoint
            if (prevFixed == null) {
                if (!motion.hasOption(Motion.MotionOption.FixedWaypoint)) {
                    throw new Exception("Motion path must begin with fixed waypoint");
                }
                prevFixed = motion;
            }
            else {
                if (nextFixed == null) {
                    // Find the next fixed Waypoint 
                    for (int j = i; j < motionPath.size(); j++) {
                        final Motion motion2 = motionPath.get(j);
                        if (motion2.hasOption(Motion.MotionOption.FixedWaypoint)) {
                            nextFixed = motion2; 
                        }
                    }
                    if (nextFixed == null) {
                        // No fixed Waypoint found and not at end of path.
                        throw new Exception("Motion path must end with fixed waypoint");
                    }
                    coordinatedMotionLimits = Motion.computeWithLimits(nextFixed.getMotionCommand(), 
                            prevFixed.getLocation().multiply(0), nextFixed.getLocation().subtract(prevFixed.getLocation()), 1.0, true, false);

                    // If this is a coordinated move, the motion is not modeled on the individual axes
                    // but over the Euclidean distance along the motion unit vector.
                    // This adds the difficulty that we might transition from a coordinated move to an uncoordinated
                    // move and vice versa. The the motion variables/gradients are in this case either distributed or 
                    // accumulated with the axis weights of the unit vector.
                    if (prevFixed.hasOption(Motion.MotionOption.CoordinatedWaypoint) 
                            && nextFixed.hasOption(Motion.MotionOption.CoordinatedWaypoint)) {
                        // Both sides coordinated: just the Euclidean axis needed
                        axes = axesEuclidean;
                        // Both have simply weight 1.0
                        weight0 = new double [] { 1.0 };
                        weight1 = new double [] { 1.0 };
                    }
                    else {
                        // Uncoordinated moves involved: all the axes needed
                        axes = axesIndividual;
                        // Calculate the weights for coordinated<->uncoordinated transitions.
                        weight0 = new double [axesIndividual.size()];
                        weight1 = new double [axesIndividual.size()];
                        int indexAxis = 0;
                        for (CoordinateAxis axis : axes) {
                            // Weight is the ratio of this axis vs. the whole Euclidean metric.
                            double euclideanMetric = coordinatedMotionLimits.getLocation().getCoordinate(Motion.Euclidean);
                            double w = (euclideanMetric > 0 ? 
                                    coordinatedMotionLimits.getLocation().getCoordinate(axis)/euclideanMetric
                                    : 1.0);
                            // Set the weight if this is a transition from/to an uncoordinated motion, otherwise 1.0
                            weight0[indexAxis] = prevFixed.hasOption(Motion.MotionOption.CoordinatedWaypoint) ? w : 1.0;
                            weight1[indexAxis] = nextFixed.hasOption(Motion.MotionOption.CoordinatedWaypoint) ? w : 1.0;
                            indexAxis++;
                        }
                    }
                }

                if (coordinatedMotionLimits != null) {
                    // Move is not empty.
                    step.handleSingleMotion(motion, prevMotion, prevFixed, nextFixed, axes, weight0,
                            weight1, coordinatedMotionLimits);
                }
                // Prepare for next motion.
                if (nextFixed == motion) { 
                    // We reached the next fixed motion waypoint.  
                    prevFixed = motion;
                    // Need to re-evaluate in next loop.
                    nextFixed = null;
                }
            }
            prevMotion = motion;
        }
    }

    protected interface SolverStep {
        void handleSingleMotion(final Motion motion, Motion prevMotion, Motion prevFixed,
                Motion nextFixed, Iterable<? extends CoordinateAxis> axes, double[] weight0,
                double[] weight1, Motion coordinatedMotionLimits) throws Exception;

    }
    /**
     * Model the constraints of one motion.
     *
     */
    protected class ConstraintsModeller implements SolverStep {
        @Override
        public void handleSingleMotion(final Motion motion, Motion prevMotion, Motion prevFixed,
                Motion nextFixed, Iterable<? extends CoordinateAxis> axes, double[] weight0,
                double[] weight1, Motion coordinatedMotionLimits) throws Exception {
            // Allocate the solver variables. 
            // No time travel allowed, t must be positive.
            int _t = new TimeVar(motion, "t", 0, Double.POSITIVE_INFINITY).i();
            // Allocation for all the axes.
            int indexAxis = 0;
            for (CoordinateAxis axis : axes) {
                // Note, all these parameters are materialized so the Constraints inner class takes them
                // as final captured locals i.e. no further processing is needed in the many solver iterations.
                final CoordinateAxis axis0 = prevFixed.hasOption(Motion.MotionOption.CoordinatedWaypoint) ? 
                        Motion.Euclidean : axis;
                final CoordinateAxis axis1 = nextFixed.hasOption(Motion.MotionOption.CoordinatedWaypoint) ? 
                        Motion.Euclidean : axis;

                // Determine the motion limits.
                final double sMin;
                final double sMax;
                if (motion.hasOption(Motion.MotionOption.FixedWaypoint)) {

                    // Location is fixed.
                    sMin = sMax = motion.getLocation().getCoordinate(axis1);
                }
                else if (motion.hasOption(Motion.MotionOption.LimitToSafeZZone) 
                        && axis1.getType() == Axis.Type.Z) {
                    // Limit to safe Z Zone
                    // TODO: implement
                    sMin = -15;
                    sMax = 0;
                }
                else if (axis1 instanceof ReferenceControllerAxis) {
                    ReferenceControllerAxis refAxis = (ReferenceControllerAxis)axis1;
                    // Apply soft limits, if any.
                    if (refAxis.isSoftLimitLowEnabled()) {
                        sMin = refAxis.getSoftLimitLow()
                                .convertToUnits(AxesLocation.getUnits()).getValue();
                    }
                    else {
                        sMin = Double.NEGATIVE_INFINITY;
                    }
                    if (refAxis.isSoftLimitHighEnabled()) {
                        sMax = refAxis.getSoftLimitHigh()
                                .convertToUnits(AxesLocation.getUnits()).getValue();
                    }
                    else {
                        sMax = Double.POSITIVE_INFINITY;
                    }
                }
                else {
                    sMin = Double.NEGATIVE_INFINITY;
                    sMax = Double.POSITIVE_INFINITY;
                }
                final double speed;
                if (motion.getMotionCommand() != null) {
                    speed = motion.getMotionCommand().getSpeed();
                }
                else {
                    speed = 1.0;
                }
                final double VMax;
                final double aMax;
                final double jMax;
                if (axis1 instanceof ControllerAxis) {
                    // Uncoordinated motion. Take the limits from the axis.
                    if (motion.hasOption(Motion.MotionOption.Stillstand)) {
                        // Velocity must be zero
                        VMax = 0;
                    }
                    else {
                        VMax = ((ControllerAxis) axis1).getMotionLimit(Motion.Derivative.Velocity.ordinal())
                                *speed;
                    }

                    aMax = ((ControllerAxis) axis1).getMotionLimit(Motion.Derivative.Acceleration.ordinal())
                            *Math.pow(speed, 2);
                    jMax = ((ControllerAxis) axis1).getMotionLimit(Motion.Derivative.Jerk.ordinal())
                            *Math.pow(speed, 3);
                }
                else {
                    // Coordinated motion. Take the limits from the euclidean motion.
                    if (motion.hasOption(Motion.MotionOption.Stillstand)) {
                        // Velocity must be zero
                        VMax = 0;
                    }
                    else {
                        VMax = coordinatedMotionLimits.getFeedRatePerSecond(AxesLocation.getUnits())
                                *speed;
                    }
                    aMax = coordinatedMotionLimits.getAccelerationPerSecond2(AxesLocation.getUnits())
                            *Math.pow(speed, 2);
                    jMax = coordinatedMotionLimits.getJerkPerSecond3(AxesLocation.getUnits())
                            *Math.pow(speed, 3);
                }

                // Get motion axis variables. 
                // From...
                final int _s0 = getAxisVar(prevMotion, axis0, "s").i();
                final int _V0 = getAxisVar(prevMotion, axis0, "V").i();
                final int _a0 = getAxisVar(prevMotion, axis0, "a").i();
                final double w0 = weight0[indexAxis];
                // ... to. Apply limits.
                final int _s1 = getAxisVar(motion, axis1, "s", sMin, sMax).i();
                final int _V1 = getAxisVar(motion, axis1, "V", -VMax, VMax).i();
                final int _a1 = getAxisVar(motion, axis1, "a", -aMax, aMax).i();
                final int _j = getAxisVar(motion, axis1, "j", -jMax, jMax).i();
                final double w1 = weight1[indexAxis];

                // Formulate the per-motion-per-axis constraints.
                addConstraints(new Constraints() {
                    @Override
                    public 
                    double function(double [] x, double [] g) {
                        // Read the variables.
                        // Axis from ...
                        double s0 = x[_s0]*w0;
                        double V0 = x[_V0]*w0;
                        double a0 = x[_a0]*w0;
                        // .. to.
                        double s1 = x[_s1]*w1;
                        double V1 = x[_V1]*w1;
                        double a1 = x[_a1]*w1;
                        double j = x[_j]*w1;
                        // Powers of time t.
                        double t = x[_t];
                        double t2 = t*t;
                        double t3 = t2*t;
                        // Calculate the constraints.
                        double s1t = s0 + V0*t + 1./2*a0*t2 + 1./6*j*t3;
                        double V1t = V0 + a0*t + 1./2*j*t2;
                        double a1t = a0 + j*t;
                        // Error against the constraints.
                        double error = Math.pow(s1-s1t, 2) + Math.pow(V1-V1t, 2) + Math.pow(a1 - a1t, 2);
                        // The gradients. 
                        // TODO: handle more common subexpressions and cascade multiplications with powers of t.  
                        g[_t] += w1*(1./6*(j*t3 + 3*a0*t2 + 6*V0*t + 6*s0 - 6*s1)*(j*t2 + 2*a0*t + 2*V0) + (j*t2 + 2*a0*t + 2*V0 - 2*V1)*(j*t + a0) + 2*(j*t + a0 - a1)*j);
                        double gs = 1./3*j*t3 + a0*t2 + 2*V0*t + 2*s0 - 2*s1;
                        g[_s0] += w0*gs;
                        double gV = j*t2 + 2*a0*t + 2*V0 - 2*V1;
                        g[_V0] += w0*(gV + 1./3*(j*t3 + 3*a0*t2 + 6*V0*t + 6*s0 - 6*s1)*t);
                        double ga =  2*j*t + 2*a0 - 2*a1;   
                        g[_a0] += w0*(ga + 1./6*(j*t3 + 3*a0*t2 + 6*V0*t + 6*s0 - 6*s1)*t2 + (j*t2 + 2*a0*t + 2*V0 - 2*V1)*t);
                        g[_s1] += w1*(-gs);
                        g[_V1] += w1*(-gV);
                        g[_a1] += w1*(-ga);
                        g[_j] += w1*(1./18*(j*t3 + 3*a0*t2 + 6*V0*t + 6*s0 - 6*s1)*t3 + 1./2*(j*t2 + 2*a0*t + 2*V0 - 2*V1)*t2 + 2*(j*t + a0 - a1)*t);

                        return error;
                    }
                });

                indexAxis++;
            }
        }
    }
    /**
     * Convert solved coordinated moves into n-dimensional axis motion.  
     *
     */
    protected class CoordinatedMoveMaterializer implements SolverStep {
        @Override
        public 
        void handleSingleMotion(final Motion motion, Motion prevMotion, Motion prevFixed,
                Motion nextFixed, Iterable<? extends CoordinateAxis> axes, double[] weight0,
                double[] weight1, Motion coordinatedMotionLimits) throws Exception {
            if (motion.hasOption(Motion.MotionOption.CoordinatedWaypoint)) {
                // Only the Euclidean distance along the coordinated trajectory has been planned in 1D.
                // Now materialize the axes vector components in N dimensions. 
                double coordinatedOverall = coordinatedMotionLimits.getLocation().getCoordinate(Motion.Euclidean);
                for (int order = 0; order <= Motion.Derivative.Jerk.ordinal(); order++) {
                    AxesLocation derivativeVector = motion.getVector(order);
                    // Get the Euclidean distance coordinate.
                    double coordinate = derivativeVector.getCoordinate(Motion.Euclidean);
                    // Compose the new dimensional vector according to the overall vector.   
                    AxesLocation newDerivativeVector = new AxesLocation(motion.getLocation().getControllerAxes(),
                            (axis) -> new Length(coordinate
                                    *coordinatedMotionLimits.getLocation().getCoordinate(axis)/coordinatedOverall, 
                                    AxesLocation.getUnits()));
                    // Replace it in the motion, add the Euclidean back.
                    motion.setVector(order, newDerivativeVector.put(new AxesLocation(Motion.Euclidean, coordinate)));
                }
            }
        }
    }

    public void solve(int maxfneval, double tolerance) throws Exception {
        // Allocate the variables.
        double [] x = new double [variables.size()];
        // Lower and upper bounds
        double [] lower = new double [variables.size()];
        double [] upper = new double [variables.size()];
        Arrays.fill(lower, Double.NEGATIVE_INFINITY);
        Arrays.fill(upper, Double.POSITIVE_INFINITY);
        // Set up the time variables.
        for (TimeVar var : timeVariables) {
            // Read the variable.
            var.get();
            // No time travel, please.
            lower[var.i()] = 0.0;
        }
        // Set up the axis variables
        for (AxisVar var : axisVariables.values()) {
            // Read the variable 
            var.get();
        }
        // Solve it.
        lastError = solve(x, lower, upper, TNC_MSG_NONE, maxfneval, tolerance, tolerance*tolerance, tolerance*tolerance);
        // Write back the variables to the motion.
        for (Var var : variables) {
            var.set(x[var.i()]);
        }
        // Transform coordinated motion into individual axis motion
    }
}
