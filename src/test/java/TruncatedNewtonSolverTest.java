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
    
    double [] target;
    double [] armAngles = new double[] { -1, 3 };  
    double [] armPosition= new double[] { 0, 0 };

    public void moveTo(double x, double y) throws Exception {
        target = new double[] {x, y};
        double t0 = NanosecondTime.getRuntimeSeconds();
        solve(armAngles, null, null, TNC_MSG_NONE, 1000, 0.001, 0.01*0.01, 0.0);
        double dx = armPosition[0] - target[0];
        double dy = armPosition[1] - target[1];
        double precision = Math.sqrt(Math.pow(dx, 2)+Math.pow(dy, 2));
        Logger.info(String.format("Robot arm commanded to %.3f, %.3f moved to %.3f, %.3f, precision %.4f, target 0.01, solved in %.2fms, %d evals, state %s.",
                target[0], target[1], armPosition[0], armPosition[1], precision, 
                (NanosecondTime.getRuntimeSeconds()-t0)*1000, getFunctionEvalCount(), getSolverState()));
        if (precision > 0.01) {
            Logger.error(String.format("Solver missed target. Distance %.4f larger than target 0.01.", precision));
            throw new Exception(String.format("Solver missed target. Distance %.4f larger than target 0.01.", precision));
        }

    }

    @Override
    protected void log(String message) {
        // Logging happens here if you enable the TMC_MSG_xxx options.
        Logger.info(message);
    }

    @Override
    protected double function(final double [] x, double [] g) throws Exception {
        // Assume a robot arm has two rotationally articulated joints for a certain reach in X/Y.
        // The joint angles are the variables in x[].
        // Solver task: adjust the angles to point the arm at the Cartesian target location.

        // First part of arm to "elbow".
        double angle1 = x[0];
        double actuator1X = Math.cos(angle1)*radius1;
        double actuator1Y = Math.sin(angle1)*radius1;
        // Second part or arm to point.
        double angle2 = x[1];
        double actuator2X = actuator1X+Math.cos(angle1+angle2)*radius2;
        double actuator2Y = actuator1Y+Math.sin(angle1+angle2)*radius2;
        // The arm put together, to the point.
        double anglePoint = Math.atan2(actuator2Y, actuator2X);
        double radiusPoint = Math.sqrt(Math.pow(actuator2X, 2)+Math.pow(actuator2Y, 2));
        // Calculate the derivatives: how does the point move, when the angles move infinitesimally.  
        double actuator1dXdT = -Math.sin(anglePoint)*radiusPoint;
        double actuator1dYdT = Math.cos(anglePoint)*radiusPoint;
        double actuator2dXdT = -Math.sin(angle1+angle2)*radius2;
        double actuator2dYdT = Math.cos(angle1+angle2)*radius2;
        // Store new arm position.
        armPosition[0] = actuator2X;
        armPosition[1] = actuator2Y;
        // Calculate the error to the correct solution. 
        double dx = armPosition[0] - target[0];
        double dy = armPosition[1] - target[1];
        double error = Math.pow(dx, 2)+Math.pow(dy, 2);
        // Unit vector and factor 2, as derivative of x^2 = 2x.
        double unit = 2/Math.sqrt(error);
        // Calculate the new gradient vector.
        g[0] = dx*unit*actuator1dXdT + dy*unit*actuator1dYdT;
        g[1] = dx*unit*actuator2dXdT + dy*unit*actuator2dYdT;

        // Function return error square.
        return error;
    }
}
