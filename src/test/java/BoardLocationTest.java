import java.awt.geom.AffineTransform;

import org.junit.Test;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.util.Utils2D;

/**
 * TODO STOPSHIP Need to stop fucking around and write the tests that matter. They are:
 * 1. calculateBoardPlacementLocation: Top, No TX
 * 2. calculateBoardPlacementLocation: Top, With TX
 * 3. calculateBoardPlacementLocation: Bottom, No TX, Width Not Set
 * 4. calculateBoardPlacementLocation: Bottom, With TX, Width Not Set
 * 5. calculateBoardPlacementLocation: Bottom, No TX, Width Set
 * 6. calculateBoardPlacementLocation: Bottom, With TX, Width Set
 * 
 * These should follow the "Understanding Board Locations" guide.
 * 
 * Also need to do deriveAffineTransform and then use it's results in the
 * above instead of building by hand. Treat it like we had done a fid check
 * like in the current test code of Utils2D.
 * 
 * The end result here is that the 8 tests should pass, and I should verify that the test job
 * works right with both width set and unset for all the boards. Once that's true, I can commit
 * and we should be gold.
 */
public class BoardLocationTest {
    @Test
    public void calculateBoardLocation_Top_NoAffine_NoWidth() throws Exception {
        Board board = new Board();
        BoardLocation boardLocation = new BoardLocation(board);
        boardLocation.setSide(Side.Top);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 7, 11, -3, 24));
        
        Placement p1 = new Placement("P1");
        p1.setLocation(new Location(LengthUnit.Millimeters, 10, 10, 0, 45));
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        Utils2DTest.checkNormalized(p1l, 12.068, 24.202, -3, 69.000);
    }
    
    @Test
    public void calculateBoardLocation_Top_WithAffine_NoWidth() throws Exception {
        Board board = new Board();
        BoardLocation boardLocation = new BoardLocation(board);
        boardLocation.setSide(Side.Top);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 7, 11, -3, 24));
        /**
         * The transform was created by performing a fid check, verifying the results manually
         * and then printing the resulting transform:
         * AffineTransform[[0.913545457642601, -0.4067366430758, 7.000000000000001], [0.4067366430758, 0.913545457642601, 11.000000000000002]]
         */
        AffineTransform tx = new AffineTransform(
                0.913545457642601, 
                0.4067366430758, 
                -0.4067366430758, 
                0.913545457642601, 
                7.000000000000001, 
                11.000000000000002
                );
        boardLocation.setPlacementTransform(tx);
        
        Placement p1 = new Placement("P1");
        p1.setLocation(new Location(LengthUnit.Millimeters, 10, 10, 0, 45));
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        Utils2DTest.checkNormalized(p1l, 12.068, 24.202, -3, 69.000);
    }
    
    @Test
    public void calculateBoardLocation_Top_NoAffine_WithWidth() throws Exception {
        Board board = new Board();
        board.setDimensions(new Location(LengthUnit.Millimeters, 100, 100, 0, 0));
        BoardLocation boardLocation = new BoardLocation(board);
        boardLocation.setSide(Side.Top);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 7, 11, -3, 24));
        
        Placement p1 = new Placement("P1");
        p1.setLocation(new Location(LengthUnit.Millimeters, 10, 10, 0, 45));
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        Utils2DTest.checkNormalized(p1l, 12.068, 24.202, -3, 69.000);
    }
    
    @Test
    public void calculateBoardLocation_Top_WithAffine_WithWidth() throws Exception {
        Board board = new Board();
        board.setDimensions(new Location(LengthUnit.Millimeters, 100, 100, 0, 0));
        BoardLocation boardLocation = new BoardLocation(board);
        boardLocation.setSide(Side.Top);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 7, 11, -3, 24));
        /**
         * The transform was created by performing a fid check, verifying the results manually
         * and then printing the resulting transform:
         * AffineTransform[[0.913545457642601, -0.4067366430758, 7.000000000000001], [0.4067366430758, 0.913545457642601, 11.000000000000002]]
         */
        AffineTransform tx = new AffineTransform(
                0.913545457642601, 
                0.4067366430758, 
                -0.4067366430758, 
                0.913545457642601, 
                7.000000000000001, 
                11.000000000000002
                );
        boardLocation.setPlacementTransform(tx);
        
        Placement p1 = new Placement("P1");
        p1.setLocation(new Location(LengthUnit.Millimeters, 10, 10, 0, 45));
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        Utils2DTest.checkNormalized(p1l, 12.068, 24.202, -3, 69.000);
    }
    
    @Test
    public void calculateBoardLocation_Bottom_NoAffine_NoWidth() throws Exception {
        Board board = new Board();
        BoardLocation boardLocation = new BoardLocation(board);
        boardLocation.setSide(Side.Bottom);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 7, 11, -3, 24));
        
        Placement p1 = new Placement("P1");
        p1.setLocation(new Location(LengthUnit.Millimeters, 10, 10, 0, 45));
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        System.out.println(p1l);
        Utils2DTest.checkNormalized(p1l, -6.202, 16.068, -3, 69);
    }
    
    @Test
    public void calculateBoardLocation_Bottom_WithAffine_NoWidth() throws Exception {
        Board board = new Board();
        BoardLocation boardLocation = new BoardLocation(board);
        boardLocation.setSide(Side.Bottom);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 7, 11, -3, 24));
        
        /**
         * The transform was created by performing a fid check, verifying the results manually
         * and then printing the resulting transform:
         * AffineTransform[[-0.913545457642601, -0.4067366430758, 7.0], [-0.4067366430758, 0.913545457642601, 11.000000000000002]]
         */
        AffineTransform tx = new AffineTransform(
                0.913545457642601, 
                0.4067366430758, 
                -0.4067366430758, 
                0.913545457642601, 
                7.000000000000001, 
                11.000000000000002
                );
        boardLocation.setPlacementTransform(tx);
        
        Placement p1 = new Placement("P1");
        p1.setLocation(new Location(LengthUnit.Millimeters, 10, 10, 0, 45));
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        System.out.println(p1l);
        Utils2DTest.checkNormalized(p1l, -6.202, 16.068, -3, 69);
    }
}
