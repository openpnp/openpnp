import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.openpnp.JobProcessorDelegate;
import org.openpnp.JobProcessorListener;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.driver.test.TestDriver;
import org.openpnp.machine.reference.driver.test.TestDriver.TestDriverDelegate;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.JobProcessor;
import org.openpnp.spi.JobProcessor.JobError;
import org.openpnp.spi.JobProcessor.JobState;
import org.openpnp.spi.JobProcessor.PickRetryAction;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

public class BasicJobTest {
    private final static Logger logger = LoggerFactory
            .getLogger(TestDriver.class);

    /**
     * Creates a basic job in memory and attempts to run it. The Driver is
     * monitored to make sure it performs a pre-defined set of expected moves.
     * This test is intended to test the primary motions and operation of the
     * entire system, including feeding, picking, placing and basic job
     * processing.
     * 
     * TODO: Don't ignore additional movements after the expected movements
     * complete. This should cause the test to fail and it does not currently.
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
        FileUtils.copyURLToFile(
        		ClassLoader.getSystemResource("config/BasicJobTest/machine.xml"),
        		new File(workingDirectory, "machine.xml"));
        FileUtils.copyURLToFile(
        		ClassLoader.getSystemResource("config/BasicJobTest/packages.xml"),
        		new File(workingDirectory, "packages.xml"));
        FileUtils.copyURLToFile(
        		ClassLoader.getSystemResource("config/BasicJobTest/parts.xml"),
        		new File(workingDirectory, "parts.xml"));

        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        Machine machine = Configuration.get().getMachine();
        ReferenceMachine referenceMachine = (ReferenceMachine) machine;
        TestDriver testDriver = (TestDriver) referenceMachine.getDriver();
        BasicJobTestDriverDelegate delegate = new BasicJobTestDriverDelegate();
        testDriver.setDelegate(delegate);

        TestCompleteNotifier notifier = new TestCompleteNotifier();

        JobProcessor jobProcessor = machine.getJobProcessors().get(JobProcessor.Type.PickAndPlace);
        jobProcessor.addListener(new BasicJobTestProcessorListener(notifier));
        jobProcessor.setDelegate(new BasicJobTestJobProcessorDelegate());

        Job job = createSimpleJob();

        Head h1 = machine.getHead("H1");
        Nozzle n1 = h1.getNozzle("N1");
        Nozzle n2 = h1.getNozzle("N2");

        delegate.expectMove("Move N1 to F1", n1, new Location(LengthUnit.Millimeters, -10, 0, 0, 0), 1.0);
        delegate.expectPick(n1);
        
        delegate.expectMove("Move N2 to F1", n2, new Location(LengthUnit.Millimeters, -20, 0, 0, 0), 1.0);
        delegate.expectPick(n2);
        
        delegate.expectMove("Move N1 to R1, Safe-Z", n1, new Location(LengthUnit.Millimeters, 0, 10, 0, 45), 1.0);
        delegate.expectMove("Move N1 to R1, Z", n1, new Location(LengthUnit.Millimeters, 0, 10, 0.825500, 45), 1.0);
        delegate.expectPlace(n1);
        delegate.expectMove("Move N1 to R1, Safe-Z", n1, new Location(LengthUnit.Millimeters, 0, 10, 0, 45), 1.0);
        
        delegate.expectMove("Move N2 to R2, Safe-Z", n2, new Location(LengthUnit.Millimeters, 00, 20, 0, 90), 1.0);
        delegate.expectMove("Move N2 to R2, Z", n2, new Location(LengthUnit.Millimeters, 00, 20, 0.825500, 90), 1.0);
        delegate.expectPlace(n2);
        delegate.expectMove("Move N2 to R2, Safe-Z", n2, new Location(LengthUnit.Millimeters, 00, 20, 0, 90), 1.0);
        
        jobProcessor.load(job);
        machine.setEnabled(true);
        synchronized (notifier) {
            jobProcessor.start();
            notifier.wait();
        }
        if (notifier.failed) {
            throw notifier.exception;
        }
    }

    private Job createSimpleJob() {
        Job job = new Job();

        Board board = new Board();
        board.setName("test");

        board.addPlacement(createPlacement("R1", "R-0805-10K", 10, 10, 0, 45,
                Side.Top));
        board.addPlacement(createPlacement("R2", "R-0805-10K", 20, 20, 0, 90,
                Side.Top));

        BoardLocation boardLocation = new BoardLocation(board);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, 0,
                0));
        boardLocation.setSide(Side.Top);

        job.addBoardLocation(boardLocation);

        return job;
    }

    public static Placement createPlacement(String id, String partId, double x,
            double y, double z, double rotation, Side side) {
        Placement placement = new Placement(id);
        placement.setPart(Configuration.get().getPart(partId));
        placement.setLocation(new Location(LengthUnit.Millimeters, x, y, z,
                rotation));
        placement.setSide(side);
        return placement;
    }

    public static class BasicJobTestJobProcessorDelegate implements
            JobProcessorDelegate {
        @Override
        public PickRetryAction partPickFailed(BoardLocation board, Part part,
                Feeder feeder) {
            return null;
        }
    }

    public static class BasicJobTestProcessorListener extends JobProcessorListener.Adapter {
        final private TestCompleteNotifier notifier;

        public BasicJobTestProcessorListener(TestCompleteNotifier notifier) {
            this.notifier = notifier;
        }

        @Override
        public void jobLoaded(Job job) {
        }

        @Override
        public void jobStateChanged(JobState state) {
            if (state == JobState.Stopped) {
                synchronized (notifier) {
                    notifier.notifyAll();
                }
            }
        }

        @Override
        public void jobEncounteredError(JobError error, String description) {
            synchronized (notifier) {
                notifier.failed = true;
                notifier.exception = new Exception(error + " " + description);
                notifier.notifyAll();
            }
        }

        @Override
        public void partProcessingStarted(BoardLocation board,
                Placement placement) {
            logger.info("Start " + placement.getId());
        }

        @Override
        public void partPicked(BoardLocation board, Placement placement) {
        }

        @Override
        public void partPlaced(BoardLocation board, Placement placement) {
        }

        @Override
        public void partProcessingCompleted(BoardLocation board,
                Placement placement) {
            logger.info("Finish " + placement.getId());
        }

        @Override
        public void detailedStatusUpdated(String status) {
        }
    }

    /**
     * TODO: Allow passing of null for the expect methods to ignore a particular
     * field.
     */
    public static class BasicJobTestDriverDelegate extends TestDriverDelegate {
        private Queue<ExpectedOp> expectedOps = new LinkedList<>();

        public void expectMove(String description, HeadMountable hm, Location location, double speed) {
            ExpectedMove o = new ExpectedMove(description, hm, location, speed);
            expectedOps.add(o);
        }

        public void expectPick(Nozzle nozzle) {
            expectedOps.add(new ExpectedPick(nozzle));
        }

        public void expectPlace(Nozzle nozzle) {
            expectedOps.add(new ExpectedPlace(nozzle));
        }

        public void expectedActuate() {
            expectedOps.add(new ExpectedActuate());
        }

        @Override
        public void moveTo(ReferenceHeadMountable hm, Location location,
                double speed) throws Exception {
            if (expectedOps.isEmpty()) {
                throw new Exception("Unexpected Move " + location + ".");
            }
            else {
                ExpectedOp op = expectedOps.remove();

                if (!(op instanceof ExpectedMove)) {
                    throw new Exception("Unexpected Move " + location
                            + ". Expected " + op);
                }

                ExpectedMove move = (ExpectedMove) op;

                if (!move.location.equals(location) || hm != move.headMountable) {
                    throw new Exception("Unexpected Move " + location
                            + ". Expected " + op);
                }
            }
        }

        @Override
        public void pick(ReferenceNozzle nozzle) throws Exception {
            if (expectedOps.isEmpty()) {
                throw new Exception("Unexpected Pick " + nozzle + ".");
            }
            else {
                ExpectedOp op = expectedOps.remove();
                if (!(op instanceof ExpectedPick)) {
                    throw new Exception("Unexpected Pick " + nozzle
                            + ". Expected " + op);
                }

                ExpectedPick pick = (ExpectedPick) op;

                if (pick.nozzle != nozzle) {
                    throw new Exception("Unexpected Pick " + nozzle
                            + ". Expected " + op);
                }
            }
        }

        @Override
        public void place(ReferenceNozzle nozzle) throws Exception {
            if (expectedOps.isEmpty()) {
                throw new Exception("Unexpected Place " + nozzle + ".");
            }
            else {
                ExpectedOp op = expectedOps.remove();
                if (!(op instanceof ExpectedPlace)) {
                    throw new Exception("Unexpected Place " + nozzle
                            + ". Expected " + op);
                }

                ExpectedPlace place = (ExpectedPlace) op;

                if (place.nozzle != nozzle) {
                    throw new Exception("Unexpected Place " + nozzle
                            + ". Expected " + op);
                }
            }
        }

        @Override
        public void actuate(ReferenceActuator actuator, boolean on)
                throws Exception {
            // TODO Auto-generated method stub
            super.actuate(actuator, on);
        }

        @Override
        public void actuate(ReferenceActuator actuator, double value)
                throws Exception {
            // TODO Auto-generated method stub
            super.actuate(actuator, value);
        }

        class ExpectedOp {
        }

        class ExpectedPick extends ExpectedOp {
            public Nozzle nozzle;

            public ExpectedPick(Nozzle nozzle) {
                this.nozzle = nozzle;
            }

            @Override
            public String toString() {
                return "Pick " + nozzle + " " + nozzle.getNozzleTip().getName();
            }
        }

        class ExpectedPlace extends ExpectedOp {
            public Nozzle nozzle;

            public ExpectedPlace(Nozzle nozzle) {
                this.nozzle = nozzle;
            }

            @Override
            public String toString() {
                return "Place " + nozzle + " " + nozzle.getNozzleTip().getName();
            }
        }

        class ExpectedActuate extends ExpectedOp {
            @Override
            public String toString() {
                return "Actuate";
            }
        }

        class ExpectedMove extends ExpectedOp {
            public HeadMountable headMountable;
            public Location location;
            public double speed;
            public String description;

            public ExpectedMove(String description, HeadMountable headMountable, Location location,
                    double speed) {
                this.headMountable = headMountable;
                this.location = location;
                this.speed = speed;
                this.description = description;
            }

            @Override
            public String toString() {
                return "Move (" + description + ") "+ headMountable + " " + location.toString();
            }
        }
    }

    public static class TestCompleteNotifier {
        public boolean failed;
        public Exception exception;
    }
}
