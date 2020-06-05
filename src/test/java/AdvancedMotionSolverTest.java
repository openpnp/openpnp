import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.driver.AdvancedMotionSolver;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Motion;
import org.openpnp.util.NanosecondTime;
import org.pmw.tinylog.Logger;

public class AdvancedMotionSolverTest {
    ReferenceControllerAxis axisX = new ReferenceControllerAxis();
    ReferenceControllerAxis axisZ = new ReferenceControllerAxis();
    AdvancedMotionSolver solver = new AdvancedMotionSolver();
    List<Motion> motionPath = new ArrayList<>();
    private double x0;
    private double z0;



    @Test 
    public void test() throws Exception {
        LengthUnit mm = LengthUnit.Millimeters;
        axisX.setName("X");
        axisX.setFeedratePerSecond(new Length(100, mm));
        axisX.setAccelerationPerSecond2(new Length(200, mm));
        axisX.setJerkPerSecond3(new Length(800, mm));
        axisX.setSoftLimitLow(new Length(-1, mm));
        axisX.setSoftLimitLowEnabled(true);
        axisX.setSoftLimitHigh(new Length(500, mm));
        axisX.setSoftLimitHighEnabled(true);
        
        axisZ.setName("Z");
        axisZ.setFeedratePerSecond(new Length(50, mm));
        axisZ.setAccelerationPerSecond2(new Length(100, mm));
        axisZ.setJerkPerSecond3(new Length(400, mm));
        axisZ.setSoftLimitLow(new Length(-15, mm));
        axisZ.setSoftLimitLowEnabled(true);
        axisZ.setSoftLimitHigh(new Length(10, mm));
        axisZ.setSoftLimitHighEnabled(true);
        /*x0 = 0;
        z0 = -10;
        moveTo(0, -10);*/
        moveTo(280, 0, true);
        /*moveTo(280, -10);
        moveTo(280, -10);*/
        double t0 = NanosecondTime.getRuntimeSeconds();
        solver.elaborateMotionPath(motionPath, solver.new ConstraintsModeller());
        solver.solve(100000, 0.000000001);
        Logger.debug("solver state: {}, iterations: {}, error: {}, time: {}ms", 
                solver.getSolverState(), solver.getFunctionEvalCount(), solver.getLastError(), 
                (NanosecondTime.getRuntimeSeconds()-t0)*1000.0);
    }

    public void moveTo(double x, double z, boolean stillstand) {
        final int segments = (x == x0 && z == z0) ? 1 : 7;
        for (int segment = motionPath.size() == 0 ? 0 : 1; segment <= segments; segment++) {
            double ratio = (double)segment/segments;
            double j = 0;
            /*if (segment == 1 || segment == segments) {
                j = 1;
            }
            else if (segment == 3 || segment == segments-2) {
                j = -1;
            }
            else {
                j = 0;
            }*/
            if (motionPath.size() == 0 || (segment == segments && stillstand)) {
                addMove(x, z, ratio, j, Motion.MotionOption.Stillstand, Motion.MotionOption.FixedWaypoint, Motion.MotionOption.CoordinatedWaypoint);
            }
            else if (segment == segments) {
                addMove(x, z, ratio, j, Motion.MotionOption.FixedWaypoint, Motion.MotionOption.CoordinatedWaypoint);
            }
            else {
                addMove(x, z, ratio, j, Motion.MotionOption.CoordinatedWaypoint);
            }
        }
        x0 = x;
        z0 = z;
    }

    public void addMove(double x, double z, double ratio, double j, Motion.MotionOption... options) {
        motionPath.add(new Motion(1.0, null, new AxesLocation[] { 
                new AxesLocation(axisX, (1.0-ratio)*x0+ratio*x).put(new AxesLocation(axisZ, (1.0-ratio)*z0+ratio*z)), 
                new AxesLocation(axisX, 0).put(new AxesLocation(axisZ, 0)), 
                new AxesLocation(axisX, 0).put(new AxesLocation(axisZ, 0)), 
                new AxesLocation(axisX, Math.signum(x-x0)*j).put(new AxesLocation(axisZ, Math.signum(z-z0)*j))
                },
                options));
    }
}
