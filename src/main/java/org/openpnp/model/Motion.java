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

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.model.MotionProfile.ProfileOption;
import org.openpnp.spi.Axis;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.HeadMountable;
import org.openpnp.util.Triplet;

/**
 * The Motion represents one segment in a motion sequence. It contains the start and end location
 * along with the motion profile i.e. the jerk/acceleration/velocity over time. If the Motion is  
 * coordinated, it will move along a straight line between start and end. Otherwise it might sway
 * from this line due to different axis velocity/acceleration/jerk limits and/or due to entry/exit
 * momentum. 
 *
 */
public class Motion {

    /**
     * Contains all possible options for a motion.
     */
    public enum MotionOption {
        /**
         * Disable backslash compensation or anything else, that causes additional moves.
         */
        SpeedOverPrecision,
        /**
         * The move does not have to be a straight line and profile segments do not need to 
         * coincide in time.
         */
        UncoordinatedMotion,
        /**
         * The driver's tool-path feed-rate limit is not applied, only the axes' feed-rate limits are.
         */
        NoDriverLimit,
        /**
         * The motion is limited to the SafeZ zone, usually required to allow uncoordinated motion.
         */
        LimitToSafeZZone,
        /**
         * The motion is open to continue, let the controller handle the real-time deceleration planning.
         */
        JogMotion,
        /**
         * A pseudo-Motion that signals still-stand of the machine. 
         */
        Stillstand;

        public int flag() {
            return 1 << this.ordinal();
        }

        public boolean isSetIn(int options) {
            return (flag() & options) != 0;
        }
    }

    final private HeadMountable headMountable;
    final private double nominalSpeed;
    final private AxesLocation location0;
    final private AxesLocation location1;
    private MotionProfile [] axesProfiles;
    private HashMap<ControllerAxis, Integer> axisIndex = new HashMap<>(); 

    private int options;
    private double effectiveSpeed;
    private double euclideanDistance;
    private double plannedTime1;

    public static int optionFlags(MotionOption... options) {
        int optionFlags = 0;
        for (MotionOption option : options) {
            optionFlags |= option.flag();
        }
        return optionFlags;
    }

    public Motion(HeadMountable headMountable, AxesLocation location0, AxesLocation location1, double nominalSpeed, int options) {
        super();
        this.headMountable = headMountable;
        this.location0 = location0;
        this.location1 = location1;
        this.nominalSpeed = nominalSpeed;
        this.options = options;
        int count = 0;
        for (ControllerAxis axis : location1.getControllerAxes()) {
            axisIndex.put(axis, count++);
        }
        axesProfiles = new MotionProfile[count];
        computeLimitsAndProfile();
    }
    public Motion(HeadMountable headMountable, AxesLocation location0, AxesLocation location1, double nominalSpeed, MotionOption... options) {
        this(headMountable, location0, location1, nominalSpeed, optionFlags(options));
    }
    public Motion(Motion motion) {
        this(motion.headMountable, motion.location0, motion.location1, motion.nominalSpeed, motion.options);
    }

    public HeadMountable getHeadMountable() {
        return headMountable;
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
    public int getOptions() {
        return options;
    }

    public double getTime() {
        return axesProfiles[0].getTime();
    }

    public double getPlannedTime0() {
        return plannedTime1 - getTime();
    }

    public double getPlannedTime1() {
        return plannedTime1;
    }

    public void setPlannedTime1(double plannedTime1) {
        this.plannedTime1 = plannedTime1;
    }

    private MotionProfile getAxisProfile(ControllerAxis axis) {
        return axesProfiles[axisIndex.get(axis)];
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(); 
        str.append("{ t=");
        str.append(getTime());
        str.append("s, o=");
        str.append(String.format("x%X", options));
        for (MotionProfile profile : axesProfiles) {
            str.append("\n");
            str.append(profile);
        }
        str.append("}");
        return str.toString();
    }

    public double getNominalSpeed() {
        return nominalSpeed;
    }

    public double getEffectiveSpeed() {
        return effectiveSpeed;
    }

    public boolean isEmpty() {
        return euclideanDistance == 0 && getTime() == 0;
    }

    /**
     * Compute the limits (maximum axis velocity, acceleration, jerk) and the initial raw axis motion profile.
     * 
     */
    protected void computeLimitsAndProfile() {
        // Create a distance vector that has only axes mentioned in location1 that at the same time 
        // do not match coordinates with location0.
        AxesLocation distance = location0.motionSegmentTo(location1);
        final int motionLimitsOrder = 3;
        if (distance.isEmpty() || hasOption(MotionOption.UncoordinatedMotion)) {
            // Zero distance or uncoordinated motion. Axes constraints simply apply directly.
            for (Entry<ControllerAxis, Integer> entry : axisIndex.entrySet()) {
                ControllerAxis axis = entry.getKey();
                double sMin = Double.NEGATIVE_INFINITY;
                double sMax = Double.POSITIVE_INFINITY;
                if (axis instanceof ReferenceControllerAxis) {
                    if (((ReferenceControllerAxis) axis).isSoftLimitLowEnabled()) {
                        sMin = ((ReferenceControllerAxis) axis).getSoftLimitLow().convertToUnits(AxesLocation.getUnits()).getValue();
                    }
                    if (((ReferenceControllerAxis) axis).isSoftLimitHighEnabled()) {
                        sMax = ((ReferenceControllerAxis) axis).getSoftLimitHigh().convertToUnits(AxesLocation.getUnits()).getValue();
                    }
                }
                effectiveSpeed = getNominalSpeed();
                
                double vMax = effectiveSpeed 
                        *axis.getMotionLimit(1);  

                double aMax = Math.pow(effectiveSpeed, 2) // speed factor must be to the power of the order of the derivative
                        *axis.getMotionLimit(2);  

                double jMax = Math.pow(effectiveSpeed, 3) // speed factor must be to the power of the order of the derivative
                        *axis.getMotionLimit(3);

                // Compute s0 by distance rather than taking location0, because some axes may have been omitted in location1. 
                double s1 = location1.getCoordinate(axis); 
                double s0 = s1 - distance.getCoordinate(axis);
                axesProfiles[entry.getValue()] = new MotionProfile(
                        s0, s1,
                        0, 0, 0, 0, // initialize with still-stand
                        sMin, sMax, 
                        vMax, aMax, aMax, jMax, 
                        0, Double.POSITIVE_INFINITY,
                        profileOptions());
            }
            // As these profiles are uncoordinated, they need to be synchronized, i.e. made sure they take the same amount of time.
            MotionProfile.synchronizeProfiles(axesProfiles);
            euclideanDistance = distance.getEuclideanMetric();
            effectiveSpeed = getNominalSpeed();
        }
        else {
            // Coordinated non-zero motion.
            // Find the euclidean distance of linear/rotational and all axes.
            // Find the most limiting axis feed-rate/acceleration/etc. limit per distance.
            // Find the most limiting driver feed-rate limit.
            double [] linearLimits = new double [motionLimitsOrder+1];
            double [] rotationalLimits = new double [motionLimitsOrder+1];
            double [] overallLimits = new double [motionLimitsOrder+1];
            for (int order = 1; order <= motionLimitsOrder; order++) {
                linearLimits[order] = Double.POSITIVE_INFINITY;
                rotationalLimits[order] = Double.POSITIVE_INFINITY;
                overallLimits[order] = Double.POSITIVE_INFINITY;
            }
            double minDriverFeedrate =  Double.POSITIVE_INFINITY;
            for (ControllerAxis axis : distance.getControllerAxes()) {
                if (axis.getDriver() != null && !hasOption(MotionOption.NoDriverLimit)) {
                    double driverFeedrate = axis.getDriver().getFeedRatePerSecond()
                            .convertToUnits(AxesLocation.getUnits()).getValue();
                    if (driverFeedrate != 0.0) {
                        minDriverFeedrate = Math.min(minDriverFeedrate, driverFeedrate);
                    }
                }
                double d = Math.abs(distance.getCoordinate(axis));
                double dSq = d*d;  
                if (dSq > 0) {
                    for (int order = 1; order <= motionLimitsOrder; order++) {
                        double limit = axis.getMotionLimit(order);
                        if (limit > 0) {
                            // In the following section we find the limiting i.e minimum limits.
                            // But why divide by d?
                            // This is due to the fact that the limits are applied to the overall motion, but one axis 
                            // will in general only contribute a fraction to the overall motion i.e. the limit is only applicable 
                            // at the rate at which the axis contributes. This is per unit of length i.e. divided by the axis' motion unit 
                            // vector component. The division by d takes care of the first step of that. The second step i.e. norming the 
                            // result to the overall motion unit, will take place  after the loop, by multiplying by the overall motion 
                            // distance.
                            if (axis.isControllerRotational()) {
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
            for (int order = 1; order <= motionLimitsOrder; order++) {
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
            if (!hasOption(MotionOption.NoDriverLimit)) {
                // According to NIST RS274NGC Interpreter - Version 3, Section 2.1.2.5 (p. 7)
                // the F feed-rate is to be interpreted over the Euclidean linear axis distance of a move 
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
            double time = Math.max(
                    linearLimits[0]/linearLimits[1],
                    rotationalLimits[0]/rotationalLimits[1]);
            double euclideanTime = overallLimits[0]/overallLimits[1];
            // We convert from the (optionally) driver-limited NIST feed-rate to Euclidean limit by relating the motion time. 
            // Also include the given speed factor.
            effectiveSpeed = (time > 0 ? euclideanTime/time : 1.0) * Math.max(0.01, nominalSpeed);
            euclideanDistance = overallLimits[0];
            
            for (Entry<ControllerAxis, Integer> entry : axisIndex.entrySet()) {
                ControllerAxis axis = entry.getKey();
                double axisFraction = Math.abs(distance.getCoordinate(axis)) // fractional axis distance
                        /euclideanDistance;
                double sMin = Double.NEGATIVE_INFINITY;
                double sMax = Double.POSITIVE_INFINITY;
                if (axis instanceof ReferenceControllerAxis) {
                    if (((ReferenceControllerAxis) axis).isSoftLimitLowEnabled()) {
                        sMin = ((ReferenceControllerAxis) axis).getSoftLimitLow().convertToUnits(AxesLocation.getUnits()).getValue();
                    }
                    if (((ReferenceControllerAxis) axis).isSoftLimitHighEnabled()) {
                        sMax = ((ReferenceControllerAxis) axis).getSoftLimitHigh().convertToUnits(AxesLocation.getUnits()).getValue();
                    }
                }
                double vMax = 
                        effectiveSpeed 
                        *overallLimits[1]*axisFraction;  

                double aMax = 
                        Math.pow(effectiveSpeed, 2) // speed factor must be to the power of the order of the derivative
                        *overallLimits[2]*axisFraction;  

                double jMax = 
                        Math.pow(effectiveSpeed, 3) // speed factor must be to the power of the order of the derivative
                        *overallLimits[3]*axisFraction;

                // Compute s0 by distance rather than taking location0, because some axes may have been omitted in location1. 
                double s1 = location1.getCoordinate(axis); 
                double s0 = s1 - distance.getCoordinate(axis);
                
                axesProfiles[entry.getValue()] = new MotionProfile(
                        s0, s1, 
                        0, 0, 0, 0, // initialize with still-stand
                        sMin, sMax, 
                        vMax, aMax, aMax, jMax, 
                        0, Double.POSITIVE_INFINITY,
                        profileOptions());
            }
            MotionProfile.coordinateProfiles(axesProfiles);
        }
    }

    private int profileOptions() {
        int profileOptions = 0;
        if (!hasOption(MotionOption.UncoordinatedMotion)) {
            profileOptions |= ProfileOption.Coordinated.flag(); 
        }
        return profileOptions;
    }

    public AxesLocation getLocation0() {
        return location0;
    }

    public AxesLocation getLocation1() {
        return location1;
    }

    public MotionProfile[] getAxesProfiles() {
        return axesProfiles;
    }

    public HashMap<ControllerAxis, Integer> getAxisIndex() {
        return axisIndex;
    }

    public double getEuclideanDistance() {
        return euclideanDistance;
    }

    public AxesLocation getMomentaryLocation(double time) {
        return new AxesLocation(axisIndex.keySet(),
                (axis) -> new Length(getAxisProfile(axis).getMomentaryLocation(time), AxesLocation.getUnits()));
    }
    public AxesLocation getMomentaryVelocity(double time) {
        return new AxesLocation(axisIndex.keySet(),
                (axis) -> new Length(getAxisProfile(axis).getMomentaryVelocity(time), AxesLocation.getUnits()));
    }
    public AxesLocation getMomentaryAcceleration(double time) {
        return new AxesLocation(axisIndex.keySet(),
                (axis) -> new Length(getAxisProfile(axis).getMomentaryAcceleration(time), AxesLocation.getUnits()));
    }
    public AxesLocation getMomentaryJerk(double time) {
        return new AxesLocation(axisIndex.keySet(),
                (axis) -> new Length(getAxisProfile(axis).getMomentaryJerk(time), AxesLocation.getUnits()));
    }

    /**
     * Get the target location of this Motion with only the moved axes of the given driver in it. 
     * 
     * @param driver
     * @return
     */
    public AxesLocation getMovingAxesTargetLocation(Driver driver) {
        AxesLocation axesMoved = location0.motionSegmentTo(location1);
        return new AxesLocation(axesMoved.getAxes(driver), 
                (axis) -> location1.getLengthCoordinate(axis));
    }

    /**
     * Get the rate function of a motion from the planned profile. 
     * 
     * @param driver The driver for which the rate is calculated i.e. for the axes mapped to it.
     * @param f The function to be applied to the motion profile to obtain the rate.
     * @return a Triplet with <linear, rotational, overall> Euclidean rate.
     */
    public Triplet<Double, Double, Double> getRate(Driver driver, BiFunction<ControllerAxis, MotionProfile, Double> f) {
        double linearRate = 0;
        double rotationalRate = 0;
        double euclideanRate = 0;
        for (Entry<ControllerAxis, Integer> entry : axisIndex.entrySet()) {
            ControllerAxis axis = entry.getKey();
            if (axis.getDriver() == driver || driver == null) {
                MotionProfile profile = axesProfiles[entry.getValue()];
                double val =  f.apply(axis, profile);
                if (axis.isControllerRotational()) {
                    rotationalRate += Math.pow(val, 2);
                }
                else {
                    linearRate += Math.pow(val, 2);
                }
                euclideanRate += Math.pow(val, 2);
            }
        }
        linearRate = Math.sqrt(linearRate);
        rotationalRate = Math.sqrt(rotationalRate);
        euclideanRate = Math.sqrt(euclideanRate);
        return new Triplet<Double, Double, Double>(linearRate, rotationalRate, euclideanRate);
    }

    /**
     * Get the rate according to NIST RS274NGC Interpreter - Version 3, Section 2.1.2.5 (p. 7).
     * The rate is to be interpreted over the Euclidean linear axis distance of a move 
     * and in the absence of any linear axes, over the Euclidean angular distance of the move.
     * @see https://tsapps.nist.gov/publication/get_pdf.cfm?pub_id=823374
     * 
     * @param driver The driver for which the rate is calculated i.e. for the axes mapped to it.
     * @param f The function to be applied to the motion profile to obtain the rate.
     * @param defaultRate
     * @return The rate in driver units per second^x
     */
    public Double getNistRate(Driver driver, BiFunction<ControllerAxis, MotionProfile, Double> f, Double defaultRate) {
        Triplet<Double, Double, Double> rate = getRate(driver, f);
        if (rate.first != 0.0) {
            return Length.convertToUnits(rate.first, 
                    AxesLocation.getUnits(), driver == null ? AxesLocation.getUnits() : driver.getUnits());
        }
        else if (rate.second != 0.0) {
            return Length.convertToUnits(rate.second, 
                    AxesLocation.getUnits(), driver == null ? AxesLocation.getUnits() : driver.getUnits());
        }
        return defaultRate;
    }

    /**
     * @param driver The driver for which the rate is calculated i.e. for the axes mapped to it.
     * @return The driver specific feed-rate according to NIST RS274NGC Interpreter - Version 3, Section 2.1.2.5 (p. 7)
     * in driver units per second. 
     */
    public Double getFeedRatePerSecond(Driver driver) {
        return getNistRate(driver,
                (axis, profile) -> (profile.getProfileVelocity()),
                driver == null ? null : driver.getFeedRatePerSecond().convertToUnits(driver.getUnits()).getValue()); 
    }
    /**
     * @param driver The driver for which the rate is calculated i.e. for the axes mapped to it.
     * @return The driver specific feed-rate according to NIST RS274NGC Interpreter - Version 3, Section 2.1.2.5 (p. 7)
     * in driver units per minute. 
     */
    public Double getFeedRatePerMinute(Driver driver) {
        return getFeedRatePerSecond(driver)*60.0;
    }
    /**
     * @param driver The driver for which the rate is calculated i.e. for the axes mapped to it.
     * @return The driver specific acceleration limit in driver units per second squared. 
     */
    public Double getAccelerationPerSecond2(Driver driver) {
        return getNistRate(driver,
                (axis, profile) -> (Math.max(profile.getProfileEntryAcceleration(), profile.getProfileExitAcceleration())),
                null);
    }
    /**
     * @param driver The driver for which the rate is calculated i.e. for the axes mapped to it.
     * @return The driver specific jerk limit in driver units per second cubed.
     */
    public Double getJerkPerSecond3(Driver driver) {
        return getNistRate(driver,
                (axis, profile) -> (profile.getProfileJerk()),
                null);
    }
}
