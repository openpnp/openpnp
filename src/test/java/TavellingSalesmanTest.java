/*
 * Copyright (C) 2020 <mark@makr.zone>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Length;
import org.openpnp.util.TravellingSalesman;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.solutions.HeadSolutions;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.CoordinateAxis;
import org.pmw.tinylog.Logger;

import com.google.common.io.Files;

public class TavellingSalesmanTest {
    /**
     * Test org.openpnp.util.TravellingSalesman<T> with random Locations like on a typical PNP machine. 
     * 
     * It generates Locations roughly arranged in X- and Y-aligned rows, like feeders would be. Plus some additional 
     * random Locations strewn in all over. Minimal Z scattering too.
     *  
     * The test generates an SVG rendering of the solution, saved as a temporary file.
     * 
     * As the actual Unit Test it checks the solution travel distance against a target. 
     * 
     * @throws Exception
     */
    // Slow acceleration
    @Test
    public void testTravellingSalesmanA() throws Exception {
        test("A",new Length(500, LengthUnit.Millimeters), new Length(500, LengthUnit.Millimeters), new double [] { 2870, 5230, 11360 });
    }
    // Fast acceleration on X
    @Test
    public void testTravellingSalesmanB() throws Exception {
        test("B",new Length(3000, LengthUnit.Millimeters), new Length(500, LengthUnit.Millimeters), new double [] { 2940, 5550, 15300 });
    }
    // Fast acceleration on X and Y
    @Test
    public void testTravellingSalesmanC() throws Exception {
        test("C",new Length(3000, LengthUnit.Millimeters), new Length(3000, LengthUnit.Millimeters), new double [] { 2870, 5000, 11500 });
    }

    public void test(String name,Length xacceleration, Length yacceleration, double targets[]) throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);

        // Copy the required configuration files over to the new configuration
        // directory.
        FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/BasicJobTest/machine.xml"),
                new File(workingDirectory, "machine.xml"));
        FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/BasicJobTest/packages.xml"),
                new File(workingDirectory, "packages.xml"));
        FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/BasicJobTest/parts.xml"),
                new File(workingDirectory, "parts.xml"));

        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        // configure the machine with the right acceleration for this test
        Machine machine = Configuration.get().getMachine();
        HeadMountable hm = machine.getDefaultHead().getDefaultCamera();
        CoordinateAxis rawAxisX = HeadSolutions.getRawAxis(machine, hm.getAxisX());
        CoordinateAxis rawAxisY = HeadSolutions.getRawAxis(machine, hm.getAxisY());
        ReferenceControllerAxis referenceControllerAxisX = (ReferenceControllerAxis)rawAxisX;
        ReferenceControllerAxis referenceControllerAxisY = (ReferenceControllerAxis)rawAxisY;
        referenceControllerAxisX.setAccelerationPerSecond2(xacceleration);
        referenceControllerAxisY.setAccelerationPerSecond2(yacceleration);

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
                    // across, loop, and no given end location respectively
                    (t == 1 ? new Location(LengthUnit.Millimeters) : 
                        (t == 0 ? new Location(LengthUnit.Millimeters, 1000.0, 500.0, 0.0, 0.0) : 
                            null)));
            // now solve the bugger
            double leastCost = tsm.solve();
            //
            // The TSM has been working with the default TravelCost, which models acceleration etc,
            // and bestDistance is actually best **time**. But now to verify that route we remove
            // the TravelCost object, and calculate the actual linear **distance** covered by the
            // selected route.
            tsm.setTravelCost(null);
            double linearDistance = tsm.getTravellingDistance();
            //
            // for the unit test, roughly check expected solution distance   
            double target = targets[t];
            System.out.println("TavellingSalesmanTest.testTravellingSalesman() test "+name+t+" solved "+list.size()+" locations, cost: "+leastCost+"sec, distance: "+linearDistance+" mm, target: "+target+"mm, time: "+tsm.getSolverDuration()+"ms");
            // save the solution, so we can have a look
            File file = File.createTempFile("travelling-salesman", ".svg");
            try (PrintWriter out = new PrintWriter(file.getAbsolutePath())) {
                out.println(tsm.asSvg());
                System.out.println(file.toURI());
            } 
            // unit test target
            // The solver was driven by total cost, which only has little relation to linear distance.
            // Therefore a tight limit to upper and lower bound is needed to detect any deviations.
            // This especially triggers if the eg. the seeding of the simulatedAnealing processes is
            // changed.
            if (linearDistance > target*1.01 || linearDistance < target*0.99) {
                throw new Exception("org.openpnp.util.TravellingSalesman.solve("+list.size()+") bestDistance "+linearDistance+" is quite different to " + target);
            }
        }
    }
}
