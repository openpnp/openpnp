import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.junit.jupiter.api.Test;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Length;
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
    
    @Test
    public void testComputeAxesScalingAndNonSquarenessCorrection() throws Exception {
        
        final double measurementErrorMag = 0.1; //mm
        
        //This is the true axes scaling and non-squareness (in the real world these are the unknowns that we are trying to find)
        final double[] actualCompensation = new double[] {1.01822770022899, 0.998429967322, -0.0028127154010650995};
        RealMatrix actualTransformFromUncompensatedToCompensated = MatrixUtils.createRealMatrix(
                new double[][] {{actualCompensation[0], actualCompensation[2]},
                    {0, actualCompensation[1]}});
        RealMatrix actualTransformFromCompensatedToUncompensated = MatrixUtils.inverse(actualTransformFromUncompensatedToCompensated);
        
        //This represents the scaling and non-squareness corrections that OpenPnP is currently applying while the operator is collecting the data
        final double[] oldCompensation = new double[] {1.0, 1.0, 0.002163};
        RealMatrix transformFromUncompensatedToCompensated = MatrixUtils.createRealMatrix(
                new double[][] {{oldCompensation[0], oldCompensation[2]},
                    {0, oldCompensation[1]}});
        
        //For this test, assume the operator is using only even (or odd) centimeter marks on the ruler. 
        Length rulerMultiple = new Length(20, LengthUnit.Millimeters);
        
        //The simulated ruler is about 300mm (1 foot) in length. This is the integer number of ticks it has.
        double numberOfRulerTicks = Math.round(new Length(300, LengthUnit.Millimeters).divide(rulerMultiple)) + 1;
        double midTickMark = (numberOfRulerTicks - 1) / 2;
        
        //For this test, assume the operator picks ticks that are within 80mm (about 3 inches) from each end of the ruler
        int tickRange = (int) Math.round(new Length(80, LengthUnit.Millimeters).divide(rulerMultiple));
        
        int numberOfRulerPlacements = 6;
        
        Random rand = new Random();
        rand.setSeed(3);
        
        //Generate data from simulated ruler placements
        List<Location> rulerLocations = new ArrayList<>();
        for (int i=0; i<numberOfRulerPlacements; i++) {
            //Simulate the operator placing the ruler with its midpoint somewhere on the table at 
            //roughly equal angle divisions over a 180 degree range
            double[] midPoint = new double[] {500 + 100*(rand.nextDouble()-0.5), 500 + 100*(rand.nextDouble()-0.5)};
            double rulerAngle = i * Math.PI / numberOfRulerPlacements + 0.3 * (rand.nextDouble() - 0.5);
            
            //Simulate the operator picking two tick marks near the ends of the ruler 
            double firstTickMark = rulerMultiple.getValue()*(rand.nextInt(tickRange));
            double secondTickMark = rulerMultiple.getValue()*(numberOfRulerTicks - rand.nextInt(tickRange));

            //Compute the true coordinates of the two tick marks (in the real world these would be unknowns)
            double[] unitVector = new double[] {Math.cos(rulerAngle), Math.sin(rulerAngle)};
            RealMatrix ticksInPerfectCoordSystem = MatrixUtils.createRealMatrix(new double[][] {
                {midPoint[0] + unitVector[0]*(firstTickMark - midTickMark), midPoint[0] + unitVector[0]*(secondTickMark - midTickMark)},
                {midPoint[1] + unitVector[1]*(firstTickMark - midTickMark), midPoint[1] + unitVector[1]*(secondTickMark - midTickMark)}});

            //Simulate some measurement error
            RealMatrix simulatedMeasurementError = MatrixUtils.createRealMatrix(new double[][] {
                {measurementErrorMag*(2*rand.nextDouble()-1), measurementErrorMag*(2*rand.nextDouble()-1)},
                {measurementErrorMag*(2*rand.nextDouble()-1), measurementErrorMag*(2*rand.nextDouble()-1)}});
            RealMatrix ticksWithMeasurementError = ticksInPerfectCoordSystem.add(simulatedMeasurementError);
            
            //Compute the coordinates in the actual uncompensated coordinate system
            RealMatrix ticksInActualUncompensatedCoordSystem = actualTransformFromCompensatedToUncompensated.multiply(ticksWithMeasurementError);
            
            //Apply the compensation that is currently in effect to get the simulated measurement
            RealMatrix ticksAsMeasuredByOpenpnp = transformFromUncompensatedToCompensated.multiply(ticksInActualUncompensatedCoordSystem);
            
            //Save the simulated measurements
            rulerLocations.add(new Location(LengthUnit.Millimeters, ticksAsMeasuredByOpenpnp.getEntry(0, 0), ticksAsMeasuredByOpenpnp.getEntry(1, 0), 0, 0) );
            rulerLocations.add(new Location(LengthUnit.Millimeters, ticksAsMeasuredByOpenpnp.getEntry(0, 1), ticksAsMeasuredByOpenpnp.getEntry(1, 1), 0, 0) );
        }
        
        //Allocate space for the new compensation values and initialize them with the current values
        double[] newCompensation = new double[3];
        System.arraycopy(oldCompensation, 0, newCompensation, 0, 3);
        
        //Allocate space for the residual errors
        double[] residualErrors = new double[rulerLocations.size()/2];
        
        //Compute the new compensation
        Length rmsError = Utils2D.computeAxesScalingAndNonSquarenessCorrection(newCompensation, rulerLocations, rulerMultiple, residualErrors);
        
        //Show the results
        System.out.println("rmsError = " + rmsError);
        System.out.print("residualErrors = [" + residualErrors[0]);
        for (int i=1; i<rulerLocations.size()/2; i++) {
            System.out.print(", " + residualErrors[i]);
        }
        System.out.println("]");
        System.out.println("Measured X Scaling      = " + newCompensation[0] + ", actual = " + actualCompensation[0]);
        System.out.println("Measured Y Scaling      = " + newCompensation[1] + ", actual = " + actualCompensation[1]);
        System.out.println("Measured Non-Squareness = " + newCompensation[2] + ", actual = " + actualCompensation[2]);
        
        //Test the results
        within("Scaling X", newCompensation[0], actualCompensation[0], 3e-4);
        within("Scaling Y", newCompensation[1], actualCompensation[1], 3e-4);
        within("Non-Squareness", newCompensation[2], actualCompensation[2], 1e-3);
    }
}


