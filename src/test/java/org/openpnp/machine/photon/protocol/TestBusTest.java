package org.openpnp.machine.photon.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.machine.photon.protocol.commands.GetVersion;
import org.openpnp.machine.photon.protocol.helpers.ResponsesHelper;
import org.openpnp.machine.photon.protocol.helpers.TestBus;
import org.opentest4j.AssertionFailedError;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TestBusTest {
    private TestBus bus;
    private ResponsesHelper responses;

    @BeforeEach
    public void setUp() {
        bus = new TestBus();

        responses = new ResponsesHelper(0);
    }

    @Test
    public void busRespondsWithReplyToRequestedCommandPacket() throws Exception {
        Packet commandPacket = new GetVersion(0x47).toPacket();
        Packet responsePacket = responses.getVersion.ok(0x47, 3);

        bus.when(commandPacket).reply(responsePacket);

        Optional<Packet> optionalResponse = bus.send(commandPacket);

        assertTrue(optionalResponse.isPresent());

        Packet actualPacket = optionalResponse.get();

        assertSame(responsePacket, actualPacket);
    }

    @Test
    public void busRespondsWithReplyToRequestedCommand() throws Exception {
        GetVersion getVersionCommand = new GetVersion(0x47);
        Packet responsePacket = responses.getVersion.ok(0x47, 3);

        bus.when(getVersionCommand).reply(responsePacket);

        Optional<Packet> optionalResponse = bus.send(getVersionCommand.toPacket());

        assertTrue(optionalResponse.isPresent());

        Packet actualPacket = optionalResponse.get();

        assertSame(responsePacket, actualPacket);
    }

    @Test
    public void busRespondsWithTimeout() throws Exception {
        Packet commandPacket = new GetVersion(0x47).toPacket();

        bus.when(commandPacket).timeout();

        Optional<Packet> optionalResponse = bus.send(commandPacket);

        assertFalse(optionalResponse.isPresent());
    }

    @Test
    public void busRespondsWithExceptionIfCommandHasNoReply() throws Exception {
        Packet commandPacket = new GetVersion(0x47).toPacket();

        bus.when(commandPacket);  // No reply specified

        assertThrows(TestBus.NoPacketMocking.class, () -> bus.send(commandPacket));
    }

    @Test
    public void busRespondsWithExceptionIfCommandIsNotMockedAtAll() {
        Packet commandPacket = new GetVersion(0x47).toPacket();

        assertThrows(TestBus.NoPacketMocking.class, () -> bus.send(commandPacket));
    }

    @Test
    public void busValidatesToAddress() throws Exception {
        Packet sendingPacket = new GetVersion(0x47).toPacket();
        Packet mockingPacket = sendingPacket.clone();
        mockingPacket.toAddress = 0x10;

        bus.when(mockingPacket).timeout();

        assertThrows(TestBus.NoPacketMocking.class, () -> bus.send(sendingPacket));
    }

    @Test
    public void busValidatesFromAddress() throws Exception {
        Packet sendingPacket = new GetVersion(0x47).toPacket();
        Packet mockingPacket = sendingPacket.clone();
        mockingPacket.fromAddress = 0x10;

        bus.when(mockingPacket).timeout();

        assertThrows(TestBus.NoPacketMocking.class, () -> bus.send(sendingPacket));
    }

    @Test
    public void busValidatesPayloadLength() throws Exception {
        Packet sendingPacket = new GetVersion(0x47).toPacket();
        Packet mockingPacket = sendingPacket.clone();
        mockingPacket.payloadLength = 0x02;

        bus.when(mockingPacket).timeout();

        assertThrows(TestBus.NoPacketMocking.class, () -> bus.send(sendingPacket));
    }

    @Test
    public void busValidatesPayloadDataLength() throws Exception {
        Packet sendingPacket = new GetVersion(0x47).toPacket();
        Packet mockingPacket = sendingPacket.clone();
        mockingPacket.payload = new int[]{0x03, 0x05};

        bus.when(mockingPacket).timeout();

        assertThrows(TestBus.NoPacketMocking.class, () -> bus.send(sendingPacket));
    }

    @Test
    public void busValidatesPayloadData() throws Exception {
        Packet sendingPacket = new GetVersion(0x47).toPacket();
        Packet mockingPacket = sendingPacket.clone();
        mockingPacket.payload = new int[]{0x04};

        bus.when(mockingPacket).timeout();

        assertThrows(TestBus.NoPacketMocking.class, () -> bus.send(sendingPacket));
    }

    @Test
    public void busAdjustsResponsePacketId() throws Exception {
        Packet commandPacket = new GetVersion(0x47).toPacket();
        commandPacket.packetId = 0x43;
        Packet responsePacket = responses.getVersion.ok(0x47, 3);

        bus.when(commandPacket).reply(responsePacket);

        // Assert the packet id is 0 before sending the command off but after setting up the mock
        assertEquals(0, responsePacket.packetId);

        Optional<Packet> optionalResponse = bus.send(commandPacket);

        assertTrue(optionalResponse.isPresent());

        Packet actualPacket = optionalResponse.get();

        assertSame(responsePacket, actualPacket);
        assertEquals(0x43, responsePacket.packetId);
    }

    @Test
    public void busCanHandleRespondingDifferentlyToDifferentSendingPackets() throws Exception {
        Packet firstSendingPacket = new GetVersion(0x01).toPacket();
        Packet firstResponsePacket = responses.getVersion.ok(0x01, 3);

        Packet secondSendingPacket = new GetVersion(0x02).toPacket();
        Packet secondResponsePacket = responses.getVersion.ok(0x02, 4);

        bus.when(firstSendingPacket).reply(firstResponsePacket);
        bus.when(secondSendingPacket).reply(secondResponsePacket);

        // Send our first packet
        Optional<Packet> firstOptionalPacket = bus.send(firstSendingPacket);

        // Validate that it's the same as firstResponsePacket
        assertTrue(firstOptionalPacket.isPresent());
        Packet actualFirstResponse = firstOptionalPacket.get();
        assertSame(firstResponsePacket, actualFirstResponse);

        // Send our second packet
        Optional<Packet> secondOptionalPacket = bus.send(secondSendingPacket);

        // Validate that it's the same as firstResponsePacket
        assertTrue(secondOptionalPacket.isPresent());
        Packet actualSecondResponse = secondOptionalPacket.get();
        assertSame(secondResponsePacket, actualSecondResponse);
    }

    /**
     * This test intentionally creates 2 objects that would otherwise be exactly the same. This verifies
     * that we're checking on data values in the "when" method instead of on it being the same object.
     *
     * @throws Exception if send throws
     */
    @Test
    public void busWillOverrideReplyIfCommandIsSpecifiedAgain() throws Exception {
        GetVersion firstCommand = new GetVersion(5);
        Packet response = responses.getVersion.ok(5, 1);
        bus.when(firstCommand)
                .reply(response);

        Optional<Packet> optionalPacket = bus.send(firstCommand.toPacket());

        assertTrue(optionalPacket.isPresent());
        assertSame(response, optionalPacket.get());

        GetVersion secondCommand = new GetVersion(5);
        bus.when(secondCommand).timeout();

        optionalPacket = bus.send(secondCommand.toPacket());

        assertFalse(optionalPacket.isPresent());
    }

    @Test
    public void busThrowsAssertionFailedErrorIfCommandNotInvoked() throws Exception {
        GetVersion command = new GetVersion(5);
        bus.when(command).timeout();

        // Nothing ever calls this bus command

        assertThrows(AssertionFailedError.class, () -> bus.verify(command));
    }

    @Test
    public void busDoesNotThrowAssertionFailedErrorIfCommandIsInvoked() throws Exception {
        GetVersion command = new GetVersion(5);
        bus.when(command).timeout();

        bus.send(command);

        bus.verify(command);  // This should not throw an Assertion Failed
    }

    @Test
    public void busVerifiesContentOfCalls() throws Exception {
        GetVersion firstCommand = new GetVersion(5);
        GetVersion secondCommand = new GetVersion(10);

        bus.when(firstCommand).timeout();
        bus.when(secondCommand).timeout();

        bus.send(secondCommand);

        assertThrows(AssertionFailedError.class, () -> bus.verify(firstCommand));
    }

    @Test
    public void busVerifiesOrderOfCalls() throws Exception {
        GetVersion firstCommand = new GetVersion(5);
        GetVersion secondCommand = new GetVersion(10);

        bus.when(firstCommand).timeout();
        bus.when(secondCommand).timeout();

        bus.send(firstCommand);
        bus.send(secondCommand);
        bus.verify(firstCommand)
                .then(secondCommand);
    }

    @Test
    public void busWillFailWithNoMoreCalls() throws Exception {
        GetVersion firstCommand = new GetVersion(5);
        GetVersion secondCommand = new GetVersion(10);

        bus.when(firstCommand).timeout();
        bus.when(secondCommand).timeout();

        bus.send(firstCommand);
        bus.send(secondCommand);

        assertThrows(AssertionFailedError.class, () -> {
            bus.verify(firstCommand)
                    .nothingElseSent();
        });
    }

    @Test
    public void canVerifyCallsThenVerifyNoMore() throws Exception {
        GetVersion firstCommand = new GetVersion(5);
        GetVersion secondCommand = new GetVersion(10);

        bus.when(firstCommand).timeout();
        bus.when(secondCommand).timeout();

        bus.send(firstCommand);
        bus.send(secondCommand);

        bus.verify(firstCommand)
                .then(secondCommand)
                .nothingElseSent();
    }

    @Test
    public void canImmediatelyVerifyNoMoreCalls() throws Exception {
        GetVersion firstCommand = new GetVersion(5);
        GetVersion secondCommand = new GetVersion(10);

        bus.when(firstCommand).timeout();
        bus.when(secondCommand).timeout();

        bus.verifyNothingSent();
    }

    @Test
    public void canImmediatelyFailIfSomethingSentAndNothingSent() throws Exception {
        GetVersion command = new GetVersion(5);

        bus.when(command).timeout();

        bus.send(command);

        assertThrows(AssertionFailedError.class, () -> bus.verifyNothingSent());
    }

    @Test
    public void callVerificationStillWorksIfReplyIsChanged() throws Exception {
        GetVersion command = new GetVersion(5);
        Packet responsePacket = responses.getVersion.ok(0x47, 3);

        bus.when(command).timeout();

        bus.send(command);

        bus.when(command).reply(responsePacket);

        bus.send(command);

        bus.verify(command)
                .then(command)
                .nothingElseSent();
    }

    @Test
    public void canVerifyInMockedOrder() throws Exception{
        GetVersion firstCommand = new GetVersion(5);
        GetVersion secondCommand = new GetVersion(10);

        bus.when(firstCommand).timeout();
        bus.when(secondCommand).timeout();

        bus.send(firstCommand);
        bus.send(secondCommand);

        bus.verifyInMockedOrder();
    }

    @Test
    public void willFailVerifyInMockedOrder() throws Exception{
        GetVersion firstCommand = new GetVersion(5);
        GetVersion secondCommand = new GetVersion(10);

        bus.when(firstCommand).timeout();
        bus.when(secondCommand).timeout();

        bus.send(secondCommand);
        bus.send(firstCommand);

        assertThrows(AssertionFailedError.class, () -> bus.verifyInMockedOrder());
    }

    @Test
    public void whenReliesOnPacketContentEvenIfContentChanges() throws Exception {
        Packet packet = new GetVersion(5).toPacket();

        bus.when(packet).timeout();

        // Change the packet, so it's no longer what we mocked
        packet.toAddress = 10;
        packet.calculateCRC();

        assertThrows(TestBus.NoPacketMocking.class, () -> bus.send(packet));
    }

    @Test
    public void verifyReliesOnPacketContentEvenIfContentChanges() throws Exception {
        Packet packet = new GetVersion(5).toPacket();

        bus.when(packet).timeout();

        bus.send(packet);

        // Change the packet, so it's no longer what we mocked
        packet.toAddress = 10;
        packet.calculateCRC();

        assertThrows(AssertionFailedError.class, () -> bus.verify(packet));
    }

    @Test
    public void verifyMockedOrderReliesOnPacketContentEvenIfContentChanges() throws Exception {
        Packet packet = new GetVersion(5).toPacket();

        bus.when(packet).timeout();

        bus.send(packet);

        // Change the packet, so it's no longer what we mocked
        packet.toAddress = 10;
        packet.calculateCRC();

        // This shouldn't fail with an assertion because we sent what we mocked and THEN changed it.
        bus.verifyInMockedOrder();
    }

    @Test
    public void verifyInMockedOrderCallsNothingElseSent() throws Exception{
        GetVersion firstCommand = new GetVersion(5);
        GetVersion secondCommand = new GetVersion(10);

        bus.when(firstCommand).timeout();
        bus.when(secondCommand).timeout();

        bus.send(firstCommand);
        bus.send(secondCommand);
        bus.send(firstCommand);

        assertThrows(AssertionFailedError.class, () -> bus.verifyInMockedOrder());
    }
}
