package org.openpnp.machine.index;

import com.google.common.io.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.openpnp.machine.index.exceptions.FeedFailureException;
import org.openpnp.machine.index.exceptions.FeederHasNoLocationOffsetException;
import org.openpnp.machine.index.exceptions.NoSlotAddressException;
import org.openpnp.machine.index.exceptions.UnconfiguredSlotException;
import org.openpnp.machine.index.sheets.FeederPropertySheet;
import org.openpnp.machine.index.sheets.SearchPropertySheet;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.model.*;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.*;
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
    private Location baseLocation;
    private Location feederOffset;

    @BeforeEach
    public void setUp() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);
        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        machine = Configuration.get().getMachine();
        feeder = new IndexFeeder();
        machine.addFeeder(feeder);

        // First remove the Reference Actuator that was made when we created the IndexFeeder
        machine.removeActuator(machine.getActuatorByName(IndexFeeder.ACTUATOR_DATA_NAME));

        // Then make a fake one for us to mock with
        mockedActuator = mock(Actuator.class);
        when(mockedActuator.getName()).thenReturn(IndexFeeder.ACTUATOR_DATA_NAME);
        machine.addActuator(mockedActuator);

        mockedNozzle = mock(Nozzle.class);
        when(mockedNozzle.getName()).thenReturn("Test Nozzle");

        indexProperties = new IndexProperties(machine);

        baseLocation = new Location(LengthUnit.Millimeters, 1, 2, 3, 0);
        feederOffset = new Location(LengthUnit.Millimeters, 1, 1, 0, 45);
    }

    private void setSlotLocation(int address, Location location) {
        indexProperties.getFeederSlots().getSlot(address).setLocation(location);
    }

    @Test
    public void getJobPreparationLocationReturnsNull() {
        assertNull(feeder.getJobPreparationLocation());
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
        assertNull(feeder.getSlotAddress());
    }
    
    @Test
    public void isEnabledReturnsFalseIfSetEnabledToFalse() {
        feeder.setEnabled(false);

        assertFalse(feeder.isEnabled());
    }

    @Test
    public void isEnabledReturnsFalseIfNoHardwareIdSet() {
        feeder.setEnabled(true);

        assertNull(feeder.getHardwareId());
        assertFalse(feeder.isEnabled());
    }

    @Test
    public void isEnabledReturnsFalseIfNoPartIsSet() {
        feeder.setEnabled(true);
        feeder.setHardwareId(hardwareId);

        assertNull(feeder.getPart());
        assertFalse(feeder.isEnabled());
    }

    @Test
    public void isEnabledReturnsFalseIfNoAddressIsSet() {
        feeder.setEnabled(true);
        feeder.setHardwareId(hardwareId);
        feeder.setPart(new Part("test-part"));

        assertNull(feeder.getSlot());
        assertFalse(feeder.isEnabled());
    }

    @Test
    public void isEnabledReturnsFalseIfSlotHasNoLocation() {
        feeder.setEnabled(true);
        feeder.setHardwareId(hardwareId);
        feeder.setPart(new Part("test-part"));
        feeder.setSlotAddress(5);

        assertNull(feeder.getSlot().getLocation());
        assertFalse(feeder.isEnabled());
    }

    @Test
    public void isEnabledReturnsFalseIfFeederHasNoOffset() {
        feeder.setEnabled(true);
        feeder.setHardwareId(hardwareId);
        feeder.setPart(new Part("test-part"));
        feeder.setSlotAddress(feederAddress);
        setSlotLocation(feederAddress, baseLocation);

        assertFalse(feeder.isEnabled());
    }

    @Test
    public void isEnabledReturnsTrueIfEverythingIsSet() {
        feeder.setEnabled(true);
        feeder.setHardwareId(hardwareId);
        feeder.setPart(new Part("test-part"));
        feeder.setSlotAddress(feederAddress);
        setSlotLocation(feederAddress, baseLocation);
        feeder.setOffset(feederOffset);

        assertTrue(feeder.isEnabled());
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
        assertFalse(feeder.isInitialized());
    }

    @Test
    public void prepareForJobFindsFeederAddressAndInitializes() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setOffset(feederOffset);
        setSlotLocation(feederAddress, baseLocation);

        String getFeederAddressCommand = getFeederAddress(hardwareId);
        when(mockedActuator.read(getFeederAddressCommand))
                .thenReturn(GetFeederAddress.ok(feederAddress, hardwareId));

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(feederAddress));

        feeder.prepareForJob(false);

        InOrder inOrder = inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(getFeederAddressCommand);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);

        assertEquals(feederAddress, (int) feeder.getSlotAddress());
        assertTrue(feeder.isInitialized());
    }
    
    @Test
    public void prepareForJobInitializesIfSlotAddressIsSet() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setOffset(feederOffset);
        feeder.setSlotAddress(feederAddress);
        setSlotLocation(feederAddress, baseLocation);

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(feederAddress));

        feeder.prepareForJob(false);

        InOrder inOrder = inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);

        assertEquals(feederAddress, (int) feeder.getSlotAddress());
        assertTrue(feeder.isInitialized());
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

        assertFalse(feeder.isInitialized());
        assertNull(feeder.getSlotAddress());
    }

    @Test
    public void prepareForJobFindsFeederAgainIfLostToTimeout() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setOffset(feederOffset);
        feeder.setSlotAddress(feederAddress);

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(Errors.timeout());

        int newAddress = 11;

        setSlotLocation(newAddress, baseLocation);

        String newGetFeederAddressCommand = getFeederAddress(hardwareId);
        when(mockedActuator.read(newGetFeederAddressCommand))
                .thenReturn(GetFeederAddress.ok(newAddress, hardwareId));

        String newInitializeFeederCommand = initializeFeeder(newAddress, hardwareId);
        when(mockedActuator.read(newInitializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(newAddress));

        feeder.prepareForJob(false);

        InOrder inOrder = inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator).read(newGetFeederAddressCommand);
        inOrder.verify(mockedActuator).read(newInitializeFeederCommand);

        assertEquals(newAddress, (int) feeder.getSlotAddress());
        assertTrue(feeder.isInitialized());
    }

    @Test
    public void prepareForJobFindsFeederAgainIfWrongFeederUUIDAndMakesNewFeeder() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setOffset(feederOffset);
        feeder.setSlotAddress(feederAddress);
        String otherHardwareId = "445566778899AABBCCDDEEFF";

        assertNull(IndexFeeder.findByHardwareId(otherHardwareId));

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(Errors.wrongFeederUUID(feederAddress, otherHardwareId));

        int newAddress = 11;

        setSlotLocation(newAddress, baseLocation);

        String newGetFeederAddressCommand = getFeederAddress(hardwareId);
        when(mockedActuator.read(newGetFeederAddressCommand))
                .thenReturn(GetFeederAddress.ok(newAddress, hardwareId));

        String newInitializeFeederCommand = initializeFeeder(newAddress, hardwareId);
        when(mockedActuator.read(newInitializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(newAddress));

        feeder.prepareForJob(false);

        InOrder inOrder = inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator).read(newGetFeederAddressCommand);
        inOrder.verify(mockedActuator).read(newInitializeFeederCommand);

        assertEquals(newAddress, (int) feeder.getSlotAddress());
        assertTrue(feeder.isInitialized());

        IndexFeeder otherFeeder = IndexFeeder.findByHardwareId(otherHardwareId);
        assertNotNull(otherFeeder);
        assertFalse(otherFeeder.initialized);
        assertEquals(feederAddress, (int) otherFeeder.slotAddress);
    }

    @Test
    public void prepareForJobFindsFeederAgainIfWrongFeederUUIDAndUsesExistingFeeder() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setOffset(feederOffset);
        feeder.setSlotAddress(feederAddress);

        String otherHardwareId = "445566778899AABBCCDDEEFF";
        IndexFeeder otherFeeder = new IndexFeeder();
        otherFeeder.setHardwareId(otherHardwareId);
        machine.addFeeder(otherFeeder);

        assertSame(otherFeeder, IndexFeeder.findByHardwareId(otherHardwareId));

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(Errors.wrongFeederUUID(feederAddress, otherHardwareId));

        int newAddress = 11;

        setSlotLocation(newAddress, baseLocation);

        String newGetFeederAddressCommand = getFeederAddress(hardwareId);
        when(mockedActuator.read(newGetFeederAddressCommand))
                .thenReturn(GetFeederAddress.ok(newAddress, hardwareId));

        String newInitializeFeederCommand = initializeFeeder(newAddress, hardwareId);
        when(mockedActuator.read(newInitializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(newAddress));

        feeder.prepareForJob(false);

        InOrder inOrder = inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator).read(newGetFeederAddressCommand);
        inOrder.verify(mockedActuator).read(newInitializeFeederCommand);

        assertEquals(newAddress, (int) feeder.getSlotAddress());
        assertTrue(feeder.isInitialized());

        IndexFeeder recalledOtherFeeder = IndexFeeder.findByHardwareId(otherHardwareId);
        assertSame(otherFeeder, recalledOtherFeeder);
        assertFalse(otherFeeder.initialized);
        assertEquals(feederAddress, (int) otherFeeder.slotAddress);
    }

    @Test
    public void prepareForJobThrowsExceptionIfNewSlotHasNoLocation() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(feederAddress);
        setSlotLocation(feederAddress, baseLocation);

        String firstInitializeCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(firstInitializeCommand))
                .thenReturn(Errors.timeout());

        int newFeederAddress = 11;
        String getFeederAddressCommand = getFeederAddress(hardwareId);
        when(mockedActuator.read(getFeederAddressCommand))
                .thenReturn(GetFeederAddress.ok(newFeederAddress, hardwareId));

        String newInitializeCommand = initializeFeeder(newFeederAddress, hardwareId);
        when(mockedActuator.read(newInitializeCommand))
                .thenReturn(InitializeFeeder.ok(newFeederAddress));


        assertThrows(UnconfiguredSlotException.class, () -> feeder.prepareForJob(false));

        assertTrue(feeder.isInitialized());
        assertEquals(newFeederAddress, feeder.getSlotAddress());
    }
    @Test
    public void prepareForJobThrowsExceptionIfFeederHasNoOffset() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(feederAddress);
        setSlotLocation(feederAddress, baseLocation);

        String initializeCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeCommand))
                .thenReturn(InitializeFeeder.ok(feederAddress));


        assertThrows(FeederHasNoLocationOffsetException.class, () -> feeder.prepareForJob(false));

        assertTrue(feeder.isInitialized());
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

        InOrder inOrder = inOrder(mockedActuator);
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

        InOrder inOrder = inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator, never()).read(any());
    }
    
    @Test
    public void feedMovesPartForwardByPitch() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setPartPitch(2);
        feeder.setOffset(feederOffset);
        feeder.setSlotAddress(feederAddress);
        setSlotLocation(feederAddress, baseLocation);

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(feederAddress));

        String moveFeedForwardCommand = moveFeedForward(feederAddress, 20);
        when(mockedActuator.read(moveFeedForwardCommand))
                .thenReturn(MoveFeedForward.ok(feederAddress));

        feeder.feed(mockedNozzle);

        InOrder inOrder = inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator).read(moveFeedForwardCommand);
    }
    
    @Test
    public void feedInitializesIfUninitializedErrorIsReturned() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setPartPitch(2);
        feeder.setOffset(feederOffset);
        feeder.setSlotAddress(feederAddress);
        setSlotLocation(feederAddress, baseLocation);

        String oldInitializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(oldInitializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(feederAddress));

        String oldMoveFeederForwardCommand = moveFeedForward(feederAddress, 20);
        when(mockedActuator.read(oldMoveFeederForwardCommand))
                .thenReturn(Errors.uninitializedFeeder(feederAddress, hardwareId));

        int newAddress = 11;

        setSlotLocation(newAddress, baseLocation);

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

        InOrder inOrder = inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(oldInitializeFeederCommand); // First initialization
        inOrder.verify(mockedActuator).read(oldMoveFeederForwardCommand); // Uninitialized feeder error
        inOrder.verify(mockedActuator).read(newGetFeederAddressCommand); // New address
        inOrder.verify(mockedActuator).read(newInitializeFeederCommand); // Second initialization in new slot
        inOrder.verify(mockedActuator).read(newMoveFeedForwardCommand); // Finally move the feeder

        assertEquals(newAddress, (int) feeder.getSlotAddress());
    }

    @Test
    public void feedThrowsExceptionAfterOneRetry() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setPartPitch(2);
        feeder.setOffset(feederOffset);
        feeder.setSlotAddress(feederAddress);
        setSlotLocation(feederAddress, baseLocation);
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

        assertThrows(FeedFailureException.class, () -> feeder.feed(mockedNozzle));

        InOrder inOrder = inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator).read(moveFeederForwardCommand);
        inOrder.verify(mockedActuator, never()).read(any());

        assertThrows(FeedFailureException.class, () -> feeder.feed(mockedNozzle));

        inOrder.verify(mockedActuator).read(getFeederAddressCommand);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator).read(moveFeederForwardCommand);
        inOrder.verify(mockedActuator, never()).read(any());
    }

    @Test
    public void feedThrowsExceptionAfterNoRetries() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setPartPitch(2);
        feeder.setOffset(feederOffset);
        feeder.setSlotAddress(feederAddress);
        setSlotLocation(feederAddress, baseLocation);
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

        assertThrows(FeedFailureException.class, () -> feeder.feed(mockedNozzle));

        InOrder inOrder = inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator).read(moveFeederForwardCommand);
        inOrder.verify(mockedActuator, never()).read(any());
    }

    @Test
    public void feedThrowsExceptionWhenFeederCannotBeInitialized() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(feederAddress);
        feeder.setPartPitch(2);
        indexProperties.setFeederCommunicationMaxRetry(1);

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(Errors.timeout());

        String getFeederAddressCommand = getFeederAddress(hardwareId);
        when(mockedActuator.read(getFeederAddressCommand))
                .thenReturn(GetFeederAddress.ok(feederAddress, hardwareId));

        assertThrows(FeedFailureException.class, () -> feeder.feed(mockedNozzle));

        InOrder inOrder = inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator).read(getFeederAddressCommand);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator, never()).read(any());
    }

    /**
     * We only use our internal retry counts if get address / initialize fails. We don't want to
     * use it if the feed command fails because OpenPnP itself has its own retries.
     */
    @Test
    public void feedThrowsExceptionIfTheFeedTimesOut() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setPartPitch(2);
        feeder.setOffset(feederOffset);
        feeder.setSlotAddress(feederAddress);
        setSlotLocation(feederAddress, baseLocation);

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(feederAddress));

        String moveFeedForwardCommand = moveFeedForward(feederAddress, 20);
        when(mockedActuator.read(moveFeedForwardCommand))
                .thenReturn(Errors.timeout());

        assertThrows(FeedFailureException.class, () -> feeder.feed(mockedNozzle));

        InOrder inOrder = inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator).read(moveFeedForwardCommand);
        inOrder.verify(mockedActuator, never()).read(any());
    }

    @Test
    public void feedInitializesOnUninitializedFeeder() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setPartPitch(2);
        feeder.setOffset(feederOffset);
        feeder.setSlotAddress(feederAddress);
        setSlotLocation(feederAddress, baseLocation);

        String oldInitializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(oldInitializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(feederAddress));

        String oldMoveFeederForwardCommand = moveFeedForward(feederAddress, 20);
        when(mockedActuator.read(oldMoveFeederForwardCommand))
                .thenReturn(Errors.uninitializedFeeder(feederAddress, "FFEEDDCCBBAA998877665544"));

        int newAddress = 11;

        setSlotLocation(newAddress, baseLocation);

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

        InOrder inOrder = inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(oldInitializeFeederCommand); // First initialization
        inOrder.verify(mockedActuator).read(oldMoveFeederForwardCommand); // Uninitialized feeder error
        inOrder.verify(mockedActuator).read(newGetFeederAddressCommand); // New address
        inOrder.verify(mockedActuator).read(newInitializeFeederCommand); // Second initialization in new slot
        inOrder.verify(mockedActuator).read(newMoveFeedForwardCommand); // Finally move the feeder

        assertEquals(newAddress, (int) feeder.getSlotAddress());
    }

    /**
     * The important thing to note here is that the feed command is never issued because we have no location.
     * Technically, the feeder could feed, but then we couldn't go pick up the part. When the issue is fixed,
     * feed would be called again essentially wasting a part. So we don't feed at all.
     */
    @Test
    public void feedThrowsExceptionIfSlotHasNoLocation() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(feederAddress);
        feeder.setPartPitch(2);

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(feederAddress));

        String moveFeedForwardCommand = moveFeedForward(feederAddress, 20);
        when(mockedActuator.read(moveFeedForwardCommand))
                .thenReturn(MoveFeedForward.ok(feederAddress));

        assertThrows(UnconfiguredSlotException.class, () -> feeder.feed(mockedNozzle));

        InOrder inOrder = inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator, never()).read(any());
    }

    @Test
    public void feedThrowsExceptionIfFeederHasNoOffset() throws Exception {
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(feederAddress);
        setSlotLocation(feederAddress, baseLocation);
        feeder.setPartPitch(2);

        String initializeFeederCommand = initializeFeeder(feederAddress, hardwareId);
        when(mockedActuator.read(initializeFeederCommand))
                .thenReturn(InitializeFeeder.ok(feederAddress));

        String moveFeedForwardCommand = moveFeedForward(feederAddress, 20);
        when(mockedActuator.read(moveFeedForwardCommand))
                .thenReturn(MoveFeedForward.ok(feederAddress));

        assertThrows(FeederHasNoLocationOffsetException.class, () -> feeder.feed(mockedNozzle));

        InOrder inOrder = inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(initializeFeederCommand);
        inOrder.verify(mockedActuator, never()).read(any());
    }

    @Test
    public void twoFeedersCanNotHaveTheSameAddress() throws Exception {
        // Remove the main feeder so we can make two of our own in this test
        machine.removeFeeder(feeder);

        // Feeder A -> Slot 1
        IndexFeeder feederA = new IndexFeeder();
        String hardwareIdA = "445566778899AABBCCDDEEFF";
        feederA.setHardwareId(hardwareIdA);
        feederA.setOffset(feederOffset);
        feederA.setSlotAddress(1);
        setSlotLocation(1, baseLocation);
        machine.addFeeder(feederA);

        // Feeder B -> Slot 2
        IndexFeeder feederB = new IndexFeeder();
        String hardwareIdB = "FFEEDDCCBBAA998877665544";
        feederB.setHardwareId(hardwareIdB);
        feederB.setOffset(feederOffset);
        feederB.setSlotAddress(2);
        setSlotLocation(2, baseLocation);
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
        assertThrows(FeedFailureException.class, () -> feederB.feed(mockedNozzle));

        // Verify all of the calls in order
        InOrder inOrder = inOrder(mockedActuator);
        inOrder.verify(mockedActuator).read(feederASlot1InitializationCommand); // First initialization Feeder A
        inOrder.verify(mockedActuator).read(feederBSlot2InitializationCommand); // First initialization Feeder B
        inOrder.verify(mockedActuator).read(feederBSlot2FeedCommand); // Feeder B timeout
        inOrder.verify(mockedActuator, never()).read(any());

        feederB.feed(mockedNozzle);

        inOrder.verify(mockedActuator).read(feederBGetAddressCommand); // Find feeder B slot
        inOrder.verify(mockedActuator).read(feederBSlot1InitializationCommand); // Initialize feeder B
        inOrder.verify(mockedActuator).read(feederBSlot1FeedCommand); // Finally move the feeder

        // Verify the state of the two feeders
        assertFalse(feederA.isInitialized());
        assertNull(feeder.getSlotAddress());

        assertTrue(feederB.isInitialized());
        assertEquals(1, (int) feederB.getSlotAddress());
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

        InOrder inOrder = inOrder(mockedActuator);
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

        InOrder inOrder = inOrder(mockedActuator);
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

        InOrder inOrder = inOrder(mockedActuator);
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

        InOrder inOrder = inOrder(mockedActuator);
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

        InOrder inOrder = inOrder(mockedActuator, progressUpdates);
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

    @Test
    public void findIssuesGivesNothingIfNoHardwareIdIsPresent() {
        feeder.setSlotAddress(feederAddress);

        List<Solutions.Issue> issues = new ArrayList<>();
        feeder.findIssues(issues);

        assertEquals(0, issues.size());
    }
    @Test
    public void findIssuesAddsIssueIfSlotHasNoLocationSet() {
        feeder.setHardwareId(hardwareId);
        feeder.setOffset(feederOffset);
        feeder.setSlotAddress(feederAddress);
        assertNull(feeder.getSlot().getLocation());

        List<Solutions.Issue> issues = new ArrayList<>();
        feeder.findIssues(issues);

        assertEquals(1, issues.size());
        Solutions.Issue issue = issues.get(0);

        assertEquals(feeder, issue.getSubject());
        assertEquals("Feeder slot has no configured location", issue.getIssue());
        assertEquals(Solutions.Severity.Error, issue.getSeverity());
        assertEquals(Solutions.State.Open, issue.getState());
    }
    @Test
    public void findIssuesAddsIssueIfFeederHasNoOffsetSet() {
        feeder.setHardwareId(hardwareId);
        feeder.setSlotAddress(feederAddress);
        setSlotLocation(feederAddress, baseLocation);

        List<Solutions.Issue> issues = new ArrayList<>();
        feeder.findIssues(issues);

        assertEquals(1, issues.size());
        Solutions.Issue issue = issues.get(0);

        assertEquals(feeder, issue.getSubject());
        assertEquals("Feeder has no configured offset", issue.getIssue());
        assertEquals(Solutions.Severity.Error, issue.getSeverity());
        assertEquals(Solutions.State.Open, issue.getState());
    }

    @Test
    public void getPickLocationThrowsExceptionIfNoSlotAddressIsSet() {
        assertThrows(NoSlotAddressException.class, () -> feeder.getPickLocation());
    }

    @Test
    public void getPickLocationThrowsExceptionIfSlotHasNoLocation() {
        feeder.setSlotAddress(feederAddress);

        assertThrows(UnconfiguredSlotException.class, () -> feeder.getPickLocation());
    }
    
    @Test
    public void getPickLocationThrowsExceptionIfFeederHasNoOffset() {
        feeder.setSlotAddress(feederAddress);
        setSlotLocation(feederAddress, baseLocation);

        assertThrows(FeederHasNoLocationOffsetException.class, () -> feeder.getPickLocation());
    }

    @Test
    public void getPickLocationUsesOffsetWithRotation() throws Exception {
        feeder.setSlotAddress(feederAddress);
        setSlotLocation(feederAddress, baseLocation);

        feeder.setOffset(feederOffset);

        Location actualLocation = feeder.getPickLocation();
        Location expectedLocation = new Location(LengthUnit.Millimeters, 2, 3, 3, 45);

        assertEquals(expectedLocation, actualLocation);
    }
}
