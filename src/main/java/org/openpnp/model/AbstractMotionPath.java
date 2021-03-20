package org.openpnp.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.openpnp.model.MotionProfile.ProfileOption;
import org.openpnp.util.XmlSerialize;

public abstract class AbstractMotionPath implements Iterable<MotionProfile []> {
    final double approximation = 0.75; // 0.75
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
        solve(approximation, iterations);
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
     *      Each partial move is then cut out from the overall mapping. TODO: Moves which reach the most restrictive V max at one
     *      or both ends are re-solved with their proper V max, if higher (this is not 100% optimal but not so bad for practical 
     *      use cases).</li>
     *      
     *  <li>Coordinated moves that are followed by an uncoordinated move are handled in two ways:<br/>
     *      a) if there is an applicable s limit, the move is handled as if it extends to the limit and then cropped<br/>
     *      b) if there is no applicable s limit, the move is handled as half-sided mapping and then extended or cropped.</li>
     *      
     *  <li>Coordinated moves that are preceded by an uncoordinated move are handled as in 3. (in reverse).</li>
     *   
     *  <li>Coordinated moves that are both preceded and followed by an uncoordinated move are left unoptimized at the moment.</li>
     *  </ol>
     *    
     * @param approximation Determines by what rate it should approximate the estimated best solution, per iteration.  
     * @param iterations How many iterations should be computed. 
     * @throws Exception
     */
    public void solve(double approximation, int iterations) throws Exception {
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
        boolean[] simplified = new boolean[size];

        for (int i = 0; i <= last; i++) {
            MotionProfile [] profiles = get(i);
            if (profiles.length == 0) {
                // Zero dimensions?
                return;
            }
            unitVector[i] = MotionProfile.getUnitVector(profiles);
            leadAxis[i] = MotionProfile.getLeadAxisIndex(unitVector[i]);
            simplified[i] = false;
            for (int axis = 0; axis < profiles.length; axis++) {
                if (!profiles[axis].isEmpty()) {
                    simplified[i] |= !profiles[axis].isSupportingUncoordinated();
                }
            }
            if (i > 0) {
                junctionCosineFromPrev[i] = MotionProfile.dotProduct(unitVector[i-1], unitVector[i]);
                colinearWithPrev[i] = 0;
                if (leadAxis[i] == leadAxis[i-1]
                        && simplified[i] == simplified[i-1]) {
                    colinearWithPrev[i] = ((junctionCosineFromPrev[i] >= 1.0 - MotionProfile.eps) ? 1 
                            : (junctionCosineFromPrev[i] <= -1.0 + MotionProfile.eps) ? -1 
                                    : 0);
                }
            }
            // Solve all and store initial times.
            if (MotionProfile.isCoordinated(profiles)) {
                int lead = leadAxis[i];
                if (profiles[lead].assertSolved()) {
                    MotionProfile.coordinateProfiles(profiles);
                }
            }
            else {
                boolean sync = false;
                for (int axis = 0; axis < profiles.length; axis++) {
                    sync = profiles[axis].assertSolved() 
                            || sync;
                }
                if (sync) {
                    MotionProfile.synchronizeProfiles(profiles);
                }
            }
            for (int axis = 0; axis < profiles.length; axis++) {
                profiles[axis].initialTime = profiles[axis].time; 
            }
        }
        int dimensions = unitVector[0].length;

        for (int iteration = 0; iteration < iterations; iteration++) {
            int iNext;
            boolean hasUncoordinated = false;
            for (int i = 0; i <= last; i = iNext) {
                iNext = i+1;
                MotionProfile [] profiles = get(i);
                MotionProfile [] prevProfiles = (i > 0 ? get(i-1) : null);
                if (simplified[i]) {
                    // We can only handle them as single coordinated moves for now, because they don't support acceleration != 0 in junctions. 
                    int lead = leadAxis[i];
                    if (profiles[lead].assertSolved()) {
                        MotionProfile.coordinateProfiles(profiles);
                    }
                }
                else if (MotionProfile.isCoordinated(profiles)) {
                    int lead = leadAxis[i];
                    // This may be a sequence of multiple co-linear moves: try create a spanning mapping.
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
                        controlOvershoot(prevProfiles, profiles, exitProfiles, nextProfiles, lead,
                                solverProfile, approximation, iteration);
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
                                //                                    // Entry and/or exit expansion, solve using an overreaching mapping.
                                //                                    MotionProfile solverProfile = new MotionProfile(profiles[axis]);
                                //                                    double signum = solverProfile.profileSignum(vEffEntry, vEffExit);
                                //                                    if (iteration > 0) {
                                //                                        // This is a further refinement.
                                //                                        double timeWastedEntry = prevProfiles == null ? 0 : (prevProfiles[axis].time - straightLineCoordinatedTime[i-1]);
                                //                                        double timeWastedExit = nextProfiles == null ? 0 : (nextProfiles[axis].time - straightLineCoordinatedTime[i+1]);
                                //                                        controlOvershoot(prevProfiles, profiles, profiles, nextProfiles, axis,
                                //                                                timeWastedEntry, timeWastedExit, solverProfile, approximation);
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
                                    //profiles[axis].validate("simply solved, move "+i);             
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
                            //  MotionProfile.validateProfiles(profiles);
                        }
                    }
                }
            }
        }
    }

    /**
     * Helper for the optimizer: reduces excess overshoot into uncoordinated moves. This is a simple
     * heuristic controlled by excess time detected in the uncoordinated move. Excess time is assumed
     * when the move takes longer that the straight line move from/to still-stand. 
     * 
     */
    protected static boolean controlOvershoot(MotionProfile[] prevProfiles,
            MotionProfile[] entryProfiles, MotionProfile[] exitProfiles,
            MotionProfile[] nextProfiles, int axis, 
            MotionProfile solverProfile, double approximation, int iteration) {
        boolean changed = false;
        double minf = 0.0;
        if (entryProfiles[axis].hasOption(ProfileOption.CroppedEntry)) {
            solverProfile.s[0] = entryProfiles[axis].sEntryControl;
            if (prevProfiles != null && !MotionProfile.isCoordinated(prevProfiles)) {
                double tDeltaOuter = prevProfiles[axis].time - prevProfiles[axis].initialTime;
                //double tDeltaInner = entryProfiles[axis].time - entryProfiles[axis].initialTime;
                double tControl = entryProfiles[axis].tEntryControl;
                if (tControl != 0) {
                    double factor = Math.max(minf, Math.min(1, 
                            (tControl - tDeltaOuter*approximation*0.5)/tControl));
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
                double tDeltaOuter = nextProfiles[axis].time - nextProfiles[axis].initialTime;
                //double tDeltaInner = exitProfiles[axis].time - exitProfiles[axis].initialTime;
                double tControl = exitProfiles[axis].tExitControl;
                if (tControl != 0) {
                    double factor = Math.max(minf, Math.min(1, 
                            (tControl - tDeltaOuter*approximation*0.5)/tControl));
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

    /**
     * Validate the path for seamless continuity. 
     * 
     * @param title
     * @throws Exception
     */
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

    public void toSvg(String title, double zr) {
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
                double time = Math.max(profiles[0].time, profiles[0].tMin); 
                for (double t = 0; t <= Math.min(10, time); t+= dt) {
                    double x = profiles[0].getMomentaryLocation(t);
                    double y = profiles[1].getMomentaryLocation(t);
                    double z = profiles[2].getMomentaryLocation(t)+zr;
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
                    double z = profiles[2].s[i]+zr;
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
            File file = File.createTempFile("mapping-path-", ".svg");
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
    }


}
