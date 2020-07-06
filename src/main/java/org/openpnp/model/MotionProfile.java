package org.openpnp.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.openpnp.util.NanosecondTime;
import org.pmw.tinylog.Logger;

public class MotionProfile {
    public final static int segments = 7;

    double [] s = new double[segments+1];
    double [] a = new double[segments+1];
    double [] v = new double[segments+1];
    double [] j = new double[segments+1];
    double [] t = new double[segments+1];

    double sMin;
    double sMax;
    double vMax;
    double aMaxEntry; 
    double aMaxExit; 
    double jMax;
    double tMin;
    double tMax;

    static final int iterations = 80;
    static final double vtol = 2.0; // mm/s
    static final double atol = 5.0; // mm/s^2
    static final double jtol = 10.0; // mm/s^3
    static final double ttol = 0.000001; // s 

    private int eval;

    private double time;

    private double solvingTime;

    private double sBound0;

    private double sBound1;

    private double tBound0;

    private double tBound1;

    private int profileOptions;

    private double vUnconstrainedEntry;

    private double vUnconstrainedExit;

    private double tMinUnconstrainedEntry;

    private double tMinUnconstrainedExit;

    public enum ProfileOption {
        Coordinated,
        Jog,
        StillstandEntry,
        StillstandExit,
        UnconstrainedExit,
        UnconstrainedEntry,
        Solved,
        Twisted;
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
        return a[segment];
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

    public double getLowerBoundary() {
        return sBound0;
    }

    public double getHigherBoundary() {
        return sBound1;
    }

    public double getLowerBoundary0Time() {
        return tBound0;
    }

    public double getHigherBoundaryTime() {
        return tBound1;
    }

    public boolean isSolved() {
        return hasOption(ProfileOption.Solved);
    }

    public double getProfileVelocity() {
        return Math.abs(v[4]);
    }

    public double getProfileEntryAcceleration() {
        return Math.abs(a[2]);
    }

    public double getProfileExitAcceleration() {
        return Math.abs(a[6]);
    }

    public double getProfileJerk() {
        return Math.abs(j[1]);
    }

    public double getTime() {
        return time;
    }

    protected double getMomentary(double time, BiFunction<Integer, Double, Double> f) {
        if (time < 0) {
            return s[0];
        }
        for (int i = 1; i <= segments; i++) {
            if (time >= 0 && time <= t[i]) {
                return f.apply(i, time);
            }
            time -= t[i];
        }
        return s[segments];
    }

    public double getMomentaryLocation(double time) { // s0 + V0*t + 1/2*a0*t^2 + 1/6*j*t^3
        return getMomentary(time, (i, ts) -> (s[i-1] + v[i-1]*ts + 1./2*a[i-1]*Math.pow(ts, 2) + 1./6*j[i]*Math.pow(ts, 3)));
    }

    public double getMomentaryVelocity(double time) { // V0 + a0*t + 1/2*j*t^2
        return getMomentary(time, (i, ts) -> (v[i-1] + a[i-1]*ts + 1./2*j[i]*Math.pow(ts, 2)));
    }

    public double getMomentaryAcceleration(double time) { // a0 + j*t
        return getMomentary(time, (i, ts) -> (a[i-1] + j[i]*ts));
    }

    public double getMomentaryJerk(double time) { 
        return getMomentary(time, (i, ts) -> (j[i]));
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

    // As we're handling millimeters and seconds, we can use a practical eps. 
    static final double eps = 1e-8;
    public ErrorState assertValidity() {
        // Assert hard constraints
        // Assert continuity.
        double tSum = 0;
        for (int i = 1; i <= segments; i++) {
            // Check timing.
            if (!(Double.isFinite(t[i]) 
                    && Double.isFinite(s[i])
                    && Double.isFinite(v[i])
                    && Double.isFinite(a[i])
                    && Double.isFinite(j[i]))) {
                return ErrorState.SolutionNotFinite;
            }
            if (t[i] < -eps) {
                return ErrorState.NegativeSegmentTime;
            }
            tSum += t[i];
            // Check continuity.
            if (mismatch(s[i], s[i-1] + v[i-1]*t[i] + 1./2*a[i-1]*Math.pow(t[i], 2) + 1./6*j[i]*Math.pow(t[i], 3))) {
                return ErrorState.LocationDiscontinuity;
            }
            if (mismatch(v[i], v[i-1] + a[i-1]*t[i] + 1./2*j[i]*Math.pow(t[i], 2))) {
                return ErrorState.VelocityDiscontinuity;
            }
            if (mismatch(a[i], a[i-1] + j[i]*t[i])) {
                return ErrorState.AccelerationDiscontinuity;
            }
        }
        // Check time sum constraints-.
        if (mismatch(tSum, time)) {
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
        if (sBound1 > sMax - eps) {
            return ErrorState.MaxLocationViolated;
        }
        // Assert lesser constraints
        for (int i = 1; i <= segments; i++) {
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

    public static void validateProfiles(MotionProfile[] profiles) {
        for (int i = 0; i < profiles.length; i++) {
            MotionProfile profile = profiles[i];
            MotionProfile.ErrorState error = profile.assertValidity();
            if (error != null) {
                Logger.error("["+i+"] "+profile+" has error: "+error);
            }
            else {
                System.out.println("["+i+"] "+profile);
            }
        }

    }

    private boolean mismatch(double a, double b) {
        if (Math.abs(a-b) > eps) {
            return true; // (debug point)
        }
        return false;
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
        str.append(String.format("%.2f@%.4f, %.2f@%.4f", sBound0, tBound0, sBound1, tBound1));
        str.append(", eval = ");
        str.append(eval);
        str.append(", ms = ");
        str.append(String.format("%.3f", solvingTime*1000));
        str.append("}");
        return str.toString();
    }

    public void solveProfile() {
        // scale down tolerances for tiny moves
        double magnitude = Math.max(eps,  Math.min(1.0, 
                0.1*(Math.abs(s[0]-s[segments])
                        +Math.abs(v[0])+Math.abs(v[segments])
                        +Math.abs(a[0])+Math.abs(a[segments]))));
        solveProfile(iterations, vtol*Math.sqrt(magnitude), ttol*Math.sqrt(magnitude));
    }
    public void solveProfile(final int iterations, final double vtol, final double ttol) {
        double tStart = NanosecondTime.getRuntimeSeconds();
        // Check for a null move. As we always handle all axes of the machine, we want to be fast with those.
        if (s[0] == s[segments]
                && v[0] == v[segments]
                        && a[0] == a[segments]
                                && (tMin == 0 || (v[0] == 0 && a[0] == 0))) {
            // Null move
            for (int i = 1; i < segments; i++) {
                s[i] = s[0];
                v[i] = v[0];
                a[i] = a[0];
                j[i] = 0;
                t[i] = 0;
            }
            t[4] = tMin;
            time = tMin;
            eval = 0;
            sBound0 = sBound1 = s[0];
            tBound0 = tBound1 = 0;
            return; // -----------------> 
        }

        System.out.println("### solving "+this);
                
        // Calculate the effective entry/exit velocity after jerk to acceleration 0.
        double vEffEntry = getEffectiveEntryVelocity(jMax);
        double vEffExit = getEffectiveExitVelocity(jMax);

        toSvg();        

        // Determine the direction of travel.
        double signum = Math.signum(s[segments]-s[0]);
        if (signum == 0) {
            // Zero displacement.
            // if (! (hasOption(ProfileOption.UnconstrainedEntry) || hasOption(ProfileOption.UnconstrainedExit))) { ???
            // Take entry/exit velocity balance as criterion. 
            signum = Math.signum(-Math.round(vEffEntry/vtol) - Math.round(vEffExit/vtol));
            // TODO: if this is still zero we just assume it's completely symmetric, but this might not be true
            // if the a vs. V mix are not the same on entry/exit and by chance still cancel out in the effective speed. 
            // We would need to calculate the displacement to still-stand and compare.
            if (signum == 0) {
                System.out.println("*** signum 0");
            }
        } 

        // Compute the profile with signed vMax first, this will give us the first indications. 
        computeProfile(signum*vMax, vEffEntry, vEffExit, tMin);
        // Immediately return right here if this is a valid solution (this happens if it is a very long move and it reaches vMax).
        if (time > tMin && t[4] >= 0 && v[0] == v[segments] && a[0] == 0 && a[segments] == 0) {
            System.out.println("Vmax symmetrical move, immediate solution");
            return;
        }
        
        // Need to solve this numerically. Because the solution can have many roots and local minimae, we need to split it into multiple
        // regions with known qualities.

        // Solver regions from -vMax to +vMax are split by effective entry/exit velocities and zero. 
        // Note, we do not allow solutions beyond vMax, even if the effective entry/exit velocities are beyond.
        double [] borders = new double [] {
                -vMax, 0, vMax, Math.max(-vMax, Math.min(vMax, vEffEntry)), Math.max(-vMax, Math.min(vMax, vEffExit)) 
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
        if (tMin == 0) {
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
                        - 2*Math.sqrt(3*Math.pow(a[1], 4) + 18*a[1]*Math.pow(j[1], 2)*s3 + 9*Math.pow(j[1], 2)*Math.pow(v[1], 2)))/j[1];
                double v3_2 = -1./6*(3*Math.pow(a[1], 2) 
                        + 2*Math.sqrt(3*Math.pow(a[1], 4) + 18*a[1]*Math.pow(j[1], 2)*s3 + 9*Math.pow(j[1], 2)*Math.pow(v[1], 2)))/j[1];
                System.out.println("Analytical solution with constant acceleration segment (1) = "+vInitialGuess+" (2) = "+v3_2);
            }
            else if (t[5] > (-t[4]*0.25)) { 
                // Deceleration segment is long enough.
                double s4 = (s[6] - (halfProfile ? (s[4] + s[3])*0.5 : s[3]));
                // We just trust the (just in time) compiler to eliminate common sub-expressions.
                vInitialGuess = (-1./6*(3*Math.pow(a[6], 2) 
                        - 2*Math.sqrt(3*Math.pow(a[6], 4) - 18*a[6]*Math.pow(j[segments], 2)*s4 + 9*Math.pow(j[segments], 2)*Math.pow(v[6], 2)))/j[segments]);
                double v3_2 = (-1./6*(3*Math.pow(a[6], 2) 
                        + 2*Math.sqrt(3*Math.pow(a[6], 4) - 18*a[6]*Math.pow(j[segments], 2)*s4 + 9*Math.pow(j[segments], 2)*Math.pow(v[6], 2)))/j[segments]);
                System.out.println("Analytical solution with constant deceleration segment (1) = "+vInitialGuess+" (2) = "+v3_2);
            }
            if (Double.isFinite(vInitialGuess) && Math.abs(vInitialGuess) > 0 && Math.abs(vInitialGuess) <= vMax) {
                computeProfile(vInitialGuess, vEffEntry, vEffExit, tMin);
                if (t[4] >= -ttol && t[4] < vttol) {
                    System.out.println("taken "+this);
                    return;
                }
            }
        }

        // Find the best solution:
        double bestTime = Double.POSITIVE_INFINITY;
        double bestVelocity = Double.NaN;

        // Solve for each region in a logical order.
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
            if (Double.isNaN(borderSResult[border0])) {
                // must first calculate border condition.
                computeProfile(vPeak0, vEffEntry, vEffExit, tMin);
                borderSResult[border0] = s[4] - s[3];
                borderTResult[border0] = time;
            }
            if (Double.isNaN(borderSResult[border1])) {
                // must first calculate border condition.
                computeProfile(vPeak1, vEffEntry, vEffExit, tMin);
                borderSResult[border1] = s[4] - s[3];
                borderTResult[border1] = time;
            }

            double sResult0 = sign*borderSResult[border0]; 
            double sResult1 = sign*borderSResult[border1];
            boolean sValid0 = (sResult0 >= -stol);
            boolean sValid1 = (sResult1 >= -stol);

            if (!(sValid0 || sValid1)) {
                // None valid -> skip this region.
                System.out.println("region invalid in s "+vPeak0+" .. "+vPeak1+", s="+sResult0+" .. "+sResult1);
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
                System.out.println("region invalid in t "+vPeak0+" .. "+vPeak1+", s="+sResult0+" .. "+sResult1+", t="+tResult0+" .. "+tResult1);
                continue;
            }

            // Consider valid border cases as solutions.
            if (sValid0 && tValid0 && tResult0 < bestTime) {
                bestVelocity = vPeak0;
                bestTime = tResult0;
                System.out.println("border case t "+vPeak0+", s="+sResult0+", t="+tResult0);
            }
            if (sValid1 && tValid1 && tResult1 < bestTime) {
                bestVelocity = vPeak1;
                bestTime = tResult1;
                System.out.println("border case t "+vPeak1+", s="+sResult1+", t="+tResult1);
            }

            if (sValid0 && sValid1 &&
                    Math.min(vEffEntry, vEffExit) <= vPeak0 
                    && Math.max(vEffEntry, vEffExit) >= vPeak1
                    /*&& vPeak0 != 0 && vPeak1 != 0*/) {
                // We're between two valid entry/exit velocities. There may be an invalid region in the middle.
                // --> Find it!
                // Work hypothesis (from observing many curves empirically): 
                // a) the curve's minimum is on the higher absolute velocity side.
                // b) the curve's second derivative is always > 0 i.e. always upward curved, "cup shaped".
                // By a) we know we can start at the middle point and proceed into the upper absolute velocity half. 
                // By b) we know that the secant method will always fall short, if there is a solution.  
                // If we detect it raising again, we know there is no solution.
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
                        System.out.println("    found invalid mid area "+vSearch+" s "+sResult+" t "+tResult);
                        break;
                    }
                    if (sResult > sSecant) {
                        // Raising result -> overshoot, this means there is no invalid section. 
                        System.out.println("    overshot invalid mid area "+vSearch+" s "+sResult+" t "+tResult);
                        break;
                    }
                    // Apply secant method. 
                    double gradient = (sResult-sSecant)/(vSecant-vSearch);
                    if (Math.abs(gradient) < vttol) {
                        // Stuck in a local minimum. This must be a near miss situation (otherwise we should see overshoot).
                        System.out.println("    stuck local minimum invalid mid area "+vSearch+" s "+sResult+" t "+tResult);
                        break;
                    }
                    double delta = -sResult/gradient;
                    // remember old values
                    vSecant = vSearch;
                    sSecant = sResult;
                    // Assign new value.
                    vSearch = Math.max(vSearch0, Math.min(vSearch1,  vSearch+delta));
                    System.out.println("    search for invalid mid area "+vSearch+" gradient "+gradient+" delta "+delta+" s "+sResult+" t "+tResult);
                }
                while (true);
                if (sResult < 0) {
                    // We have found an invalid section, solve for two roots.
                    if (solveForVelocity(vPeak0, vSearch, sResult0, sResult, tResult0, tResult, vEffEntry, vEffExit, tMin,
                            iterations, stol, vtol, ttol)) {
                        if (time < bestTime) {
                            bestVelocity = v[4];
                            bestTime = time;
                        }
                    }
                    if (solveForVelocity(vSearch, vPeak1, sResult, sResult1, tResult, tResult1, vEffEntry, vEffExit, tMin,
                            iterations, stol, vtol, ttol)) {
                        if (time < bestTime) {
                            bestVelocity = v[4];
                            bestTime = time;
                        }
                    }
                }
                else {
                    // Only one root expected. 
                    if (solveForVelocity(vPeak0, vPeak1, sResult0, sResult1, tResult0, tResult1, vEffEntry, vEffExit, tMin,
                            iterations, stol, vtol, ttol)) {
                        if (time < bestTime) {
                            bestVelocity = v[4];
                            bestTime = time;
                        }
                    }
                }
            }
            else {
                if (solveForVelocity(vPeak0, vPeak1, sResult0, sResult1, tResult0, tResult1, vEffEntry, vEffExit, tMin,
                        iterations, stol, vtol, ttol)) {
                    if (time < bestTime) {
                        bestVelocity = v[4];
                        bestTime = time;
                    }
                }
            }
        }
        System.out.println("best velocity "+bestVelocity+" best time "+bestTime+" time-tMin "+(bestTime-tMin));
        if (bestVelocity != v[4]) {
            // re-establish best solution
            System.out.println("  re-establish");
            computeProfile(bestVelocity, vEffEntry, vEffExit, tMin);
        }
        if (tMin > time - ttol) {
            // The solver may have slightly approximated. Stretch the profile into the exact minimum time. 
            retimeProfile(tMin);
        }
        // Result is now stored in the profile i.e. you can get v[4], a[2], a[6] to get the (signed) solution.
        solvingTime = NanosecondTime.getRuntimeSeconds() - tStart;
        setOption(ProfileOption.Solved);
    }


    protected boolean solveForVelocity(double vPeak0, double vPeak1, 
            double sResult0, double sResult1, 
            double tResult0, double tResult1, 
            double vEffEntry, double vEffExit, 
            double tMin, 
            final int iterations, final double stol, final double vtol, final double ttol) {

        // Solver loop.
        System.out.println("=== solveForVelocity("+vPeak0+" .. "+vPeak1+", s="+sResult0+" .. "+sResult1+", t="+tResult0+" .. "+tResult1+")");
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
        vPeak = (vPeak1 + vPeak0)*0.5;
        int converging = 0;
        boolean sAscending = sResult0 < sResult1; 
        boolean tAscending = tResult0 < tResult1; 

        for (int iter = 0; iter <= iterations; iter++) {
            
            // Compute the profile with the given velocity limit.
            computeProfile(vPeak, vEffEntry, vEffExit, tMin);
            
            double sResult = sign*(s[4] - s[3]);
            double tResult = time;
            System.out.println("vPeak = "+vPeak+" s="+sResult+" t-tMin="+(time-tMin)+" "+this);
            double magnitude = Math.max(eps, Math.min(1.0, 0.001*(Math.abs(s[3]-s[0])+Math.abs(s[segments]-s[4]))));
            if (Math.abs(vPeak - vSecant) < magnitude*vtol) {
                converging++;
            }
            else {
                converging = 0;
            }
            if (sResult >= -stol && tResult >= tMin-ttol && (tResult < tMin + ttol || converging >= 2)) {
                // That's a solution
                System.out.println("taken");
                return true;
            }
            if (converging >= 6) {
                // Local minimum but not OK 
                System.out.println("** giving up");
                return false;
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
            //                            System.out.println("secant terr method: gradient="+gradient+" delta="+-terr/gradient+" vPeak="+(vPeak - terr/gradient));
            //                            vPeak += delta;
            //                            vPeak = Math.max(vPeak0+eps,  Math.min(vPeak1, vPeak));
        }
        return false;
    }

    public void toSvg() {
        // Calculate the effective entry/exit velocity after jerk to acceleration 0.
        int eval = this.eval;
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
                "  viewBox=\""+(-vMax)+" "+1.2*sy+" "+((+vMax)-(-vMax))+" "+(-2.4*sy)+"\">\r\n");
        svg.append("<title>Profile"+(hasOption(ProfileOption.Twisted)?" twisted:":":")+" s[0]="+s[0]+" s[7]="+s[7]+" v[0]="+v[0]+" v[7]="+v[7]+" tMin="+tMin);
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
            double s3_4 = Math.signum(v)*(s[4]-s[3]);
            double s3mt = (time-tMin);
            if (v != vPrev) {
                svg.append("<line x1=\""+v+"\" y1=\""+t[4]*sy+"\" x2=\""+vPrev+"\" y2=\""+t4Prev*sy+"\" style=\"stroke-width: "+d+"; stroke:green;\"/>\n");
                svg.append("<line x1=\""+v+"\" y1=\""+time*sy+"\" x2=\""+vPrev+"\" y2=\""+timePrev*sy+"\" style=\"stroke-width: "+d+"; stroke:red;\"/>\n");
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
            timePrev = time;
        }
        svg.append("<line x1=\"0\" y1=\""+(-2*sy)+"\" x2=\"0\" y2=\""+(2*sy)+"\" style=\"stroke-width: "+d+"; stroke:grey;\"/>\\n");
        svg.append("</svg>\n");
        try {
            File file = File.createTempFile("profile-solver-", ".svg");
            try (PrintWriter out = new PrintWriter(file.getAbsolutePath())) {
                out.println(svg.toString());
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }            
        }
        catch (IOException e) {
            e.printStackTrace();
        }            
        this.eval = eval;
    }

    /**
     * Recalculate the profile to make sure it takes the given amount of time.
     * @param newTime
     */
    private void retimeProfile(double newTime) {
        if (newTime != time && newTime > 0.0 && time > 0) {
            // The derivatives need to be scaled to the power of the order of the derivative.
            double vFactor = time/newTime; 
            double aFactor = vFactor*vFactor;
            double jFactor = aFactor*vFactor;
            for (int i = 0; i <= segments; i++) {
                t[i] /= vFactor;
                v[i] *= vFactor;
                a[i] *= aFactor;
                j[i] *= jFactor;
            }
            time = newTime;
        }
    }

    protected void assertSolved() {
        if (!hasOption(ProfileOption.Solved)) {
            solveProfile();
            MotionProfile.ErrorState error = assertValidity();
            if (error != null) {
                Logger.error(this+" has error: "+error);
            }
            else if (eval > 0){
                Logger.trace(this);
            }
        }
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
            // Can't use bestDist, need it signed.
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
                System.out.println("    max time from "+profile);
            }
        }
        // Re-time the others.
        double maxTime2 = maxTime;
        for (MotionProfile profile : profiles) {
            profile.tMin = maxTime;
            if (profile.time != maxTime) {
                // TODO: remove. This cumbersome way is for tracing.
                profile.clearOption(ProfileOption.Solved); 
                profile.assertSolved();
                if (profile.time > maxTime2) {
                    // sometimes the solving with larger time cannot be done.
                    maxTime2 = profile.time;
                }
            }
        }
        if (maxTime2 > maxTime) {
            for (MotionProfile profile : profiles) {
                profile.tMin = maxTime2;
                if (profile.time != maxTime2) {
                    // TODO: remove. This cumbersome way is for tracing.
                    profile.clearOption(ProfileOption.Solved); 
                    profile.assertSolved();
                }
            }
        }
    }


    public static void solvePath(List<MotionProfile []> path) {
        // Preparation: calc some data about the path
        int [] leadAxis= new int[path.size()];
        double[][] unitVector = new double[path.size()][];
        double [] junctionCosineFromPrev = new double[path.size()];
        boolean [] colinearWithPrev = new boolean[path.size()]; 
        int last = path.size() - 1;
        for (int i = 0; i <= last; i++) {
            MotionProfile [] profiles = path.get(i);
            unitVector[i] = getUnitVector(profiles);
            leadAxis[i] = getLeadAxisIndex(unitVector[i]);
            if (i > 0) {
                junctionCosineFromPrev[i] = dotProduct(unitVector[i-1], unitVector[i]);
            }
            colinearWithPrev[i] = (junctionCosineFromPrev[i] >= 1.0 - eps); 
        }

        System.out.println(" ------------- PASS 1 -------------------------");

        // Pass 1: Greedy forward motion, unconstrained. I.e. move toward the next way-point at full
        // acceleration/speed, disregarding what happens next, except if the next move is a coordinated move
        // then colinearity or zero speed is considered. 
        MotionProfile [] prevProfiles = null;
        for (int i = 0; i <= last; i++) {
            MotionProfile [] profiles = path.get(i);

            if (isCoordinated(profiles)) {
                int lead = leadAxis[i];
                if (prevProfiles != null) {
                    // Because this is coordinated, determine the minimum unit speed.
                    double vUnitMin = Double.POSITIVE_INFINITY;
                    double aUnitMin = Double.POSITIVE_INFINITY;
                    for (int axis = 0; axis < unitVector[i].length; axis++) {
                        if (unitVector[i][axis] != 0.0) {
                            vUnitMin = Math.min(vUnitMin, prevProfiles[axis].v[segments]/unitVector[i][axis]);
                            aUnitMin = Math.min(aUnitMin, prevProfiles[axis].a[segments]/unitVector[i][axis]);
                        }
                    }
                    // Don't allow bouncing into coordinated moves.
                    vUnitMin = Math.max(0, Math.min(profiles[lead].vMax, vUnitMin));
                    aUnitMin = Math.max(0, Math.min(profiles[lead].aMaxEntry, aUnitMin));
                    // Set the entry constraint from the previous profile.
                    profiles[lead].v[0] = unitVector[i][lead]*vUnitMin;
                    profiles[lead].a[0] = unitVector[i][lead]*aUnitMin;
                }
                if (i == last) { 
                    if (! profiles[lead].hasOption(ProfileOption.Jog)) {    
                        // Must stop at the end, clear the option.
                        profiles[lead].clearOption(ProfileOption.UnconstrainedExit);
                    }   
                }
                else if (isCoordinated(path.get(i+1)) && !colinearWithPrev[i+1]) {
                    // Must stop at the end, clear the option.
                    profiles[lead].clearOption(ProfileOption.UnconstrainedExit);
                }
                else {
                    // Can be left unconstrained
                    profiles[lead].setOption(ProfileOption.UnconstrainedExit);    
                }
                profiles[lead].solveProfile();
                coordinateProfilesToLead(profiles, profiles[lead]);
            }
            else { // Uncoordinated.
                for (int axis = 0; axis < unitVector[i].length; axis++) {
                    if (prevProfiles != null) {
                        // Set the entry constraints from the previous profile, but limited (can be lower than previous segment)
                        profiles[axis].v[0] =  Math.max(-profiles[axis].vMax, Math.min(profiles[axis].vMax, prevProfiles[axis].v[segments]));
                        profiles[axis].a[0] =  Math.max(-profiles[axis].aMaxEntry, Math.min(profiles[axis].aMaxEntry, prevProfiles[axis].a[segments]));
                    } 
                    if (i == last) {
                        if (! profiles[axis].hasOption(ProfileOption.Jog)) {
                            // Must stop at the end, clear the option.
                            profiles[axis].clearOption(ProfileOption.UnconstrainedExit);
                        }
                    }
                    else if (isCoordinated(path.get(i+1)) && unitVector[i+1][axis] == 0) {
                        // Must stop at the end, clear the option.
                        profiles[axis].clearOption(ProfileOption.UnconstrainedExit);
                    }
                    else if (profiles[axis].s[0] == profiles[axis].s[segments]) {
                        // No displacement. We want reflection.
                        profiles[axis].v[segments] = -profiles[axis].v[0];
                        profiles[axis].a[segments] = profiles[axis].a[0];
                        profiles[axis].clearOption(ProfileOption.UnconstrainedExit);
                    }
                    else {
                        // Can be left unconstrained
                        profiles[axis].setOption(ProfileOption.UnconstrainedExit);    
                    }
                    profiles[axis].solveProfile();
                    // clear the option for synchronize
                    profiles[axis].clearOption(ProfileOption.UnconstrainedExit);
                }
                synchronizeProfiles(profiles);
            }
            validateProfiles(profiles);
            prevProfiles = profiles;
        }

        System.out.println(" ------------- PASS 2 -------------------------");

        // Pass 2: Greedy backward motion, unconstrained.
        MotionProfile [] nextProfiles = null;
        for (int i = last; i >= 0; i--) {
            MotionProfile [] profiles = path.get(i);
            if (i > 0) {
                prevProfiles = path.get(i-1);
            }
            else {
                prevProfiles = null;
            }

            if (isCoordinated(profiles)) {
                int lead = leadAxis[i];
                if (nextProfiles != null) {
                    // Because this is coordinated, determine the minimum unit speed.
                    double vUnitMin = Double.POSITIVE_INFINITY;
                    double aUnitMin = Double.POSITIVE_INFINITY;
                    for (int axis = 0; axis < unitVector[i].length; axis++) {
                        if (unitVector[i][axis] != 0.0) {
                            vUnitMin = Math.min(vUnitMin, nextProfiles[axis].v[0]/unitVector[i][axis]);
                            aUnitMin = Math.min(aUnitMin, nextProfiles[axis].a[0]/unitVector[i][axis]);
                        }
                    }
                    // Don't allow bouncing from coordinated moves.
                    vUnitMin = Math.max(0, Math.min(profiles[lead].vMax, vUnitMin));
                    aUnitMin = Math.max(0, Math.min(profiles[lead].aMaxEntry, aUnitMin));
                    // Set the exit constraint from the previous profile.
                    profiles[lead].v[segments] = unitVector[i][lead]*vUnitMin;
                    profiles[lead].a[segments] = unitVector[i][lead]*aUnitMin;
                }
                if (prevProfiles == null) {
                    // Must start from still-stand. Clear the unconstrained flag
                    profiles[lead].clearOption(ProfileOption.UnconstrainedEntry);
                }
                else if (isCoordinated(prevProfiles)) {
                    profiles[lead].v[0] =  
                            Math.max(-profiles[lead].vMax, Math.min(profiles[lead].vMax, 
                                    Math.max(-prevProfiles[lead].vMax, Math.min(prevProfiles[lead].vMax,
                                            prevProfiles[lead].v[segments]))));
                    profiles[lead].a[0] =  
                            Math.max(-profiles[lead].aMaxEntry, Math.min(profiles[lead].aMaxEntry, 
                                    Math.max(-prevProfiles[lead].aMaxExit, Math.min(prevProfiles[lead].aMaxExit, 
                                            prevProfiles[lead].a[segments]))));
                    // Clear the unconstrained flag
                    profiles[lead].clearOption(ProfileOption.UnconstrainedEntry);
                }
                else {
                    // Can be left unconstrained
                    profiles[lead].setOption(ProfileOption.UnconstrainedEntry);    
                }
                // Remove options from pass 1.
                profiles[lead].tMin = 0;
                profiles[lead].clearOption(ProfileOption.UnconstrainedExit);
                profiles[lead].solveProfile();
                coordinateProfilesToLead(profiles, profiles[lead]);
                validateProfiles(profiles);
            }
            else { // Uncoordinated.
                for (int axis = 0; axis < unitVector[i].length; axis++) {
                    if (nextProfiles != null) {
                        // Set the exit constraints from the next profile, but limited (can be lower than next segment).
                        profiles[axis].v[segments] =  Math.max(-profiles[axis].vMax, Math.min(profiles[axis].vMax, nextProfiles[axis].v[0]));
                        profiles[axis].a[segments] =  Math.max(-profiles[axis].aMaxExit, Math.min(profiles[axis].aMaxExit, nextProfiles[axis].a[0]));
                    } 
                    if (prevProfiles == null) {
                        // Must start from still-stand. Clear the unconstrained flag
                        profiles[axis].clearOption(ProfileOption.UnconstrainedEntry);
                    }
                    else if (profiles[axis].s[0] == profiles[axis].s[segments]
                            && Math.abs(profiles[axis].getEffectiveEntryVelocity(profiles[axis].jMax) 
                                    + profiles[axis].getEffectiveExitVelocity(profiles[axis].jMax)) < vtol*2) {
                        // Within tolerance, re-apply reflection case
                        profiles[axis].v[0] = -profiles[axis].v[segments];
                        profiles[axis].a[0] = profiles[axis].a[segments];
                    }
                    else if (isCoordinated(prevProfiles)) {
                        // Set the entry constraints from the previous profile, but limited (can be lower than next segment).
                        profiles[axis].v[0] =  
                                Math.max(-profiles[axis].vMax, Math.min(profiles[axis].vMax, 
                                        Math.max(-prevProfiles[axis].vMax, Math.min(prevProfiles[axis].vMax,
                                                prevProfiles[axis].v[segments]))));
                        profiles[axis].a[0] =  
                                Math.max(-profiles[axis].aMaxEntry, Math.min(profiles[axis].aMaxEntry, 
                                        Math.max(-prevProfiles[axis].aMaxExit, Math.min(prevProfiles[axis].aMaxExit, 
                                                prevProfiles[axis].a[segments]))));
                        // Clear the unconstrained flag
                        profiles[axis].clearOption(ProfileOption.UnconstrainedEntry);
                    }
                    else {
                        // Can be left unconstrained
                        profiles[axis].setOption(ProfileOption.UnconstrainedEntry);    
                    }
                    // Remove option from pass 1.
                    profiles[axis].tMin = 0;
                    profiles[axis].clearOption(ProfileOption.UnconstrainedExit);
                    profiles[axis].solveProfile();
                    // clear the option for synchronize
                    profiles[axis].clearOption(ProfileOption.UnconstrainedExit);
                }
                synchronizeProfiles(profiles);
                validateProfiles(profiles);
            }
            nextProfiles = profiles;
        }

        System.out.println(" ------------- PASS 3 -------------------------");

        // Pass 3: Mend the two greedy strategies together:
        // For two consecutive coordinated moves this means taking the minimum of both junction speeds.
        // For two consecutive uncoordinated moves we take the blend i.e. the mean of both junction speeds.
        // For mixed coordinated/uncoordinated moves this means taking the one governed by the coordinated move.


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

    protected static double dotProduct(double[] unitVector1, double[] unitVector2) {
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

    protected static boolean isCoordinated(MotionProfile[] profile1) {
        if (profile1.length > 0) {
            return profile1[0].hasOption(ProfileOption.Coordinated);
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

        if (!hasOption(ProfileOption.UnconstrainedEntry)) {
            // Need to determine, if this profile has a constant acceleration segment or not. The latter happens
            // if the velocity is too low to reach the acceleration limit. In this case the profile will switch 
            // directly from positive to negative jerk, a.k.a. "S-curve" and it will not reach aMax.   
            // In order to calculate this, we need to get rid of entry/exit acceleration.

            // On entry: pretend to accelerate from a = 0, move t[1] backward/forward in time.
            double j1 = signumEntry*jMax;
            double t0early = a[0]/j1;
            // What is the velocity at that point?
            double v0early = v[0] - a[0]*t0early + 1./2*j1*Math.pow(t0early, 2);
            // Half time to reach velocity with S-curve
            double t1 = Math.sqrt(Math.max(0, (vPeak - v0early)/j1));
            // Acceleration at S-Curve inflection point.
            double a1 = t1*j1;
            // If the acceleration is smaller than the limit, we have an S-curve.
            double aMaxEntry = this.aMaxEntry;
            if (Math.abs(a1) < aMaxEntry) {
                // This is an S curve. 
                aMaxEntry = Math.abs(a1);
            }


            // Phase 1: Jerk to acceleration.
            j[1] = j1;
            t[1] = Math.max(0, signumEntry*aMaxEntry/j[1] - t0early); // can be cropped by tearly
            a[1] = a[0] + j[1]*t[1];
            s[1] = s[0] + v[0]*t[1] + 1./2*a[0]*Math.pow(t[1], 2) + 1./6*j[1]*Math.pow(t[1], 3); 
            v[1] = v[0] + a[0]*t[1]+ 1./2*j[1]*Math.pow(t[1], 2);

            // Phase 2: Constant acceleration. 
            a[2] = a[1];
            j[2] = 0;
            j[3] = -j1;              // Phase 3 look-ahead
            t[3] = (0 - a[2])/j[3];  // Phase 3 look-ahead
            v[2] = vPeak + 1./2*j[3]*Math.pow(t[3], 2);
            t[2] = (a[2] == 0 ? 0.0 : (v[2] - v[1])/a[2]);
            s[2] =  s[1] + v[1]*t[2] + 1./2*a[1]*Math.pow(t[2], 2);

            // Phase 3: Negative jerk to constant velocity/zero acceleration.
            v[3] = vPeak;
            a[3] = 0;
            s[3] = s[2] + v[2]*t[3] + 1./2*a[2]*Math.pow(t[3], 2) + 1./6*j[3]*Math.pow(t[3], 3); 

        }

        // Phase 4: Constant velocity
        j[4] = 0;
        // s and t ... needs to be postponed

        if (!hasOption(ProfileOption.UnconstrainedExit)) {
            v[4] = vPeak;
            a[4] = 0;

            // Need to determine, if this profile has a constant acceleration segment or not (see analogous entry section). 
            // On exit: pretend to decelerate to a = 0, move t[7] forward/backward in time.
            double j7 = signumExit*jMax;
            double t7late = -a[7]/j7;
            // What is the velocity at that point?
            double v7late = v[7] + a[7]*t7late + 1./2*j7*Math.pow(t7late, 2);
            // Half time to reach velocity with S-curve
            double t6 = Math.sqrt(Math.max(0, (vPeak - v7late)/j7));
            // Acceleration at S-Curve inflection point.
            double a6 = -t6*j7;
            // If the acceleration is smaller than the limit, we have an S-curve.
            double aMaxExit = this.aMaxExit;
            if (Math.abs(a6) < aMaxExit) {
                // This is an S curve. 
                aMaxExit = Math.abs(a6);
            }

            // Phase 5: Negative jerk to deceleration.
            j[5] = -j7;

            j[7] = j7;                  // Phase 7 look-ahead
            t[7] = Math.max(0, signumExit*aMaxExit/j[7] - t7late); // can be cropped by tlate
            a[6] = a[7] - j[7]*t[7];    // Phase 6 look-ahead

            a[5] = a[6];
            t[5] = a[5]/j[5];
            v[5] = v[4] + 1./2*j[5]*Math.pow(t[5], 2);
            // s ... needs to be postponed

            // Phase 6: Constant deceleration. 
            j[6] = 0;
            v[6] = v[7] - a[7]*t[7] + 1./2*j[7]*Math.pow(t[7], 2);  
            t[6] = (a[6] == 0 ? 0.0 : (v[6] - v[5])/a[6]);
            // s ... needs to be postponed

            // Phase 7: Jerk to exit acceleration.
            // ... already looked ahead.

            // reverse s calculation
            s[6] = s[7] - v[7]*t[7] + 1./2*a[7]*Math.pow(t[7], 2) - 1./6*j[7]*Math.pow(t[7], 3); 
            s[5] = s[6] - v[6]*t[6] + 1./2*a[6]*Math.pow(t[6], 2); 
            s[4] = s[5] - v[5]*t[5] + 1./2*a[5]*Math.pow(t[5], 2) - 1./6*j[5]*Math.pow(t[5], 3);

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

            vUnconstrainedEntry = v[4];
            tMinUnconstrainedEntry = tMin;
        }
        if (hasOption(ProfileOption.UnconstrainedExit)) {
            s[4] = s[7];
            v[4] = v[3];
            a[4] = 0;

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

            vUnconstrainedExit = v[3];
            tMinUnconstrainedExit = tMin;
        }

        // finally t[4] calculation

        if (v[4] == 0.0 && Math.abs(s[3] - s[4]) < eps) {
            t[4] = 0;
        }
        else {
            t[4] = (s[4] - s[3])/v[4];
        }
        time = 0;
        for (double ti : this.t) {
            time += ti;
        }
        if (tMin > time && v[4] == 0 && Math.abs(s[3] - s[4]) < eps) {
            // Zero velocity profile -> can adapt minimum time directly 
            // (important for moveToLocationAtSafeZ() "dome" scenario).
            t[4] = tMin - time;
            time = tMin;
        }

        computeBounds();
        eval++;
    }

    public Double getForwardCrossingTime(double sCross, boolean halfProfile) {
        double tSeg = 0;
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
        double tSeg = time;
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
        double j = this.j[i];

        if (Math.abs(s[i-1] - sCross) < eps) {
            return t[i-1];
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
                        return tSeg+ts;
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
                return ts;
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

            if (Math.abs(dydt) < vtol) {
                // Stop if the denominator is too small
                return null;
            }
            // Do Newton's computation. Limit to interval.
            double xn = Math.max(x0, Math.min(x1, x - y/dydt));  
            //System.out.println("  newton("+x0+", "+x1+") x="+x+" y="+y+" dydt="+dydt+" xn="+xn+" iter="+iter);
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
            tBound0 = 0.0;
            sBound1 = s[segments];
            tBound1 = time;
        }
        else {
            sBound0 = s[segments];
            tBound0 = time;
            sBound1 = s[0];
            tBound1 = 0.0;
        }
        double tSeg = 0;
        for (int i = 1; i <= segments; i++) {
            // Find the velocity crossing zero, that my be an extreme for s.
            if (j[i] != 0) {
                // 3rd order segment.
                double dt = Math.sqrt(Math.pow(a[i-1], 2) - 2*v[i-1]*j[i]);
                for (double tCross : new double[] { -(a[i-1] + dt)/j[i], -(a[i-1] - dt)/j[i] }) { 
                    if (tCross >= 0 && tCross <= t[i]) {
                        // Zero-crossing inside the period, maybe an extreme.
                        double sExtreme = s[i-1] + v[i-1]*tCross + 1./2*a[i-1]*Math.pow(tCross, 2) + 1./6*j[i]*Math.pow(tCross, 3);
                        if (sExtreme < sBound0) {
                            sBound0 = sExtreme;
                            tBound0 = tSeg + tCross;
                        }
                        if (sExtreme > sBound1) {
                            sBound1 = sExtreme;
                            tBound1 = tSeg + tCross;
                        }
                    }
                }
            }
            else if (a[i] != 0) {
                // 2nd order segment.
                double tCross = -v[i-1]/a[i];
                if (tCross >= 0 && tCross <= t[i]) {
                    // Zero-crossing inside the period, maybe an extreme.
                    double sExtreme = s[i-1] + v[i-1]*tCross + 1./2*a[i-1]*Math.pow(tCross, 2);
                    if (sExtreme < sBound0) {
                        sBound0 = sExtreme;
                        tBound0 = tSeg + tCross;
                    }
                    if (sExtreme > sBound1) {
                        sBound1 = sExtreme;
                        tBound1 = tSeg + tCross;
                    }
                }
            }
            tSeg += t[i];
        }
    }


    public double getEffectiveEntryVelocity(double jMax) {
        if (hasOption(ProfileOption.UnconstrainedEntry)) {
            return 0.0;
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
        else {
            double jExit = (a[segments] == 0 ? 1.0 : Math.signum(a[segments]))*jMax;
            double tExit = -a[segments]/jExit;  
            double vExit = v[segments] + a[segments]*tExit + 1./2*jExit*Math.pow(tExit, 2);
            return vExit;
        }
    }
}

