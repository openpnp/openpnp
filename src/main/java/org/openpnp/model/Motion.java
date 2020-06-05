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

package org.openpnp.model;

import java.util.function.BiFunction;

import org.openpnp.machine.reference.axis.ReferenceVirtualAxis;
import org.openpnp.machine.reference.driver.AbstractMotionPlanner.MotionCommand;
import org.openpnp.spi.Axis;
import org.openpnp.spi.ControllerAxis;

/**
 * The Motion represents one segment/waypoint in a motion sequence. It contains the point in time
 * and the location vector along with a number of derivatives. What the derivatives mean is subject 
 * to the use case, it could mean a regular segment or the limits (feedrate, acceleration etc.) to 
 * be applied to another Motion etc.
 * 
 * Like Vector Math this is quite universal and it should be interpreted as such.    
 *
 */
public class Motion {
    public enum MotionOption {
        FixedWaypoint,
        CoordinatedWaypoint,
        LimitToSafeZZone,
        Completion, 
        Stillstand;

        protected int flag(){
            return 1 << this.ordinal();
        }
    }

    private double time;
    private AxesLocation [] vector;
    private int options;
    private MotionCommand motionCommand;

    public Motion(double time, MotionCommand motionCommand, AxesLocation[] vector, MotionOption... options) {
        super();
        this.time = time;
        this.motionCommand = motionCommand;
        this.vector = vector.clone();
        for (MotionOption option : options) {
            this.options |= option.flag();
        }
    }
    protected Motion(double time, MotionCommand motionCommand, AxesLocation[] vector, int options) {
        super();
        this.time = time;
        this.vector = vector.clone();
        this.options = options;
    }
    public Motion(double time, MotionCommand motionCommand, Motion motion) {
        this(time, motion.motionCommand, motion.vector, motion.options);
    }

    public boolean hasOption(MotionOption option) {
        return (this.options & option.flag()) != 0;
    }
    public void setOption(MotionOption option) {
        this.options |= option.flag();
    }
    public void clearOption(MotionOption option) {
        this.options &=  ~option.flag();
    }

    public double getTime() {
        return time;
    }
    public void setTime(double time) {
        this.time = time;
    }

    public MotionCommand getMotionCommand() {
        return motionCommand;
    }

    int getOrder() {
        return vector.length-1;
    }


    public enum Derivative {
        Location,
        Velocity,
        Acceleration,
        Jerk
    }

    static public final ReferenceVirtualAxis Euclidean = new ReferenceVirtualAxis() {
        { this.setName("Euclidean"); }
    };

    public AxesLocation getVector(int order) {
        if (order >= 0 && order < vector.length) { 
            return vector[order];
        }
        return AxesLocation.zero;
    }
    public AxesLocation getVector(Derivative order) {
        return getVector(order.ordinal());
    }
    public void setVector(int order, AxesLocation derivativeVector) {
        if (order >= 0 && order < vector.length) { 
            vector[order] = derivativeVector;
        }
        // TODO: else throw?
    }
    public void setVector(Derivative order, AxesLocation derivativeVector) {
        setVector(order.ordinal(), derivativeVector);
    }

    public String toString() {
        return "{ t="+time+"s, o="+String.format("x%X", options)+", l="+
                getVector(Derivative.Location)+", V="+
                getVector(Derivative.Velocity)+", a="+
                getVector(Derivative.Acceleration)+", j="+
                getVector(Derivative.Jerk)+" }";
    }

    protected Motion applyFunction(
            double time, 
            MotionCommand motionCommand, 
            BiFunction<Double, Double, Double> locationFunction, 
            BiFunction<Double, Double, Double> derivativeFunction,
            Motion other,
            int options) {
        int maxOrder = Math.max(vector.length, other.vector.length);
        AxesLocation [] newVector = new AxesLocation[maxOrder];
        for (int order = 0; order < maxOrder; order++) {
            if (order == 0) {
                newVector[order] = new AxesLocation(locationFunction, getVector(order), other.getVector(order));
            }
            else {
                newVector[order] = new AxesLocation(derivativeFunction, getVector(order), other.getVector(order));
            }
        }
        Motion motion = new Motion(time, motionCommand, newVector, options);
        return motion;
    }
    public Motion average(Motion other) {
        return applyFunction(other.getTime(),
                other.getMotionCommand(),
                (a, b) -> ((a+b)/2.0), 
                (a, b) -> ((a+b)/2.0), 
                other, other.options);
    }
    public Motion max(Motion other) {
        return applyFunction(other.getTime(),
                other.getMotionCommand(),
                (a, b) -> (Math.max(a, b)), 
                (a, b) -> (Math.max(a, b)), 
                other, other.options);
    }
    public Motion min(Motion other) {
        return applyFunction(other.getTime(),
                other.getMotionCommand(),
                (a, b) -> (Math.min(a, b)), 
                (a, b) -> (Math.min(a, b)), 
                other, other.options);
    }
    public Motion envelope(Motion other) {
        return applyFunction(other.getTime(),
                other.getMotionCommand(),
                (a, b) -> (b), 
                (a, b) -> (Math.min(a, b)), 
                other, other.options);
    }
    public Motion interpolate(Motion other, double ratio) {
        return applyFunction(ratio*other.getTime(),
                other.getMotionCommand(),
                (a, b) -> ((1.0-ratio)*a+ratio*b), 
                (a, b) -> ((1.0-ratio)*a+ratio*b), 
                other, other.options);
    }

    public static Motion computeWithLimits(MotionCommand motionCommand, AxesLocation location0, AxesLocation location1, double speed,
            boolean applyDriverLimit, boolean nistMode) throws Exception {
        return computeWithLimits(motionCommand, location0, location1, speed, applyDriverLimit, nistMode,
                (axis, order) -> (axis.getMotionLimit(order)));
    }
    public static Motion computeWithLimits(MotionCommand motionCommand, AxesLocation location0, AxesLocation location1, double speed, 
            boolean applyDriverLimit, boolean nistMode, 
            BiFunction<ControllerAxis, Integer, Double> limitProvider) throws Exception {
        // Create a distance vector that has only axes mentioned in location1 that at the same time 
        // do not match coordinates with location0.
        AxesLocation distance = location0.motionSegmentTo(location1);
        if (distance.isEmpty()) {
            return null;
        }
        // Find the euclidean distance of linear/rotational and all axes.
        // Find the most limiting axis feed-rate/acceleration/etc. limit per distance.
        // Find the most limiting driver feed-rate limit.
        double [] linearLimits = new double [ControllerAxis.motionLimitsOrder+1];
        double [] rotationalLimits = new double [ControllerAxis.motionLimitsOrder+1];
        double [] overallLimits = new double [ControllerAxis.motionLimitsOrder+1];
        for (int order = 1; order <= ControllerAxis.motionLimitsOrder; order++) {
            linearLimits[order] = Double.POSITIVE_INFINITY;
            rotationalLimits[order] = Double.POSITIVE_INFINITY;
            overallLimits[order] = Double.POSITIVE_INFINITY;
        }
        double minDriverFeedrate =  Double.POSITIVE_INFINITY;
        for (ControllerAxis axis : distance.getControllerAxes()) {
            if (applyDriverLimit && axis.getDriver() != null) {
                double driverFeedrate = axis.getDriver().getFeedRatePerSecond()
                        .convertToUnits(AxesLocation.getUnits()).getValue();
                if (driverFeedrate != 0.0) {
                    minDriverFeedrate = Math.min(minDriverFeedrate, driverFeedrate);
                }
            }
            double d = Math.abs(distance.getCoordinate(axis));
            double dSq = d*d;  
            if (dSq > 0) {
                for (int order = 1; order <= ControllerAxis.motionLimitsOrder; order++) {
                    double limit = limitProvider.apply((ControllerAxis) axis, order);
                    if (limit > 0) {
                        // In the following section we find the limiting i.e minimum limits.
                        // But why divide by d?
                        // This is due to the fact that the limits are applied to the overall motion, but one axis 
                        // will in general only contribute a fraction to the overall motion i.e. the limit is only applicable 
                        // at the rate at which the axis contributes. This is per unit of length i.e. divided by the axis' motion unit 
                        // vector component. The division by d takes care of the first step of that. As the second step the result 
                        // will be normed to the overall motion unit after the loop, by multiplying by the overall motion distance.
                        if (axis.getType() == Axis.Type.Rotation) {
                            rotationalLimits[0] += dSq;
                            rotationalLimits[order] = Math.min(rotationalLimits[order],
                                    limit/d);
                        }
                        else {
                            linearLimits[0] += dSq;
                            linearLimits[order] = Math.min(linearLimits[order],
                                    limit/d);
                        }
                        overallLimits[0] += dSq;
                        overallLimits[order] = Math.min(overallLimits[order],
                                limit/d);
                    }
                }
            }
        }
        // From the sum of squares calculate the Euclidean metric. 
        linearLimits[0] = Math.sqrt(linearLimits[0]);
        rotationalLimits[0] = Math.sqrt(rotationalLimits[0]);
        overallLimits[0] = Math.sqrt(overallLimits[0]);
        // Norm the fractional axes limits to the overall motion distances.
        for (int order = 1; order <= ControllerAxis.motionLimitsOrder; order++) {
            if (linearLimits[0] > 0) {
                linearLimits[order] = linearLimits[order] * linearLimits[0];
            }
            if (rotationalLimits[0] > 0) {
                rotationalLimits[order] = rotationalLimits[order] * rotationalLimits[0];
            }
            if (overallLimits[0] > 0) {
                overallLimits[order] = overallLimits[order] * overallLimits[0];
            }
        }
        if (applyDriverLimit) {
            // According to NIST RS274NGC Interpreter - Version 3, Section 2.1.2.5 (p. 7)
            // the feed-rate is to be interpreted over the Euclidean linear axis distance of a move 
            // and in the absence of any linear axes, over the Euclidean rotational distance 
            // of the move.
            // https://tsapps.nist.gov/publication/get_pdf.cfm?pub_id=823374
            if (linearLimits[0] > 0) {
                // Limit the linear axes limit by the driver feed-rate. 
                linearLimits[1] = Math.min(linearLimits[1], minDriverFeedrate); 
            }
            else  {
                // Limit the rotational axes limit by the driver feed-rate?
                // NOPE: it is non-sense to apply a mm/s feedrate to a rotational axis. 
                // We depart from former OpenPnP behavior here. 
                //   rotationalLimits[1] = Math.min(rotationalLimits[1], minDriverFeedrate); 
            }
        }
        // Note: inside the MotionPlanner, everything is calculated using the overall Euclidean distance
        // not the NIST distance. So we get no problems with motion across multiple drivers where the sub-set 
        // of per-driver axes might or might not include linear axes. However, the GcodeDriver needs the calculations in
        // NIST mode therefore we have these two modes switched by nistMode.
        double [] euclideanLimits;
        if (nistMode) {
            if (linearLimits[0] > 0) {
                euclideanLimits = linearLimits;
            }
            else {
                euclideanLimits = rotationalLimits;
            }
        }
        else {
            euclideanLimits = overallLimits;
        }
        // Calculate the time it would take at constant velocity.
        double time = Math.max(
                linearLimits[0]/linearLimits[1],
                rotationalLimits[0]/rotationalLimits[1]);
        if (!Double.isFinite(time)) {
            throw new Exception("Feedrate(s) missing on (some) axes: "+distance.getControllerAxes());
        }
        double euclideanTime = euclideanLimits[0]/euclideanLimits[1];
        // We convert from the NIST feed-rate limit to the Euclidean limit by relating the motion time. 
        // Also include the given speed factor.
        double speedEffective = (time > 0 ? euclideanTime/time : 1.0) * Math.max(0.01, speed);
        // Add a virtual Distance axis to store the limits in the motion, as these are applicable per overall distance.
        AxesLocation motionLocation = location1.put(new AxesLocation(Motion.Euclidean, euclideanLimits[0]));
        AxesLocation motionDistance = distance.put(new AxesLocation(Motion.Euclidean, euclideanLimits[0]));
        AxesLocation [] motionDerivatives = new AxesLocation [ControllerAxis.motionLimitsOrder+1];
        // The 0th derivative is the location itself. 
        motionDerivatives[0] = motionLocation;
        // Now calculate the axis specific limits for all derivatives. 
        for (int order = 1; order <= ControllerAxis.motionLimitsOrder; order++) {
            final int derivative = order;
            motionDerivatives[order] = new AxesLocation(motionDistance.getAxes(), 
                    (a) -> (new Length(
                            // The speed factor must be to the power of the derivative order.
                            Math.pow(speedEffective, derivative)* 
                            euclideanLimits[derivative]*Math.abs(motionDistance.getCoordinate(a)) // fractional axis distance
                            /euclideanLimits[0],  
                            AxesLocation.getUnits())));
        }
        // Save all that in a Motion. 
        return  new Motion(euclideanTime/speedEffective, 
                motionCommand, 
                motionDerivatives, 
                MotionOption.FixedWaypoint, MotionOption.CoordinatedWaypoint);
    }

    public AxesLocation getLocation() {
        return getVector(Derivative.Location);
    }
    public double getFeedRatePerSecond(LengthUnit units) {
        return getVector(Derivative.Velocity).getCoordinate(Euclidean, units);
    }
    public double getFeedRatePerMinute(LengthUnit units) {
        return getVector(Derivative.Velocity).getCoordinate(Euclidean, units)*60.0;
    }
    public double getAccelerationPerSecond2(LengthUnit units) {
        return getVector(Derivative.Acceleration).getCoordinate(Euclidean, units);
    }
    public double getJerkPerSecond3(LengthUnit units) {
        return getVector(Derivative.Jerk).getCoordinate(Euclidean, units);
    }
}
