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

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.camera.SimulatedUpCamera;
import org.openpnp.machine.reference.driver.NullDriver;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PartAlignment.PartAlignmentOffset;
import org.openpnp.util.VisionUtils;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;

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
        FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/VisionCompositingTest/packages.xml"),
                new File(workingDirectory, "packages.xml"));
        FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/VisionCompositingTest/parts.xml"),
                new File(workingDirectory, "parts.xml"));
        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        Configurator
        .currentConfig()
        .level(Level.DEBUG) // change this for other log levels.
        .activate();

    }


    @Test
    public void testPackageSolutions() throws Exception {
        Machine machine = Configuration.get().getMachine();
        Nozzle nozzle = machine.getDefaultHead().getDefaultNozzle();
        SimulatedUpCamera camera = (SimulatedUpCamera) VisionUtils.getBottomVisionCamera();
        camera.setRoamingRadius(new Length(30, LengthUnit.Millimeters));
        ReferenceBottomVision bottomVision = ReferenceBottomVision.getDefault();
        NullDriver driver = (NullDriver) ((ReferenceMachine) machine).getDefaultDriver();
        driver.setFeedRateMmPerMinute(0);

        // Set nozzle tip pick tolerances for large offsets.
        for (NozzleTip tip : Configuration.get().getMachine().getNozzleTips()) {
            ((ReferenceNozzleTip) tip).setMaxPickTolerance(new Length(1, LengthUnit.Millimeters));
        }
        // Some of these irregular parts simply need pre-rotate vision i.e. multiple passes.
        bottomVision.setPreRotate(true);
        bottomVision.setMaxAngularOffset(0.1);
        bottomVision.setMaxLinearOffset(new Length(0.1, LengthUnit.Millimeters));

        machine.setEnabled(true);
        machine.home();
        machine.execute(() -> {
            for (Part part: Configuration.get().getParts()) {
                if (!part.getId().startsWith("FID")) {
                    Location error;
                    Location maxError;
                    if (part.getId().startsWith("SMALL")) {
                        error = new Location(LengthUnit.Millimeters, 0.05, 0.1, 0, 7);
                        // For small parts, expect small linear but large angular errors.
                        maxError = new Location(LengthUnit.Millimeters, 0.025, 0.025, 0, 1.5);
                    }
                    else {
                        error = new Location(LengthUnit.Millimeters, 0.25, 0.75, 0, -2);
                        maxError = new Location(LengthUnit.Millimeters, 0.05, 0.05, 0, 0.07);
                    }
                    camera.setErrorOffsets(error);
                    nozzle.pick(part);
                    Placement placement = new Placement("Dummy");
                    placement.setLocation(Location.origin);
                    PartAlignmentOffset offset = bottomVision.findOffsets(part, null, placement, nozzle);
                    Location offsets = offset.getLocation();
                    assertMaxDelta(offsets.getX(), error.getX(), maxError.getX());
                    assertMaxDelta(offsets.getY(), error.getY(), maxError.getY());
                    assertMaxDelta(offsets.getRotation(), error.getRotation(), maxError.getRotation());
                }
            }
            return true;
        });
    }

    public static void assertMaxDelta(double a, double b, double maxDelta) throws Exception {
        if (Math.abs(a - b) > maxDelta) {
            throw new Exception(String.format("abs(%f - %f) > %f", a, b, maxDelta));
        }
    }
}
