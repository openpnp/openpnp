import java.awt.geom.AffineTransform;

import org.junit.jupiter.api.Test;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.util.Utils2D;

public class CalculateBoardLocationTests {
    @Test
    public void calculateBoardLocationTopNoAffineNoWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Top, false);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Utils2DTest.checkNormalized(p1l, 23.160, 130.902, -10.00, 119.628);
    }
    
    @Test
    public void calculateBoardLocationTopWithAffineNoWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Top, false);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);

        AffineTransform tx = simulateFiducialCheck(boardLocation);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setPlacementTransform(tx);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Utils2DTest.checkNormalized(p1l, 23.160, 130.902, -10.00, 119.628);
    }
    
    @Test
    public void calculateBoardLocationTopNoAffineWithWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Top, true);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);

        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Utils2DTest.checkNormalized(p1l, 23.160, 130.902, -10.00, 119.628);
    }
    
    @Test
    public void calculateBoardLocationTopWithAffineWithWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Top, true);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);

        AffineTransform tx = simulateFiducialCheck(boardLocation);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setPlacementTransform(tx);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Utils2DTest.checkNormalized(p1l, 23.160, 130.902, -10.00, 119.628);
    }
    
    @Test
    public void calculateBoardLocationBottomNoAffineNoWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Bottom, false);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);

        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Utils2DTest.checkNormalized(p1l, 91.015, 79.253, -10.00, 81.585);
    }
    
    @Test
    public void calculateBoardLocationBottomWithAffineNoWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Bottom, false);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);

        AffineTransform tx = simulateFiducialCheck(boardLocation);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setPlacementTransform(tx);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Utils2DTest.checkNormalized(p1l, 91.015, 79.253, -10.00, 81.585);
    }
    
    @Test
    public void calculateBoardLocationBottomNoAffineWithWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Bottom, true);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Utils2DTest.checkNormalized(p1l, 91.025, 79.246, -10.00, 81.662);
    }
    
    @Test
    public void calculateBoardLocationBottomWithAffineWithWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Bottom, true);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);

        AffineTransform tx = simulateFiducialCheck(boardLocation);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setPlacementTransform(tx);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Utils2DTest.checkNormalized(p1l, 91.025, 79.246, -10.00, 81.662);
    }
    
    @Test
    public void calculateBoardLocationInverseTopNoAffineNoWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Top, false);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Location p1li = Utils2D.calculateBoardPlacementLocationInverse(boardLocation, p1l);
        
        Utils2DTest.checkNormalized(p1.getLocation(), p1li);
    }
    
    @Test
    public void calculateBoardLocationInverseTopWithAffineNoWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Top, false);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        AffineTransform tx = simulateFiducialCheck(boardLocation);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setPlacementTransform(tx);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Location p1li = Utils2D.calculateBoardPlacementLocationInverse(boardLocation, p1l);
        
        Utils2DTest.checkNormalized(p1.getLocation(), p1li);
    }
    
    @Test
    public void calculateBoardLocationInverseTopNoAffineWithWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Top, true);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Location p1li = Utils2D.calculateBoardPlacementLocationInverse(boardLocation, p1l);
        
        Utils2DTest.checkNormalized(p1.getLocation(), p1li);
    }
    
    @Test
    public void calculateBoardLocationInverseTopWithAffineWithWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Top, true);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        AffineTransform tx = simulateFiducialCheck(boardLocation);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setPlacementTransform(tx);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Location p1li = Utils2D.calculateBoardPlacementLocationInverse(boardLocation, p1l);
        
        Utils2DTest.checkNormalized(p1.getLocation(), p1li);
    }
    
    @Test
    public void calculateBoardLocationInverseBottomNoAffineNoWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Bottom, false);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Location p1li = Utils2D.calculateBoardPlacementLocationInverse(boardLocation, p1l);
        
        Utils2DTest.checkNormalized(p1.getLocation(), p1li);
    }
    
    @Test
    public void calculateBoardLocationInverseBottomWithAffineNoWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Bottom, false);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        AffineTransform tx = simulateFiducialCheck(boardLocation);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setPlacementTransform(tx);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Location p1li = Utils2D.calculateBoardPlacementLocationInverse(boardLocation, p1l);
        
        Utils2DTest.checkNormalized(p1.getLocation(), p1li);
    }
    
    @Test
    public void calculateBoardLocationInverseBottomNoAffineWithWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Bottom, true);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Location p1li = Utils2D.calculateBoardPlacementLocationInverse(boardLocation, p1l);
        
        Utils2DTest.checkNormalized(p1.getLocation(), p1li);
    }
    
    @Test
    public void calculateBoardLocationInverseBottomWithAffineWithWidth() throws Exception {
        BoardLocation boardLocation = createTestBoardLocation(Side.Bottom, true);
        Placement p1 = boardLocation.getBoard().getPlacements().get(0);
        
        AffineTransform tx = simulateFiducialCheck(boardLocation);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setPlacementTransform(tx);
        
        Location p1l = Utils2D.calculateBoardPlacementLocation(boardLocation, p1.getLocation());
        
        Location p1li = Utils2D.calculateBoardPlacementLocationInverse(boardLocation, p1l);
        
        Utils2DTest.checkNormalized(p1.getLocation(), p1li);
    }
    
    
    
    
    
    /**
     * Simulates a 3 point fiducial check by generating 3 placements at fixed locations,
     * calculating their board placement location and running deriveAffineTransform.
     * 
     * This is to be used when it is known that calculateBoardLocation is working correctly
     * for the given BoardLocation without a placement transform set.
     * 
     * @param boardLocation
     * @return
     */
    private AffineTransform simulateFiducialCheck(BoardLocation boardLocation) {
        Placement fid1 = new Placement("FID1");
        fid1.setLocation(new Location(LengthUnit.Millimeters, 1, 1, 0, 0));
        
        Placement fid2 = new Placement("FID2");
        fid2.setLocation(new Location(LengthUnit.Millimeters, 2, 1, 0, 0));
        
        Placement fid3 = new Placement("FID3");
        fid3.setLocation(new Location(LengthUnit.Millimeters, 2, 2, 0, 0));
        
        boardLocation.setPlacementTransform(null);
        Location fid1l = Utils2D.calculateBoardPlacementLocation(boardLocation, fid1.getLocation());
        Location fid2l = Utils2D.calculateBoardPlacementLocation(boardLocation, fid2.getLocation());
        Location fid3l = Utils2D.calculateBoardPlacementLocation(boardLocation, fid3.getLocation());
        
        if (boardLocation.getGlobalSide() == Side.Bottom) {
            fid1.setLocation(fid1.getLocation().multiply(-1, 1, 1, 1));
            fid2.setLocation(fid2.getLocation().multiply(-1, 1, 1, 1));
            fid3.setLocation(fid3.getLocation().multiply(-1, 1, 1, 1));
        }
        
        AffineTransform tx = Utils2D.deriveAffineTransform(fid1.getLocation(), fid2.getLocation(), fid3.getLocation(), 
                fid1l, fid2l, fid3l);
        boardLocation.setPlacementTransform(tx);
        
        return tx;
    }
    
    /**
     * TODO STOPSHIP These all now closely match the pnp-test setup with simulation, but it's
     * not all lining up perfectly. I'm not sure why the bottom has to be different for with and
     * without width. I suspect that maybe the length is not exactly 37?
     * 
     * I think I can do the fiducial check and then create ORIG to find 0,0 and then jog to the
     * other corner and measure the distance to get the exact length?
     * 
     * Looks like it might be 37.06
     * 
     * The board lcoations have to be different from each other because of with and without width
     * but we should be able to take the distance between the two cand come up with the board
     * width, and then the results should all be the same in the tests.
     * 
     * In other words, sqrt((84.107−113.763)^2+(46.671−68.740)^2) should be 37.06 but it's 
     * not. Why?
     */
    static BoardLocation createTestBoardLocation(Side side, boolean includeBoardWidth) {
        Board board = new Board();
        if (includeBoardWidth) {
            board.setDimensions(new Location(LengthUnit.Millimeters, 37.0, 0, 0, 0));
        }
        
        BoardLocation boardLocation = new BoardLocation(board);
        boardLocation.setGlobalSide(side);
        if (side == Side.Top) {
            Placement r6 = new Placement("R6");
            r6.setLocation(new Location(LengthUnit.Millimeters, 25, 22, 0, 45));
            board.addPlacement(r6);
            
            boardLocation.setLocation(new Location(LengthUnit.Millimeters, 37.746, 100.964, -10, 74.628));
        }
        else {
            Placement r17 = new Placement("R17");
            r17.setLocation(new Location(LengthUnit.Millimeters, 12, 22, 0, 45));
            board.addPlacement(r17);

            if (includeBoardWidth) {
                boardLocation.setLocation(new Location(LengthUnit.Millimeters, 84.107, 46.671, -10, 36.662));
            }
            else {
                boardLocation.setLocation(new Location(LengthUnit.Millimeters, 113.763, 68.740, -10, 36.585));
            }
        }
        
        return boardLocation;
    }
}
