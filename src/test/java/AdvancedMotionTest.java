import org.junit.Test;
import org.openpnp.model.MotionProfile;
import org.pmw.tinylog.Logger;

public class AdvancedMotionTest {


    @Test 
    public void test() throws Exception {
        MotionProfile profile;/*
        // Move from/to still-stand
        profile = new MotionProfile(
                0, 600, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Move from/to still-stand", profile);
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
        testProfile("Move with entry/exit velocity and min time", profile);*/
        // Move with entry/exit velocity/acceleration
        profile = new MotionProfile(
                0, 400, 700, -700, 2000, 2000,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Move with entry/exit velocity/acceleration", profile);
        // Complex move with min-time
        profile = new MotionProfile(
                0, 400, 0, 200, 0, -100,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY, 0);
        testProfile("Complex move with min-time", profile);
        // Recalc a new profile with the solution params (but no min-time) to check if it results in the same profile. 
        MotionProfile profile2 = new MotionProfile(
                0, 400, 0, 200, 0, -100,
                0, 1000, 
                profile.getProfileVelocity(), 
                profile.getProfileEntryAcceleration(), 
                profile.getProfileExitAcceleration(),
                profile.getProfileJerk(),
                0.0, Double.POSITIVE_INFINITY, 0);
        testProfile("recheck solution", profile2);

        // Null moves
        profile = new MotionProfile(
                0, 0, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Still-stand", profile);
        profile = new MotionProfile(
                0, 0, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY, 0);
        testProfile("Still-stand with min.time", profile);
        profile = new MotionProfile(
                0, 0, 700, 700, 2000, 2000,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Null move", profile);

        // Pure overshoot
        profile = new MotionProfile(
                0, 0, 700, -700, 2000, 2000,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY, 0);
        testProfile("Overshoot", profile);
        // moveToLoactionAtSafeZ() with min time (given by other axes move time) 
        profile = new MotionProfile(
                0, 0, 700, -700, 2000, 2000,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY, 0);
        testProfile("moveToLoactionAtSafeZ() with min time", profile);
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
    }

    private void testProfile(String message, MotionProfile profile) {
        MotionProfile.ErrorState error;
        System.out.println(message);
        profile.solveProfile();
        System.out.println(profile);
        error = profile.assertValidity();
        if (error != null) {
            Logger.error(message+" has error "+error);
        }
        // To make sure we got all the signs right, test the reverse.
        MotionProfile profileRev = new MotionProfile(
                profile.getLocation(MotionProfile.segments), profile.getLocation(0), 
                -profile.getVelocity(MotionProfile.segments), -profile.getVelocity(0), 
                profile.getAcceleration(MotionProfile.segments), profile.getAcceleration(0),
                profile.getLocationMin(), profile.getLocationMax(),
                profile.getVelocityMax(), 
                profile.getEntryAccelerationMax(), 
                profile.getExitAccelerationMax(),
                profile.getJerkMax(),
                profile.getTimeMin(), profile.getTimeMax(), 0);
        profileRev.solveProfile();
        System.out.println(profileRev+" (reverse)");
        error = profileRev.assertValidity();
        if (error != null) {
            Logger.error(message+" (reverse) has error: "+error);
        }
        System.out.println(" ");
    }
}
