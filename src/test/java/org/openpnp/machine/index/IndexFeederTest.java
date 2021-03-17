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

    private Actuator mockedActuator;
    private Nozzle mockedNozzle;

    @Before
    public void setUp() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);
        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        Machine machine = Configuration.get().getMachine();
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

    /*
    TODO More tests:
    1. Feeder isn't in machine on prepare (timeout)
    2. Feeder isn't in machine on feed (timeout)
    3. Two feeders can't have the same address. Imagine this scenario: Feeder A is initialized in slot 1.
        Feeder B is initialized in slot 2. Feeder A is removed and Feeder B is put in slot 1. A feed on
        Feeder B occurs. We get a timeout on slot 2. We find it again in slot 1. We initialize Feeder B
        in slot 1. At this point, we MUST remove the address for Feeder A and consider it uninitialized.
        Otherwise, we'll try to feed slot 1, which will work because there's an initialized feeder in there.
     */
}
