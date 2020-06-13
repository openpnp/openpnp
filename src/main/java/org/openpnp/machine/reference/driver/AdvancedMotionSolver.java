package org.openpnp.machine.reference.driver;

import java.util.ArrayList;
import java.util.Arrays;
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
        // TODO Auto-generated method stub
        return 0;
    }

//    @Override
//    protected double function(double[] x, double[] g) throws Exception {
//        // Write back the variables to the motion.
//        for (Var var : variables) {
//            var.set(x[var.i()]);
//        }
//        outputMotionPath("", motionPath);
//
//        double err = 0.0;
//        Arrays.fill(g, 0.0);
//        for (Constraints constraints : getConstraintsList()) {
//            err += constraints.function(x, g);
//        }
//        log("eval "+getFunctionEvalCount()+" error "+err);
//        return err;
//    }
//
//    @Override
//    protected void log(String message) {
//        System.out.println(message);
//    }
//
//    private double lastError;
//
//    private List<Var> variables = new ArrayList<>();
//
//    /**
//     * Solver variables can be registered and named to simplify solver development and debugging.
//     *  
//     */
//    public abstract class Var {
//        final private int index;
//        final private String name;
//        final private double lowerLimit; 
//        final private double upperLimit;
//
//        protected Var(String name, double lowerLimit, double upperLimit) {
//            this.index = variables.size();
//            variables.add(this);
//            this.name = name;
//            this.lowerLimit = lowerLimit;
//            this.upperLimit = upperLimit;
//
//        }
//        public abstract double get();
//        protected abstract void set(double value);
//        public int i() {
//            return index;
//        }
//        public String getName() {
//            return name;
//        }
//        public double getLowerLimit() {
//            return lowerLimit;
//        }
//        public double getUpperLimit() {
//            return upperLimit;
//        }
//    }
//    /**
//     * The Constraints of the problem can be broken up into several sub-constraints. The classes here 
//     * register these. 
//     *
//     */
//    protected abstract class Constraints {
//        public abstract double function(double[] x, double[] g);
//    }
//    private List<Constraints> constraintsList = new ArrayList<>();
//    protected void addConstraints(Constraints constraints) {
//        constraintsList.add(constraints);
//    }
//
//    protected List<Constraints> getConstraintsList() {
//        return constraintsList;
//    }
//
//    private List<TimeVar> timeVariables = new ArrayList<>();
//    public class TimeVar extends Var { 
//        final private Motion motion;
//        protected TimeVar(Motion motion, String name, double lowerLimit, double upperLimit) {
//            super(name, lowerLimit, upperLimit);
//            this.motion = motion;
//            timeVariables.add(this);
//        }
//        @Override
//        public double get() {
//            return motion.getTime();
//        }
//        @Override
//        protected void set(double value) {
//            motion.setTime(value);
//        }
//        public Motion getMotion() {
//            return motion;
//        }
//    }
//
//    private Map<Triplet<Motion, CoordinateAxis, String>, AxisVar> axisVariables = new HashMap<>();
//
//    private List<Motion> motionPath;
//
//    public static int derivateOrderFromName(String name) throws Exception {
//        // The variable name gives us the derivative order. 
//        switch (name.charAt(0)) {
//            case 's':
//                return 0;
//            case 'V':
//                return 1;
//            case 'a':
//                return 2;
//            case 'j':
//                return 3;
//            default:
//                throw new Exception("Unsupported variable name "+name);
//        }
//    }
//
//    public class AxisVar extends Var {
//        final private Motion motion;
//        final private CoordinateAxis axis;
//        final private int order;
//
//        protected AxisVar(Motion motion, CoordinateAxis axis, String name, double lowerLimit, double upperLimit) throws Exception {
//            super(name, lowerLimit, upperLimit);
//            this.motion = motion;
//            this.axis = axis;
//            this.order = derivateOrderFromName(name);
//            axisVariables.put(new Triplet<>(motion, axis, name), this);
//        }
//
//
//
//        @Override
//        public double get() {
//            return motion.getVector(order).getCoordinate(axis);
//        }
//        @Override
//        protected void set(double value) {
//            motion.setVector(order, new AxesLocation (motion.getVector(order).getControllerAxes(),
//                    (a) -> (a == axis) ? new Length(value, AxesLocation.getUnits()) 
//                            : new Length(motion.getVector(order).getCoordinate(a), AxesLocation.getUnits())));
//        }
//        public Motion getMotion() {
//            return motion;
//        }
//        public CoordinateAxis getAxis() {
//            return axis;
//        }
//        public int getOrder() {
//            return order;
//        }
//    }
//    protected AxisVar getAxisVar(Motion motion, CoordinateAxis axis, String name, double lowerLimit, double upperLimit) throws Exception {
//        AxisVar axisVar = axisVariables.get(new Triplet<>(motion, axis, name));
//        if (axisVar == null) {
//            // Not there yet, create it.
//            axisVar = new AxisVar(motion, axis, name, lowerLimit, upperLimit);
//        }
//        return axisVar;
//    }
//    protected AxisVar getAxisVar(Motion motion, CoordinateAxis axis, String name) throws Exception {
//        // This method is only called for previous move variables. So they are either already defined or they 
//        // should be fixed at still-stand values.  
//        int order = derivateOrderFromName(name);
//        double lower, upper;
//        if (motion.hasOption(Motion.MotionOption.FixedWaypoint) && order == 0) {
//            lower = upper = motion.getVector(order).getCoordinate(axis);
//        }
//        else if (motion.hasOption(Motion.MotionOption.Stillstand) && order <= 2) {
//            lower = upper = 0.0;
//        }
//        else {
//            lower = Double.NEGATIVE_INFINITY;
//            upper = Double.POSITIVE_INFINITY;
//        }
//        return getAxisVar(motion, axis, name, lower, upper);
//    }
//
//    public void elaborateMotionPath(List<Motion> motionPath, MotionElaboration elaboration) throws Exception {
//        this.motionPath = motionPath;
//        if (motionPath.size() < 2) {
//            return;
//        }
//        // TODO: are we sure we got all the same axes in all Motions?
//        Set<ControllerAxis> axes = motionPath.get(0).getLocation1().getControllerAxes();
//        Motion prevFixed = null;
//        Motion nextFixed = null;
//        Motion prevMotion = null;
//        Motion coordinatedMotionLimits = null;
//        ControllerAxis coordinatedLeadAxis = null;
//        double coordinatedLeadAxisWeight = 0.0;
//        boolean skipEmptyMotion = true;
//        for (int i = 0; i < motionPath.size(); i++) {
//            final Motion motion = motionPath.get(i);
//            // Make sure we got the previous and next fixed Waypoint
//            if (prevFixed == null) {
//                if (!motion.hasOption(Motion.MotionOption.FixedWaypoint)) {
//                    throw new Exception("Motion path must begin with fixed waypoint");
//                }
//                prevFixed = motion;
//            }
//            else {
//                if (nextFixed == null) {
//                    // Find the next fixed Waypoint 
//                    for (int j = i; j < motionPath.size(); j++) {
//                        final Motion motion2 = motionPath.get(j);
//                        if (motion2.hasOption(Motion.MotionOption.FixedWaypoint)) {
//                            nextFixed = motion2; 
//                        }
//                    }
//                    if (nextFixed == null) {
//                        // No fixed Waypoint found and not at end of path.
//                        throw new Exception("Motion path must end with fixed waypoint");
//                    }
//
//                    skipEmptyMotion = false;
//                    // If this is a coordinated move, the motion must progress along the coordinated 
//                    // motion vector i.e. all the derivatives (velocity, acceleration, jerk) must act together
//                    // and proportional to the vector components.
//                    if (nextFixed.hasOption(Motion.MotionOption.CoordinatedMotion)) {
//                        coordinatedMotionLimits = new Motion(nextFixed.getMotionCommand(), 
//                                prevFixed.getLocation1(), nextFixed.getLocation1(), 0);
//                        coordinatedLeadAxis = null;
//                        coordinatedLeadAxisWeight = 0.0;
//                        if (coordinatedMotionLimits != null) {
//                            AxesLocation vector = coordinatedMotionLimits.getVector(Motion.Derivative.Velocity);
//                            for (ControllerAxis axis : axes) {
//                                double weight = vector.getCoordinate(axis)/vector.getCoordinate(Motion.EuclideanAxis);
//                                if (weight > coordinatedLeadAxisWeight) {
//                                    coordinatedLeadAxis = axis;
//                                    coordinatedLeadAxisWeight = weight;
//                                }
//                            }
//                        }
//                        else {
//                            skipEmptyMotion = true;
//                        }
//                    }
//                    else {
//                        coordinatedMotionLimits = null;
//                        coordinatedLeadAxis = null;
//                    }
//                }
//
//                if (!skipEmptyMotion) {
//                    // Do whatever it is you do in this elaboration.
//                    elaboration.handleSingleMotion(motion, prevMotion, prevFixed, nextFixed, axes, 
//                            coordinatedMotionLimits, coordinatedLeadAxis, coordinatedLeadAxisWeight);
//                }
//
//                // Prepare for next motion.
//                if (nextFixed == motion) { 
//                    // We reached the next fixed motion waypoint.  
//                    prevFixed = motion;
//                    // Need to re-evaluate in next loop.
//                    nextFixed = null;
//                }
//            }
//            prevMotion = motion;
//        }
//    }
//
//    protected interface MotionElaboration {
//        void handleSingleMotion(final Motion motion, Motion prevMotion, Motion prevFixed,
//                Motion nextFixed, Iterable<? extends ControllerAxis> axes, 
//                Motion coordinatedMotionLimits, ControllerAxis coordinatedLeadAxis, double coordinatedLeadAxisWeight) throws Exception;
//
//    }
//    /**
//     * Model the constraints of one motion.
//     *
//     * Control model: Constant Jerk between two points.
//     * 
//     * The motion is modeled as if the constant jerk model is always available on controllers.
//     * That is not the case however, so it will be simulated in these cases by an integral number of 
//     * stepped accelerations that result in the same average acceleration as the smooth transition.
//     * Making sure the average acceleration is the same will also assert that the integrals (velocity 
//     * and position) end up being the same. These assertions let us disregard the simulation entirely.  
//     * 
//     * Solver constraints from a Motion.
//     * 
//     * s1 = s0 + V0*t + 1/2*a0*t^2 + 1/6*j*t^3
//     * V1 = V0 + a0*t + 1/2*j*t^2
//     * a1 = a0 + j*t
//     * 
//     * Apply motion limits with acceleration and jerk possibly being velocity dependent (@V1).
//     * 
//     * -Vmax <= V1 <= Vmax       | can be implemented with bounds
//     * -amax@V1 <= a <= amax@V1  
//     * -jmax@V1 <= j <= jmax@V1
//     * 
//     * If a coordinated move is wanted: 
//     * 
//     * V1 = V0*euclidean(V1x)/euclidean(V0x)  | x iterating over all the axes 
//     * 
//     * And as a soft goal we want to be fast:
//     * 
//     * t = 0 
//     * 
//     * ______________
//     * 
//     * Recreate the math with Sage Math: https://www.sagemath.org/ by copying & pasting the following script. 
//     * Note: Sage math is Python based so we can't indent this.
//     * 
//
//t = var ('t')
//s0 = var ('s0')
//V0 = var ('V0')
//a0 = var ('a0')
//j = var('j')
//
//t0 = 1/2*t
//t1 = -1/2*t
//
//a0eq = a0 + j*t0
//V0eq = V0 + a0*t0 + 1/2*j*t0^2
//s0eq = s0 + V0*t0 + 1/2*a0*t0^2 + 1/6*j*t0^3
//
//a1eq = a1 + j*t1
//V1eq = V1 + a1*t1 + 1/2*j*t1^2
//s1eq = s1 + V1*t1 + 1/2*a1*t1^2 + 1/6*j*t1^3
//
//print("a0eq = ")
//a0eq
//print("V0eq = ")
//V0eq
//print("s0eq = ")
//s0eq
//print("a1eq = ")
//a1eq
//print("V1eq = ")
//V1eq
//print("s1eq = ")
//s1eq
//
//err = (s0eq-s1eq)^2 + ((V0eq-V1eq)*t)^2 + ((a0eq-a1eq)*t^2)^2  
//
//print("g[_t] += ")
//diff(err, t)
//print("g[_s0] += ")
//diff(err, s0)
//print("g[_V0] += ")
//diff(err, V0)
//print("g[_a0] += ")
//diff(err, a0)
//print("g[_s1] += ")
//diff(err, s1)
//print("g[_V1] += ")
//diff(err, V1)
//print("g[_a1] += ")
//diff(err, a1)
//print("g[_j] += ")
//diff(err, j)
//
//# Coordinated motion constraints
//
//V1Lead = var('V1Lead')
//a1Lead = var('a1Lead')
//jLead = var('jLead')
//factor=var('factor')
//err=(V1-V1Lead*factor)^2 + (a1-a1Lead*factor)^2 + (j-jLead*factor)^2
//
//print("g[_V1] += ")
//diff(err, V1)
//print("g[_a1] += ")
//diff(err, a1)
//print("g[_j] += ")
//diff(err, j)
//print("g[_V1Lead] += ")
//diff(err, V1Lead)
//print("g[_a1Lead] += ")
//diff(err, a1Lead)
//print("g[_jLead] += ")
//diff(err, jLead)
//
//>>>
//
//a0eq =
//1/2*j*t + a0
//V0eq =
//1/8*j*t^2 + 1/2*a0*t + V0
//s0eq =
//1/48*j*t^3 + 1/8*a0*t^2 + 1/2*V0*t + s0
//a1eq =
//-1/2*j*t + a1
//V1eq =
//1/8*j*t^2 - 1/2*a1*t + V1
//s1eq =
//-1/48*j*t^3 + 1/8*a1*t^2 - 1/2*V1*t + s1
//g[_t] +=
//2*(j*t + a0 - a1)*j*t^4 + 4*(j*t + a0 - a1)^2*t^3 + 1/2*(a0*t + a1*t + 2*V0 - 2*V1)*(a0 + a1)*t^2 + 1/2*(a0*t + a1*t + 2*V0 - 2*V1)^2*t + 1/96*(j*t^3 + 3*a0*t^2 - 3*a1*t^2 + 12*V0*t + 12*V1*t + 24*s0 - 24*s1)*(j*t^2 + 2*a0*t - 2*a1*t + 4*V0 + 4*V1)
//g[_s0] +=
//1/12*j*t^3 + 1/4*a0*t^2 - 1/4*a1*t^2 + V0*t + V1*t + 2*s0 - 2*s1
//g[_V0] +=
//(a0*t + a1*t + 2*V0 - 2*V1)*t^2 + 1/24*(j*t^3 + 3*a0*t^2 - 3*a1*t^2 + 12*V0*t + 12*V1*t + 24*s0 - 24*s1)*t
//g[_a0] +=
//2*(j*t + a0 - a1)*t^4 + 1/2*(a0*t + a1*t + 2*V0 - 2*V1)*t^3 + 1/96*(j*t^3 + 3*a0*t^2 - 3*a1*t^2 + 12*V0*t + 12*V1*t + 24*s0 - 24*s1)*t^2
//g[_s1] +=
//-1/12*j*t^3 - 1/4*a0*t^2 + 1/4*a1*t^2 - V0*t - V1*t - 2*s0 + 2*s1
//g[_V1] +=
//-(a0*t + a1*t + 2*V0 - 2*V1)*t^2 + 1/24*(j*t^3 + 3*a0*t^2 - 3*a1*t^2 + 12*V0*t + 12*V1*t + 24*s0 - 24*s1)*t
//g[_a1] +=
//-2*(j*t + a0 - a1)*t^4 + 1/2*(a0*t + a1*t + 2*V0 - 2*V1)*t^3 - 1/96*(j*t^3 + 3*a0*t^2 - 3*a1*t^2 + 12*V0*t + 12*V1*t + 24*s0 - 24*s1)*t^2
//g[_j] +=
//2*(j*t + a0 - a1)*t^5 + 1/288*(j*t^3 + 3*a0*t^2 - 3*a1*t^2 + 12*V0*t + 12*V1*t + 24*s0 - 24*s1)*t^3
//
//
//
//g[_V1] +=
//-2*V1Lead*factor + 2*V1
//g[_a1] +=
//-2*a1Lead*factor + 2*a1
//g[_j] +=
//-2*factor*jLead + 2*j
//g[_V1Lead] +=
//2*(V1Lead*factor - V1)*factor
//g[_a1Lead] +=
//2*(a1Lead*factor - a1)*factor
//g[_jLead] +=
//2*(factor*jLead - j)*factor
//
//
//     * 
//     */
//    public class ConstraintsModeller implements MotionElaboration {
//        @Override
//        public void handleSingleMotion(final Motion motion, Motion prevMotion, Motion prevFixed,
//                Motion nextFixed, Iterable<? extends ControllerAxis> axes, 
//                Motion coordinatedMotionLimits, ControllerAxis coordinatedLeadAxis, double coordinatedLeadAxisWeight) throws Exception {
//            AxesLocation coordinatedVector = coordinatedMotionLimits.getVector(Motion.Derivative.Velocity);
//            // Allocate the solver variables. 
//
//            final int v1Lead_;
//            final int a1Lead_;
//            final int jLead_;
//            if (coordinatedLeadAxis != null) {
//                v1Lead_ = getAxisVar(motion, coordinatedLeadAxis, "V").i();
//                a1Lead_ = getAxisVar(motion, coordinatedLeadAxis, "a").i();
//                jLead_ = getAxisVar(motion, coordinatedLeadAxis, "j").i();
//            } 
//            else {
//                v1Lead_ = -1;
//                a1Lead_ = -1;
//                jLead_ = -1;
//            }
//
//            // No time travel allowed, t must be positive.
//            int t_ = new TimeVar(motion, "t", 0, Double.POSITIVE_INFINITY).i();
//            // Allocation for all the axes.
//            for (ControllerAxis axis : axes) {
//                // Note, all these parameters are materialized so the Constraints inner class takes them
//                // as final captured locals i.e. no further processing is needed in the many solver iterations.
//                // Determine the motion limits.
//                final double sMin;
//                final double sMax;
//                final double vMax;
//                final double aMax;
//                final double jMax;
//                if (motion.hasOption(Motion.MotionOption.FixedWaypoint)) {
//                    // Location is fixed.
//                    sMin = sMax = motion.getLocation1().getCoordinate(axis);
//                }
//                else if (motion.hasOption(Motion.MotionOption.LimitToSafeZZone) 
//                        && axis.getType() == Axis.Type.Z) {
//                    // Limit to safe Z Zone
//                    // TODO: implement
//                    sMin = -15;
//                    sMax = 0;
//                }
//                else if (axis instanceof ReferenceControllerAxis) {
//                    ReferenceControllerAxis refAxis = (ReferenceControllerAxis)axis;
//                    // Apply soft limits, if any.
//                    if (refAxis.isSoftLimitLowEnabled()) {
//                        sMin = refAxis.getSoftLimitLow()
//                                .convertToUnits(AxesLocation.getUnits()).getValue();
//                    }
//                    else {
//                        sMin = Double.NEGATIVE_INFINITY;
//                    }
//                    if (refAxis.isSoftLimitHighEnabled()) {
//                        sMax = refAxis.getSoftLimitHigh()
//                                .convertToUnits(AxesLocation.getUnits()).getValue();
//                    }
//                    else {
//                        sMax = Double.POSITIVE_INFINITY;
//                    }
//                }
//                else {
//                    sMin = Double.NEGATIVE_INFINITY;
//                    sMax = Double.POSITIVE_INFINITY;
//                }
//                final double speed;
//                if (motion.getMotionCommand() != null) {
//                    speed = motion.getMotionCommand().getSpeed();
//                }
//                else {
//                    speed = 1.0;
//                }
//                if (motion.hasOption(Motion.MotionOption.Stillstand)) {
//                    // Motion must be zero
//                    vMax = 0;
//                    aMax = 0;
//                }
//                else {
//                    // Take the limits from the axis.
//                    vMax = ((ControllerAxis) axis).getMotionLimit(Motion.Derivative.Velocity.ordinal())
//                            *speed;
//                    aMax = ((ControllerAxis) axis).getMotionLimit(Motion.Derivative.Acceleration.ordinal())
//                            *Math.pow(speed, 2);
//                }
//                jMax = ((ControllerAxis) axis).getMotionLimit(Motion.Derivative.Jerk.ordinal())
//                        *Math.pow(speed, 3);
//
//                // Get motion axis variables. 
//                // From...
//                final int s0_ = getAxisVar(prevMotion, axis, "s").i();
//                final int v0_ = getAxisVar(prevMotion, axis, "V").i();
//                final int a0_ = getAxisVar(prevMotion, axis, "a").i();
//                // ... to. Apply limits.
//                final int s1_ = getAxisVar(motion, axis, "s", sMin, sMax).i();
//                final int v1_ = getAxisVar(motion, axis, "V", -vMax, vMax).i();
//                final int a1_ = getAxisVar(motion, axis, "a", -aMax, aMax).i();
//                final int j_ = getAxisVar(motion, axis, "j", -jMax, jMax).i();
//
//                final double leadFactor;
//                if (coordinatedLeadAxis != null) {
//                    leadFactor = coordinatedVector.getCoordinate(axis)/coordinatedVector.getCoordinate(Motion.EuclideanAxis)
//                            /coordinatedLeadAxisWeight;
//                }
//                else {
//                    leadFactor = 0.0;
//                }
//                final double softError =  1/(1+Math.sqrt(getFunctionEvalCount()));
//                final int segmentPhase = 0;//TODO: motion.getSegmentPhase();
//                final double signumPhase = Math.signum(leadFactor);
//
//                // Formulate the per-motion-per-axis constraints.
//                addConstraints(new Constraints() {
//                    @Override
//                    public 
//                    double function(double [] x, double [] g) {
//                        // Read the variables.
//                        // Axis from ...
//                        double s0 = x[s0_];
//                        double v0 = x[v0_];
//                        double a0 = x[a0_];
//                        // .. to.
//                        double s1 = x[s1_];
//                        double v1 = x[v1_];
//                        double a1 = x[a1_];
//                        double j = x[j_];
//                        // Powers of time t.
//                        double t = x[t_];
//                        double t2 = t*t;
//
//                        double teq = t;
//                        double seq = s1;
//                        double veq = v1;
//                        double aeq = a1;
//                        double jeq = j;
//                        int hardness = 0;
//
//                        switch (segmentPhase) {
//                            case 1:
//                                // Jerk phase to accelerate.
//                                jeq = signumPhase*jMax;
//                                teq = (aMax - signumPhase*a0)/jMax;
//                                hardness = 5;
//                                break;
//                            case 2:
//                                // Constant acceleration phase.
//                                jeq = 0;
//                                aeq = signumPhase*aMax;
//                                // Jerk time to 0 acceleration.
//                                double t3 = signumPhase*aMax/jMax;
//                                double v3 = signumPhase*(vMax - 1./2*jMax*Math.pow(t3,2));
//                                teq = (v3 - v0)/aMax;
//                                hardness = 4;
//                                break;
//                            case 3:
//                                // Negative jerk phase to constant velocity.
//                                jeq = -signumPhase*jMax;
//                                teq = signumPhase*a0/jMax;
//                                hardness = 5;
//                                break;
//                            case 4:
//                                // Constant velocity phase.
//                                jeq = 0;
//                                aeq = 0;
//                                hardness = 3;
//                                break;
//                            case 5:
//                                // Negative jerk phase to decelerate.
//                                jeq = -signumPhase*jMax;
//                                teq = -signumPhase*a1/jMax;
//                                hardness = 5;
//                                break;
//                            case 6:
//                                // Constant deceleration phase.
//                                jeq = 0;
//                                aeq = -signumPhase*aMax;
//                                // Jerk time to 0 acceleration.
//                                double t5 = signumPhase*aMax/jMax;
//                                double v5 = signumPhase*(vMax - 1./2*jMax*Math.pow(t5,2));
//                                teq = (v5 - v1)/aMax;
//                                hardness = 4;
//                                break;
//                            case 7:
//                                // Jerk phase to target acceleration.
//                                jeq = signumPhase*jMax;
//                                teq = -signumPhase*a0/jMax;
//                                hardness = 5;
//                                break;
//                        }
//
//                        if (t < 0) {
//                            hardness = 7;
//                            
//                        }
//                        double error = 0.0;
//                        // Coordinated moves.
//                        if (coordinatedLeadAxis != null
//                                && coordinatedLeadAxis != axis) {
//                            double v1Lead = x[v1Lead_];
//                            double a1Lead = x[a1Lead_];
//                            double jLead = x[jLead_];
//
//                            error += Math.pow(v1-v1Lead*leadFactor, 2) 
//                                    + Math.pow(a1-a1Lead*leadFactor, 2) 
//                                    + Math.pow(j-jLead*leadFactor, 2);
//
//                            g[v1_] += -2*v1Lead*leadFactor + 2*v1;
//                            g[a1_] += -2*a1Lead*leadFactor + 2*a1;
//                            g[j_] +=  -2*leadFactor*jLead + 2*j;
//                            g[v1Lead_] += 2*(v1Lead*leadFactor - v1)*leadFactor;
//                            g[a1Lead_] += 2*(a1Lead*leadFactor - a1)*leadFactor;
//                            g[jLead_] += 2*(leadFactor*jLead - j)*leadFactor;
//                        }
//
//                        // Soft time error
//                        //error += softError*t2;
//                        // g[_t] += 2*softError*t;
//                        return error;
//                    }
//                });
//            }
//        }
//    }
//    protected static double domainError(double d) {
//        return Math.pow(d, 2)/Math.pow(d+1, 2);
//    }
//    protected static double domainGradient(double d) {
//        double d2 = d*d;
//        double d3 = d2*d;
//        return -2*d3/Math.pow((d2 + 1), 2) + 2*d/(d2 + 1);
//    }
//
//    public double solve(int maxfneval, double tolerance) throws Exception {
//        // Allocate the variables.
//        double [] x = new double [variables.size()];
//        // Lower and upper bounds
//        double [] lower = new double [variables.size()];
//        double [] upper = new double [variables.size()];
//        Arrays.fill(lower, Double.NEGATIVE_INFINITY);
//        Arrays.fill(upper, Double.POSITIVE_INFINITY);
//        // Read the variables from the motion.
//        for (Var var : variables) {
//            x[var.i()] = var.get();
//            lower[var.i()] = var.getLowerLimit();
//            upper[var.i()] = var.getUpperLimit();
//        }
//        // Solve it.
//        lastError = solve(x, lower, upper, TNC_MSG_NONE, maxfneval, tolerance*tolerance, 0, tolerance*tolerance);
//        // Write back the variables to the motion.
//        for (Var var : variables) {
//            var.set(x[var.i()]);
//        }
//        return lastError;
//    }
//
//    public double getLastError() {
//        return lastError;
//    }
//
//    public void outputMotionPath(String message, Iterable<Motion> motionPath) {
//        double t = 0;
//        for (Motion motion : motionPath) {
//            t += motion.getTime();
//            log(String.format("%s t=%f %s", message, t, motion));
//        }
//    }
//
}
