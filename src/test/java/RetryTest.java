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
import org.openpnp.machine.reference.camera.ImageCamera;
import org.openpnp.machine.reference.driver.NullDriver;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.ErrorHandling;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PnpJobProcessor;
import org.openpnp.spi.PropertySheetHolder;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;

public class RetryTest {
    /**
     * Attempts to perform two placements using a single feeder. The feeder's pick retry is
     * set to 3, and the feeder is programmed to fail after the first pick. The result should be
     * that the second placement does not get placed, and the feeder's feed count is 4 which
     * represents one successful feed and 3 failed retries.
     */
//    @Test
//    public void feedRetryTest() throws Exception {
//        Configuration.initialize();
//        
//        ReferenceMachine machine = new ReferenceMachine();
//        Configuration.get().setMachine(machine);
//        
//        NullDriver driver = (NullDriver) machine.getDriver();
//        driver.setFeedRateMmPerMinute(Double.MAX_VALUE);
//
//        machine.addHead(new ReferenceHead());
//        
//        Actuator actuator = new ReferenceActuator();
//        actuator.setName("N1_VAC");
//        machine.getDefaultHead().addActuator(actuator);
//        
//        ReferenceNozzle nozzle = new ReferenceNozzle();
//        nozzle.setVacuumActuatorName("N1_VAC");
//        machine.getDefaultHead().addNozzle(nozzle);
//        
//        ImageCamera camera = new ImageCamera();
//        machine.getDefaultHead().addCamera(camera);
//        
//        ReferenceNozzleTip nt = new ReferenceNozzleTip();
//        machine.addNozzleTip(nt);
//        nozzle.addCompatibleNozzleTip(nt);
//        
//        org.openpnp.model.Package packag = new org.openpnp.model.Package("R0402");
//        packag.addCompatibleNozzleTip(nt);
//        
//        Part part = new Part("R-10k-0402");
//        part.setPackage(packag);
//        part.setHeight(new Length(1, LengthUnit.Millimeters));
//        
//        Board board = new Board();
//
//        Placement placement = new Placement("R1");
//        placement.setPart(part);
//        placement.setSide(Side.Top);
//        placement.setErrorHandling(ErrorHandling.Defer);
//        board.addPlacement(placement);
//        
//        placement = new Placement("R2");
//        placement.setPart(part);
//        placement.setSide(Side.Top);
//        placement.setErrorHandling(ErrorHandling.Defer);
//        board.addPlacement(placement);
//        
//        BoardLocation boardLocation = new BoardLocation(board);
//        
//        Job job = new Job();
//        job.addBoardLocation(boardLocation);
//        
//        TestFeeder feeder = new TestFeeder();
//        feeder.setPart(part);
//        feeder.setEnabled(true);
//        feeder.setFeedRetryCount(3);
//        feeder.maxFeedCount = 1;
//        machine.addFeeder(feeder);
//        
//        machine.setEnabled(true);
//        machine.home();
//        PnpJobProcessor jobProcessor = machine.getPnpJobProcessor();
//        jobProcessor.initialize(job);
//        
//        try {
//            while (jobProcessor.next());
//        }
//        catch (Exception e) {
//            
//        }
//        Assert.assertTrue("R1 should be placed.", boardLocation.getPlaced("R1"));
//        Assert.assertFalse("R2 should not be placed.", boardLocation.getPlaced("R2"));
//        Assert.assertEquals("Feed count should be 4.", 4, feeder.feedCount);
//    }
    
    @Test
    public void pickRetryTest() throws Exception {
        Configurator.currentConfig()
                    .formatPattern("{date:yyyy-MM-dd HH:mm:ss.SSS} {class_name} {level}: {message}")
                    .level(Level.INFO)
                    .activate();
        
        Configuration.initialize();
        
        ReferenceMachine machine = new ReferenceMachine();
        Configuration.get().setMachine(machine);
        
        NullDriver driver = (NullDriver) machine.getDriver();
        driver.setFeedRateMmPerMinute(Double.MAX_VALUE);
        
        machine.addHead(new ReferenceHead());
        
        Actuator actuator = new TestActuator();
        actuator.setName("N1_VAC");
        machine.getDefaultHead().addActuator(actuator);
        
        ReferenceNozzle nozzle = new ReferenceNozzle();
        nozzle.setVacuumActuatorName("N1_VAC");
        machine.getDefaultHead().addNozzle(nozzle);
        
        ImageCamera camera = new ImageCamera();
        machine.getDefaultHead().addCamera(camera);
        
        ReferenceNozzleTip nt = new ReferenceNozzleTip();
        nt.setVacuumLevelPartOffLow(0);
        nt.setVacuumLevelPartOffHigh(100);
        nt.setVacuumLevelPartOnLow(1000);
        nt.setVacuumLevelPartOnHigh(2000);
        machine.addNozzleTip(nt);
        nozzle.addCompatibleNozzleTip(nt);
        
        org.openpnp.model.Package packag = new org.openpnp.model.Package("R0402");
        packag.addCompatibleNozzleTip(nt);
        
        Part part = new Part("R-10k-0402");
        part.setPackage(packag);
        part.setHeight(new Length(1, LengthUnit.Millimeters));
        
        Board board = new Board();

        Placement placement = new Placement("R1");
        placement.setPart(part);
        placement.setSide(Side.Top);
        // TODO STOPSHIP Check defer too
        placement.setErrorHandling(ErrorHandling.Defer);
        board.addPlacement(placement);
        
        BoardLocation boardLocation = new BoardLocation(board);
        
        Job job = new Job();
        job.addBoardLocation(boardLocation);
        
        TestFeeder feeder = new TestFeeder();
        feeder.setPart(part);
        feeder.setEnabled(true);
        feeder.setFeedRetryCount(3);
        feeder.setPickRetryCount(3);
        feeder.maxFeedCount = 3;
        machine.addFeeder(feeder);
        
        machine.setEnabled(true);
        machine.home();
        PnpJobProcessor jobProcessor = machine.getPnpJobProcessor();
        jobProcessor.initialize(job);
        
        try {
            while (jobProcessor.next());
        }
        catch (Exception e) {
            
        }
        Assert.assertFalse("R1 should not be placed.", boardLocation.getPlaced("R1"));
        Assert.assertEquals("Feed count should be 3.", 3, feeder.feedCount);
    }
    
    public static class TestFeeder extends ReferenceFeeder {
        int feedCount = 0;
        int maxFeedCount = 0;
        
        @Override
        public Location getPickLocation() throws Exception {
            return new Location(LengthUnit.Millimeters);
        }

        @Override
        public void feed(Nozzle nozzle) throws Exception {
            if (++feedCount > maxFeedCount) {
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
        @Override
        public void actuate(boolean on) throws Exception {
        }

        @Override
        public String read() throws Exception {
            return "0";
        }
    }
}
