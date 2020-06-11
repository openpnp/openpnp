import org.junit.Test;
import org.openpnp.model.MotionProfile;
import org.pmw.tinylog.Logger;

public class AdvancedMotionTest {


    @Test 
    public void test() throws Exception {
        MotionProfile profile;
        // Move from/to still-stand
        profile = new MotionProfile(
                0, 400, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY);
        Logger.info("Move from/to still-stand:\n{}", profile);
        // Short move from/to still-stand
        profile = new MotionProfile(
                0, 200, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY);
        Logger.info("Short move from/to still-stand:\n{}", profile);
        // Tiny move from/to still-stand
        profile = new MotionProfile(
                0, 20, 0, 0, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY);
        Logger.info("Tiny move from/to still-stand:\n{}", profile);
        // Complex move with min-time
        profile = new MotionProfile(
                0, 400, 0, 200, 0, -100,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY);
        Logger.info("Complex move with min-time:\n{}", profile);
        // Recalc a new profile with the solution params (but no min-time) to check if it results in the same profile. 
        MotionProfile profile2 = new MotionProfile(
                0, 400, 0, 200, 0, -100,
                0, 1000, 
                profile.getProfileVelocity(), 
                profile.getProfileEntryAcceleration(), 
                profile.getProfileExitAcceleration(),
                profile.getProfileJerk(),
                0.0, Double.POSITIVE_INFINITY);
        Logger.info("recheck solution:\n{}", profile2);

        // Move with entry/exit velocity/acceleration
        profile = new MotionProfile(
                0, 400, 700, -700, 2000, 2000,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY);
        Logger.info(profile);
        // Pure overshoot
        profile = new MotionProfile(
                0, 0, 700, -700, 2000, 2000,
                0, 1000, 700, 2000, 2000, 15000, 0, Double.POSITIVE_INFINITY);
        Logger.info("Overshoot:\n{}", profile);
        // moveToLoactionAtSafeZ() with min time (given by other axes move time) 
        profile = new MotionProfile(
                0, 0, 700, -700, 2000, 2000,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY);
        Logger.info("moveToLoactionAtSafeZ() with min time:\n{}", profile);
        // S shaped curve
        profile = new MotionProfile(
                0, 0, 500, 500, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY);
        Logger.info("S-shaped curve:\n{}", profile);
        // S shaped curve
        profile = new MotionProfile(
                0, 1, 500, 500, 0, 0,
                0, 1000, 700, 2000, 2000, 15000, 4.0, Double.POSITIVE_INFINITY);
        Logger.info("S-shaped curve with delta:\n{}", profile);
    }
}
