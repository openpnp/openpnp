import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
        
        BlindsFeederTestFiducials(){
            fiducial1 = BlindsFeeder.nullLocation;
            fiducial2 = BlindsFeeder.nullLocation;
            fiducial3 = BlindsFeeder.nullLocation;
        }
    }
    
    public class BlindsFeederTestCondition {
        private BlindsFeeder testFeeder;
        private BlindsFeederTestFiducials testFiducials;
        private String testGroupName;
        private int testFeederID;
        private int testFeederNumber;
        private List<BlindsFeeder> testConnectedFeeders;
        
        BlindsFeederTestCondition(BlindsFeeder feeder, int feederID) {
            testFeeder = feeder;
            testFeederID  = feederID;
            testFeederNumber = -1;
            testFiducials = new BlindsFeederTestFiducials();
            testGroupName = BlindsFeeder.defaultGroupName;
            testConnectedFeeders = new ArrayList<BlindsFeeder>();  
        }
        
        void addConnectedFeeder(BlindsFeeder feeder){
            if (!testConnectedFeeders.contains(feeder)) {
                testConnectedFeeders.add(feeder);
            }
        }
        
        void setFiducial1(Location location) {
            testFiducials.fiducial1  = location;
        }
        
        void setFiducials(BlindsFeederTestFiducials fiducials) {
            testFiducials = fiducials;
        }

        BlindsFeederTestFiducials getFiducials() {
            return testFiducials;
        }
        
        void setGroupName(String groupName) {
            testGroupName = groupName;
        }
        
        String getGroupName() {
            return testGroupName;
        }

        void setFeederNumber(int feederNo) {
            testFeederNumber = feederNo;
        }


        public void testBlindsFeederCondition()  throws Exception {
            assert (testFeeder.getFiducial1Location().equals(testFiducials.fiducial1)) : String.format("FeederID:%d : Fiducial 1 incorrect", testFeederID);
            assert (testFeeder.getFiducial2Location().equals(testFiducials.fiducial2)) : String.format("FeederID:%d : Fiducial 2 incorrect", testFeederID);
            assert (testFeeder.getFiducial3Location().equals(testFiducials.fiducial3)) : String.format("FeederID:%d : Fiducial 3 incorrect", testFeederID);
            
            assert (testFeeder.getFeederGroupName().equals(testGroupName)) : String.format("FeederID %d : Group name incorrect", testFeederID);
            
            List<BlindsFeeder> connectedFeeders = testFeeder.getConnectedFeeders();

            assert (connectedFeeders.size() == testConnectedFeeders.size()+1) : String.format("FeederID:%d : %d connected, expected %d", testFeederID, connectedFeeders.size(), testConnectedFeeders.size()+1); 
            assert (connectedFeeders.containsAll(testConnectedFeeders)) : String.format("FeederID %d : Incorrect feeders connected", testFeederID);
            
            if (testFeederNumber >= 0) {
                assert(testFeeder.getFeederNo() == testFeederNumber) :  String.format("FeederID %d : Feeder number %d, expected %d", testFeederID, testFeeder.getFeederNo(), testFeederNumber);
            }
        }
    }
    
    public void testAllBlindsFeederConditions(List<BlindsFeederTestCondition> testConditions)  throws Exception {
        for (BlindsFeederTestCondition testCondition : testConditions) {
            try {
                testCondition.testBlindsFeederCondition();                
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.flush();
                throw(e);
            }
        }
    }
    
    public void testConnectTestConditions(List<BlindsFeederTestCondition> connectList) {
        for(BlindsFeederTestCondition testCond1 : connectList) {
            for(BlindsFeederTestCondition testCond2 : connectList) {
                if(testCond1 != testCond2) {
                    testCond1.addConnectedFeeder(testCond2.testFeeder);
                    testCond2.addConnectedFeeder(testCond1.testFeeder);
                }
            }
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
        List<BlindsFeederTestCondition> testConditions = new ArrayList<BlindsFeederTestCondition>();  
        
        BlindsFeederTestFiducials feederFiducals1 = new BlindsFeederTestFiducials(180,100, 100,100, 100,165);
//        BlindsFeederTestFiducials feederFiducals1_bad = new BlindsFeederTestFiducials(180,100, 100,100, 100,170);
        BlindsFeederTestFiducials feederFiducals2 = new BlindsFeederTestFiducials(280,100, 200,100, 200,165);
        BlindsFeederTestFiducials feederFiducals2_partial = new BlindsFeederTestFiducials(280,100, 0,0, 0,0);
        BlindsFeederTestFiducials feederFiducals3 = new BlindsFeederTestFiducials(380,100, 300,100, 300,165);

        //********************************************************************************************//
        //Test creating a new feeder
        //Create first blinds feeder and set and check its location
        BlindsFeeder blindsFeeder1 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder1);
        BlindsFeederTestCondition testConditionFeeder1 = new BlindsFeederTestCondition(blindsFeeder1, 1);
        testConditions.add(testConditionFeeder1);

        testConditionFeeder1.setFiducials(feederFiducals1);
        blindsFeeder1.setFiducial1Location(feederFiducals1.fiducial1);
        blindsFeeder1.setFiducial2Location(feederFiducals1.fiducial2);
        blindsFeeder1.setFiducial3Location(feederFiducals1.fiducial3);
        testAllBlindsFeederConditions(testConditions);
        
        //********************************************************************************************//
        //Test creating another feeder and connect it in the same location using fiducial 1 only
        BlindsFeeder blindsFeeder2 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder2);
        BlindsFeederTestCondition testConditionFeeder2 = new BlindsFeederTestCondition(blindsFeeder2, 1);
        testConditions.add(testConditionFeeder2);

        testConditionFeeder2.setFiducials(feederFiducals1);
        testConditionFeeder2.addConnectedFeeder(blindsFeeder1);
        testConditionFeeder1.addConnectedFeeder(blindsFeeder2);
        blindsFeeder2.setFiducial1Location(feederFiducals1.fiducial1);
        testAllBlindsFeederConditions(testConditions);

        //********************************************************************************************//
        //Test creating a feeder at a different location and that it does not connect to the othres
        BlindsFeeder blindsFeeder3 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder3);
        BlindsFeederTestCondition testConditionFeeder3 = new BlindsFeederTestCondition(blindsFeeder3, 1);
        testConditions.add(testConditionFeeder3);

        testConditionFeeder3.setFiducials(feederFiducals2_partial);
        //Set the feeder 3 first fiducial to location 2
        blindsFeeder3.setFiducial1Location(feederFiducals2.fiducial1);
        testAllBlindsFeederConditions(testConditions);
        
        testConditionFeeder3.setFiducials(feederFiducals2);
        //Set fiducials 2&3 and check connected feeders again.
        blindsFeeder3.setFiducial2Location(feederFiducals2.fiducial2);
        blindsFeeder3.setFiducial3Location(feederFiducals2.fiducial3);
        testAllBlindsFeederConditions(testConditions);

        //********************************************************************************************//
        //Test setting feeder pockets
        final double pocketSizeMm = 2.8;
//        final double pocketPositionMm = -1.0;
        final double pocketPitchMm = 4.0;
        final double pocketCenterline1Mm = 9.0;
        final double pocketCenterline2Mm = 19.0;

        testConditionFeeder1.setFeederNumber(2);
        testConditionFeeder2.setFeederNumber(1);
        //Set feeder1 to first position
        blindsFeeder1.setPocketCenterline(new Length(pocketCenterline1Mm, LengthUnit.Millimeters));
        blindsFeeder1.setPocketPitch(new Length(pocketPitchMm, LengthUnit.Millimeters));
        blindsFeeder1.setPocketSize(new Length(pocketSizeMm, LengthUnit.Millimeters));

        testAllBlindsFeederConditions(testConditions);
        
        testConditionFeeder1.setFeederNumber(1);
        testConditionFeeder2.setFeederNumber(2);
        //Set feeder2 to second position
        blindsFeeder2.setPocketCenterline(new Length(pocketCenterline2Mm, LengthUnit.Millimeters));
        blindsFeeder2.setPocketPitch(new Length(pocketPitchMm, LengthUnit.Millimeters));
        blindsFeeder2.setPocketSize(new Length(pocketSizeMm, LengthUnit.Millimeters));

        testAllBlindsFeederConditions(testConditions);

        //********************************************************************************************//
        //Test moving connected feeder fiducial 1 results in fiducials for conencted feeder moving relatively
        
        testConditionFeeder1.setFiducials(feederFiducals3);
        testConditionFeeder2.setFiducials(feederFiducals3);
        // Change feeder 1 fiducial 1 to location 3 and check all connected feeders are moved
        blindsFeeder1.setFiducial1Location(feederFiducals3.fiducial1);
        
        testAllBlindsFeederConditions(testConditions);
    }
    
    @Test
    public void testBlindsFeederGroups() throws Exception {
        Machine machine = Configuration.get().getMachine();
        List<Feeder> feeders = machine.getFeeders();
        List<BlindsFeederTestCondition> testConditions = new ArrayList<BlindsFeederTestCondition>();  
        List<BlindsFeederTestCondition> testConditionsGroup1 = new ArrayList<BlindsFeederTestCondition>();  

        BlindsFeederTestFiducials feederFiducals1 = new BlindsFeederTestFiducials(180,100, 100,100, 100,165);
        BlindsFeederTestFiducials feederFiducals2 = new BlindsFeederTestFiducials(280,100, 200,100, 200,165);
        BlindsFeederTestFiducials feederFiducals3 = new BlindsFeederTestFiducials(380,100, 300,100, 300,165);
        
        BlindsFeeder blindsFeeder1 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder1);
        BlindsFeederTestCondition testConditionFeeder1 = new BlindsFeederTestCondition(blindsFeeder1, 1);
        testConditions.add(testConditionFeeder1);
        testConditionsGroup1.add(testConditionFeeder1);

        testConditionFeeder1.setFiducials(feederFiducals1);
        blindsFeeder1.setFiducial1Location(feederFiducals1.fiducial1);
        blindsFeeder1.setFiducial2Location(feederFiducals1.fiducial2);
        blindsFeeder1.setFiducial3Location(feederFiducals1.fiducial3);
        
        testAllBlindsFeederConditions(testConditions);
        
        //********************************************************************************************//
        //Test conecting a second feeder using fiducial1 and that it gets the right group name 
        BlindsFeeder blindsFeeder2 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder2);
        BlindsFeederTestCondition testConditionFeeder2 = new BlindsFeederTestCondition(blindsFeeder2, 2);
        testConditions.add(testConditionFeeder2);
        testConditionsGroup1.add(testConditionFeeder2);
        
        testConnectTestConditions(testConditionsGroup1);
        testConditionFeeder2.setFiducials(feederFiducals1);
        blindsFeeder2.setFiducial1Location(feederFiducals1.fiducial1);
        testAllBlindsFeederConditions(testConditions);

        //Change group name of feeder1, check feeder 2 at same location also changes group name
        testConditionFeeder1.setGroupName("BlindsFeederTestGroup1");
        testConditionFeeder2.setGroupName("BlindsFeederTestGroup1");
        blindsFeeder1.setFeederGroupName("BlindsFeederTestGroup1");
        
        testAllBlindsFeederConditions(testConditions);
        
        //Create a new feeder at the same location
        BlindsFeeder blindsFeeder3 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder3);
        BlindsFeederTestCondition testConditionFeeder3 = new BlindsFeederTestCondition(blindsFeeder3, 3);
        testConditions.add(testConditionFeeder3);

        //Set the first fiducial and check the others are not set and it is not in the same group as above at the same location
        testConditionFeeder3.setFiducial1(feederFiducals1.fiducial1);
        blindsFeeder3.setFiducial1Location(feederFiducals1.fiducial1);
        testAllBlindsFeederConditions(testConditions);
        
        //Set the remaining fiducials to the same location and check again that it is not part of the named group
        testConditionFeeder3.setFiducials(feederFiducals1);
        blindsFeeder3.setFiducial2Location(feederFiducals1.fiducial2);
        blindsFeeder3.setFiducial3Location(feederFiducals1.fiducial3);

        testAllBlindsFeederConditions(testConditions);
        
        testConditionFeeder3.setFiducials(feederFiducals2);
        //Move feeder 3 to second fiducials and check that it has moved and that the non connected have not.
        blindsFeeder3.setFiducial1Location(feederFiducals2.fiducial1);
        blindsFeeder3.setFiducial2Location(feederFiducals2.fiducial2);
        blindsFeeder3.setFiducial3Location(feederFiducals2.fiducial3);
        
        testAllBlindsFeederConditions(testConditions);

        //********************************************************************************************//
        //Test assigning an existing group to a feeder at default location.
        
        //Create a new feeder at the first location with default group name
        BlindsFeeder blindsFeeder4 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder4);
        BlindsFeederTestCondition testConditionFeeder4 = new BlindsFeederTestCondition(blindsFeeder4, 4);
        testConditions.add(testConditionFeeder4);

        testConditionsGroup1.add(testConditionFeeder4);
        testConnectTestConditions(testConditionsGroup1);
        testConditionFeeder4.setFiducials(feederFiducals1);
        testConditionFeeder4.setGroupName("BlindsFeederTestGroup1");
        blindsFeeder4.setFeederGroupName("BlindsFeederTestGroup1");
        
        testAllBlindsFeederConditions(testConditions);
        
        //********************************************************************************************//
        //Test assigning an existing group to a placed feeder that has no connected feeders.
        //Create a new feeder at the third location
        BlindsFeeder blindsFeeder5 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder5);
        BlindsFeederTestCondition testConditionFeeder5 = new BlindsFeederTestCondition(blindsFeeder5, 5);
        testConditions.add(testConditionFeeder5);
        
        testConditionFeeder5.setFiducials(feederFiducals3);
        blindsFeeder5.setFiducial1Location(feederFiducals3.fiducial1);
        blindsFeeder5.setFiducial2Location(feederFiducals3.fiducial2);
        blindsFeeder5.setFiducial3Location(feederFiducals3.fiducial3);
        testAllBlindsFeederConditions(testConditions);

        //Set group name with default location to existing group
        testConditionsGroup1.add(testConditionFeeder5);
        testConnectTestConditions(testConditionsGroup1);
        testConditionFeeder5.setFiducials(feederFiducals1);
        testConditionFeeder5.setGroupName("BlindsFeederTestGroup1");
        blindsFeeder5.setFeederGroupName("BlindsFeederTestGroup1");
        testAllBlindsFeederConditions(testConditions);
        
        //********************************************************************************************//
        //Test assigning an existing group to a placed feeder with a non default name that has no connected feeders.
        //Create a new feeder at the third location
        BlindsFeeder blindsFeeder6 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder6);
        BlindsFeederTestCondition testConditionFeeder6 = new BlindsFeederTestCondition(blindsFeeder6, 6);
        testConditions.add(testConditionFeeder6);

        testConditionFeeder6.setFiducials(feederFiducals3);
        blindsFeeder6.setFiducial1Location(feederFiducals3.fiducial1);
        blindsFeeder6.setFiducial2Location(feederFiducals3.fiducial2);
        blindsFeeder6.setFiducial3Location(feederFiducals3.fiducial3);

        testConditionFeeder6.setGroupName("BlindsFeederTemporaryTestGroup");
        blindsFeeder6.setFeederGroupName("BlindsFeederTemporaryTestGroup");
        testAllBlindsFeederConditions(testConditions);
        
        assert(blindsFeeder6.getFeederGroupName().equals("BlindsFeederTemporaryTestGroup"));
        assert(blindsFeeder6.getConnectedFeeders().size() == 1);
        
        //Set group name with default location to existing group
        testConditionsGroup1.add(testConditionFeeder6);
        testConnectTestConditions(testConditionsGroup1);
        testConditionFeeder6.setFiducials(feederFiducals1);
        testConditionFeeder6.setGroupName("BlindsFeederTestGroup1");
        blindsFeeder6.setFeederGroupName("BlindsFeederTestGroup1");
        
        testAllBlindsFeederConditions(testConditions);
        
        //********************************************************************************************//
        //Test that a group of more than one named feeders will not join to another group at  the same location
        BlindsFeeder blindsFeeder7 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder7);
        BlindsFeederTestCondition testConditionFeeder7 = new BlindsFeederTestCondition(blindsFeeder7, 7);
        testConditions.add(testConditionFeeder7);
        
        BlindsFeeder blindsFeeder8 = new BlindsFeeder();
        machine.addFeeder(blindsFeeder8);
        BlindsFeederTestCondition testConditionFeeder8 = new BlindsFeederTestCondition(blindsFeeder8, 8);
        testConditions.add(testConditionFeeder8);

        testConditionFeeder7.setFiducials(feederFiducals2);
        testConditionFeeder8.setFiducials(feederFiducals2);
        blindsFeeder7.setFiducial1Location(feederFiducals2.fiducial1);
        blindsFeeder7.setFiducial2Location(feederFiducals2.fiducial2);
        blindsFeeder7.setFiducial3Location(feederFiducals2.fiducial3);
        
        testConditionFeeder7.setGroupName("BlindsFeederTestGroup2");
        testConditionFeeder8.setGroupName("BlindsFeederTestGroup2");
        blindsFeeder7.setFeederGroupName("BlindsFeederTestGroup2");
        blindsFeeder8.setFeederGroupName("BlindsFeederTestGroup2");
        
        testAllBlindsFeederConditions(testConditions);
        
        //Try setting this feeder group name to the name of a different group.  Check that this is blocked.
        blindsFeeder7.setFeederGroupName("BlindsFeederTestGroup1");
        testAllBlindsFeederConditions(testConditions);
        

        //********************************************************************************************//
        ///Test that a group of more than one named feeders will not join to another group at different location
        testConditionFeeder7.setFiducials(feederFiducals3);
        testConditionFeeder8.setFiducials(feederFiducals3);
        blindsFeeder7.setFiducial1Location(feederFiducals3.fiducial1);
        blindsFeeder7.setFiducial2Location(feederFiducals3.fiducial2);
        blindsFeeder7.setFiducial3Location(feederFiducals3.fiducial3);

        blindsFeeder7.setFeederGroupName("BlindsFeederTestGroup1");
        testAllBlindsFeederConditions(testConditions);

    }
    

}
