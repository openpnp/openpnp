package org.openpnp.machine.photon.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openpnp.machine.photon.protocol.helpers.ResponsesHelper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * These tests just exist to verify the TestResponsesHelper which is used in the PhotonFeedersTest
 */
public class ResponsesHelperTest {
    protected static String uuid_s = "FFEEDDCCBBAA998877665544";
    protected static int[] uuid_b = {0xFF, 0xEE, 0xDD, 0xCC, 0xBB, 0xAA, 0x99, 0x88, 0x77, 0x66, 0x55, 0x44};

    private ResponsesHelper testResponses;

    @BeforeEach
    public void setUp() {
        testResponses = new ResponsesHelper(0);
    }


    @Nested
    public class ErrorsTest {
        @Test
        public void testWrongFeederUUID() {
            Packet error = testResponses.errors.wrongFeederUUID(3, uuid_s);

            assertEquals(3, error.fromAddress);
            assertEquals(13, error.payloadLength);
            assertArrayEquals(new int[]{
                    0x01, uuid_b[0], uuid_b[1], uuid_b[2], uuid_b[3],
                    uuid_b[4], uuid_b[5], uuid_b[6], uuid_b[7],
                    uuid_b[8], uuid_b[9], uuid_b[10], uuid_b[11]
            }, error.payload);
        }

        @Test
        public void testMotorFault() {
            Packet error = testResponses.errors.motorFault(7);

            assertEquals(7, error.fromAddress);
            assertEquals(1, error.payloadLength);
            assertArrayEquals(new int[]{0x02}, error.payload);
        }

        @Test
        public void testUninitializedFeeder() {
            Packet error = testResponses.errors.uninitializedFeeder(11, uuid_s);

            assertEquals(0, error.toAddress);
            assertEquals(11, error.fromAddress);
            assertEquals(13, error.payloadLength);
            assertArrayEquals(new int[]{
                    0x03, uuid_b[0], uuid_b[1], uuid_b[2], uuid_b[3],
                    uuid_b[4], uuid_b[5], uuid_b[6], uuid_b[7],
                    uuid_b[8], uuid_b[9], uuid_b[10], uuid_b[11]
            }, error.payload);
        }
    }
    
    @Test
    public void getFeederIdOk() {
        Packet getFeederId17 = testResponses.getFeederId.ok(17, uuid_s);

        assertEquals(17, getFeederId17.fromAddress);
        assertEquals(0, getFeederId17.packetId);
        assertEquals(13, getFeederId17.payloadLength);
        assertArrayEquals(new int[]{
                0x00, uuid_b[0], uuid_b[1], uuid_b[2], uuid_b[3],
                uuid_b[4], uuid_b[5], uuid_b[6], uuid_b[7],
                uuid_b[8], uuid_b[9], uuid_b[10], uuid_b[11]
        }, getFeederId17.payload);
    }
    
    @Test
    public void initializeFeederOk() {
        Packet getFeederId17 = testResponses.initializeFeeder.ok(17, uuid_s);

        assertEquals(17, getFeederId17.fromAddress);
        assertEquals(0, getFeederId17.packetId);
        assertEquals(13, getFeederId17.payloadLength);
        assertArrayEquals(new int[]{
                0x00, uuid_b[0], uuid_b[1], uuid_b[2], uuid_b[3],
                uuid_b[4], uuid_b[5], uuid_b[6], uuid_b[7],
                uuid_b[8], uuid_b[9], uuid_b[10], uuid_b[11]
        }, getFeederId17.payload);
    }

    @Test
    public void getVersionOk() {
        Packet getVersion = testResponses.getVersion.ok(20, 1);

        assertEquals(20, getVersion.fromAddress);
        assertEquals(0, getVersion.packetId);
        assertEquals(2, getVersion.payloadLength);
        assertArrayEquals(new int[]{
                0x00, 0x01,
        }, getVersion.payload);
    }

    @Test
    public void moveFeedForwardOk() {
        Packet getVersion = testResponses.moveFeedForward.ok(20, 258);

        assertEquals(20, getVersion.fromAddress);
        assertEquals(0, getVersion.packetId);
        assertEquals(3, getVersion.payloadLength);
        assertArrayEquals(new int[]{
                0x00, 0x01, 0x02
        }, getVersion.payload);
    }

    @Test
    public void moveFeedBackwardOk() {
        Packet getVersion = testResponses.moveFeedBackward.ok(20, 258);

        assertEquals(20, getVersion.fromAddress);
        assertEquals(0, getVersion.packetId);
        assertEquals(3, getVersion.payloadLength);
        assertArrayEquals(new int[]{
                0x00, 0x01, 0x02
        }, getVersion.payload);
    }

    @Test
    public void moveFeedStatusOk() {
        Packet getVersion = testResponses.moveFeedStatus.ok(20);

        assertEquals(20, getVersion.fromAddress);
        assertEquals(0, getVersion.packetId);
        assertEquals(1, getVersion.payloadLength);
        assertArrayEquals(new int[]{
                0x00
        }, getVersion.payload);
    }

    @Test
    public void getFeederAddressOk() {
        Packet getVersion = testResponses.getFeederAddress.ok(20);

        assertEquals(20, getVersion.fromAddress);
        assertEquals(0, getVersion.packetId);
        assertEquals(1, getVersion.payloadLength);
        assertArrayEquals(new int[]{
                0x00
        }, getVersion.payload);
    }

    @Test
    public void identifyFeederOk() {
        Packet getVersion = testResponses.identifyFeeder.ok(20);

        assertEquals(20, getVersion.fromAddress);
        assertEquals(0, getVersion.packetId);
        assertEquals(1, getVersion.payloadLength);
        assertArrayEquals(new int[]{
                0x00
        }, getVersion.payload);
    }

    @Test
    public void programFeederFloorOk() {
        Packet getVersion = testResponses.moveFeedStatus.ok(20);

        assertEquals(20, getVersion.fromAddress);
        assertEquals(0, getVersion.packetId);
        assertEquals(1, getVersion.payloadLength);
        assertArrayEquals(new int[]{
                0x00
        }, getVersion.payload);
    }
}
