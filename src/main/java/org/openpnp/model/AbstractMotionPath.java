package org.openpnp.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.openpnp.model.MotionProfile.ProfileOption;
import org.openpnp.util.XmlSerialize;
import org.python.modules.math;

public abstract class AbstractMotionPath implements Iterable<MotionProfile []> {
    final double adaption = 0.38; // 0.38
    final int iterations = 3;    // 3

    protected final static int segments = MotionProfile.segments; 
    
    public abstract int size();
    public abstract MotionProfile [] get(int i);

    public Iterator<MotionProfile []> iterator() {
        return new PathIterator();
    }

    private final class PathIterator implements
    Iterator<MotionProfile []> {
        private int i = 0;

        @Override 
        public boolean hasNext() {
            return this.i < size();
        }

        @Override 
        public MotionProfile [] next() {
            if (this.hasNext()) {
                return get(i++);
            }
            throw new NoSuchElementException();
        }

        @Override 
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public void solve() throws Exception {
        solve(adaption, iterations);
    }
    
    /**
     *  <h1>Simplified "PnP use case" heuristics for continuous smoothed motion path optimization.</h1> 
     *  <p>
     *  The solver can find a partially optimized path for the typical moveToLocationAtSaveZ() moving pattern of OpenPnP.
     *  It will gracefully revert to unoptimized moving when confronted with more complex motion sequences. 
     *  </p><p>
     *  Moves are distinguished to be coordinated or not. A coordinated move follows a strict straight line from A to B while
     *  an uncoordinated moves may stray from that line to allow for a more smoothed-out motion. OpenPnP will handle all below 
     *  Safe Z moves as coordinated, while handling everything within the Safe Z zone as uncoordinated.Code may override this 
     *  default heuristic. 
     *  </p><p>   
     *  Subsequent straight coordinated moves are combined into one. Coordinated moves will overshoot into 
     *  uncoordinated moves to smooth the path. The path is then iteratively refined to reduce overshooting where it is 
     *  wasteful. However this is done in a very crude form with very few iterations to keep computation times useful. 
     *  Subsequent uncoordinated moves are not yet optimized, i.e. corners are not smoothed-out. These moves are rare
     *  in OpenPnP. 
     *  </p><p>
     *  The heuristics works as follows:
     *  </p>
     *  <ol>
     *  <li>Path begin/end and corners between subsequent coordinated moves are handled as zero speed and acceleration junctions.</li>
     *      
     *  <li>Multiple subsequent co-linear moves are solved as one overall move with the most restrictive limits applied.
     *      Each partial move is then cut out from the overall profile. TODO: Moves which reach the most restrictive V max at one 
     *      or both ends are re-solved with their proper V max, if higher (this is not 100% optimal but not so bad for practical 
     *      use cases).</li>
     *      
     *  <li>Coordinated moves that are followed by an uncoordinated move are handled in two ways:<br/>
     *      a) if there is an applicable s limit, the move is handled as if it extends to the limit and then cropped<br/>
     *      b) if there is no applicable s limit, the move is handled as half-sided profile and then extended or cropped.</li>
     *      
     *  <li>Coordinated moves that are preceded by an uncoordinated move are handled as in 3. (in reverse).</li>
     *   
     *  <li>Coordinated moves that are both preceded and followed by an uncoordinated move are left unoptimized at the moment.</li>
     *  </ol>
     *    
     * @param adaptation How much of the wasted time is attributed to one overshooting side of the move, as a factor, per iteration.  
     * @param iterations How many iterations should be computed. 
     * @throws Exception
     */
    public void solve(double adaptation, int iterations) throws Exception {
        // Basic empty test.
        int size = size();
        int last = size - 1;
        if (last < 0) {
            return;
        }
        // Preparation: some data about the Path
        int [] leadAxis= new int[size];
        double[][] unitVector = new double[size][];
        double [] junctionCosineFromPrev = new double[size];
        int [] colinearWithPrev = new int[size];
        boolean[] simplifiedSCurve = new boolean[size];

        for (int i = 0; i <= last; i++) {
            MotionProfile [] profiles = get(i);
            if (profiles.length == 0) {
                // Zero dimensions?
                return;
            }
            unitVector[i] = MotionProfile.getUnitVector(profiles);
            leadAxis[i] = MotionProfile.getLeadAxisIndex(unitVector[i]);
            simplifiedSCurve[i] = false;
            for (int axis = 0; axis < profiles.length; axis++) {
                simplifiedSCurve[i] |= profiles[axis].hasOption(ProfileOption.SimplifiedSCurve);
            }
            if (i > 0) {
                junctionCosineFromPrev[i] = MotionProfile.dotProduct(unitVector[i-1], unitVector[i]);
                colinearWithPrev[i] = 0;
                if (leadAxis[i] == leadAxis[i-1]
                        && simplifiedSCurve[i] == simplifiedSCurve[i-1]) {
                    colinearWithPrev[i] = ((junctionCosineFromPrev[i] >= 1.0 - MotionProfile.eps) ? 1 
                            : (junctionCosineFromPrev[i] <= -1.0 + MotionProfile.eps) ? -1 
                                    : 0);
                }
            }
        }
        int dimensions = unitVector[0].length;
        double[][] straightLineTime = new double[size][dimensions];
        double[] straightLineCoordinatedTime = new double[size];

        
        for (int iteration = 0; iteration < iterations; iteration++) {
            int iNext;
            boolean hasUncoordinated = false;
            for (int i = 0; i <= last; i = iNext) {
                iNext = i+1;
                MotionProfile [] profiles = get(i);
                MotionProfile [] prevProfiles = (i > 0 ? get(i-1) : null);
                if (simplifiedSCurve[i]) {
                    // We can only handle them as single coordinated moves for now, because they don't support acceleration != 0 in junctions. 
                    int lead = leadAxis[i];
                    if (profiles[lead].assertSolved()) {
                        MotionProfile.coordinateProfiles(profiles);
                    }
                }
                else if (MotionProfile.isCoordinated(profiles)) {
                    int lead = leadAxis[i];
                    // This may be a sequence of multiple co-linear moves: try create a spanning profile.
                    MotionProfile solverProfile = new MotionProfile(profiles[lead]);
                    MotionProfile [] exitProfiles = profiles;
                    for (int j = i+1; j <= last; j++) {
                        MotionProfile [] seqProfiles = get(j);
                        if (!(MotionProfile.isCoordinated(seqProfiles) && colinearWithPrev[j] == 1)) {
                            // Sequence ended.
                            break;
                        }
                        if (solverProfile.getVelocityMax() < seqProfiles[lead].getVelocityMax()) {
                            solverProfile.setVelocityMax(seqProfiles[lead].getVelocityMax());
                        }
                        if (solverProfile.getEntryAccelerationMax() < seqProfiles[lead].getEntryAccelerationMax()) {
                            solverProfile.setEntryAccelerationMax(seqProfiles[lead].getEntryAccelerationMax());
                        }
                        if (solverProfile.getExitAccelerationMax() < seqProfiles[lead].getExitAccelerationMax()) {
                            solverProfile.setExitAccelerationMax(seqProfiles[lead].getExitAccelerationMax());
                        }
                        // Extend to include this move.
                        solverProfile.s[segments] = seqProfiles[lead].s[segments];
                        // Also take the exit constraints (in case it's the's path exit condition). 
                        solverProfile.v[segments] = seqProfiles[lead].v[segments];
                        solverProfile.a[segments] = seqProfiles[lead].a[segments];
                        // Remember how far this sequence reaches.
                        iNext = j+1;
                        exitProfiles = seqProfiles;
                    }
                    MotionProfile [] nextProfiles = (iNext <= last ? get(iNext) : null);
                    if (iteration > 0) {
                        // This is a further refinement.
                        double timeWastedEntry = prevProfiles == null ? 0
                                : (prevProfiles[lead].time - straightLineCoordinatedTime[i-1]);
                        double timeWastedExit = nextProfiles == null ? 0
                                : (nextProfiles[lead].time - straightLineCoordinatedTime[iNext]);
                        controlOvershoot(prevProfiles, profiles, exitProfiles, nextProfiles, lead,
                                timeWastedEntry, timeWastedExit, solverProfile, adaption, iteration);
                    }
                    else {
                        boolean expandEntry = false;
                        boolean expandExit = false;
                        if (prevProfiles != null) {
                            if (MotionProfile.isCoordinated(prevProfiles)) { 
                                // If the previous profiles are coordinated they cannot be positively co-linear, otherwise they would be in the sequence.
                                assert(colinearWithPrev[i] != 1);
                                // This means we have a corner. Start from zero velocity/acceleration. 
                            }
                            else { // Uncoordinated previous.
                                if (unitVector[i][lead] > 0) {
                                    // Going positive, take sMin into consideration 
                                    if (Double.isFinite(prevProfiles[lead].sMin)) {
                                        solverProfile.s[0] = prevProfiles[lead].sMin;
                                    }
                                    else {
                                        expandEntry = true;
                                    }
                                }
                                else {
                                    // Going negative, take sMax into consideration 
                                    if (Double.isFinite(prevProfiles[lead].sMax)) {
                                        solverProfile.s[0] = prevProfiles[lead].sMax;
                                    }
                                    else {
                                        expandEntry = true;
                                    }
                                }
                            }
                            solverProfile.v[0] = 0;
                            solverProfile.a[0] = 0;
                        }
                        if (nextProfiles != null) {
                            if (MotionProfile.isCoordinated(nextProfiles)) { 
                                // If the next profiles are coordinated they cannot be positively co-linear, otherwise they would be in the sequence.
                                assert(colinearWithPrev[iNext] != 1);
                                // This means we have a corner. Stop to zero velocity/acceleration. 
                            }
                            else { // Uncoordinated next.
                                if (unitVector[i][lead] < 0) {
                                    // Going negative, take sMin into consideration 
                                    if (Double.isFinite(nextProfiles[lead].sMin)) {
                                        solverProfile.s[segments] = nextProfiles[lead].sMin;
                                    }
                                    else {
                                        expandExit = true;
                                    }
                                }
                                else {
                                    // Going positive, take sMax into consideration 
                                    if (Double.isFinite(nextProfiles[lead].sMax)) {
                                        solverProfile.s[segments] = nextProfiles[lead].sMax;
                                    }
                                    else {
                                        expandExit = true;
                                    }
                                }
                            }
                            solverProfile.v[segments] = 0;
                            solverProfile.a[segments] = 0;
                        }
                        if (iNext > last) { 
                            if (solverProfile.hasOption(ProfileOption.Jog)) {    
                                // This is the last move and a Jog, set the option to have open velocity/acceleration.
                                expandExit = true;
                                solverProfile.v[segments] = 0;
                                solverProfile.a[segments] = 0;
                            }
                        }
                        if (expandEntry || expandExit) {
                            // Entry and/or exit is expanded. .
                            solverProfile.solveByExpansion(Math.signum(unitVector[i][lead]), expandEntry, expandExit);
                        }
                        else {
                            // Solve to boundary conditions.
                            solverProfile.solve();
                        }
                    }
                    //validate("["+i+"]["+lead+"]", solverProfile);

                    // Cut this along the sequence.
                    double t0 = solverProfile.getForwardCrossingTime(profiles[lead].s[0], false);
                    for (int j = i; j < iNext; j++) {
                        MotionProfile [] seqProfiles = get(j);
                        // Note, we can always use forward crossing time, because in coordinated moves there is no sign reversal.
                        double t1 = solverProfile.getForwardCrossingTime(seqProfiles[lead].s[segments], false);
                        seqProfiles[lead].extractProfileSectionFrom(solverProfile, t0, t1);
                        // TODO: if the extracted move has reached solverProfile.vMax on entry/exit it may be re-optimized using its higher
                        // vMax i.e. while pinching down entry/exit velocity and acceleration, we can re-solve it.
                        MotionProfile.coordinateProfilesToLead(seqProfiles, seqProfiles[lead]);
                        //MotionProfile.validateProfiles(seqProfiles);
                        t0 = t1;
                    }
                }
                else {
                    if (iteration == 0) {
                        // Very first iteration: determine basic straight-line time.
                        straightLineCoordinatedTime[i] = 0;
                        for (int axis = 0; axis < dimensions; axis++) {
                            if (profiles[axis].tMin != 0) {
                                // Purge the minimum time.
                                profiles[axis].tMin = 0;
                                profiles[axis].solve();
                            }
                            profiles[axis].assertSolved();
                            straightLineTime[i][axis] = profiles[axis].time;
                            straightLineCoordinatedTime[i] = Math.max(profiles[axis].time, straightLineCoordinatedTime[i]);
                        }
                    }

                    // Clear all solved flags.
                    for (int axis = 0; axis < dimensions; axis++) {
                        profiles[axis].clearOption(ProfileOption.Solved);
                        profiles[axis].setTimeMin(0);
                    }
                    hasUncoordinated = true;
                }
            }

            while (hasUncoordinated) {
                hasUncoordinated = false;
                for (int i = 0; i <= last; i++) {
                    MotionProfile [] profiles = get(i);
                    MotionProfile [] prevProfiles = (i > 0 ? get(i-1) : null);
                    MotionProfile [] nextProfiles = (i < last ? get(i+1) : null);
                    if (! MotionProfile.isCoordinated(profiles)) {
                        boolean hasSolved = false;
                        for (int axis = 0; axis < dimensions; axis++) {
                            if (!profiles[axis].hasOption(ProfileOption.Solved)) {
                                boolean solve = false;
                                boolean expandEntry = false;
                                boolean expandExit = false;
                                double vEffEntry = 0;
                                double vEffExit = 0;
                                if (prevProfiles == null) {
                                    // Take path entry conditions as is.
                                    solve = true;
                                }
                                else if (prevProfiles[axis].hasOption(ProfileOption.Solved)) {
                                    profiles[axis].v[0] = prevProfiles[axis].v[segments];
                                    profiles[axis].a[0] = prevProfiles[axis].a[segments];
                                    vEffEntry = profiles[axis].getEffectiveEntryVelocity(profiles[axis].jMax);
                                    solve = true; 
                                }
                                else {
                                    expandEntry = true;
                                }
                                if (nextProfiles == null) {
                                    // Take path exit conditions as is.
                                    solve = true;
                                }
                                else if (nextProfiles[axis].hasOption(ProfileOption.Solved)) {
                                    profiles[axis].v[segments] = nextProfiles[axis].v[0];
                                    profiles[axis].a[segments] = nextProfiles[axis].a[0];
                                    vEffExit = profiles[axis].getEffectiveExitVelocity(profiles[axis].jMax);
                                    solve = true; 
                                }
                                else {
                                    expandExit = true;
                                }
//                                if (solve && (expandEntry || expandExit)) {
//                                    // Entry and/or exit expansion, solve using an overreaching profile.
//                                    MotionProfile solverProfile = new MotionProfile(profiles[axis]);
//                                    double signum = solverProfile.profileSignum(vEffEntry, vEffExit);
//                                    if (iteration > 0) {
//                                        // This is a further refinement.
//                                        double timeWastedEntry = prevProfiles == null ? 0 : (prevProfiles[axis].time - straightLineCoordinatedTime[i-1]);
//                                        double timeWastedExit = nextProfiles == null ? 0 : (nextProfiles[axis].time - straightLineCoordinatedTime[i+1]);
//                                        controlOvershoot(prevProfiles, profiles, profiles, nextProfiles, axis,
//                                                timeWastedEntry, timeWastedExit, solverProfile, adaption);
//                                        //expandEntry = profiles[axis].hasOption(ProfileOption.CroppedEntry);
//                                        //expandExit = profiles[axis].hasOption(ProfileOption.CroppedExit);
//                                    }
//                                    else {
//                                        if (expandEntry) {
//                                            if (signum > 0) {
//                                                // Going positive, take sMin into consideration 
//                                                if (prevProfiles != null && Double.isFinite(prevProfiles[axis].sMin)) {
//                                                    solverProfile.s[0] = prevProfiles[axis].sMin;
//                                                    // We got a limit, do not expand after all.
//                                                    expandEntry = false;
//                                                }
//                                            }
//                                            else if (signum < 0) {
//                                                // Going negative, take sMax into consideration 
//                                                if (prevProfiles != null && Double.isFinite(prevProfiles[axis].sMax)) {
//                                                    solverProfile.s[0] = prevProfiles[axis].sMax;
//                                                    // We got a limit, do not expand after all.
//                                                    expandEntry = false;
//                                                }
//                                            }
//                                            solverProfile.v[0] = 0;
//                                            solverProfile.a[0] = 0;
//                                        }
//                                        if (expandExit) {
//                                            if (signum < 0) {
//                                                // Going negative, take sMin into consideration 
//                                                if (nextProfiles != null && Double.isFinite(nextProfiles[axis].sMin)) {
//                                                    solverProfile.s[segments] = nextProfiles[axis].sMin;
//                                                    // We got a limit, do not expand after all.
//                                                    expandExit = false;
//                                                }
//                                            }
//                                            else if (signum > 0) {
//                                                // Going positive, take sMax into consideration 
//                                                if (nextProfiles != null && Double.isFinite(nextProfiles[axis].sMax)) {
//                                                    solverProfile.s[segments] = nextProfiles[axis].sMax;
//                                                    // We got a limit, do not expand after all.
//                                                    expandExit = false;
//                                                }
//                                            }
//                                            solverProfile.v[segments] = 0;
//                                            solverProfile.a[segments] = 0;
//                                        }
//                                    }
//                                    if  (signum != 0 && (expandEntry || expandExit)) {
//                                        // Still expanding, do it.
//                                        solverProfile.solveByExpansion(signum, expandEntry, expandExit);
//                                        double t0 = solverProfile.getForwardCrossingTime(profiles[axis].s[0], false);
//                                        double t1 = solverProfile.getBackwardCrossingTime(profiles[axis].s[segments], false);
//                                        profiles[axis].extractProfileSectionFrom(solverProfile, t0, t1);
//                                        profiles[axis].validate("extracted from expansion, move "+i);                                    
//                                    }
//                                    else {
//                                        // Solve and extract.
//                                        solverProfile.assertSolved();
//                                        double t0 = solverProfile.getForwardCrossingTime(profiles[axis].s[0], false);
//                                        double t1 = solverProfile.getBackwardCrossingTime(profiles[axis].s[segments], false);
//                                        profiles[axis].extractProfileSectionFrom(solverProfile, t0, t1);
//                                        profiles[axis].validate("extracted, move "+i);                                    
//                                    }
//                                    hasSolved = true;
//                                }
//                                else 
                                    if (solve) {
                                    // Solve with given entry/exit conditions.
                                    profiles[axis].solve();
                                    profiles[axis].validate("simply solved, move "+i);             
                                    hasSolved = true;
                                }
                                else {
                                    // Remember for another pass.
                                    hasUncoordinated = true;
                                }
                            }
                        }
                        if (hasSolved) {
                            MotionProfile.synchronizeProfiles(profiles);
                            MotionProfile.validateProfiles(profiles);
                        }
                    }
                }
            }
        }





        //        for (int passes = 10; passes > 0; passes--) {
        //            MotionProfile [] prevProfiles = get(0);
        //            for (int i = 1; i <= last; i++) {
        //                MotionProfile [] profiles = get(i);
        //                if (isCoordinated(prevProfiles) && isCoordinated(profiles)) {
        //                    // Both are coordinated.
        //                    if (colinearWithPrev[i] == 1) {
        //                        // Straight junction - this can be optimized.
        //                        int lead = leadAxis[i];
        //                        // Approximate the new junction velocity as the minimum of both peak velocities.
        //                        double signum = Math.signum(unitVector[i][lead]);
        //                        double vJunction = (signum > 0 ?
        //                                Math.min(prevProfiles[lead].vBound1, profiles[lead].vBound1)
        //                                : Math.max(prevProfiles[lead].vBound0, profiles[lead].vBound0));
        //                        // Approximate the new junction acceleration as the mean of deceleration and re-acceleration.
        //                        double signumAcceleration = (signum > 0 ?
        //                                Math.signum(profiles[lead].vBound1 - prevProfiles[lead].vBound1)
        //                                : Math.signum(profiles[lead].vBound0 - prevProfiles[lead].vBound0));
        //                        double aJunction = (signumAcceleration > 0 ?
        //                                Math.min(prevProfiles[lead].aBound1, profiles[lead].aBound1)
        //                                : Math.max(prevProfiles[lead].aBound0, profiles[lead].aBound0));
        //                        System.out.println("coordinated 1: "+prevProfiles[lead]);
        //                        System.out.println("coordinated 2: "+profiles[lead]);
        //                        System.out.println("    v="+vJunction+", a="+aJunction);
        //                        // Set the new entry/exit conditions.
        //                        prevProfiles[lead].v[7] = vJunction;
        //                        profiles[lead].v[0] = vJunction;
        ////                        prevProfiles[lead].a[7] = aJunction;
        ////                        profiles[lead].a[0] = aJunction;
        //                        prevProfiles[lead].clearOption(ProfileOption.Solved);
        //                        profiles[lead].clearOption(ProfileOption.Solved);
        //                    }
        //                }
        //                else if (isCoordinated(prevProfiles) && !isCoordinated(profiles)) {
        //                    // Mixed coordinated -> uncoordinated. 
        //                    int lead = leadAxis[i-1];
        //                    // Find the uncoordinated axis with maximum time.
        //                    double time = 0;
        //                    for (int axis = 0; axis < dimensions; axis++) {
        //                        time = Math.max(time, profiles[axis].time);
        //                    }
        //                    // Find the minimum factor. 
        //                    double vFactor = Double.POSITIVE_INFINITY;
        //                    double aFactor = Double.POSITIVE_INFINITY;
        //                    for (int axis = 0; axis < dimensions; axis++) {
        //                        double divisor = unitVector[i-1][axis];
        //                        if (divisor != 0) {
        //                            double signum = Math.signum(divisor);
        //                            double timeToSpare = Math.min(prevProfiles[axis].time - prevProfiles[axis].getSegmentBeginTime(2), 
        //                                    (time - profiles[axis].time)*0.5);
        //                            // Approximate the new junction velocity as the overshoot velocity.
        //                            double vOvershoot = prevProfiles[axis].getMomentaryVelocity(prevProfiles[axis].time - timeToSpare);
        //                            double aOvershoot = prevProfiles[axis].getMomentaryAcceleration(prevProfiles[axis].time - timeToSpare);
        //                            // Approximate the new junction velocity as the minimum of both peak velocities and take either
        //                            // this or the overshoot velocity. 
        //                            double vJunction = (signum > 0 ?
        //                                    Math.max(Math.min(prevProfiles[axis].vBound1, profiles[axis].vBound1), vOvershoot)
        //                                    : Math.min(Math.max(prevProfiles[axis].vBound0, profiles[axis].vBound0), vOvershoot));
        //                            // Approximate the new junction acceleration as the mean of deceleration and re-acceleration.
        //                            double aJunction = (vJunction == vOvershoot ? 
        //                                    aOvershoot
        //                                    : (prevProfiles[axis].a[6] + profiles[axis].a[1])*0.5);
        //                            System.out.println("vOver="+vOvershoot+", aOver="+aOvershoot+", v="+vJunction+", a="+aJunction);
        //                            // Compute the minimum vFactor
        //                            vFactor = Math.min(vFactor, 
        //                                    vJunction/divisor);
        //                            aFactor = Math.min(aFactor, 
        //                                    aJunction/divisor);
        //                            if (vFactor == 0) {
        //                                break;
        //                            }
        //                        }
        //                    }
        //                    // Apply the factor.
        //                    for (int axis = 0; axis < dimensions; axis++) {
        //                        prevProfiles[axis].v[7] = vFactor*unitVector[i-1][axis];
        //                        prevProfiles[axis].a[7] = aFactor*unitVector[i-1][axis];
        //                        profiles[axis].v[0] = prevProfiles[axis].v[7];
        //                        profiles[axis].a[0] = prevProfiles[axis].a[7];
        //                        prevProfiles[lead].clearOption(ProfileOption.Solved);
        //                        profiles[lead].clearOption(ProfileOption.Solved);
        //                    }
        //                }
        //                else if (!isCoordinated(prevProfiles) && isCoordinated(profiles)) {
        //                    // Mixed uncoordinated -> coordinated. 
        //                    int lead = leadAxis[i];
        //                    // Find the uncoordinated axis with maximum time.
        //                    double time = 0;
        //                    for (int axis = 0; axis < dimensions; axis++) {
        //                        time = Math.max(time, prevProfiles[axis].time);
        //                    }
        //                    // Find the minimum factor. 
        //                    double vFactor = Double.POSITIVE_INFINITY;
        //                    double aFactor = Double.POSITIVE_INFINITY;
        //                    for (int axis = 0; axis < dimensions; axis++) {
        //                        double divisor = unitVector[i][axis];
        //                        if (divisor != 0) {
        //                            double signum = Math.signum(divisor);
        //                            double timeToSpare = Math.min(profiles[axis].getSegmentBeginTime(5), 
        //                                    (time - prevProfiles[axis].time)*0.5);
        //                            // Approximate the new junction velocity as the overshoot velocity.
        //                            double vOvershoot = profiles[axis].getMomentaryVelocity(timeToSpare);
        //                            double aOvershoot = profiles[axis].getMomentaryAcceleration(timeToSpare);
        //                            // Approximate the new junction velocity as the minimum of both peak velocities and take either
        //                            // this or the overshoot velocity. 
        //                            double vJunction = (signum > 0 ?
        //                                    Math.max(Math.min(prevProfiles[axis].vBound1, profiles[axis].vBound1), vOvershoot)
        //                                    : Math.min(Math.max(prevProfiles[axis].vBound0, profiles[axis].vBound0), vOvershoot));
        //                            // Approximate the new junction acceleration as the mean of deceleration and re-acceleration.
        //                            double aJunction = (vJunction == vOvershoot ? 
        //                                    aOvershoot
        //                                    : (prevProfiles[axis].a[6] + profiles[axis].a[1])*0.5);
        //                            System.out.println("vOver="+vOvershoot+", aOver="+aOvershoot+", v="+vJunction+", a="+aJunction);
        //                            // Compute the minimum vFactor
        //                            vFactor = Math.min(vFactor, 
        //                                    vJunction/divisor);
        //                            aFactor = Math.min(aFactor, 
        //                                    aJunction/divisor);
        //                            if (vFactor == 0) {
        //                                break;
        //                            }
        //                        }
        //                    }
        //                    // Apply the factor.
        //                    for (int axis = 0; axis < dimensions; axis++) {
        //                        prevProfiles[axis].v[7] = vFactor*unitVector[i][axis];
        //                        prevProfiles[axis].a[7] = aFactor*unitVector[i][axis];
        //                        profiles[axis].v[0] = prevProfiles[axis].v[7];
        //                        profiles[axis].a[0] = prevProfiles[axis].a[7];
        //                        prevProfiles[lead].clearOption(ProfileOption.Solved);
        //                        profiles[lead].clearOption(ProfileOption.Solved);
        //                    }
        //                }
        //                prevProfiles = profiles;
        //            }
        //
        //            // Solve all that were changed.
        //            for (int i = 0; i <= last; i++) {
        //                MotionProfile [] profiles = get(i);
        //                if (isCoordinated(profiles)) {
        //                    int lead = leadAxis[i];
        //                    if (profiles[lead].assertSolved()) {
        //                        coordinateProfiles(profiles);
        //                    }
        //                }
        //                else {
        //                    for (int axis = 0; axis < dimensions; axis++) {
        //                        profiles[axis].assertSolved();
        //                    }
        //                    if (passes == 1) {
        //                        synchronizeProfiles(profiles);
        //                    }
        //                }
        //                validateProfiles(profiles);
        //            }
        //            //pathToSvg(path);
        //        }




        //
        //        // Pass 2: Greedy backward motion, unconstrained.
        //        MotionProfile [] nextProfiles = null;
        //        for (int i = last; i >= 0; i--) {
        //            MotionProfile [] profiles = get(i);
        //            if (i > 0) {
        //                prevProfiles = get(i-1);
        //            }
        //            else {
        //                prevProfiles = null;
        //            }
        //            if (isCoordinated(profiles)) {
        //                int lead = leadAxis[i];
        //                profiles[lead].setOption(ProfileOption.UnconstrainedEntry);
        //                if (nextProfiles != null) {
        //
        //                    if (isCoordinated(nextProfiles)) { 
        //                        if (colinearWithPrev[i+1] == 0) {
        //                            // Corner between coordinated profiles. End with zero velocity/acceleration. 
        //                            profiles[lead].v[segments] = 0;
        //                            profiles[lead].a[segments] = 0;
        //                        }
        //                        else {
        //                            // Co-linear profiles. Ride-through, but limit to forward unconstrained.
        //                            double vUnit = 0;
        //                            double aUnit = 0;
        //                            if (unitVector[i][lead] != 0.0) {
        //                                vUnit = Math.max(0, Math.min(profiles[lead].v[segments]/unitVector[i][lead], nextProfiles[lead].v[0]/unitVector[i][lead]));
        //                                aUnit = Math.max(0, Math.min(profiles[lead].a[segments]/unitVector[i][lead], nextProfiles[lead].a[0]/unitVector[i][lead]));
        //                            }
        //                            // Set the exit constraint from minimum.
        //                            profiles[lead].v[segments] = unitVector[i][lead]*vUnit;
        //                            profiles[lead].a[segments] = unitVector[i][lead]*aUnit;
        //                        }
        //                    }
        //                    /*
        //                    else { // Uncoordinated next.
        //                        // As a crude first guess, just take the unsynchronized maximum axis speed.
        //                        double vUnitMax = 0;
        //                        double aUnitMax = 0;
        //                        for (int axis = 0; axis < unitVector[i].length; axis++) {
        //                            if (unitVector[i][axis] != 0.0) {
        //                                vUnitMax = Math.max(vUnitMax, Math.min(profiles[lead].v[segments]/unitVector[i][axis], nextProfiles[axis].v[0]/unitVector[i][axis]));
        //                                aUnitMax = Math.max(aUnitMax, Math.min(profiles[lead].a[segments]/unitVector[i][axis], nextProfiles[axis].a[0]/unitVector[i][axis]));
        //                            }
        //                        }
        //                        // Set the exit constraint from maximum.
        //                        profiles[lead].v[segments] = unitVector[i][lead]*vUnitMax;
        //                        profiles[lead].a[segments] = unitVector[i][lead]*aUnitMax;
        //                    }
        //                     */
        //
        //
        //
        //                    //                    // Because this is coordinated, determine the minimum unit speed.
        //                    //                    double vUnitMin = Double.POSITIVE_INFINITY;
        //                    //                    double aUnitMin = Double.POSITIVE_INFINITY;
        //                    //                    for (int axis = 0; axis < unitVector[i].length; axis++) {
        //                    //                        if (unitVector[i][axis] != 0.0) {
        //                    //                            vUnitMin = Math.min(vUnitMin, nextProfiles[axis].v[0]/unitVector[i][axis]);
        //                    //                            aUnitMin = Math.min(aUnitMin, nextProfiles[axis].a[0]/unitVector[i][axis]);
        //                    //                        }
        //                    //                    }
        //                    //                    // Don't allow bouncing from coordinated moves.
        //                    //                    vUnitMin = Math.max(0, Math.min(profiles[lead].vMax, vUnitMin));
        //                    //                    aUnitMin = Math.max(0, Math.min(profiles[lead].aMaxEntry, aUnitMin));
        //                    //                    // Set the exit constraint from the previous profile.
        //                    //                    profiles[lead].v[segments] = unitVector[i][lead]*vUnitMin;
        //                    //                    profiles[lead].a[segments] = unitVector[i][lead]*aUnitMin;
        //                }
        //                if (prevProfiles == null) {
        //                    // Must start from path entry velocity/acceleration. Clear the unconstrained flag
        //                    profiles[lead].clearOption(ProfileOption.UnconstrainedEntry);
        //                }
        //                else if (isCoordinated(prevProfiles) && colinearWithPrev[i] == 0) {
        //                    // Goes into coordinated move. Must stop.  
        //                    profiles[lead].v[0] = 0;
        //                    profiles[lead].a[0] = 0;
        //                    // Clear the unconstrained flag
        //                    profiles[lead].clearOption(ProfileOption.UnconstrainedEntry);
        //                }
        //                // Remove options from pass 1.
        //                profiles[lead].tMin = 0;
        //                profiles[lead].clearOption(ProfileOption.UnconstrainedExit);
        //                profiles[lead].solve();
        //                // Clear the option for posterity.
        //                profiles[lead].clearOption(ProfileOption.UnconstrainedEntry);
        //                coordinateProfilesToLead(profiles, profiles[lead]);
        //                validateProfiles(profiles);
        //            }
        //            else { // Uncoordinated.
        //                for (int axis = 0; axis < unitVector[i].length; axis++) {
        //                    profiles[axis].setOption(ProfileOption.UnconstrainedEntry);
        //                    if (nextProfiles != null) {
        //                        // Set the exit constraints from the next profile, but limited (can be lower than next segment).
        //                        profiles[axis].v[segments] =  Math.max(-profiles[axis].vMax, Math.min(profiles[axis].vMax, nextProfiles[axis].v[0]));
        //                        profiles[axis].a[segments] =  Math.max(-profiles[axis].aMaxExit, Math.min(profiles[axis].aMaxExit, nextProfiles[axis].a[0]));
        //                    } 
        //
        //                    if (prevProfiles == null) {
        //                        // Must start from still-stand. Clear the unconstrained flag
        //                        profiles[axis].clearOption(ProfileOption.UnconstrainedEntry);
        //                    }
        //                    else if (profiles[axis].s[0] == profiles[axis].s[segments] && dotProduct(unitVector[i], unitVector[i]) > 0) {
        //                        // No displacement, but vector not empty. We want reflection.
        //                        profiles[axis].v[0] = -profiles[axis].v[segments];
        //                        profiles[axis].a[0] = profiles[axis].a[segments];
        //                        profiles[axis].clearOption(ProfileOption.UnconstrainedEntry);
        //                    }                    //                    else if (profiles[axis].s[0] == profiles[axis].s[segments]
        //                    //                            && Math.abs(profiles[axis].getEffectiveEntryVelocity(profiles[axis].jMax) 
        //                    //                                    + profiles[axis].getEffectiveExitVelocity(profiles[axis].jMax)) < vtol*2) {
        //                    //                        // Within tolerance, re-apply reflection case
        //                    //                        profiles[axis].v[0] = -profiles[axis].v[segments];
        //                    //                        profiles[axis].a[0] = profiles[axis].a[segments];
        //                    //                    }
        //                    //                    else if (isCoordinated(prevProfiles)) {
        //                    //                        // Set the entry constraints from the previous profile, but limited (can be lower than next segment).
        //                    //                        profiles[axis].v[0] =  
        //                    //                                Math.max(-profiles[axis].vMax, Math.min(profiles[axis].vMax, 
        //                    //                                        Math.max(-prevProfiles[axis].vMax, Math.min(prevProfiles[axis].vMax,
        //                    //                                                prevProfiles[axis].v[segments]))));
        //                    //                        profiles[axis].a[0] =  
        //                    //                                Math.max(-profiles[axis].aMaxEntry, Math.min(profiles[axis].aMaxEntry, 
        //                    //                                        Math.max(-prevProfiles[axis].aMaxExit, Math.min(prevProfiles[axis].aMaxExit, 
        //                    //                                                prevProfiles[axis].a[segments]))));
        //                    //                        // Clear the unconstrained flag
        //                    //                        profiles[axis].clearOption(ProfileOption.UnconstrainedEntry);
        //                    //                    }
        //                    // Remove option from pass 1.
        //                    profiles[axis].tMin = 0;
        //                    profiles[axis].clearOption(ProfileOption.UnconstrainedExit);
        //                    profiles[axis].solve();
        //                    // Clear the option for posterity.
        //                    profiles[axis].clearOption(ProfileOption.UnconstrainedEntry);
        //                }
        //                // DO NOT: synchronizeProfiles(profiles);
        //                validateProfiles(profiles);
        //            }
        //            nextProfiles = profiles;
        //        }
        //
        //        trace(" ------------- PASS 3 -------------------------");
        //
        //        // Pass 3: Mend the two greedy strategies together:
        //        // For two consecutive coordinated moves this means taking the minimum of both junction speeds.
        //        // For two consecutive uncoordinated moves we take the blend i.e. the mean of both junction speeds.
        //        // For mixed coordinated/uncoordinated moves this means taking the one governed by the coordinated move.
        //        for (int i = 0; i <= last; i++) {
        //            MotionProfile [] profiles = get(i);
        //            if (i < last) {
        //                nextProfiles = get(i+1);
        //            }
        //            else {
        //                nextProfiles = null;
        //            }
        //            if (isCoordinated(profiles)) {
        //                int lead = leadAxis[i];
        //                if (nextProfiles != null) {
        //                    // Coordinated is dominant by default. 
        //                    double vNew = profiles[lead].v[segments];
        //                    double aNew = profiles[lead].a[segments];
        //                    if (isCoordinated(nextProfiles) && colinearWithPrev[i+1] == 1) {
        //                        // Next is also coordinated and co-linear. Take (signed) minimum.
        //                        double signum = Math.signum(unitVector[i][lead]); 
        //                        vNew = signum*Math.min(signum*profiles[lead].v[segments], signum*nextProfiles[lead].v[0]);
        //                        aNew = signum*Math.min(signum*profiles[lead].a[segments], signum*nextProfiles[lead].a[0]);
        //                    }
        //                    if (profiles[lead].v[segments] != vNew
        //                            || profiles[lead].a[segments] != aNew) {
        //                        profiles[lead].v[segments] = vNew;
        //                        profiles[lead].a[segments] = aNew;
        //                        profiles[lead].clearOption(ProfileOption.Solved);
        //                    }
        //
        //                }
        //                if (! profiles[lead].hasOption(ProfileOption.Solved)) {
        //                    // Remove options from pass 1, 2.
        //                    profiles[lead].tMin = 0;
        //                    profiles[lead].clearOption(ProfileOption.UnconstrainedEntry);
        //                    profiles[lead].clearOption(ProfileOption.UnconstrainedExit);
        //                    profiles[lead].solve();
        //                    coordinateProfilesToLead(profiles, profiles[lead]);
        //                }
        //                // Continuity on all axes.
        //                if (nextProfiles != null) {
        //                    for (int axis = 0; axis < unitVector[i].length; axis++) {
        //                        if (nextProfiles[axis].v[0] != profiles[axis].v[segments]
        //                                || nextProfiles[axis].a[0] != profiles[axis].a[segments]) {
        //                            nextProfiles[axis].v[0] = profiles[axis].v[segments];
        //                            nextProfiles[axis].a[0] = profiles[axis].a[segments];
        //                            nextProfiles[axis].clearOption(ProfileOption.Solved);
        //                        }
        //                    }
        //                }
        //            }
        //            else { // Uncoordinated.
        //                for (int axis = 0; axis < unitVector[i].length; axis++) {
        //                    if (nextProfiles != null) {
        //                        double vNew = profiles[axis].v[segments];
        //                        double aNew = profiles[axis].a[segments];
        //                        if (isCoordinated(nextProfiles)) {
        //                            // Coordinated is dominant.
        //                            vNew = nextProfiles[axis].v[0];
        //                            aNew = nextProfiles[axis].a[0];
        //                        }
        //                        else {
        //                            // Two Uncoordinated. Take blend.
        //                            double ratio;
        //                            if (profiles[axis].time == 0) {
        //                                ratio = 1;
        //                            }
        //                            else if (nextProfiles[axis].time == 0) {
        //                                ratio = 0;
        //                            }
        //                            else {
        //                                double w0 = 1/Math.sqrt(profiles[axis].time);
        //                                double w1 = 1/Math.sqrt(nextProfiles[axis].time);
        //                                ratio = w0/(w0+w1);
        //                            }
        //                            vNew = profiles[axis].v[segments]*ratio + nextProfiles[axis].v[0]*(1-ratio);
        //                            aNew = profiles[axis].a[segments]*ratio + nextProfiles[axis].a[0]*(1-ratio);
        //                        }
        //                        if (profiles[axis].v[segments] != vNew
        //                                || profiles[axis].a[segments] != aNew) {
        //                            profiles[axis].v[segments] = vNew;
        //                            profiles[axis].a[segments] = aNew;
        //                        }
        //                        if (nextProfiles[axis].v[0] != vNew
        //                                || nextProfiles[axis].a[0] != aNew) {
        //                            nextProfiles[axis].v[0] = vNew;
        //                            nextProfiles[axis].a[0] = aNew;
        //                        }
        //                    }
        //                    // Remove options from pass 1, 2.
        //                    profiles[axis].tMin = 0;
        //                    profiles[axis].clearOption(ProfileOption.UnconstrainedEntry);
        //                    profiles[axis].clearOption(ProfileOption.UnconstrainedExit);
        //                    profiles[axis].solve();
        //                }
        //
        //                synchronizeProfiles(profiles);
        //            }
        //            validateProfiles(profiles);
        //        }
    }

    public void validate(String title) throws Exception {
        MotionProfile [] prevProfiles = null; 
        int i = 0;
        final double sErr = Math.sqrt(MotionProfile.eps);
        final double vErr = MotionProfile.vtol*0.1;
        final double aErr = MotionProfile.atol*0.1;
        for (MotionProfile [] profiles : this) {
            MotionProfile.validateProfiles(profiles);
            for (int axis = 0; axis < profiles.length; axis++) {
                if (prevProfiles == null) {
                    if (profiles[axis].v[0] != 0) {
                        throw new Exception(title+": axis "+axis+" v[0] is not zero");
                    }
                    if (!profiles[axis].isConstantAcceleration() && profiles[axis].a[0] != 0) {
                        throw new Exception(title+": axis "+axis+" a[0] is not zero");
                    }
                }
                else {
                    if ( MotionProfile.mismatch(profiles[axis].s[0], prevProfiles[axis].s[segments], sErr)) {
                        throw new Exception(title+": axis "+axis+" location discontinous into move "+i);
                    }
                    if ( MotionProfile.mismatch(profiles[axis].v[0], prevProfiles[axis].v[segments], vErr)) {
                        throw new Exception(title+": axis "+axis+" velocity discontinous into move "+i);
                    }
                    if (!profiles[axis].isConstantAcceleration() 
                            &&  MotionProfile.mismatch(profiles[axis].a[0], prevProfiles[axis].a[segments], aErr)) {
                        throw new Exception(title+": axis "+axis+" acceleration discontinous into move "+i);
                    }
                }
            }
            // Next.
            prevProfiles = profiles;
            i++;
        }
    }

    protected static boolean controlOvershoot(MotionProfile[] prevProfiles,
            MotionProfile[] entryProfiles, MotionProfile[] exitProfiles,
            MotionProfile[] nextProfiles, int axis, double timeWastedEntry, double timeWastedExit,
            MotionProfile solverProfile, double adaption, int iteration) {
        boolean changed = false;
        double minf = (iteration == 0) ? 0.71 : 0;

        if (entryProfiles[axis].hasOption(ProfileOption.CroppedEntry)) {
            solverProfile.s[0] = entryProfiles[axis].sEntryControl;
            if (prevProfiles != null && !MotionProfile.isCoordinated(prevProfiles)) {
                double tControl = entryProfiles[axis].tEntryControl;
                if (tControl != 0) {
                    double factor = Math.max(minf, Math.min(1, 
                            (tControl - timeWastedEntry*adaption)/tControl));
                    double sControl = entryProfiles[axis].sEntryControl - prevProfiles[axis].s[segments];
                    solverProfile.s[0] = prevProfiles[axis].s[segments] + sControl*factor*factor;
                    changed = true;
                }
            }
            solverProfile.v[0] = 0;
            solverProfile.a[0] = 0;
        }
        if (exitProfiles[axis].hasOption(ProfileOption.CroppedExit)) {
            solverProfile.s[segments] = exitProfiles[axis].sExitControl;
            if (nextProfiles != null && !MotionProfile.isCoordinated(nextProfiles)) {
                double tControl = exitProfiles[axis].tExitControl;
                if (tControl != 0) {
                    double factor = Math.max(minf, Math.min(1, 
                            (tControl - timeWastedExit*adaption)/tControl));
                    double sControl = exitProfiles[axis].sExitControl - nextProfiles[axis].s[0];
                    solverProfile.s[segments] = nextProfiles[axis].s[0] + sControl*factor*factor;
                    changed = true;
                }
            }
            solverProfile.v[segments] = 0;
            solverProfile.a[segments] = 0;
        }
        // Solve to boundary conditions.
        solverProfile.solve();
        return changed;
    }

    public void toSvg(String title) {
        // Calculate the effective entry/exit velocity after jerk to acceleration 0.
        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                "<svg xmlns=\"http://www.w3.org/2000/svg\"\n" + 
                "  xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" + 
                "  version=\"1.1\" baseProfile=\"full\"\n" + 
                "  width=\"100%\" height=\"100%\" \n"+
                "  viewBox=\""+-10+" "+-100+" "+450+" "+130+"\">\r\n");
        svg.append("<title>");
        svg.append(XmlSerialize.escapeXml(title));
        svg.append("</title>\n");
        double dt = 0.0005;
        double d = 0.5;
        double orthy = Math.sin(Math.toRadians(30));
        double orthx = Math.cos(Math.toRadians(30));
        double sy = -1;
        double shad = 0.0;

        svg.append("<text x=\"0\" y=\"20\" fill=\"black\" font-family=\"sans-serif\" font-size=\"7\">");
        svg.append(XmlSerialize.escapeXml(title));
        svg.append("</text>\n");

        for (boolean shadow : new boolean [] { true, false } ) {
            for (MotionProfile [] profiles : this) {
                double x0 = 0;
                double y0 = 0;
                double z0 = 0;

                for (double t = 0; t <= Math.min(10, profiles[0].time); t+= dt) {
                    double x = profiles[0].getMomentaryLocation(t);
                    double y = profiles[1].getMomentaryLocation(t);
                    double z = profiles[2].getMomentaryLocation(t)+15;
                    if (t > 0) {
                        if (shadow) {
                            svg.append("<line x1=\""+(x+orthx*y+shad*z)+"\" y1=\""+(y+shad*z)*orthy*sy+"\" x2=\""+(x0+orthx*y0+shad*z0)+"\" y2=\""+(y0+shad*z0)*orthy*sy+"\" stroke-linecap=\"round\" style=\"stroke-width: "+d+"; stroke:grey;\"/>\n");
                        }
                        else {
                            svg.append("<line x1=\""+(x+orthx*y)+"\" y1=\""+(y*orthy+z)*sy+"\" x2=\""+(x0+orthx*y0)+"\" y2=\""+(y0*orthy+z0)*sy+"\" stroke-linecap=\"round\" style=\"stroke-width: "+d+"; stroke:red;\"/>\n");
                        }
                    }
                    x0 = x;
                    y0 = y;
                    z0 = z;
                }

                for (int i : new int[] { 0, segments } ) {
                    double x = profiles[0].s[i];
                    double y = profiles[1].s[i];
                    double z = profiles[2].s[i]+15;
                    double r = 0.5*d;  
                    if (shadow) {
                        svg.append("<circle cx=\""+(x+orthx*y+shad*z)+"\" cy=\""+(y+shad*z)*orthy*sy+"\" r=\""+r+"\" style=\"stroke-width: "+d+"; stroke:grey; fill:none;\"/>\n");
                    }
                    else {
                        svg.append("<circle cx=\""+(x+orthx*y)+"\" cy=\""+(y*orthy+z)*sy+"\" r=\""+r+"\" style=\"stroke-width: "+d+"; stroke:red; fill:none;\"/>\n");
                    }
                }
            }
        }

        svg.append("</svg>\n");
        try {
            File file = File.createTempFile("profile-path-", ".svg");
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
    }


}
