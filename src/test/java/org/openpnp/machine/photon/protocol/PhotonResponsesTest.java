package org.openpnp.machine.photon.protocol;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.openpnp.machine.photon.protocol.PhotonResponses.*;

public class PhotonResponsesTest {
    protected static String uuid1_s = "00112233445566778899AABB";
    protected static int[] uuid1_b = {0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88, 0x99, 0xAA, 0xBB};
    protected static String uuid2_s = "FFEEDDCCBBAA998877665544";
    protected static int[] uuid2_b = {0xFF, 0xEE, 0xDD, 0xCC, 0xBB, 0xAA, 0x99, 0x88, 0x77, 0x66, 0x55, 0x44};

    private TestResponsesHelper testResponses;
    
    @BeforeEach
    public void setUp() {
        testResponses = new TestResponsesHelper(0);
    }

    @Nested
    public class ErrorsTest {
        @Test
        public void testWrongFeederUUID() {
            Packet error = testResponses.errors.wrongFeederUUID(3, uuid1_s);

            assertEquals(0, error.toAddress);
            assertEquals(3, error.fromAddress);
            assertEquals(0, error.packetId);
            assertEquals(13, error.payloadLength);
            assertEquals(0xCF, error.crc);
            assertArrayEquals(new int[]{
                    0x01, uuid1_b[0], uuid1_b[1], uuid1_b[2], uuid1_b[3],
                    uuid1_b[4], uuid1_b[5], uuid1_b[6], uuid1_b[7],
                    uuid1_b[8], uuid1_b[9], uuid1_b[10], uuid1_b[11]
            }, error.payload);

            assertEquals("0003000DCF01" + uuid1_s, error.toByteString());
        }
        
        @Test
        public void testMotorFault() {
            Packet error = testResponses.errors.motorFault(7);

            assertEquals(0, error.toAddress);
            assertEquals(7, error.fromAddress);
            assertEquals(0, error.packetId);
            assertEquals(1, error.payloadLength);
            assertEquals(0x79, error.crc);
            assertArrayEquals(new int[]{0x02}, error.payload);

            assertEquals("000700017902", error.toByteString());
        }

        @Test
        public void testUninitializedFeeder() {
            Packet error = testResponses.errors.uninitializedFeeder(11, uuid1_s);

            assertEquals(0, error.toAddress);
            assertEquals(11, error.fromAddress);
            assertEquals(0, error.packetId);
            assertEquals(13, error.payloadLength);
            assertEquals(0xF0, error.crc);
            assertArrayEquals(new int[]{
                    0x03, uuid1_b[0], uuid1_b[1], uuid1_b[2], uuid1_b[3],
                    uuid1_b[4], uuid1_b[5], uuid1_b[6], uuid1_b[7],
                    uuid1_b[8], uuid1_b[9], uuid1_b[10], uuid1_b[11]
            }, error.payload);

            assertEquals("000B000DF003" + uuid1_s, error.toByteString());
        }
    }

    @Nested
    public class GetVersionTest {
        @Test
        public void testOk() {
            assertEquals("0003250001F44F", GetVersion.ok(37, 1));
        }

        @Test
        public void decodeOk() {
            String message = "0003250001F44F";
            PacketResponse response = GetVersion.decode(message);

            assertTrue(response.isValid());
            assertTrue(response.isOk());
            assertNull(response.getError());
            assertNull(response.getUuid());
            assertEquals(0, response.getTargetAddress());
            assertEquals(37, response.getFeederAddress());
            assertEquals(1, response.getField(GetVersion.VERSION_FIELD));
        }

        @Test
        public void decodeMissingVersion() {
            String message = "00022500BB74";
            PacketResponse response = GetFeederId.decode(message);

            assertFalse(response.isValid());
            assertFalse(response.isOk());
            assertNull(response.getError());
            assertNull(response.getUuid());
            assertEquals(0, response.getTargetAddress());
            assertEquals(37, response.getFeederAddress());
        }
    }

    @Nested
    public class MoveFeedForwardTest {
        @Test
        public void testOk() {
            assertEquals("00022F00BDD4", MoveFeedForward.ok(47));
        }

        @Test
        public void decodeOk() {
            String message = "00022F00BDD4";
            PacketResponse response = MoveFeedForward.decode(message);

            assertTrue(response.isValid());
            assertTrue(response.isOk());
            assertNull(response.getError());
            assertNull(response.getUuid());
            assertEquals(0, response.getTargetAddress());
            assertEquals(47, response.getFeederAddress());
        }

        @Test
        public void decodeUninitialized() {
            String message = "000E2F03" + uuid1_s + "93E2";
            PacketResponse response = MoveFeedForward.decode(message);

            assertTrue(response.isValid());
            assertFalse(response.isOk());
            assertEquals(ErrorTypes.UNINITIALIZED_FEEDER, response.getError());
            assertEquals(uuid1_s, response.getUuid());
            assertEquals(0, response.getTargetAddress());
            assertEquals(47, response.getFeederAddress());
        }

        @Test
        public void decodeMotorFault() {
            String message = "00022F023C15";
            PacketResponse response = MoveFeedForward.decode(message);

            assertTrue(response.isValid());
            assertFalse(response.isOk());
            assertEquals(ErrorTypes.COULD_NOT_REACH, response.getError());
            assertNull(response.getUuid());
            assertEquals(0, response.getTargetAddress());
            assertEquals(47, response.getFeederAddress());
        }
    }

    @Nested
    public class MoveFeedBackwardTest {
        @Test
        public void testOk() {
            assertEquals("00023900B3B4", MoveFeedBackward.ok(57));
        }

        @Test
        public void decodeOk() {
            String message = "00023900B3B4";
            PacketResponse response = MoveFeedBackward.decode(message);

            assertTrue(response.isValid());
            assertTrue(response.isOk());
            assertNull(response.getError());
            assertNull(response.getUuid());
            assertEquals(0, response.getTargetAddress());
            assertEquals(57, response.getFeederAddress());
        }

        @Test
        public void decodeUninitialized() {
            String message = "000E3903" + uuid1_s + "8434";
            PacketResponse response = MoveFeedBackward.decode(message);

            assertTrue(response.isValid());
            assertFalse(response.isOk());
            assertEquals(ErrorTypes.UNINITIALIZED_FEEDER, response.getError());
            assertEquals(uuid1_s, response.getUuid());
            assertEquals(0, response.getTargetAddress());
            assertEquals(57, response.getFeederAddress());
        }

        @Test
        public void decodeMotorFault() {
            String message = "000239023275";
            PacketResponse response = MoveFeedBackward.decode(message);

            assertTrue(response.isValid());
            assertFalse(response.isOk());
            assertEquals(ErrorTypes.COULD_NOT_REACH, response.getError());
            assertNull(response.getUuid());
            assertEquals(0, response.getTargetAddress());
            assertEquals(57, response.getFeederAddress());
        }
    }

    @Nested
    public class GetFeederAddressTest {
        @Test
        public void testOk() {
            assertEquals(
                    "000E4300" + uuid2_s + "AFCC",
                    GetFeederAddress.ok(67, uuid2_s)
            );
        }

        @Test
        public void decodeOk() {
            String message = "000E4300" + uuid2_s + "AFCC";
            PacketResponse response = GetFeederAddress.decode(message);

            assertTrue(response.isValid());
            assertTrue(response.isOk());
            assertNull(response.getError());
            assertEquals(uuid2_s, response.getUuid());
            assertEquals(0, response.getTargetAddress());
            assertEquals(67, response.getFeederAddress());
        }
    }
}
