import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openpnp.model.MotionProfile;
import org.openpnp.model.MotionProfile.ProfileOption;
import org.pmw.tinylog.Logger;

public class AdvancedMotionTest {


    @Test 
    public void testMotionProfiles() throws Exception {
        MotionProfile profile;

        // Move from/to still-stand
        profile = new MotionProfile(
                0, 600, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Long move from/to still-stand", profile);
        // Short move from/to still-stand
        profile = new MotionProfile(
                0, 200, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Short move from/to still-stand", profile);
        // Low feed-rate from/to still-stand
        profile = new MotionProfile(
                0, 200, 0, 0, 0, 0,
                0, 1000, 100, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Low feed-rate from/to still-stand", profile);
        // Tiny move from/to still-stand
        profile = new MotionProfile(
                0, 10, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Tiny move from/to still-stand", profile);
        // Micro move from/to still-stand
        profile = new MotionProfile(
                0, 0.0001, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Micro move from/to still-stand", profile);
        // Short move with unconstrained exit 
        profile = new MotionProfile(
                0, 100, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, ProfileOption.UnconstrainedExit.flag());
        testProfile("Short move with unconstrained exit", profile);
        // Short move with unconstrained entry 
        profile = new MotionProfile(
                0, 100, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, ProfileOption.UnconstrainedEntry.flag());
        testProfile("Short move with unconstrained entry", profile);
        // Tiny move with unconstrained exit 
        profile = new MotionProfile(
                0, 10, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, ProfileOption.UnconstrainedExit.flag());
        testProfile("Tiny move with unconstrained exit", profile);
        // Tiny move with unconstrained entry 
        profile = new MotionProfile(
                0, 10, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, ProfileOption.UnconstrainedEntry.flag());
        testProfile("Tiny move with unconstrained entry", profile);
        // Move with entry/exit velocity
        profile = new MotionProfile(
                0, 100, 700, 700, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Move with max entry/exit velocity", profile);
        // Move with lower entry/exit velocity
        profile = new MotionProfile(
                0, 100, 200, 200, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Move with lower entry/exit velocity", profile);
        // Move with entry/exit velocity and min time
        profile = new MotionProfile(
                0, 400, 500, 300, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 2, Double.POSITIVE_INFINITY, 0);
        testProfile("Move with entry/exit velocity and min time", profile);
        // Move with entry/exit velocity/acceleration
        profile = new MotionProfile(
                0, 400, 700, -700, 2000, 2000,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Move with entry/exit velocity/acceleration", profile);
        // Null moves
        profile = new MotionProfile(
                0, 0, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Still-stand", profile);
        profile = new MotionProfile(
                0, 0, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY, 0);
        testProfile("Still-stand with min-time", profile);
        profile = new MotionProfile(
                0, 0, 700, 700, 2000, 2000,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Zero displacement move with entry/exit velocity/acceleration", profile);

        // Pure overshoot
        profile = new MotionProfile(
                0, 0, 700, -700, 2000, 2000,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Overshoot", profile);
        // moveToLoactionAtSafeZ() with min time (given by other axes move time) 
        profile = new MotionProfile(
                0, 0, 700, -700, 2000, 2000,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY, 0);
        testProfile("Z axis in moveToLoactionAtSafeZ() with min time", profile);
        // Twisted curve
        profile = new MotionProfile(
                0, 0, 500, 500, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY, 0);
        testProfile("Twisted curve (min-time)", profile);
        // Twisted curve with delta
        profile = new MotionProfile(
                0, 10, 500, 500, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY, 0);
        testProfile("Twisted curve (min-time) with delta", profile);

        // Complex move with min-time
        profile = new MotionProfile(
                0, 400, 0, -20, 0, -100,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY, 0);
        testProfile("Complex move with min-time", profile);
        // Recalc a new profile with the solution params (but no min-time) to check if it results in the same profile. 
        MotionProfile profile2 = new MotionProfile(
                0, 400, 0, -20, 0, -100,
                0, 1000, 
                profile.getProfileVelocity(), 
                profile.getProfileEntryAcceleration(), 
                profile.getProfileExitAcceleration(),
                profile.getProfileJerk(),
                0.0, Double.POSITIVE_INFINITY, 0);
        testProfile("recheck solution", profile2);

        for (int s = -1; s <= 401; s++) {
            Double tf = profile.getForwardCrossingTime(s, true);
            Double tb = profile.getBackwardCrossingTime(s, true);
            System.out.println("t cross "+s+" t fw = "+tf+" tb = "+tb
                    +" V fw = "+(tf == null ? null : profile.getMomentaryVelocity(tf))
                    +" V bw = "+(tb == null ? null : profile.getMomentaryVelocity(tb))
                    +" s fw = "+(tf  == null ? null : profile.getMomentaryLocation(tf))
                    +" s bw = "+(tb == null ? null : profile.getMomentaryLocation(tb)));
        }
    }

    private void testProfile(String message, MotionProfile profile) {
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

        MotionProfile.ErrorState error;
        System.out.println(message);
        profile.solveProfile();
        System.out.println(profile);
        error = profile.assertValidity();
        if (error != null) {
            Logger.error(message+" has error "+error);
        }
        // To make sure we got all the signs right, test the reverse.
        profileRev.solveProfile();
        System.out.println(profileRev+" (reverse)");
        error = profileRev.assertValidity();
        if (error != null) {
            Logger.error(message+" (reverse) has error: "+error);
        }
        System.out.println(" ");
    }



    @Test 
    public void testMotionPaths() throws Exception {
        moveTo(0, 0, safeZ);
        moveTo(0, 0, -15);
        moveTo(0, 0, safeZ);
        moveTo(0, 100, safeZ);
        moveTo(0, 100, -15);

        MotionProfile.solvePath(path);
    }

    List<MotionProfile []> path = new ArrayList<>();
    double x0 = 0;
    double y0 = 0;
    double z0 = 0;
    final double safeZ = -5;

    private void moveTo(double x, double y, double z) {
        MotionProfile [] profiles = new MotionProfile[3];
        profiles[0] = new MotionProfile(
                x0, x, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, (z0 >= safeZ && z >= safeZ) ? 0 : ProfileOption.Coordinated.flag());
        profiles[1] = new MotionProfile(
                y0, y, 0, 0, 0, 0,
                0, 500, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, (z0 >= safeZ && z >= safeZ) ? 0 : ProfileOption.Coordinated.flag());
        profiles[2] = new MotionProfile(
                z0, z, 0, 0, 0, 0,
                -20, 5, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, (z0 >= safeZ && z >= safeZ) ? 0 : ProfileOption.Coordinated.flag());
        path.add(profiles);
        x0 = x;
        y0 = y;
        z0 = z;
    }

}
