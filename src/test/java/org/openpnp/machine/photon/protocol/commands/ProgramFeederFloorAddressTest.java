package org.openpnp.machine.photon.protocol.commands;

import org.junit.jupiter.api.Test;
import org.openpnp.machine.photon.protocol.ErrorTypes;
import org.openpnp.machine.photon.protocol.Packet;

import static org.junit.jupiter.api.Assertions.*;

public class ProgramFeederFloorAddressTest {
    protected static String uuid_s = "FFEEDDCCBBAA998877665544";
    protected static int[] uuid_b = {0xFF, 0xEE, 0xDD, 0xCC, 0xBB, 0xAA, 0x99, 0x88, 0x77, 0x66, 0x55, 0x44};


    @Test
    public void createCommand() {
        Packet programFeederFloorAddress = new ProgramFeederFloorAddress(uuid_s, 93).toPacket();
        assertEquals(0xff, programFeederFloorAddress.toAddress);
        assertEquals(14, programFeederFloorAddress.payloadLength);
        assertArrayEquals(new int[]{
                0xC2,
                uuid_b[0], uuid_b[1], uuid_b[2], uuid_b[3],
                uuid_b[4], uuid_b[5], uuid_b[6], uuid_b[7],
                uuid_b[8], uuid_b[9], uuid_b[10], uuid_b[11],
                93,
        }, programFeederFloorAddress.payload);
    }

    @Test
    public void decodeOk() {
        Packet responsePacket = new Packet();
        responsePacket.fromAddress = 24;
        responsePacket.payload = new int[]{0x00};
        responsePacket.calculateCRC();
        ProgramFeederFloorAddress.Response response = new ProgramFeederFloorAddress.Response(responsePacket);

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
        ProgramFeederFloorAddress.Response response = new ProgramFeederFloorAddress.Response(responsePacket);

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
        ProgramFeederFloorAddress.Response response = new ProgramFeederFloorAddress.Response(responsePacket);

        assertEquals(0, response.toAddress);
        assertEquals(17, response.fromAddress);
        assertFalse(response.valid);
        assertNull(response.error);
    }
}
