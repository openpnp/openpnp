package org.openpnp.machine.photon.protocol.commands;

import org.junit.jupiter.api.Test;
import org.openpnp.machine.photon.protocol.ErrorTypes;
import org.openpnp.machine.photon.protocol.Packet;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MoveFeedStatusTest {
    @Test
    public void createCommand() {
        Packet moveFeedStatus = new MoveFeedStatus(29).toPacket();
        assertEquals(29, moveFeedStatus.toAddress);
        assertEquals(1, moveFeedStatus.payloadLength);
        assertArrayEquals(new int[]{0x06}, moveFeedStatus.payload);
    }

    @Test
    public void decodeOk() {
        Packet responsePacket = new Packet();
        responsePacket.fromAddress = 23;
        responsePacket.payload = new int[]{0x00};
        responsePacket.calculateCRC();
        MoveFeedStatus.Response response = new MoveFeedStatus.Response(responsePacket);

        assertEquals(0, response.toAddress);
        assertEquals(23, response.fromAddress);
        assertTrue(response.valid);
        assertEquals(ErrorTypes.NONE, response.error);
    }

    @Test
    public void decodeUninitializedFeeder() {
        Packet responsePacket = new Packet();
        responsePacket.fromAddress = 39;
        responsePacket.payload = new int[]{0x03};
        responsePacket.calculateCRC();
        MoveFeedStatus.Response response = new MoveFeedStatus.Response(responsePacket);

        assertEquals(0, response.toAddress);
        assertEquals(39, response.fromAddress);
        assertTrue(response.valid);
        assertEquals(ErrorTypes.UNINITIALIZED_FEEDER, response.error);
    }

    @Test
    public void decodeCouldNotReach() {
        Packet responsePacket = new Packet();
        responsePacket.fromAddress = 39;
        responsePacket.payload = new int[]{0x02};
        responsePacket.calculateCRC();
        MoveFeedStatus.Response response = new MoveFeedStatus.Response(responsePacket);

        assertEquals(0, response.toAddress);
        assertEquals(39, response.fromAddress);
        assertTrue(response.valid);
        assertEquals(ErrorTypes.COULD_NOT_REACH, response.error);
    }

    @Test
    public void decodeFeedingInProgress() {
        Packet responsePacket = new Packet();
        responsePacket.fromAddress = 39;
        responsePacket.payload = new int[]{0x04};
        responsePacket.calculateCRC();
        MoveFeedStatus.Response response = new MoveFeedStatus.Response(responsePacket);

        assertEquals(0, response.toAddress);
        assertEquals(39, response.fromAddress);
        assertTrue(response.valid);
        assertEquals(ErrorTypes.FEEDING_IN_PROGRESS, response.error);
    }

    @Test
    public void decodeLengthTooShort() {
        Packet responsePacket = new Packet();
        responsePacket.fromAddress = 17;
        responsePacket.payload = new int[]{};
        responsePacket.calculateCRC();
        MoveFeedStatus.Response response = new MoveFeedStatus.Response(responsePacket);

        assertEquals(0, response.toAddress);
        assertEquals(17, response.fromAddress);
        assertFalse(response.valid);
        assertNull(response.error);
    }

    @Test
    public void decodeLengthTooLong() {
        Packet responsePacket = new Packet();
        responsePacket.fromAddress = 17;
        responsePacket.payload = new int[]{0x00, 0x00};
        responsePacket.calculateCRC();
        MoveFeedStatus.Response response = new MoveFeedStatus.Response(responsePacket);

        assertEquals(0, response.toAddress);
        assertEquals(17, response.fromAddress);
        assertFalse(response.valid);
        assertNull(response.error);
    }
}
