import java.awt.geom.AffineTransform;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Triangle;
import org.openpnp.model.Placement;
import org.openpnp.util.Utils2D;

public class Utils2DTest {
    /**
     * Test Utils2D.calculateBoardLocation with 100k random BoardLocations.
     * 
     * @throws Exception
     */
    @Test
    public void testCalculateBoardLocationRandom() throws Exception {
        for (int i = 0; i < 100000; i++) {
            // The actual board location that we are looking for. This is where the board actually
            // is on the table.
            BoardLocation actualBoardLocation = randomBoardLocation();

            // Two sample placements
            Placement placementA = randomPlacement();

            Placement placementB = randomPlacement();

            // Given the actual board location and the two placements, calculate where they should
            // be after transforming. This calculation is known to be good.
            Location actualLocationA = Utils2D
                    .calculateBoardPlacementLocation(actualBoardLocation, placementA.getLocation())
                    .derive(null, null, null, 0d);
            Location actualLocationB = Utils2D
                    .calculateBoardPlacementLocation(actualBoardLocation, placementB.getLocation())
                    .derive(null, null, null, 0d);

            // Using a random input board location, which should not actually affect the result,
            // calculate the board location from the known actual locations. The result
            // should be the actual board location.
            BoardLocation testBoardLocation = randomBoardLocation();
            testBoardLocation.setSide(actualBoardLocation.getSide());
            Location location = Utils2D.calculateBoardLocation(testBoardLocation, placementA,
                    placementB, actualLocationA, actualLocationB);

            try {
                checkNormalized(actualBoardLocation.getLocation(), location);
            }
            catch (Exception e) {
                throw new Exception(String.format(
                        "actualBoardLocation %s, placementA %s, placementB %s, testBoardLocation %s, location %s",
                        actualBoardLocation, placementA, placementB, testBoardLocation, location),
                        e);
            }
        }
    }

    public static Placement randomPlacement() {
        Placement placement = new Placement("" + Math.random());
        placement.setLocation(randomLocation());
        return placement;
    }

    public static BoardLocation randomBoardLocation() {
        BoardLocation bl = new BoardLocation(new Board());
        bl.setLocation(randomLocation());
        bl.setSide(randomSide());
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

    /**
     * Test calculating a board location with affine transformations and without. Check that
     * the results are the same and that they match the gold results.
     * @throws Exception
     */
    @Test
    public void testCalculateBoardPlacementLocationSimple() throws Exception {
        Board board = new Board();
        
        BoardLocation boardLocation = new BoardLocation(board);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 5, 15, -8, -6));

        Placement placement = new Placement("T1");
        placement.setLocation(new Location(LengthUnit.Millimeters, 55, 5, 0, 90));
        board.addPlacement(placement);
        
        AffineTransform tx = new AffineTransform();
        tx.translate(5, 15);
        tx.rotate(Math.toRadians(-6));
        
        Location locationBefore = Utils2D.calculateBoardPlacementLocation(boardLocation, placement.getLocation());
        System.out.println(locationBefore);
        
        boardLocation.setPlacementTransform(tx);
        Location locationAfter = Utils2D.calculateBoardPlacementLocation(boardLocation, placement.getLocation());
        System.out.println(locationAfter);
        
        check(locationBefore, 60.22, 14.22, -8, 84);
        check(locationAfter, 60.22, 14.22, -8, 84);
    }

    /**
     * Test Utils2D.getAngleFromPoint() in the new correct way.
     * @throws Exception
     */
    @Test
    public void testAngleFromPoint() throws Exception {
        for (double angle = 0; angle <= 360; angle +=5) {
            Location location0 = new Location(LengthUnit.Millimeters); 
            Location location1 = new Location(LengthUnit.Millimeters,
                    Math.cos(Math.toRadians(angle)), Math.sin(Math.toRadians(angle)), -10, 0);
            double angleDetermined = Utils2D.getAngleFromPoint(location0, location1);
            if (1e-9 < Math.abs((angleDetermined - angle) % 360)) {
                throw new Exception("Utils2D.getAngleFromPoint() given "+angle+"° angled points, but returned "+angleDetermined+"°.");
            }
        }
    }

    /**
     * Test calculating a board location with affine transformations and without. Check that
     * the results are the same and that they match the gold results.
     * @throws Exception
     */
    @Test
    public void testSquarenessFromRotatedTriangles() throws Exception {
        boolean debug = false;
        
        //Reference triangle shape.  This will be rotated and translated (not scaled) later.
        Location ref1 = new Location(LengthUnit.Millimeters, 0, -98.0, 0.0, 0.0);
        Location ref2 = new Location(LengthUnit.Millimeters, .0, 0.0, 0.0, 0.0);
        Location ref3 = new Location(LengthUnit.Millimeters, 75.0, 2.0, 0.0, 0.0);
        
        final double skewX = 0.1;
        final double skewY = 0.1;
        System.out.println("skewX:" + skewX + "  skewY:" + skewY);

        Triangle ref = new Triangle(ref1, ref2, ref3);
        
        final double rot1angle = -2.25;
        final double rot2angle = 94.5;
        final Location rot1center = new Location(LengthUnit.Millimeters, -1.0, 1.5, 0.0, 0.0);
        final Location rot2center = new Location(LengthUnit.Millimeters, 35.0, 85, 0.0, 0.0);
        
        System.out.println("rot1angle:" + rot1angle + " rot1center:" + rot1center);
        System.out.println("rot2angle:" + rot2angle + " rot2center:" + rot2center);

        Triangle rot1 = ref.rotate(rot1angle,   rot1center);
        Triangle rot2 = ref.rotate(rot2angle,   rot2center);
        
        Triangle rot1skew = rot1.skew(skewX, skewY);
        Triangle rot2skew = rot2.skew(skewX, skewY);
        
        if (debug) {
            double[] refAngles = ref.getLineAngles();
            String outstr = "Ref triangle line angles";
            for (double angle : refAngles) {
                outstr = outstr.concat(" :" + angle);
            }
            System.out.println(outstr);
            
            double[] internalAngles = ref.getInternalAngles();
            outstr = "Ref triangle internal angles";
            for (double angle : internalAngles) {
                outstr = outstr.concat(" :" + angle);
            }
            System.out.println(outstr);

            internalAngles = rot1.getInternalAngles();
            outstr = "rot1 internal angles";
            for (double angle : internalAngles) {
                outstr = outstr.concat(" :" + angle);
            }
            outstr = outstr.concat(" : total angle:" + Arrays.stream(internalAngles).sum());
            System.out.println(outstr);

            internalAngles = rot2.getInternalAngles();
            outstr = "rot2 internal angles";
            for (double angle : internalAngles) {
                outstr = outstr.concat(" :" + angle);
            }
            outstr = outstr.concat(" : total angle:" + Arrays.stream(internalAngles).sum());
            System.out.println(outstr);
            
            internalAngles = rot1skew.getInternalAngles();
            outstr = "rot1skew internal angles";
            for (double angle : internalAngles) {
                outstr = outstr.concat(" :" + angle);
            }
            outstr = outstr.concat(" : total angle:" + Arrays.stream(internalAngles).sum());
            System.out.println(outstr);

            internalAngles = rot2skew.getInternalAngles();
            outstr = "rot2skew internal angles";
            for (double angle : internalAngles) {
                outstr = outstr.concat(" :" + angle);
            }
            outstr = outstr.concat(" : total angle:" + Arrays.stream(internalAngles).sum());
            System.out.println(outstr);
        }

        
        double skewAngle = Utils2D.squarenessFromRotatedTriangles(rot1skew, rot2skew);
        System.out.println("skewAngle:" + skewAngle);
        final double skewOut = Math.sin(Math.toRadians(skewAngle));
        System.out.println("skewOut:" + skewOut);
        final double skewInputDiff = (skewX+skewY);
        System.out.println("skewDiff:" + skewInputDiff);
        final double skewError = Math.abs(skewOut-skewInputDiff);
        System.out.println("skewError:" + skewError);
        if (skewError > 0.05) {
            throw new Exception("Output skew:" + skewOut + "is out of range of input skew:" + skewInputDiff + " skewX:" + skewX + " skewY:" + skewY);
        }
        System.out.println("skewOut:" + skewOut);
    }
}

