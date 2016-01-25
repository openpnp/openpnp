import org.junit.Test;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Board.Side;
import org.openpnp.util.Utils2D;

public class Utils2DTest {
    @Test
    public void testCalculateAngleAndOffset() throws Exception {
        // Create two placements to test with.
        Location idealA = new Location(LengthUnit.Millimeters, 5, 35, 0, 0);
        Location idealB = new Location(LengthUnit.Millimeters, 55, 5, 0, 0);
        
        // Rotate and translate the placements to simulate a board that has
        // been placed on the table
        double angle = 10;
        Location offset = new Location(idealA.getUnits(), 10, 10, 0, 0);
        
        Location actualA = idealA.rotateXy(angle);
        actualA = actualA.add(offset);
        Location actualB = idealB.rotateXy(angle);
        actualB = actualB.add(offset);
        
        System.out.println("idealA " + idealA);
        System.out.println("idealB " + idealB);
        System.out.println("actualA " + actualA);
        System.out.println("actualB " + actualB);
        
        Location results = Utils2D.calculateAngleAndOffset(idealA, idealB, actualA, actualB);
        
        System.out.println("results " + results);
        
        within("angle", results.getRotation(), angle, 0.001);
        within("x", results.getX(), offset.getX(), 0.01);
        within("y", results.getY(), offset.getY(), 0.01);
    }
  
    private static void check(Location loc,Location tst) throws Exception {
    check(loc,tst.getX(),tst.getY(),tst.getZ(),tst.getRotation()); 
    }

    private static void  
    check(Location results, double x, double y , double z, double c) throws Exception {
        within("angle", results.getRotation(), c, 0.001);
        within("x", results.getX(), x, 0.01);
        within("y", results.getY(), y, 0.01);
        within("z", results.getZ(), z, 0.01);
    }
    
    public static void within(String name, double value, double target, double plusMinus) throws Exception {
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
        Location loc ,loc1, loc2  ; int i=0; loc2=board;
	// simple verification:
        System.out.println("test"+ ++i);
        System.out.println("board: " + board + " 100mm" );
        System.out.println("place: " + place);
	loc=Utils2D.calculateBoardPlacementLocation(board, Side.Top, 100., place);
        System.out.println("results " + loc);
	check(loc,60,20,-8,0);
	loc1=Utils2D.calculateBoardPlacementLocationInverse(board, Side.Top, 100., loc);
	check(loc1,place);
	// now test some rotation.
        System.out.println("test"+ ++i);
        board = board.rotateXy(55);
	loc=Utils2D.calculateBoardPlacementLocation(board, Side.Top, 100., place);
	loc1=Utils2D.calculateBoardPlacementLocationInverse(board, Side.Top, 100., loc);
        System.out.println("results " + loc);
	check(loc,board.addWithRotation(place));
	check(loc1,place);

        System.out.println("test"+ ++i);
        board = board.rotateXy(222);
	loc=Utils2D.calculateBoardPlacementLocation(board, Side.Top, 100., place);
	loc1=Utils2D.calculateBoardPlacementLocationInverse(board, Side.Top, 100., loc);
	check(loc,board.addWithRotation(place));
        System.out.println("results " + loc);
        System.out.println("test"+ ++i);
        System.out.println("test"+ ++i);
	check(loc1,place);

        board = board.rotateXy(-322);
	loc=Utils2D.calculateBoardPlacementLocation(board, Side.Top, 100., place);
	loc1=Utils2D.calculateBoardPlacementLocationInverse(board, Side.Top, 100., loc);
	check(loc,board.addWithRotation(place));
        System.out.println("results " + loc);
        System.out.println("test"+ ++i);
	check(loc1,place);

	// simple verification: BOT
	board=loc2;
	loc2=place.multiply(-1.,1.,1.,1.).add(new Location(LengthUnit.Millimeters, 100., 0., 0., 0.0));
        System.out.println("test"+ ++i);
        System.out.println("board: " + board + " Width=" +100.);
        System.out.println("place: " + place);
	loc=Utils2D.calculateBoardPlacementLocation(board, Side.Bottom, 100., place);
        System.out.println("results " + loc);
	check(loc,5+-55+100,20,-8,0);
	loc1=Utils2D.calculateBoardPlacementLocationInverse(board, Side.Bottom, 100., loc);
	check(loc1,place);
	// now test some rotation.
        System.out.println("test"+ ++i);
        board = board.rotateXy(55);
	loc=Utils2D.calculateBoardPlacementLocation(board, Side.Bottom, 100., place);
	loc1=Utils2D.calculateBoardPlacementLocationInverse(board, Side.Bottom, 100., loc);
        System.out.println("results " + loc);
	check(loc,board.addWithRotation(loc2));
	check(loc1,place);

        System.out.println("test"+ ++i);
        board = board.rotateXy(222);
	loc=Utils2D.calculateBoardPlacementLocation(board, Side.Bottom, 100., place);
	loc1=Utils2D.calculateBoardPlacementLocationInverse(board, Side.Bottom, 100., loc);
	check(loc,board.addWithRotation(loc2));
        System.out.println("results " + loc);
        System.out.println("test"+ ++i);
        System.out.println("test"+ ++i);
	check(loc1,place);

        board = board.rotateXy(-322);
	loc=Utils2D.calculateBoardPlacementLocation(board, Side.Bottom, 100., place);
	loc1=Utils2D.calculateBoardPlacementLocationInverse(board, Side.Bottom, 100., loc);
	check(loc,board.addWithRotation(loc2));
        System.out.println("results " + loc);
        System.out.println("test"+ ++i);
	check(loc1,place);
}
}



