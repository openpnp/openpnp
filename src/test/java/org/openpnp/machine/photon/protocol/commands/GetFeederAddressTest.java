package org.openpnp.machine.photon.protocol.commands;

import org.junit.jupiter.api.Test;
import org.openpnp.machine.photon.protocol.ErrorTypes;
import org.openpnp.machine.photon.protocol.Packet;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GetFeederAddressTest {
    protected static String uuid_s = "FFEEDDCCBBAA998877665544";
    protected static int[] uuid_b = {0xFF, 0xEE, 0xDD, 0xCC, 0xBB, 0xAA, 0x99, 0x88, 0x77, 0x66, 0x55, 0x44};


    @Test
    public void createCommand() {
        Packet getFeederAddressPacket = new GetFeederAddress(uuid_s).toPacket();
        assertEquals(0xff, getFeederAddressPacket.toAddress);
        assertEquals(13, getFeederAddressPacket.payloadLength);
        assertArrayEquals(new int[]{
                0xC0,
                uuid_b[0], uuid_b[1], uuid_b[2], uuid_b[3],
                uuid_b[4], uuid_b[5], uuid_b[6], uuid_b[7],
                uuid_b[8], uuid_b[9], uuid_b[10], uuid_b[11],
        }, getFeederAddressPacket.payload);
    }

    @Test
    public void decodeOk() {
        Packet responsePacket = new Packet();
        responsePacket.fromAddress = 24;
        responsePacket.payload = new int[]{0x00};
        responsePacket.calculateCRC();
        GetFeederAddress.Response response = new GetFeederAddress.Response(responsePacket);

        assertEquals(0, response.toAddress);
        assertEquals(24, response.fromAddress);
        assertTrue(response.valid);
        assertEquals(ErrorTypes.NONE, response.error);
    }

    @Test
    public void decodeLengthTooShort() {
        Packet responsePacket = new Packet();
        responsePacket.fromAddress = 17;
        responsePacket.payload = new int[]{};
        responsePacket.calculateCRC();
        GetFeederAddress.Response response = new GetFeederAddress.Response(responsePacket);

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
        GetFeederAddress.Response response = new GetFeederAddress.Response(responsePacket);

        assertEquals(0, response.toAddress);
        assertEquals(17, response.fromAddress);
        assertFalse(response.valid);
        assertNull(response.error);
    }
}
