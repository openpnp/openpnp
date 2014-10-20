import org.junit.Test;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
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
    
    public static void within(String name, double value, double target, double plusMinus) throws Exception {
        if (value > target + plusMinus) {
            throw new Exception(name + " " + value + " is greater than " + (target + plusMinus));
        }
        else if (value < target - plusMinus) {
            throw new Exception(name + " " + value + " is less than " + (target - plusMinus));
        }
    }
}
