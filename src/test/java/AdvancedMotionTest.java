import java.util.ArrayList;
import java.util.List;

import org.apache.batik.transcoder.ToSVGAbstractTranscoder;
import org.junit.Test;
import org.openpnp.model.Motion;
import org.openpnp.model.MotionProfile;
import org.openpnp.model.MotionProfile.ErrorState;
import org.openpnp.model.MotionProfile.ProfileOption;
import org.openpnp.spi.Driver.MotionControlType;
import org.openpnp.util.NanosecondTime;

public class AdvancedMotionTest {

    @Test 
    public void testMotionProfiles() throws Exception {
        MotionProfile profile;

        // Move from/to still-stand
        profile = new MotionProfile(
                0, 600, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Long move from/to still-stand", profile, null);
        // Short move from/to still-stand
        profile = new MotionProfile(
                0, 200, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Short move from/to still-stand", profile, null);
        // Low feed-rate from/to still-stand
        profile = new MotionProfile(
                0, 200, 0, 0, 0, 0,
                0, 1000, 100, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Low feed-rate from/to still-stand", profile, null);
        // Tiny move from/to still-stand
        profile = new MotionProfile(
                0, 10, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Tiny move from/to still-stand", profile, null);
        // Micro move from/to still-stand
        profile = new MotionProfile(
                0, 0.0001, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Micro move from/to still-stand", profile, null);
        // Short move with unconstrained exit 
        profile = new MotionProfile(
                0, 100, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, ProfileOption.UnconstrainedExit.flag());
        testProfile("Short move with unconstrained exit", profile, null);
        // Short move with unconstrained entry 
        profile = new MotionProfile(
                0, 100, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, ProfileOption.UnconstrainedEntry.flag());
        testProfile("Short move with unconstrained entry", profile, null);
        // Tiny move with unconstrained exit 
        profile = new MotionProfile(
                0, 10, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, ProfileOption.UnconstrainedExit.flag());
        testProfile("Tiny move with unconstrained exit", profile, null);
        // Tiny move with unconstrained entry 
        profile = new MotionProfile(
                0, 10, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, ProfileOption.UnconstrainedEntry.flag());
        testProfile("Tiny move with unconstrained entry", profile, null);
        // Move with entry/exit velocity
        profile = new MotionProfile(
                0, 100, 700, 700, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Move with max entry/exit velocity", profile, null);
        // Move with lower entry/exit velocity
        profile = new MotionProfile(
                0, 100, 200, 200, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Move with lower entry/exit velocity", profile, null);
        // Move with entry/exit velocity and min time
        profile = new MotionProfile(
                0, 400, 500, 300, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 2, Double.POSITIVE_INFINITY, 0);
        testProfile("Move with entry/exit velocity and min time", profile, null);
        // Move with entry/exit velocity/acceleration (expect MaxVelocityViolated)
        profile = new MotionProfile(
                0, 400, 700, -700, 2000, 2000,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Move with entry/exit velocity/acceleration", profile, MotionProfile.ErrorState.MaxVelocityViolated);
        // Null moves
        profile = new MotionProfile(
                0, 0, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Still-stand", profile, null);
        profile = new MotionProfile(
                0, 0, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY, 0);
        testProfile("Still-stand with min-time", profile, null);
        profile = new MotionProfile(
                0, 0, 700, 700, 2000, 2000,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Zero displacement move with entry/exit velocity/acceleration", profile, null);

        // Pure overshoot
        profile = new MotionProfile(
                0, 0, 700, -700, 2000, 2000,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Overshoot", profile, null);

        // Overshoot slightly prolonged
        profile = new MotionProfile(
                0, 0, 700, -700, 2000, 2000,
                0, 1000, 700, 2000, 2000, 15000, profile.getTime()+0.01, Double.POSITIVE_INFINITY, 0);
        testProfile("Overshoot slightly prolonged", profile, null);

        // moveToLoactionAtSafeZ() with min time (given by other axes move time) 
        profile = new MotionProfile(
                0, 0, 200, -200, 2000, 2000,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY, 0);
        testProfile("Z axis in moveToLoactionAtSafeZ() with min time", profile, null);

        // Asymmetric moveToLoactionAtSafeZ() with min time (given by other axes move time) 
        profile = new MotionProfile(
                0, 0, 200, -100, 1000, 1750,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY, 0);
        testProfile("Z axis in asymmetric moveToLoactionAtSafeZ() with min time", profile, null);

        // More asymmetric moveToLoactionAtSafeZ() with min time (given by other axes move time) 
        profile = new MotionProfile(
                0, 0, 200, -50, 1000, 500,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY, 0);
        testProfile("Z axis in more asymmetric moveToLoactionAtSafeZ() with min time", profile, null);

        // Twisted curve (expect MinLocationViolated)
        profile = new MotionProfile(
                0, 0, 500, 500, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY, 0);
        testProfile("Twisted curve (min-time)", profile, MotionProfile.ErrorState.MinLocationViolated);
        // Twisted curve with delta  (expect MinLocationViolated)
        profile = new MotionProfile(
                0, 10, 500, 500, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY, 0);
        testProfile("Twisted curve (min-time) with delta", profile, MotionProfile.ErrorState.MinLocationViolated);

        // Complex move with min-time
        profile = new MotionProfile(
                0, 400, 0, -20, 0, -100,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY, 0);
        testProfile("Complex move with min-time", profile, null);

        // Recalc a new profile with the solution params (but no min-time) to check if it results in the same profile. 
        // TODO: compare as a Unit Test (now only human tested).
        MotionProfile profile2 = new MotionProfile(
                0, 400, 0, -20, 0, -100,
                0, 1000, 
                profile.getProfileVelocity(MotionControlType.Full3rdOrderControl), 
                profile.getProfileEntryAcceleration(MotionControlType.Full3rdOrderControl), 
                profile.getProfileExitAcceleration(MotionControlType.Full3rdOrderControl),
                profile.getProfileJerk(MotionControlType.Full3rdOrderControl),
                0.0, Double.POSITIVE_INFINITY, 0);
        testProfile("recheck solution", profile2, null);
    }

    private void testProfile(String message, MotionProfile profile, ErrorState expectedError) throws Exception {
        MotionProfile profileRev = new MotionProfile(
                profile.getLocation(MotionProfile.segments), profile.getLocation(0), 
                -profile.getVelocity(MotionProfile.segments), -profile.getVelocity(0), 
                profile.getAcceleration(MotionProfile.segments), profile.getAcceleration(0),
                profile.getLocationMin(), profile.getLocationMax(),
                profile.getVelocityMax(), 
                profile.getEntryAccelerationMax(), 
                profile.getExitAccelerationMax(),
                profile.getJerkMax(),
                profile.getTimeMin(), profile.getTimeMax(), 
                profile.getOptions());

        MotionProfile profileConstantAcc = new MotionProfile(
                profile.getLocation(0), profile.getLocation(MotionProfile.segments), 
                profile.getVelocity(0), profile.getVelocity(MotionProfile.segments),  
                0, 0,
                profile.getLocationMin(), profile.getLocationMax(),
                profile.getVelocityMax(), 
                profile.getEntryAccelerationMax(), 
                profile.getExitAccelerationMax(),
                0,
                profile.getTimeMin(), profile.getTimeMax(), 
                profile.getOptions());

        MotionProfile profileSimpleSCurve = new MotionProfile(
                profile.getLocation(0), profile.getLocation(MotionProfile.segments), 
                profile.getVelocity(0), profile.getVelocity(MotionProfile.segments),  
                0, 0,
                profile.getLocationMin(), profile.getLocationMax(),
                profile.getVelocityMax(), 
                profile.getEntryAccelerationMax(), 
                profile.getExitAccelerationMax(),
                profile.getJerkMax(),
                profile.getTimeMin(), profile.getTimeMax(), 
                profile.getOptions() | ProfileOption.SimplifiedSCurve.flag());

        testProfileCase(message, profile, expectedError);
        testProfileCase(message+" (reverse)", profileRev, expectedError);
        testProfileCase(message+" (constant acceleration)", profileConstantAcc, expectedError);
        testProfileCase(message+" (simplified S-Curve)", profileSimpleSCurve, expectedError);
        System.out.println(" ");
    }

    public void testProfileCase(String message, MotionProfile profile, ErrorState expectedError)
            throws Exception {
        MotionProfile.ErrorState error;
        System.out.println(message);
        profile.solve();
        System.out.println(profile);
        error = profile.checkValidity();
        if (error != null && error != expectedError) {
            throw new Exception(message+" has error "+error);
        }
    }

    final double safeZ = -5;
    private class PlannerPath extends MotionProfile.Path {
        private final List<MotionProfile []> path = new ArrayList<>();
        private final double jerk;
        private final boolean sCurves;

        public PlannerPath(double jerk, boolean sCurves) {
            this.jerk = jerk;
            this.sCurves = sCurves;
        }

        public void add(MotionProfile [] profiles) {
            path.add(profiles);
        }

        @Override
        public int size() {
            return path.size();
        }

        @Override
        public MotionProfile[] get(int i) {
            return path.get(i);
        }

        private double x0 = 0;
        private double y0 = 0;
        private double z0 = 0;

        public void moveTo(double x, double y, double z, int nozzle) {
            MotionProfile [] profiles = new MotionProfile[3];
            int options = (sCurves ? ProfileOption.SimplifiedSCurve.flag() : 0);
            double zMin, zMax;
            boolean inSafeZone = 
                    (z0 >= safeZ && z0 <= -safeZ)
                    && (z >= safeZ && z <= -safeZ);
            if (nozzle == 1) {
                zMin = -20;
                zMax = 5;
            }
            else {
                zMin = -5;
                zMax = 20;
            }
            if (inSafeZone) {
                zMin = safeZ;
                zMax = -safeZ;
            }
            else {
                options |= ProfileOption.Coordinated.flag();
            }
            profiles[0] = new MotionProfile(
                    x0, x, 0, 0, 0, 0,
                    0, 1000, 700, 2000, 2000, jerk, 0, Double.POSITIVE_INFINITY, 
                    options);
            profiles[1] = new MotionProfile(
                    y0, y, 0, 0, 0, 0,
                    0, 500, 700, 2000, 2000, jerk, 0, Double.POSITIVE_INFINITY, 
                    options);
            profiles[2] = new MotionProfile(
                    z0, z, 0, 0, 0, 0,
                    zMin, zMax, 700, 2000, 2000, jerk, 0, Double.POSITIVE_INFINITY, 
                    options);

            // Solve as a single coordinated move.
//            double [] unitVector = MotionProfile.getUnitVector(profiles);
//            int leadAxis = MotionProfile.getLeadAxisIndex(unitVector);
//            profiles[leadAxis].solve();
//            MotionProfile.coordinateProfiles(profiles);
            // Add.
            add(profiles);
            // Remember last coordinates.
            x0 = x;
            y0 = y;
            z0 = z;
        }

        public double getOverallTime() {
            double time = 0;
            for (MotionProfile [] profiles : path) {
                time += profiles[0].getTime(); 
            }
            return time;
        }
    }

    @Test 
    public void testMotionPaths() throws Exception {
        for (int warmup = 2; warmup >= 0; warmup--) {
            for (PlannerPath path : new PlannerPath[] { 
                    new PlannerPath(30000, false), new PlannerPath(30000, true),
                    new PlannerPath(15000, false), new PlannerPath(15000, true),
                    new PlannerPath(0, false), new PlannerPath(0, true) 
                    }) {
                
                // pick & place
                path.moveTo(0, 0, safeZ, 1);
                path.moveTo(0, 0, -15, 1);
                path.moveTo(0, 0, safeZ, 1);
                path.moveTo(100, 0, safeZ, 1);
                path.moveTo(100, 0, -15, 1);
                path.moveTo(100, 0, safeZ, 1);
                path.moveTo(120, 0, safeZ, 1);
                path.moveTo(120, 0, -15, 1);
                path.moveTo(120, 0, safeZ, 1);
                path.moveTo(124, 0, safeZ, 1);
                path.moveTo(124, 0, -15, 1);
                path.moveTo(124, 0, safeZ, 1);
                path.moveTo(125, 0, safeZ, 1);
                path.moveTo(125, 0, -15, 1);
                path.moveTo(125, 0, safeZ, 1);

                //path.moveTo(130, 20, safeZ);
                //path.moveTo(130, 20, -10);
                //path.moveTo(130, 20, safeZ);

                path.moveTo(0, 50, safeZ, 1);
                path.moveTo(0, 50, -10, 1);
                path.moveTo(0, 50, safeZ, 1);
                path.moveTo(100, 50, safeZ, 1);
                path.moveTo(100, 50, -15, 1);
                path.moveTo(100, 50, safeZ, 1);
                path.moveTo(120, 50, safeZ, 1);
                path.moveTo(120, 50, -10, 1);
                path.moveTo(120, 50, safeZ, 1);
                path.moveTo(124, 50, safeZ, 1);
                path.moveTo(124, 50, -15, 1);
                path.moveTo(124, 50, safeZ, 1);
                path.moveTo(125, 50, safeZ, 1);
                path.moveTo(125, 50, -10, 1);
                path.moveTo(125, 50, safeZ, 1);

                path.moveTo(0, 100, safeZ, 1);
                path.moveTo(0, 100, -15, 1);
                path.moveTo(0, 100, safeZ, 1);
                path.moveTo(100, 100, -safeZ, 2);
                path.moveTo(100, 100, 15, 2);
                path.moveTo(100, 100, -safeZ, 2);
                path.moveTo(120, 100, safeZ, 1);
                path.moveTo(120, 100, -10, 1);
                path.moveTo(120, 100, safeZ, 1);
                path.moveTo(124, 100, -safeZ, 2);
                path.moveTo(124, 100, 15, 2);
                path.moveTo(124, 100, -safeZ, 2);
                path.moveTo(125, 100, safeZ, 1);
                path.moveTo(125, 100, -10, 1);
                path.moveTo(125, 100, safeZ, 1);

                // move to push/pull feeder
                path.moveTo(200, 50, safeZ, 1);
                path.moveTo(220, 50, safeZ-5, 1);
                path.moveTo(220, 50, safeZ, 1);
                path.moveTo(200, 80, safeZ, 1);
                path.moveTo(150, 100, safeZ, 1);
                path.moveTo(150, 120, safeZ, 1);
                path.moveTo(300, 120, -15, 1);
                path.moveTo(300, 150, -15, 1);
                path.moveTo(280, 150, -15, 1);
                path.moveTo(279, 150, -15, 1);
                path.moveTo(275, 150, -15, 1);
                //path.moveTo(275, 150, safeZ);

                if (warmup == 0) {
                    System.out.println("==========================================");
                    // Unoptimized/single moves.
                    MotionProfile.pathToSvg(path);
                    double unoptimizedTime = path.getOverallTime();
                    // Solve. 
                    double t0 = NanosecondTime.getRuntimeSeconds(); 
                    MotionProfile.solvePath(path);
                    double solvingTime = NanosecondTime.getRuntimeSeconds() - t0; 
                    for (MotionProfile [] profiles : path) {
                        System.out.println("X:"+profiles[0]);
                        System.out.println("Y:"+profiles[1]);
                        System.out.println("Z:"+profiles[2]);
                        System.out.println(" ");
                    }
                    MotionProfile.pathToSvg(path);
                    System.out.println("Solved, jerk: "+path.jerk+", "+(path.sCurves ? " simple S-Curves, " : "")
                            +"total move time: "+String.format("%.2f", path.getOverallTime())+" s, "
                            +"from unoptimized: "+String.format("%.2f", unoptimizedTime)+" s, "
                            +"solving time: "+String.format("%.4f", solvingTime*1000)+" ms");
                }
            }
        }
    }
}