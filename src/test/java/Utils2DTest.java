import org.junit.Test;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
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

    private static Placement randomPlacement() {
        Placement placement = new Placement("" + Math.random());
        placement.setLocation(randomLocation());
        return placement;
    }

    private static BoardLocation randomBoardLocation() {
        BoardLocation bl = new BoardLocation(new Board());
        bl.setLocation(randomLocation());
        bl.setSide(randomSide());
        return bl;
    }

    private static Side randomSide() {
        return Math.random() > 0.5d ? Side.Bottom : Side.Top;
    }

    private static Location randomLocation() {
        return new Location(LengthUnit.Millimeters, Math.random() * 100, Math.random() * 100, 0,
                Math.random() * 720 - 360);
    }

    private static void checkNormalized(Location loc, Location tst) throws Exception {
        checkNormalized(loc, tst.getX(), tst.getY(), tst.getZ(), tst.getRotation());
    }

    private static void check(Location loc, Location tst) throws Exception {
        check(loc, tst.getX(), tst.getY(), tst.getZ(), tst.getRotation());
    }

    private static void check(Location results, double x, double y, double z, double c)
            throws Exception {
        within("angle", results.getRotation(), c, 0.001);
        within("x", results.getX(), x, 0.01);
        within("y", results.getY(), y, 0.01);
        within("z", results.getZ(), z, 0.01);
    }

    private static void checkNormalized(Location results, double x, double y, double z, double c)
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
    public void testCalculateBoardPlacementLocation() throws Exception {
        Location board = new Location(LengthUnit.Millimeters, 5, 15, -8, 0);
        Location place = new Location(LengthUnit.Millimeters, 55, 5, 0, 0);
        Location loc, loc1, loc2;
        int i = 0;
        loc2 = board;
        // simple verification:
        System.out.println("test" + ++i);
        System.out.println("board: " + board + " 100mm");
        System.out.println("place: " + place);
        loc = Utils2D.calculateBoardPlacementLocation(board, Side.Top, 100., place);
        System.out.println("results " + loc);
        check(loc, 60, 20, -8, 0);
        loc1 = Utils2D.calculateBoardPlacementLocationInverse(board, Side.Top, 100., loc);
        check(loc1, place);
        // now test some rotation.
        System.out.println("test" + ++i);
        board = board.rotateXy(55);
        loc = Utils2D.calculateBoardPlacementLocation(board, Side.Top, 100., place);
        loc1 = Utils2D.calculateBoardPlacementLocationInverse(board, Side.Top, 100., loc);
        System.out.println("results " + loc);
        check(loc, board.addWithRotation(place));
        check(loc1, place);

        System.out.println("test" + ++i);
        board = board.rotateXy(222);
        loc = Utils2D.calculateBoardPlacementLocation(board, Side.Top, 100., place);
        loc1 = Utils2D.calculateBoardPlacementLocationInverse(board, Side.Top, 100., loc);
        check(loc, board.addWithRotation(place));
        System.out.println("results " + loc);
        System.out.println("test" + ++i);
        System.out.println("test" + ++i);
        check(loc1, place);

        board = board.rotateXy(-322);
        loc = Utils2D.calculateBoardPlacementLocation(board, Side.Top, 100., place);
        loc1 = Utils2D.calculateBoardPlacementLocationInverse(board, Side.Top, 100., loc);
        check(loc, board.addWithRotation(place));
        System.out.println("results " + loc);
        System.out.println("test" + ++i);
        check(loc1, place);

        board = new Location(LengthUnit.Millimeters, 30.004, 31.386, -8, 74.646);
        loc1 = new Location(LengthUnit.Millimeters, 31, 6, 0, 0);
        System.out.println("board: " + board + " 100mm");
        System.out.println("place: " + loc1);
        loc = Utils2D.calculateBoardPlacementLocation(board, Side.Top, 100., loc1);
        System.out.println("results " + loc);
        loc1 = Utils2D.calculateBoardPlacementLocationInverse(board, Side.Top, 100., loc);
        System.out.println("inverse: " + loc1);
        check(loc, 32.426392, 62.868249, -8, 74.646);
        System.out.println("results " + loc);
        System.out.println("test" + ++i);
        check(loc1, 31, 6, 0, 0);

        // simple verification: BOT
        board = loc2;
        loc2 = place.multiply(-1., 1., 1., 1.)
                .add(new Location(LengthUnit.Millimeters, 100., 0., 0., 0.0));
        System.out.println("test" + ++i);
        System.out.println("board: " + board + " Width=" + 100.);
        System.out.println("place: " + place);
        loc = Utils2D.calculateBoardPlacementLocation(board, Side.Bottom, 100., place);
        System.out.println("results " + loc);
        check(loc, 5 + -55 + 100, 20, -8, 0);
        loc1 = Utils2D.calculateBoardPlacementLocationInverse(board, Side.Bottom, 100., loc);
        check(loc1, place);
        // now test some rotation.
        System.out.println("test" + ++i);
        board = board.rotateXy(55);
        loc = Utils2D.calculateBoardPlacementLocation(board, Side.Bottom, 100., place);
        loc1 = Utils2D.calculateBoardPlacementLocationInverse(board, Side.Bottom, 100., loc);
        System.out.println("results " + loc);
        check(loc, board.addWithRotation(loc2));
        check(loc1, place);

        System.out.println("test" + ++i);
        board = board.rotateXy(222);
        loc = Utils2D.calculateBoardPlacementLocation(board, Side.Bottom, 100., place);
        loc1 = Utils2D.calculateBoardPlacementLocationInverse(board, Side.Bottom, 100., loc);
        check(loc, board.addWithRotation(loc2));
        System.out.println("results " + loc);
        System.out.println("test" + ++i);
        System.out.println("test" + ++i);
        check(loc1, place);

        board = board.rotateXy(-322);
        loc = Utils2D.calculateBoardPlacementLocation(board, Side.Bottom, 100., place);
        loc1 = Utils2D.calculateBoardPlacementLocationInverse(board, Side.Bottom, 100., loc);
        check(loc, board.addWithRotation(loc2));
        System.out.println("results " + loc);
        System.out.println("test" + ++i);
        check(loc1, place);
    }
}


