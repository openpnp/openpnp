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
import org.openpnp.util.TravellingSalesman;

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
    @Test
    public void testTravellingSalesman() throws Exception {
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
            double bestDistance = tsm.solve();
            // for the unit test, roughly check expected solution distance   
            double target = new double [] { 20.0, 50.0, 260.0 } [t];
            System.out.println("TavellingSalesmanTest.testTravellingSalesman() solved "+list.size()+" locations, cost: "+Math.round(bestDistance)+"sec, target: "+target+"sec, time: "+tsm.getSolverDuration()+"ms");
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
