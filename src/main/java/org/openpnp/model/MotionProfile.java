package org.openpnp.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.openpnp.model.Motion.MotionOption;
import org.openpnp.spi.Axis;
import org.openpnp.util.NanosecondTime;

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

    static final int iterations = 50;
    static final double atol = 1.0; // mm/s^2
    static final double vtol = 1.0; // mm/s
    static final double ttol = 0.00001; // s 

    private int iter;

    private double time;

    private double solvingTime;

    private double sBound0;

    private double sBound1;

    private double tBound0;

    private double tBound1;

    private int motionOptions;

    private boolean solved;
    public boolean hasOption(MotionOption option) {
        return (this.motionOptions & option.flag()) != 0;
    }

    public MotionProfile(double s0, double s1, double v0, double v1, double a0, double a1,
            double sMin, double sMax, double vMax, double aMaxEntry, double aMaxExit, double jMax, double tMin, double tMax,
            int motionOptions) {
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
        this.motionOptions = motionOptions;
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
        return solved;
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
        double tSeg = 0;
        for (int i = 1; i <= segments; i++) {
            if (time >= tSeg && time <= t[i]) {
                return f.apply(i, time - t[i-1]);
            }
            tSeg += t[i];
        }
        return s[segments];
    }

    double getMomentaryLocation(double time) { // s0 + V0*t + 1/2*a0*t^2 + 1/6*j*t^3
        return getMomentary(time, (i, ts) -> (s[i-1] + v[i-1]*ts + 1./2*a[i-1]*Math.pow(ts, 2) + 1./6*j[i]*Math.pow(ts, 3)));
    }

    double getMomentaryVelocity(double time) { // V0 + a0*t + 1/2*j*t^2
        return getMomentary(time, (i, ts) -> (v[i-1] + a[i-1]*ts + 1./2*j[i]*Math.pow(ts, 2)));
    }

    double getMomentaryAcceleration(double time) { // a0 + j*t
        return getMomentary(time, (i, ts) -> (a[i-1] + j[i]*ts));
    }

    double getMomentaryJerk(double time) { 
        return getMomentary(time, (i, ts) -> (j[i]));
    }

    public enum ErrorState {
        NegativeSegmentTime,
        TimeSumMismatch,
        LocationDiscontinuity,
        VelocityDiscontinuity,
        AccelerationDiscontinuity,
        MinLocationViolated,
        MaxLocationViolated,
        MaxVelocityViolated,
        MaxAccelerationViolated,
        MaxJerkViolated,
        MinTimeViolated,
        MaxTimeViolated,
    }
    
    // As we're handling millimeters and seconds, we can use a practical eps. 
    static final double eps = 1e-8;
    public ErrorState assertValidity() {
        // Assert continuity.
        double tSum = 0;
        for (int i = 1; i <= segments; i++) {
            // Check timing.
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
            // Check constraints. 
            if (i < segments && s[i] < sMin - eps) {
                return ErrorState.MinLocationViolated;
            }
            if (i < segments && s[i] > sMax + eps) {
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
            if (Math.abs(j[i]) > jMax + atol) {
                return ErrorState.MaxJerkViolated;
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
        // TODO: check bounds
        // No error.
        return null;
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
            str.append(String.format(" %.0f", v));
        }
        str.append(", a =");
        for (double a : this.a) {
            str.append(String.format(" %.0f", a));
        }
        str.append(", j =");
        for (double j : this.j) {
            str.append(String.format(" %.0f", j));
        }
        str.append(", t =");
        for (double t : this.t) {
            str.append(String.format(" %.4f", t));
        }
        str.append(", time = ");
        str.append(String.format("%.6f", time));
        str.append("s, bound = ");
        str.append(String.format("%.2f@%.4f, %.2f@%.4f", sBound0, tBound0, sBound1, tBound1));
        str.append(", iter = ");
        str.append(iter);
        str.append(", ms = ");
        str.append(String.format("%.3f", solvingTime*1000));
        str.append("}");
        return str.toString();
    }

    public void solveProfile() {
        solveProfile(iterations, vtol, atol, ttol);
    }
    public void solveProfile(final int iterations, final double vtol, final double atol, final double ttol) {
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
            iter = 0;
            sBound0 = sBound1 = s[0];
            tBound0 = tBound1 = 0;
            return; // -----------------> 
        }

        // Calculate the effective entry/exit velocity after jerk to acceleration 0.
        double vEntry = getEffectiveEntryVelocity(jMax);
        double vExit = getEffectiveExitVelocity(jMax);

        // Solver loop.
        boolean flipSignum = false;
        iter = 1;
        while(true) {

            double aEntry = aMaxEntry;
            double aExit = aMaxExit;
            // Need to search in different domains. Above vEntry, below vEntry, below zero.
            double vMin = flipSignum ? 0 : Math.abs(vEntry);
            double vPeak = vMax; 
            double vPeak0 = vMin;
            double vPeak1 = vMax;

            // Perform simple bi-section solving.
            boolean degenerate = false;
            int initialGuesses = 2;
            double lastError = Double.POSITIVE_INFINITY;
            for (; iter <= iterations; iter++) {
                // Compute the profile with the given acceleration and velocity limits.
                computeProfile(vPeak, vEntry, vExit, aEntry, aExit, jMax, tMin, flipSignum);
                double signum = 1;//flipSignum ? Math.signum(-a[1]) : Math.signum(a[1]);
                double error =
                        Math.pow(Math.max(0, -t[4]), 2)
                        + (tMin > 0 ? Math.pow(time-tMin, 2) : 0);
                //System.out.println("--- profile "+this+" min-time="+tMin+" time = "+time+" err = "+error);
                if (t[4] < 0 || (v[4] != 0 && time < tMin - ttol)) {
                    // Uh-oh, negative constant velocity time
                    // or minimum segment time not reached
                    // --> decrease velocity limit
                    vPeak1 = vPeak;
                    if (initialGuesses > 0 && t[4] < 0 && tMin < time) {
                        // Initial guess, try solving it analytically. 
                        // Try reducing the constant acceleration.
                        double s3 = (s[4] + s[3])*0.5 - s[1];
                        double v3_1 = signum*(-1./6*(3*Math.pow(a[1], 2) 
                                - 2*Math.sqrt(3*Math.pow(a[1], 4) + 18*a[1]*Math.pow(j[1], 2)*s3 + 9*Math.pow(j[1], 2)*Math.pow(v[1], 2)))/j[1]);
                        double v3_2 = signum*(-1./6*(3*Math.pow(a[1], 2) 
                                + 2*Math.sqrt(3*Math.pow(a[1], 4) + 18*a[1]*Math.pow(j[1], 2)*s3 + 9*Math.pow(j[1], 2)*Math.pow(v[1], 2)))/j[1]);
                        // -1/6*(3*a^2 + 2*sqrt(3*a^4 + 18*a*j^2*s3 + 9*j^2*v1^2))/j,
                        if (v3_1 < vPeak1 && v3_1 > vPeak0) {
                            vPeak = v3_1;
                        }
                        else if (v3_2 < vPeak1 && v3_2 > vPeak0) {
                            vPeak = v3_2;
                        }
                        else {
                            vPeak = (vPeak1 + vPeak0)*0.5;
                        }
                        System.out.println("symbolic constant acceleration solution = ("+v3_1+", "+v3_2+") taken "+vPeak);
                    }
                    else {
                        // Just use bisection
                        vPeak = (vPeak1 + vPeak0)*0.5;
                    }
                    initialGuesses--;
                    if (vPeak - vMin < vtol) {
                        // No progress
                        if (vMin > 0) {
                            // try letting it go below V min
                            vPeak1 = vMin;
                            vMin = 0;
                            vPeak0 = vMin;
                            vPeak = (vPeak1 + vPeak0)*0.5;
                            initialGuesses = 2;
                            System.out.println("*** search lower velocity");
                        }
                        else {
                            degenerate = true;
                            System.out.println("*** search twisted");
                            break;
                        }
                    }
                    lastError = error;
                    //System.out.println("vPeak = "+vPeak);
                }
                else if ((t[4] > ttol && vPeak < vPeak1 - vtol) || (t[4] > 0 && tMin > 0.0 && v[4] != 0 && time > tMin + ttol)) {
                    // --> increase velocity limit
                    vPeak0 = vPeak;
                    if (initialGuesses > 0) {
                        vPeak = (vPeak1 + vPeak0)*0.1;
                        initialGuesses--;
                    }
                    else {
                        // Just use bisection
                        vPeak = (vPeak1 + vPeak0)*0.5;
                    }
                    //System.out.println("vPeak = "+vPeak);
                }
                else {
                    // Profile is valid -> test against external constraints
                    // TODO: limit v[0]/a[0] by sMin/sMax/tMax
                    break;
                }
                /*if (Math.abs(vPeak) <= vtol) {
                    vPeak = vtol*Math.signum(vPeak);
                }*/

            }
            if (flipSignum || !degenerate) {
                break;
            }
            // Try again by reversing the direction of the move. This is necessary when entry velocities/accelerations
            // cause overshoot or when tMin is larger than can be accommodated.
            // TODO: calc flipped signum in parallel? 
            flipSignum = true;
        }

        // The time was approximated to ttol, scale it to match perfectly.
        retimeProfile(tMin);
        // Result is now stored in the profile i.e. you can get v[4], a[2], a[6] to get the (signed) solution.
        solvingTime = NanosecondTime.getRuntimeSeconds() - tStart;
        solved = true;
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
        if (!solved) {
            solveProfile();
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
            for (MotionProfile profile : profiles) { 
                if (profile != leadProfile) {
                    double factor; 
                    if (bestDist == 0) {
                        factor = 0;
                    }
                    else {
                        factor = (profile.s[segments]-profile.s[0])/bestDist;
                    }
                    for (int i = 0; i <= segments; i++) {
                        profile.t[i] = leadProfile.t[i];
                        profile.s[i] = (leadProfile.s[i] - leadProfile.s[0])*factor + profile.s[0];
                        profile.v[i] = leadProfile.v[i]*factor;
                        profile.a[i] = leadProfile.a[i]*factor;
                        profile.j[i] = leadProfile.j[i]*factor;
                    }
                    profile.time = leadProfile.time;
                    profile.computeBounds();
                    // Treat as if solved.
                    profile.iter = 0;
                    profile.solvingTime = 0;
                    profile.solved = true;
                }
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
            }
        }
        // Re-time the others.
        for (MotionProfile profile : profiles) {
            profile.tMin = maxTime;
            if (profile.time != maxTime) {
                profile.solveProfile();
            }
        }
    }


    public static void solvePath(List<MotionProfile []> path) {
        // Coordinate uncoordinated moves to min-time

    }

    public void computeProfile(double vMax, double vEntry, double vExit, double aEntry, double aExit, double jMax, double tMin, boolean flipSignum) {
        // Determine the direction of travel.
        double signum = Math.signum(s[segments]-s[0]);
        if (signum == 0) {
            // Zero displacement, take entry/exit velocity balance as criterion. 
            signum = Math.signum(Math.round(vExit/vtol) + Math.round(vEntry/vtol));
            // Note, if this is still zero we just assume it's symmetric and we don't care 
        }
        if (flipSignum) {
            signum = -signum;
        }
        // Compare effective entry/exit velocities to vMax to know whether we need to accelerate or decelerate on entry/exit.
        double signumIn, signumOut;
        signumIn = Math.signum(signum*vMax - vEntry);
        signumOut = Math.signum(signum*vMax - vExit);
        if (signumIn == 0) {
            signumIn = (signumOut == 0.0 ? 1.0 : signumOut);
        }
        if (signumOut == 0) {
            signumOut = signumIn;
        }

        // Need to determine, if this profile has a constant acceleration segment or not. The latter happens
        // if the velocity is too low to reach the acceleration limit. In this case the profile will switch 
        // directly from positive to negative jerk, a.k.a. "S-curve".   
        // In order to calculate this, we need to get rid of entry/exit acceleration.

        // On entry: pretend to accelerate from a = 0, move t[1] backward/forward in time.
        double j1 = signumIn*jMax;
        double v3 = signum*vMax;
        double t0early = a[0]/j1;
        // What is the velocity at that point?
        double v0early = v[0] - a[0]*t0early + 1./2*j1*Math.pow(t0early, 2);
        // Half time to reach velocity with S-curve
        double t1 = Math.sqrt(Math.max(0, (v3 - v0early)/j1));
        // Acceleration at S-Curve inflection point.
        double a1 = t1*j1;
        // If the acceleration is smaller than the limit, we have an S-curve.
        if (Math.abs(a1) < aEntry) {
            // This is an S curve. 
            aEntry = Math.abs(a1);
        }

        // On exit: pretend to decelerate to a = 0, move t[7] forward/backward in time.
        double j7 = signumOut*jMax;
        double v4 = v3;
        double t7late = -a[7]/j7;
        // What is the velocity at that point?
        double v7late = v[7] + a[7]*t7late + 1./2*j7*Math.pow(t7late, 2);
        // Half time to reach velocity with S-curve
        double t6 = Math.sqrt(Math.max(0, (v4 - v7late)/j7));
        // Acceleration at S-Curve inflection point.
        double a6 = -t6*j7;
        // If the acceleration is smaller than the limit, we have an S-curve.
        if (Math.abs(a6) < aExit) {
            // This is an S curve. 
            aExit = Math.abs(a6);
        }

        // Phase 1: Jerk to acceleration.
        j[1] = j1;
        t[1] = Math.max(0, signumIn*aEntry/j[1] - t0early); // can be cropped by tearly
        a[1] = a[0] + j[1]*t[1];
        s[1] = s[0] + v[0]*t[1] + 1./2*a[0]*Math.pow(t[1], 2) + 1./6*j[1]*Math.pow(t[1], 3); 
        v[1] = v[0] + a[0]*t[1]+ 1./2*j[1]*Math.pow(t[1], 2);

        // Phase 2: Constant acceleration. 
        a[2] = a[1];
        j[2] = 0;
        j[3] = -j1;              // Phase 3 look-ahead
        t[3] = (0 - a[2])/j[3];  // Phase 3 look-ahead
        v[2] = signum*vMax + 1./2*j[3]*Math.pow(t[3], 2);
        t[2] = (a[2] == 0 ? 0.0 : (v[2] - v[1])/a[2]);
        s[2] =  s[1] + v[1]*t[2] + 1./2*a[1]*Math.pow(t[2], 2);

        // Phase 3: Negative jerk to constant velocity/zero acceleration.
        v[3] = v3;
        a[3] = 0;
        s[3] = s[2] + v[2]*t[3] + 1./2*a[2]*Math.pow(t[3], 2) + 1./6*j[3]*Math.pow(t[3], 3); 

        // Phase 4: Constant velocity
        v[4] = v[3];
        a[4] = 0;
        j[4] = 0;
        // s and t ... needs to be postponed

        // Phase 5: Negative jerk to deceleration.
        j[5] = -j7;
        
        j[7] = j7;                  // Phase 7 look-ahead
        t[7] = Math.max(0, signumOut*aExit/j[7] - t7late); // can be cropped by tlate
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

        // finally t[4] calculation
        if (v[4] == 0.0) {
            t[4] = 0;
        }
        else {
            t[4] = (s[4] - s[3])/v[4];
        }
        time = 0;
        for (double ti : this.t) {
            time += ti;
        }
        if (tMin > time && v[4] == 0) {
            // Zero velocity profile -> can adapt minimum time directly 
            // (important for moveToLocationAtSafeZ() "dome" scenario).
            t[4] = tMin - time;
            time = tMin;
        }

        computeBounds();
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
        double jEntry = (a[0] == 0 ? 1.0 : -Math.signum(a[0]))*jMax;
        double tEntry = -a[0]/jEntry;  
        double vEntry = v[0] + a[0]*tEntry + 1./2*jEntry*Math.pow(tEntry, 2);
        return vEntry;
    }

    public double getEffectiveExitVelocity(double jMax) {
        double jExit = (a[segments] == 0 ? 1.0 : Math.signum(a[segments]))*jMax;
        double tExit = -a[segments]/jExit;  
        double vExit = v[segments] + a[segments]*tExit + 1./2*jExit*Math.pow(tExit, 2);
        return vExit;
    }
}

