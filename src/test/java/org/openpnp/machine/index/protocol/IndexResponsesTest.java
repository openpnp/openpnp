package org.openpnp.machine.index.protocol;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
    }

    public static class InitializeFeederTest {
        @Test
        public void testOK() {
            assertEquals("00021B00AB14", InitializeFeeder.ok(27));
        }
    }

    public static class GetVersionTest {
        @Test
        public void testOK() {
            assertEquals("0003250001F44F", GetVersion.ok(37, 0x01));
        }
    }

    public static class MoveFeedForwardTest {
        @Test
        public void testOK() {
            assertEquals("00022F00BDD4", MoveFeedForward.ok(47));
        }
    }

    public static class MoveFeedBackwardTest {
        @Test
        public void testOK() {
            assertEquals("00023900B3B4", MoveFeedBackward.ok(57));
        }
    }

    public static class GetFeederAddressTest {
        @Test
        public void testOK() {
            assertEquals(
                    "000E4300" + uuid2 + "AFCC",
                    GetFeederAddress.ok(67, uuid2)
            );
        }
    }
}
