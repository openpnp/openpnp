package org.openpnp.machine.index;

import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;

import java.io.File;

import static org.mockito.Mockito.*;
import static org.openpnp.machine.index.protocol.IndexCommands.*;
import static org.openpnp.machine.index.protocol.IndexResponses.*;

public class IndexFeederTest {
    private final String hardwareId = "00112233445566778899AABB";
    private final int feederAddress = 5;

    private IndexFeeder feeder;

    private Machine machine;
    private Actuator mockedActuator;
    private Nozzle mockedNozzle;

    @Before
    public void setUp() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);
        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        machine = Configuration.get().getMachine();
        feeder = new IndexFeeder();
        machine.addFeeder(feeder);

        mockedActuator = Mockito.mock(Actuator.class);
        when(mockedActuator.getName()).thenReturn(IndexFeeder.ACTUATOR_NAME);
        machine.addActuator(mockedActuator);

        mockedNozzle = Mockito.mock(Nozzle.class);
        when(mockedNozzle.getName()).thenReturn("Test Nozzle");
    }

    @Test
    public void getJobPreparationLocationReturnsNull() {
        Assert.assertNull(feeder.getJobPreparationLocation());
    }

    @Test
    public void getSlotAddressReturnsNullByDefault() {
        Assert.assertNull(feeder.getSlotAddress());
    }
    
    @Test
    public void isEnabledReturnsFalseIfSetEnabledToFalse() {
        feeder.setEnabled(false);

        Assert.assertFalse(feeder.isEnabled());
    }

    @Test
    public void isEnabledReturnsFalseIfNoHardwareIdSet() {
        feeder.setEnabled(true);

        Assert.assertNull(feeder.getHardwareId());
        Assert.assertFalse(feeder.isEnabled());
    }

    @Test
    public void isEnabledReturnsFalseIfNoPartIsSet() {
        feeder.setEnabled(true);
        feeder.setHardwareId(hardwareId);

        Assert.assertNull(feeder.getPart());
        Assert.assertFalse(feeder.isEnabled());
    }

    @Test
    public void isEnabledReturnsTrueIfEverythingIsSet() {
        feeder.setEnabled(true);
        feeder.setHardwareId(hardwareId);
        feeder.setPart(new Part("test-part"));

        Assert.assertTrue(feeder.isEnabled());
    }

    @Test
    public void isInitializedByDefaultReturnsTrue() {
        Assert.assertFalse(feeder.isInitialized());
    }

    @Test
    public void prepareForJobFindsFeederAddressAndInitializes() throws Exception {
        feeder.setHardwareId(hardwareId);

        String getFeederAddressCommand = getFeederAddress(hardwareId);
        when(mockedActuator.read(getFeederAddressCommand))
                .thenReturn(GetFeederAddress.ok(feederAddress, hardwareId));

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(feederAddress));

        feeder.prepareForJob(false);

        InOrder inOrder = Mockito.inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(getFeederAddressCommand);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);

        Assert.assertEquals(feederAddress, (int) feeder.getSlotAddress());
        Assert.assertTrue(feeder.isInitialized());
    }
    
    @Test
    public void prepareForJobInitializesIfSlotAddressIsSet() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(feederAddress);

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(feederAddress));

        feeder.prepareForJob(false);

        InOrder inOrder = Mockito.inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);

        Assert.assertEquals(feederAddress, (int) feeder.getSlotAddress());
        Assert.assertTrue(feeder.isInitialized());
    }

    @Test
    public void prepareForJobFindsFeederAgainIfLostToTimeout() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(feederAddress);

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(Errors.timeout());

        int newAddress = 11;

        String newGetFeederAddressCommand = getFeederAddress(hardwareId);
        when(mockedActuator.read(newGetFeederAddressCommand))
                .thenReturn(GetFeederAddress.ok(newAddress, hardwareId));

        String newInitializeFeederCommand = initializeFeeder(newAddress, hardwareId);
        when(mockedActuator.read(newInitializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(newAddress));

        feeder.prepareForJob(false);

        InOrder inOrder = Mockito.inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator).read(newGetFeederAddressCommand);
        inOrder.verify(mockedActuator).read(newInitializeFeederCommand);

        Assert.assertEquals(newAddress, (int) feeder.getSlotAddress());
        Assert.assertTrue(feeder.isInitialized());
    }

    @Test
    public void prepareForJobFindsFeederAgainIfWrongFeederUUIDAndMakesNewFeeder() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(feederAddress);
        String otherHardwareId = "445566778899AABBCCDDEEFF";

        Assert.assertNull(IndexFeeder.findByHardwareId(otherHardwareId));

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(Errors.wrongFeederUUID(feederAddress, otherHardwareId));

        int newAddress = 11;

        String newGetFeederAddressCommand = getFeederAddress(hardwareId);
        when(mockedActuator.read(newGetFeederAddressCommand))
                .thenReturn(GetFeederAddress.ok(newAddress, hardwareId));

        String newInitializeFeederCommand = initializeFeeder(newAddress, hardwareId);
        when(mockedActuator.read(newInitializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(newAddress));

        feeder.prepareForJob(false);

        InOrder inOrder = Mockito.inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator).read(newGetFeederAddressCommand);
        inOrder.verify(mockedActuator).read(newInitializeFeederCommand);

        Assert.assertEquals(newAddress, (int) feeder.getSlotAddress());
        Assert.assertTrue(feeder.isInitialized());

        IndexFeeder otherFeeder = IndexFeeder.findByHardwareId(otherHardwareId);
        Assert.assertNotNull(otherFeeder);
        Assert.assertFalse(otherFeeder.initialized);
        Assert.assertEquals(feederAddress, (int) otherFeeder.slotAddress);
    }

    @Test
    public void prepareForJobFindsFeederAgainIfWrongFeederUUIDAndUsesExistingFeeder() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(feederAddress);

        String otherHardwareId = "445566778899AABBCCDDEEFF";
        IndexFeeder otherFeeder = new IndexFeeder();
        otherFeeder.setHardwareId(otherHardwareId);
        machine.addFeeder(otherFeeder);

        Assert.assertSame(otherFeeder, IndexFeeder.findByHardwareId(otherHardwareId));

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(Errors.wrongFeederUUID(feederAddress, otherHardwareId));

        int newAddress = 11;

        String newGetFeederAddressCommand = getFeederAddress(hardwareId);
        when(mockedActuator.read(newGetFeederAddressCommand))
                .thenReturn(GetFeederAddress.ok(newAddress, hardwareId));

        String newInitializeFeederCommand = initializeFeeder(newAddress, hardwareId);
        when(mockedActuator.read(newInitializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(newAddress));

        feeder.prepareForJob(false);

        InOrder inOrder = Mockito.inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator).read(newGetFeederAddressCommand);
        inOrder.verify(mockedActuator).read(newInitializeFeederCommand);

        Assert.assertEquals(newAddress, (int) feeder.getSlotAddress());
        Assert.assertTrue(feeder.isInitialized());

        IndexFeeder recalledOtherFeeder = IndexFeeder.findByHardwareId(otherHardwareId);
        Assert.assertSame(otherFeeder, recalledOtherFeeder);
        Assert.assertFalse(otherFeeder.initialized);
        Assert.assertEquals(feederAddress, (int) otherFeeder.slotAddress);
    }
    
    @Test
    public void feedMovesPartForwardByPitch() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(feederAddress);
        feeder.setPartPitch(2);

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(feederAddress));

        String moveFeedForwardCommand = moveFeedForward(feederAddress, 20);
        when(mockedActuator.read(moveFeedForwardCommand))
                .thenReturn(MoveFeedForward.ok(feederAddress));

        feeder.feed(mockedNozzle);

        InOrder inOrder = Mockito.inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator).read(moveFeedForwardCommand);
    }
    
    @Test
    public void feedInitializesIfUninitializedErrorIsReturned() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(feederAddress);
        feeder.setPartPitch(2);

        String oldInitializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(oldInitializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(feederAddress));

        String oldMoveFeederForwardCommand = moveFeedForward(feederAddress, 20);
        when(mockedActuator.read(oldMoveFeederForwardCommand))
                .thenReturn(Errors.uninitializedFeeder(feederAddress, hardwareId));

        int newAddress = 11;

        String newGetFeederAddressCommand = getFeederAddress(hardwareId);
        when(mockedActuator.read(newGetFeederAddressCommand))
                .thenReturn(GetFeederAddress.ok(newAddress, hardwareId));

        String newInitializeFeederCommand = initializeFeeder(newAddress, hardwareId);
        when(mockedActuator.read(newInitializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(newAddress));

        String newMoveFeedForwardCommand = moveFeedForward(newAddress, 20);
        when(mockedActuator.read(newMoveFeedForwardCommand))
                .thenReturn(MoveFeedForward.ok(newAddress));

        feeder.feed(mockedNozzle);

        InOrder inOrder = Mockito.inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(oldInitializeFeederCommand); // First initialization
        inOrder.verify(mockedActuator).read(oldMoveFeederForwardCommand); // Uninitialized feeder error
        inOrder.verify(mockedActuator).read(newGetFeederAddressCommand); // New address
        inOrder.verify(mockedActuator).read(newInitializeFeederCommand); // Second initialization in new slot
        inOrder.verify(mockedActuator).read(newMoveFeedForwardCommand); // Finally move the feeder

        Assert.assertEquals(newAddress, (int) feeder.getSlotAddress());
    }

    @Test
    public void feedInitializesOnFeederTimeout() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(feederAddress);
        feeder.setPartPitch(2);

        String oldInitializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(oldInitializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(feederAddress));

        String oldMoveFeederForwardCommand = moveFeedForward(feederAddress, 20);
        when(mockedActuator.read(oldMoveFeederForwardCommand))
                .thenReturn(Errors.timeout());

        int newAddress = 11;

        String newGetFeederAddressCommand = getFeederAddress(hardwareId);
        when(mockedActuator.read(newGetFeederAddressCommand))
                .thenReturn(GetFeederAddress.ok(newAddress, hardwareId));

        String newInitializeFeederCommand = initializeFeeder(newAddress, hardwareId);
        when(mockedActuator.read(newInitializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(newAddress));

        String newMoveFeedForwardCommand = moveFeedForward(newAddress, 20);
        when(mockedActuator.read(newMoveFeedForwardCommand))
                .thenReturn(MoveFeedForward.ok(newAddress));

        feeder.feed(mockedNozzle);

        InOrder inOrder = Mockito.inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(oldInitializeFeederCommand); // First initialization
        inOrder.verify(mockedActuator).read(oldMoveFeederForwardCommand); // Uninitialized feeder error
        inOrder.verify(mockedActuator).read(newGetFeederAddressCommand); // New address
        inOrder.verify(mockedActuator).read(newInitializeFeederCommand); // Second initialization in new slot
        inOrder.verify(mockedActuator).read(newMoveFeedForwardCommand); // Finally move the feeder

        Assert.assertEquals(newAddress, (int) feeder.getSlotAddress());
    }

    @Test
    public void twoFeedersCanNotHaveTheSameAddress() throws Exception {
        // Remove the main feeder so we can make two of our own in this test
        machine.removeFeeder(feeder);

        // Feeder A -> Slot 1
        IndexFeeder feederA = new IndexFeeder();
        String hardwareIdA = "445566778899AABBCCDDEEFF";
        feederA.setHardwareId(hardwareIdA);
        feederA.setSlotAddress(1);
        machine.addFeeder(feederA);

        // Feeder B -> Slot 2
        IndexFeeder feederB = new IndexFeeder();
        String hardwareIdB = "FFEEDDCCBBAA998877665544";
        feederB.setHardwareId(hardwareIdB);
        feederB.setSlotAddress(2);
        feederB.setPartPitch(2);
        machine.addFeeder(feederB);

        // Both feeders initialized in their known slot
        String feederASlot1InitializationCommand = initializeFeeder(1, hardwareIdA);
        when(mockedActuator.read(feederASlot1InitializationCommand))
                .thenReturn(InitializeFeeder.ok(1));

        String feederBSlot2InitializationCommand = initializeFeeder(2, hardwareIdB);
        when(mockedActuator.read(feederBSlot2InitializationCommand))
                .thenReturn(InitializeFeeder.ok(2));

        // Prepare both feeders for the job
        feederA.prepareForJob(false);
        feederB.prepareForJob(false);

        // At this point, in the real world, the job would have been started. Feeder A is removed and feeder B is put
        // into slot 1. This causes the next move command to timeout for feeder B. We don't need to be running a job in
        // this unit test, we just need to call move manually below.

        String feederBSlot2FeedCommand = moveFeedForward(2, 20);
        when(mockedActuator.read(feederBSlot2FeedCommand))
                .thenReturn(Errors.timeout());

        // Feeder B is now in slot 1
        String feederBGetAddressCommand = getFeederAddress(hardwareIdB);
        when(mockedActuator.read(feederBGetAddressCommand))
                .thenReturn(GetFeederAddress.ok(1, hardwareIdB));

        String feederBSlot1InitializationCommand = initializeFeeder(1, hardwareIdB);
        when(mockedActuator.read(feederBSlot1InitializationCommand))
                .thenReturn(InitializeFeeder.ok(1));

        // We can finally try feeding in the correct slot!
        String feederBSlot1FeedCommand = moveFeedForward(1, 20);
        when(mockedActuator.read(feederBSlot1FeedCommand))
                .thenReturn(MoveFeedForward.ok(1));

        // Actually try to feed on feeder B
        feederB.feed(mockedNozzle);

        // Verify all of the calls in order
        InOrder inOrder = Mockito.inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(feederASlot1InitializationCommand); // First initialization Feeder A
        inOrder.verify(mockedActuator).read(feederBSlot2InitializationCommand); // First initialization Feeder B
        inOrder.verify(mockedActuator).read(feederBSlot2FeedCommand); // Feeder B timeout
        inOrder.verify(mockedActuator).read(feederBGetAddressCommand); // Find feeder B slot
        inOrder.verify(mockedActuator).read(feederBSlot1InitializationCommand); // Initialize feeder B
        inOrder.verify(mockedActuator).read(feederBSlot1FeedCommand); // Finally move the feeder

        // Verify the state of the two feeders
        Assert.assertFalse(feederA.isInitialized());
        Assert.assertNull(feeder.getSlotAddress());

        Assert.assertTrue(feederB.isInitialized());
        Assert.assertEquals(1, (int) feederB.getSlotAddress());
    }

    /*
    TODO More tests:
    5. Find slot Address times out.
     */
}
