package org.openpnp.gui.processes;

import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.util.Utils2D;

public class TestTwoPlacement {
    public static Location calculateLocation(Placement placementA, Placement placementB,
            Location actualLocationA, Location actualLocationB) {
        Location idealA = placementA.getLocation();
        Location idealB = placementB.getLocation().convertToUnits(idealA.getUnits());
        Location actualA = actualLocationA.convertToUnits(idealA.getUnits());
        Location actualB = actualLocationB.convertToUnits(idealA.getUnits());

        // Calculate the angle that we expect to see between the two placements
        double idealAngle = Math.toDegrees(
                Math.atan2(idealB.getY() - idealA.getY(), idealB.getX() - idealA.getX()));
        // Now calculate the angle that we observed between the two placements
        double actualAngle = Math.toDegrees(
                Math.atan2(actualB.getY() - actualA.getY(), actualB.getX() - actualA.getX()));

        // The difference in angles is the angle of the board
        double angle = actualAngle - idealAngle;

        // Now we rotate the first placement by the angle, which gives us the location
        // that the placement would be had the board been rotated by that angle.
        Location idealARotated = idealA.rotateXy(angle);

        // And now we subtract that rotated location from the observed location to get
        // the real offset of the board.
        Location location = actualA.subtract(idealARotated);
        
        // And set the calculated angle
        location = location.derive(null, null, null, angle);
        
        return location;
    }
    
    public static void test(Location l) throws Exception {
        // The actual board location that we are looking for. This is where the board actually
        // is on the table.
        BoardLocation actualBoardLocation = new BoardLocation(new Board());
        // TODO note angle 152 gives -208 instead of 152
        // Looks like just normalizing this angle will solve the problem
        actualBoardLocation.setLocation(l);
        actualBoardLocation.setSide(Side.Top);

        // Two sample placements
        Placement placementA = new Placement("A");
        placementA.setLocation(new Location(LengthUnit.Millimeters, 10, 10, 0, 0));

        Placement placementB = new Placement("B");
        placementB.setLocation(new Location(LengthUnit.Millimeters, 40, 40, 0, 0));

        // Given the actual board location and the two placements, calculate where they should
        // be after transforming. This calculation is known to be good.
        Location actualLocationA = Utils2D
                .calculateBoardPlacementLocation(actualBoardLocation, placementA.getLocation())
                .derive(null, null, null, 0d);
        Location actualLocationB = Utils2D
                .calculateBoardPlacementLocation(actualBoardLocation, placementB.getLocation())
                .derive(null, null, null, 0d);

        // Using the input board location, which should not actually affect the result,
        // calculate the board location from the known actual locations. The result
        // should be the actual board location.
        Location location =
                calculateLocation(placementA, placementB, actualLocationA, actualLocationB);

        // Print pass or fail based on whether the calculated location was more than 0.1mm
        // away from the actual location or the resulting angle is more than 0.1 degrees off.
        if (location.getLinearDistanceTo(actualBoardLocation.getLocation()) > 0.1 || Math.abs(
                Utils2D.normalizeAngle(location.getRotation()) - Utils2D.normalizeAngle(actualBoardLocation.getLocation().getRotation())) > .1) {
            throw new Exception("Expected " + l + " got " + location);
        }
    }

    public static void main(String[] args) throws Exception {
        // new Location(LengthUnit.Millimeters, 10, 10, 0, 47.66)
        for (int i = 0; i < 1000000; i++) {
            test(new Location(LengthUnit.Millimeters, Math.random() * 100, Math.random() * 100, 0, Math.random() * 720 - 360));
        }
    }
}
