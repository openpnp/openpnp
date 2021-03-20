import javax.swing.Action;

import org.junit.Assert;
import org.junit.Test;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferenceNozzleTip;
import org.openpnp.machine.reference.ReferenceNozzleTip.VacuumMeasurementMethod;
import org.openpnp.machine.reference.axis.ReferenceVirtualAxis;
import org.openpnp.machine.reference.camera.ImageCamera;
import org.openpnp.machine.reference.camera.SimulatedUpCamera;
import org.openpnp.model.Board;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.Camera.Looking;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PnpJobProcessor;
import org.openpnp.spi.PropertySheetHolder;

public class ReferenceJobProcessorRetryTests {
    /**
     * If a feeder fails to feed, the JobProcessor should retry Feeder.feedRetryCount
     * number of times before giving up.
     */
    @Test
    public void testFeederFeedRetry() throws Exception {
        Configuration.initialize();
        Machine machine = new MachineBuilder()
                .head("H1")
                .nozzleTip("NT1")
                .nozzle("N1", "NT1")
                .topCamera("TOP")
                .bottomCamera("BOTTOM")
                .build();
        Job job = new JobBuilder()
                .board("B1", 10, 10, 10, -10)
                .packag("R0402", "NT1")
                .part("R0402-1k", "R0402")
                .feeder("F1", "R0402-1k", 100, 20, -5, 0)
                .placement("R1", "R0402-1k", 10, 10, 0)
                .placement("R2", "R0402-1k", 20, 20, 0)
                .build();
        
        TestFeeder f1 = (TestFeeder) machine.getFeederByName("F1");
        f1.setFeedRetryCount(3);
        f1.setPartCount(1);

        runJob(machine, job);
        
        /**
         * 5 because the feeder contains 1 part. The first feed succeeds, the second fails, and
         * the third through fifth are the three retries.
         */
        Assert.assertEquals("Feed count should be 5.", 5, f1.feedCount);
    }

    /**
     * If a feeder fails to feed the JobProcessor should disable it.
     * @throws Exception
     */
    @Test
    public void testFeederDisable() throws Exception {
        Configuration.initialize();
        Machine machine = new MachineBuilder()
                .head("H1")
                .nozzleTip("NT1")
                .nozzle("N1", "NT1")
                .topCamera("TOP")
                .bottomCamera("BOTTOM")
                .build();
        Job job = new JobBuilder()
                .board("B1", 10, 10, 10, -10)
                .packag("R0402", "NT1")
                .part("R0402-1k", "R0402")
                .feeder("F1", "R0402-1k", 100, 20, -5, 0)
                .placement("R1", "R0402-1k", 10, 10, 0)
                .build();
        
        TestFeeder f1 = (TestFeeder) machine.getFeederByName("F1");
        f1.setFeedRetryCount(3);
        f1.setPartCount(0);

       
        Assert.assertTrue("The feeder should be enabled.", f1.isEnabled());

        runJob(machine, job);
        
        Assert.assertFalse("The feeder should be disabled.", f1.isEnabled());
    }

    /**
     * If a vacuum check after a pick fails, the pick should be retried without feeding again.
     * @throws Exception
     */
    @Test
    public void testFeederPickRetry() throws Exception {
        Configuration.initialize();
        Machine machine = new MachineBuilder()
                .head("H1")
                .nozzleTip("NT1")
                .nozzle("N1", "NT1")
                .topCamera("TOP")
                .bottomCamera("BOTTOM")
                .build();
        Job job = new JobBuilder()
                .board("B1", 10, 10, 10, -10)
                .packag("R0402", "NT1")
                .part("R0402-1k", "R0402")
                .feeder("F1", "R0402-1k", 100, 20, -5, 0)
                .placement("R1", "R0402-1k", 10, 10, 0)
                .build();

        TestFeeder f1 = (TestFeeder) machine.getFeederByName("F1");
        f1.setPartCount(1);
        f1.setPickRetryCount(3);
        
        TestActuator n1Vac = (TestActuator) machine.getHeadByName("H1").getActuatorByName("N1_VAC");
        // Cause the vacuum check to fail.
        n1Vac.setReadValue("0");
        
        runJob(machine, job);
        
        TestNozzle n1 = (TestNozzle) machine.getHeadByName("H1").getNozzleByName("N1");
        Assert.assertEquals("Pick count should be 4.", 4, n1.getPickCount());
    }

    /**
     * If a post pick vacuum check fails the job processor should discard, then repeat the
     * feed-pick-check process 3 times. Additionally, if a feeder becomes disabled during
     * these repeats, additional feeders for the same part should be used. 
     */
    @Test
    public void testPartPickRetry() throws Exception {
        Configuration.initialize();
        Machine machine = new MachineBuilder()
                .head("H1")
                .nozzleTip("NT1")
                .nozzle("N1", "NT1")
                .topCamera("TOP")
                .bottomCamera("BOTTOM")
                .build();
        Job job = new JobBuilder()
                .board("B1", 10, 10, 10, -10)
                .packag("R0402", "NT1")
                .part("R0402-1k", "R0402")
                .feeder("F1", "R0402-1k", 100, 20, -5, 0)
                .feeder("F2", "R0402-1k", 110, 20, -5, 0)
                .feeder("F3", "R0402-1k", 120, 20, -5, 0)
                .feeder("F4", "R0402-1k", 130, 20, -5, 0)
                .feeder("F5", "R0402-1k", 140, 20, -5, 0)
                .placement("R1", "R0402-1k", 10, 10, 0)
                .build();
        
        Part r04021k = Configuration.get().getPart("R0402-1k");
        r04021k.setPickRetryCount(3);
        
        TestFeeder f1 = (TestFeeder) machine.getFeederByName("F1");
        f1.setPartCount(1);
        f1.setFeedRetryCount(0);
        
        TestFeeder f2 = (TestFeeder) machine.getFeederByName("F2");
        f2.setPartCount(1);
        f2.setFeedRetryCount(0);
        
        TestFeeder f3 = (TestFeeder) machine.getFeederByName("F3");
        f3.setPartCount(1);
        f3.setFeedRetryCount(0);
        
        TestActuator n1Vac = (TestActuator) machine.getHeadByName("H1").getActuatorByName("N1_VAC");
        // Cause the vacuum check to fail.
        n1Vac.setReadValue("0");
        
        runJob(machine, job);
        
        Assert.assertEquals("F1 Feed count should be 2.", 2, f1.feedCount);
        Assert.assertEquals("F2 Feed count should be 2.", 2, f2.feedCount);
        Assert.assertEquals("F3 Feed count should be 0.", 0, f3.feedCount);
    }
    

    
    
    
    
    static void runJob(Machine machine, Job job) throws Exception {
        machine.setEnabled(true);
        machine.home();
        PnpJobProcessor jobProcessor = machine.getPnpJobProcessor();
        jobProcessor.initialize(job);
        
        try {
            while (jobProcessor.next());
        }
        catch (Exception e) {
        }
    }
        
    public static class TestFeeder extends ReferenceFeeder {
        int feedCount = 0;
        int partCount = 0;
        
        public void setPartCount(int partCount) {
            this.partCount = partCount;
        }
        
        @Override
        public Location getPickLocation() throws Exception {
            return new Location(LengthUnit.Millimeters);
        }

        @Override
        public void feed(Nozzle nozzle) throws Exception {
            System.out.format("feed(%s) -> %s %s\n", nozzle.getName(), getName(), getPart().getId());
            if (++feedCount > partCount) {
                throw new Exception("No parts.");
            }
        }

        @Override
        public Wizard getConfigurationWizard() {
            return null;
        }

        @Override
        public String getPropertySheetHolderTitle() {
            return null;
        }

        @Override
        public PropertySheetHolder[] getChildPropertySheetHolders() {
            return null;
        }

        @Override
        public Action[] getPropertySheetHolderActions() {
            return null;
        }
    }
    
    public static class TestActuator extends ReferenceActuator {
        String readValue = "0.5";
        
        public void setReadValue(String readValue) {
            this.readValue = readValue;
        }
        
        @Override
        public void actuate(Object value) throws Exception {
        }

        @Override
        public Object read() throws Exception {
            return readValue;
        }
    }
    
    public static class TestNozzle extends ReferenceNozzle {
        int pickCount = 0;
        
        @Override
        public void pick(Part part) throws Exception {
            super.pick(part);
            pickCount++;
        }
        
        public int getPickCount() {
            return pickCount;
        }
    }
    
    static class MachineBuilder {
        final ReferenceMachine machine;
        ReferenceHead head = null;
        TestNozzle nozzle = null;
        
        public MachineBuilder() {
            machine = new ReferenceMachine();
            Configuration.get().setMachine(machine);
        }

        public MachineBuilder head(String name) throws Exception {
            head = new ReferenceHead();
            head.setName(name);
            machine.addHead(head);
            return this;
        }
        
        public MachineBuilder topCamera(String name) throws Exception {
            ImageCamera camera = new ImageCamera();
            camera.setName(name);
            camera.setLooking(Looking.Down);
            head.addCamera(camera);
            return this;
        }
        
        public MachineBuilder bottomCamera(String name) throws Exception {
            SimulatedUpCamera camera = new SimulatedUpCamera();
            camera.setName(name);
            camera.setLooking(Looking.Up);
            machine.addCamera(camera);
            return this;
        }
        
        public MachineBuilder nozzle(String name, String... compatibleNozzleTipNames) throws Exception {
            TestActuator actuator = new TestActuator();
            actuator.setName(name + "_VAC");
            head.addActuator(actuator);
            
            nozzle = new TestNozzle();
            nozzle.setName(name);
            nozzle.setAxis(new ReferenceVirtualAxis(Type.X));
            nozzle.setAxis(new ReferenceVirtualAxis(Type.Y));
            nozzle.setAxis(new ReferenceVirtualAxis(Type.Z));
            nozzle.setAxis(new ReferenceVirtualAxis(Type.Rotation));
            nozzle.setChangerEnabled(true);
            nozzle.setVacuumActuatorName(name + "_VAC");
            nozzle.setVacuumSenseActuatorName(name + "_VAC");
            
            for (String ntName : compatibleNozzleTipNames) {
                NozzleTip nt = machine.getNozzleTipByName(ntName);
                nozzle.addCompatibleNozzleTip(nt);
            }
            
            head.addNozzle(nozzle);
            return this;
        }
        
        public MachineBuilder nozzleTip(String name) throws Exception {
            ReferenceNozzleTip nt = new ReferenceNozzleTip();
            nt.setName(name);
            nt.setVacuumLevelPartOnLow(0.5);
            nt.setVacuumLevelPartOnHigh(1.0);
            nt.setMethodPartOn(VacuumMeasurementMethod.Absolute);
            machine.addNozzleTip(nt);
            return this;
        }
        
        public Machine build() throws Exception {
            return machine;
        }
    }
    
    static class JobBuilder {
        Job job = new Job();
        BoardLocation boardLocation = null;
        
        public JobBuilder board(String name, double x, double y, double z, double rotation) {
            Board board = new Board();
            board.setName(name);
            boardLocation = new BoardLocation(board);
            boardLocation.setLocation(new Location(LengthUnit.Millimeters, x, y, z, rotation));
            job.addBoardLocation(boardLocation);
            return this;
        }
        
        public JobBuilder packag(String id, String... compatibleNozzleTipNames) {
            Package packag = new Package(id);
            for (String ntName : compatibleNozzleTipNames) {
                NozzleTip nt = Configuration.get().getMachine().getNozzleTipByName(ntName);
                packag.addCompatibleNozzleTip(nt);
            }
            Configuration.get().addPackage(packag);
            return this;
        }
        
        public JobBuilder part(String id, String packageId) {
            Part part = new Part(id);
            part.setPackage(Configuration.get().getPackage(packageId));
            part.setHeight(new Length(1, LengthUnit.Millimeters));
            Configuration.get().addPart(part);
            return this;
        }
        
        public JobBuilder feeder(String name, String partId, double x, double y, double z, double rotation) throws Exception {
            TestFeeder feeder = new TestFeeder();
            feeder.setName(name);
            feeder.setPart(Configuration.get().getPart(partId));
            feeder.setLocation(new Location(LengthUnit.Millimeters, x, y, z, rotation));
            feeder.setEnabled(true);
            Configuration.get().getMachine().addFeeder(feeder);
            return this;
        }
        
        public JobBuilder placement(String id, String partId, double x, double y, double rotation) {
            Placement placement = new Placement(id);
            placement.setPart(Configuration.get().getPart(partId));
            placement.setLocation(new Location(LengthUnit.Millimeters, x, y, 0, rotation));
            boardLocation.getBoard().addPlacement(placement);
            return this;
        }
        
        public Job build() {
            return job;
        }
    }    
}
