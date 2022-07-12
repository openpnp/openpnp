/*
 * Copyright (C) 2022 <mark@makr.zone>
 * inspired and based on work
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.model.Configuration;

import com.google.common.io.Files;


public class VisionCompositingTest {

    @BeforeEach
    public void before() throws Exception {
        /**
         * Create a new config directory and load the default configuration.
         */
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);
        Configuration.initialize(workingDirectory);
        Configuration.get().load();

    }


    @Test
    public void testPackageSolutions() throws Exception {
        /*
        SimulatedUpCamera camera = (SimulatedUpCamera)VisionUtils.getBottomVisionCamera();
        NozzleTip nt = Configuration.get().getMachine().getNozzleTips().get(0); 
        org.openpnp.model.Package pkg;
        Footprint footprint;
        Pad pad;
        pkg = new org.openpnp.model.Package("PASSIVE");
        pkg.addCompatibleNozzleTip(nt);
        footprint = pkg.getFootprint();
        footprint.setOuterDimension(3);
        footprint.setInnerDimension(2);
        footprint.setPadAcross(2);
        footprint.setPadCount(2);
        footprint.generate(Generator.Dual);
        pkg.getVisionCompositing().computeCompositeShots(pkg, camera);
        assertEquals(pkg.getVisionCompositing().getMinCorners(), 3);
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().size(), 4);
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().get(0).getRating(), 6); // non-square box and X configuration.

        pkg = new org.openpnp.model.Package("DUAL"); // SOIC
        pkg.addCompatibleNozzleTip(nt);
        footprint = pkg.getFootprint();
        footprint.setOuterDimension(6.2);
        footprint.setInnerDimension(3.8);
        footprint.setPadAcross(0.5);
        footprint.setPadPitch(1.27);
        footprint.setPadCount(24);
        footprint.generate(Generator.Dual);
        pkg.getVisionCompositing().computeCompositeShots(pkg, camera);
        assertEquals(pkg.getVisionCompositing().getMinCorners(), 3);
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().size(), 4);
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().get(0).getRating(), 6); // non-square box and X configuration.

        pkg = new org.openpnp.model.Package("QUAD");
        pkg.addCompatibleNozzleTip(nt);
        footprint = pkg.getFootprint();
        footprint.setOuterDimension(4);
        footprint.setInnerDimension(2);
        footprint.setPadAcross(0.2);
        footprint.setPadPitch(0.5);
        footprint.setPadCount(16);
        footprint.generate(Generator.Quad);
        pkg.getVisionCompositing().computeCompositeShots(pkg, camera);
        assertEquals(pkg.getVisionCompositing().getMinCorners(), 2);
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().size(), 4);
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().get(0).getRating(), 9); // full square

        pkg = new org.openpnp.model.Package("BGA");
        pkg.addCompatibleNozzleTip(nt);
        footprint = pkg.getFootprint();
        footprint.setPadAcross(0.2);
        footprint.setPadPitch(0.5);
        footprint.setPadCount(64);
        footprint.setPadRoundness(100);
        footprint.generate(Generator.Bga);
        pkg.getVisionCompositing().computeCompositeShots(pkg, camera);
        assertEquals(pkg.getVisionCompositing().getMinCorners(), 2);
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().size(), 4);
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().get(0).getRating(), 9); // full square

        pkg = new org.openpnp.model.Package("HEADPHONE");
        pkg.addCompatibleNozzleTip(nt);
        footprint = pkg.getFootprint();
        pad = new Pad();            //  [1]
        pad.setX(-3);               //       [4]
        pad.setY(6);                //
        pad.setWidth(2);            //
        pad.setHeight(2);           //  [2]
        pad.setName("1");           //       [3]
        footprint.addPad(pad);      //
        pad = new Pad();            // Staggered pads.
        pad.setX(-3);
        pad.setY(-4);
        pad.setWidth(2);
        pad.setHeight(2);
        pad.setName("2");
        footprint.addPad(pad);
        pad = new Pad();
        pad.setX(3);
        pad.setY(-6);
        pad.setWidth(2);
        pad.setHeight(2);
        pad.setName("3");
        footprint.addPad(pad);
        pad = new Pad();
        pad.setX(3);
        pad.setY(4);
        pad.setWidth(2);
        pad.setHeight(2);
        pad.setName("4");
        footprint.addPad(pad);
        pkg.getVisionCompositing().computeCompositeShots(pkg, camera);
        assertEquals(pkg.getVisionCompositing().getMinCorners(), 3);
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().size(), 4);
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().get(0).getX(), -4); // upper left corner of 1st pad is lead
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().get(0).getY(), 7); 
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().get(0).getRating(), 5); // Z configuration

        pkg = new org.openpnp.model.Package("HEADPHONE2");
        pkg.addCompatibleNozzleTip(nt);
        footprint = pkg.getFootprint();
        pad = new Pad();            //       [4]
        pad.setX(-3);               //  [1]
        pad.setY(4);                //
        pad.setWidth(2);            //
        pad.setHeight(2);           //       [3]
        pad.setName("1");           //  [2]
        footprint.addPad(pad);      //
        pad = new Pad();            // Staggered pads.
        pad.setX(-3);
        pad.setY(-6);
        pad.setWidth(2);
        pad.setHeight(2);
        pad.setName("2");
        footprint.addPad(pad);
        pad = new Pad();
        pad.setX(3);
        pad.setY(-4);
        pad.setWidth(2);
        pad.setHeight(2);
        pad.setName("3");
        footprint.addPad(pad);
        pad = new Pad();
        pad.setX(3);
        pad.setY(6);
        pad.setWidth(2);
        pad.setHeight(2);
        pad.setName("4");
        footprint.addPad(pad);
        pkg.getVisionCompositing().computeCompositeShots(pkg, camera);
        assertEquals(pkg.getVisionCompositing().getMinCorners(), 3);
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().size(), 4);
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().get(0).getX(), -4); // lower left corner of 2nd pad is lead
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().get(0).getY(), -7); 
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().get(0).getRating(), 5); // Z configuration

        pkg = new org.openpnp.model.Package("PWR_FET");
        pkg.addCompatibleNozzleTip(nt);
        footprint = pkg.getFootprint();
        pad = new Pad();            //  [  1  ]
        pad.setX(-3);               //              [     ]  
        pad.setY(1);                //              [     ]
        pad.setWidth(4);            //  [  2  ]     [  4  ]
        pad.setHeight(0.5);         //              [     ]
        pad.setName("1");           //              [     ]
        footprint.addPad(pad);      //  [  3  ]
        pad = new Pad();            //
        pad.setX(-3);               // Power-FET, left-right asymmetrical.
        pad.setY(0);
        pad.setWidth(4);
        pad.setHeight(0.5);
        pad.setName("2");
        footprint.addPad(pad);
        pad = new Pad();
        pad.setX(-3);
        pad.setY(-1);
        pad.setWidth(4);
        pad.setHeight(0.5);
        pad.setName("3");
        footprint.addPad(pad);
        pad = new Pad();
        pad.setX(3);
        pad.setY(0);
        pad.setWidth(4);
        pad.setHeight(2);
        pad.setName("4");
        footprint.addPad(pad);
        pkg.getVisionCompositing().computeCompositeShots(pkg, camera);
        assertEquals(pkg.getVisionCompositing().getMinCorners(), 4);
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().size(), 4);
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().get(0).getX(), -5); // upper left corner of 1st pad is lead
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().get(0).getY(), 1.25); 
        assertEquals(pkg.getVisionCompositing().getCompositeCorners().get(0).getRating(), 2); // Trapezoid configuration
        */
    }
}
