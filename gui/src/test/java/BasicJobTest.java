import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

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
import org.openpnp.model.outline.PolygonOutline;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.JobProcessor;
import org.openpnp.spi.JobProcessor.JobError;
import org.openpnp.spi.JobProcessor.JobState;
import org.openpnp.spi.JobProcessor.PickRetryAction;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;

import com.google.common.io.Files;

public class BasicJobTest {
    /**
     * Creates a basic job in memory and attempts to run it. The Driver is
     * monitored to make sure it performs a pre-defined set of expected moves.
     * This test is intended to test the primary motions and operation of the
     * entire system, including feeding, picking, placing and basic job
     * processing.
     * 
     * @throws Exception
     */
    @Test
    public void testSimpleJob() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);

        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        Machine machine = Configuration.get().getMachine();
        ReferenceMachine referenceMachine = (ReferenceMachine) machine;
        TestDriver testDriver = (TestDriver) referenceMachine.getDriver();
        BasicJobTestDriverDelegate delegate = new BasicJobTestDriverDelegate();
        testDriver.setDelegate(delegate);

        TestCompleteNotifier notifier = new TestCompleteNotifier();

        JobProcessor jobProcessor = machine.getJobProcessor();
        jobProcessor.addListener(new BasicJobTestProcessorListener(notifier));
        jobProcessor.setDelegate(new BasicJobTestJobProcessorDelegate());

        Job job = createSimpleJob();
        
        Head h1 = machine.getHead("H1");
        Nozzle n1 = h1.getNozzle("N1"); 
        Nozzle n2 = h1.getNozzle("N2"); 

        delegate.expectMove(n1, new Location(LengthUnit.Millimeters, -10, 0,
                0, 0), 1.0);
        delegate.expectPick(n1);
        delegate.expectMove(n1, new Location(LengthUnit.Millimeters, 0, 10,
                0, 45), 1.0);
        delegate.expectMove(n1, new Location(LengthUnit.Millimeters, 0, 10,
                0.825500, 45), 1.0);
        delegate.expectPlace(n1);
        delegate.expectMove(n1, new Location(LengthUnit.Millimeters, 0, 10,
                0, 45), 1.0);

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
        board.setOutline(new PolygonOutline());

        Placement placement = new Placement("R1");
        placement.setPart(Configuration.get().getPart("R-0805-10K"));
        placement.setLocation(new Location(LengthUnit.Millimeters, 10, 10, 0,
                45));
        placement.setSide(Side.Top);
        board.addPlacement(placement);

        BoardLocation boardLocation = new BoardLocation(board);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, 0,
                0));
        boardLocation.setSide(Side.Top);

        job.addBoardLocation(boardLocation);

        return job;
    }

    public static class BasicJobTestJobProcessorDelegate implements
            JobProcessorDelegate {
        @Override
        public PickRetryAction partPickFailed(BoardLocation board, Part part,
                Feeder feeder) {
            return null;
        }
    }

    public static class BasicJobTestProcessorListener implements
            JobProcessorListener {
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
        private Queue<ExpectedOp> expectedOps = new LinkedList<ExpectedOp>();

        public void expectMove(HeadMountable hm, Location location,
                double speed) {
            ExpectedMove o = new ExpectedMove(hm, location, speed);
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
                return "Pick " + nozzle;
            }
        }

        class ExpectedPlace extends ExpectedOp {
            public Nozzle nozzle;
            
            public ExpectedPlace(Nozzle nozzle) {
                this.nozzle = nozzle;
            }
            
            @Override
            public String toString() {
                return "Place " + nozzle;
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

            public ExpectedMove(HeadMountable headMountable, Location location,
                    double speed) {
                this.headMountable = headMountable;
                this.location = location;
                this.speed = speed;
            }

            @Override
            public String toString() {
                return "Move " + headMountable + " " + location.toString();
            }
        }
    }

    public static class TestCompleteNotifier {
        public boolean failed;
        public Exception exception;
    }
}
