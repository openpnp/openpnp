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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.openpnp.spi.Driver.MotionControlType;
import org.openpnp.util.NanosecondTime;
import org.pmw.tinylog.Logger;

public class MotionProfile {
    private static final boolean traceEnabled = false;
    private static final boolean svgEnabled = false;

    public final static int segments = 7;

    double [] s = new double[segments+1];
    double [] a = new double[segments+1];
    double [] v = new double[segments+1];
    double [] j = new double[segments+1];
    // Time has one more for before/after wait time in synchronized moves.
    double [] t = new double[segments+2]; 

    double sMin;
    double sMax;
    double vMax;
    double aMaxEntry; 
    double aMaxExit; 
    double jMax;
    double tMin;
    double tMax;

    Double initialTime;

    double sEntryControl;
    double sExitControl;
    double tEntryControl;
    double tExitControl;

    static final int iterations = 80;
    static final double vtol = 2.0;      // mm/s
    static final double atol = vtol*2;   // mm/s^2
    static final double jtol = atol*4;   // mm/s^3
    static final double ttol = 0.000001; // s 
    // As we're handling millimeters and seconds, we can use a practical eps. 
    static final double eps = 1e-8;

    int eval;

    double time;

    double solvingTime;

    double sBound0;
    double sBound1;
    double tSBound0;
    double tSBound1;

    double vBound0;
    double tVBound0;
    double vBound1;
    double tVBound1;


    double aBound0;
    double tABound0;
    double aBound1;
    double tABound1;

    int profileOptions;

    public enum ProfileOption {
        Coordinated, 
        SynchronizeEarlyBird, 
        SynchronizeLastMinute,
        SynchronizeStraighten,
        Jog,
        SimplifiedSCurve,
        UnconstrainedExit,
        UnconstrainedEntry, 
        CroppedEntry, 
        CroppedExit,
        Solved;

        public int flag(){
            return 1 << this.ordinal();
        }
    }
    public boolean hasOption(ProfileOption option) {
        return (this.profileOptions & option.flag()) != 0;
    }
    public void setOption(ProfileOption option) {
        this.profileOptions |= option.flag();
    }
    public void clearOption(ProfileOption option) {
        this.profileOptions &= ~option.flag();
    }
    public int getOptions() {
        return profileOptions;
    }

    public MotionProfile(double s0, double s1, double v0, double v1, double a0, double a1,
            double sMin, double sMax, double vMax, double aMaxEntry, double aMaxExit, double jMax, double tMin, double tMax,
            int profileOptions) {
        s[0] = s0;
        s[segments] = s1;
        v[0] = v0;
        v[segments] = v1;
        a[0] = a0;
        a[segments] = a1;
        this.sMin = sMin;
        this.sMax = sMax;
        this.vMax = vMax;
        this.aMaxEntry = aMaxEntry;
        this.aMaxExit = aMaxExit;
        this.jMax = jMax;
        this.tMin = tMin;
        this.tMax = tMax;
        this.profileOptions = profileOptions;
    }

    public MotionProfile(MotionProfile template) {
        this(template.s[0], template.s[segments],
                template.v[0], template.v[segments],
                template.a[0], template.a[segments],
                template.sMin, template.sMax,
                template.vMax, 
                template.aMaxEntry, template.aMaxExit, 
                template.jMax,
                template.tMin, template.tMax,
                template.profileOptions);
    }

    public double getLocation(int segment) {
        return s[segment];
    }
    public double getVelocity(int segment) {
        return v[segment];
    }
    public double getAcceleration(int segment) {
        return a[segment];
    }
    public double getJerk(int segment) {
        return j[segment];
    }
    public double getSegmentBeginTime(int segment) {
        if (segment <= 0) {
            return 0;
        }
        return t[segment] + getSegmentBeginTime(segment-1);
    }


    public double getLocationMin() {
        return sMin;
    }

    public void setLocationMin(double sMin) {
        this.sMin = sMin;
    }

    public double getLocationMax() {
        return sMax;
    }

    public void setLocationMax(double sMax) {
        this.sMax = sMax;
    }

    public double getVelocityMax() {
        return vMax;
    }

    public void setVelocityMax(double vMax) {
        this.vMax = vMax;
    }

    public double getEntryAccelerationMax() {
        return aMaxEntry;
    }

    public void setEntryAccelerationMax(double aMaxEntry) {
        this.aMaxEntry = aMaxEntry;
    }

    public double getExitAccelerationMax() {
        return aMaxExit;
    }

    public void setExitAccelerationMax(double aMaxExit) {
        this.aMaxExit = aMaxExit;
    }

    public double getJerkMax() {
        return jMax;
    }

    public void setJerkMax(double jMax) {
        this.jMax = jMax;
    }

    public double getTimeMin() {
        return tMin;
    }

    public void setTimeMin(double tMin) {
        this.tMin = tMin;
    }

    public double getTimeMax() {
        return tMax;
    }

    public void setTimeMax(double tMax) {
        this.tMax = tMax;
    }

    public double getSolvingTime() {
        return solvingTime;
    }

    public double getLowerSBoundary() {
        return sBound0;
    }

    public double getHigherSBoundary() {
        return sBound1;
    }

    public double getLowerSBoundary0Time() {
        return tSBound0;
    }

    public double getHigherSBoundaryTime() {
        return tSBound1;
    }

    public double getLowerVBoundary() {
        return vBound0;
    }

    public double getHigherVBoundary() {
        return vBound1;
    }

    public double getLowerVBoundary0Time() {
        return tVBound0;
    }

    public double getHigherVBoundaryTime() {
        return tVBound1;
    }

    public double getLowerABoundary() {
        return aBound0;
    }

    public double getHigherABoundary() {
        return aBound1;
    }

    public double getLowerABoundary0Time() {
        return tABound0;
    }

    public double getHigherABoundaryTime() {
        return tABound1;
    }

    public boolean isSolved() {
        return hasOption(ProfileOption.Solved);
    }

    public double getProfileVelocity(MotionControlType motionControlType) {
        if (motionControlType == MotionControlType.EuclideanAxisLimits) {
            return vMax;
        }
        else {
            // TODO: for advanced motion planning: calculate the real maximum over the profile, for cases with entry/exit conditions and/or tMin != 0
            return Math.abs(v[4]);
        }
    }

    public double getProfileEntryAcceleration(MotionControlType motionControlType) {
        if (motionControlType == MotionControlType.EuclideanAxisLimits) {
            return aMaxEntry;
        }
        else {
            // TODO: for advanced motion planning: calculate the real maximum over the profile, for cases with entry/exit conditions. 
            return Math.abs(a[2]);
        }
    }

    public double getProfileExitAcceleration(MotionControlType motionControlType) {
        if (motionControlType == MotionControlType.EuclideanAxisLimits) {
            return aMaxExit;
        }
        else {
            // TODO: for advanced motion planning: calculate the real maximum over the profile, for cases with entry/exit conditions. 
            return Math.abs(a[6]);
        }
    }

    public double getProfileJerk(MotionControlType motionControlType) {
        if (motionControlType == MotionControlType.EuclideanAxisLimits) {
            return jMax;
        }
        else {
            return Math.max(Math.abs(j[0]), Math.abs(j[6]));
        }
    }

    public double getTime() {
        return time;
    }

    protected double getMomentary(double ts, double f0, double f7, BiFunction<Integer, Double, Double> f) {
        if (ts <= t[0]) {
            return f0;
        }
        ts -= t[0];
        if (ts >= time) {
            return f7;
        }
        for (int i = 1; i <= segments; i++) {
            if (ts <= t[i]) {
                return f.apply(i, ts);
            }
            ts -= t[i];
        }
        return f7;
    }

    public double getMomentaryLocation(double time) { 
        return getMomentary(time, s[0], s[segments], 
                // s0 + V0*t + 1/2*a0*t^2 + 1/6*j*t^3
                (i, ts) -> (s[i-1] + v[i-1]*ts + 1./2*a[i-1]*Math.pow(ts, 2) + 1./6*j[i-1]*Math.pow(ts, 3)));
    }

    public double getMomentaryVelocity(double time) { 
        return getMomentary(time, v[0], v[segments], 
                // V0 + a0*t + 1/2*j*t^2
                (i, ts) -> (v[i-1] + a[i-1]*ts + 1./2*j[i-1]*Math.pow(ts, 2)));
    }

    public double getMomentaryAcceleration(double time) { 
        return getMomentary(time, a[0], isConstantAcceleration() ? 0 : a[segments], 
                // a0 + j*t
                (i, ts) -> (a[i-1] + j[i-1]*ts));
    }

    public double getMomentaryJerk(double time) { 
        return getMomentary(time, j[0], 0, (i, ts) -> (j[i-1]));
    }

    public enum ErrorState {
        // In descending order of their severity.
        SolutionNotFinite,
        NegativeSegmentTime,
        TimeSumMismatch,
        LocationDiscontinuity,
        VelocityDiscontinuity,
        AccelerationDiscontinuity,
        MinTimeViolated,
        MaxTimeViolated,
        MinLocationViolated,
        MaxLocationViolated,
        MaxVelocityViolated,
        MaxAccelerationViolated,
        MaxJerkViolated;

        boolean isConsistent() {
            return this.ordinal() > AccelerationDiscontinuity.ordinal();
        }
        boolean isCoordinated() {
            return this.ordinal() > MaxTimeViolated.ordinal();
        }
        boolean isSafe() {
            return this.ordinal() > MaxLocationViolated.ordinal();
        }
        boolean isConstrained() {
            return this.ordinal() > MaxJerkViolated.ordinal();
        }
    }

    public ErrorState checkValidity() {
        // Phase 1: Assert hard constraints
        // Assert continuity.
        double tSum = 0;
        for (int i = 0; i <= segments; i++) {
            // Check numeric stability
            if (!(Double.isFinite(t[i]) 
                    && Double.isFinite(s[i])
                    && Double.isFinite(v[i])
                    && Double.isFinite(a[i])
                    && Double.isFinite(j[i]))) {
                return ErrorState.SolutionNotFinite;
            }
            // Check timing.
            if (t[i] < (i == 4 ? -ttol : -eps)) {
                return ErrorState.NegativeSegmentTime;
            }
            tSum += t[i];
            if (i > 0) {
                // Check continuity.
                if (mismatch(s[i], s[i-1] + v[i-1]*t[i] + 1./2*a[i-1]*Math.pow(t[i], 2) + 1./6*j[i-1]*Math.pow(t[i], 3), eps)) {
                    return ErrorState.LocationDiscontinuity;
                }
                if (mismatch(v[i], v[i-1] + a[i-1]*t[i] + 1./2*j[i-1]*Math.pow(t[i], 2), eps)) {
                    return ErrorState.VelocityDiscontinuity;
                }
                if (!isConstantAcceleration() && mismatch(a[i], a[i-1] + j[i-1]*t[i], eps)) {
                    return ErrorState.AccelerationDiscontinuity;
                }
            }
        }
        tSum += t[segments+1];
        // Check before/after dwell time.
        if (t[0] > eps && v[0] != 0) {
            return ErrorState.VelocityDiscontinuity;
        }
        if (t[segments+1] > eps && v[segments] != 0) {
            return ErrorState.VelocityDiscontinuity;
        }
        if (!isConstantAcceleration()) {
            if (t[0] > eps && a[0] != 0) {
                return ErrorState.AccelerationDiscontinuity;
            }
            if (t[segments+1] > eps && a[segments] != 0) {
                return ErrorState.AccelerationDiscontinuity;
            }
        }
        // Check time sum constraints.
        if (mismatch(tSum, time, eps)) {
            return ErrorState.TimeSumMismatch;
        }
        if (tSum < tMin - eps) {
            return ErrorState.MinTimeViolated;
        }
        if (tSum > tMax + eps) {
            return ErrorState.MaxTimeViolated;
        }
        // Check bounds against sMin/sMax.
        if (sBound0 < sMin - eps) {
            return ErrorState.MinLocationViolated;
        }
        if (sBound1 > sMax + eps) {
            return ErrorState.MaxLocationViolated;
        }
        // Phase 2 : Assert lesser constraints
        for (int i = 0; i <= segments; i++) {
            // Check constraints. 
            if (i < segments && s[i] < sMin - eps) { // With the bounds check this is redundant, but we want to double check.
                return ErrorState.MinLocationViolated;
            }
            if (i < segments && s[i] > sMax + eps) { // With the bounds check this is redundant, but we want to double check.
                return ErrorState.MaxLocationViolated;
            }
            if (i < segments && Math.abs(v[i]) > vMax + vtol) {
                return ErrorState.MaxVelocityViolated;
            }
            if (i <= segments/2 && Math.abs(a[i]) > aMaxEntry + atol) {
                return ErrorState.MaxAccelerationViolated;
            }
            if (i > segments/2 && i < segments && Math.abs(a[i]) > aMaxExit + atol) {
                return ErrorState.MaxAccelerationViolated;
            }
            if (Math.abs(j[i]) > jMax + jtol) {
                return ErrorState.MaxJerkViolated;
            }
        } 
        // No error.
        return null;
    }

    static boolean mismatch(double a, double b, double tol) {
        if (Math.abs(a-b) > tol) {
            return true; // (debug point)
        }
        return false;
    }

    public void validate(String label) {
        MotionProfile.ErrorState error = checkValidity();
        if (error != null) {
            Logger.error(label+this+" has error: "+error);
        }
        else {
            trace(label+": "+this);
        }
    }

    public static void validateProfiles(MotionProfile[] profiles) {
        for (int i = 0; i < profiles.length; i++) {
            MotionProfile profile = profiles[i];
            profile.validate("["+i+"]");
        }
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("{s =");
        for (double s : this.s) {
            str.append(String.format(" %.2f", s));
        }
        str.append(", V =");
        for (double v : this.v) {
            str.append(String.format(" %.2f", v));
        }
        str.append(", a =");
        for (double a : this.a) {
            str.append(String.format(" %.2f", a));
        }
        str.append(", j =");
        for (double j : this.j) {
            str.append(String.format(" %.2f", j));
        }
        str.append(", t =");
        for (double t : this.t) {
            str.append(String.format(" %.4f", t));
        }
        str.append(", time = ");
        str.append(String.format("%.6f", time));
        str.append(", tMin = ");
        str.append(String.format("%.6f", tMin));
        str.append("s, bounds s@t = ");
        str.append(String.format("%.2f@%.4f, %.2f@%.4f", sBound0, tSBound0, sBound1, tSBound1));
        str.append(", v@t = ");
        str.append(String.format("%.2f@%.4f, %.2f@%.4f", vBound0, tVBound0, vBound1, tVBound1));
        str.append(", a@t = ");
        str.append(String.format("%.2f@%.4f, %.2f@%.4f", aBound0, tABound0, aBound1, tABound1));
        str.append(", eval = ");
        str.append(eval);
        str.append(", ms = ");
        str.append(String.format("%.3f", solvingTime*1000));
        str.append("}");
        return str.toString();
    }

    public void solve() {
        // scale down tolerances for tiny moves
        double magnitude = Math.max(eps,  Math.min(1.0, 
                0.01*(Math.abs(s[0]-s[segments])
                        +Math.abs(v[0])+Math.abs(v[segments])
                        +Math.abs(a[0])+Math.abs(a[segments]))));
        solve(iterations, vtol*Math.sqrt(magnitude), ttol*Math.sqrt(magnitude));
    }
    public void solve(final int iterations, final double vtol, final double ttol) {
        double tStart = NanosecondTime.getRuntimeSeconds();
        solveForVelocity(iterations, vtol, ttol);
        // Result is now stored in the profile i.e. you can get v[4], a[2], a[6] to get the (signed) solution.
        solvingTime = NanosecondTime.getRuntimeSeconds() - tStart;
        setOption(ProfileOption.Solved);
        if (svgEnabled) {
            MotionProfile traceProfile = new MotionProfile(this);
            traceProfile.computeProfile(v[4], getEffectiveEntryVelocity(jMax), getEffectiveExitVelocity(jMax), tMin);
            traceProfile.toSvg();
        }
    }

    public boolean isConstantAcceleration() {
        return jMax == 0 || Double.isInfinite(jMax);
    }
    public boolean solveForVelocity(final int iterations, final double vtol, final double ttol) {
        // Check for a null move. As we always handle all axes of the machine, we want to be fast with those.
        if (solveIfNullMove()) {
            return true;
        }

        trace("\n### solving "+this);

        // Calculate the effective entry/exit velocity after jerk to acceleration 0.
        double vEffEntry = getEffectiveEntryVelocity(jMax);
        double vEffExit = getEffectiveExitVelocity(jMax);

        // Determine the direction of travel.
        double signum = profileSignum(vEffEntry, vEffExit); 

        // Compute the profile with directional vMax first, this will give us the first indications and may directly solve unconstrained long moves. 
        computeProfile(signum*vMax, vEffEntry, vEffExit, tMin);

        // Immediately return right here if this is a valid solution (this happens if it is a very long move that reaches vMax).
        if (tMin == 0 && t[4] >= 0 && v[0] == v[segments] && a[0] == 0 && a[segments] == 0) {
            trace("Vmax symmetrical move, immediate solution");
            return true;
        }

        // Need to solve this numerically. Because the solution can have many roots and local minimae, we need to split it into multiple
        // regions with known qualities.

        // Find the best solution:
        double bestTime = Double.POSITIVE_INFINITY;
        double bestVelocity = Double.NaN;
        // HACK: for more precise solutions with near symmetric entry/exit conditions? 
        double nearZero = 0; //(s[0] == s[segments] && (a[0] != a[segments] || v[0] != -v[segments]) ? vtol*0.1 : 0); 

        // Solver regions from -vMax to +vMax are split by effective entry/exit velocities and zero. 
        // Note, we do not allow solutions beyond vMax, even if the effective entry/exit velocities are beyond.
        double [] borders = new double [] {
                -vMax, -nearZero, 0, nearZero, vMax, Math.max(-vMax, Math.min(vMax, vEffEntry)), Math.max(-vMax, Math.min(vMax, vEffExit)) 
        };
        Arrays.sort(borders);
        int [][] regions = new int[borders.length-1][];
        int regionCount = 0;
        double borderSResult[] = new double [borders.length];
        double borderTResult[] = new double [borders.length];
        // Mark these results as missing.
        Arrays.fill(borderSResult, Double.NaN);
        int i0 = 0;
        int iVMax = -1;
        for (int i = 1; i < borders.length; i++) {
            if (borders[i0] < borders[i]) {
                // Region not empty, add it.
                regions[regionCount] = new int [] {i0, i};
                regionCount++;
                i0 = i;
                if (borders[i] == v[4]) {
                    iVMax = i;
                }
            }
        }
        // Enter our vMax results from the initial calculation.
        if (iVMax >= 0) {
            borderSResult[iVMax] = s[4] - s[3];
            borderTResult[iVMax] = time;
        }

        double vttol = Math.sqrt(ttol);
        double stol = ttol;

        // Still based on the initial calculation, try obtaining an analytical solution.
        double vInitialGuess = Double.NaN;
        if (tMin == 0 && s[0] != s[segments]) {
            if (isConstantAcceleration()) {
                // Obtained from Sage Math:
                // var ('v v0 v7 a s')
                // tEntry=(v-v0)/a
                // tExit=(v-v7)/a 
                // sEntry=v0*tEntry + 1/2*a*tEntry^2
                // sExit=v7*tEntry + 1/2*a*tExit^2
                // eq=(s==sEntry + sExit)
                // solve(eq, v)
                // [v == -sqrt(a*s + 1/2*v0^2 + v0*v7 - 1/2*v7^2), v == sqrt(a*s + 1/2*v0^2 + v0*v7 - 1/2*v7^2)]

                if (aMaxEntry == aMaxExit) {
                    double sd = signum*(s[segments]-s[0]);
                    vInitialGuess = signum*Math.sqrt(aMaxEntry*sd + 1./2*Math.pow(v[0], 2) + v[0]*v[7] - 1./2*Math.pow(v[7], 2));
                    trace("Analytical solution with constant acceleration profile = "+vInitialGuess);
                }
            }
            else if (!hasOption(ProfileOption.SimplifiedSCurve)){
                // If the move is long enough to reach aMax we can try solving for vPeak analytically.
                // Obtained from Sage Math:
                //
                //            # V max calc with constant acceleration segment
                //
                //            var('s3 v1 v3 a j')
                //            t3 = a/j
                //            v2 = v3 - 1/2*j*t3^2
                //            t2 = (v2-v1)/a
                //            s1 = 0
                //            s2 = s1 + v1*t2 + 1/2*a*t2^2
                //            eq=(s3 == s2 + v2*t3 + 1/2*a*t3^2 - 1/6*j*t3^3)
                //            solve(eq, v3)
                //
                // [v3 == -1/6*(3*a^2 + 2*sqrt(3*a^4 + 18*a*j^2*s3 + 9*j^2*v1^2))/j, 
                //  v3 == -1/6*(3*a^2 - 2*sqrt(3*a^4 + 18*a*j^2*s3 + 9*j^2*v1^2))/j]
                boolean halfProfile = !(hasOption(ProfileOption.UnconstrainedEntry) || hasOption(ProfileOption.UnconstrainedExit));
                if (t[2] > (-t[4]*0.25)) { 
                    // Acceleration segment is long enough.
                    double s3 = ((halfProfile ? (s[4] + s[3])*0.5 : s[4]) - s[1]);
                    // We just trust the (just in time) compiler to eliminate common sub-expressions.
                    vInitialGuess = -1./6*(3*Math.pow(a[1], 2) 
                            - 2*Math.sqrt(3*Math.pow(a[1], 4) + 18*a[1]*Math.pow(j[0], 2)*s3 + 9*Math.pow(j[0], 2)*Math.pow(v[1], 2)))/j[0];
                    double v3_2 = -1./6*(3*Math.pow(a[1], 2) 
                            + 2*Math.sqrt(3*Math.pow(a[1], 4) + 18*a[1]*Math.pow(j[0], 2)*s3 + 9*Math.pow(j[0], 2)*Math.pow(v[1], 2)))/j[0];
                    trace("Analytical solution with constant acceleration segment (1) = "+vInitialGuess+" (2) = "+v3_2);
                }
                else if (t[5] > (-t[4]*0.25)) { 
                    // Deceleration segment is long enough.
                    double s4 = (s[6] - (halfProfile ? (s[4] + s[3])*0.5 : s[3]));
                    // We just trust the (just in time) compiler to eliminate common sub-expressions.
                    vInitialGuess = (-1./6*(3*Math.pow(a[6], 2) 
                            - 2*Math.sqrt(3*Math.pow(a[6], 4) - 18*a[6]*Math.pow(j[6], 2)*s4 + 9*Math.pow(j[6], 2)*Math.pow(v[6], 2)))/j[6]);
                    double v3_2 = (-1./6*(3*Math.pow(a[6], 2) 
                            + 2*Math.sqrt(3*Math.pow(a[6], 4) - 18*a[6]*Math.pow(j[6], 2)*s4 + 9*Math.pow(j[6], 2)*Math.pow(v[6], 2)))/j[6]);
                    trace("Analytical solution with constant deceleration segment (1) = "+vInitialGuess+" (2) = "+v3_2);
                }
            }
            if (Double.isFinite(vInitialGuess) && Math.abs(vInitialGuess) > 0 && Math.abs(vInitialGuess) <= vMax) {
                computeProfile(vInitialGuess, vEffEntry, vEffExit, tMin);
                if (t[4] >= -ttol && t[4] < vttol) {
                    trace("taken "+this);
                    return true;
                }
            }
        }

        // Prepare border cases. 
        for (int i = 0; i < borders.length; i++) {
            if (i == 0 || borders[i-1] < borders[i]) {
                double vPeak = borders[i];
                if (Double.isNaN(borderSResult[i])) {
                    // must first calculate border condition.
                    computeProfile(vPeak, vEffEntry, vEffExit, tMin);
                    borderSResult[i] = s[4] - s[3];
                    borderTResult[i] = time;
                }
                double sign = Math.signum(vPeak);
                double sResult = 
                        (sign == 0 ? -Math.abs(borderSResult[i]) // s != 0 is always negative i.e. invalid at point V == 0 
                                : sign*borderSResult[i]); 
                double tResult = borderTResult[i];
                if (Double.isInfinite(tResult)) {
                    tResult = sResult > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
                }
                // Consider valid border cases as solutions.
                else if (sResult >= -stol && (tMin == 0 || tResult >= tMin-ttol) && tResult < bestTime) {
                    bestVelocity = vPeak;
                    bestTime = tResult;
                    trace("border case v="+vPeak+", s="+sResult+", t="+tResult+" "+this);
                }
            }
        }

        // Solve for each region in a logical order most probable to eclipse later regions.
        int regionStart, regionEnd, regionStep;
        if (signum >= 0) {
            regionStart = regionCount-1;
            regionEnd = -1;
            regionStep = -1;
        }
        else {
            regionStart = 0;
            regionEnd = regionCount;
            regionStep = 1;
        }
        for (int regionIndex = regionStart; regionIndex != regionEnd; regionIndex += regionStep) {
            int [] region = regions[regionIndex];
            final int border0 =  region[0];
            final int border1 =  region[1];
            double vPeak0 = borders[border0];
            double vPeak1 = borders[border1];
            double sign = Math.signum(vPeak0+vPeak1);

            double sResult0 = sign*borderSResult[border0]; 
            double sResult1 = sign*borderSResult[border1];
            boolean sValid0 = (sResult0 >= -stol);
            boolean sValid1 = (sResult1 >= -stol);

            if (!(sValid0 || sValid1)) {
                // None valid -> skip this region.
                trace("region invalid in s "+vPeak0+" .. "+vPeak1+", s="+sResult0+" .. "+sResult1);
                continue;
            }

            double tResult0 = borderTResult[border0];
            if (Double.isInfinite(tResult0)) {
                tResult0 = sResult0 > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            }
            double tResult1 = borderTResult[border1];
            if (Double.isInfinite(tResult1)) {
                tResult1 = sResult1 > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            }

            boolean tValid0 = (tMin == 0 || tResult0 >= tMin-ttol);
            boolean tValid1 = (tMin == 0 || tResult1 >= tMin-ttol);
            if (!(tValid0 || tValid1)) {
                // None valid -> skip this region.
                trace("region invalid in t "+vPeak0+" .. "+vPeak1+", s="+sResult0+" .. "+sResult1+", t="+tResult0+" .. "+tResult1);
                continue;
            }

            if (Math.min(tResult0,  tResult1) >= bestTime) {
                trace("region eclipsed by best t "+bestTime+" "+vPeak0+" .. "+vPeak1+", s="+sResult0+" .. "+sResult1+", t="+tResult0+" .. "+tResult1);
                continue;
            }

            if (sValid0 && sValid1 &&
                    Math.min(vEffEntry, vEffExit) <= vPeak0 
                    && Math.max(vEffEntry, vEffExit) >= vPeak1) {
                // We're between two entry/exit velocities. This means we don't have the typical acceleration/deceleration "rounded trapezoid",
                // but two successive ramps both either accelerating or decelerating instead. Increasing/decreasing velocity will no longer  
                // generate a general upwards/downwards trend in displacement (or travel time). Instead, the segment 4 distance (that when 
                // negative, indicates an illegal solution) exhibits very pointy peaks at the entry/exit velocities and a "basin" local minimum 
                // in between. Peaks are formed by the fact that when the target velocity is at or near the entry/exit
                // velocity, there is effectively only one ramp, not two, and up to half the jerk time can be saved. 
                // Consequently, displacement during the segment will also be less and we might find a local solution, above the general trend.   
                // There can be two roots inside a region and we can no longer just assume the fixed upwards/downwards relationship for root 
                // finding in the region. We need to split the region into two halves at a point where segment 4 is negative to get the root 
                // finding property back.

                // Working  hypothesis (no math proof, just from observing many curves empirically and finding valid "physicality" of the observed behavior): 
                //
                //   a) the curve's minimum leans towards the higher absolute velocity side, that is because the saved jerk time is equal at both 
                //      peaks but during that time more displacement will happen at the higher speed, i.e. the peak is pointier.
                //
                //   b) the curve's second derivative is always > 0 i.e. always upward curved.
                // 
                // By a) we know we can start at the middle point and proceed into the upper absolute velocity half. 
                // By b) we know that the secant method will always fall short, if there is yet a solution to be found.  
                // If we detect the curve raising again, we're beyond the minimum and we know there is no solution.

                double vSearch0 = vPeak0;
                double vSearch1 = vPeak1;
                double vSearch;
                // Initial momentum towards higher absolute velocity.  
                double vSecant = sign > 0 ? vPeak0 : vPeak1;
                double sSecant = sign > 0 ? sResult0 : sResult1;
                double sResult;
                double tResult;
                vSearch = (vPeak0+vPeak1)*0.5;
                do {
                    computeProfile(vSearch, vEffEntry, vEffExit, tMin);
                    sResult = sign*(s[4] - s[3]);
                    tResult = time;
                    if (sResult < 0) {
                        // Great, we found it.
                        trace("    found invalid mid area "+vSearch+" s "+sResult+" t "+tResult);
                        break;
                    }
                    if (sResult > sSecant) {
                        // Raising result -> overshoot, this means there is no invalid section. 
                        trace("    overshot, no invalid mid area "+vSearch+" s "+sResult+" t "+tResult);
                        break;
                    }
                    // Apply secant method. 
                    double gradient = (sResult-sSecant)/(vSecant-vSearch);
                    if (Math.abs(gradient) < vttol) {
                        // Stuck in a local minimum. This must be a "tangent" situation, otherwise we should see overshoot.
                        // We simply don't support tangent cases.
                        trace("    stuck local minimum, no invalid mid area "+vSearch+" s "+sResult+" t "+tResult);
                        break;
                    }
                    double delta = -sResult/gradient;
                    // remember old values
                    vSecant = vSearch;
                    sSecant = sResult;
                    // Assign new value.
                    vSearch = Math.max(vSearch0, Math.min(vSearch1,  vSearch+delta));
                    trace("    search for invalid mid area "+vSearch+" gradient "+gradient+" delta "+delta+" s "+sResult+" t "+tResult);
                }
                while (true);
                if (sResult < 0) {
                    // We have found an invalid section, solve for two roots.
                    if (solveRegion(vPeak0, vSearch, sResult0, sResult, tResult0, tResult, vEffEntry, vEffExit, tMin, bestTime,
                            iterations, stol, vtol, ttol)) {
                        tResult = time;
                        if (tResult < bestTime) {
                            bestVelocity = v[4];
                            bestTime = tResult;
                        }
                    }
                    if (solveRegion(vSearch, vPeak1, sResult, sResult1, tResult, tResult1, vEffEntry, vEffExit, tMin, bestTime,
                            iterations, stol, vtol, ttol)) {
                        tResult = time;
                        if (tResult < bestTime) {
                            bestVelocity = v[4];
                            bestTime = tResult;
                        }
                    }
                }
                else {
                    // Only one root expected. 
                    if (solveRegion(vPeak0, vPeak1, sResult0, sResult1, tResult0, tResult1, vEffEntry, vEffExit, tMin, bestTime,
                            iterations, stol, vtol, ttol)) {
                        tResult = time;
                        if (tResult < bestTime) {
                            bestVelocity = v[4];
                            bestTime = tResult;
                        }
                    }
                }
            }
            else {
                if (solveRegion(vPeak0, vPeak1, sResult0, sResult1, tResult0, tResult1, vEffEntry, vEffExit, tMin, bestTime,
                        iterations, stol, vtol, ttol)) {
                    double tResult = time;
                    if (tResult < bestTime) {
                        bestVelocity = v[4];
                        bestTime = tResult;
                    }
                }
            }
        }
        trace("best velocity "+bestVelocity+" best time "+bestTime+" time-tMin "+(bestTime-tMin));
        if (bestVelocity != v[4]) {
            // re-establish best solution
            trace("  re-establish");
            computeProfile(bestVelocity, vEffEntry, vEffExit, tMin);
        }
        if (tMin > 0 && tMin != time) {
            if (Math.abs(time/tMin - 1) < 0.001) {
                // The solver may have slightly approximated. Stretch the profile into the exact minimum time. 
                if (retimeProfile()) {
                    trace("    retimed tMin "+tMin+" time "+time);
                }
                else {
                    trace("    not retimed tMin "+tMin+" time "+time);
                }
            }
            else {
                trace("    tMin not met");
            }
        }
        return true;
    }
    public boolean solveIfNullMove() {
        if (s[0] == s[segments]
                && v[0] == v[segments]
                        && (isConstantAcceleration() || a[0] == a[segments])
                        && (tMin == 0 || (v[0] == 0 && a[0] == 0))) {
            // Null move
            t[0] = 0;
            j[0] = 0;
            for (int i = 1; i < segments; i++) {
                s[i] = s[0];
                v[i] = v[0];
                a[i] = a[0];
                j[i] = j[0];
                t[i] = 0;
            }
            t[segments] = 0;
            t[segments+1] = 0;
            j[segments] = 0;
            t[4] = tMin;
            time = tMin;
            sBound0 = sBound1 = s[0];
            tSBound0 = 0;
            tSBound1 = tMin;
            vBound0 = vBound1 = v[0];
            tVBound0 = 0;
            tVBound1 = tMin;
            aBound0 = aBound1 = a[0];
            tABound0 = 0;
            tABound1 = tMin;
            return true; // -----------------> 
        }
        return false;
    }
    public double profileSignum(double vEffEntry, double vEffExit) {
        double signum = Math.signum(s[segments]-s[0]);
        if (signum == 0) {
            // Zero displacement.
            // if (! (hasOption(ProfileOption.UnconstrainedEntry) || hasOption(ProfileOption.UnconstrainedExit))) { ???
            // Take entry/exit velocity balance as criterion.
            if (Math.abs(-vEffEntry - vEffExit) > eps) {
                signum = Math.signum(-vEffEntry - vEffExit);
            }
            // TODO: if this is still zero we just assume it's completely symmetric, but this might not be true
            // if the V&a mix are not the same on entry/exit and by chance still cancel out in the effective speed. 
            // We would need to calculate the displacement to still-stand and compare. For now, computeProfile() has to cope.
            if (signum == 0) {
                trace("*** signum 0");
            }
        }
        return signum;
    }

    protected boolean solveRegion(double vPeak0, double vPeak1, 
            double sResult0, double sResult1, 
            double tResult0, double tResult1, 
            double vEffEntry, double vEffExit, 
            double tMin, double bestTime,
            final int iterations, final double stol, final double vtol, final double ttol) {

        trace("=== solveRegion("+vPeak0+" .. "+vPeak1+", s="+sResult0+" .. "+sResult1+", t="+tResult0+" .. "+tResult1+")");
        if (Math.min(tResult0,  tResult1) >= bestTime) {
            trace("region eclipsed by best t "+bestTime);
            return false;
        }
        if (bestTime == tMin) {
            trace("region eclipsed by best t == min t "+bestTime);
            return false;
        }

        double vSecant;  
        double sSecant;
        double tSecant;
        double vPeak;
        double sign;
        if (Math.abs(vPeak0) < Math.abs(vPeak1)) {
            vSecant = vPeak1;
            sSecant = sResult1;
            tSecant = tResult1;
            sign = 1;
        }
        else {
            vSecant = vPeak0;
            sSecant = sResult0;
            tSecant = tResult0;
            sign = -1;
        }

        // The first guess is the mid-point.
        vPeak = (vPeak1 + vPeak0)*0.5;

        int converging = 0;
        double vValid = Double.NaN;
        boolean hasValid = false;
        boolean sAscending = sResult0 < sResult1; 
        boolean tAscending = tResult0 < tResult1; 

        final double maxMagnitude = Math.min((vPeak1 - vPeak0)*0.00001, 1);

        for (int iter = 0; iter <= iterations; iter++) {

            // Compute the profile with the given velocity limit.
            computeProfile(vPeak, vEffEntry, vEffExit, tMin);

            double sResult = sign*(s[4] - s[3]);
            double tResult = time;
            trace("vPeak = "+vPeak+" s="+sResult+" t-tMin="+(time-tMin)+" "+this);
            double magnitude = Math.max(eps, Math.min(maxMagnitude, 0.0001*(Math.abs(s[3]-s[0])+Math.abs(s[segments]-s[4]))));
            if (Math.abs(vPeak - vSecant) < magnitude*vtol) {
                converging++;
            }
            else {
                converging = 0;
            }
            if (sResult >= -stol && tResult >= tMin-ttol) { 
                // The solution is valid, but it may not be optimal (yet).
                if (tResult < tMin + ttol || converging >= 2) {
                    // That's an optimal solution
                    trace("taken");
                    return true;
                }
                else {
                    // The solution can still turn out to be optimal after converging.  
                    vValid = vPeak;
                    hasValid = true;
                }
            }
            else if (converging >= 4) {
                // Search converged, the current solution is not valid.
                if (hasValid) {
                    // Restore the previously valid solution after converging (turns out there was not a better one).
                    computeProfile(vValid, vEffEntry, vEffExit, tMin);
                    // That's a solution
                    trace("taken previous valid solution after converging "+vValid);
                    return true;
                }
                else {
                    // The was no valid solution
                    trace("** giving up");
                    return false;
                }
            }
            if (sResult >= 0) {
                // Valid continuity, optimize by time
                if (tAscending ^ (tResult > tMin)) {
                    vPeak0 = vPeak;
                }
                else {
                    vPeak1 = vPeak;
                }
            }
            else {
                if (sAscending) {
                    vPeak0 = vPeak;
                }
                else {
                    vPeak1 = vPeak;
                }
            }
            // Remember the last data.
            vSecant = vPeak;
            sSecant = sResult;
            tSecant = tResult;
            vPeak = (vPeak0 + vPeak1)*0.5;

            //                            // TODO: Secant method...
            //                            double gradient = (terr-terrSecant)/(vPeak-vPeakSecant);
            //                            double delta = -terr/gradient;
            //                            if (Math.abs(delta) > vMax/4) {
            //                                delta = Math.signum(delta)*vMax/4;
            //                            }
            //                            trace("secant terr method: gradient="+gradient+" delta="+-terr/gradient+" vPeak="+(vPeak - terr/gradient));
            //                            vPeak += delta;
            //                            vPeak = Math.max(vPeak0+eps,  Math.min(vPeak1, vPeak));
        }
        return false;
    }


    /**
     * Recalculate the profile to make sure it takes the given amount of time.
     * @param newTime
     */
    private boolean retimeProfile() {
        if (solveIfNullMove()) {
            return true;
        }
        else if (tMin != time && tMin > 0.0 && time > 0) {
            // The derivatives need to be scaled to the power of the order of the derivative.
            double vFactor = time/tMin; 
            double aFactor = vFactor*vFactor;
            double jFactor = aFactor*vFactor;
            for (int i = 0; i <= segments; i++) {
                t[i] /= vFactor;
                v[i] *= vFactor;
                a[i] *= aFactor;
                j[i] *= jFactor;
            }
            t[segments+1] /= vFactor;
            time = tMin;
            computeBounds();
            return true;
        }
        return false;
    }

    public boolean assertSolved() {
        if (!hasOption(ProfileOption.Solved)) {
            solve();
            return true;
        }
        return false;
    }

    public static void coordinateProfiles(MotionProfile [] profiles) {
        // Find the lead profile i.e. the one axis moving the most.
        MotionProfile leadProfile = null;
        double bestDist = Double.NEGATIVE_INFINITY;
        for (MotionProfile profile : profiles) { 
            double dist = Math.abs(profile.s[segments]-profile.s[0]);
            if (dist > bestDist) {
                leadProfile = profile;
                bestDist = dist;
            }
        }
        if (leadProfile != null) {
            leadProfile.assertSolved();
            coordinateProfilesToLead(profiles, leadProfile);
        }
    }

    public static void coordinateProfilesToLead(MotionProfile[] profiles, MotionProfile leadProfile) {
        double leadDist = leadProfile.s[segments]-leadProfile.s[0];
        for (MotionProfile profile : profiles) { 
            if (profile != leadProfile) {
                double factor; 
                if (leadDist == 0) {
                    factor = 0;
                }
                else {
                    factor = (profile.s[segments]-profile.s[0])/leadDist;
                }
                if (factor == 0) {
                    for (int i = 0; i <= segments; i++) {
                        profile.t[i] = 0;
                        profile.s[i] = profile.s[0];
                        profile.v[i] = 0;
                        profile.a[i] = 0;
                        profile.j[i] = 0;
                    }
                    profile.t[4] = leadProfile.time;
                }
                else {
                    for (int i = 0; i <= segments; i++) {
                        profile.t[i] = leadProfile.t[i];
                        profile.s[i] = (leadProfile.s[i] - leadProfile.s[0])*factor + profile.s[0];
                        profile.v[i] = leadProfile.v[i]*factor;
                        profile.a[i] = leadProfile.a[i]*factor;
                        profile.j[i] = leadProfile.j[i]*factor;
                    }
                }
                profile.time = leadProfile.time;
                profile.tMin = leadProfile.tMin;
                profile.computeBounds();
                // Treat as if solved.
                profile.eval = 0;
                profile.solvingTime = 0;
                profile.setOption(ProfileOption.Solved);
            }
        }
    }

    public static void synchronizeProfiles(MotionProfile [] profiles) {
        // Find the maximum time.
        double maxTime = 0;
        for (MotionProfile profile : profiles) {
            profile.assertSolved();
            if (profile.time > maxTime) {
                maxTime = profile.time;
                trace("    max time "+maxTime+" from "+profile);
            }
        }
        // Re-time the others.
        boolean restart;
        do {
            restart = false;
            for (MotionProfile profile : profiles) {
                profile.tMin = maxTime;
                if (profile.time != maxTime) {
                    if (profile.hasOption(ProfileOption.SynchronizeStraighten) 
                            && profile.v[0] == 0 
                            && profile.v[segments] == 0 
                            && (profile.isConstantAcceleration() || (profile.a[0] == 0 && profile.a[segments] == 0))) {
                        // Profile has no continuous entry/exit velocity acceleration, we can straighten it.
                        profile.retimeProfile();
                    }
                    else if (profile.hasOption(ProfileOption.SynchronizeEarlyBird) 
                            && profile.v[segments] == 0 
                            && (profile.isConstantAcceleration() || profile.a[segments] == 0)) {
                        // We just leave the profile as is. The axis will move to the target early and then stay put.
                        profile.t[segments+1] += profile.tMin - profile.time;
                        profile.time = profile.tMin;
                    }
                    else if (profile.hasOption(ProfileOption.SynchronizeLastMinute) 
                            && profile.v[0] == 0 
                            && (profile.isConstantAcceleration() || profile.a[0] == 0)) {
                        // We just leave the profile as is. The axis will move to the target early and then stay put.
                        profile.t[0] += profile.tMin - profile.time;
                        profile.time = profile.tMin;
                    }
                    else {
                        // None of the simple solutions applicable. Re-solve with tMin.
                        profile.solve();
                        if (profile.time > maxTime) {
                            // Sometimes the solution was at/near entry/exit speeds and in these cases, it is possible
                            // that the new tMin is impossible, i.e. more time is needed. 
                            // --> restart the process.
                            trace("    need to restart synchronize, maxTime "+maxTime+" breached with "+profile.time+" on "+profile);
                            maxTime = profile.time;
                            restart = true;
                            break;
                        }
                    }
                }
            }
        }
        while (restart);
    }


    public void solveByExpansion(double signum, boolean expandEntry, boolean expandExit) {
        computeProfile(signum*vMax, getEffectiveEntryVelocity(jMax), getEffectiveExitVelocity(jMax), tMin);
        if (expandEntry) {
            double overlap = expandExit ? 0 : signum*Math.max(0, signum*(s[0]-s[4]));
            double entryExpansion = s[3]-s[0] + overlap;
            s[0] -= entryExpansion;
            s[1] -= entryExpansion;
            s[2] -= entryExpansion;
            s[3] -= entryExpansion;
        }
        if (expandExit) {
            double overlap = expandEntry ? 0 : signum*Math.max(0, signum*(s[3]-s[segments]));
            double exitExpansion = s[segments]-s[4] + overlap;
            s[7] += exitExpansion;
            s[6] += exitExpansion;
            s[5] += exitExpansion;
            s[4] += exitExpansion;
        }

        computeTime(tMin);
        computeBounds();
        setOption(ProfileOption.Solved);
    }

    /**
     * Extract the (fractional) segments from solvedProfile between t0 and t1 and save them in this profile. 
     * The displacement between t0 and t1 must match s[0] and s[7] of this profile i.e. the t0 and t7
     * must have been obtained by computing crossing times for s[0] and s[7] on solvedProfile.
     * 
     * @param solvedProfile  
     * @param t0
     * @param t7
     */
    public void extractProfileSectionFrom(MotionProfile solvedProfile, double t0, double t7) {
        if (t0 == 0 && t7 == solvedProfile.time) {
            // Full 1:1 profile extraction.
            copyProfileSolution(solvedProfile);
        }
        else {
            // Extract a partial profile.
            // Assert the given t0 t1 match in location.
            assert Math.abs(solvedProfile.getMomentaryLocation(t0)-s[0]) < eps;
            assert Math.abs(solvedProfile.getMomentaryLocation(t7)-s[7]) < eps;
            // Assert we're not in a stretched profile.
            assert t[0] == 0;
            assert t[segments+1] == 0;
            // Get border values.
            double v0 = solvedProfile.getMomentaryVelocity(t0);
            double a0 = solvedProfile.getMomentaryAcceleration(t0);
            //double j0 = solvedProfile.getMomentaryJerk(t0);
            double v7 = solvedProfile.getMomentaryVelocity(t7);
            double a7 = solvedProfile.getMomentaryAcceleration(t7);
            //double j7 = solvedProfile.getMomentaryJerk(t7);
            double tSeg = 0;
            double tEntrySlack = 0;
            double tExitSlack = 0;
            for (int seg = 0; seg <= segments; seg++) {
                double tSegEnd = tSeg + solvedProfile.t[seg]; 
                if (tSegEnd <= t0) {
                    // Segment ends before the in cut. 
                    s[seg] = s[0];
                    v[seg] = v0;
                    a[seg] = solvedProfile.isConstantAcceleration() ? solvedProfile.a[seg] : a0; // jumps
                    j[seg] = solvedProfile.j[seg]; // jumps
                    t[seg] = 0;
                    if (seg == 4) {
                        tEntrySlack += solvedProfile.t[seg];
                    }
                }
                else if (tSegEnd >= t7) {
                    // Segment ends after the out cut. 
                    s[seg] = s[7];
                    v[seg] = v7;
                    a[seg] = a7;
                    // The beginning and/or the end of the segment may overlap.
                    t[seg] = Math.max(solvedProfile.t[seg] - Math.max(0, t0 - tSeg) - (tSegEnd - t7), 0);
                    j[seg] = 0;
                    if (seg == 4) {
                        tExitSlack += solvedProfile.t[seg] - t[seg];
                    }
                }
                else {
                    // Segments ends within cut. 
                    s[seg] = solvedProfile.s[seg];
                    v[seg] = solvedProfile.v[seg];
                    a[seg] = solvedProfile.a[seg];
                    j[seg] = solvedProfile.j[seg];
                    // Only a part of the segment may be in the cut.
                    t[seg] = solvedProfile.t[seg] - Math.max(0, t0 - tSeg);
                    if (seg == 4) {
                        tEntrySlack += solvedProfile.t[seg] - t[seg];
                    }
                }
                tSeg = tSegEnd;
            }
            time = t7 - t0;
            computeBounds();
            setOption(ProfileOption.Solved);
            if (t0 == 0) {
                // This is extracted at the beginning, inherit unconstrained entry option. 
                if (solvedProfile.hasOption(ProfileOption.UnconstrainedEntry)) {
                    setOption(ProfileOption.UnconstrainedEntry);
                }
            }
            else {
                setOption(ProfileOption.CroppedEntry);
                double sSlack = solvedProfile.v[4]*tEntrySlack;
                sEntryControl = solvedProfile.s[0] + sSlack;
                tEntryControl = t0 - tEntrySlack; 
            }
            if (t7 == solvedProfile.time) {
                // This is extracted at the end, inherit unconstrained exit option. 
                if (solvedProfile.hasOption(ProfileOption.UnconstrainedExit)) {
                    setOption(ProfileOption.UnconstrainedExit);
                }
            }
            else {
                setOption(ProfileOption.CroppedExit);
                double sSlack = solvedProfile.v[4]*tExitSlack;
                sExitControl = solvedProfile.s[segments] - sSlack;
                tExitControl = solvedProfile.time - t7 - tExitSlack;
            }
        }
    }

    /**
     * Copy the profile solution from the template. This is not a full copy, i.e. constraints etc. are not copied.
     * 
     * @param template
     */
    public void copyProfileSolution(MotionProfile template) {
        for (int seg = 0; seg <= segments; seg++) {
            s[seg] = template.s[seg];
            v[seg] = template.v[seg];
            a[seg] = template.a[seg];
            j[seg] = template.j[seg];
            t[seg] = template.t[seg];
        }
        time = template.time;

        sBound0 = template.sBound0;
        sBound1 = template.sBound1;
        tSBound0 = template.tSBound0;
        tSBound1 = template.tSBound1;

        vBound0 = template.vBound0;
        vBound1 = template.vBound1;
        tVBound0 = template.tVBound0;
        tVBound1 = template.tVBound1;

        aBound0 = template.aBound0;
        aBound1 = template.aBound1;
        tABound0 = template.tABound0;
        tABound1 = template.tABound1;

        // Only flags important to the solution are copied.
        if (template.hasOption(ProfileOption.UnconstrainedEntry)) {
            setOption(ProfileOption.UnconstrainedEntry);
        }
        if (template.hasOption(ProfileOption.UnconstrainedExit)) {
            setOption(ProfileOption.UnconstrainedExit);
        }
        if (template.hasOption(ProfileOption.Solved)) {
            setOption(ProfileOption.Solved);
        }
    }

    public static int getLeadAxisIndex(double[] vector) {
        double d = 0;
        int lead = 0;
        for (int i = 0; i < vector.length; i++) {
            if (Math.abs(vector[i]) > d) {
                // Note, we know at least one will be > 0 otherwise the dot product would be 0. 
                d = Math.abs(vector[i]);
                lead = i;
            }
        }
        return lead;
    }

    public static double dotProduct(double[] unitVector1, double[] unitVector2) {
        double dot = 0;
        for (int i = 0; i < unitVector1.length; i++) {
            dot += unitVector1[i]*unitVector2[i];
        }
        return dot;
    }

    public static double[] getUnitVector(MotionProfile[] profile) {
        double [] unitVector;
        unitVector = new double[profile.length];
        double sumSq = 0;
        for (int i = 0; i < profile.length; i++) {
            double d;
            unitVector[i] = d = profile[i].s[segments] - profile[i].s[0];
            sumSq += d*d;
        }
        double n = Math.sqrt(sumSq);
        if (n > 0) { 
            for (int i = 0; i < profile.length; i++) {

                unitVector[i] /= n;
            }
        }
        else {
            for (int i = 0; i < profile.length; i++) {
                unitVector[i] = 0;
            }   
        }
        return unitVector;
    }

    public static boolean isCoordinated(MotionProfile[] profile) {
        if (profile.length > 0) {
            return profile[0].hasOption(ProfileOption.Coordinated);
        }
        return false;
    }

    public void computeProfile(double vPeak, double vEffEntry, double vEffExit, double tMin) {

        // Compare effective entry/exit velocities to vMax to know whether we need to accelerate or decelerate on entry/exit.
        double signumEntry, signumExit;
        signumEntry = Math.signum(vPeak - vEffEntry);
        signumExit = Math.signum(vPeak - vEffExit);
        if (signumEntry == 0) {
            signumEntry = (signumExit == 0.0 ? 1.0 : signumExit);
        }
        if (signumExit == 0) {
            signumExit = signumEntry;
        }
        double dVEntry =  (vPeak - v[0]);
        double dVExit =  (v[7] - vPeak);

        // Delete any waits before/after.
        t[0] = 0;
        t[segments+1] = 0;

        if (isConstantAcceleration()) {
            // Use a simple constant acceleration profile.
            j[0] = 0;
            // Acceleration jumps right up so we need to change a[0]
            a[0] = signumEntry*aMaxEntry;
            double tAccelEntry = dVEntry/a[0];
            t[1] = 0;
            v[1] = v[0];
            s[1] = s[0]; 

            t[2] = tAccelEntry;
            j[2] = 0;
            a[1] = a[0];
            v[2] = vPeak;
            s[2] = s[1] + v[1]*t[2] + 1./2*a[1]*Math.pow(t[2], 2);

            t[3] = 0;
            j[3] = 0;
            a[2] = a[1];
            v[3] = vPeak;
            s[3] = s[2]; 
            a[3] = 0;

            // Reverse exit ramp.
            j[6] = 0;
            a[6] = -signumExit*aMaxExit;
            double tAccelExit = dVExit/a[6];
            t[7] = 0;
            v[6] = v[7];
            s[6] = s[7]; 

            t[6] = tAccelExit;
            j[5] = 0;
            a[5] = a[6];
            v[5] = vPeak;
            s[5] = s[6] - v[6]*t[6] + 1./2*a[6]*Math.pow(t[6], 2);

            t[5] = 0;
            j[4] = 0;
            a[4] = a[5];
            v[4] = vPeak;
            s[4] = s[5];
        }
        else if (hasOption(ProfileOption.SimplifiedSCurve)) {
            // We must limit acceleration on very short moves then apply jerk for equivalent avg. constant acceleration.
            double aMaxEntry = this.aMaxEntry;
            if (Math.abs(dVEntry) < eps) {
                aMaxEntry = 0;
                j[0] = signumEntry*jMax;
            }
            else {
                if (signumEntry*aMaxEntry*aMaxEntry/dVEntry > jMax) {
                    // For very low velocity deltas the acceleration must be limited, lest the jMax is violated.
                    aMaxEntry = Math.sqrt(Math.abs(dVEntry*jMax));
                }
                // 
                j[0] = aMaxEntry*aMaxEntry/dVEntry;
            }

            a[0] = 0; // nothing else is supported

            a[1] = signumEntry*aMaxEntry;
            t[1] = a[1]/j[0];
            s[1] = s[0] + v[0]*t[1] + 1./6*j[0]*Math.pow(t[1], 3); 
            v[1] = v[0] + 1./2*j[0]*Math.pow(t[1], 2);

            j[1] = 0;
            t[2] = 0;
            a[2] = a[1];
            v[2] = v[1];
            s[2] = s[1];

            j[2] = -j[0];
            t[3] = t[1];
            a[3] = 0;
            v[3] = vPeak;
            s[3] = s[2] + v[2]*t[3] + 1./2*a[2]*Math.pow(t[3], 2) + 1./6*j[2]*Math.pow(t[3], 3); 

            // Reverse exit ramp.
            double aMaxExit = this.aMaxExit;
            if (Math.abs(dVExit) < eps) {
                aMaxExit = 0;
                j[6] = signumExit*jMax;
            }
            else  {
                if (-signumExit*aMaxExit*aMaxExit/dVExit > jMax) {
                    // For very low velocity deltas the acceleration must be limited, lest the jMax is violated.
                    aMaxExit = Math.sqrt(Math.abs(dVExit*jMax));
                }
                j[6] = -aMaxExit*aMaxExit/dVExit;
            }

            a[7] = 0; // nothing else is supported
            a[6] = -signumExit*aMaxExit;
            t[7] = -a[6]/j[6];
            v[6] = v[7] + 1./2*j[6]*Math.pow(t[7], 2);
            s[6] = s[7] - v[7]*t[7] - 1./6*j[6]*Math.pow(t[7], 3);

            j[5] = 0;
            t[6] = 0;
            a[5] = a[6];
            v[5] = v[6];
            s[5] = s[6];

            j[4] = -j[6];
            t[5] = t[7];
            a[4] = 0;
            v[4] = vPeak;
            s[4] = s[5] - v[5]*t[5] + 1./2*a[5]*Math.pow(t[5], 2) - 1./6*j[4]*Math.pow(t[5], 3); 
        }
        else {
            // 3rd order profile.
            if (!hasOption(ProfileOption.UnconstrainedEntry)) {
                // Need to determine, if this profile has a constant acceleration segment or not. The latter happens
                // if the velocity is too low to reach the acceleration limit. In this case the profile will switch 
                // directly from positive to negative jerk, a.k.a. "S-curve" and it will not reach aMax.   
                // In order to calculate this, we need to get rid of entry/exit acceleration.

                // On entry: pretend to accelerate from a = 0, move t[1] backward/forward in time.
                double j0 = signumEntry*jMax;
                double t0early = a[0]/j0;
                // What is the velocity at that point?
                double v0early = v[0] - a[0]*t0early + 1./2*j0*Math.pow(t0early, 2);
                // Half time to reach velocity with S-curve
                double t1 = Math.sqrt(Math.max(0, (vPeak - v0early)/j0));
                // Acceleration at S-Curve inflection point.
                double a1 = t1*j0;
                // If the acceleration is smaller than the limit, we have an S-curve.
                double aMaxEntry = this.aMaxEntry;
                if (Math.abs(a1) < aMaxEntry) {
                    // This is an S curve. 
                    aMaxEntry = Math.abs(a1);
                }

                // Phase 1: Jerk to acceleration.
                j[0] = j0;
                t[1] = Math.max(0, signumEntry*aMaxEntry/j0 - t0early); // can be cropped by tearly
                a[1] = a[0] + j[0]*t[1];
                s[1] = s[0] + v[0]*t[1] + 1./2*a[0]*Math.pow(t[1], 2) + 1./6*j[0]*Math.pow(t[1], 3); 
                v[1] = v[0] + a[0]*t[1]+ 1./2*j[0]*Math.pow(t[1], 2);

                // Phase 2: Constant acceleration. 
                a[2] = a[1];
                j[1] = 0;
                j[2] = -j0;              // Phase 3 look-ahead
                t[3] = (0 - a[2])/j[2];  // Phase 3 look-ahead
                v[2] = vPeak + 1./2*j[2]*Math.pow(t[3], 2);
                t[2] = (a[2] == 0 ? 0.0 : (v[2] - v[1])/a[2]);
                s[2] =  s[1] + v[1]*t[2] + 1./2*a[1]*Math.pow(t[2], 2);

                // Phase 3: Negative jerk to constant velocity/zero acceleration.
                v[3] = vPeak;
                a[3] = 0;
                s[3] = s[2] + v[2]*t[3] + 1./2*a[2]*Math.pow(t[3], 2) + 1./6*j[2]*Math.pow(t[3], 3); 

            }

            // Phase 4: Constant velocity
            j[3] = 0;
            // s and t ... needs to be postponed

            if (!hasOption(ProfileOption.UnconstrainedExit)) {
                v[4] = vPeak;
                a[4] = 0;

                // Need to determine, if this profile has a constant acceleration segment or not (see analogous above). 
                // On exit: pretend to decelerate to a = 0, move t[7] forward/backward in time.
                double j6 = signumExit*jMax;
                double t7late = -a[7]/j6;
                // What is the velocity at that point?
                double v7late = v[7] + a[7]*t7late + 1./2*j6*Math.pow(t7late, 2);
                // Half time to reach velocity with S-curve
                double t6 = Math.sqrt(Math.max(0, (vPeak - v7late)/j6));
                // Acceleration at S-Curve inflection point.
                double a6 = -t6*j6;
                // If the acceleration is smaller than the limit, we have an S-curve.
                double aMaxExit = this.aMaxExit;
                if (Math.abs(a6) < aMaxExit) {
                    // This is an S curve. 
                    aMaxExit = Math.abs(a6);
                }

                // Phase 5: Negative jerk to deceleration.
                // TODO: reorder to reverse ramp as above. 
                j[4] = -j6;

                j[6] = j6;                  // Phase 7 look-ahead
                t[7] = Math.max(0, signumExit*aMaxExit/j[6] - t7late); // can be cropped by tlate
                a[6] = a[7] - j[6]*t[7];    // Phase 6 look-ahead

                a[5] = a[6];
                t[5] = a[5]/j[4];
                v[5] = v[4] + 1./2*j[4]*Math.pow(t[5], 2);
                // s ... needs to be postponed

                // Phase 6: Constant deceleration. 
                j[5] = 0;
                v[6] = v[7] - a[7]*t[7] + 1./2*j[6]*Math.pow(t[7], 2);  
                t[6] = (a[6] == 0 ? 0.0 : (v[6] - v[5])/a[6]);
                // s ... needs to be postponed

                // Phase 7: Jerk to exit acceleration.
                // ... already looked ahead.

                // reverse s calculation
                s[6] = s[7] - v[7]*t[7] + 1./2*a[7]*Math.pow(t[7], 2) - 1./6*j[6]*Math.pow(t[7], 3); 
                s[5] = s[6] - v[6]*t[6] + 1./2*a[6]*Math.pow(t[6], 2); 
                s[4] = s[5] - v[5]*t[5] + 1./2*a[5]*Math.pow(t[5], 2) - 1./6*j[4]*Math.pow(t[5], 3);
            }
        }
        // Unconstrained half-sided profile, i.e. it will accelerate towards the opposite location
        // and cruise freely through it, only constrained by distance.
        if (hasOption(ProfileOption.UnconstrainedEntry)) {

            s[3] = s[0];
            v[3] = v[4];
            a[3] = 0;
            j[3] = 0;
            t[3] = 0;

            s[2] = s[0];
            v[2] = v[4];
            a[2] = 0;
            j[2] = 0;
            t[2] = 0;

            s[1] = s[0];
            v[1] = v[4];
            a[1] = 0;
            j[1] = 0;
            t[1] = 0;

            v[0] = v[4];
            a[0] = 0;
            j[0] = 0;
        }
        if (hasOption(ProfileOption.UnconstrainedExit)) {

            s[4] = s[7];
            v[4] = v[3];
            a[4] = 0;
            j[4] = 0;

            s[5] = s[7];
            v[5] = v[3];
            a[5] = 0;
            j[5] = 0;
            t[5] = 0;

            s[6] = s[7];
            v[6] = v[3];
            a[6] = 0;
            j[6] = 0;
            t[6] = 0;

            v[7] = v[3];
            a[7] = 0;
            j[7] = 0;
            t[7] = 0;
        }

        if (tMin > 0 && v[4] == 0.0   
                && s[4] != s[3] && Math.abs(s[4] - s[3]) < vtol*5*tMin 
                && !hasOption(ProfileOption.SimplifiedSCurve)) {
            // Minimum time, zero velocity move and ramps almost but not quite meet. Try to mend it by capping the entry/exit acceleration. 
            if (j[0] != 0 && j[1] == 0) {
                // Third order solution needed. By symmetry we use the average velocity and determine the needed 
                // lengthening dt. 
                double t2 = t[2];
                double vm = v[1] + a[1]*t2*0.5;
                double dt = (s[4] - s[3])/vm;
                if (dt > 0 && dt < tMin) {
                    double js = j[0];
                    double as = a[2];
                    // Now solve for the time t that the capped acceleration takes place. 

                    /* Sage Math            
var('a2 t j t2 dt')
dv=a2*t-j*((t-t2)/2)^2  # Velocity difference with symmetrical jerk 
a=a2-j*(t-t2)/2         # Acceleration at the jerk tangent is the capped acceleration
eq=(dt==dv/a-t)         # The time difference dt with the capped acceleration vs. the original
solve(eq, t)            # Solve for t

                >>
[t == -(dt*j + sqrt(dt^2*j^2 + 2*dt*j^2*t2 + j^2*t2^2 + 4*a2*dt*j))/j, 
 t == -(dt*j - sqrt(dt^2*j^2 + 2*dt*j^2*t2 + j^2*t2^2 + 4*a2*dt*j))/j]
                     */
                    double sqrtTerm = Math.sqrt(Math.pow(dt, 2)*Math.pow(js, 2) + 2*dt*Math.pow(js, 2)*t2 + Math.pow(js, 2)*Math.pow(t2, 2) + 4*as*dt*js);
                    double[] tS = new double[] { 
                            -(dt*js + sqrtTerm)/js,
                            -(dt*js - sqrtTerm)/js 
                    };
                    for (double ts : tS) {
                        double dth = (ts-t2)/2;
                        if (dth > 0 && dth < t[1]+eps && dth < t[3]+eps) {
                            // Phase 1
                            t[1] -= dth;
                            a[1] = a[0] + j[0]*t[1];
                            v[1] = v[0] + a[0]*t[1] + 1./2*j[0]*Math.pow(t[1], 2);
                            s[1] = s[0] + v[0]*t[1] + 1./2*a[0]*Math.pow(t[1], 2) + 1./6*j[0]*Math.pow(t[1], 3); 

                            // Phase 3 backward
                            t[3] -= dth;
                            s[3] = s[4]; // mend it
                            a[2] = a[3] - j[2]*t[3];
                            v[2] = v[3] - a[3]*t[3] + 1./2*j[2]*Math.pow(t[3], 2);
                            s[2] = s[3] - v[3]*t[3] + 1./2*a[3]*Math.pow(t[3], 2) - 1./6*j[2]*Math.pow(t[3], 3);

                            // Time to mend.
                            t[2] = (v[2] - v[1])/a[1];
                        }
                    }
                }
            }
            else if (isConstantAcceleration()) {
                // Constant acceleration solution.
                double ts = t[1] + t[2] + t[3];
                if (ts > eps) {
                    double vm = (s[3] - s[0])/ts;
                    double dt = (s[4] - s[3])/vm;
                    if (dt > 0 && dt < tMin) {
                        ts += dt;
                        double as = (v[3] - v[0])/ts;
                        a[0] = as;
                        t[1] = 0;
                        a[1] = as;
                        v[1] = v[0];
                        s[1] = s[0];

                        t[2] = ts;
                        a[2] = as;
                        v[2] = v[3]; 
                        s[2] = s[4]; // mend it

                        t[3] = 0;
                        a[3] = 0;
                        s[3] = s[4]; // mend it 
                    }
                }
            }
            if (s[4] != s[3]) {
                // Not mended in the entry ramp, try the exit ramp.
                if (j[6] != 0 && j[5] == 0) {
                    // Third order solution needed.
                    double t6 = t[6];
                    double vm = v[5] + a[5]*t6*0.5;
                    double dt = (s[4] - s[3])/vm;
                    if (dt > 0 && dt < tMin) {
                        double js = j[6];
                        double as = -a[6];
                        double sqrtTerm = Math.sqrt(Math.pow(dt, 2)*Math.pow(js, 2) + 2*dt*Math.pow(js, 2)*t6 + Math.pow(js, 2)*Math.pow(t6, 2) + 4*as*dt*js);
                        double[] tS = new double[] { 
                                -(dt*js + sqrtTerm)/js,
                                -(dt*js - sqrtTerm)/js
                        };
                        for (double ts : tS) {
                            double dth = (ts-t6)/2;
                            if (dth > 0 && dth < t[7]+eps && dth < t[5]+eps) {
                                // Phase 7 backward
                                t[7] -= dth;
                                a[6] = a[7] - j[6]*t[7];
                                v[6] = v[7] - a[7]*t[7] + 1./2*j[6]*Math.pow(t[7], 2);
                                s[6] = s[7] - v[7]*t[7] + 1./2*a[7]*Math.pow(t[7], 2) - 1./6*j[6]*Math.pow(t[7], 3); 

                                // Phase 5
                                t[5] -= dth;
                                s[4] = s[3]; // mend it
                                a[5] = a[4] + j[4]*t[5];
                                v[5] = v[4] + a[4]*t[5] + 1./2*j[4]*Math.pow(t[5], 2);
                                s[5] = s[4] + v[4]*t[5] + 1./2*a[4]*Math.pow(t[5], 2) + 1./6*j[4]*Math.pow(t[5], 3);

                                // Time to mend.
                                t[6] = (v[6] - v[5])/a[5];
                            }
                        }
                    }
                }
                else if (isConstantAcceleration()) {
                    // Constant acceleration solution.
                    double ts = t[7] + t[6] + t[5];
                    if (ts > eps) {
                        double vm = (s[7] - s[4])/ts;
                        double dt = (s[4] - s[3])/vm;
                        if (dt > 0 && dt < tMin) {
                            ts += dt;
                            double as = (v[7] - v[4])/ts;
                            t[7] = 0;
                            a[6] = as;
                            v[6] = v[7];
                            s[6] = s[7];

                            t[6] = ts;
                            a[5] = as;
                            v[5] = v[4]; 
                            s[5] = s[3]; // mend it

                            t[5] = 0;
                            a[4] = as;
                            s[4] = s[3]; // mend it 
                        }
                    }
                }
            }
        }
        if (!(hasOption(ProfileOption.UnconstrainedEntry)
                ||hasOption(ProfileOption.UnconstrainedExit)
                ||hasOption(ProfileOption.SimplifiedSCurve)
                ||isConstantAcceleration())) {
            // This is a regular 3rd order profile. 
            if (Math.signum(j[2]) != Math.signum(j[4])) {
                // We have a sign reversal ramp, i.e. both accelerate or decelerate.  
                double tOverlap = 0;
                if (v[4] == 0.0 && Math.abs(s[3] - s[4]) < eps) {
                    // Velocity and s diff zero. Take up the whole jerk time.
                    t[4] = 0;
                    tOverlap = Math.min(t[3], t[5]);
                    if (tMin > 0) {
                        time = Arrays.stream(t).sum();
                        // Restrict to minimum time violation.
                        tOverlap = Math.min(tOverlap, time - tMin);
                    }
                }
                else {
                    // Velocity not zero, take s overlap time.
                    t[4] = (s[4] - s[3])/v[4];
                    tOverlap = -t[4];
                }
                if (tOverlap > 0 && Double.isFinite(tOverlap)) {
                    // tOverlap > 0 means that s overlaps in the middle, i.e. the profile is invalid. This may happen due to the jerk phases in the middle that 
                    // work against each other in sign reversal ramps.  
                    // We need to fuse the ramp together, i.e. create a constant acceleration segment instead of the two jerk phases to/from constant velocity.
                    // We can deduce from symmetry, that the time spent with constant acceleration is half the time spend with two jerk phases to constant velocity.
                    // So to eliminate the s overlap we must eliminate twice the overlap time. Therefore, we remove tOverlap from both jerk phases.  
                    if (t[3]+eps > tOverlap && t[5]+eps > tOverlap) {
                        t[3] -= tOverlap;
                        j[3] = 0;
                        a[3] = a[2] + j[2]*t[3];
                        v[3] = v[2] + a[2]*t[3] + 1./2*j[2]*Math.pow(t[3], 2);
                        s[3] = s[2] + v[2]*t[3] + 1./2*a[2]*Math.pow(t[3], 2) + 1./6*j[2]*Math.pow(t[3], 3);

                        t[4] = tOverlap;
                        a[4] = a[3];
                        v[4] = v[3] + a[3]*t[4];
                        s[4] = s[3] + v[3]*t[4] + 1./2*a[3]*Math.pow(t[4], 2);

                        t[5] -= tOverlap;
                    }
                }
            }
        }

        computeTime(tMin);
        computeBounds();
        eval++;
    }

    protected void computeTime(double tMin) {
        boolean adjustMinTime = false;
        if (a[3] == 0) { 
            // Not a fused profile, calculate the cruising time.
            if (v[4] == 0.0 && Math.abs(s[3] - s[4]) < eps) {
                t[4] = 0;
                adjustMinTime = true;
            }
            else {
                t[4] = (s[4] - s[3])/v[4];
            }
        }
        time = Arrays.stream(t).sum();
        if (adjustMinTime && (tMin > time)) {
            // Zero velocity profile -> can adapt minimum time directly 
            t[4] = tMin - time;
            time = tMin;
        }
    }

    public Double getForwardCrossingTime(double sCross, boolean halfProfile) {
        double tSeg = t[0];
        if (halfProfile) {
            // Check if we're beyond half the profile anyway.
            double signum = Math.signum(v[4]);
            if (signum*sCross >= signum*s[3]) {
                for (int i = 1; i <= 3; i++) {
                    tSeg += t[i];
                }
                return tSeg;
            }
        }
        for (int i = 1; i <= (halfProfile ? 3 : segments); i++) {
            Double ts = getSegmentCrossingTime(sCross, tSeg, i, true);
            if (ts != null) {
                return ts;
            }
            tSeg += t[i];
        }
        return null;
    }

    public Double getBackwardCrossingTime(double sCross, boolean halfProfile) {
        double tSeg = time - t[segments+1];
        if (halfProfile) {
            // Check if we're beyond half the profile anyway.
            double signum = Math.signum(v[4]);
            if (signum*sCross <= signum*s[4]) {
                for (int i = segments; i > 4; i--) {
                    tSeg -= t[i];
                }
                return tSeg;
            }
        }
        for (int i = segments; i >= (halfProfile ? 4 : 1); i--) {
            tSeg -= t[i];
            Double ts = getSegmentCrossingTime(sCross, tSeg, i, false);
            if (ts != null) {
                return ts;
            }
        }
        return null;
    }

    protected Double getSegmentCrossingTime(double sCross, double tSeg, int i, boolean forward) {
        double ti = t[i];
        double j = this.j[i-1];

        if (!forward && Math.abs(s[i] - sCross) < eps) {
            // Match, make sure to get exactly the time, if last segment. 
            if (i == segments) {
                return time;
            }
            else {
                return tSeg + ti;
            }
        }
        else if (Math.abs(s[i-1] - sCross) < eps) {
            // Match, make sure to return the exactly 0.0, if first segment. 
            if (i == 1) {
                return 0.0;
            }
            else {
                return tSeg;
            }
        }
        else if (forward && Math.abs(s[i] - sCross) < eps) {
            // Match, make sure to get exactly the time, if last segment. 
            if (i == segments) {
                return time;
            }
            else {
                return tSeg + ti;
            }
        }
        else if (j != 0) {
            // Has Jerk, uh-oh, complicated!
            double s0 = s[i-1];
            double ds = sCross - s0;
            double v0 = v[i-1];
            double a0 = a[i-1];
            double a02 = a0*a0;
            // First find the roots of the velocity (V == 0), these are the potential extremes of the 
            // curvature of the path. 
            // These give us interval boundaries, where we can search for the third order roots. 
            // [t == -(a0 + sqrt(a0^2 - 2*j*v0))/j, t == -(a0 - sqrt(a0^2 - 2*j*v0))/j]
            double sTerm = Math.sqrt(a02 - 2*j*v0);
            double ti0 = 0;
            double ti1 = Math.max(ti0, Math.min(ti, -(a0 + sTerm)/j));
            double ti2 = Math.max(ti0, Math.min(ti, -(a0 - sTerm)/j));
            // Now treat each interval and solve for roots numerically. 
            Function<Double, Double> f = (t) -> (-ds + v0*t + 1./2*a0*Math.pow(t, 2) + 1./6*j*Math.pow(t, 3)); 
            Function<Double, Double> g = (t) -> (v0 + a0*t + 1./2*j*Math.pow(t, 2));
            if (ti1 > ti2) {
                // swap
                double tmp = ti2;
                ti2 = ti1;
                ti1 = tmp;
            }
            // We want the first (forward==true) or last one in time.
            double[][]intervals = (forward ? 
                    new double[][] {{ti0, ti1}, {ti1, ti2}, {ti2, ti}}
            :       new double[][] {{ti2, ti}, {ti1, ti2}, {ti0, ti1}});
            for (double [] interval : intervals) {
                if (interval[0] < interval[1]) {
                    Double ts = newtonSolve(interval[0], interval[1], f, g, true);
                    if (ts != null) {
                        return ts + tSeg;
                    }
                }
            }
        }
        else if (a[i-1] != 0) {
            // Has acceleration. 
            // [t == -(v0 + sqrt(2*a0*s + v0^2))/a0, t == -(v0 - sqrt(2*a0*s + v0^2))/a0]
            double ds = sCross - s[i-1];
            double v0 = v[i-1];
            double v02 = v0*v0;
            //double v03 = v02*v0;
            double a0 = a[i-1];
            double rTerm = 2*a0*ds + v02;
            if (rTerm >= 0) {
                double sTerm = Math.sqrt(2*a0*ds + v02);
                double ts1 = -(v0 + sTerm)/a0;
                double ts2 = -(v0 - sTerm)/a0;
                // We want the first (forward==true) or second in time.
                if ((ts1 > ts2) ^ forward) {
                    // swap
                    double tmp = ts2;
                    ts2 = ts1;
                    ts1 = tmp;
                }
                if (ts1 >= 0 && ts1 < ti) {
                    return ts1 + tSeg;
                }
                else if (ts2 >= 0 && ts2 < ti) {
                    return ts2 + tSeg;
                }
            }
        }
        else if (v[i-1] != 0) {
            // Just Velocity.
            // [t == s1/v0]
            double ds = sCross - s[i-1];
            double v0 = v[i-1];
            double ts = ds/v0;
            if (ts > 0 && ts < ti) {
                return ts + tSeg;
            }
        }
        return null;
    }

    protected Double newtonSolve(double x0, double x1, Function<Double, Double> f,
            Function<Double, Double> g, boolean zeroes) {
        // Start value.
        double x = (x0 + x1)*0.5;
        int escapeNeg = 0;
        int escapePos = 0;
        for(int iter = 0; iter < iterations; iter++) {
            double y = f.apply(x);
            double dydt = g.apply(x);

            if (Math.abs(dydt) < ttol) {
                // Stop if the denominator is too small
                return null;
            }
            // Do Newton's computation. Limit to interval.
            double xn = Math.max(x0, Math.min(x1, x - y/dydt));  
            //trace("  newton("+x0+", "+x1+") x="+x+" y="+y+" dydt="+dydt+" xn="+xn+" iter="+iter);
            if (xn <= x0) {
                if (++escapeNeg > 1) {
                    // Multiple times outside, escaped. 
                    return null;
                }
                escapePos = 0;
            }
            else if (xn >= x1) {
                if (++escapePos > 1) {
                    // Multiple times outside, escaped. 
                    return null;
                }
                escapeNeg = 0;
            }
            else {
                escapeNeg = 0;
                escapePos = 0;
                if (zeroes) {
                    // Stop when the result is within the desired tolerance or zero.
                    if (Math.abs(y) <= ttol) { 
                        x = xn;
                        break;
                    }
                }
                else {
                    // Stop when the result is within the desired tolerance. Local minimum.
                    if (Math.abs(xn - x) <= ttol) { 
                        x = xn;
                        break;
                    }
                }
            }
            // Update x to start the process again.
            x = xn;
        }
        return x;
    }

    public void computeBounds() {
        // Calculate the bounds
        if (s[0] <= s[segments]) {
            sBound0 = s[0];
            tSBound0 = 0;
            sBound1 = s[segments];
            tSBound1 = time;
        }
        else {
            sBound0 = s[segments];
            tSBound0 = time;
            sBound1 = s[0];
            tSBound1 = 0;
        }
        vBound0 = v[0];
        vBound1 = v[0];
        tVBound0 = 0;
        tVBound1 = 0;
        aBound0 = a[0];
        aBound1 = a[0];
        tABound0 = 0;
        tABound1 = 0;

        double tSeg = 0;
        for (int i = 1; i <= segments; i++) {
            // Find the velocity crossing zero, that may be an extreme for s
            // and the acceleration crossing zero, that may be an extreme for v.
            if (j[i-1] != 0) {
                // 3rd order segment.
                double dt = Math.sqrt(Math.pow(a[i-1], 2) - 2*v[i-1]*j[i-1]);
                for (double tCross : new double[] { -(a[i-1] + dt)/j[i-1], -(a[i-1] - dt)/j[i-1] }) { 
                    if (tCross >= 0 && tCross <= t[i]) {
                        // Zero-crossing inside the period, maybe an extreme.
                        double sExtreme = s[i-1] + v[i-1]*tCross + 1./2*a[i-1]*Math.pow(tCross, 2) + 1./6*j[i-1]*Math.pow(tCross, 3);
                        if (sExtreme < sBound0) {
                            sBound0 = sExtreme;
                            tSBound0 = tSeg + tCross;
                        }
                        if (sExtreme > sBound1) {
                            sBound1 = sExtreme;
                            tSBound1 = tSeg + tCross;
                        }
                    }
                }
                double tCross = -a[i-1]/j[i-1];
                if (tCross >= 0 && tCross <= t[i]) {
                    // Zero-crossing inside the period, maybe an extreme.
                    double vExtreme = v[i-1] + a[i-1]*tCross + 1./2*j[i-1]*Math.pow(tCross, 2);
                    if (vExtreme < vBound0) {
                        vBound0 = vExtreme;
                        tVBound0 = tSeg + tCross;
                    }
                    if (vExtreme > sBound1) {
                        vBound1 = vExtreme;
                        tVBound1 = tSeg + tCross;
                    }
                }
            }
            else if (a[i] != 0) {
                // 2nd order segment.
                double tCross = -v[i-1]/a[i-1];
                if (tCross >= 0 && tCross <= t[i]) {
                    // Zero-crossing inside the period, maybe an extreme.
                    double sExtreme = s[i-1] + v[i-1]*tCross + 1./2*a[i-1]*Math.pow(tCross, 2);
                    if (sExtreme < sBound0) {
                        sBound0 = sExtreme;
                        tSBound0 = tSeg + tCross;
                    }
                    if (sExtreme > sBound1) {
                        sBound1 = sExtreme;
                        tSBound1 = tSeg + tCross;
                    }
                }
            }
            tSeg += t[i];

            // Just in case we missed them numerically, take the node extremes into consideration.
            if (s[i] < sBound0) {
                sBound0 = s[i];
                tSBound0 = tSeg;
            }
            if (s[i] > sBound1) {
                sBound1 = s[i];
                tSBound1 = tSeg;
            }

            if (v[i] < vBound0) {
                vBound0 = v[i];
                tVBound0 = tSeg;
            }
            if (v[i] > vBound1) {
                vBound1 = v[i];
                tVBound1 = tSeg;
            }

            if (a[i] < aBound0) {
                aBound0 = a[i];
                tABound0 = tSeg;
            }
            if (a[i] > aBound1) {
                aBound1 = a[i];
                tABound1 = tSeg;
            }
        }
    }


    public double getEffectiveEntryVelocity(double jMax) {
        if (hasOption(ProfileOption.UnconstrainedEntry)) {
            return 0.0;
        }
        else if (isConstantAcceleration()) {
            return v[0];
        }
        else {
            double jEntry = (a[0] == 0 ? 1.0 : -Math.signum(a[0]))*jMax;
            double tEntry = -a[0]/jEntry;  
            double vEntry = v[0] + a[0]*tEntry + 1./2*jEntry*Math.pow(tEntry, 2);
            return vEntry;
        }
    }

    public double getEffectiveExitVelocity(double jMax) {
        if (hasOption(ProfileOption.UnconstrainedExit)) {
            return 0.0;
        }
        else if (isConstantAcceleration()) {
            return v[segments];
        }
        else {
            double jExit = (a[segments] == 0 ? 1.0 : Math.signum(a[segments]))*jMax;
            double tExit = -a[segments]/jExit;  
            double vExit = v[segments] + a[segments]*tExit + 1./2*jExit*Math.pow(tExit, 2);
            return vExit;
        }
    }

    public void toSvg() {
        // Calculate the effective entry/exit velocity after jerk to acceleration 0.
        int eval = this.eval;
        double vSolved = v[4];
        double vEffEntry = getEffectiveEntryVelocity(jMax);
        double vEffExit = getEffectiveExitVelocity(jMax);
        double d = 0.6;
        double sy = ((+vMax)-(-vMax))/-2.4*3/4;
        double ssy = sy*0.01;
        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                "<svg xmlns=\"http://www.w3.org/2000/svg\"\n" + 
                "  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" + 
                "  version=\"1.1\" baseProfile=\"full\"\n" + 
                "  width=\"100%\" height=\"100%\" preserveAspectRatio=\"none\"\n"+
                "  viewBox=\""+(-vMax-d)+" "+1.2*sy+" "+((+vMax)-(-vMax)+2*d)+" "+(-2.4*sy+2*d)+"\">\r\n");
        svg.append("<title>Profile s[0]="+s[0]+" s[7]="+s[7]+" v[0]="+v[0]+" v[7]="+v[7]+" tMin="+tMin);
        svg.append("</title>\n");
        double vPrev = (-vMax);
        double t4Prev= 0;
        double timePrev = 0;
        double s3_4Prev = 0;
        double s3mtPrev = 0;
        svg.append("<line x1=\""+(-vMax)+"\" y1=\""+(tMin*sy)+"\" x2=\""+(+vMax)+"\" y2=\""+(tMin*sy)+"\" style=\"stroke-width: "+d*0.5+"; stroke:orange;\"/>\n");
        svg.append("<line x1=\""+(-vMax)+"\" y1=\""+(1*sy)+"\" x2=\""+(+vMax)+"\" y2=\""+(1*sy)+"\" style=\"stroke-width: "+d*0.5+"; stroke:lightgrey;\"/>\\n");
        svg.append("<line x1=\""+(-vMax)+"\" y1=\"0\" x2=\""+(+vMax)+"\" y2=\"0\" style=\"stroke-width: "+d+"; stroke:grey;\"/>\\n");
        svg.append("<line x1=\""+(-vMax)+"\" y1=\""+(-1*sy)+"\" x2=\""+(+vMax)+"\" y2=\""+(-1*sy)+"\" style=\"stroke-width: "+d*0.5+"; stroke:lightgrey;\"/>\\n");
        svg.append("<line x1=\""+v[0]+"\" y1=\""+(-2*sy)+"\" x2=\""+v[0]+"\" y2=\""+(2*sy)+"\" stroke-dasharray=\"5,5\" style=\"stroke-width: "+d+"; stroke:grey;\"/>\\n");
        svg.append("<line x1=\""+v[7]+"\" y1=\""+(-2*sy)+"\" x2=\""+v[7]+"\" y2=\""+(2*sy)+"\" stroke-dasharray=\"5,5\" style=\"stroke-width: "+d+"; stroke:grey;\"/>\\n");
        String color;
        for (double vi = -vMax*10; vi <= vMax*10; vi++) {
            double v= vi*0.1;
            computeProfile(v, vEffEntry, vEffExit, tMin);
            double tResult = time;
            double s3_4 = Math.signum(v)*(s[4]-s[3]);
            double s3mt = (time-tMin);
            if (v != vPrev) {
                svg.append("<line x1=\""+v+"\" y1=\""+t[4]*sy+"\" x2=\""+vPrev+"\" y2=\""+t4Prev*sy+"\" style=\"stroke-width: "+d+"; stroke:green;\"/>\n");
                svg.append("<line x1=\""+v+"\" y1=\""+tResult*sy+"\" x2=\""+vPrev+"\" y2=\""+timePrev*sy+"\" style=\"stroke-width: "+d+"; stroke:red;\"/>\n");
                //svg.append("<line x1=\""+v+"\" y1=\""+Math.min(t[4], Math.min(0, t[4]*2)+time-tMin)*sy+"\" x2=\""+vPrev+"\" y2=\""+Math.min(t4Prev, Math.min(0, t4Prev*2)+timePrev-tMin)*sy+"\" style=\"stroke-width: "+d+"; stroke:blue;\"/>\n");
                color = s3_4 > 0 ? "blue" : "lightblue"; 
                svg.append("<line x1=\""+v+"\" y1=\""+s3_4*ssy+"\" x2=\""+vPrev+"\" y2=\""+s3_4Prev*ssy+"\" style=\"stroke-width: "+d+"; stroke:"+color+";\"/>\n");
                color = s3_4 > 0 ? "red" : "pink";
                svg.append("<line x1=\""+v+"\" y1=\""+s3mt*sy+"\" x2=\""+vPrev+"\" y2=\""+s3mtPrev*sy+"\" style=\"stroke-width: "+d+"; stroke:"+color+";\"/>\n");
                if (Math.signum(s3_4) != Math.signum(s3_4Prev)) {
                    svg.append("<line x1=\""+v+"\" y1=\""+(-2*sy)+"\" x2=\""+v+"\" y2=\""+(2*sy)+"\" stroke-dasharray=\"1,1\" style=\"stroke-width: "+d+"; stroke:lightgreen;\"/>\\n");
                }
                if (Math.signum(s3mt) != Math.signum(s3mtPrev)) {
                    svg.append("<line x1=\""+v+"\" y1=\""+(-2*sy)+"\" x2=\""+v+"\" y2=\""+(2*sy)+"\" stroke-dasharray=\"1,1\" style=\"stroke-width: "+d+"; stroke:pink;\"/>\\n");
                }
            }
            vPrev = v;
            t4Prev = t[4];
            s3_4Prev = s3_4;
            s3mtPrev = s3mt;
            timePrev = tResult;
        }
        svg.append("<line x1=\"0\" y1=\""+(-2*sy)+"\" x2=\"0\" y2=\""+(2*sy)+"\" style=\"stroke-width: "+d+"; stroke:grey;\"/>\\n");
        if (hasOption(ProfileOption.Solved)) {
            svg.append("<line x1=\""+vSolved+"\" y1=\""+(-2*sy)+"\" x2=\""+vSolved+"\" y2=\""+(2*sy)+"\" style=\"stroke-width: "+d+"; stroke:yellow;\"/>\\n");
        }
        svg.append("</svg>\n");
        try {
            File file = File.createTempFile("profile-solver-", ".svg");
            try (PrintWriter out = new PrintWriter(file.getAbsolutePath())) {
                out.println(svg.toString());
                System.out.println(file.toURI());
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }            
        }
        catch (IOException e) {
            e.printStackTrace();
        }            
        // Restore
        computeProfile(vSolved, vEffEntry, vEffExit, tMin);
        this.eval = eval;
    }
    static void trace(String message) {
        if (traceEnabled) {
            System.out.println(message);
        }
    }

}

