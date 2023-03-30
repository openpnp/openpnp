package org.openpnp.machine.photon.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.openpnp.machine.photon.protocol.commands.GetFeederId;
import org.openpnp.machine.photon.protocol.helpers.ResponsesHelper;
import org.openpnp.spi.Actuator;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PhotonBusTest {
    protected static String uuid_s = "FFEEDDCCBBAA998877665544";

    private Actuator mockedActuator;

    private PhotonBus bus;
    private ResponsesHelper responses;

    @BeforeEach
    public void setUp() throws Exception {
        mockedActuator = mock(Actuator.class);

        bus = new PhotonBus(0, mockedActuator);

        responses = new ResponsesHelper(0);
    }

    private Answer<String> matchPacketId(Packet response) {
        return invocation -> {
            String commandString = invocation.getArgument(0);
            // Decode the packet
            Optional<Packet> optionalPacket = Packet.decode(commandString);
            if(! optionalPacket.isPresent()) {
                return null;
            }

            Packet commandPacket = optionalPacket.get();

            response.packetId = commandPacket.packetId;
            response.calculateCRC();

            return response.toByteString();
        };
    }

    /**
     * Mock the actuator read so when command packet is sent, response packet is replied with by the mock.
     *
     * @param response The response that we'd like to send back. It will have its packet id and CRC adjusted.
     * @throws Exception mockedActuator.read can throw an exception.
     */
    private void mockBusResponse(Packet response) throws Exception {
        when(mockedActuator.read(any(String.class))).then(matchPacketId(response));
    }

    @Test
    public void busSendsPacketInStringForm() throws Exception {
        Packet command = new GetFeederId(0x42).toPacket();
        Packet generatedResponse = responses.getFeederId.ok(0x42, uuid_s);

        mockBusResponse(generatedResponse);

        Optional<Packet> optionalResponse = bus.send(command);

        assertTrue(optionalResponse.isPresent());
        Packet response = optionalResponse.get();
        verify(mockedActuator).read(command.toByteString());
        assertEquals(generatedResponse.toByteString(), response.toByteString());

        assertEquals(generatedResponse.toAddress, response.toAddress);
        assertEquals(generatedResponse.fromAddress, response.fromAddress);
        assertEquals(command.packetId, response.packetId);
        assertEquals(generatedResponse.payloadLength, response.payloadLength);
        assertArrayEquals(generatedResponse.payload, response.payload);
    }

    @Test
    public void busReturnsEmptyOptionalIfTimeoutOccurs() throws Exception {
        Packet command = new GetFeederId(0x42).toPacket();

        when(mockedActuator.read(any(String.class))).thenReturn("TIMEOUT");

        Optional<Packet> optionalResponse = bus.send(command);

        assertFalse(optionalResponse.isPresent());
    }

    @Test
    public void busReturnsEmptyOptionalIfWrongPacketId() throws Exception {
        Packet command = new GetFeederId(0x42).toPacket();
        Packet generatedResponse = responses.getFeederId.ok(0x42, uuid_s);
        generatedResponse.packetId = 0x47;

        when(mockedActuator.read(any(String.class))).thenReturn(generatedResponse.toByteString());

        Optional<Packet> optionalResponse = bus.send(command);

        assertFalse(optionalResponse.isPresent());
    }

    @Test
    public void busIncrementsPacketId() throws Exception {
        Packet firstCommand = new GetFeederId(0x42).toPacket();
        Packet generatedResponse = responses.getFeederId.ok(0x42, uuid_s);

        mockBusResponse(generatedResponse);

        Optional<Packet> optionalResponse = bus.send(firstCommand);

        assertTrue(optionalResponse.isPresent());
        Packet response = optionalResponse.get();
        verify(mockedActuator).read(firstCommand.toByteString());

        assertEquals(firstCommand.packetId, response.packetId);

        Packet secondCommand = new GetFeederId(0x42).toPacket();
        optionalResponse = bus.send(secondCommand);

        assertTrue(optionalResponse.isPresent());
        response = optionalResponse.get();
        assertEquals(secondCommand.packetId, response.packetId);

        assertNotEquals(firstCommand.packetId, secondCommand.packetId);
    }

    @Test
    public void busPacketIdRollsOver() throws Exception {
        Packet firstCommand = new GetFeederId(0x42).toPacket();
        Packet generatedResponse = responses.getFeederId.ok(0x42, uuid_s);

        mockBusResponse(generatedResponse);

        Optional<Packet> optionalResponse = Optional.empty();
        for(int i = 0; i < 256; i++) {
            optionalResponse = bus.send(firstCommand);
        }

        assertTrue(optionalResponse.isPresent());
        Packet response = optionalResponse.get();
        assertEquals(0xff, response.packetId);  // Make sure the last command put us in a state to roll over

        Packet secondCommand = new GetFeederId(0x42).toPacket();
        optionalResponse = bus.send(secondCommand);  // This command should roll over to 0

        assertTrue(optionalResponse.isPresent());

        response = optionalResponse.get();
        assertEquals(0, response.packetId);
    }
}
