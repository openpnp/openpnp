package org.openpnp.machine.index;

import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.openpnp.machine.index.sheets.FeederPropertySheet;
import org.openpnp.machine.index.sheets.SearchPropertySheet;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;

import java.io.File;
import java.util.function.IntConsumer;

import static org.junit.Assert.*;
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
    private IndexProperties indexProperties;

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
        when(mockedActuator.getName()).thenReturn(IndexFeeder.ACTUATOR_DATA_NAME);
        machine.addActuator(mockedActuator);

        mockedNozzle = Mockito.mock(Nozzle.class);
        when(mockedNozzle.getName()).thenReturn("Test Nozzle");

        indexProperties = new IndexProperties(machine);
    }

    @Test
    public void getJobPreparationLocationReturnsNull() {
        Assert.assertNull(feeder.getJobPreparationLocation());
    }

    @Test
    public void getDataActuatorCreatesReferenceActuatorIfOneDoesNotExist() {
        machine.removeActuator(mockedActuator); // Start by removing the one we added for every other test

        Actuator actuator = IndexFeeder.getDataActuator();
        assertNotNull(actuator);
        assertTrue(actuator instanceof ReferenceActuator);
        assertEquals(IndexFeeder.ACTUATOR_DATA_NAME, actuator.getName());

        Actuator machineActuator = machine.getActuatorByName(IndexFeeder.ACTUATOR_DATA_NAME);
        assertSame(actuator, machineActuator);
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
    public void getNameByDefaultReturnsClassSimpleName() {
        assertEquals(
                "Unconfigured IndexFeeder",
                feeder.getName()
        );
    }

    @Test
    public void getNameUsesHardwareIdWhenThatIsSet() {
        feeder.setHardwareId(hardwareId);
        assertEquals(
                String.format("%s (Slot: None)", hardwareId),
                feeder.getName()
        );
    }

    @Test
    public void getNameUsesHardwareIdAndSlotWhenBothAreSet() {
        int slot = 27;
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(slot);
        assertEquals(
                String.format("%s (Slot: %s)", hardwareId, slot),
                feeder.getName()
        );
    }

    @Test
    public void setHardwareIdOverridesNameOnlyIfItIsNotAlreadySet() {
        feeder.setName("My Name");
        feeder.setHardwareId(hardwareId);

        assertEquals("My Name (Slot: None)", feeder.getName());
    }

    @Test
    public void setNameWorksWithoutSlotIncluded() {
        String name = "Some Test Name";
        feeder.setHardwareId(hardwareId);
        feeder.setName(name);

        assertEquals(
                String.format("%s (Slot: None)", name),
                feeder.getName()
        );
    }

    @Test
    public void setNameWorksWithSlotIncluded() {
        int slot = 13;
        String name = "Test Name";
        String nameWithSlot = String.format("%s (Slot: %s)", name, slot);
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(slot);
        feeder.setName(nameWithSlot);

        assertEquals(nameWithSlot, feeder.getName());
    }

    @Test
    public void setNameWorksWithOverridingNoneSlot() {
        int slot = 13;
        String name = "Test Name";
        String nameWithNoneSlot = String.format("%s (Slot: None)", name);
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(slot);
        feeder.setName(nameWithNoneSlot);

        String nameWithSlot = String.format("%s (Slot: %s)", name, slot);
        assertEquals(nameWithSlot, feeder.getName());
    }

    @Test
    public void setNameWorksWithUnexpectedSlotNumber() {
        String name = "Test Name";
        String nameWithOldSlot = String.format("%s (Slot: 13)", name);

        int newSlot = 3;
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(newSlot);
        feeder.setName(nameWithOldSlot);

        String nameWithNewSlot = String.format("%s (Slot: %s)", name, newSlot);
        assertEquals(nameWithNewSlot, feeder.getName());
    }

    @Test
    public void setNameWithMalformedSlotKeepsMalformedSlot() {
        String name = "Test Name (Slot: No";
        feeder.setHardwareId(hardwareId);
        feeder.setName(name);

        assertEquals(
                String.format("%s (Slot: None)", name),
                feeder.getName()
        );
    }

    @Test
    public void setNameCorrectlyTrimsInputName() {
        String name = "Test Name ";
        feeder.setHardwareId(hardwareId);
        feeder.setName(name);

        assertEquals(
                "Test Name (Slot: None)",
                feeder.getName()
        );
    }

    @Test
    public void setNameWithMultipleRandomSlots() {
        feeder.setHardwareId(hardwareId);
        feeder.setName("This (Slot: 1) Is (Slot: 2) A (Slot: 3) Weird (Slot: None) Test");

        /*
        We don't trim the spaces internally when a slot is removed. This is more or less intentional to keep everything
        simpler.
         */
        assertEquals("This  Is  A  Weird  Test (Slot: None)", feeder.getName());
    }

    @Test
    public void isInitializedByDefaultReturnsFalse() {
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
    public void prepareForJobDoesNotInitializeIfSlotCanNotBeFound() throws Exception {
        feeder.setHardwareId(hardwareId);

        String getFeederAddressCommand = getFeederAddress(hardwareId);
        when(mockedActuator.read(getFeederAddressCommand))
                .thenReturn(Errors.timeout());

        try {
            feeder.prepareForJob(false);
            fail("prepareForJob did not throw exception after max retries");
        } catch (Exception exception) {
            assertEquals("Failed to find and initialize the feeder", exception.getMessage());
        }

        Assert.assertFalse(feeder.isInitialized());
        Assert.assertNull(feeder.getSlotAddress());
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
    public void prepareForJobThrowsExceptionAfterOneRetry() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(feederAddress);
        indexProperties.setFeederCommunicationMaxRetry(1);

        int newAddress = 11;

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(Errors.wrongFeederUUID(newAddress, hardwareId));

        String newInitializeFeederCommand = initializeFeeder(newAddress, hardwareId);
        when(mockedActuator.read(newInitializeFeederCommand))
                .thenReturn(Errors.wrongFeederUUID(newAddress, hardwareId));

        try {
            feeder.prepareForJob(false);
            fail("prepareForJob did not throw exception after max retries");
        } catch (Exception exception) {
            assertEquals("Failed to find and initialize the feeder", exception.getMessage());
        }

        InOrder inOrder = Mockito.inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator).read(newInitializeFeederCommand);
        inOrder.verify(mockedActuator, never()).read(any());
    }

    @Test
    public void prepareForJobThrowsExceptionAfterNoRetries() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(feederAddress);
        indexProperties.setFeederCommunicationMaxRetry(0);

        int newAddress = 11;

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(Errors.wrongFeederUUID(newAddress, hardwareId));

        try {
            feeder.prepareForJob(false);
            fail("prepareForJob did not throw exception after max retries");
        } catch (Exception exception) {
            assertEquals("Failed to find and initialize the feeder", exception.getMessage());
        }

        InOrder inOrder = Mockito.inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator, never()).read(any());
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
    public void feedThrowsExceptionAfterOneRetry() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(feederAddress);
        feeder.setPartPitch(2);
        indexProperties.setFeederCommunicationMaxRetry(1);

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(feederAddress));

        String getFeederAddressCommand = getFeederAddress(hardwareId);
        when(mockedActuator.read(getFeederAddressCommand))
                .thenReturn(GetFeederAddress.ok(feederAddress, hardwareId));

        String moveFeederForwardCommand = moveFeedForward(feederAddress, 20);
        when(mockedActuator.read(moveFeederForwardCommand))
                .thenReturn(Errors.timeout());

        try {
            feeder.feed(mockedNozzle);
            fail("feed did not throw exception after max retries");
        } catch (Exception exception) {
            assertEquals("Failed to feed", exception.getMessage());
        }

        InOrder inOrder = Mockito.inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator).read(moveFeederForwardCommand);
        inOrder.verify(mockedActuator).read(getFeederAddressCommand);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator).read(moveFeederForwardCommand);
        inOrder.verify(mockedActuator, never()).read(any());
    }

    @Test
    public void feedThrowsExceptionAfterNoRetries() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(feederAddress);
        feeder.setPartPitch(2);
        indexProperties.setFeederCommunicationMaxRetry(0);

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(feederAddress));

        String getFeederAddressCommand = getFeederAddress(hardwareId);
        when(mockedActuator.read(getFeederAddressCommand))
                .thenReturn(GetFeederAddress.ok(feederAddress, hardwareId));

        String moveFeederForwardCommand = moveFeedForward(feederAddress, 20);
        when(mockedActuator.read(moveFeederForwardCommand))
                .thenReturn(Errors.timeout());

        try {
            feeder.feed(mockedNozzle);
            fail("feed did not throw exception after max retries");
        } catch (Exception exception) {
            assertEquals("Failed to feed", exception.getMessage());
        }

        InOrder inOrder = Mockito.inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator).read(moveFeederForwardCommand);
        inOrder.verify(mockedActuator, never()).read(any());
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

    @Test
    public void findAllFeedersUsingMaxFeederAddress() throws Exception {
        int maxFeederAddress = 5;
        indexProperties.setMaxFeederAddress(maxFeederAddress);

        for (int i = 1; i <= maxFeederAddress; i++) {
            when(mockedActuator.read(getFeederId(i)))
                    .thenReturn(Errors.timeout());
        }

        IndexFeeder.findAllFeeders(null);

        InOrder inOrder = Mockito.inOrder(mockedActuator);
        for (int i = 1; i <= maxFeederAddress; i++) {
            inOrder.verify(mockedActuator).read(getFeederId(i));
        }
        inOrder.verify(mockedActuator, never()).read(any());
    }

    @Test
    public void findAllFeedersFindsNewAndExistingFeeders() throws Exception {
        int maxFeederAddress = 5;
        indexProperties.setMaxFeederAddress(maxFeederAddress);

        /*
        - Existing feeder has address 1 internally, but responds on address 2
        - New feeder responds on address 1
        - Address 3-5 responds with timeout
         */

        String newHardwareUuid = "FFEEDDCCBBAA998877665544";
        feeder.setName(hardwareId);
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(1);

        when(mockedActuator.read(getFeederId(1)))
                .thenReturn(GetFeederId.ok(1, newHardwareUuid));

        when(mockedActuator.read(getFeederId(2)))
                .thenReturn(GetFeederId.ok(2, hardwareId));

        for (int i = 3; i <= maxFeederAddress; i++) {
            when(mockedActuator.read(getFeederId(i)))
                    .thenReturn(Errors.timeout());
        }

        IndexFeeder.findAllFeeders(null);

        InOrder inOrder = Mockito.inOrder(mockedActuator);
        for (int i = 1; i <= maxFeederAddress; i++) {
            inOrder.verify(mockedActuator).read(getFeederId(i));
        }
        inOrder.verify(mockedActuator, never()).read(any());

        assertEquals(2, (int) feeder.getSlotAddress());
        assertEquals(
                String.format("%s (Slot: %s)", hardwareId, 2),
                feeder.getName()
        );

        IndexFeeder newFeeder = IndexFeeder.findByHardwareId(newHardwareUuid);
        assertNotNull(newFeeder);
        assertEquals(1, (int) newFeeder.getSlotAddress());
        assertEquals(newHardwareUuid, newFeeder.getHardwareId());
        assertEquals(
                String.format("%s (Slot: %s)", newHardwareUuid, 1),
                newFeeder.getName()
        );
    }

    @Test
    public void findAllFeedersFillsNullHardwareIdFeedersBeforeCreatingNewOnes() throws Exception {
        int maxFeederAddress = 2;
        indexProperties.setMaxFeederAddress(maxFeederAddress);

        /*
        - Existing feeder has no hardware id, should be filled with hardwareId
        - New feeder responds on address 2
        - Address 3-5 responds with timeout
         */

        String newHardwareUuid = "FFEEDDCCBBAA998877665544";

        when(mockedActuator.read(getFeederId(1)))
                .thenReturn(GetFeederId.ok(1, hardwareId));

        when(mockedActuator.read(getFeederId(2)))
                .thenReturn(GetFeederId.ok(2, newHardwareUuid));

        IndexFeeder.findAllFeeders(null);

        InOrder inOrder = Mockito.inOrder(mockedActuator);
        for (int i = 1; i <= maxFeederAddress; i++) {
            inOrder.verify(mockedActuator).read(getFeederId(i));
        }
        inOrder.verify(mockedActuator, never()).read(any());

        assertEquals(1, (int) feeder.getSlotAddress());
        assertEquals(hardwareId, feeder.getHardwareId());
        assertEquals(
                String.format("%s (Slot: %s)", hardwareId, 1),
                feeder.getName()
        );

        IndexFeeder newFeeder = IndexFeeder.findByHardwareId(newHardwareUuid);
        assertNotNull(newFeeder);
        assertEquals(2, (int) newFeeder.getSlotAddress());
        assertEquals(newHardwareUuid, newFeeder.getHardwareId());
        assertEquals(
                String.format("%s (Slot: %s)", newHardwareUuid, 2),
                newFeeder.getName()
        );
    }

    @Test
    public void findAllFeedersRemovesFeederAddressIfTimeoutOccurs() throws Exception {
        int maxFeederAddress = 5;
        indexProperties.setMaxFeederAddress(maxFeederAddress);

        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(1);
        for (int i = 1; i <= maxFeederAddress; i++) {
            when(mockedActuator.read(getFeederId(i)))
                    .thenReturn(Errors.timeout());
        }

        IndexFeeder.findAllFeeders(null);

        InOrder inOrder = Mockito.inOrder(mockedActuator);
        for (int i = 1; i <= maxFeederAddress; i++) {
            inOrder.verify(mockedActuator).read(getFeederId(i));
        }
        inOrder.verify(mockedActuator, never()).read(any());

        assertNull(feeder.getSlotAddress());
    }

    @Test
    public void findAllFeedersGivesProgressUpdates() throws Exception {
        int maxFeederAddress = 5;
        indexProperties.setMaxFeederAddress(maxFeederAddress);

        for (int i = 1; i <= maxFeederAddress; i++) {
            when(mockedActuator.read(getFeederId(i)))
                    .thenReturn(Errors.timeout());
        }

        IntConsumer progressUpdates = mock(IntConsumer.class);

        IndexFeeder.findAllFeeders(progressUpdates);

        InOrder inOrder = Mockito.inOrder(mockedActuator, progressUpdates);
        for (int i = 1; i <= maxFeederAddress; i++) {
            inOrder.verify(mockedActuator).read(getFeederId(i));
            inOrder.verify(progressUpdates).accept((i * 100) / maxFeederAddress);
        }
        inOrder.verify(mockedActuator, never()).read(any());
    }

    @Test
    public void getPropertySheetHolderTitleDefault() {
        assertEquals("Unconfigured IndexFeeder", feeder.getPropertySheetHolderTitle());
    }

    @Test
    public void getPropertySheetHolderTitleUsesHardwareIdNameIfConfigured() {
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(15);

        assertEquals(
                String.format("IndexFeeder %s", feeder.getName()),
                feeder.getPropertySheetHolderTitle()
        );
    }

    @Test
    public void getPropertySheetsOnlyReturnsSearchWithNullHardwareId() {
        PropertySheetHolder.PropertySheet[] sheets = feeder.getPropertySheets();

        assertEquals(1, sheets.length);
        assertTrue(sheets[0] instanceof SearchPropertySheet);
    }

    @Test
    public void getPropertySheetsAlsoReturnsFeederConfigurationWithHardwareIdSet() {
        feeder.setHardwareId(hardwareId);

        PropertySheetHolder.PropertySheet[] sheets = feeder.getPropertySheets();

        assertEquals(2, sheets.length);
        assertTrue(sheets[0] instanceof FeederPropertySheet);
        assertTrue(sheets[1] instanceof SearchPropertySheet);
    }
}
