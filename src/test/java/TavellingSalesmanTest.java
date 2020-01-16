import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.util.TravellingSalesman;

public class TavellingSalesmanTest {
    /**
     * Test org.openpnp.util.TravellingSalesman<T> with random Locations.
     * 
     * @throws Exception
     */
    @Test
    public void testTravellingSalesman() throws Exception {
        for (int t = 2, scale = 100; scale > 0; t--, scale /= 10) {
            // make this test repeatable, by seeding the random generator.
            Random rnd = new java.util.Random(42);
            List<Location> list = new ArrayList<Location>();
            // add some random Locations all over 
            for (int i = 0; i < 1*scale+10; i++) {
                list.add(new Location(LengthUnit.Millimeters, rnd.nextDouble()*1000.0, rnd.nextDouble()*500.0, rnd.nextDouble()*20.0, 0.0));
            }
            // add some X-aligned rows of feeders
            for (int i = 0; i < 3*scale; i++) {
                list.add(new Location(LengthUnit.Millimeters, Math.floor(rnd.nextDouble()*5.0)*250.0+rnd.nextDouble()*20.0, rnd.nextDouble()*500.0, rnd.nextDouble()*10.0, 0.0));
            }
            // add some Y-aligned rows of feeders
            for (int i = 0; i < 2*scale; i++) {
                list.add(new Location(LengthUnit.Millimeters, rnd.nextDouble()*1000.0, Math.floor(rnd.nextDouble()*2.0)*500.0+rnd.nextDouble()*20.0, rnd.nextDouble()*10.0, 0.0));
            }
            // create the solver
            TravellingSalesman<Location> tsm = new TravellingSalesman<>(
                    list, 
                    new TravellingSalesman.Locator<Location>() { 
                        @Override
                        public Location getLocation(Location locatable) {
                            return locatable;
                        }
                    }, 
                    // start from origin 
                    new Location(LengthUnit.Millimeters), 
                    // in the middle test go back, otherwise no given end location
                    t == 1 ? new Location(LengthUnit.Millimeters) : null);
            // now solve the bugger
            double bestDistance = tsm.solve(false);
            // for the unit test, roughly check expected solution distance   
            double target = new double [] { 2750.0, 5100.0, 12000.0 } [t];
            System.out.println("TavellingSalesmanTest.testTravellingSalesman() solved "+list.size()+" locations, distance: "+Math.round(bestDistance)+"mm, target: "+target+"mm, time: "+tsm.getSolverDuration()+"ms");
            // save the solution, so we can have a look
            File file = File.createTempFile("travelling-salesman", ".svg");
            try (PrintWriter out = new PrintWriter(file.getAbsolutePath())) {
                out.println(tsm.asSvg());
                System.out.println(file.toURI());
            } 
            // unit test target
            if (bestDistance > target) {
                throw new Exception("org.openpnp.util.TravellingSalesman.solve("+list.size()+") bestDistance "+bestDistance+" is greater than " + target);
            }
        }
    }
}
