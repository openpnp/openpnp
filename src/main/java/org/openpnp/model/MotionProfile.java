package org.openpnp.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.openpnp.model.Motion.MotionOption;
import org.openpnp.spi.Axis;
import org.openpnp.util.NanosecondTime;

public class MotionProfile {
    final int segments = 7;

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

    static final int iterations = 250;
    static final double atol = 1.0; // mm/s^2
    static final double vtol = 1.0; // mm/s
    static final double ttol = 0.00001; // s 
    static final double teps = 0; // s epsilon (can be 0) 

    private int iter;

    private double time;

    private double solvingTime;

    private double sBound0;

    private double sBound1;

    private double tBound0;

    private double tBound1;

    private int motionOptions;
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
        solveProfile();
    }

    double getLocation(int segment) {
        return s[segment];
    }
    double getVelocity(int segment) {
        return v[segment];
    }
    double getAcceleration(int segment) {
        return a[segment];
    }
    double getJerk(int segment) {
        return a[segment];
    }
    
    protected double getMomentary(double time, BiFunction<Integer, Double, Double> f) {
        if (time < 0) {
            return s[0];
        }
        double tSeg = 0;
        for (int i = 1; i <=segments; i++) {
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

        if (s[0] == s[segments]
                && v[0] == v[segments]
                        && a[0] == a[segments]
                                && tMin == 0) {
            // Null move
            for (int i = 1; i < segments; i++) {
                s[i] = s[0];
                v[i] = v[0];
                a[i] = a[0];
                j[i] = 0;
                t[i] = 0;
            }
            time = 0;
            iter = 0;
            sBound0 = sBound1 = s[0];
            tBound0 = tBound1 = 0;
            return; // -----------------> 
        }

        boolean flipSignum = false;
        while(true) {

            double aEntry = aMaxEntry;
            double aEntry0 = 0;
            double aEntry1 = aMaxEntry;
            double aExit = aMaxExit;
            double aExit0 = 0;
            double aExit1 = aMaxExit;

            // Calculate the effective entry/exit velocity after jerk to acceleration 0.
            double vEntry = getEffectiveEntryVelocity(jMax);
            double vExit = getEffectiveExitVelocity(jMax);
            double vMin = Math.min(Math.abs(vEntry), Math.abs(vExit));
            double vPeak = vMax; 
            double vPeak0 = flipSignum ? 0 : vMin;
            double vPeak1 = vMax;
            
            // Perform simple bi-section solving.
            for (iter = 1; iter < iterations; iter++) {
                // Compute the profile with the given acceleration and velocity limits.
                computeProfile(vPeak, vEntry, vExit, aEntry, aExit, jMax, tMin, flipSignum);
                boolean aSettled = true;
                double dynatol = Math.max(atol, (vPeak1 - vPeak)/2);
                if (t[2] < -teps) { // || (t[4] == 0 && time < tMin)) {// || (t[4] == 0 && (s[4]-s[3])/a[2] < 0)) {
                    // Uh-oh, negative constant acceleration time
                    // or overshoot
                    // --> decrease acceleration limit
                    aEntry1 = aEntry;
                    aEntry = (aEntry1 + aEntry0)*0.5; 
                    aSettled = false;
                }
                else if (aEntry < aEntry1 - dynatol) { // || (tMin != 0.0 && t[4] == 0 && time > tMin + ttol)) {
                    // --> increase  acceleration limit
                    aEntry0 = aEntry;
                    aEntry = (aEntry1 + aEntry0)*0.5;
                    aSettled = false;
                }
                if (t[6] < -teps) { // || (t[4] == 0 && time < tMin)) {// || (t[4] == 0 && (s[4]-s[3])/a[5] < 0)) {
                    // Uh-oh, negative constant deceleration time
                    // or undershoot
                    // --> decrease deceleration limit
                    aExit1 = aExit;
                    aExit = (aExit1 + aExit0)*0.5; 
                    aSettled = false;
                }
                else if (aExit < aExit1 - dynatol) { // || (tMin > 0.0 && t[4] == 0 && time > tMin + ttol)) {
                    // --> increase deceleration limit
                    aExit0 = aExit;
                    aExit = (aExit1 + aExit0)*0.5;
                    aSettled = false;
                }
                if (aSettled) {
                    if (t[4] < -teps || (v[4] != 0 && time < tMin - ttol)) {
                        // Uh-oh, negative constant velocity time
                        // or minimum segment time not reached
                        // --> decrease velocity limit
                        vPeak1 = vPeak;
                        vPeak = (vPeak1 + vPeak0)*0.5;
                        // Need to open search for acceleration again
                        aEntry0 = 0;
                        aExit0 = 0;
                    }
                    else if (vPeak < vPeak1 - vtol || (tMin > 0.0 && v[4] != 0 && time > tMin + ttol)) {
                        // --> increase velocity limit
                        vPeak0 = vPeak;
                        vPeak = (vPeak1 + vPeak0)*0.5;
                        // Need to open search for acceleration again
                        aEntry1 = aMaxEntry;
                        aExit1 = aMaxExit;
                    }
                    else {
                        // Profile is valid -> test against external constraints
                        // TODO: limit v[0]/a[0] by sMin/sMax/tMax
                        break;
                    }
                    if (Math.abs(vPeak) <= vtol) {
                        vPeak = vtol*Math.signum(vPeak);
                    }
                }
            }
            if (iter < iterations || flipSignum) {
                if (flipSignum) {
                    iter += iterations;
                }
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

    public void coordinateProfile(MotionProfile leadProfile) {
        double dist = leadProfile.s[segments]-leadProfile.s[0];
        double factor; 
        if (dist == 0) {
            factor = 0;
        }
        else {
            factor = (s[segments]-s[0])/dist;
        }
        for (int i = 0; i <= segments; i++) {
            t[i] = leadProfile.t[i];
            s[i] = (leadProfile.s[i] - leadProfile.s[0])*factor + s[0];
            v[i] = leadProfile.v[i]*factor;
            a[i] = leadProfile.a[i]*factor;
            j[i] = leadProfile.j[i]*factor;
        }
        time = leadProfile.time;
        computeBounds();
    }

    public static void synchronizeProfiles(MotionProfile [] profiles) {
        double maxTime = 0;
        for (MotionProfile profile : profiles) {
            if (profile.time > maxTime) {
                maxTime = profile.time;
            }
        }
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

        // Then compare these to vMax to know whether we need to accelerate or decelerate on entry/exit.
        double signum = Math.signum(s[segments]-s[0]);
        if (signum == 0) {
            // Zero displacement, take entry/exit velocity balance as criterion. 
            signum = Math.signum(Math.round(vExit/vtol) + Math.round(vEntry/vtol));
            // Note, if this is still zero we just assume it's symmetric and we don't care 
        }
        if (flipSignum) {
            signum = -signum;
        }
        double signumIn, signumOut;
        signumIn = Math.signum(signum*vMax - vEntry);
        signumOut = Math.signum(signum*vMax - vExit);
        if (signumIn == 0) {
            signumIn = (signumOut == 0.0 ? 1.0 : signumOut);
        }
        if (signumOut == 0) {
            signumOut = signumIn;
        }

        // Phase 1: Jerk to acceleration.
        j[1] = signumIn*jMax;
        a[1] = signumIn*aEntry;
        t[1] = (a[1] - a[0])/j[1];  
        s[1] = s[0] + v[0]*t[1] + 1./2*a[0]*Math.pow(t[1], 2) + 1./6*j[1]*Math.pow(t[1], 3); 
        v[1] = v[0] + a[0]*t[1]+ 1./2*j[1]*Math.pow(t[1], 2);

        // Phase 2: Constant acceleration. 
        a[2] = signumIn*aEntry;
        j[2] = 0;
        j[3] = -signumIn*jMax;   // Phase 3 look-ahead
        t[3] = (0 - a[2])/j[3];  // Phase 3 look-ahead
        v[2] = signum*vMax + 1./2*j[3]*Math.pow(t[3], 2);
        t[2] = (v[2] - v[1])/a[2];
        s[2] =  s[1] + v[1]*t[2] + 1./2*a[1]*Math.pow(t[2], 2);

        // Phase 3: Negative jerk to constant velocity/zero acceleration.
        v[3] = signum*vMax;
        a[3] = 0;
        s[3] = s[2] + v[2]*t[3] + 1./2*a[2]*Math.pow(t[3], 2) + 1./6*j[3]*Math.pow(t[3], 3); 

        // Phase 4: Constant velocity
        v[4] = signum*vMax;
        a[4] = 0;
        j[4] = 0;
        // s and t ... needs to be postponed

        // Phase 5: Negative jerk to deceleration.
        j[5] = -signumOut*jMax;
        a[5] = -signumOut*aExit;
        t[5] = a[5]/j[5];
        v[5] = signum*vMax + 1./2*j[5]*Math.pow(t[5], 2);
        // s ... needs to be postponed

        // Phase 6: Constant deceleration. 
        a[6] = -signumOut*aExit;
        j[6] = 0;
        j[7] = signumOut*jMax;       // Phase 7 look-ahead
        t[7] = (a[7] - a[6])/j[7];  // Phase 7 look-ahead
        v[6] = v[7] - a[7]*t[7] + 1./2*j[7]*Math.pow(t[7], 2);  
        t[6] = (v[6] - v[5])/a[6];
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
        for (double t : this.t) {
            time += t;
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

