package org.openpnp.machine.index.protocol;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.openpnp.machine.index.protocol.IndexResponses.*;

public class IndexResponsesTest {
    protected static String uuid1 = "00112233445566778899AABB";
    protected static String uuid2 = "FFEEDDCCBBAA998877665544";

    public static class ErrorsTest {
        @Test
        public void testWrongFeederUUID() {
            assertEquals(
                    "000E0301" + uuid1 + "B94C",
                    Errors.wrongFeederUUID(3, uuid1)
            );
        }
        
        @Test
        public void testMotorFault() {
            assertEquals("000207022215", Errors.motorFault(7));
        }

        @Test
        public void testUninitializedFeeder() {
            assertEquals("00020B03E6D5", Errors.uninitializedFeeder(11));
        }
    }

    public static class GetFeederIdTest {
        @Test
        public void testOk() {
            assertEquals(
                    "000E1100" + uuid2 + "FC5E",
                    GetFeederId.ok(17, uuid2)
            );
        }
        
        @Test
        public void decodeOk() {
            String message = "000E1100" + uuid2 + "FC5E";
            PacketResponse response = GetFeederId.decode(message);

            assertTrue(response.isValid());
            assertTrue(response.isOk());
            assertNull(response.getError());
            assertEquals(uuid2, response.getUuid());
            assertEquals(0, response.getTargetAddress());
            assertEquals(17, response.getFeederAddress());
        }

        @Test
        public void decodeMissingUUID() {
            String message = "00021100ADB4";
            PacketResponse response = GetFeederId.decode(message);

            assertFalse(response.isValid());
            assertFalse(response.isOk());
            assertNull(response.getError());
            assertNull(response.getUuid());
            assertEquals(0, response.getTargetAddress());
            assertEquals(17, response.getFeederAddress());
        }
    }

    public static class InitializeFeederTest {
        @Test
        public void testOK() {
            assertEquals("00021B00AB14", InitializeFeeder.ok(27));
        }

        @Test
        public void decodeOk() {
            String message = "00021B00AB14";
            PacketResponse response = InitializeFeeder.decode(message);

            assertTrue(response.isValid());
            assertTrue(response.isOk());
            assertNull(response.getError());
            assertNull(response.getUuid());
            assertEquals(0, response.getTargetAddress());
            assertEquals(27, response.getFeederAddress());
        }

        @Test
        public void decodeWrongFeederUUID() {
            String message = "000E1B01" + uuid2 + "F4D5";
            PacketResponse response = InitializeFeeder.decode(message);

            assertTrue(response.isValid());
            assertFalse(response.isOk());
            assertEquals(ErrorTypes.WRONG_FEEDER_UUID, response.getError());
            assertEquals(uuid2, response.getUuid());
            assertEquals(0, response.getTargetAddress());
            assertEquals(27, response.getFeederAddress());
        }
    }

    public static class GetVersionTest {
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

    public static class MoveFeedForwardTest {
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
            String message = "000E2F03" + uuid1 + "93E2";
            PacketResponse response = MoveFeedForward.decode(message);

            assertTrue(response.isValid());
            assertFalse(response.isOk());
            assertEquals(ErrorTypes.UNINITIALIZED_FEEDER, response.getError());
            assertEquals(uuid1, response.getUuid());
            assertEquals(0, response.getTargetAddress());
            assertEquals(47, response.getFeederAddress());
        }

        @Test
        public void decodeMotorFault() {
            String message = "00022F023C15";
            PacketResponse response = MoveFeedForward.decode(message);

            assertTrue(response.isValid());
            assertFalse(response.isOk());
            assertEquals(ErrorTypes.MOTOR_FAULT, response.getError());
            assertNull(response.getUuid());
            assertEquals(0, response.getTargetAddress());
            assertEquals(47, response.getFeederAddress());
        }
    }

    public static class MoveFeedBackwardTest {
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
            String message = "000E3903" + uuid1 + "8434";
            PacketResponse response = MoveFeedBackward.decode(message);

            assertTrue(response.isValid());
            assertFalse(response.isOk());
            assertEquals(ErrorTypes.UNINITIALIZED_FEEDER, response.getError());
            assertEquals(uuid1, response.getUuid());
            assertEquals(0, response.getTargetAddress());
            assertEquals(57, response.getFeederAddress());
        }

        @Test
        public void decodeMotorFault() {
            String message = "000239023275";
            PacketResponse response = MoveFeedBackward.decode(message);

            assertTrue(response.isValid());
            assertFalse(response.isOk());
            assertEquals(ErrorTypes.MOTOR_FAULT, response.getError());
            assertNull(response.getUuid());
            assertEquals(0, response.getTargetAddress());
            assertEquals(57, response.getFeederAddress());
        }
    }

    public static class GetFeederAddressTest {
        @Test
        public void testOk() {
            assertEquals(
                    "000E4300" + uuid2 + "AFCC",
                    GetFeederAddress.ok(67, uuid2)
            );
        }

        @Test
        public void decodeOk() {
            String message = "000E4300" + uuid2 + "AFCC";
            PacketResponse response = GetFeederAddress.decode(message);

            assertTrue(response.isValid());
            assertTrue(response.isOk());
            assertNull(response.getError());
            assertEquals(uuid2, response.getUuid());
            assertEquals(0, response.getTargetAddress());
            assertEquals(67, response.getFeederAddress());
        }
    }
}
