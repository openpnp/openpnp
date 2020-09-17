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
import java.util.Map.Entry;
import java.util.function.BiFunction;

import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.model.MotionProfile.ProfileOption;
import org.openpnp.spi.ControllerAxis;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Driver.MotionControlType;
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
        this.options &=  ~option.flag();
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
        private AxesLocation location;
        private AxesLocation movedAxesLocation;
        private Double feedRatePerSecond;
        private Double accelerationPerSecond2;
        private Double jerkPerSecond3;

        public MoveToCommand(AxesLocation location, AxesLocation movedAxesLocation,
                Double feedRatePerSecond, Double accelerationPerSecond2, Double jerkPerSecond3) {
            super();
            this.location = location;
            this.movedAxesLocation = movedAxesLocation;
            this.feedRatePerSecond = feedRatePerSecond;
            this.accelerationPerSecond2 = accelerationPerSecond2;
            this.jerkPerSecond3 = jerkPerSecond3;
        }

        public Motion getMotion() {
            return Motion.this;
        }

        public AxesLocation getLocation() {
            return location;
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

    }

    /**
     * Interpolate the Motion using the given parameters and return a list of moveToCommands with waypoints and
     * envelope rate constraints (feed-rate, acceleration limits as needed). 
     * 
     * @param driver
     * @param maxSteps The maximum number of interpolation steps we're allowed to use. Governed by the controller's 
     * look-ahead queue.
     * @param timeStep The minimum interpolation time step. 
     * @param distStep The minimum distance over which an interpolated step has to move, given in axis resolution ticks.
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

        double time = getTime();
        Integer maxSteps = driver.getInterpolationMaxSteps();
        Double timeStep = driver.getInterpolationTimeStep();
        Integer distStep = driver.getInterpolationMinStep();
        if (maxSteps == null || timeStep == null || distStep == null) {
            throw new Exception("Driver does not support move interpolation.");
        }

        int numSteps = (int)Math.min(Math.floor(time/timeStep/2)*2, maxSteps);
        if (numSteps < 4) {
            // No interpolation, or move too short for interpolation. Just execute as one moderated moveTo. 
            return moderatedMoveTo(driver);
        }

        List<MoveToCommand> list = new ArrayList<>(numSteps);
        // Perform the interpolation. 
        //Sanity
        distStep = Math.max(1, distStep);
        AxesLocation locationS = getMomentaryLocation(0);
        AxesLocation velocityS = getMomentaryVelocity(0);
        double tS = 0;
        double tSNominal = 0;
        AxesLocation location0 = locationS;
        AxesLocation velocity0 = velocityS;
        double t0 = 0;
        double t0Nominal = 0;
        double t1Nominal = 0;
        double maxVelocity = 0;
        for (long i = 1; i <= numSteps; i++) {
            double t1 = i*time/numSteps; 
            AxesLocation location1 = getMomentaryLocation(t1);
            AxesLocation segment = location0.motionSegmentTo(location1).drivenBy(driver);
            boolean isTooSmall = segment.multiply(1.0/distStep).matches(AxesLocation.zero);  
            if (i == numSteps && isTooSmall) {
                // Last step distance lower than distStep resolution ticks, merge with previous segment
                if (list.size() > 0) {
                    list.remove(list.size() - 1);
                }
                location0 = locationS;
                velocity0 = velocityS;
                t0 = tS;
                t0Nominal = tSNominal;
                segment = location0.motionSegmentTo(location1).drivenBy(driver);
                isTooSmall = false;
            }
            if (!isTooSmall) {
                final AxesLocation ds = segment;
                double distance = ds.getRS274NGCMetric(driver, 
                        (axis) -> ds.getCoordinate(axis));
                AxesLocation movedAxesLocation = new AxesLocation(segment.getAxes(driver), 
                        (axis) -> location1.getLengthCoordinate(axis));
                AxesLocation velocity1 = getMomentaryVelocity(t1);
                // Note, this is an approximation if the profile is curved. We could calculate the dot product.
                // But then again, the controller will perform junction deviation and all bets are off anyway.
                final AxesLocation vel0 = velocity0;
                double v0 = segment.getRS274NGCMetric(driver, 
                        (axis) -> vel0.getCoordinate(axis));
                double v1 = segment.getRS274NGCMetric(driver, 
                        (axis) -> velocity1.getCoordinate(axis));
                maxVelocity = Math.max(Math.max(Math.abs(v0)+0.0001,  Math.abs(v1)+0.0001), maxVelocity);
                // Avg. velocity with constant velocity.
                double avgVelocity = (v0 + v1)*0.5;
                double dtNominal = distance == 0 ? 0 : distance/avgVelocity;
                t1Nominal = t0Nominal + dtNominal;
                // Acceleration is the velocity difference over nominal time.
                double acceleration = (v1 - v0)/dtNominal;

                //TRACE
                //Logger.trace("t0="+t0+", t1="+t1+", t1(nom)="+t1Nominal+", d="+distance+", v1="+v1);
                //TRACE

                // Add to list.
                list.add(new MoveToCommand(location1,
                        movedAxesLocation, // just the axes that are actually moved  
                        null,//Math.max(Math.abs(v0)+0.0001,  Math.abs(v1)+0.0001), 
                        Math.abs(acceleration)+0.0001,
                        null)); // No jerk, we're simulating it, remember?

                // Next, please.
                tS = t0;
                tSNominal = t0Nominal;
                locationS = location0;
                velocityS = velocity0;
                t0 = t1;
                t0Nominal = t1Nominal;
                location0 = location1;
                velocity0 = velocity1;
            }
        }
        if (list.size() < 4) {
            // Interpolation collapsed.
            return moderatedMoveTo(driver);
        }
        // The interpolation will use constant acceleration to reach the way-points, i.e. it will be slightly faster. 
        // Re-time the whole path to match the planning time exactly.
        double factor = t1Nominal/time;
        double factorSq = factor*factor;
        // Set the maximum for the whole move.
        list.get(0).feedRatePerSecond = maxVelocity;
        for (MoveToCommand move : list) {
            if (move.feedRatePerSecond != null) {
                move.feedRatePerSecond *= factor;
            }
            if (move.accelerationPerSecond2 != null) {
                move.accelerationPerSecond2 *= factorSq;
            }
        }
        // TODO: fuse similar acceleration segments into one. 
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
                location1,
                getMovingAxesTargetLocation(driver),
                factor*moderatedProfile.getProfileVelocity(MotionControlType.ConstantAcceleration), 
                factor*avgAcceleration,
                null));
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
                getLocation1(),
                getMovingAxesTargetLocation(driver),
                getFeedRatePerSecond(driver),
                getAccelerationPerSecond2(driver),
                getJerkPerSecond3(driver)));
        return list;
    }
}
