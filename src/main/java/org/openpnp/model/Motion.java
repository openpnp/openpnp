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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;

import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.model.MotionProfile.ProfileOption;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Driver.MotionControlType;
import org.openpnp.spi.HeadMountable;
import org.openpnp.util.NanosecondTime;
import org.openpnp.util.Triplet;
import org.pmw.tinylog.Logger;

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
         * In uncoordinated moves from/to still-stand, synchronize axes by straightening the motion into a line 
         * where possible. 
         */
        SynchronizeStraighten,
        /**
         * In uncoordinated moves to still-stand, synchronize axes by moving them as early as possible.
         */
        SynchronizeEarlyBird,
        /**
         * In uncoordinated moves from still-stand, synchronize axes by moving them as late as possible.
         */
        SynchronizeLastMinute,
        /**
         * The driver's tool-path feed-rate limit is not applied, only the axes' feed-rate limits are.
         */
        NoDriverLimit,
        /**
         * The motion is limited to the Safe Zone, usually required to allow uncoordinated motion.
         */
        LimitToSafeZone,
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

    public Motion(HeadMountable headMountable, AxesLocation location0, AxesLocation location1, double nominalSpeed, double feedrateOverride, double accelerationOverride, double jerkOverride, int options) {
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
        computeLimitsAndProfile(feedrateOverride, accelerationOverride, jerkOverride);
    }
    public Motion(HeadMountable headMountable, AxesLocation location0, AxesLocation location1, double nominalSpeed, int options) {
        this(headMountable, location0, location1, nominalSpeed, 0, 0, 0, options);
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
        this.options &= ~option.flag();
    }
    public int getOptions() {
        return options;
    }

    public double getTime() {
        return axesProfiles.length == 0 ? 0 : axesProfiles[0].getTime();
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
     * @param jerkOverride 
     * @param accelerationOverride 
     * @param feedrateOverride 
     * 
     */
    protected void computeLimitsAndProfile(double feedrateOverride, double accelerationOverride, double jerkOverride) {
        // Create a distance vector that has only axes mentioned in location that at the same time 
        // do not match coordinates with location0.
        AxesLocation distance = location0.motionSegmentTo(location1);
        final int motionLimitsOrder = 3;
        if (distance.isEmpty() || hasOption(MotionOption.UncoordinatedMotion)) {
            // Zero distance or uncoordinated motion. Axes constraints simply apply directly.
            effectiveSpeed = getNominalSpeed();
            boolean linearMove = false;
            for (ControllerAxis axis : distance.getControllerAxes()) {
                double d = Math.abs(distance.getCoordinate(axis));
                if (d > 0 && !axis.isRotationalOnController()) {
                    linearMove = true;
                }
            }
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
                    if (hasOption(MotionOption.LimitToSafeZone)) {
                        if (((ReferenceControllerAxis) axis).isSafeZoneLowEnabled()) {
                            sMin = ((ReferenceControllerAxis) axis).getSafeZoneLow().convertToUnits(AxesLocation.getUnits()).getValue();
                        }
                        if (((ReferenceControllerAxis) axis).isSafeZoneHighEnabled()) {
                            sMax = ((ReferenceControllerAxis) axis).getSafeZoneHigh().convertToUnits(AxesLocation.getUnits()).getValue();
                        }
                    }
                }

                double d = Math.abs(distance.getCoordinate(axis));

                double vMax = effectiveSpeed 
                        *axis.getMotionLimit(1);  
                if (d > 0 && (axis.isRotationalOnController() ^ linearMove) && feedrateOverride != 0) {
                    vMax = Math.min(vMax, feedrateOverride);
                }

                double aMax = Math.pow(effectiveSpeed, 2) // speed factor must be to the power of the order of the derivative
                        *axis.getMotionLimit(2);  
                if (d > 0 && (axis.isRotationalOnController() ^ linearMove) && accelerationOverride != 0) {
                    aMax = Math.min(aMax, accelerationOverride);
                }

                double jMax = Math.pow(effectiveSpeed, 3) // speed factor must be to the power of the order of the derivative
                        *axis.getMotionLimit(3);
                if (d > 0 && (axis.isRotationalOnController() ^ linearMove) && jerkOverride != 0) {
                    jMax = Math.min(jMax, jerkOverride);
                }

                // Compute s0 by distance rather than taking location0, because some axes may have been omitted in location. 
                double s1 = location1.getCoordinate(axis); 
                double s0 = s1 - distance.getCoordinate(axis);
                int options = profileOptions();
                if (axis.getDriver() != null && axis.getDriver().getMotionControlType() == MotionControlType.SimpleSCurve) {
                    options |= ProfileOption.SimplifiedSCurve.flag();
                }
                axesProfiles[entry.getValue()] = new MotionProfile(
                        s0, s1,
                        0, 0, 0, 0, // initialize with still-stand
                        sMin, sMax, 
                        vMax, aMax, aMax, jMax, 
                        0, Double.POSITIVE_INFINITY,
                        options);
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
                double d = Math.abs(distance.getCoordinate(axis));
                double dSq = d*d;  
                if (d > 0 && axis.getDriver() != null && !hasOption(MotionOption.NoDriverLimit)) {
                    double driverFeedrate = axis.getDriver().getFeedRatePerSecond()
                            .convertToUnits(AxesLocation.getUnits()).getValue();
                    if (driverFeedrate != 0.0) {
                        minDriverFeedrate = Math.min(minDriverFeedrate, driverFeedrate);
                    }
                }
                if (dSq > 0) {
                    if (axis.isRotationalOnController()) {
                        rotationalLimits[0] += dSq;
                    }
                    else {
                        linearLimits[0] += dSq;
                    }
                    overallLimits[0] += dSq;
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
                            if (axis.isRotationalOnController()) {
                                rotationalLimits[order] = Math.min(rotationalLimits[order],
                                        limit/d);
                            }
                            else {
                                linearLimits[order] = Math.min(linearLimits[order],
                                        limit/d);
                            }
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
            // According to NIST RS274NGC Interpreter - Version 3, Section 2.1.2.5 (p. 7)
            // the F feed-rate is to be interpreted over the Euclidean linear axis distance of a move 
            // and in the absence of any linear axes, over the Euclidean rotational distance 
            // of the move.
            // https://tsapps.nist.gov/publication/get_pdf.cfm?pub_id=823374
            // Calculate the factor between the overall vector and the feed-rate relevant vector.  
            double overallFactor;
            if (linearLimits[0] > 0) {
                overallFactor = overallLimits[0]/linearLimits[0];
            }
            else  {
                overallFactor = overallLimits[0]/rotationalLimits[0];
            }
            if (feedrateOverride != 0) {
                if (linearLimits[0] > 0) {
                    // Limit the linear axes limit by the override. 
                    linearLimits[1] = Math.min(linearLimits[1], feedrateOverride); 
                }
                else  {
                    // Limit the rotational axes limit by the override.
                    rotationalLimits[1] = Math.min(rotationalLimits[1], feedrateOverride); 
                }
                overallLimits[1] = Math.min(overallLimits[1], feedrateOverride*overallFactor);
            }
            else if (!hasOption(MotionOption.NoDriverLimit)) {

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
            if (accelerationOverride != 0) {
                if (linearLimits[0] > 0) {
                    // Limit the linear axes limit by override.
                    linearLimits[2] = Math.min(linearLimits[2], accelerationOverride); 
                }
                else  {
                    // Limit the rotational axes limit by the override.
                    rotationalLimits[2] = Math.min(rotationalLimits[2], accelerationOverride); 
                }
                overallLimits[2] = Math.min(overallLimits[2], accelerationOverride*overallFactor);
            }
            if (jerkOverride != 0) {
                if (linearLimits[0] > 0) {
                    // Limit the linear axes limit by the override. 
                    linearLimits[3] = Math.min(linearLimits[3], jerkOverride); 
                }
                else  {
                    // Limit the rotational axes limit by the override.
                    rotationalLimits[3] = Math.min(rotationalLimits[3], jerkOverride); 
                }
                overallLimits[3] = Math.min(overallLimits[3], jerkOverride*overallFactor); 
            }
            double time = Math.max(
                    linearLimits[0]/linearLimits[1],
                    rotationalLimits[0]/rotationalLimits[1]);
            double euclideanTime = overallLimits[0]/overallLimits[1];
            // We convert from the (optionally) driver-limited RS274NGC feed-rate to Euclidean limit by relating the motion time. 
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
                    if (hasOption(MotionOption.LimitToSafeZone)) {
                        if (((ReferenceControllerAxis) axis).isSafeZoneLowEnabled()) {
                            sMin = ((ReferenceControllerAxis) axis).getSafeZoneLow().convertToUnits(AxesLocation.getUnits()).getValue();
                        }
                        if (((ReferenceControllerAxis) axis).isSafeZoneHighEnabled()) {
                            sMax = ((ReferenceControllerAxis) axis).getSafeZoneHigh().convertToUnits(AxesLocation.getUnits()).getValue();
                        }
                    }
                }
                double vMax = 
                        effectiveSpeed 
                        *overallLimits[1]*axisFraction;  

                double aMax = 
                        Math.pow(nominalSpeed, 2) // speed factor must be to the power of the order of the derivative
                        *overallLimits[2]*axisFraction;  

                double jMax = 
                        Math.pow(nominalSpeed, 3) // speed factor must be to the power of the order of the derivative
                        *overallLimits[3]*axisFraction;

                // Compute s0 by distance rather than taking location0, because some axes may have been omitted in location. 
                double s1 = location1.getCoordinate(axis); 
                double s0 = s1 - distance.getCoordinate(axis);

                int options = profileOptions();
                if (axis.getDriver() != null && axis.getDriver().getMotionControlType() == MotionControlType.SimpleSCurve) {
                    options |= ProfileOption.SimplifiedSCurve.flag();
                }
                axesProfiles[entry.getValue()] = new MotionProfile(
                        s0, s1, 
                        0, 0, 0, 0, // initialize with still-stand
                        sMin, sMax, 
                        vMax, aMax, aMax, jMax, 
                        0, Double.POSITIVE_INFINITY,
                        options);
            }
            MotionProfile.coordinateProfiles(axesProfiles);
        }
    }

    private int profileOptions() {
        int profileOptions = 0;
        if (!hasOption(MotionOption.UncoordinatedMotion)) {
            profileOptions |= ProfileOption.Coordinated.flag(); 
        }
        if (hasOption(MotionOption.SynchronizeEarlyBird)) {
            profileOptions |= ProfileOption.SynchronizeEarlyBird.flag(); 
        }
        if (hasOption(MotionOption.SynchronizeLastMinute)) {
            profileOptions |= ProfileOption.SynchronizeLastMinute.flag(); 
        }
        if (hasOption(MotionOption.SynchronizeStraighten)) {
            profileOptions |= ProfileOption.SynchronizeStraighten.flag(); 
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

    public Integer getAxisIndex(ControllerAxis axis) {
        return axisIndex.get(axis);
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
                if (axis.isRotationalOnController()) {
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
    public Double getRS274NGCRate(Driver driver, BiFunction<ControllerAxis, MotionProfile, Double> f, Double defaultRate) {
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
        Double defaultFeedrate = driver.getFeedRatePerSecond()
                .convertToUnits(driver.getUnits()).getValue()
                *getNominalSpeed();
        if (defaultFeedrate == 0.0) {
            defaultFeedrate  = null;
        }
        if (driver.getMotionControlType() == MotionControlType.ToolpathFeedRate) {
            return defaultFeedrate;
        }
        return getRS274NGCRate(driver,
                (axis, profile) -> (profile.getProfileVelocity(driver.getMotionControlType())),
                defaultFeedrate); 
    }
    /**
     * @param driver The driver for which the rate is calculated i.e. for the axes mapped to it.
     * @return The driver specific feed-rate according to NIST RS274NGC Interpreter - Version 3, Section 2.1.2.5 (p. 7)
     * in driver units per minute. 
     */
    public Double getFeedRatePerMinute(Driver driver) {
        Double feedRate = getFeedRatePerSecond(driver);
        if (feedRate != null) {
            return feedRate*60.;
        }
        return null;
    }
    /**
     * @param driver The driver for which the rate is calculated i.e. for the axes mapped to it.
     * @return The driver specific acceleration limit in driver units per second squared. 
     */
    public Double getAccelerationPerSecond2(Driver driver) {
        if (driver.getMotionControlType() == MotionControlType.ToolpathFeedRate) {
            return null;
        }
        return getRS274NGCRate(driver,
                (axis, profile) -> (Math.max(profile.getProfileEntryAcceleration(driver.getMotionControlType()), profile.getProfileExitAcceleration(driver.getMotionControlType()))),
                null);
    }
    /**
     * @param driver The driver for which the rate is calculated i.e. for the axes mapped to it.
     * @return The driver specific jerk limit in driver units per second cubed.
     */
    public Double getJerkPerSecond3(Driver driver) {
        if (driver.getMotionControlType() == MotionControlType.ToolpathFeedRate) {
            return null;
        }
        return getRS274NGCRate(driver,
                (axis, profile) -> (profile.getProfileJerk(driver.getMotionControlType())),
                null);
    }

    public class MoveToCommand {
        private final AxesLocation location0;
        private final AxesLocation location1;
        private final AxesLocation movedAxesLocation;
        private Double feedRatePerSecond;
        private Double accelerationPerSecond2;
        private Double jerkPerSecond3;
        private Double t0;
        private Double time;

        private Double v0;
        private Double v1;

        public MoveToCommand(AxesLocation location0, AxesLocation location1, AxesLocation movedAxesLocation,
                Double feedRatePerSecond, Double accelerationPerSecond2, Double jerkPerSecond3, Double t0, Double time, Double v0, Double v1) {
            super();
            this.location0 = location0;
            this.location1 = location1;
            this.movedAxesLocation = movedAxesLocation;
            this.feedRatePerSecond = feedRatePerSecond;
            this.accelerationPerSecond2 = accelerationPerSecond2;
            this.jerkPerSecond3 = jerkPerSecond3;
            this.t0 = t0;
            this.time = time;
            this.v0 = v0;
            this.v1 = v1;
        }

        public Motion getMotion() {
            return Motion.this;
        }

        public AxesLocation getLocation0() {
            return location0;
        }

        public AxesLocation getLocation1() {
            return location1;
        }

        public AxesLocation getMovedAxesLocation() {
            return movedAxesLocation;
        }

        public Double getFeedRatePerSecond() {
            return feedRatePerSecond;
        }
        public Double getFeedRatePerMinute() {
            return feedRatePerSecond == null ? null : feedRatePerSecond*60.0;
        }

        public Double getAccelerationPerSecond2() {
            return accelerationPerSecond2;
        }

        public Double getJerkPerSecond3() {
            return jerkPerSecond3;
        }

        public Double getTimeStart() {
            return t0;
        }
        public Double getTimeDuration() {
            return time;
        }
        public Double getV0() {
            return v0;
        }
        public Double getV1() {
            return v1;
        }
    }

    /**
     * Interpolate the Motion using the given parameters and return a list of moveToCommands with waypoints and
     * envelope feed-rate and acceleration limits as needed. 
     * 
     * @param driver
     * @return
     * @throws Exception
     */
    public List<MoveToCommand> interpolatedMoveToCommands(Driver driver) throws Exception {
        if (driver.getMotionControlType() == MotionControlType.ModeratedConstantAcceleration) {
            return moderatedMoveTo(driver);
        }
        else if (!driver.getMotionControlType().isInterpolated()) {
            return singleMoveTo(driver);
        }

        // Check if this is a simple constant acceleration move.
        if (MotionProfile.isCoordinated(axesProfiles)) {
            boolean hasJerk = false;
            for (MotionProfile profile : axesProfiles) {
                if (profile.getProfileVelocity(MotionControlType.ConstantAcceleration) != 0 
                        && !profile.isConstantAcceleration()) {
                    hasJerk = true;
                    break;
                }
            }
            if (! hasJerk) {
                // Simple move.
                return moderatedMoveTo(driver);
            }
        }
        double time = getTime();
        Integer maxSteps = driver.getInterpolationMaxSteps();
        Integer maxJerkSteps = driver.getInterpolationJerkSteps();
        Double timeStep = driver.getInterpolationTimeStep();
        Integer distStep = driver.getInterpolationMinStep();
        Length junctionDeviationLength = driver.getJunctionDeviation();

        if (maxSteps == null || maxJerkSteps == null || timeStep == null || distStep == null || junctionDeviationLength == null) {
            throw new Exception("Driver does not support move interpolation.");
        }

        int numSteps = (int)Math.floor(time/timeStep/2)*2;
        if (numSteps < 4) {
            // No interpolation, or move too short for interpolation. Just execute as one moderated moveTo. 
            return moderatedMoveTo(driver);
        }
        // Sanity.
        distStep = Math.max(3, distStep);

        // Converting Junction Deviation (per axis) to allowable instant delta V
        //   s = 1/2 a t²
        //     = 1/2 a (dV/a)²
        //     = 1/2 dV²/a
        // Solving for dV
        //   dV = √(2)*√(a*s)
        // Because we treat entry/exit delta V separately, we take half.  
        double junctionDeviation = junctionDeviationLength.convertToUnits(AxesLocation.getUnits()).getValue();
        AxesLocation maxDeltaV = new AxesLocation(location0.getAxes(driver),
                (axis) -> new Length(
                        1./2*Math.sqrt(2)*Math.sqrt(junctionDeviation*axesProfiles[getAxisIndex(axis)].getAccelerationMax()), 
                        AxesLocation.getUnits()));
        // Determine per axis maximum delta a.
        AxesLocation maxDeltaA = new AxesLocation(location0.getAxes(driver),
                (axis) -> new Length(
                        (axesProfiles[getAxisIndex(axis)].isConstantAcceleration() ?
                                Double.POSITIVE_INFINITY :
                                    axesProfiles[getAxisIndex(axis)].getAccelerationMax()/Math.max(1, maxJerkSteps)), 
                        AxesLocation.getUnits()));

        /*
         * The interpolation uses minimal time intervals to step through the move. At each time step it is testing
         * the interpolation by a straight segment connecting the last interpolation point (or the starting point) to the 
         * time step point. This interpolation might be forbidden for various reasons:
         *
         * 1. Distance too small: If the distance is too small we will get artifacts from the axes' resolution when
         *    the way-points are snapped to resolution ticks. Differential Vectors will become degraded. 
         *
         * 2. Instant change of velocity too large: When interpolating uncoordinated motion, curves in N-dimensional space
         *    will occur and they need to be approximated into polygons. At the polygon corners there will be instant 
         *    velocity changes, if moved through at speed. The instant velocity change must not be too large or
         *    stepper motors may lose steps or vibrate too much. We are checking at segment begin and end and allow half
         *    the allowed instant velocity change at each end. 
         *
         * 3. Instant change of acceleration too large: In order to simulate 3rd order motion control a.k.a. jerk control, 
         *    we need to ramp up and down acceleration in multiple discrete steps to approximate a continuous acceleration 
         *    ramp. The instant acceleration change must not become too large.  
         * 
         * 4. Acceleration has already plateaued or has already left a plateau: Once constant acceleration is reached or left 
         *    (including zero acceleration when constant velocity is reached), a new segment should already have been made.  
         *
         * When 1. happens, we must continue with the next time step, effectively invalidating this one. However, if this 
         * one is the very last time step, we have no choice but to merge it with the previous interpolation segment.
         * This might not fully conform with 2. or 3. but deviations are expected to be very small due to the small distance. 
         * 
         * When 2., 3. or 4. happens, we create a new interpolation segment, but at the previous valid time step that had 
         * not yet violated these constraints. If there is no previous valid time step, we are forced to take this one anyway. 
         * In this case, time resolution was simply too coarse. Due to the nature of 3rd order motion control, i.e. due to the 
         * limits in jerk, acceleration etc., it is expected this will only occur in very tight curves, where speed is 
         * already very low and further degradation can be tolerated.    
         * 
         */

        // Perform the interpolation. 
        double compT0 = NanosecondTime.getRuntimeSeconds();
        List<MoveToCommand> list = new ArrayList<>(numSteps);
        // Last taken interpolation point, initialized to be the start. 
        AxesLocation location0 = getMomentaryLocation(0);
        AxesLocation velocity0 = getMomentaryVelocity(0);
        AxesLocation acceleration0 = getMomentaryAcceleration(0);
        double t0 = 0;
        MoveToCommand command0 = null;

        // Last candidate interpolation point. 
        AxesLocation location1 = location0;
        AxesLocation velocity1 = velocity0;
        AxesLocation acceleration1 = acceleration0;
        double t1 = 0;
        MoveToCommand command1 = null;

        // Second-last taken interpolation point.
        AxesLocation locationS = location0;
        AxesLocation velocityS = velocity0;
        AxesLocation accelerationS = acceleration0;
        double tS = 0;
        MoveToCommand commandS = null;

        double minVelocity = driver.getMinimumVelocity();
        double minAcceleration = minVelocity*4; // HACK
        double maxVelocity = minVelocity;
        
        double dt = time/numSteps;
        
//        TreeSet<Double> probes = new TreeSet<>();
//        for (MotionProfile profile : axesProfiles) {
//            double t = profile.t[0];
//            for (int i = 0; i <= MotionProfile.segments; i++) {
//                t += profile.t[i+1];
//                if (profile.j[i] != 0 || profile.a[i] != 0) {
//                    int segs = (int)Math.ceil(profile.t[i+1]/timeStep);
//                    double dt = profile.t[i+1]/segs;
//                    for (int ti = 0; ti < segs; ti++) {
//                        double tp = t-ti*dt;
//                        if (tp > timeStep*0.9) {
//                            Double higher = probes.higher(tp);
//                            if (higher == null || higher - tp > timeStep*0.9) {
//                                Double lower = probes.lower(tp);
//                                if (lower != null && tp - lower < timeStep*0.9) {
//                                    probes.remove(lower);
//                                }
//                                probes.add(tp);
//                            }
//                        }
//                    }
//                }
//            }
//        }

        int probeCount = 0;
        for (int i = 1; i <= numSteps; i++) {
            double t2 = i*dt;

//        numSteps = probes.size();
//        int probe = 0;
//        for (Double t2 : probes) {
//            //Logger.debug(t2 - t1);
//            if (t2 - t1 < timeStep*0.9) {
//                continue;
//            }
//            probe++;

            AxesLocation location2 = getMomentaryLocation(t2);
            AxesLocation acceleration2 = getMomentaryAcceleration(t2);
            if (i < numSteps 
                    && acceleration2.matches(AxesLocation.zero) && acceleration1.matches(AxesLocation.zero)) {
                // Straight line, nothing happens.
               // continue;
            }
            probeCount++;
            // When the candidate segment is added, we need to repeat the analysis, with the new origin.
            while(true) {
                AxesLocation segment = location0.motionSegmentTo(location2).drivenBy(driver);
                boolean isTooSmall = segment.multiply(1.0/distStep).matches(AxesLocation.zero);  
                if (i == numSteps && isTooSmall) {
                    // Last step distance lower than distStep resolution ticks, merge with previous segment.
                    if (command1 != null) {
                        // Just make sure its merged with candidate command 1.
                        command1 = null;
                    }
                    else {
                        // There is no candidate command. Need to merge with the previous command.
                        if (list.size() > 0) {
                            list.remove(list.size() - 1);
                        }
                        location0 = locationS;
                        velocity0 = velocityS;
                        acceleration0 = accelerationS;
                        t0 = tS;
                        command0 = commandS;
                        segment = location0.motionSegmentTo(location2).drivenBy(driver);
                    }
                    isTooSmall = false;
                }
                if (isTooSmall) {
                    break;
                }
                else {
                    final AxesLocation ds = segment;
                    double distance = ds.getRS274NGCMetric(driver, 
                            (axis) -> ds.getCoordinate(axis));
                    AxesLocation movedAxesLocation = new AxesLocation(segment.getAxes(driver), 
                            (axis) -> location2.getLengthCoordinate(axis));
                    AxesLocation velocity2 = getMomentaryVelocity(t2);
    
                    // Note, if the motion is curved, we might have an angle between the segments (corners of a polygon), 
                    // so we need to calculate the velocity projected onto the straight segment. This will lower the 
                    // absolute velocity slightly and introduce an instant velocity change in the corner instead 
                    // (in controllers this is typically called "junction deviation" or "jerk"). 
                    final AxesLocation segmentVelocity0 = velocity0.along(segment);
                    final AxesLocation segmentVelocity2 = velocity2.along(segment);
    
                    // Calculate RS274NGC (G-code) tool-path velocities.
                    double v0 = segment.getRS274NGCMetric(driver, 
                            (axis) -> segmentVelocity0.getCoordinate(axis));
                    double v2 = segment.getRS274NGCMetric(driver, 
                            (axis) -> segmentVelocity2.getCoordinate(axis));
    
                    // Avg. velocity with constant acceleration.
                    double avgVelocity = (v0 + v2)*0.5;
                    double dtNominal = distance == 0 ? 0 : distance/avgVelocity;
                    // Tool-path acceleration is the velocity difference over nominal time.
                    double acceleration = (v2 - v0)/dtNominal;
                    double dotA02 = acceleration0.dotProduct(acceleration2);
                    // Record the maximum velocity. This is done, even if this segment is later not be recorded, which is 
                    // fine because we actually want to get the true peak.
                    double maxSegmentVelocity = Math.max(Math.abs(v0), Math.abs(v2));
                    Double velocity = null;
                    double t = t2 - t0;
                    if ((dotA02 <= 0 && t < dtNominal - MotionProfile.eps)
                            || (avgVelocity < MotionProfile.eps)) {
                        // Acceleration reversal or plateau in the segment. Switch to velocity governing.
                        final AxesLocation segmentAcceleration0 = acceleration0.along(segment);
                        final AxesLocation segmentAcceleration2 = acceleration2.along(segment);
                        // Calculate RS274NGC (G-code) tool-path accelerations.
                        double a0 = segment.getRS274NGCMetric(driver, 
                                (axis) -> segmentAcceleration0.getCoordinate(axis));
                        double a2 = segment.getRS274NGCMetric(driver, 
                                (axis) -> segmentAcceleration2.getCoordinate(axis));
                        double a = Math.max(a0, a2);
                        double s = distance;
                        double signum = Math.signum(acceleration0.dotProduct(segment));
                        if (signum == 0) {
                            signum = -Math.signum(acceleration2.dotProduct(segment));;
                            if (signum == 0) {
                                signum = 1;
                            }
                        }
                        /* Solve for trapezoid V.
                         * From SageMath:
var('s t v v0 v2 a')
t0=(v-v0)/a
t2=(v-v2)/a
t1=t-t0-t2
eq=(s==v0*t0 + 1/2*a*t0^2 + v*t1 + v2*t2 + 1/2*a*t2^2)
solve(eq, v)
                        
                        >>
                        [v == 1/2*a*t + 1/2*v0 + 1/2*v2 - 1/2*sqrt(a^2*t^2 + 2*a*t*v0 - 4*a*s - v0^2 + 2*(a*t + v0)*v2 - v2^2), 
                         v == 1/2*a*t + 1/2*v0 + 1/2*v2 + 1/2*sqrt(a^2*t^2 + 2*a*t*v0 - 4*a*s - v0^2 + 2*(a*t + v0)*v2 - v2^2)]]
                         */
                        double sTerm = 1./2*Math.sqrt(Math.pow(a,2)*Math.pow(t,2) + 2*a*t*v0 - 4*a*s - Math.pow(v0,2) + 2*(a*t + v0)*v2 - Math.pow(v2,2));
                        double v = 1./2*a*t + 1./2*v0 + 1./2*v2 - signum*sTerm;
                        double v_ = 1./2*a*t + 1./2*v0 + 1./2*v2 + signum*sTerm;
                        double simpleRampDisplacement = maxSegmentVelocity*t - 1./2*a*Math.pow(t, 2); 
                        double tv0 = (v-v0)/a;
                        double tv2 = (v-v2)/a;
                        double tvMid = t-tv0-tv2;
                        if (simpleRampDisplacement + MotionProfile.eps < s 
                                && tvMid > MotionProfile.eps) {
                            Logger.debug("Trapezoidal t0="+t0+" t2="+t2+" dt="+t+" dtNominal="+dtNominal+" v0="+v0+" v2="+v2+" v="+v+" v'="+v_+" a0="+a0+" a2="+a2+" a="+a+" simple ramp="+simpleRampDisplacement+" s="+s);
                            // It is not just a simple ramp, i.e. it is trapezoidal - take the solution.
                            acceleration = a;
                            dtNominal = t;
                            avgVelocity = s/t;
                            maxSegmentVelocity = v;
                            velocity = v;
                            v0 = Math.min(v0, v);
                            v2 = Math.min(v2, v);
                        }
                        else {
                            Logger.debug("*** Not a trapezoidal t0="+t0+" t2="+t2+" dt="+t+" dtNominal="+dtNominal+" v0="+v0+" v2="+v2+" v="+v+" v'="+v_+" a0="+a0+" a2="+a2+" a="+a+" simple ramp="+simpleRampDisplacement+" s="+s);
                        }
                    }
                    maxVelocity = Math.max(maxSegmentVelocity, maxVelocity);

                    MoveToCommand command2 = new MoveToCommand(
                            location0, location2,
                            movedAxesLocation, // just the axes that are actually moved  
                            velocity, 
                            Math.max(Math.abs(acceleration), minAcceleration),
                            null, // No jerk, we're simulating it, remember?
                            t0, dtNominal, v0, v2); 

                    // Are we making a new segment?
                    boolean newSegment = false;
                    if (velocity == null && (command1 != null && command1.feedRatePerSecond != null)) {
                        // Last segment was velocity governed and this one isn't, must add it.
                        newSegment = true;
                    }
                    else {
                        // Check instant velocity change on entry.
                        AxesLocation deltaV0 = velocity0.subtract(segmentVelocity0);
                        for (ControllerAxis axis : deltaV0.getControllerAxes()) {
                            if (Math.abs(deltaV0.getCoordinate(axis)) > maxDeltaV.getCoordinate(axis)) {
                                newSegment = true;
                                break;
                            }
                        }
                    }
                    if (!newSegment) {
                        // Check instant velocity change on exit.
                        AxesLocation deltaV2 = velocity2.subtract(segmentVelocity2);
                        for (ControllerAxis axis : deltaV2.getControllerAxes()) {
                            if (Math.abs(deltaV2.getCoordinate(axis)) > maxDeltaV.getCoordinate(axis)) {
                                newSegment = true;
                                break;
                            }
                        }
                        if (!newSegment) {
                            // Check acceleration / simulate jerk control. 
                            /*double dotA01 = acceleration0.dotProduct(acceleration1);
                            double dotA12 = acceleration1.dotProduct(acceleration2);
                            if (dotA01 <= 0 ^ dotA12 <= 0) {
                                // plateau entered or left.
                                newSegment = true;
                            }
                            else {*/
                                AxesLocation deltaA20 = acceleration2.subtract(acceleration0);
                                for (ControllerAxis axis : deltaA20.getControllerAxes()) {
                                    double da20 = Math.abs(deltaA20.getCoordinate(axis));
                                    if (da20 > maxDeltaA.getCoordinate(axis)) {
                                        // Acceleration delta too high.
                                        newSegment = true;
                                        break;
                                    }
                                }
                            //}
                        }
                    }
                    if (newSegment) {
                        if (list.size() >= maxSteps) {
                            // Uh-oh, not enough steps available for interpolation. Degrade move to moderated.
                            Logger.debug("Interpolation failed! Max. steps ("+maxSteps+") reached after "
                            +String.format(Locale.US, "%.3f", 100*t2/time)+"% of move time/ "+probeCount+" probes. Degrading to moderated move.");
                            return moderatedMoveTo(driver);
                        }
                        if (command1 == null) {
                            // There is no candidate command before this one. We are forced to take it fully.
                            t1 = t2;
                            location1 = location2;
                            velocity1 = velocity2;
                            acceleration1 = acceleration2; 
                            command1 = command2;
                            command2 = null;
                        }
//                        if (command1.feedRatePerSecond != null) {
//                            Logger.debug("Trapezoidal taken");
//                        }
                        // Add to list.
                        list.add(command1);

                        // Remember previous segment begin.
                        tS = t0;
                        locationS = location0;
                        velocityS = velocity0;
                        accelerationS = acceleration0;
                        commandS = command0;

                        // Shift one segment. 
                        t0 = t1;
                        location0 = location1;
                        velocity0 = velocity1;
                        acceleration0 = acceleration1;
                        command0 = command1;
                        command1 = null;
                        if (command2 == null) {
                            // There was no previous candidate, break the inner loop.
                            break;
                        }
                        // else: continue in the inner loop, create the shifted segment. 
                    }
                    else {
                        // This segment becomes candidate.
                        t1 = t2;
                        location1 = location2;
                        velocity1 = velocity2;
                        acceleration1 = acceleration2; 
                        command1 = command2;
                        // Go to next time segment, break the inner loop.
                        break;
                    }
                }
            }
        }
        // Always add the last candidate command, if left over. 
        if (command1 != null) {
            list.add(command1);
        }

        double compTime = NanosecondTime.getRuntimeSeconds() - compT0;
        Logger.debug("Interpolation "+numSteps+" steps, "+probeCount+" probes, "+list.size()
        +" steps, comp time "+String.format(Locale.US, "%.3f", compTime*1000)+"ms");
        if (list.size() < 1) {
            // Interpolation collapsed.
            return moderatedMoveTo(driver);
        }
        // The interpolation will use constant acceleration to reach the way-points, i.e. it will be slightly faster. 
        // Re-time the whole path to match the planning time exactly.
        double timeEffective = 0;
        for (MoveToCommand move : list) {
            timeEffective += move.getTimeDuration();
        }
        double factor = timeEffective/time;
        double factorSq = factor*factor;
        // Set the maximum for the whole move.
        list.get(0).feedRatePerSecond = maxVelocity;
        double tSum = 0;
        for (MoveToCommand move : list) {
            if (move.feedRatePerSecond != null) {
                move.feedRatePerSecond *= factor;
            }
            if (move.v0 != null) {
                move.v0 *= factor;
            }
            if (move.v1 != null) {
                move.v1 *= factor;
            }
            if (move.accelerationPerSecond2 != null) {
                move.accelerationPerSecond2 *= factorSq;
            }
            move.t0 = tSum;
            move.time /= factor;
            tSum += move.time;
        }
        return list;
    }

    /**
     * Create a constant acceleration move out of a move planned with jerk control.
     * The move should take the same amount of time and have similar average acceleration
     * and peak feed-rates. This creates more defensive short moves, while allowing quicker long moves.
     * In comparison with fixed constant acceleration moves, this will already reduce vibrations a bit.  
     * Because of equal move duration, these moves can also be used to compare constant acceleration 
     * and jerk controlled moves in a fair way. 
     * 
     * @param driver
     * @param time
     * @return
     */
    protected List<MoveToCommand> moderatedMoveTo(Driver driver) {
        double [] unitVector = MotionProfile.getUnitVector(axesProfiles);
        int leadAxis = MotionProfile.getLeadAxisIndex(unitVector);
        MotionProfile profile = axesProfiles[leadAxis];
        double vPeak = profile.getProfileVelocity(MotionControlType.Full3rdOrderControl);
        double vEntry = profile.getVelocity(0);    
        double vExit = profile.getVelocity(7);    
        double dtEntry = profile.getSegmentBeginTime(3);
        double dtExit = profile.getSegmentBeginTime(7) - profile.getSegmentBeginTime(4);
        double avgAcceleration = (Math.abs(vPeak-vEntry) + Math.abs(vPeak - vExit))/(dtEntry + dtExit);
        double time = getTime();
        MotionProfile moderatedProfile = new MotionProfile(
                profile.getLocation(0), profile.getLocation(7),
                vEntry, vExit, 
                0, 0, // entry/exit acceleration is irrelevant for constant acceleration motion control.
                profile.getLocationMin(), profile.getLocationMax(),
                profile.getVelocityMax(),
                avgAcceleration, avgAcceleration,
                0, // no jerk 
                time, profile.getTimeMax(), 
                0);
        moderatedProfile.assertSolved();
        AxesLocation location0 = getLocation0();
        AxesLocation location1 = getLocation1();
        AxesLocation segment = location0.motionSegmentTo(location1).drivenBy(driver);

        // Calculate the factor from this single lead Axis to the rate along the relevant axes 
        // (either linear or rotational axes, according to RS274NGC).  
        double distance = segment.getRS274NGCMetric(driver, 
                (axis) -> segment.getCoordinate(axis));
        double factor = distance/segment.getEuclideanMetric()/Math.abs(unitVector[leadAxis]);
        List<MoveToCommand> list = new ArrayList<>(1);
        list.add(new MoveToCommand(
                location0, location1,
                getMovingAxesTargetLocation(driver),
                Math.max(driver.getMinimumVelocity(), 
                        factor*moderatedProfile.getProfileVelocity(MotionControlType.ConstantAcceleration)), 
                factor*avgAcceleration,
                null,
                0.0, time, Math.abs(factor*vEntry), Math.abs(factor*vExit)));
        return list;
    }

    /**
     * Creates a single move out of the motion. It just takes the nominal profile velocity, acceleration and
     * jerk limits with no regard to how the move will be executed in the controller, i.e. if the move was planned 
     * with jerk control and is then executed on a constant acceleration controller, it will not take the 
     * predicted amount of time. 
     * 
     * @param driver
     * @return
     */
    public List<MoveToCommand> singleMoveTo(Driver driver) {
        List<MoveToCommand> list = new ArrayList<>(1);
        list.add(new MoveToCommand(
                getLocation0(),
                getLocation1(),
                getMovingAxesTargetLocation(driver),
                getFeedRatePerSecond(driver),
                getAccelerationPerSecond2(driver),
                getJerkPerSecond3(driver),
                null, null, null, null));
        return list;
    }
}
