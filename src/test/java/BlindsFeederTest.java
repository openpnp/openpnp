import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.opencv.core.RotatedRect;
import org.junit.jupiter.api.BeforeEach;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.spi.Machine;
import org.openpnp.vision.Ransac.Line;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.machine.reference.feeder.BlindsFeeder;
import org.openpnp.machine.reference.feeder.BlindsFeeder.FindFeatures;
import org.openpnp.model.Location;
import org.openpnp.model.LengthUnit;

import com.google.common.io.Files;

public class BlindsFeederTest {
    public class BlindsFeederTestFiducials{
        Location fiducial1;
        Location fiducial2;
        Location fiducial3;
        BlindsFeederTestFiducials(double X1, double Y1, double X2, double Y2, double X3, double Y3){
            fiducial1  = new Location(LengthUnit.Millimeters, X1, Y1, 0, 0.0);
            fiducial2  = new Location(LengthUnit.Millimeters, X2, Y2, 0, 0.0);
            fiducial3  = new Location(LengthUnit.Millimeters, X3, Y3, 0, 0.0);
        }
    }
    
    @BeforeEach
    private void testBlindsFeederLoadConfiguration() throws Exception {
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
    }    
    
    @Test
    public void testBlindsFeederBasics() throws Exception {        
        Machine machine = Configuration.get().getMachine();
        List<Feeder> feeders = machine.getFeeders();
        
        BlindsFeederTestFiducials feederFiducals1 = new BlindsFeederTestFiducials(180,100, 100,100, 100,165);
        BlindsFeederTestFiducials feederFiducals2 = new BlindsFeederTestFiducials(280,100, 200,100, 200,165);
        BlindsFeederTestFiducials feederFiducals3 = new BlindsFeederTestFiducials(380,100, 300,100, 300,165);

        //********************************************************************************************//
        //Test creating a new feeder
        //Create first blinds feeder and set and check its location
        BlindsFeeder blindsFeeder1 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder1);

        blindsFeeder1.setFiducial1Location(feederFiducals1.fiducial1);
        blindsFeeder1.setFiducial2Location(feederFiducals1.fiducial2);
        blindsFeeder1.setFiducial3Location(feederFiducals1.fiducial3);

        assert(blindsFeeder1.getFiducial1Location().equals(feederFiducals1.fiducial1));
        assert(blindsFeeder1.getFiducial2Location().equals(feederFiducals1.fiducial2));
        assert(blindsFeeder1.getFiducial3Location().equals(feederFiducals1.fiducial3));
        
        assert(blindsFeeder1.getFeedersTotal() == 1);

        
        //********************************************************************************************//
        //Test creating another feeder and connect it in the same location using fiducial 1 only
        BlindsFeeder blindsFeeder2 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder2);

        blindsFeeder2.setFiducial1Location(feederFiducals1.fiducial1);
        
        //Check all fiducials match
        assert(blindsFeeder2.getFiducial1Location().equals(feederFiducals1.fiducial1));
        assert(blindsFeeder2.getFiducial2Location().equals(feederFiducals1.fiducial2));
        assert(blindsFeeder2.getFiducial3Location().equals(feederFiducals1.fiducial3));
        
        //Check the two feeders are connected in both directions
        assert(blindsFeeder1.getConnectedFeeders().contains(blindsFeeder2));
        assert(blindsFeeder2.getConnectedFeeders().contains(blindsFeeder1));

        assert(blindsFeeder1.getFeedersTotal() == 2);

        //********************************************************************************************//
        //Test creating a feeder at a different location and that it does not connect to the othres
        BlindsFeeder blindsFeeder3 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder3);

        //Set the feeder 3 first fiducial to location 2
        blindsFeeder3.setFiducial1Location(feederFiducals2.fiducial1);
        
        //Check feeder 3 is not connected to feeder 1/2
        assert(!blindsFeeder1.getConnectedFeeders().contains(blindsFeeder3));
        assert(!blindsFeeder3.getConnectedFeeders().contains(blindsFeeder1));

        //Check the count of connected feeders
        assert(blindsFeeder1.getFeedersTotal() == 2);
        assert(blindsFeeder3.getFeedersTotal() == 1);

        //Set fiducials 2&3 and check connected feeders again.
        blindsFeeder3.setFiducial2Location(feederFiducals2.fiducial2);
        blindsFeeder3.setFiducial3Location(feederFiducals2.fiducial3);
 
        assert(!blindsFeeder1.getConnectedFeeders().contains(blindsFeeder3));
        assert(!blindsFeeder3.getConnectedFeeders().contains(blindsFeeder1));

        //********************************************************************************************//
        //Test setting feeder pockets
        final double pocketSizeMm = 2.8;
        final double pocketPositionMm = -1.0;
        final double pocketPitchMm = 4.0;
        final double pocketCenterline1Mm = 9.0;
        final double pocketCenterline2Mm = 19.0;

        //Set feeder1 to first position
        blindsFeeder1.setPocketCenterline(new Length(pocketCenterline1Mm, LengthUnit.Millimeters));
        blindsFeeder1.setPocketPitch(new Length(pocketPitchMm, LengthUnit.Millimeters));
        blindsFeeder1.setPocketSize(new Length(pocketSizeMm, LengthUnit.Millimeters));

        assert(blindsFeeder1.getFeederNo() == 2);
        assert(blindsFeeder2.getFeederNo() == 1);
        
        //Set feeder2 to second position
        blindsFeeder2.setPocketCenterline(new Length(pocketCenterline2Mm, LengthUnit.Millimeters));
        blindsFeeder2.setPocketPitch(new Length(pocketPitchMm, LengthUnit.Millimeters));
        blindsFeeder2.setPocketSize(new Length(pocketSizeMm, LengthUnit.Millimeters));

        assert(blindsFeeder1.getFeederNo() == 1);
        assert(blindsFeeder2.getFeederNo() == 2);
        
        assert(blindsFeeder1.getConnectedFeeders().contains(blindsFeeder2));
        assert(blindsFeeder2.getConnectedFeeders().contains(blindsFeeder1));

        //********************************************************************************************//
        //Test moving connected feeder fiducial 1 results in fiducials for conencted feeder moving relatively
        
        //Check before moving that feeder 1|2 fiducials are at location 1
        assert(blindsFeeder1.getFiducial1Location().equals(feederFiducals1.fiducial1));
        assert(blindsFeeder1.getFiducial2Location().equals(feederFiducals1.fiducial2));
        assert(blindsFeeder1.getFiducial3Location().equals(feederFiducals1.fiducial3));        
        assert(blindsFeeder2.getFiducial1Location().equals(feederFiducals1.fiducial1));
        assert(blindsFeeder2.getFiducial2Location().equals(feederFiducals1.fiducial2));
        assert(blindsFeeder2.getFiducial3Location().equals(feederFiducals1.fiducial3));
        
        // Change feeder 1 fiducial 1 to location 3 and check all connected feeders are moved
        blindsFeeder1.setFiducial1Location(feederFiducals3.fiducial1);
        
        assert(blindsFeeder1.getFiducial1Location().equals(feederFiducals3.fiducial1));
        assert(blindsFeeder1.getFiducial2Location().equals(feederFiducals3.fiducial2));
        assert(blindsFeeder1.getFiducial3Location().equals(feederFiducals3.fiducial3));

        assert(blindsFeeder2.getFiducial1Location().equals(feederFiducals3.fiducial1));
        assert(blindsFeeder2.getFiducial2Location().equals(feederFiducals3.fiducial2));
        assert(blindsFeeder2.getFiducial3Location().equals(feederFiducals3.fiducial3));
        
        assert(blindsFeeder1.getConnectedFeeders().contains(blindsFeeder2));
        assert(blindsFeeder2.getConnectedFeeders().contains(blindsFeeder1));
    }
    
    @Test
    public void testBlindsFeederGroups() throws Exception {
        Machine machine = Configuration.get().getMachine();
        List<Feeder> feeders = machine.getFeeders();
        
        BlindsFeederTestFiducials feederFiducals1 = new BlindsFeederTestFiducials(180,100, 100,100, 100,165);
        BlindsFeederTestFiducials feederFiducals2 = new BlindsFeederTestFiducials(280,100, 200,100, 200,165);
        BlindsFeederTestFiducials feederFiducals3 = new BlindsFeederTestFiducials(380,100, 300,100, 300,165);
        
        BlindsFeeder blindsFeeder1 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder1);
        blindsFeeder1.setFiducial1Location(feederFiducals1.fiducial1);
        blindsFeeder1.setFiducial2Location(feederFiducals1.fiducial2);
        blindsFeeder1.setFiducial3Location(feederFiducals1.fiducial3);
        
        assert(blindsFeeder1.getFeederGroupName().equals(BlindsFeeder.defaultGroupName)); 
        
        BlindsFeeder blindsFeeder2 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder2);
        blindsFeeder2.setFiducial1Location(feederFiducals1.fiducial1);
        
        //Check feeders are connected
        assert(blindsFeeder1.getConnectedFeeders().contains(blindsFeeder2));
        assert(blindsFeeder2.getConnectedFeeders().contains(blindsFeeder1));

        //Change group name of feeder1, check feeder 2 at same location also changes group name
        blindsFeeder1.setFeederGroupName("BlindsFeederTestGroup1");
        assert(blindsFeeder1.getFeederGroupName().equals("BlindsFeederTestGroup1"));
        assert(blindsFeeder2.getFeederGroupName().equals(blindsFeeder1.getFeederGroupName()));
        
        //Check feeders are connected
        assert(blindsFeeder1.getConnectedFeeders().contains(blindsFeeder2));
        assert(blindsFeeder2.getConnectedFeeders().contains(blindsFeeder1));
        
        //Create a new feeder at the same location but with default group name
        BlindsFeeder blindsFeeder3 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder3);

        //Set the first fiducial and check the others are not set and it is not in the same group
        blindsFeeder3.setFiducial1Location(feederFiducals1.fiducial1);
        //Check it is not connected to the other feeders at the same location
        assert(!blindsFeeder3.getConnectedFeeders().contains(blindsFeeder1));
        //And the previous named feeders don't connect to it
        assert(!blindsFeeder1.getConnectedFeeders().contains(blindsFeeder3));

        //Check again that the first two feeders are still connected
        assert(blindsFeeder1.getConnectedFeeders().contains(blindsFeeder2));
        assert(blindsFeeder2.getConnectedFeeders().contains(blindsFeeder1));
        
        //Set the remaining fiducials to the same location and check again that it is not part of the named group
        assert(!blindsFeeder3.getFiducial2Location().equals(feederFiducals1.fiducial2));
        assert(!blindsFeeder3.getFiducial3Location().equals(feederFiducals1.fiducial3));
        assert(!blindsFeeder3.getConnectedFeeders().contains(blindsFeeder1));
        assert(!blindsFeeder1.getConnectedFeeders().contains(blindsFeeder3));

        //Set remaining fiducials and check again
        blindsFeeder3.setFiducial2Location(feederFiducals1.fiducial2);
        blindsFeeder3.setFiducial3Location(feederFiducals1.fiducial3);
        assert(!blindsFeeder3.getConnectedFeeders().contains(blindsFeeder1));
        assert(!blindsFeeder1.getConnectedFeeders().contains(blindsFeeder3));

        //Move feeder 3 to second fiducials and check that it has moved and that the non connected have not.
        blindsFeeder3.setFiducial1Location(feederFiducals2.fiducial1);
        blindsFeeder3.setFiducial2Location(feederFiducals2.fiducial2);
        blindsFeeder3.setFiducial3Location(feederFiducals2.fiducial3);
        
        assert(!blindsFeeder2.getFiducial1Location().equals(feederFiducals2.fiducial1));
        assert(!blindsFeeder2.getFiducial2Location().equals(feederFiducals2.fiducial2));
        assert(!blindsFeeder2.getFiducial3Location().equals(feederFiducals2.fiducial3));

        assert(blindsFeeder3.getFiducial1Location().equals(feederFiducals2.fiducial1));
        assert(blindsFeeder3.getFiducial2Location().equals(feederFiducals2.fiducial2));
        assert(blindsFeeder3.getFiducial3Location().equals(feederFiducals2.fiducial3));
        
        assert(!blindsFeeder3.getConnectedFeeders().contains(blindsFeeder1));
        assert(!blindsFeeder1.getConnectedFeeders().contains(blindsFeeder3));

        //********************************************************************************************//
        //Test assigning an existing group to a feeder at default location.
        
        //Create a new feeder at the first location with default group name
        BlindsFeeder blindsFeeder4 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder4);
        
        //Set group name with default location to existing group
        blindsFeeder4.setFeederGroupName("BlindsFeederTestGroup1");
        
        //Check it is connected to the group and has the expected fiducials
        assert(blindsFeeder4.getConnectedFeeders().contains(blindsFeeder1));
        assert(blindsFeeder1.getConnectedFeeders().contains(blindsFeeder4));
        assert(blindsFeeder4.getFiducial1Location().equals(blindsFeeder1.getFiducial1Location()));
        assert(blindsFeeder4.getFiducial2Location().equals(blindsFeeder1.getFiducial2Location()));
        assert(blindsFeeder4.getFiducial3Location().equals(blindsFeeder1.getFiducial3Location()));
        
        //********************************************************************************************//
        //Test assigning an existing group to a placed feeder that has no connected feeders.
        //Create a new feeder at the third location
        BlindsFeeder blindsFeeder5 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder5);
        
        blindsFeeder5.setFiducial1Location(feederFiducals3.fiducial1);
        blindsFeeder5.setFiducial2Location(feederFiducals3.fiducial2);
        blindsFeeder5.setFiducial3Location(feederFiducals3.fiducial3);
        
        //Set group name with default location to existing group
        blindsFeeder5.setFeederGroupName("BlindsFeederTestGroup1");
        assert(blindsFeeder5.getConnectedFeeders().contains(blindsFeeder1));
        assert(blindsFeeder1.getConnectedFeeders().contains(blindsFeeder5));
        assert(blindsFeeder5.getFiducial1Location().equals(blindsFeeder1.getFiducial1Location()));
        assert(blindsFeeder5.getFiducial2Location().equals(blindsFeeder1.getFiducial2Location()));
        assert(blindsFeeder5.getFiducial3Location().equals(blindsFeeder1.getFiducial3Location()));

        //********************************************************************************************//
        //Test assigning an existing group to a placed feeder with a non default name that has no connected feeders.
        //Create a new feeder at the third location
        BlindsFeeder blindsFeeder6 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder6);
        
        blindsFeeder6.setFiducial1Location(feederFiducals3.fiducial1);
        blindsFeeder6.setFiducial2Location(feederFiducals3.fiducial2);
        blindsFeeder6.setFiducial3Location(feederFiducals3.fiducial3);

        blindsFeeder6.setFeederGroupName("BlindsFeederTemporaryTestGroup");
        assert(blindsFeeder6.getFeederGroupName().equals("BlindsFeederTemporaryTestGroup"));
        assert(blindsFeeder6.getConnectedFeeders().size() == 1);
        
        //Set group name with default location to existing group
        blindsFeeder6.setFeederGroupName("BlindsFeederTestGroup1");
        assert(blindsFeeder6.getConnectedFeeders().contains(blindsFeeder1));
        assert(blindsFeeder1.getConnectedFeeders().contains(blindsFeeder6));
        assert(blindsFeeder6.getFiducial1Location().equals(blindsFeeder1.getFiducial1Location()));
        assert(blindsFeeder6.getFiducial2Location().equals(blindsFeeder1.getFiducial2Location()));
        assert(blindsFeeder6.getFiducial3Location().equals(blindsFeeder1.getFiducial3Location()));

        //********************************************************************************************//
        //Test that a group of more than one named feeders will not join to another group at  the same location
        BlindsFeeder blindsFeeder7 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder7);
        BlindsFeeder blindsFeeder8 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder8);

        blindsFeeder7.setFiducial1Location(feederFiducals2.fiducial1);
        blindsFeeder7.setFiducial2Location(feederFiducals2.fiducial2);
        blindsFeeder7.setFiducial3Location(feederFiducals2.fiducial3);
        
        blindsFeeder7.setFeederGroupName("BlindsFeederTestGroup2");
        blindsFeeder8.setFeederGroupName("BlindsFeederTestGroup2");
        
        assert(blindsFeeder7.getConnectedFeeders().contains(blindsFeeder8));
        assert(blindsFeeder8.getConnectedFeeders().contains(blindsFeeder7));
        assert(blindsFeeder7.getFeederGroupName().contentEquals("BlindsFeederTestGroup2"));

        assert(blindsFeeder7.getFiducial1Location().equals(feederFiducals2.fiducial1));
        assert(blindsFeeder7.getFiducial2Location().equals(feederFiducals2.fiducial2));
        assert(blindsFeeder7.getFiducial3Location().equals(feederFiducals2.fiducial3));
        
        //Try setting this feeder group name to the name of a different group.  Check that this is blocked.
        blindsFeeder7.setFeederGroupName("BlindsFeederTestGroup1");
        assert(!blindsFeeder7.getFeederGroupName().contentEquals("BlindsFeederTestGroup1"));
        assert(!blindsFeeder7.getFeederGroupName().contentEquals("BlindsFeederTestGroup1"));
        

        //********************************************************************************************//
        ///Test that a group of more than one named feeders will not join to another group at different location
        blindsFeeder7.setFiducial1Location(feederFiducals3.fiducial1);
        blindsFeeder7.setFiducial2Location(feederFiducals3.fiducial2);
        blindsFeeder7.setFiducial3Location(feederFiducals3.fiducial3);

        blindsFeeder7.setFeederGroupName("BlindsFeederTestGroup1");
        assert(!blindsFeeder7.getFeederGroupName().contentEquals("BlindsFeederTestGroup1"));
        assert(!blindsFeeder7.getFeederGroupName().contentEquals("BlindsFeederTestGroup1"));

    }
    

}
