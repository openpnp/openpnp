package org.openpnp.machine.photon.protocol.commands;

import org.junit.jupiter.api.Test;
import org.openpnp.machine.photon.protocol.ErrorTypes;
import org.openpnp.machine.photon.protocol.Packet;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MoveFeedBackwardTest {
    @Test
    public void createCommand() {
        Packet moveFeedBackward = new MoveFeedBackward(29, 20).toPacket();
        assertEquals(29, moveFeedBackward.toAddress);
        assertEquals(2, moveFeedBackward.payloadLength);
        assertArrayEquals(new int[]{0x05, 0x14}, moveFeedBackward.payload);
    }

    @Test
    public void decodeOk() {
        Packet responsePacket = new Packet();
        responsePacket.fromAddress = 23;
        responsePacket.payload = new int[]{0x00, 0x01, 0x02};
        responsePacket.calculateCRC();
        MoveFeedBackward.Response response = new MoveFeedBackward.Response(responsePacket);

        assertEquals(0, response.toAddress);
        assertEquals(23, response.fromAddress);
        assertTrue(response.valid);
        assertEquals(ErrorTypes.NONE, response.error);
        assertEquals(258, response.expectedTimeToFeed);
    }

    @Test
    public void decodeUninitializedFeeder() {
        Packet responsePacket = new Packet();
        responsePacket.fromAddress = 39;
        responsePacket.payload = new int[]{0x03, 0x00, 0x00};
        responsePacket.calculateCRC();
        MoveFeedBackward.Response response = new MoveFeedBackward.Response(responsePacket);

        assertEquals(0, response.toAddress);
        assertEquals(39, response.fromAddress);
        assertTrue(response.valid);
        assertEquals(ErrorTypes.UNINITIALIZED_FEEDER, response.error);
        assertEquals(0, response.expectedTimeToFeed);
    }

    @Test
    public void decodeMotorFault() {
        Packet responsePacket = new Packet();
        responsePacket.fromAddress = 39;
        responsePacket.payload = new int[]{0x02, 0x00, 0x00};
        responsePacket.calculateCRC();
        MoveFeedBackward.Response response = new MoveFeedBackward.Response(responsePacket);

        assertEquals(0, response.toAddress);
        assertEquals(39, response.fromAddress);
        assertTrue(response.valid);
        assertEquals(ErrorTypes.COULD_NOT_REACH, response.error);
        assertEquals(0, response.expectedTimeToFeed);
    }

    @Test
    public void decodeLengthTooShort() {
        Packet responsePacket = new Packet();
        responsePacket.fromAddress = 17;
        responsePacket.payload = new int[]{0x00, 0x00};
        responsePacket.calculateCRC();
        MoveFeedBackward.Response response = new MoveFeedBackward.Response(responsePacket);

        assertEquals(0, response.toAddress);
        assertEquals(17, response.fromAddress);
        assertFalse(response.valid);
        assertNull(response.error);
        assertEquals(0, response.expectedTimeToFeed);
    }

    @Test
    public void decodeLengthTooLong() {
        Packet responsePacket = new Packet();
        responsePacket.fromAddress = 17;
        responsePacket.payload = new int[]{0x00, 0x00, 0x00, 0x00};
        responsePacket.calculateCRC();
        MoveFeedBackward.Response response = new MoveFeedBackward.Response(responsePacket);

        assertEquals(0, response.toAddress);
        assertEquals(17, response.fromAddress);
        assertFalse(response.valid);
        assertNull(response.error);
    }
}
