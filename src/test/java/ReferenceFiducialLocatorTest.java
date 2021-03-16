import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openpnp.machine.reference.vision.ReferenceFiducialLocator;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;

public class ReferenceFiducialLocatorTest {
    static Placement createPlacement(double x, double y) {
        Location l = new Location(LengthUnit.Millimeters, x, y, 0, 0);
        Placement p = new Placement(Math.random() + "");
        p.setLocation(l);
        return p;
    }
    
    @Test
    public void testJust1() {
        List<Placement> points = new ArrayList<>();
        points.add(createPlacement(0, 0));
        long t = System.currentTimeMillis();
        List<Placement> results = ReferenceFiducialLocator.getBestFiducials(points);
        assertEquals(results.size(), 1);
        System.out.println("testJust1 " + results + " in " + (System.currentTimeMillis() - t));
    }
    
    @Test
    public void testJust2() {
        List<Placement> points = new ArrayList<>();
        points.add(createPlacement(0, 0));
        points.add(createPlacement(0, 50));
        long t = System.currentTimeMillis();
        List<Placement> results = ReferenceFiducialLocator.getBestFiducials(points);
        assertEquals(results.size(), 2);
        System.out.println("testJust2 " + results + " in " + (System.currentTimeMillis() - t));
    }
    
    @Test
    public void testNominal() {
        List<Placement> points = new ArrayList<>();
        points.add(createPlacement(0, 0));
        points.add(createPlacement(0, 50));
        points.add(createPlacement(10, 40));
        points.add(createPlacement(80, 10));
        points.add(createPlacement(100, 50));
        points.add(createPlacement(100, 0));
        points.add(createPlacement(110, 40));
        for (int i = 0; i < 100; i++) {
            points.add(createPlacement(Math.random() * 109, Math.random() * 49));
        }
        long t = System.currentTimeMillis();
        List<Placement> results = ReferenceFiducialLocator.getBestFiducials(points);
        assertEquals(results.size(), 3);
        System.out.println("testNominal " + results + " in " + (System.currentTimeMillis() - t));
    }
    
    @Test
    public void testCollinear() {
        List<Placement> points = new ArrayList<>();
        points.add(createPlacement(0, 0));
        points.add(createPlacement(100, 100));
        points.add(createPlacement(200, 200));
        long t = System.currentTimeMillis();
        List<Placement> results = ReferenceFiducialLocator.getBestFiducials(points);
        assertEquals(results.size(), 2);
        System.out.println("testCollinear " + results + " in " + (System.currentTimeMillis() - t));
    }
    
    @Test
    public void testSameX() {
        List<Placement> points = new ArrayList<>();
        points.add(createPlacement(10, 0));
        points.add(createPlacement(10, 100));
        points.add(createPlacement(10, 200));
        long t = System.currentTimeMillis();
        List<Placement> results = ReferenceFiducialLocator.getBestFiducials(points);
        assertEquals(results.size(), 2);
        System.out.println("testSameX " + results + " in " + (System.currentTimeMillis() - t));
    }
    
    @Test
    public void testSameY() {
        List<Placement> points = new ArrayList<>();
        points.add(createPlacement(0, 10));
        points.add(createPlacement(100, 10));
        points.add(createPlacement(200, 10));
        long t = System.currentTimeMillis();
        List<Placement> results = ReferenceFiducialLocator.getBestFiducials(points);
        assertEquals(results.size(), 2);
        System.out.println("testSameY " + results + " in " + (System.currentTimeMillis() - t));
    }
}
