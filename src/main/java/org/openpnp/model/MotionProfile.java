package org.openpnp.model;

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

    final int iterations = 1000;
    final double atol = 1.0; // mm/s^2
    final double vtol = 1.0; // mm/s
    final double ttol = 0.001; // s 
    final double teps = ttol*ttol; // s 

    private int iter;

    private double time;

    private double solvingTime;

    public MotionProfile(double s0, double s1, double v0, double v1, double a0, double a1,
            double sMin, double sMax, double vMax, double aMaxEntry, double aMaxExit, double jMax, double tMin, double tMax) {
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

        solveProfile(iterations, vtol, atol, ttol);
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

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("(s =");
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
            str.append(String.format(" %.6f", t));
        }
        str.append(", time = ");
        str.append(String.format("%.6f", time));
        str.append(", iter = ");
        str.append(iter);
        str.append(", ms = ");
        str.append(String.format("%.3f", solvingTime*1000));
        str.append(")");
        return str.toString();
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
            }
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
            for (iter = 0; iter < iterations; iter++) {
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
                    if (vPeak == 0) {
                        // Assume it's solved.
                        break;
                    }
                    else if (t[4] < -teps || (v[4] != 0 && time < tMin - ttol)) {
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
                        // done
                        // TODO: limit v[0]/a[0] by sMin/sMax/tMax
                        break;
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
            // cause overshoot or when tMin is larger than can be accomodated.
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

    private void retimeProfile(double tMin) {
        if (tMin != time && tMin > 0.0 && time > 0) {
            double factor = time/tMin;
            double factor2 = factor*factor;
            double factor3 = factor2*factor;
            for (int i = 0; i <= segments; i++) {
                t[i] /= factor;
                v[i] *= factor;
                a[i] *= factor2;
                j[i] *= factor3;
            }
            time /= factor;
        }
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
            // (important for moveToLocationAtSafeZ() scenario).
            t[4] = tMin - time;
            time = tMin;
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

