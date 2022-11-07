package org.openpnp.machine.photon.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.openpnp.machine.photon.protocol.PhotonCommands.*;

public class PhotonCommandsTest {
    protected static String uuid1 = "00112233445566778899AABB";
    protected static String uuid2 = "FFEEDDCCBBAA998877665544";

    @Test
    public void testGetFeederID() {
        assertEquals("010101E050", getFeederId(1));
        assertEquals("0201011050", getFeederId(2));
        assertEquals("0301014190", getFeederId(3));
    }

    @Test
    public void testInitializeFeeder() {
        assertEquals(
                "010D02" + uuid1 + "020A",
                initializeFeeder(1, uuid1)
        );
        assertEquals(
                "FE0D02" + uuid2 + "E8CB",
                initializeFeeder(0xFE, uuid2)
        );
    }

    @Test
    public void testGetVersion() {
        assertEquals("0101036191", getVersion(1));
        assertEquals("FE010351A1", getVersion(254));
    }

    @Test
    public void testMoveFeedForward() {
        assertEquals("0102047FE338", moveFeedForward(1, 127));
        assertEquals("0502040A23EF", moveFeedForward(5, 10));
    }

    @Test
    public void testMoveFeedBackward() {
        assertEquals("0102057FE2A8", moveFeedBackward(1, 127));
        assertEquals("0502050A227F", moveFeedBackward(5, 10));
    }

    @Test
    public void testGetFeederAddress() {
        assertEquals("FF0DC0" + uuid1 + "7A09", getFeederAddress(uuid1));
        assertEquals("FF0DC0" + uuid2 + "2F88", getFeederAddress(uuid2));
    }
}
