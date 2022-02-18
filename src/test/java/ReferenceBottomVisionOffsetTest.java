import static org.openpnp.machine.reference.vision.ReferenceBottomVision.PreRotateUsage.AlwaysOff;
import static org.openpnp.machine.reference.vision.ReferenceBottomVision.PreRotateUsage.AlwaysOn;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.machine.reference.camera.SimulatedUpCamera;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PartAlignment.PartAlignmentOffset;
import org.openpnp.util.VisionUtils;

import com.google.common.io.Files;

public class ReferenceBottomVisionOffsetTest {
    static File workingDirectory;

    @BeforeAll
    public static void setup() throws Exception {
        /**
         * Create a new config directory and load the default configuration.
         */
        workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);

        // Copy the required configuration files over to the new configuration
        // directory.
        FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/ReferenceBottomVisionOffset/packages.xml"),
                new File(workingDirectory, "packages.xml"));
        FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/ReferenceBottomVisionOffset/parts.xml"),
                new File(workingDirectory, "parts.xml"));

        Configuration.initialize(workingDirectory);
        Configuration.get().load();
        // Save back migrated.
        Configuration.get().save();
    }

    
    @BeforeEach
    public void before() throws Exception {
        Configuration.initialize(workingDirectory);
        Configuration.get().load();
    }

    
    @Test
    public void testSymetricPartNoOffsetNoPreRotation() throws Exception {

        Machine machine = Configuration.get().getMachine();
        ReferenceBottomVision bottomVision = ReferenceBottomVision.getDefault();
        Nozzle nozzle = machine.getDefaultHead().getDefaultNozzle();
       
        Part part = Configuration.get().getPart("R0805-1K");
        BottomVisionSettings bottomVisionSettings = bottomVision.getInheritedVisionSettings(part);
        
        BoardLocation boardLocation = new BoardLocation(new Board());
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setSide(Side.Top);

        // no offset
        bottomVisionSettings.setVisionOffset(new Location(LengthUnit.Millimeters));
        
        // No pre rotate
        bottomVisionSettings.setPreRotateUsage(AlwaysOff);
        
        // test data
        Location[][] testData = {
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 000.0), new Location(LengthUnit.Millimeters, 0.0, 0.0, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 045.0), new Location(LengthUnit.Millimeters, 0.0, 0.0, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 090.0), new Location(LengthUnit.Millimeters, 0.0, 0.0, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 180.0), new Location(LengthUnit.Millimeters, 0.0, 0.0, 0.0, 0.0)}
        };
        
        machine.setEnabled(true);
        Location maxError = new Location(LengthUnit.Millimeters, 0.1, 0.1, 0, 0.07);
        for (Location[] testPair: testData) {
            machine.execute(() -> {
                nozzle.pick(part);
                Placement placement = new Placement("Dummy");
                placement.setLocation(testPair[0]);
                PartAlignmentOffset offset = bottomVision.findOffsets(part, boardLocation, placement, nozzle);
                Location offsets = offset.getLocation();
                assertMaxDelta(offsets.getX(), testPair[1].getX(), maxError.getX());
                assertMaxDelta(offsets.getY(), testPair[1].getY(), maxError.getY());
                assertMaxDelta(offsets.getRotation(), testPair[1].getRotation(), maxError.getRotation());
                return true;
            });
        }
    }

    @Test
    public void testSymetricPartNoOffsetWithPreRotation() throws Exception {

        Machine machine = Configuration.get().getMachine();
        ReferenceBottomVision bottomVision = ReferenceBottomVision.getDefault();
        Nozzle nozzle = machine.getDefaultHead().getDefaultNozzle();
       
        Part part = Configuration.get().getPart("R0805-1K");
        BottomVisionSettings bottomVisionSettings = bottomVision.getInheritedVisionSettings(part);
        
        BoardLocation boardLocation = new BoardLocation(new Board());
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setSide(Side.Top);

        // no offset
        bottomVisionSettings.setVisionOffset(new Location(LengthUnit.Millimeters));
        
        // No pre rotate
        bottomVisionSettings.setPreRotateUsage(AlwaysOn);
        
        // test data
        Location[][] testData = {
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 000.0), new Location(LengthUnit.Millimeters, 0.0, 0.0, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 045.0), new Location(LengthUnit.Millimeters, 0.0, 0.0, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 090.0), new Location(LengthUnit.Millimeters, 0.0, 0.0, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 180.0), new Location(LengthUnit.Millimeters, 0.0, 0.0, 0.0, 0.0)}
        };
        
        machine.setEnabled(true);
        Location maxError = new Location(LengthUnit.Millimeters, 0.1, 0.1, 0, 0.07);
        for (Location[] testPair: testData) {
            machine.execute(() -> {
                nozzle.pick(part);
                Placement placement = new Placement("Dummy");
                placement.setLocation(testPair[0]);
                PartAlignmentOffset offset = bottomVision.findOffsets(part, boardLocation, placement, nozzle);
                Location offsets = offset.getLocation();
                assertMaxDelta(offsets.getX(), testPair[1].getX(), maxError.getX());
                assertMaxDelta(offsets.getY(), testPair[1].getY(), maxError.getY());
                assertMaxDelta(offsets.getRotation(), testPair[1].getRotation(), maxError.getRotation());
                return true;
            });
        }
    }
    
    
    @Test
    public void testSymetricPartWithOffsetWithPreRotation() throws Exception {

        Machine machine = Configuration.get().getMachine();
        ReferenceBottomVision bottomVision = ReferenceBottomVision.getDefault();
        Nozzle nozzle = machine.getDefaultHead().getDefaultNozzle();
       
        Part part = Configuration.get().getPart("R0805-1K");
        BottomVisionSettings bottomVisionSettings = bottomVision.getInheritedVisionSettings(part);
        
        BoardLocation boardLocation = new BoardLocation(new Board());
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setSide(Side.Top);

        // 1mm offset in both directions
        bottomVisionSettings.setVisionOffset(new Location(LengthUnit.Millimeters, 1.0, 1.0, 0.0, 0.0));
        
        // No pre rotate
        bottomVisionSettings.setPreRotateUsage(AlwaysOn);
        
        // test data
        Location[][] testData = {
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 000.0), new Location(LengthUnit.Millimeters, -1.0, -1.0, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 045.0), new Location(LengthUnit.Millimeters, 0.0, -Math.sqrt(2.0), 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 090.0), new Location(LengthUnit.Millimeters, 1.0, -1.0, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 180.0), new Location(LengthUnit.Millimeters, 1.0, 1.0, 0.0, 0.0)}
        };
        
        machine.setEnabled(true);
        Location maxError = new Location(LengthUnit.Millimeters, 0.1, 0.1, 0, 0.07);
        for (Location[] testPair: testData) {
            machine.execute(() -> {
                nozzle.pick(part);
                Placement placement = new Placement("Dummy");
                placement.setLocation(testPair[0]);
                PartAlignmentOffset offset = bottomVision.findOffsets(part, boardLocation, placement, nozzle);
                Location offsets = offset.getLocation();
                assertMaxDelta(offsets.getX(), testPair[1].getX(), maxError.getX());
                assertMaxDelta(offsets.getY(), testPair[1].getY(), maxError.getY());
                assertMaxDelta(offsets.getRotation(), testPair[1].getRotation(), maxError.getRotation());
                return true;
            });
        }
    }

   
    @Test
    public void testAsymetricPartNoOffsetNoPreRotation() throws Exception {

        Machine machine = Configuration.get().getMachine();
        ReferenceBottomVision bottomVision = ReferenceBottomVision.getDefault();
        Nozzle nozzle = machine.getDefaultHead().getDefaultNozzle();
       
        Part part = Configuration.get().getPart("DoubleAsymPart");
        BottomVisionSettings bottomVisionSettings = bottomVision.getInheritedVisionSettings(part);
        
        BoardLocation boardLocation = new BoardLocation(new Board());
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setSide(Side.Top);

        // no offset
        bottomVisionSettings.setVisionOffset(new Location(LengthUnit.Millimeters));
        
        // No pre rotate
        bottomVisionSettings.setPreRotateUsage(AlwaysOff);
        
        // test data
        Location[][] testData = {
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 000.0), new Location(LengthUnit.Millimeters, 0.5, -0.5, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 045.0), new Location(LengthUnit.Millimeters, 0.5, -0.5, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 090.0), new Location(LengthUnit.Millimeters, 0.5, -0.5, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 180.0), new Location(LengthUnit.Millimeters, 0.5, -0.5, 0.0, 0.0)},
        };
        
        machine.setEnabled(true);
        Location maxError = new Location(LengthUnit.Millimeters, 0.1, 0.1, 0, 0.07);
        for (Location[] testPair: testData) {
            machine.execute(() -> {
                nozzle.pick(part);
                Placement placement = new Placement("Dummy");
                placement.setLocation(testPair[0]);
                PartAlignmentOffset offset = bottomVision.findOffsets(part, boardLocation, placement, nozzle);
                Location offsets = offset.getLocation();
                assertMaxDelta(offsets.getX(), testPair[1].getX(), maxError.getX());
                assertMaxDelta(offsets.getY(), testPair[1].getY(), maxError.getY());
                assertMaxDelta(offsets.getRotation(), testPair[1].getRotation(), maxError.getRotation());
                return true;
            });
        }
    }

    @Test
    public void testAsymetricPartNoOffsetWithPreRotation() throws Exception {

        Machine machine = Configuration.get().getMachine();
        ReferenceBottomVision bottomVision = ReferenceBottomVision.getDefault();
        Nozzle nozzle = machine.getDefaultHead().getDefaultNozzle();
       
        Part part = Configuration.get().getPart("DoubleAsymPart");
        BottomVisionSettings bottomVisionSettings = bottomVision.getInheritedVisionSettings(part);
        
        BoardLocation boardLocation = new BoardLocation(new Board());
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setSide(Side.Top);

        // no offset
        bottomVisionSettings.setVisionOffset(new Location(LengthUnit.Millimeters));
        
        // With pre rotate
        bottomVisionSettings.setPreRotateUsage(AlwaysOn);
        
        // test data
        Location[][] testData = {
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 000.0), new Location(LengthUnit.Millimeters, 0.5, -0.5, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 045.0), new Location(LengthUnit.Millimeters, Math.sqrt(0.5), 0.0, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 090.0), new Location(LengthUnit.Millimeters, 0.5, 0.5, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 180.0), new Location(LengthUnit.Millimeters, -0.5, 0.5, 0.0, 0.0)}
        };
        
        machine.setEnabled(true);
        Location maxError = new Location(LengthUnit.Millimeters, 0.1, 0.1, 0, 0.07);
        for (Location[] testPair: testData) {
            machine.execute(() -> {
                nozzle.pick(part);
                Placement placement = new Placement("Dummy");
                placement.setLocation(testPair[0]);
                PartAlignmentOffset offset = bottomVision.findOffsets(part, boardLocation, placement, nozzle);
                Location offsets = offset.getLocation();
                assertMaxDelta(offsets.getX(), testPair[1].getX(), maxError.getX());
                assertMaxDelta(offsets.getY(), testPair[1].getY(), maxError.getY());
                assertMaxDelta(offsets.getRotation(), testPair[1].getRotation(), maxError.getRotation());
                return true;
            });
        }
    }

    
    @Test
    public void testAsymetricPartWithOffsetNoPreRotation() throws Exception {

        Machine machine = Configuration.get().getMachine();
        ReferenceBottomVision bottomVision = ReferenceBottomVision.getDefault();
        Nozzle nozzle = machine.getDefaultHead().getDefaultNozzle();
       
        Part part = Configuration.get().getPart("DoubleAsymPart");
        BottomVisionSettings bottomVisionSettings = bottomVision.getInheritedVisionSettings(part);
        
        BoardLocation boardLocation = new BoardLocation(new Board());
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setSide(Side.Top);

        // no offset
        bottomVisionSettings.setVisionOffset(new Location(LengthUnit.Millimeters, 0.5, -0.5, 0.0, 0.0));
        
        // No pre rotate
        bottomVisionSettings.setPreRotateUsage(AlwaysOff);
        
        // test data
        Location[][] testData = {
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 000.0), new Location(LengthUnit.Millimeters, 0.0, 0.0, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 045.0), new Location(LengthUnit.Millimeters, 0.0, 0.0, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 090.0), new Location(LengthUnit.Millimeters, 0.0, 0.0, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 180.0), new Location(LengthUnit.Millimeters, 0.0, 0.0, 0.0, 0.0)}
        };
        
        machine.setEnabled(true);
        Location maxError = new Location(LengthUnit.Millimeters, 0.1, 0.1, 0, 0.07);
        for (Location[] testPair: testData) {
            machine.execute(() -> {
                nozzle.pick(part);
                Placement placement = new Placement("Dummy");
                placement.setLocation(testPair[0]);
                PartAlignmentOffset offset = bottomVision.findOffsets(part, boardLocation, placement, nozzle);
                Location offsets = offset.getLocation();
                assertMaxDelta(offsets.getX(), testPair[1].getX(), maxError.getX());
                assertMaxDelta(offsets.getY(), testPair[1].getY(), maxError.getY());
                assertMaxDelta(offsets.getRotation(), testPair[1].getRotation(), maxError.getRotation());
                return true;
            });
        }
    }
    
    
    @Test
    public void testAsymetricPartWithOffsetWithPreRotation() throws Exception {

        Machine machine = Configuration.get().getMachine();
        ReferenceBottomVision bottomVision = ReferenceBottomVision.getDefault();
        Nozzle nozzle = machine.getDefaultHead().getDefaultNozzle();
       
        Part part = Configuration.get().getPart("DoubleAsymPart");
        BottomVisionSettings bottomVisionSettings = bottomVision.getInheritedVisionSettings(part);
        
        BoardLocation boardLocation = new BoardLocation(new Board());
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setSide(Side.Top);

        // 1mm offset in both directions
        bottomVisionSettings.setVisionOffset(new Location(LengthUnit.Millimeters, 0.5, -0.5, 0.0, 0.0));
        
        // With pre rotate
        bottomVisionSettings.setPreRotateUsage(AlwaysOn);
        
        // test data
        Location[][] testData = {
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 000.0), new Location(LengthUnit.Millimeters, 0.0, 0.0, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 045.0), new Location(LengthUnit.Millimeters, 0.0, 0.0, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 090.0), new Location(LengthUnit.Millimeters, 0.0, 0.0, 0.0, 0.0)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 180.0), new Location(LengthUnit.Millimeters, 0.0, 0.0, 0.0, 0.0)}
        };
        
        machine.setEnabled(true);
        Location maxError = new Location(LengthUnit.Millimeters, 0.1, 0.1, 0, 0.07);
        for (Location[] testPair: testData) {
            machine.execute(() -> {
                nozzle.pick(part);
                Placement placement = new Placement("Dummy");
                placement.setLocation(testPair[0]);
                PartAlignmentOffset offset = bottomVision.findOffsets(part, boardLocation, placement, nozzle);
                Location offsets = offset.getLocation();
                assertMaxDelta(offsets.getX(), testPair[1].getX(), maxError.getX());
                assertMaxDelta(offsets.getY(), testPair[1].getY(), maxError.getY());
                assertMaxDelta(offsets.getRotation(), testPair[1].getRotation(), maxError.getRotation());
                return true;
            });
        }
    }
    
    @Test
    public void testAsymetricPartWithOffsetNoPreRotationWithError() throws Exception {

        Machine machine = Configuration.get().getMachine();
        ReferenceBottomVision bottomVision = ReferenceBottomVision.getDefault();
        Nozzle nozzle = machine.getDefaultHead().getDefaultNozzle();
       
        Part part = Configuration.get().getPart("DoubleAsymPart");
        BottomVisionSettings bottomVisionSettings = bottomVision.getInheritedVisionSettings(part);
        
        BoardLocation boardLocation = new BoardLocation(new Board());
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setSide(Side.Top);

        // no offset
        bottomVisionSettings.setVisionOffset(new Location(LengthUnit.Millimeters, 0.5, -0.5, 0.0, 0.0));
        
        // No pre rotate
        bottomVisionSettings.setPreRotateUsage(AlwaysOff);
        
        // test data
        Location error = new Location(LengthUnit.Millimeters, 1.0, -0.5, 0.0, 18.0);

        Location[][] testData = {
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 000.0), error},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 045.0), error},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 090.0), error},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 180.0), error}
        };
        
        SimulatedUpCamera camera = (SimulatedUpCamera) VisionUtils.getBottomVisionCamera();
        camera.setErrorOffsets(error);
        
        machine.setEnabled(true);
        Location maxError = new Location(LengthUnit.Millimeters, 0.1, 0.1, 0, 0.07);
        for (Location[] testPair: testData) {
            machine.execute(() -> {
                nozzle.pick(part);
                Placement placement = new Placement("Dummy");
                placement.setLocation(testPair[0]);
                PartAlignmentOffset offset = bottomVision.findOffsets(part, boardLocation, placement, nozzle);
                Location offsets = offset.getLocation();
                assertMaxDelta(offsets.getX(), testPair[1].getX(), maxError.getX());
                assertMaxDelta(offsets.getY(), testPair[1].getY(), maxError.getY());
                assertMaxDelta(offsets.getRotation(), testPair[1].getRotation(), maxError.getRotation());
                return true;
            });
        }
    }
    
    
    @Test
    public void testAsymetricPartWithOffsetWithPreRotationWithError() throws Exception {

        Machine machine = Configuration.get().getMachine();
        ReferenceBottomVision bottomVision = ReferenceBottomVision.getDefault();
        Nozzle nozzle = machine.getDefaultHead().getDefaultNozzle();
       
        Part part = Configuration.get().getPart("DoubleAsymPart");
        BottomVisionSettings bottomVisionSettings = bottomVision.getInheritedVisionSettings(part);
        
        BoardLocation boardLocation = new BoardLocation(new Board());
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setSide(Side.Top);

        // 0.5mm offset in both directions
        Location partVisionOffset = new Location(LengthUnit.Millimeters, 0.5, -0.5, 0.0, 0.0);
        bottomVisionSettings.setVisionOffset(partVisionOffset);
        
        // With pre rotate
        bottomVisionSettings.setPreRotateUsage(AlwaysOn);
        
        // error of vision
        Location error = new Location(LengthUnit.Millimeters, -0.6, 1.2, 0.0, -12.0);
        
        // test data
        Location[][] testData = {
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 000.0), error.rotateXy(-error.getRotation())},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 045.0), error.rotateXy(-error.getRotation() + 45)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 090.0), error.rotateXy(-error.getRotation() + 90)},
                {new Location(LengthUnit.Millimeters, 10.0, 10.0, 0.0, 180.0), error.rotateXy(-error.getRotation() + 180)}
        };
        
        SimulatedUpCamera camera = (SimulatedUpCamera) VisionUtils.getBottomVisionCamera();
        camera.setErrorOffsets(error);

        machine.setEnabled(true);
        Location maxError = new Location(LengthUnit.Millimeters, 0.1, 0.1, 0, 0.07);
        for (Location[] testPair: testData) {
            machine.execute(() -> {
                nozzle.pick(part);
                Placement placement = new Placement("Dummy");
                placement.setLocation(testPair[0]);
                PartAlignmentOffset offset = bottomVision.findOffsets(part, boardLocation, placement, nozzle);
                Location offsets = offset.getLocation();
                assertMaxDelta(offsets.getX(), testPair[1].getX(), maxError.getX());
                assertMaxDelta(offsets.getY(), testPair[1].getY(), maxError.getY());
                assertMaxDelta(offsets.getRotation(), testPair[1].getRotation(), maxError.getRotation());
                return true;
            });
        }
    }


    public static void assertMaxDelta(double a, double b, double maxDelta) throws Exception {
        if (Math.abs(a - b) > maxDelta) {
            throw new Exception(String.format("abs(%f - %f) > %f", a, b, maxDelta));
        }
    }
    
}
