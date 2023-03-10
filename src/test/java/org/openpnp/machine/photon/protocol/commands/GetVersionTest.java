package org.openpnp.machine.photon.protocol.commands;

import org.junit.jupiter.api.Test;
import org.openpnp.machine.photon.protocol.ErrorTypes;
import org.openpnp.machine.photon.protocol.Packet;

import static org.junit.jupiter.api.Assertions.*;

public class GetVersionTest {

    @Test
    public void createCommand() {
        Packet getVersionPacket = new GetVersion(43).toPacket();
        assertEquals(43, getVersionPacket.toAddress);
        assertEquals(1, getVersionPacket.payloadLength);
        assertArrayEquals(new int[]{0x03}, getVersionPacket.payload);
    }

    @Test
    public void decodeOk() {
        Packet responsePacket = new Packet();
        responsePacket.fromAddress = 23;
        responsePacket.payload = new int[]{0x00};
        responsePacket.calculateCRC();
        GetVersion.Response response = new GetVersion.Response(responsePacket);

        assertEquals(0, response.toAddress);
        assertEquals(23, response.fromAddress);
        assertTrue(response.valid);
        assertEquals(ErrorTypes.NONE, response.error);
    }

    @Test
    public void decodeLengthTooShort() {
        Packet responsePacket = new Packet();
        responsePacket.fromAddress = 17;
        responsePacket.payload = new int[]{};
        responsePacket.calculateCRC();
        GetFeederId.Response response = new GetFeederId.Response(responsePacket);

        assertEquals(0, response.toAddress);
        assertEquals(17, response.fromAddress);
        assertFalse(response.valid);
        assertNull(response.error);
        assertNull(response.uuid);
    }

    @Test
    public void decodeLengthTooLong() {
        Packet responsePacket = new Packet();
        responsePacket.fromAddress = 17;
        responsePacket.payload = new int[]{0x00, 0x00};
        responsePacket.calculateCRC();
        GetFeederId.Response response = new GetFeederId.Response(responsePacket);

        assertEquals(0, response.toAddress);
        assertEquals(17, response.fromAddress);
        assertFalse(response.valid);
        assertNull(response.error);
        assertNull(response.uuid);
    }
}
