import org.junit.Test;
import org.openpnp.util.NanosecondTime;
import org.openpnp.util.TruncatedNewtonConstrainedSolver;
import org.pmw.tinylog.Logger;

/**
 * Simple Test and Application Example for the TruncatedNewtonConstrainedSolver.
 * 
 * Simulates a robot arm that has two rotationally articulated joints.
 * The joint angles are the variables in x[].
 * Solver task: adjust the angles to point the arm at the Cartesian target location.
 *
 */
public class TruncatedNewtonSolverTest extends TruncatedNewtonConstrainedSolver{

    @Test
    public void test() throws Exception {
        // Simple robot arm moves
        moveTo(100, 100);
        moveTo(50, 200 );
        // Draw a circle
        for (int a = 0; a <= 360; a +=15) {
            moveTo(Math.sin(Math.toRadians(a))*100,
                    200+Math.cos(Math.toRadians(a))*100);
        }
    }

    final double radius1 = 200;
    final double radius2 = 200;
    final double wantedPrecision = 0.001; // 1µm

    double [] target;
    double [] armAngles = new double[] { 0, 0 };  
    double [] armPosition= new double[] { Double.NaN, Double.NaN };

    public void moveTo(double x, double y) throws Exception {
        target = new double[] {x, y};
        double t0 = NanosecondTime.getRuntimeSeconds();
        solve(armAngles, null, null, TNC_MSG_NONE, 100, wantedPrecision, 0, 
                wantedPrecision*wantedPrecision*0.5); // Error is square, so we need to square too.
        double dx = armPosition[0] - target[0];
        double dy = armPosition[1] - target[1];
        double precision = Math.sqrt(Math.pow(dx, 2)+Math.pow(dy, 2));
        log(String.format("Robot arm commanded to %.3f, %.3f moved to %.3f, %.3f, precision %f, target %f, solved in %.3fms, %d evals, state %s.",
                target[0], target[1], armPosition[0], armPosition[1], precision, wantedPrecision,
                (NanosecondTime.getRuntimeSeconds()-t0)*1000, getFunctionEvalCount(), getSolverState()));
        if (precision > wantedPrecision) {
            Logger.error(String.format("Solver missed target: precision %f larger than target %f.", precision, wantedPrecision));
            throw new Exception("Solver missed target");
        }

    }

    @Override
    protected void log(String message) {
        // Logging happens here if you enable the TMC_MSG_xxx options.
        Logger.info(message);
    }

    @Override
    protected double function(final double [] x, double [] g) throws Exception {
        // Assume a robot arm has two rotationally articulated joints (an arm with "elbow") for a certain 2D reach in X/Y.
        // The joint angles are the variables in x[].
        // Solver task: adjust the angles to point the end of the arm at the Cartesian target location.

        // Calculate the end position of the first segment (the "elbow").
        double angle1 = x[0];
        double segment1X = Math.cos(angle1)*radius1;
        double segment1Y = Math.sin(angle1)*radius1;
        // The second segment is mounted on the first segment. Note how the angles are summed up too.
        double angle2 = x[1];
        double segment2X = segment1X+Math.cos(angle1+angle2)*radius2;
        double segment2Y = segment1Y+Math.sin(angle1+angle2)*radius2;

        // Store new arm position.
        armPosition[0] = segment2X;
        armPosition[1] = segment2Y;

        // Now calculate the error as the square distance to the target location. 
        double dx = armPosition[0] - target[0];
        double dy = armPosition[1] - target[1];
        double error = Math.pow(dx, 2)+Math.pow(dy, 2);

        // We need to calculate the gradient (or derivative) of f() for each variable.   

        // The first step is to know how the robot arm moves, when the variables change, i.e. when the
        // joint rotation angles change (momentarily i.e. "infinitesimally").

        // This can also be thought as the momentary tangent of the arc the arm describes when articulated. 
        // So rather than using trigonometrical derivative formulas let's use geometrical methods. To get the tangent, 
        // we rotate the arm vector by 90°, which is simply -Y, X. Conveniently the speed at which it would move
        // is proportional to the arm radius (leverage) so the length of the tangent vector is also just right.

        // Note, the first joint rotates the whole arm, so take the end of the second segment. And, as the arm is anchored 
        // at 0, 0, the second segment's end X, Y is directly the arm vector.  
        double actuator1dXdT = -segment2Y;
        double actuator1dYdT = segment2X;
        // The second joint (the "elbow") only rotates the second arm segment, so take only the difference from the first to 
        // the second. 
        double actuator2dXdT = -(segment2Y-segment1Y);
        double actuator2dYdT = segment2X-segment1X;

        // As the second step of calculating our gradient, we need to determine how the arm motion (the tangents) affect the error.
        // Does the tangent move the arm closer to or further away from the target? And by how much?  
        // For each variable i.e. joint angle we have the tangent vector and the distance vector.
        // The more these two vectors point in the same direction, the larger the contribution of an angular movement to reduce the error. 
        // Amazingly we can just multiply the vector coordinates with each other and build the sum to get the answer (called the dot product). 
        // The result is the cosine of the angle between the vectors, most notably it will automatically be negative if the tangent points 
        // away from the target. It will be near zero if the tangent is more or less perpendicular to the distance vector i.e. when angular 
        // movement can't help the robot move the arm closer and solve the problem. This means the other joint will be favored. 

        // We also add a Factor 2, as the derivative of the error distance square, d^2 is 2d. 
        // (We could probably leave that "math" factor away, I have not observed any meaningful difference in solver performance). 
        g[0] = 2*(dx*actuator1dXdT + dy*actuator1dYdT);
        g[1] = 2*(dx*actuator2dXdT + dy*actuator2dYdT);

        return error;
    }
}
