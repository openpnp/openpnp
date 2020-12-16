package org.openpnp.machine.index.protocol;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.openpnp.machine.index.protocol.IndexCommands.*;

public class IndexCommandsTest {
    protected static String uuid1 = "00112233445566778899AABB";
    protected static String uuid2 = "FFEEDDCCBBAA998877665544";

    public static class GetFeederIdTest {
        @Test
        public void testEncode() {
            assertEquals("010101E050", GetFeederId.encode(1));
            assertEquals("0201011050", GetFeederId.encode(2));
            assertEquals("0301014190", GetFeederId.encode(3));
        }

        @Test
        public void testOk() {
            assertEquals(
                    "000E1100" + uuid2 + "FC5E",
                    GetFeederId.ok(17, uuid2)
            );
        }
    }

    public static class InitializeFeederTest {
        @Test
        public void testEncode() {
            assertEquals(
                    "010D02" + uuid1 + "020A",
                    InitializeFeeder.encode(1, uuid1)
            );
            assertEquals(
                    "FE0D02" + uuid2 + "E8CB",
                    InitializeFeeder.encode(0xFE, uuid2)
            );
        }

        @Test
        public void testOK() {
            assertEquals("00021B00AB14", InitializeFeeder.ok(27));
        }
    }

    public static class GetVersionTest {
        @Test
        public void testEncode() {
            assertEquals("0101036191", GetVersion.encode(1));
            assertEquals("FE010351A1", GetVersion.encode(254));
        }

        @Test
        public void testOK() {
            assertEquals("0003250001F44F", GetVersion.ok(37, 0x01));
        }
    }

    public static class MoveFeedForwardTest {
        @Test
        public void testEncode() {
            assertEquals("0102047FE338", MoveFeedForward.encode(1, 127));
            assertEquals("0502040A23EF", MoveFeedForward.encode(5, 10));
        }

        @Test
        public void testOK() {
            assertEquals("00022F00BDD4", MoveFeedForward.ok(47));
        }
    }

    public static class MoveFeedBackwardTest {
        @Test
        public void testEncode() {
            assertEquals("0102057FE2A8", MoveFeedBackward.encode(1, 127));
            assertEquals("0502050A227F", MoveFeedBackward.encode(5, 10));
        }

        @Test
        public void testOK() {
            assertEquals("00023900B3B4", MoveFeedBackward.ok(57));
        }
    }

    public static class GetFeederAddressTest {
        @Test
        public void testEncode() {
            assertEquals(
                    "FF0D01" + uuid1 + "7989",
                    GetFeederAddress.encode(uuid1)
            );
            assertEquals(
                    "FF0D01" + uuid2 + "2C08",
                    GetFeederAddress.encode(uuid2)
            );
        }

        @Test
        public void testOK() {
            assertEquals(
                    "000E4300" + uuid2 + "AFCC",
                    GetFeederAddress.ok(67, uuid2)
            );
        }
    }
}
