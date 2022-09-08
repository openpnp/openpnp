import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.model.Point;
import org.openpnp.util.Collect;
import org.openpnp.util.QuickHull;
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;

import com.google.common.collect.Sets;

public class QuickHullTest {
    public static Placement randomPlacement() {
        Placement placement = new Placement("" + Math.random());
        placement.setLocation(randomLocation());
        return placement;
    }

    public static BoardLocation randomBoardLocation() {
        BoardLocation bl = new BoardLocation(new Board());
        bl.setLocation(randomLocation());
        bl.setGlobalSide(randomSide());
        return bl;
    }

    public static Side randomSide() {
        return Math.random() > 0.5d ? Side.Bottom : Side.Top;
    }

    public static Location randomLocation() {
        return new Location(LengthUnit.Millimeters, Math.random() * 100, Math.random() * 100, 0,
                Math.random() * 720 - 360);
    }

    public static void checkNormalized(Location loc, Location tst) throws Exception {
        checkNormalized(loc, tst.getX(), tst.getY(), tst.getZ(), tst.getRotation());
    }

    public static void check(Location loc, Location tst) throws Exception {
        check(loc, tst.getX(), tst.getY(), tst.getZ(), tst.getRotation());
    }

    public static void check(Location results, double x, double y, double z, double c)
            throws Exception {
        within("angle", results.getRotation(), c, 0.001);
        within("x", results.getX(), x, 0.01);
        within("y", results.getY(), y, 0.01);
        within("z", results.getZ(), z, 0.01);
    }

    public static void checkNormalized(Location results, double x, double y, double z, double c)
            throws Exception {
        within("angle", Utils2D.normalizeAngle(results.getRotation()), Utils2D.normalizeAngle(c),
                0.001);
        within("x", results.getX(), x, 0.01);
        within("y", results.getY(), y, 0.01);
        within("z", results.getZ(), z, 0.01);
    }

    public static void within(String name, double value, double target, double plusMinus)
            throws Exception {
        if (value > target + plusMinus) {
            throw new Exception(name + " " + value + " is greater than " + (target + plusMinus));
        }
        else if (value < target - plusMinus) {
            throw new Exception(name + " " + value + " is less than " + (target - plusMinus));
        }
    }

    @Test
    public void testQuickHull() throws Exception {
        List<Point> randomPoints = new ArrayList<>();
        for (int i=0; i<100000; i++) {
            double r = 100*Math.sqrt(-2*Math.log(Math.random()));
            double a = 2*Math.PI*Math.random();
            randomPoints.add(new Point(r*Math.cos(a), r*Math.sin(a)));
        }
        
        List<Point> hullPoints = QuickHull.quickHull(randomPoints);
        
        List<Point> bestPoints = null;
        double bestArea = 0;
        for (List<Point> tri : Collect.allCombinationsOfSize(hullPoints, 4)) {
            double a = Utils2D.polygonArea(tri);
            if (bestPoints == null || a > bestArea) {
                bestPoints = tri;
                bestArea = a;
            }
        }
        
        for (Point point : hullPoints) {
            Logger.trace(point);
        }
     }

}


