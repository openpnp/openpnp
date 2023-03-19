package org.openpnp.machine.photon.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.machine.photon.protocol.helpers.ResponsesHelper;
import org.openpnp.machine.photon.protocol.helpers.TestBus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TestBusTest {
    private TestBus bus;
    private PhotonCommands commands;
    private ResponsesHelper responses;

    @BeforeEach
    public void setUp() {
        bus = new TestBus();

        commands = new PhotonCommands(0);
        responses = new ResponsesHelper(0);
    }

    @Test
    public void busRespondsWithReplyToRequestedCommand() throws Exception {
        Packet commandPacket = commands.getVersion(0x47);
        Packet responsePacket = responses.getVersion.ok(0x47, 3);

        bus.when(commandPacket).reply(responsePacket);

        Optional<Packet> optionalResponse = bus.send(commandPacket);

        assertTrue(optionalResponse.isPresent());

        Packet actualPacket = optionalResponse.get();

        assertSame(responsePacket, actualPacket);
    }

    @Test
    public void busRespondsWithTimeout() throws Exception {
        Packet commandPacket = commands.getVersion(0x47);

        bus.when(commandPacket).timeout();

        Optional<Packet> optionalResponse = bus.send(commandPacket);

        assertFalse(optionalResponse.isPresent());
    }

    @Test
    public void busRespondsWithExceptionIfCommandHasNoReply(){
        Packet commandPacket = commands.getVersion(0x47);

        bus.when(commandPacket);  // No reply specified

        assertThrows(TestBus.NoPacketMocking.class, () -> bus.send(commandPacket));
    }

    @Test
    public void busRespondsWithExceptionIfCommandIsNotMockedAtAll(){
        Packet commandPacket = commands.getVersion(0x47);

        assertThrows(TestBus.NoPacketMocking.class, () -> bus.send(commandPacket));
    }

    @Test
    public void busValidatesToAddress() throws Exception {
        Packet sendingPacket = commands.getVersion(0x47);
        Packet mockingPacket = sendingPacket.clone();
        mockingPacket.toAddress = 0x10;

        bus.when(mockingPacket).timeout();

        assertThrows(TestBus.NoPacketMocking.class, () -> bus.send(sendingPacket));
    }

    @Test
    public void busValidatesFromAddress() throws Exception {
        Packet sendingPacket = commands.getVersion(0x47);
        Packet mockingPacket = sendingPacket.clone();
        mockingPacket.fromAddress = 0x10;

        bus.when(mockingPacket).timeout();

        assertThrows(TestBus.NoPacketMocking.class, () -> bus.send(sendingPacket));
    }

    @Test
    public void busValidatesPayloadLength() throws Exception {
        Packet sendingPacket = commands.getVersion(0x47);
        Packet mockingPacket = sendingPacket.clone();
        mockingPacket.payloadLength = 0x02;

        bus.when(mockingPacket).timeout();

        assertThrows(TestBus.NoPacketMocking.class, () -> bus.send(sendingPacket));
    }

    @Test
    public void busValidatesPayloadDataLength() throws Exception {
        Packet sendingPacket = commands.getVersion(0x47);
        Packet mockingPacket = sendingPacket.clone();
        mockingPacket.payload = new int[] { 0x03, 0x05 };

        bus.when(mockingPacket).timeout();

        assertThrows(TestBus.NoPacketMocking.class, () -> bus.send(sendingPacket));
    }

    @Test
    public void busValidatesPayloadData() throws Exception {
        Packet sendingPacket = commands.getVersion(0x47);
        Packet mockingPacket = sendingPacket.clone();
        mockingPacket.payload = new int[] { 0x04 };

        bus.when(mockingPacket).timeout();

        assertThrows(TestBus.NoPacketMocking.class, () -> bus.send(sendingPacket));
    }

    @Test
    public void busAdjustsResponsePacketId() throws Exception {
        Packet commandPacket = commands.getVersion(0x47);
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
        Packet firstSendingPacket = commands.getVersion(0x01);
        Packet firstResponsePacket = responses.getVersion.ok(0x01, 3);

        Packet secondSendingPacket = commands.getVersion(0x02);
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
}
