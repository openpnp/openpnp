import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferencePnpJobProcessor;
import org.openpnp.machine.reference.driver.test.TestDriver;
import org.openpnp.machine.reference.driver.test.TestDriver.TestDriverDelegate;
import org.openpnp.machine.reference.feeder.ReferenceTubeFeeder;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Motion.MoveToCommand;
import org.openpnp.model.Placement;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;

import com.google.common.io.Files;

public class BasicJobTest {
    /**
     * Creates a basic job in memory and attempts to run it. The Driver is monitored to make sure it
     * performs a pre-defined set of expected moves. This test is intended to test the primary
     * motions and operation of the entire system, including feeding, picking, placing and basic job
     * processing.
     * 
     * TODO: Don't ignore additional movements after the expected movements complete. This should
     * cause the test to fail and it does not currently.
     * 
     * @throws Exception
     */
    @Test
    public void testSimpleJob() throws Exception {
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
        // Save back migrated.
        Configuration.get().save();

        Machine machine = Configuration.get().getMachine();
        ReferenceMachine referenceMachine = (ReferenceMachine) machine;
        TestDriver testDriver = (TestDriver) referenceMachine.getDefaultDriver();
        BasicJobTestDriverDelegate delegate = new BasicJobTestDriverDelegate();
        testDriver.setDelegate(delegate);
        
        // Set feeder to Z-10 
        ReferenceTubeFeeder feeder = (ReferenceTubeFeeder) machine.getFeeder("F1");
        feeder.setLocation(feeder.getLocation().derive(null, null, -10.0, null));

        Job job = createSimpleJob();

        Head h1 = machine.getHead("H1");
        Nozzle n1 = h1.getNozzle("N1");
        Nozzle n2 = h1.getNozzle("N2");
        Camera c1 = h1.getCamera("C1");

        delegate.expectMove("Move N1 Nozzle Change Load", n1,
                new Location(LengthUnit.Millimeters, 40, 0, 0, 0), 1.0);
        delegate.expectMove("Move N2 Nozzle Change Load", n2,
                new Location(LengthUnit.Millimeters, 50, 0, 0, 0), 1.0);

        delegate.expectMove("Move N1 to F1, Safe Z", n1, new Location(LengthUnit.Millimeters, -10, 0, 0, 0),
                1.0);
        delegate.expectMove("Move N1 to F1, Feeder Z", n1, new Location(LengthUnit.Millimeters, -10, 0, -10, 0),
                1.0);

        delegate.expectedActuate();

        delegate.expectMove("Move N1 to F1, Safe Z", n1, new Location(LengthUnit.Millimeters, -10, 0, 0, 0),
                1.0);
        delegate.expectMove("Move N2 to F1, Safe Z", n2, new Location(LengthUnit.Millimeters, -20, 0, 0, 0),
                1.0);
        delegate.expectMove("Move N2 to F1, Feeder Z", n2, new Location(LengthUnit.Millimeters, -20, 0, -10, 0),
                1.0);

        delegate.expectedActuate();

        delegate.expectMove("Move N2 to F1, Safe Z", n2, new Location(LengthUnit.Millimeters, -20, 0, 0, 0),
                1.0);
        delegate.expectMove("Move N1 to R1, Safe-Z", n1,
                new Location(LengthUnit.Millimeters, 0, 10, 0, 45), 1.0);
        delegate.expectMove("Move N1 to R1, Z", n1,
                new Location(LengthUnit.Millimeters, 0, 10, 0.825500 - 10, 45), 1.0);
        delegate.expectedActuate();
        delegate.expectMove("Move N1 to R1, Safe-Z", n1,
                new Location(LengthUnit.Millimeters, 0, 10, 0, 45), 1.0);

        delegate.expectMove("Move N2 to R2, Safe-Z", n2,
                new Location(LengthUnit.Millimeters, 00, 20, 0, 90), 1.0);
        delegate.expectMove("Move N2 to R2, Z", n2,
                new Location(LengthUnit.Millimeters, 00, 20, 0.825500 - 10, 90), 1.0);
        delegate.expectedActuate();
        delegate.expectMove("Move N2 to R2, Safe-Z", n2,
                new Location(LengthUnit.Millimeters, 00, 20, 0, 90), 1.0);
        delegate.expectMove("Park", c1, new Location(LengthUnit.Millimeters, 0, 0, 0, 90), 1.0);

        ReferencePnpJobProcessor jobProcessor = (ReferencePnpJobProcessor) machine.getPnpJobProcessor();
        machine.setEnabled(true);
        machine.execute(() -> { 
            jobProcessor.addTextStatusListener(text -> {
                System.out.println(text);
            });
            jobProcessor.initialize(job);
            while (jobProcessor.next()) {
                //spin
            };
            return true;
        });
    }

    private Job createSimpleJob() {
        Job job = new Job();

        Board board = new Board();
        board.setName("test");

        board.addPlacement(createPlacement("R1", "R-0805-10K", 10, 10, 0, 45, Side.Top));
        board.addPlacement(createPlacement("R2", "R-0805-10K", 20, 20, 0, 90, Side.Top));

        BoardLocation boardLocation = new BoardLocation(board);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, -10, 0));
        boardLocation.setSide(Side.Top);

        job.addBoardLocation(boardLocation);

        return job;
    }

    public static Placement createPlacement(String id, String partId, double x, double y, double z,
            double rotation, Side side) {
        Placement placement = new Placement(id);
        placement.setPart(Configuration.get().getPart(partId));
        placement.setLocation(new Location(LengthUnit.Millimeters, x, y, z, rotation));
        placement.setSide(side);
        return placement;
    }

    /**
     * TODO: Allow passing of null for the expect methods to ignore a particular field.
     */
    public static class BasicJobTestDriverDelegate extends TestDriverDelegate {
        private Queue<ExpectedOp> expectedOps = new LinkedList<>();

        public void expectMove(String description, HeadMountable hm, Location location,
                double speed) throws Exception {
            ExpectedMove o = new ExpectedMove(description, hm, location, speed);
            expectedOps.add(o);
        }

        public void expectedActuate() {
            expectedOps.add(new ExpectedActuate());
        }

        @Override
        public void moveTo(HeadMountable hm, MoveToCommand move)
                throws Exception {
            AxesLocation location = move.getLocation1();
            
            System.out.println(hm + " " + location);
            if (expectedOps.isEmpty()) {
                throw new Exception("Unexpected Move " + hm + " " + location + ".");
            }
            else {
                ExpectedOp op = expectedOps.remove();

                if (!(op instanceof ExpectedMove)) {
                    throw new Exception("Unexpected Move " + hm + " " + location + ". Expected " + op);
                }

                ExpectedMove expectedMove = (ExpectedMove) op;

                if (!expectedMove.location.matches(location) || hm != expectedMove.headMountable) {
                    throw new Exception("Unexpected Move " + hm + " " + location + ". Expected " + op);
                }
            }
        }

        @Override
        public void actuate(Actuator actuator, boolean on) throws Exception {
            if (expectedOps.isEmpty()) {
                throw new Exception("Unexpected Actuate");
            }
            else {
                ExpectedOp op = expectedOps.remove();

                if (!(op instanceof ExpectedActuate)) {
                    throw new Exception("Unexpected Actuate. Expected " + op);
                }

            }
            super.actuate(actuator, on);
        }

        @Override
        public void actuate(Actuator actuator, double value) throws Exception {
            super.actuate(actuator, value);
        }

        class ExpectedOp {
        }

        class ExpectedActuate extends ExpectedOp {
            @Override
            public String toString() {
                return "Actuate";
            }
        }

        class ExpectedMove extends ExpectedOp {
            public HeadMountable headMountable;
            public AxesLocation location;
            public double speed;
            public String description;

            public ExpectedMove(String description, HeadMountable headMountable, Location location,
                    double speed) throws Exception {
                this.headMountable = headMountable;
                // The expected location must be converted into raw coordinates, but it is already a head location.  
                // Therefore, don't do this:  Location headLocation = headMountable.toHeadLocation(location);
                this.location = headMountable.toRaw(location);
                // Only controller coordinates (no virtual ones).
                this.location = new AxesLocation(this.location.getControllerAxes(),
                        (axis) -> this.location.getLengthCoordinate(axis));
                this.speed = speed;
                this.description = description;
            }

            @Override
            public String toString() {
                return "Move (" + description + ") " + headMountable + " " + location.toString();
            }
        }
    }
}
