import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openpnp.model.MotionProfile;
import org.openpnp.model.MotionProfile.ErrorState;
import org.openpnp.model.MotionProfile.ProfileOption;

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
        // moveToLoactionAtSafeZ() with min time (given by other axes move time) 
        profile = new MotionProfile(
                0, 0, 700, -700, 2000, 2000,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY, 0);
        testProfile("Z axis in moveToLoactionAtSafeZ() with min time", profile, null);
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
                profile.getProfileVelocity(), 
                profile.getProfileEntryAcceleration(), 
                profile.getProfileExitAcceleration(),
                profile.getProfileJerk(),
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

        testProfileCase(message, profile, expectedError);
        testProfileCase(message+" (reverse)", profileRev, expectedError);
        testProfileCase(message+" (constant aceleration)", profileConstantAcc, expectedError);
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

//  TODO: ADVANCED MOTION PLANNER 

//    @Test 
//    public void testMotionPaths() throws Exception {
//        for (int warmup = 0; warmup < 4; warmup++) {
//            path = new ArrayList<>();
//            moveTo(0, 0, safeZ);
//            // pick & place
//            moveTo(0, 0, -15);
//            moveTo(0, 0, safeZ);
//            moveTo(100, 0, safeZ);
//            moveTo(100, 0, -15);
//            moveTo(100, 0, safeZ);
//            moveTo(120, 0, safeZ);
//            moveTo(120, 0, -15);
//            moveTo(120, 0, safeZ);
//            moveTo(124, 0, safeZ);
//            moveTo(124, 0, -15);
//            moveTo(124, 0, safeZ);
//            moveTo(125, 0, safeZ);
//            moveTo(125, 0, -15);
//            moveTo(125, 0, safeZ);
//            // move to push/pull feeder
//            moveTo(200, 50, safeZ);
//            moveTo(220, 50, safeZ-5);
//            moveTo(220, 50, safeZ);
//            moveTo(200, 80, safeZ);
//            moveTo(150, 100, safeZ);
//            moveTo(150, 120, safeZ);
//            moveTo(300, 120, -15);
//            moveTo(300, 150, -15);
//            moveTo(280, 150, -15);
//            moveTo(279, 150, -15);
//            moveTo(275, 150, -15);
//            moveTo(275, 150, safeZ);
//    
//            MotionProfile.solvePath(path);
//        }
//        double solvingTime = 0;
//        for (MotionProfile [] profiles : path) {
//            System.out.println("X:"+profiles[0]);
//            System.out.println("Y:"+profiles[1]);
//            System.out.println("Z:"+profiles[2]);
//            System.out.println(" ");
//            solvingTime += profiles[0].getSolvingTime() +profiles[1].getSolvingTime() + profiles[2].getSolvingTime();  
//        }
//        System.out.println("Total solving time: "+String.format("%.4f", solvingTime*1000)+" ms");
//    }
//
//    List<MotionProfile []> path = new ArrayList<>();
//    double x0 = 0;
//    double y0 = 0;
//    double z0 = 0;
//    final double safeZ = -5;
//
//    private void moveTo(double x, double y, double z) {
//        MotionProfile [] profiles = new MotionProfile[3];
//        profiles[0] = new MotionProfile(
//                x0, x, 0, 0, 0, 0,
//                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, (z0 >= safeZ && z >= safeZ) ? 0 : ProfileOption.Coordinated.flag());
//        profiles[1] = new MotionProfile(
//                y0, y, 0, 0, 0, 0,
//                0, 500, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, (z0 >= safeZ && z >= safeZ) ? 0 : ProfileOption.Coordinated.flag());
//        profiles[2] = new MotionProfile(
//                z0, z, 0, 0, 0, 0,
//                -20, 5, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, (z0 >= safeZ && z >= safeZ) ? 0 : ProfileOption.Coordinated.flag());
//        path.add(profiles);
//        x0 = x;
//        y0 = y;
//        z0 = z;
//    }

}
