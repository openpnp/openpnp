package org.openpnp.machine.photon.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PhotonCommandsTest {
    protected static String uuid1_s = "00112233445566778899AABB";
    protected static int[] uuid1_b = {0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88, 0x99, 0xAA, 0xBB};
    protected static String uuid2_s = "FFEEDDCCBBAA998877665544";
    protected static int[] uuid2_b = {0xFF, 0xEE, 0xDD, 0xCC, 0xBB, 0xAA, 0x99, 0x88, 0x77, 0x66, 0x55, 0x44};

    protected PhotonCommands commands;

    @BeforeEach
    public void setUp() {
        commands = new PhotonCommands(0);
    }

    @Test
    public void testGetFeederID() {
        Packet feederId1 = commands.getFeederId(1);
        assertEquals(1, feederId1.toAddress);
        assertEquals(0, feederId1.fromAddress);
        assertEquals(0, feederId1.packetId);
        assertEquals(1, feederId1.payloadLength);
        assertEquals(0x70, feederId1.crc);
        assertArrayEquals(new int[]{0x01}, feederId1.payload);
        assertEquals("010000017001", feederId1.toByteString());

        Packet feederId2 = commands.getFeederId(2);
        assertEquals(2, feederId2.toAddress);
        assertEquals(0, feederId2.fromAddress);
        assertEquals(1, feederId2.packetId);
        assertEquals(1, feederId2.payloadLength);
        assertEquals(0xBD, feederId2.crc);
        assertArrayEquals(new int[]{0x01}, feederId2.payload);
        assertEquals("02000101BD01", feederId2.toByteString());

        Packet feederId3 = commands.getFeederId(3);
        assertEquals(3, feederId3.toAddress);
        assertEquals(0, feederId3.fromAddress);
        assertEquals(2, feederId3.packetId);
        assertEquals(1, feederId3.payloadLength);
        assertEquals(0x62, feederId3.crc);
        assertArrayEquals(new int[]{0x01}, feederId3.payload);
        assertEquals("030002016201", feederId3.toByteString());

        Packet feederId68 = commands.getFeederId(68);
        assertEquals(68, feederId68.toAddress);
        assertEquals(0, feederId68.fromAddress);
        assertEquals(3, feederId68.packetId);
        assertEquals(1, feederId68.payloadLength);
        assertEquals(0xE8, feederId68.crc);
        assertArrayEquals(new int[]{0x01}, feederId68.payload);
        assertEquals("44000301E801", feederId68.toByteString());
    }

    @Test
    public void testInitializeFeeder() {
        Packet initializeFeeder1 = commands.initializeFeeder(1, uuid1_s);

        assertEquals(1, initializeFeeder1.toAddress);
        assertEquals(0, initializeFeeder1.fromAddress);
        assertEquals(0, initializeFeeder1.packetId);
        assertEquals(13, initializeFeeder1.payloadLength);
        assertEquals(0x7C, initializeFeeder1.crc);
        assertArrayEquals(new int[]{
                0x02, uuid1_b[0], uuid1_b[1], uuid1_b[2], uuid1_b[3],
                uuid1_b[4], uuid1_b[5], uuid1_b[6], uuid1_b[7],
                uuid1_b[8], uuid1_b[9], uuid1_b[10], uuid1_b[11]
        }, initializeFeeder1.payload);

        assertEquals("0100000D7C02" + uuid1_s, initializeFeeder1.toByteString());


        Packet initializeFeederFE = commands.initializeFeeder(0xFE, uuid2_s);

        assertEquals(0xFE, initializeFeederFE.toAddress);
        assertEquals(0, initializeFeederFE.fromAddress);
        assertEquals(1, initializeFeederFE.packetId);
        assertEquals(13, initializeFeederFE.payloadLength);
        assertEquals(0x59, initializeFeederFE.crc);
        assertArrayEquals(new int[]{
                0x02, uuid2_b[0], uuid2_b[1], uuid2_b[2], uuid2_b[3],
                uuid2_b[4], uuid2_b[5], uuid2_b[6], uuid2_b[7],
                uuid2_b[8], uuid2_b[9], uuid2_b[10], uuid2_b[11]
        }, initializeFeederFE.payload);

        assertEquals("FE00010D5902" + uuid2_s, initializeFeederFE.toByteString()
        );
    }

    @Test
    public void testGetVersion() {
        Packet version1 = commands.getVersion(1);

        assertEquals(1, version1.toAddress);
        assertEquals(0, version1.fromAddress);
        assertEquals(0, version1.packetId);
        assertEquals(1, version1.payloadLength);
        assertEquals(0x7E, version1.crc);
        assertArrayEquals(new int[]{0x03}, version1.payload);

        assertEquals("010000017E03", version1.toByteString());


        Packet versionFE = commands.getVersion(0xFE);

        assertEquals(0xFE, versionFE.toAddress);
        assertEquals(0, versionFE.fromAddress);
        assertEquals(1, versionFE.packetId);
        assertEquals(1, versionFE.payloadLength);
        assertEquals(0x2C, versionFE.crc);
        assertArrayEquals(new int[]{0x03}, versionFE.payload);

        assertEquals("FE0001012C03", versionFE.toByteString());
    }

    @Test
    public void testMoveFeedForward() {
        Packet moveFeedForward1 = commands.moveFeedForward(1, 127);

        assertEquals(1, moveFeedForward1.toAddress);
        assertEquals(0, moveFeedForward1.fromAddress);
        assertEquals(0, moveFeedForward1.packetId);
        assertEquals(2, moveFeedForward1.payloadLength);
        assertEquals(0xD1, moveFeedForward1.crc);
        assertArrayEquals(new int[]{0x04, 0x7F}, moveFeedForward1.payload);

        assertEquals("01000002D1047F", moveFeedForward1.toByteString());


        Packet moveFeedForward5 = commands.moveFeedForward(5, 10);

        assertEquals(5, moveFeedForward5.toAddress);
        assertEquals(0, moveFeedForward5.fromAddress);
        assertEquals(1, moveFeedForward5.packetId);
        assertEquals(2, moveFeedForward5.payloadLength);
        assertEquals(0x2F, moveFeedForward5.crc);
        assertArrayEquals(new int[]{0x04, 0x0A}, moveFeedForward5.payload);

        assertEquals("050001022F040A", moveFeedForward5.toByteString());
    }

    @Test
    public void testMoveFeedBackward() {
        Packet moveFeedBackward1 = commands.moveFeedBackward(1, 127);

        assertEquals(1, moveFeedBackward1.toAddress);
        assertEquals(0, moveFeedBackward1.fromAddress);
        assertEquals(0, moveFeedBackward1.packetId);
        assertEquals(2, moveFeedBackward1.payloadLength);
        assertEquals(0xC4, moveFeedBackward1.crc);
        assertArrayEquals(new int[]{0x05, 0x7F}, moveFeedBackward1.payload);

        assertEquals("01000002C4057F", moveFeedBackward1.toByteString());


        Packet moveFeedBackward5 = commands.moveFeedBackward(5, 10);

        assertEquals(5, moveFeedBackward5.toAddress);
        assertEquals(0, moveFeedBackward5.fromAddress);
        assertEquals(1, moveFeedBackward5.packetId);
        assertEquals(2, moveFeedBackward5.payloadLength);
        assertEquals(0x3A, moveFeedBackward5.crc);
        assertArrayEquals(new int[]{0x05, 0x0A}, moveFeedBackward5.payload);

        assertEquals("050001023A050A", moveFeedBackward5.toByteString());
    }

    @Test
    public void testGetFeederAddress() {
        Packet feederAddress1 = commands.getFeederAddress(uuid1_s);

        assertEquals(0xFF, feederAddress1.toAddress);
        assertEquals(0, feederAddress1.fromAddress);
        assertEquals(0, feederAddress1.packetId);
        assertEquals(13, feederAddress1.payloadLength);
        assertEquals(0xB6, feederAddress1.crc);
        assertArrayEquals(new int[]{
                0xC0, uuid1_b[0], uuid1_b[1], uuid1_b[2], uuid1_b[3],
                uuid1_b[4], uuid1_b[5], uuid1_b[6], uuid1_b[7],
                uuid1_b[8], uuid1_b[9], uuid1_b[10], uuid1_b[11]
        }, feederAddress1.payload);

        assertEquals("FF00000DB6C0" + uuid1_s, feederAddress1.toByteString());


        Packet feederAddress2 = commands.getFeederAddress(uuid2_s);

        assertEquals(0xFF, feederAddress2.toAddress);
        assertEquals(0, feederAddress2.fromAddress);
        assertEquals(1, feederAddress2.packetId);
        assertEquals(13, feederAddress2.payloadLength);
        assertEquals(0x72, feederAddress2.crc);
        assertArrayEquals(new int[]{
                0xC0, uuid2_b[0], uuid2_b[1], uuid2_b[2], uuid2_b[3],
                uuid2_b[4], uuid2_b[5], uuid2_b[6], uuid2_b[7],
                uuid2_b[8], uuid2_b[9], uuid2_b[10], uuid2_b[11]
        }, feederAddress2.payload);

        assertEquals("FF00010D72C0" + uuid2_s, feederAddress2.toByteString());
    }
}
